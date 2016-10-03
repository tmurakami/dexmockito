package com.github.tmurakami.dexmockito;

import org.mockito.mock.MockCreationSettings;

import java.lang.ref.Reference;
import java.util.concurrent.FutureTask;

interface FutureTaskFactory {
    FutureTask<Reference<Class>> newFutureTask(MockCreationSettings<?> settings);
}
