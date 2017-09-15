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

package com.ibm.ws.sib.processor.impl;

// Import required classes.
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ejs.util.am.Alarm;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.gd.ControllableStream;
import com.ibm.ws.sib.processor.gd.RangeList;
import com.ibm.ws.sib.processor.gd.StateStream;
import com.ibm.ws.sib.processor.gd.TickRange;
import com.ibm.ws.sib.processor.impl.exceptions.ClosedException;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumableKey;
import com.ibm.ws.sib.processor.impl.interfaces.ControllableResource;
import com.ibm.ws.sib.processor.impl.interfaces.JSConsumerManager;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.AsyncUpdate;
import com.ibm.ws.sib.processor.impl.store.AsyncUpdateThread;
import com.ibm.ws.sib.processor.impl.store.items.AOValue;
import com.ibm.ws.sib.processor.impl.store.itemstreams.AOProtocolItemStream;
import com.ibm.ws.sib.processor.runtime.SIMPDeliveryTransmitControllable;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.processor.runtime.impl.RemoteConsumerTransmit;
import com.ibm.ws.sib.processor.utils.am.BatchedTimeoutManager;
import com.ibm.ws.sib.processor.utils.am.BatchedTimeoutProcessor;
import com.ibm.ws.sib.processor.utils.am.MPAlarmManager;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.SelectorDomain;

/**
 * An output stream for the 'remote get' protocol.
 *
 * !! need to change design doc: startedFlush is persisted and restored on recovery
 *
 * Implementation of ConsumerCardinality=EXCLUSIVE:
 *  The AOStream is typically provided with a JSRemoteConsumerPoint after it is created. It will
 *  try to use this AOCK. If an AOCK is not provided to it, or does not work for some
 *  reason, the AOStream will try to create a new AOCK by itself.
 *  If creating a new AOCK fails, probably because of a cardinality
 *  violation, it will flush itself.
 *  If the stream has not seen any messages
 *  from the RME within the REMOTE_CONSUMER_CARDINALITY_INACTIVITY_INTERVAL and the RME is unreachable, it will
 *  schedule itself to be flushed (if not already begun flush) AND close all its consumer keys.
 *
 * SYNCHRONIZATION: Data-structures are read and written by synchronizing on this. All
 * calls to the parent to send messages occur outside synchronized (this) (not all but the ones
 * that could be frequent and hence affect performance). Other calls
 * to the parent can occur in synchronized (this). Therefore the parent should not call
 * methods on AOStream while holding locks on its own datastructures.
 *
 * STREAM STATE MANAGEMENT:
 *   The stream state is managed using various fields
 *   active: initialized to false. set to true when start() is called, and is set to false when stop() is called.
 *         this controls whether the stream will send any messages. Note even a !active stream can accept incoming
 *         messages. It is used for flow control
 *   startedFlush: true, if the stream is trying to flush itself. Note that startedFlush to isFlushed transition
 *      typically requires communication with the RME
 *   isFlushed:  true, if the stream has flushed itself. isFlushed => closed
 *   closed: Unlike flushing a stream, which cleans up all resources used by the stream, including persistent
 *      storage, closing a stream is just cleaning up the in-memory resources.
 *      All non-persistent locks will be released, and
 *      when the close returns, the AOStream will not attempt to write any more data to persistent storage.
 *      This method can be used as part of a method to forcibly delete the stream: first call close() and
 *      once it returns remove all the persistent AOValue ticks and the persistent locks, and then delete
 *      the AOProtocolItemStream. The method can also be used if we want to cleanly (but temporarily)
 *      shutdown the ME.
 *
 *      Note that closed => !active, i.e., the act of closing the stream also makes it
 *      inactive. Note that closed does not imply isFlushed.
 */
public final class AOStream extends ControllableStream implements BatchedTimeoutProcessor, ControllableResource
{
  private MessageProcessor mp;
  private MPAlarmManager am;

  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private static TraceComponent tc =
    SibTr.register(
      AOStream.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  public static final int NUM_OF_BUCKETS = 4; // for the BatchedTimeoutManager

  /** The remote ME for this protocol stream */
  public final SIBUuid8 remoteMEUuid;

  /** The unique name of this stream, relative to the remoteMEId */
  public final SIBUuid12 streamId;

  private final long controlItemLockID; // the MS lockID used to lock any control information prior to removal.

  /**
   * The gatheringTargetDestUuid is the id used along with the targetMEUuid to lookup a specific
   * AIH. Multiple AIHs (along with their associated streams/resources) are created. There is one
   * for each remoteME/consumerTarget pair. gatheringTargetDestUuid is null for standard consumers, and
   * contains a destination uuid if it is a gathering consumer. The destination uuid can be an alias
   * representing a subset of queue points.
   */
  private SIBUuid12 gatheringTargetDestUuid;

  /**
   * This is the consumerManager for this AOStream. Each AOStream has a unique consumerDispatcher which
   * could be
   * a) A pubSub consumerdispatcher for remote durable
   * b) A local consumerdispatcher for standard remoteget
   * c) A gathered consumer dispatcher for remote gather
   */
  private JSConsumerManager consumerDispatcher;

  public final AOProtocolItemStream itemStream;

  /** This object is encapsulated in a parent object, which is a AnycastOutputHandler */
  private final AnycastOutputHandler parent;

  /** The non-persistent stream state, part of which is persisted elsewhere */
  private final StateStream stream;

  //TODO in the future we might want to expose more stream states
  private AOStreamState streamState = AOStreamState.ACTIVE;

  private RequestHighestGeneratedTick requestHighestGeneratedTickTimer;

  private long totalReceivedRequests;

  /** The first unknown tick in the stream.
   *  !startedFlush => all ticks in the range [firstUnknownTick, infinity] are unknown AND firstUnknownTick > 0
   *  startedFlush => firstUnknownTick = Long.MAX_VALUE (note this is just the boundary case, and the tick at Long.MAX_VALUE
   *   is not necessarily unknown.
   */
  private long firstUnknownTick;

  /** table of JSRemoteConsumerPoint objects, keyed by the string Selector, which is "" for no selector */
  private final Hashtable consumerKeyTable;

  /** whether this stream has started the flush protocol. If true, the consumerKeyTable is empty, and no
   * JSRemoteConsumerPoints will be created in the future. */
  private boolean startedFlush;
  /** has this stream been successfully flushed. Invariant: isFlushed => startedFlush */
  private boolean isFlushed;
  /** before changing state to startedFlush=true, this fact has to be written to persistent storage. The
   * scheduleWriteStartedFlush is set to true when this write to persistent storage has been scheduled.
   */
  private boolean scheduleWriteStartedFlush;

  /** has the completed ticks been initialized. Invariant: startedFlush => completedTicksInitialized */
  private boolean completedTicksInitialized;

  /** for repeating RequestHighestGeneratedTick */
  private Alarm initRepeatHandler;
  /** the request id used for RequestHighestGeneratedTick messages */
  private long initRequestId;
  /** The highest tick in value state. Only kept current till completedTicksInitialized becomes true */
  private long initHighestValueTick;

  /** for sending and repeating ResetRequestAck messages */
  private ResetRequestAckSender resetRequestAckSender;

  private final BatchedTimeoutManager dem;

  // Used to re-request AIMessageItems on a restarted IME
  private BatchedTimeoutManager imeRestorationHandler;

  private long dmeVerion;

  /** true if the stream is active. This is manipulated by the start(), stop() methods. A stream can be stopped
   *  and started many times. Stopping a stream prevents it from sending any messages, both data and control. Note
   * that a stopped stream will continue to use the message store to persist tick information for waiting get
   * requests that have been assigned a message. But the assigned messages will not be sent to the remote ME.
   */
  private boolean active;

  private boolean closed;
  // whether the stream has been closed. see comment on STREAM STATE MANAGEMENT above

  private final AsyncUpdateThread msUpdateThread;
  // the count of async writes submitted by this AOStream but not yet committed or aborted.
  private int countAsyncUpdatesOutstanding;
  /** The latestTick for each Qos, priority. Even thought it is only meaningful when consumer cardinality is one,
   * it is kept current even if consumer cardinality is greater than one. However the prevTick on a message to be
   * sent is only set when cardinality is one
   */
  private final long[][] latestTick;

  /**
   * Used for flushing the stream if ConsumerCardinality=1 and the RME has not sent messages in a while and
   * is currently unreachable
   */
  private Alarm inactivityTimer;
  private final InactivityTimerHandler inactivityHandler;
  /** Used to keep track of whether RME has sent any messages in a while */
  private boolean messagesReceived = false;

  private ControlAdapter _controlAdapter;

  /**
   * Constructor
   * @param remoteMEId The UUID of the remote ME
   * @param streamId The UUID of the stream
   * @param parent The container for this object
   * @param firstTime true if this stream is being created for the first time, else false
   * @param valueTicks A list of AOValue objects (null if firstTime==true)
   * @param startedFlush true if started the flush protocol on this stream prior to last ME crash
   */
  public AOStream(
    SIBUuid8 remoteMEId,
    SIBUuid12 gatheringTargetDestUuid,
    SIBUuid12 streamId,
    AOProtocolItemStream itemStream,
    AnycastOutputHandler parent,
    AsyncUpdateThread msUpdateThread,
    boolean firstTime,
    List valueTicks,
    boolean startedFlush,
    MessageProcessor mp,
    long dmeVersion,
    JSConsumerManager consumerDispatcher)
    throws Exception
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AOStream",
        new Object[]{remoteMEId,
                     gatheringTargetDestUuid,
                     streamId,
                     itemStream,
                     parent,
                     msUpdateThread,
                     Boolean.valueOf(firstTime),
                     valueTicks,
                     Boolean.valueOf(startedFlush),
                     mp, Long.valueOf(dmeVersion),
                     consumerDispatcher});

