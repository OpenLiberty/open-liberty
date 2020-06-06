/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
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
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.common.CommsUtils;
import com.ibm.ws.sib.comms.server.ConversationState;
import com.ibm.ws.sib.comms.server.clientsupport.CATConsumer.State;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.AsynchConsumerCallback;
import com.ibm.wsspi.sib.core.LockedMessageEnumeration;
import com.ibm.wsspi.sib.core.SIBusMessage;

/**
 * This class is the async callback for a read ahead session.
 * 
 * @author Gareth Matthews
 */
public class CATAsynchReadAheadReader implements AsynchConsumerCallback {
    /** Class name for FFDC's */
    private static String CLASS_NAME = CATAsynchReadAheadReader.class.getName();

    /** Trace */
    private static final TraceComponent tc = SibTr.register(CATAsynchReadAheadReader.class,
                                                            CommsConstants.MSG_GROUP,
                                                            CommsConstants.MSG_BUNDLE);
    /** Log source info on static load */
    static {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(
                        tc,
                        "Source info: @(#)SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/clientsupport/CATAsynchReadAheadReader.java, SIB.comms, WASX.SIB, aa1225.01 1.38");
    }

    /** The owning instance of the CAT consumer session */
    final CATProxyConsumer consumerSession; // d172528

    /** The main consumer on whose behalf we reading ahead */
    final CATMainConsumer mainConsumer;

    /**
     * Constructor.
     * 
     * @param consumerSession
     * @param mainConsumer
     */
    public CATAsynchReadAheadReader(CATProxyConsumer consumerSession, CATMainConsumer mainConsumer) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", new Object[] { consumerSession, mainConsumer });
        this.consumerSession = consumerSession;
        this.mainConsumer = mainConsumer;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "<init>");
    }

    /**
     * This method is called by the core API when a message is available.
     * Here, we must send the message back to the client and keep track of
     * how many bytes we have sent.
     * <p>
     * Pacing works by the read ahead consumer on the server (us) sending
     * messages to the server. We will send up to x bytes of messages, as
     * requested by the client. When we have sent enough messages, we will
     * stop the consumer, to prevent any more messages being sent.
     * <p>
     * The client application will then consume the messages that it has
     * been delivered. When the amount of bytes left to consume falls below
     * a threshold value, the client will request more messages and will
     * inform us how much has been consumed, and the total bytes they are
     * prepared to cope with. We then will resend enough messages to keep
     * the client topped up.
     * 
     * @param vEnum
     */
    public void consumeMessages(LockedMessageEnumeration vEnum) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "consumeMessages", vEnum);

        if (mainConsumer.getConversation().getConnectionReference().isClosed()) {
            // stop consumer to avoid infinite loop     
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "The connection is closed so we shouldn't consume anymore messages. Consumer Session should be closed soon");
            stopConsumer();
        } else {
            String xctErrStr = null;

            State fallback = State.UNDEFINED;
            try {
                // Get the next message in the vEnum
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Getting next locked message");

                SIBusMessage sibMessage = vEnum.nextLocked(); // d172528

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Received message", sibMessage);

                // Send the message
                int msgLen = consumerSession.sendMessage(sibMessage); // d172528    // D221806

                // If the messages are unrecoverable then we can optimise this by deleting
                // the message now
                if (!CommsUtils.isRecoverable(sibMessage, consumerSession.getUnrecoverableReliability())) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Deleting the message");
                    vEnum.deleteCurrent(null);
                }

                consumerSession.setLowestPriority(JFapChannelConstants.getJFAPPriority(sibMessage.getPriority())); // d172528

                // Start D221806
                // Ensure we take a lock on the consumer session so that the request for more messages
                // doesn't corrupt the counters.
                boolean stopConsumer = false;
                consumerSession.stateLock.lock();
                try {
                    while (consumerSession.state.isTransitioning()) consumerSession.stateTransition.await();
                    stopConsumer = (msgLen == 0) || consumerSession.updateConsumedBytes(msgLen);
                    if (stopConsumer) fallback = consumerSession.setState(State.STOPPING);
                }
                finally {
                    consumerSession.stateLock.unlock();
                }

                if (stopConsumer) {
                    // in addition to the pacing control, we must avoid an infinite loop
                    // attempting to send messages that don't get through.  If msgLen
                    // is 0 then no message was sent, and we must stop the consumer
                    // and crucially, give up the asynchconsumerbusylock so the consumer
                    // can be closed if need be.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        SibTr.debug(this
                                   ,tc
                                   ,String.format("[@%x] Stopping consumer session (@%x) "
                                                 +"(sent bytes >= requested bytes || msgLen (%d) = 0)"
                                                 ,this.hashCode()
                                                 ,consumerSession.hashCode()
                                                 ,msgLen
                                                 )
                                   );
                    }
                    stopConsumer();
                    fallback = State.UNDEFINED;
                }
            }
            // start d172528
            catch (Throwable e) {
                //No FFDC code needed
                //Only FFDC if we haven't received a meTerminated event OR if e isn't a SIException
                final ConversationState convState = (ConversationState) consumerSession.getConversation().getAttachment();

                if (!(e instanceof SIException) || !convState.hasMETerminated()) {
                    FFDCFilter.processException(e,
                                                CLASS_NAME + ".consumeMessages",
                                                CommsConstants.CATASYNCHRHREADER_CONSUME_MSGS_01,
                                                this);
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, e.getMessage(), e);

                StaticCATHelper.sendAsyncExceptionToClient(e,
                                                           CommsConstants.CATASYNCHRHREADER_CONSUME_MSGS_01, // d186970
                                                           consumerSession.getClientSessionId(),
                                                           consumerSession.getConversation(), 0);
            } // end d172528
            finally {
              if (State.UNDEFINED!=fallback) consumerSession.setState(fallback);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "consumeMessages");
    }

    /**
     * Safely stop the consumer
     * 
     * @param Message to be traced for stop reason.
     */
    public void stopConsumer() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "stopConsumer");
        try {
            // lock the consumerSession to ensure visibility of update to started.
            synchronized (consumerSession) {
                consumerSession.getConsumerSession().stop();
                consumerSession.setState(State.STOPPED);
            }
        } catch (Throwable t) {
            FFDCFilter.processException(t,
                                        CLASS_NAME + ".consumeMessages",
                                        CommsConstants.CATASYNCHRHREADER_CONSUME_MSGS_02,
                                        this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Unable to stop consumer session due to Throwable: " + t);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "stopConsumer");
    }
}
