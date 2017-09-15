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
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

public class BasicAuthenticationMechanismBeanTest {
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
    private final CreationalContext<HttpAuthenticationMechanism> cc = context.mock(CreationalContext.class, "cc1");

    @Before
    public void setUp() {}

    @After
    public void tearDown() throws Exception {
        context.assertIsSatisfied();
    }

    @Test
    public void create() {
        BasicAuthenticationMechanismBean bamb = new BasicAuthenticationMechanismBean("realm", bm);
        assertEquals("create returns incorrect object.", "com.ibm.ws.security.javaeesec.authentication.mechanism.http.BasicHttpAuthenticationMechanism",
                     bamb.create(cc).getClass().getName());
    }

    @Test
    public void getName() {
        BasicAuthenticationMechanismBean bamb = new BasicAuthenticationMechanismBean("realm", bm);
        System.out.println("getName: " + bamb.getName());
        assertEquals("getName returns incorrect value.", bamb.getClass().getName() + "[" + bamb.getTypes().iterator().next().toString() + "]", bamb.getName());
    }

    @Test
    public void getQualifiers() {
        BasicAuthenticationMechanismBean bamb = new BasicAuthenticationMechanismBean("realm", bm);
        Set<Annotation> output = bamb.getQualifiers();
        System.out.println("getQualifiers: " + output);
        assertEquals("getQualifiers returns incorrect number of elements.", 1, output.size());
        assertEquals("getQualifiers returns incorrect value.", new AnnotationLiteral<Default>() {}.toString(), output.iterator().next().toString());
    }

    @Test
    public void getScope() {
        BasicAuthenticationMechanismBean bamb = new BasicAuthenticationMechanismBean("realm", bm);
        System.out.println("getScope: " + bamb.getScope());
        assertEquals("getScope returns incorrect value.", ApplicationScoped.class.getName(), bamb.getScope().getName());
    }

    @Test
    public void getStereotypes() {
        BasicAuthenticationMechanismBean bamb = new BasicAuthenticationMechanismBean("realm", bm);
        System.out.println("getScope: " + bamb.getStereotypes());
        assertTrue("getStereotypes returns incorrect value.", bamb.getStereotypes().isEmpty());
    }

    @Test
    public void getTypes() {
        BasicAuthenticationMechanismBean bamb = new BasicAuthenticationMechanismBean("realm", bm);
        Set<Type> output = bamb.getTypes();
        System.out.println("getTypess: " + output);
        assertEquals("getTypes returns incorrect number of elements.", 1, output.size());
        assertEquals("getTypes returns incorrect value.", new TypeLiteral<HttpAuthenticationMechanism>() {}.getType().toString(), output.iterator().next().toString());
    }

    @Test
    public void isAlternative() {
        BasicAuthenticationMechanismBean bamb = new BasicAuthenticationMechanismBean("realm", bm);
        System.out.println("isAlternative: " + bamb.isAlternative());
        assertFalse("isAlternative returns incorrect value.", bamb.isAlternative());
    }

    @Test
    public void getBeanClass() {
        BasicAuthenticationMechanismBean bamb = new BasicAuthenticationMechanismBean("realm", bm);
        System.out.println("getBeanClass: " + bamb.getBeanClass());
        assertEquals("getBeanClass returns incorrect value.", HttpAuthenticationMechanism.class.toString(), bamb.getBeanClass().toString());
    }

    @Test
    public void getInjectionPoints() {
        BasicAuthenticationMechanismBean bamb = new BasicAuthenticationMechanismBean("realm", bm);
        System.out.println("getInjectionPoints: " + bamb.getInjectionPoints());
        assertTrue("getInjectionPoints returns incorrect value.", bamb.getInjectionPoints().isEmpty());
    }

    @Test
    public void isNullable() {
        BasicAuthenticationMechanismBean bamb = new BasicAuthenticationMechanismBean("realm", bm);
        System.out.println("isNullable: " + bamb.isNullable());
        assertFalse("isNullable returns incorrect value.", bamb.isNullable());
    }

    @Test
    public void getId() {
        BasicAuthenticationMechanismBean bamb = new BasicAuthenticationMechanismBean("realm", bm);
        System.out.println("getId: " + bamb.getId());
        assertEquals("getId returns incorrect value.", bm.hashCode() + "#" + bamb.getName(), bamb.getId());
    }

    @Test
    public void getRealmName() {
        String realmName = "myRealm";
        BasicAuthenticationMechanismBean bamb = new BasicAuthenticationMechanismBean(realmName, bm);
        System.out.println("getRealm: " + bamb.getRealmName());
        assertEquals("getRealm returns incorrect value.", realmName, bamb.getRealmName());
    }

}
