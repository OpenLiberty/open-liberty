/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.client.proxyqueue.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.client.AsyncCallbackSynchronizer;
import com.ibm.ws.sib.comms.client.ClientConversationState;
import com.ibm.ws.sib.comms.client.ConnectionProxy;
import com.ibm.ws.sib.comms.client.ConsumerSessionProxy;
import com.ibm.ws.sib.comms.client.DestinationSessionProxy;
import com.ibm.ws.sib.comms.client.OrderingContextProxy;
import com.ibm.ws.sib.comms.client.proxyqueue.AsynchConsumerProxyQueue;
import com.ibm.ws.sib.comms.client.proxyqueue.asynch.AsynchConsumerThreadPool;
import com.ibm.ws.sib.comms.client.proxyqueue.queue.AsynchConsumerQueue;
import com.ibm.ws.sib.comms.client.proxyqueue.queue.Queue;
import com.ibm.ws.sib.comms.client.proxyqueue.queue.QueueData;
import com.ibm.ws.sib.comms.client.proxyqueue.queue.ReadAheadQueue;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.AsynchConsumerCallback;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SICoreConnectionListener;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 * An implementation of the AsynchConsumerProxyQueue interface.
 */
public abstract class AsynchConsumerProxyQueueImpl implements AsynchConsumerProxyQueue {
  private static final String CLASS_NAME = AsynchConsumerProxyQueueImpl.class.getName();
  private static final TraceComponent tc = SibTr.register(AsynchConsumerProxyQueueImpl.class, CommsConstants.MSG_GROUP, CommsConstants.MSG_BUNDLE);
  private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);

  //@start_class_string_prolog@
  public static final String $sccsid = "@(#) 1.82 SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/proxyqueue/impl/AsynchConsumerProxyQueueImpl.java, SIB.comms, WASX.SIB, uu1215.01 09/06/16 11:25:27 [4/12/12 22:14:07]";
  //@end_class_string_prolog@

  static {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source Info: " + $sccsid);
  }

  /*******************************************************************************************************
   * Data encapsulation: Keep all variables in this class private then we can control access to them from
   * inside this class and have a sporting chance of sorting out threading/locking issues (mkv)
   *******************************************************************************************************/

  /*
   * The following group of variables are all related to a registered asynchronous callback
   */

  // Ordering context used by an ordered consumer session
  private OrderingContext currentOrderContext = null; // Only accessed by synchronized methods

  // The asynchronous consumer callback that is registered (if any).
  private volatile AsynchConsumerCallback asynchConsumerCallback = null; // Accessed by synchronised & unsynchronised methods hence volatile

  // Max no of messages permitted in a batch of messages
  private volatile int maxBatchSize; // Accessed by synchronised & unsynchronised methods hence volatile

  // Number of sequential delivery failure after which message processor will stop a stoppable asynchronous consumer
  private int maxSequentialFailures; // Only accessed by synchronized methods

  private long hiddenMessageDelay; // Only accessed by synchronized methods

  // Is this asynchronous consumer receiver listener stoppable or not
  protected boolean stoppable; // Only accessed by synchronized methods

  /*
   * Other variables
   */

  // Conversation helper implementation used to communicate with the ME we are receiving messages from
  private volatile ConversationHelper convHelper; // Accessed by synchronised & unsynchronised methods hence volatile

  // The queue used to hold messages received from the ME before they are handed to the application
  private volatile Queue queue; // Accessed by synchronised & unsynchronised methods hence volatile

  // Indicates whether read ahead of messages should be used or not. If read ahead is enabled then messages
  // will be pre-fetched from the ME before they are actually required to satisfy the application
  private volatile boolean readAhead = false; // Accessed by synchronised & unsynchronised methods hence volatile

  // The consumer session this proxy queue is receiving messages from
  protected volatile ConsumerSessionProxy consumerSession; // Believed to be accessed between threads

  // The unique ID for this proxy queue
  private volatile short id; // Accessed by synchronised & unsynchronised methods hence volatile

  // The proxy queue group that this proxy queue belongs to. The group will be notified when the proxy queue is closed
  private volatile ProxyQueueConversationGroupImpl owningGroup; // Accessed by synchronised & unsynchronised methods hence volatile

  // The exception queue - actions that change the queue content must be performed inside a synchronizsation on exceptionQueue object
  private ArrayList<Throwable> exceptionQueue = new ArrayList<Throwable>(); // Thread safe - operations that change queue content need to be synchronized

  // Number of the next message batch we are expecting to be given. Each message batch that is sent asynchronously from
  // the server contains a message batch sequence number. When processing an unlockAll() request the sequence number is
  // increased  so that a distinction can be made between old messages (prior to the unlockAll()) and new ones.
  private volatile short currentBatchNumber = 0; // Accessed by synchronised & unsynchronised methods hence volatile

  // The type of proxy queue
  static final int READAHEAD = 1;
  static final int ASYNCH = 2;
  static final int ORDERED = 3;
  private volatile int type; // Accessed by synchronised & unsynchronised methods hence volatile

  // The thread on which the application asynchronous consumer callback is currently executing (so that we can tell when
  // we are called by the asynchronous callback itself)
  private volatile Thread asynchConsumerThread; // Accessed by synchronised & unsynchronised methods hence volatile

  // Has the proxy queue been closed
  private volatile boolean _closed = false; // Accessed by synchronised & unsynchronised methods hence volatile

  // Is the proxy queue started?
  private volatile boolean _started; // Accessed by synchronised & unsynchronised methods hence volatile

  // Has the connection backing this proxy queue being dropped?
  private volatile boolean connectionDropped = false; // Accessed by synchronised & unsynchronised methods hence volatile

  // Synchronizer required to prevent deadlock between put & other methods
  private final StashSynchronizer stashSynchronizer = new StashSynchronizer();

  // Stash queue used to hold msgs when proxy queue is busy
  private final List<StashQueueEntry> stashQueue = Collections.synchronizedList(new LinkedList<StashQueueEntry>());

  // Lock required to prevent deadlock between put & other methods
  //
  // The locking hiearchy for using this lock is:
  // 1. proxyQueueSynchronizerlock
  // 2. this
  private final ProxyQueueSynchronizer proxyQueueSynchronizerLock = new ProxyQueueSynchronizer();

  /*
   * Constructor
   */
  AsynchConsumerProxyQueueImpl (final ProxyQueueConversationGroupImpl group, final short id, final Conversation conversation) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", "group="+group+", id="+id+", conversation="+conversation);

     owningGroup = group;

     if (conversation != null) {
       convHelper = new ConversationHelperImpl(conversation, id);
     }

     this.id = id;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
  }

  /*
   * Required by AsynchConsumerProxyQueue interface
   */
  public JsMessage receiveNoWait (final SITransaction transaction) throws MessageDecodeFailedException,
                                                                          SISessionUnavailableException,
                                                                          SISessionDroppedException,
                                                                          SIConnectionUnavailableException,
                                                                          SIConnectionDroppedException,
                                                                          SIResourceException,
                                                                          SIConnectionLostException,
                                                                          SILimitExceededException,
                                                                          SIErrorException,
                                                                          SINotAuthorizedException,
                                                                          SIIncorrectCallException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "receiveNoWait", "transaction="+transaction);
    throw new SIErrorException(); // This method should not be called
  }

  /*
   * Required by AsynchConsumerProxyQueue interface
   */
  public synchronized JsMessage receiveWithWait(final long timeout, final SITransaction transaction) throws MessageDecodeFailedException,
                                                                                                            SISessionUnavailableException,
                                                                                                            SISessionDroppedException,
                                                                                                            SIConnectionUnavailableException,
                                                                                                            SIConnectionDroppedException,
                                                                                                            SIResourceException,
                                                                                                            SIConnectionLostException,
                                                                                                            SILimitExceededException,
                                                                                                            SIErrorException,
                                                                                                            SINotAuthorizedException,
                                                                                                            SIIncorrectCallException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "receiveWithWait", "timeout="+timeout+", transaction="+transaction);
    throw new SIErrorException(); // This method should not be called
  }

  /*
   * Setter/Getter methods for private variables
   */

  void setReadAhead (final boolean b) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setReadAhead", "b="+b);
    readAhead = b;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setReadAhead");
  }

  public void setAsynchConsumerThread(final Thread t) { // public access required by interface
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setAsynchConsumerThread", t);
    asynchConsumerThread = t;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setAsynchConsumerThread");
  }

  void setConversationHelper (final ConversationHelper h) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setConversationHelper","convHelper="+h);
    convHelper = h;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setConversationHelper");
  }

  void setQueue (final Queue q) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setQueue", "q="+q);
    queue = q;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setQueue");
  }

  void setId (final short i) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setId","i="+i);
    id = i;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setId");
  }

  void setOwningGroup (final ProxyQueueConversationGroupImpl g) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setOwningGroup", "g="+g);
    owningGroup = g;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setOwningGroup");
  }

  void setType (final int t) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setType", "t="+t);
    type = t;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setType");
  }

  public int getBatchSize() { // public access required by interface
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getBatchSize");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getBatchSize", maxBatchSize);
    return maxBatchSize;
  }

  public AsynchConsumerCallback getAsynchConsumerCallback() { // public access required by interface
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getAsynchConsumerCallback");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getAsynchConsumerCallback", asynchConsumerCallback);
    return asynchConsumerCallback;
  }

  public Thread getAsynchConsumerThread() { // public access required by interface
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getAsynchConsumerThread");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getAsynchConsumerThread", asynchConsumerThread);
    return asynchConsumerThread;
  }

  public DestinationSessionProxy getDestinationSessionProxy() { // public access required by interface
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getDestinationSessionProxy");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getDestinationSessionProxy", consumerSession);
    return consumerSession;
  }

  public ConsumerSessionProxy getConsumerSessionProxy () { // public required by interface
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getConsumerSessionProxy");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getConsumerSessionProxy", consumerSession);
    return consumerSession;
  }

  public ConversationHelper getConversationHelper() { // public access required by interface
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getConversationHelper");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getConversationHelper", convHelper);
    return convHelper;
  }

  public Queue getQueue () { // public required by unit tests
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getQueue");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getQueue", queue);
    return queue;
  }

  public short getId() { // public required by interface
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getId");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getId", id);
    return id;
  }

  public short getCurrentMessageBatchSequenceNumber () { // public required by interface
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getCurrentMessageBatchSequenceNumber");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getCurrentMessageBatchSequenceNumber", currentBatchNumber);
    return currentBatchNumber;
  }

  public Object getLMEOperationMonitor () { // public required by interface
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getLMEOperationMonitor");
     final Object rc = consumerSession.getLMEMonitor();
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getLMEOperationMonitor", rc);
     return rc;
  }

  int getType () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getType");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getType", type);
    return type;
  }

  public boolean getStarted () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getStarted");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getStarted", _started);
    return _started;
  }

  boolean getClosed() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getClosed");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getClosed", _closed);
    return _closed;
  }

  /**
   * Place a message. The msg is put directly to the proxy queue if the proxy queue is not already being changed by another method. If the proxy queue
   * is busy because it is being changed by another method then the msg is put to a temporary stash queue and will be moved from the stash queue to the
   * proxy queue by the method which currently has the proxy queue locked after this method has finished waiting for responses etc.
   * 
   * <br>
   * <strong>
   * BE WARNED: BE VERY CAREFUL WHEN CHANGING THE LOCKING USED BY THIS METHOD. IT HAS A HABIT OF INTRODUCING DEADLOCKS.
   * SEE DEFECTS 519097, 542650 AND 577952 FOR A FEW EXAMPLES.
   * </strong> 
   */
  public void put (final CommsByteBuffer msgBuffer, final short msgBatch, final boolean lastInBatch, final boolean chunk) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "put", "msgBuffer="+msgBuffer+", msgBatch="+msgBatch+", lastInBatch="+lastInBatch+", chunk="+chunk);

    boolean stashSynchronizerLockedHeld = false;
    boolean proxyQueueSynchronizerLockHeld = false;

    try {
      stashSynchronizerLockedHeld = stashSynchronizer.enter(); // This putter is now active wrt the stash queue

      try {
        proxyQueueSynchronizerLockHeld = proxyQueueSynchronizerLock.tryLock(false); // Shared puts to proxy queue are permitted - lock hierarchy level 0->1

        if (proxyQueueSynchronizerLockHeld) { // If we get the lock then we can proceed to put the msg straight to the proxy queue
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Putting msg to proxy queue");

          stashSynchronizerLockedHeld = stashSynchronizer.exit(); // This putter is no longer active wrt the stash queue as we will use the proxy queue

          synchronized (this) { // lock hiearchy level 1->2             
            //If we are closed then don't put messages onto the queue.
            if (!_closed) {
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Async consumer is not closed - will put msg");
              
              // Is the batch number what we are expecting?
              if (msgBatch == currentBatchNumber) {
                synchronized(queue) {
                  final boolean wasEmpty = queue.isEmpty(id);
   
                  // Does this data represent a partial message or a complete message. A "chucked" message needs to be put back together
                  // before it can be handed to the application
                  if (chunk) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Dealing with a chunked message");
   
                    final byte flags = msgBuffer.get();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Flags="+flags);
   
                    if ((flags & CommsConstants.CHUNKED_MESSAGE_FIRST) == CommsConstants.CHUNKED_MESSAGE_FIRST) {
                      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "First chunk received");
                      final QueueData queueData = new QueueData(this, lastInBatch, chunk, msgBuffer);
                      queue.put(queueData, msgBatch); // Start a new (partial) message in the queue
                    } else {
                      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Middle/Last chunk received");
                        // Append the new chunk of data to the end of an existing partial message. The partial message will
                        // be the last message on the queue. This works for all cases as an async consumer cannot be driven
                        // concurrently (so the last incomplete message on the queue will be the  one we want).
                        final boolean lastChunk = ((flags & CommsConstants.CHUNKED_MESSAGE_LAST) == CommsConstants.CHUNKED_MESSAGE_LAST);
                        queue.appendToLastMessage(msgBuffer, lastChunk);
                    }
                  } else { // Not a chunk so this is an entire message
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Dealing with the entire message");
                    final QueueData queueData = new QueueData(this, lastInBatch, chunk, msgBuffer);
                    queue.put(queueData, msgBatch);
                  }
   
                  // Do now need to kick off a consumer thread to process received messages
   
                  // If the queue was originally empty and the queue now reports that it is not empty (ie a complete
                  // message or batch was placed onto the queue) and the session (proxy queue) is in started mode
                  // then either a dispatch a thread or simply notify the thread that there is work to do.
                  if (wasEmpty) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Queue was previously empty");
                    nudge();
                  } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Queue was not previously empty");
                  }
                }
                
              } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Received data for msgbatch: " + msgBatch + " when we are expecting " + currentBatchNumber + " put will be ignored");
              }
            } else {
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Async consumer is closed - will ignore msg put");   
            }
          } // lock hiearchy level 2->1

          proxyQueueSynchronizerLockHeld = proxyQueueSynchronizerLock.unlock(); // lock hiearchy level 1->0
        } else { 
          //If we don't get the lock then we have to put the msg to the stash queue as proxy queue is busy.
          //Don't do anything if we are closed. We aren't holding the lock on 'this' so we can't guarantee that _closed will not be set to true
          //under our feet, however this shouldn't be a problem as anything on the stash queue will just get discarded if we do get closed.
          if(!_closed)
          {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Putting msg to stash queue");
            stashQueue.add(new StashQueueEntry(msgBuffer, msgBatch, lastInBatch, chunk));              
          }
          else
          {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Async consumer is closed - will ignore msg put");   
          }
           
          stashSynchronizerLockedHeld = stashSynchronizer.exit(); // This putter is no longer active wrt the stash queue
        }
      } finally { // An exception can cause us to need to tidy up here
        if (proxyQueueSynchronizerLockHeld) {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Tidy up of proxyQueueSynchronizerLock required");
          proxyQueueSynchronizerLock.unlock(); // lock hiearchy level 1->0
        }
      }
    } finally { // An exception can cause us to need to tidy up here
      if (stashSynchronizerLockedHeld) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Tidy up of stashSynchronizer required");
        stashSynchronizer.exit(); // This putter is no longer active wrt the stash queue as we will use the proxy queue
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "put");
  }

  /*
   * Associated a consumer session with this proxy queue.
   */
  public void setConsumerSession (final ConsumerSessionProxy cs) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setConsumerSession", "consumerSession="+cs);

    // We should not call setConsumerSession twice. A proxy queue is associated with a session for the
    // lifetime of the session and calling it twice is not permitted.
    if (consumerSession != null) {
      SIErrorException e = new SIErrorException(nls.getFormattedMessage("RESET_OF_CONSUMER_SESSION_SICO1055", null, null));
      FFDCFilter.processException(e, CLASS_NAME + ".setConsumerSession", CommsConstants.RHPQIMPL_SETCONSUMERSESS_01, this);
      throw e;
    }

    // Setting a null consumer session is not permitted
    if (cs == null) {
      SIErrorException e = new SIErrorException(nls.getFormattedMessage("NULL_CONSUMER_SESSION_SICO1056", null, null));
      FFDCFilter.processException(e, CLASS_NAME + ".setConsumerSession", CommsConstants.RHPQIMPL_SETCONSUMERSESS_02, this);
      throw e;
    }

    consumerSession = cs;
    convHelper.setSessionId(consumerSession.getProxyID());

    // Now that we have a consumer see if there are any pending exception we can now send to the consumer
    processExceptions();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setConsumerSession");
  }

  /*
   * Deliver an asynchronous exception to any exception listeners registered for this connection.
   * Requests are first stored on a queue and only delivered if setConsumerSession() has been called.
   */
  public void deliverException (final Throwable exception) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "deliverException", "exception="+exception);

    // Add the exception to the exception queue under synchronisation as we will change the queue content
    synchronized (exceptionQueue) {
      exceptionQueue.add(exception);
    }

    // Then try to deliver the queued exception(s)
    processExceptions();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "deliverException");
  }

  /*
   * Processes all exceptions on the exception queue by passing them to any connection listeners. This method will
   * do nothing if there is no  consumer session set.
   */
  private void processExceptions () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "processExceptions");

    if (consumerSession == null) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "No consumer session has been set yet, doing nothing");
    } else {
       try {
         // Get the Event Listeners from the Core Connection
         final SICoreConnection conn = ((ClientConversationState) owningGroup.getConversation().getAttachment()).getSICoreConnection();
         final SICoreConnectionListener[] listeners = conn.getConnectionListeners();

         // Synchronise to ensure nobody changes the exceptionQueue underneath us while we are walking its members
         synchronized (exceptionQueue) {
           if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
             SibTr.debug(this, tc, "Core connection has " + listeners.length + " listeners");
             SibTr.debug(this, tc, "Exception queue has " + exceptionQueue.size() + " exception(s) on it");
           }

           // Deliver each exception message to each listener
           for (int x = 0; x < listeners.length; x++) {
             for (int y = 0; y < exceptionQueue.size(); y++) {
               final Throwable exception = exceptionQueue.get(y);
               listeners[x].asynchronousException(consumerSession, exception);
             }
           }

           // Now we are done, clear the exception queue
           exceptionQueue.clear();
         }
       } catch (SIException e) {
          FFDCFilter.processException(e, CLASS_NAME + ".processExceptions", CommsConstants.RHPQIMPL_PROCESSEXCEPTIONS_01, this);
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Failed to get the conversation from the proxy queue", e);
       }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "processExceptions");
  }

  /*
   * Called when the proxy queue is being requested to close. At this point the underlying queue
   * should be purged of messages and the queue should act as if it has been closed. This means
   * that no new threads should be kicked off to service the queue etc.
   */
  public synchronized void closing () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "closing");

    _started = false;
    _closed = true;

    // Ensure that we are not inside deliverMessages() before purging the queue
    synchronized(queue.getConcurrentAccessLock()) {
      queue.purge(id);
    }

    notify(); // Wake up any synchronous threads still waiting for a message

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "closing");
  }

  /*
   * Called when the queue should complete its closing. At this point the server is notified of the closure.
   */
  public void closed () throws SIConnectionDroppedException, SIConnectionLostException, SIResourceException, SIErrorException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "closed");

    convHelper.closeSession();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "closed");
  }

  /*
   * Start the asynchronous consumer
   */
  public synchronized void start () throws SIConnectionDroppedException, SIConnectionLostException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "start");

    checkConversationLive();

    _started = true;

    if (asynchConsumerCallback == null) {
      notify(); // Wake up synchronous threads waiting for a message
    } else if (!queue.isEmpty(id)) {
      AsynchConsumerThreadPool.getInstance().dispatch(this); // Dispatch an asynchronous callback thread
    } else if (!readAhead) {
      convHelper.sendStart(); // Proxy queue is empty so request more messages from messaging engine
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
  }

  // Flag is set true when the async consumer is in the process of stopping but not yet stopped
  private volatile boolean stopping = false; // Accessed by multiple threads so volatile

  /*
   * Start to stop the session
   */
  public void stopping ( final boolean notifypeer) throws SISessionDroppedException,
                                                          SIConnectionDroppedException,
                                                          SISessionUnavailableException,
                                                          SIConnectionUnavailableException,
                                                          SIConnectionLostException,
                                                          SIResourceException,
                                                          SIErrorException {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "stopping", "notifypeer="+notifypeer);

     stopping = true;

     if (!readAhead) {
       if (notifypeer) convHelper.exchangeStop();
       queue.waitUntilEmpty(id);
     }

     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "stopping");
  }

  public boolean isStopping () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isStopping");
    final boolean rc = stopping;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isStopping", "rc="+rc);
    return rc;
  }

  /*
   * Finish stopping a session
   */
  public void stopped () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "stopped");

    stopping = false;

    // Wait until we are not inside deliverMessages() before actually stopping the proxy
    synchronized(queue.getConcurrentAccessLock()) {
      _started = false;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "stopped");
  }

  /*
   * Set the asynchronous callback. As this involves an exchange with the server we need to ensure we don't deadlock with the put method by
   * switching the put method to use the stash queue while we wait for our response from the server. After we have received our response we
   * switch the put method back to using the proxy queue (while we still hold the 'this' lock) and move all stashed msgs to the proxy queue.
   */
  public void setAsynchCallback (final AsynchConsumerCallback callback,
                                 final int maxActiveMessages,
                                 final long messageLockExpiry,
                                 final int maxBatchSize,
                                 final OrderingContext orderContext,
                                 final int maxSequentialFailures,
                                 final long hiddenMessageDelay,
                                 final boolean stoppable) throws SISessionDroppedException,
                                                                 SIConnectionDroppedException,
                                                                 SISessionUnavailableException,
                                                                 SIConnectionUnavailableException,
                                                                 SIErrorException,
                                                                 SIIncorrectCallException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
      SibTr.entry(this, tc, "setAsynchCallback", new Object[]{callback,maxActiveMessages,messageLockExpiry,maxBatchSize,orderContext,maxSequentialFailures,hiddenMessageDelay,stoppable});
    }

    boolean proxyQueueSynchronizerLockHeld = false;

    try {
      proxyQueueSynchronizerLockHeld = proxyQueueSynchronizerLock.lock(true); // Force put msg method to use the stash queue - lock hiearchy level 0->1

      synchronized (this) { // lock hierarchy level 1->2
        _setAsynchCallback(callback, maxActiveMessages, messageLockExpiry, maxBatchSize, orderContext, maxSequentialFailures, hiddenMessageDelay, stoppable);
        proxyQueueSynchronizerLockHeld = proxyQueueSynchronizerLock.unlock(); // Switch putters back to using the proxy queue but they will block until we exit this sync block - out of hiearchy level unlock
        stashSynchronizer.waitNoPutters(); // Wait until no putters are active wrt the stash queue
        while (!stashQueue.isEmpty()) { // For each stash queue entry call put on the msg again (this time the msg is put to the proxy queue)
          final StashQueueEntry entry = stashQueue.remove(0);
          put(entry.getMsgBuffer(), entry.getMsgBatch(), entry.getLastInBatch(), entry.getChunk());
        }
      } // lock hiearchy level 2->1
    } finally { // An exception can cause us to need to tidy up here
      if (proxyQueueSynchronizerLockHeld) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Tidy up of proxyQueueSynchronizerLock required");
        proxyQueueSynchronizerLock.unlock(); // lock hiearchy level 1->0
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setAsynchCallback");
  }

  private void _setAsynchCallback (final AsynchConsumerCallback callback,
                                   final int maxActiveMessages,
                                   final long messageLockExpiry,
                                   final int maxBatchSize,
                                         OrderingContext orderContext,  //  Not final as may be overridden
                                   final int maxSequentialFailures,
                                   final long hiddenMessageDelay,
                                   final boolean stoppable) throws SISessionDroppedException,
                                                                   SIConnectionDroppedException,
                                                                   SISessionUnavailableException,
                                                                   SIConnectionUnavailableException,
                                                                   SIErrorException,
                                                                   SIIncorrectCallException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
      SibTr.entry(this, tc, "_setAsynchCallback", new Object[]{callback,maxActiveMessages,messageLockExpiry,maxBatchSize,orderContext,maxSequentialFailures,hiddenMessageDelay,stoppable});
    }

    checkConversationLive();

    if (callback == null) {
      // If we are not readAhead, inform the server of the de-registraion and clean up our resources
      if (!readAhead) {
        convHelper.unsetAsynchConsumer(stoppable);
        owningGroup.notifyClose(this);
      }

      this.currentOrderContext = null;
      this.asynchConsumerCallback = null;
      this.maxBatchSize = 0;
      this.maxSequentialFailures = 0;
      this.hiddenMessageDelay = 0;
      this.stoppable = stoppable;
    } else if (asynchConsumerCallback != null) {
      // Check and see if we are doing a re-registration of an async consumer. If the user is just
      // changing the callback we can possibly optimise this request by not flowing anything to the server.

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Performing a re-registration");

      // If we are read ahead and they supply an order context, then this is not valid and the order context will be ignored
      if (readAhead) orderContext = null;

      // There are certain cases where optimized re-reg is not possible. In the following cases, the
      // registration needs to be cancelled and re-registered. The cases we look for are:
      // - Currently they are in async un-ordered and wish to go ordered.
      // - Currently they are in async ordered and wish to go un-ordered.
      // - Currently they are in async ordered and wish to be associated with a different ordering context.
      // - They are changing the batch size.
      // - Current callback has a different Stoppability to the new callback
      // - Current & new callbacks are Stoppable but maxSequentialFailures or hiddenMessageDelay are different
      // In all read ahead cases, none of the above applies and optimized re-reg can be performed.
      if ((currentOrderContext != null && orderContext == null) ||   // Ordered -> unordered
          (currentOrderContext == null && orderContext != null) ||   // Unordered -> ordered
          (currentOrderContext != orderContext) ||                   // New order
          (this.maxBatchSize != maxBatchSize) ||                     // New Batchsize
          // Current & new callbacks have different Stoppability                                       SIB0115d.comms
          (this.stoppable != stoppable) ||                                                    //SIB0115d.comms,472879
          // Current & new callbacks are Stoppable but maxSequentialFailures is different              SIB0115d.comms
          (this.stoppable && stoppable && this.maxSequentialFailures != maxSequentialFailures) || //SIB0115d.comms,472879
          // Current & new callbacks are stoppable but hiddenMessageDelay is different
          (this.stoppable && stoppable && this.hiddenMessageDelay != hiddenMessageDelay)) {

        // Update the type parameter
        if (currentOrderContext != null && orderContext == null) {
          type = ASYNCH;
        } else if (currentOrderContext == null && orderContext != null) {
          type = ORDERED;
        }

        // Here we need to unlock any messages that are currently locked out to us. These could
        // be messages on our proxy queue as well. Note the very act of de-registration should do this.
        // See the SIB.core defect 251799
        try {
          unlockAll();
        } catch (SIResourceException e) {
          FFDCFilter.processException(e, CLASS_NAME + ".setAsynchCallback", CommsConstants.ASYNCHPQIMPL_SETASYNCCALLBACK_01, this);
          throw new SIErrorException(e);
        }

        // Deregister the server side callback
        convHelper.unsetAsynchConsumer(stoppable);

        // Now the queue is empty we need to replace it with one that meets our new requirement
        queue = obtainQueue(type, orderContext, null);

        // Set the new callback
        convHelper.setAsynchConsumer(callback,
                                     maxActiveMessages,
                                     messageLockExpiry,
                                     maxBatchSize,
                                     orderContext,
                                     maxSequentialFailures,
                                     hiddenMessageDelay,
                                     stoppable);
      }

      this.currentOrderContext = orderContext;
      this.asynchConsumerCallback = callback;
      this.maxBatchSize = maxBatchSize;
      this.maxSequentialFailures = maxSequentialFailures;
      this.hiddenMessageDelay = hiddenMessageDelay;
      this.stoppable = stoppable;
    } else {
      // Otherwise this is a new request. No need for any changing of state, simply go and tell the
      // server what we want to do. Or in the case of read ahead, even less.
      this.asynchConsumerCallback = callback;
      this.currentOrderContext = orderContext;
      this.maxBatchSize = maxBatchSize;
      this.maxSequentialFailures = maxSequentialFailures;
      this.hiddenMessageDelay = hiddenMessageDelay;
      this.stoppable = stoppable;

      if (!readAhead) {
        convHelper.setAsynchConsumer(callback,
                                     maxActiveMessages,
                                     messageLockExpiry,
                                     maxBatchSize,
                                     orderContext,
                                     maxSequentialFailures,
                                     hiddenMessageDelay,
                                     stoppable);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "_setAsynchCallback");
  }

  /*
   * This method will create an underlying queue for this proxy queue. In the case of read-ahead and
   * un-ordered async, brand new queues will be created based on the type parameter.
   *
   * In this case of ordered queues, the queue returned will be based on the orderContext parameter.
   * If another session exists for the same ordering context, it's queue will be returned. If this is
   * the first session, then a new queue will be created.
   *
   * @param queueType The type of queue to create.
   * @param oc The ordering context associated with this queue
   * @param unrecoverableReliability The unrecoverable reliability to use for read ahead
   *
   * @return Returns the queue this proxy queue should use.
   */
  Queue obtainQueue (final int queueType, final OrderingContext oc, final Reliability unrecoverableReliability) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "obtainQueue", "queueType="+queueType+", oc="+oc+", unrecoverableReliability="+unrecoverableReliability);

    Queue q = null;

     if (queueType == READAHEAD) {
       q = new ReadAheadQueue(id, convHelper, unrecoverableReliability);
     } else if (queueType == ASYNCH) {
       q = new AsynchConsumerQueue(false);
     } else { // Must be ordered
       synchronized (oc) {
         final OrderingContextProxy ocp = (OrderingContextProxy)oc;
         q = ocp.getAssociatedQueue();

         if (q == null) {
           q = new AsynchConsumerQueue(true);
           ocp.associateWithQueue(q);
         }
       }
     }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "obtainQueue", q);
    return q;
  }

  /*
   * Deliver messages
   */
  public void deliverMessages () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "deliverMessages");

    // Synchronise on the consumer session callback lock to ensure that all stop/start/reg/dereg operations performed on the callback thread
    // are completed before we allow other non-callback threads to start executing any of these actions.
    synchronized (consumerSession.getCallbackLock()) {
      try {
        final SICoreConnection conn = ((ClientConversationState) owningGroup.getConversation().getAttachment()).getSICoreConnection();
        final AsyncCallbackSynchronizer asyncCallbackSynchronizer = ((ConnectionProxy)conn).getAsyncCallbackSynchronizer();

        // Obtain permission from the callback synchronizer to call the application
        asyncCallbackSynchronizer.enterAsyncMessageCallback();

        try {
          // Grab the concurrent access lock to indicate that e are inside a deliverMessages callback
          synchronized (queue.getConcurrentAccessLock()) {
            if (_started && !queue.isEmpty(id)) {
              queue.deliverBatch(maxBatchSize, id, convHelper);
            }
          }
        } finally {
          // Tell the callback synchronizer that we have completed the message callback
          asyncCallbackSynchronizer.exitAsyncMessageCallback();
        }
      } catch (SIIncorrectCallException e) {
        FFDCFilter.processException(e, CLASS_NAME + ".deliverMessages", CommsConstants.RHPQIMPL_DELIVERMESSAGES_01, this);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Failed to obtain the AsyncCallbackSynchronizer object");
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "deliverMessages");
  }

  /*
   * Unlock all messages. As this involves an exchange with the server we need to ensure we don't deadlock with the put method by
   * switching the put method to use the stash queue while we wait for our response from the server. After we have received our
   * response we switch the put method back to using the proxy queue (while we still hold the 'this' lock) and move all stashed
   * msgs to the proxy queue.
   */
  public void unlockAll() throws SISessionUnavailableException,
                                 SISessionDroppedException,
                                 SIConnectionUnavailableException,
                                 SIConnectionDroppedException,
                                 SIResourceException,
                                 SIConnectionLostException,
                                 SIErrorException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unlockAll");

    boolean proxyQueueSynchronizerLockHeld = false;

    try {
      proxyQueueSynchronizerLockHeld = proxyQueueSynchronizerLock.lock(true); // Force put msg method to use the stash queue - lock hiearchy level 0->1

      synchronized (this) { // lock hierarchy level 1->2
        _unlockAll();
        proxyQueueSynchronizerLockHeld = proxyQueueSynchronizerLock.unlock(); // Switch putters back to using the proxy queue but they will block until we exit this sync block - out of hiearchy level unlock
        stashSynchronizer.waitNoPutters(); // Wait until no putters are active wrt the stash queue
        while (!stashQueue.isEmpty()) { // For each stash queue entry call put on the msg again (this time the msg is put to the proxy queue)
          final StashQueueEntry entry = stashQueue.remove(0);
          put(entry.getMsgBuffer(), entry.getMsgBatch(), entry.getLastInBatch(), entry.getChunk());
        }
      } // lock hiearchy level 2->1
    } finally { // An exception can cause us to need to tidy up here
      if (proxyQueueSynchronizerLockHeld) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Tidy up of proxyQueueSynchronizerLock required");
        proxyQueueSynchronizerLock.unlock(); // lock hiearchy level 1->0
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unlockAll");
  }

  private void _unlockAll () throws SISessionUnavailableException,
                                    SISessionDroppedException,
                                    SIConnectionUnavailableException,
                                    SIConnectionDroppedException,
                                    SIResourceException,
                                    SIConnectionLostException,
                                    SIErrorException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "_unlockall");

    checkConversationLive();
    currentBatchNumber++;
    queue.purge(id);
    queue.unlockAll();
    convHelper.unlockAll();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "_unlockall");
  }

  /*
   * Notify any threads waiting for a messsage that message have arrived
   */

  public synchronized void nudge () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "nudge");

    if (_started) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Queue is started");

      if (!queue.isEmpty(id)) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Queue is not empty for sessionId="+id);

        if (asynchConsumerCallback != null) {
          AsynchConsumerThreadPool.getInstance().dispatch(this);
        } else {
          notifyAll();
        }
      } else {
       if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Queue is empty for sessionId="+id);
      }
    } else {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Async consumer is not started - no initiation action will be taken to process the put message");
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "nudge");
  }

  /*
   * We are being notified that the backing conversation has closed/dropped
   */
  public synchronized void conversationDroppedNotification () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "conversationDroppedNotification");

    connectionDropped = true;
    notifyAll();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "conversationDroppedNotification");
  }

  /*
   * Checks to see if the underlying conversation has been dropped
   */
  void checkConversationLive () throws SIConnectionDroppedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "checkConversationLive");

    if (connectionDropped) {
      throw new SIConnectionDroppedException(nls.getFormattedMessage("CONVERSATION_CLOSED_SICO0068", null, null));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "checkConversationLive");
  }

  public String toString () {
    return getClass()+"@" + Integer.toHexString(hashCode()) + ": " + queue;
  }

  /*
   * Private synchronizer class which allows multiple putters to enter & exit but will only allow the waitNoPutters
   * method to proceed when there are no active (entered but not exited) putters. Uses of the enter & exit method
   * should use a suitable try-finally block to ensure that an exit is never missed should an exception be thrown
   * while entered with respect to the stashSynchronizer.
   */

  private static final TraceComponent tc1 = SibTr.register(AsynchConsumerProxyQueueImpl.StashSynchronizer.class, CommsConstants.MSG_GROUP, CommsConstants.MSG_BUNDLE);

  //@ThreadSafe
  private final class StashSynchronizer {

    //@GuardedBy("this");
    private int putters = 0;

    synchronized boolean enter () {
      if (TraceComponent.isAnyTracingEnabled() && tc1.isEntryEnabled()) SibTr.entry(this, tc1, "enter","putters="+putters);
      ++putters;
      if (TraceComponent.isAnyTracingEnabled() && tc1.isEntryEnabled()) SibTr.exit(this, tc1, "enter","putters="+putters);
      return true;
    }

    synchronized boolean exit () {
      if (TraceComponent.isAnyTracingEnabled() && tc1.isEntryEnabled()) SibTr.entry(this, tc1, "exit","putters="+putters);
      if (--putters == 0) {
        notifyAll();
      }
      if (TraceComponent.isAnyTracingEnabled() && tc1.isEntryEnabled()) SibTr.exit(this, tc1, "exit","putters="+putters);
      return false;
    }

    synchronized void waitNoPutters () {
      if (TraceComponent.isAnyTracingEnabled() && tc1.isEntryEnabled()) SibTr.entry(this, tc1, "waitNoPutters","putters="+putters);
      if (putters != 0) {
        while (true) {
          try {
            wait();
            break;
          } catch (InterruptedException e) {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc1.isDebugEnabled()) SibTr.exception(this, tc1, e);
          }
        }
      }
      if (TraceComponent.isAnyTracingEnabled() && tc1.isEntryEnabled()) SibTr.exit(this, tc1, "waitNoPutters");
    }
  }

  private static final TraceComponent tc2 = SibTr.register(AsynchConsumerProxyQueueImpl.StashQueueEntry.class, CommsConstants.MSG_GROUP, CommsConstants.MSG_BUNDLE);

  //@Immutable
  private final class StashQueueEntry {

    private final CommsByteBuffer msgBuffer;
    private final short msgBatch;
    private final boolean lastInBatch;
    private final boolean chunk;

    StashQueueEntry (final CommsByteBuffer msgBuffer, final short msgBatch, final boolean lastInBatch, final boolean chunk) {
      if (TraceComponent.isAnyTracingEnabled() && tc2.isEntryEnabled()) SibTr.entry(this, tc2, "<init>","msgBuffer="+msgBuffer+", msgBatch="+msgBatch+", lastInBatch="+lastInBatch+", chunk="+chunk);
      this.msgBuffer   = msgBuffer;
      this.msgBatch    = msgBatch;
      this.lastInBatch = lastInBatch;
      this.chunk       = chunk;
      if (TraceComponent.isAnyTracingEnabled() && tc2.isEntryEnabled()) SibTr.exit(this, tc2, "<init>");
    }

    CommsByteBuffer getMsgBuffer () {
      return msgBuffer;
    }

    short getMsgBatch () {
      return msgBatch;
    }

    boolean getLastInBatch () {
      return lastInBatch;
    }

    boolean getChunk () {
      return chunk;
    }
  }

  /*
   * A private synchronizer class used to control access to the proxy queue. Either a single exclusive lock or multiple shared locks can
   * be obtained. The class is similar to a RenetrantReadWriteLock but because RenetrantReadWriteLock doesn't quite do what we require
   * at the Java 1.5 level (at which this code must work) this class is required. The thread which holds the exclusive lock may take
   * the exclusive lock again but must release the exclusive lock the same number of times as it takes it.
   */

  private static final TraceComponent tc3 = SibTr.register(AsynchConsumerProxyQueueImpl.ProxyQueueSynchronizer.class, CommsConstants.MSG_GROUP, CommsConstants.MSG_BUNDLE);

  //@ThreadSafe
  private final class ProxyQueueSynchronizer {

    //@GuardedBy("this");
    int locks = 0; // No of locks currently held

    //@GuardedBy("this");
    boolean exclusive; // Is the lock held exclusively or not

    //@GuardedBy("this");
    Thread thread; // The thread that holds the exclusive lock otherwise null

    synchronized boolean lock (final boolean excl) {
      if (TraceComponent.isAnyTracingEnabled() && tc3.isEntryEnabled()) SibTr.entry(this, tc3, "lock"); // Defer arg tracing to tryLock method

      while (!tryLock(excl)) {
        if (TraceComponent.isAnyTracingEnabled() && tc3.isDebugEnabled()) SibTr.debug(this, tc3, "Waiting for lock...");
        try {
          wait();
        } catch (InterruptedException e) {
         // No FFDC code needed
        }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc3.isEntryEnabled()) SibTr.exit(this, tc3, "lock","rc=true");
      return true;
    }

    synchronized boolean unlock () {
      if (TraceComponent.isAnyTracingEnabled() && tc3.isEntryEnabled()) SibTr.entry(this, tc3, "unlock");
      if (TraceComponent.isAnyTracingEnabled() && tc3.isDebugEnabled()) SibTr.debug(this, tc3, "locks="+locks+", exclusive="+exclusive);

      if (--locks == 0) {
        thread = null;
        notifyAll(); // Notify all waiters as they could all be for shared locks
      }

      if (TraceComponent.isAnyTracingEnabled() && tc3.isDebugEnabled()) SibTr.debug(this, tc3, "locks="+locks+", exclusive="+exclusive);
      if (TraceComponent.isAnyTracingEnabled() && tc3.isEntryEnabled()) SibTr.exit(this, tc3, "unlock","rc=false");
      return false;
    }

    synchronized boolean tryLock (final boolean excl) {
      if (TraceComponent.isAnyTracingEnabled() && tc3.isEntryEnabled()) SibTr.entry(this, tc3, "tryLock","excl="+excl);
      if (TraceComponent.isAnyTracingEnabled() && tc3.isDebugEnabled()) SibTr.debug(this, tc3, "locks="+locks+", exclusive="+exclusive);

      boolean rc = true;

      if (locks == 0) { // Currently unlocked so just grab the lock
        locks++;
        exclusive = excl;

        if (exclusive) {
          thread = Thread.currentThread();
        } else {
          thread = null;
        }
      } else if (!exclusive && !excl) { // Currently locked shared & shared lock required
        locks++;
      } else if (exclusive && thread.equals(Thread.currentThread())) { // Currently locked exclusive by this thread
        locks++;
      } else { // Locked in an unacceptable way for us
        rc = false;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc3.isDebugEnabled()) SibTr.debug(this, tc3, "locks="+locks+", exclusive="+exclusive);
      if (TraceComponent.isAnyTracingEnabled() && tc3.isEntryEnabled()) SibTr.exit(this, tc3, "tryLock","rc="+rc);
      return rc;
    }
  }

  /**
   * Purge+unlock all messages
   * @see AsynchConsumerProxyQueue#rollbackOccurred()  
   * @throws SIErrorException 
   * @throws SIResourceException 
   * @throws SIConnectionLostException 
   * @throws SIConnectionUnavailableException 
   * @throws SISessionUnavailableException 
   * @throws SIConnectionDroppedException 
   * @throws SISessionDroppedException 
   */
  public void rollbackOccurred() throws SISessionDroppedException, SIConnectionDroppedException, SISessionUnavailableException, SIConnectionUnavailableException, SIConnectionLostException, SIResourceException, SIErrorException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "rollbackOccurred");
    // Synchronize on the flush lock before unlocking
    synchronized(queue.getConcurrentAccessLock()) {
      unlockAll();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "rollbackOccurred");
  }   
}
