/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.org.jboss.resteasy.common.component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.decorator.Decorator;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.cdi.ResteasyCdiExtension;
import org.jboss.resteasy.cdi.i18n.LogMessages;
import org.jboss.resteasy.cdi.i18n.Messages;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

import io.openliberty.org.jboss.resteasy.common.cdi.LibertyCdiInjectorFactory;

@Component(service = WebSphereCDIExtension.class,
    configurationPolicy = ConfigurationPolicy.IGNORE,
    immediate = true,
    property = { "api.classes=" +
                "jakarta.ws.rs.Path;" +
                "jakarta.ws.rs.core.Application;" +
                "jakarta.ws.rs.ext.Provider",
             "bean.defining.annotations=" +
                "jakarta.ws.rs.Path;" +
                "jakarta.ws.rs.core.Application;" +
                "jakarta.ws.rs.ApplicationPath;" +
                "jakarta.ws.rs.ext.Provider;" +
                "jakarta.annotation.ManagedBean",
             "service.vendor=IBM" })

public class LibertyResteasyCdiExtension extends ResteasyCdiExtension implements Extension, WebSphereCDIExtension {

    private static final String JAVAX_EJB_STATELESS = "javax.ejb.Stateless";
    private static final String JAVAX_EJB_SINGLETON = "javax.ejb.Singleton";


    private boolean isSessionBean(AnnotatedType<?> annotatedType)
    {
       for (Annotation annotation : annotatedType.getAnnotations())
       {
          Class<?> annotationType = annotation.annotationType();
          if (annotationType.getName().equals(JAVAX_EJB_STATELESS) || annotationType.getName().equals(JAVAX_EJB_SINGLETON))
          {
             LogMessages.LOGGER.debug(Messages.MESSAGES.beanIsSLSBOrSingleton(annotatedType.getJavaClass()));
             return true; // Do not modify scopes of SLSBs and Singletons
          }
       }
       return false;
    }

    /**
     * Set a default scope for each CDI bean which is a RESTful WS Resource.
     *
     * @param <T> type
     * @param event event
     * @param beanManager bean manager
     */
    @Override
    public <T> void observeResources(@WithAnnotations({Path.class}) @Observes ProcessAnnotatedType<T> event, BeanManager beanManager)
    {
       AnnotatedType<T> annotatedType = event.getAnnotatedType();
       Class<?> javaClass = annotatedType.getJavaClass();
       if(!javaClass.isInterface()
           && !isSessionBean(annotatedType)
           && !annotatedType.isAnnotationPresent(Decorator.class))
       {
          LogMessages.LOGGER.debug(Messages.MESSAGES.discoveredCDIBeanJaxRsResource(annotatedType.getJavaClass().getCanonicalName()));
          //Liberty change start
          if (Application.class.isAssignableFrom(javaClass)) {
              event.setAnnotatedType(wrapAnnotatedType(annotatedType, applicationScopedLiteral));
          } /* else {
              event.setAnnotatedType(wrapAnnotatedType(annotatedType, requestScopedLiteral));
          }
          */
          //Liberty change end
          this.getResources().add(javaClass);
       }
    }

    /**
     * Set a default scope for each CDI bean which is a RESTful WS Provider.
     *
     * @param <T> type
     * @param event event
     * @param beanManager bean manager
     */
    @Override
    public <T> void observeProviders(@WithAnnotations({Provider.class}) @Observes ProcessAnnotatedType<T> event, BeanManager beanManager)
    {
       AnnotatedType<T> annotatedType = event.getAnnotatedType();

       if(!annotatedType.getJavaClass().isInterface()
          && !isSessionBean(annotatedType)
          && !isUnproxyableClass(annotatedType.getJavaClass()))
       {
          LogMessages.LOGGER.debug(Messages.MESSAGES.discoveredCDIBeanJaxRsProvider(annotatedType.getJavaClass().getCanonicalName()));
          event.setAnnotatedType(wrapAnnotatedType(annotatedType, applicationScopedLiteral));
          this.getProviders().add(annotatedType.getJavaClass());
       }
    }

    /**
     * Check for select case of unproxyable bean type.
     * (see CDI 2.0 spec, section 3.11)
     * @param clazz
     * @return
     */
    private boolean isUnproxyableClass(Class<?> clazz) {
       // Unproxyable bean type: classes which are declared final,
       // or expose final methods,
       // or have no non-private no-args constructor
       return Modifier.isFinal(clazz.getModifiers()) ||
             hasNonPrivateNonStaticFinalMethod(clazz) ||
             hasNoNonPrivateNoArgsConstructor(clazz);
    }

    // Adapted from weld-core-impl:3.0.5.Final's Reflections.getNonPrivateNonStaticFinalMethod()
    private boolean hasNonPrivateNonStaticFinalMethod(Class<?> type) {
       for (Class<?> clazz = type; clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
          for (Method method : clazz.getDeclaredMethods()) {
             int modifiers = method.getModifiers();
             if (Modifier.isFinal(modifiers) && !Modifier.isPrivate(modifiers) && !Modifier.isStatic(modifiers)) {
                return true;
             }
          }
       }
       return false;
    }

    private boolean hasNoNonPrivateNoArgsConstructor(Class<?> clazz) {
       Constructor<?> constructor;
       try {
          constructor = clazz.getConstructor();
       } catch (NoSuchMethodException exception) {
          return true;
       }
       return Modifier.isPrivate(constructor.getModifiers());
    }

    @Reference
    protected void setCdiService(CDIService cdiService) {
        LibertyCdiInjectorFactory.cdiService = cdiService;
    }

    protected void unsetCdiService(CDIService cdiService) {
        LibertyCdiInjectorFactory.cdiService = null;
    }
}