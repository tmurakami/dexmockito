package com.github.tmurakami.dexmockito;

import org.mockito.mock.MockCreationSettings;

public interface MockClassGenerator {
    Class generate(MockCreationSettings<?> settings);
}
