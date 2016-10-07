package com.github.tmurakami.dexmockito;

import android.os.Build;

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
import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

import dalvik.system.DexFile;

public final class MockMakerImpl implements MockMaker, MockClassResolver {

    private final MockMaker delegate;
    private final MockClassGenerator generator;
    private final FieldSetter objectStreamClassNameFieldSetter;

    public MockMakerImpl() {
        this(new ObjenesisStd(false).newInstance(SubclassByteBuddyMockMaker.class), newMockClassGenerator(), getObjectStreamClassNameFieldSetter());
    }

    private MockMakerImpl(MockMaker delegate, MockClassGenerator generator, FieldSetter objectStreamClassNameFieldSetter) {
        this.delegate = delegate;
        this.generator = generator;
        this.objectStreamClassNameFieldSetter = objectStreamClassNameFieldSetter;
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

    @Override
    public Class resolveMockClass(ObjectStreamClass desc, MockCreationSettings<?> settings) {
        Class<?> c = generateMockClass(settings);
        String name = c.getName();
        if (!name.equals(desc.getName())) {
            objectStreamClassNameFieldSetter.setField(desc, name);
        }
        return c;
    }

    private Class<?> generateMockClass(MockCreationSettings<?> settings) {
        return generator.generateMockClass(settings);
    }

    private static FieldSetter getObjectStreamClassNameFieldSetter() {
        String name = Build.VERSION.SDK_INT < Build.VERSION_CODES.N ? "className" : "name";
        final Field f;
        try {
            f = ObjectStreamClass.class.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        return new FieldSetter() {
            @Override
            public void setField(Object instance, Object value) {
                org.mockito.internal.util.reflection.FieldSetter.setField(instance, f, value);
            }
        };
    }

    private static MockClassGenerator newMockClassGenerator() {
        final ClassLoader classLoader = MockMakerImpl.class.getClassLoader();
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
        return new MockClassGeneratorCache(
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
                });
    }

    private static File getCacheDir(ClassLoader classLoader) {
        String property = System.getProperty("dexmaker.dexcache");
        return property == null ? CacheDir.get(new File("/"), classLoader) : new File(property);
    }

}
