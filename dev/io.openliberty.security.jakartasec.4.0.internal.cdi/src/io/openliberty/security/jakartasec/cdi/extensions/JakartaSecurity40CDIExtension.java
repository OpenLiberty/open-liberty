/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec.cdi.extensions;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.security.jakartasec40.Utils;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;
import jakarta.security.enterprise.identitystore.IdentityStore;
import jakarta.security.enterprise.identitystore.InMemoryIdentityStoreDefinition;

/**
 * CDI Extension to process the {@link InMemoryIdentityStoreDefinition} annotation
 * and register beans required for Jakarta Security 4.0.
 */

@Component(service = {},
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = "service.vendor=IBM")
public class JakartaSecurity40CDIExtension implements Extension {

    private static final TraceComponent tc = Tr.register(JakartaSecurity40CDIExtension.class);

    private final Set<Bean<IdentityStore>> beansToAdd = new HashSet<Bean<IdentityStore>>();

    public JakartaSecurity40CDIExtension() {
        // empty
    }

    /*
     * Process an @InMemoryIdentityStoreDefinition if found within the application.
     */
    public <T> void processAnnotatedInMemory(@WithAnnotations({ InMemoryIdentityStoreDefinition.class }) @Observes ProcessAnnotatedType<T> event, BeanManager beanManager) {
        AnnotatedType<T> annotatedType = event.getAnnotatedType();
        Annotation inMemoryAnnotation = annotatedType.getAnnotation(InMemoryIdentityStoreDefinition.class);
        addInMemoryIdentityStoreBean(inMemoryAnnotation, beanManager);
    }

    /**
     * We create a bean of InMemoryIdentityStoreDefinition with actual values
     * from the found in memory annotation and pass it to the global bean manager
     * to be passed to the in memory wrapper class.
     *
     * @param inMemoryAnnotation is the specific in memory annotation with values.
     * @param beanManager        is the global bean manager
     */
    private <T> void addInMemoryIdentityStoreBean(Annotation inMemoryAnnotation, BeanManager beanManager) {

        for (Bean<IdentityStore> b : beansToAdd) {
            if (InMemoryIdentityStoreBean.class.equals(b.getClass())) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "InMemoryIdentityStoreBean already registered.");
                return;
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "adding InMemoryIdentityStoreBean.");
        }

        try {
            Map<String, Object> identityStoreProperties = new HashMap<String, Object>();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "fetching in memory identity store properties");
            }
            
            Class<? extends Annotation> annotationType = inMemoryAnnotation.annotationType();
            
            // these are the methods from the annotation
            Method[] methods = annotationType.getMethods();
            for (Method m : methods) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, m.getName());
                }
                if (!m.getName().equals("equals")) {
                    // annotation key/value pairs
                    identityStoreProperties.put(m.getName(), m.invoke(inMemoryAnnotation));
                }
            }

            InMemoryIdentityStoreDefinition inMemoryIdentityStoreDefinition = Utils.getInstanceOfInMemoryAnnotation(identityStoreProperties);

            beansToAdd.add(new InMemoryIdentityStoreBean(beanManager, inMemoryIdentityStoreDefinition));
        } catch (InvocationTargetException | IllegalAccessException e) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "unexpected", e);
            }
        }
    }

    public <T> void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "afterBeanDiscovery : instance : " + Integer.toHexString(this.hashCode()) + " BeanManager : " + Integer.toHexString(beanManager.hashCode()));
        }

        // Verification of mechanisms and registration of ModulePropertiesProviderBean performed in JavaEESecCDIExtension's afterBeanDiscovery()
        for (Bean<IdentityStore> bean : beansToAdd) {
            afterBeanDiscovery.addBean(bean);
        }
    }
}
