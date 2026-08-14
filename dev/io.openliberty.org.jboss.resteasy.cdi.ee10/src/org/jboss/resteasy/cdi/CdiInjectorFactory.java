/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
// Source: https://github.com/resteasy/resteasy/blob/6.2.8.Final/resteasy-cdi/src/main/java/org/jboss/resteasy/cdi/CdiInjectorFactory.java

package org.jboss.resteasy.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.ServletContext;

import org.jboss.resteasy.cdi.i18n.LogMessages;
import org.jboss.resteasy.cdi.i18n.Messages;
import org.jboss.resteasy.core.InjectorFactoryImpl;
import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.spi.ConstructorInjector;
import org.jboss.resteasy.spi.InjectorFactory;
import org.jboss.resteasy.spi.MethodInjector;
import org.jboss.resteasy.spi.PropertyInjector;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.ValueInjector;
import org.jboss.resteasy.spi.metadata.Parameter;
import org.jboss.resteasy.spi.metadata.ResourceClass;
import org.jboss.resteasy.spi.metadata.ResourceConstructor;
import org.jboss.resteasy.spi.metadata.ResourceLocator;

/**
 * @author Jozef Hartinger
 */
@SuppressWarnings("rawtypes")
public class CdiInjectorFactory implements InjectorFactory {
    public static final String BEAN_MANAGER_ATTRIBUTE_PREFIX = "org.jboss.weld.environment.servlet.";
    private final BeanManager manager;
    private final InjectorFactory delegate = new InjectorFactoryImpl();
    private final ResteasyCdiExtension extension;
    private final Map<Class<?>, Collection<Type>> sessionBeanInterface; // Liberty Change

    @SuppressWarnings("unused")
    public CdiInjectorFactory() {
        this.manager = lookupBeanManager();
        this.extension = lookupResteasyCdiExtension();
        sessionBeanInterface = extension.getSessionBeanInterface();
    }

    public CdiInjectorFactory(final BeanManager manager) {
        this.manager = manager;
        this.extension = lookupResteasyCdiExtension();
        sessionBeanInterface = extension.getSessionBeanInterface();
    }

    @Override
    public ValueInjector createParameterExtractor(Parameter parameter, ResteasyProviderFactory providerFactory) {
        return delegate.createParameterExtractor(parameter, providerFactory);
    }

    @Override
    public MethodInjector createMethodInjector(ResourceLocator method, ResteasyProviderFactory factory) {
        return delegate.createMethodInjector(method, factory);
    }

    @Override
    public PropertyInjector createPropertyInjector(ResourceClass resourceClass, ResteasyProviderFactory providerFactory) {
        return new CdiPropertyInjector(delegate.createPropertyInjector(resourceClass, providerFactory),
                resourceClass.getClazz(), sessionBeanInterface, manager); // Liberty Change
    }

    @Override
    public ConstructorInjector createConstructor(ResourceConstructor constructor, ResteasyProviderFactory providerFactory) {
        Class<?> clazz = constructor.getConstructor().getDeclaringClass();

        ConstructorInjector injector = cdiConstructor(clazz);
        if (injector != null)
            return injector;

        LogMessages.LOGGER.debug(Messages.MESSAGES.noCDIBeansFound(clazz));
        return delegate.createConstructor(constructor, providerFactory);
    }

    @Override
    public ConstructorInjector createConstructor(Constructor constructor, ResteasyProviderFactory factory) {
        Class<?> clazz = constructor.getDeclaringClass();

        ConstructorInjector injector = cdiConstructor(clazz);
        if (injector != null)
            return injector;

        LogMessages.LOGGER.debug(Messages.MESSAGES.noCDIBeansFound(clazz));
        return delegate.createConstructor(constructor, factory);
    }

    protected ConstructorInjector cdiConstructor(Class<?> clazz) {
        // Liberty Change Start - Get the no-arg constructor for fallback
        Constructor<?> constructor = AccessController.doPrivileged((PrivilegedAction<Constructor<?>>) () -> {
            try {
                Constructor<?> ctor = clazz.getDeclaredConstructor();
                ctor.setAccessible(true);
                return ctor;
            } catch (NoSuchMethodException e) {
                // No no-arg constructor available, CDI-only mode
                return null;
            }
        });
        // Liberty Change End
        
        if (!manager.getBeans(clazz).isEmpty()) {
            LogMessages.LOGGER.debug(Messages.MESSAGES.usingCdiConstructorInjector(clazz));
            return new CdiConstructorInjector(Collections.singleton(clazz), manager, constructor); // Liberty Change
        }

        if (sessionBeanInterface.containsKey(clazz)) {
            // Liberty Change Start
            Collection<Type> intfc = sessionBeanInterface.get(clazz);
            return new CdiConstructorInjector(intfc, manager, constructor);
            // Liberty Change End
        }

        return null;
    }

    public PropertyInjector createPropertyInjector(Class resourceClass, ResteasyProviderFactory factory) {
        return new CdiPropertyInjector(delegate.createPropertyInjector(resourceClass, factory), resourceClass,
                sessionBeanInterface, manager); // Liberty Change
    }

