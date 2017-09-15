/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server.clientsupport;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.server.CommsServerByteBuffer;
import com.ibm.ws.sib.comms.server.ConversationState;
import com.ibm.ws.sib.comms.server.IdToTransactionTable;
import com.ibm.ws.sib.comms.server.ServerLinkLevelState;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.Conversation.ThrottlingPolicy;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.BifurcatedConsumerSession;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.SITransaction;

/**
 * This class is the specific consumer handler for Bifurcated consumer sessions.
 * 
 * @author Gareth Matthews
 */
public class CATBifurcatedConsumer extends CATConsumer {
    /** Class name for FFDC's */
    private static String CLASS_NAME = CATBifurcatedConsumer.class.getName();

    /** Trace */
    private static final TraceComponent tc = SibTr.register(CATBifurcatedConsumer.class,
                                                            CommsConstants.MSG_GROUP,
                                                            CommsConstants.MSG_BUNDLE);

    /** Log source info on class load */
    static {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc,
                        "Source info: @(#)SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/clientsupport/CATBifurcatedConsumer.java, SIB.comms, WASX.SIB, aa1225.01 1.28"); //f168604.1
    }

    /** Reference to our main consumer */
    private final CATMainConsumer mainConsumer;

    /** The actual bifurcated session */
    private BifurcatedConsumerSession bifSession = null;

    /**
     * Constructor which is passed the reference to the 'main' consumer object.
     * 
     * @param mainConsumer
     * @param bifSession
     */
    public CATBifurcatedConsumer(CATMainConsumer mainConsumer, BifurcatedConsumerSession bifSession) {
        super();

        this.mainConsumer = mainConsumer;
        this.bifSession = bifSession;
    }

    /**
     * @return Returns null - this is because this special session does not have a consumer session.
     *         It has a bifurcatedSession saved in this object.
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

    /**
     * This overrides the default close method in <code>CATConsumer</code> as the consumer session
     * is saved with us, and not in the main consumer.
     * 
     * @param requestNumber The request number to send replies with.
     */
    @Override
    public void close(int requestNumber) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "close", requestNumber);

        try {
            bifSession.close();

            try {
                getConversation().send(poolManager.allocate(),
                                       JFapChannelConstants.SEG_CLOSE_CONSUMER_SESS_R,
                                       requestNumber,
                                       JFapChannelConstants.PRIORITY_MEDIUM,
                                       true,
                                       ThrottlingPolicy.BLOCK_THREAD,
                                       null);
            } catch (SIException e) {
                FFDCFilter.processException(e, CLASS_NAME + ".close",
                                            CommsConstants.CATBIFCONSUMER_CLOSE_01,
                                            this);

                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2033", e);
            }
        } catch (SIException e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!((ConversationState) getConversation().getAttachment()).hasMETerminated()) {
                FFDCFilter.processException(e, CLASS_NAME + ".close",
                                            CommsConstants.CATBIFCONSUMER_CLOSE_02,
                                            this);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.CATBIFCONSUMER_CLOSE_02,
                                                  getConversation(), requestNumber);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "close");
    }

    /**
     * This method will return a set of messages currently locked by the messaging engine.
     * 
     * @param requestNumber
     * @param msgIds
     */
    @Override
    public void readSet(int requestNumber, SIMessageHandle[] msgHandles) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "readSet",
                                           new Object[] { requestNumber, msgHandles });

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "Request to read " + msgHandles.length +
                                                     " message(s)");

        final ConversationState convState = (ConversationState) getConversation().getAttachment();

        try {
            SIBusMessage[] messages = bifSession.readSet(msgHandles);

            // Add the number of items first
            CommsServerByteBuffer buff = poolManager.allocate();
            buff.putInt(messages.length);

            for (int x = 0; x < messages.length; x++) {
                buff.putMessage((JsMessage) messages[x],
                                convState.getCommsConnection(),
                                getConversation());
            }

            try {
                getConversation().send(buff,
                                       JFapChannelConstants.SEG_READ_SET_R,
                                       requestNumber,
                                       JFapChannelConstants.PRIORITY_MEDIUM,
                                       true,
                                       ThrottlingPolicy.BLOCK_THREAD,
                                       null);
            } catch (SIException e) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".readSet",
                                            CommsConstants.CATBIFCONSUMER_READSET_01,
                                            this);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, e.getMessage(), e);

                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2033", e);
            }
        } catch (Exception e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event OR if this Exception isn't a SIException.
            if (!(e instanceof SIException) || !convState.hasMETerminated()) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".readSet",
                                            CommsConstants.CATBIFCONSUMER_READSET_02,
                                            this);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.CATBIFCONSUMER_READSET_02,
                                                  getConversation(), requestNumber);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "readSet");
    }

    /**
     * This method will return and delete a set of messages currently locked by the messaging
     * engine.
     * 
     * @param requestNumber
     * @param msgIds
     * @param tran
     */
    @Override
    public void readAndDeleteSet(int requestNumber, SIMessageHandle[] msgHandles, int tran) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "readAndDeleteSet",
                                           new Object[] { requestNumber, msgHandles, tran });

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "Request to read / delete " + msgHandles.length +
                                                     " message(s)");

        final ConversationState convState = (ConversationState) getConversation().getAttachment();

        try {
            SITransaction siTran = null;
            if (tran != CommsConstants.NO_TRANSACTION) {
                siTran =
                                ((ServerLinkLevelState) getConversation().getLinkLevelAttachment()).getTransactionTable().get(tran);
            }

            SIBusMessage[] messages;
            if (siTran != IdToTransactionTable.INVALID_TRANSACTION) {
                messages = bifSession.readAndDeleteSet(msgHandles, siTran);
            } else {
                messages = new SIBusMessage[0];
            }

            // Add the number of items first
            CommsServerByteBuffer buff = poolManager.allocate();
            buff.putInt(messages.length);

            for (int x = 0; x < messages.length; x++) {
                buff.putMessage((JsMessage) messages[x],
                                convState.getCommsConnection(),
                                getConversation());
            }

            try {
                getConversation().send(buff,
                                       JFapChannelConstants.SEG_READ_AND_DELETE_SET_R,
                                       requestNumber,
                                       JFapChannelConstants.PRIORITY_MEDIUM,
                                       true,
                                       ThrottlingPolicy.BLOCK_THREAD,
                                       null);
            } catch (SIException e) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".readAndDeleteSet",
                                            CommsConstants.CATBIFCONSUMER_READANDDELTESET_01,
                                            this);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, e.getMessage(), e);

                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2033", e);
            }
        } catch (Exception e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event OR if this Exception isn't a SIException.
            if (!(e instanceof SIException) || !convState.hasMETerminated()) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".readAndDeleteSet",
                                            CommsConstants.CATBIFCONSUMER_READANDDELTESET_02,
                                            this);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.CATBIFCONSUMER_READANDDELTESET_02,
                                                  getConversation(), requestNumber);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "readAndDeleteSet");
    }

    /**
     * This method will unlock a set of locked messages that have been delivered to
     * us (the server) which we have then passed on to the client.
     * 
     * @param requestNumber The request number that replies should be sent with.
     * @param msgIds The array of message id's that should be unlocked.
     * @param reply Whether this will demand a reply.
     */
    @Override
    public void unlockSet(int requestNumber, SIMessageHandle[] msgHandles, boolean reply) // f199593, F219476.2
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "unlockSet",
                                           new Object[] { requestNumber, msgHandles, reply });

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            SibTr.debug(this, tc, "Request to unlock " + msgHandles.length + " message(s)");
            if (reply)
                SibTr.debug(this, tc, "The client is expecting a reply");
        }

        try {
            bifSession.unlockSet(msgHandles);

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
                                                CommsConstants.CATBIFCONSUMER_UNLOCKSET_01,
                                                this);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, e.getMessage(), e);

                    SibTr.error(tc, "COMMUNICATION_ERROR_SICO2033", e);
                }
            }
        } catch (SIException e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!((ConversationState) getConversation().getAttachment()).hasMETerminated()) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".unlockSet",
                                            CommsConstants.CATBIFCONSUMER_UNLOCKSET_02,
                                            this);
            }

            if (reply) {
                StaticCATHelper.sendExceptionToClient(e,
                                                      CommsConstants.CATBIFCONSUMER_UNLOCKSET_02,
                                                      getConversation(),
                                                      requestNumber);
            } else {
                SibTr.error(tc, "UNABLE_TO_UNLOCK_MSGS_SICO2032", e);
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
                                           new Object[] { requestNumber, msgHandles, reply, incrementLockCount });

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            SibTr.debug(this, tc, "Request to unlock " + msgHandles.length + " message(s)");
            if (reply)
                SibTr.debug(this, tc, "The client is expecting a reply");
        }

        try {
            bifSession.unlockSet(msgHandles, incrementLockCount);

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
                                                CommsConstants.CATBIFCONSUMER_UNLOCKSET_04,
                                                this);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, e.getMessage(), e);

                    SibTr.error(tc, "COMMUNICATION_ERROR_SICO2033", e);
                }
            }
        } catch (SIException e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!((ConversationState) getConversation().getAttachment()).hasMETerminated()) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".unlockSet",
                                            CommsConstants.CATBIFCONSUMER_UNLOCKSET_03,
                                            this);
            }

            if (reply) {
                StaticCATHelper.sendExceptionToClient(e,
                                                      CommsConstants.CATBIFCONSUMER_UNLOCKSET_03,
                                                      getConversation(),
                                                      requestNumber);
            } else {
                SibTr.error(tc, "UNABLE_TO_UNLOCK_MSGS_SICO2032", e);
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
     * @param msgIds The array of message id's that should be deleted.
     * @param tran
     * @param reply Whether the client is expecting a reply or not
     */
    @Override
    public void deleteSet(int requestNumber, SIMessageHandle[] msgHandles, int tran, boolean reply) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "deleteSet",
                                           new Object[] { requestNumber, msgHandles, tran, reply });

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            SibTr.debug(this, tc, "Request to delete " + msgHandles.length + " message(s)");
            if (reply)
                SibTr.debug(this, tc, "Client is expecting a reply");
        }

        try {
            SITransaction siTran = null;
            if (tran != CommsConstants.NO_TRANSACTION) {
                siTran =
                                ((ServerLinkLevelState) getConversation().getLinkLevelAttachment()).getTransactionTable().get(tran);
            }

            if (siTran != IdToTransactionTable.INVALID_TRANSACTION) {
                bifSession.deleteSet(msgHandles, siTran);
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
                                            CommsConstants.CATBIFCONSUMER_DELETESET_01,
                                            this);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, e.getMessage(), e);

                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2033", e);
            }
        } catch (SIException e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!((ConversationState) getConversation().getAttachment()).hasMETerminated()) {
                FFDCFilter.processException(e,
                                            CLASS_NAME + ".deleteSet",
                                            CommsConstants.CATBIFCONSUMER_DELETESET_02,
                                            this);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, e.getMessage(), e);

            if (reply) {
                StaticCATHelper.sendExceptionToClient(e,
                                                      CommsConstants.CATBIFCONSUMER_DELETESET_03,
                                                      getConversation(), requestNumber);
            } else {
                SibTr.error(tc, "UNABLE_TO_DELETE_MSGS_SICO2034", e);

                // Mark the transaction as error
                if (tran != CommsConstants.NO_TRANSACTION) {
                    ((ServerLinkLevelState) getConversation().getLinkLevelAttachment()).getTransactionTable().markAsRollbackOnly(tran, e);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "deleteSet");
    }
}
