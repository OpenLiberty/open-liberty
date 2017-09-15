/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server;

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.transaction.xa.XAException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.sib.comms.CommsConnection;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.comms.common.CommsLightTrace;
import com.ibm.ws.sib.comms.common.XidProxy;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.mfp.AbstractMessage;
import com.ibm.ws.sib.mfp.IncorrectMessageTypeException;
import com.ibm.ws.sib.mfp.MessageCopyFailedException;
import com.ibm.ws.sib.mfp.MessageEncodeFailedException;
import com.ibm.ws.sib.mfp.impl.ControlMessageFactory;
import com.ibm.ws.sib.mfp.impl.JsMessageFactory;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SIUncoordinatedTransaction;
import com.ibm.wsspi.sib.core.SIXAResource;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;

/**
 * This class is an extension of the base comms byte buffer that allows server only information to
 * be added into byte buffers. Information such as ME-ME messages and also the retrieval of
 * transaction information is all handled by this class.
 * 
 * @author Gareth Matthews
 */
public class CommsServerByteBuffer extends CommsByteBuffer {
    /** Class name for FFDC's */
    private static String CLASS_NAME = CommsServerByteBuffer.class.getName();

    /** Register Class with Trace Component */
    private static final TraceComponent tc = SibTr.register(CommsServerByteBuffer.class,
                                                            CommsConstants.MSG_GROUP,
                                                            CommsConstants.MSG_BUNDLE);

    public CommsServerByteBuffer(CommsServerByteBufferPool pool) {
        super(pool);
    }

    /**
     * Reads the data for a transaction.
     * 
     * @param connectionObjectId
     * @param linkState
     * @param txOptimized
     * 
     * @return Returns the identifier for the transaction in the link level state transaction table
     *         (or the value CommsUtils.NO_TRANSACTION if there was no transaction).
     */
    public synchronized int getSITransactionId(int connectionObjectId,
                                               ServerLinkLevelState linkState,
                                               boolean txOptimized) {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "getSITransactionId",
                                           new Object[] { "" + connectionObjectId, linkState, "" + txOptimized });

        final int transactionId;
        int transactionFlags = -1;

