package com.github.tmurakami.dexmockito;

import org.mockito.mock.MockCreationSettings;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentMap;

final class MockClassMakerCache implements MockClassMaker {

    private static final Object NULL = new Object();

    private final ConcurrentMap<Reference, MockClassMaker> cache;
    private final ReferenceQueue<Object> queue;
    private final Factory mockClassMakerFactory;

    MockClassMakerCache(ConcurrentMap<Reference, MockClassMaker> cache,
                        ReferenceQueue<Object> queue,
                        Factory mockClassMakerFactory) {
        this.cache = cache;
        this.queue = queue;
        this.mockClassMakerFactory = mockClassMakerFactory;
    }

    @Override
    public Class apply(MockCreationSettings<?> settings) {
        for (Reference<?> r; (r = queue.poll()) != null; ) {
            cache.remove(r);
        }
        ClassLoader loader = settings.getTypeToMock().getClassLoader();
        Key key = new Key(loader == null ? NULL : loader, queue);
        MockClassMaker maker = cache.get(key);
        if (maker == null) {
            MockClassMaker newMaker = mockClassMakerFactory.get();
            if ((maker = cache.putIfAbsent(key, newMaker)) == null) {
                maker = newMaker;
            }
        }
        return maker.apply(settings);
    }

    private static class Key extends WeakReference<Object> {

        private final int hashCode;

        private Key(Object o, ReferenceQueue<? super Object> q) {
            super(o, q);
            hashCode = System.identityHashCode(o);
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
