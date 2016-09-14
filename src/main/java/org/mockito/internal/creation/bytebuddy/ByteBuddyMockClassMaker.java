package org.mockito.internal.creation.bytebuddy;

import com.github.tmurakami.dexmockito.Function;
import com.github.tmurakami.dexmockito.MockClassMaker;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import org.mockito.internal.creation.bytebuddy.ByteBuddyCrossClassLoaderSerializationSupport.CrossClassLoaderSerializableMock;
import org.mockito.internal.creation.bytebuddy.MockMethodInterceptor.DispatcherDefaultingToRealMethod;
import org.mockito.internal.creation.bytebuddy.MockMethodInterceptor.ForEquals;
import org.mockito.internal.creation.bytebuddy.MockMethodInterceptor.ForHashCode;
import org.mockito.internal.creation.bytebuddy.MockMethodInterceptor.ForWriteReplace;
import org.mockito.internal.creation.bytebuddy.MockMethodInterceptor.MockAccess;
import org.mockito.mock.MockCreationSettings;

import java.lang.reflect.Type;
import java.util.ArrayList;

import static net.bytebuddy.description.modifier.SynchronizationState.PLAIN;
import static net.bytebuddy.description.modifier.Visibility.PRIVATE;
import static net.bytebuddy.dynamic.Transformer.ForMethod.withModifiers;
import static net.bytebuddy.implementation.FieldAccessor.ofBeanProperty;
import static net.bytebuddy.implementation.MethodDelegation.to;
import static net.bytebuddy.implementation.attribute.MethodAttributeAppender.ForInstrumentedMethod.INCLUDING_RECEIVER;
import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.isEquals;
import static net.bytebuddy.matcher.ElementMatchers.isHashCode;
import static net.bytebuddy.matcher.ElementMatchers.named;

public final class ByteBuddyMockClassMaker implements MockClassMaker {

    private final ByteBuddy byteBuddy;
    private final Function<MockCreationSettings<?>, ClassLoader> classLoaderResolver;
    private final ClassLoadingStrategy classLoadingStrategy;

    public ByteBuddyMockClassMaker(ByteBuddy byteBuddy,
                                   Function<MockCreationSettings<?>, ClassLoader> classLoaderResolver,
                                   ClassLoadingStrategy classLoadingStrategy) {
        this.byteBuddy = byteBuddy;
        this.classLoaderResolver = classLoaderResolver;
        this.classLoadingStrategy = classLoadingStrategy;
    }

    @Override
    public Class apply(MockCreationSettings<?> settings) {
        Class<?> typeToMock = settings.getTypeToMock();
        DynamicType.Builder<?> builder = byteBuddy
                .subclass(typeToMock)
                .ignoreAlso(isDeclaredBy(named("groovy.lang.GroovyObjectSupport")))
                .annotateType(typeToMock.getAnnotations())
                .implement(new ArrayList<Type>(settings.getExtraInterfaces()))
                .method(any()).intercept(to(DispatcherDefaultingToRealMethod.class)).transform(withModifiers(PLAIN)).attribute(INCLUDING_RECEIVER)
                .serialVersionUid(42L)
                .defineField("mockitoInterceptor", MockMethodInterceptor.class, PRIVATE).implement(MockAccess.class).intercept(ofBeanProperty())
                .method(isHashCode()).intercept(to(ForHashCode.class))
                .method(isEquals()).intercept(to(ForEquals.class));
        if (settings.isSerializable()) {
            builder = builder.implement(CrossClassLoaderSerializableMock.class).intercept(to(ForWriteReplace.class));
        }
        return builder.make().load(classLoaderResolver.apply(settings), classLoadingStrategy).getLoaded();
    }

}
