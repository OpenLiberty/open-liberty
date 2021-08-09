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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.serialization.DeserializationObjectInputStream;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.client.ConsumerMonitorListenerCache;
import com.ibm.ws.sib.comms.client.DestinationListenerCache;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.comms.common.CommsByteBufferPool;
import com.ibm.ws.sib.comms.common.CommsUtils;
import com.ibm.ws.sib.comms.server.CommsServerByteBuffer;
import com.ibm.ws.sib.comms.server.ConversationState;
import com.ibm.ws.sib.comms.server.ConversationStateFullException;
import com.ibm.ws.sib.comms.server.ServerLinkLevelState;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.Conversation.ThrottlingPolicy;
import com.ibm.ws.sib.jfapchannel.HandshakeProperties;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.ConsumerSetChangeCallback;
import com.ibm.wsspi.sib.core.DestinationAvailability;
import com.ibm.wsspi.sib.core.DestinationListener;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.exception.SICommandInvocationFailedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;

/**
 * This class contains static methods that handle calls from a client that relate specifically to an SICoreConnection instance
 */
public class StaticCATConnection {
    private static final TraceComponent tc = SibTr.register(StaticCATConnection.class, CommsConstants.MSG_GROUP, CommsConstants.MSG_BUNDLE);

    //@start_class_string_prolog@
    public static final String $sccsid = "@(#) 1.81 SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/clientsupport/StaticCATConnection.java, SIB.comms, WASX.SIB, aa1225.01 11/09/16 06:56:53 [7/2/12 05:59:00]";
    //@end_class_string_prolog@

    static {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Source Info: " + $sccsid);
    }

    /** Class name for FFDC's */
    private static String CLASS_NAME = StaticCATConnection.class.getName();

    /** Reference to our buffer pool manager */
    private static CommsByteBufferPool poolManager = CommsByteBufferPool.getInstance();

