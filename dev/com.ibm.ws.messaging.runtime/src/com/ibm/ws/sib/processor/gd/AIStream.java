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
package com.ibm.ws.sib.processor.gd;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.AIStreamKey;
import com.ibm.ws.sib.processor.impl.AnycastInputHandler;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.RemoteQPConsumerKey;
import com.ibm.ws.sib.processor.impl.AnycastInputHandler.SendDispatcher;
import com.ibm.ws.sib.processor.impl.exceptions.ClosedException;
import com.ibm.ws.sib.processor.impl.interfaces.ControllableResource;
import com.ibm.ws.sib.processor.impl.interfaces.RemoteDispatchableKey;
import com.ibm.ws.sib.processor.impl.store.AsyncUpdate;
import com.ibm.ws.sib.processor.impl.store.AsyncUpdateThread;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.impl.store.items.AICompletedPrefixItem;
import com.ibm.ws.sib.processor.impl.store.items.AIMessageItem;
import com.ibm.ws.sib.processor.impl.store.items.AIProtocolItem;
import com.ibm.ws.sib.processor.impl.store.items.AOValue;
import com.ibm.ws.sib.processor.impl.store.itemstreams.AIContainerItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.AIProtocolItemStream;
import com.ibm.ws.sib.processor.runtime.SIMPDeliveryReceiverControllable;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.processor.runtime.impl.RemoteConsumerReceiver;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.processor.utils.am.AbstractBatchedTimeoutEntry;
import com.ibm.ws.sib.processor.utils.am.BatchedTimeoutEntry;
import com.ibm.ws.sib.processor.utils.am.BatchedTimeoutManager;
import com.ibm.ws.sib.processor.utils.am.BatchedTimeoutProcessor;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;

public class AIStream extends ControllableStream implements ControllableResource
{
  private static final TraceComponent tc =
    SibTr.register(AIStream.class,SIMPConstants.MP_TRACE_GROUP,SIMPConstants.RESOURCE_BUNDLE);

  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private static final int NUM_OF_BUCKETS = 4;

  private final SIBUuid12 _streamId;
  private AIProtocolItemStream _itemStream;
  private TreeSet<AIProtocolItem> _itemStreamIndex;
  private AnycastInputHandler _parent;

  private AsyncUpdateThread _msUpdateThread;

  private StateStream _targetStream;
  private AICompletedPrefixItem _completedPrefix;
  private boolean _completedPrefixNeedsToBeScheduled;
  private int _countOfOutstandingCPUpdates;
  private long _latestTick;

  private long _latestDMEVersion;

  private BatchedTimeoutManager _eagerGetTOM;
  private TraversableBatchedTimeoutManager _slowedGetTOM;
  private BatchedTimeoutManager _initialAcceptedTOM;
  private BatchedTimeoutManager _acceptedTOM;
  private BatchedTimeoutManager _rejectedTOM;

  private boolean _stopped = false;

  private MessageProcessor _mp;

  private ControlAdapter _controlAdapter;

