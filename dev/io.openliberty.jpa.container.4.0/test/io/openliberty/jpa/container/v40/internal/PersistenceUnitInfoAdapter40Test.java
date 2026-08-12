/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.jpa.container.v40.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Test;

import com.ibm.ws.jpa.management.JPAPUnitInfo;
import com.ibm.ws.jpa.management.PersistenceUnitInfoDelegate;

import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.spi.PersistenceUnitInfo;

/**
 * Verifies the binary API boundary implemented by the native Jakarta
 * Persistence 4 persistence-unit adapter.
 */
public class PersistenceUnitInfoAdapter40Test {
    @Test
    public void usesTheNativeJpa40TransactionTypeSignature() throws Exception {
        Method transactionTypeMethod = PersistenceUnitInfoAdapter40.class.getMethod("getTransactionType");

        assertEquals(PersistenceUnitTransactionType.class, transactionTypeMethod.getReturnType());
        assertTrue(PersistenceUnitInfo.class.isAssignableFrom(PersistenceUnitInfoAdapter40.class));
        assertTrue(PersistenceUnitInfoDelegate.class.isAssignableFrom(PersistenceUnitInfoAdapter40.class));
        assertFalse(Arrays.asList(JPAPUnitInfo.class.getInterfaces()).contains(PersistenceUnitInfo.class));
    }
}
