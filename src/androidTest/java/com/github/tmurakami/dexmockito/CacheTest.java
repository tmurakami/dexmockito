package com.github.tmurakami.dexmockito;

import android.test.AndroidTestCase;

import org.mockito.Mockito;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("deprecation")
public class CacheTest extends AndroidTestCase {

    public void testDexCacheDirExists() {
        File cache = new File(getContext().getCacheDir(), "dexmockito");
        assertTrue(!cache.exists() || delete(cache));
        Mockito.mock(C.class);
        assertTrue(cache.isDirectory());
        assertTrue(cache.canRead());
        assertTrue(cache.canWrite());
    }

    public void testMockClassCache() {
        Set<Class> classes = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            classes.add(Mockito.mock(C.class).getClass());
        }
        assertEquals(1, classes.size());
    }

    private static boolean delete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!delete(f)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }

    @SuppressWarnings("WeakerAccess")
    public static class C {
    }

}
