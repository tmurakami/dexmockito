package com.github.tmurakami.dexmockito;

import org.mockito.internal.configuration.plugins.Plugins;
import org.mockito.internal.creation.bytebuddy.SubclassByteBuddyMockMaker;
import org.mockito.internal.creation.instance.Instantiator;
import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;
import org.mockito.plugins.MockMaker;
import org.objenesis.ObjenesisStd;

import java.io.File;
import java.io.IOException;
import java.io.ObjectStreamClass;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

import dalvik.system.DexFile;

public final class DexMockitoMockMaker implements MockMaker {

    private final MockMaker delegate;
    private final DexMockitoMockMakerHelper helper;

    public DexMockitoMockMaker() {
        this(new ObjenesisStd(false).newInstance(SubclassByteBuddyMockMaker.class), newMockMakerHelper());
    }

    private DexMockitoMockMaker(MockMaker delegate, DexMockitoMockMakerHelper helper) {
        this.delegate = delegate;
        this.helper = helper;
    }

    @Override
    public <T> T createMock(MockCreationSettings<T> settings, MockHandler handler) {
        Class<?> c = generateMockClass(settings);
        Instantiator instantiator = Plugins.getInstantiatorProvider().getInstantiator(settings);
        T mock = settings.getTypeToMock().cast(instantiator.newInstance(c));
        resetMock(mock, handler, settings);
        return mock;
    }

    @Override
    public MockHandler getHandler(Object mock) {
        return delegate.getHandler(mock);
    }

    @Override
    public void resetMock(Object mock, MockHandler newHandler, MockCreationSettings settings) {
        delegate.resetMock(mock, newHandler, settings);
    }

    @Override
    public TypeMockability isTypeMockable(Class<?> type) {
        return delegate.isTypeMockable(type);
    }

    Class<?> resolveMockClass(ObjectStreamClass desc, MockCreationSettings<?> settings) {
        Class<?> c = helper.generateMockClass(settings);
        String name = c.getName();
        if (!name.equals(desc.getName())) {
            helper.setName(desc, name);
        }
        return c;
    }

    private <T> Class generateMockClass(MockCreationSettings<T> settings) {
        return helper.generateMockClass(settings);
    }

    private static DexMockitoMockMakerHelper newMockMakerHelper() {
        final ClassLoader classLoader = DexMockitoMockMaker.class.getClassLoader();
        final MockClassGenerator generator = new MockClassGeneratorImpl(
                new ClassLoaderResolver() {
                    @Override
                    public ClassLoader resolveClassLoader(MockCreationSettings<?> settings) {
                        ClassLoader loader = settings.getTypeToMock().getClassLoader();
                        return loader == null || loader == Object.class.getClassLoader() ? classLoader : loader;
                    }
                },
                new DexClassLoadingStrategy(
                        getCacheDir(classLoader),
                        new DexFileLoader() {
                            @Override
                            public DexFile load(String sourcePathName, String outputPathName) throws IOException {
                                return DexFile.loadDex(sourcePathName, outputPathName, 0);
                            }
                        }));
        return new DexMockitoMockMakerHelperImpl(new MockClassGeneratorCache(
                new ConcurrentHashMap<Reference, MockClassGenerator>(),
                new ReferenceQueue<>(),
                new MockClassGeneratorFactory() {
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
                }));
    }

    private static File getCacheDir(ClassLoader classLoader) {
        String property = System.getProperty("dexmaker.dexcache");
        return property == null ? CacheDir.get(new File("/"), classLoader) : new File(property);
    }

}