        if (txOptimized) {
            // Read the flags BIT32 from the data.
            transactionFlags = getInt();

            // Check transacted bit to verify whether this flow is transacted or not.
            if ((transactionFlags & CommsConstants.OPTIMIZED_TX_FLAGS_TRANSACTED_BIT) == 0) {
                transactionId = CommsConstants.NO_TRANSACTION;
            } else {
                int owningConvId = getInt();
                // Read the transaction identifier BIT32.
                transactionId = getInt();

                if (tc.isDebugEnabled()) {
                    SibTr.debug(this, tc, "Transaction Flags", "" + transactionFlags);
                    SibTr.debug(this, tc, "Owning Conversation Id", "" + owningConvId);
                    SibTr.debug(this, tc, "Transaction Id", "" + transactionId);
                }

                // Check the flags to determine if we need to create a new transaction or not.
                if ((transactionFlags & CommsConstants.OPTIMIZED_TX_FLAGS_CREATE_BIT) != 0) {
                    // Check the flags to determine if we are creating a local or global
                    // transaction.
                    if ((transactionFlags & CommsConstants.OPTIMIZED_TX_FLAGS_LOCAL_BIT) != 0) {
                        // local tran - determine if subordinates are allowed
                        final boolean allowSubordinates =
                                        (transactionFlags & CommsConstants.OPTIMIZED_TX_FLAGS_SUBORDINATES_ALLOWED) != 0;
                        try {
                            SIUncoordinatedTransaction uctran =
                                            linkState.getSICoreConnectionTable().get(owningConvId).createUncoordinatedTransaction(allowSubordinates);
                            linkState.getTransactionTable().addLocalTran(transactionId, owningConvId, uctran);
                        } catch (SIException e) {
                            // No FFDC Code Needed
                            if (tc.isEventEnabled())
                                SibTr.exception(tc, e);
                            linkState.getTransactionTable().addLocalTran(transactionId, owningConvId, IdToTransactionTable.INVALID_TRANSACTION);
                            linkState.getTransactionTable().markAsRollbackOnly(transactionId, e);
                        }
                    } else {
                        // global tran
                        SIXAResource xaRes = null;
                        XidProxy xidProxy = null;
                        try {
                            // Check the flags to determine if the XAResource needs end invoking on
                            // it before a new transaction can be started.
                            if ((transactionFlags & CommsConstants.OPTIMIZED_TX_END_PREVIOUS_BIT) != 0) {
                                int endFlags = getInt();
                                linkState.getTransactionTable().endOptimizedGlobalTransactionBranch(transactionId, endFlags);
                                xaRes = (SIXAResource) linkState.getTransactionTable().get(transactionId);
                            } else {
                                xaRes =
                                                linkState.getSICoreConnectionTable().get(owningConvId).getSIXAResource();

                            }

                            xidProxy = (XidProxy) getXid();

                            if (tc.isDebugEnabled())
                                SibTr.debug(tc, "xidProxy", xidProxy);

                            xaRes.start(xidProxy, SIXAResource.TMNOFLAGS);
                            linkState.getTransactionTable().addGlobalTransactionBranch(transactionId, owningConvId, xaRes, xidProxy, true);
                        } catch (SIException e) {
                            // No FFDC Code Needed
                            if (tc.isEventEnabled())
                                SibTr.exception(tc, e);
                            linkState.getTransactionTable().addGlobalTransactionBranch(transactionId, owningConvId, IdToTransactionTable.INVALID_TRANSACTION, xidProxy, true);
                            linkState.getTransactionTable().markAsRollbackOnly(transactionId, e);
                        } catch (XAException e) {
                            // No FFDC Code Needed
                            if (tc.isEventEnabled())
                                SibTr.exception(tc, e);
                            linkState.getTransactionTable().addGlobalTransactionBranch(transactionId, owningConvId, IdToTransactionTable.INVALID_TRANSACTION, xidProxy, true);
                            linkState.getTransactionTable().markAsRollbackOnly(transactionId, e);
                        }
                    }
                }
            }
        } else {
            // Not an optimized transaction.  Simply find its id.
            transactionId = getInt();
            if (tc.isDebugEnabled())
                SibTr.debug(tc, "transactionId", "" + transactionId);
        }

        if (TraceComponent.isAnyTracingEnabled()) {
            Object commsTx = null;
            if (transactionId != CommsConstants.NO_TRANSACTION)
                commsTx = linkState.getTransactionTable().get(transactionId, true);
            CommsLightTrace.traceTransaction(tc, "GetTxnTrace", commsTx, transactionId, transactionFlags);
        }

