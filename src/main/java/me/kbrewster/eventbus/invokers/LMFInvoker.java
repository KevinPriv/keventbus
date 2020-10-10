package me.kbrewster.eventbus.invokers;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class LMFInvoker implements InvokerType {

    public SubscriberMethod setup(Object object, Class clazz, Class parameterClazz, Method method) throws Throwable {
        method.setAccessible(true);
        final MethodHandles.Lookup caller = privateLookupIn(clazz);
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

    private static MethodHandles.Lookup privateLookupIn(Class clazz) {
        try {
            // Java 9+ has privateLookupIn method on MethodHandles, but since we are shipping and using Java 8
            // we need to access it via reflection. This is preferred way because it's Java 9+ public api and is
            // likely to not change
            final Method privateLookupIn = MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
            return (MethodHandles.Lookup) privateLookupIn.invoke(null, clazz, MethodHandles.lookup());
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            try {
                // In Java 8 we first do standard lookupIn class
                final MethodHandles.Lookup lookupIn = MethodHandles.lookup().in(clazz);

                // and then we mark it as trusted for private lookup via reflection on private field
                final Field modes = MethodHandles.Lookup.class.getDeclaredField("allowedModes");
                modes.setAccessible(true);
                modes.setInt(lookupIn, -1); // -1 == TRUSTED
                return lookupIn;
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException(ex);
            }
        }
    }


}
