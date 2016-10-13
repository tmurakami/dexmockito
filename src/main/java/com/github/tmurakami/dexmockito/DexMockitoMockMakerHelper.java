package com.github.tmurakami.dexmockito;

import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import org.mockito.mock.MockCreationSettings;

import java.io.File;
import java.io.IOException;
import java.io.ObjectStreamClass;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.FutureTask;

import dalvik.system.DexFile;

abstract class DexMockitoMockMakerHelper {

    abstract Class generateMockClass(MockCreationSettings<?> settings);

    abstract Class resolveMockClass(ObjectStreamClass desc, MockCreationSettings<?> settings);

    static DexMockitoMockMakerHelper create() {
        return new DexMockitoMockMakerHelperImpl(newMockClassGeneratorCache(newMockClassGenerator()));
    }

    private static MockClassGenerator newMockClassGenerator() {
        ClassLoaderResolver classLoaderResolver = newClassLoaderResolver();
        ClassLoadingStrategy classLoadingStrategy = new DexClassLoadingStrategy(getCacheDir(), newDexFileLoader());
        return new ByteBuddyMockClassGenerator(classLoaderResolver, classLoadingStrategy);
    }

    private static ClassLoaderResolver newClassLoaderResolver() {
        return new ClassLoaderResolver() {
            @Override
            public ClassLoader resolveClassLoader(MockCreationSettings<?> settings) {
                ClassLoader classLoader = settings.getTypeToMock().getClassLoader();
                if (classLoader == null || classLoader == Object.class.getClassLoader()) {
                    classLoader = Thread.currentThread().getContextClassLoader();
                }
                return classLoader == null ? getClass().getClassLoader() : classLoader;
            }
        };
    }

    private static File getCacheDir() {
        String property = System.getProperty("dexmaker.dexcache", "");
        if (property.length() > 0) {
            return new File(property);
        } else {
            return CacheDir.get(new File("/"), DexMockitoMockMakerHelper.class.getClassLoader());
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

    private static MockClassGeneratorCache newMockClassGeneratorCache(MockClassGenerator generator) {
        ConcurrentMap<Reference, MockClassGenerator> cache = new ConcurrentHashMap<>();
        return new MockClassGeneratorCache(cache, new ReferenceQueue<>(), newMockClassGeneratorFactory(generator));
    }

    private static MockClassGeneratorFactory newMockClassGeneratorFactory(final MockClassGenerator generator) {
        return new MockClassGeneratorFactory() {
            @Override
            public MockClassGenerator newMockClassGenerator() {
                return new MockClassCache(new FutureTaskFactory() {
                    @Override
                    public FutureTask<Reference<Class>> newFutureTask(final MockCreationSettings<?> settings) {
                        return new FutureTask<>(new Callable<Reference<Class>>() {
                            @Override
                            public Reference<Class> call() throws Exception {
                                return new WeakReference<>(generator.generateMockClass(settings));
                            }
                        });
                    }
                });
            }
        };
    }

}
