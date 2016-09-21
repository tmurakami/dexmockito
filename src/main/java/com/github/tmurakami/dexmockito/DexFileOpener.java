package com.github.tmurakami.dexmockito;

import java.io.File;
import java.io.IOException;

import dalvik.system.DexFile;

interface DexFileOpener {
    DexFile open(File source, File output) throws IOException;
}
