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

import java.io.UnsupportedEncodingException;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.common.CommsUtils;
import com.ibm.ws.sib.comms.server.CommsServerByteBuffer;
import com.ibm.ws.sib.comms.server.ConversationState;
import com.ibm.ws.sib.comms.server.IdToTransactionTable;
import com.ibm.ws.sib.comms.server.ServerLinkLevelState;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.HandshakeProperties;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.Conversation.ThrottlingPolicy;
import com.ibm.ws.sib.mfp.IncorrectMessageTypeException;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageCopyFailedException;
import com.ibm.ws.sib.mfp.MessageEncodeFailedException;
import com.ibm.ws.sib.processor.MPConsumerSession;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.LockedMessageEnumeration;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.StoppableAsynchConsumerCallback;

/**
 * <p>Class to implement the asynchronous consumer verbs from the client.
 * 
 * <p>This class is used when the client has set an asynchronous consumer
 * but has not specified read ahead on the CreateConsumer verb
 * (@see com.ibm.ws.sib.processor.ConsumerSession). This class will register
 * an asynchronous consumer with the MP and send messages to the client
 * as they arrive. Note that each LoeckedMessageEnumeration is passed to
 * the client, the async consumer is then stopped until it is started
 * again by the client.
 */
public class CATAsynchConsumer extends CATConsumer implements StoppableAsynchConsumerCallback //SIB0115d.comms
{
    /** Class name for FFDC's */
    private static String CLASS_NAME = CATAsynchConsumer.class.getName();

    /** Trace */
    private static final TraceComponent tc = SibTr.register(CATAsynchConsumer.class,
                                                            CommsConstants.MSG_GROUP,
                                                            CommsConstants.MSG_BUNDLE);

    /** Log source info on class load */
    static {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(
                        tc,
                        "Source info: @(#) SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/clientsupport/CATAsynchConsumer.java, SIB.comms, WASX.SIB, aa1225.01 1.99");
    }

    /** Reference to our main consumer */
    private final CATMainConsumer mainConsumer;

    /**
     * Constructor which is passed the reference to the 'main' consumer object.
     * 
     * @param mainConsumer
     */
    public CATAsynchConsumer(CATMainConsumer mainConsumer) {
        super();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", mainConsumer);
        this.mainConsumer = mainConsumer;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>");
    }

