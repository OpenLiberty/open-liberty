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
import com.ibm.ejs.util.am.Alarm;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.server.CommsServerByteBuffer;
import com.ibm.ws.sib.comms.server.CommsServerByteBufferPool;
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
import com.ibm.wsspi.sib.core.AsynchConsumerCallback;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.LockedMessageEnumeration;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SICoreConnectionListener;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * This class is the asynchronous message listener for synchronous sends
 * and receives. The core API will call this class's <code>onMessage()</code>
 * method indicating that a message has arrived. In which case, we must
 * send it back to the client how is waiting for the message.
 * 
 * D281779 has also made this class implement SICoreConnectionListener.
 * This is done so that exception events (which would prevent a message
 * ever being delivered to the consumeMessages method) can be intercepted and
 * used to wake up any client thread sat in a receiveWithWait prior to the
 * timeout expiring.
 * 
 * @author Gareth Matthews
 */
public class CATSyncAsynchReader implements AsynchConsumerCallback, SICoreConnectionListener {
    /** Class name for FFDC's */
    private static String CLASS_NAME = CATSyncAsynchReader.class.getName();

    /**
     * The JFAP request number that we need to send all replies with.
     */
    private int requestNumber;

    /**
     * Flag to indicate whether the async reader has processed
     * (or is processing) a message, or a no message condition.
     */
    private boolean completed = false;

    /**
     * The WAS alarm that was created if a timeout
     * was specified on the receive.
     */
    private Alarm alarm = null;

    /**
     * The transaction we are using
     */
    private int transaction = CommsConstants.NO_TRANSACTION;

    /**
     * Reference to the buffer pool manager
     */
    private static CommsServerByteBufferPool poolManager = CommsServerByteBufferPool.getInstance();

    /**
     * The conversation
     */
    private Conversation conversation = null;

    /**
     * Reference to the main consumer
     */
    private CATMainConsumer mainConsumer = null;

    /**
     * Flag to indicate whether we are in the middle of
     * a receiveWithWait()
     */
    private boolean currentlyDoingReceiveWithWait = false;

    /**
     * Register our trace component
     */
    private static final TraceComponent tc = SibTr.register(CATSyncAsynchReader.class,
                                                            CommsConstants.MSG_GROUP,
                                                            CommsConstants.MSG_BUNDLE);

    /** Log class info on load */
    static {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(
                        tc,
                        "Source info: @(#)SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/clientsupport/CATSyncAsynchReader.java, SIB.comms, WASX.SIB, aa1225.01 1.57");
    }

