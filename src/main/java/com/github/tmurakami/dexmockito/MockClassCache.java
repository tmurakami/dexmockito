package com.github.tmurakami.dexmockito;

import org.mockito.mock.MockCreationSettings;

import java.lang.ref.Reference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

final class MockClassCache implements MockClassGenerator {

    private final FutureTaskFactory futureTaskFactory;
    private final ConcurrentMap<Integer, Future<Reference<Class>>> cache = new ConcurrentHashMap<>();

    MockClassCache(FutureTaskFactory futureTaskFactory) {
        this.futureTaskFactory = futureTaskFactory;
    }

    @Override
    public Class generate(MockCreationSettings<?> settings) {
        int hash = settings.getTypeToMock().hashCode();
        hash = 31 * hash + settings.getExtraInterfaces().hashCode();
        Integer key = hash;
        Future<Reference<Class>> future = cache.get(key);
        FutureTask<Reference<Class>> task = null;
        while (true) {
            if (future != null) {
                Class<?> c = get(future).get();
                if (c != null) {
                    return c;
                }
            }
            if (task == null) {
                task = futureTaskFactory.create(settings);
            }
            if (future != null) {
                if (!cache.replace(key, future, task)) {
                    future = cache.get(key);
                    continue;
                }
            } else if ((future = cache.putIfAbsent(key, task)) != null) {
                continue;
            }
            task.run();
            future = task;
        }
    }

    private static <T> T get(Future<T> future) {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    return future.get();
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof Error) {
                        throw (Error) cause;
                    } else if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    } else {
                        throw new RuntimeException(cause);
                    }
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
