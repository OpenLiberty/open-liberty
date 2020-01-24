/*******************************************************************************
 * Copyright (c) 2012, 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server.clientsupport;

import java.util.NoSuchElementException;

import javax.security.auth.Subject;
import javax.transaction.xa.Xid;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.context.AuditManager;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.ClientConnection;
import com.ibm.ws.sib.comms.CommsConnection;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.CompHandshake;
import com.ibm.ws.sib.comms.ComponentData;
import com.ibm.ws.sib.comms.client.ConnectionMetaDataImpl;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.comms.common.DirectConnectionImpl;
import com.ibm.ws.sib.comms.common.XidProxy;
import com.ibm.ws.sib.comms.server.CommonServerReceiveListener;
import com.ibm.ws.sib.comms.server.CommsServerByteBuffer;
import com.ibm.ws.sib.comms.server.ConversationState;
import com.ibm.ws.sib.comms.server.ConversationStateFullException;
import com.ibm.ws.sib.comms.server.ServerLinkLevelState;
import com.ibm.ws.sib.comms.server.TransactionToDispatchableMap;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.Conversation.ThrottlingPolicy;
import com.ibm.ws.sib.jfapchannel.ConversationReceiveListener;
import com.ibm.ws.sib.jfapchannel.DispatchToAllNonEmptyDispatchable;
import com.ibm.ws.sib.jfapchannel.Dispatchable;
import com.ibm.ws.sib.jfapchannel.HandshakeProperties;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.JFapHeartbeatTimeoutException;
import com.ibm.ws.sib.jfapchannel.JFapConnectionBrokenException;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.mfp.impl.CompHandshakeFactory;
import com.ibm.ws.sib.processor.MPCoreConnection;
import com.ibm.ws.sib.trm.attach.TrmSingleton;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.exception.SIAuthenticationException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * This class handles all the data coming in from a connected client. It's job is
 * to farm out all the data received to appropriate handlers who will parse
 * the data and action is appropriately.
 * <p>
 * However, this class does directly handle handshake, authentication and clone connection
 * requests.
 * 
 * @author schmittm
 */
public class ServerTransportReceiveListener extends CommonServerReceiveListener implements ConversationReceiveListener {
    /** Class name for FFDC's */
    private static String CLASS_NAME = ServerTransportReceiveListener.class.getName();

    /** The trace component */
    private static final TraceComponent tc = SibTr.register(ServerTransportReceiveListener.class,
                                                            CommsConstants.MSG_GROUP,
                                                            CommsConstants.MSG_BUNDLE);