    /**
     * Constructs a new reader.
     * 
     * @param transaction The current transaction the receives are being done under.
     * @param conversation The conversation to use.
     * @param mainConsumer A reference to the CATMainConsumer object that wraps the
     *            actual consumer session.
     * @param initialRequestNumber The request number this reader will initialy use.
     */
    public CATSyncAsynchReader(int transaction, Conversation conversation,
                               CATMainConsumer mainConsumer, int initialRequestNumber) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>",
                                           new Object[]
                                           {
                                              transaction,
                                              conversation,
                                              mainConsumer,
                                              initialRequestNumber
                                           });
        this.transaction = transaction;
        this.conversation = conversation;
        this.mainConsumer = mainConsumer;
        this.requestNumber = initialRequestNumber;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /**
     * Sets whether we are currently doing a receiveWithWait()
     * 
     * @param d
     */
    public void setCurrentlyDoingReceiveWithWait(boolean d) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setCurrentlyDoingReceiveWithWait", d);
        currentlyDoingReceiveWithWait = d;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setCurrentlyDoingReceiveWithWait");
    }

    /**
     * @return Returns whether we are currently doing a receiveWithWait()
     */
    public boolean isCurrentlyDoingReceiveWithWait() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "isCurrentlyDoingReceiveWithWait");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "isCurrentlyDoingReceiveWithWait", currentlyDoingReceiveWithWait);
        return currentlyDoingReceiveWithWait;
    }

    /**
     * Set the request number.
     * 
     * @param requestNumber The request number.
     */
    public void setRequestNumber(int requestNumber) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setRequestNumber", requestNumber);
        this.requestNumber = requestNumber;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setRequestNumber");
    }

    /**
     * @return Returns whether we have sent a response.
     */
    public synchronized boolean isComplete() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "isComplete");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "isComplete", completed);
        return completed;
    }

    /**
     * Sets whether we have set a response or not.
     * 
     * @param completed
     */
    public synchronized void setComplete(boolean completed) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setComplete", completed);
        this.completed = completed;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setComplete");
    }

    /**
     * Sets the Alarm that is running with asynch session.
     * 
     * @param alarm
     */
    public void setCATTimer(Alarm alarm) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setCATTimer", alarm);
        this.alarm = alarm;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setCATTimer");
    }

    /**
     * Stops the main consumer session so that we can be sure
     * that no more messages will be delivered.
     * 
     * @throws SIException
     */
    public void stopSession() throws SIException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "stopSession");
        mainConsumer.getConsumerSession().stop();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "stopSession");
    }

    /**
     * This method will set the transaction that the current
     * receive should be performed with.
     * 
     * @param tran
     */
    public void setTransaction(int tran) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setTransaction", tran);
        this.transaction = tran;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setTransaction");
    }

    /**
     * This method will be called when the asynch consumer receives a message.
     * The message batch size will always be set to one, as this class will
     * only be used when the client issues a synchronous receive. Therefore,
     * they are only wanting one message to be returned.
     * 
     * @param lme The message received.
     */
    public void consumeMessages(LockedMessageEnumeration lme) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "consumeMessages", lme);

        // Cancel the alarm - this has no effect if it
        // is already cancelled. Note this could be null
        // if there is a message available immediately and
        // the alarm has not had chance to be started yet
        if (alarm != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Cancelling the alarm: " + alarm.toString());
            alarm.cancel();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Alarm cancelled");
        }

        // We need to perform this check here as it is possible that while we were cancelling the
        // alarm, the alarm was already processing. To guarentee integrity, we should check the
        // result of the synchronized isComplete() method to find out whether the alarm is or has
        // processed this receive request.
        // If we find the alarm has dealt with this we should ensure the message is unlocked so that
        // it can get redelivered to another suitable consumer.
        if (isComplete()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "This session has already consumed a message - not processing");
            // Make sure we unlock the message. Note there will only ever
            // be one here.
            try {
                lme.nextLocked();
                lme.unlockCurrent();
            } catch (SIException si) {
                //No FFDC code needed
                //Only FFDC if we haven't received a meTerminated event.
                if (!((ConversationState) mainConsumer.getConversation().getAttachment()).hasMETerminated()) {
                    // Not a lot we can do with this
                    FFDCFilter.processException(si, CLASS_NAME + ".consumeMessages",
                                                CommsConstants.CATSYNCASYNCHREADER_CONSUME_MSGS_04, this);
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, si.getMessage(), si);
            }

            // NOTE: Early return
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "consumeMessages");
            return;
        }

        // Set the flag to say that we have got a message.
        // Do this because if this method gets called, this is the
        // end of the line - we either send a message or an exception
        // back. This will stop the timer expiring (and sending a message)
        // while we are sending the message.
        setComplete(true);

        // Stop the session
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Stopping the session");
            stopSession();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Session stopped");
        } catch (SIException si) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!((ConversationState) mainConsumer.getConversation().getAttachment()).hasMETerminated()) {
                FFDCFilter.processException(si, CLASS_NAME + ".consumeMessages",
                                            CommsConstants.CATSYNCASYNCHREADER_CONSUME_MSGS_01, this);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, si.getMessage(), si);
        }

        // Get the message
        try {
            JsMessage message = (JsMessage) lme.nextLocked();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Received message", message);

            // Delete the message
            SITransaction siTran =
                            ((ServerLinkLevelState) conversation.getLinkLevelAttachment()).getTransactionTable().get(transaction);

            if (siTran != IdToTransactionTable.INVALID_TRANSACTION) {
                lme.deleteCurrent(siTran);
            } else {
                lme.unlockCurrent();
            }

            // Send the message back to the client
            sendMessageToClient(message);

            // Update the lowest message priority
            mainConsumer.setLowestPriority(JFapChannelConstants.getJFAPPriority(message.getPriority()));
        } catch (SIException si) {
            // We will only get an exception here if a core API
            // error is encountered. As such, flow that back to the calling
            // client and FFDC

            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!((ConversationState) conversation.getAttachment()).hasMETerminated()) {
                FFDCFilter.processException(si, CLASS_NAME + ".consumeMessages",
                                            CommsConstants.CATSYNCASYNCHREADER_CONSUME_MSGS_03, this);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, si.getMessage(), si);

            StaticCATHelper.sendExceptionToClient(si,
                                                  CommsConstants.CATSYNCASYNCHREADER_CONSUME_MSGS_03,
                                                  conversation, requestNumber);
        } finally {
            //We now no longer need a registered listener as the current synchronous receive is complete.
            try {
                final MPConsumerSession mpSession = (MPConsumerSession) mainConsumer.getConsumerSession();
                mpSession.getConnection().removeConnectionListener(this);
            } catch (SIException e) {
                //No FFDC code needed
                //Only FFDC if we haven't received a meTerminated event.
                if (!((ConversationState) conversation.getAttachment()).hasMETerminated()) {
                    FFDCFilter.processException(e, CLASS_NAME + ".consumeMessages", CommsConstants.CATSYNCASYNCHREADER_CONSUME_MSGS_05, this);
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, e.getMessage(), e);

                //No need to send an exception back to the client in this case as we are only really performing tidy up processing.
                //Also, if it is a really bad problem we are likely to have already send the exception back in the previous catch block.
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "consumeMessages");
    }

    /**
     * This method will send a JsMessage back to the client in response to a synchronous receive.
     * If the FAP level is 9 or above the message can be sent back to the client in chunks (rather
     * than as a single large message) if the message is big enough to make this worthwhile.
     * 
     * @param jsMessage The message to send
     * 
     * @throws SIResourceException
     */
    private void sendMessageToClient(JsMessage jsMessage) throws SIResourceException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "sendMessageToClient", jsMessage);

        setCurrentlyDoingReceiveWithWait(false);

        try {
            // If we are at FAP9 or above we can do a 'chunked' send of the message in seperate
            // slices to make life easier on the Java memory manager
            final HandshakeProperties props = conversation.getHandshakeProperties();
            if (props.getFapLevel() >= JFapChannelConstants.FAP_VERSION_9) {
                sendChunkedMessage(jsMessage);
            } else {
                sendEntireMessage(jsMessage, null);
            }
        } catch (Exception e) {
            FFDCFilter.processException(e, CLASS_NAME + ".sendMessageToClient",
                                        CommsConstants.CATSYNCASYNCHREADER_SEND_MSG_03, this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Encode failed: " + e.getMessage(), e);

            throw new SIResourceException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "sendMessageToClient");
    }

    /**
     * This method sends the message to the peer in chunks. As the request to receive a message is
     * a synchronous request, the client is currently blocked waiting for a reply. As such, the
     * message will be encoded into chunks, sent asynchronously to the client in chunks with the
     * final chunk sent as the reply to the exchange request. All the chunks will be sent at the
     * same priority which means that by the time the exchange reply has reached the peer, the other
     * message chunks will already be there waiting.
     * 
     * @param jsMessage
     * 
     * @throws UnsupportedEncodingException
     * @throws MessageEncodeFailedException
     * @throws IncorrectMessageTypeException
     * @throws MessageCopyFailedException
     */
    private void sendChunkedMessage(JsMessage jsMessage)
                    throws UnsupportedEncodingException, MessageEncodeFailedException,
                    IncorrectMessageTypeException, MessageCopyFailedException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "sendChunkedMessage", jsMessage);

        int msgLen = 0;

        // First of all we must encode the message ourselves
        CommsServerByteBuffer buffer = poolManager.allocate();
        ConversationState convState = (ConversationState) conversation.getAttachment();

        try {
            List<DataSlice> messageSlices = buffer.encodeFast(jsMessage,
                                                              convState.getCommsConnection(),
                                                              conversation);

            // Do a check on the size of the message. If it is less than our threshold, forget the
            // chunking and simply send the message as one
            for (DataSlice slice : messageSlices)
                msgLen += slice.getLength();
            if (msgLen < CommsConstants.MINIMUM_MESSAGE_SIZE_FOR_CHUNKING) {
                // The message is a tiddler, send it in one
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Message is smaller than " +
                                                           CommsConstants.MINIMUM_MESSAGE_SIZE_FOR_CHUNKING);
                sendEntireMessage(jsMessage, messageSlices);
            } else {
                short jfapPriority = JFapChannelConstants.getJFAPPriority(jsMessage.getPriority());
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Sending with JFAP priority of " + jfapPriority);

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

                    // Now add all the header information
                    buffer.putShort(convState.getConnectionObjectId());
                    if (!mainConsumer.getUsingConnectionReceive()) {
                        buffer.putShort(mainConsumer.getConsumerSessionId());
                    }
                    buffer.put(flags);
                    buffer.putDataSlice(slice);

                    // Decide on the segment
                    int seg = 0;
                    if (mainConsumer.getUsingConnectionReceive()) {
                        if (last)
                            seg = JFapChannelConstants.SEG_RECEIVE_CONN_MSG_R;
                        else
                            seg = JFapChannelConstants.SEG_CHUNKED_SYNC_CONN_MESSAGE;
                    } else {
                        if (last)
                            seg = JFapChannelConstants.SEG_RECEIVE_SESS_MSG_R;
                        else
                            seg = JFapChannelConstants.SEG_CHUNKED_SYNC_SESS_MESSAGE;
                    }

                    conversation.send(buffer,
                                      seg,
                                      (last ? requestNumber : 0),
                                      jfapPriority,
                                      false,
                                      ThrottlingPolicy.BLOCK_THREAD,
                                      null);
                }


                mainConsumer.messagesSent++;
            }
        } catch (SIException e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!((ConversationState) mainConsumer.getConversation().getAttachment()).hasMETerminated()) {
                FFDCFilter.processException(e, CLASS_NAME + ".sendChunkedMessage",
                                            CommsConstants.CATSYNCASYNCHREADER_SEND_MSG_02, this);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2015", e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "sendChunkedMessage");
    }

    /**
     * Sends the message in one transmission back down to our peer. If the messageSlices parameter is
     * not null then the message has already been encoded and does not need to be done again. This
     * may be in the case where the message was destined to be sent in chunks but is so small that it
     * does not seem worth it.
     * 
     * @param jsMessage The entire message to send.
     * @param messageSlices The already encoded message slices.
     * 
     * @throws UnsupportedEncodingException
     * @throws MessageCopyFailedException
     * @throws IncorrectMessageTypeException
     * @throws MessageEncodeFailedException
     */
    private void sendEntireMessage(JsMessage jsMessage, List<DataSlice> messageSlices)
                    throws UnsupportedEncodingException, MessageCopyFailedException,
                    IncorrectMessageTypeException, MessageEncodeFailedException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "sendEntireMessage",
                                           new Object[] { jsMessage, messageSlices });

        int msgLen = 0;

        try {
            CommsServerByteBuffer buffer = poolManager.allocate();

            ConversationState convState = (ConversationState) conversation.getAttachment();
            buffer.putShort(convState.getConnectionObjectId());
            if (!mainConsumer.getUsingConnectionReceive()) {
                buffer.putShort(mainConsumer.getConsumerSessionId());
            }
            // Put the message into the buffer in whatever way is suitable
            if (messageSlices == null) {
                msgLen = buffer.putMessage(jsMessage,
                                           convState.getCommsConnection(),
                                           conversation);
            } else {
                msgLen = buffer.putMessgeWithoutEncode(messageSlices);
            }

            // Decide on the segment
            int seg = JFapChannelConstants.SEG_RECEIVE_SESS_MSG_R;
            if (mainConsumer.getUsingConnectionReceive()) {
                seg = JFapChannelConstants.SEG_RECEIVE_CONN_MSG_R;
            }

            int jfapPriority = JFapChannelConstants.getJFAPPriority(jsMessage.getPriority());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Sending with JFAP priority of " + jfapPriority);

            conversation.send(buffer,
                              seg,
                              requestNumber,
                              jfapPriority,
                              false,
                              ThrottlingPolicy.BLOCK_THREAD,
                              null);

            mainConsumer.messagesSent++;
        } catch (SIException e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!((ConversationState) mainConsumer.getConversation().getAttachment()).hasMETerminated()) {
                FFDCFilter.processException(e, CLASS_NAME + ".sendEntireMessage",
                                            CommsConstants.CATSYNCASYNCHREADER_SEND_MSG_01, this);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2015", e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "sendEntireMessage");
    }

    /**
     * This method will send a message to the client informing it that
     * no message was received.
     */
    protected void sendNoMessageToClient() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "sendNoMessageToClient");

        // Ensure that we set the flag that we have done all we can
        setCurrentlyDoingReceiveWithWait(false);
        setComplete(true);

        CommsServerByteBuffer buffer = poolManager.allocate();
        int msgLen = -1;
        ConversationState convState = (ConversationState) conversation.getAttachment();

        buffer.putShort(convState.getConnectionObjectId());
        if (!mainConsumer.getUsingConnectionReceive())
            buffer.putShort(mainConsumer.getConsumerSessionId());
        buffer.putLong(msgLen);

        int seg = JFapChannelConstants.SEG_RECEIVE_SESS_MSG_R;
        if (mainConsumer.getUsingConnectionReceive()) {
            seg = JFapChannelConstants.SEG_RECEIVE_CONN_MSG_R;
        }

        try {
            conversation.send(buffer,
                              seg,
                              requestNumber,
                              JFapChannelConstants.PRIORITY_MEDIUM,
                              true,
                              ThrottlingPolicy.BLOCK_THREAD,
                              null);
        } catch (SIException c) {
            FFDCFilter.processException(c, CLASS_NAME + ".sendNoMessageToClient",
                                        CommsConstants.CATSYNCASYNCHREADER_SEND_NO_MSG_01, this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, c.getMessage(), c);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2015", c);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "sendNoMessageToClient");
    }

    /**
     * This method sends an exception to the client and marks the reader
     * that a response has been sent to the client.
     * 
     * @param e The exception to send back
     * @param probeId The probe id.
     */
    protected void sendErrorToClient(Throwable e, String probeId) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "sendErrorToClient", new Object[] { e, probeId });

        setComplete(true);
        setCurrentlyDoingReceiveWithWait(false);

        StaticCATHelper.sendExceptionToClient(e, probeId, conversation, requestNumber);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "sendErrorToClient");
    }

    /**
     * @return Returns true if the Conversation associated with this session is closed.
     */
    protected boolean isConversationClosed() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "isConversationClosed");
        boolean isClosed = conversation.isClosed();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "isConversationClosed", isClosed);
        return isClosed;
    }

    /**
     * @return Returns some status about this listener.
     */
    @Override
    public String toString() {
        return "currentlyDoingReceiveWithWait: " + currentlyDoingReceiveWithWait;
    }

    /**
     * Common routine used by all the connection listener methods that
     * deliver an exception to the client.
     */
    private void deliverAsynchExceptionToClient(Throwable throwable, String probeId) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "deliverAsynchExceptionToClient", throwable);

        if (!isComplete()) {
            if (alarm != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Cancelling the alarm: " + alarm.toString());
                alarm.cancel();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Alarm cancelled");
            }

            if (!isComplete()) {
                //Deregister listener as we are now done with our receive.
                //Deregistration will probably fail in most cases so ignore any exceptions.
                try {
                    final MPConsumerSession mpSession = (MPConsumerSession) mainConsumer.getConsumerSession();
                    mpSession.getConnection().removeConnectionListener(this);
                } catch (SIException s) {
                    //No FFDC code needed as everything has likely gone wrong already at this point.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, s.getMessage(), s);
                }

                sendErrorToClient(throwable, probeId);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "deliverAsynchExceptionToClient");
    }

    /** @see com.ibm.wsspi.sib.core.SICoreConnectionListener#asynchronousException(com.ibm.wsspi.sib.core.ConsumerSession, java.lang.Throwable) */
    public void asynchronousException(ConsumerSession consumer, Throwable exception) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "asynchronousException", new Object[] { consumer, exception });
        deliverAsynchExceptionToClient(exception, CommsConstants.CATSYNCASYNCHREADER_ASYNCHEXCEPTION_01);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "asynchronousException");
    }

    /** @see com.ibm.wsspi.sib.core.SICoreConnectionListener#meQuiescing(com.ibm.wsspi.sib.core.SICoreConnection) */
    public void meQuiescing(SICoreConnection conn) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "meQuiescing", conn);

        // This is information only - it should not prevent any asynchronous consumers
        // having their consumeMessages methods driven.

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "meQuiescing", conn);
    }

    /** @see com.ibm.wsspi.sib.core.SICoreConnectionListener#commsFailure(com.ibm.wsspi.sib.core.SICoreConnection, com.ibm.wsspi.sib.core.exception.SIConnectionLostException) */
    public void commsFailure(SICoreConnection conn, SIConnectionLostException exception) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "commsFailure", new Object[] { conn, exception });
        deliverAsynchExceptionToClient(exception, CommsConstants.CATSYNCASYNCHREADER_COMMSFAILURE_01);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "commsFailure");
    }

    /** @see com.ibm.wsspi.sib.core.SICoreConnectionListener#meTerminated(com.ibm.wsspi.sib.core.SICoreConnection) */
    public void meTerminated(SICoreConnection conn) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "meTerminated", conn);
        deliverAsynchExceptionToClient(new SIErrorException(), CommsConstants.CATSYNCASYNCHREADER_METERMINATED_01);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "meTerminated");
    }

    /**
     * Returns the value of hasMETerminated from the ConversationState object associated with this object.
     * 
     * @see ConversationState#hasMETerminated()
     * 
     * @return
     */
    public boolean hasMETerminated() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "hasMETerminated");
        final boolean hasMETerminated = ((ConversationState) mainConsumer.getConversation().getAttachment()).hasMETerminated();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "hasMETerminated", hasMETerminated);
        return hasMETerminated;
    }

    /**
     * @return the CATMainConsumer associated with this class.
     */
    CATMainConsumer getCATMainConsumer() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getCATMainConsumer");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getCATMainConsumer", mainConsumer);
        return mainConsumer;
    }
}
