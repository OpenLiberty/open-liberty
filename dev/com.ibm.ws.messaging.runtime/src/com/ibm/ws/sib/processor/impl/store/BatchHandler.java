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
package com.ibm.ws.sib.processor.impl.store;

import java.util.HashSet;
import java.util.Iterator;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.util.am.Alarm;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.BatchListener;
import com.ibm.ws.sib.processor.utils.LockManager;
import com.ibm.ws.sib.processor.utils.am.MPAlarmManager;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author tevans
 *
 * Handles batching of updates to the message store
 */
public class BatchHandler implements AlarmListener
{
  /**
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      BatchHandler.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  private LocalTransaction _currentTran = null;
  private int _currentBatchSize = 0;
  private int _batchSize;
  private long _batchTimeout;
  private HashSet _listeners;
  private LockManager _readWriteLock;
  private SIMPTransactionManager _txManager;
  private MPAlarmManager _am;
  private volatile Alarm _alarm;  // Always set/checked under synchronisation

  /**
   * A batch is constrained by a maximum size (batchSize) and a maximum time
   * interval (batchTimeout) - this is the maximum time allowed between adding the
   * first item to a batch and completing the batch.
   *
   * @param batchSize
   * @param batchTimeout
   */
  public BatchHandler(int batchSize,
                      long batchTimeout,
                      SIMPTransactionManager txManager,
                      MPAlarmManager am)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "<init>","batchSize="+batchSize+",batchTimeout="+batchTimeout+",txManager="+txManager+",am="+am);

    _batchSize = batchSize;
    _batchTimeout = batchTimeout;
    _txManager = txManager;
    _am = am;

    _readWriteLock = new LockManager();
    _listeners = new HashSet();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "<init>");
  }

  /**
   * Register an interest in the current batch. The batch can not be
   * completed until messagesAdded is called.
   *
   * @return The transaction being used for this batch.
   * @throws SIStoreException
   */
  public TransactionCommon registerInBatch()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "registerInBatch");

    _readWriteLock.lock(); // Register as a reader

    synchronized(this) // lock the state against other readers
    {
      if(_currentTran == null)
        _currentTran = _txManager.createLocalTransaction(false);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "registerInBatch", _currentTran);
    return _currentTran;
  }

  /**
   * Tell the BatchHandler how many messages were added under the current
   * transaction.
   *
   * registerInBatch must have been called first to register
   * the users interest in the current batch and to get hold of the current
   * transaction. After this method returns, if there are no remaining interested
   * users, the BatchHandler is free to commit the transaction.
   *
   * @param msgCount
   */
  public void messagesAdded(int msgCount) throws SIResourceException
  {
    messagesAdded(msgCount, null);
  }


  /**
   * Tell the BatchHandler how many messages were added under the current
   * transaction.
   *
   * registerInBatch must have been called first to register
   * the users interest in the current batch and to get hold of the current
   * transaction. After this method returns, if there are no remaining interested
   * users, the BatchHandler is free to commit the transaction.
   *
   * An optional listener may be provided to receive notification of transactional
   * events.
   *
   * @param msgCount The number of messages added, must be 0 or greater
   * @param listener An optional listener, may be null
   */
  public void messagesAdded(int msgCount, BatchListener listener) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "messagesAdded","msgCount="+msgCount+",listener="+listener);

    boolean completeBatch = false;

    try
    {
      synchronized(this) // lock the state against other readers
      {
        _currentBatchSize += msgCount;

        if((listener != null) && (!_listeners.contains(listener)))
        {
          // Remember that this listener needs calling when the batch completes
          _listeners.add(listener);
        }

        if(_currentBatchSize >= _batchSize) // Full batch, commit all updates
        {
          completeBatch = true;  // We can't do this under the synchronize as the exclusive lock
                                 // we need will deadlock with reader's trying to get this
        }                        // synchronize before these release their shared lock
        else if ((_currentBatchSize - msgCount) == 0) // New batch so ensure a timer is running
        {
          startTimer();
        }
      }
    }
    finally
    {
      _readWriteLock.unlock(); // release the read lock we took in registerInBatch
    }

    // Now we hold no locks we can try to complete he batch (this takes an exclusive lock
    if(completeBatch)
    {
      completeBatch(false);   // false = only complete a full batch
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "messagesAdded");
  }

  public void completeBatch(boolean force) throws SIResourceException
  {
    completeBatch(force, null);
  }

  /**
   * Complete the current batch. If timer is false then the batch is only completed
   * if the batch is full. If timer is true then the batch is completed so long as there
   * are one or more messages in the batch.
   *
   * @param force true if the batch should commit even when not full
   * @param finalListener an optional extra batch listener
   */
  public void completeBatch(boolean force, BatchListener finalListener) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "completeBatch","force="+force+",finalListener="+finalListener);

    _readWriteLock.lockExclusive(); // Lock the batchHandler exclusively

    try
    {
      // If force is true we complete the batch even if it isn't full, otherwise
      // we only complete a full batch (it's possible someone else got in before us and completed
      // the old batch before we got the lock).
      if( (force && (_currentBatchSize > 0)) || (_currentBatchSize >= _batchSize))
      {
        Iterator itr = _listeners.iterator();
        while(itr.hasNext())
        {
          BatchListener listener = (BatchListener) itr.next();
          listener.batchPrecommit(_currentTran);
        }
        if(finalListener != null) finalListener.batchPrecommit(_currentTran);

        boolean committed = false;
        Exception exceptionOnCommit = null;

        try
        {
          _currentTran.commit(); // Commit the current transaction (this'll drive all the CDs)
          committed = true;
        }
        catch(Exception e)
        {
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.store.BatchHandler.completeBatch",
            "1:254:1.30",
            this);

          committed = false;

          SibTr.exception(tc, e);

          exceptionOnCommit = e;
        }

        // Clear the batch details under the lock
        _currentBatchSize = 0;
        _currentTran = null;

        // Commit or Rollback the listeners
        itr = _listeners.iterator();
        while(itr.hasNext())
        {
          BatchListener listener = (BatchListener) itr.next();

          if(committed) listener.batchCommitted();
          else listener.batchRolledBack();

          itr.remove();
        }
        if(finalListener != null)
        {
          if(committed) finalListener.batchCommitted();
          else finalListener.batchRolledBack();
        }

        // If the commit threw an exception, pass it back now
        if(exceptionOnCommit != null)
        {
          //Release exclusive lock on the batch or everything grinds
          //to a halt
          _readWriteLock.unlockExclusive();

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "completeBatch", exceptionOnCommit);

          throw new SIResourceException(exceptionOnCommit);
        }

        // Ensure any running timer is cancelled
        cancelTimer();
      }
      else  // Nothing transacted to do but still need to call finalListener to cleanup
      {
        if(finalListener != null)
          finalListener.batchCommitted();
      }
    }
    finally
    {
      _readWriteLock.unlockExclusive();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "completeBatch");
  }

  private void startTimer()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "startTimer");

    synchronized(this) {
      if (_alarm == null) {
        _alarm = _am.create(_batchTimeout, this);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "startTimer");
  }

  private void cancelTimer() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "cancelTimer");

    synchronized(this) {
      if (_alarm != null) {
        _alarm.cancel();
        _alarm = null;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "cancelTimer");
  }

  // Method called when an alarm pops

  public void alarm (Object alarmContext)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "alarm", alarmContext);

    synchronized(this) {
      _alarm = null;
    }

    try {
      completeBatch(true);
    } catch(SIException e) {
      //No FFDC code needed
      SibTr.exception(this, tc, e);
      //can't do anything else here
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "alarm");
  }
}
