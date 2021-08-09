package com.ibm.ws.sib.msgstore.deliverydelay;

/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.ibm.ejs.util.am.Alarm;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.ejs.util.am.AlarmManager;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.MessageStoreRuntimeException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.XmlConstants;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Provides an DeliveryDelayManagerLock daemon (the DeliveryDelayManager) which maintains an index of
 * DeliveryDelayable items and handles their unlocking. The DeliveryDelayManager periodically scans
 * the DeliveryDelay index for items which have passed their sell-by date. The appropriate
 * callbacks are then made to the associated item to trigger its unlocking.
 */
public class DeliveryDelayManager implements AlarmListener, XmlConstants
{
    private static TraceComponent tc = SibTr.register(DeliveryDelayManager.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    private final static int BATCH_SIZE = 20;
    private final static int CLEANUP_EVERY_N_CYCLES = 5;
    private final static int MAX_DIAG_LOG = 30;
    private final static int MAX_CONSECUTIVE_FAILURES = 3;

    // Defect 560281.1
    // Use an inner class specific to this class for locking.
    private final static class DeliveryDelayManagerLock {}

    private final DeliveryDelayManagerLock lockObject = new DeliveryDelayManagerLock();

    // all these updated and read (if required) while holding lockObject
    private DeliveryDelayIndex deliveryDelayIndex = null;
    private Alarm deliveryDelayAlarm = null; // The alarm which triggers an index scan
    private volatile boolean runEnabled = false; // Allow the alarms to be scheduled
    private volatile boolean addEnabled = true; // Allow DeliveryDelayables to be added to the index
    private volatile boolean alarmScheduled = false;
    private volatile boolean alarming = false;

    private long interval = 0; // The interval at which the alarm will pop
    private byte consecutiveFailures = 0;

    private MessageStoreImpl messageStore = null;
    private JsMessagingEngine messagingEngine = null;
    // Following are for XML dump
    private Exception lastException = null;
    private long lastExceptionTime = 0;
    private long deliveryDelayManagerStartTime = 0;
    private long deliveryDelayManagerStopTime = 0;
    private long maximumTime = 0;
    private boolean cleanupDeletedItems = false;

    // Info for diagnostic dump
    private int diagIndex = 0;
    private final long alarmTime[] = new long[MAX_DIAG_LOG];
    private final long logIndexSize[] = new long[MAX_DIAG_LOG];
    private final long logProcessed[] = new long[MAX_DIAG_LOG];
    private final long logUnlocked[] = new long[MAX_DIAG_LOG];
    private final long logRemain[] = new long[MAX_DIAG_LOG];
    private final long logGone[] = new long[MAX_DIAG_LOG];
    private final long logCleaned[] = new long[MAX_DIAG_LOG];

    private int countForCleanup = 0;
    private int batchCount = 0;
    private int cleanupLimit = CLEANUP_EVERY_N_CYCLES;

    /**
     * Constructor
     * 
     * @param ms the MessageStore
     */
    public DeliveryDelayManager(MessageStoreImpl ms)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>");

        messageStore = ms;
        deliveryDelayIndex = new DeliveryDelayIndex();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>", this);
    }

