/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.zos.channel.wola.internal.msg;

import com.ibm.ws.zos.channel.wola.internal.ByteBufferVector;

/**
 * A WOLA message contains 0 or more of these message contexts.
 *
 * !!! NOTE: !!! The format of the WolaMessageContext must be kept in sync with
 * com.ibm.zos.native/include/server_wola_message.h
 */
public abstract class WolaMessageContext {

    /**
     * The size of the WOLA context header (sans data).
     */
    public static final int HeaderSize = 0x10;

    /**
     * Offset to the eyecatcher field within the context header.
     */
    public static final int EyeCatcherOffset = 0;

    /**
     * Offset to the contextId field with then context header.
     */
    public static final int ContextIdOffset = 0x08;

    /**
     * Offset to the contextLen field with then context header.
     * contextLen = length of context data NOT INCLUDING THE HEADER.
     */
    public static final int ContextLenOffset = 0x0c;

    /**
     * The eye catcher.
     */
    private final byte[] eyeCatcher;

    /**
     * The context ID.
     */
    private final WolaMessageContextId contextID;

    /**
     * CTOR.
     *
     * Note: The CTOR will NOT verify that the raw data looks like an actual context.
     * It assumes that has already been done.
     *
     * @param rawData - The raw context data (including header)
     * @param offset  - The offset of the context in rawData
     * @param length  - The length of the context.
     */
    public WolaMessageContext(ByteBufferVector rawData, int offset, int length) {
        eyeCatcher = new byte[8];
        rawData.get(offset + WolaMessageContext.EyeCatcherOffset, eyeCatcher);
        contextID = WolaMessageContextId.forNativeValue(rawData.getInt(offset + WolaMessageContext.ContextIdOffset));
    }

    /**
     * CTOR.
     *
     * @param eyeCatcher - The eyecatcher for this context
     * @param contextId  - The contextId for this context
     */
    public WolaMessageContext(byte[] eyeCatcher, WolaMessageContextId contextId) {
        this.eyeCatcher = new byte[8];
        System.arraycopy(eyeCatcher, 0, this.eyeCatcher, 0, 8);
        this.contextID = contextId;
    }

    /**
     * Verify a few things about the given WolaMessageContext rawData.
     *
     * TODO: when throwing parse exceptions, it would be nice to include at least some
     * of the raw hex data in the exception message (for debugging purposes).
     * Perhaps also include the overall offset within the WolaMessage??
     *
     * @param rawData The buffer containing the context
     * @param offset  The offset into the buffer containing the start of the context
     *
     * @throws WolaMessageParseException if the rawData doesn't look like a WOLA message context
     *                                       or if the data is incomplete
     * @throws IllegalArgumentException  if the contextId is not recognized.
     */
    protected static void verifyRawData(ByteBufferVector rawData, int offset) throws WolaMessageParseException, IllegalArgumentException {
        int rawDataRemaining = rawData.getLength() - offset;
        if (rawDataRemaining < WolaMessageContext.HeaderSize) {
            throw new WolaMessageParseException("WolaMessageContext is incomplete. Expected header size: " +
                                                WolaMessageContext.HeaderSize + "; Raw data length: " + rawData.getLength() + "; Offset: " + offset + "; ByteBufferVector: "
                                                + rawData);
        }

        // Verify the contextId is valid.
        WolaMessageContextId.forNativeValue(rawData.getInt(offset + WolaMessageContext.ContextIdOffset));

        // Verify the buffer contains the entire context
        int fullContextLen = WolaMessageContext.HeaderSize + rawData.getInt(offset + WolaMessageContext.ContextLenOffset);
        if (rawDataRemaining < fullContextLen) {
            throw new WolaMessageParseException("WolaMessageContext is incomplete. Expected full context length: " +
                                                fullContextLen + "; Raw data length: " + rawData.getLength() + "; Offset: " + offset + "; ByteBufferVector: " + rawData);
        }
    }

    /**
     * @return The WolaMessageContextId for this context.
     */
    protected WolaMessageContextId getContextId() {
        return contextID;
    }

    /**
     * Fills the context header into the input byte array.
     */
    protected void fillHeader(ByteBufferVector rawData) {
        rawData.put(EyeCatcherOffset, this.eyeCatcher);
        rawData.putInt(ContextIdOffset, contextID.nativeValue);
        rawData.putInt(ContextLenOffset, rawData.getLength() - HeaderSize);
    }

    /**
     * Obtains a byte[] version of this context.
     */
    abstract byte[] getBytes();
}
