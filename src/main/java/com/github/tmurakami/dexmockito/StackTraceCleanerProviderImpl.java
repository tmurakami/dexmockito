package com.github.tmurakami.dexmockito;

import org.mockito.exceptions.stacktrace.StackTraceCleaner;
import org.mockito.plugins.StackTraceCleanerProvider;

public final class StackTraceCleanerProviderImpl implements StackTraceCleanerProvider {
    @Override
    public StackTraceCleaner getStackTraceCleaner(final StackTraceCleaner defaultCleaner) {
        return new StackTraceCleaner() {
            @Override
            public boolean isIn(StackTraceElement e) {
                return !e.getClassName().startsWith("com.github.tmurakami.dexmockito.") && defaultCleaner.isIn(e);
            }
        };
    }
}
