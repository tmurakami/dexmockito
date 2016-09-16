package com.github.tmurakami.dexmockito;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.android.AndroidClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;

import org.mockito.internal.creation.bytebuddy.ByteBuddyMockClassMaker;
import org.mockito.mock.MockCreationSettings;

import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

final class MockClassMakerFactory {

    private MockClassMakerFactory() {
        throw new AssertionError("Do not instantiate");
    }

    static MockClassMaker newMockClassMaker() {
        return new MockClassMakerCache(
                new ConcurrentHashMap<Reference, MockClassMaker>(),
                new ReferenceQueue<>(),
                newMockClassCacheFactory(newByteBuddyMockClassMaker(MockClassMakerFactory.class.getClassLoader())));
    }

    private static MockClassMaker newByteBuddyMockClassMaker(final ClassLoader classLoader) {
        return new ByteBuddyMockClassMaker(
                new ByteBuddy()
                        .with(ClassFileVersion.JAVA_V6)
                        .with(TypeValidation.DISABLED)
                        .with(new NamingStrategy.SuffixingRandom("MockitoMock", NamingStrategy.SuffixingRandom.BaseNameResolver.ForUnnamedType.INSTANCE, "codegen")),
                new Function<MockCreationSettings<?>, ClassLoader>() {
                    @Override
                    public ClassLoader apply(MockCreationSettings<?> settings) {
                        ClassLoader loader = settings.getTypeToMock().getClassLoader();
                        return loader == null || loader == Object.class.getClassLoader() ? classLoader : loader;
                    }
                },
                new AndroidClassLoadingStrategy(CacheDir.get(new File("/data/data"), classLoader)));
    }

    private static Supplier<MockClassMaker> newMockClassCacheFactory(final MockClassMaker mockClassMaker) {
        return new Supplier<MockClassMaker>() {
            @Override
            public MockClassMaker get() {
                return new MockClassCache(
                        new ConcurrentHashMap<Integer, Future<Reference<Class>>>(),
                        new Function<MockCreationSettings<?>, FutureTask<Reference<Class>>>() {
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
        };
    }

}