    /**
     * @return Returns the actual SI ConsumerSession
     */
    @Override
    protected ConsumerSession getConsumerSession() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getConsumerSession");
        ConsumerSession sess = mainConsumer.getConsumerSession();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getConsumerSession", sess);
        return sess;
    }

    /**
     * @return Returns the conversation.
     */
    @Override
    protected Conversation getConversation() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getConversation");
        Conversation conv = mainConsumer.getConversation();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getConversation", conv);
        return conv;
    }

    /**
     * @return Returns the session lowest priority.
     */
    @Override
    protected int getLowestPriority() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getLowestPriority");
        int lowestPri = mainConsumer.getLowestPriority();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getLowestPriority", lowestPri);
        return lowestPri;
    }

    /**
     * @return Returns the client session Id.
     */
    @Override
    protected short getClientSessionId() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getClientSessionId");
        short sessId = mainConsumer.getClientSessionId();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getClientSessionId");
        return sessId;
    }

    /**
     * @return Returns the sessions unrecoverable reliability.
     */
    @Override
    protected Reliability getUnrecoverableReliability() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getUnrecoverableReliability");
        Reliability rel = mainConsumer.getUnrecoverableReliability();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getUnrecoverableReliability");
        return rel;
    }

    // SIB0115d.comms start

    /**
     * Creates a normal or stoppable async consumer for this session. This is called in response to
     * the request from a client. This differs from readahead or synchronous sessions where an async
     * callback is registered without the client knowing.
     * 
     * @param requestNumber
     * @param maxActiveMessages
     * @param messageLockExpiry
     * @param batchsize
     * @param orderContext
     * @param stoppable
     * @param maxSequentialFailures
     * @param hiddenMessageDelay
     */
    @Override
    public void setAsynchConsumerCallback(int requestNumber,
                                          int maxActiveMessages,
                                          long messageLockExpiry,
                                          int batchsize,
                                          OrderingContext orderContext,
                                          boolean stoppable,
                                          int maxSequentialFailures,
                                          long hiddenMessageDelay) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setAsynchConsumerCallback",
                                           new Object[]
                                           {
                                              requestNumber,
                                              maxActiveMessages,
                                              messageLockExpiry,
                                              batchsize,
                                              orderContext,
                                              stoppable,
                                              maxSequentialFailures,
                                              hiddenMessageDelay
                                           });

        try {
            // Here we need to examine the config parameter that will denote whether we are telling
            // MP to inline our async callbacks or not. We will default to false, but this can
            // be overrideen.
            boolean inlineCallbacks =
                            CommsUtils.getRuntimeBooleanProperty(CommsConstants.INLINE_ASYNC_CBACKS_KEY,
                                                                 CommsConstants.INLINE_ASYNC_CBACKS);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Inline async callbacks: " + inlineCallbacks);

            MPConsumerSession session = (MPConsumerSession) getConsumerSession();

            if (stoppable) {
                session.registerStoppableAsynchConsumerCallback(this,
                                                                maxActiveMessages,
                                                                messageLockExpiry,
                                                                batchsize,
                                                                getUnrecoverableReliability(),
                                                                inlineCallbacks,
                                                                orderContext,
                                                                maxSequentialFailures,
                                                                hiddenMessageDelay);
            } else {
                session.registerAsynchConsumerCallback(this,
                                                       maxActiveMessages,
                                                       messageLockExpiry,
                                                       batchsize,
                                                       getUnrecoverableReliability(),
                                                       inlineCallbacks,
                                                       orderContext);
            }

            try {
                getConversation().send(poolManager.allocate(),
                                       JFapChannelConstants.SEG_REGISTER_ASYNC_CONSUMER_R,
                                       requestNumber,
                                       JFapChannelConstants.PRIORITY_MEDIUM,
                                       true,
                                       ThrottlingPolicy.BLOCK_THREAD,
                                       null);
            } catch (SIException e) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".setAsynchConsumerCallback",
                                            CommsConstants.CATASYNCHCONSUMER_SETCALLBACK_01,
                                            this);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, e.getMessage(), e);

                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2017", e);
            }
            // End d175222
        } catch (SIException e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!((ConversationState) getConversation().getAttachment()).hasMETerminated()) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".setAsynchConsumerCallback",
                                            CommsConstants.CATASYNCHCONSUMER_SETCALLBACK_02,
                                            this);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.CATASYNCHCONSUMER_SETCALLBACK_02,
                                                  getConversation(), requestNumber);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setAsynchConsumerCallback");
    }

    // SIB0115d.comms end

    /**
     * This method will unlock a set of locked messages that have been delivered to
     * us (the server) which we have then passed on to the client.
     * 
     * @param requestNumber The request number that replies should be sent with.
     * @param msgHandles The array of message id's that should be unlocked.
     * @param reply Whether this will demand a reply.
     */
    @Override
    public void unlockSet(int requestNumber, SIMessageHandle[] msgHandles, boolean reply) // f199593, F219476.2
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "unlockSet",
                                           new Object[]
                                           {
                                             requestNumber,
                                             msgHandles,
                                             reply
                                           });

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "Request to unlock " + msgHandles.length +
                                                     " message(s)");

        try {
            getConsumerSession().unlockSet(msgHandles);

            if (reply) {
                try {
                    getConversation().send(poolManager.allocate(),
                                           JFapChannelConstants.SEG_UNLOCK_SET_R,
                                           requestNumber,
                                           JFapChannelConstants.PRIORITY_MEDIUM,
                                           true,
                                           ThrottlingPolicy.BLOCK_THREAD,
                                           null);
                } catch (SIException e) {
                    FFDCFilter.processException(e,
                                                CLASS_NAME + ".unlockSet",
                                                CommsConstants.CATASYNCHCONSUMER_UNLOCKSET_02,
                                                this);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, e.getMessage(), e);

                    SibTr.error(tc, "COMMUNICATION_ERROR_SICO2017", e);
                }
            }
        } catch (SIException e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!((ConversationState) getConversation().getAttachment()).hasMETerminated()) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".unlockSet",
                                            CommsConstants.CATASYNCHCONSUMER_UNLOCKSET_01,
                                            this);
            }

            if (reply) {
                StaticCATHelper.sendExceptionToClient(e,
                                                      CommsConstants.CATASYNCHCONSUMER_UNLOCKSET_01,
                                                      getConversation(),
                                                      requestNumber);
            } else {
                SibTr.error(tc, "UNABLE_TO_UNLOCK_MSGS_SICO2002", e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "unlockSet");
    }

    /**
     * This method will unlock a set of locked messages that have been delivered to
     * us (the server) which we have then passed on to the client.
     * 
     * @param requestNumber The request number that replies should be sent with.
     * @param msgIds The array of message id's that should be unlocked.
     * @param reply Whether this will demand a reply.
     * @param incrementLockCount Indicates whether the lock count should be incremented for this unlock
     */
    @Override
    public void unlockSet(int requestNumber, SIMessageHandle[] msgHandles, boolean reply, boolean incrementLockCount) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "unlockSet",
                                           new Object[]
                                           {
                                             requestNumber,
                                             msgHandles,
                                             reply,
                                             incrementLockCount
                                           });

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "Request to unlock " + msgHandles.length +
                                                     " message(s)");

        try {
            getConsumerSession().unlockSet(msgHandles, incrementLockCount);

            if (reply) {
                try {
                    getConversation().send(poolManager.allocate(),
                                           JFapChannelConstants.SEG_UNLOCK_SET_R,
                                           requestNumber,
                                           JFapChannelConstants.PRIORITY_MEDIUM,
                                           true,
                                           ThrottlingPolicy.BLOCK_THREAD,
                                           null);
                } catch (SIException e) {
                    FFDCFilter.processException(e,
                                                CLASS_NAME + ".unlockSet",
                                                CommsConstants.CATASYNCHCONSUMER_UNLOCKSET_04,
                                                this);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, e.getMessage(), e);

                    SibTr.error(tc, "COMMUNICATION_ERROR_SICO2017", e);
                }
            }
        } catch (SIException e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!((ConversationState) getConversation().getAttachment()).hasMETerminated()) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".unlockSet",
                                            CommsConstants.CATASYNCHCONSUMER_UNLOCKSET_03,
                                            this);
            }

            if (reply) {
                StaticCATHelper.sendExceptionToClient(e,
                                                      CommsConstants.CATASYNCHCONSUMER_UNLOCKSET_03,
                                                      getConversation(),
                                                      requestNumber);
            } else {
                SibTr.error(tc, "UNABLE_TO_UNLOCK_MSGS_SICO2002", e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "unlockSet");
    }

    /**
     * This method will inform the ME that we have consumed messages that are
     * currently locked on our behalf.
     * 
     * @param requestNumber The request number that replies should be sent with.
     * @param msgHandles The array of message id's that should be deleted.
     * @param tran
     * @param reply Whether the client is expecting a reply or not
     */
    @Override
    public void deleteSet(int requestNumber, SIMessageHandle[] msgHandles, int tran, boolean reply) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "deleteSet",
                                           new Object[]
                                           {
                                              requestNumber,
                                              msgHandles,
                                              tran,
                                              reply
                                           });

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            SibTr.debug(this, tc, "Request to delete " + msgHandles.length + " message(s)");
            if (reply)
                SibTr.debug(this, tc, "Client is expecting a reply");
        }

        try {
            SITransaction siTran =
                            ((ServerLinkLevelState) getConversation().getLinkLevelAttachment()).getTransactionTable().get(tran);

            if (siTran != IdToTransactionTable.INVALID_TRANSACTION) {
                getConsumerSession().deleteSet(msgHandles, siTran);
            }

            try {
                if (reply) {
                    getConversation().send(poolManager.allocate(),
                                           JFapChannelConstants.SEG_DELETE_SET_R,
                                           requestNumber,
                                           JFapChannelConstants.PRIORITY_MEDIUM,
                                           true,
                                           ThrottlingPolicy.BLOCK_THREAD,
                                           null);
                }
            } catch (SIException e) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".deleteSet",
                                            CommsConstants.CATASYNCHCONSUMER_DELETESET_02,
                                            this);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, e.getMessage(), e);

                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2017", e);
            }
        } catch (SIException e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!((ConversationState) getConversation().getAttachment()).hasMETerminated()) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".deleteSet",
                                            CommsConstants.CATASYNCHCONSUMER_DELETESET_01,
                                            this);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, e.getMessage(), e);

            if (reply) {
                StaticCATHelper.sendExceptionToClient(e,
                                                      CommsConstants.CATASYNCHCONSUMER_DELETESET_01,
                                                      getConversation(), requestNumber);
            } else {
                SibTr.error(tc, "UNABLE_TO_DELETE_MSGS_SICO2028", e);

                // Mark the transaction as error
                if (tran != CommsConstants.NO_TRANSACTION)
                    ((ServerLinkLevelState) getConversation().getLinkLevelAttachment()).getTransactionTable().markAsRollbackOnly(tran, e);

                StaticCATHelper.sendAsyncExceptionToClient(e,
                                                           CommsConstants.CATASYNCHCONSUMER_DELETESET_01, // d186970
                                                           getClientSessionId(), getConversation(), 0); // d172528
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "deleteSet");
    }

    /**
     * <p>This method will unlock all messages that are currently locked on the
     * ME. This is usually flowed when a failure on the client has occurred
     * and the proxy queues would like to reset to a known good state.
     * 
     * <p>It is possible though, that while we are processing this code to
     * perform the unlock that there may be messages already on their way
     * down to the client. The proxy will throw these messages away as they
     * will be unlocked by the server during this call, and the client does
     * not want to receive them twice.
     * 
     * <p>This is acheived through the message batch number that is flown
     * with every async message. When the proxy queue issues an <code>unlockAll()</code>
     * it increments it's message batch number. We also increment our server side
     * message batch number, but only after that <code>unlockAll()</code> has
     * completed. Therefore, any messages received by the client with an 'old'
     * message batch number can be safely discarded.
     * 
     * @param requestNumber The request number that replies should be sent with.
     */
    @Override
    public void unlockAll(int requestNumber) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "unlockAll", requestNumber);

        State returnToState = State.UNDEFINED;
        try {
            // Stop the session to prevent any (more) messages going - we only stop the session if the main consumer and
            // this async consumer session are both started. During async operation the main consumer will remain started
            // even when this async consumer is restarted and stopped each time a new msg is requested by the remote
            // client via SEG_RESTART_SESSION (restart) and consumeMessages (stop).
            stateLock.lock();
            try {
              while (state.isTransitioning()) stateTransition.await();
              returnToState = setState(State.PAUSED);
            } finally {
              stateLock.unlock();
            }
            final boolean wasStarted = returnToState.isStarted(); // Remember whether this session was started or not
            boolean restart = false;

            if (mainConsumer.isStarted() && wasStarted) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Stopping the consumer session");
                getConsumerSession().stop();
                returnToState = State.STOPPED;  // even if it wasn't, it is now so we would fall back to this
                restart = true;
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Consumer not fully started");
            }

            synchronized (this) {
                // Increment the message batch
                mainConsumer.incremenetMessageBatchNumber();

                // Now perform the actual unlockAll
                getConsumerSession().unlockAll();
            }

            short jfapPriority = JFapChannelConstants.getJFAPPriority(Integer.valueOf(mainConsumer.getLowestPriority()));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Sending with JFAP priority of " + jfapPriority);

            try {
                // and reply
                getConversation().send(poolManager.allocate(),
                                       JFapChannelConstants.SEG_UNLOCK_ALL_R,
                                       requestNumber,
                                       jfapPriority,
                                       true,
                                       ThrottlingPolicy.BLOCK_THREAD,
                                       null);
            } catch (SIException e) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".unlockAll",
                                            CommsConstants.CATASYNCHCONSUMER_UNLOCKALL_02,
                                            this);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, e.getMessage(), e);

                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2017", e);
            }

            // Now restart the session
            if (restart) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Starting the consumer session");
                getConsumerSession().start(false);
                returnToState = State.STARTED;    // will be set in finally block
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Consumer was not fully started");
            }
        } catch (SIException e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!((ConversationState) getConversation().getAttachment()).hasMETerminated()) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".unlockAll",
                                            CommsConstants.CATASYNCHCONSUMER_UNLOCKALL_01,
                                            this);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.CATASYNCHCONSUMER_UNLOCKALL_01, // d186970
                                                  getConversation(), requestNumber);
        } catch (InterruptedException ie) {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, ie.getMessage(), ie);
        } finally {
          if (State.UNDEFINED!=returnToState) setState(returnToState);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "unlockAll");
    }

    /**
     * This method will flush the consumer to ensure it is completely
     * out of messages.
     * 
     * @param requestNumber
     */
    @Override
    public void flush(int requestNumber) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "flush", "" + requestNumber);

        try {
            // Make sure it is stopped before we activate it
            if (mainConsumer.isStarted())
                getConsumerSession().stop();
            getConsumerSession().activateAsynchConsumer(true);
            if (mainConsumer.isStarted())
                getConsumerSession().start(false);

            short jfapPriority = JFapChannelConstants.getJFAPPriority(Integer.valueOf(mainConsumer.getLowestPriority())); // d172528
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Sending with JFAP priority of " + jfapPriority); // d172528

            try {
                getConversation().send(poolManager.allocate(),
                                       JFapChannelConstants.SEG_FLUSH_SESS_R,
                                       requestNumber,
                                       jfapPriority,
                                       true,
                                       ThrottlingPolicy.BLOCK_THREAD,
                                       null);
            } catch (SIException e) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".flush",
                                            CommsConstants.CATASYNCHCONSUMER_FLUSH_01,
                                            this);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, e.getMessage(), e);

                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2017", e);
            }
        } catch (SIException e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!((ConversationState) getConversation().getAttachment()).hasMETerminated()) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".flush",
                                            CommsConstants.CATASYNCHCONSUMER_FLUSH_02,
                                            this);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.CATASYNCHCONSUMER_FLUSH_02,
                                                  getConversation(), requestNumber);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "flush");
    }

    /**
     * Method to perform a single send of a message to the client.
     * 
     * @param sibMessage The message to send.
     * @param lastMsg true if this is the last message in the batch.
     * @param priority The priority to sent the message
     * 
     * @return Returns false if a communications error prevented the message
     *         from being sent.
     * 
     * @throws MessageEncodeFailedException if the message encoded.
     */
    private boolean sendMessage(SIBusMessage sibMessage, boolean lastMsg, Integer priority)
                    throws MessageEncodeFailedException,
                    IncorrectMessageTypeException,
                    MessageCopyFailedException,
                    UnsupportedEncodingException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "sendMessage",
                                           new Object[] { sibMessage, lastMsg, priority });

        boolean ok = false;

        // If we are at FAP9 or above we can do a 'chunked' send of the message in seperate
        // slices to make life easier on the Java memory manager
        final HandshakeProperties props = getConversation().getHandshakeProperties();
        if (props.getFapLevel() >= JFapChannelConstants.FAP_VERSION_9) {
            ok = sendChunkedMessage(sibMessage, lastMsg, priority);
        } else {
            ok = sendEntireMessage(sibMessage, null, lastMsg, priority);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "sendMessage", ok);
        return ok;
    }

    /**
     * This method will send the message to our peer in chunks as given to us by MFP. This is much
     * easier on the Java memory manager as it doesn't require the allocation of an enormous byte
     * array.
     * 
     * @param sibMessage
     * 
     * @return Returns true if the message was sent.
     * 
     * @throws MessageCopyFailedException
     * @throws IncorrectMessageTypeException
     * @throws MessageEncodeFailedException
     * @throws UnsupportedEncodingException
     */
    private boolean sendChunkedMessage(SIBusMessage sibMessage, boolean lastMsg, Integer priority)
                    throws MessageEncodeFailedException,
                    IncorrectMessageTypeException,
                    MessageCopyFailedException,
                    UnsupportedEncodingException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "sendChunkedMessage",
                                           new Object[] { sibMessage, lastMsg, priority });

        // Flag to indicate a Comms error so that we stop sending messages
        boolean ok = true;

        int jfapPriority = JFapChannelConstants.getJFAPPriority(priority);
        int msgLen = 0;

        // First of all we must encode the message ourselves
        CommsServerByteBuffer buffer = poolManager.allocate();
        ConversationState convState = (ConversationState) getConversation().getAttachment();

        try {
            List<DataSlice> messageSlices = buffer.encodeFast((JsMessage) sibMessage,
                                                              convState.getCommsConnection(),
                                                              getConversation());

            // Do a check on the size of the message. If it is less than our threshold, forget the
            // chunking and simply send the message as one
            for (DataSlice slice : messageSlices)
                msgLen += slice.getLength();
            if (msgLen < CommsConstants.MINIMUM_MESSAGE_SIZE_FOR_CHUNKING) {
                // The message is a tiddler, send it in one
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Message is smaller than " +
                                                           CommsConstants.MINIMUM_MESSAGE_SIZE_FOR_CHUNKING);
                sendEntireMessage(sibMessage, messageSlices, lastMsg, priority);
            } else {
                // Now we have the slices, send each one in turn. Each slice contains all the header
                // information so that the client code knows what to do with the message
                for (int x = 0; x < messageSlices.size(); x++) {
                    DataSlice slice = messageSlices.get(x);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Sending slice:", slice);

                    boolean first = (x == 0);
                    boolean last = (x == (messageSlices.size() - 1));
                    byte flags = 0;

                    // Work out the flags to send
                    if (first)
                        flags |= CommsConstants.CHUNKED_MESSAGE_FIRST;
                    if (last)
                        flags |= CommsConstants.CHUNKED_MESSAGE_LAST;
                    else if (!first)
                        flags |= CommsConstants.CHUNKED_MESSAGE_MIDDLE;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Flags: " + flags);

                    if (!first) {
                        // This isn't the first slice, grab a fresh buffer
                        buffer = poolManager.allocate();
                    }

                    // Set a flag to indicate the last in batch
                    short msgFlags = CommsConstants.ASYNC_START_OR_MID_BATCH;
                    if (lastMsg)
                        msgFlags |= CommsConstants.ASYNC_LAST_IN_BATCH;

                    // Now add all the header information
                    buffer.putShort(convState.getConnectionObjectId());
                    buffer.putShort(mainConsumer.getClientSessionId());
                    buffer.putShort(msgFlags);
                    buffer.putShort(mainConsumer.getMessageBatchNumber()); // BIT16 Message batch
                    buffer.put(flags);
                    buffer.putDataSlice(slice);

                    getConversation().send(buffer,
                                           JFapChannelConstants.SEG_CHUNKED_ASYNC_MESSAGE,
                                           0, // No request number
                                           jfapPriority,
                                           false,
                                           ThrottlingPolicy.BLOCK_THREAD,
                                           null);
                }

            }
        } catch (SIException e) {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".sendChunkedMessage",
                                        CommsConstants.CATASYNCHCONSUMER_SENDCHUNKEDMESS_01,
                                        this);
            ok = false;

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2017", e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "sendChunkedMessage", ok);
        return ok;
    }

    /**
     * Sends the message in one big buffer. If the messageSlices parameter is not null then
     * the message has already been encoded and does not need to be done again. This may be in the
     * case where the message was destined to be sent in chunks but is so small that it does not
     * seem worth it.
     * 
     * @param sibMessage The entire message to send.
     * @param messageSlices The already encoded message slices.
     * @param lastMsg
     * @param priority
     * 
     * @return Returns true if the message was sent.
     * 
     * @throws MessageEncodeFailedException
     * @throws IncorrectMessageTypeException
     * @throws MessageCopyFailedException
     * @throws UnsupportedEncodingException
     */
    private boolean sendEntireMessage(SIBusMessage sibMessage, List<DataSlice> messageSlices,
                                      boolean lastMsg, Integer priority)
                    throws MessageEncodeFailedException,
                    IncorrectMessageTypeException,
                    MessageCopyFailedException,
                    UnsupportedEncodingException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "sendEntireMessage",
                                           new Object[] { sibMessage, messageSlices, lastMsg, priority });

        if (lastMsg) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Sending last in batch");
        }

        ConversationState convState = (ConversationState) getConversation().getAttachment();

        // Flag to indicate a Comms error so that we stop sending messages
        boolean ok = true;

        try {
            CommsServerByteBuffer byteBuffer = poolManager.allocate();

            int msgLen = 0;
            short msgFlags = CommsConstants.ASYNC_START_OR_MID_BATCH;
            if (lastMsg)
                msgFlags |= CommsConstants.ASYNC_LAST_IN_BATCH;

            byteBuffer.putShort(convState.getConnectionObjectId());
            byteBuffer.putShort(mainConsumer.getClientSessionId());
            byteBuffer.putShort(msgFlags);
            byteBuffer.putShort(mainConsumer.getMessageBatchNumber()); // BIT16 Message Batch
            // Put the entire message into the buffer in whatever way is suitable
            if (messageSlices == null) {
                msgLen = byteBuffer.putMessage((JsMessage) sibMessage,
                                               convState.getCommsConnection(),
                                               getConversation());
            } else {
                msgLen = byteBuffer.putMessgeWithoutEncode(messageSlices);
            }

            int jfapPriority = JFapChannelConstants.getJFAPPriority(priority);

            getConversation().send(byteBuffer,
                                   JFapChannelConstants.SEG_ASYNC_MESSAGE,
                                   0, // No request number
                                   jfapPriority,
                                   false,
                                   ThrottlingPolicy.BLOCK_THREAD,
                                   null);


        } catch (SIException e) {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".sendEntireMessage",
                                        CommsConstants.CATASYNCHCONSUMER_SENDENTIREMESS_01,
                                        this);
            ok = false;

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2017", e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "sendEntireMessage", ok);
        return ok;
    }

    /**
     * Method called when the asynchronous consumer has messages to send to the client
     * 
     * @param lme
     */
    public synchronized void consumeMessages(LockedMessageEnumeration lme) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "consumeMessages", lme);

        Integer highestPriority = Integer.valueOf(0);
        int messageCount = 0;
        Integer currentPriority;
        SIBusMessage jsMessage = null;

        // First go through each message and find out what the
        // highest priority is. All messages will be sent to the
        // client at this priority.
        while (true) {
            try {
                jsMessage = lme.nextLocked();
                if (jsMessage == null) {
                    // We have run out of messages, so reset the cursor
                    // and get out of here.
                    lme.resetCursor();
                    break;
                }

                // Count the messages so that when sending them to the client
                // we know which one is the 'last in batch'
                messageCount++;

                // Get the priority
                currentPriority = jsMessage.getPriority();
                if (currentPriority.intValue() > highestPriority.intValue()) {
                    highestPriority = currentPriority;
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Message " + jsMessage.getMessageHandle() +
                                                     " has priority " + currentPriority);
            } catch (SIException e) {
                //No FFDC code needed
                //Only FFDC if we haven't received a meTerminated event.
                if (!((ConversationState) getConversation().getAttachment()).hasMETerminated()) {
                    FFDCFilter.processException(e,
                                                CLASS_NAME + ".consumeMessages",
                                                CommsConstants.CATASYNCHCONSUMER_CONSUME_MSGS_01,
                                                this);
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, e.getMessage(), e);

                StaticCATHelper.sendAsyncExceptionToClient(e,
                                                           CommsConstants.CATASYNCHCONSUMER_CONSUME_MSGS_01, // d186970
                                                           getClientSessionId(), getConversation(), 0);

                // Attempt to resetCursor so we can atleast send some of the messages, this is likely to fail 
                //  as the most likely reason we are here is because of a closed connection/session, but it is
                //  worth trying this.
                try {
                    lme.resetCursor();
                } catch (SIException e1) {
                    // No FFDC Code needed
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, e1.getMessage(), e1);
                }
                //Break out of the loop as there was an issue with nextLocked or resetCursor. 
                break;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            SibTr.debug(this, tc, "There are " + messageCount + " messages in the LME");
            SibTr.debug(this, tc, "The highest message priority is " + highestPriority);
        }

        String xctErrStr = null;
        boolean ok = true;

        for (int currMessCount = 0; currMessCount < messageCount; currMessCount++) {
            // If we failed to send a message, don't try anymore
            if (!ok)
                break;

            try {
                jsMessage = lme.nextLocked();
//                if (XctSettings.isAnyEnabled()) {
//                    String xctCorrelationID = jsMessage.getXctCorrelationID();
//                    Xct xct = Xct.fromString(xctCorrelationID);
//                    if (xct.annotationsEnabled()) {
//                        Annotation annotation = new Annotation(XctJmsConstants.XCT_SIBUS).add(XctJmsConstants.XCT_CONSUME_SEND);
//                        annotation.add(new Annotation(XctJmsConstants.XCT_SYSTEM_MESSAGE_ID).add(jsMessage.getSystemMessageId()));
//                        xct.begin(annotation);
//                    } else
//                        xct.begin();
//                }

                // Are we the last in batch?
                if ((currMessCount + 1) == messageCount) {
                    // Stop the session
                    State fallback = State.UNDEFINED;
                    try {
                      fallback = setState(State.STOPPING);
                      getConsumerSession().stop();
                      fallback = State.STOPPED;

                    } finally {
                      setState(fallback);
                    }
                    batchesSent++;
                    messagesSent++;

                    ok = sendMessage(jsMessage, true, highestPriority);
                } else {
                    ok = sendMessage(jsMessage, false, highestPriority);
                    messagesSent++;
                }

                // If the message is 'unrecoverable', then delete it immediately
                if (!CommsUtils.isRecoverable(jsMessage, mainConsumer.getUnrecoverableReliability())) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Deleting message");
                    lme.deleteCurrent(null);
                }
            } catch (Exception e) {
                //No FFDC code needed

                //Error string for XCT
                // Romil liberty changes coomented xct code 
                //xctErrStr = XctJmsConstants.XCT_ERROR_MSG_06;

                //Only FFDC if we haven't received a meTerminated event OR if this Exception isn't a SIException.
                if (!(e instanceof SIException) || !((ConversationState) getConversation().getAttachment()).hasMETerminated()) {
                    FFDCFilter.processException(e,
                                                CLASS_NAME + ".consumeMessages",
                                                CommsConstants.CATASYNCHCONSUMER_CONSUME_MSGS_02);
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, e.getMessage(), e);

                StaticCATHelper.sendAsyncExceptionToClient(e,
                                                           CommsConstants.CATASYNCHCONSUMER_CONSUME_MSGS_02,
                                                           getClientSessionId(), getConversation(), 0);

                // Attempt to stop the session before we break out
                try {
                    State fallback = State.UNDEFINED;
                    try {
                      fallback = setState(State.STOPPING);
                      getConsumerSession().stop();
                      fallback = State.STOPPED;
                    } finally {
                      setState(fallback);
                    }
                } catch (SIException e1) {
                    //No FFDC Code needed
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, e1.getMessage(), e1);
                }

                // We are unlikely to be able to move forward through the LME, i.e. nextLocked will always 
                //  fail. So break out now so we don't flood the system with messages.
                break;
            } finally {
//                if (XctSettings.isAnyEnabled()) {
//                    Xct xct = Xct.current();
//                    if (xct.annotationsEnabled()) {
//                        Annotation annotation = new Annotation(XctJmsConstants.XCT_SIBUS).add(XctJmsConstants.XCT_CONSUME_SEND);
//                        if (xctErrStr != null)
//                            annotation.add(XctJmsConstants.XCT_FAILED).add(xctErrStr);
//                        xct.end(annotation);
//                    } else
//                        xct.end();
//                }
            }
        }

        // Update the session lowest priority
        mainConsumer.setLowestPriority(JFapChannelConstants.getJFAPPriority(highestPriority));

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "consumeMessages");
    }

    // SIB0115d.comms start

    /**
     * Method called when the asynchronous consumer has been stopped duee to the maxSequentialFailures threshold being reached
     * 
     * @param lme
     */
    public void consumerSessionStopped() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "consumerSessionStopped");

        // Inform the main consumer instance that message processor has stopped the session, this will prevent any more
        // restart requests from the client starting the consumer session by mistake, instead we must wait for a start
        // request from the application.
        mainConsumer.stopStoppableSession(); //471642

        ConversationState convState = (ConversationState) getConversation().getAttachment();

        CommsServerByteBuffer buffer = poolManager.allocate();

        // Put conversation id
        buffer.putShort(convState.getConnectionObjectId());

        // Put session id
        buffer.putShort(getClientSessionId());

        try {
            getConversation().send(buffer, JFapChannelConstants.SEG_ASYNC_SESSION_STOPPED_NOREPLY, 0, JFapChannelConstants.PRIORITY_MEDIUM, false, ThrottlingPolicy.BLOCK_THREAD,
                                   null);
        } catch (SIException e) {
            FFDCFilter.processException(e, CLASS_NAME + ".consumerSessionStopped", CommsConstants.CATASYNCHCONSUMER_SENSSION_STOPPED_01, this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, e.getMessage(), e);
            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2017", e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "consumerSessionStopped");
    }

    // SIB0115d.comms end

    /**
     * <p>This method will unlock all messages that are currently locked on the
     * ME. This is usually flowed when a unlockAll(incrementUnlockCount) is called from client
     * and the proxy queues would like to reset to a known good state.
     * 
     * <p>It is possible though, that while we are processing this code to
     * perform the unlock that there may be messages already on their way
     * down to the client. The proxy will throw these messages away as they
     * will be unlocked by the server during this call, and the client does
     * not want to receive them twice.
     * 
     * <p>This is acheived through the message batch number that is flown
     * with every async message. When the proxy queue issues an <code>unlockAll()</code>
     * it increments its message batch number. We also increment our server side
     * message batch number, but only after that <code>unlockAll()</code> has
     * completed. Therefore, any messages received by the client with an 'old'
     * message batch number can be safely discarded.
     * 
     * @param requestNumber The request number that replies should be sent with.
     * @param incrementUnlockCount Option to increment the unlock count or not on unlock of messages
     */
    @Override
    public void unlockAll(int requestNumber, boolean incrementUnlockCount) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "unlockAll", new Object[] { requestNumber, incrementUnlockCount });

        State returnToState = State.UNDEFINED;
        try {
            // Stop the session to prevent any (more) messages going - we only stop the session if the main consumer and
            // this async consumer session are both started. During async operation the main consumer will remain started
            // even when this async consumer is restarted and stopped each time a new msg is requested by the remote
            // client via SEG_RESTART_SESSION (restart) and consumeMessages (stop).
            stateLock.lock();
            try {
              while (state.isTransitioning()) stateTransition.await();
              returnToState = setState(State.PAUSED);
            } finally {
              stateLock.unlock();
            }
            final boolean wasStarted = returnToState.isStarted(); // Remember whether this session was started or not
            boolean restart = false;

            if (mainConsumer.isStarted() && wasStarted) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Stopping the consumer session");
                getConsumerSession().stop();
                returnToState = State.STOPPED;  // even if it wasn't, it is now so we would fall back to this
                restart = true;
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Consumer not fully started");
            }

            synchronized (this) {
                // Increment the message batch
                mainConsumer.incremenetMessageBatchNumber();

                // Now perform the actual unlockAll
                getConsumerSession().unlockAll(incrementUnlockCount);
            }

            short jfapPriority = JFapChannelConstants.getJFAPPriority(Integer.valueOf(mainConsumer.getLowestPriority()));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Sending with JFAP priority of " + jfapPriority);

            try {
                // and reply
                getConversation().send(poolManager.allocate(),
                                       JFapChannelConstants.SEG_UNLOCK_ALL_NO_INC_LOCK_COUNT_R,
                                       requestNumber,
                                       jfapPriority,
                                       true,
                                       ThrottlingPolicy.BLOCK_THREAD,
                                       null);
            } catch (SIException e) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".unlockAll",
                                            CommsConstants.CATASYNCHCONSUMER_UNLOCKALL_03,
                                            this);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, e.getMessage(), e);

                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2017", e);
            }

            // Now restart the session
            if (restart) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Starting the consumer session");
                getConsumerSession().start(false);
                returnToState = State.STARTED;    // will be set in finally block
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Consumer was not fully started");
            }
        } catch (SIException e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!((ConversationState) getConversation().getAttachment()).hasMETerminated()) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".unlockAll",
                                            CommsConstants.CATASYNCHCONSUMER_UNLOCKALL_04,
                                            this);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.CATASYNCHCONSUMER_UNLOCKALL_04,
                                                  getConversation(), requestNumber);
        } catch (InterruptedException ie) {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, ie.getMessage(), ie);
        } finally {
          if (State.UNDEFINED!=returnToState) setState(returnToState);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "unlockAll");
    }

}
