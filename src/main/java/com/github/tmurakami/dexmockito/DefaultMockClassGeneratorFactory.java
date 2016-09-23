package com.github.tmurakami.dexmockito;

import org.mockito.internal.creation.bytebuddy.ByteBuddyMockClassGenerator;
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

enum DefaultMockClassGeneratorFactory implements MockClassGeneratorFactory {

    INSTANCE;

    @Override
    public MockClassGenerator create() {
        final ClassLoader classLoader = DefaultMockClassGeneratorFactory.class.getClassLoader();
        File cacheDir = CacheDir.get(new File("/data/data"), classLoader);
        final MockClassGenerator mockClassGenerator = new ByteBuddyMockClassGenerator(
                new ClassLoaderResolver() {
                    @Override
                    public ClassLoader resolve(MockCreationSettings<?> settings) {
                        ClassLoader loader = settings.getTypeToMock().getClassLoader();
                        return loader == null || loader == Object.class.getClassLoader() ? classLoader : loader;
                    }
                },
                new DexClassLoadingStrategy(
                        cacheDir,
                        new DexFileLoader() {
                            @Override
                            public DexFile load(File source, File output) throws IOException {
                                return DexFile.loadDex(source.getAbsolutePath(), output.getAbsolutePath(), 0);
                            }
                        }));
        return new MockClassGeneratorCache(
                new ConcurrentHashMap<Reference, MockClassGenerator>(),
                new ReferenceQueue<>(),
                new MockClassGeneratorFactory() {
                    @Override
                    public MockClassGenerator create() {
                        return new MockClassCache(new FutureTaskFactory() {
                            @Override
                            public FutureTask<Reference<Class>> create(final MockCreationSettings<?> settings) {
                                return new FutureTask<>(new Callable<Reference<Class>>() {
                                    @Override
                                    public Reference<Class> call() throws Exception {
                                        return new WeakReference<>(mockClassGenerator.generate(settings));
                                    }
                                });
                            }
                        });
                    }
                });
    }

}
