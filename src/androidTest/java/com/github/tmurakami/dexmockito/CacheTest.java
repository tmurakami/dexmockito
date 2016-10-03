package com.github.tmurakami.dexmockito;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;

public class CacheTest extends BaseAndroidTestCase {

    public void testCacheDirExists() {
        File cacheDir = new File(getContext().getCacheDir(), "dexmockito");
        assertTrue(!cacheDir.exists() || FileUtils.forceDelete(cacheDir));
        mock(C.class);
        assertTrue(cacheDir.isDirectory());
        assertTrue(cacheDir.canRead());
        assertTrue(cacheDir.canWrite());
    }

    public void testMockClassCaching() {
        Set<Class> classes = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            classes.add(mock(C.class).getClass());
        }
        assertEquals(1, classes.size());
    }

    private static class C {
    }

}
