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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ejs.util.am.Alarm;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.control.ControlSilence;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.BatchListener;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.HealthStateListener;
import com.ibm.ws.sib.processor.impl.interfaces.MessageDeliverer;
import com.ibm.ws.sib.processor.impl.interfaces.UpstreamControl;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.runtime.HealthState;
import com.ibm.ws.sib.processor.runtime.impl.TargetStreamControl;
import com.ibm.ws.sib.processor.utils.am.MPAlarmManager;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;

/**
 * An output stream that handles delivery of messages to all local consumers
 * This stream exists if and only if this ME hosts local-consumers.
 * 
 * Synchronization:
 * - Most read/writes to data-structures in this class are synchronized on 'this'.
 * Sending messages upstream is done outside 'synchronized (this)' since the
 * response to a nack sent upstream can occur on the same thread if the
 * producer is local.
 * If the nacks were sent within a synchronized block, this can cause a deadlock
 * due to nested synchronization attempts with different orders across multiple
 * OutputStream objects.
 * - Messages from local producers will go direct to local consumers where possible
 * and will therefore not be seen by the TargetStream
 * 
 */
public class GuaranteedTargetStream extends ControllableStream implements TargetStream, BatchListener
{

    private MPAlarmManager am;
    private static TraceComponent tc =
                    SibTr.register(
                                   GuaranteedTargetStream.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    private int priority;
    private Reliability reliability;

    private MessageDeliverer deliverer;
    private UpstreamControl upControl;

    private final StateStream oststream;

    // First Unknown or Requested tick in the stream
    // doubtHorizon is always updated to completedPrefix +1
    // when the horizon advances
    // NOTE: this value should be synchronized on the oststream object.
    // see defect 256036
    private long doubtHorizon;

    // Every tick > unknownHorizon is in Unknown state.
    // It is used to detect whether new gaps are created
    // when a Value or Silence message is written to the stream
    private long unknownHorizon;

    // completedPrefix of batch in progress 
    // this will become the completedPrefix of the stream when
    // the batch commits
    private long nextCompletedPrefix = 0;

    // this is used to mark the stream as blocked.
    // see defect 244425 
    private boolean isStreamBlocked = false;
    private JsDestinationAddress streamBlockingAddress = null;
    private boolean unexpectedBlock = false;

    // at any point the stream will have some tick ranges in the VALUE
    // state. This instance variable will be set to equal the highest
    // tick value of all of those messages - see defect 244425
    private long valueHorizon = 0;

    // This hashtable tracks any pending alarms which may be outstanding
    // at the time of a flush.  We need to know about these so that we
    // can stop the alarms if a flush occurs.  The key is the target
    // stream instance, and the value is an instance of Set.
    protected static final Hashtable pendingAlarms = new Hashtable();

    // The current state of this stream.
    // TODO in the future this may be extended to export other stream states
    private final GuaranteedTargetStreamState streamState =
                    GuaranteedTargetStreamState.ACTIVE;

    private int messagesInBatch = 0;
    private long numberOfMessagesReceived = 0;
    private long timeLastMsgReceived = 0;

    /**
     * The ID of the stream this data structure is associated with.
     */
    protected StreamSet streamSet;

    private SIBUuid8 remoteEngineUUID;

    // The following are used for monitoring the percentage of repeated messages that arrive
    // (510343)
    private int valueCounter = 0;
    private int repeatedValueCounter = 0;
    private long lastRepeatedValueWarningTime = 0;
    // We need a reference to the MessageProcessor object to access the custom properties
    // (510343)
    private MessageProcessor mp;

    private TargetStreamControl targetStreamControl;
    private long lastNackTick = -1;

    // The linkBlockingTick is used to track the tick associated with the message that blocked
    // a link. A subsequent SILENCE message from the source allows the link to be marked as 
    // unblocked.
    private long linkBlockingTick = -1;
    private int blockingCount = 0;

    //NLS for component
    private static final TraceNLS nls =
                    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

    // PM71430.DEV : This variable stores last tick for which an ACK was sent used in determining 
    // when ack has to be sent or not if there are a series of silences due to absence of local consumers.  
    private long _lastAckedTick = 0;

    // PM71430.DEV : An ACK is not sent for every silenced message. Only after a gap of 50 silenced messages.  
    private final long ACK_GAP_FOR_SILENCE_TICKS = 50;

    /**
     * An enumeration for the possible states for this type of target stream
     * 
     * @author tpm100
     */
    public static class GuaranteedTargetStreamState extends TargetStream.TargetStreamState
    {
        public static final GuaranteedTargetStreamState ACTIVE =
                        new GuaranteedTargetStreamState("Active", 1);

        private GuaranteedTargetStreamState(String _name, int _id)
        {
            super(_name, _id);
        }
    }

    public GuaranteedTargetStream(
                                  MessageDeliverer deliverer,
                                  UpstreamControl upControl,
                                  MPAlarmManager am,
                                  StreamSet streamSet,
                                  int priority,
                                  Reliability reliability,
                                  List scratch,
                                  MessageProcessor mp)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "GuaranteedTargetStream",
                        new Object[] { deliverer,
                                      upControl,
                                      am,
                                      streamSet,
                                      Integer.valueOf(priority),
                                      reliability,
                                      scratch,
                                      mp });

        oststream = new StateStream();
        // synchronization ensures that other synchronized methods will see values
        // initialized in constructor

