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
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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

    // The set of Expirables maintained in expiry order, soonest to latest.
    private final ExpiryIndex expiryIndex = new ExpiryIndex();

    // The alarm which will perform the next ExpiryIndex scan and its read write lock.
    private Alarm expiryAlarm = null;
    // Only allow the alarms to be added and scheduled once the Expirer has been started.
    private final AtomicBoolean started = new AtomicBoolean(false);
    // Lock to protect writing the expiryAlarm and the started state.
    private final ReentrantReadWriteLock expiryAlarmLock = new ReentrantReadWriteLock();

    private long interval = 0; // The interval at which the alarm will pop
    private byte consecutiveFailures = 0;

    private final MessageStoreImpl messageStore;
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
     * @param messageStore the MessageStore
     */
    public Expirer(MessageStoreImpl messageStore) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", messageStore);

        this.messageStore = messageStore;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
    }

    /**
     * Include a WeakReference to an Expirable in the set of pending Expirables.
     *
     * @param expirable the Expirable to be added to the set of Expirables.
     * @throws SevereMessageStoreException if the Expirable is already added.
     */
    public final void addExpirable(Expirable expirable) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "addExpirable",
                       "objId="
                       + (expirable == null ? "null" : String.valueOf(expirable.expirableGetID()))
                       + " started="+started);
        }

        // Ignore this entry if the expirer has ended or the given entry is null
        // or the referenced item has already gone from the message store.
        if (expirable != null && started.get() && expirable.expirableIsInStore()){
            ExpirableReference expirableRef = new ExpirableReference(expirable);

            if (expiryIndex.put(expirableRef)) {
                scheduleAlarm(interval);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    SibTr.debug(tc, "Added: expiryTime=" + expirableRef.getExpiryTime() + ", objectId=" + expirableRef.getID());
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    SibTr.debug(tc, "Duplicate expirable: ExpirtTime=" + expirableRef.getExpiryTime() + ", obectId=" + expirableRef.getID());
                }

                stop();
                Object[] o = { expirableRef.getExpiryTime() + " : " + expirableRef.getID() };
                SevereMessageStoreException severeMessageStoreException = new SevereMessageStoreException("DUPLICATE_EXPIRABLE_SIMS2000", o);
                lastException = severeMessageStoreException;
                lastExceptionTime = System.currentTimeMillis();
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "addExpirable");
                throw severeMessageStoreException;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addExpirable");
    }

    /**
     * Used by the cache to determine whether the expirer has been started.
     * @return true if the expirer has been started but not yet stopped.
     */
    public final boolean isRunning() {
        return started.get();
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
                        + " started="+ started);
        }

        boolean removed = false;
        if (expirable != null) {
            ExpirableReference expirableRef = new ExpirableReference(expirable);
            removed = expiryIndex.remove(expirableRef);
            // If the expiryIndex has become empty, the alarm will not be refreshed after the next scan.
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "removeExpirable", "removed=" + removed);
        return removed;
    }

    /**
     * Return the size of the expiry index.
     * @return the number of references in the expiry index.
     */
    public final int size() {
        return expiryIndex.size();
    }

    /**
     * Start the expiry daemon.
     * @param expiryInterval An interval in milliseconds which may be set via
     * a custom property and if zero or more will be used to override the
     * default expiry interval which was set when the Expirer was created.
     * @throws SevereMessageStoreException
     */
    public final void start(long expiryInterval, JsMessagingEngine jsMessagingEngine) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "start", "expityInterval=" + expiryInterval +" jsMessagingEngine="+jsMessagingEngine+ " indexSize=" + expiryIndex.size());

        messagingEngine = jsMessagingEngine;

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
                lastExceptionTime = System.currentTimeMillis();
                SibTr.debug(this, tc, "start", "Unable to parse property: " + e);
                this.interval = 1000; // Use hard coded default as last resort
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "start", "expiryInterval=" + this.interval);


            if (interval < 1) {
                stop();
            } else {
                expiryAlarmLock.writeLock().lock();
                try {
                    if (started.get()) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Expiry already started");

                        SevereMessageStoreException e = new SevereMessageStoreException("EXPIRY_THREAD_ALREADY_RUNNING_SIMS2004");
                        lastException = e;
                        lastExceptionTime = System.currentTimeMillis();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "start");
                        throw e;

                    } else {
                        started.set(true);
                        expirerStartTime = System.currentTimeMillis();
                        // Process any pending Expirables.
                        if (!expiryIndex.isEmpty())
                            scheduleAlarm(interval);
                    }

                } finally {
                    expiryAlarmLock.writeLock().unlock();
                }

            }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start", "started=" + started + " interval=" + interval);
    }

    /**
     * Stop the expiry daemon.
     * Cancel any existing alarm, even if the Expirer is already stopped.
     * We do not purge the ExpiryIndex of any pending ExpirableReferences.
     */
    public final void stop()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "stop", "started="+started+" expiryAlarm="+expiryAlarm);

        expiryAlarmLock.writeLock().lock();
        try {
            started.set(false);
            if (expiryAlarm != null ) {
                expiryAlarm.cancel();
                expiryAlarm = null;
            }

        } finally {
            expiryAlarmLock.writeLock().unlock();
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
    @Override
    public final void alarm(Object obj) {
        if (messagingEngine != null) SibTr.push(messagingEngine);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "alarm", "expiryIndex.isEmpty()=" + expiryIndex.isEmpty());

        if (!started.get()) {
            // The Expirer was stopped, after the alarm was scheduled, but before it was cancelled.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "alarm", " started="+ started + expiryIndex.size());
            if (messagingEngine != null) SibTr.pop();
            return;
        }

        // A new Expirable added after now, may not be processed as part of this scan will result in a new scan.

        LocalTransaction transaction = null;
        int indexUsed = 0;
        long processed = 0;
        long expired = 0;
        long cleaned = 0;
        long gone = 0;
        long remain = 0;

        try {
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

            try {
                // Get the first entry from the index and start the cycle
                ExpirableReference expirableRef = expiryIndex.first();
                while (expirableRef.getExpiryTime() <= startTime) {
                    processed++;

                    Expirable expirable = expirableRef.get();
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
                        if (expirable.expirableExpire((PersistentTransaction)transaction)) {
                            boolean removed = expiryIndex.remove(expirableRef);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc,"Removed (expired) removed="+removed+" ExpiryTime="+ expirableRef.getExpiryTime()+ " objectId="+ expirableRef.getID());
                            expired++;

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

                    } else {
                        boolean removed = expiryIndex.remove(expirableRef);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc,"Removed (gone) removed="+removed+" ExpiryTime="+ expirableRef.getExpiryTime()+ " objectId="+ expirableRef.getID());
                        if(removed) gone++;

                    }
                    expirableRef = expiryIndex.first();
                }
            } catch (NoSuchElementException noSuchElementException) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "No more ExpirableReferences, processed="+processed+" expired="+expired+" gone="+gone);
            }

            // Once every 'n' cycles, continue to the end of the expiry index
            // removing any entries for items which are no longer in the store.
            if (cleanupDeletedItems)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Expiry cleanup in progress");
                cleaned = expiryIndex.clean();

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
            lastExceptionTime = System.currentTimeMillis();

            if (consecutiveFailures > MAX_CONSECUTIVE_FAILURES)
            {
                stop();

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "alarm", e);
                throw new MessageStoreRuntimeException("EXPIRY_DAEMON_TERMINATED_SIMS2001", new Object[] { e}, e);
            }

        } finally {
            long currIndexSize = 0;
            // Refresh the alarm if a the ExpiryIndex still contains potential work.
            expiryAlarmLock.writeLock().lock();
            try {
                if (started.get() && !expiryIndex.isEmpty()) {
                    expiryAlarm = AlarmManager.createNonDeferrable(interval, this);
                    currIndexSize = expiryIndex.size();
                } else {
                    expiryAlarm = null;
                }
                    
            } finally {
                expiryAlarmLock.writeLock().unlock();
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

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "alarm", " processed="+processed+" gone="+ gone+ " expired="+ expired+ " remain="+ remain+ " cleaned="+ cleaned+ " expiry="+ expiryIndex.size()+ " cleanupLimit="+ cleanupLimit);

        if (messagingEngine != null) SibTr.pop();
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
     * Schedule the next alarm.
     * Callers of this method would typically hold lockObject already.
     * @param timeOut the interval in milliseconds before the expirableIndex scan is performed.
     */
    private void scheduleAlarm(long timeOut) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "scheduleAlarm", "timeOut=" + timeOut+ " started="+started+ " expiryAlarm="+expiryAlarm);

        // NB PM27294 You cannot decrease the the timeOut if an alarm is already scheduled.
        // This is OK for the expirer as the timeout does not change once Expirer is started.

        expiryAlarmLock.readLock().lock();
        // If there is no alarm already scheduled create a new alarm.
        if (started.get() && expiryAlarm == null) {
            // Upgrade to write lock.
            expiryAlarmLock.readLock().unlock();
            expiryAlarmLock.writeLock().lock();
            try {
                if (started.get() && expiryAlarm == null) {
                    expiryAlarm = AlarmManager.createNonDeferrable(timeOut, this);
                }
            } finally {
                expiryAlarmLock.writeLock().unlock(); // Unlock write, still hold read
            }
        } else {
            expiryAlarmLock.readLock().unlock();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "scheduleAlarm","started=" + started + " expiryAlarm=" + expiryAlarm);
    }

    /**
     * Dump the XML representation of the expirer.
     * @param writer The formatted writer to which the dump should be directed.
     * @throws IOException
     */
    public void xmlWriteOn(FormattedWriter writer) throws IOException
    {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd @ HH:mm:ss.SSS");
        writer.newLine();
        writer.startTag(XML_EXPIRER);
        writer.indent();
        writer.newLine();
        writer.taggedValue("timeNow", dateFormat.format(new Date()));
        writer.newLine();
        writer.taggedValue("interval", interval);
        writer.newLine();
        writer.taggedValue("started", started);
        writer.newLine();
        writer.taggedValue("cleanupFlag", Boolean.valueOf(cleanupDeletedItems));
        writer.newLine();
        writer.taggedValue("expirerStartTime", dateFormat.format(new Date(expirerStartTime)));
        writer.newLine();
        writer.taggedValue("expirerStopTime", dateFormat.format(new Date(expirerStopTime)));
        writer.newLine();
        writer.taggedValue("nextLogIndex", diagIndex);
        writer.newLine();

        for (int i = 0; i < MAX_DIAG_LOG; i++) {
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
