/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

// Source: https://github.com/resteasy/resteasy/blob/7.0.0.Final/resteasy-cdi/src/main/java/org/jboss/resteasy/cdi/CdiPropertyInjector.java

package org.jboss.resteasy.cdi;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.ws.rs.WebApplicationException;

import org.jboss.resteasy.cdi.i18n.LogMessages;
import org.jboss.resteasy.spi.ApplicationException;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.PropertyInjector;

/**
 * JAX-RS property injection is performed twice on CDI Beans. Firstly by the JaxrsInjectionTarget
 * wrapper and then again by RESTEasy (which operates on Weld proxies instead of the underlying instances).
 * To eliminate this, we enabled the injector only for non-CDI beans (JAX-RS components outside of BDA) or
 * CDI components that are not JAX-RS components.
 *
 * @author <a href="mailto:jharting@redhat.com">Jozef Hartinger</a>
 */
public class CdiPropertyInjector implements PropertyInjector {
    private static final Class<?> WELD_PROXY_CLASS;
    private static final CallSite META_DATA_GETTER;
    private static final CallSite CONTEXTUAL_INSTANCE_GETTER;

    static {
        Class<?> weldProxyClass = null;
        MethodHandle metaDataGetter = null;
        MethodHandle contextualInstanceGetter = null;
        try {
            final MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            weldProxyClass = Class.forName("org.jboss.weld.proxy.WeldClientProxy");
            final Class<?> metaData = Class.forName("org.jboss.weld.proxy.WeldClientProxy$Metadata");
            metaDataGetter = lookup.findVirtual(weldProxyClass, "getMetadata", MethodType.methodType(metaData));
            contextualInstanceGetter = lookup.findVirtual(metaData, "getContextualInstance",
                    MethodType.methodType(Object.class));
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException ignore) {
        }
        if (weldProxyClass == null || metaDataGetter == null || contextualInstanceGetter == null) {
            WELD_PROXY_CLASS = null;
            META_DATA_GETTER = null;
            CONTEXTUAL_INSTANCE_GETTER = null;
        } else {
            WELD_PROXY_CLASS = weldProxyClass;
            META_DATA_GETTER = new ConstantCallSite(metaDataGetter);
            CONTEXTUAL_INSTANCE_GETTER = new ConstantCallSite(contextualInstanceGetter);
        }
    }
    private final PropertyInjector delegate;
    private final Class<?> clazz;
    private boolean injectorEnabled = true;

    public CdiPropertyInjector(final PropertyInjector delegate, final Class<?> clazz,
            final Map<Class<?>, Collection<Type>> sessionBeanInterface, final BeanManager manager) { // Liberty Change
        this.delegate = delegate;
        this.clazz = clazz;

        if (sessionBeanInterface.containsKey(clazz)) {
            injectorEnabled = false;
        }
        if (!manager.getBeans(clazz).isEmpty() && Utils.isJaxrsComponent(clazz)) {
            injectorEnabled = false;
        }
    }

    @Override
    public CompletionStage<Void> inject(Object target, boolean unwrapAsync) {
        if (injectorEnabled) {
            return delegate.inject(target, unwrapAsync);
        }
        return null;
    }

    @Override
    public CompletionStage<Void> inject(HttpRequest request, HttpResponse response, Object target, boolean unwrapAsync)
            throws Failure, WebApplicationException, ApplicationException {
        if (injectorEnabled) {
            return delegate.inject(request, response, unwrapIfRequired(target), unwrapAsync);
        }
        return null;
    }

    @Override
    public String toString() {
        return "CdiPropertyInjector (enabled: " + injectorEnabled + ") for " + clazz;
    }

    private static Object unwrapIfRequired(final Object target) {
        try {
            if (WELD_PROXY_CLASS != null && WELD_PROXY_CLASS.isInstance(target)) {
                final var metaData = META_DATA_GETTER.dynamicInvoker().invoke(target);
                return CONTEXTUAL_INSTANCE_GETTER.dynamicInvoker().invoke(metaData);
            }
        } catch (Throwable e) {
            LogMessages.LOGGER.debugf(e, "Failed to handle unwrapping of %s", target);
        }
        return target;
    }
}
