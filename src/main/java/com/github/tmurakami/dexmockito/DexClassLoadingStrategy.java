package com.github.tmurakami.dexmockito;

import com.android.dx.cf.direct.DirectClassFile;
import com.android.dx.cf.direct.StdAttributeFactory;
import com.android.dx.dex.DexOptions;
import com.android.dx.dex.cf.CfOptions;
import com.android.dx.dex.cf.CfTranslator;
import com.android.dx.dex.file.DexFile;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.utility.RandomString;

import org.mockito.internal.util.io.IOUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;

final class DexClassLoadingStrategy implements ClassLoadingStrategy {

    private final File cacheDir;
    private final DexFileLoader dexFileLoader;
    private final RandomString randomString = new RandomString();

    DexClassLoadingStrategy(File cacheDir, DexFileLoader dexFileLoader) {
        this.cacheDir = cacheDir;
        this.dexFileLoader = dexFileLoader;
    }

    @Override
    public Map<TypeDescription, Class<?>> load(ClassLoader classLoader,
                                               Map<TypeDescription, byte[]> types) {
        DexOptions dexOptions = new DexOptions();
        CfOptions cfOptions = new CfOptions();
        DexFile dexFile = new DexFile(dexOptions);
        for (Map.Entry<TypeDescription, byte[]> entry : types.entrySet()) {
            String path = entry.getKey().getName().replace('.', '/') + ".class";
            byte[] bytes = entry.getValue();
            DirectClassFile cf = new DirectClassFile(bytes, path, false);
            cf.setAttributeFactory(StdAttributeFactory.THE_ONE);
            dexFile.add(CfTranslator.translate(cf, bytes, cfOptions, dexOptions, dexFile));
        }
        String name = randomString.nextString();
        File jar = new File(cacheDir, name + ".jar");
        JarOutputStream out = null;
        dalvik.system.DexFile loadedFile = null;
        try {
            out = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(jar)));
            out.putNextEntry(new JarEntry("classes.dex"));
            dexFile.writeTo(out, null, false);
            out.close();
            loadedFile = dexFileLoader.loadDex(jar, new File(cacheDir, name + ".dex"), 0);
            Map<TypeDescription, Class<?>> classMap = new HashMap<>();
            for (TypeDescription td : types.keySet()) {
                classMap.put(td, loadClass(loadedFile, td, classLoader));
            }
            return classMap;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (loadedFile != null) {
                try {
                    loadedFile.close();
                } catch (IOException ignored) {
                }
            }
            IOUtil.closeQuietly(out);
            if (jar.exists() && !jar.delete()) {
                Logger.getLogger("com.github.tmurakami.dexmockito").warning("Cannot delete " + jar);
            }
        }
    }

    private static Class<?> loadClass(dalvik.system.DexFile dexFile,
                                      TypeDescription typeDescription,
                                      ClassLoader classLoader) throws ClassNotFoundException {
        Class<?> c = dexFile.loadClass(typeDescription.getName(), classLoader);
        if (c == null) {
            throw new ClassNotFoundException(typeDescription.getName());
        }
        return c;
    }

    interface DexFileLoader {
        dalvik.system.DexFile loadDex(File sourceFile, File outputFile, int flags) throws IOException;
    }

}
