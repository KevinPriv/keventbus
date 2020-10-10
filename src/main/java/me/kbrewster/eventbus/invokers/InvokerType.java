package me.kbrewster.eventbus.invokers;

import java.lang.reflect.Method;

public interface InvokerType {

    SubscriberMethod setup(Object object, Class clazz, Class parameterClazz, Method method) throws Throwable;

    @FunctionalInterface
    interface SubscriberMethod
    {
        void invoke(Object event) throws Exception;
    }
}
