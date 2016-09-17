package com.github.tmurakami.dexmockito;

import com.android.dex.DexFormat;
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

    private static final String LOGGER_NAME = "com.github.tmurakami.dexmockito";

    private final File cacheDir;
    private final DexFileLoader dexFileLoader;
    private final DexOptions dexOptions = new DexOptions();
    private final CfOptions cfOptions = new CfOptions();
    private final RandomString randomString = new RandomString();

    DexClassLoadingStrategy(File cacheDir, DexFileLoader dexFileLoader) {
        this.cacheDir = cacheDir;
        this.dexFileLoader = dexFileLoader;
        this.dexOptions.targetApiLevel = DexFormat.API_NO_EXTENDED_OPCODES;
    }

    @Override
    public Map<TypeDescription, Class<?>> load(ClassLoader classLoader,
                                               Map<TypeDescription, byte[]> types) {
        DexFile file = new DexFile(dexOptions);
        for (Map.Entry<TypeDescription, byte[]> entry : types.entrySet()) {
            String path = entry.getKey().getName().replace('.', '/') + ".class";
            byte[] bytes = entry.getValue();
            DirectClassFile f = new DirectClassFile(bytes, path, false);
            f.setAttributeFactory(StdAttributeFactory.THE_ONE);
            file.add(CfTranslator.translate(f, bytes, cfOptions, dexOptions, file));
        }
        try {
            return load(file, classLoader, types);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<TypeDescription, Class<?>> load(DexFile file,
                                                ClassLoader classLoader,
                                                Map<TypeDescription, byte[]> types)
            throws IOException, ClassNotFoundException {
        String name = randomString.nextString();
        File src = new File(cacheDir, name + ".jar");
        String out = cacheDir.getAbsolutePath() + '/' + name + ".dex";
        dalvik.system.DexFile dexFile = null;
        try {
            if (!src.createNewFile()) {
                throw new RuntimeException("Cannot create " + src);
            }
            writeDexToJar(file, src);
            dexFile = dexFileLoader.loadDex(src.getAbsolutePath(), out, 0);
            Map<TypeDescription, Class<?>> classMap = new HashMap<>();
            for (TypeDescription d : types.keySet()) {
                classMap.put(d, loadClass(dexFile, d, classLoader));
            }
            return classMap;
        } finally {
            if (dexFile != null) {
                try {
                    dexFile.close();
                } catch (IOException ignored) {
                }
            }
            if (src.exists() && !src.delete()) {
                Logger.getLogger(LOGGER_NAME).warning("Cannot delete " + src);
            }
        }
    }

    private static void writeDexToJar(DexFile dexFile, File file) throws IOException {
        JarOutputStream out = null;
        try {
            out = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            out.putNextEntry(new JarEntry("classes.dex"));
            dexFile.writeTo(out, null, false);
            out.closeEntry();
        } finally {
            IOUtil.closeQuietly(out);
        }
    }

    private static Class<?> loadClass(dalvik.system.DexFile file,
                                      TypeDescription typeDescription,
                                      ClassLoader classLoader) throws ClassNotFoundException {
        Class<?> c = file.loadClass(typeDescription.getName(), classLoader);
        if (c == null) {
            throw new ClassNotFoundException(typeDescription.getName());
        }
        return c;
    }

    interface DexFileLoader {
        dalvik.system.DexFile loadDex(String sourcePathName, String outputPathName, int flags) throws IOException;
    }

}
