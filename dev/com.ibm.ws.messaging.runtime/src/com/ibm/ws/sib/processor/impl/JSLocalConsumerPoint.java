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
package com.ibm.ws.sib.processor.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ejs.util.am.Alarm;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIRCConstants;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageCopyFailedException;
import com.ibm.ws.sib.msgstore.LockingCursor;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPConnectionVersionException;
import com.ibm.ws.sib.processor.exceptions.SIMPMessageNotLockedException;
import com.ibm.ws.sib.processor.exceptions.SIMPNoLocalisationsException;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumableKey;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerKey;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DispatchableConsumerPoint;
import com.ibm.ws.sib.processor.impl.interfaces.ExternalConsumerLock;
import com.ibm.ws.sib.processor.impl.interfaces.JSConsumerManager;
import com.ibm.ws.sib.processor.impl.interfaces.JSKeyGroup;
import com.ibm.ws.sib.processor.impl.interfaces.LocalConsumerPoint;
import com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.MessageEvents;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.processor.utils.am.MPAlarmManager;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.AsynchConsumerCallback;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.StoppableAsynchConsumerCallback;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionMismatchException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException;
import com.ibm.wsspi.sib.core.exception.SINonDurableSubscriptionMismatchException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/*
 * Locking hierarchy:
 * 
 *       ConsumerDispatcher.consumerPoints
 *          JSConsumerSet.consumerList
 *             JSLocalConsumerPoint._asynchConsumerBusyLock
 *                JSLocalConsumerPoint._keyGroup
 *                   JSLocalConsumerPoint.this
 *                      BaseDestinationHandler.readyConsumerPointLock
 *                      JSConsumerSet.maxActiveMessagePrepareLock (Read/Write)
 *                         JSConsumerSet.maxActiveMessageLock
 *                      JSLocalConsumerPoint._maxActiveMessageLock
 *          JSConsumerSet.this
 *          JSConsumerSet.classifications
 */
