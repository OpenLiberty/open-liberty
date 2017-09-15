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

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.exceptions.ClosedException;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class executed operations on the Message Store asynchronously.
 * Operations are encapsulated as AsyncUpdates and enqueued on this object.
 * Periodically, this class will take all the enqueued AsyncUpdates,
 * get a thread, and execute all these updates on the thread using a single
 * local transaction. The AsyncUpdates are informed if the transaction
 * commits or rolls back.
 * The conditions that trigger the execution of AsyncUpdates are:
 * (1) The previous thread executing AsyncUpdates is finished (either has committed or
 *     rolledback), so this class behaves in a single threaded manner.
 *   AND
 * (2) either (a) or (b) is true
 *    (a) the number of enqueued AsyncUpdates has exceeded the batchThreshold.
 *    (b) an interval greater than maxCommitInterval has elapsed since the start
 *        of the previous transaction.
 */
public class AsyncUpdateThread implements AlarmListener
{
  private static final TraceComponent tc =
    SibTr.register(
      AsyncUpdateThread.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  private final SIMPTransactionManager tranManager;
  private final MessageProcessor mp;

  /** AsyncUpdates that have been enqueued */
  private ArrayList enqueuedUnits;

  /** true if an execution thread has been scheduled and has not finished */
  private boolean executing;
  /** the AsyncUpdates to execute */
  private ArrayList executingUnits;

  private final int batchThreshold;

  private final long maxCommitInterval;
  private boolean executeSinceExpiry;

  private boolean closed;

  /**
   * NLS for component
   */
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  // this object is no longer providing service since it is closed
  /**
   * Constructor
   * @param mp The MessageProcessor, used to get an execution thread.
   * @param tranManager The transaction manager, to get a local transaction
   * @param batchThreshold When the number of enqueued AsyncUpdates exceeds this threshold,
   *        their execution is scheduled
   * @param maxCommitInterval Execution will be scheduled at an interval that does not
   *        exceed the range [maxCommitInterval, 2*maxCommitInterval]. A value < 0 disables periodic commit.
   *
   */
  public AsyncUpdateThread(
    MessageProcessor mp,
    SIMPTransactionManager tranManager,
    int batchThreshold,
    long maxCommitInterval)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "AsyncUpdateThread",
        new Object[] {
          mp,
          tranManager,
          Integer.valueOf(batchThreshold),
          Long.valueOf(maxCommitInterval)});

    this.mp = mp;
    this.tranManager = tranManager;
    this.batchThreshold = batchThreshold;
    this.maxCommitInterval = maxCommitInterval;
    this.closed = false;

    enqueuedUnits = new ArrayList(10);
    executingUnits = new ArrayList(10);
    executing = false;

    executeSinceExpiry = false;

    if (maxCommitInterval > 0)
    {
      mp.getAlarmManager().create(maxCommitInterval, this);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AsyncUpdateThread", this);

  }

