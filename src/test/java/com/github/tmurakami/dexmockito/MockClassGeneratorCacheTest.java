package com.github.tmurakami.dexmockito;

import net.bytebuddy.utility.StreamDrainer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.io.IOUtil;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.mock.MockCreationSettings;

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
    MockClassGeneratorFactory generatorFactory;
    @Mock
    MockClassGenerator generator;
    @Mock
    MockCreationSettings<C> settings;

    private final ConcurrentMap<Reference, MockClassGenerator> cache = new ConcurrentHashMap<>();
    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();
    private MockClassGeneratorCache target;

    @Before
    public void setUp() {
        target = new MockClassGeneratorCache(cache, queue, generatorFactory);
    }

    @Test
    public void testGenerateMockClass() throws Exception {
        int count = 5;
        for (int i = 0; i < 5; i++) {
            Class<C> c = redefineClass(C.class);
            given(generator.generateMockClass(any(MockCreationSettings.class))).willReturn(c);
            given(generatorFactory.newMockClassGenerator()).willReturn(generator);
            given(settings.getTypeToMock()).willReturn(c);
            target.generateMockClass(settings);
        }
        assertEquals(count, cache.size());
        Mockito.reset(generatorFactory, generator, settings);
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
            return new TestClassLoader().defineClass(name, StreamDrainer.DEFAULT.drain(in));
        } finally {
            IOUtil.closeQuietly(in);
        }
    }

    private static class C {
    }

}
