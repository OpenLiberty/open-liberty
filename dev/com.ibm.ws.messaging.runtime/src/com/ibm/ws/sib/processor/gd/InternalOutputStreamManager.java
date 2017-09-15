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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.control.ControlAck;
import com.ibm.ws.sib.mfp.control.ControlAreYouFlushed;
import com.ibm.ws.sib.mfp.control.ControlNack;
import com.ibm.ws.sib.mfp.control.ControlNotFlushed;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPErrorException;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.DownstreamControl;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.interfaces.UpstreamControl;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPPubSubOutboundTransmitControllable;
import com.ibm.ws.sib.processor.runtime.impl.BasicSIMPIterator;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.processor.runtime.impl.InternalOutputStreamSetControl;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

public class InternalOutputStreamManager
{
    private final UpstreamControl upControl;

    private final SIBUuid8 targetMEUuid;

    private static final TraceComponent tc =
                    SibTr.register(
                                   InternalOutputStreamManager.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    private static final TraceNLS nls =
                    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

    private final DownstreamControl downControl;

    // Maps stream IDs to instances of StreamSet.  For v1, we should
    // only be maintaining information about the stream in the local
    // PubSubInputHandler (for a given destination).  Longer term,
    // we may have many streams stored here.
    private final Map<SIBUuid12, StreamSet> streamSets;

    // Has a streamID been set into the streamSet created when the InternalOutputStreamManager is 
    // instantiated. 
    private boolean shellStreamSetOnly = false;

    // Are we targetting a foreign bus
    private boolean isLink = false;

    private final MessageProcessor messageProcessor;

    public InternalOutputStreamManager(DownstreamControl downControl,
                                       UpstreamControl upControl,
                                       MessageProcessor messageProcessor,
                                       SIBUuid8 targetMEUuid,
                                       boolean isLink)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "InternalOutputStreamManager", new Object[] {
                                                                         downControl, upControl, messageProcessor, targetMEUuid,
                                                                         Boolean.valueOf(isLink) });

        this.downControl = downControl;
        this.upControl = upControl;
        this.targetMEUuid = targetMEUuid;
        this.isLink = isLink;
        this.messageProcessor = messageProcessor;

        streamSets = Collections.synchronizedMap(new HashMap<SIBUuid12, StreamSet>());

