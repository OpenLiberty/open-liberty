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

import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.server.CommsServerByteBuffer;
import com.ibm.ws.sib.comms.server.ConversationState;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.HandshakeProperties;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.Conversation.ThrottlingPolicy;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.BrowserSession;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 * This class is the specific consumer handler for browser sessions.
 * 
 * @author Adrian Preston
 */
public class CATBrowseConsumer extends CATConsumer {
    /** Class name for FFDC's */
    private static String CLASS_NAME = CATBrowseConsumer.class.getName();

    /** Trace */
    private static final TraceComponent tc = SibTr.register(CATBrowseConsumer.class,
                                                            CommsConstants.MSG_GROUP,
                                                            CommsConstants.MSG_BUNDLE);

    // A reference to the main consumer which wraps us.
    private CATMainConsumer mainConsumer = null;

    // The number of bytes we have sent to the client as part of pacing.
    private long sentBytes = 0;

    // The current message batch number to use for the messages sent to the client.
    private short msgBatch = 0;

    /**
     * Constructor. Creates a new browse consumer.
     * 
     * @param mainConsumer
     */
    public CATBrowseConsumer(CATMainConsumer mainConsumer) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", mainConsumer);
        this.mainConsumer = mainConsumer;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "<init>");
    }

    /**
     * Private exception class used to notify methods in this class when one of the
     * helper functions fails. This is used to map one of the several disperate exceptions
     * a given helper method might encounter into a generic "badness happened" notification
     * to the caller.
     */
    private class OperationFailedException extends Exception {
        private static final long serialVersionUID = -5191481178136106792L;
    }

    /**
     * Helper function. Sends an SIBusMessage to the client, taking care of the
     * myriad pesky exceptions which may get thrown. If something does go wrong then
     * the caller is notified by a single OperationFailedException. By this point the
     * appropriate error flow has also been transmitted to the client.
     * 
     * @param msg The message to send.
     * 
     * @return long The size of the data sent.
     * 
     * @throws OperationFailedException Thrown if something goes wrong. Before this is
     *             thrown the appropriate clean up and notification of the client will have occurred.
     *             It is intended that this exception is used to notify the caller to abort whatever
     *             it was doing.
     */
    private long sendMessage(SIBusMessage msg) throws OperationFailedException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "sendMessage", msg);

        long retValue = 0;

        // If we are at FAP9 or above we can do a 'chunked' send of the message in seperate
        // slices to make life easier on the Java memory manager
        final HandshakeProperties props = getConversation().getHandshakeProperties();
        if (props.getFapLevel() >= JFapChannelConstants.FAP_VERSION_9) {
            retValue = sendChunkedMessage(msg);
        } else {
            retValue = sendEntireMessage(msg, null);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "sendMessage", "" + retValue);
        return retValue;
    }

    /**
     * This method is used to send the entire message to the peer in one go. If the message has
     * already been encoded into slices (and passed in using the messageSlices parameter) this data
     * is put into the buffer.
     * 
     * @param msg
     * @param messageSlices
     * 
     * @return Returns the amount of data sent.
     * 
     * @throws OperationFailedException
     */
    private long sendEntireMessage(SIBusMessage msg, List<DataSlice> messageSlices)
                    throws OperationFailedException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "sendEntireMessage", new Object[] { msg, messageSlices });

        ConversationState convState = (ConversationState) getConversation().getAttachment();
        long retValue = 0;

        // Now build the header for this message.
        CommsServerByteBuffer buffer = poolManager.allocate();
        try {
            buffer.putShort(convState.getConnectionObjectId());
            buffer.putShort(mainConsumer.getClientSessionId());
            buffer.putShort(msgBatch);
            int msgLen = 0;
            if (messageSlices == null) {
                msgLen = buffer.putMessage((JsMessage) msg,
                                           convState.getCommsConnection(),
                                           getConversation());
            } else {
                msgLen = buffer.putMessgeWithoutEncode(messageSlices);
            }

            // Perform the send (and take care of any exceptions)
            try {
                retValue = getConversation().send(buffer,
                                                  JFapChannelConstants.SEG_BROWSE_MESSAGE,
                                                  0,
                                                  JFapChannelConstants.PRIORITY_LOWEST,
                                                  false,
                                                  ThrottlingPolicy.BLOCK_THREAD,
                                                  null);


            } catch (SIException e) {
                FFDCFilter.processException(e, CLASS_NAME + ".sendEntireMessage",
                                            CommsConstants.CATBROWSECONSUMER_SENDMESSAGE_02,
                                            this);

                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2012", e);

                // No point trying to transmit this to the client, as it has already gone away.
                throw new OperationFailedException();
            }
        } catch (Exception e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event OR if this Exception is a SIException.
            if (!(e instanceof SIException) || !convState.hasMETerminated()) {
                FFDCFilter.processException(e, CLASS_NAME + ".sendEntireMessage",
                                            CommsConstants.CATBROWSECONSUMER_SENDMESSAGE_01,
                                            this);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.exception(this, tc, e);

            // If the connection drops during the encode, don't try and send an exception to the client
            if (e instanceof SIConnectionDroppedException) {
                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2012", e);

                // No point trying to transmit this to the client, as it has already gone away.
                throw new OperationFailedException();
            }

            SIResourceException coreException = new SIResourceException();
            coreException.initCause(e);
            StaticCATHelper.sendAsyncExceptionToClient(coreException,
                                                       CommsConstants.CATBROWSECONSUMER_SENDMESSAGE_01,
                                                       getClientSessionId(), getConversation(), 0);
            throw new OperationFailedException();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "sendEntireMessage", retValue);
        return retValue;
    }

    /**
     * Sends a message to the client in chunks. This is easier on the Java memory manager as it means
     * it can avoid allocating very large chunks of memory in one contiguous block. This may result
     * in multiple JFap sends for each message chunk.
     * 
     * @param sibMessage
     * 
     * @return Returns the amount of data sent across to the peer.
     * 
     * @throws OperationFailedException
     */
    private long sendChunkedMessage(SIBusMessage sibMessage) throws OperationFailedException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "sendChunkedMessage", sibMessage);

        long retValue = 0;

        // First of all we must encode the message ourselves
        CommsServerByteBuffer buffer = poolManager.allocate();
        ConversationState convState = (ConversationState) getConversation().getAttachment();

        try {
            List<DataSlice> messageSlices = buffer.encodeFast((JsMessage) sibMessage,
                                                              convState.getCommsConnection(),
                                                              getConversation());

            int msgLen = 0;
            // Do a check on the size of the message. If it is less than our threshold, forget the
            // chunking and simply send the message as one
            for (DataSlice slice : messageSlices)
                msgLen += slice.getLength();
            if (msgLen < CommsConstants.MINIMUM_MESSAGE_SIZE_FOR_CHUNKING) {
                // The message is a tiddler, send it in one
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Message is smaller than " +
                                                           CommsConstants.MINIMUM_MESSAGE_SIZE_FOR_CHUNKING);
                retValue = sendEntireMessage(sibMessage, messageSlices);
            } else {
                short jfapPriority = JFapChannelConstants.getJFAPPriority(sibMessage.getPriority());
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Sending with JFAP priority of " + jfapPriority);

                // Now we have the slices, send each one in turn. Each slice contains all the header
                // information so that the client code knows what to do with the message
                try {
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
                        buffer.putShort(mainConsumer.getClientSessionId());
                        buffer.putShort(msgBatch); // BIT16 Message batch
                        buffer.put(flags);
                        buffer.putDataSlice(slice);

                        retValue = getConversation().send(buffer,
                                                          JFapChannelConstants.SEG_CHUNKED_BROWSE_MESSAGE,
                                                          0,
                                                          jfapPriority,
                                                          false,
                                                          ThrottlingPolicy.BLOCK_THREAD,
                                                          null);
                    }


                    messagesSent++;
                } catch (SIException e) {
                    FFDCFilter.processException(e,
                                                CLASS_NAME + ".sendChunkedMessage",
                                                CommsConstants.CATBROWSECONSUMER_SEND_CHUNKED_MSG_01,
                                                this);

                    SibTr.error(tc, "COMMUNICATION_ERROR_SICO2012", e);
                }
            }
        } catch (Exception e) {
            FFDCFilter.processException(e, CLASS_NAME + ".sendChunkedMessage",
                                        CommsConstants.CATBROWSECONSUMER_SEND_CHUNKED_MSG_02,
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.exception(this, tc, e);

            // If the connection drops during the encode, don't try and send an exception to the client
            if (e instanceof SIConnectionDroppedException) {
                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2012", e);

                // No point trying to transmit this to the client, as it has already gone away.
                throw new OperationFailedException();
            }

            SIResourceException coreException = new SIResourceException();
            coreException.initCause(e);
            StaticCATHelper.sendAsyncExceptionToClient(coreException,
                                                       CommsConstants.CATBROWSECONSUMER_SEND_CHUNKED_MSG_02,
                                                       getClientSessionId(), getConversation(), 0);
            throw new OperationFailedException();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "sendChunkedMessage", retValue);
        return retValue;
    }

    /**
     * Helper method. Wraps the action of getting the next message for a browser session
     * and deals with the exceptions which may be thrown. If an exception is thrown the
     * client is notified (if appropriate) and an OperationFailedException is thrown.
     * This allows the caller to notice something bad has happened, but it need not take
     * any further action.
     * 
     * @param browserSession The browser session to get the next message from.
     * @param conversation The conversation to notify of any exceptions.
     * @param requestNumber The request number to use when performing exception notification.
     * 
     * @return SIBusMessage The message (if any) returned from the browser session next call.
     * 
     * @throws OperationFailedException Thrown if something bad happens. Before this is
     *             thrown the appropriate client notification of badness will have happened.
     */
    private SIBusMessage getNextMessage(BrowserSession browserSession,
                                        Conversation conversation,
                                        short requestNumber)
                    throws OperationFailedException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getNextMessage",
                                           new Object[]
                                           {
                                              browserSession,
                                              conversation,
                                              "" + requestNumber
                                           });

        SIBusMessage msg = null;

        try {
            msg = browserSession.next();
        } catch (SIException e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!((ConversationState) conversation.getAttachment()).hasMETerminated()) {
                FFDCFilter.processException(e, CLASS_NAME + ".getNextMessage",
                                            CommsConstants.CATBROWSECONSUMER_GETNEXTMESSAGE_01,
                                            this);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.CATBROWSECONSUMER_GETNEXTMESSAGE_01,
                                                  conversation, requestNumber);
            throw new OperationFailedException();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getNextMessage", msg);
        return msg;
    }

    /**
     * Invoked when the client requests some more messages for a browser session.
     * This code attempts to browse a number of messages (using the pacing algorithm
     * described in the Client Design Document) and send them to the client.
     * 
     * @param requestNumber
     * @param receiveBytes
     * @param requestedBytes
     */
    @Override
    public void requestMsgs(int requestNumber, int receiveBytes, int requestedBytes) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "requestMessages",
                                           new Object[]
                                           {
                                              "" + requestNumber,
                                              "" + receiveBytes,
                                              "" + requestedBytes
                                           });

        BrowserSession browserSession = mainConsumer.getBrowserSession();
        Conversation conversation = getConversation();

        // Calculate the number of bytes we should send
        sentBytes -= receiveBytes;
        boolean done = sentBytes > requestedBytes;
        try {
            // Loop, sending messages to the client until we either run out or
            // hit the size limit imposed by our pacing algorithm.
            while (!done) {

                SIBusMessage msg = null;
                msg = getNextMessage(browserSession, conversation, (short) requestNumber);

                done = msg == null;
                if (!done) {
                    sentBytes += sendMessage(msg);
                }

                done |= sentBytes > requestedBytes;
            }
        } catch (OperationFailedException e) {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.exception(this, tc, e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "requestMessages");
    }

    /**
     * Called in response to a client requesting we reset the browse cursor for
     * a particular broser session.
     * 
     * @throws SISessionUnavailableException
     * @throws SISessionDroppedException
     * @throws SIConnectionUnavailableException
     * @throws SIConnectionDroppedException
     * @throws SIResourceException
     * @throws SIConnectionLostException
     * @throws SIErrorException
     */
    @Override
    public void reset()
                    throws SISessionUnavailableException, SISessionDroppedException,
                    SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException,
                    SIErrorException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "reset");

        BrowserSession browserSession = mainConsumer.getBrowserSession();
        ++msgBatch;
        browserSession.reset();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "reset");
    }

    /**
     * Invoked when the client sends a flush consumer. Since browser sessions do
     * not have an activeConsumer method this translates to attempting a single
     * browse next and sending back the result.
     * 
     * @param requestNumber
     */
    @Override
    public void flush(int requestNumber) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "flush", "" + requestNumber);

        // Locate the browser session to use.
        BrowserSession browserSession = mainConsumer.getBrowserSession();
        SIBusMessage msg = null;

        // Browse the next message and send it to the client.
        try {
            msg = getNextMessage(browserSession, getConversation(), (short) requestNumber);
            if (msg != null)
                sendMessage(msg);

            // Send a response to the browse request.
            try {
                getConversation().send(poolManager.allocate(),
                                       JFapChannelConstants.SEG_FLUSH_SESS_R,
                                       requestNumber,
                                       Conversation.PRIORITY_LOWEST,
                                       true,
                                       ThrottlingPolicy.BLOCK_THREAD,
                                       null);
            } catch (SIException e) {
                FFDCFilter.processException(e, CLASS_NAME + ".flush",
                                            CommsConstants.CATBROWSECONSUMER_FLUSH_01,
                                            this);

                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2012", e);

                // Nothing else we can do at this point as the comms exception suggests that the
                // conversation is dead, anyway.
            }

        } catch (OperationFailedException e) {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.exception(this, tc, e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "flush");
    }

    /**
     * Closes the browser.
     * 
     * @param requestNumber
     */
    @Override
    public void close(int requestNumber) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "close", "" + requestNumber);

        BrowserSession browserSession = mainConsumer.getBrowserSession();
        try {
            browserSession.close();
        } catch (SIException e) {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if (!((ConversationState) getConversation().getAttachment()).hasMETerminated()) {
                FFDCFilter.processException(e, CLASS_NAME + ".close",
                                            CommsConstants.CATBROWSECONSUMER_CLOSE_01,
                                            this);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.CATBROWSECONSUMER_CLOSE_01,
                                                  getConversation(), requestNumber);
        }

        try {
            getConversation().send(poolManager.allocate(),
                                   JFapChannelConstants.SEG_CLOSE_CONSUMER_SESS_R,
                                   requestNumber,
                                   Conversation.PRIORITY_LOWEST,
                                   true,
                                   ThrottlingPolicy.BLOCK_THREAD,
                                   null);
        } catch (SIException e) {
            FFDCFilter.processException(e, CLASS_NAME + ".close",
                                        CommsConstants.CATBROWSECONSUMER_CLOSE_02,
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, e.getMessage(), e); // d175222

            // Cannot do anything else at this point as a comms exception suggests that the
            // connection is unusable.
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "close");
    }

    /** @see com.ibm.ws.sib.comms.server.clientsupport.CATConsumer#getConsumerSession() */
    @Override
    protected ConsumerSession getConsumerSession() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getConsumerSession");
        ConsumerSession sess = mainConsumer.getConsumerSession();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getConsumerSession", sess);
        return sess;
    }

    /** @see com.ibm.ws.sib.comms.server.clientsupport.CATConsumer#getConversation() */
    @Override
    protected Conversation getConversation() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getConversation");
        Conversation conv = mainConsumer.getConversation();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getConversation", conv);
        return conv;
    }

    /** @see com.ibm.ws.sib.comms.server.clientsupport.CATConsumer#getLowestPriority() */
    @Override
    protected int getLowestPriority() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getLowestPriority");
        int lowestPri = mainConsumer.getLowestPriority();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getLowestPriority", lowestPri);
        return lowestPri;
    }

    /** @see com.ibm.ws.sib.comms.server.clientsupport.CATConsumer#getClientSessionId() */
    @Override
    protected short getClientSessionId() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getClientSessionId");
        short sessId = mainConsumer.getClientSessionId();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getClientSessionId");
        return sessId;
    }

    /** @see com.ibm.ws.sib.comms.server.clientsupport.CATConsumer#getUnrecoverableReliability() */
    @Override
    protected Reliability getUnrecoverableReliability() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getUnrecoverableReliability");
        Reliability rel = mainConsumer.getUnrecoverableReliability();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getUnrecoverableReliability");
        return rel;
    }
}
