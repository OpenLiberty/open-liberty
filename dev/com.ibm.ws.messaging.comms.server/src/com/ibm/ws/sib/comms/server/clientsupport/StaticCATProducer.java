/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.comms.server.clientsupport;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.comms.common.CommsByteBufferPool;
import com.ibm.ws.sib.comms.common.CommsUtils;
import com.ibm.ws.sib.comms.server.CommsServerByteBuffer;
import com.ibm.ws.sib.comms.server.ConversationState;
import com.ibm.ws.sib.comms.server.ConversationStateFullException;
import com.ibm.ws.sib.comms.server.IdToTransactionTable;
import com.ibm.ws.sib.comms.server.ServerLinkLevelState;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.Conversation.ThrottlingPolicy;
import com.ibm.ws.sib.jfapchannel.HandshakeProperties;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.mfp.impl.JsMessageFactory;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.ProducerSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;

/**
 * This class takes responsibility for dealing with all FAP flows
 * relating to message producers.
 */
public class StaticCATProducer
{
    /** Class name for FFDC's */
    private static String CLASS_NAME = StaticCATProducer.class.getName();

    /** Referecence to our pool manager */
    private static CommsByteBufferPool poolManager = CommsByteBufferPool.getInstance();

    /** Registers our trace component */
    private static final TraceComponent tc =
                    SibTr.register(StaticCATProducer.class, CommsConstants.MSG_GROUP,
                                   CommsConstants.MSG_BUNDLE);

