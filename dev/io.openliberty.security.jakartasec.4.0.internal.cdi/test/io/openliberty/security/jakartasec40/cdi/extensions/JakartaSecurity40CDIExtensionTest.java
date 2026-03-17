/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec40.cdi.extensions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.ProcessBean;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanismHandler;

/**
 * Unit tests for the JakartaSecurity40CDIExtension class.
 * 
 * 
 */
public class JakartaSecurity40CDIExtensionTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private JakartaSecurity40CDIExtension extension;
    private ProcessBean<?> processBean;
    private Bean<?> bean;
    private BeanManager beanManager;
    private AfterBeanDiscovery afterBeanDiscovery;
    private HttpAuthenticationMechanismHandler hamHandler;
    
    @Before
    public void setUp() throws Exception {
        processBean = mockery.mock(ProcessBean.class);
        bean = mockery.mock(Bean.class);
        beanManager = mockery.mock(BeanManager.class);
        afterBeanDiscovery = mockery.mock(AfterBeanDiscovery.class);
        extension = new JakartaSecurity40CDIExtension();
        hamHandler = mockery.mock(HttpAuthenticationMechanismHandler.class);
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testProcessBeanWithNonHttpAuthenticationMechanismHandler() throws Exception {
        // given ...
        mockery.checking(new Expectations() {
            {
                oneOf(processBean).getBean();
                will(returnValue(bean));
                
                oneOf(bean).getBeanClass();
                will(returnValue(String.class));
                
                oneOf(bean).getTypes();
                will(returnValue(createTypeSet(String.class)));
            }
        });
        setPrivateField(extension, "httpAuthenticationMechanismHandlerRegistered", false);
        
        // when ...
        extension.processBean(processBean, beanManager);
        
        // then ...
        assertFalse("httpAuthenticationMechanismHandlerRegistered should still be false",
                   getPrivateField(extension, "httpAuthenticationMechanismHandlerRegistered"));
    }

    @Test
    public void testProcessBeanWithHttpAuthenticationMechanismHandler() throws Exception {
    	// given
        mockery.checking(new Expectations() {
            {
                oneOf(processBean).getBean();
                will(returnValue(bean));
                
                oneOf(bean).getBeanClass();
                will(returnValue(hamHandler.getClass()));
                
                oneOf(bean).getTypes();
                will(returnValue(createTypeSet(HttpAuthenticationMechanismHandler.class)));
            }
        });
        setPrivateField(extension, "httpAuthenticationMechanismHandlerRegistered", false);
        
        // when ...
        extension.processBean(processBean, beanManager);
        
        // then ...
        assertTrue("httpAuthenticationMechanismHandlerRegistered should be true",
                  getPrivateField(extension, "httpAuthenticationMechanismHandlerRegistered"));
    }

    @Test
    public void testAfterBeanDiscoveryWithNoCustomHandler() throws Exception {
    	// given ...
        mockery.checking(new Expectations() {
            {
                oneOf(afterBeanDiscovery).addBean(with(any(HttpAuthenticationMechanismHandlerBean.class)));
            }
        });
        setPrivateField(extension, "httpAuthenticationMechanismHandlerRegistered", false);
        
        // when/then ...
        extension.afterBeanDiscovery(afterBeanDiscovery, beanManager);
    }

    @Test
    public void testAfterBeanDiscoveryWithCustomHandler() throws Exception {
    	// given ...
        setPrivateField(extension, "httpAuthenticationMechanismHandlerRegistered", true);
        
        // when/then ...
        extension.afterBeanDiscovery(afterBeanDiscovery, beanManager);
    }

    // Helper method to create a set of types
    private Set<java.lang.reflect.Type> createTypeSet(Class<?>... types) {
        Set<java.lang.reflect.Type> typeSet = new HashSet<>();
        for (Class<?> type : types) {
            typeSet.add(type);
        }
        return typeSet;
    }
    
    // Helper method to set a private field
    private void setPrivateField(Object object, String fieldName, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }
    
    // Helper method to get a private field
    private boolean getPrivateField(Object object, String fieldName) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (boolean) field.get(object);
    }
}
