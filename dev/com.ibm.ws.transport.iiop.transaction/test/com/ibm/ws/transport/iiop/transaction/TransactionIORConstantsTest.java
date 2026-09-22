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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.junit.Test;

/**
 * Unit tests for {@link TransactionIORConstants}.
 *
 * Regression guard for the UUID constants and byte encoding used by
 * IORTransactionInterceptor and ClientTransactionInterceptor for the
 * local-call optimisation (TAG_IBM_SERVER_UUID).
 */
public class TransactionIORConstantsTest {

    /**
     * TAG_IBM_SERVER_UUID must be 0x49424d26 — the value tWAS/OL uses to identify
     * the server instance UUID tagged component in the IOR.
     */
    @Test
    public void testTagIBMServerUUID_value() {
        assertEquals("TAG_IBM_SERVER_UUID must be 0x49424d26",
                     0x49424d26, TransactionIORConstants.TAG_IBM_SERVER_UUID);
    }

    /**
     * SERVER_INSTANCE_UUID must be non-null — it is generated once at class load
     * via UUID.randomUUID() and used for the lifetime of the JVM.
     */
    @Test
    public void testServerInstanceUUID_notNull() {
        assertNotNull("SERVER_INSTANCE_UUID must not be null",
                      TransactionIORConstants.SERVER_INSTANCE_UUID);
    }

    /**
     * SERVER_INSTANCE_UUID_BYTES must be exactly 16 bytes — the standard UUID byte
     * representation (two longs). If the static initializer changes this length,
     * the IOR payload parsing in ClientTransactionInterceptor will break.
     */
    @Test
    public void testServerInstanceUUIDBytesLength() {
        assertEquals("SERVER_INSTANCE_UUID_BYTES must be 16 bytes",
                     16, TransactionIORConstants.SERVER_INSTANCE_UUID_BYTES.length);
    }

    /**
     * The byte array must be a faithful encoding of SERVER_INSTANCE_UUID — i.e.
     * reading it back as two longs must reconstruct the same UUID.
     * This guards the ByteBuffer.putLong(MSB)/putLong(LSB) encoding used in the
     * static initializer.
     */
    @Test
    public void testServerInstanceUUIDBytesRoundTrip() {
        ByteBuffer buf = ByteBuffer.wrap(TransactionIORConstants.SERVER_INSTANCE_UUID_BYTES);
        UUID reconstructed = new UUID(buf.getLong(), buf.getLong());
        assertEquals("UUID reconstructed from SERVER_INSTANCE_UUID_BYTES must equal SERVER_INSTANCE_UUID",
                     TransactionIORConstants.SERVER_INSTANCE_UUID, reconstructed);
    }

    /**
     * The private constructor must throw AssertionError to prevent instantiation.
     * This is the standard utility-class guard pattern.
     */
    @Test
    public void testConstructorThrowsAssertionError() throws Exception {
        Constructor<TransactionIORConstants> c =
            TransactionIORConstants.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
            fail("Expected InvocationTargetException wrapping AssertionError");
        } catch (InvocationTargetException e) {
            assertTrue("Cause must be AssertionError, was: " + e.getCause(),
                       e.getCause() instanceof AssertionError);
        }
    }
}
