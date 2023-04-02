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

import io.openliberty.security.jakartasec.identitystore.OidcIdentityStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.security.enterprise.identitystore.IdentityStore;

public class OidcIdentityStoreBeanTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private OidcIdentityStoreBean oidcIdentityStoreBean;
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
        oidcIdentityStoreBean = new OidcIdentityStoreBean(beanManager);
    }

    @After
    public void tearDown() throws Exception {
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreate() {
        CreationalContext<IdentityStore> creationalContext = mockery.mock(CreationalContext.class);

        assertTrue("Must create an OidcIdentityStore instance.", oidcIdentityStoreBean.create(creationalContext) instanceof OidcIdentityStore);
    }

    @Test
    public void testGetScope() {
        assertEquals("Must be ApplicationScoped.", ApplicationScoped.class, oidcIdentityStoreBean.getScope());
    }

    @SuppressWarnings("serial")
    @Test
    public void testGetType() {
        Set<Type> types = oidcIdentityStoreBean.getTypes();
        String expectedTypeString = new TypeLiteral<IdentityStore>() {
        }.getType().toString();

        assertEquals("Must be a TypeLiteral<IdentityStore> type.", expectedTypeString, types.iterator().next().toString());
    }

    @Test
    public void testGetBeanClass() {
        assertEquals("Must be OidcIdentityStoreBean.", OidcIdentityStoreBean.class, oidcIdentityStoreBean.getBeanClass());
    }

}