    private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);

    /** Log class info on load */
    static
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc,
                        "Source info: @(#)SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/clientsupport/StaticCATProducer.java, SIB.comms, WASX.SIB, aa1225.01 1.78");
    }

    /**
     * Create a Synchronous Producer Session, assign the object to the conversation
     * state, and return the ID to client.
     * 
     * Fields:
     * 
     * BIT16 SIConnectionObjectId
     * BIT16 MessageOrderContextId
     * BIT16 DestinationType
     * BIT16 ProducerFlags
     * 
     * BIT16 Uuid Length
     * BYTE[] Uuid
     * BIT16 DestinationNameLength
     * BYTE[] DestinationName
     * 
     * BIT16 DiscriminatorLength
     * BYTE[] Discriminator
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param allocatedFromBufferPool
     * @param partOfExchange
     */
    static void rcvCreateProducerSess(CommsByteBuffer request, Conversation conversation,
                                      int requestNumber, boolean allocatedFromBufferPool,
                                      boolean partOfExchange)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "rcvCreateProducerSess",
                        new Object[]
                        {
                         request,
                         conversation,
                         "" + requestNumber,
                         "" + allocatedFromBufferPool,
                         "" + partOfExchange
                        });

        ConversationState convState = (ConversationState) conversation.getAttachment();

        short connectionObjectID = request.getShort(); // BIT16 ConnectionObjectId
        short orderId = request.getShort(); // BIT32 MessageOrderId
        short destinationTypeShort = request.getShort(); // BIT16 DestinationType

        boolean bindToQueuePoint = false; // Default value                                             SIB0113.comms.1 start
        boolean preferLocalQueuePoint = true; // Default value
        final HandshakeProperties handshakeProps = conversation.getHandshakeProperties();
        if (handshakeProps.getFapLevel() >= JFapChannelConstants.FAP_VERSION_9) {
            final short producerFlags = request.getShort();

            if (producerFlags > CommsConstants.PF_MAX_VALID)
            {
                // The flags appear to be invalid
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Producer flags (" + producerFlags + ") > " + CommsConstants.PF_MAX_VALID);

                SIErrorException e = new SIErrorException(nls.getFormattedMessage("INVALID_PROP_SICO8018", new Object[] { "" + producerFlags }, null));

                FFDCFilter.processException(e, CLASS_NAME + ".rcvCreateProducerSess", CommsConstants.STATICCATPRODUCER_CREATE_03);

                throw e;
            }

            bindToQueuePoint = (producerFlags & CommsConstants.PF_BIND_TO_QUEUE_POINT) != 0;
            preferLocalQueuePoint = (producerFlags & CommsConstants.PF_PREFER_LOCAL_QUEUE_POINT) != 0;
        } //SIB0113.comms.1 end

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            SibTr.debug(tc, "ConnectionObjectID", connectionObjectID);
            SibTr.debug(tc, "MessageOrderId", orderId);
            SibTr.debug(tc, "DestinationType", destinationTypeShort);
            SibTr.debug(tc, "BindToQueuePoint", bindToQueuePoint); //SIB0113.comms.1
            SibTr.debug(tc, "PreferLocalQueuePoint", preferLocalQueuePoint); //SIB0113.comms.1
        }

        /**************************************************************/
        /* Destination Type */
        /**************************************************************/
        DestinationType destType = null;
        if (destinationTypeShort != CommsConstants.NO_DEST_TYPE)
        {
            destType = DestinationType.getDestinationType(destinationTypeShort);
        }

        try
        {
            /**************************************************************/
            /* Destination information */
            /**************************************************************/
            SIDestinationAddress destAddress = request.getSIDestinationAddress(conversation.getHandshakeProperties().getFapLevel());

            /**************************************************************/
            /* Discriminator */
            /**************************************************************/
            String discriminator = request.getString();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Discriminator:", discriminator);

            /**************************************************************/
            /* Alternate User Id */
            /**************************************************************/
            String alternateUser = request.getString();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Alternate User Id:", alternateUser);

            SICoreConnection connection =
                            ((CATConnection) convState.getObject(connectionObjectID)).getSICoreConnection();

            OrderingContext siOrder = null;
            if (orderId != CommsConstants.NO_ORDER_CONTEXT)
            {
                siOrder = ((CATOrderingContext) convState.getObject(orderId)).getOrderingContext();
            }

            ProducerSession prodSession = connection.createProducerSession(destAddress,
                                                                           discriminator,
                                                                           destType,
                                                                           siOrder,
                                                                           alternateUser,
                                                                           bindToQueuePoint, //SIB0113.comms.1
                                                                           preferLocalQueuePoint); //SIB0113.comms.1

            short prodSessionObjectID = (short) convState.addObject(prodSession);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Producer Session Id:", prodSessionObjectID);

            StaticCATHelper.sendSessionCreateResponse(JFapChannelConstants.SEG_CREATE_PRODUCER_SESS_R,
                                                      requestNumber,
                                                      conversation,
                                                      prodSessionObjectID,
                                                      prodSession,
                                                      destAddress);
        } catch (SINotAuthorizedException e)
        {
            // No FFDC Code Needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  null,
                                                  conversation, requestNumber);
        } catch (SIException e)
        {
            //No FFDC code needed - processor will have already FFDC'ed any interesting ones....
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.STATICCATPRODUCER_CREATE_01,
                                                  conversation, requestNumber);

        } catch (ConversationStateFullException e)
        {
            SILimitExceededException ex = new SILimitExceededException(nls.getFormattedMessage("MAX_SESSIONS_REACHED_SICO1019", new Object[] { "" + Short.MAX_VALUE }, null));

            FFDCFilter.processException(ex,
                                        CLASS_NAME + ".rcvCreateProducerSess",
                                        CommsConstants.STATICCATPRODUCER_CREATE_02);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.STATICCATPRODUCER_CREATE_02,
                                                  conversation, requestNumber);

        }

        request.release(allocatedFromBufferPool);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "rcvCreateProducerSess");
    }

    /**
     * Close the Synchronous Producer Session provided by the client.
     * 
     * Fields:
     * 
     * BIT16 ConnectionObjectId
     * BIT16 ProducerSessionId
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param allocatedFromBufferPool
     * @param partOfExchange
     */
    static void rcvCloseProducerSess(CommsByteBuffer request, Conversation conversation,
                                     int requestNumber, boolean allocatedFromBufferPool,
                                     boolean partOfExchange)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "rcvCloseProducerSess",
                        new Object[]
                        {
                         request,
                         conversation,
                         "" + requestNumber,
                         "" + allocatedFromBufferPool,
                         "" + partOfExchange
                        });
        ConversationState convState = (ConversationState) conversation.getAttachment();

        short connectionObjectID = request.getShort(); // BIT16 ConnectionObjectId
        short producerObjectID = request.getShort(); // BIT16 SyncProducerSessionId

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            SibTr.debug(tc, "connectionObjectID", connectionObjectID);
            SibTr.debug(tc, "producerObjectID", producerObjectID);
        }

        ProducerSession producerSession = ((ProducerSession) convState.getObject(producerObjectID));

        try
        {
            producerSession.close();
            convState.removeObject(producerObjectID);

            try
            {
                conversation.send(poolManager.allocate(),
                                  JFapChannelConstants.SEG_CLOSE_PRODUCER_SESS_R,
                                  requestNumber,
                                  JFapChannelConstants.PRIORITY_MEDIUM,
                                  true,
                                  ThrottlingPolicy.BLOCK_THREAD,
                                  null);
            } catch (SIException e)
            {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".rcvCloseProducerSess",
                                            CommsConstants.STATICCATPRODUCER_CLOSE_01);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, e.getMessage(), e);

                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2024", e);
            }
        } catch (SIException e)
        {
            //No FFDC code needed - processor will have already FFDC'ed any interesting ones....
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.STATICCATPRODUCER_CLOSE_02, // d186970
                                                  conversation, requestNumber); // f172297

        }

        request.release(allocatedFromBufferPool);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "rcvCloseProducerSess");
    }

    /**
     * Calls the internal send method that will send a message and inform the
     * client as to the outcome.
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param allocatedFromBufferPool
     * @param partOfExchange
     */
    static void rcvSendSessMsg(CommsServerByteBuffer request,
                               Conversation conversation,
                               int requestNumber,
                               boolean allocatedFromBufferPool,
                               boolean partOfExchange)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "rcvSendSessMsg");

        final boolean optimizedTx = CommsUtils.requiresOptimizedTransaction(conversation);
        sendSessMsg(request, conversation, requestNumber, partOfExchange,
                    allocatedFromBufferPool, true, optimizedTx);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "rcvSendSessMsg");
    }

    /**
     * Calls the internal send method that will send a message. However, no reply
     * will be sent to the client.
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param allocatedFromBufferPool
     * @param partOfExchange
     */
    static void rcvSendSessMsgNoReply(CommsServerByteBuffer request,
                                      Conversation conversation,
                                      int requestNumber,
                                      boolean allocatedFromBufferPool,
                                      boolean partOfExchange)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "rcvSendSessMsgNoReply");

        final boolean optimizedTx = CommsUtils.requiresOptimizedTransaction(conversation);
        sendSessMsg(request,
                    conversation,
                    requestNumber,
                    partOfExchange,
                    allocatedFromBufferPool,
                    false,
                    optimizedTx);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "rcvSendSessMsgNoReply");
    }

    /**
     * Send a message using the Synchronous Producer Session provided by the client.
     * 
     * Fields:
     * 
     * BIT16 ConnectionObjectId
     * BIT16 ProducerSessionId
     * BIT32 TransactonId
     * BIT32 JMOLength
     * BYTE[] JMO Serialised JMO object
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param partOfExchange
     * @param allocatedFromBufferPool
     * @param sendReply
     */
    private static void sendSessMsg(CommsServerByteBuffer request,
                                    Conversation conversation,
                                    int requestNumber,
                                    boolean partOfExchange,
                                    boolean allocatedFromBufferPool,
                                    boolean sendReply,
                                    boolean txOptimized)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "sendSessMsg",
                        new Object[]
                        {
                         request,
                         conversation,
                         "" + requestNumber,
                         "" + partOfExchange,
                         "" + allocatedFromBufferPool,
                         "" + sendReply,
                         "" + txOptimized,
                        });

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            if (sendReply)
                SibTr.debug(tc, "The client will be informed of the outcome");
            else
                SibTr.debug(tc, "The client will NOT be informed of the outcome");
        }

        ConversationState convState = (ConversationState) conversation.getAttachment();
        ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();

        short connectionObjectId = request.getShort(); // BIT16 ConnectionObjectId
        short producerObjectId = request.getShort(); // BIT16 SyncProducerSessionId

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            SibTr.debug(tc, "connectionObjectId", connectionObjectId);
            SibTr.debug(tc, "producerObjectId", producerObjectId);
        }

        // Get the transaction
        int transactionId = request.getSITransactionId(connectionObjectId, linkState, txOptimized);
        SITransaction siTran = linkState.getTransactionTable().get(transactionId);

        try
        {
            try
            {
                // Now get the message
                int messageLength = (int) request.peekLong();
                SIBusMessage sibMessage = request.getMessage(null);

                ProducerSession producer =
                                ((ProducerSession) convState.getObject(producerObjectId));

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                {
                    SibTr.debug(tc, "Sending client message - " + sibMessage);
                    SibTr.debug(tc, "Destination: " + producer.getDestinationAddress());
                    SibTr.debug(tc, "Discriminator: " + sibMessage.getDiscriminator());
                    SibTr.debug(tc, "Reliability: " + sibMessage.getReliability());
                }

                if (siTran != IdToTransactionTable.INVALID_TRANSACTION)
                {
                    producer.send(sibMessage, siTran);
                }

                if (sendReply)
                {
                    try
                    {
                        conversation.send(poolManager.allocate(),
                                          JFapChannelConstants.SEG_SEND_SESS_MSG_R,
                                          requestNumber,
                                          JFapChannelConstants.PRIORITY_MEDIUM,
                                          true,
                                          ThrottlingPolicy.BLOCK_THREAD,
                                          null);
                    } catch (SIException e)
                    {
                        FFDCFilter.processException(e,
                                                    CLASS_NAME + ".sendSessMsg",
                                                    CommsConstants.STATICCATPRODUCER_SEND_01);

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, e.getMessage(), e);

                        SibTr.error(tc, "COMMUNICATION_ERROR_SICO2024", e);
                    }
                }
            } catch (Exception e)
            {
                // No FFDC code needed
                // (actually we FFDC later on - but we need that line to fool the FFDC tool :-)

                // If we are sending replies, rethrow the exception so
                // that the specific exeption handlers will deal with it
                // and send the response to the client
                if (sendReply)
                    throw e;

                // Otherwise, mark this transaction as error if we are not sending a reply
                if (transactionId != CommsConstants.NO_TRANSACTION)
                    linkState.getTransactionTable().markAsRollbackOnly(transactionId, e);

                //Only FFDC if we haven't received a meTerminated event OR if this Exception isn't a SIException.
                if (!(e instanceof SIException) || !convState.hasMETerminated())
                {
                    FFDCFilter.processException(e,
                                                CLASS_NAME + ".sendSessMsg",
                                                CommsConstants.STATICCATPRODUCER_SEND_02);
                }

                SibTr.error(tc, SibTr.Suppressor.ALL_FOR_A_WHILE_SIMILAR_INSERTS,
                            "UNABLE_TO_SEND_MESSAGE_SICO2009", e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Failed to send a message", e);
            }
        } catch (SIException e)
        {
            //No FFDC code needed - processor will have already FFDC'ed any interesting ones....
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.STATICCATPRODUCER_SEND_03,
                                                  conversation, requestNumber);

        } catch (Exception e)
        {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".sendSessMsg",
                                        CommsConstants.STATICCATPRODUCER_SEND_04);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.STATICCATPRODUCER_SEND_04,
                                                  conversation, requestNumber);
        }

        request.release(allocatedFromBufferPool);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendSessMsg");
    }

    /**
     * Calls the internal send method that will send a message and inform the
     * client as to the outcome.
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param allocatedFromBufferPool
     * @param partOfExchange
     */
    static void rcvSendConnMsg(CommsServerByteBuffer request,
                               Conversation conversation,
                               int requestNumber,
                               boolean allocatedFromBufferPool,
                               boolean partOfExchange)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "rcvSendConnMsg");

        final boolean optimizedTx = CommsUtils.requiresOptimizedTransaction(conversation);

        sendConnMsg(request,
                    conversation,
                    requestNumber,
                    partOfExchange,
                    allocatedFromBufferPool,
                    true,
                    optimizedTx);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "rcvSendConnMsg");
    }

    /**
     * Calls the internal send method that will send a message. However, no reply
     * will be sent to the client.
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param allocatedFromBufferPool
     * @param partOfExchange
     */
    static void rcvSendConnMsgNoReply(CommsServerByteBuffer request,
                                      Conversation conversation,
                                      int requestNumber,
                                      boolean allocatedFromBufferPool,
                                      boolean partOfExchange)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "rcvSendConnMsgNoReply");

        final boolean optimizedTx = CommsUtils.requiresOptimizedTransaction(conversation);

        sendConnMsg(request,
                    conversation,
                    requestNumber,
                    partOfExchange,
                    allocatedFromBufferPool,
                    false,
                    optimizedTx);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "rcvSendConnMsgNoReply");
    }

    /**
     * Send a message using the SICoreConnection provided by the client.
     * 
     * Fields:
     * 
     * BIT16 ConnectionObjectId
     * BIT16 MessageOrderContextId
     * BIT32 TransactionId
     * BIT16 DestinationType
     * BIT16 ProducerFlags
     * 
     * BIT16 Uuid length
     * BYTE[] Uuid
     * BIT16 DestinationNameLength
     * BYTE[] DestinationName
     * 
     * BIT64 JMOLength
     * BYTE[] JMO Serialised JMO object
     * 
     * The last parameter will determine whether a reply is sent to the client or not.
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param partOfExchange
     * @param allocatedFromBufferPool
     * @param sendReply Determines whether the client receieves notification about
     *            whether the message was sent or not.
     * @param optimizedTx determines whether the transaction is "optimized" or not. Optimized
     *            transactions are created at the point they are first used - rather than requiring an
     *            explicit transaction creation flow.
     */
    private static void sendConnMsg(CommsServerByteBuffer request, Conversation conversation,
                                    int requestNumber, boolean partOfExchange,
                                    boolean allocatedFromBufferPool, boolean sendReply,
                                    boolean optimizedTx)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "sendConnMsg",
                        new Object[]
                        {
                         request,
                         conversation,
                         "" + requestNumber,
                         "" + partOfExchange,
                         "" + allocatedFromBufferPool,
                         "" + sendReply,
                         "" + optimizedTx
                        });

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            if (sendReply)
                SibTr.debug(tc, "The client will be informed of the outcome");
            else
                SibTr.debug(tc, "The client will NOT be informed of the outcome");
        }

        ConversationState convState = (ConversationState) conversation.getAttachment();
        ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();

        short connectionObjectId = request.getShort(); // BIT16 ConnectionObjectId
        short orderId = request.getShort(); // BIT16 MessageOrderContextId
        int transactionId = request.getSITransactionId(connectionObjectId, linkState, optimizedTx);
        short destinationTypeShort = request.getShort(); // BIT16 DestinationType
        String alternateUser = request.getString();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            SibTr.debug(tc, "connectionObjectId", connectionObjectId);
            SibTr.debug(tc, "orderId", orderId);
            SibTr.debug(tc, "transactionId", transactionId);
            SibTr.debug(tc, "destinationType", destinationTypeShort);
            SibTr.debug(tc, "alternateUser", alternateUser);
        }

        SICoreConnection connection =
                        ((CATConnection) convState.getObject(connectionObjectId)).getSICoreConnection();

        /**************************************************************/
        /* Destination type */
        /**************************************************************/
        DestinationType destType = null;
        if (destinationTypeShort != CommsConstants.NO_DEST_TYPE)
        {
            destType = DestinationType.getDestinationType(destinationTypeShort);
        }

        try
        {
            /**************************************************************/
            /* Destination address */
            /**************************************************************/
            SIDestinationAddress destAddress = request.getSIDestinationAddress(conversation.getHandshakeProperties().getFapLevel());

            int messageLength = (int) request.peekLong();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Message length", "" + messageLength);

            // Get the transaction
            SITransaction siTran = null;
            siTran = linkState.getTransactionTable().get(transactionId);

            OrderingContext siOrder = null;
            if (orderId != CommsConstants.NO_ORDER_CONTEXT)
            {
                siOrder = ((CATOrderingContext) convState.getObject(orderId)).getOrderingContext();
            }

            try
            {
                SIBusMessage sibMessage = request.getMessage(null);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                {
                    SibTr.debug(tc, "Sending client message - " + sibMessage);
                    SibTr.debug(tc, "Destination: " + destAddress);
                    SibTr.debug(tc, "Discriminator: " + sibMessage.getDiscriminator());
                    SibTr.debug(tc, "Reliability: " + sibMessage.getReliability());
                }

                if (siTran != IdToTransactionTable.INVALID_TRANSACTION)
                {
                    connection.send(sibMessage, siTran, destAddress, destType, siOrder, alternateUser);
                }

                if (sendReply)
                {
                    try
                    {
                        conversation.send(poolManager.allocate(),
                                          JFapChannelConstants.SEG_SEND_CONN_MSG_R,
                                          requestNumber,
                                          JFapChannelConstants.PRIORITY_MEDIUM,
                                          true,
                                          ThrottlingPolicy.BLOCK_THREAD,
                                          null);
                    } catch (SIException e)
                    {
                        FFDCFilter.processException(e,
                                                    CLASS_NAME + ".sendConnMsg",
                                                    CommsConstants.STATICCATPRODUCER_CONNSEND_01);

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, e.getMessage(), e);

                        SibTr.error(tc, "COMMUNICATION_ERROR_SICO2024", e);
                    }
                }
            } catch (Exception e)
            {
                // No FFDC code needed
                // (actually we FFDC later on - but we need that line to fool the FFDC tool :-)

                // If we are sending replies, rethrow the exception so
                // that the specific exeption handlers will deal with it
                // and send the response to the client
                if (sendReply)
                    throw e;

                // Otherwise, mark this transaction as error
                if (transactionId != CommsConstants.NO_TRANSACTION)
                {
                    linkState.getTransactionTable().markAsRollbackOnly(transactionId, e);
                }

                //No FFDC code needed
                //Only FFDC if we haven't received a meTerminated event OR if this Exception isn't a SIException.
                if (!(e instanceof SIException) || !convState.hasMETerminated())
                {
                    FFDCFilter.processException(e,
                                                CLASS_NAME + ".sendConnMsg",
                                                CommsConstants.STATICCATPRODUCER_CONNSEND_02);
                }

                SibTr.error(tc, SibTr.Suppressor.ALL_FOR_A_WHILE_SIMILAR_INSERTS,
                            "UNABLE_TO_SEND_MESSAGE_SICO2009", e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Failed to send a message", e);
            }
        } catch (SINotAuthorizedException e)
        {
            // No FFDC Code Needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  null,
                                                  conversation, requestNumber);
        } catch (SIException e)
        {
            //No FFDC code needed - processor will have already FFDC'ed any interesting ones....
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.STATICCATPRODUCER_CONNSEND_03,
                                                  conversation, requestNumber);
        } catch (Exception e)
        {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".sendConnMsg",
                                        CommsConstants.STATICCATPRODUCER_CONNSEND_04);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.STATICCATPRODUCER_CONNSEND_04,
                                                  conversation, requestNumber);

        }

        request.release(allocatedFromBufferPool);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendConnMsg");
    }

    /**
     * This method is invoked when we are processing the send of a chunked messsage is received by
     * the server. When the last chunk is received, the sendReply parameter is used to determine
     * whether a reply is expected to this overall send request.
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param allocatedFromBufferPool
     * @param sendReply
     */
    static void rcvSendChunkedSessMsg(CommsServerByteBuffer request,
                                      Conversation conversation,
                                      int requestNumber,
                                      boolean allocatedFromBufferPool,
                                      boolean sendReply)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "rcvSendSegmentedSessMsg",
                        new Object[] { request, conversation, requestNumber, allocatedFromBufferPool, sendReply });

        final boolean txOptimized = CommsUtils.requiresOptimizedTransaction(conversation);

        ConversationState convState = (ConversationState) conversation.getAttachment();
        ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();
        ChunkedMessageWrapper wrapper = null;

        short connectionObjectId = request.getShort(); // BIT16 ConnectionObjectId
        short producerObjectId = request.getShort(); // BIT16 SyncProducerSessionId
        // Get the transaction id
        int transactionId = request.getSITransactionId(connectionObjectId, linkState, txOptimized);
        // Get the flags for the message
        byte flags = request.get();
        boolean first = ((flags & CommsConstants.CHUNKED_MESSAGE_FIRST) == CommsConstants.CHUNKED_MESSAGE_FIRST);
        boolean last = ((flags & CommsConstants.CHUNKED_MESSAGE_LAST) == CommsConstants.CHUNKED_MESSAGE_LAST);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            SibTr.debug(tc, "connectionObjectId", connectionObjectId);
            SibTr.debug(tc, "producerObjectId", producerObjectId);
            SibTr.debug(tc, "transactionId", transactionId);
            SibTr.debug(tc, "flags", flags);
        }

        long wrapperId = getWrapperId(connectionObjectId, producerObjectId, transactionId);

        // If this is the first chunk of data, create a wrapper to save it in. Otherwise retrieve the
        // wrapper to append to.
        if (first)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "This is the first chunk of data");

            // Get the transaction and produced session now and stash it away for later
            SITransaction siTran = linkState.getTransactionTable().get(transactionId);
            ProducerSession producer = ((ProducerSession) convState.getObject(producerObjectId));

            // Create a wrapper for this data to stash in the conversation state
            wrapper = new ChunkedMessageWrapper(siTran, producer);
            convState.putChunkedMessageWrapper(wrapperId, wrapper);
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Appending to chunks already collected");
            wrapper = convState.getChunkedMessageWrapper(wrapperId);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Appending to wrapper: ", wrapper);
        }

        // If the wrapper is null at this point, this is messed up
        if (wrapper == null)
        {
            SIErrorException e = new SIErrorException(
                            TraceNLS.getFormattedMessage(CommsConstants.MSG_BUNDLE, "CHUNK_WRAPPER_NULL_SICO2165", null, null)
                            );
            FFDCFilter.processException(e, CLASS_NAME + ".rcvSendSegmentedSessMsg",
                                        CommsConstants.STATICCATPRODUCER_SENDCHUNKED_01,
                                        "" + wrapperId);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Chunked message wrapper is null!");
            throw e;
        }

        // Now get the chunk from the message and add it to the wrapper
        wrapper.addDataSlice(request.getDataSlice());

        // If this was the last slice we have received all the data we need and we must now send the
        // message into the bus and (possibly) send back a reply to the client.
        if (last)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                SibTr.debug(tc, "This is the last chunk - sending message");
                if (sendReply)
                    SibTr.debug(tc, "The client will be informed of the outcome");
                else
                    SibTr.debug(tc, "The client will NOT be informed of the outcome");
            }

            // Remove the chunks from the conversation state
            convState.removeChunkedMessageWrapper(wrapperId);

            try
            {
                // Recreate the message
                SIBusMessage sibMessage = JsMessageFactory.getInstance().createInboundJsMessage(wrapper.getMessageData());
                // And get all the info we need
                ProducerSession producer = wrapper.getProducerSession();
                SITransaction siTran = wrapper.getTransaction();

                try
                {

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    {
                        SibTr.debug(tc, "Sending client message - " + sibMessage);
                        SibTr.debug(tc, "Destination: " + producer.getDestinationAddress());
                        SibTr.debug(tc, "Discriminator: " + sibMessage.getDiscriminator());
                        SibTr.debug(tc, "Reliability: " + sibMessage.getReliability());
                    }

                    if (siTran != IdToTransactionTable.INVALID_TRANSACTION)
                    {
                        producer.send(sibMessage, siTran);
                    }

                    if (sendReply)
                    {
                        try
                        {
                            conversation.send(poolManager.allocate(),
                                              JFapChannelConstants.SEG_SEND_CHUNKED_SESS_MSG_R,
                                              requestNumber,
                                              JFapChannelConstants.PRIORITY_MEDIUM,
                                              true,
                                              ThrottlingPolicy.BLOCK_THREAD,
                                              null);
                        } catch (SIException e)
                        {
                            FFDCFilter.processException(e,
                                                        CLASS_NAME + ".rcvSendSegmentedSessMsg",
                                                        CommsConstants.STATICCATPRODUCER_SENDCHUNKED_02);

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(tc, e.getMessage(), e);

                            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2024", e);
                        }
                    }
                } catch (Exception e)
                {
                    // No FFDC code needed
                    // (actually we FFDC later on - but we need that line to fool the FFDC tool :-)

                    // If we are sending replies, rethrow the exception so
                    // that the specific exeption handlers will deal with it
                    // and send the response to the client
                    if (sendReply)
                        throw e;

                    // Otherwise, mark this transaction as error if we are not sending a reply
                    if (transactionId != CommsConstants.NO_TRANSACTION)
                        linkState.getTransactionTable().markAsRollbackOnly(transactionId, e);

                    //No FFDC code needed
                    //Only FFDC if we haven't received a meTerminated event OR if this Exception isn't a SIException.
                    if (!(e instanceof SIException) || !convState.hasMETerminated())
                    {
                        FFDCFilter.processException(e,
                                                    CLASS_NAME + ".rcvSendSegmentedSessMsg",
                                                    CommsConstants.STATICCATPRODUCER_SENDCHUNKED_03);
                    }

                    SibTr.error(tc, SibTr.Suppressor.ALL_FOR_A_WHILE_SIMILAR_INSERTS,
                                "UNABLE_TO_SEND_MESSAGE_SICO2009", e);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Failed to send a message", e);
                }
            } catch (SIException e)
            {
                //No FFDC code needed - processor will have already FFDC'ed any interesting ones....
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, e.getMessage(), e);

                StaticCATHelper.sendExceptionToClient(e,
                                                      CommsConstants.STATICCATPRODUCER_SENDCHUNKED_04,
                                                      conversation, requestNumber);

            } catch (Exception e)
            {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".rcvSendSegmentedSessMsg",
                                            CommsConstants.STATICCATPRODUCER_SENDCHUNKED_05);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, e.getMessage(), e);

                StaticCATHelper.sendExceptionToClient(e,
                                                      CommsConstants.STATICCATPRODUCER_SENDCHUNKED_05,
                                                      conversation, requestNumber);
            }
        }

        request.release(allocatedFromBufferPool);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "rcvSendSegmentedSessMsg");
    }

    /**
     * This method is invoked when we are processing the send of a chunked messsage is received by
     * the server. When the last chunk is received, the sendReply parameter is used to determine
     * whether a reply is expected to this overall send request.
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param allocatedFromBufferPool
     * @param sendReply
     */
    static void rcvSendChunkedConnMsg(CommsServerByteBuffer request,
                                      Conversation conversation,
                                      int requestNumber,
                                      boolean allocatedFromBufferPool,
                                      boolean sendReply)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "rcvSendSegmentedConnMsg",
                        new Object[] { request, conversation, requestNumber, allocatedFromBufferPool, sendReply });

        final boolean txOptimized = CommsUtils.requiresOptimizedTransaction(conversation);

        ConversationState convState = (ConversationState) conversation.getAttachment();
        ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();
        ChunkedMessageWrapper wrapper = null;

        short connectionObjectId = request.getShort(); // BIT16 ConnectionObjectId
        short orderId = request.getShort(); // BIT16 MessageOrderContextId
        // Get the transaction id
        int transactionId = request.getSITransactionId(connectionObjectId, linkState, txOptimized);
        // Get the flags for the message
        byte flags = request.get();
        boolean first = ((flags & CommsConstants.CHUNKED_MESSAGE_FIRST) == CommsConstants.CHUNKED_MESSAGE_FIRST);
        boolean last = ((flags & CommsConstants.CHUNKED_MESSAGE_LAST) == CommsConstants.CHUNKED_MESSAGE_LAST);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            SibTr.debug(tc, "connectionObjectId", connectionObjectId);
            SibTr.debug(tc, "orderId", orderId);
            SibTr.debug(tc, "transactionId", transactionId);
            SibTr.debug(tc, "flags", flags);
        }

        long wrapperId = getWrapperId(connectionObjectId, orderId, transactionId);

        // If this is the first chunk of data, create a wrapper to save it in. Otherwise retrieve the
        // wrapper to append to.
        if (first)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "This is the first chunk of data");

            short destinationTypeShort = request.getShort(); // BIT16 DestinationType
            String alternateUser = request.getString();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                SibTr.debug(tc, "destinationType", destinationTypeShort);
                SibTr.debug(tc, "alternateUser", alternateUser);
            }

            SICoreConnection connection =
                            ((CATConnection) convState.getObject(connectionObjectId)).getSICoreConnection();

            /**************************************************************/
            /* Destination type */
            /**************************************************************/
            DestinationType destType = null;
            if (destinationTypeShort != CommsConstants.NO_DEST_TYPE)
            {
                destType = DestinationType.getDestinationType(destinationTypeShort);
            }

            /**************************************************************/
            /* Destination address */
            /**************************************************************/
            SIDestinationAddress destAddress = request.getSIDestinationAddress(conversation.getHandshakeProperties().getFapLevel());

            int messageLength = (int) request.peekLong();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Message length", "" + messageLength);

            // Get the transaction
            SITransaction siTran = linkState.getTransactionTable().get(transactionId);

            OrderingContext siOrder = null;
            if (orderId != CommsConstants.NO_ORDER_CONTEXT)
            {
                siOrder = ((CATOrderingContext) convState.getObject(orderId)).getOrderingContext();
            }

            // Create a wrapper for this data to stash in the conversation state
            wrapper = new ChunkedMessageWrapper(siTran, connection, destType, destAddress, siOrder, alternateUser);
            convState.putChunkedMessageWrapper(wrapperId, wrapper);
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Appending to chunks already collected");
            wrapper = convState.getChunkedMessageWrapper(wrapperId);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Appending to wrapper: ", wrapper);
        }

        // If the wrapper is null at this point, this is messed up
        if (wrapper == null)
        {
            SIErrorException e = new SIErrorException(
                            TraceNLS.getFormattedMessage(CommsConstants.MSG_BUNDLE, "CHUNK_WRAPPER_NULL_SICO2165", null, null)
                            );
            FFDCFilter.processException(e, CLASS_NAME + ".rcvSendSegmentedSessMsg",
                                        CommsConstants.STATICCATPRODUCER_SENDCHUNKEDCONN_01,
                                        "" + wrapperId);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Chunked message wrapper is null!");
            throw e;
        }

        // Now get the chunk from the message and add it to the wrapper
        wrapper.addDataSlice(request.getDataSlice());

        // If this was the last slice we have received all the data we need and we must now send the
        // message into the bus and (possibly) send back a reply to the client.
        if (last)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                SibTr.debug(tc, "This is the last chunk - sending message");
                if (sendReply)
                    SibTr.debug(tc, "The client will be informed of the outcome");
                else
                    SibTr.debug(tc, "The client will NOT be informed of the outcome");
            }

            // Remove the chunks from the conversation state
            convState.removeChunkedMessageWrapper(wrapperId);

            try
            {
                // Recreate the message
                SIBusMessage sibMessage = JsMessageFactory.getInstance().createInboundJsMessage(wrapper.getMessageData());

                try
                {

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    {
                        SibTr.debug(tc, "Sending client message - " + sibMessage);
                        SibTr.debug(tc, "Destination: " + wrapper.getDestinationAddress());
                        SibTr.debug(tc, "Discriminator: " + sibMessage.getDiscriminator());
                        SibTr.debug(tc, "Reliability: " + sibMessage.getReliability());
                    }

                    if (wrapper.getTransaction() != IdToTransactionTable.INVALID_TRANSACTION)
                    {
                        wrapper.getConnection().send(sibMessage,
                                                     wrapper.getTransaction(),
                                                     wrapper.getDestinationAddress(),
                                                     wrapper.getDestinationType(),
                                                     wrapper.getOrderingContext(),
                                                     wrapper.getAlternateUser());
                    }

                    if (sendReply)
                    {
                        try
                        {
                            conversation.send(poolManager.allocate(),
                                              JFapChannelConstants.SEG_SEND_CHUNKED_CONN_MSG_R,
                                              requestNumber,
                                              JFapChannelConstants.PRIORITY_MEDIUM,
                                              true,
                                              ThrottlingPolicy.BLOCK_THREAD,
                                              null);
                        } catch (SIException e)
                        {
                            FFDCFilter.processException(e,
                                                        CLASS_NAME + ".rcvSendChunkedConnMsg",
                                                        CommsConstants.STATICCATPRODUCER_SENDCHUNKEDCONN_02);

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(tc, e.getMessage(), e);

                            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2024", e);
                        }
                    }
                } catch (Exception e)
                {
                    // No FFDC code needed
                    // (actually we FFDC later on - but we need that line to fool the FFDC tool :-)

                    // If we are sending replies, rethrow the exception so
                    // that the specific exeption handlers will deal with it
                    // and send the response to the client
                    if (sendReply)
                        throw e;

                    // Otherwise, mark this transaction as error if we are not sending a reply
                    if (transactionId != CommsConstants.NO_TRANSACTION)
                        linkState.getTransactionTable().markAsRollbackOnly(transactionId, e);

                    //No FFDC code needed
                    //Only FFDC if we haven't received a meTerminated event OR if this Exception isn't a SIException.
                    if (!(e instanceof SIException) || !convState.hasMETerminated())
                    {
                        FFDCFilter.processException(e,
                                                    CLASS_NAME + ".rcvSendChunkedConnMsg",
                                                    CommsConstants.STATICCATPRODUCER_SENDCHUNKEDCONN_03);
                    }

                    SibTr.error(tc, SibTr.Suppressor.ALL_FOR_A_WHILE_SIMILAR_INSERTS,
                                "UNABLE_TO_SEND_MESSAGE_SICO2009", e);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Failed to send a message", e);
                }
            } catch (SIException e)
            {
                //No FFDC code needed - processor will have already FFDC'ed any interesting ones....
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, e.getMessage(), e);

                StaticCATHelper.sendExceptionToClient(e,
                                                      CommsConstants.STATICCATPRODUCER_SENDCHUNKEDCONN_04,
                                                      conversation, requestNumber);

            } catch (Exception e)
            {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".rcvSendChunkedConnMsg",
                                            CommsConstants.STATICCATPRODUCER_SENDCHUNKEDCONN_05);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, e.getMessage(), e);

                StaticCATHelper.sendExceptionToClient(e,
                                                      CommsConstants.STATICCATPRODUCER_SENDCHUNKEDCONN_05,
                                                      conversation, requestNumber);
            }
        }

        request.release(allocatedFromBufferPool);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "rcvSendSegmentedConnMsg");
    }

    static long getWrapperId(short connectionId, short producerId, int transactionId)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getWrapperId", new Object[] { connectionId, producerId, transactionId });

        // Calculate the wrapper id. This is made up of the connection object Id (first 16 bytes),
        // the producer session Id (next 16 bytes) followed by the 32 byte transaction Id.
        long wrapperId = (((long) connectionId) << 48) +
                         (((long) producerId) << 32) +
                         transactionId;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getWrapperId", wrapperId);
        return wrapperId;
    }
}