    public ValueInjector createParameterExtractor(Class injectTargetClass, AccessibleObject injectTarget, String defaultName,
            Class type, Type genericType, Annotation[] annotations, ResteasyProviderFactory factory) {
        return delegate.createParameterExtractor(injectTargetClass, injectTarget, defaultName, type, genericType, annotations,
                factory);
    }

    public ValueInjector createParameterExtractor(Class injectTargetClass, AccessibleObject injectTarget, String defaultName,
            Class type,
            Type genericType, Annotation[] annotations, boolean useDefault, ResteasyProviderFactory factory) {
        return delegate.createParameterExtractor(injectTargetClass, injectTarget, defaultName, type, genericType, annotations,
                useDefault, factory);
    }

    /**
     * Do a lookup for BeanManager instance. JNDI and ServletContext is searched.
     *
     * @return BeanManager instance
     */
    protected BeanManager lookupBeanManager() {
        BeanManager beanManager;

        // Do a lookup for BeanManager in JNDI (this is the only *portable* way)
        beanManager = lookupBeanManagerInJndi("java:comp/BeanManager");
        if (beanManager != null) {
            LogMessages.LOGGER.debug(Messages.MESSAGES.foundBeanManagerAtJavaComp());
            return beanManager;
        }

        // Do a lookup for BeanManager at an alternative JNDI location (workaround for WELDINT-19)
        beanManager = lookupBeanManagerInJndi("java:app/BeanManager");
        if (beanManager != null) {
            LogMessages.LOGGER.debug(Messages.MESSAGES.foundBeanManagerAtJavaApp());
            return beanManager;
        }

        beanManager = lookupBeanManagerCDIUtil();
        if (beanManager != null) {
            LogMessages.LOGGER.debug(Messages.MESSAGES.foundBeanManagerViaCDI());
            return beanManager;
        }

        beanManager = lookupBeanManagerViaServletContext();
        if (beanManager != null) {
            LogMessages.LOGGER.debug(Messages.MESSAGES.foundBeanManagerInServletContext());
            return beanManager;
        }

        throw new RuntimeException(Messages.MESSAGES.unableToLookupBeanManager());
    }

    private BeanManager lookupBeanManagerInJndi(String name) {
        try {
            InitialContext ctx = new InitialContext();
            LogMessages.LOGGER.debug(Messages.MESSAGES.doingALookupForBeanManager(name));
            return (BeanManager) ctx.lookup(name);
        } catch (NamingException e) {
            LogMessages.LOGGER.debug(Messages.MESSAGES.unableToObtainBeanManager(name));
            return null;
        } catch (NoClassDefFoundError ncdfe) {
            LogMessages.LOGGER.debug(Messages.MESSAGES.unableToPerformJNDILookups());
            return null;
        }
    }

    private static BeanManager lookupBeanManagerViaServletContext() {
        BeanManager beanManager = null;
        try {
            // Look for BeanManager in ServletContext
            ServletContext servletContext = ResteasyContext.getContextData(ServletContext.class);
            // null check for RESTEASY-1009
            if (servletContext != null) {
                beanManager = (BeanManager) servletContext
                        .getAttribute(BEAN_MANAGER_ATTRIBUTE_PREFIX + BeanManager.class.getName());
                if (beanManager != null) {
                    LogMessages.LOGGER.debug(Messages.MESSAGES.foundBeanManagerInServletContext());
                    return beanManager;
                }

                // Look for BeanManager in ServletContext (the old attribute name for backwards compatibility)
                beanManager = (BeanManager) servletContext.getAttribute(BeanManager.class.getName());
                if (beanManager != null) {
                    LogMessages.LOGGER.debug(Messages.MESSAGES.foundBeanManagerInServletContext());
                    return beanManager;
                }
            }
        } catch (NoClassDefFoundError e) {
            LogMessages.LOGGER.debug(Messages.MESSAGES.unableToFindServletContextClass(), e);
        }

        catch (Exception e) {
            LogMessages.LOGGER.debug(Messages.MESSAGES.errorOccurredLookingUpServletContext(), e);
        }
        return beanManager;
    }

    public static BeanManager lookupBeanManagerCDIUtil() {
        BeanManager bm = null;
        try {
            bm = CDI.current().getBeanManager();
        } catch (NoClassDefFoundError e) {
            LogMessages.LOGGER.debug(Messages.MESSAGES.unableToFindCDIClass(), e);
        } catch (Exception e) {
            LogMessages.LOGGER.debug(Messages.MESSAGES.errorOccurredLookingUpViaCDIUtil(), e);
        }
        return bm;
    }

    /**
     * Lookup ResteasyCdiExtension instance that was instantiated during CDI bootstrap
     *
     * @return ResteasyCdiExtension instance
     */
    private ResteasyCdiExtension lookupResteasyCdiExtension() {
        Set<Bean<?>> beans = manager.getBeans(ResteasyCdiExtension.class);
        Bean<?> bean = manager.resolve(beans);
        if (bean == null) {
            throw new IllegalStateException(Messages.MESSAGES.unableToObtainResteasyCdiExtension());
        }
        CreationalContext<?> context = manager.createCreationalContext(bean);
        return (ResteasyCdiExtension) manager.getReference(bean, ResteasyCdiExtension.class, context);
    }
}