    /** Our NLS reference object */
    private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);

    /** The singleton instance of this class */
    private static ServerTransportReceiveListener instance;

    /** Log source info on class load */
    static {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(
                        tc,
                        "Source info: @(#)SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/clientsupport/ServerTransportReceiveListener.java, SIB.comms, WASX.SIB, aa1225.01 1.173.1.2");
        instance = new ServerTransportReceiveListener();
    }

    /**
     * Singleton getter
     * 
     * @return Returns the instance.
     */
    public static ServerTransportReceiveListener getInstance() {
        return instance;
    }

    /**
     * Constructor ServerTransportReceiveListener.
     */
    private ServerTransportReceiveListener() {
        super(true);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "<init>");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "<init>");
    }

    /**
     * Interpret the segment type sent from the client and call the call the appropriate "segment" method.
     * The called method will extract the data from the WsByteBuffer, perform desired behavior, and send a
     * reply back to the client on the conversation referencing the request number initially sent. If the
     * segment type is not understood the default case will be called and a comms exception will be thrown.
     * 
     * @param data
     * @param segmentType
     * @param requestNumber
     * @param priority
     * @param allocatedFromBufferPool
     * @param partOfExchange
     * @param conversation
     * 
     * @return ConversationReceiveListener A more appropriate receiveListener to receive
     *         future data on this conversation
     */
    @Override
    public ConversationReceiveListener dataReceived(WsByteBuffer data, int segmentType,
                                                    int requestNumber, int priority,
                                                    boolean allocatedFromBufferPool,
                                                    boolean partOfExchange,
                                                    Conversation conversation) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "dataReceived");

        AuditManager auditManager = new AuditManager();
        auditManager.setJMSConversationMetaData(conversation.getMetaData());

        // Get a CommsServerByteBuffer to wrap the data
        CommsServerByteBuffer buffer = poolManager.allocate();
        buffer.reset(data);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            String LF = System.lineSeparator();
            String debugInfo = LF + LF + "-------------------------------------------------------" + LF;

            debugInfo += " Segment type  : " + JFapChannelConstants.getSegmentName(segmentType) +
                         " - " + segmentType +
                         " (0x" + Integer.toHexString(segmentType) + ")" + LF;
            debugInfo += " Request number: " + requestNumber + LF;
            debugInfo += " Priority      : " + priority + LF;
            debugInfo += " Exchange?     : " + partOfExchange + LF;
            debugInfo += " From pool?    : " + allocatedFromBufferPool + LF;
            debugInfo += " Conversation  : " + conversation + LF;

            debugInfo += "-------------------------------------------------------" + LF;

            SibTr.debug(this, tc, debugInfo);
            SibTr.debug(this, tc, conversation.getFullSummary());
        }

        try {
            switch (segmentType) {
                case (JFapChannelConstants.SEG_HANDSHAKE):
                    rcvHandshake(buffer, conversation, requestNumber, allocatedFromBufferPool);

                    break;

                case (JFapChannelConstants.SEG_TOPOLOGY):
                    rcvTRMExchange(buffer, conversation, requestNumber, allocatedFromBufferPool,
                                   partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_MESSAGE_FORMAT_INFO):
                    rcvMFPExchange(buffer, conversation, requestNumber, allocatedFromBufferPool,
                                   partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_DIRECT_CONNECT):
                    rcvDirectConnect(buffer, conversation, requestNumber, allocatedFromBufferPool,
                                     partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_SEND_SCHEMA_NOREPLY):
                    rcvMFPSchema(buffer, conversation, requestNumber, allocatedFromBufferPool,
                                 partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_XACOMMIT):
                    StaticCATXATransaction.rcvXACommit(buffer, conversation, requestNumber,
                                                       allocatedFromBufferPool,
                                                       partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_XAEND):
                    StaticCATXATransaction.rcvXAEnd(buffer, conversation, requestNumber,
                                                    allocatedFromBufferPool,
                                                    partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_XAFORGET):
                    StaticCATXATransaction.rcvXAForget(buffer, conversation, requestNumber,
                                                       allocatedFromBufferPool,
                                                       partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_XAOPEN):
                    StaticCATXATransaction.rcvXAOpen(buffer, conversation, requestNumber,
                                                     allocatedFromBufferPool,
                                                     partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_XAPREPARE):
                    StaticCATXATransaction.rcvXAPrepare(buffer, conversation, requestNumber,
                                                        allocatedFromBufferPool,
                                                        partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_XARECOVER):
                    StaticCATXATransaction.rcvXARecover(buffer, conversation, requestNumber,
                                                        allocatedFromBufferPool,
                                                        partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_XAROLLBACK):
                    StaticCATXATransaction.rcvXARollback(buffer,
                                                         conversation,
                                                         requestNumber,
                                                         allocatedFromBufferPool,
                                                         partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_XASTART):
                    StaticCATXATransaction.rcvXAStart(buffer, conversation, requestNumber,
                                                      allocatedFromBufferPool,
                                                      partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_XA_GETTXTIMEOUT):
                    StaticCATXATransaction.rcvXA_getTxTimeout(buffer, conversation, requestNumber,
                                                              allocatedFromBufferPool,
                                                              partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_XA_SETTXTIMEOUT):
                    StaticCATXATransaction.rcvXA_setTxTimeout(buffer, conversation, requestNumber,
                                                              allocatedFromBufferPool,
                                                              partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_CLOSE_CONNECTION):
                    StaticCATConnection.rcvCloseConnection(buffer, conversation, requestNumber,
                                                           allocatedFromBufferPool,
                                                           partOfExchange);

                    break;

                case (JFapChannelConstants.SEG_CREATE_CLONE_CONNECTION):
                    StaticCATConnection.rcvCloneConnection(buffer, conversation, requestNumber,
                                                           allocatedFromBufferPool,
                                                           partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_CREATE_TEMP_DESTINATION):
                    StaticCATDestination.rcvCreateTempDestination(buffer, conversation, requestNumber,
                                                                  allocatedFromBufferPool,
                                                                  partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_DELETE_TEMP_DESTINATION):
                    StaticCATDestination.rcvDeleteTempDestination(buffer, conversation, requestNumber,
                                                                  allocatedFromBufferPool,
                                                                  partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_GET_DESTINATION_CONFIGURATION):
                    StaticCATDestination.rcvGetDestinationConfiguration(buffer, conversation, requestNumber,
                                                                        allocatedFromBufferPool,
                                                                        partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_CREATE_DURABLE_SUB):
                    StaticCATSubscription.rcvCreateDurableSub(buffer, conversation, requestNumber,
                                                              allocatedFromBufferPool,
                                                              partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_DELETE_DURABLE_SUB):
                    StaticCATSubscription.rcvDeleteDurableSub(buffer, conversation, requestNumber,
                                                              allocatedFromBufferPool,
                                                              partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_SEND_CONN_MSG):
                    StaticCATProducer.rcvSendConnMsg(buffer, conversation, requestNumber,
                                                     allocatedFromBufferPool,
                                                     partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_SEND_CONN_MSG_NOREPLY):
                    StaticCATProducer.rcvSendConnMsgNoReply(buffer, conversation, requestNumber,
                                                            allocatedFromBufferPool,
                                                            partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_RECEIVE_CONN_MSG):
                    StaticCATConnection.rcvReceiveConnMsg(buffer, conversation, requestNumber,
                                                          allocatedFromBufferPool,
                                                          partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_CREATE_PRODUCER_SESS):
                    StaticCATProducer.rcvCreateProducerSess(buffer, conversation, requestNumber,
                                                            allocatedFromBufferPool,
                                                            partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_CLOSE_CONSUMER_SESS):
                    StaticCATConsumer.rcvCloseConsumerSess(buffer, conversation, requestNumber,
                                                           allocatedFromBufferPool,
                                                           partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_CLOSE_PRODUCER_SESS):
                    StaticCATProducer.rcvCloseProducerSess(buffer, conversation, requestNumber,
                                                           allocatedFromBufferPool,
                                                           partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_SEND_SESS_MSG):
                    StaticCATProducer.rcvSendSessMsg(buffer, conversation, requestNumber,
                                                     allocatedFromBufferPool,
                                                     partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_SEND_SESS_MSG_NOREPLY):
                    StaticCATProducer.rcvSendSessMsgNoReply(buffer, conversation, requestNumber,
                                                            allocatedFromBufferPool,
                                                            partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_CREATE_CONSUMER_SESS):
                    StaticCATConsumer.rcvCreateConsumerSess(buffer, conversation, requestNumber,
                                                            allocatedFromBufferPool,
                                                            partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_RECEIVE_SESS_MSG):
                    StaticCATConsumer.rcvSessReceive(buffer, conversation, requestNumber,
                                                     allocatedFromBufferPool,
                                                     partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_REQUEST_MSGS):
                    StaticCATConsumer.rcvRequestMsgs(buffer, conversation, requestNumber,
                                                     allocatedFromBufferPool,
                                                     partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_CREATE_UCTRANSACTION):
                    StaticCATTransaction.rcvCreateUCTransaction(buffer, conversation, requestNumber,
                                                                allocatedFromBufferPool,
                                                                partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_COMMIT_TRANSACTION):
                    StaticCATTransaction.rcvCommitTransaction(buffer, conversation, requestNumber,
                                                              allocatedFromBufferPool,
                                                              partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_ROLLBACK_TRANSACTION):
                    StaticCATTransaction.rcvRollbackTransaction(buffer, conversation, requestNumber,
                                                                allocatedFromBufferPool,
                                                                partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_REGISTER_ASYNC_CONSUMER):
                case (JFapChannelConstants.SEG_REGISTER_STOPPABLE_ASYNC_CONSUMER): //SIB0115d.comms
                    StaticCATConsumer.rcvRegisterAsyncConsumer(buffer, conversation, requestNumber,
                                                               allocatedFromBufferPool,
                                                               partOfExchange,
                                                               segmentType == JFapChannelConstants.SEG_REGISTER_STOPPABLE_ASYNC_CONSUMER); //SIB0115d.comms
                    break;

                case (JFapChannelConstants.SEG_DEREGISTER_ASYNC_CONSUMER):
                case (JFapChannelConstants.SEG_DEREGISTER_STOPPABLE_ASYNC_CONSUMER): //SIB0115d.comms
                    StaticCATConsumer.rcvDeregisterAsyncConsumer(buffer, conversation, requestNumber,
                                                                 allocatedFromBufferPool,
                                                                 partOfExchange,
                                                                 segmentType == JFapChannelConstants.SEG_DEREGISTER_STOPPABLE_ASYNC_CONSUMER); //SIB0115d.comms
                    break;

                case (JFapChannelConstants.SEG_START_SESS):
                case (JFapChannelConstants.SEG_RESTART_SESS): //471642
                    StaticCATConsumer.rcvStartSess(buffer, conversation, requestNumber,
                                                   allocatedFromBufferPool,
                                                   partOfExchange,
                                                   (segmentType == JFapChannelConstants.SEG_RESTART_SESS)); //471642
                    break;

                case (JFapChannelConstants.SEG_STOP_SESS):
                    StaticCATConsumer.rcvStopSess(buffer, conversation, requestNumber,
                                                  allocatedFromBufferPool,
                                                  partOfExchange);

                    break;

                case (JFapChannelConstants.SEG_FLUSH_SESS):
                    StaticCATConsumer.rcvFlushSess(buffer, conversation, requestNumber,
                                                   allocatedFromBufferPool,
                                                   partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_UNLOCK_ALL):
                    StaticCATConsumer.rcvUnlockAll(buffer, conversation, requestNumber,
                                                   allocatedFromBufferPool,
                                                   partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_UNLOCK_SET):
                case (JFapChannelConstants.SEG_UNLOCK_SET_NOREPLY):
                    StaticCATConsumer.rcvUnlockSet(buffer, conversation, requestNumber,
                                                   allocatedFromBufferPool,
                                                   partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_DELETE_SET):
                case (JFapChannelConstants.SEG_DELETE_SET_NOREPLY):
                    StaticCATConsumer.rcvDeleteSet(buffer, conversation, requestNumber,
                                                   allocatedFromBufferPool,
                                                   partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_CREATE_BROWSER_SESS):
                    StaticCATBrowser.rcvCreateBrowserSess(buffer, conversation, requestNumber,
                                                          allocatedFromBufferPool,
                                                          partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_RESET_BROWSE):
                    StaticCATBrowser.rcvResetBrowse(buffer, conversation, requestNumber,
                                                    allocatedFromBufferPool,
                                                    partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_GET_UNIQUE_ID):
                    StaticCATConnection.rcvGetUniqueId(buffer, conversation, requestNumber,
                                                       allocatedFromBufferPool,
                                                       partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_CREATE_CONS_FOR_DURABLE_SUB):
                    StaticCATSubscription.rcvCreateConsumerForDurableSub(buffer, conversation, requestNumber,
                                                                         allocatedFromBufferPool,
                                                                         partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_READ_AND_DELETE_SET):
                    StaticCATConsumer.rcvReadAndDeleteSet(buffer, conversation, requestNumber,
                                                          allocatedFromBufferPool,
                                                          partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_READ_SET):
                    StaticCATConsumer.rcvReadSet(buffer, conversation, requestNumber,
                                                 allocatedFromBufferPool,
                                                 partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_CREATE_BIFURCATED_SESSION):
                    StaticCATConsumer.rcvCreateBifurcatedSess(buffer, conversation, requestNumber,
                                                              allocatedFromBufferPool,
                                                              partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_CREATE_ORDER_CONTEXT):
                    StaticCATConnection.rcvCreateOrderContext(buffer, conversation, requestNumber,
                                                              allocatedFromBufferPool, partOfExchange);

                    break;

                case (JFapChannelConstants.SEG_SEND_TO_EXCEPTION_DESTINATION):
                    StaticCATDestination.rcvSendToExceptionDest(buffer, conversation, requestNumber,
                                                                allocatedFromBufferPool,
                                                                partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_CLOSE_ORDER_CONTEXT):
                    StaticCATConnection.rcvCloseOrderContext(buffer, conversation, requestNumber,
                                                             allocatedFromBufferPool, partOfExchange);

                    break;

                case (JFapChannelConstants.SEG_REQUEST_SCHEMA):
                    rcvMFPRequestSchema(buffer, conversation, requestNumber, segmentType,
                                        allocatedFromBufferPool, partOfExchange);

                    break;

                case (JFapChannelConstants.SEG_CHECK_MESSAGING_REQUIRED):
                    StaticCATConnection.rcvCheckMessagingRequired(buffer, conversation, requestNumber,
                                                                  allocatedFromBufferPool, partOfExchange);

                    break;

                case (JFapChannelConstants.SEG_INVOKE_COMMAND):
                    StaticCATConnection.rcvInvokeCommand(buffer, conversation, requestNumber,
                                                         allocatedFromBufferPool, false);
                    break;

                case (JFapChannelConstants.SEG_INVOKE_COMMAND_WITH_TX):
                    StaticCATConnection.rcvInvokeCommand(buffer, conversation, requestNumber,
                                                         allocatedFromBufferPool, true);
                    break;

                case (JFapChannelConstants.SEG_SEND_CHUNKED_SESS_MSG):
                case (JFapChannelConstants.SEG_SEND_CHUNKED_SESS_MSG_NOREPLY):
                    StaticCATProducer.rcvSendChunkedSessMsg(buffer, conversation, requestNumber,
                                                            allocatedFromBufferPool, partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_SEND_CHUNKED_CONN_MSG):
                case (JFapChannelConstants.SEG_SEND_CHUNKED_CONN_MSG_NOREPLY):
                    StaticCATProducer.rcvSendChunkedConnMsg(buffer, conversation, requestNumber,
                                                            allocatedFromBufferPool, partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_SEND_CHUNKED_TO_EXCEPTION_DESTINATION):
                    StaticCATDestination.rcvSendChunkedToExceptionDest(buffer, conversation, requestNumber,
                                                                       allocatedFromBufferPool,
                                                                       partOfExchange);
                    break;

                case (JFapChannelConstants.SEG_ADD_DESTINATION_LISTENER): //SIB0137.comms.2
                    StaticCATConnection.rcvAddDestinationListener(buffer, conversation, requestNumber, //SIB0137.comms.2
                                                                  allocatedFromBufferPool, //SIB0137.comms.2
                                                                  partOfExchange); //SIB0137.comms.2
                    break; //SIB0137.comms.2

                case (JFapChannelConstants.SEG_REGISTER_CONSUMER_SET_MONITOR): //F011127
                    StaticCATConnection.rcvAddConsumerMonitorListener(buffer, conversation, requestNumber, //F011127
                                                                      allocatedFromBufferPool, //F011127
                                                                      partOfExchange); //F011127
                    break;

                case (JFapChannelConstants.SEG_UNLOCK_ALL_NO_INC_LOCK_COUNT): //F013661
                    StaticCATConsumer.rcvUnlockAllWithUnlockCountFlag(buffer, conversation, requestNumber,
                                                                      allocatedFromBufferPool,
                                                                      partOfExchange);
                    break;

                default:
                    String nlsText = nls.getFormattedMessage("INVALID_PROP_SICO8011",
                                                             new Object[] { "" + segmentType },
                                                             null);
                    SIConnectionLostException commsException = new SIConnectionLostException(nlsText);

                    if (partOfExchange) {
                        StaticCATHelper.sendExceptionToClient(commsException,
                                                              null,
                                                              conversation,
                                                              requestNumber);
                    }

                    if (allocatedFromBufferPool) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "releasing WsByteBuffer");
                        buffer.release();
                    }

                    // At this point we should close the connection down as we have no idea what the
                    // client is up to - sending us invalid segments.
                    closeConnection(conversation);

                    break;
            }
        } catch (Throwable t) {
            // If an exception is caught here this indicates that something has
            // thrown an exception, probably a RunTimeException that was not expected.
            // This may have been the result of a CoreAPI operation.
            // At this point we should FFDC and respond with the exception to the
            // client.
            FFDCFilter.processException(t,
                                        CLASS_NAME + ".dataReceived",
                                        CommsConstants.SERVERTRANSPORTRECEIVELISTENER_DATARCV_01,
                                        new Object[]
                                        {
                                         buffer.getDumpReceivedBytes(128),
                                         this
                                        });

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Caught an exception: ", t);

            if (partOfExchange) {
                StaticCATHelper.sendExceptionToClient(t,
                                                      CommsConstants.SERVERTRANSPORTRECEIVELISTENER_DATARCV_01,
                                                      conversation, requestNumber);
            }
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "dataReceived");
        }

        return null;
    }

    /**
     * This method is called by the JFAP channel when it detects a serious error.
     * At this point we should spit out all the info we have and close the connection
     * to the ME. This will ensure that the ME realises that we have died and
     * shutdown any resources we own accordingly.
     * 
     * @param exception
     * @param segmentType
     * @param requestNumber
     * @param priority
     * @param conversation
     */
    @Override
    public void errorOccurred(SIConnectionLostException exception, // F174602
                              int segmentType,
                              int requestNumber,
                              int priority,
                              Conversation conversation) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "errorOccurred",
                        new Object[]
                        {
                         exception,
                         "" + segmentType,
                         "" + requestNumber,
                         "" + priority,
                         conversation
                        });

        // Check and see if we think this is a heartbeat timeout. If it is, don't FFDC.
        Throwable cause = exception.getCause();
        if (cause != null && cause instanceof JFapHeartbeatTimeoutException) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Error is due to heartbeat timeout");
        } else if (exception instanceof JFapConnectionBrokenException
                   &&null!=cause
                   &&cause instanceof java.io.EOFException
                  ) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                SibTr.debug(this, tc, "Error is due to reaching the end of stream during a read");
            }
            System.out.println("Connection unexpectedly broken during read.");
        } else {
            FFDCFilter.processException(exception,
                                        CLASS_NAME + ".errorOccurred",
                                        CommsConstants.SERVERTRANSPORTRECEIVELISTENER_ERROR_01,
                                        this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Object[] debug =
            {
             "Segment type  : " + segmentType + " (0x" + Integer.toHexString(segmentType) + ")",
             "Request number: " + requestNumber,
             "Priority      : " + priority
            };

            SibTr.debug(this, tc, "Received an error in the ServerTransportReceiveListener", debug);
            SibTr.debug(this, tc, "Primary exception:", exception);
        }

        // D363790 removed the call to cleanupConnection().
        // This is because we will now rely on the ConnectionClosedListener to trigger the connection
        // cleanup as that will be driven not only on error conditions but also on normal socket
        // closure as well.

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "errorOccurred");
    }

    /**
     * This method is used to clean up any resources that are held by a Conversation. The steps to
     * clean up these resources are as follows:
     * <ol>
     * <li>The ServerSideConnection instance is notified of a connection failure which will cause
     * an event notification to be generated.
     * <li>The object store associated with the Conversation is searched for the connection. If
     * it is found, it is closed and any associated in-flight transactions are rolled back.
     * </ol>
     * <p>
     * Note that this method should not be called with a null Conversation.
     * 
     * @param conversation
     */
    protected void cleanupConnection(Conversation conversation) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "cleanupConnection", conversation);

        ConversationState convState = (ConversationState) conversation.getAttachment();
        ServerSideConnection serverSideConn = ((ServerSideConnection) convState.getCommsConnection());

        // This may be null if the connection falls over before it has had time to get going
        if (serverSideConn != null)
            serverSideConn.failed(); // F206161.5

        // Now go through the object table looking for the SICoreConnection
        Object obj = null;
        for (int x = ConversationState.OBJECT_TABLE_ORIGIN;; x++) {
            try {
                obj = convState.removeObject(x);

                if (obj instanceof CATConnection) // D254870
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Found a CATConnection at position " + x + " in object store");

                    CATConnection catConnection = (CATConnection) obj;
                    ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();

                    // At this point we also need to attempt to clean up any global transactions that
                    // are in use by this connection
                    linkState.getTransactionTable().rollbackTxWithoutCompletionDirection(conversation); // D257768

                    // Start d192146
                    // We have found the connection. Now close him down.
                    SICoreConnection connection = catConnection.getSICoreConnection(); // D254870, D257768
                    connection.removeConnectionListener(linkState.getSICoreConnectionListener());
                    connection.close();

                    linkState.getSICoreConnectionListener().removeSICoreConnection(connection);
                    linkState.getSICoreConnectionTable().remove(conversation.getId());
                    linkState.getTransactionTable().removeTransactions(conversation, linkState.getDispatchableMap()); // D254870, D257768
                    // End d192146
                }
            } catch (NoSuchElementException e) {
                // No FFDC code needed

                // We have reached a gap in the object table. We can only
                // assume that we have already closed the connection.
                break;
            } catch (SIException e) {
                // Oops, we could not close down. Not alot we can do here
                // except FFDC as we have no where else to put the error.
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".cleanupConnection",
                                            CommsConstants.SERVERTRANSPORTRECEIVELISTENER_ERROR_02,
                                            this);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    SibTr.debug(this, tc, "Unable to close SI connection", e);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "cleanupConnection");
    }

    /**
     * This method is called in extreme cases where the connection needs to be closed right away
     * without any mucking around. Cases such as this is where the initial handshake has failed.
     * 
     * @param conversation The conversation that needs to be closed down.
     * 
     * @see com.ibm.ws.sib.comms.server.CommonServerReceiveListener#closeConnection(com.ibm.ws.sib.jfapchannel.Conversation)
     */
    @Override
    public void closeConnection(Conversation conversation) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "closeConnection");

        try {
            conversation.close();
        } catch (SIException e) {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rejectHandshake",
                                        CommsConstants.SERVERTRANSPORTRECEIVELISTENER_CLOSECONN_01,
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Unable to close the conversation", e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "closeConnection");
    }

    // Start F201521
    /**
     * This method is called by the JFap channel to give us the oppurtunity to specify which thread
     * the receive listener should dispatch the data on.
     * <p>
     * At this point we can be a bit clever and look at the data to determine if it can be dispatched
     * onto a different thread. If we return null, then data will be dispatched on a per conversation
     * basis.
     * <p>
     * As such, we can allow JFap to execute the following operations on a different thread to that
     * of the Conversation:
     * <ul>
     * <li>If we receive a segment of the following types, we will inform the dispatcher to go
     * to all the active dispatch queues so that it is guarenteed to go behind any other
     * data currently in the queues:
     * <ul>
     * <li>SEG_CLOSE_CONSUMER_SESSION
     * <li>SEG_CLOSE_PRODUCER_SESSION
     * <li>SEG_CLOSE_CONNECTION
     * <li>SEG_CLOSE_ORDER_CONTEXT
     * <li>SEG_STOP_SESS
     * <li>SEG_UNLOCK_ALL
     * </ul>
     * </li>
     * <li>If the packet contains a SEG_XAOPEN or SEG_XARECOVER then conversation ordering will
     * be applied. In the case of XAOPEN - this no longer performs any real processing - so
     * we simply don't care. For SEG_XARECOVER there is no ordering requirement relative to
     * other operations.
     * </li>
     * <li>If the packet is one of the following types, then we will place an entry in the
     * link level state TransactionToDispatchableMap and use the entry to dispatch the
     * data:
     * <ul>
     * <li>SEG_CREATE_UCTRANSACTION</li>
     * <li>SEG_XASTART</li>
     * </ul>
     * This is done so that the creation of the transaction (or in the case of XA start the
     * notification of the resources participation in a global transaction) and subsequent
     * work done in the scope of the transaction (up to the point it is no longer in flight)
     * may be ordered.
     * </li>
     * <li>If the packet contains a transaction Id then data for the same transcation will be
     * dispatched on a seperate receive listener thread. The possible packets are:
     * <ul>
     * <li>SEG_SEND_CONN_MSG</li>
     * <li>SEG_SEND_CONN_MSG_NOREPLY</li>
     * <li>SEG_RECEIVE_CONN_MSG</li>
     * <li>SEG_SEND_SESS_MSG</li>
     * <li>SEG_SEND_SESS_MSG_NOREPLY</li>
     * <li>SEG_RECEIVE_SESS_MSG</li>
     * <li>SEG_DELETE_SET</li>
     * <li>SEG_DELETE_SET_NOREPLY</li>
     * <li>SEG_SEND_TO_EXCEPTION_DEST</li>
     * 
     * </ul>
     * The mechanism used to achive this is to query the link level state's transaction to
     * dispatchable map.
     * </li>
     * <li>The following packets ends the "in-flight" period of a transaction:
     * <ul>
     * <li>SEG_XAEND</li>
     * <li>SEG_XAPREPARE</li>
     * <li>SEG_XACOMMIT</li>
     * <li>SEG_XAROLLBACK</li>
     * <li>SEG_COMMIT_TRANSACTION</li>
     * <li>SEG_ROLLBACK_TRANSACTION</li>
     * </ul>
     * These are ordered using the appropriate entry from the link level state
     * transaction to dispatchable map. They also result in the entry being
     * removed from the map - as there is no longer any requirement to order subsequent
     * transacted operations in relation to this dispatchable.
     * <p>
     * <strong>note:</strong> there are some (unusual) circumstances where the XA
     * varients may not find a dispatchable in the map. Specifically if the transaction
     * was created using a now closed physical connection. This is most likely to occurre
     * during recovery. In these situations the flow is ordered by the connection.
     * </li>
     * <li>The following packets may or may not relate to an "in-flight" transaction:
     * <ul>
     * <li>SEG_XA_GETTXTIMEOUT</li>
     * <li>SEG_XA_SETTXTIMEOUT</li>
     * <li>SEG_XAFORGET</li>
     * <li>SEG_XARECOVER</li>
     * </ul>
     * They are ordered based on the contents of the link level state's transaction to
     * dispatch map. If this does not contain a suitable entry then they are ordered by
     * the connection. This could, in principe, create a problem for the getting and
     * setting of timeouts - however, it turns out that the SIB SIXAResource implementation
     * doesn't support this...
     * </li>
     * <li>If the segment type is a SEG_SEND_CONN_MSG, SEG_SEND_CONN_MSG_NOREPLY,
     * SEG_CREATE_PRODUCER_SESSION or SEG_REGISTER_ASYNC_CONSUMER then the packet may contain
     * a message order context Id. If it does so, dispatching will be done with respect to
     * that.
     * </li>
     * <li>Failing that, null will be returned indicating that the JFap channel should dispatche by
     * Conversation.
     * </li>
     * </ul>
     * <p>
     * Note: It is possible to indicate to the JFap channel that no thread switch should be performed
     * if the segment refers to data that will be actioned extremely quickly. Care should be taken
     * when doing this as the processing of the data will be done on the TCP channel thread, thus
     * blocking the TCP channel giving us more data.
     * <p>
     * To do this, the object com.ibm.ws.sib.jfapchannel.NonThreadSwitchingDispatchable.getInstance()
     * should be returned.
     * 
     * @param conversation The conversation the data is about to be dispatched on.
     * @param data The data about to be dispatched.
     * @param segmentType The segment type of the data.
     * 
     * @return Returns null.
     */
    @Override
    public Dispatchable getThreadContext(Conversation conversation, WsByteBuffer data, int segmentType) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getThreadContext",
                        new Object[]
                        {
                         conversation,
                         data,
                         "" + segmentType
                        });

        Dispatchable dispatchable = null;
        int transactionId = CommsConstants.NO_TRANSACTION;
        int messageOrderId = CommsConstants.NO_ORDER_CONTEXT;

        CommsByteBuffer buffer = poolManager.allocate();
        buffer.reset(data);

        ConversationState convState = (ConversationState) conversation.getAttachment();
        HandshakeProperties handshakeProperties = conversation.getHandshakeProperties();

        // If we are stopping or closing a session or a connection, ensure we dispatch to all the dispatch
        // queues so that it goes behind any other data currently in the queues.
        switch (segmentType) {
            case JFapChannelConstants.SEG_CLOSE_CONSUMER_SESS:
            case JFapChannelConstants.SEG_CLOSE_PRODUCER_SESS:
            case JFapChannelConstants.SEG_CLOSE_CONNECTION:
            case JFapChannelConstants.SEG_CLOSE_ORDER_CONTEXT:
            case JFapChannelConstants.SEG_STOP_SESS:
            case JFapChannelConstants.SEG_UNLOCK_ALL:

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Processing Session stop, close / Unlock All");

                try {
                    dispatchable = DispatchToAllNonEmptyDispatchable.getInstance();
                } catch (Exception e) {
                    // We couldn't find the instance of the DispatchToAllNonEmptyDispatchable. This is
                    // probably a classpath or class loader problem locating the class in JFap impl.
                    FFDCFilter.processException(e, CLASS_NAME + ".getThreadContext",
                                                CommsConstants.SERVERTRANSPORTRECEIVELISTENER_GETTHREAD_01, this);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Unable to create dispatchable", e);

                    throw new SIErrorException(
                                    nls.getFormattedMessage("UNABLE_TO_CREATE_DISPATCH_TO_ALL_SICO2054", null, null),
                                    e);
                }
        }

        if (dispatchable == null) {
            // Now switch on the segment type to obtain the transaction id (if one exists)
            switch (segmentType) {
                case JFapChannelConstants.SEG_CREATE_UCTRANSACTION:
                    TransactionToDispatchableMap map = ((ServerLinkLevelState) conversation.getLinkLevelAttachment()).getDispatchableMap();
                    buffer.getShort(); // Skip the BIT16 ConnectionObjectId
                    int clientTxId = buffer.getInt(); // Get BIT32 Client transaction Id
                    dispatchable = map.addDispatchableForLocalTransaction(clientTxId);
                    break;

                case JFapChannelConstants.SEG_XAOPEN:
                case JFapChannelConstants.SEG_XARECOVER:
                    dispatchable = null;
                    break;

                case JFapChannelConstants.SEG_XASTART:
                    map = ((ServerLinkLevelState) conversation.getLinkLevelAttachment()).getDispatchableMap();
                    int clientXAResourceId = buffer.getInt(); // BIT32 ClientTransactionId
                    Xid xid = buffer.getXid();
                    dispatchable = map.addEnlistedDispatchableForGlobalTransaction(clientXAResourceId,
                                                                                   (XidProxy) xid);

                    if (dispatchable == null) {
                        final SIErrorException exception = new SIErrorException(CommsConstants.SERVERTRANSPORTRECEIVELISTENER_GETTHREAD_02);
                        FFDCFilter.processException(exception, CLASS_NAME + ".getThreadContext",
                                                    CommsConstants.SERVERTRANSPORTRECEIVELISTENER_GETTHREAD_02, new Object[] { map, this });
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                            SibTr.exception(this, tc, exception);
                        throw exception;
                    }
                    break;

                case JFapChannelConstants.SEG_XAPREPARE:
                    map = ((ServerLinkLevelState) conversation.getLinkLevelAttachment()).getDispatchableMap();
                    clientXAResourceId = buffer.getInt(); // BIT32 ClientTransactionId
                    xid = buffer.getXid();
                    dispatchable = map.getDispatchable(clientXAResourceId);
                    break;

                case JFapChannelConstants.SEG_XACOMMIT:
                case JFapChannelConstants.SEG_XAROLLBACK:
                case JFapChannelConstants.SEG_XAFORGET:
                    map = ((ServerLinkLevelState) conversation.getLinkLevelAttachment()).getDispatchableMap();
                    clientXAResourceId = buffer.getInt(); // BIT32 ClientTransactionId
                    xid = buffer.getXid();
                    dispatchable = map.removeDispatchableForGlobalTransaction(clientXAResourceId,
                                                                              (XidProxy) xid);
                    break;

                case JFapChannelConstants.SEG_XAEND:
                    map = ((ServerLinkLevelState) conversation.getLinkLevelAttachment()).getDispatchableMap();
                    clientXAResourceId = buffer.getInt(); // BIT32 ClientTransactionId
                    xid = buffer.getXid();
                    dispatchable = map.getDispatchable(clientXAResourceId);
                    break;

                case JFapChannelConstants.SEG_XA_GETTXTIMEOUT:
                case JFapChannelConstants.SEG_XA_SETTXTIMEOUT:
                    map = ((ServerLinkLevelState) conversation.getLinkLevelAttachment()).getDispatchableMap();
                    clientXAResourceId = buffer.getInt(); // BIT32 ClientTransactionId
                    dispatchable = map.getDispatchable(clientXAResourceId);
                    break;

                case JFapChannelConstants.SEG_SEND_TO_EXCEPTION_DESTINATION:
                case JFapChannelConstants.SEG_SEND_CHUNKED_TO_EXCEPTION_DESTINATION:
                case JFapChannelConstants.SEG_RECEIVE_CONN_MSG:
                case JFapChannelConstants.SEG_INVOKE_COMMAND_WITH_TX:
                    boolean requiresOptimizedTransactions =
                                    (handshakeProperties.getFapLevel() >= JFapChannelConstants.FAP_VERSION_5) &&
                                                    ((handshakeProperties.getCapabilites() & CommsConstants.CAPABILITIY_REQUIRES_OPTIMIZED_TX) != 0);
                    if (requiresOptimizedTransactions) {
                        map = ((ServerLinkLevelState) conversation.getLinkLevelAttachment()).getDispatchableMap();
                        buffer.getShort(); // Skip the BIT16 ConnectionObjectId
                        int txFlags = buffer.getInt(); // Transaction flags
                        buffer.getInt(); // Skip owning conversation Id
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "txFlags=" + txFlags);
                        // Examine the transaction flags to determine if this flow is transacted
                        // or not.
                        if ((txFlags & CommsConstants.OPTIMIZED_TX_FLAGS_TRANSACTED_BIT) == 0) {
                            dispatchable = null;
                        } else {
                            // Flow is transacted - obtain transaction ID.
                            transactionId = buffer.getInt(); // BIT32 transaction id
                            if ((txFlags & CommsConstants.OPTIMIZED_TX_FLAGS_CREATE_BIT) != 0) {
                                // If flags indicate that the transaction should be created by this
                                // operation - then create a new dispatchable.
                                if ((txFlags & CommsConstants.OPTIMIZED_TX_FLAGS_LOCAL_BIT) != 0) {
                                    // local transaction
                                    dispatchable = map.addDispatchableForOptimizedLocalTransaction(transactionId);
                                } else {
                                    // global transaction
                                    if ((txFlags & CommsConstants.OPTIMIZED_TX_END_PREVIOUS_BIT) != 0)
                                        buffer.getInt();
                                    xid = buffer.getXid();
                                    dispatchable = map.addEnlistedDispatchableForGlobalTransaction(transactionId,
                                                                                                   (XidProxy) xid);
                                }
                            } else {
                                // Flags do not indicate that we need to create a new transaction
                                // (and hence allocate a new dispatchable).  Find the existing
                                // dispatchable.
                                dispatchable = map.getDispatchable(transactionId);
                            }
                        }
                    } else {
                        // Code for processing non-optimized (version 6) transacted flows.

                        buffer.getShort(); // Skip the BIT16 ConnectionObjectId
                        clientXAResourceId = buffer.getInt(); // BIT32 ClientTransactionId
                        if (clientXAResourceId == CommsConstants.NO_TRANSACTION) {
                            dispatchable = null;
                        } else {
                            map = ((ServerLinkLevelState) conversation.getLinkLevelAttachment()).getDispatchableMap();
                            dispatchable = map.getDispatchable(clientXAResourceId);
                            if (dispatchable == null) {
                                final SIErrorException exception = new SIErrorException(CommsConstants.SERVERTRANSPORTRECEIVELISTENER_GETTHREAD_03);
                                FFDCFilter.processException(exception, CLASS_NAME + ".getThreadContext",
                                                            CommsConstants.SERVERTRANSPORTRECEIVELISTENER_GETTHREAD_03,
                                                            new Object[] { map, this });
                                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                                    SibTr.exception(this, tc, exception);
                                throw exception;
                            }
                        }
                    }
                    break;

                case JFapChannelConstants.SEG_SEND_CONN_MSG:
                case JFapChannelConstants.SEG_SEND_CONN_MSG_NOREPLY:
                case JFapChannelConstants.SEG_SEND_SESS_MSG:
                case JFapChannelConstants.SEG_SEND_SESS_MSG_NOREPLY:
                case JFapChannelConstants.SEG_RECEIVE_SESS_MSG:
                case JFapChannelConstants.SEG_DELETE_SET:
                case JFapChannelConstants.SEG_DELETE_SET_NOREPLY:
                case JFapChannelConstants.SEG_READ_AND_DELETE_SET:
                case JFapChannelConstants.SEG_SEND_CHUNKED_SESS_MSG:
                case JFapChannelConstants.SEG_SEND_CHUNKED_SESS_MSG_NOREPLY:
                case JFapChannelConstants.SEG_SEND_CHUNKED_CONN_MSG:
                case JFapChannelConstants.SEG_SEND_CHUNKED_CONN_MSG_NOREPLY:
                    requiresOptimizedTransactions =
                                    (handshakeProperties.getFapLevel() >= JFapChannelConstants.FAP_VERSION_5) &&
                                                    ((handshakeProperties.getCapabilites() & CommsConstants.CAPABILITIY_REQUIRES_OPTIMIZED_TX) != 0);
                    if (requiresOptimizedTransactions) {
                        map = ((ServerLinkLevelState) conversation.getLinkLevelAttachment()).getDispatchableMap();
//                     data.getShort();                 // Skip the BIT16 ConnectionObjectId
//                     data.getShort();                 // Skip the BIT16 MOId / Producer Id / Consumer Id
                        buffer.getInt(); // skip both BIT16 ConnectionObjectId and BIT16 MOId/ProducerId/ConsumerId
                        int txFlags = buffer.getInt(); // Transaction flags
                        buffer.getInt(); // Skip "owning" conversation id
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "txFlags=" + txFlags);

                        // Examine the transaction flags to determine if this flow is transacted
                        // or not.
                        if ((txFlags & CommsConstants.OPTIMIZED_TX_FLAGS_TRANSACTED_BIT) == 0) {
                            dispatchable = null;
                        } else {
                            // Flow is transacted - obtain transaction ID.
                            transactionId = buffer.getInt(); // Get the BIT32 Transaction Id
                            if ((txFlags & CommsConstants.OPTIMIZED_TX_FLAGS_CREATE_BIT) != 0) {
                                // If flags indicate that the transaction should be created by this
                                // operation - then create a new dispatchable.
                                if ((txFlags & CommsConstants.OPTIMIZED_TX_FLAGS_LOCAL_BIT) != 0) {
                                    // local transaction
                                    dispatchable = map.addDispatchableForOptimizedLocalTransaction(transactionId);
                                } else {
                                    // global transaction
                                    if ((txFlags & CommsConstants.OPTIMIZED_TX_END_PREVIOUS_BIT) != 0)
                                        buffer.getInt();
                                    xid = buffer.getXid();
                                    dispatchable = map.addEnlistedDispatchableForGlobalTransaction(transactionId,
                                                                                                   (XidProxy) xid);
                                }
                            } else {
                                // Flags do not indicate that we need to create a new transaction
                                // (and hence allocate a new dispatchable).  Find the existing
                                // dispatchable.
                                dispatchable = map.getDispatchable(transactionId);
                            }
                        }
                    } else {
                        // Code for processing non-optimized (version 6) transacted flows.
//                     data.getShort();                 // Skip the BIT16 ConnectionObjectId
//                     data.getShort();                 // Skip the BIT16 MOId / Producer Id / Consumer Id
                        buffer.getInt(); // skip both BIT16 ConnectionObjectId and BIT16 MOId/ProducerId/ConsumerId
                        transactionId = buffer.getInt(); // Get the BIT32 Transaction Id
                        if (transactionId == CommsConstants.NO_TRANSACTION) {
                            dispatchable = null;
                        } else {
                            map = ((ServerLinkLevelState) conversation.getLinkLevelAttachment()).getDispatchableMap();
                            dispatchable = map.getDispatchable(transactionId);

                            // PK91199 It is possible for our transaction not to be in our map,
                            // if it was joined to transaction owned by a connection that has now been closed.
                            // Throwing a runtime exception here would bring down the whole socket, causing
                            // all conversations to be terminated.
                            // It is better (as is the case in V6.1 with optimized txns) to continue
                            // with a default dispatchable.
                            // If we're in a JFAPSend (rather than a JFAPExchange) this means we silently
                            // swallow the XAER_NOTA exception. However, we will throw that exception
                            // when they later attempt to commit the transaction.
                        }
                    }
                    break;

                case JFapChannelConstants.SEG_COMMIT_TRANSACTION:
                case JFapChannelConstants.SEG_ROLLBACK_TRANSACTION:
                    buffer.getShort(); // Skip the BIT16 ConnectionObjectId
                    transactionId = buffer.getInt(); // Get the BIT32 Transaction Id
                    if (transactionId == CommsConstants.NO_TRANSACTION) {
                        dispatchable = null;
                    } else {
                        map = ((ServerLinkLevelState) conversation.getLinkLevelAttachment()).getDispatchableMap();
                        dispatchable = map.removeDispatchableForLocalTransaction(transactionId);
                        if (dispatchable == null) {
                            final SIErrorException exception = new SIErrorException(CommsConstants.SERVERTRANSPORTRECEIVELISTENER_GETTHREAD_05);
                            FFDCFilter.processException(exception, CLASS_NAME + ".getThreadContext",
                                                        CommsConstants.SERVERTRANSPORTRECEIVELISTENER_GETTHREAD_05,
                                                        new Object[] { map, this });
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                                SibTr.exception(this, tc, exception);
                            throw exception;
                        }
                    }
                    break;
            }

            // If we found a transaction - use that above all else
            // Otherwise try for a MessageOrderContextId
            if (dispatchable == null) {
                buffer.rewind();

                switch (segmentType) {
                    case JFapChannelConstants.SEG_SEND_CONN_MSG:
                    case JFapChannelConstants.SEG_SEND_CONN_MSG_NOREPLY:
                    case JFapChannelConstants.SEG_CREATE_PRODUCER_SESS:
                        buffer.getShort(); // Skip the BIT16 ConnectionObjectId
                        messageOrderId = buffer.getShort(); // Get the BIT16 MessageOrderId
                        break;

                    case JFapChannelConstants.SEG_REGISTER_ASYNC_CONSUMER:
                        buffer.getShort(); // Skip the BIT16 ConnectionObjectId
                        buffer.getShort(); // Skip the BIT16 ConsumerId
                        messageOrderId = buffer.getShort(); // Get the BIT16 MessageOrderId
                        break;
                }

                // If we found a message order, use that - otherwise drop out and return null
                if (messageOrderId != CommsConstants.NO_ORDER_CONTEXT) {
                    dispatchable = (Dispatchable) convState.getObject(messageOrderId);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getThreadContext", dispatchable);
        return dispatchable;
    }

    /**
     * Pass information from TRM on the client to TRM on the server. This will only be called by
     * comms when it receives a segment indicating that TRM has passed a WsByteBuffer of information
     * to be passed along to TRM.
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param allocatedFromBufferPool
     * @param partOfExchange
     */
    private void rcvTRMExchange(CommsByteBuffer request, Conversation conversation, int requestNumber,
                                boolean allocatedFromBufferPool, boolean partOfExchange) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "rcvTRMExchange",
                        new Object[]
                        {
                         request,
                         conversation,
                         Integer.valueOf(requestNumber),
                         Boolean.valueOf(allocatedFromBufferPool),
                         Boolean.valueOf(partOfExchange)
                        });

        ConversationState convState = (ConversationState) conversation.getAttachment();

        TrmSingleton trm = TrmSingleton.getTrmSingleton();
        ComponentData cd = (ComponentData) trm.getComponentData();
        ClientConnection cc = (ClientConnection) convState.getCommsConnection();

        // There is a chance that we are the first conversation on a connection
        // and the ServerSideConnection will have already been created. If this is the
        // case use it.
        if (convState.getCommsConnection() == null) {
            cc = new ServerSideConnection(conversation);
            convState.setCommsConnection(cc);
        }

        try {
            byte[] trmRequestData = request.getRemaining();
            byte[] trmReplyData = cd.handShake(cc, trmRequestData);

            // Only save info about the connection if we got a connection
            if (cc.getSICoreConnection() != null) {

                // First ensure inform MP to stop making lazy copies of messages
                ((MPCoreConnection) cc.getSICoreConnection()).setMessageCopiedWhenSent(false);
                ((MPCoreConnection) cc.getSICoreConnection()).setMessageCopiedWhenReceived(false);

                // Add the connection object to the store
                convState.addObject(cc);

                // Send info of the connection to the client
                sendConnectionInfo(conversation);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "TRM did not allocate us a connection");
            }

            CommsByteBuffer reply = poolManager.allocate();
            reply.wrap(trmReplyData);

            conversation.send(reply,
                              JFapChannelConstants.SEG_TOPOLOGY,
                              requestNumber,
                              JFapChannelConstants.PRIORITY_HANDSHAKE,
                              true,
                              ThrottlingPolicy.BLOCK_THREAD,
                              null);
        } catch (ConversationStateFullException e) {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvTRMExchange",
                                        CommsConstants.SERVERTRANSPORTRECEIVELISTENER_TRMEXCG_01,
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.SERVERTRANSPORTRECEIVELISTENER_TRMEXCG_01,
                                                  conversation, requestNumber);
        } catch (SIException e) {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvTRMExchange",
                                        CommsConstants.SERVERTRANSPORTRECEIVELISTENER_TRMEXCG_02,
                                        this);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2019", e);
        }

        request.release(allocatedFromBufferPool);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "rcvTRMExchange");
    }

    /**
     * Pass information from MFP on the client to MFP on the server. This will only be called by
     * comms when it receives a segment indicating that MFP has passed a WsByteBuffer of information
     * to be passed along to MFP.
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param allocatedFromBufferPool
     * @param partOfExchange
     */
    private void rcvMFPExchange(CommsByteBuffer request, Conversation conversation, int requestNumber,
                                boolean allocatedFromBufferPool, boolean partOfExchange) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "rcvMFPExchange",
                        new Object[]
                        {
                         request,
                         conversation,
                         Integer.valueOf(requestNumber),
                         Boolean.valueOf(allocatedFromBufferPool),
                         Boolean.valueOf(partOfExchange)
                        });

        CompHandshake ch = null;
        ConversationState convState = (ConversationState) conversation.getAttachment();

        // Store this connection away for MFP's benefit
        ClientConnection cc = new ServerSideConnection(conversation);
        convState.setCommsConnection(cc);

        // Get MFP Singleton
        try {
            ch = (CompHandshake) CompHandshakeFactory.getInstance();

            byte[] mfpRequestData = request.getRemaining();

            // Get hold of product version
            HandshakeProperties handshakeGroup = conversation.getHandshakeProperties();
            int productVersion = handshakeGroup.getMajorVersion();

            byte[] mfpReplyData = ch.compHandshakeData(cc, productVersion, mfpRequestData);

            CommsByteBuffer reply = poolManager.allocate();
            reply.wrap(mfpReplyData);

            conversation.send(reply,
                              JFapChannelConstants.SEG_MESSAGE_FORMAT_INFO,
                              requestNumber,
                              JFapChannelConstants.PRIORITY_HANDSHAKE,
                              true,
                              ThrottlingPolicy.BLOCK_THREAD,
                              null);
        } catch (SIException e) {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvMFPExchange",
                                        CommsConstants.SERVERTRANSPORTRECEIVELISTENER_MFPEXCG_02,
                                        this);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2019", e);
        } catch (Exception e1) {
            FFDCFilter.processException(e1, CLASS_NAME + ".rcvMFPExchange",
                                        CommsConstants.SERVERTRANSPORTRECEIVELISTENER_MFPEXCG_01, this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "MFP unable to create CompHandshake Singleton", e1);

            StaticCATHelper.sendExceptionToClient(e1,
                                                  CommsConstants.SERVERTRANSPORTRECEIVELISTENER_MFPEXCG_01,
                                                  conversation, requestNumber);
        }

        request.release(allocatedFromBufferPool);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "rcvMFPExchange");
    }

    /**
     * Pass information from MFP on the client to MFP on the server. This will only be called by
     * comms when it receives a segment indicating that MFP has passed a WsByteBuffer of information
     * to be passed along to MFP.
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param allocatedFromBufferPool
     * @param partOfExchange
     */
    private void rcvMFPSchema(CommsByteBuffer request, Conversation conversation, int requestNumber,
                              boolean allocatedFromBufferPool, boolean partOfExchange) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "rcvMFPSchema",
                        new Object[]
                        {
                         request,
                         conversation,
                         Integer.valueOf(requestNumber),
                         Boolean.valueOf(allocatedFromBufferPool),
                         Boolean.valueOf(partOfExchange)
                        });

        CompHandshake ch = null;
        ConversationState convState = (ConversationState) conversation.getAttachment();

        CommsConnection cc = convState.getCommsConnection();

        // Get MFP Singleton
        try {
            ch = (CompHandshake) CompHandshakeFactory.getInstance();

            byte[] mfpRequestData = request.getRemaining();

            // Get hold of product version
            HandshakeProperties handshakeGroup = conversation.getHandshakeProperties();
            int productVersion = handshakeGroup.getMajorVersion();

            // Give MFP received Schema
            ch.compData(cc, productVersion, mfpRequestData);
        } catch (Exception e1) {
            FFDCFilter.processException(e1, CLASS_NAME + ".rcvMFPSchema",
                                        CommsConstants.SERVERTRANSPORTRECEIVELISTENER_MFPSCHEMA_01, this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "MFP unable to create CompHandshake Singleton", e1);
        }

        request.release(allocatedFromBufferPool);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "rcvMFPSchema");
    }

    /**
     * This gets called when the server receives a request from the client about an MFP schema. In
     * this case we pass the call off to MFP and return any result they give us.
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param segmentId
     * @param allocatedFromBufferPool
     * @param partOfExchange
     */
    private void rcvMFPRequestSchema(CommsByteBuffer request, Conversation conversation, int requestNumber,
                                     int segmentId, boolean allocatedFromBufferPool,
                                     boolean partOfExchange) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "rcvMFPRequestSchema",
                        new Object[]
                        {
                         request,
                         conversation,
                         Integer.valueOf(requestNumber),
                         Boolean.valueOf(allocatedFromBufferPool),
                         Boolean.valueOf(partOfExchange)
                        });

        CompHandshake ch = null;
        ConversationState convState = (ConversationState) conversation.getAttachment();
        CommsConnection cc = convState.getCommsConnection();

        try {
            // Get MFP Singleton
            ch = (CompHandshake) CompHandshakeFactory.getInstance();

            byte[] mfpRequestData = request.getRemaining();

            // Get hold of product version
            HandshakeProperties handshakeGroup = conversation.getHandshakeProperties();
            int productVersion = handshakeGroup.getMajorVersion();

            // Give MFP received Schema
            byte[] mfpReplyData = ch.compRequest(cc, productVersion, segmentId, mfpRequestData);

            if (mfpReplyData == null) {
                // Oops something went wrong in MFP
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "MFP returned null");

                SIErrorException e = new SIErrorException(
                                nls.getFormattedMessage("MFP_SCHEMA_REQUEST_FAILED_SICO2056", null, null) // D256974
                );

                FFDCFilter.processException(e, CLASS_NAME + ".rcvMFPRequestSchema",
                                            CommsConstants.SERVERTRANSPORTRECEIVELISTENER_MFPREQSCH_01,
                                            this);

                StaticCATHelper.sendExceptionToClient(e, null, conversation, requestNumber);
            } else {
                // Otherwise send the data back for MFP
                CommsByteBuffer reply = poolManager.allocate();
                reply.wrap(mfpReplyData);

                try {
                    conversation.send(reply,
                                      JFapChannelConstants.SEG_REQUEST_SCHEMA_R,
                                      requestNumber,
                                      JFapChannelConstants.PRIORITY_HIGHEST,
                                      true,
                                      ThrottlingPolicy.BLOCK_THREAD,
                                      null);
                } catch (SIException e) {
                    FFDCFilter.processException(e,
                                                CLASS_NAME + ".rcvMFPRequestSchema",
                                                CommsConstants.SERVERTRANSPORTRECEIVELISTENER_MFPREQSCH_02,
                                                this);

                    SibTr.error(tc, "COMMUNICATION_ERROR_SICO2019", e);
                }
            }
        } catch (Exception e) {
            FFDCFilter.processException(e, CLASS_NAME + ".rcvMFPRequestSchema",
                                        CommsConstants.SERVERTRANSPORTRECEIVELISTENER_MFPREQSCH_03,
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "MFP unable to create CompHandshake Singleton", e);
        }

        request.release(allocatedFromBufferPool);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "rcvMFPRequestSchema");
    }

    /**
     * This method will send back a connection information flow to the peer.
     * This method should be invoked directly before a TRM reply is sent to
     * the peer, as we are going to need to provide this information when TRM
     * get their response.
     * <p>
     * In this reply we send back the object ID on the server of the SICoreConnection
     * object and the ME Name.
     * 
     * @param conversation
     */
    private void sendConnectionInfo(Conversation conversation) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "sendConnectionInfo", conversation);

        ConversationState convState = (ConversationState) conversation.getAttachment();
        // Get a handle on the client connection object which will have an
        // SICoreConnection object set into it by TRM. Since this is the initial
        // object created, this we be at the object table origin (the 1st place)
        ClientConnection clientConnection = (ClientConnection) convState.getObject(ConversationState.OBJECT_TABLE_ORIGIN);
        SICoreConnection conn = null;

        try {
            // Now get the SICoreConnection
            conn = clientConnection.getSICoreConnection();
            short connectionObjectId = 0;
            byte[] uniqueId = new byte[0];
            String meName = null;
            String meUuid = null;
            String resolvedUserId = null;

            try {
                // Get the first unique Id
                uniqueId = conn.createUniqueId();

                CATConnection catConnection = new CATConnection(conn);
                connectionObjectId = (short) convState.addObject(catConnection);
                convState.setConnectionObjectId(connectionObjectId);

                meName = conn.getMeName();
                meUuid = conn.getMeUuid();
                resolvedUserId = conn.getResolvedUserid();
            } catch (SIException e) {
                // If we get here, do not worry so much. Something has obviously gone
                // wrong, but this is not a good time to flag it. So log it and return null
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".sendConnectionInfo",
                                            CommsConstants.SERVERTRANSPORTRECEIVELISTENER_CONNGET_04,
                                            this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Unable to get the unique ID", e);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                SibTr.debug(this, tc, "SICoreConnection Id:", Short.valueOf(connectionObjectId));
                SibTr.debug(this, tc, "ME Name:", meName);
                SibTr.debug(this, tc, "ME UUId:", meUuid);
                SibTr.debug(this, tc, "Resolved User Id:", resolvedUserId);
            }

            CommsByteBuffer reply = poolManager.allocate();
            reply.putShort(connectionObjectId);
            reply.putString(meName);
            reply.putShort(uniqueId.length);
            reply.put(uniqueId);
            reply.putString(meUuid);
            reply.putString(resolvedUserId);

            conversation.send(reply,
                              JFapChannelConstants.SEG_CONNECTION_INFO,
                              0,
                              JFapChannelConstants.PRIORITY_HIGH,
                              true,
                              ThrottlingPolicy.BLOCK_THREAD,
                              null);
        } catch (SIException e) {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".sendConnectionInfo",
                                        CommsConstants.SERVERTRANSPORTRECEIVELISTENER_CONNGET_02,
                                        this);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2019", e);
        } catch (ConversationStateFullException e) {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".sendConnectionInfo",
                                        CommsConstants.SERVERTRANSPORTRECEIVELISTENER_CONNGET_03,
                                        this);

            SibTr.error(tc, "INTERNAL_OBJECT_STORE_FULL_SICO2010", e);
        }

        // Now only attach the connection listener for this connection when the info for this
        // connection has been sent to the client.
        attachConnectionListener(conversation, conn);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "sendConnectionInfo"); //173660
    }

    /**
     * Helper method used to register the SICoreConnection with the connection listener.
     * 
     * @param conversation
     * @param conn
     */
    private void attachConnectionListener(Conversation conversation, SICoreConnection conn) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "attachConnectionListener",
                        new Object[]
                        {
                         conversation,
                         conn
                        });

        // The connection listener is saved in the link level attachement so that each connection that
        // uses the same physical link uses the same connection listener

        // So get the connection listener from the link level state and register ourselves with it
        ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment(); //d173544
        ServerSICoreConnectionListener listener = linkState.getSICoreConnectionListener();

        listener.addSICoreConnection(conn, conversation);
        // Finally attach it to the actual core connection
        // Start f173765.2
        try {
            conn.addConnectionListener(listener);
        } catch (SIException e) {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".attachConnectionListener",
                                        CommsConstants.SERVERTRANSPORTRECEIVELISTENER_CONNGET_01,
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Unable to register connection listener", e);
        }
        // End f173765.2

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "attachConnectionListener");
    }

    // End D208070

    // Start F202571
    /**
     * This method processes a direct connection request. Such requests generally come in from
     * non-Java or non-Jetstream clients who wish to connect to a messaging engine but are unable to
     * or do not wish to perform TRM handshaking. As such, a direct connection request must be made
     * to a specific bus or ME and no attempts to redirect will be made even if the target bus / ME
     * knows the route to the requested engine.
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param allocatedFromBufferPool
     * @param partOfExchange
     */
    private void rcvDirectConnect(CommsByteBuffer request, Conversation conversation, int requestNumber,
                                  boolean allocatedFromBufferPool, boolean partOfExchange) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "rcvDirectConnect",
                        new Object[]
                        {
                         request,
                         conversation,
                         Integer.valueOf(requestNumber),
                         Boolean.valueOf(allocatedFromBufferPool),
                         Boolean.valueOf(partOfExchange)
                        });

        ConversationState convState = (ConversationState) conversation.getAttachment();

        /**************************************************************/
        /* ME Name */
        /**************************************************************/
        String meName = request.getString();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "Me Name", meName);

        /**************************************************************/
        /* Bus Name */
        /**************************************************************/
        String busName = request.getString();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "Bus Name", busName);

        /**************************************************************/
        /* User Id */
        /**************************************************************/
        String userId = request.getString();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "User Id", userId);

        /**************************************************************/
        /* Password */
        /**************************************************************/
        String password = request.getString();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "Password", "****");

        //Liberty COMMS TODO
        // Has to be replaced with proper security code
        // For now just have some dummy subject as this flow would not get called in any of current functionality
        // First authenticate the user

        /*
         * SibLoginFactory loginFactory = SibLoginFactory.getInstance();
         * final ConnectionMetaData loginMetaData = new ConnectionMetaDataImpl(conversation.getMetaData(), null);
         * Subject subject = loginFactory.createNewSibLogin().login(busName,
         * userId,
         * password,
         * loginMetaData);
         */
        Subject subject = new Subject();

        if (subject == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Authentication failed");

            StaticCATHelper.sendExceptionToClient(new SIAuthenticationException(null),
                                                  null,
                                                  conversation,
                                                  requestNumber);
        } else {
            // Now invoke the Direct connection method
            TrmSingleton trmSingleton = TrmSingleton.getTrmSingleton();
            ComponentData componentData = (ComponentData) trmSingleton.getComponentData();
            ConnectionMetaDataImpl metaData =
                            new ConnectionMetaDataImpl(conversation.getMetaData(), conversation.getHandshakeProperties());
            DirectConnectionImpl connProps = new DirectConnectionImpl(metaData);
            connProps.setBus(busName);
            connProps.setName(meName);

            boolean rc = componentData.directConnect(connProps, subject);
            SICoreConnection conn = connProps.getSICoreConnection();

            if (!rc || conn == null) {
                // TRM failed to locate an ME or Bus for us
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "TRM did not allocate us a connection");

                StaticCATHelper.sendExceptionToClient(new SIResourceException(),
                                                      null,
                                                      conversation,
                                                      requestNumber);
            } else {
                try {
                    // Otherwise we got a connection ok, so stash it in the object store
                    ServerSideConnection cc = new ServerSideConnection(conversation);
                    convState.setCommsConnection(cc);
                    convState.addObject(cc);

                    // Save the SICoreConnection
                    short objId = (short) convState.addObject(new CATConnection(conn));
                    convState.setConnectionObjectId(objId);

                    cc.setSICoreConnection(conn);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "SICoreConnection object ID", "" + objId);

                    // Register the connection listener
                    attachConnectionListener(conversation, conn);

                    // Also grab the unique message id stem
                    byte[] idStem = conn.createUniqueId();

                    // Now send back a reply
                    CommsByteBuffer reply = poolManager.allocate();
                    reply.putShort(objId);
                    reply.putShort((short) idStem.length);
                    reply.put(idStem);

                    try {
                        conversation.send(reply,
                                          JFapChannelConstants.SEG_DIRECT_CONNECT_R,
                                          requestNumber,
                                          JFapChannelConstants.PRIORITY_MEDIUM,
                                          true,
                                          ThrottlingPolicy.BLOCK_THREAD,
                                          null);
                    } catch (SIException e) {
                        FFDCFilter.processException(e,
                                                    CLASS_NAME + ".,rcvDirectConnect",
                                                    CommsConstants.SERVERTRANSPORTRECEIVELISTENER_DIRECTCN_02,
                                                    this);

                        SibTr.error(tc, "COMMUNICATION_ERROR_SICO2019", e);
                    }
                } catch (ConversationStateFullException e) {
                    FFDCFilter.processException(e,
                                                CLASS_NAME + ".,rcvDirectConnect",
                                                CommsConstants.SERVERTRANSPORTRECEIVELISTENER_DIRECTCN_01,
                                                this);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, e.getMessage(), e);

                    StaticCATHelper.sendExceptionToClient(e,
                                                          CommsConstants.SERVERTRANSPORTRECEIVELISTENER_DIRECTCN_01,
                                                          conversation, requestNumber);
                } catch (SIException e) {
                    FFDCFilter.processException(e,
                                                CLASS_NAME + ".,rcvDirectConnect",
                                                CommsConstants.SERVERTRANSPORTRECEIVELISTENER_DIRECTCN_03,
                                                this);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, e.getMessage(), e);

                    StaticCATHelper.sendExceptionToClient(e,
                                                          CommsConstants.SERVERTRANSPORTRECEIVELISTENER_DIRECTCN_03,
                                                          conversation, requestNumber);
                }
            }
        }

        request.release(allocatedFromBufferPool);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "rcvDirectConnect");
    }
}
