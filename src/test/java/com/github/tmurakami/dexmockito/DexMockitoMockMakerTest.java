package com.github.tmurakami.dexmockito;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.MockHandler;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.mock.MockCreationSettings;
import org.mockito.plugins.MockMaker;

import static org.junit.Assert.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@RunWith(MockitoJUnitRunner.class)
public class DexMockitoMockMakerTest {

    @Mock
    MockMaker delegate;
    @Mock
    MockCreationSettings<C> settings;
    @Mock
    MockHandler handler;
    @Mock
    MockMaker.TypeMockability typeMockability;

    @InjectMocks
    DexMockitoMockMaker target;

    @Test
    public void testCreateMock() {
        C c = new C();
        given(delegate.createMock(settings, handler)).willReturn(c);
        assertSame(c, target.createMock(settings, handler));
    }

    @Test
    public void testGetHandler() {
        Object o = new Object();
        given(delegate.getHandler(o)).willReturn(handler);
        assertSame(handler, target.getHandler(o));
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
        assertSame(typeMockability, target.isTypeMockable(C.class));
    }

    private static class C {
    }

}
