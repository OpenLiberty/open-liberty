/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.processor.impl.store.itemstreams;

import java.util.Hashtable;
import java.util.Properties;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.management.SibNotificationConstants;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NotInMessageStore;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.LinkHandler;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.ControllableResource;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.processor.runtime.impl.MPRuntimeEvent;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author nyoung
 */
public abstract class BaseMessageItemStream extends SIMPItemStream
                implements ControllableResource
{

    private static final TraceComponent tc =
                    SibTr.register(
                                   BaseMessageItemStream.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    private static final TraceNLS nls =
                    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

    protected ControlAdapter controlAdapter;
    /**
     * The destinationHandler for which this is a localisation.
     */
    protected BaseDestinationHandler destinationHandler;

    /**
     * The MessageProcessor of the ME for which this is a localisation.
     */
    protected MessageProcessor mp;

    /**
     * Cache up whether event notification is enabled.
     */
    protected boolean _isEventNotificationEnabled = false;

    /**
     * The currently used high message limit count for this localisation.
     */
    protected long _destHighMsgs;

    /**
     * The currently used low message limit count for this localisation.
     */
    protected long _destLowMsgs;

    /**
     * The interval between reporting message depths (defaults to being disabled)
     * (510343)
     */
    protected long _destMsgInterval = 0;
    protected long _nextUpDepthInterval = Long.MAX_VALUE;
    protected long _nextDownDepthInterval = Long.MIN_VALUE;

    /**
     * The current MsgStore watermarks for this destination (these will change if
     * message depth intervals are being reported) (510343)
     */
    protected long _nextHighWatermark;
    protected long _nextLowWatermark;

    /**
     * The state that indicates that the WLM advert has been removed due to limits.
     * This flag is also used in determining when to fire events.
     */
    boolean wlmRemoved = false;

    /**
     * Constructor.
     */
    public BaseMessageItemStream()
    {
        super();
    }

    /**
     * Initialize non-persistent fields. These fields are common to both MS
     * reconstitution of DestinationHandlers and initial creation.
     * <p>
     * In the warm start case, by the time this method is called, we know we
     * have all the persistent information available.
     * <p>
     * Feature 174199.2.9
     * 
     * @param destinationHandler to reference
     */
    public void initializeNonPersistent(BaseDestinationHandler destinationHandler)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "initializeNonPersistent", destinationHandler);

        /**
         * Remember the destinationHandler that represents the destination
         * for which this is a localisation.
         */
        this.destinationHandler = destinationHandler;
        this.mp = destinationHandler.getMessageProcessor();
        // Is event notification enabled
        _isEventNotificationEnabled = mp.getMessagingEngine().isEventNotificationEnabled();

        createControlAdapter();

        //defect 260346
        //registerControlAdapterAsMBean();         

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "initializeNonPersistent");
    }

    /**
     * Set the default limits for this itemstream
     * 
     */
    public synchronized void setDefaultDestLimits() throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setDefaultDestLimits");

        // Defaults are based on those defined to the ME, the low is 80% of the high
        // Use setDestLimits() to set the initial limits/watermarks (510343)
        long destHighMsgs = mp.getHighMessageThreshold();
        setDestLimits(destHighMsgs,
                      (destHighMsgs * 8) / 10);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setDefaultDestLimits");
    }

    /**
     * Gets the destination high messages limit currently being used by this localization.
     * 
     * @return
     */
    public long getDestHighMsgs()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getDestHighMsgs");
            SibTr.exit(tc, "getDestHighMsgs", Long.valueOf(_destHighMsgs));
        }
        return _destHighMsgs;
    }

    /**
     * Gets the destination low messages limit currently being used by this localization.
     * 
     * @return
     */
    public long getDestLowMsgs()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getDestLowMsgs");
            SibTr.exit(tc, "getDestLowMsgs", Long.valueOf(_destLowMsgs));
        }
        return _destLowMsgs;
    }

    /**
     * Allows the mbean, or dynamic config to set the destination limits currently used
     * by this localization.
     * 
     * @param newDestHighMsgs
     */
    public void setDestHighMsgs(long newDestHighMsgs)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setDestHighMsgs", Long.valueOf(newDestHighMsgs));

        setDestLimits(newDestHighMsgs, _destLowMsgs);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setDestHighMsgs");
    }

    /**
     * Allows the mbean, or dynamic config to set the destination limits currently used
     * by this localization.
     * 
     * @param newDestLowMsgs
     */
    public void setDestLowMsgs(long newDestLowMsgs)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setDestLowMsgs", Long.valueOf(newDestLowMsgs));

        setDestLimits(_destHighMsgs, newDestLowMsgs);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setDestLowMsgs");
    }

    /**
     * Allows the mbean, or dynamic config to set the destination limits currently used
     * by this localization.
     * 
     * @param newDestHighMsgs
     * @param newDestLow
     */
    protected synchronized void setDestLimits(long newDestHighMsgs,
                                              long newDestLowMsgs)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setDestLimits",
                        new Object[] { new Long(newDestHighMsgs),
                                      new Long(newDestLowMsgs) });

        _destHighMsgs = newDestHighMsgs;
        if (newDestLowMsgs >= _destHighMsgs)
        {
            //defect 244425: it is not sensible to allow the destLowMessages
            //watermark to be set higher than destinationHighMessages 
            long decrease = (long) (_destHighMsgs * destinationHandler.getMessageProcessor().getCustomProperties().get_dest_low_unset_percentage_decrease());
            _destLowMsgs = _destHighMsgs - decrease;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "setDestLowMsgs",
                            "Cannot set destLowMsgs to be higher than destHighMsgs. New destLowMsgs="
                                            + _destLowMsgs);
        }
        else
        {
            _destLowMsgs = newDestLowMsgs;
        }

        // If we're logging or monitoring the high/low limits or message depth intervals we need
        // to set the MsgStore watermarks accordingly (510343)
        if (isThresholdNotificationRequired())
            updateWatermarks(getTotalMsgCount());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setDestLimits");
    }

    /**
     * This method may be implemented by subclasses of this class
     * (510343)
     * 
     * @return
     */
    protected boolean isThresholdNotificationRequired()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isThresholdNotificationRequired");

        boolean result = false;

        if (_isEventNotificationEnabled ||
            (mp.getCustomProperties().getOutputLinkThresholdEventsToLog() && destinationHandler.isLink()) ||
            (mp.getCustomProperties().getOutputDestinationThresholdEventsToLog() && !destinationHandler.isLink()))
            result = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isThresholdNotificationRequired", new Boolean(result));

        return result;
    }

    /**
     * If message depth intervals are configured, set the MsgStore watermarks
     * accordinly (510343)
     * 
     */
    public synchronized void setDestMsgInterval()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setDestMsgInterval");

        // See if we're issuing message depth thresholds for everything
        long newInterval = mp.getCustomProperties().getLogAllMessageDepthIntervals();
        if (newInterval < 0)
            newInterval = 0;

        // Check for a specific custom setting for this destination/foreign bus
        String destName = destinationHandler.getName();
        if (destinationHandler.isLink())
            destName = ((LinkHandler) destinationHandler).getBusName();

        Hashtable<String, Long> depthIntervals = mp.getCustomProperties().getLogMessageDepthIntervalsTable();

        if (depthIntervals.containsKey(destName))
            newInterval = (depthIntervals.get(destName)).longValue();

        if (newInterval != _destMsgInterval)
        {
            _destMsgInterval = newInterval;

            if (_destMsgInterval > 0)
            {
                // Get our current message depth so that we can work out the next up and down interval
                long currentDepth = getTotalMsgCount();
                _nextUpDepthInterval = ((currentDepth / _destMsgInterval) + 1) * _destMsgInterval;
                _nextDownDepthInterval = ((currentDepth - 1) / _destMsgInterval) * _destMsgInterval;
            }
            else
            {
                _nextUpDepthInterval = Long.MAX_VALUE;
                _nextDownDepthInterval = Long.MIN_VALUE;
            }

            // Set the MsgStore watermarks so that we find out when we cross either of these thresholds
            updateWatermarks(getTotalMsgCount());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setDestMsgInterval");
    }

    public abstract long getTotalMsgCount();

    /**
     * @return boolean true if this itemstream has reached destinationHighMsgs
     */
    public boolean isQHighLimit()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isQHighLimit");

        boolean limited = false;
        if (_destHighMsgs != -1)
            limited = (getTotalMsgCount() >= _destHighMsgs);

        if (limited)
        {
            // This will fire events and additionally, in the local ptop case will ensure 
            // that WLM advertisement is consistent with itemStream state 
            notifyClients();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isQHighLimit", Boolean.valueOf(limited));

        return limited;
    }

    /**
     * @return boolean true if this itemstream is below
     *         the destination Low limit. If this has not been set, we compliment
     *         the q_high limit
     */
    public boolean isQLowLimit()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isQLowLimit");

        boolean isUnderDestinationLowLimit = false;

        if (_destLowMsgs != -1)
        {
            isUnderDestinationLowLimit = (getTotalMsgCount() <= _destLowMsgs);
        }
        else
        {
            //no valid destination low messages has been set
            //therefore we use the q_Hi paramter to determine
            //whether the destination is full or not
            isUnderDestinationLowLimit = !isQHighLimit();
        }
        if (isUnderDestinationLowLimit)
        {
            // Ensure that WLM advertisement is consistent with itemStream state 
            notifyClients();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isQLowLimit", Boolean.valueOf(isUnderDestinationLowLimit));

        return isUnderDestinationLowLimit;
    }

    /**
     * @return boolean true if this itemstream has reached destinationHighMsgs
     */
    public boolean isQFull()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isQFull");

        boolean qFull = false;
        long totalMessages = getTotalMsgCount();

        if (_destHighMsgs != -1)
        {
            // The queue is clearly full if it has reached capacity     
            qFull = (totalMessages >= _destHighMsgs);
            if (!qFull)
            {
                // If the queue is below capacity, then we need to check whether the
                // wlmRemoved flag has been set. If it has then we need to verify that the
                // message depth has dropped below the low water mark before declaring the
                // queue not full.
                if (_destLowMsgs != -1)
                {
                    if (wlmRemoved)
                        qFull = !(totalMessages < _destLowMsgs);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isQFull", Boolean.valueOf(qFull));

        return qFull;
    }

    /**
     * <p>This method fires an appropriate event if Notification
     * eventing is enabled.
     * 
     * @return true if destination is advertised on WLM
     */
    public void notifyClients()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "notifyClients");

        // We'll only fire events if eventing is enabled or depths are being monitored
        // (510343)
        if (isThresholdNotificationRequired())
        {
            long totalItems = getTotalMsgCount();
            synchronized (this)
            {
                if (wlmRemoved && ((_destLowMsgs == -1) || (totalItems <= _destLowMsgs)))
                {
                    wlmRemoved = false;
                    // Fire event
                    fireDepthThresholdReachedEvent(getControlAdapter(),
                                                   false,
                                                   totalItems, _destLowMsgs); // Reached low
                }
                else if (!wlmRemoved
                         && (_destHighMsgs != -1)
                         && (_destLowMsgs != -1)
                         && (totalItems >= _destHighMsgs))
                {
                    wlmRemoved = true;
                    // Fire event
                    fireDepthThresholdReachedEvent(getControlAdapter(),
                                                   true,
                                                   totalItems, _destHighMsgs); // Reached High
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "notifyClients");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.ItemStream#getByteHighWaterMark()
     */
    @Override
    public long getByteHighWaterMark() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getByteHighWaterMark");
            SibTr.exit(tc, "getByteHighWaterMark");
        }
        return -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.ItemStream#getByteLowWaterMark()
     */
    @Override
    public long getByteLowWaterMark() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getByteLowWaterMark");
            SibTr.exit(tc, "getByteLowWaterMark");
        }
        return -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.ItemStream#getCountHighWaterMark()
     */
    @Override
    public long getCountHighWaterMark() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getCountHighWaterMark");

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getCountHighWaterMark", new Long(_nextHighWatermark));
        return _nextHighWatermark;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.ItemStream#getCountLowWaterMark()
     */
    @Override
    public long getCountLowWaterMark() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getCountLowWaterMark");

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getCountLowWaterMark", new Long(_nextLowWatermark));
        return _nextLowWatermark;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.ItemStream#eventWatermarkBreached()
     */
    @Override
    public synchronized void eventWatermarkBreached() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "eventWatermarkBreached");

        // Get the current message depth
        long currentDepth = getTotalMsgCount();

        // If we're monitoring the depth of the queue then we need to issue a message
        // informing that we've crossed an interval (if we have actually crissed one, and
        // we haven't just been notified by MsgStore due to a high/log breach) (510343)
        if (_destMsgInterval != 0)
        {
            // If we've exceeded the next depth interval we need to issue a message
            if (currentDepth >= _nextUpDepthInterval)
            {
                // Issue message
                issueDepthIntervalMessage(_nextUpDepthInterval);

                // Update the next depth thresholds
                _nextDownDepthInterval = _nextUpDepthInterval - _destMsgInterval;
                _nextUpDepthInterval = _nextUpDepthInterval + _destMsgInterval;
            }
            else if (currentDepth <= _nextDownDepthInterval)
            {
                // Issue message
                issueDepthIntervalMessage(_nextDownDepthInterval);

                _nextUpDepthInterval = _nextDownDepthInterval + _destMsgInterval;
                _nextDownDepthInterval = _nextDownDepthInterval - _destMsgInterval;
            }
        }

        // Tell anyone who's interested in this event
        notifyClients();

        // If we're monitoring the depth of the queue then we need set up the next
        // up/down watermarks in MsgStore (510343)
        updateWatermarks(currentDepth);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "eventWatermarkBreached");
    }

    /**
     * Issue an informational message informing that a depth interval has been crossed
     * (510343)
     * 
     * @param depth
     */
    protected void issueDepthIntervalMessage(long depth)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "issueDepthIntervalMessage", new Long(depth));

        // Destination {0} on messaging engine {1} has reached a depth of {2} messages
        SibTr.info(tc, "DESTINATION_DEPTH_INTERVAL_REACHED_CWSIP0787",
                   new Object[] { destinationHandler.getName(),
                                 mp.getMessagingEngineName(),
                                 new Long(depth) });

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "issueDepthIntervalMessage");
    }

    /**
     * Update the MsgStore watermarks so that we get informed the next time the message
     * depth crosses either of them (510343)
     * 
     * @param currentDepth
     * @throws NotInMessageStore
     */
    protected void updateWatermarks(long currentDepth)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "updateWatermarks", new Long(currentDepth));

        boolean setWatermarks = false;
        _nextHighWatermark = _nextUpDepthInterval;
        _nextLowWatermark = _nextDownDepthInterval;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "_destMsgInterval: " + _destMsgInterval +
                            " _destHighMsgs: " + _destHighMsgs +
                            " _destLowMsgs: " + _destLowMsgs +
                            " _nextUpDepthInterval: " + _nextUpDepthInterval +
                            " _nextDownDepthInterval: " + _nextDownDepthInterval);

        if (_destMsgInterval > 0)
            setWatermarks = true;

        if (isThresholdNotificationRequired())
        {
            setWatermarks = true;

            // If message depth interval reporting is enabled, work out from the
            // current depth what are the next interesting watermarks to breach
            // (an interval threshold or a high/low limit?)
            if (_destMsgInterval > 0)
            {
                if ((_nextHighWatermark > _destHighMsgs) &&
                    (currentDepth < _destHighMsgs))
                    _nextHighWatermark = _destHighMsgs;

                if ((_nextLowWatermark < _destLowMsgs) &&
                    (currentDepth > _destLowMsgs))
                    _nextLowWatermark = _destLowMsgs;
            }
            else
            {
                // If we're not reporting depth intervals we'll reset the watermarks to the
                // destination's high and low values.
                _nextHighWatermark = _destHighMsgs;
                _nextLowWatermark = _destLowMsgs;
            }
        }

        if (setWatermarks)
        {
            setWatermarks(_nextLowWatermark, _nextHighWatermark);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "updateWatermarks",
                       new Object[] { new Long(_nextLowWatermark), new Long(_nextHighWatermark) });
    }

    /**
     * Set the MsgStore watermarks so that we get informed the next time the message
     * depth crosses either of them (510343)
     * 
     * @param currentDepth
     * @throws SevereMessageStoreException
     * @throws NotInMessageStore
     */
    protected void setWatermarks(long nextLowWatermark, long nextHighWatermark)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setWatermarks",
                        new Object[] { new Long(nextLowWatermark), new Long(nextHighWatermark) });

        try
        {
            setWatermarks(nextLowWatermark, nextHighWatermark, -1, -1);
        } catch (MessageStoreException e) {
            // MessageStoreException shouldn't occur so FFDC.
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.itemstreams.BaseMessageItemStream.setWatermarks",
                                        "1:702:1.24",
                                        this);

            SibTr.exception(tc, e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setWatermarks");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#getControlAdapter()
     */
    @Override
    public ControlAdapter getControlAdapter()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getControlAdapter");
            SibTr.exit(tc, "getControlAdapter", controlAdapter);
        }
        return controlAdapter;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#deregisterControlAdapterMBean()
     */
    @Override
    public void deregisterControlAdapterMBean()
    {}

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#registerControlAdapterAsMBean()
     */
    @Override
    public void registerControlAdapterAsMBean()
    {}

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#dereferenceControlAdapter()
     */
    @Override
    public void dereferenceControlAdapter()
    {
        controlAdapter.dereferenceControllable();
        controlAdapter = null;
    }

    /**
     * Fire an event notification of type TYPE_SIB_MESSAGEPOINT_DEPTH_THRESHOLD_REACHED
     * 
     * @param newState
     */
    public void fireDepthThresholdReachedEvent(ControlAdapter cAdapter,
                                               boolean reachedHigh,
                                               long numMsgs, long msgLimit)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "fireDepthThresholdReachedEvent",
                        new Object[] { cAdapter, new Boolean(reachedHigh), new Long(numMsgs) });

        // Retrieve appropriate information
        String destinationName = destinationHandler.getName();

        String meName = mp.getMessagingEngineName();

        // If we've been told to output the event message to the log, do it...
        if (mp.getCustomProperties().getOutputDestinationThresholdEventsToLog())
        {
            if (reachedHigh)
                SibTr.info(tc, "NOTIFY_DEPTH_THRESHOLD_REACHED_CWSIP0553",
                           new Object[] { destinationName, meName, msgLimit });
            else
                SibTr.info(tc, "NOTIFY_DEPTH_THRESHOLD_REACHED_CWSIP0554",
                           new Object[] { destinationName, meName, msgLimit });
        }

        // If we're actually issuing events, do that too...
        if (_isEventNotificationEnabled)
        {
            if (cAdapter != null)
            {
                // Build the message for the Notification
                String message = null;
                if (reachedHigh)
                    message = nls.getFormattedMessage("NOTIFY_DEPTH_THRESHOLD_REACHED_CWSIP0553",
                                                      new Object[] { destinationName,
                                                                    meName, msgLimit },
                                                      null);
                else
                    message = nls.getFormattedMessage("NOTIFY_DEPTH_THRESHOLD_REACHED_CWSIP0554",
                                                      new Object[] { destinationName,
                                                                    meName, msgLimit },
                                                      null);

                // Build the properties for the Notification
                Properties props = new Properties();

                props.put(SibNotificationConstants.KEY_DESTINATION_NAME, destinationName);
                props.put(SibNotificationConstants.KEY_DESTINATION_UUID, destinationHandler.getUuid().toString());

                if (reachedHigh)
                    props.put(SibNotificationConstants.KEY_DEPTH_THRESHOLD_REACHED,
                              SibNotificationConstants.DEPTH_THRESHOLD_REACHED_HIGH);
                else
                    props.put(SibNotificationConstants.KEY_DEPTH_THRESHOLD_REACHED,
                              SibNotificationConstants.DEPTH_THRESHOLD_REACHED_LOW);

                // Number of Messages
                props.put(SibNotificationConstants.KEY_MESSAGES, String.valueOf(numMsgs));
                // Now create the Event object to pass to the control adapter
                MPRuntimeEvent MPevent =
                                new MPRuntimeEvent(SibNotificationConstants.TYPE_SIB_MESSAGEPOINT_DEPTH_THRESHOLD_REACHED,
                                                message,
                                                props);
                // Fire the event
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "fireDepthThresholdReachedEvent", "Drive runtimeEventOccurred against Control adapter: " + cAdapter);

                cAdapter.runtimeEventOccurred(MPevent);
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "fireDepthThresholdReachedEvent", "Control adapter is null, cannot fire event");
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "fireDepthThresholdReachedEvent");
    }

    /**
     * @return The BaseDestinationHandler for this item stream
     * @author tpm
     */
    public abstract BaseDestinationHandler getDestinationHandler();
}
