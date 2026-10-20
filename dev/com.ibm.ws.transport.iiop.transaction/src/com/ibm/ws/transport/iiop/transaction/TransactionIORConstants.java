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

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Constants and utilities for transaction-related IOR tagged components.
 * This class centralizes shared constants used by both IORTransactionInterceptor
 * and ClientTransactionInterceptor.
 */
public final class TransactionIORConstants {

    /**
     * TAG_IBM_SERVER_UUID - Custom IOR tagged component for server instance UUID.
     * Structure: [length MSB, length LSB, version, UUID (16 bytes)]
     */
    public static final int TAG_IBM_SERVER_UUID = 0x49424d26;

    /**
     * Server instance UUID - generated once at class initialization and
     * regenerated on InstantOn restore.
     * This UUID uniquely identifies this server instance and is used for
     * local optimization detection (avoiding transaction propagation overhead
     * when calling back to the same server instance).
     *
     * Declared volatile (not final) so it can be regenerated on InstantOn restore.
     */
    public static volatile UUID SERVER_INSTANCE_UUID;
    
    /**
     * Pre-computed byte array representation of SERVER_INSTANCE_UUID (16 bytes).
     * Regenerated together with SERVER_INSTANCE_UUID by
     * {@link #regenerateServerInstanceUUID()}.
     */
    public static volatile byte[] SERVER_INSTANCE_UUID_BYTES;
    
    static {
        regenerateServerInstanceUUID();
    }

    /**
     * Generate a new random UUID for this server instance and update both
     * SERVER_INSTANCE_UUID and SERVER_INSTANCE_UUID_BYTES.
     *
     * Called at class initialization and again by the CheckpointHook on
     * InstantOn restore.
     */
    public static void regenerateServerInstanceUUID() {
        UUID uuid = UUID.randomUUID();
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        SERVER_INSTANCE_UUID = uuid;
        SERVER_INSTANCE_UUID_BYTES = buffer.array();
    }
    
    // Private constructor to prevent instantiation
    private TransactionIORConstants() {
        throw new AssertionError("Utility class should not be instantiated");
    }
}

// Made with Bob
