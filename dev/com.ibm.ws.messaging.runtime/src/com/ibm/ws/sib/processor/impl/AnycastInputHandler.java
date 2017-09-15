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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ejs.util.am.Alarm;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.JsAdminUtils;
import com.ibm.ws.sib.comms.ProtocolVersion;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageCreateFailedException;
import com.ibm.ws.sib.mfp.ProtocolType;
import com.ibm.ws.sib.mfp.control.ControlAccept;
import com.ibm.ws.sib.mfp.control.ControlAreYouFlushed;
import com.ibm.ws.sib.mfp.control.ControlBrowseEnd;
import com.ibm.ws.sib.mfp.control.ControlBrowseGet;
import com.ibm.ws.sib.mfp.control.ControlBrowseStatus;
import com.ibm.ws.sib.mfp.control.ControlCardinalityInfo;
import com.ibm.ws.sib.mfp.control.ControlCompleted;
import com.ibm.ws.sib.mfp.control.ControlCreateStream;
import com.ibm.ws.sib.mfp.control.ControlDecisionExpected;
import com.ibm.ws.sib.mfp.control.ControlFlushed;
import com.ibm.ws.sib.mfp.control.ControlHighestGeneratedTick;
import com.ibm.ws.sib.mfp.control.ControlMessage;
import com.ibm.ws.sib.mfp.impl.ControlMessageFactory;
import com.ibm.ws.sib.mfp.control.ControlMessageType;
import com.ibm.ws.sib.mfp.control.ControlNotFlushed;
import com.ibm.ws.sib.mfp.control.ControlReject;
import com.ibm.ws.sib.mfp.control.ControlRequest;
import com.ibm.ws.sib.mfp.control.ControlRequestAck;
import com.ibm.ws.sib.mfp.control.ControlRequestFlush;
import com.ibm.ws.sib.mfp.control.ControlRequestHighestGeneratedTick;
import com.ibm.ws.sib.mfp.control.ControlResetRequestAck;
import com.ibm.ws.sib.mfp.control.ControlResetRequestAckAck;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPConnectionVersionException;
import com.ibm.ws.sib.processor.gd.AIRequestedTick;
import com.ibm.ws.sib.processor.gd.AIStream;
import com.ibm.ws.sib.processor.gd.AIValueTick;
import com.ibm.ws.sib.processor.gd.GDConfig;
import com.ibm.ws.sib.processor.impl.exceptions.SIMPNoResponseException;
import com.ibm.ws.sib.processor.impl.interfaces.BrowseCursor;
import com.ibm.ws.sib.processor.impl.interfaces.ControlHandler;
import com.ibm.ws.sib.processor.impl.interfaces.InputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.MPDestinationChangeListener;
import com.ibm.ws.sib.processor.impl.interfaces.RefillKey;
import com.ibm.ws.sib.processor.impl.interfaces.RemoteDispatchableKey;
import com.ibm.ws.sib.processor.impl.store.AIExecuteUpdate;
import com.ibm.ws.sib.processor.impl.store.AsyncUpdate;
import com.ibm.ws.sib.processor.impl.store.AsyncUpdateThread;
import com.ibm.ws.sib.processor.impl.store.filters.MessageSelectorFilter;
import com.ibm.ws.sib.processor.impl.store.items.AICompletedPrefixItem;
import com.ibm.ws.sib.processor.impl.store.items.AIMessageItem;
import com.ibm.ws.sib.processor.impl.store.items.AIProtocolItem;
import com.ibm.ws.sib.processor.impl.store.items.AOValue;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.impl.store.itemstreams.AIProtocolItemStream;
import com.ibm.ws.sib.processor.io.MPIO;
import com.ibm.ws.sib.processor.runtime.IndoubtAction;
import com.ibm.ws.sib.processor.runtime.impl.AttachedRemoteSubscriberControl;
import com.ibm.ws.sib.processor.runtime.impl.RemoteTopicSpaceControl;
import com.ibm.ws.sib.processor.utils.Queue;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.processor.utils.UserTrace;
import com.ibm.ws.sib.processor.utils.am.AbstractBatchedTimeoutEntry;
import com.ibm.ws.sib.processor.utils.am.BatchedTimeoutManager;
import com.ibm.ws.sib.processor.utils.am.BatchedTimeoutProcessor;
import com.ibm.ws.sib.processor.utils.am.MPAlarmManager;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.trm.dlm.Capability;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;

/**
 * An AnycastInputHandler implements the RME side of the assured anycast protocol for a given destination. It
 * also implements the RME side of the remote browse protocol.
 *
 * STREAM STATUS:
 *   States: Non-existent, Creating, Ready, NotFlushed, Flushing, Card1-Flush, Inact-Flush, Restart-Flush, Force-deleting
 *   Transitions:
 *     Non-existent -> Ready        -- starting with a non-empty persistent image and cons card > 1
 *                  -> NotFlushed   -- starting with a non-empty persistent image and cons card = 1
 *                  -> Creating     -- due to a consumer attach
 *                  -> Restart-flush - starting from stale backup, stream not reconstituted
 *     Ready        -> Non-existent -- stale stream: asked are you flushed due to msg for diff stream and were told flushed
 *                  -> Flushing     -- the RCD invoked rejectAll
 *                  -> Card1-Flush  -- flush requested, received ControlCardinalityInfo
 *                  -> Inact-Flush  -- flush requested due to inactivity
 *     Creating     -> Non-existent -- received ControlCardinalityInfo, or create stream failed due to, e.g., no reachability
 *                  -> Ready        -- received ControlNotFlushed
 *     NotFlushed   -> Flushing     -- the RCD invoked rejectAll
 *     Flushing     -> Non-existent -- received ControlFlushed
 *     Card1-Flush  -> Non-existent -- received ControlFlushed
 *     Inact-Flush  -> Non-existent -- received ControlFlushed
 *    Restart-Flush -> Non-existent -- received ControlFlushed
 *     Ready        -> Force-deleting- ControlFlushed arrived after ControlAreYouFlushed was sent or DME recovered from stale
 *       *          -> Force-deleting- forceFlushedAtTarget called after DME was deleted
 *   Force-deleting -> Non-existent -- RCD calls closeAllConsumersForFlushDone
 *
 * SYNCHRONIZATION:
 *
 *   Locking heirarchy:
 *          RemoteConsumerDispatcher.consumerPoints
 *            msgsToBeDelivered
 *            this
 *              _streamStatus
 *                _aiStream
 *              _persistentStreamState
 *              _forceDeleteMonitor
 *              
 * Control messages (completed, decision expected, request ack and reset request ack), as
 * well as RCD invocations (issueGet, accept, committed rolledback, reject) and non-browse data messages,
 * require a ready stream status.
 * To achieve this, these interactions hold a lock on streamStatus. Other methods (consumerAttaching, processing
 * ControlFlushed, ControlNotFlushed and CardinalityInfo) that modify streamStatus, hold a lock when they do so
 * and release it if they need to wait. Methods that do not depend on the stream do not deal with streamStatus.
 * 
 * The _aiStream object can only be changed while 'this' and _streamStatus are locked.
 * 
 */
public class AnycastInputHandler implements InputHandler, ControlHandler
{
  public static final long INVALID_ACCEPTED_ITEM_ID = -1;
  public static final long INVALID_COMPLETED_ITEM_ID = -1;

  // NLS for component
  private static final TraceNLS nls_mt =
    TraceNLS.getTraceNLS(SIMPConstants.TRACE_MESSAGE_RESOURCE_BUNDLE);
  
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  public static final long INVALID_TICK = -1L;

  private static int NUM_OF_BUCKETS = 4;
  
  private MPAlarmManager _alarmManager;

  private MessageProcessor _messageProcessor;
  private String _destName;
  private SIBUuid12 _destUuid;
  private SIBUuid8 _destMEUuid;
  
  /**
   * The gatheringTargetDestUuid is the id used along with the targetMEUuid to lookup a specific
   * AIH. Multiple AIHs (along with their associated streams/resources) are created. There is one
   * for each remoteME/consumerTarget pair. gatheringTargetDestUuid is null for standard consumers, and 
   * contains a destination uuid if it is a gathering consumer. The destination uuid can be an alias
   * representing a subset of queue points.
   */
  private SIBUuid12 _gatheringTargetDestUuid; 
  // Only contains the persistent stream state item stream, but it makes removing it more efficient
  private ItemStream _containerItemStream;
  // Can only set it if it is null, to protect from aync remove
  private AIProtocolItemStream _persistentStreamState;
  private boolean _streamStateRemoveNeedsToWait;
  private RemoteConsumerDispatcher _remoteCD;

  // Stream status object to serialize modification of the status of the stream
  protected AIStreamStatus _streamStatus;

  // True if this is a consumer cardinality = 1 destination
  private boolean _cardinalityOne;
  // Currently, ordered delivery is implied by cardinality one
  private boolean _deliverInOrder;
  // The time of arrival of a message minus the time the get was issued minus the wait time at the DME
  private long _currentRoundtripTime;

  // For each priority level, keep the tick of the highest delivered V/U for order enforcement
  private long[] _highestDeliveredExpressTicks;

  // Buffer to hold messages to be delivered. It holds messages intended to be put on the
  // remoteCD while we hold a lock on the stream. A separate block that only locks this
  // buffer does the actual delivery to the remoteCD.
  Queue msgsToBeDelivered = null;

  // Table of browse cursors by browse id
  private Hashtable _browseCursorTable;

  private AsyncUpdateThread _msUpdateThread;

  /**
   * A collection of 'FlushWorkItem' objects.
   * Various client calls can cause the stream to flush.
   * We should not flush if there are still indoubt transactions.
   * If there are any indoubt transactions, any call that would ordinarily
   * result in a flush is instead stored in this queue. The call will
   * then execute once the indoubt transactions have been resolved.
   * (see defect 269699)
   *
   * Access to this queue is protected by the _streamStatus monitor.
   */
  private LinkedList _flushWorkQueue;

  protected long _areYouFlushedId;

  private BatchedTimeoutManager _createStreamTOM;
  private BatchedTimeoutManager _requestFlushTOM;
  
  /**
   * 
   *    The following class variables are protected by synchronizing on 'this'
   * 
   */

  // _aiStream actually needs both 'this' and _streamStatus locked
  private volatile AIStream _aiStream;
  
  private CreateStreamTimeoutEntry _createStreamEntry;
  private RequestFlushTimeoutEntry _requestFlushEntry;
  private boolean _requestFlushFailed = false; // Flag to indicate we failed to send a flush

  /** Inactivity is defined as not having any consumer attached for cardinality > 1. As soon as this happens, the
   * timer is created, and as soon as a consumer attaches, the timer is cancelled (default = 500 seconds)
   */
  private Alarm _inactivityTimeoutAlarm;
  private AlarmListener _inactivityTimeoutListener;
  private boolean _inactivityAlarmCancelled = false;

  /**
   * 
   *    End of 'this' synchronized variables
   * 
   */

  private ReachabilityChangeListener _reachabilityChangeListener;

  /** The BDH that is handling this inputHandler */
  private BaseDestinationHandler _baseDestinationHandler;

  /**
   * A list of Runnables to be called when our stream
   * finally flushes.  Used by remote durable during
   * reconstitution.  We expect only one callback here,
   * but make it a list in case more general usage is
   * required.
   */
  private ArrayList _flushCallbacks = new ArrayList();

  private LockingBoolean _forceDeleteMonitor =  new LockingBoolean(false);

  // This flag is only set when destinationDeleted is called
  private boolean _needToRedriveDeleteDestination = false;

  /**
   * The number of requests that have been completed.
   * We cannot use the completedPrefix as this might increase faster
   * than the number of message requests.
   * This variable need not be thread safe.
   */
  private volatile long _totalCompletedRequests=0;

  /**
   * The number of requests that have been sent since reboot.
   * This variable need not be thread safe.
   */
  private volatile long _totalSentRequests=0;

  /**
   * The control adapter for this class. This is used if the AIH
   * is for a durable subscription
   */
  private AttachedRemoteSubscriberControl _control;


  /**
   * This is the number of FlushWorkItem objects
   * that are currently queued or executing in the
   * system.
   * If this is greater than zero then VALUE and REQUEST
   * ticks should NOT be written into the stream:
   * see defect 282249
   *
   * Synchronize on _streamStatus before use.
   */
  private int _numberOfFlushWorkItemsOnStream = 0;
  // No lock is held while performing flush work, this makes the work in progress visible
  private int _inProgressFlushWork = 0;

  // Lists of people (potential consumers) waiting for a flush or create to complete
  private ArrayList _flushWaiters = new ArrayList();
  private ArrayList _createWaiters = new ArrayList();
  
  
  // Standard debug/trace
  private static final TraceComponent tc =
    SibTr.register(
      AnycastInputHandler.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  /**
   * Constructor
   * @param destinationDefinition
   * @param messageProcessor
   * @param containerItemStream A stream that contains the stream used to persist L/A and L/D ticks.
   *                            Using the convention that if containerItemStream has size=0, then
   *                            there was no protocol stream and the create stream protocol needs to be started.
   * @param dmeId The uuid of the ME of the localization point. Needed to originate get requests.
   * @param msUpdateThread
   * @param baseDestinationHandler The desinationHandler for this AIH
   *
   */
  public AnycastInputHandler(
    String destName,
    SIBUuid12 destUuid,
    boolean receiveExclusive,
    MessageProcessor messageProcessor,
    ItemStream containerItemStream,
    SIBUuid8 dmeId,
    SIBUuid12 gatheringTargetDestUuid, 
    AsyncUpdateThread msUpdateThread,
    BaseDestinationHandler baseDestinationHandler,
    boolean restartFromStaleBackup)
    throws SIResourceException
    {
      this(
         destName,
         destUuid,
         receiveExclusive,
         messageProcessor,
         containerItemStream,
         dmeId,
         gatheringTargetDestUuid,
         msUpdateThread,
         baseDestinationHandler,
         restartFromStaleBackup,
         false);
    }


  /**
   * Constructor
   * @param destinationDefinition
   * @param messageProcessor
   * @param containerItemStream A stream that contains the stream used to persist L/A and L/D ticks.
   *                            Using the convention that if containerItemStream has size=0, then
   *                            there was no protocol stream and the create stream protocol needs to be started.
   * @param dmeId The uuid of the ME of the localization point. Needed to originate get requests.
   * @param msUpdateThread
   * @param baseDestinationHandler The desinationHandler for this AIH
   *
   * @param createControllable creates an AttachedRemoteSubscriber control for this AIH
   *
   */
  public AnycastInputHandler(
    String destName,
    SIBUuid12 destUuid,
    boolean receiveExclusive,
    MessageProcessor messageProcessor,
    ItemStream containerItemStream,
    SIBUuid8 dmeId,
    SIBUuid12 gatheringTargetDestUuid, 
    AsyncUpdateThread msUpdateThread,
    BaseDestinationHandler baseDestinationHandler,
    boolean restartFromStaleBackup,
    boolean createControllable)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AnycastInputHandler",
        new Object[]{destName,
                     destUuid,
                     Boolean.valueOf(receiveExclusive),
                     messageProcessor,
                     containerItemStream,
                     dmeId,
                     gatheringTargetDestUuid,
                     msUpdateThread,
                     baseDestinationHandler,
                     Boolean.valueOf(restartFromStaleBackup),
                     Boolean.valueOf(createControllable)});

    synchronized (this)
    {
      _messageProcessor = messageProcessor;
      _destName = destName;
      _destUuid = destUuid;
      _alarmManager = messageProcessor.getAlarmManager();
      _containerItemStream = containerItemStream;
      _destMEUuid = dmeId;
      _gatheringTargetDestUuid = gatheringTargetDestUuid;
      _msUpdateThread = msUpdateThread;
      _baseDestinationHandler = baseDestinationHandler;

      if (receiveExclusive)
      {
        _cardinalityOne = true;
      }
      else
      {
        _cardinalityOne = false;
      }

      _streamStatus =
        new AIStreamStatus(AIStreamStatus.STREAM_NON_EXISTENT);
      // Only need to deliver in order if cardinality is one
      _deliverInOrder = _cardinalityOne;

      // Next tick to initialize stream if necessary and to initialize highest delivered express ticks
      long tick = generateUniqueValue();
      // Exception is serious error, generateUniqueValue already processed exception so just throw it back

      msgsToBeDelivered = new Queue();

      _createStreamTOM =
        new BatchedTimeoutManager(
          NUM_OF_BUCKETS,
          _messageProcessor.getCustomProperties().get_create_stream_repetition_interval(),
          null,
          new CreateStreamTimeoutProcessor(),
          messageProcessor);
      _createStreamEntry = null; // to be initialized at create stream time
      // create requestFlushTOM now in case of a stale or flushStarted startup
      _requestFlushTOM =
        new BatchedTimeoutManager(
          NUM_OF_BUCKETS,
          _messageProcessor.getCustomProperties().get_request_flush_repetition_interval(),
          null,
          new RequestFlushTimeoutProcessor(),
          messageProcessor);
      _requestFlushEntry = null; // to be initialized at request flush time

      _areYouFlushedId = -1L;
      // to be initialized at are you flushed query time

      boolean startInactivityTimer = false;
      _streamStateRemoveNeedsToWait = false;
      NonLockingCursor cursor = null;
      try
      {
        // Recover the stream if there was one, i.e., the persistent stream state has size > 0
        // If there was a card=1 consumer before the warm start, we assume that the RCD takes notice of the card=1
        // restart and calls rejectAll, which eventually flushes the stream
        // If there was no stream, defer start of creation stream protocol until the RCD invokes consumer attaching,
        // regardless of consumer cardinality
        cursor = containerItemStream.newNonLockingItemStreamCursor(null);
        _persistentStreamState = (AIProtocolItemStream) cursor.next();
        // if (containerItemStream.getStatistics().getAvailableItemCount() > 0)
        if (_persistentStreamState != null)
        {
          // Instantiate stream and initialize with persistent state
          SIBUuid12 streamId = null; // will get it from the item stream
          _aiStream =
            new AIStream(
              streamId,
              _persistentStreamState,
              this,
              msUpdateThread,
              tick,
              null,
              true,
              messageProcessor);

          // If we're restarting with an AIStream in place we flush it immediately under some
          // conditions:
          //    - A flush was started before we were stopped
          //    - The destination is receiveExclusive, in which case the previous consumer must be
          //      detached now (as we restarted)
          //    - We're starting from an old backup, so any stream state is now invalid and needs
          //      clearing
          if (restartFromStaleBackup || _persistentStreamState.isFlushStarted() || _cardinalityOne)
          {
            _streamStatus.set(AIStreamStatus.STREAM_RESTART_FLUSH);

            sendRequestFlush(IndoubtAction.INDOUBT_DELETE); // Repetition will be started                            
          }
          // Otherwise, we keep the existing AIStream for future consumers, although we start the
          // inactivity timer so it'll be cleaned up eventually if no-one uses it.
          else
          {
            _streamStatus.set(AIStreamStatus.STREAM_READY);
            startInactivityTimer = true;
          }
        }
        else
        {
          _aiStream = null;
          _persistentStreamState = null;
        }
      }
      catch (MessageStoreException e)
      {
        // MessageStoreException shouldn't occur so FFDC.
        // Serious error, can't go on without a stream
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.AnycastInputHandler.AnycastInputHandler",
          "1:590:1.219.1.1",
          this);

        SibTr.exception(tc, e);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "AnycastInputHandler", "SIResourceException");

