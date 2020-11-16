package me.kbrewster.eventbus.invokers;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class LMFInvoker implements InvokerType {

    private MethodLookup lookup;

    public SubscriberMethod setup(Object object, Class clazz, Class parameterClazz, Method method) throws Throwable {
        method.setAccessible(true);
        final MethodHandles.Lookup caller = lazyPrivateLookup(clazz);
        final MethodType subscription = MethodType.methodType(void.class, parameterClazz);
        final MethodHandle target = caller.findVirtual(clazz, method.getName(), subscription);
        final CallSite site = LambdaMetafactory.metafactory(
                caller,
                "invoke",
                MethodType.methodType(SubscriberMethod.class, clazz),
                subscription.changeParameterType(0, Object.class),
                target,
                subscription);

        final MethodHandle factory = site.getTarget();
        return (SubscriberMethod) factory.bindTo(object).invokeExact();

    }

    private MethodHandles.Lookup lazyPrivateLookup(Class clazz) throws Exception {
        if(this.lookup == null) { // try java 9 lookup
            try {
                this.lookup = MethodLookup.JAVA_9; // cache
                return this.lookup.privateLookup(clazz);
            } catch (NoSuchMethodException e) { // try java 8 lookup
                this.lookup = MethodLookup.JAVA_8; // cache
                return this.lookup.privateLookup(clazz);
            }
        }
        return this.lookup.privateLookup(clazz);
    }

    enum MethodLookup {
        JAVA_8 { // Java 8
            @Override
            MethodHandles.Lookup privateLookup(Class clazz) throws Exception {
                final MethodHandles.Lookup lookupIn = MethodHandles.lookup().in(clazz);

                // and then we mark it as trusted for private lookup via reflection on private field
                final Field modes = MethodHandles.Lookup.class.getDeclaredField("allowedModes");
                modes.setAccessible(true);
                modes.setInt(lookupIn, -1); // -1 == TRUSTED
                return lookupIn;
            }
        },
        JAVA_9 { // Java 9+
            @Override
            MethodHandles.Lookup privateLookup(Class clazz) throws Exception {
                final Method privateLookupIn = MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
                return (MethodHandles.Lookup) privateLookupIn.invoke(null, clazz, MethodHandles.lookup());
            }
        };

        abstract MethodHandles.Lookup privateLookup(Class clazz) throws Exception;
    }


}
