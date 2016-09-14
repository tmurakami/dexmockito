package com.github.tmurakami.dexmockito;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.mock.MockCreationSettings;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class MockClassCacheTest {

    @Mock
    Function<MockCreationSettings<?>, FutureTask<Reference<Class>>> taskFactory;
    @Mock
    FutureTask<Reference<Class>> task;
    @Mock
    Reference<Class> ref;
    @Mock
    MockCreationSettings<C> settings;

    private MockClassCache target;

    private final ConcurrentMap<Integer, Future<Reference<Class>>> cache = new ConcurrentHashMap<>();

    @Before
    public void setUp() {
        target = new MockClassCache(cache, taskFactory);
    }

    @Test
    public void testGenerate() throws Throwable {
        Class[] classes = {null, C.class};
        BDDMockito.BDDMyOngoingStubbing<Class> stubbing = given(ref.get());
        for (Class<?> c : classes) {
            stubbing = stubbing.willReturn(c);
        }
        given(task.get()).willReturn(ref);
        given(taskFactory.apply(any(MockCreationSettings.class))).willReturn(task);
        given(settings.getTypeToMock()).willReturn(C.class);
        int count = 10;
        List<Callable<Class<?>>> tasks = new ArrayList<>(count);
        final MockCreationSettings<C> settings = this.settings;
        for (int i = 0; i < count; i++) {
            tasks.add(new Callable<Class<?>>() {
                @Override
                public Class<?> call() throws Exception {
                    return target.apply(settings);
                }
            });
        }
        Set<Class<?>> result = new HashSet<>();
        for (Future<Class<?>> f : Executors.newFixedThreadPool(count).invokeAll(tasks)) {
            try {
                result.add(f.get());
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        }
        assertEquals(1, result.size());
        assertEquals(C.class, result.iterator().next());
        then(task).should(times(classes.length)).run();
    }

    private static class C {
    }

}
