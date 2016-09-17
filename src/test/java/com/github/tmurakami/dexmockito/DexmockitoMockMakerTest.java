package com.github.tmurakami.dexmockito;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;
import org.mockito.plugins.MockMaker;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@RunWith(MockitoJUnitRunner.class)
public class DexmockitoMockMakerTest {

    @Mock
    MockMaker delegate;
    @Mock
    MockClassMaker mockClassMaker;
    @Mock
    MockCreationSettings<C> settings;
    @Mock
    MockHandler handler;
    @Mock
    MockMaker.TypeMockability typeMockability;

    @InjectMocks
    DexmockitoMockMaker target;

    @Test
    public void testCreateMock() {
        Class<C> c = C.class;
        given(mockClassMaker.apply(settings)).willReturn(c);
        given(settings.getTypeToMock()).willReturn(c);
        Object mock = target.createMock(settings, handler);
        assertTrue(c.isInstance(mock));
        then(delegate).should().resetMock(mock, handler, settings);
    }

    @Test
    public void testGetHandler() {
        Object o = new Object();
        given(delegate.getHandler(o)).willReturn(handler);
        assertEquals(handler, target.getHandler(o));
    }

    @Test
    public void testResetMock() {
        Object o = new Object();
        target.resetMock(o, handler, settings);
        then(delegate).should().resetMock(o, handler, settings);
    }

    @Test
    public void testIsTypeMockable() {
        given(delegate.isTypeMockable(C.class)).willReturn(typeMockability);
        assertEquals(typeMockability, target.isTypeMockable(C.class));
    }

    private static class C {
    }

}
