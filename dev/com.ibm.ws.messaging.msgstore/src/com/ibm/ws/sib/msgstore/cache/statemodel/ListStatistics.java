package com.ibm.ws.sib.msgstore.cache.statemodel;
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
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.Statistics;
import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;
import com.ibm.ws.sib.msgstore.cache.links.LinkOwner;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class provides a convenient place to collect statistics. 
 * We can have one for a whole collection of sublists. 
 * 
 * As far as I know, the increments and decrements are only called from
 * the state model.
 */
public final class ListStatistics implements Statistics 
{
    private static TraceComponent tc = SibTr.register(ListStatistics.class, 
                                                      MessageStoreConstants.MSG_GROUP, 
                                                      MessageStoreConstants.MSG_BUNDLE);

    private static final int MOVING_AVERAGE_LENGTH = 20;

    private long _countAdding = 0;
    private long _countAvailable = 0;
    private long _countExpiring = 0;
    private long _countLocked = 0;
    private long _countRemoving = 0;
    private long _countTotal = 0;
    private long _countTotalBytes = 0;
    private long _countUpdating = 0;

    private final int _movingAverageHighLimit;
    private final int _movingAverageLowLimit;
    private long _movingTotal = 0;

    private final LinkOwner _owningStreamLink;

    // Defect 484799
    private final long _totalSizeHighLimit;
    private final long _totalSizeLowLimit;
    private boolean _spilling = false;

    private long _watermarkBytesHigh = 0;
    private long _watermarkBytesLow = 0;

    private long _watermarkCountHigh = 0;
    private long _watermarkCountLow = 0;

    /*
     * Note: I do not like polluting the listStatistic class with messageStoreImpl, 
     * but hopefully we will find out soon how much of this functionality
     * is actually needed.
     * 
     * @param instrumentation
     * @param messageStore
     */
    public ListStatistics(LinkOwner owner) 
    {
        super();
        _owningStreamLink = owner;
        MessageStoreImpl messageStore = ((AbstractItemLink) _owningStreamLink).getMessageStoreImpl();

        _movingAverageHighLimit = messageStore.getSpillUpperLimit();
        _movingAverageLowLimit = messageStore.getSpillLowerLimit();

        // Defect 484799
        _totalSizeHighLimit = messageStore.getSpillUpperSizeLimit();
        _totalSizeLowLimit  = messageStore.getSpillLowerSizeLimit();
    }

    // Defect 484799
    /**
     * Instead of just triggering spilling when we have a certain number of
     * Items on a stream we now have the ability to trigger spilling if the
     * total size of the Items on a stream goes over a pre-defined limit.
     * This should allow us to control the memory usage of a stream in a
     * more intuitive fashion.
     * 
     * For the count limit we use a moving average as this allows us to 
     * flatten out and short term spikes in the number of items on the 
     * stream. This allows us to pop over the item count for a short while 
     * without triggering spilling and slowing item consumption even more.
     * 
     * For the size limit we do not use a moving average as this would
     * flatten off any spikes in the memory usage of the queue. In the 
     * size case we need to pay attention to any spikes as they could 
     * quickly blow the heap if allowed to build up.
     * 
     * i.e. a moving average of 20
     * 
     * 19 x 1k message + 1 x 100MB message = 20 messages
     * 
     * Moving average totals        |  Moving average
     * ------------------------------------------------
     * 1K                           |  
     * 1K + 2k                      |
     * 1K + 2K + 3K                 |
     * .                            |
     * .                            |
     * 1K + 2K + .... + 19K + 100MB |  105,052,160 / 20 = 5,252,608
     * 
     * So a queue with over 100MB of message data on it would only produce 
     * an average of just over 5MB. This inconsistency makes it difficult 
     * to control the actual amount of memory being used and also for the
     * user to judge the correct values for the limits.
     */
    public final void checkSpillLimits()
    {
        long currentTotal;
        long currentSize;
        synchronized(this)
        {
            currentTotal = _countTotal;
            currentSize  = _countTotalBytes;
        }

        if (!_spilling)
        {
            // We are not currently spilling so we need to calculate the moving average 
            // for the number of items on our stream and then check the moving average 
            // and the total size of the items on the stream against our configured limits.
            
            // moving total is accumulated value over last 20 calculations minus the
            // immediately previous value.  This saves both a division and storing the 
            // last average.  To get the current running total we add the current total
            // to the _movingTotal
            _movingTotal = _movingTotal + currentTotal;
    
            // we calculate the moving average by dividing moving total by number of
            // cycles in the moving window.
            long movingAverage = _movingTotal / MOVING_AVERAGE_LENGTH;
    
            // we then diminish the moving total by the moving average leaving it free for next time.
            _movingTotal = _movingTotal - movingAverage;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Stream is NOT SPILLING");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Current size  :" + currentSize);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Current total :" + currentTotal);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Moving average:" + movingAverage);