        if (tc.isEntryEnabled())
            SibTr.exit(tc, "getSITransactionId", "" + transactionId);
        return transactionId;
    }

    /**
     * This method will retrieve an ME-ME message from the buffer. A JsMessage or ControlMessage may
     * be returned from this method.
     * 
     * @return Returns an ME-ME message
     * @throws Exception if the message cannot be decoded
     */
    public synchronized AbstractMessage getMEMEMessage() throws Exception {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMEMEMessage");

        // GRRRRRRRRRRRR Read the FAP Clifford
        int messageLength = getInt();
        boolean isControlMessage = get() == CommsConstants.MEME_CONTROLMESSAGE;
        AbstractMessage message = null;

        if (tc.isDebugEnabled()) {
            SibTr.debug(tc, "Message Length", messageLength);
            SibTr.debug(tc, "Control Message", "" + isControlMessage);
        }

        if (isControlMessage) {
            ControlMessageFactory fac = ControlMessageFactory.getInstance();
            if (receivedBuffer.hasArray()) {
                message = fac.createInboundControlMessage(receivedBuffer.array(),
                                                          receivedBuffer.position() + receivedBuffer.arrayOffset(),
                                                          messageLength);
            } else {
                byte[] messageArray = get(messageLength);

                message = fac.createInboundControlMessage(messageArray,
                                                          0,
                                                          messageLength);
            }
        } else {
            JsMessageFactory fac = JsMessageFactory.getInstance();
            if (receivedBuffer.hasArray()) {
                message = fac.createInboundJsMessage(receivedBuffer.array(),
                                                     receivedBuffer.position() + receivedBuffer.arrayOffset(),
                                                     messageLength);
            } else {
                byte[] messageArray = get(messageLength);

                message = fac.createInboundJsMessage(messageArray,
                                                     0,
                                                     messageLength);
            }
        }

        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "getMEMEMessage", message);
        return message;
    }

    /**
     * Puts a message into the byte buffer using the <code>encodeFast()</code method of encoding
     * messages. This method takes account of any <i>capabilities</i> negotiated at the point the
     * connection was established. This method should only be used for ME-ME JsMessage's.
     * 
     * @param memeMessage The message to encode.
     * @param conversation The conversation over which the encoded message data is to be transferred.
     * @return Returns the message length.
     * 
     * @exception MessageCopyFailedException
     * @exception IncorrectMessageTypeException
     * @exception MessageEncodeFailedException
     * @exception UnsupportedEncodingException
     * @exception SIConnectionDroppedException
     */
    public synchronized int putMEMEMessage(AbstractMessage memeMessage, Conversation conversation)
                    throws SIConnectionDroppedException,
                    UnsupportedEncodingException,
                    IncorrectMessageTypeException,
                    MessageCopyFailedException,
                    MessageEncodeFailedException {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "putMEMEMessage",
                                           new Object[] { memeMessage, conversation });

        List<DataSlice> messageParts = null;
        CommsConnection commsConnection = ((ConversationState) conversation.getAttachment()).getCommsConnection();

        messageParts = encodeFast(memeMessage, commsConnection, conversation);
        int messageLength = putMEMEMessageWithoutEncode(memeMessage, messageParts);

        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "putMEMEMessage", messageLength);
        return messageLength;
    }

    /**
     * This method puts a message into the buffer using the already encoded messageParts parameter.
     * This is for the case where the encoding has already been performed. The memeMessage parameter
     * is used to ensure the correct message type is worked out.
     * 
     * @param memeMessage
     * @param messageParts
     * 
     * @return Returns the overall length of the message.
     */
    public synchronized int putMEMEMessageWithoutEncode(AbstractMessage memeMessage,
                                                        List<DataSlice> messageParts) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "putMEMEMessageWithoutEncode",
                                           new Object[] { memeMessage, messageParts });

        int messageLength = 0;
        // Now we have a list of MessagePart objects. First work out the overall length.
        for (int x = 0; x < messageParts.size(); x++) {
            messageLength += messageParts.get(x).getLength();
        }

        if (tc.isDebugEnabled())
            SibTr.debug(this, tc, "Message is " + messageLength + "byte(s) in length");

        // Write the length
        // GRRRRRRRRRRRRRRRR Read the FAP Clifford
        putInt(messageLength);
        // Write the control flag
        if (memeMessage.isControlMessage())
            put(CommsConstants.MEME_CONTROLMESSAGE); // Control message flag
        else
            put(CommsConstants.MEME_JSMESSAGE); // Message data flag

        // Now take the message parts and wrap them into byte buffers using the offset's supplied
        for (int x = 0; x < messageParts.size(); x++) {
            DataSlice messPart = messageParts.get(x);
            if (tc.isDebugEnabled())
                SibTr.debug(tc, "DataSlice[" + x + "]: " +
                                                  "Array: " + messPart.getBytes() + ", " +
                                                  "Offset: " + messPart.getOffset() + ", " +
                                                  "Length: " + messPart.getLength());

            wrap(messPart.getBytes(), messPart.getOffset(), messPart.getLength());
        }

        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "putMEMEMessageWithoutEncode", messageLength);
        return messageLength;
    }
}