    /**
     * Add an DeliveryDelayable reference for an item to the deliveryDelay index. The reference will be
     * added to the index in order of delivery delay time (which must be set within the DeliveryDelayable).
     * Once added to the index, it will become eligible for unlock processing at the
     * designated time.
     * 
     * @param deliveryDelayable the DeliveryDelayable item for which a reference is to be added to the deliveryDelay index.
     * 
     * @return true if the reference was added to the index, false otherwise. False may
     *         be returned if the item has not yet been added to an item stream and therefore does not
     *         have a unique ID. False may also be returned if the DeliveryDelayManager has not been started.
     * @throws SevereMessageStoreException
     */
    public final boolean addDeliveryDelayable(DeliveryDelayable deliveryDelayable) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "addDeliveryDelayable",
                        "objId="
                                        + (deliveryDelayable == null ? "null" : String.valueOf(deliveryDelayable.deliveryDelayableGetID()))
                                        + " addEnabled="
                                        + addEnabled);
        }

        boolean reply = false;

        // Ignore this entry if the deliveryDelayManager has ended or the given entry is null
        if (addEnabled && deliveryDelayable != null)
        {
            long deliveryDelayTime = deliveryDelayable.deliveryDelayableGetDeliveryDelayTime();

            DeliveryDelayableReference delayedDeliverableRef = new DeliveryDelayableReference(deliveryDelayable);
            delayedDeliverableRef.setDeliveryDelayTime(deliveryDelayTime);

            // Ignore this entry if the referenced item has already gone from the message store. 
            if (deliveryDelayable.deliveryDelayableIsInStore())
            {
                synchronized (lockObject)
                {
                    // Add the deliveryDelayable to the deliveryDelay index
                    reply = deliveryDelayIndex.put(delayedDeliverableRef);
                    if (reply)
                    {
                        boolean scheduled = false;

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            SibTr.debug(this, tc, "Before scheduleAlarm", "deliveryDelayIndexSize=" + deliveryDelayIndex.size() + " runEnabled" + runEnabled);
                        }

                        if (runEnabled && deliveryDelayIndex.size() == 1) // We just added the first entry
                        {
                            scheduleAlarm(interval);
                            scheduled = true;
                        }
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        {
                            SibTr.debug(tc, "Added: DDT=" + delayedDeliverableRef.getDeliveryDelayTime() + ", objId=" + delayedDeliverableRef.getID() + ", scheduled=" + scheduled);
                        }
                    }
                    else
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        {
                            SibTr.debug(tc, "Duplicate deliveyDelayable: DDT=" + delayedDeliverableRef.getDeliveryDelayTime() + ", objId=" + delayedDeliverableRef.getID());
                        }
                        runEnabled = false; // End the DeliveryDelayManagerLock daemon thread
                        Object[] o = { delayedDeliverableRef.getDeliveryDelayTime() + " : " + delayedDeliverableRef.getID() };
                        SevereMessageStoreException e = new SevereMessageStoreException("DUPLICATE_DELIVERYDELAYABLE_SIMS2010", o);
                        lastException = e;
                        lastExceptionTime = timeNow();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "addDeliveryDelayable");
                        throw e;
                    }
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "addDeliveryDelayable", "reply=" + reply);
        return reply;
    }

    /**
     * Used by the cache to determine whether the DeliveryDelayManager has been started.
     * 
     * @return true if the DeliveryDelayManager has been started but not yet stopped.
     */
    public final boolean isRunning()
    {
        return runEnabled;
    }

    /**
     * Remove an DeliveryDelayable reference for an item from the deliveryDelay index.
     * 
     * @param deliveryDelayable the DeliveryDelayable item for which a reference is to be removed from the deliveryDelay index.
     * @return true if the reference was removed from the index, false otherwise.
     * @throws SevereMessageStoreException
     */
    public final boolean removeDeliveryDelayable(DeliveryDelayable deliveryDelayable) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "removeDeliveryDelayable",
                        "objId="
                                        + (deliveryDelayable == null ? "null" : String.valueOf(deliveryDelayable.deliveryDelayableGetID()))
                                        + " ET="
                                        + (deliveryDelayable == null ? "null" : String.valueOf(deliveryDelayable.deliveryDelayableGetDeliveryDelayTime()))
                                        + " addEnabled="
                                        + addEnabled);
        }

        boolean reply = false;
        boolean cancelled = false;
        // Ignore this request if the DeliveryDelayManager has ended or the given entry is null
        synchronized (lockObject)
        {
            if (addEnabled && deliveryDelayable != null)
            {
                long deliveryDelay = deliveryDelayable.deliveryDelayableGetDeliveryDelayTime();
                DeliveryDelayableReference delayedDeliverableRef = new DeliveryDelayableReference(deliveryDelayable);
                delayedDeliverableRef.setDeliveryDelayTime(deliveryDelay);
                // Remove the DeliveryDelayable from the deliveryDelay index
                reply = deliveryDelayIndex.remove(delayedDeliverableRef);
                if (reply && deliveryDelayIndex.size() <= 0) // We just removed the last entry
                {
                    if (deliveryDelayAlarm != null)
                    {
                        deliveryDelayAlarm.cancel();
                        alarmScheduled = false;
                        cancelled = true;
                    }
                }

            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeDeliveryDelayable", "deliveryDelayIndexSize=" + deliveryDelayIndex.size() + " reply=" + reply + " cancelled=" + cancelled);
        return reply;
    }

    /**
     * Return the size of the deliverydelay index.
     * 
     * @return the number of references in the deliverydelay index.
     */
    public final int size()
    {
        int size = deliveryDelayIndex.size();

        return size;
    }

    /**
     * Start the DeliveryDelayManager daemon.
     * 
     * @param deliveryDelayScanInterval An interval in milliseconds which may be set via
     *            a custom property and if zero or more will be used to override the
     *            default deliverydelayscan interval which was set when the DeliveryDelayManager was created.
     * @throws SevereMessageStoreException
     */
    public final void start(long deliveryDelayScanInterval, JsMessagingEngine jsme) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "start", "interval=" + deliveryDelayScanInterval + " indexSize=" + deliveryDelayIndex.size());

        messagingEngine = jsme;
        
        if (deliveryDelayScanInterval >= 0) // If an deliverydelayscan interval was given, use it
        {
            interval = deliveryDelayScanInterval;
        }
        else // Otherwise, get it from the system property
        {
            // Get property for deliverydelayscan interval
            String value =
                            messageStore.getProperty(
                                                     MessageStoreConstants.PROP_DELIVERY_DELAY_SCAN_INTERVAL,
                                                     MessageStoreConstants.PROP_DELIVERY_DELAY_SCAN_INTERVAL_DEFAULT);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "start", "Value from property=<" + value + ">");

            try
            {
                this.interval = Long.parseLong(value.trim());
            } catch (NumberFormatException e)
            {
                // No FFDC Code Needed.
                lastException = e;
                lastExceptionTime = timeNow();
                SibTr.debug(this, tc, "start", "Unable to parse property: " + e);
                this.interval = 1000; // Use hard coded default as last resort
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "start", "deliveryDelayScanInterval=" + this.interval);

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
                if (deliveryDelayAlarm == null)
                {           
                	scanForInvalidDeliveryDelay();
                	
                    runEnabled = true;
                    addEnabled = true;
                    deliveryDelayManagerStartTime = timeNow();
                    // Now we look at the size of the index and only schedule the first
                    // alarm if the index is not empty. Remember that deliveryDelayables can be
                    // added BEFORE the delivery delay manager is started so it may not be empty.
                    if (deliveryDelayIndex.size() > 0) // If the index is not empty,
                    {
                        scheduleAlarm(interval); // ... schedule the first alarm.  
                    }
                }
                else
                {
                    // DeliveryDelayManager thread already running
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "DeliveryDelayManager already started");

                    SevereMessageStoreException e = new SevereMessageStoreException("DELIVERYDELAYMANAGER_THREAD_ALREADY_RUNNING_SIMS2012");
                    lastException = e;
                    lastExceptionTime = timeNow();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "start");
                    throw e;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "start", "runEnabled=" + runEnabled + " addEnabled=" + addEnabled + " interval=" + interval);
    }

    /**
     * Scan the delivery delay intervals loaded at startup to determine 
     * if any may have been migrated incorrectly.
     * 
     * Caller must hold a lock on lockObject.
     *
     * @throws SevereMessageStoreException
     */
    private void scanForInvalidDeliveryDelay() throws SevereMessageStoreException {
    	final String methodName = "scanForInvalidDeliveryDelay";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, methodName);


        String str = messageStore.getProperty(MessageStoreConstants.PROP_MAXIMUM_ALLOWED_DELIVERY_DELAY_INTERVAL, MessageStoreConstants.PROP_MAXIMUM_ALLOWED_DELIVERY_DELAY_INTERVAL_DEFAULT);
        if (str.equals(MessageStoreConstants.PROP_MAXIMUM_ALLOWED_DELIVERY_DELAY_INTERVAL_DEFAULT)) {
        	 if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                 SibTr.exit(this, tc, methodName, "No scan");	
        	 return;
        }
        try {
            final long maximumAllowedDeliveryDelayInterval = Long.parseLong(str);
            maximumTime = System.currentTimeMillis() + maximumAllowedDeliveryDelayInterval;

        } catch (NumberFormatException exception) {
            // No FFDC Code Needed.
            lastException = exception;
            lastExceptionTime = timeNow();
            SibTr.debug(this, tc, methodName, "Unable to parse property: " + exception);
            //TODO Output a warning message, no scan.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, methodName, "No scan, exception="+exception);	
       	    return;
        }
        
        MessageStoreConstants.MaximumAllowedDeliveryDelayAction action; 
        try {
        	String actionStr = messageStore.getProperty(MessageStoreConstants.PROP_MAXIMUM_ALLOWED_DELIVERY_DELAY_ACTION, MessageStoreConstants.PROP_MAXIMUM_ALLOWED_DELIVERY_DELAY_ACTION_DEFAULT);        
            action = MessageStoreConstants.MaximumAllowedDeliveryDelayAction.valueOf(actionStr);

        } catch (IllegalArgumentException illegalArgumentException) {
        	 lastException = illegalArgumentException;
             lastExceptionTime = timeNow();
             SibTr.debug(this, tc, methodName, "Unable to parse property: " + illegalArgumentException);
             action = MessageStoreConstants.MaximumAllowedDeliveryDelayAction.warn;
        }       
        
        // Point to the start of the deliveryDelayIndex tree.                  
        deliveryDelayIndex.resetIterator();
        DeliveryDelayableReference deliveryDelayableRef = deliveryDelayIndex.next();
        try {
        	while (deliveryDelayableRef != null && deliveryDelayableRef.getDeliveryDelayTime() > maximumTime) {

        		DeliveryDelayable deliveryDelayable = (DeliveryDelayable) deliveryDelayableRef.get();
        		 SibTr.debug(this, tc, methodName, "deliveryDelayable="+deliveryDelayable+" deliveryDelayable.deliveryDelayableIsInStore()="+deliveryDelayable.deliveryDelayableIsInStore());	
        		// The soft reference should not be null during startup when the scan is done. 
        		if (deliveryDelayable != null && deliveryDelayable.deliveryDelayableIsInStore()) {
        			boolean unlocked = deliveryDelayable.handleInvalidDeliveryDelayable(action);
                    if (unlocked)
    			        remove(deliveryDelayableRef, true);   			
        		}
        		deliveryDelayableRef = deliveryDelayIndex.next();
        	}
        	
        } catch (MessageStoreException | SIException exception) {

            SevereMessageStoreException severeMessageStoreException = new SevereMessageStoreException(exception);
            lastException = exception;
            lastExceptionTime = timeNow();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, methodName, severeMessageStoreException);
            throw severeMessageStoreException;
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, methodName);		
	}

	/**
     * Stop the DeliveryDelayManager daemon.
     */
    public final void stop()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "stop");

        synchronized (lockObject)
        {
            addEnabled = false; // Prevent further DeliveryDelayables being added
            if (runEnabled)
            {
                runEnabled = false; // This should terminate DeliveryDelayManager
                deliveryDelayManagerStopTime = timeNow();
            }
            if (deliveryDelayAlarm != null) // Cancel any outstanding alarm
            {
                deliveryDelayAlarm.cancel();
                deliveryDelayAlarm = null;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "stop");
    }

    /**
     * Process the delivery delay index.
     * 
     * This will process the delivery delay index in sequence of delivery delay time up to the
     * current time. Any entries which have passed their delivery delay time will be called
     * to unlock itself. If they return true, their references will be removed from the index,
     * otherwise their references will remain and be processed again on subsequent cycles.
     * 
     * This method is triggered by the AlarmManager. On completion, it schedules the
     * next alarm, unless stop() has been called or the index is empty.
     * 
     * @throws SevereMessageStoreException
     */
    @Override
    public final void alarm(Object obj)
    {
        if (messagingEngine != null)
            SibTr.push(messagingEngine);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "alarm", "deliveryDelayIndexSize=" + deliveryDelayIndex.size());

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
                } catch (InterruptedException e)
                {
                    // No FFDC Code Needed
                }
            }

            alarming = true;
        }

        LocalTransaction transaction = null;
        int indexUsed = 0;
        long processed = 0;
        long unlocked = 0;
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
            maximumTime = System.currentTimeMillis();
            indexUsed = saveStartTime(maximumTime);

            // Get the first entry from the index and start the cycle
            DeliveryDelayableReference deliveryDelayableRef = null;
            synchronized (lockObject)
            {
                // Point to the start of the tree                  
                deliveryDelayIndex.resetIterator();
                deliveryDelayableRef = deliveryDelayIndex.next();
            }
            while (runEnabled && deliveryDelayableRef != null && deliveryDelayableRef.getDeliveryDelayTime() <= maximumTime)
            {
                processed++;

                DeliveryDelayable deliveryDelayable = (DeliveryDelayable) deliveryDelayableRef.get();
                // If the soft reference is null or if the item indicates that it has
                // already gone from the store, then remove the DeliveryDelayable ref from the index 
                if (deliveryDelayable != null && deliveryDelayable.deliveryDelayableIsInStore())
                {
                    // Now processing items which are either at or past their delivery delay time.
                    // Create a transaction if we don't have one already
                    if (transaction == null)
                    {
                        transaction = messageStore.getTransactionFactory().createLocalTransaction();
                    }
                    // Tell the DeliveryDelayable to unlock. If it returns true, then remove it from the DeliveryDelay index.
                    if (deliveryDelayable.deliveryDelayableUnlock((PersistentTransaction) transaction, AbstractItem.DELIVERY_DELAY_LOCK_ID))
                    {
                        unlocked++;
                        // Remove from DeliveryDelay index
                        remove(deliveryDelayableRef, true);
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
                    if (remove(deliveryDelayableRef, false))
                    {
                        gone++;
                    }
                }
                synchronized (lockObject)
                {
                    deliveryDelayableRef = deliveryDelayIndex.next();
                }
            }

            // Once every 'n' cycles, continue to the end of the DeliveryDelay index
            // removing any entries for items which are no longer in the store.
            if (cleanupDeletedItems)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "DeliveryDelayManager cleanup in progress");

                while (runEnabled && deliveryDelayableRef != null)
                {
                    DeliveryDelayable deliveryDelayable = (DeliveryDelayable) deliveryDelayableRef.get();
                    // If the soft reference is null or if the item indicates that it has
                    // gone from the store, then remove the DeliveryDelayable ref from the index 
                    if (deliveryDelayable == null || !(deliveryDelayable.deliveryDelayableIsInStore()))
                    {
                        if (remove(deliveryDelayableRef, false))
                        {
                            cleaned++;
                        }
                    }
                    synchronized (lockObject)
                    {
                        deliveryDelayableRef = deliveryDelayIndex.next();
                    }
                }
                // Check how much useful work we did, and vary the frequency 
                // of the cleanup cycle. 
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
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.deliverydelay.DeliveryDelayManager.alarm", "550", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.event(this, tc, "alarm", "DeliveryDelayManager thread exception: " + e);

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

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "alarm", e);
                throw new MessageStoreRuntimeException("DELIVERYDELAYMANAGER_DAEMON_TERMINATED_SIMS2011", new Object[] { e }, e);
            }
        } finally
        {
            long currIndexSize = 0;
            // Synchronize while we decide whether to schedule another alarm or not
            synchronized (lockObject)
            {
                alarming = false;
                currIndexSize = deliveryDelayIndex.size();

                if (runEnabled) // We have not been told to stop,
                {
                    if (currIndexSize > 0) // and the index is not empty, 
                    {
                        scheduleAlarm(interval); // ... schedule the next alarm
                    }
                }
                else // DeliveryDelayManager is stopping
                {
                    // Disallow addition of any further DeliveryDelayable to the index.
                    addEnabled = false;
                }
                lockObject.notifyAll();
            }

            // Save statistics for diagnostic dump using the same log index as
            // returned by saveStartTime() called at start of this cycle.
            logIndexSize[indexUsed] = currIndexSize;
            logProcessed[indexUsed] = processed;
            logUnlocked[indexUsed] = unlocked;
            logGone[indexUsed] = gone;
            logRemain[indexUsed] = remain;
            logCleaned[indexUsed] = cleaned;

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc,
                       "alarm",
                       " processed=" + processed + " gone=" + gone + " unlocked=" + unlocked + " remain=" + remain + " cleaned=" + cleaned + " DeliveryDelayIndexSize="
                                       + deliveryDelayIndex.size()
                                       + " cleanupLimit="
                                       + cleanupLimit + " alarmScheduled=" + alarmScheduled);

        if (messagingEngine != null)
            SibTr.pop();
    } // end run()

    /**
     * Remove the DeliveryDelayable reference from the DeliveryDelay index. This will remove the
     * current entry pointed-to by the iterator.
     * 
     * @param deliveryDelayableReference our reference to the DeliveryDelayable
     * @param unlocked true if the item is being removed after delivery delay time, false if being
     *            removed because it is no longer in store. Used for diagnostics only.
     * 
     * @return true if the object was successfully removed from the index.
     */
    private final boolean remove(DeliveryDelayableReference deliveryDelayableReference, boolean unlocked)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "remove",
                        " deliveryDelayableReference=" + deliveryDelayableReference + " unlocked=" +
                                        unlocked + " deliveryDelayIndex=" + deliveryDelayIndex.size());

        boolean reply = false;
        // synchronize on the lockObject
        synchronized (lockObject)
        {
            reply = deliveryDelayIndex.remove();
        }
        if (reply)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Removed (" + (unlocked ? "unlocked" : "gone") + ")" + " DDT=" + deliveryDelayableReference.getDeliveryDelayTime() + " objId="
                                + deliveryDelayableReference.getID() + " DeliveryDelayIndexSize=" + deliveryDelayIndex.size());
        }
        else
        {
            // can happen if the element is already deleted
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Did not remove from index: " + " DDT=" + deliveryDelayableReference.getDeliveryDelayTime() + " objId=" + deliveryDelayableReference.getID());
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "remove", "deliveryDelayIndex=" + deliveryDelayIndex.size() + " reply=" + reply);

        return reply;
    }

    /**
     * Keep last n delivery delay manager cycle start times for diagnostic dump.
     * 
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
     * Schedule the next alarm. Callers of this method would typically hold lockObject already.
     * 
     * @param timeOut the interval for the alarm.
     */
    private void scheduleAlarm(long timeOut)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "scheduleAlarm", "timeOut=" + timeOut);

        // NB PM27294 implementation now means you cannot decrease the the timeOut if an alarm is already scheduled.
        // This is OK for the DeliveryDelayManager as the timeout does not change once DeliveryDelayManager is started.
        synchronized (lockObject)
        {
            // if there is not an alarm already scheduled and there exists no thread that will go on to call scheduleAlarm then
            // create an alarm.
            if (!alarmScheduled && !alarming)
            {
                deliveryDelayAlarm = AlarmManager.createNonDeferrable(timeOut, this);
                alarmScheduled = true;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "scheduleAlarm", alarmScheduled);
    }

    /**
     * Provide the current system time.
     * 
     * @return the time
     */
    public static long timeNow()
    {
        return System.currentTimeMillis();
    }

    /**
     * Dump the XML representation of the DeliveryDelayManager.
     * 
     * @param writer The formatted writer to which the dump should be directed.
     * @throws IOException
     */
    public void xmlWriteOn(FormattedWriter writer) throws IOException
    {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd @ HH:mm:ss.SSS");
        String timeNow = dateFormat.format(new Date());
        writer.newLine();
        writer.startTag(XML_DELIVERYDELAYMANAGER);
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
        writer.taggedValue("deliveryDelayManagerStartTime", dateFormat.format(new Date(deliveryDelayManagerStartTime)));
        writer.newLine();
        writer.taggedValue("deliveryDelayManagerStopTime", dateFormat.format(new Date(deliveryDelayManagerStopTime)));
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
                                            + " unlocked="
                                            + logUnlocked[i]
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
        writer.endTag(XML_DELIVERYDELAYMANAGER);
    }
} // end DeliveryDelayManager
