package com.github.tmurakami.dexmockito;

import org.mockito.exceptions.stacktrace.StackTraceCleaner;
import org.mockito.plugins.StackTraceCleanerProvider;

public final class DexMockitoStackTraceCleanerProvider implements StackTraceCleanerProvider {
    @Override
    public StackTraceCleaner getStackTraceCleaner(StackTraceCleaner defaultCleaner) {
        return new DexMockitoStackTraceCleaner(defaultCleaner);
    }
}
