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

import java.io.UnsupportedEncodingException;

import com.ibm.ws.zos.channel.wola.internal.ByteBufferVector;
import com.ibm.ws.zos.channel.wola.internal.natv.CodepageUtils;

/**
 * The WOLA Service Name context identifies the target service of the WOLA request.
 *
 * On the Liberty server side, this is the JNDI name of the target WOLA EJB.
 *
 * On the client-hosted service side, this is the name of the service to be invoked.
 *
 * !!! NOTE !!!: The context format must be kept in sync with
 * com.ibm.zos.native/include/server_wola_message.h.
 */
public class WolaServiceNameContext extends WolaMessageContext {

    /**
     * ServiceName context eyecatcher ('BBOASNC ' in EBCDIC).
     */
    public static final byte[] EyeCatcher = CodepageUtils.getEbcdicBytesPadded("BBOASNC ", 8); // 0xc2c2d6c1e2d5c340L;

    /**
     * Offset to the nameLength field within the context.
     */
    public static final int NameLengthOffset = 0x12;

    /**
     * Offset to the name field within the context
     */
    public static final int NameOffset = 0x14;

    /**
     * Max length of the serviceName.
     *
     * !!! Note: must be kept in sync with com.ibm.zos.native/include/server_wola_message.h
     */
    public static final int BBOA_REQUEST_SERVICE_NAME_MAX = 256;

    /**
     * The service name (cached here once it is parsed).
     */
    private String serviceName;

    /**
     * The service name as EBCDIC bytes (cached here once it is parsed).
     */
    private final byte[] serviceNameAsEBCDICBytes;

    /**
     * CTOR. Caller has already verified that the context ID is for this type of context.
     *
     * @param rawData - The context data (must not be null).
     * @param offset  - The offset into the rawData where the context starts.
     * @param length  - The full length (header plus data) of the context.
     *
     * @throws WolaMessageParseException if the raw data doesn't look like a WolaServiceNameContext.
     */
    public WolaServiceNameContext(ByteBufferVector rawData, int offset, int length) throws WolaMessageParseException {
        super(rawData, offset, length);
        verifyRawData(rawData, offset, length);
        int serviceNameLength = rawData.getUnsignedShort(offset + NameLengthOffset);
        serviceNameAsEBCDICBytes = new byte[serviceNameLength];
        rawData.get(offset + NameOffset, serviceNameAsEBCDICBytes);
    }

    /**
     * CTOR. Build a context using the given serviceName.
     *
     * @param serviceName - must not be null.
     *
     * @throws IllegalArgumentException if the serviceName is longer than the maximum allowed length
     */
    public WolaServiceNameContext(String serviceName) {
        super(WolaServiceNameContext.EyeCatcher, WolaMessageContextId.BBOASNC_Identifier);

        if (serviceName.length() > BBOA_REQUEST_SERVICE_NAME_MAX) {
            throw new IllegalArgumentException("Service name length (" + serviceName.length() + ") is longer than max (" + BBOA_REQUEST_SERVICE_NAME_MAX + "). Service name: "
                                               + serviceName);
        }

        this.serviceName = serviceName;

        try {
            serviceNameAsEBCDICBytes = serviceName.getBytes(CodepageUtils.EBCDIC);
        } catch (UnsupportedEncodingException uee) {
            throw new IllegalArgumentException(uee);
        }
    }

    /**
     * @throws WolaMessageParseException if the raw data doesn't look like a WolaServiceNameContext.
     */
    private void verifyRawData(ByteBufferVector rawData, int offset, int length) throws WolaMessageParseException {
        // Verify the context contains the length of the service name.
        if ((rawData.getLength() - offset) < WolaServiceNameContext.NameOffset) {
            throw new WolaMessageParseException("WolaServiceNameContext is incomplete. Raw data length: "
                                                + rawData.getLength() + "; ByteBufferVector: " + rawData);
        }

        // Verify the buffer contains the entire context
        int fullContextLen = WolaServiceNameContext.NameOffset + rawData.getUnsignedShort(offset + WolaServiceNameContext.NameLengthOffset);
        if ((rawData.getLength() - offset) < fullContextLen) {
            throw new WolaMessageParseException("WolaServiceNameContext is incomplete. Expected full context length: "
                                                + fullContextLen + "; Raw data length: " + (rawData.getLength() - offset) + "; ByteBufferVector: " + rawData);
        }
    }

    /**
     * @return the service name
     */
    public String getServiceName() {

        if (serviceName == null) {
            try {
                byte[] serviceNameBytes = getServiceNameAsEBCDICBytes();
                serviceName = new String(serviceNameBytes, CodepageUtils.EBCDIC);
            } catch (UnsupportedEncodingException uee) {
                throw new IllegalArgumentException(uee);
            }
        }

        return serviceName;
    }

    /**
     * @return the service name as EBCDIC bytes.
     */
    public byte[] getServiceNameAsEBCDICBytes() {
        return serviceNameAsEBCDICBytes;
    }

    /** {@inheritDoc} */
    @Override
    byte[] getBytes() {
        ByteBufferVector rawBytes = new ByteBufferVector(new byte[WolaMessageContext.HeaderSize + 4 + serviceNameAsEBCDICBytes.length]);
        fillHeader(rawBytes);
        rawBytes.putShort(NameLengthOffset, (short) serviceNameAsEBCDICBytes.length);
        rawBytes.put(NameOffset, serviceNameAsEBCDICBytes);
        return rawBytes.toByteArray();
    }
}