        synchronized (oststream)
        {
            this.remoteEngineUUID = streamSet.getRemoteMEUuid();
            this.am = am;
            this.priority = priority;
            this.reliability = reliability;

            // setup the default values first
            unknownHorizon = 0;

            doubtHorizon = 1; //we do not need to sync here as we are construcing

            oststream.init();

            this.deliverer = deliverer;
            this.upControl = upControl;

            this.streamSet = streamSet;

            this.mp = mp;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "GuaranteedTargetStream", this);
    }

    @Override
    public long reconstituteCompletedPrefix(long newPrefix)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "reconstituteCompletedPrefix", Long.valueOf(newPrefix));

        long prefix;
        // if the completedPrefix is updated, return
        // the new value     
        if (oststream.setCompletedPrefix(newPrefix) == true)
        {
            prefix = newPrefix;
        }
        else
        {
            // return the old value
            prefix = oststream.getCompletedPrefix();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reconstituteCompletedPrefix", Long.valueOf(prefix));

        return prefix;
    }

    private void persistCompletedPrefix(TransactionCommon tran) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "persistCompletedPrefix", tran);

        // Note that we take the stream lock to get the nextcompletedPrefix           
        streamSet.setPersistentData(
                                    priority,
                                    reliability,
                                    getNextCompletedPrefix());

        streamSet.requestUpdate(reliability, tran);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "persistCompletedPrefix");
    }

    // This method is only called by createStream in
    // TargetStreamManager.
    @Override
    public void setCompletedPrefix(long newPrefix)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setCompletedPrefix", Long.valueOf(newPrefix));

        // If the completedPrefix is updated then update the 
        // other control variables.
        //We synchronize to ensure the saftey
        synchronized (oststream) //defect 256036
        {
            if (oststream.setCompletedPrefix(newPrefix) == true)
            {
                doubtHorizon = newPrefix + 1;
                unknownHorizon = newPrefix;

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "doubtHorizon:" + doubtHorizon + " unknownHorizon:" + unknownHorizon);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setCompletedPrefix");
    }

    private void updateCompletedPrefix()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "updateCompletedPrefix");

        synchronized (oststream)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "nextCompletedPrefix: " + nextCompletedPrefix);

            oststream.setCompletedPrefix(nextCompletedPrefix);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "updateCompletedPrefix");
    }

    public void resetDoubtHorizon()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "resetDoubtHorizon");

        synchronized (oststream) //defect 256036
        {
            resetDoubtHorizon(oststream.getCompletedPrefix() + 1, null, null);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "resetDoubtHorizon");
    }

    public void resetDoubtHorizon(long tick)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "resetDoubtHorizon", Long.valueOf(tick));

        resetDoubtHorizon(tick, null, null);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "resetDoubtHorizon");
    }

    public void resetDoubtHorizon(long tick, Exception e, JsDestinationAddress address)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "resetDoubtHorizon", new Object[] { Long.valueOf(tick),
                                                                     e,
                                                                     address });

        synchronized (oststream) //defect 256036
        {
            doubtHorizon = tick;

            // If an exception is passed in then the caller obviously hit a problem trying to
            // deliver a message, that message will have effectively blocked the stream, so
            // mark it as such so that we make it visible to the user.
            if (e != null)
            {
                setStreamIsBlocked(true, 0, e, address);
                linkBlockingTick = tick;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "resetDoubtHorizon");
    }

    public long getDoubtHorizon()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDoubtHorizon");

        long retDoubtHorizon = 0;
        synchronized (oststream) //defect 256036
        {
            retDoubtHorizon = this.doubtHorizon;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getDoubtHorizon", Long.valueOf(retDoubtHorizon));

        return retDoubtHorizon;
    }

    /**
     * 
     * @exception GDException thrown from either initialiseStream or checkForWindowAdvanceGaps
     */
    @Override
    public void writeValue(MessageItem msgItem)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "writeValue", new Object[] { msgItem });

        TickRange tr = null;
        List msgList = null;
        JsMessage m = msgItem.getMessage();

        long valueTick = m.getGuaranteedValueValueTick();
        timeLastMsgReceived = System.currentTimeMillis();

        // Take lock on target stream and hold it until messages have been
        // added to batch by deliverOrderedMessages 
        synchronized (this)
        {
            synchronized (oststream)
            {
                //see if this stream can accept new messages BEFORE
                //we write messages into the stream in the 'value' state
                boolean canProcessMessage = false;
                try
                {
                    canProcessMessage = streamCanAcceptNewMessage(msgItem, valueTick);
                } catch (SIException e)
                {
                    //FFDC
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.gd.GuaranteedTargetStream.writeValue",
                                                "1:452:1.110",
                                                this);

                    SibTr.exception(tc, e);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "writeValue", e);
                    return;
                }
                if (canProcessMessage)
                {
                    boolean repeatedValue = false;
                    long completedPrefix = oststream.getCompletedPrefix();

                    // Check if this is a repeated Value message which we have
                    // already Acked and process accordingly
                    if (valueTick > completedPrefix)
                    {
                        // Message has not already been acked
                        tr = TickRange.newValueTick(valueTick, msgItem, -1);
                        tr.startstamp = m.getGuaranteedValueStartTick();
                        tr.endstamp = m.getGuaranteedValueEndTick();

                        // write this into the state stream
                        boolean streamChanged = false;
                        streamChanged = oststream.writeCombinedRange(tr);

                        // Check that we didn't already have this Value in the stream
                        if (streamChanged)
                        {
                            if (tr.valuestamp > valueHorizon)
                            {
                                valueHorizon = tr.valuestamp;
                            }

                            boolean gapcreated = false;

                            // determine the range of tick in the Completed state
                            // either side of this Value tick
                            // Note only the startstamp and endstamp will have been modified
                            tr = oststream.findCompletedRange(tr);

                            if (tr.endstamp > unknownHorizon)
                            {
                                // check if gap created
                                if (tr.startstamp > (unknownHorizon + 1))
                                {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                        SibTr.debug(
                                                    tc,
                                                    "created gap ["
                                                                    + (unknownHorizon + 1)
                                                                    + ", "
                                                                    + (tr.startstamp - 1)
                                                                    + "]");

                                    gapcreated = true;

                                    handleNewGap(unknownHorizon + 1, tr.startstamp - 1);
                                }
                                unknownHorizon = tr.endstamp;
                            }

                            if (!gapcreated)
                            {
                                // Only if gapcreated == false, can this message have the possibility
                                // of advancing the doubtHorizon and being delivered.

                                if (tr.startstamp <= doubtHorizon)
                                {
                                    // Deliver this message
                                    // We use a list to deliver messages
                                    if (msgList == null)
                                        msgList = new ArrayList();
                                    msgList.add(msgItem);

                                    // Set doubtHorizon to tr.endstamp + 1
                                    doubtHorizon = tr.endstamp + 1;
                                    msgList = advanceDoubtHorizon(msgList);
                                }
                                else
                                {
                                    // This case is only used if the previous batch of 
                                    // messages we delivered was rolled back and we need
                                    // to deliver them again
                                    msgList = advanceDoubtHorizon(msgList);
                                }
                            } // end if (!gapCreated)

                            // Reset the health state
                            if (lastNackTick >= 0)
                            {
                                if (tr.startstamp <= lastNackTick && tr.endstamp >= lastNackTick)
                                {
                                    getControlAdapter().getHealthState().updateHealth(HealthStateListener.MSG_LOST_ERROR_STATE,
                                                                                      HealthState.GREEN);
                                    lastNackTick = -1;
                                }
                            }
                        } // end if ( streamChanged )
                        else
                        {
                            // Stream was not changed so this was a repeated Value which 
                            // we have not yet Acked
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(
                                            tc,
                                            "Repeated V tick value - not yet acked "
                                                            + valueTick
                                                            + " : "
                                                            + completedPrefix);

                            repeatedValue = true;
                        }
                    }
                    else
                    { // valueTick <= completedPrefix

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(
                                        tc,
                                        "Repeated V tick value - already acked "
                                                        + valueTick
                                                        + " : "
                                                        + completedPrefix);

                        repeatedValue = true;
                    }

                    // It's acceptable to recieve the odd repeated value, for example a message
                    // is sent from the source slightly out of order with subsequent messages
                    // due to it's transaction taking longer to complete than others). So we (the
                    // target) see a gap appear, nack the gap (after a few milliseconds) then the
                    // message arrives anyway so we add it to the stream. Then the source receives
                    // the nack and re-sends the message. When that arrives at us we just ignore it
                    // becuase we already have it.
                    // You'd expect this to happen occasionally but if it happens too often it
                    // will add unnecessary traffic, reducing performance.
                    // There are a couple of reasons this can occur:
                    //  1) the initial nack is sent too fast to allow for slightly out of order
                    //     streams. This is set by GDConfig.GD_GAP_CURIOSITY_THRESHOLD (to 200 msecs)
                    //  2) The end-to-end transmission time of messages is excessively long, so
                    //     once a gap is introduced (by the sender overloading the socket) the gap
                    //     is nacked multiple times. This is due to the nack repetition timer (NRT)
                    //     popping multiple times before the re-sent message (from the first nack) is
                    //     actually recieved and the NRT cancelled. This end-to-end delay has been seen due
                    //     to excessive buffering of inbound messages in the Comms layer before being
                    //     processed by us. This buffer in Comms can be configured using
                    //     com.ibm.ws.sib.jfapchanel.RL_DISPATCH_MAXQUEUESIZE. The excessive buffering
                    //     is still only a symptom of the socket being overloaded with work, the thing
                    //     sending all the messages needs to back off.
                    // We would expect to see this high repetition after a restart of an ME as a whole
                    // 'sendWindow' of messages will flood into the system, this defaults to 1000 and could
                    // be turned down using sib.processor.sendWindow
                    // (510343)

                    // If we're monitoring the number of repeated values that we see, check
                    // to see if we've seen too many...
                    int repeatedPercent = mp.getCustomProperties().getRepeatedValuePercentage();
                    int repeatedInterval = mp.getCustomProperties().getRepeatedValueInterval();
                    if ((repeatedPercent > 0) &&
                        (repeatedInterval > 0))
                    {
                        // Repeated or not, this counts towards the interval that we count
                        // repeats in.
                        valueCounter++;

                        // Calculate how many repeated messages we allow per REPEATED_VALUE_INTERVAL
                        int repeatedValueThreshold =
                                        (repeatedPercent * repeatedInterval) / 100;

                        // If this was a repeated value (a message tick that we've already seen)
                        // add it to the counter and check to see if we've breached the threshold.
                        if (repeatedValue)
                        {
                            repeatedValueCounter++;

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(tc, "valueCounter:" + valueCounter +
                                                " repeatedValueThreshold:" + repeatedValueThreshold +
                                                " repeatedValueCounter:" + repeatedValueCounter +
                                                " lastRepeatedValueWarningTime:" + lastRepeatedValueWarningTime);

                            if (repeatedValueCounter >= repeatedValueThreshold)
                            {
                                // Issue a warning message (if it's not too soon after the last message issued,
                                // to prevent flooding the log)
                                long currentTime = System.currentTimeMillis();
                                if ((currentTime - lastRepeatedValueWarningTime) > SIMPConstants.REPEATED_VALUE_WARNING_INTERVAL)
                                {
                                    // Issue a message
                                    int percent = (repeatedValueCounter * 100) / valueCounter;
                                    deliverer.reportRepeatedMessages(remoteEngineUUID.toString(), percent);

                                    lastRepeatedValueWarningTime = currentTime;

                                    // Reset the counters
                                    valueCounter = 0;
                                    repeatedValueCounter = 0;
                                }
                            }
                        }

                        // If we've completed a whole interval, reset the counters.
                        if (valueCounter == repeatedInterval)
                        {
                            // Reset the counters as we've entered a new interval
                            valueCounter = 0;
                            repeatedValueCounter = 0;
                        }
                    }
                }
                else
                {
                    //the stream is blocked - cannot accept new messages
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(
                                    tc,
                                    "The stream is blocked: msg discarded for resend.");
                }
            } // end sync ( release the oststream lock )

            // Deliver messages outside of synchronise 
            // We do this because deliverOrderedMessages takes the 
            // BatchHandler lock and the BatchHandler callbacks require
            // the ostsstream lock to update the completedPrefix. If we call
            // the BatchHandler when we hold the oststream lock it could cause
            // a deadlock
            if (msgList != null)
            {
                // Call the Input or Output Handler to deliver the messages
                try
                {
                    messagesInBatch += msgList.size();
                    deliverer.deliverOrderedMessages(
                                                     msgList,
                                                     this,
                                                     priority,
                                                     reliability);
                } catch (SINotPossibleInCurrentConfigurationException e)
                {
                    // No FFDC code needed
                    SibTr.exception(tc, e);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "writeValue", "GDException");

                    // Dont rethrow the exception. The GD protocols will handle the resend of
                    // the message
                } catch (SIException e)
                {
                    // FFDC
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.gd.GuaranteedTargetStream.writeValue",
                                                "1:709:1.110",
                                                this);

                    SibTr.exception(tc, e);
                    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                new Object[] {
                                              "com.ibm.ws.sib.processor.gd.GuaranteedTargetStream",
                                              "1:716:1.110",
                                              e });

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "writeValue", "GDException");

                    throw new SIErrorException(
                                    nls.getFormattedMessage(
                                                            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                            new Object[] {
                                                                          "com.ibm.ws.sib.processor.gd.GuaranteedTargetStream",
                                                                          "1:726:1.110",
                                                                          e },
                                                            null),
                                    e);
                }
            }

        } // end sync - release lock on target stream

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "writeValue");

    }

    /**
     * This method uses a Value message to write Silence into the stream
     * because a message has been filtered out
     * 
     * @param m The value message
     * 
     * @exception Thrown from writeRange, handleNewGap
     *                and checkForWindowAdvanceGaps
     * 
     */
    @Override
    public void writeSilence(MessageItem m)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "writeSilence", new Object[] { m });

        JsMessage jsMsg = m.getMessage();

        // There may be Completed ticks after the Value, if so then
        // write these into the stream too
        long stamp = jsMsg.getGuaranteedValueValueTick();
        long ends = jsMsg.getGuaranteedValueEndTick();
        if (ends < stamp)
            ends = stamp;

        TickRange tr =
                        new TickRange(
                                        TickRange.Completed,
                                        jsMsg.getGuaranteedValueStartTick(),
                                        ends);

        // Update the stream 
        writeSilenceInternal(tr, false);

        /*
         * PM71430.DEV : We have to send the ACK control message for the messages which are to be silenced.
         * Otherwise, if for some reason, source side ME stops sending the ACKEXPECTED control messages,
         * target ME will never send ACK for these messages. This will result into messages getting piled
         * on the source ME and never get removed.
         * 
         * ACK message is not sent for silence message. Instead, it is sent only after a gap of 50 (default)
         * messages from the last ACKED message.
         * 
         * If any consumers come up in between, this method is not
         * called to write silence. In this case, ACK is always sent during batch commit call back.
         * 
         * Please note this piece of code has been written to handle specific scenario when there are no
         * local consumers on the target side for a long time which results in never acknowledging any of
         * the ticks and the local completed prefix keeps moving ahead.
         */

        long gapFromPreviousAck = ends - this._lastAckedTick;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "gap from the previous ack: " + gapFromPreviousAck + " lastAckedTick: " + this._lastAckedTick);

        if (gapFromPreviousAck >= this.ACK_GAP_FOR_SILENCE_TICKS) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "sending the ACK message for the batch of silence message ", new Object[] { m });

            sendAck();

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "writeSilence");

    }

    /**
     * Writes a range of Silence from a ControlSilence message to the stream
     * 
     * @param m The ControlSilence message
     * 
     * @exception Thrown from writeRange, handleNewGap
     *                and checkForWindowAdvanceGaps
     */
    @Override
    public void writeSilence(ControlSilence m)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "writeSilence", new Object[] { m });

        // Construct a TickRange from the Silence message 
        TickRange tr =
                        new TickRange(TickRange.Completed, m.getStartTick(), m.getEndTick());

        writeSilenceInternal(tr, false);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "writeSilence");

    }

    /**
     * Writes a range of Silence from a ControlSilence message to the stream
     * 
     * @param m The ControlSilence message
     * 
     * @exception Thrown from writeRange, handleNewGap
     *                and checkForWindowAdvanceGaps
     */
    @Override
    public void writeSilenceForced(long tick)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "writeSilenceForced", Long.valueOf(tick));

        TickRange tr = null;
        synchronized (oststream) //see defect 289889
        {
            oststream.setCursor(tick);

            // Get the TickRange containing this tick 
            tr = oststream.getNext();
        }

        writeSilenceInternal(tr, true);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "writeSilenceForced");

    }

    /**
     * Writes the range of Silence specified by the TickRange into the stream
     * 
     */
    private void writeSilenceInternal(TickRange tr, boolean forced)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "writeSilenceInternal", new Object[] { tr, Boolean.valueOf(forced) });

        List msgList = null;

        // Take lock on target stream and hold it until messages have been
        // added to batch by deliverOrderedMessages 
        synchronized (this)
        {
            // We are only allowed to remove a message from the stream and replace 
            // it with Silence if it has not yet been added to a Batch to be delivered
            // We know that this can't happen asynchronously once we hold the targetStream lock
            // and the nextCompletedPrefix tells us the last message in the current batch 
            if (!forced || tr.valuestamp > this.nextCompletedPrefix)
            {

                synchronized (oststream)
                {
                    // Get the completedPrefix 
                    long completedPrefix = oststream.getCompletedPrefix();

                    // check if all ticks in Silence msg are already acked as if
                    // so they will be changed to Completed anyway and we don't need
                    // to do it now
                    if (tr.endstamp > completedPrefix)
                    {
                        if (forced == false)
                            oststream.writeRange(tr);
                        else
                            oststream.writeCompletedRangeForced(tr);

                        // Get updated completedPrefix
                        completedPrefix = oststream.getCompletedPrefix();

                    }

                    // The completedPrefix may have advanced when the Silence message
                    // was writen to the stream or both
                    if ((completedPrefix + 1) > doubtHorizon)
                    {
                        // advance the doubt horizon
                        doubtHorizon = completedPrefix + 1;

                        // now call the method which trys to advance the doubtHorizon
                        // from it's current setting by moving over any ticks in Completed 
                        // state and sending Values messages until it reaches a tick in
                        // Unknown or Requested
                        msgList = advanceDoubtHorizon(null);
                    }

                    if ((doubtHorizon - 1) > unknownHorizon)
                        unknownHorizon = doubtHorizon - 1;

                    // the message was writen to the stream
                    // see if this message created a gap
                    if (tr.endstamp > unknownHorizon)
                    {
                        // check if gap created
                        if (tr.startstamp > (unknownHorizon + 1))
                        {
                            handleNewGap(unknownHorizon + 1, tr.startstamp - 1);
                        }
                        unknownHorizon = tr.endstamp;
                    }

                    // Reset the health state
                    if (lastNackTick >= 0)
                    {
                        if (tr.startstamp <= lastNackTick && tr.endstamp >= lastNackTick)
                        {
                            getControlAdapter().getHealthState().updateHealth(HealthStateListener.MSG_LOST_ERROR_STATE,
                                                                              HealthState.GREEN);
                            lastNackTick = -1;
                        }
                    }

                    // If the stream is blocked, see if we have a silence for the blocking tick
                    if (isStreamBlocked())
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "Stream is blocked on tick: " + linkBlockingTick + ", see if we can unblock it");
                        if (tr.endstamp >= linkBlockingTick)
                        {
                            if (tr.startstamp <= linkBlockingTick)
                            {
                                // The stream is no longer blocked
                                setStreamIsBlocked(false, DestinationHandler.OUTPUT_HANDLER_FOUND, null, null);
                            }
                        }
                    } // eof isStreamBlocked()
                } // end sync ( release the oststream lock )

                // Deliver messages outside of synchronise 
                // We do this because deliverOrderedMessages takes the 
                // BatchHandler lock and the BatchHandler callbacks require
                // the stream lock to update the completedPrefix. If we call
                // the BatchHandler when we hold the stream lock it could cause
                // a deadlock
                if (msgList != null)
                {
                    // Call the Input or Output Handler to deliver the messages
                    try
                    {
                        deliverer.deliverOrderedMessages(msgList, this, priority, reliability);
                    } catch (SINotPossibleInCurrentConfigurationException e)
                    {
                        // No FFDC code needed
                        SibTr.exception(tc, e);

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "writeSilenceInternal", "GDException");

                        // Dont rethrow the exception. The GD protocols will handle the resend of
                        // the message
                    } catch (SIException e)
                    {
                        // FFDC
                        FFDCFilter.processException(
                                                    e,
                                                    "com.ibm.ws.sib.processor.gd.GuaranteedTargetStream.writeSilenceInternal",
                                                    "1:960:1.110",
                                                    this);

                        SibTr.exception(tc, e);
                        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                    new Object[] {
                                                  "com.ibm.ws.sib.processor.gd.GuaranteedTargetStream",
                                                  "1:967:1.110",
                                                  e });

                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "writeSilenceInternal", "GDException");

                        throw new SIErrorException(
                                        nls.getFormattedMessage(
                                                                "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                                new Object[] {
                                                                              "com.ibm.ws.sib.processor.gd.GuaranteedTargetStream",
                                                                              "1:977:1.110",
                                                                              e },
                                                                null),
                                        e);
                    }
                }

            } // else can't remove as already in batch 
        } // end sync - release lock on target stream

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "writeSilenceInternal");
    }

    /**
     * This method will walk the oststream from the doubtHorizon to the stamp
     * and send Nacks for any Unknown or Requested ticks it finds.
     * It will also change Unknown ticks to Requested.
     * 
     * @exception GDException Thrown from the writeRange method
     */
    @Override
    public void processAckExpected(long stamp)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "processAckExpected", new Object[] { Long.valueOf(stamp) });

        List nackList = null;
        List msgList = null;
        boolean processNack = true;

        // Take lock on target stream and hold it until messages have been
        // added to batch by deliverOrderedMessages 
        synchronized (this)
        {
            synchronized (oststream) //see defect 289889
            {
                long completedPrefix = oststream.getCompletedPrefix();
                long blockingTick = -1;
                // Send Ack message for ticks up to completedPrefix
                sendAck();
                if (isStreamBlocked())
                {
                    // If the link was implicitly blocked due to an 'unexpected' problem when we tried
                    // to deliver a message (the most obvious reason is due to not being authorized)
                    // we can't keep the stream blocked as we need to allow messages in again to re-try
                    // the deliverery of the message to be able to tell if the problem still exists.
                    // However, rather than nack all messages currently blocked on the stream (which is
                    // what the source end will be ack-expecting) we only nack the blocking message,
                    // if that succeeds the stream will be left un-blocked, but if it fails the stream
                    // will be re-locked. If the stream is left un-blocked, the next time an ack-expected
                    // comes in we'll nack everything.
                    if (isStreamBlockedUnexpectedly())
                    {
                        stamp = linkBlockingTick;
                        blockingTick = linkBlockingTick;

                        // We need to unblock the stream, otherwise the failing message won't be re-delivered
                        // when it arrives (it'll get bounced in writeValue), and that's the only way to tell
                        // is the failure is still valid (as it's not detected by streamCanAcceptNewMessage).
                        setStreamIsBlocked(false, DestinationHandler.OUTPUT_HANDLER_FOUND, null, null);
                    }
                    else
                    {
                        // The stream has been marked as blocked. We should only send 
                        // NACKs if we are able to accept the resent message once it arrives.
                        // Because ACKs are for the stream as a whole, if this is a link
                        // then we need to check whether the link's exception destination is
                        // able to accept messages. If no exception destination is defined on the
                        // link then we check the link blocking destination.
                        int blockingReason = deliverer.checkStillBlocked();
                        if (blockingReason == DestinationHandler.OUTPUT_HANDLER_FOUND)
                        {
                            //the destination is no longer blocked
                            setStreamIsBlocked(false, DestinationHandler.OUTPUT_HANDLER_FOUND, null, null);
                        }
                        else
                        {
                            // Its possible that the reason for the blockage has changed. This code allows
                            // the link health state to be altered to reflect that change (but keeps the
                            // same blocking tick as that hasn't changed)
                            setStreamIsBlocked(true, blockingReason, null, streamBlockingAddress);

                            // It's possible that the blockage has been cleared on the sending side without
                            // us knowing about it (e.g. the offending message was deleted but the silence never
                            // made it to us). So, to err on the side of caution, we occasionally forget about
                            // the blockage and allow the blocking tick to be nack'd (just that one, not all
                            // of them, we don't want them all to be re-sent until we know the problem doesn't
                            // still exist). This will either cause the same problematic message to be re-sent
                            // and the link will stay blocked or a silence will arrive indicating the blockage
                            // has been cleared, allowing the subsequent ackExcepted to result in a full Nack
                            if (blockingCount++ > 3)
                            {
                                stamp = linkBlockingTick;
                                blockingCount = 0;
                                blockingTick = linkBlockingTick;

                                // Note: In this case (unlike the unexpected block case above) we can leave the
                                // stream blocked as the original problem was detected by streamCanAcceptNewMessage,
                                // if the mesasge is re-sent writeValue will have a chance of processing it if
                                // the call to streamCanAcceptNewMessage thinks the problem has been resolved.

                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    SibTr.debug(tc, "Nacking the first tick (" + stamp + ") of a blocked link");
                            }
                            // The stream is blocked (for now) so we don't want the blocking message (and all
                            // the ones built up behind it) to be resent because we nack them!
                            else
                                processNack = false;
                        }
                    }
                }
                //We're allowed to send Nacks
                if (processNack)
                {
                    if (stamp <= completedPrefix)
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "processAckExpected");
                        return;
                    }

                    // walk the oststream from the completedPrefix to endstamp and Nack
                    // everything that is Unknown or Requested
                    // Change everything which is Unknown to Requested.
                    nackList = new ArrayList();
                    long startstamp = completedPrefix + 1;
                    long endstamp = stamp;
                    oststream.setCursor(startstamp);
                    TickRange tr2 = null;
                    TickRange tr1 = oststream.getNext();
                    do
                    {
                        if ((tr1.type == TickRange.Unknown)
                            || (tr1.type == TickRange.Requested))
                        {
                            long ss, es;

                            // Handle case where start or endstamps fall within a range
                            ss = tr1.startstamp > startstamp ? tr1.startstamp : startstamp;
                            es = endstamp > tr1.endstamp ? tr1.endstamp : endstamp;

                            TickRange tr = new TickRange(TickRange.Requested, ss, es);

                            // Only need to update stream if this is currently Unknown
                            if (tr1.type == TickRange.Unknown)
                            {
                                oststream.writeRange(tr);
                            }

                            // SIB0105
                            // Set the head msg health state to AMBER if neccessary
                            synchronized (pendingAlarms)
                            {
                                // If we have no current gaps then we must be missing msg at the head of the stream
                                if (!getAlarms(this).hasNext())
                                {
                                    getControlAdapter().getHealthState().updateHealth(HealthStateListener.MSG_LOST_ERROR_STATE,
                                                                                      HealthState.AMBER);
                                    lastNackTick = endstamp;
                                }
                            }

                            nackList.add(tr);
                        }
                        // If the stream is blocked but we want to force the message (or a silence if it's
                        // been deleted on the source side) to be sent again then we need to add the blocking
                        // range to the nack list, even if it's in value state (which is possible if we tried
                        // to deliver it before but failed (so it's already added to the stream, but not
                        // delivered yet).
                        else if ((tr1.type == TickRange.Value) && (tr1.endstamp == blockingTick))
                        {
                            nackList.add(tr1);
                        }
                        tr2 = tr1;
                        tr1 = oststream.getNext();
                    } while ((tr1.startstamp <= endstamp) && (tr1 != tr2));
                }//end processNack
                else
                {
                    //we did not process the AckExpected message for any nacks
                    //as we did not have room for the replies
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "did not process AckExpected message for nacks.");

                }

                // PK57736 We should should this stimulus from the sending side
                // to advance the doubt horizon, as it is possible they are not
                // sending messages any longer (which is the other stimulus).
                msgList = advanceDoubtHorizon(null);

            } // end synchronized

            // PK57736 Deliver messages outside of synchronise 
            // We do this because deliverOrderedMessages takes the 
            // BatchHandler lock and the BatchHandler callbacks require
            // the stream lock to update the completedPrefix. If we call
            // the BatchHandler when we hold the stream lock it could cause
            // a deadlock
            if (msgList != null)
            {
                // Call the Input or Output Handler to deliver the messages
                try
                {
                    deliverer.deliverOrderedMessages(msgList, this, priority, reliability);
                } catch (SINotPossibleInCurrentConfigurationException e)
                {
                    // No FFDC code needed
                    SibTr.exception(tc, e);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "processAckExpected", "GDException");

                    // Dont rethrow the exception. The GD protocols will handle the resend of
                    // the message
                } catch (SIException e)
                {
                    // FFDC
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.gd.GuaranteedTargetStream.processAckExpected",
                                                "1:1201:1.110",
                                                this);

                    SibTr.exception(tc, e);
                    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                new Object[] {
                                              "com.ibm.ws.sib.processor.gd.GuaranteedTargetStream",
                                              "1:1208:1.110",
                                              e });

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "processAckExpected", "GDException");

                    throw new SIErrorException(
                                    nls.getFormattedMessage(
                                                            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                            new Object[] {
                                                                          "com.ibm.ws.sib.processor.gd.GuaranteedTargetStream",
                                                                          "1:1218:1.110",
                                                                          e },
                                                            null),
                                    e);
                }
            }
        } // end sync - release lock on target stream

        // send nacks
        for (int j = 0; processNack && j < nackList.size(); j++)
        {
            TickRange temptr = (TickRange) nackList.get(j);

            try
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "processAckExpected sending Nack from: " + temptr.startstamp + " to " + temptr.endstamp);

                upControl.sendNackMessage(
                                          streamSet.getRemoteMEUuid(),
                                          streamSet.getDestUuid(),
                                          streamSet.getBusUuid(),
                                          temptr.startstamp,
                                          temptr.endstamp,
                                          priority,
                                          reliability,
                                          streamSet.getStreamID());
            } catch (SIResourceException e)
            {
                // FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.gd.GuaranteedTargetStream.processAckExpected",
                                            "1:1252:1.110",
                                            this);

                SibTr.exception(tc, e);
                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.gd.GuaranteedTargetStream",
                                          "1:1259:1.110",
                                          e });

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "processAckExpected", e);

                throw new SIErrorException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.gd.GuaranteedTargetStream",
                                                                      "1:1270:1.110",
                                                                      e },
                                                        null),
                                e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processAckExpected");
    }

    // The following methods, handleNewGap, advanceDoubtHorizon are
    // important for detecting and handling gaps, and advancing the 
    // doubt horizon after a gap is filled and delivering messages
    // which were in the gap

    /**
     * all methods calling this are already synchronized
     * 
     * @exception GDException thrown from the writeRange method
     */
    private void handleNewGap(long startstamp, long endstamp)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "handleNewGap",
                        new Object[] { Long.valueOf(startstamp), Long.valueOf(endstamp) });

        TickRange tr = new TickRange(TickRange.Requested, startstamp, endstamp);
        oststream.writeRange(tr);

        // SIB0115 
        // Update Health State due to detected gap
        getControlAdapter().getHealthState().updateHealth(HealthStateListener.GAP_DETECTED_STATE,
                                                          HealthState.AMBER);

        NRTExpiryHandle nexphandle = new NRTExpiryHandle(tr, this);
        nexphandle.timer =
                        am.create(mp.getCustomProperties().getGapCuriosityThreshold(), nexphandle);

        addAlarm(this, nexphandle);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "handleNewGap");
    }

    /**
     * Returns List of messages whic can be delivered.
     * NOTE: This should be called when holding the oststream lock.
     */
    private List advanceDoubtHorizon(List existingMsgList)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "advanceDoubtHorizon", existingMsgList);

        // set cursor to doubtHorizon, try to advance it, 
        // and deliver messages to clients as
        // we go along. 
        oststream.setCursor(doubtHorizon);

        TickRange tr = null;
        TickRange tr2 = null; //PK58188
        List msgList = null;

        // Add messages to list if on already exists
        if (existingMsgList != null)
            msgList = existingMsgList;

        while (true)
        {
            tr2 = tr; //PK58188
            tr = oststream.getNext();
            if (tr2 == tr) //PK58188
                break; //PK58188

            if (tr.type == TickRange.Completed)
                continue;
            else if (tr.type == TickRange.Value)
            {
                try
                {
                    // Get the message item from the stream
                    MessageItem m = (MessageItem) tr.value;

                    // Add to list to deliver
                    // Create the array if required
                    if (msgList == null)
                        msgList = new ArrayList();
                    msgList.add(m);

                } catch (Exception e)
                {
                    // FFDC
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.gd.GuaranteedTargetStream.advanceDoubtHorizon",
                                                "1:1368:1.110",
                                                this);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "advanceDoubtHorizon", "GDException");
                    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                new Object[] {
                                              "com.ibm.ws.sib.processor.gd.GuaranteedTargetStream",
                                              "1:1375:1.110",
                                              e });

                    throw new SIErrorException(
                                    nls.getFormattedMessage(
                                                            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                            new Object[] {
                                                                          "com.ibm.ws.sib.processor.gd.GuaranteedTargetStream",
                                                                          "1:1383:1.110",
                                                                          e },
                                                            null),
                                    e);
                }
            }
            else // type is not Completed or Value so give up
            {
                break;
            }
        }

        // Check whether the doubtHorizon got advanced
        if (tr.startstamp > doubtHorizon)
        {
            doubtHorizon = tr.startstamp;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "advanceDoubtHorizon", new Object[] { doubtHorizon, msgList });

        return msgList;
    }

    protected void releaseMemory()
    {
        // do nothing to the knowledge and curious streams since memory usage
        //  is already restricted by receive window size.
    }

    /**
     * Objects in this class are responsible for repeating nacks. There can be 0 or more
     * NRTExpiryHandles that are at any time registered with the timer pool.
     * Each NRT will repeat nacks for non-overlapping ticks.
     */
    class NRTExpiryHandle implements AlarmListener
    {

        RangeObject nackRange;

        // after how many NRT expiries should nack be resent
        int repeatCount;

        // the current count of NRT expiries. when nrtIteration == repeatCount,
        // send nack, and set nrtIteration=0
        int nrtIteration;

        // reference to alarm in case in needs to be cancelled later (e.g. due to a flush)
        Alarm timer;

        // The stream which created this alarm.  Needed to manage the alarm set.
        Object parent;

        // The time we last reported that we had a gap for this 'original' range
        // (the range may become partially filled over time, so we consider any
        // gaps in this range as the thing that is being nacked) (510343)
        long lastNackReportedTime = 0;
        long lastNackReported = -1;

        NRTExpiryHandle(RangeObject nackRange, Object p)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "NRTExpiryHandle", new Object[] { nackRange, p });

            this.nackRange = nackRange;

            repeatCount = 1;
            nrtIteration = 0;

            parent = p;

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "NRTExpiryHandle", this);
        }

        /**
         * Method alarm
         * 
         * @param alarmContext
         * @see com.ibm.ejs.util.am.AlarmListener#alarm(Object)
         * 
         *      <p>This is the method that is driven by Alarm Manager when
         *      the time interval expires. </p>
         */
        @Override
        public void alarm(Object alarmContext)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "alarm", new Object[] { alarmContext, parent });

            // As we didnt pass an object, alarmContext will be null

            // Before we do anything else, remove ourself from the set of
            // pending alarms.  We'll add ourselves later if necessary.
            removeAlarm(parent, this);

            boolean foundRequestedTicks = false;
            long numTicksNacked = 0;
            nrtIteration++;

            if (nrtIteration == repeatCount)
            {
                // have done enough iterations to resend the nack if necessary
                numTicksNacked = checkCuriosityAndNack();

                if (numTicksNacked > 0)
                    foundRequestedTicks = true;

                nrtIteration = 0; // reset it to 0
                repeatCount = 2 * repeatCount; // backoff repeating the nack

                // Cap the time between checking for gaps to prevent an ever increasing interval
                if ((repeatCount * GDConfig.GD_NACK_REPETITION_THRESHOLD) > GDConfig.GD_MAX_NACK_REPETITION_THRESHOLD)
                    repeatCount = GDConfig.GD_MAX_NACK_REPETITION_THRESHOLD / GDConfig.GD_NACK_REPETITION_THRESHOLD;

                if (foundRequestedTicks)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "repeatCount=" + repeatCount);
                }
            }
            else
            {
                foundRequestedTicks = checkCuriosity(); // only check curiosity
            }

            if (foundRequestedTicks)
            {
                // found some unsatisfied Reqeested ticks.
                // Restart my timer etc.

                // Start another alarm and add ourselves back to the alarm set
                timer = am.create(GDConfig.GD_NACK_REPETITION_THRESHOLD, this);
                addAlarm(parent, this);
            }
            else
            {
                synchronized (pendingAlarms)
                {
                    // SIB0115
                    // If we have no more pending Nack alarms we are free of gaps so update health state
                    if (!getAlarms(this).hasNext())
                        getControlAdapter().getHealthState().updateHealth(HealthStateListener.GAP_DETECTED_STATE,
                                                                          HealthState.GREEN);

                    // If we've reported this gap as a problem, we now issue an 'all clear' message.
                    // (510343)
                    if (lastNackReported != -1)
                    {
                        deliverer.reportResolvedGap(remoteEngineUUID.toString(), lastNackReported);
                        lastNackReported = -1;
                    }
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(
                                tc,
                                " Gap filled from "
                                                + nackRange.startstamp
                                                + ","
                                                + nackRange.endstamp
                                                + "doubtHorizon="
                                                + getDoubtHorizon());
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "alarm");

        } // end public void alarm

        long checkCuriosityAndNack()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "checkCuriosityAndNack");

            // check what already satisfied. send nack for whatever not satisfied.
            // shrink ro to what not satisfied.
            TickRange tr1;
            boolean foundRequested = false; // were any Requested ticks found
            long tickCount = 0; // count of Requested ticks nacked

            List nackList = null;

            synchronized (GuaranteedTargetStream.this.oststream) //see defect 289889
            {

                // Check which ticks in [ro.startstamp, ro.endstamp] are still 
                // Requested and send nacks for these.
                /**
                 * Nack messages are sent after the end of the synchronized block, since if the pubend is local, the response
                 * will be on the same thread, and as the response will go to both the EdgeOut and the InternalOut(s),
                 * a deadlock may happen if the lock is not released here.
                 */

                oststream.setCursor(nackRange.startstamp);
                tr1 = oststream.getNext();
                TickRange tr2;
                do
                {
                    if (tr1.type == TickRange.Requested)
                    {
                        if (!foundRequested)
                        {
                            foundRequested = true;
                            if (tr1.startstamp > nackRange.startstamp)
                                // shrink the range this timer checks next time.
                                // We are lazy in that we do not
                                // shrink the range from the top i.e. ro.endstamp.
                                nackRange.startstamp = tr1.startstamp;
                        }
                        // need to send nack for tr1
                        if (nackList == null)
                            nackList = new ArrayList();
                        nackList.add(tr1.clone());
                    }
                    tr2 = tr1;
                    tr1 = oststream.getNext();
                } while ((tr1.startstamp <= nackRange.endstamp) && (tr1 != tr2));
            } // end synchronized

            if (nackList != null)
            {
                // send the nacks
                for (int j = 0; j < nackList.size(); j++)
                {
                    TickRange temptr = (TickRange) nackList.get(j);
                    long nackstart = temptr.startstamp;
                    long nackend = temptr.endstamp;
                    if (nackstart < nackRange.startstamp)
                        nackstart = nackRange.startstamp;
                    if (nackend > nackRange.endstamp)
                        nackend = nackRange.endstamp;

                    if (nackstart <= nackend)
                    {
                        // If this is the first nack message in the range, use it to count
                        // the number of times we have to actually send a nack message
                        // (510343)
                        if ((tickCount == 0) &&
                            (mp.getCustomProperties().getNackLogInterval() > 0))
                        {
                            synchronized (this)
                            {
                                // If this is the first nack EVER sent for this range then remember the
                                // time we first checked but don't report the gap
                                if (lastNackReportedTime == 0)
                                {
                                    lastNackReportedTime = System.currentTimeMillis();
                                }
                                else
                                {
                                    // If we've already reported this gap we need to see if the start tick of
                                    // the gap has changed or not, if it has we can issue an all-clear for that tick
                                    // before going on to report the start of the remaining gap
                                    if ((lastNackReported != -1) &&
                                        (lastNackReported != nackstart))
                                    {
                                        deliverer.reportResolvedGap(remoteEngineUUID.toString(), lastNackReported);
                                        lastNackReported = -1;
                                    }
                                    // See if we've been hitting our heads against a brick wall for long enough and
                                    // need to report this gap
                                    if ((System.currentTimeMillis() - lastNackReportedTime) > (mp.getCustomProperties().getNackLogInterval() * 1000))
                                    {
                                        deliverer.reportUnresolvedGap(remoteEngineUUID.toString(), nackstart);
                                        lastNackReported = nackstart;
                                        lastNackReportedTime = System.currentTimeMillis();
                                    }
                                }
                            }
                        }

                        // Calculate how big the gap is
                        tickCount += (nackend - nackstart + 1);
                        try
                        {
                            upControl.sendNackMessage(
                                                      streamSet.getRemoteMEUuid(),
                                                      streamSet.getDestUuid(),
                                                      streamSet.getBusUuid(),
                                                      nackstart,
                                                      nackend,
                                                      priority,
                                                      reliability,
                                                      streamSet.getStreamID());
                        } catch (SIException e)
                        {
                            // FFDC
                            FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ws.sib.processor.gd.GuaranteedTargetStream.NRTExpiryHandle.checkCuriosityAndNack",
                                                        "1:1674:1.110",
                                                        this);

                            SibTr.exception(tc, e);
                            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                        new Object[] {
                                                      "com.ibm.ws.sib.processor.gd.NRTExpiryHandle",
                                                      "1:1681:1.110",
                                                      e });

                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(tc, "checkCuriosityAndNack", e);

                            throw new SIErrorException(
                                            nls.getFormattedMessage(
                                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                                    new Object[] {
                                                                                  "com.ibm.ws.sib.processor.gd.NRTExpiryHandle",
                                                                                  "1:1692:1.110",
                                                                                  e },
                                                                    null),
                                            e);
                        }
                    }
                }
            } // end if (nackList != null)

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkCuriosityAndNack", Long.valueOf(tickCount));

            return tickCount;
        }

        boolean checkCuriosity()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "checkCuriosity");

            // check if all Requested ticks turned to Unknown
            // shrink ro to what not satisfied.

            TickRange tr1;
            boolean foundRequested = false; // were any Reqeetsed ticks found

            // Check what ticks in [ro.startstamp, ro.endstamp]
            // are still Requested
            /**
             * There is only 1 cursor, hence the synchronization.
             */
            synchronized (GuaranteedTargetStream.this.oststream) //see defect 289889
            {
                oststream.setCursor(nackRange.startstamp);
                tr1 = oststream.getNext();
                TickRange tr2;
                do
                {
                    if (tr1.type == TickRange.Requested)
                    {
                        if (!foundRequested)
                        {
                            foundRequested = true;
                            if (tr1.startstamp > nackRange.startstamp)
                                // shrink the range this NRTEH checks next time. We are lazy in that we do not
                                // shrink the range from the top i.e. ro.endstamp.
                                nackRange.startstamp = tr1.startstamp;
                            break;
                        }
                    }
                    tr2 = tr1;
                    tr1 = oststream.getNext();
                } while ((tr1.startstamp <= nackRange.endstamp) && (tr1 != tr2));

            } // end synchronized

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkCuriosity", Boolean.valueOf(foundRequested));

            return foundRequested;
        }

    }

    //Used for debug
    @Override
    public StateStream getStateStream()
    {
        return oststream;
    }

    /**
     * Flush this stream by discarding any nacks we may be waiting on
     * (all such ticks automatically become finality). When this
     * process is complete, any persistent state for the stream may be
     * discarded.
     * 
     * @throws GDException if an error occurs in writeRange.
     */
    @Override
    public void flush()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "flush");
        // Cleanup any pending NACK alarms.  Since we're flushing we don't
        // care about these anymore.
        synchronized (pendingAlarms)
        {
            for (Iterator i = getAlarms(this); i.hasNext();)
            {
                NRTExpiryHandle next = (NRTExpiryHandle) i.next();
                next.timer.cancel();
                i.remove();
            }
        }

        // SIB0115
        // If nack alarms are cancelled then deregister the health state for detected gaps
        getControlAdapter().getHealthState().deregister(HealthStateListener.GAP_DETECTED_STATE);

        // We shouldn't be flushed if we have pending unacked data.  But
        // just as a sanity check, throw an exception here if we have
        // something undelivered.
        if (oststream
                        .containsState(
                                       new TickRange(TickRange.Unknown, 0, RangeList.INFINITY),
                                       TickRange.Value)
            || oststream.containsState(
                                       new TickRange(TickRange.Unknown, 0, RangeList.INFINITY),
                                       TickRange.Uncommitted))
        {
            //TODO: throw some interesting exception
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "flush");
    }

    /**
     * Utility method for adding an alarm which needs to be expired if a
     * flush occurs.
     * 
     * @param key The key for the set of alarms to which this alarm
     *            should be added. Normally this is a stream instance.
     * @param alarmObject The alarm object to add to the set.
     */
    protected static void addAlarm(Object key, Object alarmObject)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "addAlarm", new Object[] { key, alarmObject });

        synchronized (pendingAlarms)
        {
            Set alarms = null;
            if (pendingAlarms.containsKey(key))
                alarms = (Set) pendingAlarms.get(key);
            else
            {
                alarms = new HashSet();
                pendingAlarms.put(key, alarms);
            }
            alarms.add(alarmObject);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "addAlarm");
    }

    /**
     * Utility method for removing an alarm from the alarm set. Has no
     * effect if either no alarms are associated with the given key, or
     * the given alarm object does not exist.
     * 
     * @param key The key for the set of alarms from which an alarm will
     *            be removed. Normally this is a stream instance.
     * @param alarmObject The alarm object to remove.
     */
    protected static void removeAlarm(Object key, Object alarmObject)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "removeAlarm", new Object[] { key, alarmObject });

        synchronized (pendingAlarms)
        {
            if (pendingAlarms.containsKey(key))
                ((Set) pendingAlarms.get(key)).remove(alarmObject);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "removeAlarm");
    }

    /**
     * Utility method for getting the list of all alarms associated with
     * a particular key. Returns an empty Iterator if the given key
     * has no alarms.
     * 
     * @param key The key for the set of alarms to return.
     * @return An enumeration of all alarms associated with the given
     *         key (may be empty).
     */
    protected static Iterator getAlarms(Object key)
    {
        synchronized (pendingAlarms)
        {
            if (pendingAlarms.containsKey(key))
                return ((Set) pendingAlarms.get(key)).iterator();

            return new GTSIterator();
        }
    }

    private static class GTSIterator implements Iterator
    {
        @Override
        public boolean hasNext()
        {
            return false;
        }

        @Override
        public Object next()
        {
            throw new java.util.NoSuchElementException();
        }

        @Override
        public void remove()
        {}

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.BatchListener#batchPrecommit(com.ibm.ws.sib.msgstore.transactions.Transaction)
     */
    @Override
    public void batchPrecommit(TransactionCommon currentTran)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "batchPrecommit", currentTran);
        try
        {
            persistCompletedPrefix(currentTran);
        } catch (SIResourceException e)
        {
            //FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.gd.GuaranteedTargetStream.batchPrecommit",
                                        "1:1911:1.110",
                                        this);

            SibTr.exception(tc, e);

            //TODO don't know what to do here - rethrow?!
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "batchPrecommit");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.BatchListener#batchCommitted()
     */
    @Override
    public void batchCommitted()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "batchCommitted", Integer.valueOf(messagesInBatch));

        //we have succesfully completed the batch
        numberOfMessagesReceived += messagesInBatch;
        messagesInBatch = 0;
        updateCompletedPrefix();

        sendAck();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "batchCommitted");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.BatchListener#batchRolledBack()
     */
    @Override
    public void batchRolledBack()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "batchRolledBack");

        resetDoubtHorizon();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "batchRolledBack");

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.gd.Stream#getCompletedPrefix()
     */
    @Override
    public long getCompletedPrefix()
    {
        return oststream.getCompletedPrefix();
    }

    public void setNextCompletedPrefix(long tick)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "setNextCompletedPrefix", Long.valueOf(tick));
            SibTr.exit(tc, "setNextCompletedPrefix");
        }

        nextCompletedPrefix = tick;
    }

    public long getNextCompletedPrefix()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getNextCompletedPrefix");
            SibTr.exit(tc, "getNextCompletedPrefix", Long.valueOf(nextCompletedPrefix));
        }

        return nextCompletedPrefix;
    }

    /*
     * Utility method to send Ack up to completedPrefix
     */
    private void sendAck()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "sendAck");

        long completedPrefix = oststream.getCompletedPrefix();

        // Now that all messages are delivered we need to send Acks
        // sendAck back to sending ME (RemoteMessageTransmitter)
        try
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "sendAck up to: " + completedPrefix);

            upControl.sendAckMessage(
                                     streamSet.getRemoteMEUuid(),
                                     streamSet.getDestUuid(),
                                     streamSet.getBusUuid(),
                                     completedPrefix,
                                     priority,
                                     reliability,
                                     streamSet.getStreamID(),
                                     true);
            this._lastAckedTick = completedPrefix;
        } catch (SIResourceException e)
        {
            //FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.gd.GuaranteedTargetStream.sendAck",
                                        "1:2019:1.110",
                                        this);

            SibTr.exception(tc, e);

            //TODO don't know what to do here - rethrow?!
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendAck");
    }

    // Generate a toString of the stream (thread-safe)
    public String streamToString(String str)
    {
        synchronized (oststream) //defect 256036
        {
            return getStateStream().stateString(str);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.gd.TargetStream#getLastKnownTick()
     */
    @Override
    public long getLastKnownTick()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getLastKnownTick");
            SibTr.exit(tc, "getLastKnownTick", Long.valueOf(unknownHorizon));
        }

        return unknownHorizon;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.gd.ControllableStream#getPriority()
     */
    @Override
    protected int getPriority()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getPriority");
            SibTr.exit(tc, "getPriority", Integer.valueOf(priority));
        }

        return priority;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.gd.ControllableStream#getReliability()
     */
    @Override
    protected Reliability getReliability()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getReliability");
            SibTr.exit(tc, "getReliability", reliability);
        }

        return reliability;
    }

    /**
     * Sets the stream to blocked or not blocked. The stream is set to blocked in a
     * number of situations, e.g.
     * - when a message comes in over a link and it is found that there is no room for it on the
     * target destination and it cannot be moved to an appropriate exception destination.
     * - when a message comes in over a link
     * 
     * Once blocked, the stream can only be unblocked when an AckExpected comes in and it is found
     * that either the link exception destination or the link blocking destination has room.
     * 
     * A new message coming in cannot set the stream from blocked to unblocked.
     * This is to avoid the scenario when a batch of messages have been
     * refused: if some space clears up on the destination then we do
     * not want to allow a message through that is later than the batch
     * because that will enable the put of all of the messages before it too.
     * It is safer to enable an AckExpected to prompt the unsetting of the flag
     * as this will then mean messages that have already been refused will have
     * priority over new messages. Also the system will be given more time to
     * recover.
     * 
     * @param value
     */
    private void setStreamIsBlocked(boolean value,
                                    int blockingReason,
                                    Exception exception,
                                    JsDestinationAddress blockedAddress)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setStreamIsBlocked", new Object[] { Boolean.valueOf(value),
                                                                      Integer.valueOf(blockingReason),
                                                                      exception,
                                                                      blockedAddress });

        unexpectedBlock = false;
        isStreamBlocked = value;
        streamBlockingAddress = blockedAddress;

        // Defect 535791 - Update the health state of the stream to signal that it has either become
        // blocked or unblocked
        if (isStreamBlocked)
        {
            // First we build a string representation of the blocking address, using ":" to delimit the
            // parts of the address (name, bus, ME)
            String blockingAddress = "NULL";
            if (blockedAddress != null)
            {
                String busName = blockedAddress.getBusName();
                SIBUuid8 meUuid = blockedAddress.getME();
                String meUuidStr = null;
                if (meUuid != null)
                    meUuidStr = meUuid.toString();

                blockingAddress = blockedAddress.getDestinationName();
                if (busName != null)
                    blockingAddress += ":" + busName;
                if (meUuidStr != null)
                {
                    if (busName == null)
                        blockingAddress += ":";
                    blockingAddress += ":" + meUuidStr;
                }
            }

            // Now represent the reason as a simple string
            String reasonText = "NONE";

            if (exception == null)
            {
                switch (blockingReason)
                {
                    case DestinationHandler.OUTPUT_HANDLER_NOT_FOUND:
                        reasonText = "NOT_FOUND";
                        break;
                    case DestinationHandler.OUTPUT_HANDLER_SEND_ALLOWED_FALSE:
                        reasonText = "SEND_NOT_ALLOWED";
                        break;
                    case DestinationHandler.OUTPUT_HANDLER_ALL_HIGH_LIMIT:
                        reasonText = "DESTINATION_FULL";
                        break;
                }
            }
            else
            {
                // If we caught an unexpected failure to process a message, the
                // stream becomes implicitly blocked. The one time we would 'expect'
                // this is if we had an authorization problem (as that's not checked
                // as part of streamCanAcceptNewMessage) so we handle that
                // explicitly. For anything else, putting out the exception class
                // name is probably as good as it gets (if anything really nasty happened
                // then deliverOrderedMessages will probably have FFDC'd too).
                if (exception instanceof SINotAuthorizedException)
                    reasonText = "NOT_AUTHORIZED";
                else
                    reasonText = exception.getClass().getSimpleName();

                // If an exception is passed in then we obviously thought the message should
                // have worked but it actually failed to be delivered for some reason. We need
                // to handle those failures specially, so remember this fact.
                unexpectedBlock = true;
            }

            String[] inserts = { reasonText, blockingAddress };

            // The stream is blocked
            getControlAdapter().getHealthState().updateHealth(HealthStateListener.BLOCKED_TARGET_STREAM_STATE,
                                                              HealthState.AMBER,
                                                              inserts);
        }
        else
        {
            // The link is no longer blocked
            getControlAdapter().getHealthState().updateHealth(HealthStateListener.BLOCKED_TARGET_STREAM_STATE,
                                                              HealthState.GREEN);
            linkBlockingTick = -1;
            blockingCount = 0;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setStreamIsBlocked");
    }

    /**
     * Is the stream marked as blocked.
     * 
     * @return true if the stream is blocked.
     */
    private boolean isStreamBlocked()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "isStreamBlocked");
            SibTr.exit(tc, "isStreamBlocked", new Object[] { Boolean.valueOf(isStreamBlocked),
                                                            Long.valueOf(linkBlockingTick) });
        }
        return this.isStreamBlocked;

    }

    /**
     * Is the stream marked as blocked unexpectedly
     * 
     * @return true if the stream is blocked.
     */
    private boolean isStreamBlockedUnexpectedly()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "isStreamBlockedUnexpectedly");
            SibTr.exit(tc, "isStreamBlockedUnexpectedly", Boolean.valueOf(unexpectedBlock));
        }
        return unexpectedBlock;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.gd.TargetStream#getStreamState
     */
    @Override
    public TargetStreamState getStreamState()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getStreamState");
            SibTr.exit(tc, "getStreamState", streamState);
        }

        return streamState;
    }

    /**
     * Check to see if the stream is able to accept a new message. If it cannot, then the message
     * is only allowed through if it has the possibility of filling in a gap. See defects 244425 and
     * 464463.
     * 
     * @param msgItem The msg to be let through.
     * @param valueTick GD Value Tick
     * @return true if the message should be written as value to the stream
     * @throws SIException
     */
    private boolean streamCanAcceptNewMessage(MessageItem msgItem, long valueTick) throws SIException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "streamCanAcceptNewMessage", new Object[] { msgItem, Long.valueOf(valueTick) });
        boolean allowSend = false;

        if (isStreamBlocked())
        {
            //once the stream is blocked, only an AckExpected message or a Silence on the blocking tick
            //can unset it - therefore we do not re-check and we only allow this message through if it fills a gap.
            if (valueTick <= valueHorizon)
            {
                //the message might fill in a gap so we allow it through
                allowSend = true;
            }
        }
        else
        {
            //the stream is not currently blocked.
            //However, the destination might no longer be able to accept messages, in which case
            //we should update the flag and only allow the send if the message fills a gap.
            JsMessage msg = msgItem.getMessage();
            int blockingReason = deliverer.checkAbleToAcceptMessage(msg.getRoutingDestination());
            if (blockingReason != DestinationHandler.OUTPUT_HANDLER_FOUND)
            {
                //We might still be able to let this message through if it
                //fills in a gap
                if (valueTick <= valueHorizon)
                {
                    //the message might fill in a gap so we allow it through
                    allowSend = true;
                }
                else
                {
                    // The stream is now blocked
                    setStreamIsBlocked(true, blockingReason, null, msg.getRoutingDestination());
                    // Keep track of the value tick. We may subsequently get a SILENCE for the
                    // tick signalling that the associated message has been deleted from the 
                    // source and that the stream may be marked as unblocked.
                    linkBlockingTick = valueTick;
                }
            }
            else
            {
                allowSend = true;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "streamCanAcceptNewMessage", Boolean.valueOf(allowSend));

        return allowSend;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.gd.TargetStream#getControlAdapter
     */
    @Override
    public TargetStreamControl getControlAdapter()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getControlAdapter");

        if (targetStreamControl == null)
            targetStreamControl =
                            new TargetStreamControl(remoteEngineUUID, this, streamSet.getStreamID(),
                                            getReliability(), getPriority());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getControlAdapter", targetStreamControl);
        return targetStreamControl;
    }

    @Override
    public List<MessageItem> getAllMessagesOnStream()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getAllMessagesOnStream");

        List<MessageItem> msgs = new LinkedList<MessageItem>();

        synchronized (oststream)
        {
            oststream.setCursor(0);

            // Get the first TickRange
            TickRange tr = oststream.getNext();

            while (tr.endstamp < RangeList.INFINITY)
            {
                if (tr.type == TickRange.Value)
                {
                    msgs.add((MessageItem) tr.value);
                }
                tr = oststream.getNext();
            } // end while      
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getAllMessagesOnStream", msgs);

        return Collections.unmodifiableList(msgs);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.gd.TargetStream#getNumberOfMessagesReceived
     */
    @Override
    public long getNumberOfMessagesReceived()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getNumberOfMessagesReceived");
            SibTr.exit(tc, "getNumberOfMessagesReceived", Long.valueOf(numberOfMessagesReceived));
        }

        return numberOfMessagesReceived;
    }

    @Override
    public long countAllMessagesOnStream()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "countAllMessagesOnStream");

        long count = 0;

        synchronized (oststream) //see defect 289889
        {
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
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "countAllMessagesOnStream", Long.valueOf(count));
        return count;
    }

    @Override
    public long getLastMsgReceivedTimestamp() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getLastMsgReceivedTimestamp");
            SibTr.exit(tc, "getLastMsgReceivedTimestamp", Long.valueOf(timeLastMsgReceived));
        }
        return timeLastMsgReceived;
    }

    @Override
    public String toString()
    {
        String text = super.toString() + " [";

        text += deliverer;
        if (streamSet != null)
            text += streamSet.getStreamID();
        text += remoteEngineUUID + "]";

        return text;
    }
}