    /**
     * <p>Receive a message using the SICoreConnection provided by the client.
     * 
     * <p>The flow should take the following format:
     * 
     * <ul>
     * <li>BIT16 - ConnectionObjectId
     * <li>BIT32 - Transaction ID
     * <li>BIT32 - Reliability
     * <li>BIT64 - TimeOut
     * <li>BIT16 - DestinationType
     * <li>BIT16 - Unrecoverable reliability
     * <li>BIT16 - DestinationNameLength
     * <li>BYTE[] - DestinationName
     * <li>BIT16 - DiscriminatorLength
     * <li>BYTE[] - Discriminator
     * <li>BIT16 - Selector Length
     * <li>BYTE[] - Selector
     * <li>BIT16 - Mediation Length
     * <li>BYTE[] - Mediation
     * </ul>
     * 
     * <p>Note that because this is a call directly on the connection
     * no CATMainConsumer has been created and so this cannot be dealt
     * with in the normal way. To save duplicating code therefore, this
     * method will create a ConsumerSession as if the client had done so,
     * cache it incase of repeated requests and then call the CATConsumer's
     * receive method.
     * 
     * @param request The data from the client
     * @param conversation The conversation to use
     * @param requestNumber The request number of this exchange.
     * @param allocatedFromBufferPool Whether to release the byte buffer when we are finished.
     * @param partOfExchange Whether the client is expecting a reply to this call.
     */
    static void rcvReceiveConnMsg(CommsServerByteBuffer request,
                                  Conversation conversation,
                                  int requestNumber,
                                  boolean allocatedFromBufferPool,
                                  boolean partOfExchange) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "rcvReceiveConnMsg",
                        new Object[]
                        {
                         request,
                         conversation,
                         "" + requestNumber,
                         "" + allocatedFromBufferPool,
                         "" + partOfExchange
                        });

        ConversationState convState = (ConversationState) conversation.getAttachment();
        ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();

        final boolean optimizedTx = CommsUtils.requiresOptimizedTransaction(conversation);

        short connectionObjectId = request.getShort(); // BIT16 ConnectionObjectId
        int transactionId = request.getSITransactionId(connectionObjectId, linkState, optimizedTx);
        short reliabilityShort = request.getShort(); // BIT32 Reliability
        long timeout = request.getLong(); // BIT64 Timeout
        short destinationTypeShort = request.getShort();// BIT32 Destination type
        short unrecovShort = request.getShort(); // BIT16 Unrecoverable reliability

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            SibTr.debug(tc, "consumerObjectId", Short.valueOf(connectionObjectId));
            SibTr.debug(tc, "transactionId", Integer.valueOf(transactionId));
            SibTr.debug(tc, "reliability", Short.valueOf(reliabilityShort));
            SibTr.debug(tc, "timeout", Long.valueOf(timeout));
            SibTr.debug(tc, "destinationType", Short.valueOf(destinationTypeShort));
            SibTr.debug(tc, "Unrecov Reliability", Short.valueOf(unrecovShort));
        }

        /**************************************************************/
        /* Reliability */
        /**************************************************************/
        Reliability reliability = null;
        if (reliabilityShort != -1) {
            reliability = Reliability.getReliability(reliabilityShort);
        }

        /**************************************************************/
        /* Destination type */
        /**************************************************************/
        DestinationType destType = null;
        if (destinationTypeShort != CommsConstants.NO_DEST_TYPE) {
            destType = DestinationType.getDestinationType(destinationTypeShort);
        }

        /**************************************************************/
        /* Unrecoverable reliability */
        /**************************************************************/
        // Note this will never be null
        Reliability unrecoverableReliability = Reliability.getReliability(unrecovShort);

        SIDestinationAddress destAddr = request.getSIDestinationAddress(conversation.getHandshakeProperties().getFapLevel());
        SelectionCriteria criteria = request.getSelectionCriteria();
        String alternateUser = request.getString();

        // Get the connection
        SICoreConnection connection =
                        ((CATConnection) convState.getObject(connectionObjectId)).getSICoreConnection();

        // Have we already got a session for this destination?
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Cached destination is: " + convState.getCachedConsumerProps());

        CachedSessionProperties newProps = new CachedSessionProperties(destAddr,
                        criteria,
                        reliability);

        CATMainConsumer consumer = convState.getCachedConsumer();

        // We need to create the session if no consumer exists or if the properties passed in
        // are different to what we had before
        if (convState.getCachedConsumer() == null || (!newProps.equals(convState.getCachedConsumerProps()))) {
            try {
                // If not, close the old one, and create a new one.
                if (consumer != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Cached consumer was not null. Closing old session");
                    consumer.getConsumerSession().close();
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Creating new session");

                ConsumerSession consumerSession =
                                connection.createConsumerSession(destAddr,
                                                                 destType,
                                                                 criteria,
                                                                 reliability,
                                                                 false,
                                                                 false, // No local
                                                                 unrecoverableReliability,
                                                                 false,
                                                                 alternateUser);

                consumer =
                                new CATMainConsumer(conversation,
                                                (short) 0,
                                                consumerSession,
                                                false,
                                                false,
                                                unrecoverableReliability);

                consumer.setUsingConnectionReceive(true);
                // Make sure that the session is started - otherwise we get no messages
                consumer.start(requestNumber, false, false, null);
            } catch (SINotAuthorizedException e) {
                // No FFDC Code Needed
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, e.getMessage(), e);

                StaticCATHelper.sendExceptionToClient(e,
                                                      null,
                                                      conversation, requestNumber);
            } catch (SIException e) {
                //No FFDC code needed
                //Only FFDC if we haven't received a meTerminated event.
                if (!convState.hasMETerminated()) {
                    FFDCFilter.processException(e,
                                                CLASS_NAME + ".rcvReceiveConnMsg",
                                                CommsConstants.STATICCATCONNECTION_RCVCONNMSG_01);
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, e.getMessage(), e);

                StaticCATHelper.sendExceptionToClient(e,
                                                      CommsConstants.STATICCATCONNECTION_RCVCONNMSG_01,
                                                      conversation, requestNumber);
            }

            // Store the new desintation name and consumer
            convState.setCachedConsumerProps(newProps);
            convState.setCachedConsumer(consumer);
        }

        // Pass off the receive to the session
        consumer.receive(requestNumber, transactionId, timeout);

        request.release(allocatedFromBufferPool);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "rcvReceiveConnMsg");
    }

    /**
     * Retrieve a cloned SICoreConnection based on the SICoreConnection ID passed from the client.
     * Return the new connection object ID to the client so it can create a cloned proxy.
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param allocatedFromBufferPool
     * @param partOfExchange
     */
    static void rcvCloneConnection(CommsByteBuffer request, Conversation conversation, int requestNumber,
                                   boolean allocatedFromBufferPool, boolean partOfExchange) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "rcvCloneConnection",
                        new Object[]
                        {
                         request,
                         conversation,
                         "" + requestNumber,
                         "" + allocatedFromBufferPool
                        });

        final ConversationState convState = (ConversationState) conversation.getAttachment();

        // So here is the deal:
        // This callback has been driven on the brand new created Conversation. Therefore we have
        // no way to get back to the old Conversation, get the SICoreConnection and call clone() on
        // it. However, the flow passed up the ConversationId of the original conversation. We can
        // use that to get the SICoreConnection as this is saved in the Link state. It is also worth
        // noting that cloned connections will always appear on the same physical link - hence we can
        // do this.
        // Once we have the SICoreConnection, we call clone() on it and save that in the Link table.
        // Then we should create a new ServerSideConnection for the connection to use and add the
        // connection listener on the new connection.
        // Then we save the connection in the Conversation state and send the reply.
        // Simple as that.
        //
        // From JFAP 9 (WAS 7) upwards we include in the reply a Unique Id (cf SEG_GET_UNIQUE_ID) and
        // an Order Context (cf SEG_CREATE_ORDER_CONTEXT). These values are cached in the client and
        // used to service subsequent client requests for a Unique Id & Order Context - this saves
        // two line turn arounds so improves the JMS createConnection/create Sessions costs for JMS
        // clients running inside an application server.

        final short conversationId = request.getShort();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "conversationId", Short.valueOf(conversationId));
        // Get the original connection
        final SICoreConnection parent = ((ServerLinkLevelState) conversation.getLinkLevelAttachment()).getSICoreConnectionTable().get(conversationId);

        try {
            final CommsByteBuffer reply = poolManager.allocate();

            _cloneConnection(conversation, parent, reply);

            try {
                conversation.send(reply,
                                  JFapChannelConstants.SEG_CREATE_CLONE_CONNECTION_R,
                                  requestNumber,
                                  JFapChannelConstants.PRIORITY_MEDIUM,
                                  true,
                                  ThrottlingPolicy.BLOCK_THREAD,
                                  null);
            } catch (SIException e) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".rcvCloneConnection",
                                            CommsConstants.STATICCATCONNECTION_CONNCLONE_01);

                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2021", e);
            }
        } catch (SIException e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!convState.hasMETerminated()) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".rcvCloneConnection",
                                            CommsConstants.STATICCATCONNECTION_CONNCLONE_02);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.STATICCATCONNECTION_CONNCLONE_02,
                                                  conversation, requestNumber);
        } catch (ConversationStateFullException e) {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvCloneConnection",
                                        CommsConstants.STATICCATCONNECTION_CONNCLONE_03);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.STATICCATCONNECTION_CONNCLONE_03,
                                                  conversation, requestNumber);
        }

        request.release(allocatedFromBufferPool);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "rcvCloneConnection");
    }

    // Clone a connection from an existing SICoreConnection and put reply information into the supplied reply buffer
    private static void _cloneConnection(final Conversation conversation, final SICoreConnection parent, final CommsByteBuffer reply)
                    throws SIConnectionUnavailableException, ConversationStateFullException, SIConnectionUnavailableException, SIConnectionUnavailableException, SIResourceException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "_cloneConnection", "conversation=" + conversation + ", parent=" + parent + ", reply=" + reply);

        final ConversationState convState = (ConversationState) conversation.getAttachment();

        // Create a new ServerSideConnection and save in the ConversationState object
        final ServerSideConnection cc = new ServerSideConnection(conversation, parent); // Note that this is a cloned connection
        convState.setCommsConnection(cc);

        // Clone the parent connection and save in the ConversationState object
        final SICoreConnection clonedConnection = parent.cloneConnection();
        cc.setSICoreConnection(clonedConnection);

        // Register with the global connection listener & attach to the cloned connection
        final ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();
        final ServerSICoreConnectionListener listener = linkState.getSICoreConnectionListener();
        listener.addSICoreConnection(clonedConnection, conversation);
        clonedConnection.addConnectionListener(listener);

        // Obtain a new connection object Id for the cloned connection
        final CATConnection clonedConn = new CATConnection(clonedConnection);
        final short connectionObjectID = (short) convState.addObject(clonedConn);
        convState.setConnectionObjectId(connectionObjectID);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "clonedConnectionID=", Short.valueOf(connectionObjectID));

        // Put the reply information into the reply buffer
        reply.putShort(connectionObjectID);

        // If the client is JFAP 9 or above include a Unique Id & Order Context in the reply
        if (conversation.getHandshakeProperties().getFapLevel() >= JFapChannelConstants.FAP_VERSION_9) {
            // Unique Id
            final byte[] uniqueId = createUniqueId(clonedConnection);
            reply.putShort(uniqueId.length);
            reply.put(uniqueId);

            // Order Context
            final int storeId = createOrderContext(convState, clonedConnection);
            reply.putShort(storeId);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "_cloneConnection");
    }

    /**
     * Close the SICoreConnection. If FAP9+ see if the connection should be reset (rather than closed). The
     * resetting of a connection means that the existing connection can be reused by the client on a
     * subsequent clone without communicating with the server.
     * 
     * This method uses only required fields. All mandatory fields have a fixed order and size.
     * 
     * Mandatory Fields:
     * BIT16 ConnectionObjectId
     * BYTE reset/close connection (FAP9+)
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param allocatedFromBufferPool
     * @param partOfExchange
     */
    static void rcvCloseConnection(CommsByteBuffer request, Conversation conversation,
                                   int requestNumber, boolean allocatedFromBufferPool,
                                   boolean partOfExchange) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "rcvCloseConnection",
                        new Object[]
                        {
                         request,
                         conversation,
                         "" + requestNumber,
                         "" + allocatedFromBufferPool
                        });

        final ConversationState convState = (ConversationState) conversation.getAttachment();

        final short connectionObjectID = request.getShort();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "connectionObjectID", Short.valueOf(connectionObjectID));

        // Get connection from connectionObjectID
        final CATConnection catConnection = (CATConnection) convState.getObject(connectionObjectID);
        final SICoreConnection connection = catConnection.getSICoreConnection();
        final ServerSideConnection serverConnection = (ServerSideConnection) convState.getCommsConnection();

        // Determine whether we should reset this connection or not. A connection can only be reset if it was cloned and the
        // client has request a reset.
        boolean resetConnection = false; // Always false for below FAP9
        final SICoreConnection parent = serverConnection.getParentConnection();

        final boolean fap9orAbove = (conversation.getHandshakeProperties().getFapLevel() >= JFapChannelConstants.FAP_VERSION_9);
        if (fap9orAbove) {
            resetConnection = (request.get() == (byte) 1); // Did client ask for a connection reset or close
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "resetConnection=" + resetConnection);

            if (parent == null) { // If current connection has no parent it can't be reset so force a close
                resetConnection = false;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Current connection has no parent so forcing a close on this connection, resetConnection=" + resetConnection);
            }
        }

        try {
            // This is stuff we need to clean up whether resetting or closing
            final ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();
            connection.removeConnectionListener(linkState.getSICoreConnectionListener());
            connection.close(); // Close the original SICoreConnection with message processor

            linkState.getSICoreConnectionListener().removeSICoreConnection(connection);
            linkState.getSICoreConnectionTable().remove(conversation.getId());
            convState.removeObject(connectionObjectID);

            // Ensure all transactions are cleaned up
            linkState.getTransactionTable().rollbackEnlisted(conversation);
            linkState.getTransactionTable().removeTransactions(conversation, linkState.getDispatchableMap());

            // Ensure the conversation state object store is emptied of artifacts belonging to the old conversation
            convState.emptyObjectStore();

            CommsByteBuffer reply = poolManager.allocate();

            if (fap9orAbove) {
                if (resetConnection) {
                    try {
                        reply.put((byte) 1); // Connection will be reset
                        _cloneConnection(conversation, parent, reply);
                    } catch (SIConnectionUnavailableException e) {
                        // This exception means the parent has been closed in the time it has taken
                        // for this close connection request to come over. Change the resetConnection 
                        // to not reset as we failed

                        // Release the current bytebuffer as we have already started populating it
                        reply.release();
                        // Obtain a new bytebuffer
                        reply = poolManager.allocate();

                        reply.put((byte) 0); // Connection not reset
                        resetConnection = false;
                    }
                } else {
                    reply.put((byte) 0); // Connection not reset
                }
            }

            try {
                conversation.send(reply,
                                  JFapChannelConstants.SEG_CLOSE_CONNECTION_R,
                                  requestNumber,
                                  JFapChannelConstants.PRIORITY_LOWEST,
                                  true,
                                  ThrottlingPolicy.BLOCK_THREAD,
                                  null);
            } catch (SIException e) {
                FFDCFilter.processException(e, CLASS_NAME + ".rcvCloseConnection", CommsConstants.STATICCATCONNECTION_CONNCLOSE_01);
                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2021", e);
            }

            serverConnection.close(resetConnection);
        } catch (SIException e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!convState.hasMETerminated()) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".rcvCloseConnection",
                                            CommsConstants.STATICCATCONNECTION_CONNCLOSE_02);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.STATICCATCONNECTION_CONNCLOSE_02,
                                                  conversation, requestNumber);
        } catch (ConversationStateFullException e) {
            FFDCFilter.processException(e, CLASS_NAME + ".rcvCloseConnection", CommsConstants.STATICCATCONNECTION_CONNCLOSE_03);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e, CommsConstants.STATICCATCONNECTION_CONNCLOSE_03, conversation, requestNumber);
        }

        request.release(allocatedFromBufferPool);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "rcvCloseConnection");
    }

    /**
     * This method handles the request for another unique Id stem. This is retrieved
     * from the connection object and returned to the caller.
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param allocatedFromBufferPool
     * @param partOfExchange
     */
    static void rcvGetUniqueId(CommsByteBuffer request, Conversation conversation, int requestNumber,
                               boolean allocatedFromBufferPool, boolean partOfExchange) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "rcvGetUniqueId",
                        new Object[]
                        {
                         request,
                         conversation,
                         "" + requestNumber,
                         "" + allocatedFromBufferPool
                        });

        ConversationState convState = (ConversationState) conversation.getAttachment();

        short connectionObjectID = request.getShort();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "connectionObjectID", Short.valueOf(connectionObjectID));

        // Get connection from connectionObjectID
        SICoreConnection connection =
                        ((CATConnection) convState.getObject(connectionObjectID)).getSICoreConnection();

        try {
            final byte[] uniqueId = createUniqueId(connection);

            CommsByteBuffer reply = poolManager.allocate();
            reply.putShort(uniqueId.length);
            reply.put(uniqueId);

            try {
                conversation.send(reply,
                                  JFapChannelConstants.SEG_GET_UNIQUE_ID_R,
                                  requestNumber,
                                  JFapChannelConstants.PRIORITY_MEDIUM,
                                  true,
                                  ThrottlingPolicy.BLOCK_THREAD,
                                  null);
            } catch (SIException e) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".rcvGetUniqueId",
                                            CommsConstants.STATICCATCONNECTION_UNIQID_01);

                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2021", e);
            }
        } catch (SIException e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!convState.hasMETerminated()) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".rcvGetUniqueId",
                                            CommsConstants.STATICCATCONNECTION_UNIQID_02);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.STATICCATCONNECTION_UNIQID_02,
                                                  conversation, requestNumber);
        }

        request.release(allocatedFromBufferPool);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "rcvGetUniqueId");
    }

    private static byte[] createUniqueId(final SICoreConnection connection) throws SIConnectionUnavailableException, SIResourceException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createUniqueId", "connection=" + connection);

        final byte[] uniqueId = connection.createUniqueId();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            SibTr.debug(tc, "ID Length", Integer.valueOf(uniqueId.length));
            SibTr.debug(tc, "ID: ");
            SibTr.bytes(tc, uniqueId);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createUniqueId");
        return uniqueId;
    }

    /**
     * Creates the server side message order context and saves it in the link level state.
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param allocatedFromBufferPool
     * @param partOfExchange
     */
    static void rcvCreateOrderContext(CommsByteBuffer request, Conversation conversation, int requestNumber,
                                      boolean allocatedFromBufferPool, boolean partOfExchange) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "rcvCreateOrderContext",
                        new Object[]
                        {
                         request,
                         conversation,
                         "" + requestNumber,
                         "" + allocatedFromBufferPool
                        });

        ConversationState convState = (ConversationState) conversation.getAttachment();

        short connectionObjectId = request.getShort(); // BIT16 ConnectionObjectId

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "SICoreConnection Id:", "" + connectionObjectId);

        SICoreConnection connection =
                        ((CATConnection) convState.getObject(connectionObjectId)).getSICoreConnection();

        try {
            final int storeId = createOrderContext(convState, connection);

            // Construct a reply
            CommsByteBuffer reply = poolManager.allocate();
            reply.putShort(storeId);

            // Now reply
            try {
                conversation.send(reply,
                                  JFapChannelConstants.SEG_CREATE_ORDER_CONTEXT_R,
                                  requestNumber,
                                  JFapChannelConstants.PRIORITY_MEDIUM,
                                  true,
                                  ThrottlingPolicy.BLOCK_THREAD,
                                  null);
            } catch (SIException e) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".rcvCreateOrderContext",
                                            CommsConstants.STATICCATCONNECTION_CREATE_OC_01);

                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2021", e);
            }
        } catch (SIException e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!convState.hasMETerminated()) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".rcvCreateOrderContext",
                                            CommsConstants.STATICCATCONNECTION_CREATE_OC_02);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.STATICCATCONNECTION_CREATE_OC_02,
                                                  conversation, requestNumber);
        } catch (ConversationStateFullException e) {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvCreateOrderContext",
                                        CommsConstants.STATICCATCONNECTION_CREATE_OC_03);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.STATICCATCONNECTION_CREATE_OC_03,
                                                  conversation, requestNumber);
        }

        request.release(allocatedFromBufferPool);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "rcvCreateOrderContext");
    }

    private static int createOrderContext(final ConversationState convState, final SICoreConnection connection) throws SIConnectionUnavailableException, ConversationStateFullException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createOrderContext", "convState=" + convState + ", connection=" + connection);

        // Create the server side order context
        final CATOrderingContext orderContext = new CATOrderingContext(connection.createOrderingContext()); // F201521

        // Save it in the conversation state
        final int storeId = convState.addObject(orderContext);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createOrderContext", "rc=" + storeId);
        return storeId;
    }

    /**
     * Removes the server side message order context from the conversation store.
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param allocatedFromBufferPool
     * @param partOfExchange
     */
    static void rcvCloseOrderContext(CommsByteBuffer request, Conversation conversation, int requestNumber,
                                     boolean allocatedFromBufferPool, boolean partOfExchange) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "rcvCloseOrderContext",
                        new Object[]
                        {
                         request,
                         conversation,
                         "" + requestNumber,
                         "" + allocatedFromBufferPool
                        });

        ConversationState convState = (ConversationState) conversation.getAttachment();

        short connectionObjectId = request.getShort(); // BIT16 ConnectionObjectId
        short orderContextId = request.getShort(); // BIT16 OrderContextId

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            SibTr.debug(tc, "SICoreConnection Id:", "" + connectionObjectId);
            SibTr.debug(tc, "OrderContext Id:", "" + orderContextId);
        }

        // Now all we need to do here is remove the item from our table
        convState.removeObject(orderContextId);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Successfully removed the item from the store");

        // Now reply
        try {
            conversation.send(poolManager.allocate(),
                              JFapChannelConstants.SEG_CLOSE_ORDER_CONTEXT_R,
                              requestNumber,
                              JFapChannelConstants.PRIORITY_MEDIUM,
                              true,
                              ThrottlingPolicy.BLOCK_THREAD,
                              null);
        } catch (SIException e) {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvCloseOrderContext",
                                        CommsConstants.STATICCATCONNECTION_CLOSE_OC_01);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2021", e);
        }

        request.release(allocatedFromBufferPool);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "rcvCloseOrderContext");
    }

    /**
     * Creates the server side message order context and saves it in the link level state.
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param allocatedFromBufferPool
     * @param partOfExchange
     */
    static void rcvCheckMessagingRequired(CommsByteBuffer request, Conversation conversation,
                                          int requestNumber, boolean allocatedFromBufferPool,
                                          boolean partOfExchange) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "rcvCheckMessagingRequired",
                        new Object[]
                        {
                         request,
                         conversation,
                         "" + requestNumber,
                         "" + allocatedFromBufferPool
                        });

        ConversationState convState = (ConversationState) conversation.getAttachment();

        short connectionObjectId = request.getShort(); // BIT16 ConnectionObjectId
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "SICoreConnection Id:", "" + connectionObjectId);

        /**************************************************************/
        /* Request destination address */
        /**************************************************************/
        SIDestinationAddress requestDestinationAddress = request.getSIDestinationAddress(conversation.getHandshakeProperties().getFapLevel());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Request destination address",
                        requestDestinationAddress);

        /**************************************************************/
        /* Reply destination address */
        /**************************************************************/
        SIDestinationAddress replyDestinationAddress = request.getSIDestinationAddress(conversation.getHandshakeProperties().getFapLevel());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Reply destination address",
                        replyDestinationAddress);

        /**************************************************************/
        /* Destination type */
        /**************************************************************/
        short destinationTypeShort = request.getShort();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Destination Code", "" + destinationTypeShort);

        DestinationType destType = null;
        if (destinationTypeShort != CommsConstants.NO_DEST_TYPE) {
            destType = DestinationType.getDestinationType(destinationTypeShort);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Destination Type", destType);

        /**************************************************************/
        /* Alternate user Id */
        /**************************************************************/
        String alternateUser = request.getString();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Alternate User", alternateUser);

        try {
            // Now do the FAP check and throw it out if the FAP level is not Version 2
            final HandshakeProperties props = conversation.getHandshakeProperties();
            CommsUtils.checkFapLevel(props, JFapChannelConstants.FAP_VERSION_2);

            // Get the connection
            SICoreConnection connection =
                            ((CATConnection) convState.getObject(connectionObjectId)).getSICoreConnection();

            // Now make the Core SPI call
            SIDestinationAddress resolvedRequestDestination =
                            connection.checkMessagingRequired(requestDestinationAddress,
                                                              replyDestinationAddress,
                                                              destType,
                                                              alternateUser);

            CommsByteBuffer reply = poolManager.allocate();

            // Now reply - If null was returned, do not send back any additional data on the reply
            if (resolvedRequestDestination != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Sending back ", resolvedRequestDestination);

                // Convert the destination address into bytes and add it into the buffer to send back
                reply.putSIDestinationAddress(resolvedRequestDestination, conversation.getHandshakeProperties().getFapLevel());
            }

            // And wake up the client
            try {
                conversation.send(reply,
                                  JFapChannelConstants.SEG_CHECK_MESSAGING_REQUIRED_R,
                                  requestNumber,
                                  JFapChannelConstants.PRIORITY_MEDIUM,
                                  true,
                                  ThrottlingPolicy.BLOCK_THREAD,
                                  null);
            } catch (SIException e) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".rcvCheckMessagingRequired",
                                            CommsConstants.STATICCATCONNECTION_CHK_MESSAGING_REQ_01);

                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2021", e);
            }
        } catch (SINotAuthorizedException e) {
            // No FFDC Code Needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  null,
                                                  conversation, requestNumber);
        } catch (SIException e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!convState.hasMETerminated()) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".rcvCheckMessagingRequired",
                                            CommsConstants.STATICCATCONNECTION_CHK_MESSAGING_REQ_02);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.STATICCATCONNECTION_CHK_MESSAGING_REQ_02,
                                                  conversation, requestNumber);

        }

        request.release(allocatedFromBufferPool);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "rcvCheckMessagingRequired");
    }

    /**
     * This method performs the server side invoke command operation as requested by the peer.
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param allocatedFromBufferPool
     */
    static void rcvInvokeCommand(CommsServerByteBuffer request, Conversation conversation,
                                 int requestNumber, boolean allocatedFromBufferPool,
                                 boolean transactedCall) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "rcvInvokeCommand",
                        new Object[]
                        {
                         request,
                         conversation,
                         "" + requestNumber,
                         "" + allocatedFromBufferPool,
                         "" + transactedCall
                        });

        ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();
        ConversationState convState = (ConversationState) conversation.getAttachment();
        final boolean optimizedTx = CommsUtils.requiresOptimizedTransaction(conversation);

        short connectionObjectId = request.getShort(); // BIT16 ConnectionObjectId
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "SICoreConnection Id:", "" + connectionObjectId);

        /**************************************************************/
        /* The transaction (only present on FAP 6+) */
        /**************************************************************/
        SITransaction siTran = null;
        if (transactedCall) {
            int transactionId = request.getSITransactionId(connectionObjectId,
                                                           linkState,
                                                           optimizedTx);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Transaction Id: ", "" + transactionId);

            siTran = linkState.getTransactionTable().get(transactionId);
        }

        /**************************************************************/
        /* Key name */
        /**************************************************************/
        String keyName = request.getString();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Key Name", keyName);

        /**************************************************************/
        /* Command name */
        /**************************************************************/
        String cmdName = request.getString();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Command Name", cmdName);

        /**************************************************************/
        /* Command data */
        /**************************************************************/
        int dataLength = request.getInt();
        byte[] cmdData = request.get(dataLength);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.bytes(tc, cmdData);

        try {
            // Now do the FAP check and throw it out if the FAP level is not Version 5 or above
            final HandshakeProperties props = conversation.getHandshakeProperties();
            CommsUtils.checkFapLevel(props, JFapChannelConstants.FAP_VERSION_5);
            // The transaction version of this call is only present in FAP6 and above
            if (transactedCall)
                CommsUtils.checkFapLevel(props, JFapChannelConstants.FAP_VERSION_6);

            // We can now try and make an object out of the data that arrived over the wire
            Serializable inputObject = null;
            ObjectInputStream ois = null;
            try {

                ClassLoader cl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>()
                {
                    @Override
                    public ClassLoader run() {
                        return Thread.currentThread().getContextClassLoader();
                    }
                });
                ois = new DeserializationObjectInputStream(new ByteArrayInputStream(cmdData), cl);
                inputObject = (Serializable) ois.readObject();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Successfully deserialized the object", inputObject);
            } catch (Exception e) {
                // No FFDC Code needed
                // This will be FFDC'd as we are re-throwing as an SI exception
                throw new SICommandInvocationFailedException(
                                TraceNLS.getFormattedMessage(CommsConstants.MSG_BUNDLE, "FAILED_TO_DESERIALIZE_COMMAND_SICO2162", null, "FAILED_TO_DESERIALIZE_COMMAND_SICO2162"),
                                e);
            } finally {
                try {
                    if (ois != null) {
                        ois.close();
                    }
                } catch (IOException ex) {
                    // No FFDC code needed
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Exception closing the ObjectInputStream", ex);
                }
            }

            // Get the connection
            SICoreConnection connection =
                            ((CATConnection) convState.getObject(connectionObjectId)).getSICoreConnection();

            // Now make the Core SPI call
            Serializable returnObject = null;

            // Now invoke the right call depending on whether the transacted flavour of the call was
            // invoked or not
            if (transactedCall) {
                returnObject = connection.invokeCommand(keyName, cmdName, inputObject, siTran);
            } else {
                returnObject = connection.invokeCommand(keyName, cmdName, inputObject);
            }

            // Now reply - If null was returned, then do not return any data back
            CommsByteBuffer reply = poolManager.allocate();

            if (returnObject != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Sending back ", returnObject);

                // Create the data to send back
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(returnObject);
                    byte[] returnDataBytes = baos.toByteArray();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.bytes(tc, returnDataBytes);

                    reply.putInt(returnDataBytes.length);
                    reply.put(returnDataBytes);
                } catch (IOException e) {
                    // No FFDC Code needed
                    // This will be FFDC'd as we are re-throwing as an SI exception
                    throw new SICommandInvocationFailedException(
                                    TraceNLS.getFormattedMessage(CommsConstants.MSG_BUNDLE, "FAILED_TO_SERIALIZE_COMMAND_SICO2161", null, "FAILED_TO_SERIALIZE_COMMAND_SICO2161"),
                                    e);
                }
            }

            // And wake up the client
            try {
                conversation.send(reply,
                                  JFapChannelConstants.SEG_INVOKE_COMMAND_R,
                                  requestNumber,
                                  JFapChannelConstants.PRIORITY_MEDIUM,
                                  true,
                                  ThrottlingPolicy.BLOCK_THREAD,
                                  null);
            } catch (SIException e) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".rcvInvokeCommand",
                                            CommsConstants.STATICCATCONNECTION_INVOKECMD_01);

                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2021", e);
            }
        } catch (SINotAuthorizedException e) {
            // No FFDC Code Needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  null,
                                                  conversation, requestNumber);
        } catch (SIException e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!convState.hasMETerminated()) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".rcvInvokeCommand",
                                            CommsConstants.STATICCATCONNECTION_INVOKECMD_02);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.STATICCATCONNECTION_INVOKECMD_02,
                                                  conversation, requestNumber);

        }

        request.release(allocatedFromBufferPool);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "rcvInvokeCommand");
    }

    // SIB0137.comms.2 start

    /*
     * addDestinationListener
     */

    static void rcvAddDestinationListener(CommsServerByteBuffer request, Conversation conversation, int requestNumber, boolean allocatedFromBufferPool, boolean partOfExchange) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "rcvAddDestinationListener");

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "conversation=" + conversation +
                            ",requestNumber=" + requestNumber +
                            ",allocatedFromBufferPool=" + allocatedFromBufferPool +
                            ",partOfExchange=" + partOfExchange);

        // Get Connection object Id
        final short connectionObjectId = request.getShort();
        final short destinationListenerId = request.getShort();
        final short destinationTypeShort = request.getShort();
        final short destinationAvailabilityShort = request.getShort();
        final String destinationNamePattern = request.getString();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "connectionObjectId=" + connectionObjectId);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "destinationListenerId=" + destinationListenerId);

        DestinationType destinationType = null;
        if (destinationTypeShort != CommsConstants.NO_DEST_TYPE) {
            destinationType = DestinationType.getDestinationType(destinationTypeShort);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "destinationType=" + destinationType);

        DestinationAvailability destinationAvailability = null;
        if (destinationAvailabilityShort != CommsConstants.NO_DEST_AVAIL) {
            destinationAvailability = DestinationAvailability.getDestinationAvailability(destinationAvailabilityShort);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "destinationAvailability=" + destinationAvailability);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "destinationNamePattern=" + destinationNamePattern);

        SIDestinationAddress results[] = null;
        final ConversationState convState = (ConversationState) conversation.getAttachment();
        try {
            final SICoreConnection connection = ((CATConnection) convState.getObject(connectionObjectId)).getSICoreConnection();
            final DestinationListenerCache destinationListenerCache = convState.getDestinationListenerCache();

            // See if the DestinationListener Id is already cached
            DestinationListener destinationListener = destinationListenerCache.get(destinationListenerId);
            if (destinationListener == null) { // If Id is not already in use, create a new DestinationListener for Id and add to cache
                destinationListener = new ServerDestinationListener(destinationListenerId, connectionObjectId, conversation); //SIB0137.comms.3
                destinationListenerCache.add(destinationListenerId, destinationListener);
            }

            results = connection.addDestinationListener(destinationNamePattern, destinationListener, destinationType, destinationAvailability);

            CommsByteBuffer reply = poolManager.allocate();

            // Put the connection object Id
            reply.putShort(connectionObjectId);

            // Put the number of SIDestinationAddress entries
            final short count = (results == null) ? (short) 0 : (short) results.length;
            reply.putShort(count);

            // Put the SIDestinationAddress entries
            for (int i = 0; i < count; i++) {
                reply.putSIDestinationAddress(results[i], conversation.getHandshakeProperties().getFapLevel());
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "rcvAddDestinationListener", count + " SIDestinationAddress objects to return"); //473617

            try {
                conversation.send(reply,
                                  JFapChannelConstants.SEG_ADD_DESTINATION_LISTENER_R,
                                  requestNumber,
                                  JFapChannelConstants.PRIORITY_MEDIUM,
                                  true,
                                  ThrottlingPolicy.BLOCK_THREAD,
                                  null);
            } catch (SIException e) {
                FFDCFilter.processException(e, CLASS_NAME + ".rcvAddDestinationListener", CommsConstants.STATICCATCONNECTION_ADD_DEST_LISTENER_01);
                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2021", e);
            }
        } catch (SIIncorrectCallException e) {
            // No FFDC Code Needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);
            StaticCATHelper.sendExceptionToClient(e, null, conversation, requestNumber);
        } catch (SICommandInvocationFailedException e) {
            // No FFDC Code Needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);
            StaticCATHelper.sendExceptionToClient(e, null, conversation, requestNumber);
        } catch (SIConnectionUnavailableException e) {
            // No FFDC Code Needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);
            StaticCATHelper.sendExceptionToClient(e, null, conversation, requestNumber);
        } catch (SIException e) {
            // No FFDC Code Needed
            // Only FFDC if we haven't received a meTerminated event.
            if (!convState.hasMETerminated()) {
                FFDCFilter.processException(e, CLASS_NAME + ".rcvAddDestinationListener", CommsConstants.STATICCATCONNECTION_ADD_DEST_LISTENER_02);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);
            StaticCATHelper.sendExceptionToClient(e, null, conversation, requestNumber);
        }

        request.release(allocatedFromBufferPool);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "rcvAddDestinationListener");
    }

    // SIB0.comms.2 end

    // F011127 START
    static void rcvAddConsumerMonitorListener(CommsServerByteBuffer request, Conversation conversation, int requestNumber, boolean allocatedFromBufferPool, boolean partOfExchange) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "rcvAddConsumerMonitorListener", new Object[] {
                        "conversation=" + conversation +
                                        ",requestNumber=" + requestNumber +
                                        ",allocatedFromBufferPool=" + allocatedFromBufferPool +
                                        ",partOfExchange=" + partOfExchange
            });

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "conversation=" + conversation +
                            ",requestNumber=" + requestNumber +
                            ",allocatedFromBufferPool=" + allocatedFromBufferPool +
                            ",partOfExchange=" + partOfExchange);

        // Get Connection object Id
        final short connectionObjectId = request.getShort();
        SIDestinationAddress destinationAddress = request.getSIDestinationAddress(conversation.getHandshakeProperties().getFapLevel());
        final String topicExpression = request.getString();
        final short consumerMonitorListenerID = request.getShort();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            SibTr.debug(tc, "connectionObjectId=" + connectionObjectId);
            SibTr.debug(tc, "destinationAddress=" + destinationAddress);
            SibTr.debug(tc, "topicExpression=" + topicExpression);
            SibTr.debug(tc, "consumerMonitorListenerID=" + consumerMonitorListenerID);
        }

        boolean areConsumers = false;
        final ConversationState convState = (ConversationState) conversation.getAttachment();
        try {
            final SICoreConnection connection = ((CATConnection) convState.getObject(connectionObjectId)).getSICoreConnection();
            final ConsumerMonitorListenerCache consumerMonitorListenerCache = convState.getConsumerMonitorListenerCache();

            // See if the consumerMonitorListener Id is already cached
            ConsumerSetChangeCallback consumerSetChangeCallback = consumerMonitorListenerCache.get(consumerMonitorListenerID);
            if (consumerSetChangeCallback == null) { // If Id is not already in use, create a new ConsumerSetChangeCallback for Id and add to cache
                consumerSetChangeCallback = new ServerConsumerMonitorListener(consumerMonitorListenerID, connectionObjectId, conversation);
                consumerMonitorListenerCache.add(consumerMonitorListenerID, consumerSetChangeCallback);
            }

            areConsumers = connection.registerConsumerSetMonitor(destinationAddress, topicExpression, consumerSetChangeCallback);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "rcvAddConsumerMonitorListener", "areConsumers : " + areConsumers);

            CommsByteBuffer reply = poolManager.allocate();

            // Put the connection object Id
            reply.putShort(connectionObjectId);

            // Put areConsumers
            reply.putBoolean(areConsumers);

            try {
                conversation.send(reply,
                                  JFapChannelConstants.SEG_REGISTER_CONSUMER_SET_MONITOR_R,
                                  requestNumber,
                                  JFapChannelConstants.PRIORITY_MEDIUM,
                                  true,
                                  ThrottlingPolicy.BLOCK_THREAD,
                                  null);
            } catch (SIException e) {
                FFDCFilter.processException(e, CLASS_NAME + ".rcvAddConsumerMonitorListener", CommsConstants.STATICCATCONNECTION_REG_SET_CONSUMER_MON_01);
                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2021", e);
            }
        } catch (SIErrorException e) {
            // No FFDC Code Needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);
            StaticCATHelper.sendExceptionToClient(e, null, conversation, requestNumber);
        } catch (SIException e) {
            // No FFDC Code Needed
            // Only FFDC if we haven't received a meTerminated event.
            if (!convState.hasMETerminated()) {
                FFDCFilter.processException(e, CLASS_NAME + ".rcvAddConsumerMonitorListener", CommsConstants.STATICCATCONNECTION_REG_SET_CONSUMER_MON_02);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);
            StaticCATHelper.sendExceptionToClient(e, null, conversation, requestNumber);
        }

        request.release(allocatedFromBufferPool);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "rcvAddConsumerMonitorListener");

    }
    // F011127 END
}
