package com.github.tmurakami.dexmockito;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.jar.asm.ClassWriter;
import net.bytebuddy.jar.asm.Type;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import dalvik.system.DexFile;

import static net.bytebuddy.jar.asm.Opcodes.V1_6;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class DexClassLoadingStrategyTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Mock
    DexFileLoader dexFileLoader;
    @Mock
    DexFile dexFile;

    @Captor
    ArgumentCaptor<String> sourcePathNameCaptor;
    @Captor
    ArgumentCaptor<String> outputPathNameCaptor;

    private File cacheDir;
    private DexClassLoadingStrategy target;

    @Before
    public void setUp() throws IOException {
        cacheDir = folder.newFolder();
        target = new DexClassLoadingStrategy(cacheDir, dexFileLoader);
    }

    @Test
    public void testLoad() throws Exception {
        String baseName = getClass().getName();
        String[] names = {baseName + "$A", baseName + "$B", baseName + "$C"};
        Map<TypeDescription, byte[]> bytecodeMap = new HashMap<>();
        Map<TypeDescription, Class> classMap = new HashMap<>();
        TestClassLoader classLoader = new TestClassLoader();
        for (String name : names) {
            TypeDescription td = mock(TypeDescription.class);
            given(td.getName()).willReturn(name);
            byte[] bytecode = generateBytecode(name);
            bytecodeMap.put(td, bytecode);
            classMap.put(td, classLoader.defineClass(name, bytecode));
        }
        given(dexFileLoader.load(anyString(), anyString())).willReturn(dexFile);
        assertEquals(classMap, target.load(classLoader, bytecodeMap));
        for (String name : names) {
            then(dexFile).should().loadClass(name, classLoader);
        }
        then(dexFile).should().close();
        then(dexFileLoader).should().load(sourcePathNameCaptor.capture(), outputPathNameCaptor.capture());
        File[] files = {new File(sourcePathNameCaptor.getValue()), new File(outputPathNameCaptor.getValue())};
        for (File f : files) {
            assertFalse(f.exists());
            assertEquals(cacheDir, f.getParentFile());
        }
        assertTrue(files[0].getName().endsWith(".jar"));
        assertTrue(files[1].getName().endsWith(".dex"));
    }

    private static byte[] generateBytecode(String name) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_6, 0, name.replace('.', '/'), null, Type.getInternalName(Object.class), null);
        cw.visitEnd();
        return cw.toByteArray();
    }

}
