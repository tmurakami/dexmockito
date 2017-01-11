package com.github.tmurakami.dexmockito;

import android.support.test.InstrumentationRegistry;
import android.text.TextUtils;

import net.bytebuddy.android.AndroidClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import org.mockito.internal.creation.bytebuddy.SubclassLoader;

import java.io.File;

final class DexSubclassLoader implements SubclassLoader {

    private static final String[] CACHE_PROPERTY_KEYS = {
            "dexmaker.dexcache",
            "org.mockito.android.target",
    };

    @Override
    public ClassLoadingStrategy<ClassLoader> getStrategy(Class<?> mockedType) {
        File dexCacheDir = getDexCacheDir();
        if (!dexCacheDir.exists() && !dexCacheDir.mkdirs()) {
            throw new Error("Cannot access DexMockito cache directory");
        }
        return new AndroidClassLoadingStrategy.Injecting(dexCacheDir);
    }

    private static File getDexCacheDir() {
        for (String key : CACHE_PROPERTY_KEYS) {
            String property = System.getProperty(key);
            if (!TextUtils.isEmpty(property)) {
                return new File(property);
            }
        }
        return new File(getCacheDir(), "dexmockito");
    }

    private static File getCacheDir() {
        try {
            return InstrumentationRegistry.getTargetContext().getCacheDir();
        } catch (NoClassDefFoundError e) {
            return CacheDir.get(new File("/"), DexSubclassLoader.class.getClassLoader());
        }
    }

}