        // Defect 526370: Instantiate a shell StreamSet. The streamSets collection currently has a single 
        // active member streamSet whose streamId is defined based on the first publication handled by this 
        // InternalOutputStreamManager (IOSM). We create the StreamSet object here so that the controllable 
        // infrastructure that is associated with the StreamSet but is used in processing resources for 
        // the IOSM as a whole, is in place.
        createShellStreamSet();
        shellStreamSetOnly = true;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "InternalOutputStreamManager", this);
    }

    /**
     * This method will create a StreamSet with null StreamId.
     * 
     */
    public void createShellStreamSet()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createShellStreamSet");

        StreamSet streamSet = null;

        synchronized (streamSets)
        {
            streamSet = new StreamSet(null,
                            targetMEUuid,
                            0,
                            isLink ? StreamSet.Type.LINK_INTERNAL_OUTPUT : StreamSet.Type.INTERNAL_OUTPUT);
            streamSets.put(null, streamSet);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createShellStreamSet", streamSet);
    }

    /**
     * Get a StreamSet for a given streamID. Optionally create the StreamSet
     * if it doesn't already exit.
     * 
     * @param streamID The streamID to map to a StreamSet.
     * @param create If TRUE then create the StreamSet if it doesn't already exit.
     * @return An instance of StreamSet
     */
    public StreamSet getStreamSet(SIBUuid12 streamID, boolean create)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getStreamSet", new Object[] { streamID, Boolean.valueOf(create) });

        StreamSet streamSet = null;
        synchronized (streamSets)
        {
            if (shellStreamSetOnly)
            {
                // We have yet to fully initialise the StreamSet managed by this object. Start by
                // retrieving the shell StreamSet object
                streamSet = streamSets.get(null);
                // remove the shell from the Collection
                streamSets.remove(null);
                shellStreamSetOnly = false;
                // Set the streamID into the StreamSet
                if (streamSet != null)
                {
                    // Set the StreamId into the shell StreamSet
                    streamSet.setStreamID(streamID);
                    // Store the StreamSet in the Collection keyed on StreamId.
                    streamSets.put(streamID, streamSet);
                }
            }
            else
            {
                streamSet = streamSets.get(streamID);
            }

            if ((streamSet == null) && create)
            {
                streamSet = new StreamSet(streamID,
                                targetMEUuid,
                                0,
                                isLink ? StreamSet.Type.LINK_INTERNAL_OUTPUT : StreamSet.Type.INTERNAL_OUTPUT);
                streamSets.put(streamID, streamSet);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getStreamSet", streamSet);
        return streamSet;
    }

    public void addMessage(SIMPMessage msgItem, boolean commitInsert) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "addMessage", new Object[] { msgItem, Boolean.valueOf(commitInsert) });

        // Get the JsMessage as we need to get the Guaranteed fields
        JsMessage jsMsg = msgItem.getMessage();
        SIBUuid12 streamID = jsMsg.getGuaranteedStreamUUID();
        int priority = jsMsg.getPriority().intValue();
        Reliability reliability = jsMsg.getReliability();
        StreamSet streamSet = getStreamSet(streamID, true);
        // Defect 520453: Allocating the messages's tick and creating this stream is not atomic so there
        // is no guarantee that the message that causes the stream to be created is the 'first' message
        // in the stream. Therefore, the start point cannot be this tick-1. Instead we use the completedPrefix
        // of the SOurceStream at the time that the tick was allocated, in the knowledge that any message's
        // before us cannot possibly by complete yet as the output stream hasn;t even been created.
        long streamStart = jsMsg.getGuaranteedValueCompletedPrefix();
        InternalOutputStream internalOutputStream = null;
        synchronized (streamSet)
        {
            internalOutputStream = (InternalOutputStream) streamSet.getStream(priority, reliability);

            if (internalOutputStream == null &&
                (reliability.compareTo(Reliability.BEST_EFFORT_NONPERSISTENT) > 0))
            {
                internalOutputStream = createStream(streamSet, priority, reliability, streamStart, jsMsg.getGuaranteedValueStartTick() - 1); //PM62615.dev
            }
        }

        // If the message came from a remoteME we don't bother to add it to the stream
        // as we don't have it on a local ItemStream and we have to ask the sourceME to resend
        // it if we need to satisfy a Nack. 
        if (!msgItem.isFromRemoteME())
        {

            // NOTE: internalOutputStream should only be null for express qos
            if (internalOutputStream != null)
            {
                if (commitInsert)
                    internalOutputStream.writeValue(msgItem);
                else
                    internalOutputStream.writeUncommitted(msgItem);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "addMessage");
    }

    public void addSilence(SIMPMessage msgItem) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "addSilence", msgItem);
        // Get the JsMessage as we need to update the Guaranteed fields
        JsMessage jsMsg = msgItem.getMessage();
        SIBUuid12 streamID = jsMsg.getGuaranteedStreamUUID();
        int priority = jsMsg.getPriority().intValue();
        Reliability reliability = jsMsg.getReliability();
        StreamSet streamSet = getStreamSet(streamID, true);
        // Defect 520453: Allocating the messages's tick and creating this stream is not atomic so there
        // is no guarantee that the message that causes the stream to be created is the 'first' message
        // in the stream. Therefore, the start point cannot be this tick-1. Instead we use the completedPrefix
        // of the SOurceStream at the time that the tick was allocated, in the knowledge that any message's
        // before us cannot possibly by complete yet as the output stream hasn;t even been created.
        long streamStart = jsMsg.getGuaranteedValueCompletedPrefix();
        InternalOutputStream internalOutputStream = null;
        synchronized (streamSet)
        {

            internalOutputStream = (InternalOutputStream) streamSet.getStream(priority, reliability);

            if (internalOutputStream == null &&
                (reliability.compareTo(Reliability.BEST_EFFORT_NONPERSISTENT) > 0))
            {
                internalOutputStream = createStream(streamSet, priority, reliability, streamStart, jsMsg.getGuaranteedValueStartTick() - 1); //PM62615.dev    
            }
        }

        // NOTE: sourceStream should only be null for express qos
        if (internalOutputStream != null)
        {
            internalOutputStream.writeSilence(msgItem);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "addSilence");
    }

    // Called by PubSubOutputHandler when the associated PubSubInputHandler
    // needs to send an AckExpected downstream.
    public void processAckExpected(long stamp,
                                   int priority,
                                   Reliability reliability,
                                   SIBUuid12 streamID)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "processAckExpected",
                        new Object[] { Long.valueOf(stamp), Integer.valueOf(priority), reliability, streamID });

        // NOTE: pass false here (meaning don't create a new streamSet) because we should only get 
        // an AckExpected (under v1 anyway) if we've already received data of some sort
        // (and hence already created the stream).
        StreamSet streamSet = getStreamSet(streamID, false);

        if (streamSet != null)
        {
            InternalOutputStream internalOutputStream = (InternalOutputStream) streamSet.getStream(priority, reliability);

            // There is no stream for BestEffort non persistent messages         
            if (reliability.compareTo(Reliability.BEST_EFFORT_NONPERSISTENT) > 0)
            {
                // We do need to write the message to the stream    
                if (internalOutputStream != null)
                {
                    internalOutputStream.processAckExpected(stamp);
                }
                else
                {
                    // We didn't expect an AckExpected for this stream so throw it away but write an entry 
                    // to the trace log indicating that it happened
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Unexpected AckExpected message for streamID " + streamID
                                        + " Reliability " + reliability + " priority " + priority);
                }
            }
        }
        else
        {
            // We didn't expect an AckExpected for this streamID so throw it away but write an entry  
            // to the trace log indicating that it happened
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "AckExpected message for unknown streamID " + streamID);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processAckExpected");
    }

    public void processAck(ControlAck ackMsg) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "processAck",
                        new Object[] { ackMsg });

        // Get the ackPrefix from the message
        long ackPrefix = ackMsg.getAckPrefix();

        processAck(ackMsg, ackPrefix);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processAck");
    }

    public void processAck(ControlAck ackMsg, long ackPrefix) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "processAck",
                        new Object[] { ackMsg });

        int priority = ackMsg.getPriority().intValue();
        Reliability reliability = ackMsg.getReliability();
        SIBUuid12 streamID = ackMsg.getGuaranteedStreamUUID();

        // NOTE: pass false here (meaning don't create a new streamSet) because we should only get 
        // an Ack (under v1 anyway) if we've already received data of some sort
        // (and hence already created the stream).
        StreamSet streamSet = getStreamSet(streamID, false);

        if (streamSet != null)
        {
            InternalOutputStream internalOutputStream = (InternalOutputStream) streamSet.getStream(priority, reliability);

            // There is no stream for BestEffort non persistent messages         
            if (reliability.compareTo(Reliability.BEST_EFFORT_NONPERSISTENT) > 0)
            {
                // We do need to write the message to the stream    
                if (internalOutputStream != null)
                {
                    /* PM62615.dev- Start */
                    //If internalOutputStream presents, then call removeTickRangeObjectFromHashmap
                    // so that it will delete any TickRange object from InternalOutputStream internal hashmap
                    // new TickRange object is created with start and end stamps as ackPrefix and TickRange state as Completed
                    // TickRange is state is ignored by the removeTickRangeObjectFromHashmap, so it is immaterial
                    internalOutputStream.removeTickRangeObjectFromHashmap(new TickRange(TickRange.Completed, ackPrefix, ackPrefix));

                    // the new TickRange object created as passed as parameter to the above function, should be eligible to be
                    // Garbage collected when the function returns as inside the function the new object reference is not kept(further chaining) by any other object
                    /* PM62615- End */

                    // If this increases the finality prefix then
                    // update it and delete the acked messages from the ItemStream
                    long completedPrefix = internalOutputStream.getAckPrefix();
                    if (ackPrefix > completedPrefix)
                    {

                        // Update the completedPrefix and the oack value for the stream
                        // returns a lit of the itemStream ids of the newly Acked messages
                        // which we can then remove from the itemStream
                        internalOutputStream.writeAckPrefix(ackPrefix);
                    }
                    else
                    {
                        //the msg has already been completed - we should log this
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "Unexpected Ack " + ackPrefix + " : completed prefix is " + completedPrefix);
                    }
                }
                else
                {
                    // We didn't expect an Ack for this stream so throw it away but write an entry 
                    // to the trace log indicating that it happened
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Unexpected Ack message for streamID " + streamID
                                        + " Reliability " + reliability + " priority " + priority);
                }
            }
        }
        else
        {
            // We didn't expect an Ack for this streamID so throw it away but write an entry  
            // to the trace log indicating that it happened
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Ack message for unknown streamID " + streamID);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processAck");
    }

    public void processNack(ControlNack nackMsg)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "processNack", nackMsg);

        int priority = nackMsg.getPriority().intValue();
        Reliability reliability = nackMsg.getReliability();
        SIBUuid12 streamID = nackMsg.getGuaranteedStreamUUID();

        // NOTE: pass false here (meaning don't create a new streamSet) because we should only get 
        // a Nack (under v1 anyway) if we've already received data of some sort
        // (and hence already created the stream).
        StreamSet streamSet = getStreamSet(streamID, false);

        if (streamSet != null)
        {
            InternalOutputStream internalOutputStream = (InternalOutputStream) streamSet.getStream(priority, reliability);

            // There is no stream for BestEffort non persistent messages         
            if (reliability.compareTo(Reliability.BEST_EFFORT_NONPERSISTENT) > 0)
            {
                // We do need to write the message to the stream    
                if (internalOutputStream != null)
                {
                    internalOutputStream.processNack(nackMsg);
                }
                else
                {

                    // We didn't expect a Nack for this stream so throw it away but write an entry 
                    // to the trace log indicating that it happened
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Unexpected Nack message for streamID " + streamID
                                        + " Reliability " + reliability + " priority " + priority);
                }
            }
        }
        else
        {
            // We didn't expect a Nack for this streamID so throw it away but write an entry  
            // to the trace log indicating that it happened
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Nack message for unknown streamID " + streamID);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processNack");

    }

    /**
     * Handle a flush query.
     * 
     * @param flushQuery The control message which elicited the query.
     * @throws SIResourceException I'm pretty sure this can't actually
     *             be thrown.
     */
    public void processFlushQuery(ControlAreYouFlushed flushQuery) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "processFlushQuery", new Object[] { flushQuery });

        SIBUuid12 streamID = flushQuery.getGuaranteedStreamUUID();

        try
        {
            //synchronize to give a consistent view of the flushed state
            synchronized (this)
            {
                SIBUuid8 requestor = flushQuery.getGuaranteedSourceMessagingEngineUUID();
                if (isFlushed(streamID))
                {
                    // It's flushed.  Send a message saying as much.
                    downControl.sendFlushedMessage(requestor, streamID);
                }
                else
                {
                    // Not flushed.  Send a message saying as much.
                    downControl.sendNotFlushedMessage(requestor, streamID, flushQuery.getRequestID());
                }
            }
        } catch (SIResourceException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.gd.InternalOutputStreamManager.processFlushQuery",
                                        "1:516:1.48.1.1",
                                        this);
            SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "processFlushQuery", e);
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processFlushQuery");

    }

    private InternalOutputStream createStream(StreamSet streamSet,
                                              int priority,
                                              Reliability reliability,
                                              long completedPrefix,
                                              long LastMessageTickWhenStreamCreated) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createStream", new Object[] { Integer.valueOf(priority), reliability });

        InternalOutputStream stream = null;

        //there is no source stream for express messages
        if (reliability.compareTo(Reliability.BEST_EFFORT_NONPERSISTENT) > 0)
        {
            stream = new InternalOutputStream(priority,
                            reliability,
                            completedPrefix,
                            downControl,
                            upControl,
                            null,
                            streamSet.getStreamID(),
                            streamSet,
                            messageProcessor.getAlarmManager(),
                            messageProcessor,
                            LastMessageTickWhenStreamCreated); //PM62615.dev
        }

        streamSet.setStream(priority, reliability, stream);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createStream", stream);

        return stream;
    }

    public boolean commitInsert(SIMPMessage msg) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "commitInsert", new Object[] { msg });

        boolean sendMessage = false;
        JsMessage jsMsg = msg.getMessage();

        SIBUuid12 streamID = jsMsg.getGuaranteedStreamUUID();

        // When getting the streamSet, don't create a new one since the previous call
        // to addMessage should have already done that.
        StreamSet streamSet = getStreamSet(streamID, false);

        // We should always have a StreamSet at this point as it must have been created at preCommit
        // time. FFDC and throw an exception if we don't
        if (streamSet == null)
        {
            SIMPErrorException e = new SIMPErrorException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0005",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.gd.InternalOutputStreamManager.commitInsert",
                                                                  "1:585:1.48.1.1",
                                                                  this },
                                                    null));

            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.gd.InternalOutputStreamManager.commitInsert",
                                        "1:592:1.48.1.1",
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "commitInsert");

            throw e;
        }

        int priority = jsMsg.getPriority().intValue();
        Reliability reliability = jsMsg.getReliability();

        InternalOutputStream internalOutputStream = (InternalOutputStream) streamSet.getStream(priority, reliability);

        // Should only be null for express streams...
        if (internalOutputStream != null)
        {
            // Write the value message to the stream
            // This will also write a range of Completed ticks between
            // the previous Value tick and the new one
            sendMessage = internalOutputStream.writeValue(msg);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "commitInsert");

        return sendMessage;
    }

    public boolean rollbackInsert(MessageItem msg)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "rollbackInsert", msg);

        boolean sendMessage = false;
        JsMessage jsMsg = msg.getMessage();

        SIBUuid12 streamID = jsMsg.getGuaranteedStreamUUID();

        // pass false since previous addMessage should have already 
        // created the stream if it didn't exist.
        StreamSet streamSet = getStreamSet(streamID, false);

        // We should always have a StreamSet at this point as it must have been created at preCommit
        // time. FFDC and throw an exception if we don't
        if (streamSet == null)
        {
            SIMPErrorException e = new SIMPErrorException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0005",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.gd.InternalOutputStreamManager.rollbackInsert",
                                                                  "1:647:1.48.1.1",
                                                                  this },
                                                    null));

            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.gd.InternalOutputStreamManager.rollbackInsert",
                                        "1:654:1.48.1.1",
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "rollbackInsert");

            throw e;
        }

        int priority = jsMsg.getPriority().intValue();
        Reliability reliability = jsMsg.getReliability();

        InternalOutputStream internalOutputStream = (InternalOutputStream) streamSet.getStream(priority, reliability);

        // should only be null for express streams
        if (internalOutputStream != null)
        {
            // Write the value message to the stream
            // This will also write a range of Completed ticks between
            // the previous Value tick and the new one
            sendMessage = internalOutputStream.writeSilence(msg);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "rollbackInsert");

        return sendMessage;
    }

    /**
     * @param ack
     * @param min
     * @throws SIResourceException
     */
    public long checkAck(ControlAck ack, long min) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkAck", new Object[] { ack, Long.valueOf(min) });
        int priority = ack.getPriority().intValue();
        Reliability reliability = ack.getReliability();
        SIBUuid12 streamID = ack.getGuaranteedStreamUUID();
        StreamSet streamSet = getStreamSet(streamID, false);

        // It's possible that we've never seen any messages on this
        // stream due to filtering.  So if we don't have a stream set
        // for it, then just return the min.
        if (streamSet == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkAck", Long.valueOf(min));
            return min;
        }

        InternalOutputStream internalOutputStream = (InternalOutputStream) streamSet.getStream(priority, reliability);

        if (internalOutputStream != null)
        {
            long ackPrefix = internalOutputStream.getAckPrefix();
            if (ackPrefix < min)
                min = ackPrefix;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkAck", new Long(min));
        return min;
    }

    /**
     * This method will be called to force flush of streamSet
     */
    public void forceFlush(SIBUuid12 streamID) throws SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "forceFlush", streamID);

        StreamSet streamSet = streamSets.get(streamID);
        streamSet.dereferenceControlAdapter();

        // Send out a flushed message.  If this fails, make sure we get
        // to at least invoke the callback.
        try
        {
            // The Cellule argument is null as it is ignored by our parent handler
            // this flush message should be broadcast downstream.
            // This will also implicitly remove the streamSet
            downControl.sendFlushedMessage(null, streamID);
        } catch (Exception e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.gd.InternalOutputStreamManager.forceFlush",
                                        "1:743:1.48.1.1",
                                        this);

            // Note that it doesn't make much sense to throw an exception here since
            // this is a callback from stream array map.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "forceFlush", e);
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "forceFlush");
    }

    /**
     * Remove any info for the given stream. NOOP if the stream
     * isn't known to us.
     * 
     * @param streamID The stream to remove.
     */
    public void remove(SIBUuid12 streamID)
    {
//    StreamSet set = (StreamSet)streamSets.get(streamID);
//    if (set!=null)
//    {
//      try
//      {
//        set.remove();
//      }
//      catch (SIException e) // Cant remove persistent state
//      {
//        FFDCFilter.processException(
//          e,
//          "com.ibm.ws.sib.processor.gd.InternalOutputStreamManager.remove",
//          "1:777:1.48.1.1",
//          this);
//    
//        SIMPRuntimeOperationFailedException finalE =
//          new SIMPRuntimeOperationFailedException(
//            nls.getFormattedMessage(
//              "INTERNAL_MESSAGING_ERROR_CWSIP0003",
//              new Object[] {"InternalOutputStreamManager.remove",
//                            "1:785:1.48.1.1",
//                            e},
//              null), e);
//
//        SibTr.exception(tc, finalE);
//      }
//    }
        streamSets.remove(streamID);
    }

    /**
     * returns true if a given set of streams are flushed
     * 
     * @return true if flushed
     */
    private boolean isFlushed(SIBUuid12 streamID)
    {
        if (getStreamSet(streamID, false) == null)
            return true;

        return false;
    }

    /**
     * Attach the appropriate completed and duplicate prefixes for the
     * stream stored in this array to a ControlNotFlushed message.
     * 
     * @param msg The ControlNotFlushed message to stamp.
     * @throws SIResourceException
     */
    public ControlNotFlushed stampNotFlushed(ControlNotFlushed msg, SIBUuid12 streamID) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "stampNotFlushed", new Object[] { msg });

        int count = 0;

        //the maximum possible number of streams in a set
        int max = (SIMPConstants.MSG_HIGH_PRIORITY + 1) * (Reliability.MAX_INDEX + 1);
        //an array of priorities
        int[] ps = new int[max];
        //an array of reliabilities
        int[] qs = new int[max];
        //an array of prefixes
        long[] cs = new long[max];

        StreamSet streamSet = getStreamSet(streamID, false);

        //iterate through the non-null streams
        Iterator itr = streamSet.iterator();
        while (itr.hasNext())
        {
            InternalOutputStream oStream = (InternalOutputStream) itr.next();

            // for each stream, store it's priority, reliability and completed prefix
            ps[count] = oStream.getPriority();
            qs[count] = oStream.getReliability().toInt();
            cs[count] = oStream.getCompletedPrefix();
            count++;
        }

        //create some arrays which are of the correct size
        int[] realps = new int[count];
        int[] realqs = new int[count];
        long[] realcs = new long[count];

        //copy the data in to them
        System.arraycopy(ps, 0, realps, 0, count);
        System.arraycopy(qs, 0, realqs, 0, count);
        System.arraycopy(cs, 0, realcs, 0, count);

        //set the appropriate message fields
        msg.setCompletedPrefixPriority(realps);
        msg.setCompletedPrefixQOS(realqs);
        msg.setCompletedPrefixTicks(realcs);

        msg.setDuplicatePrefixPriority(realps);
        msg.setDuplicatePrefixQOS(realqs);
        msg.setDuplicatePrefixTicks(realcs);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "stampNotFlushed", msg);
        //return the message
        return msg;
    }

    /**
     * We need this class because we need to set the InternalOutputStreamManager
     * in each InternalOutputStreamSetControllable that exists.
     * For the source stream equivalent, we simply performed this in the
     * getStreamSetRuntimeControl() of SourceStreamManager. In this case it is
     * more complex as we have to support the possibility of multiple
     * InternalOutputStreams for future releases.
     * 
     * @author tpm100
     */
    private static final class InternalOutputStreamSetControllableIterator extends BasicSIMPIterator
    {
        private final TraceComponent innerTC =
                        SibTr.register(
                                       InternalOutputStreamSetControllableIterator.class,
                                       SIMPConstants.MP_TRACE_GROUP,
                                       SIMPConstants.RESOURCE_BUNDLE);

        ControlAdapter parent = null;

        InternalOutputStreamSetControllableIterator(Iterator it, ControlAdapter parent)
        {
            super(it);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(innerTC, "InternalOutputStreamSetControllableIterator", new Object[] { it, parent });
            this.parent = parent;

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(innerTC, "InternalOutputStreamSetControllableIterator", this);
        }

        @Override
        public Object next()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(innerTC, "next");
            StreamSet set = (StreamSet) super.next();
            SIMPPubSubOutboundTransmitControllable ioSet = null;
            if (set != null)
            {
                ioSet = (SIMPPubSubOutboundTransmitControllable) set.getControlAdapter();
                ((InternalOutputStreamSetControl) ioSet).setParentControlAdapter(parent);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(innerTC, "next", ioSet);
            return ioSet;
        }

    }

    public SIMPIterator getStreamSetControlIterator(ControlAdapter parent)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getStreamSetControlIterator", parent);

        InternalOutputStreamSetControllableIterator itr =
                        new InternalOutputStreamSetControllableIterator(streamSets.values().iterator(), parent);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getStreamSetControlIterator", streamSets.values());
        return itr;
    }

    /**
     * Remove a message from the appropriate source stream.
     * This is done by replacing the message with Silence in the stream
     * Since it can only be called when a message is already in the stream the
     * stream must exist. If it doesn't we have an internal error
     * 
     * @param the stream set from which the messae should be removed
     * @param msgItem The message to remove
     * @throws SIResourceException
     */
    public void removeMessage(StreamSet streamSet, SIMPMessage msgItem) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "removeMessage", new Object[] { streamSet, msgItem });

        int priority = msgItem.getPriority();
        Reliability reliability = msgItem.getReliability();

        InternalOutputStream ioStream =
                        (InternalOutputStream) streamSet.getStream(priority, reliability);
        JsMessage jsMsg = msgItem.getMessage();
        long start = jsMsg.getGuaranteedValueStartTick();
        long end = jsMsg.getGuaranteedValueEndTick();

        TickRange tr =
                        new TickRange(
                                        TickRange.Completed,
                                        start,
                                        end);

        if (ioStream != null)
        {
            ioStream.writeSilenceForced(tr);
        }
        //else
        // {
        // Throw internal error

        // }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "removeMessage");
    }

}
