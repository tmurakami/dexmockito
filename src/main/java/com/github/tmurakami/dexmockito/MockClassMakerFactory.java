package com.github.tmurakami.dexmockito;

import org.mockito.internal.creation.bytebuddy.ByteBuddyMockClassMaker;
import org.mockito.mock.MockCreationSettings;

import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

import dalvik.system.DexFile;

enum MockClassMakerFactory implements MockClassMaker.Factory {

    INSTANCE;

    @Override
    public MockClassMaker get() {
        final ClassLoader classLoader = MockClassMakerFactory.class.getClassLoader();
        File cacheDir = CacheDir.get(new File("/data/data"), classLoader);
        final MockClassMaker mockClassMaker = new ByteBuddyMockClassMaker(
                new ByteBuddyMockClassMaker.ClassLoaderResolver() {
                    @Override
                    public ClassLoader apply(MockCreationSettings<?> settings) {
                        ClassLoader loader = settings.getTypeToMock().getClassLoader();
                        return loader == null || loader == Object.class.getClassLoader() ? classLoader : loader;
                    }
                },
                new DexClassLoadingStrategy(
                        cacheDir,
                        new DexClassLoadingStrategy.DexFileLoader() {
                            @Override
                            public DexFile loadDex(String sourcePathName, String outputPathName, int flags) throws IOException {
                                return DexFile.loadDex(sourcePathName, outputPathName, flags);
                            }
                        }));
        return new MockClassMakerCache(
                new ConcurrentHashMap<Reference, MockClassMaker>(),
                new ReferenceQueue<>(),
                new MockClassMaker.Factory() {
                    @Override
                    public MockClassMaker get() {
                        return new MockClassCache(new MockClassCache.TaskFactory() {
                            @Override
                            public FutureTask<Reference<Class>> apply(final MockCreationSettings<?> settings) {
                                return new FutureTask<>(new Callable<Reference<Class>>() {
                                    @Override
                                    public Reference<Class> call() throws Exception {
                                        return new WeakReference<>(mockClassMaker.apply(settings));
                                    }
                                });
                            }
                        });
                    }
                });
    }

}
