package com.github.tmurakami.dexmockito;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy.SuffixingRandom;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;

import org.mockito.internal.creation.bytebuddy.MockAccess;
import org.mockito.internal.creation.bytebuddy.MockMethodInterceptor;
import org.mockito.internal.creation.bytebuddy.MockMethodInterceptor.DispatcherDefaultingToRealMethod;
import org.mockito.internal.creation.bytebuddy.MockMethodInterceptor.ForEquals;
import org.mockito.internal.creation.bytebuddy.MockMethodInterceptor.ForHashCode;
import org.mockito.mock.MockCreationSettings;
import org.mockito.mock.SerializableMode;

import java.lang.reflect.Type;
import java.util.Set;

import static net.bytebuddy.NamingStrategy.SuffixingRandom.BaseNameResolver.ForUnnamedType;
import static net.bytebuddy.description.modifier.SynchronizationState.PLAIN;
import static net.bytebuddy.description.modifier.Visibility.PRIVATE;
import static net.bytebuddy.description.modifier.Visibility.PUBLIC;
import static net.bytebuddy.dynamic.Transformer.ForMethod.withModifiers;
import static net.bytebuddy.implementation.FieldAccessor.ofBeanProperty;
import static net.bytebuddy.implementation.MethodDelegation.to;
import static net.bytebuddy.implementation.MethodDelegation.toConstructor;
import static net.bytebuddy.implementation.attribute.MethodAttributeAppender.ForInstrumentedMethod.INCLUDING_RECEIVER;
import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.isEquals;
import static net.bytebuddy.matcher.ElementMatchers.isHashCode;
import static net.bytebuddy.matcher.ElementMatchers.named;

final class ByteBuddyMockClassGenerator implements MockClassGenerator {

    private final ClassLoaderResolver resolver;
    private final ClassLoadingStrategy strategy;
    private final ByteBuddy byteBuddy = new ByteBuddy()
            .with(ClassFileVersion.JAVA_V6)
            .with(TypeValidation.DISABLED)
            .with(new SuffixingRandom("MockitoMock", ForUnnamedType.INSTANCE, "codegen"));

    ByteBuddyMockClassGenerator(ClassLoaderResolver resolver, ClassLoadingStrategy strategy) {
        this.resolver = resolver;
        this.strategy = strategy;
    }

    @Override
    public Class generateMockClass(MockCreationSettings<?> settings) {
        Class<?> typeToMock = settings.getTypeToMock();
        Set<Class<?>> extraInterfaces = settings.getExtraInterfaces();
        DynamicType.Builder<?> builder = byteBuddy
                .subclass(typeToMock)
                .ignoreAlso(isDeclaredBy(named("groovy.lang.GroovyObjectSupport")))
                .annotateType(typeToMock.getAnnotations())
                .implement(extraInterfaces.toArray(new Type[extraInterfaces.size()]))
                .method(any()).intercept(to(DispatcherDefaultingToRealMethod.class)).transform(withModifiers(PLAIN)).attribute(INCLUDING_RECEIVER)
                .serialVersionUid(42L)
                .defineField("mockitoInterceptor", MockMethodInterceptor.class, PRIVATE).implement(MockAccess.class).intercept(ofBeanProperty())
                .method(isHashCode()).intercept(to(ForHashCode.class))
                .method(isEquals()).intercept(to(ForEquals.class));
        if (settings.getSerializableMode() == SerializableMode.ACROSS_CLASSLOADERS) {
            builder = builder.defineMethod("writeReplace", Object.class, PUBLIC).intercept(toConstructor(SerializableMockProxy.class));
        }
        return builder.make().load(resolver.resolveClassLoader(settings), strategy).getLoaded();
    }

}
