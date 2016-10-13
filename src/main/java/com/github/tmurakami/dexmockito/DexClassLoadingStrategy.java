package com.github.tmurakami.dexmockito;

import com.github.tmurakami.dexmockito.repackaged.com.android.dx.dex.DexFormat;
import com.github.tmurakami.dexmockito.repackaged.com.android.dx.dex.DexOptions;
import com.github.tmurakami.dexmockito.repackaged.com.android.dx.dex.cf.CfOptions;
import com.github.tmurakami.dexmockito.repackaged.com.android.dx.dex.cf.CfTranslator;
import com.github.tmurakami.dexmockito.repackaged.com.android.dx.dex.file.DexFile;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import org.mockito.internal.util.io.IOUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class DexClassLoadingStrategy implements ClassLoadingStrategy {

    private final File cacheDir;
    private final DexFileLoader fileLoader;
    private final DexOptions dexOptions = new DexOptions();
    private final CfOptions cfOptions = new CfOptions();

    DexClassLoadingStrategy(File cacheDir, DexFileLoader fileLoader) {
        this.cacheDir = cacheDir;
        this.fileLoader = fileLoader;
        this.dexOptions.targetApiLevel = DexFormat.API_NO_EXTENDED_OPCODES;
    }

    @Override
    public Map<TypeDescription, Class<?>> load(ClassLoader classLoader,
                                               Map<TypeDescription, byte[]> types) {
        DexFile dxDexFile = new DexFile(dexOptions);
        for (Map.Entry<TypeDescription, byte[]> e : types.entrySet()) {
            String path = e.getKey().getName().replace('.', '/') + ".class";
            dxDexFile.add(CfTranslator.translate(path, e.getValue(), cfOptions, dexOptions));
        }
        File zip = null;
        File dex = null;
        dalvik.system.DexFile dexFile = null;
        try {
            zip = File.createTempFile("classes", ".zip", cacheDir);
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
            try {
                out.putNextEntry(new ZipEntry("classes.dex"));
                dxDexFile.writeTo(out, null, false);
            } finally {
                IOUtil.closeQuietly(out);
            }
            dex = new File(cacheDir, zip.getName() + ".dex");
            dexFile = fileLoader.load(zip.getCanonicalPath(), dex.getCanonicalPath());
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
            closeQuietly(dexFile);
            deleteFiles(zip, dex);
        }
    }

    private static void closeQuietly(dalvik.system.DexFile dexFile) {
        if (dexFile != null) {
            try {
                dexFile.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void deleteFiles(File... files) {
        for (File f : files) {
            if (f.exists() && !f.delete()) {
                Logger.getLogger("com.github.tmurakami.dexmockito").warning("Cannot delete " + f);
            }
        }
    }

}
