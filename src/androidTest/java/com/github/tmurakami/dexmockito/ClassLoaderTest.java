package com.github.tmurakami.dexmockito;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.jar.asm.ClassWriter;
import net.bytebuddy.jar.asm.Type;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import dalvik.system.DexFile;

import static net.bytebuddy.jar.asm.Opcodes.V1_6;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;

public class ClassLoaderTest extends BaseAndroidTestCase {

    @Mock
    TypeDescription typeDescription;
    @Mock
    DexFileLoader dexFileLoader;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
    }

    public void testLoadByCustomClassLoader() throws IOException {
        String name = getClass().getName() + "$C";
        given(typeDescription.getName()).willReturn(name);
        given(dexFileLoader.load(any(File.class), any(File.class))).will(new Answer<DexFile>() {
            @Override
            public DexFile answer(InvocationOnMock invocation) throws Throwable {
                File source = invocation.getArgument(0);
                File output = invocation.getArgument(1);
                return DexFile.loadDex(source.getAbsolutePath(), output.getAbsolutePath(), 0);
            }
        });
        Map<TypeDescription, byte[]> bytecodeMap = new HashMap<>();
        bytecodeMap.put(typeDescription, generateBytecode(name));
        File cacheDir = new File(getContext().getCacheDir(), getClass().getSimpleName().toLowerCase(Locale.US));
        ClassLoader loader = new ClassLoader() {
        };
        Map<TypeDescription, Class<?>> classMap;
        try {
            assertTrue(cacheDir.mkdir());
            classMap = new DexClassLoadingStrategy(cacheDir, dexFileLoader).load(loader, bytecodeMap);
        } finally {
            assertTrue(FileUtils.forceDelete(cacheDir));
        }
        Class<?> c = classMap.get(typeDescription);
        assertNotNull(c);
        assertEquals(name, c.getName());
        assertEquals(loader, c.getClassLoader());
    }

    private static byte[] generateBytecode(String name) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_6, 0, name.replace('.', '/'), null, Type.getInternalName(Object.class), null);
        cw.visitEnd();
        return cw.toByteArray();
    }

}