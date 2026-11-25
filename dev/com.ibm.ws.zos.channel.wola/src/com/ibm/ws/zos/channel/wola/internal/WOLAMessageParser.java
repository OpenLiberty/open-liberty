/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal;

import java.nio.ByteBuffer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.zos.channel.wola.internal.msg.WolaMessage;
import com.ibm.ws.zos.channel.wola.internal.msg.WolaMessageParseException;

/**
 * Parses WOLA messages.
 */
public class WOLAMessageParser {

    /**
     *
     */
    private static final TraceComponent tc = Tr.register(WOLAMessageParser.class);

    /**
     * The raw message data is contained in 1 or more ByteBuffers, which are all
     * encapsulated by the ByteBufferVector object.
     */
    private final ByteBufferVector byteBufferVector;

    /**
     * The leftovers after a parse.
     */
    private ByteBufferVector leftovers = null;

    /**
     * Indicates that a parseMessage() could not return a message. This is a clue to
     * getLeftovers() that it can just return the byte buffer vector as leftovers
     * without any further processing.
     */
    private boolean parseMessageFailed = false;

    /**
     * Indicates that a parseMessage() used up all of the byteBufferVector. There
     * are no leftovers. The WOLAMessage object has a reference to the byteBufferVector
     * so it should not be modified.
     */
    private boolean parseMessageUsedAllData = false;

    /**
     * Cached value of isHeaderComplete for this parser.
     */
    private boolean isHeaderComplete = false;

    /**
     * Cached value of isValidEyeCatcher for this parser.
     */
    private boolean isValidEyeCatcher = false;

    /**
     * CTOR.
     *
     * @param prevData - leftover data from a previous round of parsing (i.e. an incomplete message).
     * @param currData - message data
     */
    protected WOLAMessageParser(WOLAMessageLeftovers prevData, ByteBuffer currData) {
        if (prevData != null) {
            byteBufferVector = new ByteBufferVector(prevData.getByteBuffers(), currData.array());
            isHeaderComplete = prevData.isMessageHeaderComplete();
            isValidEyeCatcher = prevData.isEyeCatcherValid();
        } else {
            byteBufferVector = new ByteBufferVector(currData.array());
        }
    }

    /**
     * CTOR.
     *
     * @param byteBufferVector - raw message data
     */
    protected WOLAMessageParser(ByteBufferVector byteBufferVector) {
        this.byteBufferVector = byteBufferVector;
    }

    /**
     * Attempt to parse a message from the raw message data (provided on the CTOR).
     *
     * @return The WOLAMessage parsed from the raw message data, or null if the data
     *         did not contain a full WOLA message.
     */
    public WolaMessage parseMessage() throws WolaMessageParseException {

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "parseMessage", DoubleGutterServiceTracker.get().asDoubleGutter(1, byteBufferVector.toByteArray()));
        }

        if (!isMessageComplete()) {
            // Not enough here to parse a full WOLA message.
            parseMessageFailed = true;
            return null;
        }

        // Peel off the WOLA message.  We're optimizing here for the case where we read exactly one
        // message (there are no leftovers).  If it turns out that there are leftovers, we'll incur the
        // cost of one additional BBV.limit() call.  Remember, this code assumes that the buffer is
        // normalized (the first buffer starts with the WOLA message header, and the position is 0).
        WolaMessage retMe = null;
        int totalMessageSize = byteBufferVector.getInt(WolaMessage.TotalMessageSizeOffset);
        if (totalMessageSize == byteBufferVector.getLength()) {
            retMe = new WolaMessage(byteBufferVector);
            parseMessageUsedAllData = true;
        } else {
            // Split the byte buffer vector into the message and leftovers.  Create a message object, and
            // then replace the internal byte buffer vector with the leftovers.
            java.util.List<ByteBufferVector> messageAndLeftovers = byteBufferVector.split(totalMessageSize);
            retMe = new WolaMessage(messageAndLeftovers.get(0));
            leftovers = messageAndLeftovers.get(1);
        }

        return retMe;
    }

    /**
     *
     * Note: This method should be called AFTER WOLAMessageParser.parseMessage.
     *
     * @return "leftover" data - i.e. incomplete message data, or whatever wasn't parsed by parseMessage().
     */
    public WOLAMessageLeftovers getLeftovers() {
        // If unable to parse a message, the whole buffer array is left over and there's no need
        // to normalize and copy what's left.
        if (parseMessageFailed) {
            return new WOLAMessageLeftovers(byteBufferVector, isValidEyeCatcher, isHeaderComplete);
        }

        // If we used all the data, there are no leftovers.
        if (parseMessageUsedAllData) {
            return null;
        }

        // TODO: State machine, make sure leftovers is not null?
        return new WOLAMessageLeftovers(leftovers, false, false);
    }

    /**
     * Note: this method should be called *after* a call to hasCompleteHeader(), otherwise
     * it could raise an IndexOutOfBoundsException if the message data contains less
     * than 8 bytes (the eyecatcher field (a long)).
     *
     * @throws WolaMessageParseException if eyecatcher is not valid.
     */
    private boolean verifyEyeCatcher() throws WolaMessageParseException {
        if (isValidEyeCatcher == false) {
            if (byteBufferVector.getLong(WolaMessage.EyeCatcherOffset) != WolaMessage.BBOAMSG_EYE) {
                throw new WolaMessageParseException("Invalid Eye Catcher: " + Long.toHexString(byteBufferVector.getLong(WolaMessage.EyeCatcherOffset)));
            }
            isValidEyeCatcher = true;
        }
        return true;
    }

    /**
     * @return true if the message data contains a full WOLA message header; false otherwise.
     */
    protected boolean isHeaderComplete() {
        if (isHeaderComplete == false) {
            isHeaderComplete = (byteBufferVector.getLength() >= WolaMessage.HeaderSize);
        }

        return isHeaderComplete;
    }

    /**
     * @return true if the raw message data contains a full WOLA message; false otherwise.
     *
     * @throws WolaMessageParseException - if the WOLA message is malformed.
     */
    protected boolean isMessageComplete() throws WolaMessageParseException {
        return (isHeaderComplete() &&
                verifyEyeCatcher() && byteBufferVector.getLength() >= byteBufferVector.getInt(WolaMessage.TotalMessageSizeOffset));
    }

}