public class JSLocalConsumerPoint extends ReentrantLock
                implements DispatchableConsumerPoint, LocalConsumerPoint, MessageEventListener, AlarmListener
{

    /**
     * Generated serialised Id
     */
    private static final long serialVersionUID = 6225084023637058815L;

    // NLS for component
    private static final TraceNLS nls =
                    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

    // NLS for component
    private static final TraceNLS nls_cwsir =
                    TraceNLS.getTraceNLS(SIMPConstants.CWSIR_RESOURCE_BUNDLE);

    // NLS for component
    private static final TraceNLS nls_cwsik =
                    TraceNLS.getTraceNLS(SIMPConstants.CWSIK_RESOURCE_BUNDLE);

    //trace
    private static final TraceComponent tc =
                    SibTr.register(
                                   JSLocalConsumerPoint.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    final static int NO_LOCAL_BATCH_SIZE = 10;

    private final SIMPTransactionManager _txManager;

    private final MessageProcessor _messageProcessor;

    // GetCursor for this ConsumerPoint, this is a handle to the consumerKey's
    // default cursor and is used for it's lockId. when search an ItemStream
    // you must use the getCursor straight from the consmerKey itself. This is
    // because it may be a member of a group and therefore the cursor will have
    // a different filter and be in a different position. It is also possible that
    // the cursor may change between uses (if a new member joins the group the
    // current cursor is removed and a new one created to ensure we do not skip
    // over matching messages
    //private LockingCursor _getCursor;

    // Key to identify this consumer to the consumer dispatcher
    private ConsumableKey _consumerKey;

    // Condition associated with the ReentrantLock toallow us to wait/notify ourselves
    private final Condition _waiter;

    // ConsumerSession which created us.
    // There is one LocalConsumerPoint per ConsumerSession
    private final ConsumerSessionImpl _consumerSession;

    //The ConsumerDispatcher to which this LCP is attached
    private JSConsumerManager _consumerDispatcher;

    //shows that we have begun a receive call but have not yet
    //been started
    private boolean _waitingToStart;

    //shows if this LCP is started or not
    private boolean _stopped = true;

    //shows if this LCP was stopped by stop request
    private boolean _stoppedByRequest = true;

    //shows if this LCP was stopped for ReceiveAllowed
    private boolean _stoppedForReceiveAllowed = false;

    //Is the consumer transacted?
    //when synchronous, it is decided per receive call?
    //when asynch it's always transacted
    private boolean _transacted = true;

    // Fine grain recoverability can be set per message QoS
    // These options are only used when the consumer is transacted
    // Current setting:
    private Reliability _unrecoverableOptions = Reliability.NONE;
    // Original session setting:
    private Reliability _baseUnrecoverableOptions = Reliability.NONE;

    //shows if this LCP is closed (or being closed)
    private volatile boolean _closed = false;

    //shows if this LCP is being closed
    private volatile boolean _closing = false;

    //A simple wrapper for a registered AsynchConsumerCallback
    private final AsynchConsumer _asynchConsumer = new AsynchConsumer();
    private final DestinationHandler _destinationAttachedTo;

    /**
     * The lock which we use to synchronize with, while there is a consumer
     * thread in-flight with messages being processed.
     * <p>
     * We use this to stop other threads being issued with work to do, but
     * also when the consumer thread needs to stop. The stop stimulus
     * doesn't get a look in until the consumer thread returns. ie: When stop
     * returns, it's completely stopped.
     */
    private Object _asynchConsumerBusyLock;

    private ExternalConsumerLock _externalConsumerLock = null;
    private volatile boolean _interruptConsumer = false;

    //is a callback registered
    private boolean _asynchConsumerRegistered = false;

    //the maximum number of messages which may be delivered in each
    //asynch callback
    private int _maxBatchSize = 0;

    // The maximum number of active (locked or tranactionally deleted)
    // messages assigned to this consumer (including bifurcated children)
    // NOTE: This only applies when bifurcated.
    // NOTE: It is volatile because it can change (by mediations) but we
    //       check it outside of any lock.
    private volatile int _maxActiveMessages = 0;
    private int _currentActiveMessages = 0;
    private boolean _consumerSuspended = false;
    private int _suspendFlags = 0;
    private Object _maxActiveMessageLock = null;
    private int _activeMsgBlockInterval = -1; // (F001731)
    private ActiveMsgBlockAlarm _activeMsgBlockAlarm = null; // (F001731)

    private boolean _bifurcatable = false;

    //Indicates whether to run AsynchConsumer as soon as it is started
    //and on same thread (inline)
    //private boolean deliverImmediately = false; //169892

    //Inline the calling of the asynch callback on the calling thread (the
    // producer's thread probably).
    private boolean _inLine = false;

    //shows if we are waiting in a synchronous receive ... and may be woken
    //up with a new message
    private boolean _waiting = false;

    // This is true when a synchronous consumer is in
    // a receive and waiting for a message.
    // It is always true for an AsynchConsumer unless it is
    // busy consuming messages
    private boolean _ready = false;

    //When this LCP is Ready, a message may be attached to it by the CD
    //This is a reference to that message
    private SIMPMessage _attachedMessage = null;
    //That message may be already stored on an item stream
    private boolean _msgOnItemStream = false;
    //indicates if a message is attached
    private boolean _msgAttached = false;
    // indicates if the attached message has been locked before delivery
    private boolean _msgLocked = false;

    //Used to hold list of all messages locked by this LCP (asynch only)
    private final JSLockedMessageEnumeration _allLockedMessages;

    // Consumer's connection Uuid;
    private final SIBUuid12 _connectionUuid;

    // The keyGroup that this consumer belongs too (if the consumer has
    // an ordering context). The keyGroup is specific to a particular
    // consumer dispatcher.
    private JSKeyGroup _keyGroup = null;
    private OrderingContextImpl _orderingGroup = null;

    // These variables are here to get around the inability to return more
    // than one value from a method without creating an object to hold them
    private JSLocalConsumerPoint _transferredConsumer;// Used by processQueuedMsgs and lockMessages()
    boolean _drainQueue; // Used by processQueuedMsgs and lockMessages()

    // Java chooses to keep the currently running thread running for as long
    // as possible, irrespective if another thread has attempted to take a lock
    // and the running thread releases it then takes it again some time later.
    // Under some circumstances this is desirable but in the case where one thread
    // calls stop or close on an asynch consumer that is currently draining a deep
    // queue it is not.
    // To try to get around this we set this flag outside of any lock to indicate
    // that we're about to try to lock it. If another thread sees this flag set
    // in between releaseing the lock and taking it again it will try to yield the
    // thread and therefore, allow the other thread to take the lock.
    // (yield() may not be correctly implemented on all JVMs but its the best we
    // can do here)
    private volatile boolean _lockRequested = false;

    private TransactionCommon _autoCommitTransaction = null;

    // Indicates whether we have a localQP for this destination on the consuming ME
    private boolean _hasLocalQP;

    // Cached SLME so we can reuse it each time
    private SingleLockedMessageEnumerationImpl _singleLockedMessageEnum = null;

    // Set if currently performing an isolated run (ConsumerSession.activateAsynchConsumer())
    private boolean _isolatedRun = false;

    /**
     * SIB0115: Sequential message failure monitoring
     * 
     * This feature adds the ability to set a limit on how many messages a consumer can
     * 'fail' to consume sequentially before we automatically stop their ConsumerSession.
     * 
     * A failed message is one that is rolled back after being deleted by the consumer
     * (n - 1) times, where n is the maxRetry on a message.
     * 
     * If sequential failure monitoring is enabled a delay may be specified (_hiddenMessageDelay).
     * This is the time (in milliseconds) that a message stays locked for after a rollback,
     * thus hiding it from any consumers for that period of time. A consumer can have at most
     * _maxSequentialFailuresThreshold messages hidden at any one time. Once that limit is
     * reached the whole consumer will become blocked until one of the hidden messages is
     * unlocked and reprocessed. Once a message reached its maxRetry limit this delay no longer
     * applies as the message will either be offloaded to an exception destination of the whole
     * queue point/subscription will be blocked.
     * 
     * This feature is used by MDBs to prevent a queue being immediately drained if there's
     * a long-running problem with the MDB itself.
     */
    // A list of all messages currently 'hidden' from this (or other) consumers (SIB0115)
    private List<SIMPMessage> _hiddenMessages = new ArrayList<SIMPMessage>();
    // Maximum number of sequential failed messages allowed before we stop
    private int _maxSequentialFailuresThreshold = 0;
    // Time in milliseconds that a message is hidden for after a rollback (if sequential failures
    // are being tracked)
    private long _hiddenMessageDelay = 0;
    // The current number of sequential failures that this consumer has performed
    private int _sequentialMessageCounter = 0;
    // Shows if this a 'stoppable' type of asynch consumer, and therefore if sequential
    // failures show be monitored or not.
    private volatile boolean _consumerStoppable = false;
    // If false then no-one has restarted the consumer since we told them we've stopped
    // it - therefore there's no need to tell them again
    private boolean _startSinceSequentialStop = false;
    // AlarmManager to support messages timing out of hidden state
    private MPAlarmManager _alarmManager = null;
    // Flag to keep track of whether a hidden message alarm has been registered
    private boolean _hiddenMsgAlarmRegistered = false;
    // Shows that a thread has been scheduled to stop the consumer
    private boolean _consumerStopThreadRunning = false;

    private Throwable _notifiedException = null;

    // Set to true if a thread is currently working its way through runAsynchConsumer
    // (which takes and releases the 'this' lock repeatedly)
    // Usually this will never occur, the normal behaviour is for a runAsynchConsumer
    // to be kicked off by a start(), if that runs out of queued messages it will end
    // in 'ready' state.
    // At some point after that the ConsumerDispatch may attach a message to it and
    // kick it off again. Another won't be kicked off until this one drains the queue
    // and exits (in 'ready' state).
    // However, there is a third case. If maxActiveMessages are in use a runAsynchConsumer
    // thread will be started when the consumer is resumed (e.g. commits a message). This
    // Can happen while we're already in the middle of a runAsynchConsumer. Resulting in
    // concurrent instances.
    // We only need one running (otherwise you can get ordering issues or multiple 'ready'
    // references in the ConsumerDispatcher) so we allow the first one to complete and the
    // others simply die.
    private boolean _runningAsynchConsumer = false;

    // 597496
    // As the runAsynchConsumer method now releases the asynch lock periodically it's possible
    // for another thread to get in and stop/close the consumer. If that occurs the stop/close
    // still needs to wait for the runAsynchConsumer method to complete before the stop/close
    // completes, otherwise there's a chance that the consumer could be restarted before
    // runAsynchConsumer re-takes the lock - in which case it would incorrectly carry on or
    // prevent a new runAsynchConsumer from correctly processing messages.
    private boolean _runAsynchConsumerInterupted = false;

    // Are we a gathering consumer
    private boolean _gatherMessages = false;

    /**
     * Creates a new LocalConsumerPoint to consume messages from the given
     * Destination.
     * 
     * @param destination
     * @param subState
     * @param consumerSession
     * @throws SIResourceException
     * @throws SIDestinationAlreadyExistsException
     */
    JSLocalConsumerPoint(
                         DestinationHandler destination,
                         JsDestinationAddress jsDestAddr,
                         ConsumerDispatcherState state,
                         ConsumerSessionImpl consumerSession,
                         Reliability unrecoverableReliability,
                         boolean bifurcatable,
                         boolean gatherMessages)

        throws SIDurableSubscriptionMismatchException,
        SIDurableSubscriptionNotFoundException,
        SIIncorrectCallException,
        SISessionDroppedException,
        SISessionUnavailableException,
        SIDestinationLockedException, SISelectorSyntaxException, SIDiscriminatorSyntaxException,
        SINotPossibleInCurrentConfigurationException, SIResourceException, SINonDurableSubscriptionMismatchException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "JSLocalConsumerPoint",
                        new Object[] { destination,
                                      jsDestAddr,
                                      state,
                                      consumerSession,
                                      unrecoverableReliability,
                                      Boolean.valueOf(bifurcatable),
                                      Boolean.valueOf(gatherMessages) });

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Constructing local consumer point " + this);

        // Save parent consumer session
        _consumerSession = consumerSession;
        ConnectionImpl conn = (ConnectionImpl) consumerSession.getConnectionInternal();
        //Save the destination that this consumerSession is attached too
        _destinationAttachedTo = destination;

        // Set up the default lock held when the consumer callback is busy, or
        // when we want to stop it becoming busy.
        _asynchConsumerBusyLock = _asynchConsumer;

        // This lock is used when updating/checking the maxActiveMessage counter
        // It comes below 'this' in the hierarchy so can either be taken once
        // 'this' is held or taken without 'this' as long as you don't call anything
        // from inside the lock which may try to lock 'this'.
        // This lock was introduced to remove the need to lock 'this' while removing
        // an active message to reduce contention on that lock.
        _maxActiveMessageLock = new Object();

        _messageProcessor = conn.getMessageProcessor();
        _txManager = _messageProcessor.getTXManager();
        _connectionUuid = consumerSession.getConnectionUuid();
        _bifurcatable = bifurcatable;
        _gatherMessages = gatherMessages;
        _waiter = this.newCondition();

        // Get a reference to the Alarm Manager to support the expiry of hidden messages
        _alarmManager = _messageProcessor.getAlarmManager();

        // Set the session's recoverability
        if (unrecoverableReliability != null)
            setBaseRecoverability(unrecoverableReliability);

        /* Determine which sort of consumerDispatcher we wish to attach to */
        try
        {
            if (destination.isPubSub())
            {
                if (state.isDurable())
                {
                    /* If durable subscription then we need to create a durable sub */
                    //Create and attach to a durable subscription
                    _consumerKey =
                                    destination.attachToDurableSubscription(this, state);

                    //Retrieve the consumerDispatcher from the consumerKey
                    _consumerDispatcher = _consumerKey.getConsumerManager();

                }
                else
                { // Non-durable pub-sub

                    //create/obtain consumerDispatcher and then attach consumer point to CD
                    _consumerKey =
                                    (ConsumableKey) destination.createSubscriptionConsumerDispatcherAndAttachCP(this, state);

                    //Retrieve the consumerDispatcher from the consumerKey
                    _consumerDispatcher = _consumerKey.getConsumerManager();

                }
            }
            else
            { //pt-pt
              // Pick a consumer dispatcher to receive from
              // Provide any ME that the consumer could have been fixed to
                _consumerDispatcher =
                                (JSConsumerManager) destination.chooseConsumerManager(gatherMessages ? destination.getUuid() : null,
                                                                                      jsDestAddr.getME(),
                                                                                      null);

                if (_consumerDispatcher == null)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "ConsumerDispatcher not found (" + jsDestAddr.getME() + ")");

                    //We can't find a suitable localisation.
                    //Although a queue must have at least one localisation this is
                    //possible if the sender restricted the potential localisations
                    //using a fixed ME or a scoping alias (to an out-of-date set of localisations)
                    //We throw an exception to the application.
                    SIMPNoLocalisationsException e = new SIMPNoLocalisationsException(
                                    nls_cwsik.getFormattedMessage(
                                                                  "DELIVERY_ERROR_SIRC_26",
                                                                  new Object[] { jsDestAddr.getDestinationName() },
                                                                  null));

                    e.setExceptionReason(SIRCConstants.SIRC0026_NO_LOCALISATIONS_FOUND_ERROR);
                    e.setExceptionInserts(new String[] { jsDestAddr.getDestinationName() });

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "JSLocalConsumerPoint", e);

                    throw e;
                }

                // If XD message classification is enabled, then assign this consumer to
                // a ConsumerSet. We determine whether classification is enabled by checking
                // whether a MessageController has been passed to us. Note that we only
                // classify messages for a pt-pt consumer. 
                JSConsumerSet consumerSet = null;
                //attach to the CD (with a selector this time)
                _consumerKey = (ConsumableKey) _consumerDispatcher.attachConsumerPoint(this,
                                                                                       state.getSelectionCriteria(),
                                                                                       consumerSession.getConnectionUuid(),
                                                                                       consumerSession.getReadAhead(),
                                                                                       consumerSession.getForwardScanning(),
                                                                                       consumerSet);
            }
        } catch (SIMPConnectionVersionException e)
        {
            // No FFDC code needed
            // Need to turn this particular resource exception into an incorrect call
            SIIncorrectCallException e2 = new SIIncorrectCallException(e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "JSLocalConsumerPoint", e2);

            throw e2;
        }

        if (!ignoreInitialIndoubts())
            _consumerDispatcher.checkInitialIndoubts(this);

        if (!(_consumerKey instanceof RemoteQPConsumerKey))
            _hasLocalQP = true;

        // Get the default cursor, this we can use for it's lockId
        // _getCursor = _consumerKey.getGetCursor();

        //Create a new instance of the LockedMessageEnumeration
        //This actually holds references to all of the locked messages but only
        //a sub-set are visible at any one time via the AsynchConsumerCallback
        _allLockedMessages =
                        new JSLockedMessageEnumeration(this, _consumerKey, _messageProcessor);

        // Ensure initial state is stopped for Receive Allowed false reason (if applicable)
        checkReceiveAllowed();

        // We keep an autoCommitTransaction in our back pocket just in case we
        // need it later
        _autoCommitTransaction =
                        _messageProcessor.getTXManager().createAutoCommitTransaction();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "JSLocalConsumerPoint", this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.ConsumerPoint#put(com.ibm.ws.sib.processor.impl.ItemReference)
     */
    @Override
    public boolean put(SIMPMessage msg, boolean isOnItemStream)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "put", new Object[] { this, msg, Boolean.valueOf(isOnItemStream), this });

        //we will return this var to say if we have NOT taken the message
        boolean msgTaken = false;
        //indicates if we should try and deliver this message asychronously
        boolean deliverToAsynchCon = false;
        // And on the same thread or not
        boolean inLineAsynchCon = false;

        // We can't hold the LCP lock before we hold the keyGroup lock and we don't want to
        // take the asynch lock as we'll block on a consumerMessage call but the keyGroup
        // is set inside the LCP and asynch locks and could therefore vanish between checking for it
        // and trying to lock it here. So we take a dirty read of the keyGroup and if we
        // got it we'll lock it. This does mean there are slight windows around setting
        // and unsetting of the keyGroup but the worst case is that we lock the group when
        // we didn't need to (as we're no longer a member of the group).
        JSKeyGroup cachedKeyGroup = _keyGroup;

        // We're about to take the consumer's lock and we want to return as quickly as
        // possible because we're currently running on the producer's thread, so we set
        // the flag in the hope that anyone who had the lock will yield for us.
        _lockRequested = true;

        // We're a member of a group so we must lock it before we go for the LCP lock
        // and take the message
        if (cachedKeyGroup != null)
            cachedKeyGroup.lock();

        try
        {
            //Can't put more than one message at once so synchronize on this
            this.lock();
            try
            {
                // We've got the lock now so unset the flag
                _lockRequested = false;

                // Try to take the message that we've been given
                boolean msgLockingError = false;
                try
                {
                    msgTaken = takeMessage(msg, isOnItemStream);
                } catch (SIResourceException e)
                {
                    // No FFDC code needed
                    SibTr.exception(tc, e);

                    msgLockingError = true;
                }

                // Take a snapshot of the deliverToAsynchCon value before we release the lock
                // (if the consumer is in an isolated run they will have been notified as if
                // they were a synchronous consumer (inside takeMessage).
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "asynch consumer reg: " + Boolean.valueOf(_asynchConsumerRegistered)
                                    + ", isolatedRun: " + Boolean.valueOf(_isolatedRun));

                if (msgTaken || msgLockingError)
                    deliverToAsynchCon = (_asynchConsumerRegistered && !_isolatedRun);

                // Remember how they wish to be run
                inLineAsynchCon = _inLine;
            } finally
            {
                this.unlock();
            }
        } finally
        {
            if (cachedKeyGroup != null)
                cachedKeyGroup.unlock();
        }

        // If we want to deliver the attached message to an Asynchronous Consumer
        // (Outside synchronization to avoid lock conflict)
        if (deliverToAsynchCon)
        {
            // If the consumer has said it want's inlining then we should call them
            // on this thread - lets hope it doesn't take too long.
            // We can't do this if the message is being made reavailable as the thread
            // that did this may already hold the LCP lock so it can't take the Asynch
            // lock as this would break the locking hierarchy.
            if (inLineAsynchCon && !msg.isReavailable())
            {
                try
                {
                    runAsynchConsumer(false);
                } catch (Throwable e)
                {
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.put",
                                                "1:817:1.22.5.1",
                                                this);

                    SibTr.exception(tc, e);

                    try
                    {
                        // Since the asynchConsumer has experienced an error of some kind, the best form
                        // of cleanup is to close down the session. This ensures any listeners get notified
                        // and can retry at some point later.
                        closeSession(e);
                    } catch (Exception ee)
                    {
                        // No FFDC code needed
                        SibTr.exception(tc, ee);
                    }

                    if (e instanceof ThreadDeath)
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "put", e);
                        throw (ThreadDeath) e;
                    }

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "put", e);
                }
            }
            else
            {
                try
                {
                    //start up a new thread (from the MP's thread pool)
                    //to deliver the message asynchronously
                    _messageProcessor.startNewThread(new AsynchThread(this, false));
                } catch (InterruptedException e)
                {
                    // No FFDC code needed

                    //Trace only, maybe we'll be able to deliver later?
                    SibTr.exception(tc, e);
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "put", Boolean.valueOf(msgTaken));
        //Does the consumer dispatcher have to pick another consumer?
        //return true if we have taken the message given to us
        return msgTaken;
    }

    /*
     * NOTE: Callers to this method will have the JSLocalConsumerPoint locked
     * (and their JSKeyGroup locked if applicable)
     */
    private boolean takeMessage(SIMPMessage msg, boolean isOnItemStream)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "takeMessage", new Object[] { this, msg, Boolean.valueOf(isOnItemStream) });

        boolean msgTaken = false;

        // Put should only have been called if we said we were Ready but
        // we'll double check we are still Ready as there is a window where
        // we could have stopped/closed/ended the receive after the CD
        // had picked us from the ready list.
        if ((_ready && (_keyGroup == null)) || (_keyGroup != null) && (_keyGroup.isGroupReady()))
        {
            //Consumer is still ready so it can take the message
            //indicate that we're not Ready for any more messages
            //setReadyState(false);
            _ready = false;

            if (_keyGroup != null)
                _keyGroup.groupNotReady();

            // Before we can take the message we need to know that this consumer or its
            // ConsumerSet will allow it (they haven't reached their maxActiveMessage limits).
            // If they will accept it we have to, in the case of a ConsumerSet, 'reserve' the
            // space upfront because multiple members of the set could be concurrently adding
            // active messages. If this add happens to take the last available slot in the set's
            // quota it will keep the set write-locked for the duration of the message lookup. During
            // this time no other consumers in the set will be able to add any more messages.
            // This is done to ensure that the set's suspend/resume state is consistent and we don't
            // have to try to un-suspend consumers when we find there isn't a message to take after all.
            boolean msgAccepted = prepareAddActiveMessage();
            if (!msgAccepted)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "The consumer set is already suspended ");
            }
            else
            {
                boolean activeMessageAttached = false;
                // Whatever the outcome we must commit or rollback the prepareAddActiveMessage()
                try
                {
                    //we HAVE taken this message
                    msgTaken = true;

                    //attach the message we have taken
                    try
                    {
                        activeMessageAttached = attachAndLockMsg(msg, isOnItemStream);
                    } catch (SIResourceException e)
                    {
                        // No FFDC code needed
                        SibTr.exception(tc, e);

                        activeMessageAttached = false;
                        checkWaiting();

                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "takeMessage", e);

                        throw e;
                    }

                    // We found an attached message, therefore we have another active
                    // message so commit the prepare we did above.
                    if (activeMessageAttached)
                        commitAddActiveMessage();

                    checkWaiting();
                } finally
                {
                    // If a message wasn't attached the commit won't have happened, instead
                    // roll back the prepare of the add, as if the prepare never happened.
                    if (!activeMessageAttached)
                        rollbackAddActiveMessage();
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "takeMessage", Boolean.valueOf(msgTaken));

        return msgTaken;
    }

    private void checkWaiting()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkWaiting");

        //if there is a synchronous consumer waiting for a message,
        //wake up that consumer
        if (!_waiting)
        {
            // We've already assigned the msg to the Asynch consumer under the
            // lock but we don't want to start the thread here, defer that till
            // a little later.

            // If we weren't waiting we MUST have an asynch consumer registered
            // otherwise we've screwed up
            if (!_asynchConsumerRegistered)
            {
                SIErrorException e = new SIErrorException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.checkWaiting",
                                                                      "1:985:1.22.5.1" },
                                                        null));

                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.checkWaiting",
                                            "1:991:1.22.5.1",
                                            this);

                SibTr.exception(tc, e);
                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.checkWaiting",
                                          "1:998:1.22.5.1" });

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "checkWaiting", e);

                throw e;
            }
        }

        // If the consumer is waiting on a synchronous receive or in an
        // isolated run (a remote activateAsynchConsumer call) then we wake the
        // thread here.
        if (_waiting || _isolatedRun)
        {
            // Notify the waiting thread here and now
            _waiter.signal();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkWaiting");
    }

    /**
     * Attach and lock a message to this LCP
     * 
     * NOTE: Callers to this method will have the JSLocalConsumerPoint locked
     * (and their JSKeyGroup locked if applicable)
     * 
     * @param msgItem The message to be attached
     * @param isOnItemStream true if the message being attached is stored on the
     *            QP's itemStream
     */
    private boolean attachAndLockMsg(SIMPMessage msgItem, boolean isOnItemStream)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "attachAndLockMsg",
                        new Object[] { msgItem, Boolean.valueOf(isOnItemStream), this });

        // If we're attaching a message and we already have one something's gone really bad.
        // It's too late to stop it so all we can do is issue an FFDC so that when they
        // finally spot a message is stuck in locked state we can see why (although how we
        // got here is still unknown).
        if (_msgAttached)
        {
            String text = "Message already attached [";

            if (_attachedMessage != null)
                text = text + _attachedMessage.getMessage().getMessageHandle();

            text = text + "," + msgItem.getMessage().getMessageHandle() + "]";

            SIErrorException e = new SIErrorException(text);

            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.attachAndLockMsg",
                                        "1:1057:1.22.5.1",
                                        this);
        }

        // If the message is in the Message Store we need to remember the MS ID
        // of this message so that we can retrieve it by ID later
        if (isOnItemStream)
        {
            // If the consumer is a forward scanning consumer then it has to
            // use the MS cursor to find the message (this message may be behind
            // the cursor) so we don't lock it here
            if (_consumerSession.getForwardScanning())
            {
                _msgLocked = false; // We can't lock a message for a FS consumer
                _msgAttached = true;
                _msgOnItemStream = true; // But it is on the itemstream
                _attachedMessage = null; // Let the consumer find the actual message
            }
            // Otherwise the consumer can go directly to the message
            else
            {
                // Determine which cursor to use
                LockingCursor cursor = _consumerKey.getGetCursor(msgItem);
                // We need to lock the message here so that another consumer does
                // not snatch it after we tell the CD that we'll take it, if it did
                // it could potentially result in this consumer being unsatisfied
                // even when there was a matching message available
                try
                {
                    _msgLocked = msgItem.lockItemIfAvailable(cursor.getLockID());
                } catch (MessageStoreException e)
                {
                    // FFDC
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.attachAndLockMsg",
                                                "1:1094:1.22.5.1",
                                                this);

                    SibTr.exception(tc, e);

                    _msgLocked = false;
                    _msgAttached = true;
                    _msgOnItemStream = true;
                    _attachedMessage = null;

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "attachAndLockMsg", e);

                    throw new SIResourceException(e);
                }

                if (_msgLocked)
                {
                    _msgAttached = true;
                    _msgOnItemStream = true;
                    _attachedMessage = msgItem;
                }
                // If we fail to lock it then someone else must have got in there before
                // us. But because we've already removed this consumer from the ready list
                // we're going to have to follow through and wake up the consumer so that
                // they go round the loop of making themselves ready, checking the queue
                // for messages, and waiting. Otherwise we could miss a message that was
                // put on the queue while we weren't ready.
                else
                {
                    // We set the strange combination of _msgAttached and _msgOnItemStream
                    // (but not _msgLocked) so that we force the consumer to check the queue
                    // for more messages before continueing (see getAttachedMessage).
                    _msgAttached = true;
                    _msgOnItemStream = true;
                    _attachedMessage = null;

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Missed the message!");
                }
            }
        }
        // We locked the message (if it was never on the itemStream we've sort of
        // locked it by default).
        else
        {
            _msgLocked = false;
            _msgAttached = true;
            _msgOnItemStream = false;
            _attachedMessage = msgItem;
        }

        // If we are a member of a key group we must make the group aware that a
        // message has been attached to one of its members
        if (_msgAttached && (_keyGroup != null))
            _keyGroup.attachMessage(_consumerKey);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "attachAndLockMsg", Boolean.valueOf(_attachedMessage != null));

        return (_attachedMessage != null);
    }

    /**
     * If a message has been attached to this LCP, detach it and return it
     * 
     * NOTE: Callers to this method will have the JSLocalConsumerPoint locked
     * 
     * @return The message which was attached
     * @throws SIResourceException
     */
    SIMPMessage getAttachedMessage() throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getAttachedMessage", this);

        SIMPMessage msg = null;

        //check if there really is a message attached
        if (_msgAttached)
        {
            // If the message wasn't locked when it was attached (we are a forwardScanning
            // consumer) we need to go and look for it now.
            if (_msgOnItemStream && !_msgLocked)
            {
                // Attempt to lock the attached message on the itemstream
                msg = getEligibleMsgLocked(null);
            }
            else
                msg = _attachedMessage;

            //show that there is no longer an attached message available
            _msgAttached = false;
            _msgLocked = false;
            _attachedMessage = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getAttachedMessage", msg);

        //simply return the attached message (if there is one)
        return msg;
    }

    /**
     * Checks the destination allows receive
     * 
     * If receive not allowed, and LCP started, stops it
     * If receive allowed, and LCP stopped, may start it
     * 
     */
    private boolean checkReceiveAllowed() throws SISessionUnavailableException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkReceiveAllowed");

        boolean allowed = _destinationAttachedTo.isReceiveAllowed();

        if (!allowed)
        {
            // Register that LCP stopped for Receive Allowed
            _stoppedForReceiveAllowed = true;

            // Stop LCP for Receive Allowed
            if (!_stopped)
            {
                internalStop();
            }
        }
        else
        {
            // Register that LCP not stopped for Receive Allowed
            _stoppedForReceiveAllowed = false;

            // Start LCP if no reason to be stopped
            if ((_stopped)
                && (!_stoppedByRequest))
            {
                internalStart(false);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkReceiveAllowed", Boolean.valueOf(allowed));

        return allowed;
    }

    /**
     * Checks the state for the synchronous consumer
     * 
     * Will check for not closed,
     * AsynchConsumer
     * and if it is already in a wait
     * 
     * If any of those conditions appear - an exception will be thrown.
     * 
     */
    private void checkReceiveState()
                    throws SIIncorrectCallException, SISessionUnavailableException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkReceiveState");

        // Only valid if the consumer session is still open
        checkNotClosed();

        //if there is an AsynchConsumerCallback registered, throw an exception
        if (_asynchConsumerRegistered)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkReceiveState", "asynchConsumerRegistered == true ");

            throw new SIIncorrectCallException(
                            nls.getFormattedMessage(
                                                    "RECEIVE_USAGE_ERROR_CWSIP0171",
                                                    new Object[] { _consumerDispatcher.getDestination().getName(),
                                                                  _messageProcessor.getMessagingEngineName() },
                                                    null));
        }
        // If we already have the waiting bit set it implies another
        // thread is sat waiting in a receive for this consumer (we release
        // the consumer lock while waiting so this is possible).
        else if (_waiting)
        {
            SIIncorrectCallException e =
                            new SIIncorrectCallException(
                                            nls.getFormattedMessage(
                                                                    "RECEIVE_USAGE_ERROR_CWSIP0178",
                                                                    new Object[] { _consumerDispatcher.getDestination().getName(),
                                                                                  _messageProcessor.getMessagingEngineName() },
                                                                    null));

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkReceiveState", "receive already in progress");

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkReceiveState");
    }

    /**
     * removeUnwantedMsgs - removes messages that are not required due to noLocal checking
     * 
     * @param list of unwanted msgs
     * @param transaction under which to delete them
     */
    private void removeUnwantedMsgs(List<SIMPMessage> unwantedMsgList, TransactionCommon tran) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "removeUnwantedMsgs", new Object[] { this, unwantedMsgList, tran });

        LocalTransaction localTran = null;
        Transaction msTran = null;
        if (tran == null)
        {
            localTran = _txManager.createLocalTransaction(true);
            msTran = (Transaction) localTran;
        }
        else
        {
            msTran =
                            _messageProcessor.resolveAndEnlistMsgStoreTransaction(tran);
        }

        //We iterate through all the message that were marked for deletion and delete them.
        //Under the transaction we created
        Iterator<SIMPMessage> it = unwantedMsgList.iterator();
        while (it.hasNext())
        {
            // Remove the ineligible message from the itemstream
            try
            {
                SIMPMessage unwanted = it.next();

                unwanted.remove(
                                msTran,
                                _consumerKey.getGetCursor(unwanted).getLockID());
            } catch (MessageStoreException e)
            {
                // MessageStoreException shouldn't occur so FFDC.
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.removeUnwantedMsgs",
                                            "1:1340:1.22.5.1",
                                            this);

                SibTr.exception(tc, e);

                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint",
                                          "1:1348:1.22.5.1",
                                          SIMPUtils.getStackTrace(e),
                                          _consumerDispatcher.getDestination().getName() });

                //deactivate this consumer
                _waiting = false;
                unsetReady();

                //Rethrow the exception
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "removeUnwantedMsgs", e);

                throw new SIResourceException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint",
                                                                      "1:1365:1.22.5.1",
                                                                      e,
                                                                      _consumerDispatcher.getDestination().getName() },
                                                        null),
                                e);
            }
        }

        try
        {
            // Commit the deletion of the ineligible messages (if we have our own transaction)
            if (localTran != null)
            {
                localTran.commit();
            }
        } catch (SIException e)
        {
            // MessageStoreException shouldn't occur so FFDC.
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.removeUnwantedMsgs",
                                        "1:1387:1.22.5.1",
                                        this);

            SibTr.exception(tc, e);

            //deactivate this consumer
            _waiting = false;
            unsetReady();

            //Rollback the transaction if something went wrong
            //If there were no eligible messages on the itemstream

            try
            {
                localTran.rollback();
            } catch (SIException ee)
            {
                FFDCFilter.processException(
                                            ee,
                                            "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.removeUnwantedMsgs",
                                            "1:1408:1.22.5.1",
                                            this);

                SibTr.exception(tc, e);

                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint",
                                          "1:1416:1.22.5.1",
                                          SIMPUtils.getStackTrace(ee) });

                //Rethrow the exception
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "removeUnwantedMsgs", ee);

                throw new SIResourceException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint",
                                                                      "1:1428:1.22.5.1",
                                                                      ee },
                                                        null),
                                ee);
            }

            //Rethrow the exception
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "removeUnwantedMsgs", e);

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint",
                                      "1:1441:1.22.5.1",
                                      SIMPUtils.getStackTrace(e) });

            throw new SIResourceException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint",
                                                                  "1:1449:1.22.5.1",
                                                                  e },
                                                    null),
                            e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "removeUnwantedMsgs");
    }

    /**
     * Retrieves the next eligible message for delivery to this consumer. Performs
     * a check for noLocal and takes this into account when retrieving the next eligible
     * message. If any messages are not eligible for delivery, they are deleted from the
     * itemstream before the first eligble one is returned.
     * 
     * NOTE: Callers to this method will have the JSLocalConsumerPoint locked
     * 
     * @returns SIMPMessage - the next eligible message for this consumer.
     * 
     */
    private SIMPMessage getEligibleMsgLocked(TransactionCommon tranImpl) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getEligibleMsgLocked", new Object[] { this, tranImpl });

        /*
         * 166829.1 Support for noLocal delivery
         * 
         * If we are pubsub and noLocal has been specified we should not deliver the message
         * to this consumer if it was published on the same connection. We cannot do this at
         * fanout stage since we do not know the current connection until delivery time.
         * 
         * On delivery we need to iterate through the itemstream until we find an eligible
         * message (i.e. not from this connection). For each message that does not comply, we
         * mark it for deletion. If we find a message, we deliver and delete the marked messages
         * under the given transaction.
         * If we do not find a message, we create a transaction and delete all the marked messages.
         */

        SIMPMessage msg = null;

        // Before we try to lock a message we need to know that this consumer or its
        // ConsumerSet will allow it (they haven't reached their maxActiveMessage limits).
        // If they will accept it we have to, in th case of a ConsumerSet, 'reserve' the
        // space upfront because multiple members of the set could be concurrently adding
        // active messages. If this add happens to take the last available slot in the set's
        // quota it will keep the set write-locked for the duration of the message lookup. During
        // this time no other consumers in the set will be able to add any more messages.
        // This is done to ensure that the set's suspend/resume state is consistent and we don't
        // have to un-suspend consumers when we find there isn't a message on the queue
        // after all.
        boolean msgAccepted = prepareAddActiveMessage();
        if (!msgAccepted)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "The consumer (or its set) is currently maxed out");
        }
        else
        {
            // We do the following in a try so that we're guaranteed to release any activeMessage
            // lock that we may hold (in JSConsumerSet.addActiveMessage())
            try
            {
                // Retrieve the next message on the itemstream
                msg = retrieveMsgLocked();

                // If noLocal is set, then we check if this message came from the same connection.
                // If so, we mark it for deletion.
                if (msg != null && _consumerDispatcher.getConsumerDispatcherState().isNoLocal())
                {
                    LinkedList<SIMPMessage> markedForDeletion = new LinkedList<SIMPMessage>();
                    int msgCount = 0;
                    while (msg != null && _connectionUuid.equals(msg.getProducerConnectionUuid()))
                    {
                        markedForDeletion.add(msg);
                        msgCount++;
                        if (msgCount >= NO_LOCAL_BATCH_SIZE)
                        {
                            removeUnwantedMsgs(markedForDeletion, null);
                            markedForDeletion.clear();
                            msgCount = 0;
                        }
                        // If we weren't looking for a particular message then we can just lock the
                        // next available one.
                        msg = retrieveMsgLocked();
                    }

                    // If any messages were marked for deletion, then delete them.
                    if (!markedForDeletion.isEmpty())
                    {
                        //If we reached the end of the itemstream (and therefore have a null message) then
                        //we need to create our own transaction under which to delete the marked messages. If
                        //not, we have a message and we deliver and delete under the given transaction.
                        TransactionCommon tran = null;
                        if (tranImpl != null && msg != null)
                            tran = tranImpl;

                        removeUnwantedMsgs(markedForDeletion, tran);
                    }
                }
            } finally
            {
                // If a message was locked we commit the adding of it to the maxActiveMessage
                // quotas
                if (msg != null)
                    commitAddActiveMessage();
                // If no message was there then we rollback the prepared add that we did above.
                else
                    rollbackAddActiveMessage();
            }
        } // msgAccepted

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getEligibleMsgLocked", msg);

        return msg;
    }

    /**
     * <p>Performs a synchronous receive.</p>
     * <p>A positive timeout causes this method to check
     * once for a message and then wait for that amount of time, during which it may
     * be woken up when a message becomes available. However, by the time we actually
     * try and get hold of that message, it may have already gone so we go back and
     * continue to wait out any remaining time.</p>
     * <p>A timeout of zero causes this method to wait indefinitely until a message
     * is obtained.</p>
     * <p>A timeout of NO_WAIT (-1) causes this method to just check once and return
     * without waiting at all</p>
     * 
     * @param timeout The time to wait for a message, in milliseconds
     * @param transaction The transaction to be used to get messages from the msgStore.
     *            If this is specified as null, internal LocalTransaction is created for the job.
     * @return The obtained message, if any
     *         AsynchConsumerCallback registered when the recieve is attempted
     * @throws SIResourceException Thrown if a problem occurs in the msgStore.
     */
    @Override
    public JsMessage receive(long originalTimeout,
                             TransactionCommon transaction)
                    throws SISessionUnavailableException,
                    SIIncorrectCallException,
                    SIResourceException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "receive",
                        new Object[] { this, Long.valueOf(originalTimeout), transaction });

        if (transaction != null && !transaction.isAlive())
        {
            SIIncorrectCallException e = new SIIncorrectCallException(nls.getFormattedMessage(
                                                                                              "TRANSACTION_RECEIVE_USAGE_ERROR_CWSIP0777",
                                                                                              new Object[] { _destinationAttachedTo },
                                                                                              null));

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "receive", e);

            throw e;
        }

        //indicates if we should continue to wait for a message or not
        boolean timeToRun = true;
        SIMPMessage msg = null;

        long timeout = originalTimeout;
        //The time we start this call
        long startTimeMillis = java.lang.System.currentTimeMillis();
        long currentTimeMillis = startTimeMillis;

        // This is used to tell if we have locked the message by removing
        // it from the item stream.  This will allow us later to remove the
        // message from the item stream and unlock it.
        boolean messageLocked = false;
        // Used to tell if we passed the message straight through
        // without touching an itemstream
        boolean wasMessageOnStream = false;

        //Local tran was created for this receive
        boolean localTranCreated = false;
        LocalTransaction localTran = null;

        //while we should still be waiting for a message
        while (timeToRun)
        {
            // Synchronize to check and modify consumer state
            this.lock();
            try
            {
                // Check that this consumer can receive the message.
                checkReceiveState();

                // If message ordering is required, throw exception if current receive on a different tran is active
                if (_destinationAttachedTo.isOrdered() && !_consumerDispatcher.isNewTransactionAllowed(transaction))
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "receive", "SISessionUnavailableException - transaction active");

                    throw new SISessionUnavailableException(
                                    nls.getFormattedMessage(
                                                            "ORDERED_TRANSACTED_RECEIVE_ERROR_CWSIP0180",
                                                            new Object[] {
                                                                          _destinationAttachedTo.getName(),
                                                                          _messageProcessor.getMessagingEngineName() },
                                                            null));

                    // suspendConsumer(SUSPEND_FLAG_TRAN_ACTIVE);
                }

                //Shows that this is a synchronous consumer waiting for a message
                _waiting = true;
                //if this was intended by the caller as a non-transactional operation,
                //then it is not recoverable
                if (transaction == null)
                {
                    _transacted = false;
                }
                else
                {
                    _transacted = true;
                }
                //if this LCP is stopped when the receive call is made then we can't look for messages
                //just yet. However, we could still begin looking for a message when we are started,
                //assuming we haven't run out of time
                if (_stopped || (_suspendFlags & SUSPEND_FLAG_RETRY_TIMER) > 0)
                {
                    _waitingToStart = true;
                }
                else
                {
                    //at this point we know we're not closed or stopped and we know there isn't an
                    //AsynchConsumerCalback registered, so we're definitely ready to receive a message
                    setReady();
                    //attempt to retrieve an eligible message in a locked state
                    msg = getEligibleMsgLocked(transaction);

                    if (msg != null)
                    {
                        // Indicate that we have locked the message by getting it.
                        messageLocked = true;
                        timeToRun = false;
                        unsetReady();
                    }
                }

                // If we don't have a message we may have to wait for one to arrive.
                // If consuming remote from the destination the Cd will be interested
                // in this nugget of information (even if we're not planning on waiting)
                // When running locally this is a no-op
                // 512943.1 - if gathering, we may have got no msg back but still need to 
                // call waiting to initiate prefils to the remote DMEs
                if (msg == null || _gatherMessages)
                {
                    timeout = _consumerKey.waiting(timeout, true);
                    // If we do a remote get with no wait - we need to wait at least
                    // the time of the roundTripTime (minus the time taken to get here)
                    if (originalTimeout == LocalConsumerPoint.NO_WAIT)
                        originalTimeout = timeout - (System.currentTimeMillis() - startTimeMillis);
                }

                //First time through, we've checked for a message once, if the timeout is set
                //to NO_WAIT then indicate that we're not going to wait for a message to
                //turn up
                if (timeout == LocalConsumerPoint.NO_WAIT)
                {
                    timeToRun = false;
                }
                //if we haven't got a message yet and we are prepared to wait a bit
                //longer for one to turn up
                if (msg == null && timeToRun)
                {
                    try
                    {
                        //wait for the specified time
                        if (timeout == LocalConsumerPoint.INFINITE_WAIT)
                            _waiter.await();
                        else
                            _waiter.await(timeout, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e)
                    {
                        // No FFDC code needed
                        // No message arrived so return null
                    }
                }

                //we're not waiting any longer
                _waitingToStart = false;
                _waiting = false;
                //we're not ready for any more messages
                unsetReady();

                //Have we been woken up with a message?
                if (_msgAttached)
                {
                    wasMessageOnStream = _msgOnItemStream;
                    //attempt to retrieve the message in a locked state
                    msg = getAttachedMessage();

                    //if we got a message successfully then don't bother waiting any longer
                    if (msg != null)
                    {
                        // If the message was on the item stream
                        // then we would have locked it from the getAttachedMessage() call
                        if (wasMessageOnStream)
                            messageLocked = true;

                        timeToRun = false;
                    }
                }
                // If we didn't get a message, maybe we were interupted with an exception.
                else if (_notifiedException != null)
                {
                    Throwable e = _notifiedException;
                    _notifiedException = null;

                    SibTr.exception(tc, e);

                    if (e instanceof SISessionUnavailableException)
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "receive", e);

                        throw (SISessionUnavailableException) e;
                    }
                    else if (e instanceof SIIncorrectCallException)
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "receive", e);

                        throw (SIIncorrectCallException) e;
                    }
                    else if (e instanceof SIResourceException)
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "receive", e);

                        throw (SIResourceException) e;
                    }
                    else if (e instanceof SINotPossibleInCurrentConfigurationException)
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "receive", e);

                        throw (SINotPossibleInCurrentConfigurationException) e;
                    }
                    else
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "receive", e);

                        throw new SIErrorException(e);
                    }
                }
            } // synchronized
            finally
            {
                this.unlock();
            }

            //if we still want to wait some more and the original timeout wasn't infinite (i.e. 0)
            if (timeToRun && timeout != LocalConsumerPoint.INFINITE_WAIT)
            {
                //the time left to wait is the originalTimeout minus the difference
                //between now and when we starte
                currentTimeMillis = java.lang.System.currentTimeMillis();
                timeout = originalTimeout - (currentTimeMillis - startTimeMillis);

                //if there is no time left to wait
                if (timeout <= 0)
                {
                    //then don't wait any longer!
                    timeToRun = false;
                }
            }
        }

        JsMessage returnMsg = null;

        if (msg != null)
        {

            try
            {
                // Copy the message for receive when the connection indicates that it should.
                // If the message was not on the ItemStream there is no chance of the consumer
                // fiddling with the message then backing out, in which case there is no need
                // to copy it.
                // Make another copy if we are pub/sub
                if ((messageLocked || _consumerDispatcher.isPubSub()) && ((ConnectionImpl) (_consumerSession.getConnection())).getMessageCopiedWhenReceived())
                    returnMsg = msg.getMessage().getReceived();
                else
                    returnMsg = msg.getMessage();
            } catch (MessageCopyFailedException e)
            {
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.receive",
                                            "1:1856:1.22.5.1",
                                            this);

                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint",
                                          "1:1862:1.22.5.1",
                                          SIMPUtils.getStackTrace(e) });

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "receive", e);

                throw new SIResourceException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint",
                                                                      "1:1875:1.22.5.1",
                                                                      e },
                                                        null),
                                e);
            }

            if (!msg.isItemReference())
            {
                if ((msg.getReportCOD() != null) || (msg.isFromRemoteME()))
                {
                    // If the message wasn't in the MS we can't rely on a callback
                    // to drive the report, instead we drive it ourselves
                    if (!messageLocked)
                        ((MessageItem) msg).beforeCompletion(_autoCommitTransaction);
                    else
                    {
                        // If the message is not transacted and a report message is
                        // required, we cant use the cached auto-commit transaction
                        // so we create a local transaction to use
                        if (transaction == null)
                        {
                            localTran = _txManager.createLocalTransaction(false);
                            transaction = localTran;
                            localTranCreated = true;
                        }

                        transaction.registerCallback(msg);

                    }
                }
            }

            // Set underlying message attributes if there's a chance of them being
            // different from the defaults
            if ((returnMsg != null) && messageLocked)
            {
                //set the redeliveredCount if required
                if (msg.guessRedeliveredCount() != 0)
                    returnMsg.setRedeliveredCount(msg.guessRedeliveredCount());

                // Calculate the message wait time
                // NB: Idealy we'd only do this if we knew stats was on, or we needed
                //     it in the message but we don't have the stats information
                //     available to us here.
                long waitTime = msg.updateStatisticsMessageWaitTime();

                //Only store wait time in message for mediations or when
                // explicitly asked for
                if (((ConnectionImpl) (_consumerSession.getConnection())).getSetWaitTimeInMessage())
                {
                    returnMsg.setMessageWaitTime(waitTime);
                }

            }

            //We have successfully obtained a locked message,
            //get it out of the CD's itemStream and pass it back to the consumer
            try
            {
                //Remove the message from the itemstream if the message was locked
                if (messageLocked)
                {
                    // If no transaction was supplied, then use the cached one.
                    if (transaction == null)
                    {
                        transaction = _autoCommitTransaction;
                    }

                    Transaction msTran = _messageProcessor.resolveAndEnlistMsgStoreTransaction(transaction);

                    // set currentTransaction to be the one in use
                    if (_destinationAttachedTo.isOrdered())
                        _consumerDispatcher.setCurrentTransaction(transaction, null);

                    // maxActiveMessages on the consumer is only for use with asynch consumers but if this
                    // consumer is a member of a ConsumerSet then it'll be keeping track of all active messages
                    // across the set so we need to register with the tran to make sure the count is decremented
                    // when this transaction is committed/rolled back.
                    // We will also have incremented our count if we were previously a bifurcatable async consumer 
                    if (_consumerKey.getConsumerSet() != null ||
                        (_bifurcatable && (_maxActiveMessages != 0)))
                        transaction.registerCallback(this);

                    msg.remove(msTran, _consumerKey.getGetCursor(msg).getLockID()); // 172968

                    if (localTranCreated)
                    {
                        localTran.commit();
                        transaction = null;
                    }
                }
                // If the message never got into the message store we'll still have incremented the active
                // mesasge count on any ConsumerSet we're a member of, so we must decrement it here (as
                // there's no transaction to use the commit/rollback of.
                // We will also have incremented our count if we were previously a bifurcatable async consumer 
                else if (_consumerKey.getConsumerSet() != null ||
                         (_bifurcatable && (_maxActiveMessages != 0)))
                    removeActiveMessages(1);
            } catch (MessageStoreException e)
            {
                // MessageStoreException shouldn't occur so FFDC.
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.receive",
                                            "1:2010:1.22.5.1",
                                            this);

                SibTr.exception(tc, e);

                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint",
                                          "1:2018:1.22.5.1",
                                          SIMPUtils.getStackTrace(e),
                                          _consumerDispatcher.getDestination().getName() });

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "receive", e);

                throw new SIResourceException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint",
                                                                      "1:2030:1.22.5.1",
                                                                      e,
                                                                      _consumerDispatcher.getDestination().getName() },
                                                        null),
                                e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() &&
            (returnMsg != null))
            SibTr.debug(this, tc, "verboseMsg OUT : " + returnMsg.toVerboseString());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "receive", msg);

        //if all went well and it's now tuesday, we return the message to the consumer
        return returnMsg; // or null;
    }

    /**
     * Wakeup our thread if we're waiting on a receive. This method is normally
     * called as part of remoteDurable when we need to resubmit a get because
     * a previous get caused a noLocal discard.
     */
    protected void waitingNotify()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "waitingNotify");

        this.lock();
        try
        {
            if (_waiting)
                _waiter.signal();
        } finally
        {
            this.unlock();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "waitingNotify");
    }

    /**
     * Attempt to retrieve a message from the CD's itemStream in a locked state.
     * 
     * @return A locked message
     */
    private SIMPMessage retrieveMsgLocked() throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "retrieveMsgLocked", new Object[] { this });

        SIMPMessage msg = null;

        // Look in the Cd's ItenStream for the next available message
        try
        {

            msg = _consumerKey.getMessageLocked();

            if (msg != null)
                msg.eventLocked();
        } catch (MessageStoreException e)
        {
            // MessageStoreException shouldn't occur so FFDC.
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.retrieveMsgLocked",
                                        "1:2101:1.22.5.1",
                                        this);

            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint",
                                      "1:2108:1.22.5.1",
                                      SIMPUtils.getStackTrace(e) });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "retrieveMsgLocked", e);

            throw new SIResourceException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint",
                                                                  "1:2119:1.22.5.1",
                                                                  e },
                                                    null),
                            e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "retrieveMsgLocked", msg);
        //return the locked message

        return msg;
    }

    /**
     * Checks that the maxBatchSize is >0
     * Checks that messasgeLockExpiry >=0
     * Checks that maxActiveMessages >=0
     * 
     * @param maxBatchSize
     * @param max
     */
    private void checkParams(int maxActiveMessages,
                             long messageLockExpiry,
                             int maxBatchSize)

                    throws SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "checkParams",
                        new Object[] {
                                      Integer.valueOf(maxActiveMessages),
                                      Long.valueOf(messageLockExpiry),
                                      Integer.valueOf(maxBatchSize) });

        if (maxActiveMessages < 0)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkParams", "SIIncorrectCallException maxActiveMessages < 0");
            throw new SIIncorrectCallException(
                            nls_cwsir.getFormattedMessage(
                                                          "REG_ASYNCH_CONSUMER_ERROR_CWSIR0141",
                                                          new Object[] { Integer.valueOf(maxActiveMessages) },
                                                          null));
        }

        if (messageLockExpiry < 0)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkParams", "SIIncorrectCallException messageLockExpiry < 0");
            throw new SIIncorrectCallException(
                            nls_cwsir.getFormattedMessage(
                                                          "REG_ASYNCH_CONSUMER_ERROR_CWSIR0142",
                                                          new Object[] { Long.valueOf(messageLockExpiry) },
                                                          null));
        }

        if (maxBatchSize <= 0)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkParams", "SIIncorrectCallException maxBatchSize <= 0");

            throw new SIIncorrectCallException(
                            nls_cwsir.getFormattedMessage(
                                                          "REG_ASYNCH_CONSUMER_ERROR_CWSIR0143",
                                                          new Object[] { Integer.valueOf(maxBatchSize) },
                                                          null));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkParams");
    }

    /**
     * Register an AsynchConsumerCallback. This creates the AsynchConsumer wrapper
     * if needed. If there was a callback already registered then it first attempts
     * to process any existing attached messages on that callback.
     * 
     * @param callback The AsynchConsumerCallback to be registered
     * @param maxBatchSize The maximum number of messages to be passed on each callback
     * @param optionalCallbackBusyLock A lock which should be held when the callback
     *            is busy, or to prevent the callback from becoming busy.
     * @throws SIResourceException Thrown if there was a problem in the msgStore
     *             receive right now
     */
    @Override
    public void registerAsynchConsumer(
                                       AsynchConsumerCallback callback,
                                       int maxActiveMessages,
                                       long messageLockExpiry,
                                       int maxBatchSize,
                                       Reliability unrecoverableReliability,
                                       boolean inLine,
                                       OrderingContextImpl orderingGroup,
                                       ExternalConsumerLock optionalCallbackBusyLock)
                    throws SISessionUnavailableException, SISessionDroppedException,
                    SIErrorException,
                    SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "registerAsynchConsumer",
                        new Object[] {
                                      this,
                                      callback,
                                      Integer.valueOf(maxActiveMessages),
                                      Long.valueOf(messageLockExpiry),
                                      Integer.valueOf(maxBatchSize),
                                      unrecoverableReliability,
                                      Boolean.valueOf(inLine),
                                      orderingGroup,
                                      optionalCallbackBusyLock });

        // Check that the values are valid
        checkParams(maxActiveMessages, messageLockExpiry, maxBatchSize);

        if (optionalCallbackBusyLock != null)
        {
            // The callback wants to block our asynch consumer thread sometimes.
            // It wants to know that when it has this lock, the asynch consumer
            // thread is not currently busy.
            // As of 14May2004, the only consumer to use this function is the
            // mediation point. Doing so prevents deadlocks between the mediation
            // state machine lock, and a separate async consumer lock, by making
            // them one and the same lock.
            // Thus, when an action is being processed within the state machine,
            // the async consumer is blocked, and similarly, if the async
            // consumer is busy passing a message to the mediation, no new events
            // can enter the mediation point state machine.

            // Use this optional lock instead of the default lock we wouls use.
            // ie: The "this" reference on the async consumer itself.
            _asynchConsumerBusyLock = optionalCallbackBusyLock;
        }

        // Synchronized against any other asyncConsumer work on this LCP or keyGroup
        synchronized (_asynchConsumerBusyLock)
        {
            // Synchronize on the consumer state
            this.lock();
            try
            {
                // Only valid if the consumer session is still open
                checkNotClosed();

                // we can't register a callback if the LCP is not in a stopped state
                // so throw an exception
                if (!_stopped)
                {
                    SIIncorrectCallException e =
                                    new SIIncorrectCallException(
                                                    nls.getFormattedMessage(
                                                                            "ASYNCH_CONSUMER_RUN_ERROR_CWSIP0176",
                                                                            new Object[] { _consumerDispatcher.getDestination().getName(),
                                                                                          _consumerDispatcher.getMessageProcessor().getMessagingEngineName() },
                                                                            null));

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "registerAsynchConsumer", e);
                    throw e;
                }
                if (_waiting) // We're in a synchronous receive!
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "registerAsynchConsumer", "waiting == true");
                    throw new SIIncorrectCallException(
                                    nls.getFormattedMessage("RECEIVE_USAGE_ERROR_CWSIP0174",
                                                            new Object[] { _consumerDispatcher.getDestination().getName(),
                                                                          _messageProcessor.getMessagingEngineName() },
                                                            null));
                }

                // set the recoverability options (they will only be overridden by
                // an internal Core SPI user (Comms).
                if (unrecoverableReliability != null)
                    setUnrecoverability(unrecoverableReliability);

                // Remember the callback object (could be null)
                _asynchConsumer.registerCallback(callback);
                if (callback != null)
                {
                    // If ordered, override maxbatchsize to be 1
                    // defect 114998 
                    if (_destinationAttachedTo.isOrdered() && maxBatchSize > 1)
                    {
                        _maxBatchSize = 1;
                        // Override and warn if ordered destination
                        SibTr.warning(tc, SibTr.Suppressor.ALL_AFTER_FIRST_SIMILAR_INSERTS, "MAX_BATCH_SIZE_OVERRIDE_WARNING_CWSIP0181",
                                      new Object[] { _destinationAttachedTo.getName() });
                    }
                    else
                        _maxBatchSize = maxBatchSize;

                    _inLine = inLine;
                    _maxActiveMessages = maxActiveMessages;
                    if (_maxActiveMessages > 0)
                    {
                        // (F001731)
                        // If the destination has the "MsgBlockWarningInterval" context property set (probably WPS event sequencing)
                        // then we need to warn if this consumer ever stays stuck for too long with its maximum active messages.
                        _activeMsgBlockInterval = -1;
                        Object value = _destinationAttachedTo.getContextValue("MsgBlockWarningInterval");
                        if ((value != null) && (value instanceof Integer))
                        {
                            _activeMsgBlockInterval = ((Integer) value).intValue();
                            if (_activeMsgBlockInterval < 0)
                                _activeMsgBlockInterval = -1;
                        }
                    }

                    // Reset the active messages under this new asynch consumer
                    _currentActiveMessages = 0;

                    // Tell the LME the currentmessageLockExpiry value
                    _allLockedMessages.setMessageLockExpiry(messageLockExpiry);

                    // This flag is checked by synchronous consumers
                    _asynchConsumerRegistered = true;

                    // Asynch consumers are always transacted.
                    _transacted = true;

                    // Create an ordering group member if we need one.
                    // Ordering in conjunction with pubsub or reading ahead means nothing
                    // so we just ignore the request to order
                    if ((orderingGroup != null) && !_destinationAttachedTo.isPubSub() && !_consumerSession.getReadAhead())
                    {
                        // If we're not just re-registering with the same orderingGroup we have some work
                        // to do.
                        if (_orderingGroup != orderingGroup)
                        {
                            // If we're already in a different group remove ourselves from it first
                            if (_orderingGroup != null)
                                _consumerKey.leaveKeyGroup();

                            _orderingGroup = orderingGroup;

                            // Move this consumer's key into a key group that represents this
                            // ordering group on this consumerDispatcher
                            try
                            {
                                _keyGroup = (LocalQPConsumerKeyGroup) _consumerDispatcher.joinKeyGroup(_consumerKey, orderingGroup);
                            } catch (SIResourceException e)
                            {
                                // FFDC
                                FFDCFilter.processException(
                                                            e,
                                                            "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.registerAsynchConsumer",
                                                            "1:2367:1.22.5.1",
                                                            this);

                                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                    SibTr.exit(tc, "registerAsynchConsumer", "SIIncorrectCallException");
                                throw new SIIncorrectCallException(
                                                nls.getFormattedMessage(
                                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                                        new Object[] {
                                                                                      "com.ibm.ws.sib.processor.impl.ConsumerDispatcher",
                                                                                      "1:2376:1.22.5.1",
                                                                                      SIMPUtils.getStackTrace(e) },
                                                                        null));
                            }

                            if (_keyGroup != null)
                            {
                                // Where we would lock our asynchConsumer we now need to lock the
                                // entire keyGroup.
                                // NOTE: if a callback busy lock was supplied this overrides the group
                                //       lock, we assume this user knows what they are doing (all the ordered
                                //       members have the same callback lock passed in).
                                // From this point on the lock we currently hold (asynchConsumer) is next
                                // to useless as anyone else will lock the asynchGroupLock instead so we
                                // must not do ANYTHING else important after this point!
                                if (optionalCallbackBusyLock == null)
                                    _asynchConsumerBusyLock = _keyGroup.getAsynchGroupLock();
                            }
                        }
                    }
                }
                else
                {
                    // The callback is actually being deregistered
                    _asynchConsumerRegistered = false;
                    // Restore the recoverability of the session
                    resetBaseUnrecoverability();
                    _inLine = false;
                    if (_orderingGroup != null)
                    {
                        // Remove us from the keyGroup
                        _consumerKey.leaveKeyGroup();
                        _keyGroup = null;
                        _asynchConsumerBusyLock = _asynchConsumer;
                        _orderingGroup = null;
                    }
                }

                // We keep a handle to the external lock object as we need to check
                // with it periodically to see if there is anyone wanting to take
                // the lock that we hold.
                _externalConsumerLock = optionalCallbackBusyLock;
            } // synchronized
            finally
            {
                this.unlock();
            }
        } // synchronized

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerAsynchConsumer");
    }

    /**
     * Try to asynchronously deliver any attached messages
     * 
     * @return true if a message was delivered
     * @throws SIResourceException
     * @throws SISessionDroppedException
     */
    boolean processAttachedMsgs() throws SIResourceException, SISessionDroppedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "processAttachedMsgs", this);

        SIMPMessage msg = null;

        // Check if there is a message attached to this consumer, we synchronize to make sure we
        // pick up the current settings and not something from our memory cache (we also do this
        // before we check the keyGroup as that is also changed under this lock)
        this.lock();
        try
        {
            msg = getAttachedMessage();
        } finally
        {
            this.unlock();
        }

        // If we're a member of a group there may be a message attached to one of
        // the members other than us
        if (_keyGroup != null)
        {
            ConsumableKey attachedKey = _keyGroup.getAttachedMember();

            // Another member of the group has a message attached for it so let
            // them process it.
            if ((attachedKey != null) && (attachedKey != _consumerKey))
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "processAttachedMsgs");

                return ((JSLocalConsumerPoint) attachedKey.getConsumerPoint()).processAttachedMsgs();
            }
        }

        //if we actually have a message attached, process it
        if (msg != null)
        {
            // Is it a MessageItem or an ItemReference?
            // Or bifurcatable: We can't do this if it's bifurcatable because the
            // bifurcated consumers need to be able to call readSet() on the LME and
            // get the message back, so we HAVE to add it to the real LME rather than
            // the SingleLockedMessageEnumeration.
            if (!_msgOnItemStream && !_bifurcatable)
            {
                // As the message never got to the MS we won't get a callback to
                // generate a COD from, instead we generate one now using the
                // autoCommitTransaction
                if (!msg.isItemReference() && msg.getReportCOD() != null)
                {
                    ((MessageItem) msg).beforeCompletion(_autoCommitTransaction);
                }

                //if it is not on an itemStream then it is a single attached message.
                //This means that it's not locked in the traditional msgStore sense but
                //we can handle it as if it were because we should be the only ones to
                //have a reference to it at the moment.

                // Have to turn this into a LockedMessageEnumeration
                // Use single unlocked Message version of LME

                // We used the cached one if we have it
                if (_singleLockedMessageEnum != null)
                    _singleLockedMessageEnum.newMessage(msg);
                else
                    _singleLockedMessageEnum = new SingleLockedMessageEnumerationImpl(this, msg);

                //initiate the callback via the AsynchConsumer wrapper
                _asynchConsumer.processMsgs(_singleLockedMessageEnum, _consumerSession);

                // Now that we've processed the message remove it from our active message
                // count (if we're counting). We know we can always do it here because there's no
                // chance of the consumer holding onto the message past exiting consumeMessages()
                // as it's an unrecoverable message.
                removeActiveMessages(1);

                // Clear the message from the cached enum
                _singleLockedMessageEnum.clearMessage();
            }
            else
            {
                // Do we really need this message in the MS?
                boolean isRecoverable = true;
                if ((_unrecoverableOptions != Reliability.NONE) &&
                    (msg.getReliability().compareTo(_unrecoverableOptions) <= 0))
                    isRecoverable = false;

                registerForEvents(msg);

                //store a reference to the locked message in the LME
                _allLockedMessages.addNewMessage(msg, _msgOnItemStream, isRecoverable);

                //initiate the callback via the AsynchConsumer wrapper
                _asynchConsumer.processMsgs(_allLockedMessages, _consumerSession);

                _allLockedMessages.resetCallbackCursor();
            }

            // Now we've processed one message we better check to see if there is
            // someone waiting for the asynch lock
            if (_externalConsumerLock != null)
                _interruptConsumer = _externalConsumerLock.isLockYieldRequested();

            //a message was delivered, return true
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "processAttachedMsgs", Boolean.TRUE);
            return true;
        }

        // no message was delivered, return false
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processAttachedMsgs", Boolean.FALSE);
        return false;
    }

    /**
     * Explicity go and look in the CD's itemStream for messages and deliver them via the
     * asynch callback. The asynch lock MUST be held on entry to this method
     * Each call to this method will process at most _maxBatchSize messages, then return
     * 
     * @param isolatedRun if true then drain the queue even if stopped. Should only
     *            be set to true if this the LCP is in a stopped state, otherwise behaviour is undefined
     * @throws SIResourceException Thrown if there is a problem in the msgStore
     *             when the session is not in the stopped state
     * @throws SISessionDroppedException
     */
    protected JSLocalConsumerPoint processQueuedMsgs(boolean isolatedRun) throws SIResourceException, SISessionDroppedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "processQueuedMsgs", new Object[] { this, Boolean.valueOf(isolatedRun) });

        JSLocalConsumerPoint actualConsumer = this;
        boolean started = true;
        _drainQueue = true;
        long timeout = INFINITE_WAIT;

        if (isolatedRun)
        {
            timeout = NO_WAIT;
        }

        //need to count the number of messages we get so that we don't exceed the maxBatchSize
        int numOfMsgs = 0;

        // The following value indicates that we are an isolatedRun and that we need to perform
        // a wait on a message.
        boolean isolatedWait = false;

        _transferredConsumer = null;

        // Java doesn't like giving up control of a thread once its got it. We repeatedly
        // take and release the consumer lock here but there may be others trying to stop
        // us processing the entire queue before we stop. If they are we can at least try
        // a yield, that may give the JVM the hint.
        if (_lockRequested)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Yield requested");
            Thread.yield();
        }

        ConsumableKey attachedKey = null;

        // We don't hold the LCP lock here (just like in put()) but we do hold the asynch lock
        // so we're protected against the keyGroup being unset underneath us
        if ((_keyGroup != null))
            _keyGroup.lock();

        try
        {
            // We need the consumer lock so that we can set the consumer as
            // ready or not (and check the close state).
            this.lock(); // Short held lock - checking/changing state and locking msgs
            try
            {
                // If the consumer has been closed or is currently suspended (e.g. has hit the
                // maxActiveMEssage limit) then don't go round again looking for another message.
                // If we're suspended we'll be resumed once the reason for suspension has been
                // sorted.
                if (_closed || isSuspended() || !_asynchConsumerRegistered)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "processQueuedMsgs", "Closed:" + _closed + " Suspended:" + _consumerSuspended);

                    started = false;
                    _drainQueue = false;
                }
                // When we're counting active messages there's a possibility that a runAsynchConsumer
                // thread will be dispatched due to a resumeConsumer while a producer thread is just
                // about to attach a message to us (there is a legit window where the ConsumerDispatcher
                // can think we're 'ready' even when we're not). If the attach gets in between the checkin
                // processAttachedMsgs() (called just before this) and here then we've got to realise it
                // and process the attached message first
                else
                {
                    if (_keyGroup != null)
                    {
                        // Look for a message attached to the group, but don't process them under this lock
                        attachedKey = _keyGroup.getAttachedMember();
                    }
                    // When we're counting active messages there's a possibility that a runAsynchConsumer
                    // thread will be dispatched due to a resumeConsumer while a producer thread is just
                    // about to attach a message to us (there is a legit window where the ConsumerDispatcher
                    // can think we're 'ready' even when we're not). If the attach gets in between the checkin
                    // processAttachedMsgs() (called just before this) and here then we've got to realise it
                    // and process the attached message first
                    else if (_msgAttached)
                    {
                        // Process the attached message, but not under this lock
                        attachedKey = _consumerKey; // We're not in a group so it must be for us
                    }

                    // If we don't have an attached message, check the queue
                    if ((attachedKey == null) && _drainQueue)
                    {
                        numOfMsgs = lockMessages(isolatedRun);
                        if (_transferredConsumer != null)
                            actualConsumer = _transferredConsumer;

                        // If we get to this point and we're still ready then we are basically
                        // waiting for messages to arrive, when remote from the destination the CD
                        // needs to know when we are doing this by us calling waiting().
                        // As we are an async consumer we will wait indefinitely for a message
                        // NB. This doesn't actually cause this thread to wait!
                        if ((_ready && (_keyGroup == null)) || (_keyGroup != null) && (_keyGroup.isGroupReady()))
                        {
                            timeout = _consumerKey.waiting(timeout, true);
                            isolatedWait = _isolatedRun;
                        }
                        else if (_gatherMessages) // DID get a msg - but need to consider other partitions...
                            // If we are a gatherer then we still want to call waiting to perform refills.
                            // Dont want to set the timeout though as this will cause us to wait.
                            _consumerKey.waiting(timeout, true);
                    }
                }
            } // sync
            finally
            {
                this.unlock();
            }
        } finally
        {
            if (_keyGroup != null)
                _keyGroup.unlock();
        }

        // Now we've released the lock, process the attached message and exit, forcing
        // runAsynchConsumer to go round the loop again.
        if (attachedKey != null)
        {
            // If we found one, process it
            ((JSLocalConsumerPoint) attachedKey.getConsumerPoint()).processAttachedMsgs();

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "processQueuedMsgs", actualConsumer);
            return (JSLocalConsumerPoint) attachedKey.getConsumerPoint();
        }

        // If the session has been closed but we managed to lock one
        // or more messages we must have taken the asynchConsumer lock
        // before the close() method. Therefore, when we release it here
        // the close() method will eventually aquire it and unlock all
        // locked messages so we don't need to do anything here.
        // That's the theory anyway.
        if (started)
        {
            //if we got at least one message
            if ((numOfMsgs > 0) && (actualConsumer == this))
            {
                //initiate the callback
                _asynchConsumer.processMsgs(_allLockedMessages, _consumerSession);

                // Clean up the LME
                _allLockedMessages.resetCallbackCursor();

                // Go back round in case more came in while we were busy
                if (!isolatedRun)
                    _drainQueue = true;
                else
                    _drainQueue = false;
            }
        }

        // If a message was found on the queue for a different member of the ordering group
        // we process that message now on the correct consumer.
        if (actualConsumer != this)
        {
            actualConsumer.processTransferredMsg();
        }

        // If we're performing a remote or gathering isolated run but failed to find a message
        // at this ME we need to wait for the request to return from the DME. We do it
        // here under the asynch lock so that no-one else can try anything else on
        // this session till we've finished.
        if (isolatedWait)
        {
            this.lock();
            try
            {
                // It's just possible a message arrived before we took this
                // lock, if so skip the wait.
                if (!_msgAttached)
                {
                    // The RCD will have calculated an appropriate period
                    // for this command to take to complete, use that here to
                    // block the thread for at most that period. If a message
                    // is returned in that time this thread will be notified.
                    try
                    {
                        //wait for the specified time
                        if (timeout == LocalConsumerPoint.INFINITE_WAIT)
                            _waiter.await();
                        else
                            _waiter.await(timeout, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e)
                    {
                        // No FFDC code needed
                        // No message arrived so return null
                    }
                }

                //we're not ready for any more messages
                unsetReady();

                // We've finished this isolated run request
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Finished isolatedrun request set to FALSE");
                _isolatedRun = false;
            } finally
            {
                this.unlock();
            }

            // If we had a message delivered while we were waiting, process it
            processAttachedMsgs();
        }

        // After processing messages for this consumer we better check to see
        // if someone else is waiting for the lock
        if (_externalConsumerLock != null)
        {
            // lockMessages() may already have spotted the yield request
            if (!_interruptConsumer)
                _interruptConsumer = _externalConsumerLock.isLockYieldRequested();
        }

        // If we're no longer draining the queue (maybe we've been stopped or
        // ran out of messages) indicate this to the caller by returning null
        if (!_drainQueue)
            actualConsumer = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processQueuedMsgs", actualConsumer);

        return actualConsumer;
    }

    protected void processTransferredMsg() throws SIResourceException, SISessionDroppedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "processTransferredMsg", this);

        // keyGroup lock must be held when calling this method

        //initiate the callback
        _asynchConsumer.processMsgs(_allLockedMessages, _consumerSession);

        // Clean up the LME
        _allLockedMessages.resetCallbackCursor();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processTransferredMsg");
    }

    /*
     * NOTE: Callers to this method will have the JSLocalConsumerPoint locked
     * (and their JSKeyGroup locked if applicable)
     */
    private int lockMessages(boolean isolatedRun) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "lockMessages", new Object[] { this, Boolean.valueOf(isolatedRun) });

        int numOfMsgs = 0;

        _transferredConsumer = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "GatherMessages: " + Boolean.valueOf(_gatherMessages)
                            + ", instance isolatedRun: " + Boolean.valueOf(_isolatedRun)
                            + ", _hasLocalQP: " + Boolean.valueOf(_hasLocalQP));
        // If we're doing an isolated run then we don't want to become ready
        // but we do still want to drain the queue. Unfortunately, if we're remote
        // from the queue we probably won't have a message sat there waiting and
        // therefore we do need to make ourselves ready and issue a request back
        // to the DME. (unless we are gathering in which case we have to treat 
        // the consumer as if it is local)
        if (!isolatedRun || (!_hasLocalQP || _gatherMessages))
        {
            // If the LCP is stopped then do nothing (unless we're an isolated run)
            if (_stopped && !isolatedRun)
            {
                //we're not ready for more messages
                unsetReady();
                //don't try and get any more
                _drainQueue = false;
            }
            else
            {
                // we are ready for new messages, before we look for one on the ItemStream
                // we tell teh COnsumerDispatcher that we're available for messages just to
                // make sure we don't miss one coming in while we're looking on the ItemStream
                // (and not finding any). If we do find a suitable message on the ItemStream
                // we mark ourselves as unready. This obviously introduces the possibility of
                // the ConsumerDispatcher trying to give us a message, but better a false
                // possitive than a message falling down the gap.
                try
                {
                    setReady();
                } catch (SINotPossibleInCurrentConfigurationException e)
                {
                    // No FFDC code needed
                    //don't try and get any more
                    _drainQueue = false;
                }
            }

            // Remember we're in the middle of an isolated run (so that put() knows
            // what to do)
            if (isolatedRun)
                _isolatedRun = isolatedRun;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Instance isolatedRun: " + Boolean.valueOf(_isolatedRun));
        }

        if ((_keyGroup != null) && !_keyGroup.isStarted())
            _drainQueue = false;

        boolean consumerReady = true;

        // If the consumer has asked for batches of messages we'll look for that number
        // of available messages on the queue and give them to the asynch consumer all in
        // one go. If this is a stoppable consumer we'd like to keep the number of messages
        // being processed within the number of possible hidden messages (which equals the
        // maxSequentialFailuresThreshold). So we crop the batch size to the remaining
        // space we have left for hidden messages (or one) whichever is the larger.
        // However, that would limit the batch size when there are no problems which is a little
        // rough so we only do the cropping once we've started hiding messages (which can result
        // in a few more messages being processed/hidden to start with but that's life). 
        int currentMaxBatchSize = _maxBatchSize;
        if (_consumerStoppable && (currentMaxBatchSize > 1) && (_hiddenMessages.size() > 0))
        {
            int remainingHiddenSpace = _maxSequentialFailuresThreshold - _hiddenMessages.size();
            if (remainingHiddenSpace < 1)
                remainingHiddenSpace = 1;
            if (remainingHiddenSpace < currentMaxBatchSize)
                currentMaxBatchSize = remainingHiddenSpace;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "lockMessages", "Cropped maxBatchSize to " + currentMaxBatchSize);
        }

        //while we still want more messages
        while ((numOfMsgs < currentMaxBatchSize) && // filled the batch
               _drainQueue && // emptied the queue
               (!_lockRequested || (numOfMsgs == 0)) && // Asked to interupt
               (!_interruptConsumer || (numOfMsgs == 0)) && // Interrupted (ignore first time round)
               (_transferredConsumer == null)) // switched ordered consumer
        {
            SIMPMessage msg = null;

            if (_keyGroup != null)
            {
                // If we are part of a group, we want to remember which member of the group actually gets matched
                // when the filter is called.
                _keyGroup.setConsumerActive(true);
            }

            //try and get a locked eligible message
            msg = getEligibleMsgLocked(null);

            if (msg != null)
            {
                JSLocalConsumerPoint matchingConsumer = this;

                // We've found a message that matches. If we are a member of
                // an ordering group it is possible that this message matches
                // a different member of the group from us. If that is that is
                // the case we have to possibilities....
                // i) If this is the fist message in the batch we can pass the locked
                // message to the matching consumer for them to process.
                // ii) If we already have messages in our batch we will have to
                // unlock this message and process the batch first.
                if (_keyGroup != null)
                {
                    // find out which member of the group actually got matched
                    matchingConsumer = (JSLocalConsumerPoint) _keyGroup.getMatchingMember(_consumerKey).getConsumerPoint();
                    // and show that we're done with the filter for now
                    _keyGroup.setConsumerActive(false);
                }

                // The message is intended for this consumer
                if (matchingConsumer == this)
                {
                    // If we've got a message then we're no longer interested
                    // in messages straight from the ConsumerDispatcher but if
                    // looking for a batch of messages we may take a long time
                    // to exit this loop, so take ourselves out of the runnin
                    // as soon as possible.
                    if (consumerReady)
                    {
                        consumerReady = false;
                        unsetReady();
                    }

                    // Do we really need this message in the MS?
                    boolean isRecoverable = true;
                    if ((_unrecoverableOptions != Reliability.NONE) &&
                        (msg.getReliability().compareTo(_unrecoverableOptions) <= 0))
                        isRecoverable = false;

                    registerForEvents(msg);

                    //store a reference to the locked message in the LME
                    _allLockedMessages.addNewMessage(msg, true, isRecoverable);

                    //If we were successful, increment our counter
                    numOfMsgs++;

                    // We check for an interupt after every message is locked
                    if (_externalConsumerLock != null)
                        _interruptConsumer = _externalConsumerLock.isLockYieldRequested();
                }
                // The message is intended for another member of the key group
                else
                {
                    // If we already have messages locked to this consumer we must
                    // unlock the last message and process the others first. Then
                    // we can come back and pick this one up later (if it is still
                    // available)
                    if (numOfMsgs > 0)
                    {
                        try
                        {
                            msg.unlockMsg(msg.getLockID(), null, true);
                            msg = null;
                        } catch (MessageStoreException e)
                        {
                            // MessageStoreException shouldn't occur so FFDC.
                            FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.lockMessages",
                                                        "1:2995:1.22.5.1",
                                                        this);

                            SibTr.exception(tc, e);

                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(tc, "lockMessages", e);

                            throw new SIResourceException(
                                            nls.getFormattedMessage(
                                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                                    new Object[] {
                                                                                  "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint",
                                                                                  "1:3008:1.22.5.1",
                                                                                  e },
                                                                    null),
                                            e);
                        }
                    }
                    // This is the only message locked so we can just drive the
                    // correct consumer
                    else
                    {
                        numOfMsgs++;

                        // Mark the fact that we've transferred ownership of a locked message
                        // so that it can be processed by the calling method (outside of any
                        // LCP locks)
                        _transferredConsumer = matchingConsumer;

                        _transferredConsumer.lock();
                        try
                        {
                            _transferredConsumer.unsetReady();
                            _transferredConsumer.addLockedMessage(msg);
                        } finally
                        {
                            _transferredConsumer.unlock();
                        }

                        // We now want to drop out of the loop so we can process this message
                        // with the correct consumer

                        // We check for an interupt after every message is locked
                        if (_externalConsumerLock != null)
                            _interruptConsumer = _externalConsumerLock.isLockYieldRequested();
                    }
                }
            }
            if (msg == null)
            {
                // We have all the messages currently on the
                // queue so we are about to callack to
                // get them processed.

                //stop trying to get any more
                _drainQueue = false;
            }

            // We check for an interupt after every message is locked
            if (_externalConsumerLock != null)
                _interruptConsumer = _externalConsumerLock.isLockYieldRequested();
        } // end while

        // If we're an isolated run and we found a message (i.e. not ready) then
        // no-one will try to deliver a message to us and therefore we can forget
        // that we were ever here. On the other hand, if we couldn't find a message
        // and the queue is remote we need to signal that we're still waiting.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Instance isolatedRun: " + Boolean.valueOf(_isolatedRun) +
                            ", method isolatedRun: " + Boolean.valueOf(isolatedRun) +
                            ", ready: " + Boolean.valueOf(_ready));
        if (isolatedRun && !_ready)
            _isolatedRun = false;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "lockMessages", Integer.valueOf(numOfMsgs));

        return numOfMsgs;
    }

    protected void addLockedMessage(SIMPMessage msg)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "addLockedMessage", new Object[] { this, msg });

        // Prevent any more messages being assigned to this consumer
        unsetReady();

        // Do we really need this message in the MS?
        boolean isRecoverable = true;
        if ((_unrecoverableOptions != Reliability.NONE) &&
            (msg.getReliability().compareTo(_unrecoverableOptions) <= 0))
            isRecoverable = false;

        //store a reference to the locked message in the LME
        _allLockedMessages.addNewMessage(msg, true, isRecoverable);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "addLockedMessage");
    }

    /**
     * Run the asynch consumer once only when the session is stopped.
     * 
     * registered or if the session is not stopped
     * 
     * @throws SIResourceException thrown if there is a problem in the message store
     */
    @Override
    public void runIsolatedAsynch(boolean deliverImmediately) throws SIIncorrectCallException, SISessionUnavailableException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "runIsolatedAsynch", new Object[] { this, Boolean.valueOf(deliverImmediately) });

        synchronized (_asynchConsumerBusyLock)
        {
            // Lock the consumer session while we check that it is in a valid
            // state
            this.lock();
            try
            {
                // Only valid if the consumer session is still open
                checkNotClosed();

                //if there is no callback registered, throw an exception
                if (!_asynchConsumerRegistered)
                {
                    SIIncorrectCallException e =
                                    new SIIncorrectCallException(
                                                    nls.getFormattedMessage(
                                                                            "ASYNCH_CONSUMER_ERROR_CWSIP0175",
                                                                            new Object[] { _consumerDispatcher.getDestination().getName(),
                                                                                          _messageProcessor.getMessagingEngineName() },
                                                                            null));

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "runIsolatedAsynch", e);

                    throw e;
                }
                // we can't do an isolated run if the LCP is not in a stopped state
                // so throw an exception
                if (!_stopped)
                {
                    SIIncorrectCallException e =
                                    new SIIncorrectCallException(
                                                    nls.getFormattedMessage(
                                                                            "ASYNCH_CONSUMER_RUN_ERROR_CWSIP0176",
                                                                            new Object[] { _consumerDispatcher.getDestination().getName(),
                                                                                          _consumerDispatcher.getMessageProcessor().getMessagingEngineName() },
                                                                            null));

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "runIsolatedAsynch", e);
                    throw e;
                }

                // If the consumer has been stopped because the destination is not
                // allowing consumers to get messages then we simply return.
                if (_stoppedForReceiveAllowed)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "runIsolatedAsynch", "Receive not allowed");

                    return;
                }
            } // synchronized (this)
            finally
            {
                this.unlock();
            }
        } // synchronized (asynchConsumer)

        //if we get this far then if deliverImmediately is set then this
        //implies that the callback should be inline
        if (deliverImmediately)
        {
            //run the asynch inline
            try
            {
                runAsynchConsumer(true);
            } catch (Throwable e)
            {
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.runIsolatedAsynch",
                                            "1:3182:1.22.5.1",
                                            this);

                SibTr.exception(tc, e);

                try
                {
                    // Since the asynchConsumer has experienced an error of some kind, the best form
                    // of cleanup is to close down the session. This ensures any listeners get notified
                    // and can retry at some point later.
                    _consumerSession.close();
                    // don't notify asynchconsumer as we are been called inline
                } catch (Exception ee)
                {
                    // No FFDC code needed
                    SibTr.exception(tc, ee);
                }

                if (e instanceof ThreadDeath)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "runIsolatedAsynch", e);
                    throw (ThreadDeath) e;
                }

                SISessionDroppedException sessionDroppedException = new SISessionDroppedException(
                                nls.getFormattedMessage("CONSUMER_CLOSED_ERROR_CWSIP0177"
                                                        , new Object[] { _consumerDispatcher.getDestination().getName(),
                                                                        _messageProcessor.getMessagingEngineName() }
                                                        , null), e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "runIsolatedAsynch", sessionDroppedException);

                //inline call so throwing exception rather than notifying
                throw sessionDroppedException;
            }
        }
        else
        {
            //start up a new thread (from the MP's thread pool)
            //to deliver the message asynchronously
            try
            {
                _messageProcessor.startNewThread(new AsynchThread(this, true));
            } catch (InterruptedException e)
            {
                // No FFDC code needed

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "runIsolatedAsynch", e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "runIsolatedAsynch");
    }

    /**
     * Go and look for both attached messages and messages on the QP.
     * If any are found then deliver them via the asynch callback
     * 
     * @param isolatedRun if true then call the Asynch callback at most once
     * @throws SIResourceException Thrown if there are problems in the msgStore
     * @throws SISessionDroppedException
     */
    void runAsynchConsumer(boolean isolatedRun) throws SIResourceException, SISessionDroppedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "runAsynchConsumer", new Object[] { this, Boolean.valueOf(isolatedRun) });

        JSLocalConsumerPoint nextConsumer = this;
        boolean firstTimeRound = true;

        // We continue processing messages until we're told not to (we, or a
        // member of our group stops) or we run out of messages on our queue.
        // Each time round we drop the async lock
        do // while(nextConsumer != null)
        {
            // If we've been asked to yield, we yield
            if (_interruptConsumer)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "External yield requested");

                Thread.yield();

                // We would expect to be stopped by the interrupting thread
                if (!_stopped)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Yield appears to have been ignored");
                }
            }

            synchronized (_asynchConsumerBusyLock)
            {
                // Another runAsynchConsumer thread could get kicked off in parallel due to a suspended
                // active-message consumer being resumed or maybe a message being attached by
                // the ConsumerDispatcher.
                // As we release and re-take the asynch lock each time round we need to keep
                // a track of who was here first and let them continue, killing off any threads
                // that come in halfway through.
                if (!_runningAsynchConsumer || (!firstTimeRound && _runningAsynchConsumer))
                {
                    // check to see if we still want to do this, if we dropped the lock it's possible
                    // someone else came in and stopped/closed us. In which case, they'll still be waiting
                    // for us to come back to them to say we've finished. So we need to notify them
                    // on our way out
                    if ((_stopped && !(isolatedRun))
                        || (!_stopped && isolatedRun) // We have to be stopped while in an isolated run
                        || _closed
                        || !_asynchConsumerRegistered)
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "We've been interferred with: " + _stopped + ", " + _closed + ", " + _asynchConsumerRegistered);

                        // Obviously not, so bomb out...
                        nextConsumer = null;
                        _runningAsynchConsumer = false;
                    }
                    else
                    {
                        if (firstTimeRound)
                        {
                            firstTimeRound = false;

                            //process any messages which are already attached to this LCP
                            boolean msgDelivered = processAttachedMsgs();

                            //Process any msgs that built up while we were busy
                            //but not if this was an isolatedRun and we already delivered a message
                            if (!(msgDelivered && isolatedRun))
                            {
                                nextConsumer = processQueuedMsgs(isolatedRun);
                            }
                        }

                        if (!isolatedRun && (nextConsumer != null))
                        {
                            // Every time we transfer a message to a different member of the keyGroup
                            // (the last message matched someone else's selector) we have to re-run
                            // processQueuedmsgs() to make sure nothing is left on the queue after
                            // processing
                            nextConsumer = nextConsumer.processQueuedMsgs(isolatedRun);
                        }

                        // Indicate that this consumer is about to go round again 
                        if (nextConsumer != null)
                            _runningAsynchConsumer = true;
                        else
                            _runningAsynchConsumer = false;
                    }

                    // If we're on our way out and we were interupted by another thread then we need
                    // to wake them up on our way out as they may be waiting for us to finish.
                    if (!_runningAsynchConsumer && _runAsynchConsumerInterupted)
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "We've been told to stop, wake up whoever asked us");
                        _runAsynchConsumerInterupted = false;
                        _asynchConsumerBusyLock.notifyAll();
                    }
                }
                else
                    nextConsumer = null;
            } // synchronized
        } while (nextConsumer != null);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "runAsynchConsumer");
    }

    /**
     * Closes the LocalConsumerPoint.
     * 
     * @throws
     */
    @Override
    public void close() throws SIResourceException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "close", this);

        // On closing, we need to run the async consumer a final time to process
        // any messages it has attached.
        //
        // We need to take the async consumer lock so we don't step on a currently
        // running consumer (so that running asyncs can complete with all their
        // resources available - i.e. without object closed exceptions - as per the
        // JMS 1.1 spec).
        //
        // Taking this lock also prevents other invocations of close which take
        // place before this invocation has completed from proceeding (as per the
        // JMS 1.1 spec).
        //
        // We also taken the consumer session lock for most of the method.  In
        // theory, we could release this lock while running the async consumer,
        // but this makes things a little more complicated and there seems little
        // point when we're closing down anyway.
        //
        // The async consumer could concievably spin forever and prevent the close.
        // This is an accepted fact.

        // We need to get any asynch thread to stop looping round the queue to
        // allow us to have a chance of closing this consumer. We have to do this outside
        // of the asynch lock as that will be held by the other thread at this time.
        _interruptConsumer = true;

        synchronized (_asynchConsumerBusyLock)
        {
            if (_closed || _closing)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "close", "Already Closed");
                return;
            }

            _lockRequested = true;

            // Lock the KeyGroup if we have one before we lock this consumer
            if (_keyGroup != null)
                _keyGroup.lock();

            try
            {
                this.lock();
                try
                {
                    _lockRequested = false;

                    _consumerKey.stop();

                    // We don't want new messages to be attached if we're closing.
                    unsetReady();

                    // We need to release the LCP lock while we process attached messages
                    // but first we mark the consumer as closing to stop other calls succeeding
                    _closing = true;
                } finally
                {
                    this.unlock();
                }
            } finally
            {
                if (_keyGroup != null)
                    _keyGroup.unlock();
            }

            // Process any attached messages already on the async consumer.
            if (_asynchConsumerRegistered)
            {
                // If we reach here and an async consumer is running, close() must
                // have been called from the async consumer callback as we have been
                // able to acquire the asynchConsumer lock.
                //
                // Therefore, do not erroneously recursively invoke the callback.
                if (!_asynchConsumer.isAsynchConsumerRunning())
                {
                    try
                    {
                        processAttachedMsgs();
                    } catch (SISessionDroppedException e)
                    {
                        //No FFDC code needed
                        //essentially the session is already closed but we'll try to carry
                        //cleaning up
                    }
                }

                // If we are a member of an ordering group then we need to leave it
                // before we close
                if (_keyGroup != null)
                {
                    _consumerKey.leaveKeyGroup();
                    _keyGroup = null;
                }
            }

            //Now take the lock back and unlock any locked messages and kick out
            // any waiting receiver
            this.lock();
            try
            {
                synchronized (_allLockedMessages)
                {
                    try
                    {
                        // Unlock any remaining locked messages
                        _allLockedMessages.unlockAll();
                    } catch (SIMPMessageNotLockedException e)
                    {
                        // No FFDC code needed
                        // This exception has occurred beause someone has deleted the
                        // message(s). Ignore this exception as it is unlocked anyway
                    } catch (SISessionDroppedException e)
                    {
                        //No FFDC code needed
                        //essentially the session is already closed but we'll try to carry
                        //cleaning up
                    }

                    // Mark this consumer session (point) as closed
                    _closed = true;
                }

                // If we have a synchronous receive we need to wake it up
                if (_waiting)
                {
                    _waiter.signal();
                }
            } // synchronized(this)
            finally
            {
                this.unlock();
            }

            _interruptConsumer = false;

            // If we've managed to interupt another thread that's currently checking for messages
            // to give to a consumer (runAsynchConsumer) then we need to wait until they realise
            // that we've interupted them before we return. Otherwise we'll have a dangling thread
            // that's technically 'working', even though the consumer is closed
            if (_runningAsynchConsumer && !_asynchConsumer.isAsynchConsumerRunning())
            {
                _runAsynchConsumerInterupted = true;
                try
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Waiting for runAsynchConsumer to complete");

                    _asynchConsumerBusyLock.wait();
                } catch (InterruptedException e)
                {
                    // No FFDC code needed
                }
            }
        } // synchronized(asynch)

        // Now we no longer hold a lock, unlock any messages we happen
        // to have hidden for this consumer (SIB0115)
        unlockAllHiddenMessages();

        //detach from the CD - we CANNOT hold any consumer locks at this point!
        _consumerKey.detach();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "close");
    }

    /**
     * Returns true if this LCP is closed.
     * 
     * @return
     */
    public boolean isClosed()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isClosed", this);
            SibTr.exit(tc, "isClosed", Boolean.valueOf(_closed));
        }
        return _closed;
    }

    /**
     * Returns true if this LCP is suspended.
     * If we're not already suspended and we find our ConsumerSet is
     * suspended we suspend ourselves - this is because the ConsumerSet
     * doesn't tell all its members about a suspension, it lets them find
     * out in a lazy way (i.e. here)
     * 
     * NOTE: Callers to this will have LCP locked
     * 
     * @return
     */
    private boolean isSuspended()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isSuspended", this);

        boolean suspended = true;

        // First we check this consumer's suspend status
        if (!_consumerSuspended)
        {
            // If this consumer is not currently suspended we check any ConsumerSet
            // it's a member of. If that is currently suspended then we suspend this
            // consumer so that we know to resume it when the set gets resumed and
            // therefore kickstart this consumer.
            if (_consumerKey.isConsumerSetSuspended())
                suspendConsumer(DispatchableConsumerPoint.SUSPEND_FLAG_SET_ACTIVE_MSGS);
            else
                suspended = false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isSuspended", Boolean.valueOf(suspended));

        return suspended;
    }

    /**
     * This class implements Runnable so that we can easily call it in a new Thread.
     * It's sole purpose is to call the AsynchConsumerCallback code inside a separate
     * Thread.
     * 
     * @author tevans
     */
    private class AsynchThread implements Runnable
    {
        //true if this thread run the asynch callback in an isolated way
        private final boolean _isolatedRun;

        //a reference to this LCP
        private final JSLocalConsumerPoint _localConsumerPoint;

        /**
         * Create a new AsynchThread for the given LCP
         * 
         * @param lcp The 'owning' LCP
         * @param isolatedRun true if this should be an isolated run :-o
         */
        AsynchThread(JSLocalConsumerPoint lcp, boolean isolatedRun)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(
                            tc,
                            "AsynchThread",
                            new Object[] { lcp, Boolean.valueOf(isolatedRun) });

            //store the variables
            _localConsumerPoint = lcp;
            _isolatedRun = isolatedRun;

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "AsynchThread", this);
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "run", this);

            try
            {
                //Begin looking for messages to deliver via the callback
                _localConsumerPoint.runAsynchConsumer(_isolatedRun);
            } catch (Throwable e)
            {
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.AsynchThread.run",
                                            "1:3644:1.22.5.1",
                                            this);

                SibTr.exception(tc, e);

                notifyException(e);

                try
                {
                    // Since the asynchConsumer has experienced an error of some kind, the best form
                    // of cleanup is to close down the session. This ensures any listeners get notified
                    // and can retry at some point later.
                    closeSession(e);
                } catch (Exception ee)
                {
                    // No FFDC code needed
                    SibTr.exception(tc, ee);
                }

                if (e instanceof ThreadDeath)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "run", e);
                    throw (ThreadDeath) e;
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "run", e);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "run");
        }
    }

    /**
     * Start this LCP. If there are any synchronous receives waiting, wake them up
     * If there is a AsynchConsumerCallback registered look on the QP for messages
     * for asynch delivery. If deliverImmediately is set, this Thread is used to deliver
     * any initial messages rather than starting up a new Thread.
     */
    @Override
    public void start(boolean deliverImmediately) throws SISessionUnavailableException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "start", new Object[] {
                                                   Boolean.valueOf(deliverImmediately), this });

        // Register that LCP has been stopped by a request to stop
        _stoppedByRequest = false;

        // Run the private method
        internalStart(deliverImmediately);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "start");
    }

    private void internalStart(boolean deliverImmediately)
                    throws SISessionUnavailableException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "internalStart", new Object[] {
                                                           Boolean.valueOf(deliverImmediately), this });

        boolean spinOffANewThread = false;

        // Lock any asynch consumers
        synchronized (_asynchConsumerBusyLock)
        {
            // Clear any interrupt as we're re-starting the consumer
            // (there is a small window where a close/stop comes in just before
            // this point but after the start has the lock. Thois will meen an
            // interrupt is missed - not a big deal considering it only meens
            // a yield won't be performed)
            _interruptConsumer = false;

            // If we are a member of a key group we must lock the group before making it
            // not ready and before we hold the LCP lock
            if (_keyGroup != null)
                _keyGroup.lock();

            try
            {
                // Lock the consumer session while we check that it is in a valid
                // state
                this.lock();
                try
                {
                    // Only valid if the consumer session is still open
                    checkNotClosed();

                    // Remember that we've been started - incase anyone asks (SIB0115)
                    _startSinceSequentialStop = true;

                    if (_stoppedForReceiveAllowed == false)
                    {
                        _stopped = false;

                        // Let the key (or it's group) know we're started
                        _consumerKey.start();

                        //wake up any waiting synch receivers
                        if (_waitingToStart)
                        {
                            _waiter.signal();
                        }
                    }
                } finally
                {
                    this.unlock();
                }
            } finally
            {
                if (_keyGroup != null)
                    _keyGroup.unlock();
            }

            //if there is a callback registered
            if (_asynchConsumerRegistered)
            {
                //If we are calling stop from inside a consumeMessages callback then we'll
                // automatically look for more messages anyway once consumeMessages returns.
                if (!_asynchConsumer.isAsynchConsumerRunning())
                {
                    //169892
                    //if deliverImmediately is set
                    if (deliverImmediately)
                    { //Begin processing messages on the QP
                        try
                        {
                            runAsynchConsumer(false);
                        } catch (Throwable e)
                        {
                            FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.internalStart",
                                                        "1:3784:1.22.5.1",
                                                        this);

                            SibTr.exception(tc, e);

                            try
                            {
                                // Since the asynchConsumer has experienced an error of some kind, the best form
                                // of cleanup is to close down the session. This ensures any listeners get notified
                                // and can retry at some point later.
                                _consumerSession.close();
                                // don't notify asynchconsumer as we are been called inline
                            } catch (Exception ee)
                            {
                                // No FFDC code needed
                                SibTr.exception(tc, ee);
                            }

                            if (e instanceof ThreadDeath)
                            {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                    SibTr.exit(tc, "internalStart", e);
                                throw (ThreadDeath) e;
                            }

                            SISessionDroppedException sessionDroppedException = new SISessionDroppedException(
                                            nls.getFormattedMessage("CONSUMER_CLOSED_ERROR_CWSIP0177"
                                                                    , new Object[] { _consumerDispatcher.getDestination().getName(),
                                                                                    _messageProcessor.getMessagingEngineName() }
                                                                    , null), e);

                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(tc, "internalStart", sessionDroppedException);

                            //inline call so throwing exception rather than notifying
                            throw sessionDroppedException;
                        }
                    }
                    else
                        spinOffANewThread = true;

                }
            }
        } // synchronized

        //169892
        //if deliverImmediately was not set then start up a new Thread to process
        //any messages which may already be on the QP.
        if (spinOffANewThread)
        {
            try
            {
                //start a new thread to run the callback
                _messageProcessor.startNewThread(new AsynchThread(this, false));
            } catch (InterruptedException e)
            {
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.internalStart",
                                            "1:3845:1.22.5.1",
                                            this);

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "internalStart", e);

            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "internalStart");
    }

    /**
     * Spin off a thread that checks for any stored messages.
     * This is called by a consumerKeyGroup to try to kick a group back
     * into life after a stopped member detaches and makes the group ready
     * again.
     */
    @Override
    public void checkForMessages()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkForMessages", this);

        try
        {
            //start a new thread to run the callback
            _messageProcessor.startNewThread(new AsynchThread(this, false));
        } catch (InterruptedException e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.checkForMessages",
                                        "1:3881:1.22.5.1",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkForMessages", e);

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkForMessages");
    }

    @Override
    public void stop() throws SISessionUnavailableException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "stop", this);

        // Register that LCP has been stopped by a request to stop
        _stoppedByRequest = true;

        // Run the private method
        internalStop();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "stop");
    }

    private void internalStop() throws SISessionUnavailableException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "internalStop", this);

        // We need to get any asynch thread to stop looping round the queue to
        // allow us to have a chance to stop this consumer. We have to do this outside
        // of the asynch lock as that will be held by the other thread at this time.
        _interruptConsumer = true;

        synchronized (_asynchConsumerBusyLock)
        {
            _lockRequested = true;

            // If we are a member of a key group we must lock the group before making it
            // not ready and before we hold the LCP lock
            if (_keyGroup != null)
                _keyGroup.lock();

            try
            {
                // Lock the consumer session while we check that it is in a valid
                // state
                this.lock();
                try
                {
                    _lockRequested = false;
                    // Only valid if the consumer session is still open
                    checkNotClosed();

                    _stopped = true;

                    // Let the key (or it's group) know we're stopped
                    _consumerKey.stop();

                    unsetReady();

                    // If there is a blocking receiver we need to remember to re-start
                    // them if the session is re-started
                    if (_waiting)
                        _waitingToStart = true;
                } finally
                {
                    this.unlock();
                }
            } finally
            {
                if (_keyGroup != null)
                    _keyGroup.unlock();
            }

            if (_asynchConsumerRegistered)
            {
                //If we are calling stop from inside a callback there's nothing to do
                if (!_asynchConsumer.isAsynchConsumerRunning())
                {
                    // Process any assigned msgs on this thread before exiting stop
                    processAttachedMsgs();
                }
            }

            _interruptConsumer = false;

            // If we've managed to interupt another thread that's currently checking for messages
            // to give to a consumer (runAsynchConsumer) then we need to wait until they realise
            // that we've interupted them before we return. Otherwise we'll have a dangling thread
            // that's technically 'working', even though the consumer is stopped
            if (_runningAsynchConsumer && !_asynchConsumer.isAsynchConsumerRunning())
            {
                _runAsynchConsumerInterupted = true;
                try
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Waiting for runAsynchConsumer to complete");

                    _asynchConsumerBusyLock.wait();
                } catch (InterruptedException e)
                {
                    // No FFDC code needed
                }
            }
        } // synchronized

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "internalStop");
    }

    /**
     * Unlock all messages which have been locked to this LCP but not consumed
     * 
     * @throws SIResourceException Thrown if there is a problem in the msgStore
     */
    @Override
    public void unlockAll() throws SISessionUnavailableException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "unlockAll", this);

        synchronized (_asynchConsumerBusyLock)
        {
            this.lock();
            try
            {
                // Only valid if the consumer session is still open
                checkNotClosed();

                try
                {
                    // Unlock the messages (take this lock to ensure we don't have a problem
                    // getting it if we need it to re-deliver the unlocked messages to ourselves)
                    _allLockedMessages.unlockAll();
                } catch (SIMPMessageNotLockedException e)
                {
                    // No FFDC code needed
                    // This exception has occurred beause someone has deleted the
                    // message(s). Ignore this exception as it is unlocked anyway
                }
            } finally
            {
                this.unlock();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "unlockAll");
    }

    /**
     * Set the consumerSession's recoverability
     */
    private void setBaseRecoverability(Reliability unrecoverableReliability)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setBaseRecoverability", new Object[] { this, unrecoverableReliability });

        setUnrecoverability(unrecoverableReliability);
        _baseUnrecoverableOptions = _unrecoverableOptions;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setBaseRecoverability");
    }

    /**
     * Restore the original unrecoverability of the session
     */
    private void resetBaseUnrecoverability()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "resetBaseUnrecoverability", this);

        _unrecoverableOptions = _baseUnrecoverableOptions;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "resetBaseUnrecoverability");
    }

    /**
     * @param unrecoverableReliability
     */
    private void setUnrecoverability(Reliability unrecoverableReliability)
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setUnrecoverability", new Object[] { this, unrecoverableReliability });

        // Setup the recoverability options
        // These can be different for each receive() call
        // or for each AsyncConsumer
        _unrecoverableOptions = unrecoverableReliability;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setUnrecoverability");
    }

    /**
     * Change the Ready state to true
     */
    private void setReady() throws SINotPossibleInCurrentConfigurationException
    {
        // If we're not transacted no messages are recoverable
        Reliability unrecoverable = Reliability.ASSURED_PERSISTENT;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setReady", this);

        // If the consumer is transacted inform the CD what
        // recoverability is required.
        if (_transacted)
            unrecoverable = _unrecoverableOptions;

        //set the ready state
        _ready = true;

        if (_keyGroup != null)
            _keyGroup.groupReady();

        //and inform the consumerDispatcher that we are ready
        _consumerKey.ready(unrecoverable);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setReady");
    }

    /**
     * Change the Ready state to false
     */
    protected void unsetReady()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "unsetReady", this);

        //set the ready state
        _ready = false;

        if (_keyGroup != null)
            _keyGroup.groupNotReady();

        // if consumerKey is null then it can't be already in ready state!
        if (_consumerKey != null)
        {
            //tell the consumerDispatcher that we are no longer ready
            _consumerKey.notReady();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "unsetReady");
    }

    /**
     * Unlock/delete/read a set of locked messages based on their short ID (which
     * is unique to this LCP). Warning: The array must have at least one entry!
     * 
     * @param msgIds
     */
    @Override
    public SIBusMessage[] processMsgSet(SIMessageHandle[] msgHandles,
                                        TransactionCommon transaction,
                                        BifurcatedConsumerSessionImpl owner,
                                        boolean unlock,
                                        boolean delete,
                                        boolean read,
                                        boolean incrementLockCount)

                    throws SISessionUnavailableException, SIMPMessageNotLockedException, SIConnectionLostException, SIIncorrectCallException, SIResourceException, SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "processMsgSet",
                        new Object[] { this,
                                      SIMPUtils.messageHandleArrayToString(msgHandles),
                                      transaction,
                                      owner,
                                      Boolean.valueOf(unlock),
                                      Boolean.valueOf(delete),
                                      Boolean.valueOf(read) });

        // Do a dirty read of the (volatile) closed/closing states to avoid the synch block if poss
        if (_closed || _closing)
        {
            this.lock();
            try
            {
                checkNotClosed();
            } finally
            {
                this.unlock();
            }
        }

        int numMsgs = 0;
        SIBusMessage messages[] = null;

        if (msgHandles != null && msgHandles.length > 0)
            numMsgs = msgHandles.length;

        if (numMsgs > 0)
        {
            // Actually process the message Ids
            messages = _allLockedMessages.processMsgSet(msgHandles, transaction, owner, unlock, delete, read, incrementLockCount);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processMsgSet", messages);

        return messages;
    }

    /**
   *
   */
    @Override
    public ConsumerSessionImpl getConsumerSession()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getConsumerSession");
            SibTr.exit(tc, "getConsumerSession", _consumerSession);
        }

        return _consumerSession;
    }

    /**
   *
   */
    @Override
    public JSConsumerManager getConsumerManager()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getConsumerManager");
            SibTr.exit(tc, "getConsumerManager", _consumerDispatcher);
        }

        return _consumerDispatcher;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.ConsumerPoint#notifyException(com.ibm.ws.sib.processor.SIMPException)
     */
    @Override
    public void notifyException(Throwable exception)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "notifyException", new Object[] { this, exception });

        // If an asynch consumer is registered we need to let them know something went wrong
        if (_asynchConsumerRegistered)
            _asynchConsumer.notifyExceptionListeners(exception, _consumerSession);

        this.lock();
        try
        {
            if (_waiting)
            {
                // We need to get the exception back to the synchronous consumer
                _notifiedException = exception;
                _waiter.signal();
            }
        } finally
        {
            this.unlock();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "notifyException");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ConsumerPoint#notifyReceiveAllowed(boolean)
     */
    @Override
    public void notifyReceiveAllowed(boolean isAllowed)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "notifyReceiveAllowed", new Object[] { Boolean.valueOf(isAllowed), this });

        this.lock();
        try
        {
            try
            {
                checkReceiveAllowed();
            } catch (SISessionUnavailableException e)
            {
                // FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.notifyReceiveAllowed",
                                            "1:4287:1.22.5.1",
                                            this);

                SibTr.exception(tc, e);
            } catch (SIResourceException e)
            {
                // FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.notifyReceiveAllowed",
                                            "1:4298:1.22.5.1",
                                            this);

                SibTr.exception(tc, e);
            }
        } finally
        {
            this.unlock();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "notifyReceiveAllowed");
    }

    /**
     * Check that this consumer point is not closed.
     * <p>
     * Callers must be synchronized on either 'this' ( the local consumer point),
     * 'asynchConsumer' or 'allLockedMessages'.
     * 
     * @throws SIObjectClosedException
     */
    @Override
    public void checkNotClosed() throws SISessionUnavailableException
    {
        if ((_closed) || (_closing && !(_asynchConsumer.isAsynchConsumerRunning())))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "checkNotClosed");

            //check the close conditions to produce a well-formed error message
            if (_consumerKey.isClosedDueToDelete())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "checkNotClosed", "SISessionDroppedException - deleted");
                throw new SISessionDroppedException(
                                nls.getFormattedMessage(
                                                        "DESTINATION_DELETED_ERROR_CWSIP00221",
                                                        new Object[] {
                                                                      _consumerDispatcher.getDestination().getName(),
                                                                      _consumerDispatcher.getMessageProcessor().getMessagingEngineName() },
                                                        null));
            }
            if (_consumerKey.isClosedDueToReceiveExclusive())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "checkNotClosed", "SISessionDroppedException - receive Exclusive");
                throw new SISessionDroppedException(
                                nls.getFormattedMessage(
                                                        "DESTINATION_EXCLUSIVE_ERROR_CWSIP00222",
                                                        new Object[] {
                                                                      _consumerDispatcher.getDestination().getName(),
                                                                      _consumerDispatcher.getMessageProcessor().getMessagingEngineName() },
                                                        null));
            }
            if (_consumerKey.isClosedDueToLocalizationUnreachable())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "checkNotClosed", "SISessionDroppedException - localisation unreachable");
                throw new SISessionDroppedException(
                                nls.getFormattedMessage(
                                                        "DESTINATION_UNREACHABLE_ERROR_CWSIP00223",
                                                        new Object[] {
                                                                      _consumerDispatcher.getDestination().getName(),
                                                                      _consumerDispatcher.getMessageProcessor().getMessagingEngineName() },
                                                        null));
            }

            SISessionUnavailableException e =
                            new SISessionUnavailableException(
                                            nls.getFormattedMessage(
                                                                    "CONSUMER_CLOSED_ERROR_CWSIP0177",
                                                                    new Object[] { _consumerDispatcher.getDestination().getName(),
                                                                                  _messageProcessor.getMessagingEngineName() },
                                                                    null));

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkNotClosed", "consumer closed");

            throw e;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ConsumerPoint#destinationMatches()
     */
    @Override
    public boolean destinationMatches(DestinationHandler destinationHandlerToCompare,
                                      JSConsumerManager consumerDispatcher)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "destinationMatches", new Object[] { this, destinationHandlerToCompare, consumerDispatcher });

        boolean matches = (_destinationAttachedTo == destinationHandlerToCompare);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "destinationMatches", Boolean.valueOf(matches));

        return matches;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ConsumerPoint#getNamedDestination(com.ibm.ws.sib.processor.impl.ConsumerDispatcher)
     */
    @Override
    public DestinationHandler getNamedDestination(ConsumerDispatcher cd)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getNamedDestination", cd);
            SibTr.exit(tc, "getNamedDestination", _destinationAttachedTo);
        }
        return _destinationAttachedTo;
    }

    /**
     * An autocommit transaction is not threadsafe, therefore any users must
     * either prevent concurrent use or use separate transactions. All
     * references in JSLocalConsumerPoint to _autoCommitTransaction are threadsafe
     * so the cached transaction is ok. However, callers to this method are not,
     * so a new transaction is returned each time.
     * 
     * @return tran A new autocommit transaction
     */
    protected TransactionCommon getAutoCommitTransaction()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getAutoCommitTransaction", this);
        }
        TransactionCommon tran =
                        _messageProcessor.getTXManager().createAutoCommitTransaction();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.exit(tc, "getAutoCommitTransaction", tran);
        }
        return tran;
    }

    /**
     * @return
     */
    @Override
    public SIBusMessage relockMessageUnderAsynchCursor() throws SISessionUnavailableException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "relockMessageUnderAsynchCursor");

        checkNotClosed();

        SIBusMessage msg = _allLockedMessages.relockSavedMsg();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "relockMessageUnderAsynchCursor", msg);

        return msg;
    }

    /**
     * Prepare to add an active message to the counter(s) if there is space.
     * 
     * False is returned when there is no room for this message.
     * 
     * If we're counting active messages on this consumer (maxActiveMessages > 0)
     * then we don't need to do any prepare-time work, instead we just check that
     * we have space, i.e. we're not already suspended.
     * 
     * If we're in a ConsumerSet we must 'reserve' our active message now, before
     * we actually have it in our hands, to make sure multiple members of the set
     * don't concurrently add lots of messages, thus exceeding the concurrency limit
     * on the set.
     * 
     * This prepare is either committed in the case where the caller goes on to
     * successfully lock a message, or rolledback where they fail (e.g. no message
     * is available)
     * 
     * NOTE: Callers to this method will have the JSLocalConsumerPoint locked
     */
    private boolean prepareAddActiveMessage()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "prepareAddActiveMessage");

        boolean messageAccepted = true;

        // We can't even accept the next message if the consumer is already
        // suspended
        if (_consumerSuspended)
            messageAccepted = false;
        else
        {
            // If we're a member of a ConsumerSet then add this message to the
            // set's quota - if there's space (returning 'false' indicates that
            // the set is already full without this message)
            if (!_consumerKey.prepareAddActiveMessage())
            {
                messageAccepted = false;
                suspendConsumer(DispatchableConsumerPoint.SUSPEND_FLAG_SET_ACTIVE_MSGS);
            }
        }

        // We can defer any local maxActiveMessage changes until the commit as there's
        // no chance of another thread getting for this consumer getting in before then

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc,
                       "prepareAddActiveMessage",
                       new Object[] { Boolean.valueOf(messageAccepted) });

        return messageAccepted;
    }

    /*
     * If the caller prepared the add of an active message they must come back
     * with either a commit or rollback of that prepare.
     * 
     * This method commits the add of the active message, i.e a message was actually
     * locked to the consumer
     * 
     * NOTE: Callers to this method will still have the JSLocalConsumerPoint locked
     * since the prepare
     */
    private void commitAddActiveMessage()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "commitAddActiveMessage");

        // If this consumer is in a set then commit the add to the set
        _consumerKey.commitAddActiveMessage();

        // Check the _maxActiveMessage counter for this specific consumer.
        // We're only interested if we're counting messages and we can only do that if
        // we're bifurcatable
        if (_bifurcatable && (_maxActiveMessages != 0))
        {
            // Lock down the active message counter (removing an active message will
            // only hold this lock, not the 'this' lock)
            synchronized (_maxActiveMessageLock)
            {
                // Add the message to the local consumer count
                _currentActiveMessages++;

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "_currentActiveMessages: " + _currentActiveMessages);

                // If we've reached the maximum, suspend the consumer
                if (_currentActiveMessages == _maxActiveMessages)
                {
                    // Suspend the consumer for the next time round
                    suspendConsumer(DispatchableConsumerPoint.SUSPEND_FLAG_ACTIVE_MSGS);

                    // (F001731)
                    // If we're monitoring the maxActiveMessage limit then start an alarm that
                    // will ping when the allowable suspended time passes
                    if (_activeMsgBlockInterval > -1)
                    {
                        if (_activeMsgBlockAlarm == null)
                            _activeMsgBlockAlarm = new ActiveMsgBlockAlarm();

                        _activeMsgBlockAlarm.startAlarm();
                    }
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "commitAddActiveMessage");
    }

    /*
     * If the caller prepared the add of an active message they must come back
     * with either a commit or rollback of that prepare.
     * 
     * This method rolls back the add of the active message, i.e a message wasn't
     * locked to the consumer
     * 
     * NOTE: Callers to this method will still have the JSLocalConsumerPoint locked
     * since the prepare
     */
    private void rollbackAddActiveMessage()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "rollbackAddActiveMessage");

        // If this consumer is in a set then commit the add to the set
        _consumerKey.rollbackAddActiveMessage();

        // There's nothing to do locally as we didn't do anything on the prepare, so
        // there's no change here.

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "rollbackAddActiveMessage");
    }

    /**
     * Decrement the active message count(s)
     * 
     * NOTE: This thread MUST NOT HOLD THE LCP LOCK as we may try to take the
     * ConsumerSet.consumerList lock inside ConsumerSet.removeActiveMessages(),
     * this lock comes before the LCP lock in the hierarchy
     * 
     * @param messages
     */
    @Override
    public void removeActiveMessages(int messages)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeActiveMessages", new Object[] { Integer.valueOf(messages),
                                                                        Integer.valueOf(_currentActiveMessages),
                                                                        Integer.valueOf(_maxActiveMessages),
                                                                        Boolean.valueOf(_consumerSuspended),
                                                                        Boolean.valueOf(_bifurcatable) });

        // First we decrement the message count for any ConsumerSet we happen to be
        // a part of. If this happens to take us below the max for the set then we
        // will resume any consumers in the set (where appropriate). We then
        // handle any resuming of this consumer in this method (as it has to take
        // account of any maxActiveMessage setting for this consumer.
        _consumerKey.removeActiveMessages(messages);

        // Need to check maxActiveMessages for this consumer
        if (_bifurcatable && (_maxActiveMessages != 0))
        {
            boolean threasholdCrossed = false;

            // Lock the active message counter
            synchronized (_maxActiveMessageLock)
            {
                _currentActiveMessages -= messages;

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "_currentActiveMessages: " + _currentActiveMessages);

                // See if we've gone below the active limit we therefore need to
                // resume the consumer (resumeConsumer() checks that the set is also
                // resumed)
                if ((_currentActiveMessages == (_maxActiveMessages - messages)))
                {
                    // Remember to resume the consumer once we've released the maxActiveMessage
                    // lock
                    threasholdCrossed = true;
                }

                // We should never go negative, if we do something has gone wrong - FFDC
                if (_currentActiveMessages < 0)
                {
                    SIErrorException e = new SIErrorException(
                                    nls.getFormattedMessage(
                                                            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                                            new Object[] {
                                                                          "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.removeActiveMessages",
                                                                          "1:4657:1.22.5.1",
                                                                          Integer.valueOf(_currentActiveMessages) },
                                                            null));

                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.removeActiveMessages",
                                                "1:4664:1.22.5.1",
                                                this);

                    SibTr.exception(tc, e);
                    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                new Object[] {
                                              "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.removeActiveMessages",
                                              "1:4671:1.22.5.1" });
                }
            } // synchronized

            // The threashold was crossed, first take the LCP lock then the maxActiveMessage
            // lock again and re-check the current state before resuming the consumer.
            if (threasholdCrossed)
            {
                this.lock();
                try
                {
                    synchronized (_maxActiveMessageLock)
                    {
                        // Re-check to make sure someone else hasn't got in between the
                        // above release of the maxActiveMessage lock and this point and
                        // added more active messages - suspending us again.
                        // The count could be less than the threashold because someone else
                        // also released a message - in this case we still need to be the
                        // one to resume the consumer as the other release wouldn't have crossed
                        // the threashold and therefore not be in this bit of code.
                        if (_currentActiveMessages < _maxActiveMessages)
                        {
                            resumeConsumer(SUSPEND_FLAG_ACTIVE_MSGS);

                            // (F001731)
                            // If we have an alarm registered to warn of long pauses then cancel it now
                            // (this will issue an all-clear message is the pause has already been reported)
                            if (_activeMsgBlockAlarm != null)
                                _activeMsgBlockAlarm.cancelAlarm();
                        }
                    }
                } finally
                {
                    this.unlock();
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeActiveMessages");
    }

    /**
     * @return true if we are counting the max active messages
     */
    @Override
    public boolean isCountingActiveMessages()
    {
        boolean isCounting = false;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "isCountingActiveMessages");

        // We're either explicitly counting messages for this consumer or we're
        // a member of a set, in which case we are probably counting messages, if
        // not now, sometime in the future, so we need to always keep a count
        if ((_bifurcatable && (_maxActiveMessages != 0)) ||
            (_consumerKey.getConsumerSet() != null))
            isCounting = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "isCountingActiveMessages", Boolean.valueOf(isCounting));

        return isCounting;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.LocalConsumerPoint#getConsumerSetLabel()
     */
    @Override
    public String getConsumerSetLabel()
    {
        String consumerSetLabel = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getConsumerSetLabel");
        }

        consumerSetLabel = _consumerKey.getConsumerSet().getLabel();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.exit(tc, "getConsumerSetLabel", consumerSetLabel);
        }

        return consumerSetLabel;
    }

    /**
     * Gets the max active message count,
     * Currently only used by the unit tests to be sure that the max active count has
     * been updated
     * 
     * @return
     */
    public int getMaxActiveMessages()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getMaxActiveMessages");
            SibTr.exit(tc, "getMaxActiveMessages", Integer.valueOf(_maxActiveMessages));
        }
        return _maxActiveMessages;
    }

    /**
     * Update the max active messages field
     * 
     * Set by the MessagePump class to indicate that there has been an update
     * in the Threadpool class.
     * 
     * WARNING: if the max is ever set to zero (now or on registration) then counting
     * is disabled (for performance), so if it ever gets reset to a non-zero
     * value we will not have accounted for any currently active messages so
     * the count will be out by a certain offset, which could cause it to go
     * negative.
     * 
     * @param maxActiveMessages
     */
    @Override
    public void setMaxActiveMessages(int maxActiveMessages)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setMaxActiveMessages", Integer.valueOf(maxActiveMessages));

        synchronized (_asynchConsumerBusyLock)
        {
            this.lock();
            try
            {
                synchronized (_maxActiveMessageLock)
                {
                    // If the new value is > previous value and
                    // check if consumer is suspended and resume consumer.
                    // But only do this if the message count is less than the curent active count
                    if (maxActiveMessages > _maxActiveMessages && maxActiveMessages < _currentActiveMessages)
                    {
                        // If the consumer was suspended - reenable it
                        if (_consumerSuspended)
                        {
                            // If this is part of an asynch callback we don't need to try and kick
                            // off a new thread as the nextLocked will return us another message on
                            // exit of the consumeMessages() method.
                            if (_runningAsynchConsumer)
                            {
                                _consumerSuspended = false;
                                _suspendFlags &= ~DispatchableConsumerPoint.SUSPEND_FLAG_ACTIVE_MSGS;
                            }
                            // Otherwise perform a full resume
                            else
                            {
                                resumeConsumer(DispatchableConsumerPoint.SUSPEND_FLAG_ACTIVE_MSGS);
                            }
                        }
                    }
                    else if (maxActiveMessages <= _currentActiveMessages && !_consumerSuspended)
                    {
                        // If the maxActiveMessages has been set to something lower than the current active message
                        // count, then suspend the consumer until the messages have been processed
                        _consumerSuspended = true;
                        _suspendFlags |= DispatchableConsumerPoint.SUSPEND_FLAG_ACTIVE_MSGS;
                    }

                    _maxActiveMessages = maxActiveMessages;
                } // active message lock
            } // this lock
            finally
            {
                this.unlock();
            }
        } // async lock

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setMaxActiveMessages");
    }

    /**
     * @param consumer
     * @throws SISessionDroppedException
     */
    @Override
    public void cleanupBifurcatedConsumer(BifurcatedConsumerSessionImpl consumer) throws SIResourceException, SISessionDroppedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "cleanupBifurcatedConsumer", consumer);

        _allLockedMessages.cleanOutBifurcatedMessages(consumer, _consumerSession.getBifurcatedConsumerCloseRedeliveryMode());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "cleanupBifurcatedConsumer");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.transactions.TransactionCallback#beforeCompletion(com.ibm.ws.sib.msgstore.transactions.Transaction)
     */
    @Override
    public void beforeCompletion(TransactionCommon arg0)
    {
        // Nothing to do
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.transactions.TransactionCallback#afterCompletion(com.ibm.ws.sib.msgstore.transactions.Transaction, boolean)
     */
    @Override
    public void afterCompletion(TransactionCommon tran, boolean committed)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "afterCompletion", new Object[] { tran, Boolean.valueOf(committed) });

        // We don't care if the transaction committed or rolled back, either way
        // this callback indicates that a message that this consumer had active
        // is no longer active and therefore we should decrement the active message
        // count (this callback is called once per active message).
        removeActiveMessages(1);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "afterCompletion");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ConsumerPoint#resumeConsumer()
     */
    @Override
    public void resumeConsumer(int suspendFlag)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "resumeConsumer", this);

        // Use this flag to identify whether a new thread must be spun off to look for more
        // messages
        boolean spinOffNewThread = false;
        try
        {
            // Take the lock
            this.lock();

            if (_consumerSuspended)
            {
                boolean setStillSuspended = false;

                // If we're resuming due to the ConsumerSet being resumed then we need to
                // re-check the set's state due to us not holding the set's activeMessageCount
                // lock at this time (due to the hierarchy). And if we find that the set is
                // actually suspended (due to someone else in the set getting in and adding
                // another message then we just ignore this resume as there'll be another one
                // along when the count drops back below the limit again.
                if (suspendFlag == DispatchableConsumerPoint.SUSPEND_FLAG_SET_ACTIVE_MSGS)
                    setStillSuspended = _consumerKey.isConsumerSetSuspended();

                if (!setStillSuspended)
                {
                    // clear the bit provided in the _suspendFlags
                    _suspendFlags &= ~suspendFlag;

                    if (_suspendFlags == 0) // No flags set so resume the consumer
                    {
                        _consumerSuspended = false;

                        // If the consumer is still active (started) we need
                        // to kickstart the consumer back into life to check for more
                        // messages
                        if (!_stopped && !_closing)
                        {
                            if (_asynchConsumerRegistered)
                            {
                                if (!_asynchConsumer.isAsynchConsumerRunning())
                                {
                                    // We'll have to take the full lock hierarchy and
                                    // possibly spin off another thread to start looking for more messages
                                    // (just like start())
                                    spinOffNewThread = true;
                                }
                            }
                            else
                            {
                                // Wake up any synchronous receivers
                                _waiter.signal();
                            }
                        }
                    }
                }
            }
        } finally
        {
            this.unlock();
        }

        // start a new thread to run the callback if we determined that it was necessary   
        if (spinOffNewThread)
        {
            try
            {
                _messageProcessor.startNewThread(new AsynchThread(this, false));
            } catch (InterruptedException e)
            {
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.resumeConsumer",
                                            "1:4978:1.22.5.1",
                                            this);

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "resumeConsumer", e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "resumeConsumer");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ConsumerPoint#suspendConsumer()
     */
    @Override
    public boolean suspendConsumer(int suspendFlag)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "suspendConsumer", Integer.valueOf(suspendFlag));

        boolean didSuspendConsumer;
        //  We can't hold the LCP lock before we hold the keyGroup lock and we don't want to
        // take the asynch lock as we'll block on a consumerMessage call but the keyGroup
        // is set inside the LCP and asynch locks and could therefore vanish between checking for it
        // and trying to lock it here. So we take a dirty read of the keyGroup and if we
        // got it we'll lock it. This does mean there are slight windows around setting
        // and unsetting of the keyGroup but the worst case is that we lock the group when
        // we didn't need to (as we're no longer a member of the group).
        JSKeyGroup cachedKeyGroup = _keyGroup;

        // We're a member of a group so we must lock it before we go for the LCP lock
        // and take the message
        if (cachedKeyGroup != null)
            cachedKeyGroup.lock();

        try
        {
            this.lock();
            try
            {
                if (!isConsumerSuspended(suspendFlag)) // Is the consumer already suspended
                {
                    _consumerSuspended = true;
                    _suspendFlags |= suspendFlag;
                    unsetReady();
                    didSuspendConsumer = true;
                }
                else
                {
                    didSuspendConsumer = false;
                }
            } finally
            {
                this.unlock();
            }
        } finally
        {
            if (cachedKeyGroup != null)
                cachedKeyGroup.unlock();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "suspendConsumer", didSuspendConsumer);
        return didSuspendConsumer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ConsumerPoint#closeSession()
     */
    @Override
    public void closeSession(Throwable e) throws SIConnectionLostException, SIResourceException, SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "closeSession", e);

        if (e == null)
        {
            //try and build an appropriate exception
            if (_consumerKey.isClosedDueToDelete())
            {
                e = new SISessionDroppedException(
                                nls.getFormattedMessage(
                                                        "DESTINATION_DELETED_ERROR_CWSIP00221",
                                                        new Object[] {
                                                                      _consumerDispatcher.getDestination().getName(),
                                                                      _messageProcessor.getMessagingEngineName() },
                                                        null));
            }
            else if (_consumerKey.isClosedDueToReceiveExclusive())
            {
                e = new SISessionDroppedException(
                                nls.getFormattedMessage(
                                                        "DESTINATION_EXCLUSIVE_ERROR_CWSIP00222",
                                                        new Object[] {
                                                                      _consumerDispatcher.getDestination().getName(),
                                                                      _messageProcessor.getMessagingEngineName() },
                                                        null));
            }
        }

        //and close the session
        _consumerSession.close();

        if (e != null)
            notifyException(e);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "closeSession", e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DispatchableConsumerPoint#ignoreInitialIndoubts()
     */
    @Override
    public boolean ignoreInitialIndoubts() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "ignoreInitialIndoubts");

        boolean ignoreInitialIndoubts = _consumerSession.ignoreInitialIndoubts();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "ignoreInitialIndoubts", Boolean.valueOf(ignoreInitialIndoubts));

        return ignoreInitialIndoubts;
    }

    @Override
    public MessageProcessor getMessageProcessor()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getMessageProcessor");

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getMessageProcessor", _messageProcessor);

        return _messageProcessor;
    }

    /*
     * (non-Javadoc)
     * If the maxSequentialFailures value less than 1 then the consumer is deemed non stoppable
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.LocalConsumerPoint#registerStoppableAsynchConsumer(com.ibm.wsspi.sib.core.StoppableAsynchConsumerCallback, int, long, int,
     * com.ibm.websphere.sib.Reliability, boolean, com.ibm.ws.sib.processor.impl.OrderingContextImpl, com.ibm.ws.sib.processor.impl.interfaces.ExternalConsumerLock, int)
     */
    @Override
    public void registerStoppableAsynchConsumer(
                                                StoppableAsynchConsumerCallback callback,
                                                int maxActiveMessages,
                                                long messageLockExpiry,
                                                int maxBatchSize,
                                                Reliability unrecoverableReliability,
                                                boolean inLine,
                                                OrderingContextImpl orderingGroup,
                                                ExternalConsumerLock optionalCallbackBusyLock,
                                                int maxSequentialFailures,
                                                long hiddenMessageDelay)
                    throws SISessionUnavailableException, SISessionDroppedException,
                    SIErrorException,
                    SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "registerStoppableAsynchConsumer",
                        new Object[] {
                                      this,
                                      callback,
                                      Integer.valueOf(maxActiveMessages),
                                      Long.valueOf(messageLockExpiry),
                                      Integer.valueOf(maxBatchSize),
                                      unrecoverableReliability,
                                      Boolean.valueOf(inLine),
                                      orderingGroup,
                                      optionalCallbackBusyLock,
                                      maxSequentialFailures,
                                      hiddenMessageDelay });

        try
        {
            this.lock();
            {
                _maxSequentialFailuresThreshold = maxSequentialFailures;

                // If message ordering is required or if no exception destination has been specified, then any 
                // positive value for the threshold should be set to one, so that a consumer will be stopped 
                // as soon as the first message hits its redelivery limit
                if (_destinationAttachedTo.isOrdered() ||
                    (_destinationAttachedTo.getExceptionDestination() == null) ||
                    (_destinationAttachedTo.getExceptionDestination().equals("")))
                {
                    if (_maxSequentialFailuresThreshold > 0)
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "Setting maxSequentialMessageThreshold to 0");

                        _maxSequentialFailuresThreshold = 1;
                    }
                }

                if (_maxSequentialFailuresThreshold <= 0)
                {
                    // if the max sequential failure threshold is zero or less then the consumer
                    //  will never be stopped so we don't want to hide messages
                    _consumerStoppable = false;
                    _hiddenMessageDelay = 0;
                }
                else
                {
                    _consumerStoppable = true;
                    _hiddenMessageDelay = hiddenMessageDelay;
                }
            }
        } finally
        {
            this.unlock();
        }

        registerAsynchConsumer(callback, maxActiveMessages, messageLockExpiry, maxBatchSize, unrecoverableReliability, inLine, orderingGroup, optionalCallbackBusyLock);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerStoppableAsynchConsumer");
    }

    @Override
    public void messageEventOccurred(int event, SIMPMessage msg, TransactionCommon tran) throws SIErrorException, SIRollbackException, SIConnectionLostException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "messageEventOccurred", new Object[] { Integer.valueOf(event), msg, tran });

        if (event == MessageEvents.UNLOCKED)
        {
            // Currently this event is only used incase the asynch consumer does do a
            // remove of any sort. Getting the unlock callback will allow us to deregister
            // the current event listeners.
            eventUnlocked(msg);
        }
        else if (event == MessageEvents.POST_ROLLBACK_REMOVE)
        {
            eventPostRollbackRemove(msg);
        }
        else if (event == MessageEvents.POST_COMMIT_REMOVE)
        {
            eventPostCommitRemove(msg);
        }
        else
        {
            SIErrorException e = new SIErrorException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint",
                                                                  "1:5270:1.22.5.1" },
                                                    null));

            // FFDC
            FFDCFilter
                            .processException(
                                              e,
                                              "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.messageEventOccurred",
                                              "1:5278:1.22.5.1", this);

            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint",
                                      "1:5284:1.22.5.1" });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "messageEventOccurred", e);

            throw e;
        }

        deregisterForEvents(msg);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "messageEventOccurred");
    }

    /*
     * (non-Javadoc)
     * Register for the Unlocked/Post_Rollback_Remove/Post_Commit_Remove events
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener#registerForEvents(com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage)
     */
    @Override
    public void registerForEvents(SIMPMessage msg)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "registerForEvents", msg);

        msg.registerMessageEventListener(MessageEvents.UNLOCKED, this);
        msg.registerMessageEventListener(MessageEvents.POST_ROLLBACK_REMOVE, this);
        msg.registerMessageEventListener(MessageEvents.POST_COMMIT_REMOVE, this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerForEvents");
    }

    /*
     * Deregister for the Unlocked/Post_Rollback_Remove/Post_Commit_Remove events
     */
    public void deregisterForEvents(SIMPMessage msg)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deregisterForEvents", msg);

        msg.deregisterMessageEventListener(MessageEvents.UNLOCKED, this);
        msg.deregisterMessageEventListener(MessageEvents.POST_ROLLBACK_REMOVE, this);
        msg.deregisterMessageEventListener(MessageEvents.POST_COMMIT_REMOVE, this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deregisterForEvents");
    }

    /*
     * eventPostCommitRemove is called when a commit has occurred
     * of a remove of a message by an async consumer. If the async consumer
     * implements the StoppableAsynchConsumerCallback then this method will
     * reset the current sequential failure count. The re-availability of this
     * message will be handled by the consumer dispatcher callbacks.
     */
    private void eventPostCommitRemove(SIMPMessage message) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "eventPostCommitRemove", message);

        if (_consumerStoppable)
        {
            // Lock down the consumer as we need to count stuff...

            //  We can't hold the LCP lock before we hold the keyGroup lock and we don't want to
            // take the asynch lock as we'll block on a consumerMessage call but the keyGroup
            // is set inside the LCP and asynch locks and could therefore vanish between checking for it
            // and trying to lock it here. So we take a dirty read of the keyGroup and if we
            // got it we'll lock it. This does mean there are slight windows around setting
            // and unsetting of the keyGroup but the worst case is that we lock the group when
            // we didn't need to (as we're no longer a member of the group).
            JSKeyGroup cachedKeyGroup = _keyGroup;

            // We're a member of a group so we must lock it before we go for the LCP lock
            // and take the message
            if (cachedKeyGroup != null)
                cachedKeyGroup.lock();

            try
            {
                this.lock();
                try
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Resetting sequential count to zero (from " + _sequentialMessageCounter + ")");

                    // Set the sequential failure counter back to zero as we've now seen a message succeed!
                    _sequentialMessageCounter = 0;
                } finally
                {
                    this.unlock();
                }
            } finally
            {
                if (cachedKeyGroup != null)
                    cachedKeyGroup.unlock();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "eventPostCommitRemove");
    }

    private void unlockAllHiddenMessages()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "unlockAllHiddenMessages");

        List<SIMPMessage> privateMsgList = null;

        // Lock down this consumer to prevent any other thread from modifying the hidden message
        // list (this method can be called from any asynch thread, e.g. a deleteSet() or alarm)
        this.lock();
        try
        {
            // We only have to do anything if we have messages locked
            if (!_hiddenMessages.isEmpty())
            {
                // We need to unlock all the messages we've hidden, however we can't do that under
                // any lock because the messages may get given straight back to us (screwing up the
                // locking heirarchy). So instead we move the list and process it outside of the lock

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "number of hidden messages to unlock: " + _hiddenMessages.size() + "(" + _sequentialMessageCounter + ")");

                privateMsgList = _hiddenMessages;
                _hiddenMessages = new ArrayList<SIMPMessage>();
            }
        } finally
        {
            this.unlock();
        }

        if (privateMsgList != null)
        {
            //Make any messages in the LME unhidden
            // a hidden msg wouldn't be in the lme
            //_allLockedMessages.unhideAllHiddenMessages();
            for (int i = 0; i < privateMsgList.size(); i++)
            {
                SIMPMessage hiddenMessage = privateMsgList.get(i);
                try
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Unlocking hidden message: " + hiddenMessage.getMessage().getSystemMessageId());

                    hiddenMessage.markHiddenMessage(false);
                    hiddenMessage.unlockMsg(_consumerKey.getGetCursor(hiddenMessage).getLockID(), null, true);
                } catch (MessageStoreException e)
                {
                    // MessageStoreException shouldn't occur so FFDC.
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.unlockAllHiddenMessages",
                                                "1:5437:1.22.5.1",
                                                this);

                    SibTr.exception(tc, e);
                    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                new Object[] {
                                              "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.unlockAllHiddenMessage",
                                              "1:5444:1.22.5.1",
                                              e });

                    // Don't throw anything - continue on regardless so that all messages are unlocked
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "unlockAllHiddenMessages");
    }

    /*
     * eventPostRollbackRemove is called when a rollback has occurred
     * of a remove of a message by an async consumer. If the async consumer
     * implements the StoppableAsynchConsumerCallback then this method can
     * hide (keep locked) this message and cause the consumer session to be stopped
     * dependent on how many retries this message has had.
     */
    private void eventPostRollbackRemove(SIMPMessage message)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "eventPostRollbackRemove", message);

        boolean startStopThread = false;
        if (_consumerStoppable)
        {
            int maxFailedDeliveries = _destinationAttachedTo.getMaxFailedDeliveries();
            // redeliveredCount means the number of times it has been redelivered
            // maxFailedDeliveries means the number of times a message has failed to be delivered.
            // at this point in time the first time we are here the redeliveredCount would be zero as
            // it hasn't been REdelivered yet.
            // To make things consistent we'll make it a Delivery count from here on
            int msgDeliveryCount = message.guessRedeliveredCount() + 1;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Message delivery:" + msgDeliveryCount + " Destination max:" + maxFailedDeliveries +
                                " Sequential max:" + _maxSequentialFailuresThreshold + " Delay:" + _hiddenMessageDelay);

            // Lock down the consumer as we need to count stuff...

            //  We can't hold the LCP lock before we hold the keyGroup lock and we don't want to
            // take the asynch lock as we'll block on a consumerMessage call but the keyGroup
            // is set inside the LCP and asynch locks and could therefore vanish between checking for it
            // and trying to lock it here. So we take a dirty read of the keyGroup and if we
            // got it we'll lock it. This does mean there are slight windows around setting
            // and unsetting of the keyGroup but the worst case is that we lock the group when
            // we didn't need to (as we're no longer a member of the group).
            JSKeyGroup cachedKeyGroup = _keyGroup;

            // We're a member of a group so we must lock it before we go for the LCP lock
            // and take the message
            if (cachedKeyGroup != null)
                cachedKeyGroup.lock();

            try
            {
                this.lock();
                try
                {
                    // We don't want to stop the consumer if this consumer is already closed, this can easily
                    // happen due to the fact that this is a transaction rollback event, which is not
                    // scoped to the lifetime of the consumer.
                    if (!_closed)
                    {
                        // If maxFailedDeliveries is 1 then no retries are permitted on a message, so we can't even attempt to
                        // hide the message or stop the consumer before we'd exception it. Instead we simply count this as a
                        // sequential failure and allow the message to be exceptioned (not hidden).
                        // If a message can be retried before being exceptioned then we have a chance to hide it, so if the
                        // message has been tried (max - 1) times we class it as 'failed' and add it to our sequential failure count
                        if ((maxFailedDeliveries == 1) ||
                            (msgDeliveryCount == (maxFailedDeliveries - 1)))
                        {
                            // Handle the sequential failures configuration. Increment the counter 
                            _sequentialMessageCounter++;

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(tc, "Incrementing sequential count to: " + _sequentialMessageCounter);
                        }

                        // If we've reached the  sequential failure threshold then we need to stop this consumer
                        // (outside of this lock unfortunately)
                        if ((_sequentialMessageCounter == _maxSequentialFailuresThreshold) && !_consumerStopThreadRunning)
                        {
                            startStopThread = true;
                            _consumerStopThreadRunning = true;
                        }
                        // If we haven't reached the failure threshold but a delay has been set then we
                        // can hide a 'few' messages temporarily to prevent them being immediately retried (and
                        // probably fail).
                        // This delay only applies to messages that haven't reached their max retry limit,
                        // if they have then they'll be exceptioned or the whole queue point will be blocked
                        // using the 'blockedRetryTimout' on the queue/ME.
                        else if (msgDeliveryCount < maxFailedDeliveries)
                        {
                            if (_hiddenMessageDelay > 0)
                            {
                                // Hide the message
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    SibTr.debug(tc, "Hiding message: " + message.getMessage().getSystemMessageId());

                                // The message is hidden for a set period, if no alarm is currently registered to unhide a message
                                // then we need to register an alarm. First set the hiddenMessageExpiry into the message.
                                long hiddenExpiryTime = System.currentTimeMillis() + _hiddenMessageDelay;
                                message.setHiddenExpiryTime(hiddenExpiryTime);

                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    SibTr.debug(tc, "Set hidden expiry time to: " + hiddenExpiryTime + " : " + new Date(hiddenExpiryTime));

                                // If we don't have an alarm registered, register one now
                                if (!_hiddenMsgAlarmRegistered)
                                {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                        SibTr.debug(tc, "Registering Hidden Message Expiry alarm for: " + message);

                                    _alarmManager.create(_hiddenMessageDelay, this);
                                    _hiddenMsgAlarmRegistered = true;
                                }

                                // Add the message to the hidden messages list
                                _hiddenMessages.add(message);

                                // Mark the message as hidden so that the ConsumerDispatcher doesn't unlock it when it
                                // is called for the rollback (just after this call)
                                message.markHiddenMessage(true);
                            }
                        }

                        // If we now have as many hidden messages as we're allowed sequential failures we
                        // need to suspend the consumer until one of the hidden messages reappears. This stops
                        // us hiding as many messages from the queue as we can process in the 'delay' period.
                        // We also suspend the consumer if we're about to schedule an implicit stop (we've
                        // reached the failure threshold), this'll stop us processing more messages in the
                        // time between now and the stop actually happening
                        if ((_hiddenMessages.size() == _maxSequentialFailuresThreshold) || startStopThread)
                        {
                            _consumerSuspended = true;
                            _suspendFlags |= DispatchableConsumerPoint.SUSPEND_FLAG_MAX_HIDDEN_MSGS_REACHED;
                            unsetReady();
                        }
                    }
                } finally
                {
                    this.unlock();
                }
            } finally
            {
                if (cachedKeyGroup != null)
                    cachedKeyGroup.unlock();
            }

            // If we have reached the sequential message threshold we need to stop the consumer. Stopping
            // the consumer may involve running a consumeMessages() call so we don't want to do it under
            // this innocent bystander's thread. Instead we spin one off to do the work. This obviously
            // introduces the possibility of more messages getting in there and being processed - oh well,
            // we'll have to live with the threashold being slightly exceeded.
            if (startStopThread)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "SequentialMessageThreshold hit, starting new thread to stop consumer");
                Runnable runnable = new ConsumerStopThread(_asynchConsumer);
                try
                {
                    _messageProcessor.startNewThread(runnable);
                } catch (InterruptedException e)
                {
                    // No FFDC Code needed
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "eventPostRollbackRemove");
    }

    /*
     * event Unlocked callback
     * 
     * Does nothing today
     */
    private void eventUnlocked(SIMPMessage msg)
    {

    }

    /**
     * ConsumerStopThread will stop the consumer session, callback to the
     * stoppable asynch consumer to notify that the consumer session has been
     * stopped.
     * 
     * Any previous hidden messages will be unhidden and the current
     * sequential message failure counter reset.
     * 
     */
    private class ConsumerStopThread implements Runnable
    {
        // The asyncConsumer to call back on
        private final AsynchConsumer asynchConsumer;

        public ConsumerStopThread(AsynchConsumer asynchConsumer)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "ConsumerStopThread", asynchConsumer);
            this.asynchConsumer = asynchConsumer;
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "ConsumerStopThread");
        }

        @Override
        public void run()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "run", "ConsumerStopThread");

            boolean closed;
            boolean restarted;

            // We only want to call the callback if this session has actually done anything
            // since the last time we needed to stop them. It's quite possible that we hit the
            // limit multiple times without the consumer ever getting re-started. This can happen
            // if the sequential limit is lower than the maxActiveMessage limit, e.g. 10 messages
            // get farmed out to the consumer (maxActiveMessages = 10), the sequential limit is 2.
            // The 10 messages start to fail (get rolled back). The first two hit the sequential
            // limit, stop the consumer and clear the counter. The next two fail, we 'stop' the
            // consumer (although it's already stopped) and clear the counter. etc.
            // In this scenario we only want to call the callback once, unless they've restarted
            // the session at some point in between. 
            JSLocalConsumerPoint.this.lock();
            {
                restarted = _startSinceSequentialStop;
                closed = _closed || _closing; // Grab our state while we have the lock
            }
            JSLocalConsumerPoint.this.unlock();

            // If the consumer isn't closed, stop them
            if (!closed)
            {
                try
                {
                    // Stop the ConsumerSession
                    _consumerSession.stop();
                } catch (SIException e)
                {
                    // No FFDC code needed 
                    // Added no FFDC statement as it needs to be the first line after exception.
                    // We shouldn't fail to stop but there is a slim possibility that the consumer has
                    // been closed since we checked. Any other exception deserves an FFDC (although we
                    // still carry on and unlock the messages). There's no point throwing the exception
                    // as we're on a new thread.
                    if (!(e instanceof SISessionUnavailableException))
                    {
                        // No FFDC code needed
                        SibTr.exception(tc, e);

                        FFDCFilter.processException(
                                                    e,
                                                    "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.ConsumerStopThread.run",
                                                    "1:5698:1.22.5.1",
                                                    this);
                    }
                }
            }

            // Before we spun this thread off to stop the consumer we suspended it to prevent
            // more messages being processed in the gap - resume it here to clear to flag (obviously
            // we're stopped so the consumer won't actualy get any more messages until they're
            // restarted).
            resumeConsumer(SUSPEND_FLAG_MAX_HIDDEN_MSGS_REACHED);

            // Irrespective of the outcome of the stop() we need to unlock those messages

            // Now we're stopped allow another implicit stop to occur if more messages fail to be processed
            JSLocalConsumerPoint.this.lock();
            {
                _sequentialMessageCounter = 0;
                _startSinceSequentialStop = false;
                _consumerStopThreadRunning = false;
                closed = _closed || _closing; // Grab our state again while we have the lock
            }
            JSLocalConsumerPoint.this.unlock();
            // Having stopped the consumer we must unlock any hidden messages so that they can
            // be processed by someone else or this consumer once it's been re-started
            unlockAllHiddenMessages();

            // If the consumer has been restarted since the last time we told them about the
            // implicit stop (and they haven't been closed) then tell them about the stop
            if (restarted && !closed)
            {
                // Tell the consumer that it's been stopped
                asynchConsumer.consumerSessionStopped();
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "run", "ConsumerStopThread");
        }
    }

    @Override
    public ConsumableKey getConsumerKey()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getConsumerKey");
            SibTr.exit(this, tc, "getConsumerKey", _consumerKey);
        }
        return _consumerKey;
    }

    @Override
    public String toString()
    {
        String returnStr = "";
        if (_consumerKey != null)
        {
            returnStr = returnStr + ", " + _consumerKey.toString();
        }

        return returnStr;
    }

    /**
     * This alarm is used in the timing out of messages from the hidden state on a queue. An alarm is
     * registered in the eventPostRollbackRemove() method where a message is to be hidden and a
     * delay has been configured.
     */
    @Override
    public void alarm(Object arg0)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "alarm", new Object[] { this });

        // Get the current time to use in expiry processing
        long currentTime = System.currentTimeMillis();
        long nextExpiry = 0;
        List<SIMPMessage> expiredHiddenMessages = null;
        boolean resumeConsumerAfterUnlock = false;

        // Lock down this consumer to prevent any other thread from modifying the hidden message list
        this.lock();
        try
        {
            boolean continueExpiryProcessing = true;
            while (continueExpiryProcessing)
            {
                continueExpiryProcessing = false;

                // We only have to do anything if we have hidden messages
                if (!_hiddenMessages.isEmpty())
                {
                    // Get the first hidden message from the list
                    SIMPMessage firstHiddenMessage = _hiddenMessages.get(0);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Checking hidden message " + firstHiddenMessage.getMessage().getSystemMessageId() + " from list");

                    // See if the message has completed its delay
                    if (firstHiddenMessage.getHiddenExpiryTime() <= (currentTime + 10)) // Add 10 milliseconds just to make sure there's no rounding problems
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "alarm", "Expiring hidden state on: " + firstHiddenMessage);

                        if (expiredHiddenMessages == null)
                            expiredHiddenMessages = new ArrayList<SIMPMessage>(1);

                        expiredHiddenMessages.add(firstHiddenMessage);

                        _hiddenMessages.remove(firstHiddenMessage);

                        continueExpiryProcessing = true;
                    }
                    // As these hidden messages have the same delay they're stored in the list in order. So as
                    // soon as we see one message that hasn't finished its hide period we can drop out.
                    else
                    {
                        nextExpiry = firstHiddenMessage.getHiddenExpiryTime() - currentTime;
                    }
                } // !empty
            } // while

            // If we've suspended this consumer because we hid the maximum allowed number of messages and we're
            // about to unlock some we can resume it, but only after we've unlocked the messages, otherwise
            // they'll start processing older ones first.
            if (((_suspendFlags & SUSPEND_FLAG_MAX_HIDDEN_MSGS_REACHED) > 0) && (expiredHiddenMessages != null))
            {
                resumeConsumerAfterUnlock = true;
            }

            // If there is a message to "reveal" in the future register an alarm for it (before
            // we unlock the other messages as that may take some time)
            if (nextExpiry > 0)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "alarm", "Registering alarm for: " + nextExpiry);

                _alarmManager.create(nextExpiry, this);
            }
            else
                _hiddenMsgAlarmRegistered = false;

        } // try
        finally
        {
            this.unlock();
        }

        // If we found expired hidden messages, then mark then as unhidden and unlock it (outside
        // a lock
        if (expiredHiddenMessages != null)
        {
            for (int i = 0; i < expiredHiddenMessages.size(); i++)
            {
                try
                {
                    SIMPMessage hiddenMessage = expiredHiddenMessages.get(i);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Unlocking hidden message: " + hiddenMessage.getMessage().getSystemMessageId());

                    hiddenMessage.markHiddenMessage(false);
                    hiddenMessage.unlockMsg(_consumerKey.getGetCursor(hiddenMessage).getLockID(), null, true);
                } catch (MessageStoreException e)
                {
                    // MessageStoreException shouldn't occur so FFDC.
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.alarm",
                                                "1:5951:1.22.5.1",
                                                this);

                    SibTr.exception(tc, e);
                    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                new Object[] {
                                              "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.alarm",
                                              "1:5958:1.22.5.1",
                                              e });

                    // Don't throw anything - continue on regardless so that all messages are processed
                }
            } // for
        } // expired hidden messages found

        if (resumeConsumerAfterUnlock)
        {
            resumeConsumer(SUSPEND_FLAG_MAX_HIDDEN_MSGS_REACHED);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "alarm", this);
    }

    @Override
    public void implicitClose(SIBUuid12 deletedUuid, SIException exception, SIBUuid8 qpoint)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "implicitClose", new Object[] { deletedUuid, exception });

        boolean closed = false;

        if (deletedUuid == null || deletedUuid.equals(_destinationAttachedTo.getUuid()))
        {
            if (deletedUuid != null)
                // Close due to delete
                closed = _consumerKey.close(ConsumerKey.CLOSED_DUE_TO_DELETE, qpoint);
            else if (exception != null)
                // Close due to ME unreachable
                closed = _consumerKey.close(ConsumerKey.CLOSED_DUE_TO_ME_UNREACHABLE, qpoint);
            else
                // Close due to receive exclusive
                closed = _consumerKey.close(ConsumerKey.CLOSED_DUE_TO_RECEIVE_EXCLUSIVE, qpoint);
        }

        if (closed)
        {
            try
            {
                closeSession(exception);
            } catch (SIException e)
            {
                //since this is on a close it is ok for us to swallow the exception
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.JSLocalConsumerPoint.implicitClose",
                                            "1:6007:1.22.5.1",
                                            this);

                SibTr.exception(tc, e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "implicitClose");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ConsumerPoint#isConsumerSuspended()
     */
    @Override
    public boolean isConsumerSuspended(int suspendFlag)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isConsumerSuspended");
        }

        boolean suspended;
        try
        {
            this.lock();

            if (suspendFlag == 0)
            {
                // the passed in suspend flag was 0 this means the caller wants to know if the consumer
                // is suspended for any reason
                suspended = _consumerSuspended;
            }
            else if (_suspendFlags == 0) // No flags set so it must not be suspended
            {
                suspended = false;
            }
            else if ((_suspendFlags & suspendFlag) == suspendFlag)
            {
                //We were asked about certain type of suspended and we are suspended for that reason
                suspended = true;
            }
            else
            {
                suspended = false;
            }
        } finally
        {
            this.unlock();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.exit(tc, "isConsumerSuspended", Boolean.valueOf(suspended));
        }
        return suspended;
    }

    @Override
    public boolean isGatheringConsumer()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isGatheringConsumer");
            SibTr.exit(tc, "isGatheringConsumer", Boolean.valueOf(_gatherMessages));
        }
        return _gatherMessages;
    }

    // (F001731)
    private class ActiveMsgBlockAlarm implements AlarmListener
    {
        private boolean _popped = false;
        private long _startTime = 0;
        private Alarm _alarm = null;

        /**
         * (F001730)
         * Called while holding the JSLocalConsumerPoint.this.lock()
         */
        public void startAlarm()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "startAlarm", Integer.valueOf(_activeMsgBlockInterval));

            // If we already have an alarm scheduled there's no point scheduling
            // another one (this would imply two suspends in a row which would be odd
            // but we'll let it go here)
            if (_alarm == null)
            {
                _popped = false;
                _alarm = _messageProcessor.getAlarmManager().create(_activeMsgBlockInterval * 1000, this);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "startAlarm", _alarm);
        }

        /**
         * (F001731)
         * Called while holding the JSLocalConsumerPoint.this.lock()
         */
        public void cancelAlarm()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "cancelAlarm", _alarm);

            _startTime = 0;
            // If the scheduled alarm had popped then we need to issue an "all clear"
            // message to show the problem has been resolved
            if (_popped)
            {
                _popped = false;

                // Get all the info needed.
                long consumerId = _consumerSession.getIdInternal();

                String dest = null;
                if ((_destinationAttachedTo.isPubSub()) &&
                    (_consumerDispatcher.getConsumerDispatcherState() != null)) // Just to be on the safe side
                    dest = _consumerDispatcher.getConsumerDispatcherState().getSubscriberID();

                if (dest == null) // A queue or a non-durable subscription
                    dest = _destinationAttachedTo.getName();

                String me;
                // If the CD is remote, find the name of the remote ME we're consuming from
                if (_consumerDispatcher instanceof RemoteConsumerDispatcher)
                {
                    me = SIMPUtils.getMENameFromUuid(((RemoteConsumerDispatcher) _consumerDispatcher).getLocalisationUuid().toString());

                    if (me == null)
                        me = "UNKNOWN";
                }
                // Otherwise we must be consuming from the local ME
                else
                    me = _messageProcessor.getMessagingEngineName();

                // Issue an all clear message
                // "Consumer X of resource Y on messaging engine Z is no longer blocked" 
                SibTr.info(tc, "MSG_BLOCK_CLEARED_CWSIP0183",
                           new Object[] { Long.valueOf(consumerId),
                                         dest,
                                         me });
            }

            // Get rid of the alarm if we have one
            if (_alarm != null)
            {
                _alarm.cancel();
                _alarm = null;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "cancelAlarm");
        }

        /**
         * (F001731)
         */
        @Override
        public void alarm(Object arg0)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "alarm", new Object[] { "ActiveMsgBlockAlarm",
                                                       _alarm,
                                                       Long.valueOf(_startTime) });

            JSLocalConsumerPoint.this.lock();
            try
            {
                // Just to make sure we weren't cancelled as we were popping...
                if (_alarm != null)
                {
                    _popped = true;

                    // Calculate when we were blocked
                    if (_startTime == 0)
                        _startTime = System.currentTimeMillis() - (_activeMsgBlockInterval * 1000);

                    // Get all the info needed.
                    long consumerId = _consumerSession.getIdInternal();

                    String dest = null;
                    if ((_destinationAttachedTo.isPubSub()) &&
                        (_consumerDispatcher.getConsumerDispatcherState() != null)) // Just to be on the safe side
                        dest = _consumerDispatcher.getConsumerDispatcherState().getSubscriberID();

                    if (dest == null) // A queue or a non-durable subscription
                        dest = _destinationAttachedTo.getName();

                    String me;
                    // If the CD is remote, find the name of the remote ME we're consuming from
                    if (_consumerDispatcher instanceof RemoteConsumerDispatcher)
                    {
                        me = SIMPUtils.getMENameFromUuid(((RemoteConsumerDispatcher) _consumerDispatcher).getLocalisationUuid().toString());

                        if (me == null)
                            me = "UNKNOWN";
                    }
                    // Otherwise we must be consuming from the local ME
                    else
                        me = _messageProcessor.getMessagingEngineName();

                    long time = (System.currentTimeMillis() - _startTime) / 1000;

                    // Issue the warning
                    // "Consumer X of resource Y on messaging engine Z has been blocked for N seconds" 
                    SibTr.warning(tc, "MSG_BLOCK_WARNING_CWSIP0182",
                                  new Object[] { Long.valueOf(consumerId),
                                                dest,
                                                me,
                                                Long.valueOf(time) });

                    // Start a new alarm to repeat the warning (after a long interval so we don't flood the log)
                    _alarm = _messageProcessor.getAlarmManager().create(_messageProcessor.getCustomProperties().getMsgBlockWarningRepeat() * 1000, this);
                }
            } finally
            {
                JSLocalConsumerPoint.this.unlock();
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "alarm", _alarm);
        }
    }

    @Override
    /**
     * Unlock all messages which have been locked to this LCP but not consumed
     * with an option to increment the redelivery count or not
     *
     * @throws SIResourceException Thrown if there is a problem in the msgStore
     */
    public void unlockAll(boolean incrementUnlockCount)
                    throws SISessionUnavailableException, SIResourceException,
                    SIMPMessageNotLockedException
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "unlockAll", new Object[] { this, incrementUnlockCount });

        synchronized (_asynchConsumerBusyLock)
        {
            this.lock();
            try
            {
                // Only valid if the consumer session is still open
                checkNotClosed();

                try
                {
                    // Unlock the messages (take this lock to ensure we don't have a problem
                    // getting it if we need it to re-deliver the unlocked messages to ourselves)
                    _allLockedMessages.unlockAll(false, incrementUnlockCount);
                } catch (SIMPMessageNotLockedException e)
                {
                    // No FFDC code needed
                    // This exception has occurred beause someone has deleted the
                    // message(s). Ignore this exception as it is unlocked anyway
                }
            } finally
            {
                this.unlock();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "unlockAll");

    }
}
