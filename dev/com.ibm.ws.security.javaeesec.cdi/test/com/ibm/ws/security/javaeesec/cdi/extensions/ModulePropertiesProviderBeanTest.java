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
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.TypeLiteral;
import com.ibm.ws.security.javaeesec.properties.ModuleProperties;
import com.ibm.ws.security.javaeesec.properties.ModulePropertiesProvider;
import com.ibm.ws.security.javaeesec.properties.ModulePropertiesProviderImpl;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;


public class ModulePropertiesProviderBeanTest {
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
    private final CreationalContext<ModulePropertiesProvider> cc = context.mock(CreationalContext.class, "cc1");

    @Before
    public void setUp() {}

    @After
    public void tearDown() throws Exception {
        context.assertIsSatisfied();
    }

    @Test
    public void create() {
        ModulePropertiesProviderBean mppb = new ModulePropertiesProviderBean(bm, null);
        assertEquals("create returns incorrect object.", "com.ibm.ws.security.javaeesec.properties.ModulePropertiesProviderImpl", mppb.create(cc).getClass().getName());
    }

    @Test
    public void getName() {
        ModulePropertiesProviderBean mppb = new ModulePropertiesProviderBean(bm, null);
        System.out.println("getName: " + mppb.getName());
        assertEquals("getName returns incorrect value.", mppb.getClass().getName() + "[" + mppb.getTypes().iterator().next().toString() + "]", mppb.getName());
    }

    @Test
    public void getQualifiers() {
        ModulePropertiesProviderBean mppb = new ModulePropertiesProviderBean(bm, null);
        Set<Annotation> output = mppb.getQualifiers();
        System.out.println("getQualifiers: " + output);
        assertEquals("getQualifiers returns incorrect number of elements.", 1, output.size());
        assertEquals("getQualifiers returns incorrect value.", new AnnotationLiteral<Default>() {}.toString(), output.iterator().next().toString());
    }

    @Test
    public void getScope() {
        ModulePropertiesProviderBean mppb = new ModulePropertiesProviderBean(bm, null);
        System.out.println("getScope: " + mppb.getScope());
        assertEquals("getScope returns incorrect value.", ApplicationScoped.class.getName(), mppb.getScope().getName());
    }

    @Test
    public void getStereotypes() {
        ModulePropertiesProviderBean mppb = new ModulePropertiesProviderBean(bm, null);
        System.out.println("getScope: " + mppb.getStereotypes());
        assertTrue("getStereotypes returns incorrect value.", mppb.getStereotypes().isEmpty());
    }

    @Test
    public void getTypes() {
        ModulePropertiesProviderBean mppb = new ModulePropertiesProviderBean(bm, null);
        Set<Type> output = mppb.getTypes();
        System.out.println("getTypess: " + output);
        assertEquals("getTypes returns incorrect number of elements.", 1, output.size());
        assertEquals("getTypes returns incorrect value.", new TypeLiteral<ModulePropertiesProvider>() {}.getType().toString(), output.iterator().next().toString());
    }

    @Test
    public void isAlternative() {
        ModulePropertiesProviderBean mppb = new ModulePropertiesProviderBean(bm, null);
        System.out.println("isAlternative: " + mppb.isAlternative());
        assertFalse("isAlternative returns incorrect value.", mppb.isAlternative());
    }

    @Test
    public void getBeanClass() {
        ModulePropertiesProviderBean mppb = new ModulePropertiesProviderBean(bm, null);
        System.out.println("getBeanClass: " + mppb.getBeanClass());
        assertEquals("getBeanClass returns incorrect value.", ModulePropertiesProvider.class.toString(), mppb.getBeanClass().toString());
    }

    @Test
    public void getInjectionPoints() {
        ModulePropertiesProviderBean mppb = new ModulePropertiesProviderBean(bm, null);
        System.out.println("getInjectionPoints: " + mppb.getInjectionPoints());
        assertTrue("getInjectionPoints returns incorrect value.", mppb.getInjectionPoints().isEmpty());
    }

    @Test
    public void isNullable() {
        ModulePropertiesProviderBean mppb = new ModulePropertiesProviderBean(bm, null);
        System.out.println("isNullable: " + mppb.isNullable());
        assertFalse("isNullable returns incorrect value.", mppb.isNullable());
    }

    @Test
    public void getId() {
        ModulePropertiesProviderBean mppb = new ModulePropertiesProviderBean(bm, null);
        System.out.println("getId: " + mppb.getId());
        assertEquals("getId returns incorrect value.", bm.hashCode() + "#" + mppb.getName(), mppb.getId());
    }

}
