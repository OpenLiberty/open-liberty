package com.ibm.ws.sib.msgstore.expiry;
/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.util.Iterator;
import java.util.Set;
import java.io.IOException;
import java.util.Date;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.util.am.Alarm;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.ejs.util.am.AlarmManager;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;
import com.ibm.ws.sib.msgstore.cache.links.LinkOwner;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.MessageStoreRuntimeException;
import com.ibm.ws.sib.msgstore.XmlConstants;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.msgstore.persistence.PersistentMessageStore;
import com.ibm.ws.sib.msgstore.PersistenceException;

import com.ibm.ws.sib.utils.ras.SibTr;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * This class runs asynchronously inside the MessageStore and identifies 
 * streams which contain items which are expirable; that is, those items
 * which have non-zero expiry times. The streams which contain these items 
 * are then reloaded. Once this has happened, then the Cache Expirer will
 * handle their expiry. This object exists to handle restart of the Message
 * Store, which would otherwise be unaware of any expirable items which were 
 * persisted during a previous invocation (of the Message Store).
 * 
 * The CacheLoader makes an initial call to the Persistence layer
 * to obtain the IDs of any streams containing expirable items.
 * The CacheLoader then runs periodically (by default once every 60
 * seconds) and works through the list of streams. On each cycle, it
 * reloads a number of streams (maximum 10 per cycle) and then sleeps
 * until the next cycle. When all the streams in the set have been 
 * reloaded, the CacheLoader terminates itself. It will not run again
 * until the next MessageStore restart. 
 */
public class CacheLoader implements AlarmListener, XmlConstants
{
    private static TraceComponent tc = SibTr.register(CacheLoader.class, 
                                                      MessageStoreConstants.MSG_GROUP, 
                                                      MessageStoreConstants.MSG_BUNDLE);

    private final static int MAX_DIAG_LOG = 10;

    private Alarm loaderAlarm = null; // The alarm which triggers a reload cycle 
    private long interval = 0; // The interval at which the alarm will fire
    private boolean enabled = false; // Keep the CacheLoader alive and ready to scan
    private boolean shutdown = false; // Used for a silent shutdown interrupt

    private MessageStoreImpl messageStore = null;
    // Defect 560281.1
    // Use an inner class specific to this class for locking.
    private final static class AlarmLock {}

    private final AlarmLock alarmLock = new AlarmLock();

    private JsMessagingEngine messagingEngine = null;

    private Set results = null;
    private Iterator iter = null;

    // Following are for the XML dump
    private long loaderStartTime = 0;
    private long loaderStopTime = 0;
    private Throwable lastException = null;
    private long lastExceptionTime = 0;
    private int maxStreamsPerCycle = 0;
    private long totalStreams = 0;

    //Info for diagnostic dump
    private int diagIndex = 0;
    private long cycleTime[] = new long[MAX_DIAG_LOG];
    private long logStreamsLoaded[] = new long[MAX_DIAG_LOG];
    private long logDuration[] = new long[MAX_DIAG_LOG];

    /**
     * Constructor
     * @param ms the MessageStore in which the CacheLoader is running.
     */
    public CacheLoader(MessageStoreImpl ms)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", ms);