            if (movingAverage >= _movingAverageHighLimit ||  // There are too many items on the stream
                  currentSize >= _totalSizeHighLimit)        // The size of items on the stream is too large
            {
                _spilling = true;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Stream has STARTED SPILLING");
            }
        }
        else
        {
            // We are spilling so we just need to check against our configured limits and 
            // if we are below them for both number of items and total size of items then
            // we can stop spilling. If we do stop spilling we also need to reset our moving 
            // average counter.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Stream is SPILLING");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Current size   :" + currentSize);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Current total  :" + currentTotal);

            if (currentTotal <= _movingAverageLowLimit &&   // There are only a few items on the stream
                 currentSize <= _totalSizeLowLimit)         // AND The items on the stream are small
            {
                _spilling = false;
                _movingTotal = _movingAverageLowLimit * (MOVING_AVERAGE_LENGTH - 1);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Stream has STOPPED SPILLING");
            }
        }
    }

    public final synchronized boolean canDelete(int removesUnderTransaction) 
    {
        boolean canDelete = true;
        // we can delete if there are no items in the list.  
        final long totalItems = _countTotal;
        if (totalItems > 0)
        {
            canDelete = false;
            // We can allow a delete if all items in the list are 'removing' under the same
            // transaction.
            final long removesFromMe = _countRemoving;
            if (totalItems == removesFromMe)
            {
                if (removesFromMe == removesUnderTransaction)
                {
                    canDelete = true;
                }
            }
        }
        return canDelete;
    }

    /*******************************************************************************/
    /*                                  ADDING                                     */
    /*******************************************************************************/

    public final synchronized void incrementAdding()
    {
        _countAdding++;
    }

    // Defect 510343.1
    public final void incrementAdding(int sizeInBytes) throws SevereMessageStoreException
    {
        boolean doCallback = false;

        synchronized(this)
        {
            _countAdding++;

            doCallback = _incrementTotal(sizeInBytes);
        }

        if (doCallback)
        {
            _owningStreamLink.eventWatermarkBreached();
        }
    }

    public final synchronized void decrementAdding()
    {
        _countAdding--;
    }

    // Defect 510343.1
    public final void decrementAdding(int sizeInBytes) throws SevereMessageStoreException
    {
        boolean doCallback = false;

        synchronized(this)
        {
            _countAdding--;

            doCallback = _decrementTotal(sizeInBytes);
        }

        if (doCallback)
        {
            _owningStreamLink.eventWatermarkBreached();
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.Statistics#getAddingItemCount()
     */
    public final synchronized long getAddingItemCount()
    {
        return _countAdding;
    }

    /*******************************************************************************/
    /*                                  ADDING                                     */
    /*******************************************************************************/


    /*******************************************************************************/
    /*                                AVAILABLE                                    */
    /*******************************************************************************/

    public final synchronized void incrementAvailable()
    {
        _countAvailable++;
    }

    // Defect 510343.1
    public final void incrementAvailable(int sizeInBytes) throws SevereMessageStoreException
    {
        boolean doCallback = false;

        synchronized(this)
        {
            _countAvailable++;

            doCallback = _incrementTotal(sizeInBytes);
        }

        if (doCallback)
        {
            _owningStreamLink.eventWatermarkBreached();
        }
    }

    public final synchronized void decrementAvailable()
    {
        _countAvailable--;
    }

    // Defect 510343.1
    public final void decrementAvailable(int sizeInBytes) throws SevereMessageStoreException
    {
        boolean doCallback = false;

        synchronized(this)
        {
            _countAvailable--;

            doCallback = _decrementTotal(sizeInBytes);
        }

        if (doCallback)
        {
            _owningStreamLink.eventWatermarkBreached();
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.Statistics#getAvailableItemCount()
     */
    public final synchronized long getAvailableItemCount()
    {
        return _countAvailable;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.Statistics#getUnavailableItemCount()
     */
    public final synchronized long getUnavailableItemCount()
    {
        return _countTotal - _countAvailable;
    }

    /*******************************************************************************/
    /*                                AVAILABLE                                    */
    /*******************************************************************************/


    /*******************************************************************************/
    /*                                EXPIRING                                     */
    /*******************************************************************************/

    public final synchronized void incrementExpiring()
    {
        _countExpiring++;
    }

    public final synchronized void decrementExpiring()
    {
        _countExpiring--;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.Statistics#getExpiringItemCount()
     */
    public final synchronized long getExpiringItemCount()
    {
        return _countExpiring;
    }

    /*******************************************************************************/
    /*                                EXPIRING                                     */
    /*******************************************************************************/


    /*******************************************************************************/
    /*                                LOCKED                                       */
    /*******************************************************************************/

    public final synchronized void incrementLocked()
    {
        _countLocked++;
    }

    // Defect 510343.1
    public final void incrementLocked(int sizeInBytes) throws SevereMessageStoreException
    {
        boolean doCallback = false;

        synchronized(this)
        {
            _countLocked++;

            doCallback = _incrementTotal(sizeInBytes);
        }

        if (doCallback)
        {
            _owningStreamLink.eventWatermarkBreached();
        }
    }

    public final synchronized void decrementLocked()
    {
        _countLocked--;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.Statistics#getLockedItemCount()
     */
    public final synchronized long getLockedItemCount()
    {
        return _countLocked;
    }

    /*******************************************************************************/
    /*                                LOCKED                                       */
    /*******************************************************************************/


    /*******************************************************************************/
    /*                               UPDATING                                      */
    /*******************************************************************************/

    public final synchronized void incrementUpdating()
    {
        _countUpdating++;
    }

    public final synchronized void decrementUpdating()
    {
        _countUpdating--;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.Statistics#getUpdatingItemCount()
     */
    public final synchronized long getUpdatingItemCount()
    {
        return _countUpdating;
    }

    /*******************************************************************************/
    /*                               UPDATING                                      */
    /*******************************************************************************/


    /*******************************************************************************/
    /*                               REMOVING                                      */
    /*******************************************************************************/

    public final synchronized void incrementRemoving()
    {
        _countRemoving++;
    }

    // Defect 510343.1
    public final void incrementRemoving(int sizeInBytes) throws SevereMessageStoreException
    {
        boolean doCallback = false;

        synchronized(this)
        {
            _countRemoving++;

            doCallback = _incrementTotal(sizeInBytes);
        }

        if (doCallback)
        {
            _owningStreamLink.eventWatermarkBreached();
        }
    }

    public final synchronized void decrementRemoving()
    {
        _countRemoving--;
    }

    // Defect 510343.1
    public final void decrementRemoving(int sizeInBytes) throws SevereMessageStoreException
    {
        boolean doCallback = false;

        synchronized(this)
        {
            _countRemoving--;

            doCallback = _decrementTotal(sizeInBytes);
        }

        if (doCallback)
        {
            _owningStreamLink.eventWatermarkBreached();
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.Statistics#getRemovingItemCount()
     */
    public final synchronized long getRemovingItemCount()
    {
        return _countRemoving;
    }

    /*******************************************************************************/
    /*                               REMOVING                                      */
    /*******************************************************************************/


    /*******************************************************************************/
    /*                                TOTAL                                        */
    /*******************************************************************************/

    // Defect 510343.1
    public final void incrementTotal(int sizeInBytes) throws SevereMessageStoreException
    {
        boolean doCallback = _incrementTotal(sizeInBytes);

        // do callback outside synchronization
        if (doCallback)
        {
            _owningStreamLink.eventWatermarkBreached();
        }
    }

    // Defect 510343.1
    // This method only carries out the changes to the total
    // counters. Calling of the watermark event callbacks must 
    // be done OUTSIDE of this method.
    private final synchronized boolean _incrementTotal(int sizeInBytes)
    {
        boolean doCallback = false;

        _countTotal++;
        if (_watermarkCountHigh == _countTotal)
        {
            doCallback = true;
        }

        boolean wasBelowHighLimit = (_countTotalBytes < _watermarkBytesHigh);

        _countTotalBytes = _countTotalBytes + sizeInBytes;

        if (wasBelowHighLimit && _countTotalBytes >= _watermarkBytesHigh)
        {
            doCallback = true;
        }

        return doCallback;
    }

    // Defect 510343.1
    public final void decrementTotal(int sizeInBytes) throws SevereMessageStoreException
    {
        boolean doCallback = _decrementTotal(sizeInBytes);

        // do callback outside synchronization
        if (doCallback)
        {
            _owningStreamLink.eventWatermarkBreached();
        }
    }

    // Defect 510343.1
    // This method only carries out the changes to the total
    // counters. Calling of the watermark event callbacks must 
    // be done OUTSIDE of this method.
    private final synchronized boolean _decrementTotal(int sizeInBytes)
    {
        boolean doCallback = false;

        // Decrement the count before we check for the watermark being reached
        // (510343)
        _countTotal--;
        if (_watermarkCountLow == _countTotal)
        {
            // guaranteed to go below....
            doCallback = true;
        }

        boolean wasAboveLowLimit = (_countTotalBytes >= _watermarkBytesLow);

        _countTotalBytes = _countTotalBytes - sizeInBytes;

        if (wasAboveLowLimit && _countTotalBytes < _watermarkBytesLow)
        {
            doCallback = true;
        }

        return doCallback;
    }

    // Defect 510343.1
    // This method is needed to allow us to replace a previous
    // estimation of an Items size with a more accurate size
    // once we have the Item available to determine it from.
    public final void updateTotal(int oldSizeInBytes, int newSizeInBytes) throws SevereMessageStoreException
    {
        boolean doCallback = false;

        synchronized(this)
        {
            // We're only replacing an old size estimation
            // with a new one so we do not need to change
            // or inspect the count total and watermark
            
            // Check whether we were between our limits before
            // before this update.
            boolean wasBelowHighLimit = (_countTotalBytes < _watermarkBytesHigh);
            boolean wasAboveLowLimit  = (_countTotalBytes >= _watermarkBytesLow);

            // Update our count to the new value by adding the
            // difference between the old and new sizes
            _countTotalBytes = _countTotalBytes + (newSizeInBytes - oldSizeInBytes);

            if ((wasBelowHighLimit && _countTotalBytes >= _watermarkBytesHigh)  // Was below the HIGH watermark but now isn't
                || (wasAboveLowLimit && _countTotalBytes < _watermarkBytesLow)) // OR was above LOW watermark but now isn't
            {
                doCallback = true;
            }
        }

        if (doCallback)
        {
            _owningStreamLink.eventWatermarkBreached();
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.Statistics#getTotalItemCount()
     */
    public final synchronized long getTotalItemCount()
    {
        return _countTotal;
    }

    /*******************************************************************************/
    /*                                TOTAL                                        */
    /*******************************************************************************/

    // Defect 463642 
    // Revert to using spill limits previously removed in SIB0112d.ms.2
    /**
     * @return true if currently spilling
     */
    public final boolean isSpilling() 
    {
        return _spilling;
    }

    public final synchronized void setWatermarks(long countLow, long countHigh, long bytesLow, long bytesHigh)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setWatermarks", new Object[]{"Count Low="+countLow, "Count High="+countHigh, "Bytes Low="+bytesLow, "Bytes High="+bytesHigh});

        _watermarkCountHigh = countHigh;
        _watermarkCountLow = countLow;
        _watermarkBytesHigh = bytesHigh;
        _watermarkBytesLow = bytesLow;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setWatermarks");
    }
}
