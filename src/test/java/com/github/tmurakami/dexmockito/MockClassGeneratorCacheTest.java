package com.github.tmurakami.dexmockito;

import net.bytebuddy.utility.StreamDrainer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.io.IOUtil;
import org.mockito.mock.MockCreationSettings;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class MockClassGeneratorCacheTest {

    @Mock
    MockClassGeneratorFactory mockClassGeneratorFactory;
    @Mock
    MockClassGenerator mockClassGenerator;
    @Mock
    MockCreationSettings<C> settings;

    private MockClassGeneratorCache target;

    private final ConcurrentMap<Reference, MockClassGenerator> cache = new ConcurrentHashMap<>();
    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();

    @Before
    public void setUp() {
        target = new MockClassGeneratorCache(cache, queue, mockClassGeneratorFactory);
    }

    @Test
    public void testGenerate() throws Exception {
        int count = 5;
        for (int i = 0; i < 5; i++) {
            Class<C> c = redefineClass(C.class);
            given(mockClassGenerator.generate(any(MockCreationSettings.class))).willReturn(c);
            given(mockClassGeneratorFactory.create()).willReturn(mockClassGenerator);
            given(settings.getTypeToMock()).willReturn(c);
            target.generate(settings);
        }
        assertEquals(count, cache.size());
        Mockito.reset(mockClassGeneratorFactory, mockClassGenerator, settings);
        System.gc();
        Thread.sleep(500);
        for (Reference<?> r; (r = queue.poll()) != null; ) {
            cache.remove(r);
        }
        assertTrue(cache.isEmpty());
    }

    private static <T> Class<T> redefineClass(Class<T> c) throws IOException {
        String name = c.getName();
        InputStream in = c.getResourceAsStream('/' + name.replace('.', '/') + ".class");
        try {
            return new ClassLoader() {
                @SuppressWarnings("unchecked")
                Class<T> defineClass(String name, byte[] bytecode) {
                    return (Class<T>) defineClass(name, bytecode, 0, bytecode.length);
                }
            }.defineClass(name, StreamDrainer.DEFAULT.drain(in));
        } finally {
            IOUtil.closeQuietly(in);
        }
    }

    private static class C {
    }

}
