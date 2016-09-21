package com.github.tmurakami.dexmockito;

import android.test.AndroidTestCase;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;

@SuppressWarnings("deprecation")
public class CacheTest extends AndroidTestCase {

    public void testCacheDirExists() {
        File cache = new File(getContext().getCacheDir(), "dexmockito");
        assertTrue(!cache.exists() || delete(cache));
        mock(C.class);
        assertTrue(cache.isDirectory());
        assertTrue(cache.canRead());
        assertTrue(cache.canWrite());
    }

    public void testMockClassCaching() {
        Set<Class> classes = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            classes.add(mock(C.class).getClass());
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

    private static class C {
    }

}
