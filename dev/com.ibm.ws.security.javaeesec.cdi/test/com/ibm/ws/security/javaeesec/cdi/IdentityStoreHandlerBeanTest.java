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

package com.ibm.ws.security.javaeesec.cdi;

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
import javax.security.enterprise.identitystore.IdentityStoreHandler;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

public class IdentityStoreHandlerBeanTest {
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
    private final CreationalContext<IdentityStoreHandler> cc = context.mock(CreationalContext.class, "cc1");

    @Before
    public void setUp() {}

    @After
    public void tearDown() throws Exception {
        context.assertIsSatisfied();
    }

    @Test
    public void create() {
        IdentityStoreHandlerBean ishb = new IdentityStoreHandlerBean(bm);
        assertEquals("create returns incorrect object.", "com.ibm.ws.security.javaeesec.identitystore.IdentityStoreHandlerImpl", ishb.create(cc).getClass().getName());
    }

    @Test
    public void getName() {
        IdentityStoreHandlerBean ishb = new IdentityStoreHandlerBean(bm);
        System.out.println("getName: " + ishb.getName());
        assertEquals("getName returns incorrect value.", ishb.getClass().getName() + "[" + ishb.getTypes().iterator().next().toString() + "]", ishb.getName());
    }

    @Test
    public void getQualifiers() {
        IdentityStoreHandlerBean ishb = new IdentityStoreHandlerBean(bm);
        Set<Annotation> output = ishb.getQualifiers();
        System.out.println("getQualifiers: " + output);
        assertEquals("getQualifiers returns incorrect number of elements.", 1, output.size());
        assertEquals("getQualifiers returns incorrect value.", new AnnotationLiteral<Default>() {}.toString(), output.iterator().next().toString());
    }

    @Test
    public void getScope() {
        IdentityStoreHandlerBean ishb = new IdentityStoreHandlerBean(bm);
        System.out.println("getScope: " + ishb.getScope());
        assertEquals("getScope returns incorrect value.", ApplicationScoped.class.getName(), ishb.getScope().getName());
    }

    @Test
    public void getStereotypes() {
        IdentityStoreHandlerBean ishb = new IdentityStoreHandlerBean(bm);
        System.out.println("getScope: " + ishb.getStereotypes());
        assertTrue("getStereotypes returns incorrect value.", ishb.getStereotypes().isEmpty());
    }

    @Test
    public void getTypes() {
        IdentityStoreHandlerBean ishb = new IdentityStoreHandlerBean(bm);
        Set<Type> output = ishb.getTypes();
        System.out.println("getTypess: " + output);
        assertEquals("getTypes returns incorrect number of elements.", 1, output.size());
        assertEquals("getTypes returns incorrect value.", new TypeLiteral<IdentityStoreHandler>() {}.getType().toString(), output.iterator().next().toString());
    }

    @Test
    public void isAlternative() {
        IdentityStoreHandlerBean ishb = new IdentityStoreHandlerBean(bm);
        System.out.println("isAlternative: " + ishb.isAlternative());
        assertFalse("isAlternative returns incorrect value.", ishb.isAlternative());
    }

    @Test
    public void getBeanClass() {
        IdentityStoreHandlerBean ishb = new IdentityStoreHandlerBean(bm);
        System.out.println("getBeanClass: " + ishb.getBeanClass());
        assertEquals("getBeanClass returns incorrect value.", IdentityStoreHandler.class.toString(), ishb.getBeanClass().toString());
    }

    @Test
    public void getInjectionPoints() {
        IdentityStoreHandlerBean ishb = new IdentityStoreHandlerBean(bm);
        System.out.println("getInjectionPoints: " + ishb.getInjectionPoints());
        assertTrue("getInjectionPoints returns incorrect value.", ishb.getInjectionPoints().isEmpty());
    }

    @Test
    public void isNullable() {
        IdentityStoreHandlerBean ishb = new IdentityStoreHandlerBean(bm);
        System.out.println("isNullable: " + ishb.isNullable());
        assertFalse("isNullable returns incorrect value.", ishb.isNullable());
    }

    @Test
    public void getId() {
        IdentityStoreHandlerBean ishb = new IdentityStoreHandlerBean(bm);
        System.out.println("getId: " + ishb.getId());
        assertEquals("getId returns incorrect value.", bm.hashCode() + "#" + ishb.getName(), ishb.getId());
    }

}
