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
import com.ibm.ws.security.javaeesec.authentication.mechanism.http.LoginToContinueProperties;
import com.ibm.ws.security.javaeesec.authentication.mechanism.http.LoginToContinuePropertiesImpl;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;


public class LoginToContinuePropertiesBeanTest {
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
    private final CreationalContext<LoginToContinueProperties> cc = context.mock(CreationalContext.class, "cc1");
    private final Annotation ltc = context.mock(Annotation.class, "an1");;
    private final Properties props = new Properties();

    @Before
    public void setUp() {}

    @After
    public void tearDown() throws Exception {
        context.assertIsSatisfied();
    }

    @Test
    public void create() {
        LoginToContinuePropertiesBean ltcpb = new LoginToContinuePropertiesBean(bm, ltc, props);
        assertEquals("create returns incorrect object.", "com.ibm.ws.security.javaeesec.authentication.mechanism.http.LoginToContinuePropertiesImpl", ltcpb.create(cc).getClass().getName());
    }

    @Test
    public void getName() {
        LoginToContinuePropertiesBean ltcpb = new LoginToContinuePropertiesBean(bm, ltc, props);
        System.out.println("getName: " + ltcpb.getName());
        assertEquals("getName returns incorrect value.", ltcpb.getClass().getName() + "[" + ltcpb.getTypes().iterator().next().toString() + "]", ltcpb.getName());
    }

    @Test
    public void getQualifiers() {
        LoginToContinuePropertiesBean ltcpb = new LoginToContinuePropertiesBean(bm, ltc, props);
        Set<Annotation> output = ltcpb.getQualifiers();
        System.out.println("getQualifiers: " + output);
        assertEquals("getQualifiers returns incorrect number of elements.", 1, output.size());
        assertEquals("getQualifiers returns incorrect value.", new AnnotationLiteral<Default>() {}.toString(), output.iterator().next().toString());
    }

    @Test
    public void getScope() {
        LoginToContinuePropertiesBean ltcpb = new LoginToContinuePropertiesBean(bm, ltc, props);
        System.out.println("getScope: " + ltcpb.getScope());
        assertEquals("getScope returns incorrect value.", ApplicationScoped.class.getName(), ltcpb.getScope().getName());
    }

    @Test
    public void getStereotypes() {
        LoginToContinuePropertiesBean ltcpb = new LoginToContinuePropertiesBean(bm, ltc, props);
        System.out.println("getScope: " + ltcpb.getStereotypes());
        assertTrue("getStereotypes returns incorrect value.", ltcpb.getStereotypes().isEmpty());
    }

    @Test
    public void getTypes() {
        LoginToContinuePropertiesBean ltcpb = new LoginToContinuePropertiesBean(bm, ltc, props);
        Set<Type> output = ltcpb.getTypes();
        System.out.println("getTypess: " + output);
        assertEquals("getTypes returns incorrect number of elements.", 1, output.size());
        assertEquals("getTypes returns incorrect value.", new TypeLiteral<LoginToContinueProperties>() {}.getType().toString(), output.iterator().next().toString());
    }

    @Test
    public void isAlternative() {
        LoginToContinuePropertiesBean ltcpb = new LoginToContinuePropertiesBean(bm, ltc, props);
        System.out.println("isAlternative: " + ltcpb.isAlternative());
        assertFalse("isAlternative returns incorrect value.", ltcpb.isAlternative());
    }

    @Test
    public void getBeanClass() {
        LoginToContinuePropertiesBean ltcpb = new LoginToContinuePropertiesBean(bm, ltc, props);
        System.out.println("getBeanClass: " + ltcpb.getBeanClass());
        assertEquals("getBeanClass returns incorrect value.", LoginToContinueProperties.class.toString(), ltcpb.getBeanClass().toString());
    }

    @Test
    public void getInjectionPoints() {
        LoginToContinuePropertiesBean ltcpb = new LoginToContinuePropertiesBean(bm, ltc, props);
        System.out.println("getInjectionPoints: " + ltcpb.getInjectionPoints());
        assertTrue("getInjectionPoints returns incorrect value.", ltcpb.getInjectionPoints().isEmpty());
    }

    @Test
    public void isNullable() {
        LoginToContinuePropertiesBean ltcpb = new LoginToContinuePropertiesBean(bm, ltc, props);
        System.out.println("isNullable: " + ltcpb.isNullable());
        assertFalse("isNullable returns incorrect value.", ltcpb.isNullable());
    }

    @Test
    public void getId() {
        LoginToContinuePropertiesBean ltcpb = new LoginToContinuePropertiesBean(bm, ltc, props);
        System.out.println("getId: " + ltcpb.getId());
        assertEquals("getId returns incorrect value.", bm.hashCode() + "#" + ltcpb.getName(), ltcpb.getId());
    }

}