  /**
   * The constructor
   * @param streamId null if existingIS
   * @param itemStream Freshly created if !existingIS, recovered otherwise
   * @param parent
   * @param latestTick
   * @param completedPrefix The completed prefix item in the itemStream
   *                        invariant: existingIS <=> (completedPrefix == null), i.e., if the itemStream was
   *                        recovered we need to read the completed prefix from it, otherwise the completed
   *                        prefix was freshly created and handed to us so we don't have to read it
   * @param existingIS If the itemStream was recovered or it was freshly created
   */
  public AIStream(
    SIBUuid12 streamId,
    AIProtocolItemStream itemStream,
    AnycastInputHandler parent,
    AsyncUpdateThread msUpdateThread,
    long latestTick,
    AICompletedPrefixItem completedPrefix,
    boolean existingIS,
    MessageProcessor mp)
    throws MessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "AIStream",
        new Object[] {
          streamId,
          itemStream,
          parent,
          msUpdateThread,
          Long.valueOf(latestTick),
          completedPrefix,
          Boolean.valueOf(existingIS),
          mp });

    synchronized (this)
    {
      _itemStream = itemStream;
      ProtocolItemComparator piComparator = new ProtocolItemComparator();
      _itemStreamIndex = new TreeSet<AIProtocolItem>(piComparator);
      _parent = parent;
      _msUpdateThread = msUpdateThread;
      _targetStream = new StateStream();
      _targetStream.init();
      _latestTick = latestTick;
      _latestDMEVersion = SIMPConstants.NULL_DME_VERSION;
      _mp = mp;

      CreateTOMs createTOMs = null;
      if (existingIS)
      {
        _streamId = itemStream.getStreamId();
        try
        {
          // this will also set the completed prefix here and in the target stream
          createTOMs = recoverFromPersistentState(itemStream);
        }
        catch (MessageStoreException e)
        {
          // MessageStoreException shouldn't occur so FFDC.
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.gd.AIStream.AIStream",
            "1:214:1.108.2.18",
            this);

          SibTr.exception(tc, e);

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "AIStream", e);

          throw e;
        }
      }
      else
      {
        _streamId = streamId;
        _completedPrefix = completedPrefix;
        long cpTick = completedPrefix.getTick();
        _targetStream.setCompletedPrefix(cpTick);

        // Fill in with L/R the gap between the completed prefix and the latest tick
        ArrayList<AIRejectedRange> rejectedTicks = null;
        if (cpTick < latestTick)
        {
          rejectedTicks = new ArrayList<AIRejectedRange> ();

          AIRejectedRange rr = writeRejectedNoTimeout(cpTick+1, latestTick, 0, false);
          rejectedTicks.add(rr);
        }

        createTOMs = new CreateTOMs(null, rejectedTicks, null);
      }

      // We haven't scheduled the completed prefix for update, so next time it is modified it needs to be
      // scheduled
      _completedPrefixNeedsToBeScheduled = true;
      _countOfOutstandingCPUpdates = 0;

      createTOMs.create();

      // Call the remote queue point control to register the fact that there will
      //  be a remote get issued from this ME
      if (!parent.getBaseDestinationHandler().isPubSub())
      {
        ControlAdapter remoteQueuePoint =
          (ControlAdapter) parent.getBaseDestinationHandler().getRemoteQueuePointControl(parent.getLocalisationUuid(), true);
        //we also register the mbean - this method is protected against
        //registering twice in the case when we have already produced
        //messages to this queue
        remoteQueuePoint.registerControlAdapterAsMBean();
      }


      // start the timers
      start();
    } // end synchronized(this)

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AIStream", this);
  }

  public AIStreamState getStreamState()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getStreamState");

    AIStreamState streamState = AIStreamState.ACTIVE;

    if(_parent.isStreamBeingFlushed())
      streamState = AIStreamState.REMOVING;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getStreamState", streamState);

    return streamState;
  }

  /////////////////////////////////////////////////////////////
  // Methods invoked by AnycastInputHandler
  /////////////////////////////////////////////////////////////

  public SIBUuid12 getStreamId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getStreamId");

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getStreamId", _streamId);

    return _streamId;
  }

  /**
   * Insert a get request at tick t. Every tick from latestTick+1 to t-1 is set to rejected. In general,
   * trying to insert the tick could be denied because there may be a previous L/R in the stream
   * that needs to turn to L/D. However, in the latest design the RME does not reject any ticks while
   * the consumer is connected, waiting instead until it disconnects to reject all ticks.
   *
   * @param tick The point in the stream to insert the request
   * @param selector The selector associated with the request
   * @param timeout The timeout on the request, either provided by the consumer or assigned by default by the RCD
   *
   * @return The reject start tick or -1 if no ticks rejected
   */
  public long insertRequest(
    AIRequestedTick rt,
    long tick,
    long timeout)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "insertRequest",
        new Object[] {
          rt,
          Long.valueOf(tick),
          Long.valueOf(timeout)});

    long rejectStart = _latestTick + 1;

    if (rejectStart < tick)
    {
      // Notice that the ticks have recovery=false, but the request sent over does not
      writeRejected(rejectStart, tick - 1, 0);
    }
    else
    {
      rejectStart = tick;
    }

    TickRange requestRange = new TickRange(TickRange.Requested, tick, tick);
    // Associate the tick with the requesting consumer key so that, when the data message arrives,
    // we can tell the RCD what consumer it is for
    requestRange.value = rt;
    requestRange.valuestamp = tick;

    _targetStream.writeRange(requestRange);

    // Start the get repetition (eager get) timeout, only if timeout is not zero (i.e., it's not a NoWait)
    if (timeout > 0L || timeout == _mp.getCustomProperties().get_infinite_timeout())
    {
      _eagerGetTOM.addTimeoutEntry(rt);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "insertRequest", Long.valueOf(rejectStart));

    return rejectStart;
  }

  /**
   * Counts the number of messages in the value state on the stream.
   *
   * NOTE: this method is not synchronized - the client should protect
   * against concurrent access.
   *
   * @return a long for the number of messages in 'value' on the stream.
   */
  public long countAllMessagesOnStream()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "countAllMessagesOnStream");

    long count=0;
    _targetStream.setCursor(0);
    // Get the first TickRange
    TickRange tr = _targetStream.getNext();
    while (tr.endstamp < RangeList.INFINITY)
    {
      if (tr.type == TickRange.Value)
      {
        count++;
      }
      if (tr.type == TickRange.Requested)
      {
        // If we are an restarted IME (gathering) then we may have to restore msgs from the
        // DME which were previously Value msgs in this stream. We need to count any re-requests
        // in our count as they contribute to the total.
        if (((AIRequestedTick)tr.value).getRestoringAOValue() != null)
          count++;
      }
      tr = _targetStream.getNext();
    } // end while

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "countAllMessagesOnStream", Long.valueOf(count));
    return count;
  }

  /**
   * Determine whether an assured V/U tick can be delivered in order:
   * (a) If prevTick is unknown, then the V/U tick can be delivered only if there are no preceding Q/U
   *     and Q/G ticks, and all preceding V/U ticks have been delivered
   * (b) If prevTick is t, then the V/U tick can be delivered if t is L/D or L/A, or if it's V/U and it
   *     has been delivered
   *
   * @return Whether or not the V/U tick can be delivered
   */
  public boolean canDeliverAssuredInOrder(
    long tick,
    long prevTick,
    int priority)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "canDeliverAssuredInOrder",
        new Object[] {
          Long.valueOf(tick),
          Long.valueOf(prevTick),
          Integer.valueOf(priority)});

    boolean canDeliverInOrder = false;

    synchronized (this)
    {
      // Is prevTick unknown?
      if (prevTick == _mp.getCustomProperties().get_unknown_prev_tick())
      {
        // start assuming we can deliver, but if we find Q/U, Q/G or undelivered V/U, then we cannot
        canDeliverInOrder = true;

        long start = _targetStream.getCompletedPrefix() + 1;
        if (start < tick)
        {
          long end = tick - 1;
          _targetStream.setCursor(start);
          TickRange tr2 = null;
          TickRange tr1 = _targetStream.getNext();
          do
          {
            if ((tr1.type == TickRange.Unknown)
              || (tr1.type == TickRange.Requested))
            {
              canDeliverInOrder = false;
              break;
            }
            else if (tr1.type == TickRange.Value)
            {
              AIValueTick valueTick = (AIValueTick) tr1.value;
              boolean delivered = valueTick.isDelivered();
              Reliability currentReliability = valueTick.getMsgReliability();
              boolean isAssured =
                currentReliability.compareTo(Reliability.RELIABLE_PERSISTENT)
                  >= 0;
              boolean samePriority = (valueTick.getMsgPriority() == priority);
              if (isAssured
                && samePriority
                && !delivered) // has it not been delivered?
              {
                canDeliverInOrder = false;
                break;
              }
            }

            tr2 = tr1;
            tr1 = _targetStream.getNext();
          }
          while ((tr1.startstamp <= end) && (tr1 != tr2));
        }

      }
      else // prevTick not unknown
        {
        _targetStream.setCursor(prevTick);
        TickRange prevTickRange = _targetStream.getNext();
        if ((prevTickRange.type == TickRange.Completed)
          || (prevTickRange.type == TickRange.Accepted))
        {
          canDeliverInOrder = true;
        }
        else if (prevTickRange.type == TickRange.Value)
        {
          // Can deliver if the previous tick is a value and it has been delivered
          canDeliverInOrder = ((AIValueTick) prevTickRange.value).isDelivered();
        }
        else // prevTickRange is not L/D, L/A or delivered V/U
          {
          canDeliverInOrder = false;
        }
      }
    } // end synchronized(this)

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(
        tc,
        "canDeliverAssuredInOrder",
        Boolean.valueOf(canDeliverInOrder));

    return canDeliverInOrder;
  }

  /**
   * Turn a Q/G to V/U. Associate with the V/U tick the in-memory reference to the message and whether the message
   * was delivered.
   *
   * @return The Q/G tick, from which the consumer key and time issued can be obtained
   */
  public AIRequestedTick updateRequestToValue(
    long tick,
    AIMessageItem msgItem,
    boolean valueDelivered,
    SendDispatcher sendDispatcher)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "updateRequestToValue",
        new Object[] { Long.valueOf(tick), msgItem, Boolean.valueOf(valueDelivered)});

    AIRequestedTick rt = null;
    _targetStream.setCursor(tick);
    TickRange tickRange = _targetStream.getNext();
    if (tickRange.type == TickRange.Requested)
    {
      rt = (AIRequestedTick) tickRange.value;
      RemoteDispatchableKey ck = rt.getRemoteDispatchableKey();

      // The tick keeps the in-memory Java reference of the message object if valueDelivered=false,
      //      the constructor takes the reference to extract reliability and priority
      // Re-enter the ck as part of the V/U's state, just in case ordered
      //      delivery applies to consumer cardinality other than one
      AIValueTick valueTick =
        new AIValueTick(
          tick,
          msgItem,
          valueDelivered,
          ck,
          rt.getOriginalTimeout(),
          rt.getIssueTime(),
          msgItem.getMessage().getRedeliveredCount().intValue());
      TickRange valueRange = new TickRange(TickRange.Value, tick, tick);
      valueRange.value = valueTick;
      valueRange.valuestamp = tick;

      _targetStream.writeRange(valueRange);

      if (rt.getTimeout() > 0L || rt.getTimeout() == _mp.getCustomProperties().get_infinite_timeout())
      {
        if (rt.isSlowed())
        {
          _slowedGetTOM.removeTimeoutEntry(rt);
        }
        else
        {
          _eagerGetTOM.removeTimeoutEntry(rt);
        }
      }
    }
    else if (tickRange.type == TickRange.Accepted)
    {
      sendDispatcher.sendAccept(tick);
    }
    else if (tickRange.type == TickRange.Rejected)
    {
      AIRejectedRange rr = (AIRejectedRange)tickRange.value;
      sendDispatcher.sendReject(tick, tick, rr.unlockCount, rr.recovery);
    }
    else if (tickRange.type == TickRange.Completed)
    {
      sendDispatcher.sendCompleted(tick, tick);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateRequestToValue", rt);

    return rt;
  }

  /**
   * sets the unlock count of the value tick's msg.
   * @param tick The tick in the stream
   */
  public final void incrementUnlockCount(long tick)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "incrementUnlockCount", Long.valueOf(tick));

    _targetStream.setCursor(tick);
    TickRange tickRange = _targetStream.getNext();
    if (tickRange.type == TickRange.Value)
    {
      AIValueTick valueTick = (AIValueTick) tickRange.value;
      if (valueTick != null)
        valueTick.incRMEUnlockCount();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "incrementUnlockCount", tickRange);
  }

  /**
   * This method returns the list of undelivered messages that follow the message at 'tick', given by
   * prevTick. It assumes that a message is in the list if its state is undelivered and has the same
   * qos and priority as that of the message provided. Notice that if ordered delivery applies to consumer
   * cardinality other than one, then this list is still correct in the absence of rejected messages.
   * Since prevTick defines a reverse order than we are looking for, we start at the end of the stream
   * looking for messages in the list. If we have seen one, we can start following prevTick links, otherwise
   * we iterate to the previous tick in the stream. Notice that if a prevTick link points to a message not
   * in the list, then we have to start looking for a message in the list from that point. We stop looking
   * when we have arrived at the tick provided.
   *
   * @param tick The tick of the message that completes the undelivered list to be found
   * @param message The message at tick
   * @return A list of TickRange
   */
  // TODO Incorporate the case of ordered delivery for consumer cardinality other than one, if necessary
  public List findUndeliveredList(long tick, JsMessage message)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "findUndeliveredList",
        new Object[] { Long.valueOf(tick), message });

    int priority = message.getPriority().intValue();
    // The priority to compare against
    _targetStream.setCursor(_latestTick);
    TickRange tr2 = null;
    TickRange tr1 = _targetStream.getPrev();
    ArrayList<AIValueTick> temp = null;
    boolean startNewList = true;
    do
    {
      if (tr1.type == TickRange.Value)
      {
        AIValueTick valueTick = (AIValueTick) tr1.value;
        boolean delivered = valueTick.isDelivered();
        int currentPriority = valueTick.getMsgPriority();
        // If we did not follow a prevTick link, we need to make sure we use the correct priority
        // only assured messages can be undelivered, so we need not check for reliability
        if (!delivered && (currentPriority == priority))
        {
          //JsMessage currentMsg = valueTick.getMsg();
          long prevTick = valueTick.getMsg().getMessage().getGuaranteedRemoteGetPrevTick();
          if (startNewList)
          {
            temp = new ArrayList<AIValueTick>(10); // this code is typically not executed since messages usually arrive in order
            startNewList = false;
          }
          temp.add(valueTick);
          //targetStream.setCursor(currentMsg.getGuaranteedRemoteGetPrevTick());
          _targetStream.setCursor(prevTick);
          tr2 = tr1;
          tr1 = _targetStream.getPrev();
          continue;
        }
      }

      // We've hit a gap in the requests (an unknown/unsatisfied request), blank out the list built so far as
      // those cannot be delivered yet.
      startNewList = true;
      temp = null;

      tr2 = tr1;
      tr1 = _targetStream.getPrev();
    }
    while ((tr1.endstamp > tick) && (tr1 != tr2));

    // turn the list around before returning
    ArrayList<AIValueTick> undeliveredList = null;
    if (temp != null)
    {
      undeliveredList = new ArrayList<AIValueTick>(temp.size());
      for (int i = temp.size() - 1; i >= 0; i--)
      {
        undeliveredList.add(temp.get(i));
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "findUndeliveredList", undeliveredList);

    return undeliveredList;
  }

  /**
   * Mark each tick in the list as delivered. We assume the list contains TickRange objects corresponding to
   * V/U (value) ticks.
   *
   * @param list A list assumed to contain TickRange objects
   */
  public void markListDelivered(List list)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "markListDelivered", list);

    int len = list.size();
    for (int i = 0; i < len; i++)
    {
      AIValueTick valueTick = (AIValueTick) list.get(i);
      valueTick.setDelivered(true);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "markListDelivered");

  }

  public AIProtocolItem processAccepted(long tick, TransactionCommon transaction)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "processAccepted",
        new Object[] { Long.valueOf(tick), transaction });

    AIProtocolItem acceptedItem = null;
    boolean isExpress = false;
    Reliability rel = null;

    synchronized (this)
    {
      _targetStream.setCursor(tick);
      TickRange valueRange = _targetStream.getNext();

      if (valueRange.type != TickRange.Value)
      {
        // Can only process value ticks
        SIErrorException e1 =
          new SIErrorException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0001",
              new Object[] {
                "com.ibm.ws.sib.processor.gd.AIStream",
                "1:737:1.108.2.18" },
              null));

        // FFDC
        FFDCFilter.processException(
          e1,
          "com.ibm.ws.sib.processor.gd.AIStream.processAccepted",
          "1:744:1.108.2.18",
          this);

        SibTr.exception(tc, e1);

        SibTr.error(tc,
                    "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                    new Object[] {
                      "com.ibm.ws.sib.processor.gd.AIStream",
                      "1:753:1.108.2.18" });

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "processAccepted", null);

        throw e1;
      }

      AIValueTick vt = (AIValueTick) valueRange.value;
      rel = vt.getMsgReliability();
      isExpress = rel.compareTo(Reliability.RELIABLE_NONPERSISTENT) <= 0;

      if (transaction == null)
      {
        if (isExpress)
        {
          updateToAccepted(tick, null);
        }
        else
        {
          // log error, Can't process assured w/o transaction
          SIErrorException e2 =
            new SIErrorException(
              nls.getFormattedMessage(
                "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                new Object[] {
                  "com.ibm.ws.sib.processor.gd.AIStream",
                  "1:780:1.108.2.18" },
                null));

          // FFDC
          FFDCFilter.processException(
            e2,
            "com.ibm.ws.sib.processor.gd.AIStream.processAccepted",
            "1:787:1.108.2.18",
            this);

          SibTr.exception(tc, e2);

          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                      new Object[] {
                        "com.ibm.ws.sib.processor.gd.AIStream",
                        "1:795:1.108.2.18" });

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processAccepted", null);

          throw e2;
        }
      }
      else
      {
        // If express and non-null transaction, no-op
        // ControlAccepted will be sent and stream will be updated in committed
      }
    }

    if (!isExpress && transaction != null)
    {
      // write Accepted protocol item to persistent stream state,
      // ControlAccepted will be sent and stream will be updated in committed
      acceptedItem = new AIProtocolItem(tick, TickRange.Accepted, rel, _parent);

      try
      {
        Transaction msTran = _mp.resolveAndEnlistMsgStoreTransaction(transaction);
        _itemStream.addItem(acceptedItem, msTran);
      }
      catch (MessageStoreException e)
      {
        // MessageStoreException shouldn't occur so FFDC and rethrow
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.gd.AIStream.processAccepted",
          "1:827:1.108.2.18",
          this);

        // Make sure null is returned
        acceptedItem = null;

        SIErrorException e2 =
          new SIErrorException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0002",
              new Object[] {
                "com.ibm.ws.sib.processor.gd.AIStream",
                "1:839:1.108.2.18",
                e },
              null),
           e);

        SibTr.exception(tc, e2);
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.gd.AIStream",
            "1:848:1.108.2.18",
            SIMPUtils.getStackTrace(e) });

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "processAccepted", acceptedItem);

        throw e2;
      }
      catch (SIResourceException e)
      {
        // MessageStoreException shouldn't occur so FFDC and rethrow
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.gd.AIStream.processAccepted",
          "1:862:1.108.2.18",
          this);

        // Make sure null is returned
        acceptedItem = null;

        SIErrorException e2 =
          new SIErrorException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0002",
              new Object[] {
                "com.ibm.ws.sib.processor.gd.AIStream",
                "1:874:1.108.2.18",
                e },
              null),
           e);

        SibTr.exception(tc, e2);
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.gd.AIStream",
            "1:883:1.108.2.18",
            SIMPUtils.getStackTrace(e) });

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "processAccepted", acceptedItem);

        throw e2;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processAccepted", acceptedItem);

    return acceptedItem;
  }

  /**
   * Turn a tick to L/A. Used to either:
   * (1) turn an assured V/U to L/A, in which case acceptedItemId is valid, or
   * (2) turn an express Q/G to L/A when discarding a message that came in out of order
   *
   */
  public void updateToAccepted(long tick, AIProtocolItem acceptedItem)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "updateToAccepted",
        new Object[] { Long.valueOf(tick), acceptedItem });

    _targetStream.setCursor(tick);
    TickRange tr = _targetStream.getNext();
    if ((tr.type == TickRange.Requested) || (tr.type == TickRange.Value))
    {
      if (acceptedItem != null)
      {
        // At this point, the persist of the accepted item has committed so we can add it to the index
        synchronized(_completedPrefix)
        {
          _itemStreamIndex.add(acceptedItem);
        }
      }

      writeAccepted(tick);

      // This is delayed until ACCEPT_INITIAL_THRESHOLD, writeAccepted batches the accepted tick
      // When the TOM fires, it sends the initial batch of accepts
      // long[] ticks = { tick };
      // sendDispatcher.sendAccept(ticks);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateToAccepted");
  }

  /**
   * Turn a tick to L/R. Used to:
   * (1) Reject a Q/G that timed out
   * (2) Implement AIH.reject invoked by the RCD
   * (3) Implement AIH.rolledback invoked by a dirty accepted item
   *
   */
  public void updateToRejected(long tick, SendDispatcher sendDispatcher)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "updateToRejected", Long.valueOf(tick));

    _targetStream.setCursor(tick);
    TickRange tr = _targetStream.getNext();
    if (tr.type == TickRange.Requested)
    {
      writeRejected(tick, tick, 0);
      sendDispatcher.sendReject(tick, tick, 0L, false);
    }
    if (tr.type == TickRange.Value)
    {
      long unlockCount = ((AIValueTick)(tr.value)).getRMEUnlockCount();
      writeRejected(tick, tick, unlockCount);
      sendDispatcher.sendReject(tick, tick, unlockCount, false);
    }
    else
    {
      // Can only process requested and value ticks
      SIErrorException e1 =
        new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.gd.AIStream",
              "1:972:1.108.2.18" },
            null));

      // FFDC
      FFDCFilter.processException(
        e1,
        "com.ibm.ws.sib.processor.gd.AIStream.updateToRejected",
        "1:979:1.108.2.18",
        this);

      SibTr.exception(tc, e1);

      SibTr.error(tc,
                  "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                  new Object[] {
                    "com.ibm.ws.sib.processor.gd.AIStream",
                    "1:988:1.108.2.18" });

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "updateToRejected", null);

      throw e1;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateToRejected");
  }

  public void updateAllToRejected(SendDispatcher sendDispatcher)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "updateAllToRejected");

    long start = _targetStream.getCompletedPrefix() + 1;
    long end = _latestTick;
    _targetStream.setCursor(start);
    TickRange tr2 = null;
    TickRange tr1 = _targetStream.getNext();

    do
    {
      if (tr1.type == TickRange.Requested)
      {
        long rStart = tr1.startstamp;
        long rEnd = tr1.endstamp;
        TickRange rejectedRange =
          new TickRange(TickRange.Rejected, rStart, rEnd);
        AIRejectedRange rr = new AIRejectedRange(rStart, rEnd, 0, false);
        rejectedRange.value = rr;

        _targetStream.writeRange(rejectedRange);

        _rejectedTOM.addTimeoutEntry(rr);

        sendDispatcher.sendReject(Long.valueOf(rStart), Long.valueOf(rEnd), rr.unlockCount, false);
      }
      else if (tr1.type == TickRange.Value)
      {
        long rStart = tr1.startstamp;
        long rEnd = tr1.endstamp;
        long unlockCount = ((AIValueTick)(tr1.value)).getRMEUnlockCount();
        TickRange rejectedRange =
          new TickRange(TickRange.Rejected, rStart, rEnd);
        AIRejectedRange rr = new AIRejectedRange(rStart, rEnd, unlockCount, false);
        rejectedRange.value = rr;

        _targetStream.writeRange(rejectedRange);

        _rejectedTOM.addTimeoutEntry(rr);

        sendDispatcher.sendReject(Long.valueOf(rStart), Long.valueOf(rEnd), rr.unlockCount, false);
      }

      tr2 = tr1;
      tr1 = _targetStream.getNext();
    }
    while ((tr1.startstamp <= end) && (tr1 != tr2));

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateAllToRejected");
  }

  /**
   * Update a tick range to completed. This assumes that a single existing range is updated which, other than
   * for rejected ranges, will have a startTick=endTick. Rely on targetStream to consolidate L/D ranges.
   */
  public void updateToCompleted(long startTick, long endTick) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "updateToCompleted",
        new Object[] { Long.valueOf(startTick), Long.valueOf(endTick)});

    boolean checkCompletedPrefix = false;
    _targetStream.setCursor(startTick);
    TickRange tr1 = _targetStream.getNext();
    TickRange tr2 = null;

    do
    {
      // long acceptedItemId = AnycastInputHandler.INVALID_ACCEPTED_ITEM_ID;
      boolean update = true;
      boolean resolve = false;
      AIStreamKey tsKey = null;

      // TickRange combinedRange = null;
      if (tr1.type == TickRange.Accepted)
      {
        AIAcceptedTick at = (AIAcceptedTick) tr1.value;
        // acceptedItemId = at.acceptedItemId;
        _acceptedTOM.removeTimeoutEntry(at);
        // Remove from initialTOM just in case we are not on a first time round (e.g. If a DecisionRequested msg came in from the DME
        // before the initialTOM popped)
        _initialAcceptedTOM.removeTimeoutEntry(at);

      }
      else if (tr1.type == TickRange.Rejected)
      {
        AIRejectedRange rr = (AIRejectedRange) tr1.value;
        _rejectedTOM.removeTimeoutEntry(rr);
      }
      else if (tr1.type == TickRange.Requested)
      {
        // This can occur if the DME crashes and recovers, at which point
        // it will have forgotten the Q/G and replaced it with an L/D; it's
        // up to the consumer to reissue the get request
        AIRequestedTick rt = (AIRequestedTick) tr1.value;
        // TODO would it be better to have stored the key with the tick?
        tsKey =
          new AIStreamKey(
            tr1.startstamp,
            rt.getRemoteDispatchableKey(),
            rt.getOriginalTimeout(),
            rt.getIssueTime());
        resolve = true;

        // If the request hasn't timed out, stop repeating it because it will be
        // re-requested as part of the resolve below
        if (rt.getTimeout() > 0 || rt.getOriginalTimeout() == SIMPConstants.INFINITE_TIMEOUT)
        {
          if (rt.isSlowed())
          {
            _slowedGetTOM.removeTimeoutEntry(rt);
          }
          else
          {
            _eagerGetTOM.removeTimeoutEntry(rt);
          }
        }
      }
      else if (tr1.type == TickRange.Value)
      {
        // This could be due to admin cleanup at the DME
        AIValueTick vt = (AIValueTick) tr1.value;
        if (!vt.isDelivered())
        {
          tsKey =
            new AIStreamKey(
              tr1.startstamp,
              vt.getRemoteDispatchableKey(),
              vt.getOriginalTimeout(),
              vt.getIssueTime());
          resolve = true;
        }
        else
        {
          // Tick was already delivered and turned to L/D
          SIErrorException e1 =
            new SIErrorException(
              nls.getFormattedMessage(
                "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                new Object[] {
                  "com.ibm.ws.sib.processor.gd.AIStream",
                  "1:1146:1.108.2.18"  },
                null));

          // FFDC
          FFDCFilter.processException(
            e1,
            "com.ibm.ws.sib.processor.gd.AIStream.updateToCompleted",
            "1:1153:1.108.2.18" ,
            this);

          SibTr.exception(tc, e1);

          SibTr.error(tc,
                      "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                      new Object[] {
                        "com.ibm.ws.sib.processor.gd.AIStream",
                        "1:1162:1.108.2.18"  });

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "updateToCompleted", null);

          throw e1;
        }
      }
      else
      {
        // If it's completed, no need to update; should not happen otherwise
        update = false;
      }

      if (update)
      {
        /* combinedRange = */
        writeCompletedRange(tr1.startstamp, tr1.endstamp);
        checkCompletedPrefix = true;
      }

      // If a completed came in before we send an accept or reject then we need
      // to re-request a msg so we dont forget about the outstanding request.
      if (resolve)
        _parent.resolve(tsKey);

      tr2 = tr1;
      tr1 = _targetStream.getNext();
    }
    while ((tr1.startstamp <= endTick) && (tr1 != tr2));

    if (checkCompletedPrefix)
    {
      synchronized (_completedPrefix)
      {
        if (_targetStream.getCompletedPrefix() > _completedPrefix.getTick())
        {
          // Update the item and schedule it for update if it is not already scheduled
          _completedPrefix.setTick(_targetStream.getCompletedPrefix());
          if (_completedPrefixNeedsToBeScheduled)
          {
            CompletedPrefixAsyncUpdate update = new CompletedPrefixAsyncUpdate();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
              SibTr.debug(tc, "CompletedPrefixAsyncUpdate " + update + " : " + _countOfOutstandingCPUpdates);
            if (doEnqueueWork(update))
            {
              _completedPrefixNeedsToBeScheduled = false;
              //  Make sure a thread that may be trying to remove the completed prefix waits
              // for notification when the count goes down to zero
              _countOfOutstandingCPUpdates++;
            }
            // else something went seriously wrong but we already FFDCed
          }
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateToCompleted");
  }

  /*
   * Re-drive all scheduled reject messages
   */
  public void resendScheduledRejects()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "resendScheduledRejects");

    _rejectedTOM.driveAllActiveEntries();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "resendScheduledRejects");
  }

  public void updateAllToCompleted() throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "updateAllToCompleted");

    long start = _targetStream.getCompletedPrefix() + 1;
    long end = _latestTick;

    updateToCompleted(start, end);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateAllToCompleted");
  }

  public void processDecisionExpected(long tick, SendDispatcher sendDispatcher)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processDecisionExpected", Long.valueOf(tick));

    _targetStream.setCursor(tick);
    TickRange tickRange = _targetStream.getNext();
    if (tickRange.type == TickRange.Unknown)
    {
      // this should not happen since we fill gaps between Q/G ticks
    }
    else if (tickRange.type == TickRange.Requested)
    {
      AIRequestedTick rt = (AIRequestedTick) tickRange.value;
      sendDispatcher.sendRequest(rt.getTick(), rt.getTick(), rt.getCriterias(), rt.getTimeout() );
    }
    else if (tickRange.type == TickRange.Value)
    {
      // this should be rare, ignore until we have a decision
    }
    else if (tickRange.type == TickRange.Accepted)
    {
      sendDispatcher.sendAccept(tick);
    }
    else if (tickRange.type == TickRange.Rejected)
    {
      AIRejectedRange rr = (AIRejectedRange) tickRange.value;
      sendDispatcher.sendReject(rr.startTick, rr.endTick, rr.unlockCount,  rr.recovery);
    }
    else if (tickRange.type == TickRange.Completed)
    {
      sendDispatcher.sendCompleted(tickRange.startstamp, tickRange.endstamp);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processDecisionExpected");
  }

  /**
   * A ControlRequestAck message tells the RME to slow down its get repetition timeout since we now know that the
   * DME has received the request.
   *
   */
  public void processRequestAck(long tick, long dmeVersion)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "processRequestAck",
        new Object[] { Long.valueOf(tick), Long.valueOf(dmeVersion)});

    // Only consider non-stale request acks
    if (dmeVersion >= _latestDMEVersion)
    {
      _targetStream.setCursor(tick);
      TickRange tickRange = _targetStream.getNext();

      // Make sure that we still have a Q/G
      if (tickRange.type == TickRange.Requested)
      {
        AIRequestedTick airt = (AIRequestedTick) tickRange.value;

        // serialize with get repetition and request timeouts
        synchronized (airt)
        {
          // Set the request timer to slow, but only if it is not already slowed
          if (!airt.isSlowed())
          {
            _eagerGetTOM.removeTimeoutEntry(airt);
            airt.setSlowed(true);
            airt.setAckingDMEVersion(dmeVersion);
            long to = airt.getTimeout();
            if (to > 0  || to == _mp.getCustomProperties().get_infinite_timeout())
            {
              _slowedGetTOM.addTimeoutEntry(airt);
            }
          }
        }
      }
      else
      {
        // This can happen if the request ack got in too late and the Q/G was timed out and rejected
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processRequestAck");
  }

  /**
   * A ControlResetRequestAck message tells the RME to start re-sending get requests with an eager timeout since
   * the DME has crashed and recovered.
   *
   */
  public void processResetRequestAck(long dmeVersion, SendDispatcher sendDispatcher)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processResetRequestAck", Long.valueOf(dmeVersion));

    // Only consider non-stale request acks
    if (dmeVersion >= _latestDMEVersion)
    {
      if (dmeVersion > _latestDMEVersion)
        _latestDMEVersion = dmeVersion;

      _slowedGetTOM.applyToEachEntry(new AddToEagerTOM(_slowedGetTOM, dmeVersion));
      sendDispatcher.sendResetRequestAckAck(dmeVersion);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processResetRequestAck");
  }

  public synchronized long getLatestTick()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getLatestTick");
      SibTr.exit(tc, "getLatestTick", Long.valueOf(_latestTick));
    }

    return _latestTick;
  }

  /**
   * Returns the AnycastInputHandler associated with this AIStream
   * @return
   */
  public AnycastInputHandler getAnycastInputHandler()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getAnycastInputHandler");
      SibTr.exit(tc, "getAnycastInputHandler", _parent);
    }
    return _parent;
  }

  public synchronized void setLatestTick(long tick)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setLatestTick", Long.valueOf(tick));

    _latestTick = tick;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setLatestTick");
  }

  public void removeCompletedPrefix(TransactionCommon t, long lockID)
    throws MessageStoreException, InterruptedException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "removeCompletedPrefix",
        new Object[] { t, Long.valueOf(lockID), Integer.valueOf(_countOfOutstandingCPUpdates)});

    synchronized (_completedPrefix)
    {
      // wait for any thread that may have updated the completed prefix
      if (_countOfOutstandingCPUpdates > 0)
      {
        _completedPrefix.wait();
      }

      _completedPrefix.lockItemIfAvailable(lockID);
      Transaction msTran = _mp.resolveAndEnlistMsgStoreTransaction(t);
      _completedPrefix.remove(msTran, lockID);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeCompletedPrefix");
  }

  /////////////////////////////////////////////////////////////
  // Additional Methods
  /////////////////////////////////////////////////////////////

  public void start()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "start");

    synchronized (this)
    {
      _stopped = false;
    }

    // start all the liveness timers
    _eagerGetTOM.startTimer();
    _slowedGetTOM.startTimer();
    _initialAcceptedTOM.startTimer();
    _acceptedTOM.startTimer();
    _rejectedTOM.startTimer();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "start");
  }

  public void stop()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "stop");

    synchronized (this)
    {
      _stopped = true;
    }

    // stop all the liveness timers
    _eagerGetTOM.stopTimer();
    _slowedGetTOM.stopTimer();
    _initialAcceptedTOM.stopTimer();
    _acceptedTOM.stopTimer();
    _rejectedTOM.stopTimer();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "stop");
  }

  /////////////////////////////////////////////////////////////
  // Private methods
  /////////////////////////////////////////////////////////////

  private void writeAccepted(long tick)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "writeAccepted",
        new Object[] { Long.valueOf(tick)});

    AIAcceptedTick at = writeAcceptedNoTimeout(tick);
    _initialAcceptedTOM.addTimeoutEntry(at);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "writeAccepted");
  }

  private AIAcceptedTick writeAcceptedNoTimeout(long tick)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "writeAcceptedNoTimeout",
        new Object[] { Long.valueOf(tick)});

    AIAcceptedTick at = new AIAcceptedTick(tick);
    TickRange acceptedRange = new TickRange(TickRange.Accepted, tick, tick);
    acceptedRange.value = at;

    _targetStream.writeRange(acceptedRange);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "writeAcceptedNoTimeout", at);

    return at;
  }

  private AIRejectedRange writeRejected(long startTick, long endTick, long unlockCount)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "writeRejected",
        new Object[] {
          Long.valueOf(startTick),
          Long.valueOf(endTick),
          Long.valueOf(unlockCount)});

    AIRejectedRange rr = writeRejectedNoTimeout(startTick, endTick, unlockCount, false);
    _rejectedTOM.addTimeoutEntry(rr);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "writeRejected", rr);

    return rr;
  }

  private AIRejectedRange writeRejectedNoTimeout(long startTick, long endTick, long unlockCount, boolean recovery)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "writeRejectedNoTimeout",
        new Object[] {
          Long.valueOf(startTick),
          Long.valueOf(endTick),
          Long.valueOf(unlockCount),
          Boolean.valueOf(recovery)});

    AIRejectedRange rr = new AIRejectedRange(startTick, endTick, unlockCount, recovery);

    TickRange rejectedRange =
      new TickRange(TickRange.Rejected, startTick, endTick);
    rejectedRange.value = rr;

    _targetStream.writeRange(rejectedRange);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "writeRejectedNoTimeout", rr);

    return rr;
  }

  private TickRange writeCompletedRange(long startTick, long endTick)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "writeCompletedRange",
        new Object[] { Long.valueOf(startTick), Long.valueOf(endTick)});

    TickRange completedRange =
      new TickRange(TickRange.Completed, startTick, endTick);
    TickRange combinedRange = null;

    combinedRange = _targetStream.writeCompletedRange(completedRange);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "writeCompletedRange", combinedRange);

    return combinedRange;
  }

  /**
   * Read L/A ticks from item stream and set rest to L/R, read and set L/D prefix
   */
  private CreateTOMs recoverFromPersistentState(AIProtocolItemStream persStreamState)
    throws MessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "recoverFromPersistentState", persStreamState);

    NonLockingCursor cursor = null;

    // ticks may still be locked by a transaction that did not commit before failure
    AbstractItem abitem = null;
    ArrayList<AIAcceptedTick> acceptedTicks = null;
    ArrayList<AIRejectedRange> rejectedTicks = null;
    ArrayList<AIRequestedTick> requestedTicks = null;
    long completedPrefix = 0;

    try
    {
      cursor = persStreamState.newNonLockingItemCursor(null);
      cursor.allowUnavailableItems();
      while ((abitem = cursor.next()) != null)
      {
        if (abitem instanceof AIProtocolItem)
        {
          AIProtocolItem protocolItem = (AIProtocolItem) abitem;
          byte protocolState = protocolItem.getProtocolState();

          if (protocolState == TickRange.Accepted)
          {
            // We expect all accepted items to be before our completed
            // prefix, as they are removed from the stream as the prefix
            // is updated.
            if (protocolItem.getTick() > completedPrefix)
            {
              protocolItem.setAIHCallbackTarget(_parent);
              persStreamState.setCurrentTransaction(protocolItem, false);
              if (protocolItem.isAvailable())
              {
                AIAcceptedTick at = null;
                try
                {
                  at = writeAcceptedNoTimeout(protocolItem.getTick());
                }
                catch(SIErrorException e)
                {
                  //see defect 273949:
                  //invalid stream state transitions have been observed in this path.
                  //The errors are not repeatable, and have not been diagnosed.
                  //The symptom is that the code attempts to modify ticks into 'accepted' state
                  //when they are already 'completed'.
                  //It is possible that this is caused when AIProtocolItem objects are stored
                  //as STORE_EVENTUALLY while AICompletedPrefixItem objects are stored as STORE_ALWAYS.
                  //This FFDC has been added in order to capture enough information if this
                  //error is observed again.
                  FFDCFilter.processException(
                    e,
                    "com.ibm.ws.sib.processor.gd.AIStream.recoverFromPersistentState",
                    "1:1636:1.108.2.18",
                    new Object[]{
                      Long.valueOf(this.getCompletedPrefix()),
                      Long.valueOf(protocolItem.getTick()),
                      protocolItem,
                      persStreamState,
                      _targetStream,
                      this
                    });
                  if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "recoverFromPersistentState", e);
                  throw e;
                }
                if (acceptedTicks == null)
                {
                  acceptedTicks = new ArrayList<AIAcceptedTick>();
                }
                acceptedTicks.add(at);
                // build the index as we go
                synchronized(_completedPrefix)
                {
                  _itemStreamIndex.add(protocolItem);
                }
              }
              else
              {
                // set tick back to V/U, but have no data message!
                long tick = protocolItem.getTick();
                AIValueTick valueTick =
                  new AIValueTick(tick, Reliability.ASSURED_PERSISTENT);
                TickRange valueRange = new TickRange(TickRange.Value, tick, tick);
                valueRange.value = valueTick;
                valueRange.valuestamp = tick;
                _targetStream.writeRange(valueRange);
                protocolItem.setUnavailableAfterRecovery(true);
              }
            }
            // PK75049 Accepted items before the completed prefix in our
            // persistent state are a result of a rollback while removing thems.
            // See CompletedPrefixAsyncUpdate for details.
            else
            {
              // We need to cleanup these redundant entries.
              // This prevents problems if we later attempt to delete the stream.
              SIMPTransactionManager txMan = _mp.getTXManager();
              LocalTransaction mpTxn = txMan.createLocalTransaction(false);
              try {
                Transaction msTxn = txMan.resolveAndEnlistMsgStoreTransaction(mpTxn);
                // No need to lock the item first as we haven't completed GD recovery yet
                protocolItem.remove(msTxn, Item.NO_LOCK_ID);
                mpTxn.commit();
              }
              catch (Exception e) {
                // FFDC and continue. The state will remain.
                FFDCFilter.processException(
                    e,
                    "com.ibm.ws.sib.processor.gd.AIStream.recoverFromPersistentState",
                    "1:1693:1.108.2.18",
                    this);
              }
            }
          }
          else
          {
            // there only should be accepted items as we are not persisting completed items
            // FFDC to log the issue (which could prevent deletion of the stream) and continue.
            SIErrorException e = new SIErrorException();              
            FFDCFilter.processException(
                e,
                "com.ibm.ws.sib.processor.gd.AIStream.recoverFromPersistentState",
                "1:1706:1.108.2.18",
                this);
          }
        }
        else if (abitem instanceof AICompletedPrefixItem)
        {
          // A completed prefix item is always available, since it is always processed in the context of a local tran
          _completedPrefix = (AICompletedPrefixItem) abitem;
          long startTick = 0;
          completedPrefix = _completedPrefix.getTick();
          writeCompletedRange(startTick, completedPrefix);
          _targetStream.setCompletedPrefix(completedPrefix);
        }
      }
    }
    finally
    {
      if (cursor != null)
        cursor.finished();
    }

    // SIB0113
    // Now fill in the reconstituted AOValue ticks with Requested ranges
    ArrayList<AOValue> values =
      ((AIContainerItemStream)_itemStream.getItemStream()).getAOLinks();
    if (values != null)
    {
      Iterator<AOValue> it = values.iterator();
      while(it.hasNext())
      {
        AOValue val = it.next();

        // Set appropriate tick to an Requested value.
        long tick = val.getAIRequestTick();
        TickRange requestRange = new TickRange(TickRange.Requested, tick, tick);
        AIRequestedTick rt = new AIRequestedTick(tick, val, SIMPConstants.INFINITE_TIMEOUT);
        requestRange.value = rt;
        requestRange.valuestamp = tick;
        _targetStream.writeRange(requestRange);
        if (requestedTicks == null)
          requestedTicks = new ArrayList<AIRequestedTick>();
        requestedTicks.add(rt);
      }
    }

    // Now fill in gaps with L/R (Rejected ranges)
    long start = _targetStream.getCompletedPrefix() + 1;
    long end = _latestTick;
    _targetStream.setCursor(start);
    TickRange tr2 = null;
    TickRange tr1 = _targetStream.getNext();
    while ((tr1.startstamp <= end) && (tr1 != tr2))
    {
      if (tr1.type == TickRange.Unknown)
      {
        long rangeStart = tr1.startstamp;
        long rangeEnd = ((tr1.endstamp > end) ? end : tr1.endstamp);
        AIRejectedRange rr = writeRejectedNoTimeout(rangeStart, rangeEnd, 0, true);
        if (rejectedTicks == null)
          rejectedTicks = new ArrayList<AIRejectedRange>();
        rejectedTicks.add(rr);
      }
      else if (tr1.type == TickRange.Value)
      {
        // Must have been recovered above, leave it as is
      }
      else if (tr1.type == TickRange.Accepted)
      {
        // Must have been recovered above, leave it as is
      }
      else if (tr1.type == TickRange.Requested)
      {
        // Recovered during IME reconstitute
      }
      else
      {
        // No other types have been recovered
      }

      tr2 = tr1;
      tr1 = _targetStream.getNext();
    }

    CreateTOMs ct = new CreateTOMs(acceptedTicks, rejectedTicks, requestedTicks);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "recoverFromPersistentState", ct);

    return ct;
  }

  private boolean doEnqueueWork(AsyncUpdate au)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "doEnqueueWork", au);

    try
    {
      _msUpdateThread.enqueueWork(au);
    }
    catch (ClosedException e)
    {
      // need to be able to enqueue work, log error
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.gd.AIStream.doEnqueueWork",
        "1:1812:1.108.2.18",
        this);
      SIErrorException e2 =
        new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
              "com.ibm.ws.sib.processor.gd.AIStream",
              "1:1820:1.108.2.18",
              e },
            null),
          e);
      SibTr.exception(tc, e2);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.gd.AIStream",
          "1:1828:1.108.2.18",
          SIMPUtils.getStackTrace(e) });

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "doEnqueueWork", false);
      return false;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "doEnqueueWork", true);
    return true;
  }

  private void sendRequest(
    long rejectStartTick,
    long tick,
    SelectionCriteria[] criterias,
    long timeout)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "sendRequest",
        new Object[] {
          Long.valueOf(rejectStartTick),
          Long.valueOf(tick),
          criterias,
          Long.valueOf(timeout)});

    long[] rst = { rejectStartTick };
    long[] ticks = { tick };
    long[] tos = { timeout };
    _parent.sendRequest(rst, ticks, criterias, tos, SIMPConstants.CONTROL_MESSAGE_PRIORITY+1);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendRequest");
  }

  public AIProtocolItemStream getAIProtocolItemStream()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getAIProtocolItemStream");
      SibTr.exit(tc, "getAIProtocolItemStream", _itemStream);
    }
    return _itemStream;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.Stream#getCompletedPrefix()
   */
  public long getCompletedPrefix()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getCompletedPrefix");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getCompletedPrefix", Long.valueOf(_targetStream.getCompletedPrefix()));

    return _targetStream.getCompletedPrefix();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.Stream#getID()
   */
  public String getID()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getID");
    String id = _streamId.toString();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getID", id);
    return id;
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

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.Stream#getStateStream()
   */
  public StateStream getStateStream()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getStateStream");
      SibTr.exit(tc, "getStateStream", _targetStream);
    }
    return _targetStream;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.Stream#writeSilenceForced(long)
   */
  public void writeSilenceForced(long tick)
  {
    // Empty
  }

  /////////////////////////////////////////////////////////////
  // Inner classes
  /////////////////////////////////////////////////////////////

  /** Class to process eager get timeouts */

  class EagerGetTimeoutProcessor implements BatchedTimeoutProcessor
  {
    // This method does not need to be synchronized because we are working on a private list of timed out entries
    public void processTimedoutEntries(List entries)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "processTimedoutEntries",
            new Object[] {"EagerGetTimeoutProcessor",
            AIStream.this,
            entries});

      synchronized (AIStream.this)
      {
        // ignore timeout if we are stopped
        if (AIStream.this._stopped)
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processTimedoutEntries", this);

          return;
        }
      }

      int len = entries.size();
      for (int i = 0; i < len; i++)
      {
        AIRequestedTick airt = (AIRequestedTick) entries.get(i);

        boolean rejectRequest = false;
        synchronized (airt)
        {
          long to = airt.getTimeout();
          if (to != _mp.getCustomProperties().get_infinite_timeout())
          {
            to -= _mp.getCustomProperties().get_eager_get_request_interval();
            airt.resetTimeout(to);
          }
          // are we still within timeout limits?
          if (to > 0 || to == _mp.getCustomProperties().get_infinite_timeout())
          {
            // since, in general, the filter may be different for each request, we cannot send a single message out
            long rejectStart = airt.getTick();
            sendRequest(
              rejectStart,
              airt.getTick(),
              airt.getCriterias(),
              to);
          }
          else
          {
            // stop repeating since we must have timed out
            _eagerGetTOM.removeTimeoutEntry(airt);
            rejectRequest = true;
          }
        }

        // change the tick to L/R and send reject via the parent to synchronize with stream
        // make sure this happens outside of synchronized(airt)
        if (rejectRequest)
        {
          _parent.reject(airt.getTick());
          
          // The request has expired, so reset the refill state if this 
          // was a refill request. Note this will only be called for non-infinite
          // requests as they will not expire like this.
          RemoteDispatchableKey rdk = airt.getRemoteDispatchableKey();
          if (rdk instanceof RemoteQPConsumerKey)
            ((RemoteQPConsumerKey)rdk).checkAndResetRefillState(airt.getTick());
        }
       }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "processTimedoutEntries");
    }
  }

  /** Class to process slowed get timeouts */

  class SlowedGetTimeoutProcessor implements BatchedTimeoutProcessor
  {
    // This method does not need to be synchronized because we are working on a private list of timed out entries
    public void processTimedoutEntries(List entries)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "processTimedoutEntries",
            new Object[] {"SlowedGetTimeoutProcessor",
            AIStream.this,
            entries});

      synchronized (AIStream.this)
      {
        // ignore timeout if we are stopped
        if (AIStream.this._stopped)
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processTimedoutEntries", this);

          return;
        }
      }

      int len = entries.size();
      for (int i = 0; i < len; i++)
      {
        AIRequestedTick airt = (AIRequestedTick) entries.get(i);

        boolean rejectRequest = false;
        synchronized (airt)
        {
          long to = airt.getTimeout();
          if (to != _mp.getCustomProperties().get_infinite_timeout())
          {
            to -= _mp.getCustomProperties().get_slowed_get_request_interval();
            airt.resetTimeout(to);
          }
          // are we still within timeout limits?
          if (to > 0 || to == _mp.getCustomProperties().get_infinite_timeout())
          {
            _slowedGetTOM.removeTimeoutEntry(airt);
            airt.setSlowed(false);
            _eagerGetTOM.addTimeoutEntry(airt);

            // since, in general, the filter may be different for each request, we cannot send a single message out
            long rejectStart = airt.getTick();
            sendRequest(
              rejectStart,
              airt.getTick(),
              airt.getCriterias(),
              to);
          }
          else
          {
            // stop repeating since we must have timed out
            _slowedGetTOM.removeTimeoutEntry(airt);
            rejectRequest = true;
          }
        }

        // change the tick to L/R and send reject via the parent to synchronize with stream
        // make sure this happens outside of synchronized(airt)
        if (rejectRequest)
        {
          _parent.reject(airt.getTick());
        }
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "processTimedoutEntries", entries);
    }
  }

  /** Class to allow traversal of entries in Batched Timeout Manager */

  class TraversableBatchedTimeoutManager extends BatchedTimeoutManager
  {
    public TraversableBatchedTimeoutManager(
      int numOfBuckets,
      long timeoutInterval,
      List timeoutEntries,
      BatchedTimeoutProcessor handler,
      MessageProcessor mp)
    {
      super(numOfBuckets, timeoutInterval, timeoutEntries, handler, mp);
    }

    public synchronized void applyToEachEntry(EntryAction action)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "applyToEachEntry", action);

      LinkedListEntry entry = (LinkedListEntry) activeEntries.getFirst();
      while(entry != null && activeEntries.contains(entry))
      {
        action.theAction(entry.bte);
        entry = (LinkedListEntry)entry.getNext();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "applyToEachEntry");
    }
  } // end of class TraversableBatchedTimeoutManager

  interface EntryAction
  {
    public void theAction(BatchedTimeoutEntry entry);
  }

  /** Class to reset slowed get timeouts in a bucket back to eager get timeouts */

  class AddToEagerTOM implements EntryAction
  {
    private TraversableBatchedTimeoutManager timeoutManager;

    /* this is the dmeVersion coming in the ControlResetRequestAck */
    private long dmeVersion;

    public AddToEagerTOM(TraversableBatchedTimeoutManager timeoutManager, long dmeVersion)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "AddToEagerTOM",
          new Object[] { timeoutManager, Long.valueOf(dmeVersion) });


      this.timeoutManager = timeoutManager;
      this.dmeVersion = dmeVersion;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "AddToEagerTOM", this);
    }

    /* Remove requested ticks from this bucket and add them to the eager TOM only if their dmeVersion
     * is previous to the one coming in the ControlResetRequestAck.
     */
    public synchronized void theAction(BatchedTimeoutEntry entry)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "theAction", entry);

      AIRequestedTick airt = (AIRequestedTick) entry;
      // It may be that the resetting dme (with dmeVersion) already acked this request, so we don't need to
      // reset it
      if (airt.getAckingDMEVersion() < dmeVersion)
      {
        timeoutManager.removeTimeoutEntry(airt);
        synchronized (airt)
        {
          long to = airt.getTimeout();
          if (to != _mp.getCustomProperties().get_infinite_timeout())
          {
            // TODO this may be too much to subtract, but we don't really know how long we spent since the
            //      last repetition
            to -= _mp.getCustomProperties().get_slowed_get_request_interval();
          }

          if (to > 0 || to == _mp.getCustomProperties().get_infinite_timeout())
          {
            _eagerGetTOM.addTimeoutEntry(airt);
            airt.setSlowed(false);
            // since, in general, the filter may be different for each request, we cannot send a single message out
            long rejectStart = airt.getTick();
            sendRequest(
              rejectStart,
              airt.getTick(),
              airt.getCriterias(),
              to);
          }
          else
          {
            // we must have timed out, so change the tick to L/R and send reject
            // we are running on a thread that processes a reset request ack message, which synchronizes on the
            // stream status, so we don't have to
            try
            {
              SendDispatcher rejectedDispatcher = _parent.new SendDispatcher();
              updateToRejected(airt.getTick(), rejectedDispatcher);
              rejectedDispatcher.dispatch();
            }
            catch (SIErrorException e)
            {
              // No FFDC code needed; ignore, updateToRejected processed the exception
            }
          }
        }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "theAction");
    }
  } // end of class AddToEagerTom

  class AIAcceptedTick extends AbstractBatchedTimeoutEntry
  {
    public long tick;

    public AIAcceptedTick(long tick)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "AIAcceptedTick",
          new Object[] { Long.valueOf(tick)});

      this.tick = tick;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "AIAcceptedTick", this);
    }
  }

  class AIRejectedRange extends AbstractBatchedTimeoutEntry
  {
    public long startTick;
    public long endTick;
    public boolean recovery;

    // The number of times the msg that we are rejected was received and
    // rolledback before we rejected it. Used to enable redeliveryThreshold
    // checking across remote get.
    public long unlockCount;

    public AIRejectedRange(long startTick, long endTick, long unlockCount, boolean recovery)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "AIRejectedRange",
          new Object[] {
            Long.valueOf(startTick),
            Long.valueOf(endTick),
            Long.valueOf(unlockCount),
            Boolean.valueOf(recovery)});

      this.startTick = startTick;
      this.endTick = endTick;
      this.unlockCount = unlockCount;
      this.recovery = recovery;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "AIRejectedRange", this);
    }
  }

  /** Class to process accept initial send */
  class InitialAcceptedTimeoutProcessor extends AcceptedTimeoutProcessor
  {
    public InitialAcceptedTimeoutProcessor()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "InitialAcceptedTimeoutProcessor");

      initialSend = true;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "InitialAcceptedTimeoutProcessor", this);
    }
  }

  /** Class to process accept timeouts */

  class AcceptedTimeoutProcessor implements BatchedTimeoutProcessor
  {
    protected boolean initialSend = false;

    public void processTimedoutEntries(List entries)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc,
         "processTimedoutEntries",
         new Object[] {"AcceptedTimeoutProcessor",
                       AIStream.this,
                       entries,
                       Boolean.valueOf(initialSend),
                       this});

      synchronized (AIStream.this)
      {
        // ignore timeout if we are stopped
        if (AIStream.this._stopped)
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processTimedoutEntries");

          return;
        }
      }

      int len = entries.size();
      long[] ticks = new long[len];
      for (int i = 0; i < len; i++)
      {
        AIAcceptedTick at = (AIAcceptedTick) entries.get(i);
        ticks[i] = at.tick;

        if (initialSend)
        {
          _initialAcceptedTOM.removeTimeoutEntry(at);
          _acceptedTOM.addTimeoutEntry(at);
        }
      }
      _parent.sendAccept(ticks);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "processTimedoutEntries", this);
    }
  }

  /** Class to process reject timeouts */

  class RejectedTimeoutProcessor implements BatchedTimeoutProcessor
  {
    public void processTimedoutEntries(List entries)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "processTimedoutEntries",
            new Object[] {"RejectedTimeoutProcessor",
                          AIStream.this,
                          entries});

      synchronized (AIStream.this)
      {
        // ignore timeout if we are stopped
        if (AIStream.this._stopped)
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processTimedoutEntries", this);

          return;
        }
      }

      int len = entries.size();
      SendDispatcher sendDispatcher = _parent.new SendDispatcher();
      for (int i = 0; i < len; i++)
      {
        AIRejectedRange rr = (AIRejectedRange) entries.get(i);
        sendDispatcher.sendReject(rr.startTick, rr.endTick, rr.unlockCount, rr.recovery);
      }
      sendDispatcher.dispatch();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "processTimedoutEntries", this);
    }
  }

  /**
   * An enumeration for the possible states for this stream
   * @author tpm100
   */
  public static class AIStreamState implements SIMPDeliveryReceiverControllable.StreamState
  {
    public static final AIStreamState ACTIVE = new AIStreamState("Active", 1);
    public static final AIStreamState REMOVING = new AIStreamState("Removing", 2);
    private String name;
    private int id;
    private AIStreamState(String _name, int _id)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "AIStreamState", new Object[]{_name, Integer.valueOf(_id)});

      name = _name;
      id = _id;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "AIStreamState", this);
    }
    public String toString()
    {
      return name;
    }

    public int getValue()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "getValue");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getValue", Integer.valueOf(id));
      return id;
    }

  }

  /**
   * Class to create the timeout managers
   */

  class CreateTOMs
  {
    private List acceptedTicks;
    private List rejectedTicks;
    private List requestedTicks;

    public CreateTOMs()
    {
      this.acceptedTicks = null;
      this.rejectedTicks = null;
      this.requestedTicks = null;
    }

    public CreateTOMs(List acceptedTicks, List rejectedTicks, List requestedTicks)
    {
      this.acceptedTicks = acceptedTicks;
      this.rejectedTicks = rejectedTicks;
      this.requestedTicks = requestedTicks;
    }

    public void create()
    {
      _eagerGetTOM =
        new BatchedTimeoutManager(
          NUM_OF_BUCKETS,
          _mp.getCustomProperties().get_eager_get_request_interval(),
          requestedTicks,
          new EagerGetTimeoutProcessor(),
          _mp);
      _slowedGetTOM =
        new TraversableBatchedTimeoutManager(
          NUM_OF_BUCKETS,
          _mp.getCustomProperties().get_slowed_get_request_interval(),
          null,
          new SlowedGetTimeoutProcessor(),
          _mp);
      _initialAcceptedTOM =
        new BatchedTimeoutManager(
          NUM_OF_BUCKETS,
          _mp.getCustomProperties().get_accept_initial_threshold(),
          acceptedTicks,
          new InitialAcceptedTimeoutProcessor(),
          _mp);
      _acceptedTOM =
        new BatchedTimeoutManager(
          NUM_OF_BUCKETS,
          _mp.getCustomProperties().get_accept_repetition_interval(),
          acceptedTicks,
          new AcceptedTimeoutProcessor(),
          _mp);
      _rejectedTOM =
        new BatchedTimeoutManager(
          NUM_OF_BUCKETS,
          _mp.getCustomProperties().get_reject_repetition_interval(),
          rejectedTicks,
          new RejectedTimeoutProcessor(),
          _mp);
    }
  }

  /**
   * Class to asynchronously persist the current value of the completed prefix. Any protocol items, of which
   * there will only be accepted items, that have been "advanced over" by the completed prefix are removed.
   * If the in-memory completed prefix item is further modified and advances over accepted items we did not
   * remove, the accepted items that were advanced over after execute but before committed will be removed
   * the next time we execute.
   */
  class CompletedPrefixAsyncUpdate extends AsyncUpdate
  {
    public void execute(TransactionCommon transactionCommon) throws Throwable
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "execute", new Object[] { this, transactionCommon });

      Transaction t = _mp.resolveAndEnlistMsgStoreTransaction(transactionCommon);

      synchronized (_completedPrefix)
      {
        try
        {
          // Update the completed prefix
          if (!_completedPrefix.isUpdating())
            _completedPrefix.requestUpdate(t);

          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "_completedPrefix tick: " + _completedPrefix.getTick());

          // Iterate from start of item stream index removing each one until the completed prefix
          Iterator items = _itemStreamIndex.iterator();
          AIProtocolItem nextItem = null;
          if(items.hasNext())
            nextItem = (AIProtocolItem) items.next();
          while (nextItem != null
            && nextItem.getTick() <= _completedPrefix.getTick())
          {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
              SibTr.debug(tc, "processing tick: " + nextItem.getTick());

            long lockID = _parent.getUniqueLockID(AbstractItem.STORE_NEVER);
            nextItem.lockItemIfAvailable(lockID);
            nextItem.remove(t, lockID);

            // Now remove it from the index
            // Note that we are removing the item from the itemstream index outside of any
            // transaction, which means that if this transaction rolls back for any reason
            // we will have an AIProtocolItem in our persistent state which is not in the iterator.
            // When we retry, we will update the prefix without removing that state.
            // Cleanup logic has been added at startup (recoverFromPersistentState)
            // to remove redundant entries of this type. 
            items.remove();

            if(items.hasNext())
              nextItem = (AIProtocolItem) items.next();
            else
              nextItem = null;
          }
          if ((nextItem != null) && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Didn't process tick: " + nextItem.getTick());

          // Flag that if the completed prefix is modified, its update needs to be scheduled
          _completedPrefixNeedsToBeScheduled = true;
          // we just updated the completed prefix, and we just flagged that it needs to be re-scheduled if
          // it is modified again, so there is no need to re-schedule a new update at this point
        }
        catch(Throwable e)
        {
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.gd.AIStream.CompletedPrefixAsyncUpdate.execute",
            "1:2544:1.108.2.18",
            new Object[] {this, _itemStreamIndex, Long.valueOf(_completedPrefix.getTick())});

          SibTr.exception(tc, e);

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "execute", e);

          throw e;
        }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "execute");
    }

    public void committed()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "committed");

      synchronized (_completedPrefix)
      {
        // this update is not outstanding any more
        _countOfOutstandingCPUpdates--;
        if (_countOfOutstandingCPUpdates == 0)
        {
          // notify a waiting thread that may be trying to remove the completed prefix
          _completedPrefix.notify();
        }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "committed", Integer.valueOf(_countOfOutstandingCPUpdates));
    }

    public void rolledback(Throwable t)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "rolledback", t);

      // Something went wrong, try again, potentially with further advanced completed prefix
      // Note that we still removed items from the itemstream index, and these will
      // not be cleaned up until next restart.
      doEnqueueWork(this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "rolledback");
    }
  }

  class ProtocolItemComparator implements Comparator
  {
    public int compare(Object o1, Object o2)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "compare", new Object[] { o1, o2 });

      if (o1 instanceof AIProtocolItem && o2 instanceof AIProtocolItem)
      {
        AIProtocolItem pi1 = (AIProtocolItem) o1;
        AIProtocolItem pi2 = (AIProtocolItem) o2;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "o1.tick:" + pi1.getTick() + " o2.tick:" + pi2.getTick());

        int result;
        if (pi1.getTick() < pi2.getTick())
        {
          result = -1;
        }
        else if (pi1.getTick() > pi2.getTick())
        {
          result = 1;
        }
        else
        {
          result = 0;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "compare", Integer.valueOf(result));

        return result;
      }

      ClassCastException e = new ClassCastException();
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.gd.AIStream.ProtocolItemComparator.compare",
        "1:2634:1.108.2.18",
        new Object[] {this, o1, o2});

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "compare", Integer.valueOf(0));

      // Throw exception to itemStreamIndex (GBSTree method should know what to do with it)
      throw e;
    }
  }

  public void createControlAdapter()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createControlAdapter");
    _controlAdapter = new RemoteConsumerReceiver(this);
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
    text += "StreamId:" + _streamId + "]";

    return text;
  }
}
