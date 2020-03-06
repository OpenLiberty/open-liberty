package com.ibm.ws.sib.msgstore.expiry;
/*******************************************************************************
 * Copyright (c) 2012, 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import com.ibm.ejs.util.am.Alarm;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.ejs.util.am.AlarmManager;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.MessageStoreRuntimeException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.XmlConstants;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;

import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import java.io.IOException;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;


/**
 * Provides an expiry daemon (the Expirer) which maintains an index of
 * expirable items and handles their demise. The Expirer periodically scans
 * the expiry index for items which have passed their sell-by date. The appropriate
 * callbacks are then made to the associated item to trigger its deletion. 
 */
public class Expirer implements AlarmListener,  XmlConstants 
{
    private static TraceComponent tc = SibTr.register(Expirer.class, 
                                                      MessageStoreConstants.MSG_GROUP, 
                                                      MessageStoreConstants.MSG_BUNDLE);

    private final static int BATCH_SIZE = 20;
    private final static int CLEANUP_EVERY_N_CYCLES = 5;
    private final static int MAX_DIAG_LOG = 30;
    private final static int MAX_CONSECUTIVE_FAILURES = 3;

    // Defect 560281.1
    // Use an inner class specific to this class for locking.
    private final static class ExpirerLock {}

    private final ExpirerLock lockObject = new ExpirerLock();
    
    // all these updated and read (if required) while holding lockObject
    private ExpiryIndex expiryIndex = null;
    private Alarm expiryAlarm = null; // The alarm which triggers an index scan
    private boolean runEnabled = false; // Allow the alarms to be scheduled
    private boolean addEnabled = true; // Allow expirables to be added to the index
    private boolean alarmScheduled = false;
    private boolean alarming = false;
    
    private long interval = 0; // The interval at which the alarm will pop
    private byte consecutiveFailures = 0;
    
    private MessageStoreImpl messageStore = null;
    private JsMessagingEngine messagingEngine = null;
    // Following are for XML dump
    private Exception lastException = null;
    private long lastExceptionTime = 0;
    private long expirerStartTime = 0;
    private long expirerStopTime = 0;
    private long startTime = 0;
    private boolean cleanupDeletedItems = false;

    // Info for diagnostic dump
    private int diagIndex = 0;
    private long alarmTime[] = new long[MAX_DIAG_LOG];
    private long logIndexSize[] = new long[MAX_DIAG_LOG];
    private long logProcessed[] = new long[MAX_DIAG_LOG];
    private long logExpired[] = new long[MAX_DIAG_LOG];
    private long logRemain[] = new long[MAX_DIAG_LOG];
    private long logGone[] = new long[MAX_DIAG_LOG];
    private long logCleaned[] = new long[MAX_DIAG_LOG];

    private int countForCleanup = 0;
    private int batchCount = 0;
    private int cleanupLimit = CLEANUP_EVERY_N_CYCLES;

    /**
     * Constructor
     * @param ms the MessageStore  
     */
    public Expirer(MessageStoreImpl ms)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");