  /**
   * Enqueue an AsyncUpdate
   * @param unit the AsyncUpdate
   */
  public void enqueueWork(AsyncUpdate unit) throws ClosedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "enqueueWork", unit);

    synchronized (this)
    {
      if (closed)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "enqueueWork", "ClosedException");
        throw new ClosedException();
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Enqueueing update: " + unit);
      
      enqueuedUnits.add(unit);
      if (executing)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "enqueueWork", "AsyncUpdateThread executing");
        return;
      }

      // not executing enqueued updates
      if (enqueuedUnits.size() > batchThreshold)
      {
        executeSinceExpiry = true;
        try
        {
          startExecutingUpdates();
        }
        catch (ClosedException e)
        {
          // No FFDC code needed
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "enqueueWork", e);
          throw e;
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "enqueueWork");
  }

  /**
   * Internal method. Should be called from within a synchronized block.
   */
  private void startExecutingUpdates() throws ClosedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "startExecutingUpdates");

    // swap the enqueuedUnits and executingUnits.
    ArrayList temp = executingUnits;
    executingUnits = enqueuedUnits;
    enqueuedUnits = temp;
    enqueuedUnits.clear();
    // enqueuedUnits is now ready to accept AsyncUpdates in enqueueWork()

    executing = true;
    try
    {
      LocalTransaction tran = tranManager.createLocalTransaction(false);
      ExecutionThread thread = new ExecutionThread(executingUnits, tran);
      mp.startNewSystemThread(thread);
    }
    catch (InterruptedException e)
    {
      // this object cannot recover from this exception since we don't know how much work the ExecutionThread
      // has done. should not occur!
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.AsyncUpdateThread.startExecutingUpdates",
        "1:222:1.28",
        this);
      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "startExecutingUpdates", e);

      closed = true;
      throw new ClosedException(e.getMessage());
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "startExecutingUpdates");

  }

  class ExecutionThread implements Runnable
  {
    private List _list;
    private LocalTransaction _tran;

    ExecutionThread(List list, LocalTransaction tran)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "ExecutionThread", new Object[] {list, tran});
      
      _list = list;
      _tran = tran;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "ExecutionThread", this);
    }

    public void run()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "run", this);

      try
      {
        Throwable ex = null;

        // Keep running around the do-while until we decide otherwise
        boolean keepRunning = true;

        do
        {
          int length = _list.size();
          for (int i = 0; i < length; i++)
          {
            AsyncUpdate unit = (AsyncUpdate) _list.get(i);
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
              SibTr.debug(tc, "Executing update: " + unit);
            
            try
            {
              unit.execute(_tran);
            }
            catch (Throwable e)
            {
              FFDCFilter.processException(
                  e,
                  "com.ibm.ws.sib.processor.impl.store.AsyncUpdateThread.ExecutionThread.run",
                  "1:286:1.28",
                  this);
              
              ex = e;
              break; // break out of the for loop
            }
          }

          if (ex == null)
          {
            // commit
            try
            {
              _tran.commit();
            }
            catch (SIException x)
            {
              // apparently the transaction has already committed. This is a serious bug!!
              FFDCFilter.processException(
                x,
                "com.ibm.ws.sib.processor.impl.store.AsyncUpdateThread.ExecutionThread.run",
                "1:307:1.28",
                this);
              SibTr.exception(tc, x);

              ex = x;
            }
            catch (SIErrorException x)
            {
              // this is probably a serious problem
              FFDCFilter.processException(
                x,
                "com.ibm.ws.sib.processor.impl.store.AsyncUpdateThread.ExecutionThread.run",
                "1:319:1.28",
                this);
              SibTr.exception(tc, x);

              ex = x;
            }
            catch (Throwable x)
            {
              // this is probably a serious problem
              FFDCFilter.processException(
                x,
                "com.ibm.ws.sib.processor.impl.store.AsyncUpdateThread.ExecutionThread.run",
                "1:331:1.28",
                this);
              SibTr.exception(tc, x);

              ex = x;

            }
          } // end if (ex == null)
          else
          { // one of the execute() methods threw an exception
            // so rollback this transaction
            try
            {
              _tran.rollback();
            }
            catch (SIException x)
            {
              // apparently the transaction has already committed. This is a serious bug!!
              FFDCFilter.processException(
                x,
                "com.ibm.ws.sib.processor.impl.store.AsyncUpdateThread.ExecutionThread.run",
                "1:352:1.28",
                this);
              SibTr.exception(tc, x);
            }
            catch (SIErrorException x)
            {
              // may be a serious problem
              FFDCFilter.processException(
                x,
                "com.ibm.ws.sib.processor.impl.store.AsyncUpdateThread.ExecutionThread.run",
                "1:362:1.28",
                this);
              SibTr.exception(tc, x);
            }
            catch (Throwable x)
            {
              // this is probably a serious problem
              FFDCFilter.processException(
                x,
                "com.ibm.ws.sib.processor.impl.store.AsyncUpdateThread.ExecutionThread.run",
                "1:372:1.28",
                this);
              SibTr.exception(tc, x);
            }
          }

          // notify
          for (int i = 0; i < length; i++)
          {
            AsyncUpdate unit = (AsyncUpdate) _list.get(i);
            if (ex == null)
              try
              {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  SibTr.debug(tc, "Committing update: " + unit);
                
                unit.committed();
              }
              catch (SIException e)
              {
                // FFDC
                FFDCFilter.processException(
                  e,
                  "com.ibm.ws.sib.processor.impl.store.AsyncUpdateThread.ExecutionThread.run",
                  "1:396:1.28",
                  this);

                SibTr.exception(tc, e);
              }
            else
            {
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Rolling back update: " + unit);
              
              unit.rolledback(ex);
            }
          }

          // now indicate that done executing, and schedule another execution if batchThreshold exceeded
          synchronized (AsyncUpdateThread.this)
          {
            if (!closed)
            {
              executing = false;

              if (ex == null) // If tran could not commit - Wait for the alarm to pop before retrying
              {
                // not executing enqueued updates
                if (enqueuedUnits.size() > batchThreshold)
                {
                  executeSinceExpiry = true;

                  ArrayList temp = executingUnits;
                  executingUnits = enqueuedUnits;
                  enqueuedUnits = temp;
                  enqueuedUnits.clear();
                  // enqueuedUnits is now ready to accept AsyncUpdates in enqueueWork()

                  executing = true;

                  _list = executingUnits;
                  _tran = tranManager.createLocalTransaction(false);
                }
              }
            }
            AsyncUpdateThread.this.notify();

            // In order to avoid holding the 'AsyncUpdateThread.this' all the way around
            // do while loop, we need to have a local copy of the updated executing flag
            keepRunning = executing; // Ensure we stop if we're about to tell the world we're stopping
          }
        } while (keepRunning);  // New work on queue while we were running and no errors so far
      }
      catch (RuntimeException e)
      {

        // FFDC
        FFDCFilter.processException(e,
                                    "com.ibm.ws.sib.processor.impl.store.AsyncUpdateThread.ExecutionThread.run",
                                    "1:451:1.28",
                                    this);

        synchronized(AsyncUpdateThread.this)
        {
          closed = true;
        }

        SibTr.error(tc,
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] { "com.ibm.ws.sib.processor.impl.store.AsyncUpdateThread.ExecutionThread.run", "1:462:1.28", e },
            null));

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
          SibTr.exception(tc, e);
          SibTr.exit(tc, "run", e);
        }
        throw e;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "run");
    } // end public void run()

  } // end class ExecutionThread ...

  public void alarm(Object thandle)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "alarm", new Object[] {this, mp.getMessagingEngineUuid()});

    synchronized (this)
    {
      if (!closed)
      {
        if ((executeSinceExpiry) || executing)
        { // has committed recently
          executeSinceExpiry = false;
        }
        else
        { // has not committed recently
          try
          {
            if (enqueuedUnits.size() > 0)
              startExecutingUpdates();
          }
          catch (ClosedException e)
          {
            // No FFDC code needed
            // do nothing as error already logged by startExecutingUpdates
          }
        }
      }
    } // end synchronized (this)

    if (maxCommitInterval > 0)
    {
      mp.getAlarmManager().create(maxCommitInterval, this);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "alarm");
  }

  /**
   * Close this. Note that committed(),rolledback(),execute() callbacks can occur after this is closed.
   */
  public void close()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "close");
    synchronized (this)
    {
      closed = true;
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "close");
  }

  /**
   * This method blocks till there are 0 enqueued updates and 0 executing updates.
   * Useful for unit testing.
   */
  public void waitTillAllUpdatesExecuted() throws InterruptedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "waitTillAllUpdatesExecuted");
    synchronized (this)
    {
      while (enqueuedUnits.size() > 0 || executing)
      {
        try
        {
          this.wait();
        }
        catch (InterruptedException e)
        {
          // No FFDC code needed
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "waitTillAllUpdatesExecuted", e);
          throw e;
        }
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "waitTillAllUpdatesExecuted");
  }

}
