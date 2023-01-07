/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.security.jakartasec.cdi.extensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Type;
import java.util.Set;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;

public class OpenIdContextBeanTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private OpenIdContextBean openIdContextBean;
    private BeanManager beanManager;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        beanManager = mockery.mock(BeanManager.class);
        openIdContextBean = new OpenIdContextBean(beanManager);
    }

    @After
    public void tearDown() throws Exception {
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreate() {
        CreationalContext<OpenIdContext> creationalContext = mockery.mock(CreationalContext.class);

        assertTrue("Must create an OpenIdContext instance.", openIdContextBean.create(creationalContext) instanceof OpenIdContext);
    }

    @Test
    public void testGetScope() {
        assertEquals("Must be SessionScoped.", SessionScoped.class, openIdContextBean.getScope());
    }

    @SuppressWarnings("serial")
    @Test
    public void testGetType() {
        Set<Type> types = openIdContextBean.getTypes();
        String expectedTypeString = new TypeLiteral<OpenIdContext>() {
        }.getType().toString();

        assertEquals("Must be a TypeLiteral<OpenIdContextBean> type.", expectedTypeString, types.iterator().next().toString());
    }

    @Test
    public void testGetBeanClass() {
        assertEquals("Must be OpenIdContextBean.", OpenIdContextBean.class, openIdContextBean.getBeanClass());
    }

    @Test
    public void testGetOpenIdContext() {
        assertNotNull("Must be not Null.", openIdContextBean.getOpenIdContext());
    }

}
