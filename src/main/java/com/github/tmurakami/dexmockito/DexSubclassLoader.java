package com.github.tmurakami.dexmockito;

import android.support.test.InstrumentationRegistry;

import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import org.mockito.internal.creation.bytebuddy.SubclassLoader;

import java.io.File;
import java.io.IOException;

import dalvik.system.DexFile;

final class DexSubclassLoader implements SubclassLoader {

    @Override
    public ClassLoadingStrategy<ClassLoader> getStrategy(Class<?> mockedType) {
        File dexCacheDir = getDexCacheDir();
        if (!dexCacheDir.exists() && !dexCacheDir.mkdirs()) {
            throw new Error("Cannot access DexMockito cache directory");
        }
        return new DexClassLoadingStrategy(dexCacheDir, newDexFileLoader());
    }

    private static File getDexCacheDir() {
        String property = System.getProperty("dexmaker.dexcache", "");
        return property.length() > 0 ? new File(property) : new File(getCacheDir(), "dexmockito");
    }

    private static File getCacheDir() {
        try {
            return InstrumentationRegistry.getTargetContext().getCacheDir();
        } catch (NoClassDefFoundError e) {
            return CacheDir.get(new File("/"), DexSubclassLoader.class.getClassLoader());
        }
    }

    private static DexFileLoader newDexFileLoader() {
        return new DexFileLoader() {
            @Override
            public DexFile load(String sourcePathName, String outputPathName) throws IOException {
                return DexFile.loadDex(sourcePathName, outputPathName, 0);
            }
        };
    }

}
