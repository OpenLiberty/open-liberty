/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.org.jboss.resteasy.common.component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import jakarta.decorator.Decorator;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.cdi.ResteasyCdiExtension;
import org.jboss.resteasy.cdi.Utils;
import org.jboss.resteasy.cdi.i18n.LogMessages;
import org.jboss.resteasy.cdi.i18n.Messages;

public class LibertyResteasyCdiExtension extends ResteasyCdiExtension implements Extension {

    private static final String JAKARTA_EJB_STATELESS = "jakarta.ejb.Stateless";
    private static final String JAKARTA_EJB_SINGLETON = "jakarta.ejb.Singleton";


    private boolean isSessionBean(AnnotatedType<?> annotatedType)
    {
       for (Annotation annotation : annotatedType.getAnnotations())
       {
          Class<?> annotationType = annotation.annotationType();
          if (annotationType.getName().equals(JAKARTA_EJB_STATELESS) || annotationType.getName().equals(JAKARTA_EJB_SINGLETON))
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
    * @param <T>         type
    * @param event       event
    * @param beanManager bean manager
    */
   public <T> void observeResources(@WithAnnotations({Path.class}) @Observes ProcessAnnotatedType<T> event,
                                    BeanManager beanManager) {
      AnnotatedType<T> annotatedType = event.getAnnotatedType();
      Class<?> javaClass = annotatedType.getJavaClass();
      if (!javaClass.isInterface()
              && !isSessionBean(annotatedType)
              && !annotatedType.isAnnotationPresent(Decorator.class)) {
         if (!Utils.isScopeDefined(annotatedType, beanManager)) {
            LogMessages.LOGGER.debug(Messages.MESSAGES.discoveredCDIBeanJaxRsResource(annotatedType.getJavaClass()
                    .getCanonicalName()));
            //Liberty change start
            if (Application.class.isAssignableFrom(javaClass)) {
              event.configureAnnotatedType().add(applicationScopedLiteral);
            } 
          //Liberty change end
         }
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
}