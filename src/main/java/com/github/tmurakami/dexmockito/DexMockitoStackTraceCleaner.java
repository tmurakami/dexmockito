package com.github.tmurakami.dexmockito;

import org.mockito.exceptions.stacktrace.StackTraceCleaner;

final class DexMockitoStackTraceCleaner implements StackTraceCleaner {

    private final StackTraceCleaner defaultCleaner;

    DexMockitoStackTraceCleaner(StackTraceCleaner defaultCleaner) {
        this.defaultCleaner = defaultCleaner;
    }

    @Override
    public boolean isIn(StackTraceElement candidate) {
        return !candidate.getClassName().startsWith("com.github.tmurakami.dexmockito.") && defaultCleaner.isIn(candidate);
    }

}
