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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;

final class ClassLoadingStrategyImpl implements ClassLoadingStrategy {

    private static final String TAG = "dexmockito";

    private final DexOptions dexOptions;
    private final CfOptions cfOptions;
    private final File cacheDir;
    private final RandomString randomString = new RandomString();

    ClassLoadingStrategyImpl(DexOptions dexOptions, CfOptions cfOptions, File cacheDir) {
        this.dexOptions = dexOptions;
        this.cfOptions = cfOptions;
        this.cacheDir = cacheDir;
    }

    @Override
    public Map<TypeDescription, Class<?>> load(ClassLoader classLoader,
                                               Map<TypeDescription, byte[]> types) {
        DexFile dex = new DexFile(dexOptions);
        for (Map.Entry<TypeDescription, byte[]> entry : types.entrySet()) {
            String name = entry.getKey().getName().replace('.', '/') + ".class";
            DirectClassFile f = new DirectClassFile(entry.getValue(), name, false);
            f.setAttributeFactory(StdAttributeFactory.THE_ONE);
            dex.add(CfTranslator.translate(f, entry.getValue(), cfOptions, dexOptions, dex));
        }
        String name = randomString.nextString();
        File jar = new File(cacheDir, name + ".jar");
        try {
            createNewFile(jar);
            writeDexToJar(dex, jar);
            return loadClasses(loadDex(jar, new File(cacheDir, name + ".dex")), classLoader, types);
        } finally {
            if (jar.exists() && !jar.delete()) {
                Logger.getLogger(TAG).warning("Cannot delete " + jar);
            }
        }
    }

    private static void createNewFile(File file) {
        IOException ioe = null;
        try {
            if (file.createNewFile()) {
                return;
            }
        } catch (IOException e) {
            ioe = e;
        }
        throw new IllegalStateException("Cannot create " + file, ioe);
    }

    private static void writeDexToJar(DexFile dex, File jar) {
        JarOutputStream out = null;
        try {
            out = new JarOutputStream(new FileOutputStream(jar));
            out.putNextEntry(new JarEntry("classes.dex"));
            dex.writeTo(out, null, false);
            out.closeEntry();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write classes.dex to " + jar, e);
        } finally {
            IOUtil.closeQuietly(out);
        }
    }

    private static dalvik.system.DexFile loadDex(File src, File out) {
        dalvik.system.DexFile file;
        try {
            file = dalvik.system.DexFile.loadDex(src.getAbsolutePath(), out.getAbsolutePath(), 0);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load dex file from " + src);
        }
        return file;
    }

    private static Map<TypeDescription, Class<?>> loadClasses(dalvik.system.DexFile file,
                                                              ClassLoader classLoader,
                                                              Map<TypeDescription, byte[]> types) {
        Map<TypeDescription, Class<?>> map = new HashMap<>();
        for (TypeDescription td : types.keySet()) {
            Class<?> c = file.loadClass(td.getName(), classLoader);
            if (c == null) {
                throw new IllegalStateException("Cannot load class " + td.getName());
            }
            map.put(td, c);
        }
        return map;
    }

}
