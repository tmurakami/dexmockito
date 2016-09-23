package com.github.tmurakami.dexmockito;

import java.io.File;
import java.io.IOException;

import dalvik.system.DexFile;

interface DexFileLoader {
    DexFile load(File source, File output) throws IOException;
}
