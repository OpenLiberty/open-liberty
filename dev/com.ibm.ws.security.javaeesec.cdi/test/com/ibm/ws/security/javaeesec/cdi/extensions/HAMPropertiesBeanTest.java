/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.javaeesec.cdi.extensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Properties;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.TypeLiteral;
import com.ibm.ws.security.javaeesec.authentication.mechanism.http.HAMProperties;
import com.ibm.ws.security.javaeesec.authentication.mechanism.http.HAMPropertiesImpl;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;


public class HAMPropertiesBeanTest {
    static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery context = new JUnit4Mockery();
    private final BeanManager bm = context.mock(BeanManager.class, "bm1");
    @SuppressWarnings("unchecked")
    private final CreationalContext<HAMProperties> cc = context.mock(CreationalContext.class, "cc1");
    private final Class implClass = String.class;
    private final Properties props = new Properties();

    @Before
    public void setUp() {}

    @After
    public void tearDown() throws Exception {
        context.assertIsSatisfied();
    }

    @Test
    public void create() {
        HAMPropertiesBean hampb = new HAMPropertiesBean(bm, implClass, props);
        assertEquals("create returns incorrect object.", "com.ibm.ws.security.javaeesec.authentication.mechanism.http.HAMPropertiesImpl", hampb.create(cc).getClass().getName());
    }

    @Test
    public void getName() {
        HAMPropertiesBean hampb = new HAMPropertiesBean(bm, implClass, props);
        System.out.println("getName: " + hampb.getName());
        assertEquals("getName returns incorrect value.", hampb.getClass().getName() + "[" + hampb.getTypes().iterator().next().toString() + "]", hampb.getName());
    }

    @Test
    public void getQualifiers() {
        HAMPropertiesBean hampb = new HAMPropertiesBean(bm, implClass, props);
        Set<Annotation> output = hampb.getQualifiers();
        System.out.println("getQualifiers: " + output);
        assertEquals("getQualifiers returns incorrect number of elements.", 1, output.size());
        assertEquals("getQualifiers returns incorrect value.", new AnnotationLiteral<Default>() {}.toString(), output.iterator().next().toString());
    }

    @Test
    public void getScope() {
        HAMPropertiesBean hampb = new HAMPropertiesBean(bm, implClass, props);
        System.out.println("getScope: " + hampb.getScope());
        assertEquals("getScope returns incorrect value.", ApplicationScoped.class.getName(), hampb.getScope().getName());
    }

    @Test
    public void getStereotypes() {
        HAMPropertiesBean hampb = new HAMPropertiesBean(bm, implClass, props);
        System.out.println("getScope: " + hampb.getStereotypes());
        assertTrue("getStereotypes returns incorrect value.", hampb.getStereotypes().isEmpty());
    }

    @Test
    public void getTypes() {
        HAMPropertiesBean hampb = new HAMPropertiesBean(bm, implClass, props);
        Set<Type> output = hampb.getTypes();
        System.out.println("getTypess: " + output);
        assertEquals("getTypes returns incorrect number of elements.", 1, output.size());
        assertEquals("getTypes returns incorrect value.", new TypeLiteral<HAMProperties>() {}.getType().toString(), output.iterator().next().toString());
    }

    @Test
    public void isAlternative() {
        HAMPropertiesBean hampb = new HAMPropertiesBean(bm, implClass, props);
        System.out.println("isAlternative: " + hampb.isAlternative());
        assertFalse("isAlternative returns incorrect value.", hampb.isAlternative());
    }

    @Test
    public void getBeanClass() {
        HAMPropertiesBean hampb = new HAMPropertiesBean(bm, implClass, props);
        System.out.println("getBeanClass: " + hampb.getBeanClass());
        assertEquals("getBeanClass returns incorrect value.", HAMProperties.class.toString(), hampb.getBeanClass().toString());
    }

    @Test
    public void getInjectionPoints() {
        HAMPropertiesBean hampb = new HAMPropertiesBean(bm, implClass, props);
        System.out.println("getInjectionPoints: " + hampb.getInjectionPoints());
        assertTrue("getInjectionPoints returns incorrect value.", hampb.getInjectionPoints().isEmpty());
    }

    @Test
    public void isNullable() {
        HAMPropertiesBean hampb = new HAMPropertiesBean(bm, implClass, props);
        System.out.println("isNullable: " + hampb.isNullable());
        assertFalse("isNullable returns incorrect value.", hampb.isNullable());
    }

    @Test
    public void getId() {
        HAMPropertiesBean hampb = new HAMPropertiesBean(bm, implClass, props);
        System.out.println("getId: " + hampb.getId());
        assertEquals("getId returns incorrect value.", bm.hashCode() + "#" + hampb.getName(), hampb.getId());
    }

}
