package com.github.tmurakami.dexmockito;

import com.android.dex.DexFormat;
import com.android.dx.dex.DexOptions;
import com.android.dx.dex.cf.CfOptions;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;

import org.mockito.internal.creation.bytebuddy.ByteBuddyMockClassMaker;
import org.mockito.mock.MockCreationSettings;

import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import dalvik.system.DexFile;

final class MockClassMakerFactory {

    private MockClassMakerFactory() {
        throw new AssertionError("Do not instantiate");
    }

    static MockClassMaker newMockClassMaker() {
        return new MockClassMakerCache(
                new ConcurrentHashMap<Reference, MockClassMaker>(),
                new ReferenceQueue<>(),
                new MockClassCacheFactory(newByteBuddyMockClassMaker(MockClassMakerFactory.class.getClassLoader())));
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
                newClassLoadingStrategy(classLoader));
    }

    private static ClassLoadingStrategy newClassLoadingStrategy(ClassLoader classLoader) {
        DexOptions dexOptions = new DexOptions();
        dexOptions.targetApiLevel = DexFormat.API_NO_EXTENDED_OPCODES;
        File cacheDir = CacheDir.get(new File("/data/data"), classLoader);
        return new DexClassLoadingStrategy(dexOptions, new CfOptions(), cacheDir, new DexLoaderFactory());
    }

    private static class MockClassCacheFactory implements Supplier<MockClassMaker> {

        private final MockClassMaker mockClassMaker;

        private MockClassCacheFactory(MockClassMaker mockClassMaker) {
            this.mockClassMaker = mockClassMaker;
        }

        @Override
        public MockClassMaker get() {
            final MockClassMaker maker = mockClassMaker;
            return new MockClassCache(
                    new ConcurrentHashMap<Integer, Future<Reference<Class>>>(),
                    new Function<MockCreationSettings<?>, FutureTask<Reference<Class>>>() {
                        @Override
                        public FutureTask<Reference<Class>> apply(final MockCreationSettings<?> settings) {
                            return new FutureTask<>(new Callable<Reference<Class>>() {
                                @Override
                                public Reference<Class> call() throws Exception {
                                    return new WeakReference<>(maker.apply(settings));
                                }
                            });
                        }
                    });
        }

    }

    private static class DexLoaderFactory implements BiFunction<File, File, DexLoader> {

        @Override
        public DexLoader apply(File src, File out) {
            final DexFile file = loadDex(src, out);
            return new DexLoader() {
                @Override
                public void close() throws IOException {
                    file.close();
                }

                @Override
                public Class apply(String name, ClassLoader classLoader) {
                    Class<?> c = file.loadClass(name, classLoader);
                    if (c == null) {
                        throw new RuntimeException("Cannot find class " + name);
                    }
                    return c;
                }
            };
        }

        private DexFile loadDex(File src, File out) {
            try {
                return DexFile.loadDex(src.getAbsolutePath(), out.getAbsolutePath(), 0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