        messageStore = ms;
        expiryIndex = new ExpiryIndex();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>", this);
    }

    /**
     * Add an Expirable reference for an item to the expiry index. The reference will be 
     * added to the index in order of expiry time (which must be set within the Expirable).
     * Once added to the index, it will become eligible for expiry processing at the 
     * designated time.
     * 
     * @param expirable the Expirable item for which a reference is to be added to the expiry index.
     * 
     * @return true if the reference was added to the index, false otherwise. False may
     * be returned if the item has not yet been added to an item stream and therefore does not 
     * have a unique ID. False may also be returned if the expirer has not been started.
     * @throws SevereMessageStoreException 
     */ 
    public final boolean addExpirable(Expirable expirable) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "addExpirable",
                       "objId="
                       + (expirable == null ? "null" : String.valueOf(expirable.expirableGetID()))
                       + " addEnabled="
                       + addEnabled);
        }

        boolean reply = false;

        // Ignore this entry if the expirer has ended or the given entry is null
        if (addEnabled && expirable != null)
        {
            long expiryTime = expirable.expirableGetExpiryTime();

            ExpirableReference expirableRef = new ExpirableReference(expirable);
            expirableRef.setExpiryTime(expiryTime);

            // Ignore this entry if the referenced item has already gone from the message store. 
            if (expirable.expirableIsInStore())
            {
                synchronized (lockObject)
                {
                    // Add the expirable to the expiry index
                    reply = expiryIndex.put(expirableRef);
                    if (reply)
                    {
                        boolean scheduled = false;
                        if (runEnabled && expiryIndex.size() == 1) // We just added the first entry
                        {
                            scheduleAlarm(interval);
                            scheduled = true;
                        }
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        {
                            SibTr.debug(tc, "Added: ET="+expirableRef.getExpiryTime()+", objId="+expirableRef.getID()+", scheduled="+scheduled);
                        }
                    }
                    else
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        {
                            SibTr.debug(tc, "Duplicate expirable: ET="+expirableRef.getExpiryTime()+", objId="+expirableRef.getID());
                        }
                        runEnabled = false; // End the expiry daemon thread
                        Object[] o = { expirableRef.getExpiryTime() + " : " + expirableRef.getID()};
                        SevereMessageStoreException e = new SevereMessageStoreException("DUPLICATE_EXPIRABLE_SIMS2000", o);
                        lastException = e;
                        lastExceptionTime = timeNow();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "addExpirable");
                        throw e;
                    }
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addExpirable", "reply=" + reply);
        return reply;
    }

    /**
     * Used by the cache to determine whether the expirer has been started.
     * @return true if the expirer has been started but not yet stopped.
     */
    public final boolean isRunning()
    {
        return runEnabled;
    }

    /**
     * Remove an Expirable reference for an item from the expiry index. 
     * 
     * @param expirable the Expirable item for which a reference is to be removed from the expiry index.
     * @return true if the reference was removed from the index, false otherwise.
     * @throws SevereMessageStoreException 
     */
    public final boolean removeExpirable(Expirable expirable) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "removeExpirable",
                        "objId="
                        + (expirable == null ? "null" : String.valueOf(expirable.expirableGetID()))
                        + " ET="
                        + (expirable == null ? "null" : String.valueOf(expirable.expirableGetExpiryTime()))
                        + " addEnabled="
                        + addEnabled);
        }

        boolean reply = false;
        boolean cancelled = false;
        // Ignore this request if the expirer has ended or the given entry is null
        synchronized (lockObject)
        {
            if (addEnabled && expirable != null)
            {
                long expiryTime = expirable.expirableGetExpiryTime();
                ExpirableReference expirableRef = new ExpirableReference(expirable);
                expirableRef.setExpiryTime(expiryTime);
                // Remove the expirable from the expiry index
                reply = expiryIndex.remove(expirableRef);
                if (reply && expiryIndex.size() <= 0) // We just removed the last entry
                {
                    if (expiryAlarm != null)
                    {
                        expiryAlarm.cancel();
                        alarmScheduled = false;
                        cancelled = true;
                    }
                }

            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "removeExpirable", "reply=" + reply + " cancelled=" + cancelled);
        return reply;
    }

    /**
     * Return the size of the expiry index.
     * @return the number of references in the expiry index. 
     */
    public final int size()
    {
        int size = expiryIndex.size();

        return size;
    }

    /**
     * Start the expiry daemon.
     * @param expiryInterval An interval in milliseconds which may be set via
     * a custom property and if zero or more will be used to override the
     * default expiry interval which was set when the Expirer was created.
     * @throws SevereMessageStoreException 
     */
    public final void start(long expiryInterval, JsMessagingEngine jsme) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "start", "interval=" + expiryInterval + " indexSize=" + expiryIndex.size());

        messagingEngine = jsme;

        if (expiryInterval >= 0) // If an expiry interval was given, use it
        {
            interval = expiryInterval;
        }
        else // Otherwise, get it from the system property
        {
            // Get property for expiry interval
            String value =
            messageStore.getProperty(
                                    MessageStoreConstants.PROP_EXPIRY_INTERVAL,
                                    MessageStoreConstants.PROP_EXPIRY_INTERVAL_DEFAULT);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "start", "Value from property=<" + value + ">");

            try
            {
                this.interval = Long.parseLong(value.trim());
            }
            catch (NumberFormatException e)
            {
                // No FFDC Code Needed.
                lastException = e;
                lastExceptionTime = timeNow();
                SibTr.debug(this, tc, "start", "Unable to parse property: " + e);
                this.interval = 1000; // Use hard coded default as last resort
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "start", "expiryInterval=" + this.interval);

        // about to tinker with various variables so take the lock now
        synchronized (lockObject)
        {
            if (interval < 1)
            {
                runEnabled = false; 
                addEnabled = false;
            }
            else
            {
                if (expiryAlarm == null)
                {
                    runEnabled = true;
                    addEnabled = true;
                    expirerStartTime = timeNow();
                    // Now we look at the size of the index and only schedule the first
                    // alarm if the index is not empty. Remember that expirables can be
                    // added BEFORE the expirer is started so it may not be empty.
                    if (expiryIndex.size() > 0) // If the index is not empty,
                    {
                        scheduleAlarm(interval); // ... schedule the first alarm.  
                    }
                }
                else
                {
                    // Expiry thread already running
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Expiry already started");

                    SevereMessageStoreException e = new SevereMessageStoreException("EXPIRY_THREAD_ALREADY_RUNNING_SIMS2004");
                    lastException = e;
                    lastExceptionTime = timeNow();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "start");
                    throw e;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start", "runEnabled=" + runEnabled + " addEnabled=" + addEnabled + " interval=" + interval);
    }

    /**
     * Stop the expiry daemon. 
     */
    public final void stop()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "stop");

        synchronized (lockObject)
        {
            addEnabled = false; // Prevent further expirables being added
            if (runEnabled)
            {
                runEnabled = false; // This should terminate expiry
                expirerStopTime = timeNow();
            }
            if (expiryAlarm != null) // Cancel any outstanding alarm
            {
                expiryAlarm.cancel();
                expiryAlarm = null;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "stop");
    }

    /**
     * Process the expiry index.
     * 
     * This will process the expiry index in sequence of expiry time up to the 
     * current time. Any entries which have passed their expiry time will be called
     * to expire. If they return true, their references will be removed from the index, 
     * otherwise their references will remain and be processed again on subsequent cycles.
     * 
     * This method is triggered by the AlarmManager. On completion, it schedules the
     * next alarm, unless stop() has been called or the index is empty.
     * @throws SevereMessageStoreException 
     */
    public final void alarm(Object obj)
    {
        if (messagingEngine != null) SibTr.push(messagingEngine);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "alarm", "Expiry=" + expiryIndex.size());
        
        synchronized (lockObject)
        {
            // we try ensure there is only ever 1 alarm thread running and check at the end of each alarm 
            // if another needs scheduling.  3 consecutive checked exceptions are tolerated.
            alarmScheduled = false;
            
            while (alarming)
            {
                // this is only now possible with a highly unlikely thread scheduling, but cope with it
                // anyway.
                try
                {
                    lockObject.wait();
                }
                catch (InterruptedException e)
                {
                    // No FFDC Code Needed
                }
            }
            
            alarming = true;
        }

        LocalTransaction transaction = null;
        int indexUsed = 0;
        long processed = 0;
        long expired = 0;
        long cleaned = 0;
        long gone = 0;
        long remain = 0;

        try
        {
            // Once every 5 cycles, set the flag to initiate full-index scan
            // for items which are no longer in the store.
            countForCleanup++;
            if (countForCleanup >= cleanupLimit)
            {
                countForCleanup = 0;
                cleanupDeletedItems = true;
            }
            else
            {
                cleanupDeletedItems = false;
            }

            // Get current system time for start of cycle
            startTime = System.currentTimeMillis();
            indexUsed = saveStartTime(startTime);

            // Point to the start of the tree                  
            expiryIndex.resetIterator();

            // Get the first entry from the index and start the cycle
            ExpirableReference expirableRef = expiryIndex.next();
            while (runEnabled && expirableRef != null && expirableRef.getExpiryTime() <= startTime)
            {
                processed++;

                Expirable expirable = (Expirable) expirableRef.get();
                // If the weak reference is null or if the item indicates that it has
                // already gone from the store, then remove the expirable ref from the index 
                if (expirable != null && expirable.expirableIsInStore())
                {
                    // Now processing items which are either at or past their expiry time.
                    // Create a transaction if we don't have one already
                    if (transaction == null)
                    {
                        transaction = messageStore.getTransactionFactory().createLocalTransaction();
                    }
                    // Tell the expirable to expire. If it returns true, then remove it from the expiry index.
                    if (expirable.expirableExpire((PersistentTransaction)transaction))
                    {
                        expired++;
                        // Remove from expiry index
                        remove(expirableRef, true);
                        batchCount++;
                        if (batchCount >= BATCH_SIZE)
                        {
                            transaction.commit();
                            transaction = null;
                            batchCount = 0;
                        }
                    }
                    else
                    {
                        remain++;
                    }
                }
                else
                {
                    if (remove(expirableRef, false))
                    {
                        gone++;
                    }
                }
                expirableRef = expiryIndex.next();
            }

            // Once every 'n' cycles, continue to the end of the expiry index
            // removing any entries for items which are no longer in the store.
            if (cleanupDeletedItems)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Expiry cleanup in progress");

                while (runEnabled && expirableRef != null)
                {
                    Expirable expirable = (Expirable) expirableRef.get();
                    // If the weak reference is null or if the item indicates that it has
                    // gone from the store, then remove the expirable ref from the index 
                    if (expirable == null || !(expirable.expirableIsInStore()))
                    {
                        if (remove(expirableRef, false))
                        {
                            cleaned++;
                        }
                    }
                    expirableRef = expiryIndex.next();
                }
                // Check how much useful work we did, and vary the frequency 
                // of the cleanup cycle. 199808.1 
                if (cleaned < 10 && cleanupLimit < 100)
                {
                    cleanupLimit += CLEANUP_EVERY_N_CYCLES; // Slow down
                }
                else if (cleaned > 1000 && cleanupLimit > CLEANUP_EVERY_N_CYCLES)
                {
                    cleanupLimit = CLEANUP_EVERY_N_CYCLES; // Restore default
                }
                else if (cleaned > 100 && cleanupLimit > CLEANUP_EVERY_N_CYCLES)
                {
                    cleanupLimit -= CLEANUP_EVERY_N_CYCLES; // Speed up
                }
            }

            if (transaction != null) // Did we create a transaction?
            {
                if (batchCount == 0) // No work outstanding  
                {
                    transaction.rollback();
                }
                else // Work outstanding, must be committed  
                {
                    transaction.commit();
                    batchCount = 0;
                }
                transaction = null;
            }
            consecutiveFailures = 0;
        } // end try
        catch (Exception e)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.expiry.Expirer.run", "441", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "alarm", "Expiry thread exception: " + e);

            consecutiveFailures++;
            lastException = e;
            lastExceptionTime = timeNow();
            
            if (consecutiveFailures > MAX_CONSECUTIVE_FAILURES)
            {
                synchronized (lockObject)
                {
                    runEnabled = false;
                    addEnabled = false;    
                }
                
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "alarm", e);
                throw new MessageStoreRuntimeException("EXPIRY_DAEMON_TERMINATED_SIMS2001", new Object[] { e}, e);
            }
        } 
        finally
        {
            long currIndexSize = 0;
            // Synchronize while we decide whether to schedule another alarm or not
            synchronized (lockObject)
            {
                alarming = false;
                currIndexSize = expiryIndex.size();
                
                if (runEnabled) // We have not been told to stop,
                {
                    if (currIndexSize > 0) // and the index is not empty, 
                    {
                        scheduleAlarm(interval); // ... schedule the next alarm
                    }
                }
                else // Expirer is stopping
                {
                    // Disallow addition of any further expirables to the index.
                    addEnabled = false;
                }
                lockObject.notifyAll();
            }

        

            // Save statistics for diagnostic dump using the same log index as
            // returned by saveStartTime() called at start of this cycle.
            logIndexSize[indexUsed] = currIndexSize;
            logProcessed[indexUsed] = processed;
            logExpired[indexUsed] = expired;
            logGone[indexUsed] = gone;
            logRemain[indexUsed] = remain;
            logCleaned[indexUsed] = cleaned;

        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "alarm", " processed="+processed+" gone="+ gone+ " expired="+ expired+ " remain="+ remain+ " cleaned="+ cleaned+ " expiry="+ expiryIndex.size()+ " cl="+ cleanupLimit+ " alarmScheduled="+ alarmScheduled);

        if (messagingEngine != null) SibTr.pop();
    } // end run()

    /** 
     * Remove the expirable reference from the expiry index. This will remove the 
     * current entry pointed-to by the iterator.
     * 
     * @param expirableRef our reference to the expirable
     * @param expired true if the item is being removed after expiry, false if being
     *        removed because it is no longer in store. Used for diagnostics only.
     * 
     * @return true if the object was successfully removed from the index.
     */
    private final boolean remove(ExpirableReference expirableRef, boolean expired)
    {
        boolean reply = expiryIndex.remove();

        if (reply)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc,"Removed ("+ (expired ? "expired" : "gone")+ ")"+ " ET="+ expirableRef.getExpiryTime()+ " objId="+ expirableRef.getID());
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Unable to remove from index: " + " ET=" + expirableRef.getExpiryTime() + " objId=" + expirableRef.getID());
        }
        return reply;
    }

    /**
     * Keep last n expiry cycle start times for diagnostic dump.
     * @param time the time this cycle started.
     * @return the log index used for this entry.
     */
    private int saveStartTime(long time)
    {
        int indexUsed = diagIndex;
        alarmTime[diagIndex++] = time;
        if (diagIndex >= MAX_DIAG_LOG)
        {
            diagIndex = 0;
        }
        return indexUsed;
    }

    /**
     * Schedule the next alarm.  Callers of this method would typically hold lockObject already.
     * @param timeOut the interval for the alarm.
     */
    private void scheduleAlarm(long timeOut)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "scheduleAlarm", "timeOut=" + timeOut);
        
        // NB PM27294 implementation now means you cannot decrease the the timeOut if an alarm is already scheduled.
        // This is OK for the expirer as the timeout does not change once Expirer is started.
        synchronized (lockObject)
        {
            // if there is not an alarm already scheduled and there exists no thread that will go on to call scheduleAlarm then
            // create an alarm.
            if (! alarmScheduled && ! alarming)
            {
                expiryAlarm = AlarmManager.createNonDeferrable(timeOut, this);
                alarmScheduled = true;
            }
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "scheduleAlarm", alarmScheduled);
    }

    /**
     * Provide the current system time.
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
    public void xmlWriteOn(FormattedWriter writer) throws IOException 
    {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd @ HH:mm:ss.SSS");
        String timeNow = dateFormat.format(new Date());
        writer.newLine(); 
        writer.startTag(XML_EXPIRER);
        writer.indent();
        writer.newLine();
        writer.taggedValue("timeNow", timeNow);
        writer.newLine();
        writer.taggedValue("interval", interval);
        writer.newLine();
        writer.taggedValue("addEnabled", Boolean.valueOf(addEnabled));
        writer.newLine();
        writer.taggedValue("runEnabled", Boolean.valueOf(runEnabled));
        writer.newLine();
        writer.taggedValue("cleanupFlag", Boolean.valueOf(cleanupDeletedItems));
        writer.newLine();
        writer.taggedValue("expirerStartTime", dateFormat.format(new Date(expirerStartTime)));
        writer.newLine();
        writer.taggedValue("expirerStopTime", dateFormat.format(new Date(expirerStopTime)));
        writer.newLine();
        writer.taggedValue("nextLogIndex", diagIndex);
        writer.newLine();

        for (int i = 0; i < MAX_DIAG_LOG; i++)
        {
            String str =
            "Cycle="
            + i
            + (diagIndex == i ? ":*" : ": ")
            + dateFormat.format(new Date(alarmTime[i]))
            + " size="
            + logIndexSize[i]
            + " processed="
            + logProcessed[i]
            + " expired="
            + logExpired[i]
            + " remain="
            + logRemain[i]
            + " gone="
            + logGone[i]
            + " cleaned="
            + logCleaned[i];
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
        writer.endTag(XML_EXPIRER);
    }
} // end Expirer
