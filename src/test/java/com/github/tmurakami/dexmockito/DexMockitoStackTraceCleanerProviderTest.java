package com.github.tmurakami.dexmockito;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.exceptions.stacktrace.StackTraceCleaner;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@RunWith(MockitoJUnitRunner.class)
public class DexMockitoStackTraceCleanerProviderTest {

    @Mock
    StackTraceCleaner defaultCleaner;

    @Test
    public void testGetStackTraceCleaner_in() {
        StackTraceCleaner cleaner = new DexMockitoStackTraceCleanerProvider().getStackTraceCleaner(defaultCleaner);
        StackTraceElement e = new StackTraceElement(Object.class.getName(), "", null, 0);
        given(defaultCleaner.isIn(e)).willReturn(true);
        assertTrue(cleaner.isIn(e));
    }

    @Test
    public void testGetStackTraceCleaner_out() {
        StackTraceCleaner cleaner = new DexMockitoStackTraceCleanerProvider().getStackTraceCleaner(defaultCleaner);
        StackTraceElement e = new StackTraceElement(DexMockitoStackTraceCleanerProvider.class.getName(), "", null, 0);
        assertFalse(cleaner.isIn(e));
        then(defaultCleaner).should(never()).isIn(e);
    }

}
