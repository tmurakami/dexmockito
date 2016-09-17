package com.github.tmurakami.dexmockito;

import junit.framework.TestCase;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class MockTest extends TestCase {

    public void testMock() {
        Foo mock = mock(Foo.class);
        given(mock.message()).willReturn("test");
        Bar bar = new Bar(mock);
        assertEquals("test", bar.doIt());
    }

    public void testSpy() {
        Foo mock = spy(new Foo());
        given(mock.message()).willReturn("test");
        Bar bar = new Bar(mock);
        assertEquals("test", bar.doIt());
    }

    @SuppressWarnings("WeakerAccess")
    static class Foo {
        String message() {
            return "foo";
        }
    }

    private static class Bar {

        private final Foo foo;

        private Bar(Foo foo) {
            this.foo = foo;
        }

        private String doIt() {
            return foo.message();
        }

    }

}
