package com.github.tmurakami.dexmockito;

import com.android.dx.dex.DexFormat;
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
        dexOptions.targetApiLevel = DexFormat.API_NO_EXTENDED_OPCODES;
        CfOptions cfOptions = new CfOptions();
        com.android.dx.dex.file.DexFile dxDexFile = new com.android.dx.dex.file.DexFile(dexOptions);
        for (Map.Entry<TypeDescription, byte[]> e : types.entrySet()) {
            String path = e.getKey().getName().replace('.', '/') + ".class";
            dxDexFile.add(CfTranslator.translate(path, e.getValue(), cfOptions, dexOptions));
        }
        String fileName = randomString.nextString();
        File[] files = new File[2];
        files[0] = new File(cacheDir, fileName + ".jar");
        files[1] = new File(cacheDir, fileName + ".dex");
        DexFile dexFile = null;
        try {
            JarOutputStream out = new JarOutputStream(new FileOutputStream(files[0]));
            try {
                out.putNextEntry(new JarEntry("classes.dex"));
                dxDexFile.writeTo(out, null, false);
            } finally {
                IOUtil.closeQuietly(out);
            }
            dexFile = dexFileLoader.load(files[0].getCanonicalPath(), files[1].getCanonicalPath());
            Map<TypeDescription, Class<?>> classMap = new HashMap<>();
            for (TypeDescription td : types.keySet()) {
                String name = td.getName();
                dexFile.loadClass(name, classLoader);
                classMap.put(td, Class.forName(name, false, classLoader));
            }
            return classMap;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (dexFile != null) {
                try {
                    dexFile.close();
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
