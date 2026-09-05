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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.transaction.TransactionManager;

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
 * provider registration/removal, getSortedProviders, and createServiceLocator
 * null guards.
 *
 * <p>Also contains the single worthwhile {@link ServerTransactionPolicy} assertion
 * (copy() shares the same config reference) since that policy is created by the
 * factory's getTargetPolicy() method.
 *
 * <p>activate()/deactivate() require live OSGi BundleContext and Register — not tested here.
 */
public class TransactionSubsystemFactoryTest {

    // All mocked types are interfaces — no ClassImposteriser (JDK 17 safe).
    private final Mockery mock = new JUnit4Mockery();

    private final TransactionProtocolProvider p1  = mock.mock(TransactionProtocolProvider.class, "p1");
    private final TransactionProtocolProvider p2  = mock.mock(TransactionProtocolProvider.class, "p2");
    private final TransactionManager          tm  = mock.mock(TransactionManager.class);
    private final RemoteTransactionController rtc = mock.mock(RemoteTransactionController.class);

    private TransactionSubsystemFactory factory;

    @Before
    public void setUp() {
        factory = new TransactionSubsystemFactory();
    }

    @After
    public void tearDown() {
        // Clear any locator the factory may have pushed during tests
        TransactionServiceLocator.clearInstance();
    }

    // -------------------------------------------------------------------------
    // getSortedProviders
    // -------------------------------------------------------------------------

    @Test
    public void testGetSortedProviders_empty() {
        assertTrue("New factory must have no providers", factory.getSortedProviders().isEmpty());
    }

    @Test
    public void testAddProvider_appearsInSortedList() {
        mock.checking(new Expectations() {{
            allowing(p1).getIORTagId(); will(returnValue(1));
            allowing(p1).getPriority(); will(returnValue(10));
        }});
        factory.addTransactionProtocolProvider(p1);
        assertTrue("Provider must appear in getSortedProviders() after addTransactionProtocolProvider()",
                   factory.getSortedProviders().contains(p1));
    }

    @Test
    public void testRemoveProvider_removedFromList() {
        mock.checking(new Expectations() {{
            allowing(p1).getIORTagId(); will(returnValue(1));
            allowing(p1).getPriority(); will(returnValue(10));
        }});
        factory.addTransactionProtocolProvider(p1);
        factory.removeTransactionProtocolProvider(p1);
        assertFalse("Provider must be absent after removeTransactionProtocolProvider()",
                    factory.getSortedProviders().contains(p1));
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
    public void testSortedProviders_priorityOrder() {
        mock.checking(new Expectations() {{
            allowing(p1).getIORTagId(); will(returnValue(1));
            allowing(p1).getPriority(); will(returnValue(10));
            allowing(p2).getIORTagId(); will(returnValue(2));
            allowing(p2).getPriority(); will(returnValue(3));
        }});
        factory.addTransactionProtocolProvider(p1);
        factory.addTransactionProtocolProvider(p2);
        List<TransactionProtocolProvider> sorted = factory.getSortedProviders();
        assertEquals("Expected 2 providers", 2, sorted.size());
        assertSame("Provider with priority 3 must be first", p2, sorted.get(0));
        assertSame("Provider with priority 10 must be second", p1, sorted.get(1));
    }

    @Test
    public void testSortedProviders_equalPriority_bothPresent() {
        mock.checking(new Expectations() {{
            allowing(p1).getIORTagId(); will(returnValue(1));
            allowing(p1).getPriority(); will(returnValue(5));
            allowing(p2).getIORTagId(); will(returnValue(2));
            allowing(p2).getPriority(); will(returnValue(5));
        }});
        factory.addTransactionProtocolProvider(p1);
        factory.addTransactionProtocolProvider(p2);
        assertEquals("Both providers with equal priority must appear in sorted list",
                     2, factory.getSortedProviders().size());
    }

    // -------------------------------------------------------------------------
    // createServiceLocator null guards
    // -------------------------------------------------------------------------

    @Test
    public void testCreateServiceLocator_nullTM_returnsNull() {
        // Only RTC injected — TM is missing
        factory.setRemoteTransactionController(rtc);
        assertNull("createServiceLocator() must return null when TransactionManager is not injected",
                   factory.createServiceLocator());
    }

    @Test
    public void testCreateServiceLocator_nullRTC_returnsNull() {
        // Only TM injected — RTC is missing
        factory.setTransactionManager(tm);
        assertNull("createServiceLocator() must return null when RemoteTransactionController is not injected",
                   factory.createServiceLocator());
    }

    @Test
    public void testCreateServiceLocator_bothPresent_returnsLocator() {
        factory.setTransactionManager(tm);
        factory.setRemoteTransactionController(rtc);
        assertNotNull("createServiceLocator() must return a non-null locator when both services are injected",
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