    synchronized (this)
    {
      this.controlItemLockID = mp.getMessageStore().getUniqueLockID(AbstractItem.STORE_NEVER);
      this.remoteMEUuid = remoteMEId;
      this.gatheringTargetDestUuid = gatheringTargetDestUuid;
      this.streamId = streamId;
      this.itemStream = itemStream;
      this.parent = parent;
      this.consumerDispatcher = consumerDispatcher;
      this.msUpdateThread = msUpdateThread;
      this.countAsyncUpdatesOutstanding = 0;
      this.mp = mp;
      am = mp.getAlarmManager();

      this.startedFlush = startedFlush;
      this.isFlushed = false;
      this.consumerKeyTable = new Hashtable();
      this.scheduleWriteStartedFlush = false;
      this.closed = false;
      this.inactivityHandler = new InactivityTimerHandler();
      this.dmeVerion = dmeVersion;

      this.latestTick =
        new long[Reliability.MAX_INDEX
          + 1][SIMPConstants.MSG_HIGH_PRIORITY
          + 1];
      // initialize latestTicks to unknown if reliability not ASSURED
      // TODO - check that the reliability is set correctly
      for (int i = 0; i <= Reliability.MAX_INDEX; i++)
      {
        for (int j = 0; j <= SIMPConstants.MSG_HIGH_PRIORITY; j++)
        {
          if (i == Reliability.MAX_INDEX)
            latestTick[i][j] = 0;
          else
            latestTick[i][j] = SIMPConstants.UNKNOWN_PREV_TICK;
        }
      }

      // use valueTicks to initialize StateStream
      initHighestValueTick = 0;
      stream = new StateStream();
      stream.init();

      if (!firstTime)
      {
        // to keep track of value ticks that we need to remove from the item stream
        // this occurs because messages can be STORE_EVENTUALLY, so one of the following 3 cases
        // will occur:
        // - The message was not spilled: use the MessageStore.findById() to discover this condition,
        // - The message was spilled but not the lock: call getLockID() on the SIMPMessage and compare
        // it to the lock id stored in the AOValue.
        // - The message and the lock was spilled
        // In the first 2 cases, we will remove the AOValue
        ArrayList<AOValue> valueTicksToRemove = null;

        // now go through all the AOValue ticks
        for (int i = 0; i < valueTicks.size(); i++)
        {
          try
          {
            AOValue value = (AOValue) valueTicks.get(i);
            boolean toRemove = false; // should we remove 'value'
            // SIB0113 - We only remove values if we are not on an IME (message gathering)
            // Otherwise we go on to restore them.
            if (value.getStorageStrategy() == AbstractItem.STORE_EVENTUALLY && gatheringTargetDestUuid==null)
            {
              SIMPMessage msg = consumerDispatcher.
                getMessageByValue(value);

              if (msg == null)
                toRemove = true;
              else
              {
                // we found the message. Now check if the lockId's match
                if (msg.getLockID() != value.getPLockId())
                  toRemove = true;
              }
            }

            if (toRemove)
            {
              if (valueTicksToRemove == null)
              {
                valueTicksToRemove = new ArrayList<AOValue>(10); // lazy creation
              }
              valueTicksToRemove.add(value);
            }
            else // !toRemove
            {

              if (value.getTick() > initHighestValueTick)
              {
                initHighestValueTick = value.getTick();
              }
              TickRange tr = TickRange.newValueTick(value.getTick(), value, 0L);
              stream.writeRange(tr);
              
              // As this value is left over from a request from a previous incarnation of the
              // AOStream then we bump the request count. This way we don't have more current
              // requests (e.g. value ranges) than total requests.
              totalReceivedRequests++;

              SIMPMessage msg = consumerDispatcher.getMessageByValue(value);
              int reliability, priority;
              if (msg!=null)
              {
                // Register callbacks for ordered messaging
                if (consumerDispatcher.getDestination().isOrdered())
                  consumerDispatcher.setCurrentTransaction(msg, true);

                reliability = msg.getMessage().getReliability().getIndex();
                priority = msg.getMessage().getPriority().intValue();
              }
              else
              {
                // We have no msg so we must be a restoring IME (gathering msgs)
                // We therefore use the persisted priority and reliability from the original msg.
                priority =value.getMsgPriority();
                reliability =value.getMsgReliability();
                value.restored = false;
              }

              if (latestTick[reliability][priority] < value.getTick())
              {
                latestTick[reliability][priority] = value.getTick();
              }
            } // end else
          }
          catch (Exception e)
          {
            // log and throw serious error
            FFDCFilter.processException(
              e,
              "com.ibm.ws.sib.processor.impl.AOStream.AOStream",
              "1:455:1.80.3.24",
              this);
            SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.exit(tc, "AOStream", e);
            throw e;
          }
        } // end for (int i=0; i < valueTicks.size(); i++)

        if (valueTicksToRemove != null)
        {
          // create a local transaction and delete all these ticks
          LocalTransaction tran = parent.getLocalTransaction();
          try
          {
            Transaction msTran = mp.resolveAndEnlistMsgStoreTransaction(tran);
            int length = valueTicksToRemove.size();
            for (int i = 0; i < length; i++)
            {
              AOValue value = (AOValue) valueTicksToRemove.get(i);
              unlockRejectedTick(msTran, value);
            }
            tran.commit();
          }
          catch (Exception e)
          {
            FFDCFilter.processException(
              e,
              "com.ibm.ws.sib.processor.impl.AOStream.AOStream",
              "1:484:1.80.3.24",
              this);
            SibTr.exception(tc, e);

            // Rather than leave any work sat in an orphaned transaction, roll back
            // any work performed.
            try
            {
              tran.rollback();
            }
            catch(Exception e2)
            {
              // No FFDC code needed
              // We're not interested in any problem with a rollback
            }
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.exit(tc, "AOStream", e);

            throw e;
          }
        }
      } // end if (!firstTime)

      dem =
        new BatchedTimeoutManager(
          AOStream.NUM_OF_BUCKETS,
          mp.getCustomProperties().get_decision_expected_repetition_interval(),
          valueTicks,
          this,
          mp);

      if (firstTime)
      {
        completedTicksInitialized = true;
        this.firstUnknownTick = 1L;
        changeUnknownToCompleted(0, 0);
      }
      else if (startedFlush) // started the flush prior to crash
      {
        completedTicksInitialized = true;
        // don't need to initialize completed ticks by communicating with the RME
        if (valueTicks.isEmpty())
        {
          isFlushed = true;
          closed = true;
          // how will the parent find out that this is flushed?
          // Well, the parent shouldn't be calling this constructor if valueTicks is empty and startedFlush=true!!
          SIErrorException e =
            new SIErrorException(
              nls.getFormattedMessage(
                "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                new Object[] {
                  "com.ibm.ws.sib.processor.impl.AOStream",
                  "1:538:1.80.3.24" },
                null));
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.AOStream.AOStream",
            "1:543:1.80.3.24",
            this);
          SibTr.exception(tc, e);
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.AOStream",
              "1:549:1.80.3.24" });
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "AOStream", e);
          throw e;

        }
        this.firstUnknownTick = Long.MAX_VALUE;
        // change all unknown ticks in the stream to completed. Since only value and unknown ticks are
        // in the stream at this point, the result will be a stream with only value and completed ticks.
        changeUnknownToCompleted(0, Long.MAX_VALUE);
      }
      else
      { // have not started flush, so have to initialize completed ticks by
        // asking the remote ME for the highest generated tick
        completedTicksInitialized = false;
        initRequestId = -1; // initialize later
        resetRequestAckSender = new ResetRequestAckSender();
      }

    } // end synchronized (this)

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AOStream", this);

  }

  /**
   * Called by AOH when ConsumerCardinality=1, before starting the stream
   */
  public void setConsumerKey(JSRemoteConsumerPoint aock)
  {
    synchronized (this)
    {
      consumerKeyTable.put("", aock);
    }
  }

  /**
   * Start the stream, i.e., start sending data and control messages
   */
  public void start() throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "start");

    synchronized (this)
    {
      active = true;
      // start the liveness timer for sending ControlRequestHighestGeneratedTick, if needed
      if (!completedTicksInitialized)
      {
        if (initRequestId == -1)
          initRequestId = parent.generateUniqueValue();
        requestHighestGeneratedTickTimer = new RequestHighestGeneratedTick();
        //we call the alarm directly
        requestHighestGeneratedTickTimer.alarm(null);
      }
      if (parent.getCardinalityOne() && !isFlushed)
      {
        inactivityTimer =
          am.create(
            mp.getCustomProperties().get_remote_consumer_cardinality_inactivity_interval(),
            inactivityHandler);
      }
      if (resetRequestAckSender != null)
      {
        boolean done = resetRequestAckSender.start();
        if (done)
          resetRequestAckSender = null;
      }
      // start all the liveness timers for value ticks
      dem.startTimer();
      if (imeRestorationHandler!=null)
        imeRestorationHandler.startTimer();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "start");
  }

  /**
   * Stop the stream, i.e., stop sending data and control messages
   */
  public void stop()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "stop");

    synchronized (this)
    {
      active = false;
      // stop the liveness timer for sending ControlRequestHighestGeneratedTick, if needed

      //NOTE: the requestHighestGeneratedTickTimer will stop of its own accord once the
      //stream is noticed to be

      if (initRepeatHandler != null)
      {
        initRepeatHandler.cancel();
      }
      if (inactivityTimer != null)
      {
        inactivityTimer.cancel();
        inactivityTimer = null;
      }
      if (resetRequestAckSender != null)
      {
        resetRequestAckSender.stop();
      }
      // stop all the liveness timers for value ticks
      dem.stopTimer();
      if (imeRestorationHandler!=null)
        imeRestorationHandler.stopTimer();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "stop");
  }

  // for unit testing
  public final long getInitRequestId()
  {
    return initRequestId;
  }


  class InactivityTimerHandler implements AlarmListener
  {
    public void alarm(Object thandle)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "alarm", 
            new Object[] {thandle,
              "InactivityTimerHandler",
              AOStream.this});

      synchronized (AOStream.this)
      {
        if (active)
        {
          if ((!isFlushed)
            && (!messagesReceived)
            && (!parent.isMEReachable(remoteMEUuid)))
          {
            if (!startedFlush)
            {
              processRequestFlush();
            }
            // now close all the JSRemoteConsumerPoints
            Enumeration vEnum = consumerKeyTable.elements();
            while (vEnum.hasMoreElements())
            {
              JSRemoteConsumerPoint aock = (JSRemoteConsumerPoint) vEnum.nextElement();
              aock.close();
            }
            consumerKeyTable.clear();

            // now decide whether we need to repeat this alarm
            if (startedFlush)
            {
              // no possibility that this will create a new JSRemoteConsumerPoint, and
              // we have just closed all the existing JSRemoteConsumerPoints so no need to repeat this alarm
            }
            else
            {
              inactivityTimer =
                am.create(
                  mp.getCustomProperties().get_remote_consumer_cardinality_inactivity_interval(),
                  inactivityHandler);
            }
          }
          else
          {
            if (!isFlushed)
              inactivityTimer =
                am.create(
                  mp.getCustomProperties().get_remote_consumer_cardinality_inactivity_interval(),
                  inactivityHandler);
          }
          messagesReceived = false;
        } // end if (active)
      } // end synchronized (this)
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "alarm");
    }
  }

  /**
   * Sends regular RequestHighestGeneratedTick to the RME
   * if necessary - see defect 344280.
   */
  class RequestHighestGeneratedTick implements AlarmListener
  {
    /**
     * Initialization alarm
     */
    public void alarm(Object thandle)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "alarm",
            new Object[] {thandle, "RequestHighestGeneratedTick",
              AOStream.this});

      boolean sendMsg = false;
      long    requestId = -1;
      synchronized (AOStream.this)
      {
        if(isFlushed)
        {
          //see defect 344280: this is not really valid.
          //We behave in the same way as we did before and continue to send the msg,
          //but now we output a warning debug message
          if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Sending requestHighestGeneratedTick despite stream being flushed!");
        }

        if ((active) && (!completedTicksInitialized))
        {
          //only send a message and reschedule if it is necessary
          sendMsg = true;
          requestId = initRequestId; // Save the request id to be used
          initRepeatHandler =
            am.create(mp.getCustomProperties().get_init_repetition_interval(), this);
        }
      }
      if (sendMsg)
        parent.sendRequestHighestGeneratedTick(
            remoteMEUuid,
            gatheringTargetDestUuid,
          streamId,
          requestId);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "alarm");
    }
  }

  // class that handles repetition of ResetRequestAckSender
  class ResetRequestAckSender implements AlarmListener
  {
    final long initTime;
    boolean done; // no more work left to do
    Alarm rraAlarm;

    ResetRequestAckSender()
    {
      initTime = System.currentTimeMillis();
      done = false;
    }
    boolean start()
    {
      synchronized (AOStream.this)
      {
        if (!done)
          alarm(null);
        return done;
      }
    }
    void stop()
    {
      synchronized (AOStream.this)
      {
        if (rraAlarm != null)
        {
          rraAlarm.cancel();
          rraAlarm = null;
        }
      }
    }
    public void alarm(Object thandle)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "alarm",
            new Object[] {thandle, "ResetRequestAckSender",
              AOStream.this});

      boolean sendMsg = false;
      long currTime = System.currentTimeMillis();
      synchronized (AOStream.this)
      {
        if (currTime - initTime
          > mp.getCustomProperties().get_slowed_get_request_interval())
        {
          // too much time has elapsed so the slowed get are no longer slow.
          // so no need to repeat this further
          rraAlarm = null;
          done = true;
        }
        else
        {
          // continue repeating
          sendMsg = active;
          rraAlarm =
            am.create(
              mp.getCustomProperties().get_reset_repetition_interval(),
              this);
        }
      }
      if (sendMsg)
      {
        parent.sendResetRequestAck(
            remoteMEUuid,
            gatheringTargetDestUuid,
          streamId,
          parent.dmeVersion);
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "alarm");
    }
  } // end class ResetRequestAckSender
  /**
   * Called when the DecisionExpected timeout occurs
   * @param timedout
   */
  public void processTimedoutEntries(List timedout)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processTimedoutEntries", new Object[] {this,timedout});

    boolean sendMsg = false;
    synchronized (this)
    {
      if (active && completedTicksInitialized && !timedout.isEmpty())
      {
        sendMsg = true;
      }
    }
    if (sendMsg)
      parent.sendDecisionExpected(remoteMEUuid, gatheringTargetDestUuid, streamId, timedout);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processTimedoutEntries");
  }

  /**
   * Callback from JSRemoteConsumerPoint that the given tick in the stream should be changed to the completed state.
   * @param tick
   */
  public final void expiredRequest(long tick)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "expiredRequest", Long.valueOf(tick));
    expiredRequest(tick, false);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "expiredRequest");
  }

  public final void expiredRequest(long tick, boolean flushValue)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "expiredRequest",
      new Object[]{Long.valueOf(tick), Boolean.valueOf(flushValue)});

    TickRange r2 = null;
    boolean transitionToFlushed = false;
    synchronized (this)
    {
      stream.setCursor(tick);
      TickRange r = stream.getNext();
      byte type = r.type;

      if ((type == TickRange.Requested) || (type == TickRange.Unknown)
        || (flushValue && type==TickRange.Value))
      {
        // change to completed
        r2 = new TickRange(TickRange.Completed, tick, tick);
        stream.writeCompleted(r2);
        if (!active)
        {
          r2 = null;
        }

         transitionToFlushed = tryTransitionToFlushed();
      }
    } // end synchronized (this)

    if (r2 != null)
    {
      parent.sendCompleted(remoteMEUuid, gatheringTargetDestUuid, streamId, r2);
    }
    if (transitionToFlushed)
    {
      parent.sendFlushed(remoteMEUuid, gatheringTargetDestUuid, streamId);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "expiredRequest");

  }

  /**
   * Callback from JSRemoteConsumerPoint that the given tick in the stream should be changed to the value state.
   * The message has already been non-persistently locked.
   * @param tick The tick in the stream
   * @param msg The value
   */
  public final void satisfiedRequest(long tick, SIMPMessage msg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "satisfiedRequest", new Object[] { Long.valueOf(tick), msg });

    boolean sendMsg = false;
    // used to send a message outside the synchronized block
    long startstamp = tick;
    AOValue value = null;
    boolean transitionToFlushed = false;

    synchronized (this)
    {
      try
      {
        if (startedFlush || closed)
        { // unlock the message
          msg.unlockMsg(msg.getLockID(),null,true);
          // change this tick to final
          TickRange r = new TickRange(TickRange.Completed, tick, tick);
          stream.writeCompleted(r);
          transitionToFlushed = tryTransitionToFlushed();
        }
        else
        {

          stream.setCursor(tick);
          TickRange r = stream.getNext();
          if (r.type != TickRange.Requested)
          {
            // this tick is no longer requested
            // unlock the message
            msg.unlockMsg(msg.getLockID(),null,true);
          }
          else
          {
            AORequested requested = (AORequested) r.value;
            long waitTime = System.currentTimeMillis() - requested.startTime;
            if (msg
              .getReliability()
              .compareTo(Reliability.RELIABLE_NONPERSISTENT)
              <= 0)
            {
              // Note that this means that the message storage strategy in the MS is either
              // STORE_NEVER or STORE_MAYBE. In either case, we create an AOValue tick with
              // storage strategy equal to STORE_NEVER. In fact, we don't even add this AOValue
              // tick to an ItemStream. Also, we do not persistently lock the message

              int reliability = msg.getReliability().getIndex();
              int priority = msg.getMessage().getPriority().intValue();
              // message is already locked. change tick to value state

              value =
                new AOValue(
                  tick,
                  msg,
                  msg.getID(),
                  AbstractItem.STORE_NEVER,
                  0L,
                  waitTime,
                  latestTick[reliability][priority]);
              latestTick[reliability][priority] = tick; // update latestTick

              TickRange r2 = TickRange.newValueTick(tick, value, 0L);

              stream.writeCombinedRange(r2);
              r2 = stream.findCompletedRange(r2);
              // now start the DecisionExpected timer
              dem.addTimeoutEntry(value);
              // send message if active
              if (active)
              {
                sendMsg = true;
                startstamp = r2.startstamp;
              }
            }
            else
            {
              int storagePolicy;
              if (msg
                .getReliability()
                .compareTo(Reliability.ASSURED_PERSISTENT)
                < 0)
                storagePolicy = AbstractItem.STORE_EVENTUALLY;
              else
                storagePolicy = AbstractItem.STORE_ALWAYS;

              // change the AORequested tick to INSERTING state
               ((AORequested) r.value).inserting = true;

              // message needs to be persistently locked, and tick persisted
              int reliability = msg.getReliability().getIndex();
              int priority = msg.getMessage().getPriority().intValue();
              PersistLockAndTick update =
                new PersistLockAndTick(
                  tick,
                  msg,
                  storagePolicy,
                  waitTime,
                  latestTick[reliability][priority]);
              latestTick[reliability][priority] = tick; // update latestTick

              parent.getPersistLockThread().enqueueWork(update);
              countAsyncUpdatesOutstanding++;
            }
          }
        }
      }
      catch (MessageStoreException e)
      {
        // MessageStoreException shouldn't occur so FFDC.
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.AOStream.satisfiedRequest",
          "1:1058:1.80.3.24",
          this);

        SibTr.exception(tc, e);
      }
      catch (Exception e)
      {
        // probably a bug - log error
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.AOStream.satisfiedRequest",
          "1:1069:1.80.3.24",
          this);
        SIErrorException e2 =
          new SIErrorException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0002",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.AOStream",
                "1:1077:1.80.3.24",
                e },
              null));
        SibTr.exception(tc, e2);
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.AOStream",
            "1:1084:1.80.3.24",
            e });
      }
    } // end synchronized (this)

    if (sendMsg)
    {
      long prevTick = value.getPrevTick();
      parent.sendRemoteGetData(
        msg,
        remoteMEUuid,
        gatheringTargetDestUuid,
        streamId,
        prevTick,
        startstamp,
        tick,
        value.getWaitTime());

      msg.releaseJsMessage();
    }
    if (transitionToFlushed)
    {
      parent.sendFlushed(remoteMEUuid, gatheringTargetDestUuid, streamId);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "satisfiedRequest");

  }

  /**
   * Method to remove the given JSRemoteConsumerPoint from the consumerKeyTable
   * @param selector The string representation of the SelectionCriteria(s) of this JSRemoteConsumerPoint
   * @param aock The JSRemoteConsumerPoint to remove
   */
  public final void removeConsumerKey(String selector, JSRemoteConsumerPoint aock)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeConsumerKey", new Object[] { selector, aock });

    synchronized (this)
    {
      JSRemoteConsumerPoint aock2 = (JSRemoteConsumerPoint) consumerKeyTable.get(selector);
      if (aock2 == aock)
      { // the object is still in the table
        consumerKeyTable.remove(selector);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeConsumerKey");
  }

  /**
   * Class to asynchronously persist the lock and tick information for a message
   */
  class PersistLockAndTick extends AnycastOutputHandler.AsyncUpdateWithRetry
  {
    long tick;
    SIMPMessage msg;
    int storagePolicy;
    long waitTime;
    long prevTick;

    AOValue storedTick;

    public PersistLockAndTick(
      long tick,
      SIMPMessage msg,
      int storagePolicy,
      long waitTime,
      long prevTick)
    {
      super(
        SIMPConstants.INFINITE_TIMEOUT,
        parent.getPersistLockThread(), parent.getDestName());

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "PersistLockAndTick",
          new Object[] {
            Long.valueOf(tick),
            msg,
            Integer.valueOf(storagePolicy),
            Long.valueOf(waitTime),
            Long.valueOf(prevTick)});

      this.tick = tick;
      this.msg = msg;
      this.storagePolicy = storagePolicy;
      this.waitTime = waitTime;
      this.prevTick = prevTick;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "PersistLockAndTick", this);
    }

    public void execute(TransactionCommon t) throws Throwable
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "execute",
            new Object[] {t, "PersistLockAndTick",
            AOStream.this});
      
      storedTick =
        parent.persistLockAndTick(
          t,
          AOStream.this,
          tick,
          msg,
          storagePolicy,
          waitTime,
          prevTick);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "execute");
    }

    public void rolledback(Throwable e)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "rolledback",
            new Object[] {e, "PersistLockAndTick",
              AOStream.this});

      if (!rolledbackRetry(e))
      {
        // giving up
        synchronized (this)
        {
          countAsyncUpdatesOutstanding--;
        }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "rolledback");
    }

    public void committed()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "committed",
            new Object[] {"PersistLockAndTick",
              AOStream.this});

      TickRange r2 = null;
      boolean sendMsg = false;
      synchronized (AOStream.this)
      {
        countAsyncUpdatesOutstanding--;
        r2 = TickRange.newValueTick(tick, storedTick, 0L);

        stream.writeCombinedRange(r2);
        r2 = stream.findCompletedRange(r2);
        // now start the DecisionExpected timer
        dem.addTimeoutEntry(storedTick);

        // send message if active
        if (active)
        {
          sendMsg = true;
        }
      }
      if (sendMsg)
      {
        long prevTick = storedTick.getPrevTick();
        parent.sendRemoteGetData(
          msg,
          remoteMEUuid,
          gatheringTargetDestUuid,
          streamId,
          prevTick,
          r2.startstamp,
          tick,
          waitTime);

        msg.releaseJsMessage();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "committed");

    }
  }

  /**
   * Class to asynchronously unlock messages and remove the persistent tick information
   */
  class UnlockRejectedTicks extends AnycastOutputHandler.AsyncUpdateWithRetry
  {
    /** list of AOValue objects that have been persisted */
    java.util.ArrayList storedTicks;
    public UnlockRejectedTicks(java.util.ArrayList storedTicks)
    {
      super(
        SIMPConstants.INFINITE_TIMEOUT,
        msUpdateThread, parent.getDestName());

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "UnlockRejectedTicks", storedTicks);

      this.storedTicks = storedTicks;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "UnlockRejectedTicks", this);
    }

    public void execute(TransactionCommon t) throws Throwable
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "execute",
            new Object[] {t, "UnlockRejectedTicks",
              AOStream.this});

      for (int i = 0; i < storedTicks.size(); i++)
      {
        unlockRejectedTick(t, (AOValue) storedTicks.get(i));
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "execute");
    }

    public void committed()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "committed",
            new Object[] {"UnlockRejectedTicks",
              AOStream.this});

      storedTicksRemoved(storedTicks);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "committed");
    }

    public void rolledback(Throwable e)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "rolledback",
            new Object[] {e,"UnlockRejectedTicks",
              AOStream.this});

      if (!rolledbackRetry(e))
      {
        // giving up
        synchronized (this)
        {
          countAsyncUpdatesOutstanding--;
        }

      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "rolledback");
    }

  }

  /**
   * Class to asynchronously consume a message and remove the persistent tick information
   */
  class ConsumeAcceptedTicks extends AnycastOutputHandler.AsyncUpdateWithRetry
  {
    /** list of AOValue objects that have been persisted */
    java.util.ArrayList storedTicks;

    public ConsumeAcceptedTicks(java.util.ArrayList storedTicks)
    {
      super(
        SIMPConstants.INFINITE_TIMEOUT,
        msUpdateThread, parent.getDestName());

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "ConsumeAcceptedTicks", storedTicks);

      this.storedTicks = storedTicks;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "ConsumeAcceptedTicks", this);
    }

    public void execute(TransactionCommon t) throws Throwable
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "execute",
            new Object[] {t,
              "ConsumeAcceptedTicks",
              AOStream.this});

      for (int i = 0; i < storedTicks.size(); i++)
      {
        consumeAcceptedTick(
          t,
          (AOValue) storedTicks.get(i));
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "execute");
    }

    public void committed()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "committed",
            new Object[] {"ConsumeAcceptedTicks",
            AOStream.this});

      storedTicksRemoved(storedTicks);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "committed");
    }

    public void rolledback(Throwable e)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "rolledback",
            new Object[] {e, "ConsumeAcceptedTicks",
              AOStream.this});

      if (!rolledbackRetry(e))
      {
        // giving up
        synchronized (this)
        {
          countAsyncUpdatesOutstanding--;
        }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "rolledback");
    }

  }

  private void storedTicksRemoved(java.util.ArrayList storedTicks)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "storedTicksRemoved", storedTicks);

    java.util.ArrayList ranges = new java.util.ArrayList(storedTicks.size());
    TickRange r2 = null;
    boolean sendMsg = false;
    boolean transitionToFlushed = false;
    synchronized (this)
    {
      countAsyncUpdatesOutstanding--;
      if (active)
      {
        sendMsg = true;
      }
      for (int i = 0; i < storedTicks.size(); i++)
      {
        AOValue stick = (AOValue) storedTicks.get(i);
        dem.removeTimeoutEntry(stick);
        r2 =
          new TickRange(TickRange.Completed, stick.getTick(), stick.getTick());
        r2 = stream.writeCompleted(r2);
        ranges.add(r2);
      }
      // all ticks may be completed now, so set flushed to true etc.
      transitionToFlushed = tryTransitionToFlushed();
    } // end synchronized (this)
    if (sendMsg)
    {
      if (ranges.size() > 0)
        parent.sendCompleted(remoteMEUuid, gatheringTargetDestUuid, streamId, ranges);
      if (transitionToFlushed)
      {
        parent.sendFlushed(remoteMEUuid, gatheringTargetDestUuid, streamId);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "storedTicksRemoved");
  }

  public void processRequest(
    String[] discriminators,
    int[] selectorDomains,
    String[] selectors,
    long[] rejectStartTicks,
    long[] getTicks,
    long[] timeouts)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "processRequest",
        new Object[] { Arrays.toString(discriminators),
                       Arrays.toString(selectorDomains),
                       Arrays.toString(selectors),
                       Arrays.toString(rejectStartTicks),
                       Arrays.toString(getTicks),
                       Arrays.toString(timeouts) });

    // holds the AORequested ticks for which we should send a RequestAck message
    java.util.ArrayList requestAckList = null;

    // we assume that for new requests, the getTicks will typically be sorted in increasing order
    // if not, some requests will be immediately satisfied with completed ticks
    for (int i = 0; i < rejectStartTicks.length; i++)
    {
      long rejectStartTick = rejectStartTicks[i];
      long getTick = getTicks[i];
      long timeout = timeouts[i];

      // stuff for sending data msg outside synchronized block
      boolean sendDataMsg = false;
      AOValue value = null;
      SIMPMessage msg = null;
      long starttick = getTick;

      // stuff for sending completed msg outside synchronized block
      boolean sendCompletedMsg = false;
      TickRange completedRange = null;

      synchronized (this)
      {
        messagesReceived = true;
        // only process if the stream is active, and completedTicksInitialized is true, and ...
        if (completedTicksInitialized && active)
        {
          if ((getTick >= firstUnknownTick) && (!startedFlush))
          { // common case

            if (rejectStartTick < getTick)
            { // there are some rejected ticks
              if (rejectStartTick < firstUnknownTick)
              { // look at stuff in the range [rejectStartTick, firstUnknownTick-1].
                // Not common.
                // HACK: ignore the rejected ticks for now, since expect ControlReject messages for these
              }
            }
            try
            {
              // change the range [firstUnknownTick, getTick-1] to completed states
              if (firstUnknownTick < getTick)
              {
                completedRange =
                  new TickRange(
                    TickRange.Completed,
                    firstUnknownTick,
                    getTick - 1);
                stream.writeCompletedRange(completedRange);
              }
              starttick = firstUnknownTick;
              firstUnknownTick = getTick + 1;
              // change getTick to requested state, find or create appropriate ConsumerKey, and give it
              // this request. If message locked immediately, do further processing synchronously or
              // asynchronously

              JSRemoteConsumerPoint rcp = findOrCreateJSRemoteConsumerPoint(discriminators, selectorDomains, selectors);
              AORequestedTick aoTick = null;
              // Synchronize here to ensure a msg doesnt get given to/taken away from the
              // return aoTick value before we can call getMessage
              rcp.lock();
              try
              {
                aoTick = rcp.newRequest(getTick, timeout);
                msg = aoTick.getMessage();
              }
              finally
              {
                rcp.unlock();
              }
              
              totalReceivedRequests++;
              if (msg != null)
              {
                if (msg
                  .getReliability()
                  .compareTo(Reliability.RELIABLE_NONPERSISTENT)
                  <= 0)
                {
                  // Note that this means that the message storage strategy in the MS is either
                  // STORE_NEVER or STORE_MAYBE. In either case, we create an AOValue tick with
                  // storage strategy equal to STORE_NEVER. In fact, we don't even add this AOValue
                  // tick to an ItemStream. Also, we do not persistently lock the message

                  // message is already locked. just change tick in state stream to value state
                  int reliability = msg.getReliability().getIndex();
                  int priority = msg.getMessage().getPriority().intValue();

                  value =
                    new AOValue(
                      getTick,
                      msg,
                      msg.getID(),
                      AbstractItem.STORE_NEVER,
                      0L,
                      0L,
                      latestTick[reliability][priority]);
                  latestTick[reliability][priority] = getTick;
                  // update latestTick

                  TickRange r = TickRange.newValueTick(getTick, value, 0L);
                  stream.writeCombinedRange(r);
                  r = stream.findCompletedRange(r);
                  starttick = r.startstamp;
                  dem.addTimeoutEntry(value);

                  sendDataMsg = true;
                }
                else
                {
                  int storagePolicy;
                  if (msg
                    .getReliability()
                    .compareTo(Reliability.ASSURED_PERSISTENT)
                    < 0)
                    storagePolicy = AbstractItem.STORE_EVENTUALLY;
                  else
                    storagePolicy = AbstractItem.STORE_ALWAYS;

                  // change the tick in state stream to requested state, with inserting=true
                  AORequested requested =
                    new AORequested(rcp, System.currentTimeMillis(), timeout, getTick);
                  requested.inserting = true;
                  TickRange r =
                    new TickRange(TickRange.Requested, getTick, getTick);
                  r.value = requested;
                  stream.writeRange(r);
                  // schedule an asynchronous write
                  int reliability = msg.getReliability().getIndex();
                  int priority = msg.getMessage().getPriority().intValue();

                  PersistLockAndTick asyncUpdate =
                    new PersistLockAndTick(
                      getTick,
                      msg,
                      storagePolicy,
                      0L,
                      latestTick[reliability][priority]);
                  latestTick[reliability][priority] = getTick;
                  // update latestTick

                  parent.getPersistLockThread().enqueueWork(asyncUpdate);
                  countAsyncUpdatesOutstanding++;
                }
              } // end if (msg != null)
              else if (aoTick.timeout != 0L)
              { // no message available immediately, and the timeout is not 0,
                // change the tick in state stream to requested state.
                // When the timeout is 0, the tick will already have been changed to completed state.

                // NOTE: we do not send the RequestAck until the request is repeated, since it is possible
                // that this request will be satisfied before the eager get repetition interval
                AORequested requested =
                  new AORequested(
                    rcp,
                    System.currentTimeMillis(),
                    aoTick.timeout,
                    getTick);
                TickRange r =
                  new TickRange(TickRange.Requested, getTick, getTick);
                r.value = requested;
                stream.writeRange(r);
              }
            }
            catch (ClosedException e)
            {
              // No FFDC code needed
              // we can recover from this exception without restarting the ME
              // change the getTick to completed state
                TickRange r =
                  new TickRange(TickRange.Completed, getTick, getTick);
                stream.writeCompletedRange(r);

              // if ConsumerCardinality=1, start flushing this stream
              if (parent.getCardinalityOne())
              {
                processRequestFlush();
              }
            }
            catch (MessageStoreException e)
            {
              // serious error. should never occur!
              FFDCFilter.processException(
                e,
                "com.ibm.ws.sib.processor.impl.AOStream.processRequest",
                "1:1666:1.80.3.24",
                this);
              SibTr.exception(tc, e);
            }
            catch (SINotPossibleInCurrentConfigurationException e)
            {
              // serious error. should never occur!
              FFDCFilter.processException(
                e,
                "com.ibm.ws.sib.processor.impl.AOStream.processRequest",
                "1:1676:1.80.3.24",
                this);
              SibTr.exception(tc, e);
            }
          } // end if ((getTick >= firstUnknownTick) && (!startedFlush))
          else
          {
            // HACK: ignore the rejected ticks for now, since expect ControlReject messages for these

            // only look at the getTick
            stream.setCursor(getTick);
            TickRange r = stream.getNext();
            if (r.type == TickRange.Completed)
            {
              sendCompletedMsg = true;
              completedRange = r;
            }
            else if (r.type == TickRange.Value)
            {
                // send value message
                value = (AOValue) r.value;
                try
                {
                  msg = consumerDispatcher.getMessageByValue(value);
                }
                catch(SIResourceException e)
                {
                  // No FFDC code needed
                  // Treat exception as if we found no msg
                  SibTr.exception(tc, e);
                }

                if (msg!=null) // Ignore non restored msgs
                {
                  /**
                   * SIB0113
                   * If control msgs come in for a restored AOValue on an IME (message gathering)
                   * and we dont have the referenced msg then we ignore the control message until
                   * the refererence has been restored.
                   */
                  starttick = getTick;
                  sendDataMsg = true;
                }
            }
            else if (r.type == TickRange.Requested)
            {
              AORequested requested = (AORequested) r.value;
              // only send RequestAck if not already in the process of inserting value tick
              if (!requested.inserting)
              {
                long remainingExpiryInterval;
                if (requested.expiryInterval
                  == SIMPConstants.INFINITE_TIMEOUT)
                  remainingExpiryInterval = Long.MAX_VALUE;
                // effectively infinite
                else
                {
                  long elapsedInterval =
                    System.currentTimeMillis() - requested.startTime;
                  remainingExpiryInterval =
                    requested.expiryInterval - elapsedInterval;
                }
                if (remainingExpiryInterval
                  >= mp.getCustomProperties().get_slowed_get_request_interval())
                {
                  // need to send a RequestAck
                  if (requestAckList == null)
                  {
                    requestAckList = new java.util.ArrayList(getTicks.length);
                  }
                  requestAckList.add(requested);
                }
              }
            }
          } // end else
        } // end if (completedTicksInitialized && active && !startedFlush)
      } // end synchronized

      if (sendDataMsg)
      {
        // send value message with [starttick, getTick-1] in completed state, and getTick
        // in value state
        long prevTick = SIMPConstants.UNKNOWN_PREV_TICK;
        if (parent.getCardinalityOne())
          prevTick = value.getPrevTick();
        parent.sendRemoteGetData(
          msg,
          remoteMEUuid,
          gatheringTargetDestUuid,
          streamId,
          prevTick,
          starttick,
          getTick,
          value.getWaitTime());

        msg.releaseJsMessage();
      }
      if (sendCompletedMsg)
      {
        // send Completed message
        parent.sendCompleted(remoteMEUuid, gatheringTargetDestUuid, streamId, completedRange);
      }
    } // end for

    if (requestAckList != null)
    {
      long[] ralist = new long[requestAckList.size()];
      for (int i = 0; i < ralist.length; i++)
      {
        ralist[i] = ((AORequested) requestAckList.get(i)).tick;
      }
      parent.sendRequestAck(
          remoteMEUuid,
          gatheringTargetDestUuid,
        streamId,
        parent.dmeVersion,
        ralist);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processRequest");
  }

  public void processAccept(long[] tick)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processAccept", Arrays.toString(tick));

    java.util.ArrayList completedRanges = null;
    boolean transitionToFlushed = false;
    synchronized (this)
    {
      messagesReceived = true;
      // only process this message if the stream is active and completedTicksInitialized is true.
      if (completedTicksInitialized && active)
      {
        completedRanges =
          processDecisionInternal(tick, tick, null, ACCEPT_MSG, false);
        transitionToFlushed = tryTransitionToFlushed();
      }
    }
    if ((completedRanges != null) && completedRanges.size() != 0)
      parent.sendCompleted(remoteMEUuid, gatheringTargetDestUuid, streamId, completedRanges);
    if (transitionToFlushed)
    {
      parent.sendFlushed(remoteMEUuid, gatheringTargetDestUuid, streamId);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processAccept");
  }

  public void processReject(long[] startTick, long[] endTick, long[] unlockCount, boolean recovery)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "processReject",
        new Object[] { Arrays.toString(startTick), Arrays.toString(endTick),  Arrays.toString(unlockCount), Boolean.valueOf(recovery)});

    java.util.ArrayList completedRanges = null;
    boolean transitionToFlushed = false;
    synchronized (this)
    {
      messagesReceived = true;
      // only process this message if the stream is active and completedTicksInitialized is true.
      if (completedTicksInitialized && active)
      {
        completedRanges =
          processDecisionInternal(startTick, endTick, unlockCount, REJECT_MSG, recovery);
        transitionToFlushed = tryTransitionToFlushed();
      }
    }
    if ((completedRanges != null) && completedRanges.size() != 0)
      parent.sendCompleted(remoteMEUuid, gatheringTargetDestUuid, streamId, completedRanges);
    if (transitionToFlushed)
    {
      parent.sendFlushed(remoteMEUuid, gatheringTargetDestUuid, streamId);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processReject");
  }

  public void processCompleted(long[] startTick, long[] endTick)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processCompleted", new Object[] { Arrays.toString(startTick), Arrays.toString(endTick) });

    boolean transitionToFlushed = false;
    synchronized (this)
    {
      messagesReceived = true;
      // only process this message if the stream is active and completedTicksInitialized is true.
      if (completedTicksInitialized && active)
      {
        processDecisionInternal(startTick, endTick, null, COMPLETED_MSG, false);
        transitionToFlushed = tryTransitionToFlushed();
      }
    }

    if (transitionToFlushed)
    {
      parent.sendFlushed(remoteMEUuid, gatheringTargetDestUuid, streamId);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processCompleted");
  }

  // these are only constants used within this class.
  private static final int ACCEPT_MSG = 1;
  private static final int REJECT_MSG = 2;
  private static final int COMPLETED_MSG = 3;

  private java.util.ArrayList processDecisionInternal(
    long[] startTick,
    long[] endTick,
    long[] rejectUnlockCount,
    int type,
    boolean recovery)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "processDecisionInternal",
        new Object[] {
          Arrays.toString(startTick),
          Arrays.toString(endTick),
          Arrays.toString(rejectUnlockCount),
          Integer.valueOf(type),
          Boolean.valueOf(recovery)});

    ArrayList<AOValue> asyncTicksToRemove = new ArrayList<AOValue>(startTick.length); // list of AOValue objects
    ArrayList<AOValue> syncTicksToRemove = new ArrayList<AOValue>(startTick.length); // ''
    ArrayList<TickRange> completedRanges = new ArrayList<TickRange>(startTick.length);

    // iterate through all the tick ranges
    // will change following ticks immediately:
    // unknown -> completed
    // value -> completed if value not assured and not in REMOVING state
    // requested -> completed if not in INSERTING state, and will also cancel the request to ConsumerKey
    // the following transition will be scheduled asynchronously:
    // value -> completed if value is assured and not in REMOVING state
    for (int i = 0; i < startTick.length; i++)
    {
      // position the cursor
      stream.setCursor(startTick[i]);
      TickRange r1 = stream.getNext();
      TickRange r2 = r1;
      TickRange writeRange = null;
      // contains the latest range of completed ticks to be written
      while (r1.startstamp <= endTick[i])
      {
        if ((r1.type == TickRange.Unknown) || (r1.type == TickRange.Completed))
        { // can include req in writeRange
          writeRange =
            startOrIncludeInWriteRange(
              writeRange,
              startTick[i],
              endTick[i],
              r1);
        }
        else if (r1.type == TickRange.Requested)
        {
          AORequested req = (AORequested) r1.value;
          if (req.inserting)
          {
            // cannot include req in the writeRange
            if (writeRange != null)
            {
              completedRanges.add(writeRange);
              writeRange = null;
            }
          }
          else
          { // can include req in writeRange
            writeRange =
              startOrIncludeInWriteRange(
                writeRange,
                startTick[i],
                endTick[i],
                r1);
            // cancel the request to ConsumerKey
            req.aock.cancelRequest(r1.startstamp);
          }
        }
        else if (r1.type == TickRange.Value)
        {
          AOValue value = (AOValue) r1.value;
          if (value.removing)
          {
            // cannot include req in the writeRange
            if (writeRange != null)
            {
              completedRanges.add(writeRange);
              writeRange = null;
            }
          }
          else if (
            (value.getStorageStrategy() == AbstractItem.STORE_ALWAYS)
              || (value.getStorageStrategy() == AbstractItem.STORE_EVENTUALLY))
          {
            /**
             * SIB0113
             * If control msgs come in for a restored AOValue on an IME (message gathering)
             * and we dont have the referenced msg then we ignore the control message until
             * the refererence has been restored.
             */
            if (value.isRestored())
            {
              value.removing = true;
              // From V7 onwards we flow the unlock count from the RME and use that to adjust
              // the unlock count of the actual message on the DME.
              // If the remote getter's ME is on a pre-V7 node then no unlock count is flowed
              // so we have to put up with assuming a value of one
              if (type == AOStream.REJECT_MSG)
              {
                if((rejectUnlockCount != null) &&
                   (rejectUnlockCount.length > i))
                  value.rmeUnlockCount = rejectUnlockCount[i];
                else
                  value.rmeUnlockCount = 1;
              }
                  
              // schedule asynchronous removal
              asyncTicksToRemove.add(value);
            }
            // cannot include req in the writeRange
            if (writeRange != null)
            {
              completedRanges.add(writeRange);
              writeRange = null;
            }
          }
          else if (value.getStorageStrategy() == AbstractItem.STORE_NEVER)
          {
            // Note that this corresponds to a message that is either STORE_NEVER or STORE_MAYBE,
            // since we create a STORE_NEVER AOValue in either case
            // can remove immediately
            syncTicksToRemove.add(value);
            // can include req in writeRange
            writeRange =
              startOrIncludeInWriteRange(
                writeRange,
                startTick[i],
                endTick[i],
                r1);
          }
        } // end else if (r1.getType() == TickRange.Value)

        r2 = r1;
        r1 = stream.getNext();
        if (r1 == r2) // reached end of stream
          break; // break out of while loop
      } // end while (r1.startstamp <= endTick[i])
      if (firstUnknownTick <= endTick[i])
        firstUnknownTick = endTick[i] + 1;
      if (writeRange != null)
      {
        completedRanges.add(writeRange);
        writeRange = null;
      }
    } // end for

    // write all the collected completed ranges
    doWriteCompletedRanges(completedRanges);

    // schedule asyncTicksToRemove
    if (asyncTicksToRemove.size() > 0)
    {
      if (type == AOStream.ACCEPT_MSG)
      {
        ConsumeAcceptedTicks asyncUpdate =
          new ConsumeAcceptedTicks(asyncTicksToRemove);
        doEnqueueWork(asyncUpdate);
      }
      else if (type == AOStream.REJECT_MSG)
      {
        UnlockRejectedTicks asyncUpdate =
          new UnlockRejectedTicks(asyncTicksToRemove);
        doEnqueueWork(asyncUpdate);
      }
      else if (type == AOStream.COMPLETED_MSG)
      {
        // TODO - don't have a way to tell ConsumerDispatcher that these should be potential duplicates
        // so just unlock it, for now.
        UnlockRejectedTicks asyncUpdate =
          new UnlockRejectedTicks(asyncTicksToRemove);
        doEnqueueWork(asyncUpdate);
      }
    }

    // do syncTicksToRemove
    if (syncTicksToRemove.size() > 0)
    {
      if (type == AOStream.ACCEPT_MSG)
      {
        // use a local transaction to consume all these messages
        consumeNonPersistentMessages(syncTicksToRemove);
      }
      else if (type == AOStream.REJECT_MSG)
      {
        if (recovery)
        { // rme recovered, so these messages may already have been consumed remotely.
          // use a local transaction to consume all these messages
          consumeNonPersistentMessages(syncTicksToRemove);
        }
        else
        {
          // unlock all these messages
          unlockNonPersistentMessages(syncTicksToRemove);
        }
      }
      else if (type == AOStream.COMPLETED_MSG)
      {
        // TODO - don't have a way to tell ConsumerDispatcher that these should be potential duplicates
        // so just unlock it, for now.

        // unlock all these messages
        unlockNonPersistentMessages(syncTicksToRemove);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processDecisionInternal", completedRanges);

    return completedRanges;
  }

  /*
   * synchronously remove a value tick from the stream. 
   */
  public synchronized void syncRemoveValueTick(long tick, Transaction msTran, SIMPMessage msgToDiscard) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "syncRemoveValueTick",
        new Object[] { Long.valueOf(tick), msTran });

    AOValue value = null;
    boolean transitionToFlushed;
    TickRange r2;
    
    stream.setCursor(tick);
    TickRange r = stream.getNext();
  
    if (r.type == TickRange.Value)
    {
      value = (AOValue) r.value;
      if (value.removing)
      {
        // already being removed so do nothing
        value = null;
      }
      else
      {
        value.removing = true;
      }
    }
    
    r2 = new TickRange(TickRange.Completed, tick, tick);
    r2 = stream.writeCompleted(r2);
    if (!active)
      r2 = null;

    // all ticks may be completed now, so set flushed to true etc.
    transitionToFlushed = tryTransitionToFlushed();
    
    
    
    if (value != null)
    {
      dem.removeTimeoutEntry(value);
      try
      {
        if ((value.getStorageStrategy() == AbstractItem.STORE_ALWAYS)
        || (value.getStorageStrategy() == AbstractItem.STORE_EVENTUALLY))
        {
          // PK67067 We may not find a message in the store for this tick, because
          // it may have been removed using the SIBQueuePoint MBean
          if (msgToDiscard != null && msgToDiscard.isInStore())
            msgToDiscard.remove(msTran, value.getPLockId());
          value.remove(msTran, AbstractItem.NO_LOCK_ID);
        }
        else
        {
          // PK67067 We may not find a message in the store for this tick, because
          // it may have been removed using the SIBQueuePoint MBean
          if (msgToDiscard != null && msgToDiscard.isInStore()) 
            msgToDiscard.remove(msTran, msgToDiscard.getLockID());
        }

      }
      catch (MessageStoreException e)
      {
        // FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.AOStream.syncRemoveValueTick",
          "1:2176:1.80.3.24",
          this);            
        SibTr.exception(tc, e);        
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "syncRemoveValueTick", e);
        
        throw new SIResourceException(e);
      }
        
      if (r2 != null)
      {
        parent.sendCompleted(remoteMEUuid, gatheringTargetDestUuid, streamId, r2);
      }
      if (transitionToFlushed)
      {
        parent.sendFlushed(remoteMEUuid, gatheringTargetDestUuid, streamId);
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "syncRemoveValueTick");
  }

  public void processHighestGeneratedTick(long requestId, long tick)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "processHighestGeneratedTick",
        new Object[] { Long.valueOf(requestId), Long.valueOf(tick)});

    synchronized (this)
    {
      if (requestId == initRequestId)
      {
        // the id is the same as in our request
        if (!completedTicksInitialized)
        {
          changeUnknownToCompleted(0, tick);
          firstUnknownTick = tick + 1; // as tick >= 0, firstUnknownTick > 0
          // for sanity, make sure that the highestGeneratedTick makes sense
          if (tick < initHighestValueTick)
          {
            // log serious error
            SIErrorException e =
              new SIErrorException(
                nls.getFormattedMessage(
                  "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                  new Object[] {
                    "com.ibm.ws.sib.processor.impl.AOStream",
                    "1:2226:1.80.3.24" },
                  null));

            FFDCFilter.processException(
              e,
              "com.ibm.ws.sib.processor.impl.AOStream.processHighestGeneratedTick",
              "1:2232:1.80.3.24",
              this);
            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.AOStream",
                "1:2238:1.80.3.24" });
          }
          else
          {
            completedTicksInitialized = true;
            if (initRepeatHandler != null)
            {
              initRepeatHandler.cancel();
              initRepeatHandler = null;
            }
          }
        } // end if (!completedTicksInitialized)
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processHighestGeneratedTick");
  }

  public void processAreYouFlushed(long requestId, SIBUuid12 parentDest)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processAreYouFlushed", new Object[] {new Long(requestId), parentDest});

    boolean sendFlushed = false;

    boolean sendNotFlushed = false;
    long completedPrefix = 0L;
    long duplicatePrefix = 0L;

    synchronized (this)
    {
      // only process this message if the stream is active
      if (active)
      {
        if (isFlushed)
        {
          sendFlushed = true;
        }
        else
        {
          sendNotFlushed = true;
          if (completedTicksInitialized)
          {
            completedPrefix = stream.getCompletedPrefix();
            duplicatePrefix = firstUnknownTick - 1;
          }
          else
          {
            completedPrefix = 0L; // can always set a value < the most accurate completed prefix
            duplicatePrefix = 0L; // duplicatePrefix is ignored at the RME, so anything is ok
          }
        }
      }
    }

    if (sendFlushed)
      parent.sendFlushed(remoteMEUuid, gatheringTargetDestUuid, streamId);
    else if (sendNotFlushed)
      parent.sendNotFlushed(
          remoteMEUuid,
          gatheringTargetDestUuid,
        streamId,
        requestId,
        completedPrefix,
        duplicatePrefix,
        parentDest);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processAreYouFlushed");
  }

  public void processRequestFlush()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processRequestFlush");

    boolean sendFlushed = false;
    synchronized (this)
    {
      if (startedFlush)
      {
        if (isFlushed && active)
          sendFlushed = true;
        else
          sendFlushed = tryTransitionToFlushed();
      }
      else if (!scheduleWriteStartedFlush)
      {
        scheduleWriteStartedFlush = true;
        WriteStartedFlush update = new WriteStartedFlush();
        doEnqueueWork(update);
      }
    }
    if (sendFlushed)
      parent.sendFlushed(remoteMEUuid, gatheringTargetDestUuid, streamId);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processRequestFlush");
  }

  public void close()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "close");
    synchronized (this)
    {
      closeInternal();

      // unlock all the non-persistent locks
      stream.setCursor(0L);
      TickRange r1 = stream.getNext();
      TickRange r2 = r1;
      ArrayList syncTicksToRemove = new ArrayList();
      while (true)
      {
        if (r1.type == TickRange.Value)
        {
          AOValue value = (AOValue) r1.value;
          if (value.getStorageStrategy() == AbstractItem.STORE_NEVER)
          {
            // Note that this corresponds to a message that is either STORE_NEVER or STORE_MAYBE,
            // since we create a STORE_NEVER AOValue in either case
            // can remove immediately
            syncTicksToRemove.add(value);
          }
        } // end else if (r1.getType() == TickRange.Value)

        r2 = r1;
        r1 = stream.getNext();
        if (r1 == r2) // reached end of stream
          break; // break out of while loop
      } // end while (true)
      unlockNonPersistentMessages(syncTicksToRemove);

      // lastly, wait till all asynchronous writes that have been scheduled are completed
      if (countAsyncUpdatesOutstanding != 0)
      {
        // this is very rare, since close() is very rare.
        // therefore, for performance reasons i prefer to do a 'busy wait' rather than add notify() calls
        // to all the places where countAsyncUpdatesOutstanding is decremented (which is very common).

        // wait for 60 seconds maximum
        long sleepTime = 5000;
        long maxWaitTime = 60000;
        int sleepCount = (int) (maxWaitTime/sleepTime);
        int i = 0;
        while ((countAsyncUpdatesOutstanding != 0) && (i < sleepCount))
        {
          try
          {
            Thread.sleep(sleepTime);
          }
          catch (InterruptedException e)
          {
            // No FFDC code needed
          }
          i++;
        }

        if (countAsyncUpdatesOutstanding != 0)
        {
          // should throw SIMPRuntimeException
          SIErrorException e =
            new SIErrorException(
              nls.getFormattedMessage(
                "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                new Object[] {
                  "com.ibm.ws.sib.processor.impl.AOStream",
                  "1:2407:1.80.3.24" },
                null));
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.AOStream.close",
            "1:2412:1.80.3.24",
            this);
          SibTr.exception(tc, e);
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.AOStream",
              "1:2418:1.80.3.24" });
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "close");

          throw e;
        }
      } // end if (countAsyncUpdatesOutstanding != 0)
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "close");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.Stream#getID()
   */
  public String getID()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getID");
    String id = streamId.toString();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getID", id);
    return id;
  }

  /**
   * Counts the number of requests that have been completed since reboot
   * @return
   * @author tpm
   */
  public synchronized long getNumberOfRequestsInState(int requiredState)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getNumberOfRequestsInState", Integer.valueOf(requiredState));

    //Count the number of tick range objects that are in the
    //specified state
    long requestCount=0;

    // Initial range in stream is always completed Range
    stream.setCursor(0);
    // skip this and move to next range
    stream.getNext();
    // Get the first TickRange after completed range and move cursor to the next one
    TickRange tr = stream.getNext();
    // Iterate until we reach final Unknown range
    while (tr.endstamp < RangeList.INFINITY)
    {
      if((tr.type == requiredState) )
      {
        requestCount++;
      }
      tr = stream.getNext();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getNumberOfRequestsInState", Long.valueOf(requestCount));
    return requestCount;
  }

  public long getDMEVersion()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getDMEVersion");
      SibTr.exit(tc, "getDMEVersion", Long.valueOf(dmeVerion));
    }
    return this.dmeVerion;
  }
  private void writtenStartedFlush()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "writtenStartedFlush");

    boolean sendFlushed = false;
    synchronized (this)
    {
      if (!startedFlush)
      {
        startedFlush = true;

        completedTicksInitialized = true; // set to true in case it was false
        if (initRepeatHandler != null)
        {
          initRepeatHandler.cancel();
          initRepeatHandler = null;
        }
        firstUnknownTick = Long.MAX_VALUE;

        // close all the JSRemoteConsumerPoints if ConsumerCardinality>1 or RME is unreachable
        // otherwise we will wait till the flush is complete (unless the RME becomes unreachable before the flush
        // is complete)
        if (!parent.getCardinalityOne() || !parent.isMEReachable(remoteMEUuid))
        {
          Enumeration vEnum = consumerKeyTable.elements();
          while (vEnum.hasMoreElements())
          {
            JSRemoteConsumerPoint aock = (JSRemoteConsumerPoint) vEnum.nextElement();
            aock.close();
          }
          consumerKeyTable.clear();
        }
        // change all unknown ticks to completed
        changeUnknownToCompleted(0, Long.MAX_VALUE);
        // all the ticks may have turned to completed state
        sendFlushed = tryTransitionToFlushed();

      }
    } // end synchronized
    if (sendFlushed)
      parent.sendFlushed(remoteMEUuid, gatheringTargetDestUuid, streamId);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "writtenStartedFlush");
  }

  /**
   * Class to asynchronously write startedFlush
   */
  class WriteStartedFlush extends AnycastOutputHandler.AsyncUpdateWithRetry
  {
    Item startedFlushItem = null;

    WriteStartedFlush()
    {
      super(
        SIMPConstants.INFINITE_TIMEOUT,
        msUpdateThread, parent.getDestName());

      // intentionally empty

    }

    public void execute(TransactionCommon t) throws Throwable
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "execute",
            new Object[] {t,"WriteStartedFlush",
              AOStream.this});

      startedFlushItem = parent.writeStartedFlush(t, AOStream.this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "execute");
    }


    public void rolledback(Throwable e)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "rolledback",
            new Object[] {e,"WriteStartedFlush",
              AOStream.this});

      if (!rolledbackRetry(e))
      {
        // giving up
        synchronized (this)
        {
          countAsyncUpdatesOutstanding--;
        }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "rolledback");
    }

    public void committed()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "committed",
            new Object[] {"WriteStartedFlush",
              AOStream.this});

      synchronized (this)
      {
        countAsyncUpdatesOutstanding--;
      }
      parent.writtenStartedFlush(AOStream.this, startedFlushItem);
      writtenStartedFlush();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "committed");
    }
  }

  public void processResetRequestAckAck()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processResetRequestAckAck");

    synchronized (this)
    {
      if (resetRequestAckSender != null)
      {
        resetRequestAckSender.stop();
        resetRequestAckSender = null;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processResetRequestAckAck");
  }

  /**
   * Change all unknown ticks in the range [startTick, endTick] to completed state.
   * Called from withing a synchronized (this) block.
   * @param startTick
   * @param endTick
   */
  private void changeUnknownToCompleted(long startTick, long endTick)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "changeUnknownToCompleted",
        new Object[] { Long.valueOf(startTick), Long.valueOf(endTick)});

    // position the cursor
    stream.setCursor(startTick);
    TickRange r1 = stream.getNext();
    TickRange r2 = r1;
    TickRange writeRange = null;
    java.util.ArrayList list = new ArrayList(5); // arbitrary size. this code is not on the usual processing path.
    // contains the latest range of completed ticks to be written
    while (r1.startstamp <= endTick)
    {
      if (r1.type == TickRange.Unknown)
      { // can include req in writeRange
        writeRange =
          startOrIncludeInWriteRange(writeRange, startTick, endTick, r1);
      }
      else
      {
        // cannot include req in the writeRange
        if (writeRange != null)
        {
          list.add(writeRange);
          writeRange = null;
        }
      }
      r2 = r1;
      r1 = stream.getNext();
      if (r1 == r2) // reached end of stream
        break; // break out of while loop
    } // end while (r1.startstamp <= endTick[i])
    if (writeRange != null)
    {
      list.add(writeRange);
      writeRange = null;
    }
    doWriteCompletedRanges(list);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "changeUnknownToCompleted");
  }

  /**
   * Helper function. Writes ranges of completed ticks and updates them to include the expanded range
   * Called from withing a synchronized (this) block.
   * @param completedRanges List of TickRange objects representing ranges to write
   */
  private final void doWriteCompletedRanges(List completedRanges)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "doWriteCompletedRanges",
        new Object[] { completedRanges });

    int length = completedRanges.size();
    for (int i=0; i<length; i++)
    {
      completedRanges.set(i, stream.writeCompletedRange((TickRange) completedRanges.get(i)));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "doWriteCompletedRanges");
  }

  // always called from within a synchronized (this) block
  private final void doEnqueueWork(AsyncUpdate asyncUpdate)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "doEnqueueWork", asyncUpdate);

    try
    {
      msUpdateThread.enqueueWork(asyncUpdate);
      countAsyncUpdatesOutstanding++; // can do it here since synchronized on this
    }
    catch (ClosedException e)
    {
      // should not occur!
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AOStream.doEnqueueWork",
        "1:2713:1.80.3.24",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "doEnqueueWork");
  }

  /**
   * Helper method. We are given a range, writeRange, which may be null. We are
   * also given a range r (which is not null), that needs to be included in writeRange.
   * The constraint on the inclusion is the resulting writeRange should be in the interval
   * [lowerBound, upperBound].
   * @param writeRange
   * @param lowerBound
   * @param upperBound
   * @param r
   * @return The resulting writeRange
   */
  private final TickRange startOrIncludeInWriteRange(
    TickRange writeRange,
    long lowerBound,
    long upperBound,
    TickRange r)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "startOrIncludeInWriteRange");

    if (writeRange == null)
    {
      writeRange = new TickRange(TickRange.Completed, 0L, 0L);
      writeRange.startstamp = max(r.startstamp, lowerBound);
      writeRange.endstamp = min(r.endstamp, upperBound);
    }
    else
    {
      writeRange.endstamp = min(r.endstamp, upperBound);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "startOrIncludeInWriteRange", writeRange);

    return writeRange;
  }

  /**
   * Helper method. Consumes STORE_NEVER Items synchronously.
   * Called from withing a synchronized (this) block.
   * @param consumeList A list of AOValue objects
   */
  private final void consumeNonPersistentMessages(java.util.ArrayList consumeList)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "consumeNonPersistentMessages", consumeList);

    // use a local transaction to consume all these messages
    LocalTransaction tran = parent.getLocalTransaction();
    try
    {
      Transaction msTran = parent.getMessageProcessor().resolveAndEnlistMsgStoreTransaction(tran);
      int length = consumeList.size();
      for (int i = 0; i < length; i++)
      {
        AOValue value = (AOValue) consumeList.get(i);
        dem.removeTimeoutEntry(value);
        SIMPMessage msgItem = consumerDispatcher.getMessageByValue(value);

        // PK67067 We may not find a message in the store for this tick, because
        // it may have been removed using the SIBQueuePoint MBean
        if (msgItem != null) msgItem.remove(msTran, msgItem.getLockID());
      }
      tran.commit();
    }
    catch (Exception e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AOStream.consumeNonPersistentMessages",
        "1:2792:1.80.3.24",
        this);
      SibTr.exception(tc, e);
      
      // No matter what happens above we try to commit the transaction as we're simply
      // trying to get rid of these messages and there's no point keeping them in
      // the case of a failure
      try
      {
        tran.commit();
      }
      catch (SIException e2)
      {
        // No FFDC code needed
        
        // We really don't care about a problem in this commit
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "commit failed " + e2);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "consumeNonPersistentMessages");
  }

  /**
   * Helper method. Unlocks STORE_NEVER Items synchronously.
   * Called from withing a synchronized (this) block.
   * @param unlockList A list of AOValue objects
   */
  private final void unlockNonPersistentMessages(java.util.ArrayList unlockList)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "unlockNonPersistentMessages", unlockList);

    try
    {
      int length = unlockList.size();
      for (int i = 0; i < length; i++)
      {
        AOValue value = (AOValue) unlockList.get(i);
        dem.removeTimeoutEntry(value);
        SIMPMessage msgItem = consumerDispatcher.getMessageByValue(value);

        // PK67067 We may not find a message in the store for this tick, because
        // it may have been removed using the SIBQueuePoint MBean
        if (msgItem != null) msgItem.unlockMsg(msgItem.getLockID(),null, true);
      }
    }
    catch (Exception e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AOStream.unlockNonPersistentMessages",
        "1:2846:1.80.3.24",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "unlockNonPersistentMessages");
  }

  /**
   * Helper method.
   * Called from withing a synchronized (this) block.
   * @param discriminators
   * @param selectorDomains
   * @param selectors
   * @return
   */
  private final JSRemoteConsumerPoint findOrCreateJSRemoteConsumerPoint(String[] discriminators, int[] selectorDomains,
                                                        String[] selectors)
    throws ClosedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "findOrCreateJSRemoteConsumerPoint",
          new Object[]{Arrays.toString(discriminators),
                       Arrays.toString(selectorDomains),
                       Arrays.toString(selectors)});

    String selectionCriteriasAsString = parent.convertSelectionCriteriasToString(
                                                  discriminators, selectorDomains, selectors);

    JSRemoteConsumerPoint aock = (JSRemoteConsumerPoint) consumerKeyTable.get(selectionCriteriasAsString);
    if (aock == null)
    {
      try
      {
        // create an JSRemoteConsumerPoint
        aock = new JSRemoteConsumerPoint();
        SelectionCriteria[] criterias = new SelectionCriteria[discriminators.length];
        ConsumableKey[] consumerKeys = new ConsumableKey[discriminators.length];
        OrderingContextImpl ocontext = null;
        SIBUuid12 connectionUuid = new SIBUuid12();

        if (discriminators.length > 1)
          ocontext = new OrderingContextImpl(); // create a new ordering context
        for (int i=0; i < discriminators.length; i++)
        {
          SelectorDomain domain = SelectorDomain.getSelectorDomain(selectorDomains[i]);
          criterias[i] = parent.createSelectionCriteria(discriminators[i], selectors[i], domain);

          // attach as many times as necessary
          consumerKeys[i] =
            (ConsumableKey) consumerDispatcher.attachConsumerPoint(
              aock,
              criterias[i],
              connectionUuid,
              false,
              false,
              null);

          if (ocontext != null)
            consumerDispatcher.joinKeyGroup(consumerKeys[i], ocontext);

          consumerKeys[i].start(); // in case we use a ConsumerKeyGroup, this is essential

        }

        if (parent.getCardinalityOne() || consumerDispatcher.isPubSub())
        {
          // effectively infinite timeout, since don't want to close the ConsumerKey if RME is inactive for
          // a while. Only close this ConsumerKey when start flushing this stream.
          // NOTE shared durable subs might not be cardinality one but we still do not want the streams
          // to flush on timeout
          //
          // Defect 516583, set the idleTimeout parameter to 0, not to Long.MAX_VALUE in order to have an
          // "infinite timeout".
          aock.init(this, selectionCriteriasAsString, consumerKeys, 0, am, criterias);
        }
        else
        {
          aock.init(
            this,
            selectionCriteriasAsString,
            consumerKeys,
            mp.getCustomProperties().get_ck_idle_timeout(),
            am,
            criterias);
        }
        consumerKeyTable.put(selectionCriteriasAsString, aock);
      }
      catch (Exception e)
      {
        // should not occur!
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.AOStream.findOrCreateJSRemoteConsumerPoint",
          "1:2942:1.80.3.24",
          this);
        SibTr.exception(tc, e);

        aock = null;
        ClosedException e2 = new ClosedException(e.getMessage());
        // just using the ClosedException as a convenience
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "findOrCreateJSRemoteConsumerPoint", e2);

        throw e2;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "findOrCreateJSRemoteConsumerPoint", aock);

    return aock;
  }

  /**
   * Tries to transition stream to flushed state (isFlushed = true) if the stream has already started being flushed.
   * Must be called from within synchronized (this)
   * @return true if this method set isFlushed to true and the stream was active when this method was called.
   */
  private final boolean tryTransitionToFlushed()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "tryTransitionToFlushed");

    boolean ret = false;
    if (startedFlush)
    {
      // check if completed flush
      if (!isFlushed && stream.getCompletedPrefix() == Long.MAX_VALUE)
      {
        isFlushed = true;
        ret = active;
        closeInternal();
        parent.streamIsFlushed(this);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "tryTransitionToFlushed", Boolean.valueOf(ret));

    return ret;
  }

  /**
   * Must be called from within synchronized (this)
   */
  private final void closeInternal()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "closeInternal");

    stop(); // stop the stream
    dem.close();
    closed = true;

    // close all the JSRemoteConsumerPoints.
    Enumeration vEnum = consumerKeyTable.elements();
    while (vEnum.hasMoreElements())
    {
      JSRemoteConsumerPoint aock = (JSRemoteConsumerPoint) vEnum.nextElement();
      aock.close();
    }
    consumerKeyTable.clear();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "closeInternal");
  }

  /**
   * Maximum value of a and b
   */
  private final static long max(long a, long b)
  {
    return (a > b ? a : b);
  }
  /**
   * Minimum value of a and b
   */
  private final static long min(long a, long b)
  {
    return (a > b ? b : a);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.Stream#getCompletedPrefix()
   */
  public long getCompletedPrefix()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getCompletedPrefix");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getCompletedPrefix", Long.valueOf(stream.getCompletedPrefix()));

    return stream.getCompletedPrefix();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.ControllableStream#getPriority()
   */
  protected int getPriority()
  {
    // Empty
    return 0;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.ControllableStream#getReliability()
   */
  protected Reliability getReliability()
  {
    // Empty
    return null;
  }

  public SIBUuid8 getRemoteMEUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getRemoteMEUuid");
      SibTr.exit(tc, "getRemoteMEUuid", remoteMEUuid);
    }
    return remoteMEUuid;
  }

  public SIBUuid12 getGatheringTargetDestUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "gatheringTargetDestUuid");
      SibTr.exit(tc, "gatheringTargetDestUuid", gatheringTargetDestUuid);
    }
    return gatheringTargetDestUuid;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.Stream#getStateStream()
   */
  public StateStream getStateStream()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getStateStream");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getStateStream", stream);

    return stream;
  }

  public AOStreamState getStreamState()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getStreamState");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getStreamState", streamState);

    return streamState;
  }

  public synchronized long getTotalRequestsReceived()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getTotalRequestsReceived");
      SibTr.exit(tc, "getTotalRequestsReceived");
    }
    return this.totalReceivedRequests;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.Stream#writeSilenceForced(long)
   */
  public void writeSilenceForced(long tick)
  {
    // Empty
  }

  /**
   * An enumeration for the possible states for this stream
   * @author tpm100
   */
  public static class AOStreamState implements SIMPDeliveryTransmitControllable.StreamState
  {
    public static final AOStreamState ACTIVE = new AOStreamState("Active", 1);
    private String name;
    private int id;
    private AOStreamState(String _name, int _id)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "AOStreamState",
        new Object[]{_name, Integer.valueOf(_id)});

      name = _name;
      id = _id;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "AOStreamState", this);
    }
    public String toString()
    {
      return name;
    }

    public int getValue()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.entry(tc, "getValue");
        SibTr.exit(tc, "getValue", Integer.valueOf(id));
      }
      return id;
    }

  }

  /**
   * Helper method called by the AOStream when a persistent tick representing a persistently locked
   * message should be removed since the message has been rejected. This method will also unlock the
   * message
   * @param t the transaction
   * @param stream the stream making this call
   * @param storedTick the persistent tick
   * @throws SIResourceException
   * @throws Exception
   */
  public final void unlockRejectedTick(TransactionCommon t, AOValue storedTick) throws MessageStoreException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "unlockRejectedTick");

    try {
      SIMPMessage msg =
        consumerDispatcher.getMessageByValue(storedTick);

      Transaction msTran = mp.resolveAndEnlistMsgStoreTransaction(t);
      
      if(msg != null)
      {
        // Set the rme unlock count into the Messageitem so it can be taken into
        // account on further rollback threshold checks. (Note we take one off
        // because the following operation will perform an unlock itself)
        // 488794.
    	boolean incrementCount = false;

        // TODO: THIS DOESN'T WORK - MsgStore does not support the use of 'don't increment count'
        // when unlocking a persistently locked message (i.e. one of these). So the count will
        // get incremented anyway. So if the message has been rejected because it was never consumed
        // (and therefore, shouldn't have the count incremented) the unlock count WILL be incremented
        // which could cause us to exception it.
    	
    	// TODO: However, this is still an improvement on the pre-V7 logic and not a regression
    	// so we'll leave it as is unless someone complains.
        
        // TODO: I also doubt all this logic as the rmeUnlockCount is only transient, if MsgStore
        // chooses to un-cache this message we'll lose the extra count info. Although I guess it does
        // solve the case when the RME has rolled it back enough to reach the max failure limit, so I
        // guess we leave it as is.
    	if(storedTick.rmeUnlockCount > 0)
    	{
          incrementCount = true;
          msg.setRMEUnlockCount(storedTick.rmeUnlockCount - 1);
    	}
        if (msg.getLockID()==storedTick.getPLockId())
          msg.unlockMsg(storedTick.getPLockId(), msTran, incrementCount);
      }
      storedTick.lockItemIfAvailable(controlItemLockID); // should always be successful
      storedTick.remove(msTran, controlItemLockID);
    }
    catch (MessageStoreException e)
    {
      // No FFDC code needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "unlockRejectedTick", e);

      throw e;
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "unlockRejectedTick");
  }

  /**
   * Helper method called by the AOStream when a persistent tick representing a persistently locked
   * message should be removed since the message has been accepted. This method will also consume the
   * message
   * @param t the transaction
   * @param stream the stream making this call
   * @param storedTick the persistent tick
   * @throws Exception
   */
  public final void consumeAcceptedTick(TransactionCommon t, AOValue storedTick) throws Exception
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "consumeAcceptedTick", storedTick);

    try {

      SIMPMessage msg =
        consumerDispatcher.getMessageByValue(storedTick);
      Transaction msTran = mp.resolveAndEnlistMsgStoreTransaction(t);

      // PK67067 We may not find a message in the store for this tick, because
      // it may have been removed using the SIBQueuePoint MBean
      if (msg != null) {
        msg.remove(msTran, storedTick.getPLockId());
      }

      storedTick.lockItemIfAvailable(controlItemLockID);  // should always be successful
      storedTick.remove(msTran, controlItemLockID);
    }
    catch (Exception e)
    {
      // No FFDC code needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "consumeAcceptedTick", e);

      throw e;
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "consumeAcceptedTick");
  }

  public void createControlAdapter()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createControlAdapter");
    _controlAdapter = new RemoteConsumerTransmit(this, parent);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createControlAdapter");
  }

  public void dereferenceControlAdapter()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "dereferenceControlAdapter");
    _controlAdapter = null;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "dereferenceControlAdapter");
  }

  public ControlAdapter getControlAdapter()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getControlAdapter");
    if (_controlAdapter==null)
      createControlAdapter();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getControlAdapter");
    return _controlAdapter;
  }

  public void registerControlAdapterAsMBean()  {
    // no-op
  }
  public void deregisterControlAdapterMBean() {
    // no-op
  }

  public String toString()
  {
    String text = super.toString() + "[";
    text += "StreamId:" + streamId + "]";
    
    return text;
  }
}
