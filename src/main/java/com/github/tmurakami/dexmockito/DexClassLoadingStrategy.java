package com.github.tmurakami.dexmockito;

import com.android.dx.cf.direct.DirectClassFile;
import com.android.dx.cf.direct.StdAttributeFactory;
import com.android.dx.dex.DexOptions;
import com.android.dx.dex.cf.CfOptions;
import com.android.dx.dex.cf.CfTranslator;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.utility.RandomString;

import org.mockito.internal.util.io.IOUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;

import dalvik.system.DexFile;

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
        com.android.dx.dex.file.DexFile dexFile = new com.android.dx.dex.file.DexFile(dexOptions);
        for (Map.Entry<TypeDescription, byte[]> e : types.entrySet()) {
            String path = e.getKey().getName().replace('.', '/') + ".class";
            byte[] bytes = e.getValue();
            DirectClassFile cf = new DirectClassFile(bytes, path, false);
            cf.setAttributeFactory(StdAttributeFactory.THE_ONE);
            dexFile.add(CfTranslator.translate(cf, bytes, cfOptions, dexOptions, dexFile));
        }
        String fileName = randomString.nextString();
        File[] files = new File[2];
        files[0] = new File(cacheDir, fileName + ".jar");
        files[1] = new File(cacheDir, fileName + ".dex");
        DexFile loadedDexFile = null;
        try {
            JarOutputStream out = new JarOutputStream(new FileOutputStream(files[0]));
            try {
                out.putNextEntry(new JarEntry("classes.dex"));
                dexFile.writeTo(out, null, false);
            } finally {
                IOUtil.closeQuietly(out);
            }
            loadedDexFile = dexFileLoader.load(files[0], files[1]);
            Map<TypeDescription, Class<?>> classMap = new HashMap<>();
            for (TypeDescription td : types.keySet()) {
                String name = td.getName();
                loadedDexFile.loadClass(name, classLoader);
                classMap.put(td, Class.forName(name, false, classLoader));
            }
            return classMap;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (loadedDexFile != null) {
                try {
                    loadedDexFile.close();
                } catch (IOException ignored) {
                }
            }
            for (File f : files) {
                if (f.exists() && !f.delete()) {
                    Logger.getLogger("com.github.tmurakami.dexmockito").warning("Cannot delete " + f);
                }
            }
        }
    }

}
