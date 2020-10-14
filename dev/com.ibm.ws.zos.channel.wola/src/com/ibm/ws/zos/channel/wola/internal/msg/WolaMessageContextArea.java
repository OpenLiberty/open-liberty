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

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.zos.channel.wola.internal.ByteBufferVector;
import com.ibm.ws.zos.channel.wola.internal.natv.CodepageUtils;

/**
 * The WolaMessageContextArea contains the list of WolaMessageContexts for a
 * particular WOLA message.
 *
 * !!! NOTE: !!! The format of the WolaMessageContextArea must be kept in sync with
 * com.ibm.zos.native/include/server_wola_message.h
 */
public class WolaMessageContextArea {

    /**
     * The size of the WOLA context area header (sans context data).
     */
    public static final int HeaderSize = 0x10;

    /**
     * Context area eyecatcher ('BBOACTX ' in EBCDIC).
     */
    public static final long EyeCatcher = CodepageUtils.getEbcdicBytesAsLong("BBOACTX "); // 0xc2c2d6c1c3e3e740L;

    /**
     * Offset to the eyecatcher field within the context area header.
     */
    public static final int EyeCatcherOffset = 0;

    /**
     * Offset to the numContexts field with then context area header.
     */
    public static final int NumContextsOffset = 0x0c;

    /**
     * The raw context area data.
     */
    private ByteBufferVector rawData = null;

    /**
     * The offset into the rawData where the contexts start.
     */
    private int offset = 0;

    /**
     * The length of the rawData containing the contexts.
     */
    private int length = 0;

    /**
     * A list of WolaMessageContexts parsed from the raw data.
     */
    private List<WolaMessageContext> wolaMessageContexts = null;

    /**
     * Flag indicating the rawData is in sync with the wolaMessageContexts list.
     */
    private boolean listInSyncWithRawData = true;

    /**
     *
     * @param data   - a WOLA message
     * @param offset - the offset into the byte buffer vector where the contexts start.
     * @param length - the length of the contexts
     *
     * @throws WolaMessageParseException if context area raw data is not valid.
     */
    public WolaMessageContextArea(ByteBufferVector data, int offset, int length) throws WolaMessageParseException {
        this.rawData = data;
        this.offset = offset;
        this.length = length;
        verifyRawData();
    }

    /**
     * CTOR. Builds an empty context area.
     */
    public WolaMessageContextArea() {
        rawData = new ByteBufferVector(new byte[WolaMessageContextArea.HeaderSize]);
        length = WolaMessageContextArea.HeaderSize;
        rawData.putLong(offset + WolaMessageContextArea.EyeCatcherOffset, WolaMessageContextArea.EyeCatcher);

        // Init the list here so getWolaMessageContexts doesn't try to parse them.
        wolaMessageContexts = new ArrayList<WolaMessageContext>(5);
    }

    /**
     * @param rawData - context area raw data, as parsed from the WOLA message
     *
     * @throws WolaMessageParseException if the raw data does not look like a valid context area.
     */
    protected void verifyRawData() throws WolaMessageParseException {
        if (length < WolaMessageContextArea.HeaderSize) {
            throw new WolaMessageParseException("WolaMessageContextArea is incomplete. Expected header size: " +
                                                WolaMessageContextArea.HeaderSize + "; Raw data length: " + length
                                                + "; ByteBufferVector: " + rawData);
        }

        if (rawData.getLong(offset + WolaMessageContextArea.EyeCatcherOffset) != WolaMessageContextArea.EyeCatcher) {
            throw new WolaMessageParseException("Invalid Eye Catcher for WolaMessageContextArea: "
                                                + Long.toHexString(rawData.getLong(offset + WolaMessageContextArea.EyeCatcherOffset)));
        }
    }

    /**
     * @return rawData.
     */
    protected ByteBufferVector getRawData() {
        if (listInSyncWithRawData == false) {
            rawData = new ByteBufferVector(new byte[WolaMessageContextArea.HeaderSize]);
            offset = 0;
            rawData.putLong(offset + WolaMessageContextArea.EyeCatcherOffset, WolaMessageContextArea.EyeCatcher);
            rawData.putInt(offset + WolaMessageContextArea.NumContextsOffset, wolaMessageContexts.size());

            for (WolaMessageContext curContext : wolaMessageContexts) {
                byte[] curContextBytes = curContext.getBytes();
                rawData.append(curContextBytes);
            }

            length = rawData.getLength();
            listInSyncWithRawData = true;
        }

        return rawData;
    }

    /**
     * @return A List<WolaMessageContext>
     *
     * @throws WolaMessageParseException if any of the contexts could not be parsed
     */
    protected List<WolaMessageContext> getWolaMessageContexts() throws WolaMessageParseException {

        if (wolaMessageContexts == null) {

            int numContexts = rawData.getInt(offset + WolaMessageContextArea.NumContextsOffset);

            wolaMessageContexts = new ArrayList<WolaMessageContext>(numContexts);

            // The first context starts immediately after the header.
            int contextOffset = offset + WolaMessageContextArea.HeaderSize;

            // Parse out the WolaMessageContexts.
            for (int i = 0; i < numContexts; ++i) {
                WolaMessageContext.verifyRawData(rawData, contextOffset);

                int fullContextLen = WolaMessageContext.HeaderSize + rawData.getInt(contextOffset + WolaMessageContext.ContextLenOffset);
                WolaMessageContextId contextId = WolaMessageContextId.forNativeValue(rawData.getInt(contextOffset + WolaMessageContext.ContextIdOffset));
                WolaMessageContext curContext = null;
                switch (contextId) {
                    case BBOASNC_Identifier:
                        curContext = new WolaServiceNameContext(rawData, contextOffset, fullContextLen);
                        break;
                    case CicsLinkServerContextId:
                        // We should not be parsing this context out on an inbound request/response.
                    default:
                        throw new IllegalArgumentException("Unsupported context ID: " + contextId.toString());
                }
                wolaMessageContexts.add(curContext);

                // Move to the next context in the buffer.
                contextOffset += fullContextLen;
            }
        }

        return wolaMessageContexts;
    }

    /**
     * @param contextId - The ID of the context to get
     *
     * @return the WolaMessageContext for the given ID.
     *
     * @throws WolaMessageParseException if the context could not be parsed
     */
    protected WolaMessageContext getWolaMessageContext(WolaMessageContextId contextId) throws WolaMessageParseException {

        for (WolaMessageContext context : getWolaMessageContexts()) {
            if (context.getContextId() == contextId) {
                return context;
            }
        }

        return null;
    }

    /**
     * Add the given WolaMessageContext to the area.
     *
     * Note: the WolaMessageContext should be fully built before adding to the area.
     * Changes made to WolaMessageContext *after* adding will NOT be reflected in the
     * WolaMessageContextArea.
     *
     * @param wolaMessageContext - must not be null
     *
     * @return this
     */
    public WolaMessageContextArea addWolaMessageContext(WolaMessageContext wolaMessageContext) throws WolaMessageParseException {
        wolaMessageContexts.add(wolaMessageContext);
        listInSyncWithRawData = false;

        return this;
    }

}
