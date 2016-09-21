package com.github.tmurakami.dexmockito;

import org.mockito.mock.MockCreationSettings;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentMap;

final class MockClassGeneratorCache implements MockClassGenerator {

    private static final Object NULL = new Object();

    private final ConcurrentMap<Reference, MockClassGenerator> cache;
    private final ReferenceQueue<Object> queue;
    private final MockClassGeneratorFactory mockClassGeneratorFactory;

    MockClassGeneratorCache(ConcurrentMap<Reference, MockClassGenerator> cache,
                            ReferenceQueue<Object> queue,
                            MockClassGeneratorFactory mockClassGeneratorFactory) {
        this.cache = cache;
        this.queue = queue;
        this.mockClassGeneratorFactory = mockClassGeneratorFactory;
    }

    @Override
    public Class generate(MockCreationSettings<?> settings) {
        for (Reference<?> r; (r = queue.poll()) != null; ) {
            cache.remove(r);
        }
        ClassLoader loader = settings.getTypeToMock().getClassLoader();
        Key key = new Key(loader == null ? NULL : loader, queue);
        MockClassGenerator generator = cache.get(key);
        if (generator == null) {
            MockClassGenerator newGenerator = mockClassGeneratorFactory.create();
            if ((generator = cache.putIfAbsent(key, newGenerator)) == null) {
                generator = newGenerator;
            }
        }
        return generator.generate(settings);
    }

    private static class Key extends WeakReference<Object> {

        private final int hashCode;

        Key(Object referent, ReferenceQueue<? super Object> q) {
            super(referent, q);
            hashCode = System.identityHashCode(referent);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o != null && getClass() == o.getClass() && hashCode == ((Key) o).hashCode;
        }

    }

}
