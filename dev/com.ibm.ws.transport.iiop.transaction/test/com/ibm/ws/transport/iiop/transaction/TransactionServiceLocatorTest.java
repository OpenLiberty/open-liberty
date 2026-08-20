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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.TransactionManager;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Test;

import com.ibm.tx.remote.RemoteTransactionController;
import com.ibm.ws.transport.iiop.transaction.extension.TransactionHandlerContext;
import com.ibm.ws.transport.iiop.transaction.extension.TransactionProtocolProvider;

/**
 * Unit tests for {@link TransactionServiceLocator}.
 *
 * <p>This class manages a {@code static volatile} singleton. Every test that
 * touches the singleton or the factory's {@code activeFactory} static MUST
 * clean up in {@code @After} to prevent inter-test pollution.
 *
 * <p>TransactionServiceLocator.create() is package-private, so this test must
 * live in the same package.
 */
public class TransactionServiceLocatorTest {

    // All mocked types are interfaces — no ClassImposteriser (JDK 17 safe).
    private final Mockery mock = new JUnit4Mockery();

    private final TransactionHandlerContext   context = mock.mock(TransactionHandlerContext.class);
    private final TransactionProtocolProvider p1      = mock.mock(TransactionProtocolProvider.class, "p1");
    private final TransactionProtocolProvider p2      = mock.mock(TransactionProtocolProvider.class, "p2");

    @After
    public void tearDown() {
        // MANDATORY: clear both statics after every test
        TransactionServiceLocator.clearInstance();
        setActiveFactory(null);
    }

    // -------------------------------------------------------------------------
    // Reflection helper — sets TransactionSubsystemFactory.activeFactory
    // -------------------------------------------------------------------------

    private static void setActiveFactory(TransactionSubsystemFactory f) {
        try {
            Field field = TransactionSubsystemFactory.class.getDeclaredField("activeFactory");
            field.setAccessible(true);
            field.set(null, f);
        } catch (Exception e) {
            throw new RuntimeException("Could not set activeFactory via reflection", e);
        }
    }

    // -------------------------------------------------------------------------
    // Singleton lifecycle
    // -------------------------------------------------------------------------

    @Test
    public void testGetInstance_nullAfterClear() {
        TransactionServiceLocator.clearInstance();
        assertNull("getInstance() must return null after clearInstance() with no active factory",
                   TransactionServiceLocator.getInstance());
    }

    @Test
    public void testSetInstance_thenGet() {
        TransactionServiceLocator locator =
            TransactionServiceLocator.create(context, new HashMap<Integer, TransactionProtocolProvider>());
        TransactionServiceLocator.setInstance(locator);
        assertSame("getInstance() must return the locator set via setInstance()",
                   locator, TransactionServiceLocator.getInstance());
    }

    @Test
    public void testClearInstance_afterSet() {
        TransactionServiceLocator.setInstance(
            TransactionServiceLocator.create(context, new HashMap<Integer, TransactionProtocolProvider>()));
        TransactionServiceLocator.clearInstance();
        assertNull("getInstance() must return null after clearInstance()",
                   TransactionServiceLocator.getInstance());
    }

    // -------------------------------------------------------------------------
    // Factory method
    // -------------------------------------------------------------------------

    @Test
    public void testCreate_returnsNonNull() {
        assertNotNull("create() must return a non-null locator",
                      TransactionServiceLocator.create(context, new HashMap<Integer, TransactionProtocolProvider>()));
    }

    @Test
    public void testGetContext_returnsConstructed() {
        TransactionServiceLocator loc =
            TransactionServiceLocator.create(context, new HashMap<Integer, TransactionProtocolProvider>());
        assertSame("getContext() must return the context passed to create()",
                   context, loc.getContext());
    }

    // -------------------------------------------------------------------------
    // Provider access
    // -------------------------------------------------------------------------

    @Test
    public void testGetProviders_unmodifiable() {
        Map<Integer, TransactionProtocolProvider> map = new HashMap<Integer, TransactionProtocolProvider>();
        TransactionServiceLocator loc = TransactionServiceLocator.create(context, map);
        try {
            loc.getProviders().put(99, p1);
            fail("getProviders() must return an unmodifiable view");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testGetProviders_reflectsLiveMap() {
        Map<Integer, TransactionProtocolProvider> map = new HashMap<Integer, TransactionProtocolProvider>();
        TransactionServiceLocator loc = TransactionServiceLocator.create(context, map);
        mock.checking(new Expectations() {{
            allowing(p1).getIORTagId(); will(returnValue(42));
        }});
        map.put(42, p1);
        assertTrue("getProviders() must reflect additions to the backing map",
                   loc.getProviders().containsKey(42));
    }

    @Test
    public void testGetSortedProviders_empty() {
        TransactionServiceLocator loc =
            TransactionServiceLocator.create(context, new HashMap<Integer, TransactionProtocolProvider>());
        assertTrue("getSortedProviders() on empty map must return empty list",
                   loc.getSortedProviders().isEmpty());
    }

    @Test
    public void testGetSortedProviders_sortedByPriority() {
        mock.checking(new Expectations() {{
            allowing(p1).getPriority(); will(returnValue(10));
            allowing(p1).getIORTagId(); will(returnValue(1));
            allowing(p2).getPriority(); will(returnValue(5));
            allowing(p2).getIORTagId(); will(returnValue(2));
        }});
        Map<Integer, TransactionProtocolProvider> map = new HashMap<Integer, TransactionProtocolProvider>();
        map.put(1, p1);
        map.put(2, p2);
        TransactionServiceLocator loc = TransactionServiceLocator.create(context, map);
        List<TransactionProtocolProvider> sorted = loc.getSortedProviders();
        assertEquals("Expected 2 providers in sorted list", 2, sorted.size());
        assertSame("Provider with priority 5 must be first", p2, sorted.get(0));
        assertSame("Provider with priority 10 must be second", p1, sorted.get(1));
    }

    @Test
    public void testGetSortedProviders_isCopy() {
        Map<Integer, TransactionProtocolProvider> map = new HashMap<Integer, TransactionProtocolProvider>();
        TransactionServiceLocator loc = TransactionServiceLocator.create(context, map);
        List<TransactionProtocolProvider> sorted = loc.getSortedProviders();
        sorted.clear(); // mutate the returned list
        // The original backing map must be unaffected
        assertEquals("Mutating the sorted list must not affect the backing map", 0, map.size());
    }

    // -------------------------------------------------------------------------
    // Lazy initialisation via activeFactory
    // -------------------------------------------------------------------------

    /**
     * Verifies the double-checked locking lazy-init path in {@code getInstance()}:
     * when {@code instance} is null but a wired factory is set as {@code activeFactory},
     * {@code getInstance()} must initialise and return a locator.
     */
    @Test
    public void testGetInstance_lazyInit_viaActiveFactory() {
        // Arrange: inject services into a real factory instance
        TransactionSubsystemFactory factory = new TransactionSubsystemFactory();
        factory.setTransactionManager(mock.mock(TransactionManager.class, "lazyTm"));
        factory.setRemoteTransactionController(
            mock.mock(RemoteTransactionController.class, "lazyRtc"));
        setActiveFactory(factory);
        TransactionServiceLocator.clearInstance(); // ensure null before test

        // Act: first call triggers lazy init
        TransactionServiceLocator result = TransactionServiceLocator.getInstance();

        // Assert
        assertNotNull("Lazy init must produce a locator when active factory is available", result);
        assertSame("Subsequent getInstance() calls must return the same instance",
                   result, TransactionServiceLocator.getInstance());
    }
}
