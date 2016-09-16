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
    private final BiFunction<File, File, BiFunction<String, ClassLoader, Class>> dexClassLoaderFactory;
    private final RandomString randomString = new RandomString();

    DexClassLoadingStrategy(DexOptions dexOptions,
                            CfOptions cfOptions,
                            File cacheDir,
                            BiFunction<File, File, BiFunction<String, ClassLoader, Class>> dexClassLoaderFactory) {
        this.dexOptions = dexOptions;
        this.cfOptions = cfOptions;
        this.cacheDir = cacheDir;
        this.dexClassLoaderFactory = dexClassLoaderFactory;
    }

    @Override
    public Map<TypeDescription, Class<?>> load(ClassLoader classLoader,
                                               Map<TypeDescription, byte[]> types) {
        DexFile dex = newDexFile(types);
        String name = randomString.nextString();
        File src = new File(cacheDir, name + ".jar");
        File dst = new File(cacheDir, name + ".dex");
        try {
            createNewFile(src);
            writeDexToJar(dex, src);
            return loadClasses(classLoader, types, dexClassLoaderFactory.apply(src, dst));
        } finally {
            if (src.exists() && !src.delete()) {
                Logger.getLogger(TAG).warning("Cannot delete " + src);
            }
        }
    }

    private DexFile newDexFile(Map<TypeDescription, byte[]> types) {
        DexFile file = new DexFile(dexOptions);
        for (Map.Entry<TypeDescription, byte[]> entry : types.entrySet()) {
            String name = entry.getKey().getName().replace('.', '/') + ".class";
            DirectClassFile f = new DirectClassFile(entry.getValue(), name, false);
            f.setAttributeFactory(StdAttributeFactory.THE_ONE);
            file.add(CfTranslator.translate(f, entry.getValue(), cfOptions, dexOptions, file));
        }
        return file;
    }

    private static void createNewFile(File file) {
        IOException cause = null;
        try {
            if (file.createNewFile()) {
                return;
            }
        } catch (IOException e) {
            cause = e;
        }
        throw new IllegalStateException("Cannot create " + file, cause);
    }

    private static void writeDexToJar(DexFile dex, File jar) {
        JarOutputStream out = null;
        try {
            out = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(jar)));
            out.putNextEntry(new JarEntry("classes.dex"));
            dex.writeTo(out, null, false);
            out.closeEntry();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write classes.dex to " + jar, e);
        } finally {
            IOUtil.closeQuietly(out);
        }
    }

    private static Map<TypeDescription, Class<?>> loadClasses(ClassLoader classLoader,
                                                              Map<TypeDescription, byte[]> types,
                                                              BiFunction<String, ClassLoader, Class> dexClassLoader) {
        Map<TypeDescription, Class<?>> map = new HashMap<>();
        for (TypeDescription td : types.keySet()) {
            Class<?> c = dexClassLoader.apply(td.getName(), classLoader);
            if (c == null) {
                throw new IllegalStateException("Cannot load class " + td.getName());
            }
            map.put(td, c);
        }
        return map;
    }

}