        messageStore = ms;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>", this);
    }

    /**
     * Start the CacheLoader daemon.
     * @param loaderInterval A time in seconds which will be used as the
     * interval between cycles. If a negative number is given (the 
     * default case), then the interval is picked up from a custom 
     * property.
     * @param jsme the Messaging Engine instance
     * @throws MessageStoreRuntimeException 
     */
    public final void start(long loaderInterval, JsMessagingEngine jsme) throws MessageStoreRuntimeException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "start", Long.valueOf(loaderInterval));

        messagingEngine = jsme;

        if (loaderInterval >= 0) // Use the given interval
        {
            interval = loaderInterval * 1000;
        }
        else
        {
            // Get property for cache loader interval
            String value = messageStore.getProperty(MessageStoreConstants.PROP_CACHELOADER_INTERVAL,
                                                    MessageStoreConstants.PROP_CACHELOADER_INTERVAL_DEFAULT);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                SibTr.debug(this, tc, "start", "Interval from system prop=<" + value + ">");
            }
            try
            {
                this.interval = Long.parseLong(value.trim()) * 1000;
            }
            catch (NumberFormatException e)
            {
                //No FFDC Code Needed.
                lastException = e;
                lastExceptionTime = timeNow();
                SibTr.debug(this, tc, "start", "Unable to parse cacheLoaderInterval property: " + e);
                this.interval = 60000; // Use hard coded default as last resort
            }
        }
        // Get value of maxStreamsPerCycle from property
        String value = messageStore.getProperty(MessageStoreConstants.PROP_MAX_STREAMS_PER_CYCLE,
                                                MessageStoreConstants.PROP_MAX_STREAMS_PER_CYCLE_DEFAULT);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            SibTr.debug(this, tc, "start", "maxStreamsPerCycle from system prop=<" + value + ">");
        }
        try
        {
            maxStreamsPerCycle = Integer.parseInt(value.trim());
        }
        catch (NumberFormatException e)
        {
            //No FFDC Code Needed.
            lastException = e;
            lastExceptionTime = timeNow();
            SibTr.debug(this, tc, "start", "Unable to parse maxStreamsPerCycle property: " + e);
            maxStreamsPerCycle = 10; // Use hard coded default as last resort
        }

        PersistentMessageStore pm = messageStore.getPersistentMessageStore();
        // Get a set of IDs which represent streams containing expirable items.
        try
        {
            results = pm.identifyStreamsWithExpirableItems();
            totalStreams = results.size();
            iter = results.iterator();

            // Now, schedule the first alarm
            if (interval < 1)
            {
                enabled = false;
            }
            else
            {
                if (loaderAlarm == null)
                {
                    enabled = true;
                    shutdown = false;
                    loaderStartTime = timeNow();
                    loaderAlarm = scheduleAlarm(interval);
                }
            }
        }
        catch (PersistenceException pe)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(pe,"com.ibm.ws.sib.msgstore.expiry.CacheLoader.run","191",this);
            lastException = pe;
            lastExceptionTime = timeNow();
            enabled = false;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                SibTr.debug(this, tc, "run", "CacheLoader stopping - interrupted: " + pe);
            }
            throw new MessageStoreRuntimeException("CACHE_LOADER_TERMINATED_SIMS2003", new Object[] { pe}, pe);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start", "enabled=" + enabled + " interval=" + interval);
    }

    /**
     * Stop the CacheLoader daemon. 
     */
    public final void stop()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "stop");

        synchronized (alarmLock)
        {
            if (enabled)
            {
                loaderStopTime = timeNow();
                enabled = false;
                shutdown = true;
            }
            if (loaderAlarm != null)
            {
                loaderAlarm.cancel();
                loaderAlarm = null;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "stop");
    }

    /**
     * Run when fired by the alarm manager.
     * @throws MessageStoreRuntimeException 
     */
    public final void alarm(Object obj)
    {
        if (messagingEngine != null) SibTr.push(messagingEngine);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "alarm");

        int indexUsed = 0;
        long streamsLoaded = 0;
        long startTime = 0;
        String str = "";

        synchronized (alarmLock)
        {
            try
            {
                if (enabled)
                {
                    // Perform a cycle 
                    startTime = timeNow();
                    indexUsed = saveStartTime(startTime);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "CacheLoader cycle starting");
                    streamsLoaded = 0;

                    while (enabled && iter.hasNext() && streamsLoaded < maxStreamsPerCycle)
                    {
                        // Get next ID from result set
                        long key = ((Long) iter.next()).longValue();

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Reloading stream ID=" + key);

                        // Find the link for this ID
                        AbstractItemLink link = messageStore.getLink(key);
                        // Request the link to reload its items. It will reply true if
                        // it reloads any items and false if it does not.
                        if (link != null)
                        {
                            boolean loaded = ((LinkOwner) link).loadOwnedLinks();
                            if (loaded)
                            {
                                streamsLoaded++;
                            }
                        }
                        else
                        {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "ID returned from persistence gave null link?");
                        }
                        // No more work is done here, but now that the expired items have
                        // been reloaded into memory, the cache expirer will handle them.
                    }

                    // Log diagnostic info for this cycle
                    long duration = timeNow() - startTime;
                    logDuration[indexUsed] = duration;
                    logStreamsLoaded[indexUsed] = streamsLoaded;

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "End of CacheLoader cycle. " + streamsLoaded + " stream(s) loaded in " + duration + "ms");
                }
            } // end try
            catch (Exception e)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.expiry.CacheLoader.run","301",this);

                lastException = e;
                lastExceptionTime = timeNow();
                enabled = false;
                if (!shutdown || !(e instanceof InterruptedException))
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    {
                        SibTr.debug(this, tc, "run", "CacheLoader stopping - interrupted: " + e);
                    }
                    throw new MessageStoreRuntimeException("CACHE_LOADER_TERMINATED_SIMS2003", new Object[] { e}, e);
                }
            }

            // Decide what to do next, end or continue?
            if (enabled)
            {
                if (iter.hasNext()) // Setting up for another cycle
                {
                    loaderAlarm = scheduleAlarm(interval);
                    str = "More";
                }
                else // Stopping, no more work to do
                {
                    loaderAlarm = null;
                    loaderStopTime = timeNow();
                    str = "Finished";
                }
            }
            else // Stopping on request from caller to stop()
            {
                loaderAlarm = null;
                loaderStopTime = timeNow();
                str = "Ended";
            }

        } // end synch

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "alarm", str);

        if (messagingEngine != null) SibTr.pop();
    } // end run()

    /** 
      * Keep last n cycle start times for diagnostic dump.
      * @param time the time this cycle started.
      * @return the log index used for this entry.
      */
    private int saveStartTime(long time)
    {
        int indexUsed = diagIndex;
        cycleTime[diagIndex++] = time;
        if (diagIndex >= MAX_DIAG_LOG)
        {
            diagIndex = 0;
        }
        return indexUsed;
    }

    /**
     * Schedule the next alarm.
     * @param timeOut the interval for the alarm.
     */
    private Alarm scheduleAlarm(long timeOut)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "scheduleAlarm", "timeOut=" + timeOut);

        Alarm alarm = AlarmManager.createNonDeferrable(timeOut, this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "scheduleAlarm");
        return alarm;
    }

    /**
     * Return the current system time.
     * @return the time
     */
    public static long timeNow()
    {
        return System.currentTimeMillis();
    }

    /** 
     * Dump the XML representation of the expirer.
     * @param writer The formatted writer to which the dump should be directed.
     * @throws IOException
     */
    public void xmlWriteOn(FormattedWriter writer) throws IOException {

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd @ HH:mm:ss.SSS");
        String timeNow = dateFormat.format(new Date());
        writer.newLine();
        writer.startTag(XML_CACHE_LOADER);
        writer.indent();

        writer.newLine();
        writer.taggedValue("timeNow", timeNow);
        writer.newLine();
        writer.taggedValue("interval", interval);
        writer.newLine();
        writer.taggedValue("enabled", Boolean.valueOf(enabled));
        writer.newLine();
        writer.taggedValue("maxStreamsPerCycle", maxStreamsPerCycle);
        writer.newLine();
        writer.taggedValue("loaderStartTime", dateFormat.format(new Date(loaderStartTime)));
        writer.newLine();
        writer.taggedValue("loaderStopTime", dateFormat.format(new Date(loaderStopTime)));
        writer.newLine();
        writer.taggedValue("totalStreams", totalStreams);
        writer.newLine();

        for (int i = 0; i < MAX_DIAG_LOG; i++)
        {
            String str =
            "Cycle="
            + i
            + (diagIndex == i ? ":*" : ": ")
            + dateFormat.format(new Date(cycleTime[i]))
            + " streamsLoaded="
            + logStreamsLoaded[i]
            + " duration="
            + logDuration[i];
            writer.taggedValue("info", str);
            writer.newLine();
        }

        writer.startTag(XML_STORED_EXCEPTION);
        if (lastException == null)
        {
            writer.write("No exceptions recorded");
        }
        else
        {
            writer.indent();
            writer.newLine();
            writer.taggedValue("time", new Date(lastExceptionTime));
            writer.outdent();
            writer.write(lastException);
            writer.newLine();
        }
        writer.endTag(XML_STORED_EXCEPTION);

        writer.outdent();
        writer.newLine();
        writer.endTag(XML_CACHE_LOADER);
    }
} // end Cache Loader
