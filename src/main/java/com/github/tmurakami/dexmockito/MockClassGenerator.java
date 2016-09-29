package com.github.tmurakami.dexmockito;

import org.mockito.mock.MockCreationSettings;

interface MockClassGenerator {
    Class generate(MockCreationSettings<?> settings);
}
