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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import javax.transaction.TransactionManager;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;

import com.ibm.tx.remote.RemoteTransactionController;

/**
 * Unit tests for {@link TransactionHandlerContextImpl}.
 *
 * TransactionHandlerContextImpl is package-private so this test must live in the
 * same package. Tests verify that the constructor arguments are stored correctly
 * and accessible via the SPI getters.
 */
public class TransactionHandlerContextImplTest {

    // All mocked types are interfaces — no ClassImposteriser needed (JDK 17 safe).
    private final Mockery mock = new JUnit4Mockery();

    private final RemoteTransactionController rtc = mock.mock(RemoteTransactionController.class);
    private final TransactionManager          tm  = mock.mock(TransactionManager.class);

    @Test
    public void testGetRemoteTransactionController() {
        TransactionHandlerContextImpl ctx = new TransactionHandlerContextImpl(rtc, tm);
        assertSame("getRemoteTransactionController() must return the injected RTC",
                   rtc, ctx.getRemoteTransactionController());
    }

    @Test
    public void testGetTransactionManager() {
        TransactionHandlerContextImpl ctx = new TransactionHandlerContextImpl(rtc, tm);
        assertSame("getTransactionManager() must return the injected TM",
                   tm, ctx.getTransactionManager());
    }

    /**
     * Null arguments are accepted without NPE. Providers must be able to receive a
     * context before all services are available (defensive construction).
     */
    @Test
    public void testNullsPermitted() {
        TransactionHandlerContextImpl ctx = new TransactionHandlerContextImpl(null, null);
        assertNull("getRemoteTransactionController() must return null when constructed with null",
                   ctx.getRemoteTransactionController());
        assertNull("getTransactionManager() must return null when constructed with null",
                   ctx.getTransactionManager());
    }
}
