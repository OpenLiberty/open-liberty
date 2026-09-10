/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.transport.iiop.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import javax.transaction.TransactionManager;

import org.apache.yoko.osgi.locator.Register;
import org.apache.yoko.osgi.locator.ServiceProvider;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.omg.CORBA.Policy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.tx.remote.RemoteTransactionController;
import com.ibm.ws.transport.iiop.transaction.extension.TransactionProtocolProvider;

/**
 * Unit tests for the pure-Java subset of {@link TransactionSubsystemFactory}:
 * provider registration/removal, getProviders, and createServiceLocator.
 *
 * <p>Also contains the single worthwhile {@link ServerTransactionPolicy} assertion
 * (copy() shares the same config reference) since that policy is created by the
 * factory's getTargetPolicy() method.
 *
 * <p>The activation constructor performs the production initialization; OSGi-specific
 * lifecycle integration is not tested here.
 */
public class TransactionSubsystemFactoryTest {

    // All mocked types are interfaces — no ClassImposteriser (JDK 17 safe).
    private final Mockery mock = new JUnit4Mockery();

    private final TransactionProtocolProvider p1  = mock.mock(TransactionProtocolProvider.class, "p1");
    private final TransactionProtocolProvider p2  = mock.mock(TransactionProtocolProvider.class, "p2");
    private final Register                    providerRegistry = mock.mock(Register.class);
    private final TransactionManager          tm  = mock.mock(TransactionManager.class);
    private final RemoteTransactionController rtc = mock.mock(RemoteTransactionController.class);

    private TransactionSubsystemFactory factory;

    @Before
    public void setUp() {
        mock.checking(new Expectations() {{
            allowing(providerRegistry).registerProvider(with(any(ServiceProvider.class)));
        }});
        factory = new TransactionSubsystemFactory(providerRegistry, tm, rtc);
    }

    @After
    public void tearDown() {
        // Clear any locator or active factory the constructor may have pushed during tests
        TransactionServiceLocator.clearInstance();
        setActiveFactory(null);
    }

    private static void setActiveFactory(TransactionSubsystemFactory factory) {
        try {
            Field field = TransactionSubsystemFactory.class.getDeclaredField("activeFactory");
            field.setAccessible(true);
            field.set(null, factory);
        } catch (Exception e) {
            throw new RuntimeException("Could not set activeFactory via reflection", e);
        }
    }

    // -------------------------------------------------------------------------
    // getProviders
    // -------------------------------------------------------------------------

    @Test
    public void testGetSortedProviders_empty() {
        assertTrue("New factory must have no providers", factory.getProviders().isEmpty());
    }

    @Test
    public void testAddProvider_appearsInSortedList() {
        mock.checking(new Expectations() {{
            allowing(p1).getIORTagId(); will(returnValue(1));
        }});
        factory.addTransactionProtocolProvider(p1);
        assertTrue("Provider must appear in getProviders() after addTransactionProtocolProvider()",
                   factory.getProviders().contains(p1));
    }

    @Test
    public void testRemoveProvider_removedFromList() {
        mock.checking(new Expectations() {{
            allowing(p1).getIORTagId(); will(returnValue(1));
        }});
        factory.addTransactionProtocolProvider(p1);
        factory.removeTransactionProtocolProvider(p1);
        assertFalse("Provider must be absent after removeTransactionProtocolProvider()",
                    factory.getProviders().contains(p1));
    }

    @Test
    public void testRemoveProvider_nonExistent_noException() {
        mock.checking(new Expectations() {{
            allowing(p1).getIORTagId(); will(returnValue(1));
        }});
        // Must not throw — ConcurrentHashMap.remove() on absent key is a no-op
        factory.removeTransactionProtocolProvider(p1);
    }

    @Test
    public void testGetSortedProviders_twoProviders_bothPresent() {
        mock.checking(new Expectations() {{
            allowing(p1).getIORTagId(); will(returnValue(1));
            allowing(p2).getIORTagId(); will(returnValue(2));
        }});
        factory.addTransactionProtocolProvider(p1);
        factory.addTransactionProtocolProvider(p2);
        List<TransactionProtocolProvider> providers = factory.getProviders();
        assertEquals("Expected 2 providers", 2, providers.size());
        assertTrue("p1 must be present", providers.contains(p1));
        assertTrue("p2 must be present", providers.contains(p2));
    }

    // -------------------------------------------------------------------------
    // createServiceLocator
    // -------------------------------------------------------------------------

    @Test
    public void testCreateServiceLocator_constructorDependenciesPresent_returnsLocator() {
        assertNotNull("createServiceLocator() must return a non-null locator when constructed with required services",
                      factory.createServiceLocator());
    }

    // -------------------------------------------------------------------------
    // ServerTransactionPolicy.copy() — single worthwhile policy assertion
    // -------------------------------------------------------------------------

    /**
     * ServerTransactionPolicy.copy() must return a new distinct instance that
     * shares the SAME ServerTransactionPolicyConfig reference as the original.
     *
     * This matters because ServerTransactionInterceptor calls
     * policy.getConfig().isTransactionImportEnabled() — if copy() created a new
     * config with different defaults the interceptor gate would silently break.
     */
    @Test
    public void testServerTransactionPolicy_copy_sharesSameConfig() {
        ServerTransactionPolicyConfig config = new ServerTransactionPolicyConfig(false, 10);
        ServerTransactionPolicy policy = new ServerTransactionPolicy(config);

        Policy copy = policy.copy();

        assertNotSame("copy() must return a new instance, not the original", policy, copy);
        assertSame("copy() must share the same ServerTransactionPolicyConfig reference",
                   config, ((ServerTransactionPolicy) copy).getConfig());
    }
}
