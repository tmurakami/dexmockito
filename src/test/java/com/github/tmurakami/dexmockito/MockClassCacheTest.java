package com.github.tmurakami.dexmockito;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.mock.MockCreationSettings;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class MockClassCacheTest {

    @Mock
    FutureTaskFactory futureTaskFactory;
    @Mock
    FutureTask<Reference<Class>> futureTask;
    @Mock
    Reference<Class> reference;
    @Mock
    MockCreationSettings<C> settings;

    @InjectMocks
    MockClassCache target;

    @Test
    public void testGenerate() throws Throwable {
        Class[] classes = {null, C.class};
        BDDMockito.BDDMyOngoingStubbing<Class> stubbing = given(reference.get());
        for (Class<?> c : classes) {
            stubbing = stubbing.willReturn(c);
        }
        given(futureTask.get()).willReturn(reference);
        given(futureTaskFactory.create(settings)).willReturn(futureTask);
        given(settings.getTypeToMock()).willReturn(C.class);
        int count = 10;
        List<Callable<Class<?>>> tasks = new ArrayList<>(count);
        final MockCreationSettings<C> settings = this.settings;
        for (int i = 0; i < count; i++) {
            tasks.add(new Callable<Class<?>>() {
                @Override
                public Class<?> call() throws Exception {
                    return target.generate(settings);
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
        then(futureTask).should(times(classes.length)).run();
        assertEquals(1, result.size());
        assertEquals(C.class, result.iterator().next());
    }

    private static class C {
    }

}
