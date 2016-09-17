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

    private static final String TAG = "dexmockito";

    private final DexOptions dexOptions;
    private final CfOptions cfOptions;
    private final File cacheDir;
    private final BiFunction<File, File, DexLoader> dexLoaderFactory;
    private final RandomString randomString = new RandomString();

    DexClassLoadingStrategy(DexOptions dexOptions,
                            CfOptions cfOptions,
                            File cacheDir,
                            BiFunction<File, File, DexLoader> dexLoaderFactory) {
        this.dexOptions = dexOptions;
        this.cfOptions = cfOptions;
        this.cacheDir = cacheDir;
        this.dexLoaderFactory = dexLoaderFactory;
    }

    @Override
    public Map<TypeDescription, Class<?>> load(ClassLoader classLoader,
                                               Map<TypeDescription, byte[]> types) {
        DexFile dexFile = new DexFile(dexOptions);
        for (Map.Entry<TypeDescription, byte[]> entry : types.entrySet()) {
            String name = entry.getKey().getName().replace('.', '/') + ".class";
            byte[] value = entry.getValue();
            DirectClassFile f = new DirectClassFile(value, name, false);
            f.setAttributeFactory(StdAttributeFactory.THE_ONE);
            dexFile.add(CfTranslator.translate(f, value, cfOptions, dexOptions, dexFile));
        }
        String name = randomString.nextString();
        File jar = new File(cacheDir, name + ".jar");
        DexLoader dexLoader = null;
        try {
            createNewFile(jar);
            writeDex(dexFile, jar);
            dexLoader = dexLoaderFactory.apply(jar, new File(cacheDir, name + ".dex"));
            Map<TypeDescription, Class<?>> classMap = new HashMap<>();
            for (TypeDescription td : types.keySet()) {
                classMap.put(td, dexLoader.apply(td.getName(), classLoader));
            }
            return classMap;
        } finally {
            IOUtil.closeQuietly(dexLoader);
            if (jar.exists() && !jar.delete()) {
                Logger.getLogger(TAG).warning("Cannot delete " + jar);
            }
        }
    }

    private static void createNewFile(File file) {
        try {
            if (!file.createNewFile()) {
                throw new RuntimeException("Cannot create " + file);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeDex(com.android.dx.dex.file.DexFile dexFile, File file) {
        JarOutputStream out = null;
        try {
            out = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            out.putNextEntry(new JarEntry("classes.dex"));
            dexFile.writeTo(out, null, false);
            out.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtil.closeQuietly(out);
        }
    }

}
