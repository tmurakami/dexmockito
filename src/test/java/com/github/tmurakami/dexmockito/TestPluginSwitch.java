package com.github.tmurakami.dexmockito;

import org.mockito.plugins.PluginSwitch;

public class TestPluginSwitch implements PluginSwitch {
    @Override
    public boolean isEnabled(String pluginClassName) {
        return false;
    }
}
