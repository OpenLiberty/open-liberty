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

package com.ibm.ws.sib.processor.gd;

// Import required classes.
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.control.ControlNack;
import com.ibm.ws.sib.mfp.control.ControlSilence;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.gd.SourceStream.SourceStreamState;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.DownstreamControl;
import com.ibm.ws.sib.processor.impl.interfaces.HealthStateListener;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.interfaces.UpstreamControl;
import com.ibm.ws.sib.processor.runtime.HealthState;
import com.ibm.ws.sib.processor.runtime.impl.InternalOutputStreamControl;
import com.ibm.ws.sib.processor.utils.am.MPAlarmManager;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;
//PM62615.dev
//PM62615.dev

/**
 * An output stream for serving a downstream cellule.
 * 
 */
public class InternalOutputStream extends ControllableStream
{

    /**
     * Class that implements blocked stream health checker
     */
    public class BlockedStreamAlarm implements AlarmListener
    {
        private final InternalOutputStream sourceStream;
        private long previousCompletedPrefix;

        public BlockedStreamAlarm(InternalOutputStream sourceStream, long completedPrefix)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "BlockedStreamAlarm", sourceStream);

            this.sourceStream = sourceStream;
            previousCompletedPrefix = completedPrefix;

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "BlockedStreamAlarm", this);
        }

        public void checkState(boolean create) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "checkState", Boolean.valueOf(create));

            synchronized (sourceStream)
            {
                if (hasMsgsOnStream())
                {
                    // Check the completed prefix is different from the previous check
                    long completedPrefix = getCompletedPrefix();
                    oststream.setCursor(completedPrefix + 1);

                    if (oststream.getNext().type == TickRange.Uncommitted &&
                        completedPrefix == previousCompletedPrefix)
                    {
                        // Update the health to report that there has been no change at the front of the stream
                        // This means that the msg at the head of the stream has not been committed.
                        // Update the health state of this stream 
                        getControlAdapter().getHealthState().updateHealth(HealthStateListener.BLOCKED_STREAM_STATE,
                                                                          HealthState.AMBER);
                    }
                    else
                    {
                        previousCompletedPrefix = completedPrefix;
                        // Update health to green
                        getControlAdapter().getHealthState().updateHealth(HealthStateListener.BLOCKED_STREAM_STATE,
                                                                          HealthState.GREEN);
                    }

                    // Reset the timer
                    if (create)
                        am.create(GDConfig.BLOCKED_STREAM_HEALTH_CHECK_INTERVAL, this);
                }
                else
                {
                    // Cancel the alarm - no msgs on stream
                    if (create)
                        blockedStreamAlarm = null;

                    // Update health to green
                    getControlAdapter()
                                    .getHealthState().updateHealth(HealthStateListener.BLOCKED_STREAM_STATE,
                                                                   HealthState.GREEN);
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkState");

        }

        @Override
        public void alarm(Object thandle)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "alarm", thandle);

            checkState(true);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "alarm");

        } // End of method timerExpired

    }

    private UpstreamControl upControl;
    private static TraceComponent tc =
                    SibTr.register(
                                   InternalOutputStream.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    private int priority;
    private Reliability reliability;
    private DownstreamControl downControl;

    private long totalMessagesSent = 0;
    private long timeLastMsgSent = 0;

    // The stream this output stream is associated with
    private SIBUuid12 streamID;

    private StateStream oststream;

    // monotonically increasing and read without synchronization
    private long oack = 0;

    // highest ack received
    private long ack = 0;

    // last tick sent from this stream
    private long lastSent = 0;

    //for now we simply use the SourceStreamState enuemration
    private final SourceStreamState internalOutputState = SourceStreamState.ACTIVE;

    private StreamSet streamSet;
    // The latest tick we sent an ackExcpected for on this stream
    private long lastAckExpTick;
    // The latest tick we received a Nack for on this stream
    private long lastNackReceivedTick;

    //Hashmp to maintain tickrange and number of times it received NACKs
    private final HashMap<TickRange, Integer> gapMap = new HashMap<TickRange, Integer>(); //PM62615.dev
    private long _lastTickWhenStreamCreated;//PM62615.dev
    private long _streamStartTick;//PM62615.dev

    // The alarm handle which checks the front of the stream for healthy usage
    private BlockedStreamAlarm blockedStreamAlarm;

    //holds MessageProcessor object
    private MessageProcessor messageProcessor;

    // The alarm manager to use for AckExpected alarms
    private MPAlarmManager am;
    private InternalOutputStreamControl srcStreamControl;

    public InternalOutputStream(
                                int priority,
                                Reliability reliability,
                                long completedPrefix,
                                DownstreamControl downControl,
                                UpstreamControl upControl,
                                List scratch,
                                SIBUuid12 stream,
                                StreamSet streamSet,
                                MPAlarmManager am,
                                MessageProcessor mp,
                                long LastMessageTickWhenStreamCreated)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "InternalOutputStream",
                        new Object[] {
                                      Integer.valueOf(priority),
                                      reliability,
                                      Long.valueOf(completedPrefix),
                                      downControl,
                                      upControl,
                                      scratch,
                                      stream,
                                      streamSet,
                                      am,
                                      mp,
                                      LastMessageTickWhenStreamCreated });

        // synchronization ensures that other synchronized methods will see values 
        // initialized in constructor
        synchronized (this)
        {
            this.streamSet = streamSet;
            this.priority = priority;
            this.reliability = reliability;

            this.downControl = downControl;
            this.upControl = upControl;

            this.streamID = stream;
            this.am = am;
            this.messageProcessor = mp;

            // the state stream is created here
            oststream = new StateStream();
            oststream.init();
            oststream.setCompletedPrefix(completedPrefix);
            //PM62615.dev+
            _lastTickWhenStreamCreated = LastMessageTickWhenStreamCreated;
            _streamStartTick = completedPrefix;
            //PM62615.dev-
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "InternalOutputStream", this);

    }

    public boolean hasMsgsOnStream()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "hasMsgsOnStream");

        boolean hasMsgs = false;
        oststream.setCursor(0);
        // Get the first TickRange
        TickRange tr = oststream.getNext();
        while (tr.endstamp < RangeList.INFINITY)
        {
            if (tr.type == TickRange.Value || tr.type == TickRange.Uncommitted)
            {
                hasMsgs = true;
                break;
            }
            tr = oststream.getNext();
        } // end while

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "hasMsgsOnStream", Boolean.valueOf(hasMsgs));
        return hasMsgs;
    }

    @Override
    public long getCompletedPrefix()
    {
        long returnLong = oststream.getCompletedPrefix();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getCompletedPrefix");
            SibTr.exit(tc, "getCompletedPrefix", Long.valueOf(returnLong));
        }
        return returnLong;
    }

    @Override
    public int getPriority()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getPriority");
            SibTr.exit(tc, "getPriority", Integer.valueOf(priority));
        }
        return priority;
    }

    @Override
    public Reliability getReliability()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getReliability");
            SibTr.exit(tc, "getReliability", reliability);
        }
        return reliability;
    }

    public long getAckPrefix()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getAckPrefix");
            SibTr.exit(tc, "getAckPrefix", Long.valueOf(oack));
        }
        return oack;
    }

    public void setLastSent(long sentTick)
    {
        if (sentTick > lastSent)
            lastSent = sentTick;
    }

    public void sendSilence(long starts, long completedPrefix) throws SIResourceException
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "sendSilence", Long.valueOf(starts));

        // Work out whether to send a Silence message
        // Only need to do this if the last message sent from
        // from this stream is earlier than the startstamp of the
        // incoming message

        // Don't need to send Silence for anything the target has
        // already sent us an Ack for as it must already have it
        if (ack < (starts - 1))
        {
            // This method finds the lowest tick in Completed state
            // that is contiguous with the tick at starts.
            long prev = oststream.discoverPrevCompleted(starts);

            if (prev < ack + 1)
                prev = ack + 1;

            if (prev < starts)
            {
                // Need to send a Silence message from last message sent
                // up to the one we have just added but only back as far as
                // any Unknown ticks which may be in the stream ( this is
                // because ticks for this stream can arrive out of order )
                try
                {

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    {
                        SibTr.debug(tc, "send Silence from: " + prev + " to " + (starts - 1) + " on Stream " + streamID);
                    }

                    downControl.sendSilenceMessage(
                                                   prev,
                                                   starts - 1,
                                                   completedPrefix,
                                                   false, // not requestedOnly
                                                   priority,
                                                   reliability,
                                                   streamID);
                } catch (SIResourceException e)
                {
                    // FFDC
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.gd.InternalOutputStream.sendSilence",
                                                "1:433:1.83.1.1",
                                                this);

                    SibTr.exception(tc, e);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "sendSilence", e);

                    throw e;
                }

                setLastSent(starts - 1);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendSilence");

    }

    // Used for debug
    @Override
    public StateStream getStateStream()
    {
        return oststream;
    }

    public synchronized long getTotalMessagesSent()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getTotalMessagesSent");
            SibTr.exit(tc, "getTotalMessagesSent", Long.valueOf(totalMessagesSent));
        }
        return totalMessagesSent;
    }

    public InternalOutputStreamControl getControlAdapter()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getControlAdapter");
        if (srcStreamControl == null)
            srcStreamControl = new InternalOutputStreamControl(this, streamSet, downControl);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getControlAdapter", srcStreamControl);
        return srcStreamControl;
    }

    /**
     * This method uses a Value message to write a Value tick
     * into the stream.
     * It is called at postCommit time.
     * 
     * @param m The value message to write to the stream
     * 
     * @return boolean true if the message can be sent downstream
     *         message is not sent if it has the RequestedOnly
     *         flag set and its tick in the stream is not in
     *         the Requested state
     * 
     */
    public boolean writeValue(SIMPMessage m) throws SIResourceException
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "writeValue", new Object[] { m });

        TickRange tr = null;

        long msgStoreId = AbstractItem.NO_ID;
        try
        {
            if (m.isInStore())
                msgStoreId = m.getID();
        } catch (MessageStoreException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.gd.InternalOutputStream.writeValue",
                                        "1:513:1.83.1.1",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "writeValue", e);

            throw new SIResourceException(e);
        }

        JsMessage jsMsg = m.getMessage();
        long stamp = jsMsg.getGuaranteedValueValueTick();

        // should a Value message be sent downstream
        // this is the return value of this method
        boolean sendMessage = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            SibTr.debug(tc, "writeValue at: " + stamp + " on Stream " + streamID);
        }
        if (stamp > lastSent)
        {
            totalMessagesSent++;
            timeLastMsgSent = System.currentTimeMillis();
        }

        synchronized (this)
        {
            // If message is before completedPrefix we ignore it and
            // return false
            if (stamp > oststream.getCompletedPrefix())
            {
                if (jsMsg.getGuaranteedValueRequestedOnly())
                {
                    // Only send this Value message if its tick is in 
                    // Requested state in the stream 
                    if (oststream.isRequested(stamp) == false)
                        sendMessage = false;
                }

                // write into stream
                tr = TickRange.newValueTick(stamp, null, msgStoreId);

                oststream.writeRange(tr);

                // SIB0105
                // Message committed so reset the health state
                if (sendMessage && blockedStreamAlarm != null)
                    blockedStreamAlarm.checkState(false);
            }
            else
            {
                sendMessage = false;
            }

        } // end synchronized

        if (sendMessage)
            setLastSent(jsMsg.getGuaranteedValueEndTick());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "writeValue", Boolean.valueOf(sendMessage));

        return sendMessage;

    }

    /**
     * This method uses a Value message to write an Uncommitted tick
     * into the stream.
     * It is called at preCommit time.
     * 
     * @param m The value message to write to the stream
     * 
     */
    public void writeUncommitted(SIMPMessage m)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "writeUncommitted", new Object[] { m });

        TickRange tr = null;

        JsMessage jsMsg = m.getMessage();
        long stamp = jsMsg.getGuaranteedValueValueTick();

        long starts = jsMsg.getGuaranteedValueStartTick();
        long ends = jsMsg.getGuaranteedValueEndTick();
        long completedPrefix;

        tr = TickRange.newUncommittedTick(stamp);
        tr.startstamp = starts;
        tr.endstamp = ends;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            SibTr.debug(tc, "writeUncommitted at: " + stamp + " with Silence from: " + starts + " to " + ends + " on Stream " + streamID);
        }

        synchronized (this)
        {
            // write into stream
            oststream.writeCombinedRange(tr);

            completedPrefix = oststream.getCompletedPrefix();

            sendSilence(starts, completedPrefix);

            // SIB0105
            // If this is the first uncommitted messages to be added to the stream, start off the 
            // blocked message health state timer. This will check back in the future to see if the
            // message successfully sent.
            if (blockedStreamAlarm == null)
            {
                blockedStreamAlarm = new BlockedStreamAlarm(this, getCompletedPrefix());
                am.create(GDConfig.BLOCKED_STREAM_HEALTH_CHECK_INTERVAL, blockedStreamAlarm);
            }

        } // end synchronized

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "writeUncommitted");

    }

    /**
     * This method uses a Value message to write Silence into the stream,
     * either because a message has been filtered out or because it has
     * been rolled back
     * 
     * @param m The value message
     * 
     * @return boolean true if the message can be sent downstream
     *         message is not sent if it has the RequestedOnly
     *         flag set and its tick in the stream is not in
     *         the Requested state
     * 
     */

    public boolean writeSilence(SIMPMessage m)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "writeSilence", new Object[] { m });

        JsMessage jsMsg = m.getMessage();

        // There may be Completed ticks after the Value, if so then
        // write these into the stream too
        long stamp = jsMsg.getGuaranteedValueValueTick();
        long starts = jsMsg.getGuaranteedValueStartTick();
        long ends = jsMsg.getGuaranteedValueEndTick();
        if (ends < stamp)
            ends = stamp;

        TickRange tr =
                        new TickRange(
                                        TickRange.Completed,
                                        starts,
                                        ends);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            SibTr.debug(tc, "writeSilence from: " + starts + " to " + ends + " on Stream " + streamID);
        }

        synchronized (this)
        {
            tr = oststream.writeCompletedRange(tr);

            long completedPrefix = oststream.getCompletedPrefix();
            if (completedPrefix > oack)
            {
                oack = completedPrefix;
            }

            // Only want to send Silence if we know it has been missed
            // because we have sent something beyond it
            if (lastSent > ends)
                sendSilence(ends + 1, completedPrefix);

            // SIB0105
            // Message silenced so reset the health state
            if (blockedStreamAlarm != null)
                blockedStreamAlarm.checkState(false);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "writeSilence");

        setLastSent(jsMsg.getGuaranteedValueEndTick());

        return true;
    }

    /**
     * This method writes the ticks in a Silence message into the stream
     * It is called when a Silence message arrives from another ME
     * If the RequestedOnly flag in the message is set then the message is
     * only sent on to downstream MEs if they have previously requested
     * it. Otherwise it is always sent.
     * 
     * @param m The Silence message
     * 
     */
    public void writeSilence(ControlSilence m) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "writeSilence", new Object[] { m });

        boolean sendMessage = false;

        // to use in messages sent downstream
        long completedPrefix;

        // Construct a TickRange from the Silence message 
        long starts = m.getStartTick();
        long ends = m.getEndTick();
        boolean requestedOnly = m.getRequestedOnly();

        TickRange tr =
                        new TickRange(TickRange.Completed, starts, ends);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            SibTr.debug(tc, "writeSilence from: " + starts + " to " + ends + " on Stream " + streamID);
        }

        synchronized (this)
        {
            if (requestedOnly)
            {
                // Only send this Silence message if there are some ticks in 
                // its tick range which are in Requested state in the stream 
                if (oststream.containsRequested(tr) == false)
                    sendMessage = false;
            }

            // Write the range of Completed ticks to the stream and  
            // get the resultant maximal Completed range
            tr = oststream.writeCompletedRange(tr);

            if (oststream.getCompletedPrefix() > oack)
            {
                oack = oststream.getCompletedPrefix();
            }

            completedPrefix = oststream.getCompletedPrefix();

        } // end synchronized

        if (sendMessage)
        {
            // send silence message corresponding to tr to downstream cellule 
            // to target cellule 
            try
            {
                downControl.sendSilenceMessage(
                                               tr.startstamp,
                                               tr.endstamp,
                                               completedPrefix,
                                               requestedOnly,
                                               priority,
                                               reliability,
                                               streamID);
            } catch (SIResourceException e)
            {
                // FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.gd.InternalOutputStream.writeSilence",
                                            "1:788:1.83.1.1",
                                            this);

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "writeSilence", e);

                throw e;
            }

            setLastSent(tr.endstamp);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "writeSilence", Boolean.valueOf(sendMessage));
    }

    public void processAckExpected(long stamp)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "processAckExpected", new Object[] { Long.valueOf(stamp) });

        if (oack < stamp)
        {
            try
            {
                downControl.sendAckExpectedMessage(
                                                   stamp,
                                                   priority,
                                                   reliability,
                                                   streamID);
            } catch (SIResourceException e)
            {
                // FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.gd.InternalOutputStream.processAckExpected",
                                            "1:828:1.83.1.1",
                                            this);

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "processAckExpected", e);

                throw e;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processAckExpected");
    }

    /**
     * This method is called when an Ack message is recieved from a downstream
     * ME. It updates the ackPrefix of the stream and then passes the message
     * up to the PubSubInputHandler which will aggregate the Acks from all
     * InternalOutputStreams.
     * 
     * @param stamp The ackPrefix
     * 
     */
    public void writeAckPrefix(long stamp) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "writeAckPrefix", Long.valueOf(stamp));

        synchronized (this)
        {
            // SIB0105
            // Update controllable health state if poss
            if (stamp >= lastAckExpTick)
            {
                getControlAdapter().getHealthState().updateHealth(HealthStateListener.ACK_EXPECTED_STATE,
                                                                  HealthState.GREEN);
                // Disable the checking until another ackExpected is sent
                lastAckExpTick = Long.MAX_VALUE;
            }
            if (stamp >= lastNackReceivedTick)
            {
                getControlAdapter()
                                .getHealthState().updateHealth(HealthStateListener.NACK_RECEIVED_STATE,
                                                               HealthState.GREEN);
                // Disable the checking until another ackExpected is sent
                lastNackReceivedTick = Long.MAX_VALUE;
            }

            // Keep track of the highest ack we have received
            if (stamp > ack)
                ack = stamp;

            // This will update the prefix if stamp > completedPrefix
            // It also combines any adjacent Completed ranges 
            oststream.setCompletedPrefix(stamp);
            long newCompletedPrefix = oststream.getCompletedPrefix();

            if (newCompletedPrefix > oack)
            {
                oack = newCompletedPrefix;
                upControl.sendAckMessage(null, null, null,
                                         newCompletedPrefix, priority, reliability, streamID, false);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "writeAckPrefix");
        }
    }

    /**
     * This method is called when a Nack message is received from the
     * downstream ME corresponding to this InternalOutputStream.
     * It sends Value and Silence messages downstream for any ticks
     * in these states in the stream, and for any ticks in Unknown or
     * Requested state it sends a Nack upstream.
     * 
     * @param nm
     * 
     * @return null The corresponding method on the SourceStream returns
     *         the list of messages to be deleted from the ItemStream.
     *         The calling code relies on this method returning null to
     *         indiacte that no messages should be deleted.
     */
    public List processNack(ControlNack nm)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "processNack", new Object[] { nm, Boolean.valueOf(false) });

        boolean sendPending = false;
        ArrayList sendList = new ArrayList();

        boolean sendLeadingSilence = false;
        long lsstart = 0;
        long lsend = 0;
        boolean sendTrailingSilence = false;
        long tsstart = 0;
        long tsend = 0;

        long startstamp = nm.getStartTick();
        long endstamp = nm.getEndTick();

        long completedPrefix;

        // the TickRange to hold information discovered as we traverse stream
        TickRange r = null;

        // Go through oststream and see which ticks in the Nack range are
        // Completed or Value or Uncommitted and send appropriate messages
        // for these ticks. 
        synchronized (this)
        {
            // SIB0105
            // Update the health state of this stream 
            getControlAdapter().getHealthState().updateHealth(HealthStateListener.NACK_RECEIVED_STATE,
                                                              HealthState.AMBER);
            lastNackReceivedTick = endstamp;

            completedPrefix = oststream.getCompletedPrefix();

            // If some of the ticks in the Nack range are before the completedPrefix
            // of our Stream we send a Silence message from the startstamp 
            // to the completedPrefix.
            if (startstamp <= completedPrefix)
            {
                sendLeadingSilence = true;
                lsstart = startstamp;
                lsend = completedPrefix;

                //Some of the ticks in the Nack range are before the completedPrefix
                //of our Stream so start from there
                startstamp = completedPrefix + 1;
            }

            // If there are any tick in the Nack range which are not yet
            // complete process these now
            if (endstamp > completedPrefix)
            {
                oststream.setCursor(startstamp);

                // Get the first TickRange
                TickRange tr = oststream.getNext();

                TickRange tr2 = null;
                while ((tr.startstamp <= endstamp) && (tr != tr2))
                {
                    if ((tr.type == TickRange.Unknown)
                        || (tr.type == TickRange.Requested))
                    {
                        // send Nack to parent and if Unknown change to Requested
                        long ss, es;
                        ss = max(tr.startstamp, startstamp);
                        es = min(endstamp, tr.endstamp);
                        TickRange ntr = new TickRange(TickRange.Requested, ss, es);
                        if (tr.type == TickRange.Unknown)
                        {
                            oststream.writeRange(ntr);
                        }

                        // Some syncronization issues b/w Source Stream and InternalOutputStream. 
                        //  So, We get Source Stream completed prefix and decide whether to mark this tick range(i.e ss to es) as COMPLETED or not

                        long sourceStream_completedPrefix = upControl.sendNackMessageWithReturnValue(null, null, null, ss, es, priority, reliability, streamID);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "sourceStream_completedPrefix : " + sourceStream_completedPrefix + "  es :" + es);
                        if (es <= sourceStream_completedPrefix) {
                            //if end stamp  is less than or equal to Source Stream completed prefix, then it means
                            //Source Stream is not having any information regarding these ticks (already COMPLETED)
                            // then forcefully send silence in these ticks and change InternalOutputStream completedPrefix
                            TickRange ctr = new TickRange(TickRange.Completed, ss, es);
                            writeSilenceForced(ctr); //this should change InternalOutputStream completedPrefix
                            downControl.sendSilenceMessage(
                                                           ss,
                                                           es,
                                                           oststream.getCompletedPrefix(),
                                                           true,
                                                           nm.getPriority().intValue(),
                                                           nm.getReliability(),
                                                           streamID);
                            /* PM62615.dev- Start */
                        } else {

                            if (sourceStream_completedPrefix != -1) {
                                // we are not IME..

                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    SibTr.debug(tc, "NACK Range is Unknown and greater than completed prefix ");

                                if (oststream.getCompletedPrefix() <= _lastTickWhenStreamCreated) {
                                    // If completedPrefix is less than
                                    // _lastMessageTickWhenStreamCreated means gap exits

                                    if (gapMap.get(tr) == null) {
                                        if ((tr.startstamp > _streamStartTick)
                                            && (tr.endstamp <= _lastTickWhenStreamCreated)) {
                                            // igonre of tick range object, i.e ignore tick
                                            // range objects whcih are outside of gap
                                            gapMap.put(tr, new Integer(1));

                                            if (TraceComponent.isAnyTracingEnabled()
                                                && tc.isDebugEnabled())
                                                SibTr.debug(tc,
                                                            "Adding Tickrange object into Gap map : "
                                                                            + Integer.toHexString(tr
                                                                                            .hashCode()));
                                        }
                                    } else {
                                        if (gapMap.get(tr).intValue() > 9) {
                                            if (TraceComponent.isAnyTracingEnabled()
                                                && tc.isDebugEnabled())
                                                SibTr
                                                                .debug(tc,
                                                                       "Reached max nack tolerance(10) for mesages in gap ");
                                            // This is 10th time NACK coming for same tick
                                            // range. Go ahead and send silence
                                            TickRange ctr = new TickRange(
                                                            TickRange.Completed, ss, es);
                                            writeSilenceForced(ctr); // this should change
                                                                     // InternalOutputStream
                                                                     // completedPrefix
                                            downControl.sendSilenceMessage(ss, es,
                                                                           oststream.getCompletedPrefix(), true, nm
                                                                                           .getPriority().intValue(), nm
                                                                                           .getReliability(), streamID);

                                            // remove entry from tickRangeMap
                                            gapMap.remove(tr);
                                            if (TraceComponent.isAnyTracingEnabled()
                                                && tc.isDebugEnabled())
                                                SibTr.debug(tc,
                                                            "Removing TickRange object from Gap map : "
                                                                            + Integer.toHexString(tr
                                                                                            .hashCode()));
                                        } else {
                                            // Add number of NACK occurances into
                                            // tickRangeMap
                                            gapMap.put(tr, new Integer(gapMap.get(tr)
                                                            .intValue() + 1));
                                        }
                                    }
                                } else {
                                    // oststream.getCompletedPrefix() <
                                    // this._lastMessageTickWhenStreamCreated .. means
                                    // gap either filled up or not exists at first place.
                                    // so just empty out tickRangeMap .. as no furhter
                                    // use of this map.
                                    gapMap.clear();
                                }
                            }
                        }
                    }
                    else {
                        //remove entry from tickRangeMap. if tr is not there in map, then it is harmless operation.
                        removeTickRangeObjectFromHashmap(tr);

                        /* PM62615.dev- End */if (tr.type == TickRange.Value)
                        {
                            // Do we have a previous Value message to add
                            // to the list
                            if (sendPending == true)
                            {
                                sendList.add(r);
                            }

                            // Copy the Value tick range into r 
                            r = (TickRange) tr.clone();
                            sendPending = true;

                        }
                        else if (tr.type == TickRange.Uncommitted)
                        {
                            // If there is a previous Value message in the list
                            // we can put any Completed ticks between that and this
                            // Uncommitted tick into it.
                            if (sendPending == true)
                            {
                                // If there are Completed ticks between
                                // the Value and Uncommitted ticks
                                // Add them to the end of the Value message
                                if (tr.valuestamp > (r.valuestamp + 1))
                                {
                                    r.endstamp = tr.valuestamp - 1;
                                }
                                sendList.add(r);
                                sendPending = false;
                            }
                        }
                    }
                    tr2 = tr;
                    tr = oststream.getNext();
                } // end while

                // If we finish on a Completed range then add this to the 
                // last Value in our list
                // Check for null as we may have dropped out first time round loop
                // above without ever initialising tr2
                if ((tr2 != null) && (tr2.type == TickRange.Completed))
                {
                    if (sendPending == true)
                    {
                        r.endstamp = tr2.endstamp;
                    }
                    else
                    {
                        // Need to send this Completed range in a Silence
                        // message as there is no Value to add it to
                        // This may be beacuse the whole Nack range is Completed or
                        // because the previous range was Uncommitted
                        sendTrailingSilence = true;
                        tsstart = tr2.startstamp;
                        tsend = tr2.endstamp;

                    }
                }
                if (sendPending == true)
                {
                    sendList.add(r);
                }
            }
        } // end sync

        if (sendLeadingSilence == true)
        {
            try
            {
                downControl.sendSilenceMessage(
                                               lsstart,
                                               lsend,
                                               completedPrefix,
                                               true, // requestedOnly
                                               nm.getPriority().intValue(),
                                               nm.getReliability(),
                                               streamID);
            } catch (SIResourceException e)
            {
                // FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.gd.InternalOutputStream.processNack",
                                            "1:1096:1.83.1.1",
                                            this);

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "processNack", e);

                throw e;
            }
        }

        // send list 
        if (sendList.size() != 0)
        {
            // This will return a list of expired Messages 
            // If there are any we have to replace them with Silence in the stream
            List expiredMsgs = null;
            try
            {
                expiredMsgs = downControl.sendValueMessages(sendList,
                                                            getCompletedPrefix(),
                                                            true, // requestedOnly
                                                            priority,
                                                            reliability,
                                                            streamID);
            } catch (SIResourceException e)
            {
                // FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.gd.InternalOutputStream.processNack",
                                            "1:1129:1.83.1.1",
                                            this);

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "processNack", e);

                throw e;
            }

            // If we were told to remove some messages from the stream do it now
            if (expiredMsgs != null)
            {
                TickRange vtr = null;
                for (int i = 0; i < expiredMsgs.size(); i++)
                {
                    vtr = (TickRange) expiredMsgs.get(i);
                    writeSilenceForced(vtr);
                }
            }

        }

        if (sendTrailingSilence == true)
        {
            try
            {
                downControl.sendSilenceMessage(
                                               tsstart,
                                               tsend,
                                               completedPrefix,
                                               true, // requestedOnly
                                               nm.getPriority().intValue(),
                                               nm.getReliability(),
                                               streamID);
            } catch (SIResourceException e)
            {
                // FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.gd.InternalOutputStream.processNack",
                                            "1:1172:1.83.1.1",
                                            this);

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "processNack", e);

                throw e;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processNack");

        return null;
    }

    /* PM62615.dev- Start */
    public void removeTickRangeObjectFromHashmap(TickRange tr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "removeTickRangeObjectFromHashmap", new Object[] { tr });

        if (gapMap.isEmpty())
            return;

        synchronized (this) {

            if (oststream.getCompletedPrefix() >= _lastTickWhenStreamCreated) {
                //if gap is filled then just clear the hashmap
                gapMap.clear();
                return;
            }

            if (gapMap.remove(tr) != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Removing Tr object from Hashmap : "
                                    + Integer.toHexString(tr.hashCode()));
            } else {
                // tr is not in HashMap tickRangeMap
                // check in case this tr is the result of split of larger TR
                Object[] trArray = gapMap.entrySet().toArray();

                for (Object trFromHashmap : trArray) {
                    TickRange TrObjFromHashMap = ((Map.Entry<TickRange, Integer>) trFromHashmap)
                                    .getKey();

                    if (tr.startstamp == TrObjFromHashMap.startstamp) {
                        // The single TickRange object (either UNCOMMIT or VALUE) falls
                        // in exactly start stamp of TrObjFromHashMap
                        // go ahead and remove trFromHashmap from hashmap
                        gapMap.remove(TrObjFromHashMap);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "Removing Tr object from Hashmap : "
                                            + Integer.toHexString(tr.hashCode()));
                        break;
                    } else {
                        if ((TrObjFromHashMap.endstamp != Long.MAX_VALUE)
                            && ((tr.startstamp > TrObjFromHashMap.startstamp) && (tr.startstamp < TrObjFromHashMap.endstamp))) {
                            // The single TickRange object (either UNCOMMIT or VALUE)
                            // falls in tickRangeMap TickRange object
                            // go ahead and remove trFromHashmap from hashmap
                            gapMap.remove(TrObjFromHashMap);
                            if (TraceComponent.isAnyTracingEnabled()
                                && tc.isDebugEnabled())
                                SibTr.debug(tc, "Removing Tr object from Hashmap : "
                                                + Integer.toHexString(tr.hashCode()));
                            break;
                        }
                    }
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "removeTickRangeObjectFromHashmap");
        }
    }

    /* PM62615.dev- End */

    public void releaseMemory()
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "releaseMemory");

        synchronized (this)
        {

            // v is unused
            //ArrayList v = new ArrayList();

            // Create a new stream to replace old one
            // as set latest ls prefix
            oststream = new StateStream();
            oststream.init();
            oststream.setCompletedPrefix(oack);

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "releaseMemory");
    }

    protected static long max(long a, long b)
    {
        return a > b ? a : b;
    }

    protected static long min(long a, long b)
    {
        return a < b ? a : b;
    }

    /**
     * This method uses a Value TickRange to write Silence into the stream,
     * because a message has expired before it was sent and so needs to be removed
     * from the stream
     * It forces the stream to be updated to Silence without checking the
     * existing state
     * It then updates the upstream control with this new completed prefix
     * 
     * @param m The value tickRange
     * 
     * 
     */
    public void writeSilenceForced(TickRange vtr)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "writeSilenceForced", new Object[] { vtr });

        long start = vtr.startstamp;
        long end = vtr.endstamp;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            SibTr.debug(tc, "writeSilenceForced from: " + start + " to " + end + " on Stream " + streamID);
        }

        TickRange tr =
                        new TickRange(
                                        TickRange.Completed,
                                        start,
                                        end);

        synchronized (this)
        {
            tr = oststream.writeCompletedRangeForced(tr);
            try
            {
                if (tr.startstamp == 0)
                {
                    //we are completed up to this point so we can
                    //simulate receiving an ack. This will enable the 
                    //src stream to remove the msg from the store if it is no longer
                    //needed
                    writeAckPrefix(tr.endstamp);
                }
            } catch (SIResourceException e)
            {
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.gd.InternalOutputStream.writeSilenceForced",
                                            "1:1273:1.83.1.1",
                                            this);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "writeSilenceForced");

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.gd.Stream#writeSilenceForced(long)
     */
    @Override
    public void writeSilenceForced(long tick)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "writeSilenceForced", Long.valueOf(tick));

        TickRange tr = null;
        synchronized (this)
        {
            oststream.setCursor(tick);

            // Get the TickRange containing this tick 
            tr = oststream.getNext();
            if (tr != null)
            {
                writeSilenceForced(tr);
            }

        }//end sync

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "writeSilenceForced");
    }

    /**
     * Get an unmodifiable list of all of the message items in the VALUE
     * state on this stream and, optionally, in the Uncommitted state
     */
    public synchronized List<TickRange> getAllMessageItemsOnStream(boolean includeUncommitted)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getAllMessageItemsOnStream", Boolean.valueOf(includeUncommitted));

        List<TickRange> msgs = new LinkedList<TickRange>();
        oststream.setCursor(0);
        // Get the first TickRange
        TickRange tr = oststream.getNext();
        while (tr.endstamp < RangeList.INFINITY)
        {
            if (tr.type == TickRange.Value)
            {
                //get this msg from the downstream control
                msgs.add((TickRange) tr.clone());
            }
            else if (tr.type == TickRange.Uncommitted && includeUncommitted)
            {
                //get this msg directly
                if (tr.value != null)
                    msgs.add((TickRange) tr.clone());
            }
            tr = oststream.getNext();
        } // end while

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getAllMessageItemsOnStream", msgs);
        return Collections.unmodifiableList(msgs);
    }

    public synchronized long countAllMessagesOnStream()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "countAllMessagesOnStream");

        long count = 0;
        oststream.setCursor(0);
        // Get the first TickRange
        TickRange tr = oststream.getNext();
        while (tr.endstamp < RangeList.INFINITY)
        {
            if (tr.type == TickRange.Value)
            {
                count++;
            }
            tr = oststream.getNext();
        } // end while

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "countAllMessagesOnStream", Long.valueOf(count));
        return count;
    }

    public SourceStreamState getStreamState()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getStreamState");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getStreamState", internalOutputState);
        return internalOutputState;
    }

    public long getLastMsgSentTime()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getLastMsgSentTime");
            SibTr.exit(tc, "getLastMsgSentTime", Long.valueOf(timeLastMsgSent));
        }
        return timeLastMsgSent;
    }

    public synchronized void setLatestAckExpected(long ackExpStamp) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "setLatestAckExpected", Long.valueOf(ackExpStamp));
            SibTr.entry(tc, "setLatestAckExpected");
        }
        this.lastAckExpTick = ackExpStamp;
    }
}