        throw new SIResourceException(e);
      }
      finally
      {
        if (cursor != null)
          cursor.finished();
      }

      _currentRoundtripTime = _messageProcessor.getCustomProperties().get_init_round_trip_time();

      // Initialize highestDeliveredExpress ticks to the tick generator
      // This should suffice for either a cold or a warm start
      _highestDeliveredExpressTicks =
        new long[SIMPConstants.MSG_HIGH_PRIORITY + 1];
      for (int i = 0; i < SIMPConstants.MSG_HIGH_PRIORITY; i++)
      {
        _highestDeliveredExpressTicks[i] = tick;
      }

      _browseCursorTable = new Hashtable();

      // start the timers
      start();

      if (startInactivityTimer)
      {
        startInactivityTimer();
      }

      // Create reachability change listener, cannot init its RCD until ours is
      _reachabilityChangeListener = new ReachabilityChangeListener();
      // Register reachability change listener with mp's main destination change listener
      messageProcessor.getDestinationChangeListener().addMPDestinationChangeListener(_reachabilityChangeListener);
    } // end synchronized(this)

    //finally, we create a control adapter if one is needed
    if(createControllable)
    {
      createControlAdapter();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AnycastInputHandler", this);
  }

  ////////////////////////////////////////////////////////////////////////
  // Admin methods
  ////////////////////////////////////////////////////////////////////////

  public void initRCD(RemoteConsumerDispatcher remoteCD)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "initRCD", remoteCD);
    _remoteCD = remoteCD;
    _reachabilityChangeListener.initRCD(remoteCD);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "initRCD");
  }

  public RemoteConsumerDispatcher getRCD()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getRCD");
      SibTr.exit(tc, "getRCD", _remoteCD);
    }
    return _remoteCD;
  }

  public synchronized void delete()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "delete");

    synchronized (_streamStatus)
    {
      try
      {
        if (!_streamStatus.test(AIStreamStatus.STREAM_NON_EXISTENT))
        {
          cleanupStreamState();
          _streamStatus.set(AIStreamStatus.STREAM_NON_EXISTENT);
        }
      }
      catch (SIResourceException e)
      {
        // No FFDC code needed; the exception has been processed
      }
      catch (SIErrorException e)
      {
        // No FFDC code needed; the exception has been processed
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "delete");
  }

  public void start()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "start");

    // start all the liveness timers
    _createStreamTOM.startTimer();
    _requestFlushTOM.startTimer();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "start");
  }

  public void stop()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "stop");

    // stop all the liveness timers
    _createStreamTOM.stopTimer();
    _requestFlushTOM.stopTimer();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "stop");
  }

  public synchronized void startInactivityTimer()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "startInactivityTimer");

    if (_inactivityTimeoutListener == null)
    {
      _inactivityTimeoutListener = new InactivityTimeoutListener();
    }
    _inactivityTimeoutAlarm = _alarmManager.create(_messageProcessor.getCustomProperties().get_sender_inactivity_timeout(), _inactivityTimeoutListener);
    _inactivityAlarmCancelled = false;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "startInactivityTimer");
  }

  public synchronized void cancelInactivityTimer()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "cancelInactivityTimer");

    // If we happened to be inactive and had an alarm created, we cancel it and get rid of it.
    if (_inactivityTimeoutAlarm != null)
    {
      _inactivityTimeoutAlarm.cancel();
      _inactivityTimeoutAlarm = null;
      _inactivityAlarmCancelled = true;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "cancelInactivityTimer");
  }

  public void changeReceiveExclusive(boolean isReceiveExclusive)
  {
    _cardinalityOne = isReceiveExclusive;
    _deliverInOrder = isReceiveExclusive;
  }

  ////////////////////////////////////////////////////////////////////////
  // InputHandler methods
  ////////////////////////////////////////////////////////////////////////

  /**
   * The handleMessage method is invoked by the Remote Message Receiver to
   * provide an incoming message.
   *
   * @param msg The message to be handled
   * @param transaction
   * @param producerSession
   * @param sourceCellule
   * @param targetCellule
   */
  public void handleMessage(
    MessageItem msg,
    TransactionCommon transaction,
    SIBUuid8 sourceMEUuid) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "handleMessage",
        new Object[] { msg, transaction, sourceMEUuid });

    // We want to put an arrival timestamp in the message as soon as possible
    msg.setCurrentMEArrivalTimestamp(System.currentTimeMillis());

    JsMessage jsMsg = msg.getMessage();
    if (jsMsg.isGuaranteedRemoteBrowse())
    {
      handleBrowseDataMessage(msg);
    }
    else
    {
      handleDataMessage(msg);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleMessage");
  }

  /////////////////////////////////////////////////////////////
  // ControlHandler methods
  /////////////////////////////////////////////////////////////

  public void handleControlMessage(SIBUuid8 sourceMEUuid, ControlMessage cMsg) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "handleControlMessage",
        new Object[] { sourceMEUuid, cMsg });

    // work out type of ControlMessage and process it
    ControlMessageType type = cMsg.getControlMessageType();

    if (type == ControlMessageType.COMPLETED)
    {
      processCompletedMessage(cMsg);
    }
    else if (type == ControlMessageType.DECISIONEXPECTED)
    {
      processDecisionExpectedMessage(cMsg);
    }
    else if (type == ControlMessageType.REQUESTHIGHESTGENERATEDTICK)
    {
      processRequestHighestGeneratedTickMessage(cMsg);
    }
    else if (type == ControlMessageType.BROWSEEND)
    {
      processBrowseEndMessage(cMsg);
    }
    else if (type == ControlMessageType.REQUESTACK)
    {
      processRequestAckMessage(cMsg);
    }
    else if (type == ControlMessageType.RESETREQUESTACK)
    {
      processResetRequestAckMessage(cMsg);
    }
    else if (type == ControlMessageType.FLUSHED)
    {
      processFlushedMessage(cMsg);
    }
    else if (type == ControlMessageType.NOTFLUSHED)
    {
      processNotFlushedMessage(cMsg);
    }
    else if (type == ControlMessageType.CARDINALITYINFO)
    {
      processCardinalityInfoMessage(cMsg);
    }
    else
    {
      // unknown type, log error
      SIErrorException e =
        new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.AnycastInputHandler",
              "1:862:1.219.1.1" },
            null));

      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AnycastInputHandler.handleControlMessage",
        "1:868:1.219.1.1",
        this);
      SibTr.exception(tc, e);

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
      new Object[] {
        "com.ibm.ws.sib.processor.impl.AnycastInputHandler",
        "1:875:1.219.1.1" });

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "handleControlMessage", e);
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleControlMessage");
  }

  /////////////////////////////////////////////////////////////
  // Methods invoked by Remote Consumer Dispatcher
  /////////////////////////////////////////////////////////////

  /**
   * The consumerAttaching method is invoked by the Remote Consumer Dispatcher. If cardinality = 1, the Remote
   * Consumer Dispatcher makes sure that it only calls this method when no other local consumer is attached.
   * When that happens, this method knows that it must start the create stream protocol. If cardinality > 1 and
   * no stream existed, this method starts the create stream protocol. Otherwise, this method is a no-op. That is,
   * an nth consumer that is attaching with an existing stream does not require any action.
   * This method returns immediately after sending create stream. The RCD knows it must wait until readyToIssueGet
   * is called on it when the ControlNotFlushed arrives in response to create stream. Notice that currently there
   * is no message type for the DME to send if it fails to create the stream. So, it is possible that the RCD will
   * wait forever. One option being considered is to handle this via flow control.
   * 
   * Concurrent calls to this method are not possible - the caller is synchronized. Therefore, only one createStream
   * can be in progress at a time.
   * @param responseTimeout 
   */
  public synchronized boolean consumerAttaching(long responseTimeout) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "consumerAttaching");
    
    // First, we check that the DME is reachable, technically the RCD has performed a
    // similar check, but that was based on WLM rather than an actual ME-ME connection.
    // Therefore, we make doubly sure that the DME is there by checking for a connection
    // before proceeding.
    MPIO mpio = _messageProcessor.getMPIO();
    if (!mpio.isMEReachable(_destMEUuid))
    {
      String meName = JsAdminUtils.getMENameByUuidForMessage(_destMEUuid.toString());
      if (meName == null)
        meName = _destMEUuid.toString();

      SIResourceException e = new SIResourceException(
        nls.getFormattedMessage(
          "ANYCAST_CANNOT_CREATE_STREAM_CWSIP0517",
          new Object[] {
            _destName,
            meName
          },
          null));

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "consumerAttaching", e);
      
      throw e;
    }

    boolean waitForCreate = false;
    boolean waitForFlush = false;
    synchronized (_streamStatus)
    {
      // If the DME is reachable (to get this far we must think it is)
      // but we already have a requestFlush registered that failed to be sent (probably
      // because the DME was down at the time) we resend it now, rather than wait for
      // the retry timer to kick in (much) later, as we're going to wait for a response.
      if((_requestFlushEntry != null) && _requestFlushFailed)
        redriveFailedRequestFlush();
      
      // The current status of the stream defines how we handle attaching consumers:
      //   - The stream exists      : nothing to do, just attach
      //   - No stream exists       : We create it and wait before attaching
      //   - The stream is flushing : Wait for the flush to complete before creating a new one
      //     but available            and then attaching
      //   - The stream is flushing : Fail the attach
      //     but not available
      
      switch (_streamStatus.get())
      {
        // The stream is ready for us, just attach
        case AIStreamStatus.STREAM_READY :
          // It's possible that the stream is scheduled to be flushed once all current indoubt
          // requests are resolved (from a previously attached consumer). If that's the case we
          // can cancel the scheduled flush (none of it has happened yet) and re-use the stream.
          // We're safe to do this in the cardinalityOne case becasue we know that the previous
          // consumer has already detached, otherwise the RCD wouldn't let us through, and we
          // allow consumers to re-attach even if there are indoubt messages from the previous
          // consumer.
          if(!checkStreamHasNoFlushWork())
          {
            //the stream is ready but there is flush work outstanding
            //Ideally we will now cancel the flush work since the flush is no
            //longer necessary
            
            // There's a slim chance that the scheduled flush work is currently underway,
            // in this case we can't simply cancel the work and attach. We also have no
            // idea if the flush work is about to happen, happening, or already happended,
            // so we can't just check the stream status either. The only option seems to
            // be, go to sleep for a little while and try again, although we can't hold the
            // AIH lock or the RCD consumerPoints lock while we do that either or we may
            // deadlock, so we have to drop right out and let the RCD retry.
            if(!cancelAllFlushWork())
            {
              if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "consumerAttaching", Boolean.valueOf(false));
              return false;
            }
          }
          break;
          
        // If stream is non-existent, send ControlCreateStream and wait before attaching
        // the consumer
        case AIStreamStatus.STREAM_NON_EXISTENT :
          _streamStatus.set(AIStreamStatus.STREAM_CREATING);
          waitForCreate = true;
          break;
          
        // If we are midflush due to inactivity/restart flush/normal flush then wait til the 
        // flushed comes in before allowing the create/attach to continue.
        case AIStreamStatus.STREAM_FLUSHING :
        case AIStreamStatus.STREAM_INACT_FLUSH :
        case AIStreamStatus.STREAM_RESTART_FLUSH :
          waitForFlush = true;
          waitForCreate = true;
          break;
          
        // If the stream is halfway through a flush because the destination has been deleted
        // there's no point waiting for the outcome (a deleted destination) so just fail the
        // attach now.
        case AIStreamStatus.STREAM_FORCE_DELETING :
        {
          // throw exception, should be rare
          SIResourceException e =
            new SIResourceException(
              nls.getFormattedMessage(
                "ANYCAST_STREAM_NOT_FLUSHED_CWSIP0512",
                new Object[]{_destName, _destMEUuid.toString()},
                null));
          SibTr.exception(tc, e);
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "consumerAttaching", e);
          throw e; // this will break out of the switch
        }
          
        // The stream can only be in this state within the confines of this method
        // and the caller ensures that concurrent calls are never made, so we should
        // never find the stream in this state.
        case AIStreamStatus.STREAM_CREATING:
        {
          SIErrorException e = new SIErrorException(
              nls.getFormattedMessage(
                "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                new Object[] {
                  "com.ibm.ws.sib.processor.impl.AnycastInputHandler.consumerAttaching",
                  "1:1031:1.219.1.1" },
                null));

          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.AnycastInputHandler.consumerAttaching",
            "1:1037:1.219.1.1",
            this);
          
          SibTr.exception(tc, e);

          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.AnycastInputHandler.consumerAttaching",
            "1:1045:1.219.1.1" });

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "consumerAttaching", e);
          throw e;
        }
      } // switch
    } // end synchronized(streamStatus)

    // To get this far we must already have a stream or about to try creating one, either
    // way we should cancel the inactivity timer until the last attached consumer detaches
    cancelInactivityTimer();
    
    // A flush on an existing stream is in progress. wait for this to complete before
    // we do anything else (i.e. create a new one)
    if (waitForFlush)
    {
      SIResourceException sendException = waitForResponse(responseTimeout, true);

      // Stream status will still be FLUSHING, throw exception and don't send create stream
      if (sendException != null)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "consumerAttaching", sendException);

        throw sendException;
      }
    }
    
    // No stream exists, send a create stream flow and wait for it to come back
    // from the DME (or cancel due to a failure)
    if (waitForCreate)
    {
      sendCreateStreamAndWait(responseTimeout);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "consumerAttaching", Boolean.valueOf(true));
    
    return true;
  }

  public RemoteConsumerDispatcher getResolvedDurableCD(RemoteConsumerDispatcher rcd) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getResolvedDurableCD", rcd);

    RemoteConsumerDispatcher returnRCD = null;
    synchronized(_streamStatus)
    {
      // We can't re-use an AIH/RCD if a flush has been initiated (or completed),
      // we have to wait for this to complete and start from fresh.
      if(_streamStatus.test(AIStreamStatus.STREAM_READY))
      {
        if(cancelAllFlushWork())
          returnRCD = rcd;
      }
      
      // If we can't use the RCD it may be bacause a previous flush failed to complete due
      // to the DME not being available. If it's available now, try again.
      if(returnRCD == null)
      {
        MPIO mpio = _messageProcessor.getMPIO();
        if (mpio.isMEReachable(_destMEUuid))
        {
          if((_requestFlushEntry != null) && _requestFlushFailed)
            redriveFailedRequestFlush();
        }
        else
        {
          String meName = JsAdminUtils.getMENameByUuidForMessage(_destMEUuid.toString());
          if (meName == null)
            meName = _destMEUuid.toString();

          SIResourceException e = new SIResourceException(
            nls.getFormattedMessage(
              "ANYCAST_CANNOT_CREATE_STREAM_CWSIP0517",
              new Object[] {
                _destName,
                meName
              },
              null));

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getResolvedDurableCD", e);
          
          throw e;
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getResolvedDurableCD", returnRCD);

    return returnRCD;
  }

  /**
   * This method is used by the RCD to notify us that the last consumer (under cardinality > 1) has detached.
   * We can then start the inactivity timer.
   */
  public void lastCardNConsumerDetached()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "lastCardNConsumerDetached");

    startInactivityTimer();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "lastCardNConsumerDetached");
  }

  /**
   * This issueGet method is the standard method invoked by the Remote Consumer Dispatcher to have a single get
   * request sent to the DME; it returns an IHKey instance for further reference to this request; it may return
   * null to indicate that the get request cannot be sent due to, e.g., ordering constraints. It may also return
   * null if the tick generator fails to return a tick. A third condition that can cause a null to be returned
   * is that the stream is being created. However, the RCD knows not to invoke issueGet after invoking
   * consumerAttaching until readyToIssueGet is called on it.
   *
   * @param criteria The criteria associated with the request
   * @param timeout The timeout on the request, either provided by the consumer or assigned by default by the RCD
   * @param ck A handle on the consumer to be cached and provided to the RCD on receipt of message
   * @param AOvalue SIB0113 - We can now issue a get for a specific request tick in order to restore old msgs
   */
  public AIStreamKey issueGet(SelectionCriteria[] criterias, long timeout, RemoteDispatchableKey ck, AOValue restoreValue, RefillKey refillCallback) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "issueGet",
        new Object[] { criterias, Long.valueOf(timeout), ck });

    AIStreamKey requestKey = null;
    long tick = INVALID_TICK;
    long rejectStartTick = INVALID_TICK;
    boolean sendRequest = false;

    synchronized (_streamStatus)
    {
      if (isStreamReady() && checkStreamHasNoFlushWork())
      {
        long issueTime = System.currentTimeMillis();
        
        AIRequestedTick airt = null;
        if (restoreValue!=null)
        {
          // SIB0113 - If we are trying to restore a particular message we need to rerequest
          // using the request tick that we originally used to ask for it.
          tick = restoreValue.getAIRequestTick();
          airt = new AIRequestedTick(tick, restoreValue, timeout);
        }
        else
        {
          tick = generateUniqueValue();
          airt = new AIRequestedTick(tick, criterias, ck, timeout, false, issueTime);
        }

        requestKey = new AIStreamKey(tick, ck, timeout, issueTime);
        
        // Set the latest tick we are requesting on the refillingCallback
        if (refillCallback!=null)
          refillCallback.setLatestTick(tick);

        // Insert the request in the stream. In general, this could be denied because there may be a previous L/R in
        // the stream that needs to turn to L/D. However, in the latest design the RME does not reject any ticks while
        // the consumer is connected, waiting instead until it disconnects to reject all ticks.

        // The stream can be assumed to be non-null since RCD will not call us before it gets readyToIssueGet

        // Insert Q/G in stream, which then issues sendRequest callback
        try
        {
          rejectStartTick =
            _aiStream.insertRequest(airt, tick, timeout);
          // could be negative (none rejected)
          // Tell the state stream to advance its latest tick
          _aiStream.setLatestTick(tick);
          _totalSentRequests++;
          sendRequest = true;
        }
        catch (SIErrorException e)
        {
          // FFDC
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.AnycastInputHandler.issueGet",
            "1:1232:1.219.1.1",
            this);

          if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          {
            SibTr.exception(tc, e);
            SibTr.exit(tc, "issueGet", e);
          }

          throw e;
        }
      }
      else
      {
        //there IS outstanding flush work
        // log an error
        SIErrorException e =
          new SIErrorException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0001",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.AnycastInputHandler",
                "1:1254:1.219.1.1" },
              null));
        // FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.AnycastInputHandler.issueGet",
          "1:1260:1.219.1.1",
          this);

        if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
          SibTr.exception(tc, e);
          SibTr.exit(tc, "issueGet", e);
        }

        throw e;
      }
    }

    if (sendRequest)
    {
      long rejectStartTicks[] = { rejectStartTick };
      long ticks[] = { tick };
      long timeouts[] = { timeout };
      sendRequest(rejectStartTicks, ticks, criterias, timeouts, SIMPConstants.CONTROL_MESSAGE_PRIORITY);
      
      if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())    
        SibTr.debug(UserTrace.tc_mt,       
         nls_mt.getFormattedMessage(
           "REMOTE_REQUEST_SENT_CWSJU0030",
           new Object[] {
             getDestName(),
             _messageProcessor.getMessagingEngineUuid(),
             _destMEUuid,
             _gatheringTargetDestUuid},
           null));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "issueGet", requestKey);

    return requestKey;
  }

  /**
   * This issueGet method allows multiple get requests to be batched and sent to the DME; it is used by the Remote
   * Consumer Dispatcher when pre-fetching messages, so the timeout is infinity.
   *
   * @param criteria
   * @param count
   * @param ck
   */
  public AIStreamKey[] issueGet(SelectionCriteria[] criterias, int count, RemoteDispatchableKey ck) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "issueGet",
        new Object[] { criterias, Integer.valueOf(count), ck });

    AIStreamKey[] keys = null;
    long[] rejectStartTicks = null;
    long[] ticks = null;
    long timeout = _messageProcessor.getCustomProperties().get_infinite_timeout();
    long[] timeouts = null;
    boolean sendRequest = false;

    synchronized (_streamStatus)
    {
      //we only issue a GET if the stream is ready and if
      //there are no flushes in progress
      if (isStreamReady() && checkStreamHasNoFlushWork())
      {
        rejectStartTicks = new long[count];
        ticks = new long[count];
        timeouts = new long[count];
        keys = new AIStreamKey[count];

        for (int i = 0; i < count; i++)
        {
          long tick = INVALID_TICK;

          tick = generateUniqueValue();

          ticks[i] = tick;
          long issueTime = System.currentTimeMillis();
          keys[i] = new AIStreamKey(tick, ck, timeout, issueTime);
          timeouts[i] = timeout;

          try
          {
            AIRequestedTick airt =
              new AIRequestedTick(tick, criterias, ck, timeout, false, issueTime);
            rejectStartTicks[i] =
              _aiStream.insertRequest(airt, tick, timeout);
            _aiStream.setLatestTick(tick);
            _totalSentRequests++;
            sendRequest = true;
          }
          catch (SIErrorException e)
          {
            // No FFDC code needed; insertRequest already processed the exception
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.exit(tc, "issueGet", null);

            return null;
          }
        }
      }//end if isStreamReady()
      else
      {
        //there IS outstanding flush work
        // log an error
        SIErrorException e =
          new SIErrorException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0001",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.AnycastInputHandler",
                "1:1373:1.219.1.1"},
              null));
        // FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.AnycastInputHandler.issueGet",
          "1:1379:1.219.1.1",
          this);

        if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
          SibTr.exception(tc, e);
          SibTr.exit(tc, "issueGet", e);
        }

        throw e;
      }
    }

    if (sendRequest)
    {
      sendRequest(rejectStartTicks, ticks, criterias, timeouts, SIMPConstants.CONTROL_MESSAGE_PRIORITY);
      
      if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())    
        SibTr.debug(UserTrace.tc_mt,       
         nls_mt.getFormattedMessage(
           "REMOTE_REQUEST_SENT_CWSJU0030",
           new Object[] {
             getDestName(),
             _messageProcessor.getMessagingEngineUuid(),
             _destMEUuid,
             _gatheringTargetDestUuid},
           null));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "issueGet", keys);

    return keys;
  }

  /**
   * The accept method is invoked by the Remote Consumer Dispatcher to turn the tick referred to by k to L/A; a value
   * of null for transaction for an assured message is not supported, since it would require persisting the L/A in the
   * I/O thread.
   *
   * @param key
   * @param transaction
   */
  public void accept(AIStreamKey key, TransactionCommon transaction)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "accept", new Object[] { key, transaction });

    // SendDispatcher acceptedDispatcher = new SendDispatcher();
    synchronized (_streamStatus)
    {
      if (isStreamFlushing())
      {
        // Allow accept to be called if stream is being flushed and do nothing.
        // The RCD may may have consumers that were in the middle of processing messages
        // and that expect to still be able to accept.
      }
      else if (isStreamReady())
      {
        AIProtocolItem acceptedItem =
          // aiStream.processAccepted(key.getTimestamp(), transaction, acceptedDispatcher);
          _aiStream.processAccepted(key.getTick(), transaction);
        if (acceptedItem != null)
        {
          // TODO this is a hack, to pass the id of the persistent L/A from here to the post-commit flow
          key.setAcceptedItem(acceptedItem);
        }
      }
    }//end sync

    // Send message dispatched by updateToAccepted outside of synchronized block
    // sending of accept is now batched 224469
    // acceptedDispatcher.dispatch();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "accept");
  }

  public void createControlAdapter()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.entry(tc, "createControlAdapter");

    //we first need to find the remote topic space control
    //for this ME
    PubSubOutputHandler psoh =
      this.getBaseDestinationHandler().getPubSubOutputHandler(_destMEUuid);
    RemoteTopicSpaceControl remotetopicSpaceControl = null;
    if(psoh==null || psoh.getControlAdapter()==null)
    {
      //there is no runtime control, so we create one
      remotetopicSpaceControl =
        new RemoteTopicSpaceControl(null, this, _messageProcessor);
    }
    else
    {
      //we have the control at the ready
      remotetopicSpaceControl = (RemoteTopicSpaceControl)psoh.getControlAdapter();
      remotetopicSpaceControl.setAnycastInputHandler(this);
    }
    _control = new AttachedRemoteSubscriberControl(_destName,
                                                   this,
                                                   _messageProcessor,
                                                   remotetopicSpaceControl);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createControlAdapter");
  }

  public AttachedRemoteSubscriberControl getControlAdapter()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getControlAdapter");
      SibTr.exit(tc, "getControlAdapter", _control);
    }
    return _control;
  }

  /**
   * The committed method is invoked by the accepted protocol item during an eventPostCommitAdd to update the protocol
   * stream.
   *
   * @param key
   */
  public void committed(AIStreamKey key)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "committed", new Object[] { key });

    // SendDispatcher acceptedDispatcher = new SendDispatcher();
    synchronized (_streamStatus)
    {
      if (isStreamFlushing())
      {
        // Allow committed to be called if stream is being flushed and do nothing.
        // The RCD may be in have consumers that are in the middle of processing messages
        // and that expect to be able to commit.
      }
      else if (isStreamReady())
      {
        // No need to determine qos of message, send ControlAccepted and update stream to L/A
        long tick = key.getTick();
        AIProtocolItem acceptedItem = key.getAcceptedItem();
        try
        {
          // aiStream.updateToAccepted(tick, acceptedItem, acceptedDispatcher);
          _aiStream.updateToAccepted(tick, acceptedItem);
        }
        catch (SIErrorException e)
        {
          // No FFDC code needed; ignore, updateToAccepted processed the exception
        }

        //see defect 269699:
        //Now that we have accepted a msg we may be in a position
        //to flush the stream (should a flush have come in earlier).
        //asynchronously drain the flushWorkQueue
        if(_flushWorkQueue!=null)
        {
          if(!streamHasIndoubtTransactions())
          {
            if(_flushWorkQueue.size()>0)
            {
              //there are items to drain.
              //This could be expensive so we perform the work
              //asynchronously
              Runnable flushWorker = new Runnable(){
                public void run(){
                  if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.entry(tc, "run");
                  AnycastInputHandler.this.drainFlushWorkQueue();
                  if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "run");
                }
              };
              try
              {
                _messageProcessor.startNewSystemThread(flushWorker);
              }
              catch(InterruptedException e )
              {
                FFDCFilter.processException(
                  e,
                  "com.ibm.ws.sib.processor.impl.AnycastInputHandler.committed",
                  "1:1564:1.219.1.1",
                  this);
              }
            }
          }//end if(inDoubt trans)
        }//end if(_flushWorkQueue)
      }
    }//end sync

    // Send message dispatched by updateToAccepted outside of synchronized block
    // sending of accept is now batched 224469
    // acceptedDispatcher.dispatch();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "committed");
  }

  /**
   * The rolledBack method is invoked by the accepted protocol item during an eventPostRollbackAdd to update the
   * protocol stream only if the item was unavailable, which means that there was a failure before the transaction in which
   * the item was added could complete.
   *
   * @param key
   */
  public void rolledback(AIStreamKey key)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "rolledback", new Object[] { key });

    SendDispatcher rejectedDispatcher = new SendDispatcher();
    synchronized (_streamStatus)
    {
      if (isStreamFlushing())
      {
        // Allow rolledback to be called if stream is being flushed and do nothing.
        // The RCD may be in have consumers that are in the middle of processing messages
        // and that expect to be able to rollback.
      }
      else if (isStreamReady())
      {
        try
        {
          _aiStream.updateToRejected(key.getTick(), rejectedDispatcher);
        }
        catch (SIErrorException e)
        {
          // No FFDC code needed; ignore, updateToRejected processed the exception
        }
        
        // PK63551 - Add logic to drain the flush queue in rollback case. 
        if(_flushWorkQueue!=null)
        {
          if(!streamHasIndoubtTransactions())
          {
            if(_flushWorkQueue.size()>0)
            {
              //there are items to drain. 
              //This could be expensive so we perform the work
              //asynchronously
              Runnable flushWorker = new Runnable(){
                public void run(){
                  if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.entry(tc, "run");
                  AnycastInputHandler.this.drainFlushWorkQueue();
                  if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "run");
                }
              };
              try
              {
                _messageProcessor.startNewSystemThread(flushWorker);
              }
              catch(InterruptedException e )
              {
                FFDCFilter.processException(
                  e,
                  "com.ibm.ws.sib.processor.impl.AnycastInputHandler.rolledback",
                  "1:1641:1.219.1.1",
                  this);
              }
            }
          }//end if(inDoubt trans)
        }//end if(_flushWorkQueue)              
      }
    }    
    
    // Send message dispatched by updateToRejected outside of synchronized block
    rejectedDispatcher.dispatch();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "rolledback");
  }

  /**
   * The reject method is invoked by the Remote Consumer Dispatcher to turn a tick to L/R and send a reject message
   * to the DME; this method is only used when consumer cardinality is greater than one.
   *
   * @param key
   */
  public void reject(AIStreamKey key)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reject", key);

    reject(key.getTick());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reject");
  }

  /* This reject method also allows the AIStream to reject a tick (e.g., at Q/G timeout) while making sure that it is
   * synchronized with the stream status.
   */
  public void reject(long tick)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reject", Long.valueOf(tick));

    SendDispatcher rejectedDispatcher = new SendDispatcher();
    synchronized (_streamStatus)
    {
      if (isStreamFlushing())
      {
        // Allow reject to be called if stream is being flushed and do nothing.
        // The RCD may be in detachConsumerPoint after rejectAll (which would have started a flush) and then
        // it tries to remove the items from its item stream via expiryCallback, which calls us.
        // Also, a test scenario has an expiry thread calling us with the stream not ready, which may be
        // avoided this way.
      }
      else if (isStreamReady())
      {
        try
        {
          _aiStream.updateToRejected(tick, rejectedDispatcher);
        }
        catch (SIErrorException e)
        {
          // No FFDC code needed; ignore, exception has been processed
        }
      }
    }

    // Send message dispatched by updateToRejected outside of synchronized block
    rejectedDispatcher.dispatch();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reject");
  }

  /**
   * The rejectAll method is invoked by the Remote Consumer Dispatcher when consumer cardinality is one and the consumer
   * disconnects; this also results in the flush protocol being started by the DME, when it sees that it only has the
   * completed prefix.
   */
  public void rejectAll()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "rejectAll");

    boolean sendRequestFlush = false;
    SendDispatcher rejectedDispatcher = new SendDispatcher();
    synchronized (_streamStatus)
    {
      if (_streamStatus.test(AIStreamStatus.STREAM_READY) ||
      _streamStatus.test(AIStreamStatus.STREAM_FORCE_DELETING))
      {
        // Allow this when force deleting since part of force deleting is to send request flush
        // If force deleting is due to unilateral flushed then no second flushed is expected in order to delete()
        _streamStatus.set(AIStreamStatus.STREAM_FLUSHING);

        try
        {
          _aiStream.updateAllToRejected(rejectedDispatcher);
          // repeated calls to this will not result in multiple sendRejects, but the rejects
          // are on a retry timer, so will get repeated if unanswered
        }
        catch (SIErrorException e)
        {
          // No FFDC code needed; updateAllToRejected already processed the exception
        }

        sendRequestFlush = true;
      }
      else if (isStreamFlushing())
      {
        // Allow rejectAll to be called if stream is being flushed and do nothing.
      }
      else if (_streamStatus.test(AIStreamStatus.STREAM_NON_EXISTENT))
      {
        // Allow the RCD constructor to rejectAll regardless of whether we had a stream or not
      }
      else
      {
        // log error
        SIErrorException e =
          new SIErrorException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0001",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.AnycastInputHandler",
                "1:1764:1.219.1.1" },
              null));

        // FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.AnycastInputHandler.rejectAll",
          "1:1771:1.219.1.1",
          this);

        SibTr.exception(tc, e);

        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.AnycastInputHandler",
          "1:1779:1.219.1.1" });

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "rejectAll", e);
        throw e;

      }
    }

    // Send message dispatched by updateToRejected outside of synchronized block
    rejectedDispatcher.dispatch();

    if (sendRequestFlush)
    {
      sendRequestFlush(IndoubtAction.INDOUBT_DELETE);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "rejectAll");
  }

  /**
   * The RCD calls this method back after we call closeAllConsumersForFlush and it is done closing all consumers.
   */
  public void closeAllConsumersForFlushDone()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "closeAllConsumersForFlushDone");

    if (!flushedWillDelete)
    {
      // Call cleanupStreamState under streamStatus lock if we are not non-existent
      // and set stream status to non-existent
      delete();
    }

    if (_forceDeleteMonitor != null)
    {
      synchronized (_forceDeleteMonitor)
      {
        if (_forceDeleteMonitor.booleanValue())
        {
          _forceDeleteMonitor.setBooleanValue(false);
          _forceDeleteMonitor.notifyAll();
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "closeAllConsumersForFlushDone");
  }

  /**
   * The getRoundTripTime method returns the value for the time it takes a request to go to the DME and its response to
   * get back.
   */
  public long getRoundTripTime()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getRoundTripTime");

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getRoundTripTime", Long.valueOf(_currentRoundtripTime));

    return _currentRoundtripTime;
  }

  /**
   *
   */
  /** This flag tells closeAllConsumersForFlushDone that a subsequent arriving flushed will call delete
   * and so it does not need to */
  private boolean flushedWillDelete = false;

  public boolean forceFlushAtTarget()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "forceFlushAtTarget");

    boolean returnValue = forceFlushAtTarget(false);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "forceFlushAtTarget", Boolean.valueOf(returnValue));
    return returnValue;
  }

  private boolean forceFlushAtTarget(boolean flushRelevant)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "forceFlushAtTarget", Boolean.valueOf(flushRelevant));

    boolean deleted = false;

    // if we need to wait for a flushed then it will call delete
    flushedWillDelete = flushRelevant;

    synchronized (_streamStatus)
    {
      if (_streamStatus.test(AIStreamStatus.STREAM_NON_EXISTENT))
      {
        // Nothing to do, just make sure that redrive delete is unset
        _needToRedriveDeleteDestination = false;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "forceFlushAtTarget", "Stream non existent");

        return true;
      }
      else if (isStreamFlushing())
      {
        // We were already force deleting or flushing so we don't have to do it again
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "forceFlushAtTarget", "Stream force deleting or flushing");

        return false;
      }
      else
      {
        _streamStatus.set(AIStreamStatus.STREAM_FORCE_DELETING);
        if (flushRelevant)
        {
          _needToRedriveDeleteDestination = true;
        }
      }
    }

    // forceDeleteMonitor = new MyBoolean(true);
    _forceDeleteMonitor.setBooleanValue(true);

    _remoteCD.closeAllConsumersForFlush();

    // If a flush is relevant, i.e. deleteDestination, then when flushed arrives we will wait
    // on force delete monitor there before calling delete and so we don't need to wait here
    if (!flushRelevant)
    {
      // this will be interrupted by closeAllConsumersForFlushDone
      waitOnForceDeleteMonitor();

      // flush not relevant means that closeAllConsumersForFlushDone called delete
      deleted = true;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "forceFlushAtTarget", Boolean.valueOf(deleted));

    return deleted;

    /*
    if (needToWaitForFlushed)
    {
      boolean waitForFlushed = false;

      // To know whether to wait or not, we check the status of the stream; if flushed already arrived
      // then it called delete(), which set the status to non-existent before notifying
      synchronized (streamStatus)
      {
        waitForFlushed = streamStatus.test(AIStreamStatus.STREAM_FORCE_DELETING) ||
                         streamStatus.test(AIStreamStatus.STREAM_FLUSHING);
      }

      if (waitForFlushed)
      {
        synchronized (this)
        {
          try
          {
            wait(); // this will be interrupted by processFlushed
          }
          catch(InterruptedException e)
          {
            // May not need to FFDC
            FFDCFilter.processException(
              e,
              "com.ibm.ws.sib.processor.impl.AnycastInputHandler.forceFlushAtTargetInternal",
              PROBE_ID_340,
              this);

            SibTr.exception(tc, e);
          }
        }
      }
    }
    else // !needToWaitForFlushed
    {
      // At this point, closeAllConsumersForFlushDone has cleaned up so we can just return
    }
    */
  }

  /**
   * @return true if there are no queued FlushWorkItem objects being
   * processed by the system.
   * Otherwise return false
   *
   * see defect 282249
   */
  private boolean checkStreamHasNoFlushWork()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "checkStreamHasNoFlushWork");

    boolean returnValue = true;

    synchronized(_streamStatus)
    {
      if(_numberOfFlushWorkItemsOnStream>0)
      {
          returnValue = false;
      }
    }//end sync

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "checkStreamHasNoFlushWork", Boolean.valueOf(returnValue));

    return returnValue;
  }

  private void waitOnForceDeleteMonitor()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "waitOnForceDeleteMonitor");

    if (_forceDeleteMonitor != null)
    {
      synchronized (_forceDeleteMonitor)
      {
        if (_forceDeleteMonitor.booleanValue())
        {
          try
          {
            _forceDeleteMonitor.wait(); // this will be interrupted by closeAllConsumersForFlushDone
          }
          catch(InterruptedException e)
          {
            // May not need to FFDC
            FFDCFilter.processException(
              e,
              "com.ibm.ws.sib.processor.impl.AnycastInputHandler.waitOnForceDeleteMonitor",
              "1:2015:1.219.1.1",
              this);

            SibTr.exception(tc, e);

            // forceDeleteMonitor = null;
          }
        }

        // forceDeleteMonitor = null;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "waitOnForceDeleteMonitor");
  }

  /**
   *
   */
  public boolean destinationDeleted()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "destinationDeleted");

    boolean deleted = false;

    deleted = forceFlushAtTarget(true);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "destinationDeleted", Boolean.valueOf(deleted));

    return deleted;
  }

  /**
   * The newBrowseCursor method is invoked by the Remote Consumer Dispatcher in its implementation of
   * BrowserSession.getBrowseCursor. This method creates a new AIBrowseCursor and returns it.
   *
   * @return The AIBrowseCursor implementation of NonLockingCursor, null if error
   */
  public BrowseCursor newBrowseCursor(SelectionCriteria criteria) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "newBrowseCursor", criteria);

    long nextBrowseId = 0L;
    BrowseCursor browseCursor = null;

    nextBrowseId = generateUniqueValue();

    Filter filter = null;
    //if there is a selection criteria then we have to
    //create a MessageStore filter
    if ( criteria != null &&
         ((criteria.getSelectorString() != null && !criteria.getSelectorString().equals("")) ||
         (criteria.getDiscriminator() != null && !criteria.getDiscriminator().equals(""))))
    {
      try
      {
        filter = new MessageSelectorFilter(_messageProcessor, criteria);
      }
      catch(SISelectorSyntaxException e)
      {
        //should not happen so ffdc
        // shouldn't occur so FFDC.
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.AnycastInputHandler.newBrowseCursor",
          "1:2084:1.219.1.1",
          this);

        SibTr.exception(tc, e);
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.AnycastInputHandler",
            "1:2091:1.219.1.1",
            e });


        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "newBrowseCursor", e);

        throw new SIResourceException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.AnycastInputHandler",
              "1:2103:1.219.1.1",
              e },
            null),
          e);
      }
    }//end if

    synchronized (_browseCursorTable)
    {
      browseCursor = new AIBrowseCursor(this, filter, nextBrowseId, _alarmManager);
      // use more elaborate key if needed, the browse id seems to suffice for now
      _browseCursorTable.put(Long.valueOf(nextBrowseId), browseCursor);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "newBrowseCursor", browseCursor);

    return browseCursor;
  }

  /////////////////////////////////////////////////////////////
  // Methods invoked by AIStream
  /////////////////////////////////////////////////////////////

  public void sendRequest(
    long[] rejectStartTicks,
    long[] ticks,
    SelectionCriteria[] criterias,
    long[] timeout,
    int priority)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "sendRequest",
        new Object[] { SIMPUtils.longArrayToString(rejectStartTicks),
                       SIMPUtils.longArrayToString(ticks),
                       criterias,
                       SIMPUtils.longArrayToString(timeout),
                       Integer.valueOf(priority)});

    try
    {
      // Check the AIStream has not been removed (stream closed)
      AIStream localAIStream = _aiStream;
      
      if (localAIStream != null)
      {
        ControlMessageFactory cmf;
        ControlRequest msg;
        // Create message
        cmf = MessageProcessor.getControlMessageFactory();
        msg = cmf.createNewControlRequest();
        initializeControlMessage(msg, _destMEUuid, _gatheringTargetDestUuid, localAIStream.getStreamId());
  
        String[] filters = null;
        int[] selectorDomains = null;
        String[] discriminators = null;
  
        if (criterias != null)
        {
          filters = new String[criterias.length];
          selectorDomains = new int[criterias.length];
          discriminators = new String[criterias.length];
  
  
          for (int i = 0; i < criterias.length; i++)
          {
            filters[i] = criterias[i].getSelectorString();
            selectorDomains[i] = criterias[i].getSelectorDomain().toInt();
            discriminators[i] = criterias[i].getDiscriminator();
          }
        }
        else
        {
          // Need to initialise with empty arrays
          filters = new String[]{};
          discriminators = new String[]{};
          selectorDomains = new int[]{};
        }
  
        msg.setRejectStartTick(rejectStartTicks);
        msg.setGetTick(ticks);
        msg.setFilter(filters);
        msg.setSelectorDomain(selectorDomains);
        msg.setControlDiscriminator(discriminators);
  
        msg.setTimeout(timeout);
  
        // Send message to destination using RemoteMessageTransmitter
        // Notice that we are implicitly sending rejects but they don't say if they are recovery
        MPIO mpio = _messageProcessor.getMPIO();
        mpio.sendToMe(
            _destMEUuid,
          priority,
          msg);
      }
    }
    catch (MessageCreateFailedException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AnycastInputHandler.sendRequest",
        "1:2206:1.219.1.1",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendRequest");
  }

  public void sendAccept(long[] ticks)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendAccept", SIMPUtils.longArrayToString(ticks));

    try
    {
      // Check the AIStream has not been removed (stream closed)
      AIStream localAIStream = _aiStream;
      
      if (localAIStream != null)
      {
        ControlMessageFactory cmf;
        ControlAccept msg;
        // Create message
        cmf = MessageProcessor.getControlMessageFactory();
        msg = cmf.createNewControlAccept();
        initializeControlMessage(msg, _destMEUuid, _gatheringTargetDestUuid, localAIStream.getStreamId());
  
        msg.setTick(ticks);
  
        // Send message to destination using RemoteMessageTransmitter
        MPIO mpio = _messageProcessor.getMPIO();
        mpio.sendToMe(
            _destMEUuid,
          SIMPConstants.CONTROL_MESSAGE_PRIORITY,
          msg);
      }
    }
    catch (MessageCreateFailedException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AnycastInputHandler.sendAccept",
        "1:2249:1.219.1.1",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendAccept");
  }

  public void sendReject(long[] startTicks, long[] endTicks, long[] unlockCounts, boolean recovery)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "sendReject",
        new Object[] { SIMPUtils.longArrayToString(startTicks),
                       SIMPUtils.longArrayToString(endTicks),
                       SIMPUtils.longArrayToString(unlockCounts),
                       Boolean.valueOf(recovery)});

    try
    {
      // Check the AIStream has not been removed (stream closed)
      AIStream localAIStream = _aiStream;
      
      if (localAIStream != null)
      {
        // Create message
        ControlMessageFactory cmf = MessageProcessor.getControlMessageFactory();
        ControlReject msg = cmf.createNewControlReject();
        initializeControlMessage(msg, _destMEUuid, _gatheringTargetDestUuid, localAIStream.getStreamId());
  
        msg.setStartTick(startTicks);
        msg.setEndTick(endTicks);
        msg.setRecovery(recovery);
        msg.setRMEUnlockCount(unlockCounts);
  
        // Send message to destination using RemoteMessageTransmitter
        MPIO mpio = _messageProcessor.getMPIO();
        mpio.sendToMe(
            _destMEUuid,
          SIMPConstants.CONTROL_MESSAGE_PRIORITY,
          msg);
      } 
    }
    catch (MessageCreateFailedException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AnycastInputHandler.sendReject",
        "1:2299:1.219.1.1",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendReject");
  }

  public void resolve(AIStreamKey tsKey) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "resolve", tsKey);

    _remoteCD.resolve(tsKey);
  }

  public void sendCompleted(long[] startTicks, long[] endTicks)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendCompleted",
        new Object[] { SIMPUtils.longArrayToString(startTicks),
                       SIMPUtils.longArrayToString(endTicks) });

    try
    {
      // Check the AIStream has not been removed (stream closed)
      AIStream localAIStream = _aiStream;
      
      if (localAIStream != null)
      {
        ControlMessageFactory cmf;
        ControlCompleted msg;
        // Create message
        cmf = MessageProcessor.getControlMessageFactory();
        msg = cmf.createNewControlCompleted();
        initializeControlMessage(msg, _destMEUuid, _gatheringTargetDestUuid, localAIStream.getStreamId());
  
        msg.setStartTick(startTicks);
        msg.setEndTick(endTicks);
  
        // Send message to destination using RemoteMessageTransmitter
        MPIO mpio = _messageProcessor.getMPIO();
        mpio.sendToMe(
            _destMEUuid,
          SIMPConstants.CONTROL_MESSAGE_PRIORITY,
          msg);
      }
    }
    catch (MessageCreateFailedException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AnycastInputHandler.sendCompleted",
        "1:2353:1.219.1.1",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendCompleted");
  }

  /**
   * The sendAreYouFlushed method is called when a message (control or value) for an unknown stream arrives.
   * 
   * Caller has _streamStatus locked
   */
  public void sendAreYouFlushed(SIBUuid12 streamId)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendAreYouFlushed", streamId);

    try
    {
      ControlMessageFactory cmf;
      ControlAreYouFlushed msg;
      // Create message
      cmf = MessageProcessor.getControlMessageFactory();
      msg = cmf.createNewControlAreYouFlushed();
      initializeControlMessage(msg,_destMEUuid, _gatheringTargetDestUuid, streamId);

      _areYouFlushedId = generateUniqueValue();
      msg.setRequestID(_areYouFlushedId);

      // Send message to destination using RemoteMessageTransmitter
      MPIO mpio = _messageProcessor.getMPIO();
      mpio.sendToMe(
          _destMEUuid,
        SIMPConstants.CONTROL_MESSAGE_PRIORITY,
        msg);
    }
    catch (Exception e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AnycastInputHandler.sendAreYouFlushed",
        "1:2396:1.219.1.1",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendAreYouFlushed");
  }

  public long getUniqueLockID(int storageStrategy) throws PersistenceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getUniqueLockID");
    long uniqueId = _messageProcessor.getMessageStore().getUniqueLockID(storageStrategy);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getUniqueLockID", Long.valueOf(uniqueId));
    return uniqueId;
  }

  public long getTotalSentRequests()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTotalSentRequests");

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTotalSentRequests", Long.valueOf(_totalSentRequests));
    return _totalSentRequests;
  }

  /////////////////////////////////////////////////////////////
  // Other send methods
  /////////////////////////////////////////////////////////////

  public void sendHighestGeneratedTick(long requestId, long tick)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "sendHighestGeneratedTick",
        new Object[] { Long.valueOf(requestId), Long.valueOf(tick)});

    try
    {
      // Check the AIStream has not been removed (stream closed)
      AIStream localAIStream = _aiStream;
      
      if (localAIStream != null)
      {
        ControlMessageFactory cmf;
        ControlHighestGeneratedTick msg;
        // Create message
        cmf = MessageProcessor.getControlMessageFactory();
        msg = cmf.createNewControlHighestGeneratedTick();
        initializeControlMessage(msg,_destMEUuid, _gatheringTargetDestUuid, localAIStream.getStreamId());
  
        msg.setRequestID(requestId);
        msg.setTick(tick);
  
        // Send message to destination using RemoteMessageTransmitter
        MPIO mpio = _messageProcessor.getMPIO();
        mpio.sendToMe(
            _destMEUuid,
          SIMPConstants.CONTROL_MESSAGE_PRIORITY,
          msg);
      }
    }
    catch (MessageCreateFailedException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AnycastInputHandler.sendHighestGeneratedTick",
        "1:2467:1.219.1.1",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendHighestGeneratedTick");
  }

  public void sendDecision(long tick)
  {
    // TODO this method may not be needed if we decide to send completed instead
  }

  public void sendResetRequestAckAck(long dmeVersion)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendResetRequestAckAck", Long.valueOf(dmeVersion));

    try
    {
      // Check the AIStream has not been removed (stream closed)
      AIStream localAIStream = _aiStream;
      
      if (localAIStream != null)
      {
        ControlMessageFactory cmf;
        ControlResetRequestAckAck msg;
        // Create message
        cmf = MessageProcessor.getControlMessageFactory();
        msg = cmf.createNewControlResetRequestAckAck();
        initializeControlMessage(msg, _destMEUuid, _gatheringTargetDestUuid, localAIStream.getStreamId());
  
        msg.setDMEVersion(dmeVersion);
  
        // Send message to destination using RemoteMessageTransmitter
        MPIO mpio = _messageProcessor.getMPIO();
        mpio.sendToMe(
            _destMEUuid,
          SIMPConstants.CONTROL_MESSAGE_PRIORITY,
          msg);
      }
    }
    catch (MessageCreateFailedException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AnycastInputHandler.sendResetRequestAckAck",
        "1:2515:1.219.1.1",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendResetRequestAckAck");
  }

  /* sendCreateStream is only invoked by this class but we want to be able to override it, so we make it public */
  public void sendCreateStream(long createStreamId) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendCreateStream", Long.valueOf(createStreamId));

    MPIO mpio = _messageProcessor.getMPIO();
    if (!mpio.isMEReachable(_destMEUuid))
    {
      String meName = JsAdminUtils.getMENameByUuidForMessage(_destMEUuid.toString());
      if (meName == null)
        meName = _destMEUuid.toString();

      SIResourceException e = new SIResourceException(
        nls.getFormattedMessage(
          "ANYCAST_CANNOT_CREATE_STREAM_CWSIP0517",
          new Object[] {
            _destName,
            meName
          },
          null));

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "sendCreateStream", e);
    }

    if (_gatheringTargetDestUuid != null && !mpio.isCompatibleME(_destMEUuid, ProtocolVersion.VERSION_7))
    {
      String meName = JsAdminUtils.getMENameByUuidForMessage(_destMEUuid.toString());
      if (meName == null)
        meName = _destMEUuid.toString();

      SIMPConnectionVersionException e = new SIMPConnectionVersionException(
          nls.getFormattedMessage(
              "GATHERING_ME_VERSION_INCOMPATIBLE_CWSIP0856",
              new Object[] {
                _destName,
                meName
              },
              null));

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "sendCreateStream", e);
    }
      
    try
    {
      ControlMessageFactory cmf;
      ControlCreateStream msg;
      // Create message
      cmf = MessageProcessor.getControlMessageFactory();
      msg = cmf.createNewControlCreateStream();
      initializeControlMessage(msg, _destMEUuid, _gatheringTargetDestUuid, null);

      msg.setRequestID(createStreamId);
      msg.setDurableSubName(null);
      msg.setDurableDiscriminator(null);
      msg.setDurableSelector(null);
      msg.setDurableSelectorDomain(0);
      msg.setSecurityUserid(null);
      // Set the flag that signals whether this is
      // the privileged SIBServerSubject.
      msg.setSecurityUseridSentBySystem(false); // the userid is null

      //defect 259036
      msg.setCloned(false);
      msg.setNoLocal(false);

      // Send message to destination using RemoteMessageTransmitter
      mpio.sendToMe(
          _destMEUuid,
        SIMPConstants.CONTROL_MESSAGE_PRIORITY,
        msg);

      synchronized (_streamStatus)
      {
        if (_streamStatus.test(AIStreamStatus.STREAM_NON_EXISTENT))
          _streamStatus.set(AIStreamStatus.STREAM_CREATING);
      }
    }
    catch (Exception e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AnycastInputHandler.sendCreateStream",
        "1:2609:1.219.1.1",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendCreateStream");
  }

  public synchronized void sendRequestFlush(IndoubtAction indoubtAction)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendRequestFlush", indoubtAction);

    // cancel any inactivity alarm as we're 'upgrading' to a flush now
    if(_inactivityTimeoutAlarm != null)
    {
      _inactivityTimeoutAlarm.cancel();
      _inactivityTimeoutAlarm = null;
    }    
    
    // Only send RequestFlush if entry is null, otherwise RequestFlush has been sent and is being repeated
    if (_requestFlushEntry != null)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "sendRequestFlush");
      return;
    }

    // Persist flag if not set
    if (!_persistentStreamState.isFlushStarted())
    {
      synchronized (_persistentStreamState)
      {
        _persistentStreamState.setFlushStarted(true);
        PersistentStreamAsyncUpdate psau = new PersistentStreamAsyncUpdate();
        // Need to run the persistence synchronously; the point of persisting the
        // flag is that we know we sent the flush request; it's no good to wait
        // until the persist commits to send the request, it may not be sent at
        // all and we still will not remember we need to send it at restart
        _streamStateRemoveNeedsToWait = true;
        AIExecuteUpdate xu = new AIExecuteUpdate(psau, _messageProcessor);
        xu.run();
        // doEnqueueWork(psau);
      }
    }

    try
    {
      SIResourceException ex = null;
      
      // We need a unique id for the requestFlush, this can fail (e.g. the DB is down).
      // Rather than forget about the flush request we carry on with a dummy id as if the
      // DME was down, i.e. we still schedule the flush for a later date (where we'll
      // try to get an id again)
      long id = -1;
      try
      {
        id = generateUniqueValue();
      }
      catch(SIResourceException e1)
      {
        // No FFDC code needed
        
        ex = e1;
      }

      _requestFlushEntry = new RequestFlushTimeoutEntry(id);
      //Set the TimeoutEntry now
      _requestFlushTOM.addTimeoutEntry(_requestFlushEntry);

      // HACK: (TODO fix in a later release?)
      // It's currently possible to add an alarm against a stopped ME (!), so the alarm will never actually
      // be woken up or cancelled. Therefore we need to check after we've added it to see if that's the case
      // and pretend that we were cancelled here.
      if(!_messageProcessor.isStarted())
      {
        SIResourceException e =
          new SIResourceException(
            nls.getFormattedMessage(
              "ANYCAST_STREAM_UNAVAILABLE_CWSIP0481",
              new Object[]{_destName, _destMEUuid.toString()},
              null));
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "sendRequestFlush", e);

        throw e;
      }

      // If we failed to get an id, rethrow the exception to drop into the error
      // handling below.
      if(ex != null)
        throw ex;
      
      sendRequestFlush(_requestFlushEntry.requestFlushId, indoubtAction);
      _requestFlushTOM.updateTimeout(_messageProcessor.getCustomProperties().get_request_flush_repetition_interval());
      _requestFlushFailed = false;
      
    }
    catch (SIResourceException e)
    {
      // No FFDC code needed

      // If the ME is unreachable and can't send the message at the moment we need
      // to retry, but we'll do this more sedately (if the DME comes back up and someone
      // comes along and tries to attach while we're 'sleeping' it'll instigate a re-request
      // immediately).
      _requestFlushTOM.updateTimeout(_messageProcessor.getCustomProperties().get_request_flush_slow_repetition_interval());
      _requestFlushFailed = true;

      // The DME isn't reachable so wakeup any consumers currently sat waiting for it
      // while trying to attach (consumers already attached will get kicked off by a
      // change in reachability, but we can't be guaranteed of that when flushing as the
      // DME may not be advertising the queue anymore)
      AnycastInputHandler.this.wakeUpWaiters(true, e);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "sendRequestFlush", e);
      
      // Swallow the exception as there's nothing anyone else can do now as we're past
      // the point of no return for kicking off the flush.
      return;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendRequestFlush");
  }

  protected void sendRequestFlush(long requestFlushId, IndoubtAction indoubtAction)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendRequestFlush", new Object[] {Long.valueOf(requestFlushId), indoubtAction });

    MPIO mpio = _messageProcessor.getMPIO();
    if (!mpio.isMEReachable(_destMEUuid))
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "sendRequestFlush", null);

      throw new SIResourceException(
        nls.getFormattedMessage(
          "ANYCAST_CANNOT_REQUEST_FLUSH_CWSIP0518",
          new Object[] {
            _destName,
            _destMEUuid
          },
          null));
    }

    try
    {
      // Check the AIStream has not been removed (stream closed)
      AIStream localAIStream = _aiStream;
      
      if (localAIStream != null)
      {
        // Create message
        ControlMessageFactory cmf = MessageProcessor.getControlMessageFactory();
        ControlRequestFlush msg = cmf.createNewControlRequestFlush();
        initializeControlMessage(msg, _destMEUuid, _gatheringTargetDestUuid, localAIStream.getStreamId());
  
        msg.setRequestID(requestFlushId);
  
        if (indoubtAction == IndoubtAction.INDOUBT_DELETE)
        {
          msg.setIndoubtDiscard(true);
        }
        else if (indoubtAction == IndoubtAction.INDOUBT_LEAVE)
        {
          msg.setIndoubtDiscard(false);
        }
  
        // Send message to destination using RemoteMessageTransmitter
        mpio.sendToMe(
            _destMEUuid,
          SIMPConstants.CONTROL_MESSAGE_PRIORITY,
          msg);
      }
    }
    catch (Exception e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AnycastInputHandler.sendRequestFlush",
        "1:2795:1.219.1.1",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendRequestFlush");
  }

  /*
   * Redrive a failed requestFlush (and any scheduled rejects)
   * 
   * This method must be called while _streamStatus is held
   */
  private void redriveFailedRequestFlush() throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "redriveFailedRequestFlush");
    
    // If the requestFlushEntry has no id we must have failed to generate one the
    // first time it was scheduled. Try to generate one now, if we fail again we'll
    // throw an SIResourceException and drop out.
    if(_requestFlushEntry.requestFlushId == -1)
      _requestFlushEntry.requestFlushId = generateUniqueValue();
    
    // If we failed to send a flush, we may have failed to send any reject messages,
    // redrive any we have scheduled
    if(_aiStream != null)
      _aiStream.resendScheduledRejects();
    
    // Try and send the requestFlush, if we fail we throw the back to the caller,
    // We don't need to wake anyone else up as there's no chance of them starting to wait
    // since we last did an DME check (above).
    sendRequestFlush(_requestFlushEntry.requestFlushId, IndoubtAction.INDOUBT_DELETE);
    _requestFlushTOM.updateTimeout(_messageProcessor.getCustomProperties().get_request_flush_repetition_interval());
    _requestFlushFailed = false;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "redriveFailedRequestFlush");
  }
  
  protected final void initializeControlMessage(
    ControlMessage msg,
    SIBUuid8 remoteMEId,
    SIBUuid12 gatheringTargetDestUuid, 
    SIBUuid12 streamId)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "initializeControlMessage",
        new Object[] { msg, remoteMEId, streamId });

    msg.setPriority(SIMPConstants.CONTROL_MESSAGE_PRIORITY);
    msg.setReliability(SIMPConstants.CONTROL_MESSAGE_RELIABILITY);
    
    SIMPUtils.setGuaranteedDeliveryProperties(msg,
        _messageProcessor.getMessagingEngineUuid(), 
        remoteMEId,
        streamId,
        gatheringTargetDestUuid,
        _destUuid,
        ProtocolType.ANYCASTOUTPUT,
        GDConfig.PROTOCOL_VERSION);    
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "initializeControlMessage");
  }

  /////////////////////////////////////////////////////////////
  // Methods invoked by AIBrowseCursor
  /////////////////////////////////////////////////////////////

  public void sendBrowseGet(long browseId, long seqNum, Filter filter)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "sendBrowseGet",
        new Object[] { Long.valueOf(browseId), Long.valueOf(seqNum), filter });

    try
    {
      ControlMessageFactory cmf;
      ControlBrowseGet msg;
      // Create message
      cmf = MessageProcessor.getControlMessageFactory();
      msg = cmf.createNewControlBrowseGet();
      initializeControlMessage(msg,_destMEUuid, _gatheringTargetDestUuid,  null);

      msg.setBrowseID(browseId);
      msg.setSequenceNumber(seqNum);
//      String stringFilter = ((filter == null) ? null : filter.toString());
      // Only handle MessageSelectorFilters here, anything else maps to "no filter"
      if(filter != null && filter instanceof MessageSelectorFilter)
      {
        MessageSelectorFilter msf = (MessageSelectorFilter)filter;
        String selectorString = msf.getSelectorString();
        msg.setFilter(selectorString);
        msg.setSelectorDomain(msf.getDomain().toInt());
        msg.setControlDiscriminator(msf.getDiscriminator());
      }
      else
      {
        msg.setFilter("");
        msg.setSelectorDomain(0);
        msg.setControlDiscriminator(null);
     }

      // Send message to destination using RemoteMessageTransmitter
      MPIO mpio = _messageProcessor.getMPIO();
      mpio.sendToMe(
          _destMEUuid,
        SIMPConstants.CONTROL_MESSAGE_PRIORITY,
        msg);
    }
    catch (MessageCreateFailedException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AnycastInputHandler.sendBrowseGet",
        "1:2916:1.219.1.1",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendBrowseGet");
  }

  public void sendBrowseStatus(int status, long browseId)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "sendBrowseStatus",
        new Object[] { Integer.valueOf(status), Long.valueOf(browseId)});

    try
    {
      ControlMessageFactory cmf;
      ControlBrowseStatus msg;
      // Create message
      cmf = MessageProcessor.getControlMessageFactory();
      msg = cmf.createNewControlBrowseStatus();
      initializeControlMessage(msg, _destMEUuid, _gatheringTargetDestUuid, null);

      msg.setStatus(status);
      msg.setBrowseID(browseId);

      // Send message to destination using RemoteMessageTransmitter
      MPIO mpio = _messageProcessor.getMPIO();
      mpio.sendToMe(
          _destMEUuid,
        SIMPConstants.CONTROL_MESSAGE_PRIORITY,
        msg);
    }
    catch (MessageCreateFailedException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AnycastInputHandler.sendBrowseStatus",
        "1:2957:1.219.1.1",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendBrowseStatus");
  }

  public void removeBrowseCursor(long browseId)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "removeBrowseCursor",
        new Object[] { Long.valueOf(browseId)});

    synchronized (_browseCursorTable)
    {
      Long bid = Long.valueOf(browseId);
      AIBrowseCursor browseCursor = (AIBrowseCursor) _browseCursorTable.get(bid);

      if (browseCursor != null)
      {
        _browseCursorTable.remove(bid);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeBrowseCursor");
  }

  public long getCompletedRequestCount()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getCompletedRequestCount");

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getCompletedRequestCount", Long.valueOf(_totalCompletedRequests));
    return _totalCompletedRequests;
  }

  /////////////////////////////////////////////////////////////
  // Private methods
  /////////////////////////////////////////////////////////////

  /**
   * Caller holds 'this' lock
   */
  private void sendCreateStreamAndWait(long responseTimeout) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendCreateStreamAndWait");

    SIResourceException sendException = null;

    // Only one thread should be able to create the stream at a time
    // By now, the stream status is assumed to be 'creating'. Since the stream was non-existent,
    // there is nothing to lock

    _createStreamEntry = new CreateStreamTimeoutEntry();
    try
    {
      sendCreateStream(_createStreamEntry.createStreamId);
    }
    catch (SIResourceException e)
    {
      // No FFDC code needed
      
      // This simply means we don't have a connection to the DME at this point
      // in time

      // Set the stream status back to non-existent in case the stream
      // was never created and we are stuck in the creating state.
      synchronized(_streamStatus)
      {
        _streamStatus.set(AIStreamStatus.STREAM_NON_EXISTENT);
      }


      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "sendCreateStreamAndWait", e);

      throw e;
    }
    // We only want to add the entry if the send succeeded, if the dme is unreachable and can't send
    // then we should not repeat
    // Under certain circumstances (unit test with mock dme), the NotFlushed response may arrive before
    // timeout entry has been added, that should not occur in a real remote get setting
    _createStreamTOM.addTimeoutEntry(_createStreamEntry);
    
    // HACK: (TODO fix in a later release?)
    // It's currently possible to add an alarm against a stopped ME (!), so the alarm will never actually
    // be woken up or cancelled. Therefore we need to check after we've added it to see if that's the case
    // and pretend that we were cancelled here.
    if(!_messageProcessor.isStarted())
    {
      SIResourceException e =
        new SIResourceException(
          nls.getFormattedMessage(
            "ANYCAST_STREAM_UNAVAILABLE_CWSIP0481",
            new Object[]{_destName, _destMEUuid.toString()},
            null));
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "sendCreateStreamAndWait", e);

      throw e;
    }

    // 243428; restart timer in case it was stopped by a flush
    _createStreamTOM.startTimer();

    // Now wait for something to happen (a NotFlushed arrives or maybe we timeout)
    sendException = waitForResponse(responseTimeout, false);

    if (sendException == null)
    {
      synchronized (_streamStatus)
      {
        // If we received ControlCardinalityInfo, then stream status is now non-existent
        if (_streamStatus.test(AIStreamStatus.STREAM_NON_EXISTENT))
        {
          // throw exception
          SIResourceException e =
            new SIResourceException(
              nls.getFormattedMessage(
                "CONSUMERCARDINALITY_LIMIT_REACHED_CWSIP0514",
                new Object[]{_destName, _destMEUuid.toString()},
                null));
          
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendCreateStreamAndWait", e);

          throw e;
        }

        // If we received ControlNotFlushed, then we should be ready for business,
        // Otherwise something went wrong
        if (!_streamStatus.test(AIStreamStatus.STREAM_READY))
        {
          // This should not occur, so log error
          SIErrorException e =
            new SIErrorException(
              nls.getFormattedMessage(
                "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                new Object[] {
                  "com.ibm.ws.sib.processor.impl.AnycastInputHandler",
                    "1:3105:1.219.1.1" },
                null));

          // FFDC
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.AnycastInputHandler.sendCreateStreamAndWait",
              "1:3112:1.219.1.1",
            this);

          SibTr.exception(tc, e);

          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.AnycastInputHandler",
                  "1:3120:1.219.1.1" });
          
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendCreateStreamAndWait", e);
          
          throw e;
        }
      }
    }
    else
    {
      // Set the stream status back to non-existent in case the stream
      // was never created and we are stuck in the creating state.
      synchronized(_streamStatus)
      {
        _streamStatus.set(AIStreamStatus.STREAM_NON_EXISTENT);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "sendCreateStreamAndWait", null);

      throw sendException;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendCreateStreamAndWait");
  }

  /**
   * set the redelivered count of the tick which is in value state.
   * @param tick The tick in the stream
   */
  public final void incrementUnlockCount(long tick)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "incrementUnlockCount", Long.valueOf(tick));

    synchronized (_streamStatus)
    {
      if (_streamStatus.test(AIStreamStatus.STREAM_READY))
        _aiStream.incrementUnlockCount(tick);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "incrementUnlockCount");
  }

  private void handleDataMessage(MessageItem msg) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "handleDataMessage", msg);

    long arrivalTime = System.currentTimeMillis();

    JsMessage jsMsg = msg.getMessage();
    AIMessageItem mainMsgItem = new AIMessageItem(jsMsg);
    mainMsgItem.setCurrentMEArrivalTimestamp(msg.getCurrentMEArrivalTimestamp());
    mainMsgItem.setFromRemoteME(msg.isFromRemoteME());
    mainMsgItem.setFromRemoteBus(msg.isFromRemoteBus());
    AIRequestedTick rt = null;

    // Obtain tick and prevTick from message
    long tick = jsMsg.getGuaranteedRemoteGetValueTick();
    long prevTick = jsMsg.getGuaranteedRemoteGetPrevTick();

    long startTick = jsMsg.getGuaranteedRemoteGetStartTick();

    SendDispatcher requestToValueDispatcher = new SendDispatcher();
    // SendDispatcher acceptedDispatcher = new SendDispatcher();

    boolean deliverToRCD = true;
    boolean restoring = false;

    // Hold message to be delivered until stream is released as _streamStatus can
    // be taken if a new request is issued (due to readAhead) as a result of handling this
    // message.
    synchronized (msgsToBeDelivered)
    {
      synchronized (_streamStatus)
      {
        if (okToProcess(jsMsg))
        {
          // First process completed ticks preceding value tick in message
          if (startTick < tick && tick > 0)
          {
            try
            {
              _aiStream.updateToCompleted(startTick, tick-1);
            }
            catch(SIErrorException e)
            {
              // No FFDC code needed, updateToCompleted processed the exception
            }
          }

          // Now process the value tick
          Reliability rel = jsMsg.getReliability();
          boolean isExpress =
            rel.compareTo(Reliability.RELIABLE_NONPERSISTENT) <= 0;
          int priority = jsMsg.getPriority().intValue();

          boolean canDeliver = true;
          if (_deliverInOrder)
          {
            if (isExpress)
            {
              canDeliver = canDeliverExpressInOrder(tick, priority);
            }
            else
            {
              // If need to deliver in order, need to check in stream by prevTick
              canDeliver =
                _aiStream.canDeliverAssuredInOrder(tick, prevTick, priority);
            }
          }

          // Update stream to V/U. The tick keeps the in-memory Java reference of the message object
          // If we crash, we lose all knowledge of the message anyway
          try
          {
            rt = _aiStream.updateRequestToValue(tick, mainMsgItem, canDeliver, requestToValueDispatcher);
          }
          catch (SIErrorException e)
          {
            // No FFDC code needed; can't continue, updateRequestToValue processed the exception

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.exit(tc, "handleDataMessage");

            return;
          }

          if (rt != null) // the tick was a request
          {
            RemoteDispatchableKey cKey = rt.getRemoteDispatchableKey();

            AOValue restoreValue =  rt.getRestoringAOValue();
            if (restoreValue!=null)
            {
              restoring = true;
              mainMsgItem.setRestoredTargetAOValue(restoreValue);
            }
            
            // Receiving a value for a RequestedTick means we satisfied a request. Whether
            // we then go on to accept or reject, this request is still "completed". If
            // we dont do this then we cannot calculate the correct currentActiveRequests value.
            _totalCompletedRequests++;
            
            // Put message to remote consumer dispatcher
            if (canDeliver)
            {
              AIStreamKey tsKey =
                new AIStreamKey(
                  tick,
                  cKey,
                  rt.getOriginalTimeout(),
                  rt.getIssueTime());
              mainMsgItem.setInfo(this, tsKey);

              msgsToBeDelivered.enqueue(mainMsgItem);

              if (_deliverInOrder)
              {
                if (isExpress)
                {
                  setHighestDeliveredExpress(priority, tick);
                }
                else
                {
                  if (!restoring)
                  {
                    List undeliveredList =
                      _aiStream.findUndeliveredList(tick, jsMsg);
                    if (undeliveredList != null)
                    {
                      // Undelivered list is a list of AIValueTick
                      int size = undeliveredList.size();
    
                      for (int i = 0; i < size; i++)
                      {
                        AIValueTick vt = (AIValueTick) undeliveredList.get(i);
                        AIMessageItem nextMsgItem = vt.getMsg();
                        tsKey =
                          new AIStreamKey(
                            vt.getTick(),
                            vt.getRemoteDispatchableKey(),
                            vt.getOriginalTimeout(),
                            vt.getIssueTime());
                        nextMsgItem.setInfo(this, tsKey);
                        msgsToBeDelivered.enqueue(nextMsgItem);
                      }
    
                      _aiStream.markListDelivered(undeliveredList);
                    }
                  }
                }
              }
            }
            else if (isExpress && _deliverInOrder)
            {
              // Couldn't deliver an express, out of order message, turn tick to L/A non-persistently
              try
              {
                // aiStream.updateToAccepted(tick, null, acceptedDispatcher);
                _aiStream.updateToAccepted(tick, null);
              }
              catch (SIErrorException e)
              {
                // No FFDC code needed; ignore, updateToAccepted processed the exception
              }
            }
          }
          else // rt == null, the tick was not a request
          {
            deliverToRCD = false;
          }
        }
      } // end synchronized(streamStatus)

      // Send messages dispatched by updateRequestToValue and updateToAccepted outside of synchronized block
      requestToValueDispatcher.dispatch();
      // sending of accept is now batched 224469
      // acceptedDispatcher.dispatch();

      // Now that we are not locking the stream, we can deliver to the remoteCD
      if (deliverToRCD)
      {
        ArrayList<AIMessageItem> msgList = null;
        while (!msgsToBeDelivered.isEmpty())
        {
          if (msgList == null)
          {
            msgList = new ArrayList<AIMessageItem>(msgsToBeDelivered.size());
          }
          AIMessageItem nextMsgItem = (AIMessageItem)msgsToBeDelivered.dequeue();
          msgList.add(nextMsgItem);
        }

        if (msgList != null)
        {
          _remoteCD.put(msgList, restoring);
        }
      }
    } // synch(msgsToBeDelivered)    

    if (rt != null) // TODO : And if not restore AOValueLink
    {
      // Obtain waitTime from message and use it to compute current round trip time
      long waitTime = jsMsg.getGuaranteedRemoteGetWaitTime();
      updateRoundtripTime(rt.getIssueTime(), arrivalTime, waitTime);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleDataMessage");
  }

  /**
   * Determine if the stream is ready, process exception if not.
   * This assumes that streamStatus is locked
   *
   */
  private boolean isStreamReady()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isStreamReady");

    boolean isStreamReady = false;
    if ( _streamStatus.test(AIStreamStatus.STREAM_READY))
    {
      isStreamReady = true;
    }
    else
    {
      // log error
      SIErrorException e =
        new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.AnycastInputHandler",
              "1:3400:1.219.1.1" },
            null));
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AnycastInputHandler.isStreamReady",
        "1:3406:1.219.1.1",
        this);
      SibTr.exception(tc, e);

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
      new Object[] {
        "com.ibm.ws.sib.processor.impl.AnycastInputHandler",
        "1:3413:1.219.1.1" });

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "isStreamReady", e);
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isStreamReady", Boolean.valueOf(isStreamReady));

    return isStreamReady;
  }

  /*
   * A public, synchronized, version of isStreamFlushing()
   */
  public boolean isStreamBeingFlushed()
  {
    synchronized(_streamStatus)
    {
      return isStreamFlushing();
    }
  }
  
  private boolean isStreamFlushing()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isStreamFlushing");

    boolean isStreamFlushing = false;
    if (_streamStatus.test(AIStreamStatus.STREAM_FLUSHING) ||
        _streamStatus.test(AIStreamStatus.STREAM_INACT_FLUSH) ||
        _streamStatus.test(AIStreamStatus.STREAM_RESTART_FLUSH) ||
        _streamStatus.test(AIStreamStatus.STREAM_FORCE_DELETING))
    {
      isStreamFlushing = true;
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isStreamFlushing", Boolean.valueOf(isStreamFlushing));

    return isStreamFlushing;
  }

  /**
   * Determine whether a message can be processed.
   * This depends on whether its sream id matches ours and we
   * are ready. If the message does not match our stream or our
   * stream is non-existent, send are you flushed.
   * If the stream has any flush work items in process then we
   * cannot accept this message.
   */
  private boolean okToProcess(JsMessage jsMsg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "okToProcess", jsMsg);

    boolean okToProcess = false;
    SIBUuid12 incomingStreamId = jsMsg.getGuaranteedStreamUUID();
    //we can o
    if (_streamStatus.test(AIStreamStatus.STREAM_READY) &&
        checkStreamHasNoFlushWork())
    {
      SIBUuid12 streamId = _aiStream.getStreamId();
      if (!incomingStreamId.equals(streamId))
      {
        // If a message arrives for a different stream then we ask if our stream is flushed. If the DME
        // says not flushed then this was a bogus message. If the DME says flushed then our stream must be
        // stale and we flush it.
        sendAreYouFlushed(streamId);

        okToProcess = false;
      }
      else
      {
        okToProcess = true;
      }
    }
    else if (_streamStatus.test(AIStreamStatus.STREAM_NON_EXISTENT))
    {
      // This could happen if our stream was stale and we flushed it, and messages for the other stream keep
      // coming in. We ask if the other stream is flushed. If it is we do nothing. If it is not and we are
      // still non existent, we reconstitute.
      sendAreYouFlushed(incomingStreamId);

      okToProcess = false;
    }
    else
    {
      // ignore message until we are ready or flushed
      okToProcess = false;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "okToProcess", Boolean.valueOf(okToProcess));

    return okToProcess;
  }


  /**
   * Determine whether a message can be processed. This depends on whether its sream id matches ours and we
   * are ready. If the message does not match our stream or our stream is non-existent, send are you flushed.
   *
   */
  private boolean okToProcess(ControlMessage cMsg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "okToProcess", cMsg);

    boolean okToProcess = false;
    SIBUuid12 incomingStreamId = cMsg.getGuaranteedStreamUUID();
    if (_streamStatus.test(AIStreamStatus.STREAM_READY))
    {
      SIBUuid12 streamId = _aiStream.getStreamId();
      if (!incomingStreamId.equals(streamId))
      {
        // If a message arrives for a different stream then we ask if our stream is flushed. If the DME
        // says not flushed then this was a bogus message. If the DME says flushed then our stream must be
        // stale and we flush it.
        sendAreYouFlushed(streamId);

        okToProcess = false;
      }
      else
      {
        okToProcess = true;
      }
    }
    else if (_streamStatus.test(AIStreamStatus.STREAM_NON_EXISTENT))
    {
      // This could happen if our stream was stale and we flushed it, and messages for the other stream keep
      // coming in. We ask if the other stream is flushed. If it is we do nothing. If it is not and we are
      // still non existent, we reconstitute.
      sendAreYouFlushed(incomingStreamId);

      okToProcess = false;
    }
    else
    {
      // ignore message until we are ready or flushed
      okToProcess = false;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "okToProcess", Boolean.valueOf(okToProcess));

    return okToProcess;
  }

  /**
   * Determine whether an express V/U tick can be delivered in order. Any express V/U ahead of tick will suffice.
   * We assume that if there is such a V/U, it will have been delivered.
   *
   * @return Whether or not the V/U tick can be delivered
   */
  private boolean canDeliverExpressInOrder(long tick, int priority)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "canDeliverExpressInOrder",
        new Object[] { Long.valueOf(tick), Integer.valueOf(priority)});

    boolean canDeliver = tick > _highestDeliveredExpressTicks[priority];

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "canDeliverExpressInOrder", Boolean.valueOf(canDeliver));

    return canDeliver;
  }

  private void setHighestDeliveredExpress(int priority, long tick)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "setHighestDeliveredExpress",
        new Object[] { Integer.valueOf(priority), Long.valueOf(tick)});

    _highestDeliveredExpressTicks[priority] = tick;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setHighestDeliveredExpress");
  }

  private void handleBrowseDataMessage(MessageItem msg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "handleBrowseDataMessage", msg);

    JsMessage jsMsg = msg.getMessage();
    long browseId = jsMsg.getGuaranteedRemoteBrowseID();
    AIBrowseCursor browseCursor = null;
    synchronized (_browseCursorTable)
    {
      browseCursor = (AIBrowseCursor) _browseCursorTable.get(Long.valueOf(browseId));
    }
    if (browseCursor == null)
    {
      // TODO make sure that it's ok to drop message
      // this situation can happen if a browse data arrives after recovery
    }
    else
    {
      browseCursor.put(msg);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleBrowseDataMessage");
  }

  private void processCompletedMessage(ControlMessage cMsg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processCompletedMessage", cMsg);

    synchronized (_streamStatus)
    {
      // allow to process if stream is flushing
      if (okToProcess(cMsg) || isStreamFlushing())
      {
        ControlCompleted cm = (ControlCompleted) cMsg;
        // Obtain tick from message
        long[] startTicks = cm.getStartTick();
        long[] endTicks = cm.getEndTick();

        // Write L/D to the stream
        int len = startTicks.length;
        for (int i = 0; i < len; i++)
        {
          try
          {
            _aiStream.updateToCompleted(startTicks[i], endTicks[i]);
          }
          catch (SIResourceException e)
          {
            // No FFDC code needed; exception has been processed, break
            break;
          }
          catch (SIErrorException e)
          {
            // No FFDC code needed; exception has been processed, break
            break;
          }
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processCompletedMessage");
  }

  private void processDecisionExpectedMessage(ControlMessage cMsg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processDecisionExpectedMessage", cMsg);

    SendDispatcher decisionExpectedDispatcher = new SendDispatcher();
    synchronized (_streamStatus)
    {
      // c: Even after a flush has been started the DME may need to know the state of certain
      // requests so that it can get into a state that allows it to flush. For example, if it has locked
      // message requests but did not receive a reject for them (due to the DME being down at the time
      // that the RME rejected them).
      if (okToProcess(cMsg) || isStreamFlushing())
      {
        long[] ticks = ((ControlDecisionExpected) cMsg).getTick();
        int numOfTicks = ticks.length;
        for (int i = 0; i < numOfTicks; i++)
        {
          _aiStream.processDecisionExpected(ticks[i], decisionExpectedDispatcher);
        }
      }
    }

    // Send message dispatched by processDecisionExpected outside of synchronized block
    decisionExpectedDispatcher.dispatch();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processDecisionExpectedMessage");
  }

  /**
   * A ControlRequestHighestGeneratedTick message is sent by the DME at recovery to determine the boundary up
   * until which any ticks that were not persisted as V/U will be recovered as L/D.
   *
   */
  private void processRequestHighestGeneratedTickMessage(ControlMessage cMsg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processRequestHighestGeneratedTickMessage", cMsg);

    synchronized (_streamStatus)
    {
      if (okToProcess(cMsg))
      {
        long incomingRequestId =
          ((ControlRequestHighestGeneratedTick) cMsg).getRequestID();
        long highestGeneratedTick = _aiStream.getLatestTick();

        sendHighestGeneratedTick(incomingRequestId, highestGeneratedTick);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processRequestHighestGeneratedTickMessage");
  }

  private void processBrowseEndMessage(ControlMessage cMsg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processBrowseEndMessage", cMsg);

    ControlBrowseEnd bem = (ControlBrowseEnd) cMsg;
    long browseId = bem.getBrowseID();
    AIBrowseCursor browseCursor = null;
    synchronized (_browseCursorTable)
    {
      browseCursor = (AIBrowseCursor) _browseCursorTable.get(Long.valueOf(browseId));
    }
    if (browseCursor == null)
    {
      // this situation can happen if a browse data arrives after recovery
    }
    else
    {
      int reasonCode = bem.getExceptionCode();
      switch (reasonCode)
      {
        case SIMPConstants.BROWSE_OK :
          browseCursor.endBrowse();
          break;
        case SIMPConstants.BROWSE_STORE_EXCEPTION :
        case SIMPConstants.BROWSE_OUT_OF_ORDER :
        case SIMPConstants.BROWSE_BAD_FILTER :
          browseCursor.browseFailed(reasonCode);
          break;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processBrowseEndMessage");
  }

  /**
   * A ControlRequestAck message tells the RME to slow down its get repetition timeout since we now know that the
   * DME has received the request.
   *
   */
  private void processRequestAckMessage(ControlMessage cMsg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processRequestAckMessage", cMsg);

    synchronized (_streamStatus)
    {
      if (okToProcess(cMsg))
      {
        ControlRequestAck ram = (ControlRequestAck) cMsg;
        long[] ticks = ram.getTick();
        long dmeVersion = ram.getDMEVersion();

        int numOfTicks = ticks.length;
        for (int i = 0; i < numOfTicks; i++)
        {
          _aiStream.processRequestAck(ticks[i], dmeVersion);
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processRequestAckMessage");
  }

  /**
   * A ControlResetRequestAck message tells the RME to start re-sending get requests with an eager timeout since
   * the DME has crashed and recovered.
   *
   */
  private void processResetRequestAckMessage(ControlMessage cMsg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processResetRequestAckMessage", cMsg);

    SendDispatcher resetDispatcher = new SendDispatcher();
    synchronized (_streamStatus)
    {
      if (okToProcess(cMsg))
      {
        long dmeVersion = ((ControlResetRequestAck) cMsg).getDMEVersion();

        // Ask the stream to do it
        _aiStream.processResetRequestAck(dmeVersion, resetDispatcher);
      }
    }

    // Send message dispatched by processResetRequestAck outside of synchronized block
    resetDispatcher.dispatch();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processResetRequestAckMessage");
  }

  /**
   * A ControlFlushed message tells the RME: (1) that the stream for which a data message arrived and did not exist
   * has indeed been flushed (this, in response to having sent ControlAreYouFlushed), or (2) that the stream is to
   * be discarded after the inactivity timer expired and a ControlAreYouFlushed was sent, or (3) that the stream is
   * being flushed by the DME because it only has the completed prefix left (especially for card=1, when the RCD calls
   * rejectAll).
   * NOTE: since the DME is flushed we cannot really serve any outstanding transactions.
   * Therefore we do not wrapper this in a flush work item.
   */
  private void processFlushedMessage(ControlMessage cMsg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processFlushedMessage", cMsg);

    synchronized(this)
    {
      // Check if this is due to a RequestFlush and stop repetition if so
      // Since ControlFlushed does not carry a requestId, we cannot check against it
      if (_requestFlushEntry != null)
      {
        _requestFlushTOM.removeTimeoutEntry(_requestFlushEntry);
        _requestFlushEntry = null;
        _requestFlushFailed = false;
      }
    }

    // A flushed message can arrive either due to sendAreYouFlushed,
    //   if so either (1) a message that arrived did not match and we asked whether we were flushed,
    //                (2) we were non existent and we asked whether the stream for a message that arrived
    //                    was flushed
    // or due to sendRequestFlush
    //   if so either (1) we were in rejectAll, or
    //                (2) we received ControlCardinalityInfo, or
    //                (3) the inactivity timeout expired, or
    //                (4) we restarted from stale or trying to flush

    boolean cleanup = false;
    boolean closeConsumers = false;
    boolean redriveDelete = false;

    synchronized(_streamStatus)
    {
      switch (_streamStatus.get())
      {
        case AIStreamStatus.STREAM_FLUSHING : 
        case AIStreamStatus.STREAM_INACT_FLUSH :   // inactivity timeout
        case AIStreamStatus.STREAM_RESTART_FLUSH : // requested flush at restart
          cleanup = true;
          // While we hold the lock, see if we need to delete the destination now it's flushed
          redriveDelete = _needToRedriveDeleteDestination;
          break;

        case AIStreamStatus.STREAM_READY :
        {
          SIBUuid12 flushedStreamId =
            ((ControlFlushed) cMsg).getGuaranteedStreamUUID();
          SIBUuid12 streamId = _aiStream.getStreamId();

          if (flushedStreamId.equals(streamId))
          {
            // We weren't expecting to be told that the DME was flushed on the current stream,
            // but it can happen (e.g. the destination is being deleted) so defer the cleanup
            // and first notify the RCD to close all consumers. 
            _streamStatus.set(AIStreamStatus.STREAM_FORCE_DELETING);

            // when RCD is done, it will call back and we can then clean up
            // make sure that when RCD calls closeAllConsumersForFlushDone, it does call delete()
            // i.e., no further flushed is expected or coming
            flushedWillDelete = false;
            
            // We can't close the consumers here as we hold the _streamStatus lock, which
            // is lower down the hierarchy than the RCD.conusmeroints lock (which
            // RCD.closeAllConsumersForFlush() takes)
            closeConsumers = true;
          }
          // else, the stream (not ours) for a message that arrived was flushed, do nothing
          break;
        }

        default :
          break;
      }
    } // synchrnized
   
    // now we don't hold the lock, close consumers if we have to
    if(closeConsumers)
      _remoteCD.closeAllConsumersForFlush();

    if (cleanup)
    {
      try
      {
        // First make sure that a force flush at target has finished closing consumers
        // this will be interrupted by closeAllConsumersForFlushDone
        waitOnForceDeleteMonitor();
        // Call cleanupStreamState under streamStatus lock if we are not non-existent
        // and set stream status to non-existent
        delete();
      }
      catch (SIErrorException e)
      {
        // No FFDC code needed; the exception has been processed, ignore
      }
    }

    // Wake up anyone waiting for the flush to be completed (any attaching consumers)
    wakeUpWaiters(true, null);

    // Wakeup any callbacks waiting for the flush
    synchronized (_flushCallbacks)
    {
      for(Iterator i=_flushCallbacks.iterator(); i.hasNext(); )
      {
        Runnable next = (Runnable) i.next();
        try
        {
          _messageProcessor.startNewSystemThread(next);
        } catch(InterruptedException e) {
          // FFDC since this shouldn't happen
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.AnycastInputHandler.processFlushedMessage",
            "1:3938:1.219.1.1",
            this);
          SibTr.exception(tc, e);
        }
      }
      _flushCallbacks.clear();
    }

    // If we flushed because of destination delete, redrive the process if necessary
    if (redriveDelete)
    {
      // we assume concurrency issues are handled, including only one deletion thread
      // running at a time, and a possible race with the thread that invoked the
      // destinationDeleted call that requested the flush we are processing
      _baseDestinationHandler.getDestinationManager().startAsynchDeletion();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processFlushedMessage");
  }

  /**
   * Must be called inside synchronized(_streamStatus) and synchronized(this)
   */
  private void cleanupStreamState() throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "cleanupStreamState");

    stop();
    
    // Check the AIStream has not been removed (stream closed)
    AIStream localAIStream = _aiStream;
    
    // There's a chance we haven't actually got an _aiStream at this point
    // (I think we could be in the process of creating one - outstanding requestFlush 
    if (localAIStream != null)
    {
      localAIStream.stop();
      localAIStream.updateAllToCompleted();
    }
    
    if(_control!=null)
    {
      //deregister the control so that subsequent streams for this me
      //can register
      _control.deregisterControlAdapterMBean();
      _control = null;
    }

    // Schedule removal of persistent stream; reference is not nulled out until commit, before then any attempt
    // to set it will be an exception
    RemovePersistentStream update = new RemovePersistentStream();
    AIExecuteUpdate xu = new AIExecuteUpdate(update, _messageProcessor);
    xu.run();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "cleanupStreamState");
  }

  /**
   * A ControlNotFlushed message is used: (1) as a response to ControlCreateStream when a stream is
   * first being created; (2) to confirm that our non-existent stream for which a message arrives
   * does indeed exist and needs to be reconstituted; this can happen if we send ControlAreYouFlushed
   * because we did not have a stream; (3) to confirm that our stream is still valid after an inactivity
   * timeout
   */
  private synchronized void processNotFlushedMessage(ControlMessage cMsg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processNotFlushedMessage", cMsg);

    ControlNotFlushed nfm = (ControlNotFlushed) cMsg;
    long incomingRequestId = nfm.getRequestID();

    // This NotFlushed message corresponds to the currently outstanding createStream request
    // that we sent.
    if (_createStreamEntry != null
      && incomingRequestId == _createStreamEntry.createStreamId)
    {
      // stop repetition of create stream send (don't do this for
      // durable, since we didn't send the request in the first place)
      if (cMsg.getGuaranteedProtocolType() != ProtocolType.DURABLEINPUT)
        _createStreamTOM.removeTimeoutEntry(_createStreamEntry);
      
      _createStreamEntry = null;

      synchronized (_streamStatus)
      {
        // The stream should still be in STREAM_CREATING state (as we have a
        // _createStreamEntry for this NotFLushed). Otherwise something went wrong
        if (!_streamStatus.test(AIStreamStatus.STREAM_CREATING))
        {
          // log error
          SIErrorException e2 =
            new SIErrorException(
              nls.getFormattedMessage(
                "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                new Object[] {
                  "com.ibm.ws.sib.processor.impl.AnycastInputHandler.processNotFlushedMessage",
                  "1:4038:1.219.1.1" },
                null));
          FFDCFilter.processException(
            e2,
            "com.ibm.ws.sib.processor.impl.AnycastInputHandler.processNotFlushedMessage",
            "1:4043:1.219.1.1",
            new Object[] {this, _streamStatus, cMsg.toVerboseString()} );
          SibTr.exception(tc, e2);

          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.AnycastInputHandler.processNotFlushedMessage",
            "1:4050:1.219.1.1" });

          // If stream was not in creating state then there is no one to wake up, so just return
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processNotFlushedMessage", e2);

          // Even though we're horribly wrong, we should have the decency to wakeup anyone waiting
          SIResourceException ex = new SIResourceException(e2);
          
          wakeUpWaiters(false, ex);
          
          return;
        }
      } // synchronized(_streamStatus)

      // The request id matches create stream request id so create the stream
      // and initialize it
      
      // The DME provided the Stream Uuid
      SIBUuid12 streamUuid = nfm.getGuaranteedStreamUUID();

      long tick = 0;
      try
      {
        // Generate the tick that we'll start the stream with
        tick = generateUniqueValue();
      }
      catch(SIResourceException e)
      {
        // No FFDC code needed
        SibTr.exception(tc, e);
        
        // The DME now has the stream created but we failed at the last hurdle (couldn't
        // generate a starting tick - maybe the DB is down). All we can do is forget about
        // this stream for now and kick out the attaching consumer.
        synchronized (_streamStatus)
        {
          if (_streamStatus.test(AIStreamStatus.STREAM_CREATING))
            _streamStatus.set(AIStreamStatus.STREAM_NON_EXISTENT);

          // And finally wake up the waiting thread(s) (sendCreateStreamAndWait() method)
          wakeUpWaiters(false, e);
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "processNotFlushedMessage", e);
        
        return;
      }

      // Update the status of the stream to being ready for business
      synchronized (_streamStatus)
      {
        // Create the stream...
        CreatePersistentStream update =
          new CreatePersistentStream(streamUuid, tick, tick);

        // ...and persist it straight away
        AIExecuteUpdate xu = new AIExecuteUpdate(update, _messageProcessor);
        xu.run();

        if (_streamStatus.test(AIStreamStatus.STREAM_CREATING))
          _streamStatus.set(AIStreamStatus.STREAM_READY);
      }

      // And finally wake up the waiting thread(s) (sendCreateStreamAndWait() method)
      wakeUpWaiters(false, null);
      
    } // created new stream
    
    // The NotFlushed message was sent because we asked the DME if it was flushed or not
    else if (incomingRequestId == _areYouFlushedId)
    {
      // Our question has been answered
      synchronized (_streamStatus)
      {
        _areYouFlushedId = -1;
      }      
      
      SIBUuid12 notFlushedStreamId = nfm.getGuaranteedStreamUUID();

      boolean createNewStream = true; //set to true if we find we need to create a new AIStream

      AIStream localAIStream = _aiStream;
      
      // If we already have a stream then we need to check if it's the same as the DME's
      if (localAIStream != null) // _aiStream can't change while 'this' or _streamStatus is held
      {
        SIBUuid12 streamId = localAIStream.getStreamId();
        
        // If it's the same stream then we have nothing to do
        if (notFlushedStreamId.equals(streamId))
        {
          //We must have sent AreYouFlushed due to a message for some other stream.
          createNewStream = false;
        }
        // Otherwise we have an existing stream but the DME is telling us that there
        // is a different stream in use.
        else
        {
          //Since this NotFlushed must have come in response to an AreYouFlushed
          //request (and not a create stream request) we are not really in a
          //position to handle this so we should throw a RuntimeException
          
          // This could occur if the DME and RME have different streams for the same
          // destination. This should only happen if the two MessageStores have become
          // out of sync (i.e. one end was deleted/restored and not the other)
          SIErrorException e =
            new SIErrorException(
              nls.getFormattedMessage(
                "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                new Object[] {
                  "com.ibm.ws.sib.processor.impl.AnycastInputHandler.processNotFlushedMessage",
                  "1:4163:1.219.1.1" },
                null));

          FFDCFilter.processException(
              e,
              "com.ibm.ws.sib.processor.impl.AnycastInputHandler.processNotFlushedMessage",
              "1:4169:1.219.1.1",
              new Object[] {this, streamId, _streamStatus, nfm.toVerboseString()});

          SibTr.exception(tc, e);
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processNotFlushedMessage", e);

          throw e;
        }
      } //_aiStream != null

      // We need to create stream to match the DME's
      if(createNewStream)
      {
        if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "Creating new stream");

        // This must be in response to an AreYouFlushed that we previously sent
        // for some other stream that we sent when the stream
        // was non-existent; if it still is non-existent, create it

        //NOTE: this design needs to be checked.
        //Q: Is it correct to create a new stream simply because we received a NotFlushed
        //see defect 344280
        //A: Yes - we need something, even if we just request a flush on it later
        synchronized (_streamStatus)
        {
          if (_streamStatus.test(AIStreamStatus.STREAM_NON_EXISTENT))
          {

            long tick = 0;
            try
            {
              // Generate the tick that we'll start the stream with
              tick = generateUniqueValue();
            }
            catch(SIResourceException e)
            {
              // No FFDC code needed
              
              // The DME now has the stream created but we failed at the last hurdle (couldn't
              // generate a starting tick - maybe the DB is down). All we can do is forget about
              // this stream for now.
              
              if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "processNotFlushedMessage", e);
              
              return;
            }

            // only need to get the completed prefix, the duplicate prefix is not used by the AIH
            long[] completedPrefixTicks = nfm.getCompletedPrefixTicks();
            long completedPrefix = completedPrefixTicks[0];

            // Create the stream...
            CreatePersistentStream update =
              new CreatePersistentStream(
                notFlushedStreamId,
                tick,
                completedPrefix);
            
            // ...and persist it straight away
            AIExecuteUpdate xu =
              new AIExecuteUpdate(update, _messageProcessor);
            xu.run();

            _streamStatus.set(AIStreamStatus.STREAM_READY);
          }
        }
      } // createNewStream
    } // _areYouFlushed reply

    // If we don't think we asked for this, we ignore it. If it's important the DME
    // will soon get impatient and start sending other control messages and if
    // we don't recognise the stream then we'll ask for another NotFlushed message.
    // At which point we'll actually create it.
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processNotFlushedMessage");
  }

  /**
   * A ControlCardinalityInfo message is used to deny requests to create a stream, probably because
   * it's receive exclusive and already has a consumer or due to some other failure to create the
   * DME side, so it bombs out.
   */
  private synchronized void processCardinalityInfoMessage(ControlMessage cMsg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processCardinalityInfoMessage", cMsg);

    ControlCardinalityInfo cim = (ControlCardinalityInfo) cMsg;
    long requestId = cim.getRequestID();
    int cardinality = cim.getCardinality();

    // NOTE: We used to check for a streamId in this message to see if an existing consumer was
    //       being kicked off remotely, but that's no-longer the case as a receiveExclusive change
    //       is now detected locally and handled elsewhere.
    
    if ((_createStreamEntry != null) &&
        (requestId == _createStreamEntry.createStreamId))
    {
      // If trying to create a stream, wake up consumerAttaching thread and tell it to throw exception

      // stop repetition of create stream send
      _createStreamTOM.removeTimeoutEntry(_createStreamEntry);
      _createStreamEntry = null;

      // Set the status of the stream so that the waiting thread realises that the create failed
      synchronized (_streamStatus)
      {
        if (_streamStatus.test(AIStreamStatus.STREAM_CREATING))
          _streamStatus.set(AIStreamStatus.STREAM_NON_EXISTENT);
      }
      
      SIResourceException e = null;
      // If we get a cardinalityInfo message with a zero cardinality then we must have failed for
      // some reason other that an existing consumer, so we'll issue a different exception back to
      // the attaching consumer. The only or finding out why this failed is to look at the logs of
      // the DME for this point in time.
      if(cardinality == 0)
      {
        String meName = JsAdminUtils.getMENameByUuidForMessage(_destMEUuid.toString());
        if (meName == null)
          meName = _destMEUuid.toString();

        e = new SIResourceException(
          nls.getFormattedMessage(
            "ANYCAST_CANNOT_CREATE_STREAM_CWSIP0517",
            new Object[] {
              _destName,
              meName
            },
            null));
      }

      // Wake up anyone waiting in sendCreateStreamAndWait()
      wakeUpWaiters(false, e);
    }
    // Otherwise, just ignore the message.

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processCardinalityInfoMessage");
  }

  private void updateRoundtripTime(
    long issueTime,
    long arrivalTime,
    long waitTime)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "updateRoundtripTime",
        new Object[] {
          Long.valueOf(issueTime),
          Long.valueOf(arrivalTime),
          Long.valueOf(waitTime)});

    long latestRoundTripTime = arrivalTime - issueTime - waitTime;
    _currentRoundtripTime = (_currentRoundtripTime + latestRoundTripTime) / 2;

    if (_currentRoundtripTime < _messageProcessor.getCustomProperties().get_round_trip_time_low_limit())
    {
     _currentRoundtripTime = _messageProcessor.getCustomProperties().get_round_trip_time_low_limit();
    }

    if (_currentRoundtripTime > _messageProcessor.getCustomProperties().get_round_trip_time_high_limit())
    {
      _currentRoundtripTime = _messageProcessor.getCustomProperties().get_round_trip_time_high_limit();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateRoundtripTime", Long.valueOf(_currentRoundtripTime));
  }

  protected long generateUniqueValue() throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "generateUniqueValue");

    long uniqueValue = 0L;

    uniqueValue = _messageProcessor.nextTick();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "generateUniqueValue", Long.valueOf(uniqueValue));

    return uniqueValue;
  }

  /**
   * This method is called after the remote durable protocols successfully
   * establish a new stream with the durable home.  This method makes the AIH
   * behave as if someone had called isStreamReady() and is about to receive
   * the ControlNotFlushed message.
   *
   * @param createID The id that the create stream request was issued on.
   */
  public synchronized void prepareForDurableStartup(long createID) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "prepareForDurableStartup", Long.valueOf(createID));

    synchronized (_streamStatus)
    {
      // ASSERT: streamStatus.test(AIStreamStatus.STREAM_NON_EXISTENT) == true
      _streamStatus.set(AIStreamStatus.STREAM_CREATING);
    }
    _createStreamEntry = new CreateStreamTimeoutEntry();
    _createStreamEntry.createStreamId = createID;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "prepareForDurableStartup");
  }

  /**
   * Return the destination that this AIH is associated with.  This is used
   * by remote durable pub/sub support since "pseudo" destinations are used
   * in place of the real destination (which is normally accessible from
   * the DestinationHandler).
   *
   * @return The DestinationDefinition for this AIH.
  public final DestinationDefinition getDestDef()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getDestDef");

    DestinationDefinition destDef = (DestinationDefinition)baseDestinationHandler.getDefinition();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getDestDef", destDef);

    return destDef;
  }
  */

  public final String getDestName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getDestName");
      SibTr.exit(tc, "getDestName");
    }

    return _destName;
  }

  public final SIBUuid12 getDestUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getDestUuid");
      SibTr.exit(tc, "getDestUuid", _destUuid);
    }

    return _destUuid;
  }

  /**
   * The gatheringTargetDestUuid is the id used along with the targetMEUuid to lookup a specific
   * AIH. Multiple AIHs (along with their associated streams/resources) are created. There is one
   * for each remoteME/consumerTarget pair. gatheringTargetDestUuid is null for standard consumers, and 
   * contains a destination uuid if it is a gathering consumer. The destination uuid can be an alias
   * representing a subset of queue points.
   */
  
  public final SIBUuid12 getGatheringTargetDestUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getGatheringTargetDestUuid");
      SibTr.exit(tc, "getGatheringTargetDestUuid", _gatheringTargetDestUuid);
    }

    return _gatheringTargetDestUuid;
  }
  
  /**
   * Return the ME which is the localisation point with which this AIH is communicating
   * @return The localisation ME
   */
  public final SIBUuid8 getLocalisationUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getLocalisationUuid");
      SibTr.exit(tc, "getLocalisationUuid",_destMEUuid);
    }
    return _destMEUuid;
  }

  /**
   * @return the AIStream for this anycastInputHandler
   */
  public AIStream getAIStream()
  {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                {
                        SibTr.entry(tc, "getAIStream");
                        SibTr.exit(tc, "getAIStream", new Object[] {_aiStream});
                }
        return _aiStream;
  }

  public BaseDestinationHandler getBaseDestinationHandler()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getBaseDestinationHandler");
      SibTr.exit(tc, "getBaseDestinationHandler", new Object[] {_baseDestinationHandler});
    }
    return _baseDestinationHandler;
  }

  /**
   * Add a callback to be invoked the next time the stream is flushed.
   * When the stream is flushed, then "run" method will be invoked on
   * the callback.  Each callback is invoked on a new thread from
   * the thread pool.
   *
   * @param callback The callback to invoke when the stream is flushed.
   */
  public void addFlushedCallback(Runnable callback)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addFlushedCallback", callback);
    synchronized (_flushCallbacks)
    {
      _flushCallbacks.add(callback);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addFlushedCallback");
  }

  /**
   * Allow external code to test stream status
   *
   * @param test One of the AIStreamStatus codes to test.
   * @return The result of streamStatus.test(test)
   */
  public boolean testStreamStatus(int test)
  {
    boolean result = false;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "testStreamStatus", Integer.valueOf(test));
    synchronized (_streamStatus)
    {
      result = _streamStatus.test(test);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "testStreamStatus", Boolean.valueOf(result));
    return result;
  }

  /////////////////////////////////////////////////////////////
  // Inner classes
  /////////////////////////////////////////////////////////////

  /**
   * Class to hold and control access to the status of the state stream
   */
  public class AIStreamStatus
  {
    public static final int STREAM_NON_EXISTENT = 0;
    public static final int STREAM_FLUSHING = 2;
    public static final int STREAM_CREATING = 3;
    public static final int STREAM_READY = 4;
    public static final int STREAM_INACT_FLUSH = 6;
    public static final int STREAM_RESTART_FLUSH = 7;
    public static final int STREAM_FORCE_DELETING = 8;

    private int status;

    public AIStreamStatus(int status)
    {

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "AIStreamStatus", Integer.valueOf(status));

      this.status = status;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "AIStreamStatus", this);
    }

    public boolean test(int testStatus)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "test", new Object[] {Integer.valueOf(testStatus), Integer.valueOf(status)});

      boolean test = status == testStatus;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "test", Boolean.valueOf(test));

      return test;
    }

    public int get()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "get");

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "get", Integer.valueOf(status));

      return status;
    }

    public void set(int newStatus)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "set", Integer.valueOf(newStatus));

      status = newStatus;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "set");
    }
    
    public String toString()
    {
      return String.valueOf(status);
    }
    
  } // end of class AIStreamStatus

  /**
   * Abstract common rolledback sync retry
   */
  abstract class SyncUpdateWithRetry extends AsyncUpdate
  {
    private int repetitionCount;

    public SyncUpdateWithRetry()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "SyncUpdateWithRetry");

      this.repetitionCount = 1;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "SyncUpdateWithRetry", this);
    }

    public void rolledback(Throwable e)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "rolledback", e);

      // log error
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AnycastInputHandler.SyncUpdateWithRetry.rolledback",
        "1:4624:1.219.1.1",
        this);
      SibTr.exception(tc, e);
      // try again
      repetitionCount++;
      if (repetitionCount
        > SIMPConstants.MS_WRITE_REPETITION_THRESHOLD)
      {
        // log that giving up
        Exception e2 =
          new Exception(
            nls.getFormattedMessage(
              "MSGSTORE_STOP_RETRY_CWSIP0515",
              new Object[] {
                _destName,
                Integer.valueOf(SIMPConstants.MS_WRITE_REPETITION_THRESHOLD)
              },
              null));
        FFDCFilter.processException(
          e2,
          "com.ibm.ws.sib.processor.impl.AnycastInputHandler.SyncUpdateWithRetry.rolledback",
          "1:4645:1.219.1.1",
          this);
        SibTr.exception(tc, e2);
      }
      else
      {
        AIExecuteUpdate xu = new AIExecuteUpdate(this, _messageProcessor);
        xu.run();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "rolledback");
    }
  }

  /**
   * Creates persistent protocol stream
   */
  class CreatePersistentStream extends SyncUpdateWithRetry
  {
    private SIBUuid12 streamId;
    private AIProtocolItemStream itemStream;
    private long latestTick;
    private AICompletedPrefixItem completedPrefixItem;

    CreatePersistentStream(
      SIBUuid12 streamId,
      long latestTick,
      long completedPrefix)
    {
      super();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "CreatePersistentStream",
          new Object[] {
            streamId,
            Long.valueOf(latestTick),
            Long.valueOf(completedPrefix)});

      this.streamId = streamId;
      this.latestTick = latestTick;
      this.completedPrefixItem = new AICompletedPrefixItem(completedPrefix);
      this.itemStream = new AIProtocolItemStream(streamId);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "CreatePersistentStream", this);
    }

    public void execute(TransactionCommon t) throws Throwable
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "execute", t);
      Transaction msTran = _messageProcessor.resolveAndEnlistMsgStoreTransaction(t);

      _containerItemStream.addItemStream(itemStream, msTran);
      itemStream.addItem(completedPrefixItem, msTran);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "execute");
    }

    /**
     * Called while 'this' and _streamStatus is held
     */
    public void committed()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "committed");

      try
      {
        _aiStream =
          new AIStream(
            streamId,
            itemStream,
            AnycastInputHandler.this,
            _msUpdateThread,
            latestTick,
            completedPrefixItem,
            false,
            _messageProcessor);
      }
      catch (Exception e)
      {
        // this cannot happen since new AIStream only throws if there is an existing stream
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.AnycastInputHandler.CreatePersistentStream.committed",
          "1:4735:1.219.1.1",
          this);

        SibTr.exception(tc, e);
      }

      _persistentStreamState = itemStream;

      // 243428; now that we have a stream, make sure that the timers are started
      start();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "committed");
    }

  } // end of class CreatePersistentStream

  /**
   * Update the persistent stream, used to persist the flush started flag
   */
  class PersistentStreamAsyncUpdate extends SyncUpdateWithRetry
  {
    public PersistentStreamAsyncUpdate()
    {
    }

    public void execute(TransactionCommon t) throws Throwable
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "execute", new Object[]{this, t});

      Transaction msTran = _messageProcessor.resolveAndEnlistMsgStoreTransaction(t);
      _persistentStreamState.requestUpdate(msTran);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "execute");
    }

    public void committed()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "committed", this);

      if (_streamStateRemoveNeedsToWait)
      {
        synchronized (_persistentStreamState)
        {
          _streamStateRemoveNeedsToWait = false;
          _persistentStreamState.notify();
        }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "committed");
    }

  } // end of class PersistentStreamAsyncUpdate

  /**
  * Remove the persistent state for a protocol stream
  */
  class RemovePersistentStream extends SyncUpdateWithRetry
  {
    RemovePersistentStream()
    {
      super();
    }

    /**
     * Called while 'this' and _streamStatus is held
     */
    public void execute(TransactionCommon t) throws Throwable
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "execute", t);

      _aiStream.removeCompletedPrefix(
        t,
        _messageProcessor.getMessageStore().getUniqueLockID(AbstractItem.STORE_NEVER));
      synchronized (_persistentStreamState)
      {
        if (_streamStateRemoveNeedsToWait)
        {
          _persistentStreamState.wait();
        }

        long lockID = _messageProcessor.getMessageStore().getUniqueLockID(AbstractItem.STORE_NEVER);
        _persistentStreamState.lockItemIfAvailable(lockID);
        Transaction msTran = _messageProcessor.resolveAndEnlistMsgStoreTransaction(t);
        _persistentStreamState.remove(msTran, lockID);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "execute");
    }

    /**
     * Called while 'this' and _streamStatus is held
     */
    public void committed()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "committed");

      synchronized (_streamStatus)
      {
        _aiStream = null;
        _persistentStreamState = null;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "committed");
    }
  } // end of class RemovePersistentStream

  /** Class to process create stream timeouts */

  class CreateStreamTimeoutProcessor implements BatchedTimeoutProcessor
  {
    public void processTimedoutEntries(List entries)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "processTimedoutEntries", 
            new Object[] {"CreateStreamTimedoutProcessor",
                          AnycastInputHandler.this,
                          entries});

      int len = entries.size();
      for (int i = 0; i < len; i++)
      {
        // We've timeout waiting for an answer to our createStream request so send
        // another one
        CreateStreamTimeoutEntry cste =
          (CreateStreamTimeoutEntry) entries.get(i);
        try
        {
          sendCreateStream(cste.createStreamId);
        }
        catch (SIResourceException e)
        {
          // No FFDC code needed
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "processTimedoutEntries", e);

          // This exception means that the dme was not reachable
          // stop repetition of create stream send
          synchronized (AnycastInputHandler.this)
          {
            _createStreamTOM.removeTimeoutEntry(_createStreamEntry);
            _createStreamEntry = null;

            // Set stream status back to non-existent
            synchronized (_streamStatus)
            {
              if (_streamStatus.test(AIStreamStatus.STREAM_CREATING))
                _streamStatus.set(AIStreamStatus.STREAM_NON_EXISTENT);
            }

            // Pass the exception back to the waiting creator then wake them up
            // (sendCreateStreamAndWait() method)
            AnycastInputHandler.this.wakeUpWaiters(false, e);
          }
        }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "processTimedoutEntries");
    }
  }

  class CreateStreamTimeoutEntry extends AbstractBatchedTimeoutEntry
  {
    public long createStreamId;

    public CreateStreamTimeoutEntry() throws SIResourceException
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "CreateStreamTimeoutEntry");

      this.createStreamId = generateUniqueValue();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "CreateStreamTimeoutEntry", this);
    }
    
    public void cancel()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(this,tc, "cancel");

      // If the alarm is being cancelled, wakeup any one waiting for a stream to be created
      SIResourceException e = new SIResourceException(
          nls.getFormattedMessage(
            "ANYCAST_CANNOT_CREATE_STREAM_CWSIP0517",
            new Object[] {
              _destName,
              _destMEUuid
            },
            null));
      
      AnycastInputHandler.this.wakeUpWaiters(false, e);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "cancel");
    }
  }

  /** Class to process request flush timeouts */

  class RequestFlushTimeoutProcessor implements BatchedTimeoutProcessor
  {
    public void processTimedoutEntries(List entries)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "processTimedoutEntries",
            new Object[] {"RequestFlushTimeoutProcessor",
            AnycastInputHandler.this,
            entries});

      int len = entries.size();
      for (int i = 0; i < len; i++)
      {
        synchronized (AnycastInputHandler.this)
        {
          RequestFlushTimeoutEntry rfte =
            (RequestFlushTimeoutEntry) entries.get(i);
          try
          {
            // If the requestFlushEntry has no id we must have failed to generate one the
            // first time it was scheduled. Try to generate one now, if we fail again we'll
            // throw an SIResourceException and drop out.
            if(rfte.requestFlushId == -1)
              rfte.requestFlushId = generateUniqueValue();
            
            sendRequestFlush(rfte.requestFlushId, IndoubtAction.INDOUBT_DELETE);
            _requestFlushTOM.updateTimeout(_messageProcessor.getCustomProperties().get_request_flush_repetition_interval());
            _requestFlushFailed = false;
          }
          catch (SIResourceException e)
          {
            // No FFDC code needed

            // And notify send create stream thread
            // This exception means that the dme was not reachable
            // stop repetition of request flush send
            
            _requestFlushTOM.updateTimeout(_messageProcessor.getCustomProperties().get_request_flush_slow_repetition_interval());
            _requestFlushFailed = true;

            // The DME isn't reachable so wakeup any consumers currently sat waiting for it
            // while trying to attach (consumers already attached will get kicked off by a
            // change in reachability, but we can't be guaranteed of that when flushing as the
            // DME may not be advertising the queue anymore)
            AnycastInputHandler.this.wakeUpWaiters(true, e);
          }
        } // synchronized
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "processTimedoutEntries");
    }
  }

  class RequestFlushTimeoutEntry extends AbstractBatchedTimeoutEntry
  {
    public long requestFlushId;

    public RequestFlushTimeoutEntry(long requestFlushId)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "RequestFlushTimeoutEntry", Long.valueOf(requestFlushId));

      this.requestFlushId = requestFlushId;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "RequestFlushTimeoutEntry", this);
    }
    
    public void cancel()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(this,tc, "cancel");

      // If the alarm is being cancelled, wakeup any one waiting for a stream to be flushed
      SIResourceException e = new SIResourceException(
          nls.getFormattedMessage(
            "ANYCAST_CANNOT_CREATE_STREAM_CWSIP0517",
            new Object[] {
              _destName,
              _destMEUuid
            },
            null));
      
      AnycastInputHandler.this.wakeUpWaiters(true, e);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "cancel");
    }
  }

  /**
   */
  public class InactivityTimeoutListener implements AlarmListener
  {
    public void alarm(Object thandle)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "alarm", new Object[] {thandle, this});

      SendDispatcher rejectedDispatcher = new SendDispatcher();
      boolean sendRequestFlush = false;
      boolean dispatch = false;

      // We're operating on the inactivityTimer so lock down 'this'
      synchronized(AnycastInputHandler.this)
      {
        //NOTE: since this is a timer we check if it is safe to flush and, if so,
        //do the work ourselves. Otherwise we wait for
        //the timer to re-pop
        synchronized (_streamStatus)
        {
          if ( !_inactivityAlarmCancelled )
          {
            //see if it is safe to process the flush (i.e. no outstanding trans)
            boolean streamOkToProcessFlush = !streamHasIndoubtTransactions() && checkStreamHasNoFlushWork();
            if (_streamStatus.test(AIStreamStatus.STREAM_READY))
            {
              try
              {
                //we need to check that it is safe to flush the stream
                //If not we wait for the timer to re-pop
                if(streamOkToProcessFlush)
                {
                  //It is ok to flush the stream
                  // an additional call to this should not find any Q/G or V/U any more
                  _aiStream.updateAllToRejected(rejectedDispatcher);
                  _streamStatus.set(AIStreamStatus.STREAM_INACT_FLUSH);
                  sendRequestFlush = true;
                  dispatch=true;
                }//end if

              }
              catch (SIErrorException e)
              {
                // No FFDC code needed; updateAllToRejected already processed the exception
              }
            }

            if(!streamOkToProcessFlush) {
              //we need to reschedule this timer for a later time when, hopefully, the stream will
              //be able to flush messages
              _inactivityTimeoutAlarm =
                _alarmManager.create(_messageProcessor.getCustomProperties().get_sender_inactivity_timeout(), this);
            }
          }
        }//end sync

        if(dispatch)
        {
          // Send message dispatched by updateToRejected outside of synchronized block
          rejectedDispatcher.dispatch();
        }

        if (sendRequestFlush)
        {
          //we only enter this if we have already determined it is safe to do so
          sendRequestFlush(IndoubtAction.INDOUBT_DELETE);
        }
      } // synchronized(this)

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "alarm");
    }  } // end of class InactivityTimeoutListener

  /**
   */
  public class ReachabilityChangeListener implements MPDestinationChangeListener
  {
    private RemoteConsumerDispatcher rcd = null;

    public void initRCD(RemoteConsumerDispatcher rcd)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "initRCD", new Object[] {this, rcd});

      this.rcd = rcd;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "initRCD");
    }

    public void destinationLocationChange (SIBUuid12 destId, Set additions, Set deletions, Capability capability)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc,
                    "destinationLocationChange",
                    new Object[] {this, destId, additions, deletions, capability});

      // Only pay attention if we're being notified about our destination
      if (rcd != null && _destUuid.equals(destId))
      {
        // Process additions to the WLM destination set
        Iterator i = additions.iterator();

        while(i.hasNext())
        {
          SIBUuid8 meUuid = (SIBUuid8) i.next();
          // only notify RCD if the DME's uuid is found
          if (_destMEUuid.equals(meUuid))
          {
            // Force connection to be re-established before we attempt to use
            // it for a request flush
            _messageProcessor.getMPIO().forceConnect(meUuid);
            
            synchronized(_streamStatus)
            {
              // If the DME is now reachable and we have a requestFlush registered that failed to
              // be sent (probably because the DME was down at the time) we resend it now, rather
              // than wait for the retry timer to kick in (much) later, as we're going to wait
              // for a response.
              if((_requestFlushEntry != null) && _requestFlushFailed)
              {
                try
                {
                  redriveFailedRequestFlush();
                }
                catch(SIResourceException e)
                {
                  // No FFDC code needed
                  
                  // Oh well, it was worth a try, we'll leave the retry to happen later
                }
              }
            }
            
            if(capability == Capability.GET)
            {
              rcd.reachabilityChange(true);
            }
          }
        }

        // Process deletions from the WLM set for the destination
        i = deletions.iterator();

        while(i.hasNext())
        {
          SIBUuid8 meUuid = (SIBUuid8) i.next();
          // only notify RCD if the DME's uuid is found
          // only check capability for completeness
          if (_destMEUuid.equals(meUuid) && capability == Capability.GET)
          {
            rcd.reachabilityChange(false);
          }
        }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "destinationLocationChange");
    }
  }

  /**
   * See defect 269699:
   * If the stream is flushed when a transacted get is still outstanding
   * then the flush will fail: this will cause the overal transaction to rollback.
   * Under such circumstances it is correct to store the work so that it can be carried
   * out asynchronously once the transaction has completed and the stream is ok to flush.
   *
   * Any work that could potentially generate a stream flush should be wrapped in an
   * instance of this interface so that it can be carried out by the stream at a time when it is
   * safe to do so.
   */
  public interface FlushWorkItem
  {
    /**
     * This is the method that will be invoked by the stream when it is safe to perform
     * the flush work.
     * The wrappered work should be started from this overriden version of this method.
     *
     */
    public void performWorkItem();
  }

  /**
   * Takes a FlushWorkItem and performs the work wrapped by that item.
   * If the stream has no outstanding transacted gets then the work will
   * be performed synchronously and in-place, unless the forceUpdate
   * flag is specified.
   * If there are transacted gets outstanding and if the forceUpdate
   * flag is not specified, then the work will be carried
   * out when those transactions are completed (see defect 269699).
   *
   *
   * @param flushWorkItem the work to be performed
   * @param forceUpdated if true then the state of any indoubt transactions is
   * ignored and the work is carried out synchronously regardless.
   */
  public void performFlushWork(FlushWorkItem flushWorkItem, boolean forceUpdate)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "performFlushWork",
                  new Object[]{flushWorkItem, Boolean.valueOf(forceUpdate)});

    boolean performWorkSynchronously=false;

    //we sync on _streamStatus to stop any calls to
    //accept() occuring underneath us
    synchronized (_streamStatus)
    {
      //prevent anyone from writing any more VALUE or REQUEST
      //ticks into the stream - see defect 282249
      _numberOfFlushWorkItemsOnStream++;

      //if there are indoubt transactions then we cannot complete this work
      //unless explicitly told to ignore outstanding transactions.
      if(!forceUpdate && streamHasIndoubtTransactions())
      {
        //see defect 269699:
        //we cannot perform this work now so we schedule it for later
        if(_flushWorkQueue==null)
        {
          //instantiate
          _flushWorkQueue = new LinkedList();
        }

        //we do not synchronize on the queue - we cannot be sure what locks
        //the caller has
        _flushWorkQueue.add(flushWorkItem);
        if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
          SibTr.debug(tc, "flush work has queued until all indoubt-transactions are resolved");
        }
      }
      else
      {
        //no indoubt trans so we perform this work synchronously
        performWorkSynchronously = true;
        // Indicate that flush work is currently going on
        _inProgressFlushWork++;
      }
    }//end sync


    if(performWorkSynchronously)
    {
      //we do the work without holding the lock - we are safe from the
      //stream accquiring any new indoubt trans as we have set
      //_numberOfFlushWorkItemsOnStream to greater than zero, which
      //prevents VALUE and REQUEST from being written into the stream
      try
      {
        flushWorkItem.performWorkItem();
      }
      catch(Exception e)
      {
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.AnycastInputHandler.performFlushWork",
          "1:5292:1.219.1.1",
          this);
      }
      synchronized(_streamStatus)
      {
        _numberOfFlushWorkItemsOnStream--;
        _inProgressFlushWork--;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "performFlushWork");
  }

  /**
   * Cancels all flush work on the stream.
   * This is called when a consumer comes in when the stream is waiting for
   * a transaction to finish so that it can flush.
   * In such a situation the flush is no longer necessary so we can
   * simply not do it.
   */
  public boolean cancelAllFlushWork()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "cancelAllFlushWork");
    
    boolean cancelAllowed = false;
    
    synchronized(_streamStatus)
    {
      // We don't hold any locks while performing flush work, so we can't let anyone
      // cancel it while it's actually doing the work, instead reject the cancel.
      if(_inProgressFlushWork == 0)
      {
        cancelAllowed = true;
        _numberOfFlushWorkItemsOnStream=0;
        if (_flushWorkQueue != null)
          _flushWorkQueue.clear();
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "cancelAllFlushWork", Boolean.valueOf(cancelAllowed));
    
    return cancelAllowed;
  }
  /**
   * Takes the FlushWorkItem objects from the flushworkQueue and
   * performs the work in those objects.
   * This method takes the _streamStatus lock.
   */
  private void drainFlushWorkQueue()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "drainFlushWorkQueue");

    List localList = null;
    int workCount = 0;

    //we take the streamStatus lock to prevent people changing
    //the list
    synchronized(_streamStatus)
    {
      //we have some flush work to do.
      //We cannot synchronize on the queue because the work items take locks
      //and we therefore risk deadlock with any clients adding more work.
      //Instead we take a local copy of the list - any subsequent clients
      //will create a new list
      localList = _flushWorkQueue;
      
      // As we don't hold a lock during the actual performing of the flush work we
      // need an indicator that it is actually happening
      workCount = localList.size();
      _inProgressFlushWork += workCount; 
      
      // Clear the queue for someone else's use
      _flushWorkQueue = null;
    }

    Iterator iterator = localList.iterator();

    //perform the work on our local list
    while(iterator.hasNext())
    {
      FlushWorkItem flushWorkItem = (FlushWorkItem)iterator.next();
      try
      {
        flushWorkItem.performWorkItem();
      }
      catch(Exception e)
      {
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.AnycastInputHandler.drainFlushWorkQueue",
          "1:5386:1.219.1.1",
          this);
      }
    }//end while

    synchronized(_streamStatus)
    {
      //this may enable messages to be
      //written into VALUE/REQUESTED state again.
      _numberOfFlushWorkItemsOnStream -= workCount;
      _inProgressFlushWork -= workCount;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "drainFlushWorkQueue");
  }

  /**
   * @return true if the stream has outstanding transactions
   * i.e. messages on the stream that are in the value state
   */
  private boolean streamHasIndoubtTransactions()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "streamHasIndoubtTransactions");

    boolean hasIndoubtTrans = false;

    //we sync on _streamStatus to stop any calls to
    //accept() occuring underneath us
    synchronized (_streamStatus)
    {
      if(_aiStream!=null && _aiStream.countAllMessagesOnStream()>0)
      {
        //outstanding transacted 'gets' are only completed if there
        //are no messages in 'value' state
        hasIndoubtTrans=true;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "streamHasIndoubtTransactions", Boolean.valueOf(hasIndoubtTrans));
    return hasIndoubtTrans;
  }

  /**
   */
  public class SendDispatcher
  {
    // Bit states
    private static final int SEND_REQUEST = 1;
    private static final int SEND_ACCEPT = 2;
    private static final int SEND_REJECT = 4;
    private static final int SEND_COMPLETED = 8;
    private static final int SEND_RESET_REQUEST_ACK_ACK = 16;
    private static final int SEND_RECOVERED_REJECT = 32;
    
    private boolean needToSend = false;
    private int typeToSend = 0;
    
    // Accept properties
    private ArrayList<Long> acceptTicks = null;

    // Request properties
    private ArrayList<Long> requestTicks = null;
    private ArrayList<Long> requestRejectStartTicks = null;
    private ArrayList<SelectionCriteria[]> criterias = null;
    private ArrayList<Long> timeouts = null;
    
    // Completed properties    
    private ArrayList<Long> completedStartTicks = null;
    private ArrayList<Long> completedEndTicks = null;
    
    // Reject properties
    private ArrayList<Long> rejectStartTicks = null;
    private ArrayList<Long> rejectEndTicks = null;
    private ArrayList<Long> rejectUnlockCounts = null;    
    private ArrayList<Long> rejectRecoveredStartTicks = null;
    private ArrayList<Long> rejectRecoveredEndTicks = null;
    
    // Reset Req Ack Properties
    private long dmeVersion = -1;
   
    public void sendRequest(
      Long rejectStartTick,
      Long tick,
      SelectionCriteria criteria[],
      Long timeout)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "sendRequest", 
            new Object[] {rejectStartTick,
                          tick,
                          criteria,
                          timeout});

      if (requestTicks == null)
      {
        requestTicks = new ArrayList<Long>();
        requestRejectStartTicks = new ArrayList<Long>();
        criterias = new ArrayList<SelectionCriteria[]>();
        timeouts = new ArrayList<Long>();
      }
      
      requestRejectStartTicks.add(rejectStartTick);
      requestTicks.add(tick);
      criterias.add(criteria);
      timeouts.add(timeout);
      
      this.needToSend = true;
      this.typeToSend |= SendDispatcher.SEND_REQUEST;      

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "sendRequest", this);
    }

    public void sendAccept(Long tick)
    {      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "sendAccept", tick);
      
      if (acceptTicks == null)
        acceptTicks = new ArrayList<Long>();
      
      acceptTicks.add(tick);
      
      this.needToSend = true;
      this.typeToSend |= SendDispatcher.SEND_ACCEPT;  
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "sendAccept", this);
    }
    
    public void sendReject(Long startTick, Long endTick, Long unlockCount, boolean recovery)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "sendReject", new Object[] {startTick,
                                                    endTick,
                                                    recovery});
      
      // The recovery paramter could be different for each tick but we can only
      // send one value per msg. We still want to combine multiple ticks in a msg
      // so we send 2 msgs, one for those ticks with recovery and one for those without
      if (!recovery)
      {
        if (rejectStartTicks == null)
        {
          rejectStartTicks = new ArrayList<Long>();
          rejectEndTicks = new ArrayList<Long>();
          rejectUnlockCounts = new ArrayList<Long>();
        }      
        rejectStartTicks.add(startTick);
        rejectEndTicks.add(endTick);
        rejectUnlockCounts.add(unlockCount);
        this.typeToSend |= SendDispatcher.SEND_REJECT;
      }
      else
      {
        if (rejectRecoveredStartTicks == null)
        {
          rejectRecoveredStartTicks = new ArrayList<Long>();
          rejectRecoveredEndTicks = new ArrayList<Long>();
        }      
        rejectRecoveredStartTicks.add(startTick);
        rejectRecoveredEndTicks.add(endTick);
        this.typeToSend |= SendDispatcher.SEND_RECOVERED_REJECT;
      }
      
      this.needToSend = true;
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "sendReject", this);
    }

    public void sendCompleted(Long startTick, Long endTick)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "sendCompleted", new Object[] {startTick,
                                                       endTick});

      if (completedStartTicks == null)
      {
        completedStartTicks = new ArrayList<Long>();
        completedEndTicks = new ArrayList<Long>();
      }
      
      completedStartTicks.add(startTick);
      completedEndTicks.add(endTick);
      
      this.needToSend = true;
      this.typeToSend |= SendDispatcher.SEND_COMPLETED;
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "sendCompleted", this);
    }

    public void sendResetRequestAckAck(long dmeVersion)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "sendResetRequestAckAck", Long.valueOf(dmeVersion));
      
      this.dmeVersion = dmeVersion;
      
      this.needToSend = true;
      this.typeToSend |= SendDispatcher.SEND_RESET_REQUEST_ACK_ACK;
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "sendResetRequestAckAck", this);
    }

    public void dispatch()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "dispatch");
      
      if (needToSend)
      {
        if ((typeToSend & SendDispatcher.SEND_REQUEST) > 0)
        {
          for (int i=0; i<requestTicks.size();i++)
            AnycastInputHandler.this.sendRequest(toArray(requestRejectStartTicks.get(i)), toArray(requestTicks.get(i)), criterias.get(i), toArray(timeouts.get(i)), SIMPConstants.CONTROL_MESSAGE_PRIORITY);
        }
        
        if ((typeToSend & SendDispatcher.SEND_ACCEPT) > 0)
            AnycastInputHandler.this.sendAccept(toArray(acceptTicks));
        
        if ((typeToSend & SendDispatcher.SEND_REJECT) > 0)
            AnycastInputHandler.this.sendReject(toArray(rejectStartTicks), toArray(rejectEndTicks), toArray(rejectUnlockCounts), false);        
        
        if ((typeToSend & SendDispatcher.SEND_RECOVERED_REJECT) > 0)
          AnycastInputHandler.this.sendReject(toArray(rejectRecoveredStartTicks), toArray(rejectRecoveredEndTicks), new long[rejectRecoveredStartTicks.size()], true);        
        
        if ((typeToSend & SendDispatcher.SEND_COMPLETED) > 0)
            AnycastInputHandler.this.sendCompleted(toArray(completedStartTicks), toArray(completedEndTicks));

        if ((typeToSend & SendDispatcher.SEND_RESET_REQUEST_ACK_ACK) > 0)
            AnycastInputHandler.this.sendResetRequestAckAck(dmeVersion);
      }

      reset();
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "dispatch", this);
    }

    private long[] toArray(ArrayList<Long> items) 
    {
      long[] array = new long[items.size()];
      int i = 0;
      
      for (Iterator<Long> it = items.iterator() ; it.hasNext() ;)
        array[i++] = it.next().intValue();
           
      return array;
    }
        
    private long[] toArray(Long item) 
    {
      long[] array = new long[1];
      array[0] = item.longValue();           
      return array;
    }

    private void reset()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "reset");
      
      needToSend = false;
      typeToSend = 0;
      
      requestTicks = null;
      requestRejectStartTicks = null;
      criterias = null;
      timeouts = null;
      
      rejectStartTicks = null;
      rejectEndTicks = null;
      rejectUnlockCounts = null;
      rejectRecoveredStartTicks = null;
      rejectRecoveredEndTicks = null;

      completedEndTicks = null;
      completedStartTicks = null;
      
      dmeVersion = -1;
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "reset", this);
    }
  }

  /**
   * We cant use Booleans as locks. Probably because they are typeSafe enums.
   * We therefore use our own if we want to lock on it.
   */
  static class LockingBoolean
  {
    private boolean myBooleanValue;

    public LockingBoolean(boolean value)
    {
      myBooleanValue = value;
    }

    public boolean booleanValue()
    {
      return myBooleanValue;
    }

    public void setBooleanValue(boolean value)
    {
      myBooleanValue = value;
    }
  }

  /**
   * @return
   */
  public AIProtocolItemStream getAIProtocolStream() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getAIProtocolStream");
      SibTr.exit(tc, "getAIProtocolStream", _persistentStreamState);
    }
    return _persistentStreamState;
  }

  public MessageProcessor getMessageProcessor () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getMessageProcessor");
      SibTr.exit(tc, "getMessageProcessor",_messageProcessor);
    }
    return _messageProcessor;
  } 

  public String toString()
  {
    String text = super.toString() + "[";
    text += "Dest:" + _destName + "," + _destMEUuid + "]";
    
    return text;
  }
  
  private SIResourceException waitForResponse(long responseTimeout, boolean flush)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "waitForResponse", Boolean.valueOf(flush));
    
    boolean notified = false;
    SIResourceException ex = null;
    long startTime = System.currentTimeMillis();
    
    // If responseTimeout is any bigger than 10 seconds then we`ll split the timeout into
    // 10 second chunks so we get some trace out and know this thread isnt dead
    long waitTime = 10000;
    if (responseTimeout < 10000)
      waitTime = responseTimeout;
    
    while(!notified)
    {
      AIHWaiter waiter = new AIHWaiter(flush);
      
      try
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(this,tc, "Waiting for " + waiter);

        wait(waitTime); // this wait should be interrupted by ControlFlushed
        // it could also have been interrupted by exception in repeated request flush
      }
      catch (InterruptedException e)
      {
        // InterruptedException shouldn't occur so FFDC.
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.AnycastInputHandler.waitForResponse",
          "1:5764:1.219.1.1",
          this);

        SibTr.exception(tc, e);
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(this,tc, "Woken from waiting for " + waiter);
      
      notified = waiter.isNotified();

      if(!notified)
      {
        if((System.currentTimeMillis() - startTime) > responseTimeout)
        {
          ex = new SIMPNoResponseException(
                nls.getFormattedMessage(
                    "ANYCAST_STREAM_UNAVAILABLE_CWSIP0481",
                    new Object[]{_destName, _destMEUuid.toString()},
                    null));

          SibTr.exception(tc, ex);
          
          notified = true;
        }
        
      }
      else
      {
        // If wait was interrupted by exception in repeated request flush, then dmeNotReachableException
        // will not be null
        ex = waiter.getException();
      }        
    } // while
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "waitForResponse", ex);
    
    return ex;
  }
  
  
  private synchronized void wakeUpWaiters(boolean flush,
                                          SIResourceException ex)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "wakeUpWaiters", new Object[] {Boolean.valueOf(flush), ex});

    boolean notify = false;
    
    if(!_flushWaiters.isEmpty())
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "_flushWaiters: " + _flushWaiters.size());
      
      notify = true;
      if(flush || (ex != null))
      {
        for(Iterator i=_flushWaiters.iterator(); i.hasNext(); )
        {
          AIHWaiter waiter = (AIHWaiter)i.next();
          
          waiter.setNotified();
          if(ex != null)
            waiter.setException(ex);
        }
      }
      _flushWaiters.clear();
    }
    
    if(!_createWaiters.isEmpty())
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "_createWaiters: " + _createWaiters.size());
      
      notify = true;
      if(!flush || (ex != null))
      {
        for(Iterator i=_createWaiters.iterator(); i.hasNext(); )
        {
          AIHWaiter waiter = (AIHWaiter)i.next();
          
          waiter.setNotified();
          if(ex != null)
            waiter.setException(ex);
        }
      }
      _createWaiters.clear();
    }
    
    if(notify)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Notifying all waiters");
      
      notifyAll();
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "wakeUpWaiters");
  }
  
  public class AIHWaiter
  {
    boolean _notified = false;
    SIResourceException _failureEx = null;
    
    public AIHWaiter(boolean flush)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "AIHWaiter", Boolean.valueOf(flush));
        
      if(flush)
        AnycastInputHandler.this._flushWaiters.add(this);
      else
        AnycastInputHandler.this._createWaiters.add(this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(this, tc, "AIHWaiter", new Object[] {_flushWaiters, _createWaiters});
    }
    
    public void setNotified()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.entry(this, tc, "setNotified");
        SibTr.exit(tc, "setNotified");
      }
      
      _notified = true;
    }
    
    public boolean isNotified()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.entry(this, tc, "isNotified");
        SibTr.exit(tc, "isNotified", Boolean.valueOf(_notified));
      }
      
      return _notified;
    }
    
    public void setException(SIResourceException ex)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.entry(this, tc, "setException", ex);
        SibTr.exit(tc, "setException");
      }

      _failureEx = ex;
    }
    public SIResourceException getException()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.entry(this, tc, "getException");
        SibTr.exit(tc, "getException", _failureEx);
      }
      
      return _failureEx;
    }
    
  }
  
  // Used for totalMessageOrdering to clear the transaction when the previous one has been committed/rolledback
  public void clearOrderedTran() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "clearOrderedTran");
    
    if (_aiStream!=null)
      _aiStream.getAIProtocolItemStream().clearOrderedTran();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "clearOrderedTran");
  }

  public long handleControlMessageWithReturnValue(SIBUuid8 sourceMEUuid,
		ControlMessage cMsg) throws SIIncorrectCallException,
		SIResourceException, SIConnectionLostException, SIRollbackException {
	return 0;
  }  
}
