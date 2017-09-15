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
import java.util.Iterator;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageCopyFailedException;
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
import com.ibm.ws.sib.mfp.control.ControlRequestHighestGeneratedTick;
import com.ibm.ws.sib.mfp.control.ControlResetRequestAck;
import com.ibm.ws.sib.mfp.control.ControlResetRequestAckAck;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.gd.GDConfig;
import com.ibm.ws.sib.processor.gd.TickRange;
import com.ibm.ws.sib.processor.impl.destination.RemotePtoPSupport;
import com.ibm.ws.sib.processor.impl.exceptions.ClosedException;
import com.ibm.ws.sib.processor.impl.interfaces.Browsable;
import com.ibm.ws.sib.processor.impl.interfaces.BrowseCursor;
import com.ibm.ws.sib.processor.impl.interfaces.JSConsumerManager;
import com.ibm.ws.sib.processor.impl.interfaces.ControlHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumableKey;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.AsyncUpdate;
import com.ibm.ws.sib.processor.impl.store.AsyncUpdateThread;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.impl.store.items.AOStartedFlushItem;
import com.ibm.ws.sib.processor.impl.store.items.AOValue;
import com.ibm.ws.sib.processor.impl.store.itemstreams.AOProtocolItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream;
import com.ibm.ws.sib.processor.io.MPIO;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.processor.utils.UserTrace;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.SelectorDomain;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;

/**
 * The output handler for the anycast 'remote get' protocol.
 * Manages multiple AOStreams, that may be concurrently active.
 * Dispatches incoming messages that have a streamId to the correct AOStream (or may respond with a flushed message)
 * Provides helper methods for the AOStreams to send messages to remote MEs.
 *
 *
 * Implementation of ConsumerCardinality=EXCLUSIVE:
 *   The AOH will have a maximum of one AOStream in the streamTable that has not yet begun to be flushed.
 *   Typically, an AOStream closes its JSRemoteConsumerPoint only after the flush is complete. However, if the RME is
 *   unreachable it may close the JSRemoteConsumerPoint after the AOStartedFlushItem is written but before the flush
 *   is complete. The code relies on the ConsumerDispatcher to enforce cardinality by not allowing more
 *   than one ConsumerKey from attaching.
 *   Before scheduling the creation of an AOStream, the AOH will try to attach to the ConsumerDispatcher.
 *   Only if the attach succeeds
 *   is the creation scheduled. Here is the list of events which involve checking or doing something when
 *   ConsumerCardinality=EXCLUSIVE
 *   - AOH Construction: Let k be the number of persistent streams that are not being flushed. If an AOStream
 *      is being flushed, it is created with no JSRemoteConsumerPoint.
 *     - If k > 1 : error
 *     - If k == 1
 *       - try to attach to the CD and get a CK
 *         - if successful, create the AOStream with a ConsumerKey
 *         - if not successful, create the AOStream with no ConsumerKey, and tell the stream to start flushing.
 *     - if k == 0
 *       - do nothing
 *   - CreateStream request:
 *       - try to attach to the CD and get a CK
 *         - if successful, schedule creation of the AOStream with this CK
 *         - if not successful, tell RME that creation is denied due to cardinality restriction.
 *
 * SYNCHRONIZATION:
 *
 * For AOStreams: All the AOStream information is contained in StreamInfo objects. All the StreamInfo
 * objects are in a hashtable streamTable. All reads and writes of this hashtable occur using
 * synchronized (streamTable). Once a StreamInfo object is retrieved from streamTable most reads of
 * its fields occur without any synchronization. Fields read without synchronization must have be
 * initialized prior to or during the synchronization block that inserted the StreamInfo object into
 * the streamTable. This is sufficient to ensure that the reader will see the latest value.
 * Reading and writing a field that is not initialized in this manner is synchronized on the StreamInfo object.
 * The AOStreams issue 2 kinds of callbacks on the AnycastOutputHandler:
 * (1) send*(): These are methods to send messages, and for performance reasons, the AOStreams
 *     do not hold any locks when calling these methods.
 * (2) Other methods that read/write data-structures: The AOStream may hold locks when calling these
 *     methods. The AnycastOutputHandler may need to acquire locks to execute these methods.
 * In general, to avoid deadlocks between AOStream and AnycastOutputHandler locks, the former should
 * be acquired before the latter. Therefore, before calling any method on an AOStream, the AnycastOutputHandler
 * releases all its locks.
 *
 * For AOBrowserSessions: The Hashtable browserSessionTable is already synchronized. AOBrowserSessions
 * handle their own synchronization. A AOBrowserSession does hold a lock on itself when calling send*().
 * This could be improved, but concurrency is not a concern at this point since the browse protocol only
 * has one request-response active at any time.
 *
 * STARTUP & SHUTDOWN: The AOH is active and capable of handling messages as soon as the constructor returns.
 *  There are 3 kinds of shutdown
 *  (1) close(): This is used to cleanup non-persistent resources, and quiesce the streams etc.
 *      Does not flush the streams! Useful for temporary ME shutdown.
 *      The method is asynchronous since there can be attempts to send a message or log something to the MS
 *      after the method returns. The caller of this method should ensure that no method of the ControlHandler
 *      interface is called once close() is invoked.
 *  (2) closeAndFlush(): This is used to cleanup both persistent and non-persistent resourced. Useful when
 *      the destination is being deleted, or the ME is being shutdown for a long period of time. The method
 *      is asynchronous, since flushing a stream can take some time. However, remote browse sessions are
 *      immediately terminated. The method isCloseAndFlushCompleted() is used to query whether it is done.
 *   (3) cleanup(boolean forceCleanup): If !forceCleanup the behavior is equivalent to closeAndFlush() except
 *       this method is synchronous, i.e., it will not return until all streams are flushed. If forceCleanup
 *       this method does not have to wait for the streams to flush.
 */
public class AnycastOutputHandler implements ControlHandler
{

  private static TraceComponent tc =
        SibTr.register(
          AnycastOutputHandler.class,
          SIMPConstants.MP_TRACE_GROUP,
          SIMPConstants.RESOURCE_BUNDLE);

 
  // NLS for component
  private static final TraceNLS nls_mt =
    TraceNLS.getTraceNLS(SIMPConstants.TRACE_MESSAGE_RESOURCE_BUNDLE);
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private final String destName;
  private final SIBUuid12 destUuid;

  private final DestinationHandler destinationHandler;
  // This will only be set where the AOH is being used for durable pub sub - otherwise we look up
  // the consumerDispatcher dynamically
  private JSConsumerManager pubSubConsumerDispatcher;
  private final boolean isPubSub;
  
  private final ItemStream containerItemStream; // contains all the AOProtocolItemStreams
  private final MessageProcessor mp;
  private final AsyncUpdateThread msUpdateThread; // used for all the asynchronous updates to MS, except for persisting locks and value ticks
  private final AsyncUpdateThread persistLockThread; // used for persisting locks and value ticks
  /** the table containing all the active BrowseSessions */
  private final Hashtable<AOBrowserSessionKey, AOBrowserSession> browserSessionTable;

  /** The monotonically increasing version of the DME (after each crash). Used for resetting request acks. */
  public final long dmeVersion;
  /**
   * For messages to a particular stream, we lookup the StreamInfo object (sinfo) in the streamTable
   * - If there is a sinfo, we dispatch the message to that stream, (if sinfo.stream!=null, else ignore the message)
   * - else we send a flushed message to the sender.
   * For stream creation requests, we
   * - if a stream already exists,
   *    - if the stream is not flushed send NotFlushed message
   *    - else don't do anything
   * - if no stream exists for this RME
   *    - we get a new stream name
   *    - create a StreamInfo object with stream and itemStream equal to null (as a placeholder) and
   *      insert into streamTable.
   *    - create a AOProtocolItemStream, AOStream, and then we asynchronously add the itemStream to the
   *      containerItemStream. When this async insertion commits, we create another StreamInfo object
   *      with non-null stream and itemStream and replace the placeholder. Note that we replace the placeholder
   *      instead of mutating the stream and itemStream fields since these will be read outside a synchronization
   *      block (see synchronization comments above)
   */
  /** the table containing all the StreamInfo objects, keyed by the gathering consumer id/remote id */
  private final Hashtable<String, StreamInfo> streamTable;

  private boolean cardinalityOne;
  private final Object cardinalityOneLock;

  private boolean closed; // initialized to false, and set to true when close() is called

  // initialized to false, and set to true when closeAndFlush() is called
  private boolean startedCloseAndFlush;
  // initialized to false, and set to true when closeAndFlush() has been called, and all streams have been flushed
  private volatile boolean finishedCloseAndFlush;
  private boolean redriveDeletionThread; // set to true if should redrive deletion thread when finishedCloseAndFlush becomes true

  private final long controlItemLockID; // the MS lockID used to lock any control information prior to removal.

  class StreamInfo { // the stuff for each protocol stream
    final String streamKey;
    final SIBUuid12 streamId;

    AOStream stream; // the in-memory stuff and the message processing logic is here
    AOProtocolItemStream itemStream; // the persistent information is here, as AOValue items.

    AOStartedFlushItem item;

    StreamInfo(String streamKey, SIBUuid12 streamId, AOStream stream, AOProtocolItemStream itemStream)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "StreamInfo",
          new Object[] { streamKey, streamId, stream, itemStream });

   
      this.streamKey = streamKey;
      this.streamId = streamId;
      this.stream = stream;
      this.itemStream = itemStream;
      this.item = null;
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "StreamInfo", this);         
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
      String strRep = "Key: " + streamKey + ", Id: " + streamId + ", stream: " + stream +
        ", itemstream: " + itemStream + ", item: " + item;
      return strRep;
    }
  }

  /**
   * The constructor. The destName, destUuid, receiveExclusive are passed separately from the destination handler since durable
   * subscriptions use a pseudo destination per subscription, but the destination handler is the same for all subscriptions
   * to the topic space. This code works for both durable subs and remote get from queues.
   * @param destName The name of the destination
   * @param destUuid The UUID of the destination
   * @param receiveExclusive true if only 1 consumer can receive from this destination
   * @param cd
   * @param containerItemStream Contains the persistent protocol streams
   * @param mp
   * @param msUpdateThread
   * @param persistLockThread
   * @param dmeVersion
   * @param restartFromStaleBackup true if restarting from stale backup, else false
   */
  public AnycastOutputHandler(String destName, SIBUuid12 destUuid, boolean receiveExclusive,
    DestinationHandler destinationHandler, ConsumerDispatcher cd, SIMPItemStream containerItemStream, MessageProcessor mp, AsyncUpdateThread msUpdateThread,
    AsyncUpdateThread persistLockThread, long dmeVersion, boolean restartFromStaleBackup) throws Exception
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "AnycastOutputHandler",
        new Object[] {
          destName,
          destUuid,
          Boolean.valueOf(receiveExclusive),
          destinationHandler,
          cd,
          containerItemStream,
          mp,
          msUpdateThread,
          persistLockThread,
          Long.valueOf(dmeVersion),
          Boolean.valueOf(restartFromStaleBackup)});

    this.destName = destName;
    this.destUuid = destUuid;
    this.cardinalityOneLock = new Object();
    setCardinalityOne(receiveExclusive);

    this.destinationHandler = destinationHandler;
    this.pubSubConsumerDispatcher = cd;
    if (pubSubConsumerDispatcher != null)
      isPubSub = true;
    else
      isPubSub = false;

    this.containerItemStream = containerItemStream;
    this.mp = mp;
    this.controlItemLockID = mp.getMessageStore().getUniqueLockID(AbstractItem.STORE_NEVER);

    this.msUpdateThread = msUpdateThread;
    this.persistLockThread = persistLockThread;
    this.dmeVersion = dmeVersion;

    this.browserSessionTable = new Hashtable<AOBrowserSessionKey, AOBrowserSession>();
    this.streamTable = new Hashtable<String,StreamInfo>();


    // streams that should be flushed because ConsumerCardinality=1 and either
    // (1) JSRemoteConsumerPoint could not be created
    // (2) more than 1 stream has not started flush (this may happen if the consumer cardinality was
    //     changed while the ME was down). We start flush on all the streams in this case
    ArrayList<AOStream> toFlushStream = new ArrayList<AOStream>();
    boolean redoInitialization = false;
    ArrayList clashingRemoteMEIdList = new ArrayList();
    
    // now initialize the streamTable
    try
    {
      synchronized (streamTable)
      {
        NonLockingCursor cursor;
        AbstractItem abitem;
        do 
        {
          redoInitialization = false;
          streamTable.clear(); // ensure the table is clear
          
          cursor = containerItemStream.newNonLockingItemStreamCursor(null);
          while ((abitem=cursor.next()) != null)
          {
            if (abitem instanceof AOProtocolItemStream)
            {
              AOProtocolItemStream itemStream = (AOProtocolItemStream) abitem;
              SIBUuid8 remoteMEId = itemStream.getRemoteMEId();
              /**
               * The key we use to lookup/insert a streamInfo object is based off the remoteMEuuid +
               * the gatheringTargetDestUuid. This second value is null for standard consumer and set
               * to a destinationUuid (which could be an alias) for gathering consumers. In this way
               * we have seperate streams per consumer type.
               */
              String streamKey = 
                SIMPUtils.getRemoteGetKey(remoteMEId,itemStream.getGatheringTargetDestUuid());
              SIBUuid12 streamId = itemStream.getStreamId();
              
              StreamInfo sinfo = streamTable.get(streamKey);
              
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "AnycastOutputHandler retrieved: " + sinfo + ", using key: " + streamKey +
                  ", streamId: " + streamId + ", AOProtocolItemStream: " + itemStream);
              
              if (sinfo != null)
              {
                if (sinfo.streamId.equals(streamId))
                {
                  if (sinfo.itemStream == null)
                  {
                    sinfo.itemStream = itemStream;
                  }
                  else
                  {
                    if (clashingRemoteMEIdList.contains(remoteMEId))
                    {
                      // We have seen this Id before, tried to clean it up
                      // but failed for some reason. Throw exception rather 
                      // than cause an infinite loop
                      // ERROR - two itemStreams with the same remoteMEId and streamId!
                      // log error and throw exception
                      SIErrorException e = new SIErrorException(
                        nls.getFormattedMessage(
                          "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                          new Object[] {
                            "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
                            "1:441:1.89.4.1" },
                          null));
    
                      // FFDC
                      FFDCFilter.processException(
                        e,
                        "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.AnycastOutputHandler",
                        "1:448:1.89.4.1",
                        this);
    
                      SibTr.exception(tc, e);
    
                      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                      new Object[] {
                        "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
                        "1:456:1.89.4.1" });
    
                      throw e;
                    }
                    else
                    {
                      clashingRemoteMEIdList.add(remoteMEId);
                      cleanupPersistentStreams(containerItemStream, remoteMEId);
                      redoInitialization = true;
                      break;
                    }
                  }
                }
                else
                {
                  // remoteMEId is the same
                  if (clashingRemoteMEIdList.contains(remoteMEId))
                  {
                    // We have seen this Id before, tried to clean it up
                    // but failed for some reason. Throw exception rather 
                    // than cause an infinite loop
                
                    // ERROR - two itemStreams with the same streamKey!
                    // log error and throw exception
                    SIErrorException e = new SIErrorException(
                      nls.getFormattedMessage(
                        "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                        new Object[] {
                          "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
                          "1:485:1.89.4.1" },
                        null));
                    // FFDC
                    FFDCFilter.processException(
                      e,
                      "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.AnycastOutputHandler",
                      "1:491:1.89.4.1",
                      this);
    
                    SibTr.exception(tc, e);
    
                    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                    new Object[] {
                      "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
                      "1:499:1.89.4.1" });
                    throw e;
                  }
                  else
                  {
                    clashingRemoteMEIdList.add(remoteMEId);
                    cleanupPersistentStreams(containerItemStream, remoteMEId);
                    redoInitialization = true;
                    break;
                  }
                }
              } // end if (sinfo != null)
              else
              {
                sinfo = new StreamInfo(streamKey, streamId, null, itemStream);
                
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  SibTr.debug(tc, "AnycastOutputHandler AOProtocolItemStream put: " + sinfo + ", using key: " + streamKey);  
                
                streamTable.put(streamKey, sinfo);
              }
            } // end if (abitem instanceof AOProtocolItemStream)
          } // end while ((abitem=cursor.next()) != null)
          cursor.finished();
        } while (redoInitialization); // end while-do (redoInitialization)
        
        cursor = containerItemStream.newNonLockingItemCursor(null);
        while ((abitem=cursor.next()) != null)
        {
          if (abitem instanceof AOStartedFlushItem)
          {
            AOStartedFlushItem item = (AOStartedFlushItem) abitem;
            String streamKey = item.getStreamKey();
            SIBUuid12 streamId = item.getStreamId();
           
            StreamInfo sinfo = streamTable.get(streamKey);
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
              SibTr.debug(tc, "AnycastOutputHandler retrieved: " + sinfo + ", using key: " + streamKey +
                ", streamId: " + streamId + ", AOStartedFlushItem: " + item);            
            
            if (sinfo != null)
            {
              if (sinfo.streamId.equals(streamId))
              {
                if (sinfo.item == null)
                {
                  sinfo.item = item;
                }
                else
                {
                  // ERROR - two items with the same streamKey and streamId!
                  // log error and throw exception
                  SIErrorException e = new SIErrorException(
                    nls.getFormattedMessage(
                      "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                      new Object[] {
                        "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
                        "1:557:1.89.4.1" },
                      null));

                  // FFDC
                  FFDCFilter.processException(
                    e,
                    "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.AnycastOutputHandler",
                    "1:564:1.89.4.1",
                    this);

                  SibTr.exception(tc, e);

                  SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                  new Object[] {
                    "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
                    "1:572:1.89.4.1" });

                  throw e;
                }
              }
              else
              {
                // ERROR - two items with the same streamKey!
                // log error and throw exception
                SIErrorException e = new SIErrorException(
                  nls.getFormattedMessage(
                    "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                    new Object[] {
                      "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
                      "1:586:1.89.4.1" },
                    null));

                // FFDC
                FFDCFilter.processException(
                  e,
                  "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.AnycastOutputHandler",
                  "1:593:1.89.4.1",
                  this);
                SibTr.exception(tc, e);
                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                new Object[] {
                  "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
                  "1:599:1.89.4.1" });
                throw e;
              }
            } // end if (sinfo != null)
            else
            {
              sinfo = new StreamInfo(streamKey, streamId, null, null);
              sinfo.item = item;
              
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "AnycastOutputHandler AOStartedFlushItem put: " + sinfo + ", using key: " + streamKey);  
              
              streamTable.put(streamKey, sinfo);
            }

          } // end if (abitem instanceof AOStartedFlushItem)
        } // end while
        cursor.finished();

        // now do the remaining initialization of these streams
        boolean toStartFlushAllStreams = false; // set to true only if cardinality one and more than 1 stream has not started flush
        if (getCardinalityOne())
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "AnycastOutputHandler prepare to counting non-flushed streams, table size: " + streamTable.size());
          
          if (streamTable.size() > 1)
          {
            // count the number of streams that have not started flush
            int tempCount = 0;
            Enumeration vEnum = streamTable.elements();
            while (vEnum.hasMoreElements())
            {
              StreamInfo sinfo = (StreamInfo) vEnum.nextElement();
              
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "AnycastOutputHandler counting non-flushed streams: " + sinfo);
              
              if (sinfo.item == null)
                tempCount++;
            }

            if (tempCount > 1)
            {

              // log error. we don't throw an exception since the consumer cardinality may have
              // been changed while the ME was down
              SIErrorException e = new SIErrorException(
                nls.getFormattedMessage(
                  "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                  new Object[] {
                    "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
                    "1:651:1.89.4.1" },
                  null));

              // FFDC
              FFDCFilter.processException(
                e,
                "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.AnycastOutputHandler",
                "1:658:1.89.4.1",
                this);

              SibTr.exception(tc, e);
              SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
                "1:665:1.89.4.1" });

              toStartFlushAllStreams = true;
            }
          }
        }

        Enumeration vEnum = streamTable.elements();
        ArrayList<AOValue> valueTicks = new ArrayList<AOValue>();
        
        while (vEnum.hasMoreElements())
        {
          StreamInfo sinfo = (StreamInfo) vEnum.nextElement();
          
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "AnycastOutputHandler initialising stream: " + sinfo);
          
          boolean startedFlush = (sinfo.item != null);
          if (sinfo.itemStream == null)
          {
            // ERROR: must have found an item but no itemStream
            // log error and throw exception
            SIErrorException e = new SIErrorException(
              nls.getFormattedMessage(
                "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                new Object[] {
                  "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
                  "1:692:1.89.4.1" },
                null));

            // FFDC
            FFDCFilter.processException(
              e,
              "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.AnycastOutputHandler",
              "1:699:1.89.4.1",
              this);

            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
              "1:706:1.89.4.1" });

            throw e;
          }

          // get all the value ticks from the itemStream
          valueTicks.clear();
          cursor = sinfo.itemStream.newNonLockingItemCursor(null);
          AOValue tick;
          while ((tick = (AOValue) cursor.next()) != null)
          {
            valueTicks.add(tick);
          }
          cursor.finished();
                   
          boolean aliasNotFound = false;
          JSConsumerManager streamConsumerManager = null;
          if (!isPubSub)
          {         
            DestinationHandler dest = destinationHandler;
            SIBUuid12 gatheringTargetUuid = sinfo.itemStream.getGatheringTargetDestUuid();
            try
            {
              // If we are performing a remote gather we need to work out where to gather from
              if (gatheringTargetUuid!=null) // If we are an IME/DME...
              {
                // SIB0113 After IME reconstitute we have lost any AIMessageItems that our AOValues
                // point at. We need to hang the list of AOValues off of the reconstituted AIStream
                // so they can be inserted into it when the AIStream is reconstituted.
                ((RemotePtoPSupport)
                ((BaseDestinationHandler)destinationHandler).
                  getPtoPRealization().
                  getRemoteSupport()).reconstituteIMELinks(valueTicks);
                
                if (!gatheringTargetUuid.equals(destinationHandler.getUuid()))
                {
                  // We are a performing a remote gather and the uuid does not match our destination, it must
                  // therefore be an alias. If the alias does not exist we reconstitute using the full destination's
                  // information and then flush the stream 
                  dest = mp.getDestinationManager().getAliasDestination(gatheringTargetUuid, destinationHandler, false);  
                }
                // Lookup the relevant ConsumerManager for this AOStream (This will either be a local consumer dispatcher
                // usually - or a gathering consumer dispatcher for a remote gatherer)
                streamConsumerManager = (JSConsumerManager)
                  dest.chooseConsumerManager(gatheringTargetUuid, null, null);
              }
              else // We are not gathering so need the local CD
                streamConsumerManager = (JSConsumerManager)
                  dest.getLocalPtoPConsumerManager();            
            }
            catch (SIException e)
            {
              // No FFDC code needed

              // TODO : specific error required here
              SibTr.warning(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                new Object[] {
                  "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
                  "1:764:1.89.4.1" });
              
              // Lookup the relevant ConsumerManager for this AOStream (This will be
              // a gathering consumer dispatcher for a remote gatherer)
              streamConsumerManager = (JSConsumerManager)
                dest.chooseConsumerManager(gatheringTargetUuid, null, null);
              
              aliasNotFound = true;                  
            }  
          }
          else
            streamConsumerManager = pubSubConsumerDispatcher;

          if (restartFromStaleBackup)
          {
            // delete all AOValue ticks, unlock the persistent locks,
            // and then remove sinfo.itemStream, and sinfo.item
            deleteAndUnlockPersistentStream(sinfo, valueTicks);

            // send flushed message to RME
            sendFlushed(sinfo.itemStream.getRemoteMEId(), sinfo.itemStream.getGatheringTargetDestUuid(), sinfo.streamId);

          }
          else if (startedFlush && valueTicks.size()==0)
          {
            // this stream is essentially flushed! so don't create it and instead schedule the removal of the
            // persistent information. Note that sinfo will remain in the streamTable with sinfo.stream==null,
            // till the persistent information is removed
            String key = 
              SIMPUtils.getRemoteGetKey(sinfo.itemStream.getRemoteMEId(), sinfo.itemStream.getGatheringTargetDestUuid());
            AsyncUpdate update =
              new RemovePersistentStream(key ,sinfo.streamId, sinfo.itemStream, sinfo.item);
            msUpdateThread.enqueueWork(update);
          }
          else
          {
            
            sinfo.stream = new AOStream(sinfo.itemStream.getRemoteMEId(),
                                        sinfo.itemStream.getGatheringTargetDestUuid(),
                                        sinfo.streamId,
                                        sinfo.itemStream,
                                        this,
                                        msUpdateThread,
                                        false,
                                        valueTicks,
                                        startedFlush,
                                        mp,
                                        this.dmeVersion,
                                        streamConsumerManager);

            if (aliasNotFound || toStartFlushAllStreams)
            {
              // SIB0113... Other than starting with a flushAllStreams...
              // ...We should only end up in here if we are on an IME and the destination/alias
              // has dissapeared. In this situation we want to flush everything out but must
              // first reconstitute the AOStream as this sets up links to the DME msgs so the 
              // chain of ME all flush together
              toFlushStream.add(sinfo.stream);
            }
            else if (getCardinalityOne() && !startedFlush && !toStartFlushAllStreams)
            { 
              // If we are here we must be the ONLY stream that is valid in receiving messages
              // from the queue point (I.e. we are the only stream that hasnt been told to flush).
              // If there were 2 then there would be contention - since we are cardinalityOne, both
              // streams would be told to flush.
              
              // this is an active stream, so try to create an JSRemoteConsumerPoint for its use
              try
              {
                // create an JSRemoteConsumerPoint and provide it to the (only) AOStream
                JSRemoteConsumerPoint aock = new JSRemoteConsumerPoint();
                               
                ConsumableKey ck = (ConsumableKey) streamConsumerManager.attachConsumerPoint(aock, null, new SIBUuid12(), false, false, null);
                // infinite timeout, since don't want to close the ConsumerKey if RME is inactive for
                // a while. Only close this ConsumerKey when start flushing this stream.

                aock.init(sinfo.stream, "", new ConsumableKey[]{ck}, 0L, mp.getAlarmManager(), null);

                sinfo.stream.setConsumerKey(aock);
              }
              catch (Exception e)
              {
                // No FFDC code needed
                // expected, when cardinalityOne.
                SibTr.exception(tc, e);

                // schedule this stream to be flushed.
                toFlushStream.add(sinfo.stream);
              }
            } // end if (getCardinalityOne) ...
  
            sinfo.stream.start(); // start the stream

          } // end else
        } // end while (vEnum.hasMoreElements())

        if (restartFromStaleBackup)
        {
          // empty the streamTable since deleted all the streams
          streamTable.clear();

          // Log message to console saying we have finished flush for anycast DME for this destination
          SibTr.info(
            tc,
            "FLUSH_COMPLETE_CWSIP0451",
            new Object[] { mp.getMessagingEngineName(),
                           destName });
        }

      } // end synchronized (streamTable)
    }
    catch (Exception e)
    {
      // log error and throw exception
      FFDCFilter.processException(e,
        "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.AnycastOutputHandler",
        "1:880:1.89.4.1",
        this);

      // TODO : This needs to be an INTERNAL ERROR
      SibTr.exception(tc, e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "AnycastOutputHandler", e);
      throw e;
    }

    if (toFlushStream != null)
    {
      for (int i=0; i<toFlushStream.size(); i++)
      {
        AOStream stream = (AOStream) toFlushStream.get(i);
        stream.processRequestFlush();
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AnycastOutputHandler", this);
  }
  
  /*
   * This method should only be called when we have a situation where two or more
   * AOProtocolItemStreams exist with the same RemoteMEId. This method will delete
   * these itemstreams assuming that no items exist on them. It should not be 
   * possible to have two (or more) AOProtocolItemStreams pointing to the same
   * remote ME AND both having items on more than one of them.
   */
  private void cleanupPersistentStreams(ItemStream containerItemStream, SIBUuid8 clashingRemoteMeId) throws Exception
  {
    if (tc.isEntryEnabled()) SibTr.entry(tc, "cleanupPersistentStreams", new Object[] {containerItemStream, clashingRemoteMeId} );
   
    NonLockingCursor cursor = containerItemStream.newNonLockingItemStreamCursor(null);
    AbstractItem abitem;
    while ((abitem=cursor.next()) != null)
    {
      if (abitem instanceof AOProtocolItemStream)
      {
        AOProtocolItemStream protocolItemStream = (AOProtocolItemStream) abitem;
        SIBUuid8 remoteMEId = protocolItemStream.getRemoteMEId();
        SIBUuid12 streamId = protocolItemStream.getStreamId();
        SIBUuid12 gatheringTargetDestUuid = protocolItemStream.getGatheringTargetDestUuid();
        
        if (remoteMEId.equals(clashingRemoteMeId))
        {
          // Found a itemstream with the clashing remote ME id
          NonLockingCursor protocolCursor = protocolItemStream.newNonLockingItemCursor(null);
          if (protocolCursor.next() == null)
          {
            String key = 
              SIMPUtils.getRemoteGetKey(remoteMEId, gatheringTargetDestUuid);
            //empty itemstream delete it asynchronously
            AsyncUpdate update = new RemovePersistentStream(key, streamId, protocolItemStream, null);
            msUpdateThread.enqueueWork(update);
            // We want to wait for the asyncupdate to finish as we will be obtaining a new
            // cursor on our containeritemstream soon
            msUpdateThread.waitTillAllUpdatesExecuted();
          }
          else
          {
            // We have some Items on this itemstream, leave it for now it should be the only one that
            //  has some items.
          }
          protocolCursor.finished();
        }
      }
    }
    cursor.finished();
    
    if (tc.isEntryEnabled()) SibTr.exit(tc, "cleanupPersistentStreams");
  }

  /**
   * Cleans up the non-persistent state. No methods on the ControlHandler interface should be called after this is called.
   */
  public void close()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "close");

    Enumeration streams = null;
    synchronized (this)
    {
      synchronized (streamTable)
      {
        closed = true;
        closeBrowserSessionsInternal();
        streams = streamTable.elements();
      }
    }
    // since already set closed to true, no more AOStreams will be created and added to the streamTable,
    // even if there is an
    // asynchronous stream creation in progress (the itemStream will be created, but no AOStream will
    // be added to the streamTable).
    while (streams.hasMoreElements())
    {
      StreamInfo sinfo = (StreamInfo) streams.nextElement();
      if (sinfo.stream != null)
      {
        sinfo.stream.close();
      }
    }

    synchronized (streamTable)
    {
      streamTable.clear();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "close");
  }

  private void closeAndFlush(boolean redriveDeletionThread)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "closeAndFlush", Boolean.valueOf(redriveDeletionThread));

    Hashtable streamTableClone = null;
    synchronized (this)
    {
      synchronized (streamTable)
      {
        if (!startedCloseAndFlush)
        {
          startedCloseAndFlush = true;
          this.redriveDeletionThread = redriveDeletionThread;
          closeBrowserSessionsInternal(); // close all browser sessions

          if (streamTable.size() == 0)
          { // no streams exist and none are being asynchronously created
            finishedCloseAndFlush = true;
            streamTable.notifyAll();
          }
          else
          {
            streamTableClone = (Hashtable) streamTable.clone();
          }
        } // end if (!startedCloseAndFlush)
      }
    }

    if (streamTableClone != null)
    { // tell all these streams to start flushing
      Enumeration e = streamTableClone.elements();
      while (e.hasMoreElements())
      {
        StreamInfo sinfo = (StreamInfo) e.nextElement();
        if (sinfo.stream != null)
          sinfo.stream.processRequestFlush();
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "closeAndFlush");
  }

  /**
   * Cleanup the state in this AnycastOutputHandler. Called when this localisation is being
   * deleted.
   * @param flushStreams If true, need to flush the streams, else do not need to flush all streams. The cleanup
   *    may be asynchronous in the former case.
   * @param redriveDeletionThread If the cleanup is asynchronous, this class needs to do something when the cleanup
   *     terminates. If true, it will redrive the AsynchDeletionThread, else it will itself delete its container item
   *     stream.
   * @return true, if finished cleaning up, else false (will redrive the AsynchDeletionThread when finishes cleaning
   * up
   */
  public boolean cleanup(boolean flushStreams, boolean redriveDeletionThread)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "cleanup",
          new Object[]{Boolean.valueOf(flushStreams), Boolean.valueOf(redriveDeletionThread)});

    boolean retvalue = false;

    // first check if already finishedCloseAndFlush, since this call can be redriven
    synchronized (streamTable)
    {
      if (finishedCloseAndFlush)
      {
        retvalue = true;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "cleanup", Boolean.valueOf(retvalue));
        return retvalue;
      }
    }

    if (!flushStreams)
    {
      // Simply cleanup the non-persistent state. The persistent state will get cleaned up when
      // the caller deletes everything from the AOContainerItemStream
      close();
      retvalue = true;
    }
    else
    {
      // have to flush all the streams
      closeAndFlush(redriveDeletionThread);
      synchronized (streamTable)
      {
        retvalue = finishedCloseAndFlush;
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "cleanup", Boolean.valueOf(retvalue));
    return retvalue;
  }

  public boolean isCloseAndFlushCompleted()
  {
    synchronized (streamTable)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.entry(tc, "isCloseAndFlushCompleted");
        SibTr.exit(tc, "isCloseAndFlushCompleted", Boolean.valueOf(finishedCloseAndFlush));
      }
      return finishedCloseAndFlush;
    }
  }

  public void closeBrowserSessionsInternal()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "closeBrowserSessionsInternal");
    Enumeration e = browserSessionTable.elements();
    while (e.hasMoreElements())
    {
      AOBrowserSession session = (AOBrowserSession) e.nextElement();
      session.close();
    }
    browserSessionTable.clear();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "closeBrowserSessionsInternal");
  }

  public final boolean getCardinalityOne()
  {
    synchronized (cardinalityOneLock)
    {

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.entry(tc, "getCardinalityOne");
        SibTr.exit(tc, "getCardinalityOne", Boolean.valueOf(cardinalityOne));
      }
      return cardinalityOne;
    }
  }

  private final void setCardinalityOne(boolean value)
  {
    synchronized (cardinalityOneLock)
    {
      cardinalityOne = value;
    }
  }
  public final boolean isMEReachable(SIBUuid8 rme)
  {
   if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
     SibTr.entry(tc, "isMEReachable", rme);
   
    MPIO msgTran = mp.getMPIO();
    boolean reachable = msgTran.isMEReachable(rme); 

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "isMEReachable", Boolean.valueOf(reachable));

    return reachable;
  }

  // for unit testing
  public final AsyncUpdateThread getAsyncUpdateThread()
  {
    return msUpdateThread;
  }

  public final AsyncUpdateThread getPersistLockThread()
  {
    return persistLockThread;
  }
  // for unit testing
  public final AOStream getAOStream(String streamKey, SIBUuid12 streamId)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getAOStream", new Object[]{streamKey, streamId});

    StreamInfo streamInfo = getStreamInfo(streamKey, streamId);
    if (streamInfo != null)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getAOStream", streamInfo.stream);
      return streamInfo.stream;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getAOStream", null);
    return null;
  }

  public void handleControlMessage(SIBUuid8 sourceMEUuid, ControlMessage msg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "handleControlMessage", new Object[]{sourceMEUuid, msg});

    SIBUuid8 remoteMEId = msg.getGuaranteedSourceMessagingEngineUUID();
    SIBUuid12 gatheringTargetDestUuid = msg.getGuaranteedGatheringTargetUUID();
    
    ControlMessageType type = msg.getControlMessageType();
    if (type == ControlMessageType.BROWSEGET)
    {
      ControlBrowseGet cmsg = (ControlBrowseGet) msg;
      long browseId = cmsg.getBrowseID();
      String selectorString = cmsg.getFilter();
      String discriminator = cmsg.getControlDiscriminator();
      SelectorDomain domain = SelectorDomain.
                                getSelectorDomain(cmsg.getSelectorDomain());

      SelectionCriteria criteria = mp.getSelectionCriteriaFactory().
                                     createSelectionCriteria(discriminator, selectorString, domain);

      long seqNum = cmsg.getSequenceNumber();
      handleControlBrowseGet(remoteMEId, gatheringTargetDestUuid, browseId, criteria, seqNum);
    }
    else if (type == ControlMessageType.BROWSESTATUS)
    {
      ControlBrowseStatus cmsg = (ControlBrowseStatus) msg;
      long browseId = cmsg.getBrowseID();
      int status = cmsg.getStatus();
      handleControlBrowseStatus(remoteMEId, gatheringTargetDestUuid, browseId, status);
    }
    else if (type == ControlMessageType.CREATESTREAM)
    {
      ControlCreateStream cmsg = (ControlCreateStream) msg;
      long requestId = cmsg.getRequestID();
      SIBUuid12 parent = null;
      // Only set parent if we're handling a durable request
      if (msg.getGuaranteedProtocolType() == ProtocolType.DURABLEOUTPUT)
        parent = msg.getGuaranteedTargetDestinationDefinitionUUID();
      handleControlCreateStream(remoteMEId, gatheringTargetDestUuid, requestId, parent);
    }
    else
    {
      SIBUuid12 streamId = msg.getGuaranteedStreamUUID();

      String streamKey = 
        SIMPUtils.getRemoteGetKey(remoteMEId, gatheringTargetDestUuid);
      
      StreamInfo streamInfo = getStreamInfo(streamKey, streamId);
      if (streamInfo == null)
      {
        // send ControlFlushed message
        sendFlushed(remoteMEId, gatheringTargetDestUuid, streamId);
      }
      else if (streamInfo.stream == null)
      {
        // not yet ready to process messages. ignore this message
      }
      else
      {
        // stream exists and is ready to process messages
        if (type == ControlMessageType.ACCEPT)
        {
          ControlAccept cmsg = (ControlAccept) msg;
          long[] ticks = cmsg.getTick();
          streamInfo.stream.processAccept(ticks);
        }
        else if (type == ControlMessageType.REJECT)
        {
          ControlReject cmsg = (ControlReject) msg;
          long[] startTicks = cmsg.getStartTick();
          long[] endTicks = cmsg.getEndTick();
          long[] unlockCounts = cmsg.getRMEUnlockCount();
          boolean recovery = cmsg.getRecovery();
          streamInfo.stream.processReject(startTicks, endTicks, unlockCounts, recovery);
        }
        else if (type == ControlMessageType.COMPLETED)
        {
          ControlCompleted cmsg = (ControlCompleted) msg;
          long[] startTicks = cmsg.getStartTick();
          long[] endTicks = cmsg.getEndTick();
          streamInfo.stream.processCompleted(startTicks, endTicks);
        }
        else if (type == ControlMessageType.REQUEST)
        {
          ControlRequest cmsg = (ControlRequest) msg;
          String[] discriminators = cmsg.getControlDisciminator();
          String[] filters = cmsg.getFilter();
          int[] selectorDomains = cmsg.getSelectorDomain();

          long[] getTicks = cmsg.getGetTick();
          long[] rejectStartTicks = cmsg.getRejectStartTick();
          long[] timeouts = cmsg.getTimeout();
          streamInfo.stream.processRequest(discriminators, selectorDomains, filters,
                                           rejectStartTicks, getTicks, timeouts);
        }
        else if (type == ControlMessageType.HIGHESTGENERATEDTICK)
        {
          ControlHighestGeneratedTick cmsg  = (ControlHighestGeneratedTick) msg;
          long requestId = cmsg.getRequestID();
          long tick = cmsg.getTick();
          streamInfo.stream.processHighestGeneratedTick(requestId, tick);
        }
        else if (type == ControlMessageType.AREYOUFLUSHED)
        {
          ControlAreYouFlushed cmsg = (ControlAreYouFlushed) msg;
          long requestId = cmsg.getRequestID();
                                        // NOTE: parentDest is null here since this request can never
                                        // originate from a DurableInputHandler
          streamInfo.stream.processAreYouFlushed(requestId, null);
        }
        else if (type == ControlMessageType.REQUESTFLUSH)
        {
          streamInfo.stream.processRequestFlush();
        }
        else if (type == ControlMessageType.RESETREQUESTACKACK)
        {
          ControlResetRequestAckAck cmsg = (ControlResetRequestAckAck) msg;
          long dmeVersion = cmsg.getDMEVersion();
          if (this.dmeVersion == dmeVersion)
            streamInfo.stream.processResetRequestAckAck();
        }
        else if (type == ControlMessageType.REQUESTCARDINALITYINFO)
        {
          // Probably never occurs
        }
        else
        {
          // unknown type, log error
          SIErrorException e = new SIErrorException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0001",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
                "1:1303:1.89.4.1" },
              null));

          // FFDC
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.handleControlMessage",
            "1:1310:1.89.4.1",
            this);

          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
            "1:1316:1.89.4.1" });

          SibTr.exception(tc, e);
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "handleControlMessage", e);
          throw e;
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleControlMessage");
  }
  /**
   * Method to handle a ControlBrowseGet message from an RME
   * @param remoteME The UUID of the RME
   * @param browseId The unique browseId, relative to this RME
   * @param selector The selector, valid only when seqNum=0
   * @param seqNum The cursor position in the browse
   */
  private final void handleControlBrowseGet(SIBUuid8 remoteME, SIBUuid12 gatheringTargetDestUuid, long browseId, SelectionCriteria criteria, long seqNum)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "handleControlBrowseGet",
      new Object[] {remoteME, gatheringTargetDestUuid, Long.valueOf(browseId), criteria, Long.valueOf(seqNum)});

    // first we see if there is an existing AOBrowseSession
    AOBrowserSessionKey key = new AOBrowserSessionKey(remoteME, gatheringTargetDestUuid, browseId);
    AOBrowserSession session;
    synchronized (this)
    {
      if (startedCloseAndFlush)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "handleControlBrowseGet");
        return;
      }
      session = (AOBrowserSession) browserSessionTable.get(key);
    }
    if (session == null)
    { // there is not an existing session
      if (seqNum == 0)
      { // this message indicates a start of session, so create a session

        try
        {
          JSConsumerManager streamConsumerDispatcher = null;         
          if (!isPubSub)
          {
            try
            {
              // If we are performing a remote gather we need to work out where to gather from
              DestinationHandler dest = destinationHandler;
              if (gatheringTargetDestUuid!=null)
              {
                // We are a performing a remote gather. We need to lookup the destination that the gatheringUuid
                // refers to. It may be an alias (used for scoping the MEs), or the real destination we
                // are on (no scoping), or may not exist at all (exception thrown and caught below) 
                dest = mp.getDestinationManager().getDestination(gatheringTargetDestUuid, true);  
              }
            
              // Lookup the relevant ConsumerManager for this AOStream (This will either be a local consumer dispatcher
              // usually - or a gathering consumer dispatcher for a remote gatherer)
              streamConsumerDispatcher = (JSConsumerManager)
                dest.chooseConsumerManager(gatheringTargetDestUuid, null, null);             
            }
            catch (SIException e)
            {
              // FFDC
              // Couldnt locate the gathering target MEs so flush the stream                   
              FFDCFilter.processException(
                e,
                "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.handleControlBrowseGet",
                "1:1388:1.89.4.1",
                this);
  
              SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                new Object[] {
                  "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
                  "1:1394:1.89.4.1" });
              
              throw e;                    
            } 
          }
          else
            streamConsumerDispatcher = pubSubConsumerDispatcher;
          
          BrowseCursor browseCursor = ((Browsable) streamConsumerDispatcher).getBrowseCursor(criteria);

          // next, create the session, and add it to the table
          session = new AOBrowserSession(this,
                                         browseCursor,
                                         remoteME,
                                         gatheringTargetDestUuid,
                                         browseId,
                                         mp.getAlarmManager());
          key = session.getKey();
          AOBrowserSession existing = (AOBrowserSession) browserSessionTable.put(key, session);
          if (existing != null)
          {
            // log error (probable bug since two BrowseGets with same browseId, remoteME and seqNum==0
            //  must have been received)
            SIErrorException e = new SIErrorException(
              nls.getFormattedMessage(
                "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                new Object[] {
                  "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
                  "1:1422:1.89.4.1" },
                null));

            // FFDC
            FFDCFilter.processException(
              e,
              "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.handleControlBrowseGet",
              "1:1429:1.89.4.1",
              this);

            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
              "1:1436:1.89.4.1" });

            existing.close();
          }
        }
        catch (SISelectorSyntaxException e1)
        {
          FFDCFilter.processException(e1,
            "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.handleControlBrowseGet","1:1188:1.77",this);
          SibTr.exception(tc, e1);

          sendBrowseEnd(remoteME, gatheringTargetDestUuid, browseId,
            SIMPConstants.BROWSE_BAD_FILTER);
          session = null;
        }
        catch (SIException e1)
        {
          FFDCFilter.processException(e1,
            "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.handleControlBrowseGet","1:1454:1.89.4.1",this);
          SibTr.exception(tc, e1);

          sendBrowseEnd(remoteME, gatheringTargetDestUuid, browseId,
            SIMPConstants.BROWSE_STORE_EXCEPTION);
          session = null;
        }//end catch

      }//end if (seqNum == 0)
      else
      {
        // this ControlBrowseGet is out-of-order.
        //Return error to the remote ME
        sendBrowseEnd(remoteME, gatheringTargetDestUuid, browseId,
          SIMPConstants.BROWSE_OUT_OF_ORDER);
      }
    }
    // process the message if session exists/created
    if (session != null)
    {
      boolean closed = session.next(seqNum);
      if (closed)
      {
        browserSessionTable.remove(key);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleControlBrowseGet");

  }

  /**
   * Method to handle a ControlBrowseStatus message from an RME
   * @param remoteME The UUID of the RME
   * @param browseId The unique browseId, relative to this RME
   * @param status The status
   */
  private final void handleControlBrowseStatus(SIBUuid8 remoteME, SIBUuid12 gatheringTargetDestUuid, long browseId, int status)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "handleControlBrowseStatus",
      new Object[] {remoteME, gatheringTargetDestUuid, Long.valueOf(browseId), Integer.valueOf(status)});

    // first we see if there is an existing AOBrowseSession
    AOBrowserSessionKey key = new AOBrowserSessionKey(remoteME, gatheringTargetDestUuid, browseId);
    AOBrowserSession session = (AOBrowserSession) browserSessionTable.get(key);
    if (session != null)
    {
      if (status == SIMPConstants.BROWSE_CLOSE)
      {
        session.close();
        browserSessionTable.remove(key);
      }
      else if (status == SIMPConstants.BROWSE_ALIVE)
      {
        session.keepAlive();
      }
    }
    else
    { // session == null. ignore the status message
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleControlBrowseStatus");

  }

  /**
   * Remove an AOBrowserSession that is already closed
   * @param key The key of the session
   */
  public final void removeBrowserSession(AOBrowserSessionKey key)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeBrowserSession", key);

    browserSessionTable.remove(key);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeBrowserSession");

  }


  private final void handleControlCreateStream(SIBUuid8 remoteMEId, SIBUuid12 gatheringTargetDestUuid, long requestId, SIBUuid12 parentDest)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "handleControlCreateStream", new Object[]{remoteMEId, gatheringTargetDestUuid, Long.valueOf(requestId), parentDest});

    StreamInfo sinfo = null;
    boolean sendControlCardinality = false; // should we send a ControlCardinality message
    boolean createStreamSuccess = true;
    synchronized (streamTable)
    {
      if (!startedCloseAndFlush)
      {      
        /**
         * The key we use to lookup/insert a streamInfo object is based off the remoteMEuuid +
         * the gatheringTargetDestUuid. This second value is null for standard consumer and set
         * to a destinationUuid (which could be an alias) for gathering consumers. In this way
         * we have seperate streams per consumer type.
         */
        String streamTableKey = 
            SIMPUtils.getRemoteGetKey(remoteMEId, gatheringTargetDestUuid);
        
        sinfo = streamTable.get(streamTableKey);
        if (sinfo == null)
        { // need to create a stream
  
          JSRemoteConsumerPoint aock = null;
          ConsumableKey ck = null;        
          JSConsumerManager streamConsumerDispatcher = null;
          if (!isPubSub)
          {
            try
            {
              // If we are performing a remote gather we need to work out where to gather from
              DestinationHandler dest = destinationHandler;
              if (gatheringTargetDestUuid!=null)
              {
                // We are a performing a remote gather. We need to lookup the destination that the gatheringUuid
                // refers to. It may be an alias (used for scoping the MEs), or the real destination we
                // are on (no scoping), or may not exist at all (exception thrown and caught below) 
                dest = mp.getDestinationManager().getDestinationByUuid(gatheringTargetDestUuid, true);  
              }
            
              // Lookup the relevant ConsumerManager for this AOStream (This will either be a local consumer dispatcher
              // usually - or a gathering consumer dispatcher for a remote gatherer)
              streamConsumerDispatcher = (JSConsumerManager)
                dest.chooseConsumerManager(gatheringTargetDestUuid, null, null);
            }
            catch (SIException e)
            {
              // FFDC
              // Couldnt locate the gathering target MEs so flush the stream                   
              FFDCFilter.processException(
                e,
                "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.handleControlCreateStream",
                "1:1593:1.89.4.1",
                this);
  
              SibTr.exception(tc, e);
              
              createStreamSuccess = false;                             
            } 
          }
          else
            streamConsumerDispatcher = pubSubConsumerDispatcher;
          
          if (createStreamSuccess)
          {
            // Need to check if cardinalityOne is violated
            if (getCardinalityOne())
            {
              // We are only allowed to connect one consumer to the local queue point so if this 
              // remote consumer wants to do it we throw it out if there are existing ones.
              // This is checked inside attachConsumerPoint on the local ConsumerDispatcher
              
              try
              {
                aock = new JSRemoteConsumerPoint();
                          
                // The following will throw an exception if existing consumers on the local queue point exist
                ck = (ConsumableKey) streamConsumerDispatcher.attachConsumerPoint(aock, null, new SIBUuid12(), false, false, null);
              }
              catch (Exception e)
              {
                // No FFDC code needed
                sendControlCardinality = true;
                // expected. don't throw the exception
                SibTr.exception(tc, e);
              }
            }
    
            if (!sendControlCardinality)
            { // cardinality is ok, so we can create the stream
    
              SIBUuid12 streamId = generateUniqueStreamName();
              StreamInfo sinfo2 = new StreamInfo(streamTableKey, streamId, null, null); // the placeholder
              streamTable.put(streamTableKey, sinfo2);
              // now asynchronously create the rest
              try
              {
                CreatePersistentStream update = new CreatePersistentStream(remoteMEId, gatheringTargetDestUuid, streamId, requestId, aock, ck, parentDest, destinationHandler, streamConsumerDispatcher);
                msUpdateThread.enqueueWork(update);
              }
              catch (Exception e)
              {
                // FFDC
                FFDCFilter.processException(e,
                  "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.handleControlCreateStream",
                  "1:1646:1.89.4.1",this);
                SibTr.exception(tc, e);
    
                //  remove this stream from the table
                streamTable.remove(streamTableKey);
                
                createStreamSuccess = false;
                
                // detach the ConsumerKey if not null
                try
                {
                  if (ck != null)
                    ck.detach();
                }
                catch (Exception e2)
                {
                  // FFDC
                  FFDCFilter.processException(e2,
                    "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.handleControlCreateStream",
                    "1:1665:1.89.4.1",this);
                  SibTr.exception(tc, e2);
                }
              }
            }
          }
        }
      }
      else
        createStreamSuccess = false;
    } // end synchronized (this)
    
    if ((sinfo != null) && (sinfo.stream != null))
    { // stream already exists and is fully initialized
      // pretend as if AreYouFlushed request received, since want it to send a NotFlushed reply
      sinfo.stream.processAreYouFlushed(requestId, parentDest);
    }
    
    if (sendControlCardinality && createStreamSuccess)
    {
      sendCardinalityInfo(remoteMEId, gatheringTargetDestUuid, requestId, 1, parentDest);
    }
    
    if (!createStreamSuccess)
    {
      // Bit of a hack - We need to reject the waiting createStream request and the only
      // way to do that is with a cardinalityInfo msg - We set the cardinality to 0 to show
      // we dont actually have any consumers attached (or in other words, that we dont care
      // how many consumers are attached)
      sendCardinalityInfo(remoteMEId, gatheringTargetDestUuid, requestId, 0, parentDest);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleControlCreateStream");
  }

  /**
   * Class to asynchronously persist the lock and tick information for a message
   */
  public static abstract class AsyncUpdateWithRetry extends AsyncUpdate
  {

    private int repetitionCount;
    private final long repetitionThreshold;
    private final AsyncUpdateThread msUpdateThread;
    private final String destName;

    public AsyncUpdateWithRetry(long repetitionThreshold, AsyncUpdateThread msUpdateThread, String destName)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "AsyncUpdateWithRetry",
          new Object[] { Long.valueOf(repetitionThreshold), msUpdateThread, destName });

      this.repetitionThreshold = repetitionThreshold;
      this.msUpdateThread = msUpdateThread;
      this.repetitionCount = 1;
      this.destName = destName;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "AsyncUpdateWithRetry", this);
    }

    public void rolledback(Throwable e)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "rolledback", e);

      rolledbackRetry(e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "rolledback");
    }

    /**
     * @return true if retrying else false
     */
    public boolean rolledbackRetry(Throwable e)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "rolledbackRetry", new Object[] {this, e});

      boolean retvalue = true;
      // log error
      FFDCFilter.processException(e,"com.ibm.ws.sib.processor.impl.AnycastOutputHandler.AsyncUpdateWithRetry.rolledbackRetry",
          "1:1748:1.89.4.1",this);
      SibTr.exception(tc, e);

      // try again!! If we don't try again, and instead change the tick to completed, ordering
      // could be violated since a later value tick assignment may have successfully committed.
      // message needs to be persistently locked, and tick persisted
      repetitionCount++;
      if (repetitionThreshold != SIMPConstants.INFINITE_TIMEOUT
          && repetitionCount > repetitionThreshold)
      {
        retvalue = false;
        // log that giving up
        Exception e2 = new Exception(nls.getFormattedMessage("MSGSTORE_STOP_RETRY_CWSIP0457",
                                    new Object[]{destName, Long.valueOf(repetitionThreshold)},
                                    null));

        FFDCFilter.processException(e2,"com.ibm.ws.sib.processor.impl.AnycastOutputHandler.AsyncUpdateWithRetry.rolledbackRetry",
            "1:1765:1.89.4.1",this);
        SibTr.exception(tc, e2);
      }
      else
      {
        try
        {
          msUpdateThread.enqueueWork(this);
        }
        catch (ClosedException e2)
        {
          FFDCFilter.processException(e2,"com.ibm.ws.sib.processor.impl.AnycastOutputHandler.AsyncUpdateWithRetry.rolledbackRetry",
              "1:1777:1.89.4.1",this);

          // giving up, therefore FFDC
          retvalue = false;
          Exception e3 = new Exception(nls.getFormattedMessage("MSGSTORE_STOP_RETRY_CWSIP0457",
                                      new Object[]{destName, Integer.valueOf(repetitionCount - 1)},
                                      null));
          SibTr.exception(tc, e3);
        }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "rolledbackRetry");

      return retvalue;
    }
  }

  /**
   * Asynchronously creates a persistent protocol stream
   */
  class CreatePersistentStream extends AsyncUpdateWithRetry
  {
    final SIBUuid8 remoteMEId;
    final SIBUuid12 gatheringTargetDestUuid;
    final SIBUuid12 streamId;
    final String streamKey;
    final long requestId;
    final AOProtocolItemStream itemStream;
    final JSRemoteConsumerPoint aock; // not null when consumerCardinality=1
    final ConsumableKey ck; // not null when consumerCardinality=1
    final JSConsumerManager cd;
    final SIBUuid12 parentDest;

    CreatePersistentStream(SIBUuid8 remoteMEId, SIBUuid12 gatheringTargetDestUuid, SIBUuid12 streamId, long requestId,
        JSRemoteConsumerPoint aock, ConsumableKey ck, SIBUuid12 parentDest, DestinationHandler destinationHandler, JSConsumerManager cd)
    {
      super(SIMPConstants.MS_WRITE_REPETITION_THRESHOLD, msUpdateThread, destName);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "CreatePersistentStream",
          new Object[] { remoteMEId, gatheringTargetDestUuid, streamId, Long.valueOf(requestId), aock, ck, parentDest, destinationHandler,cd });


      this.remoteMEId = remoteMEId;
      this.gatheringTargetDestUuid = gatheringTargetDestUuid;
      this.streamId = streamId;
      this.streamKey = SIMPUtils.getRemoteGetKey(remoteMEId, gatheringTargetDestUuid);
      this.requestId = requestId;
      this.itemStream = new AOProtocolItemStream(remoteMEId, gatheringTargetDestUuid, streamId, destinationHandler);
      this.aock = aock;
      this.ck = ck;
      this.cd = cd;
      this.parentDest = parentDest;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "CreatePersistentStream", this);
    }

    public void execute(TransactionCommon t) throws Throwable
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "execute");

      Transaction msTran = mp.resolveAndEnlistMsgStoreTransaction(t);
      containerItemStream.addItemStream(itemStream, msTran);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "execute");
    }

    public void rolledback(Throwable e)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "rolledback", e);

      boolean isRetrying = rolledbackRetry(e);
      if (!isRetrying)
      {
        // remove the placeholder from the streamTable
        synchronized (streamTable)
        {
          streamTable.remove(streamKey);
        }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "rolledback");
    }

    public void committed() throws SIResourceException, SINotPossibleInCurrentConfigurationException
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "committed");

      try
      {
        // create an AOStream object, and then the initialized StreamInfo object
        AOStream stream = new AOStream(remoteMEId,
                                       gatheringTargetDestUuid,
                                       streamId,
                                       itemStream,
                                       AnycastOutputHandler.this,
                                       msUpdateThread,
                                       true,
                                       null,
                                       false,
                                       mp,
                                       dmeVersion,
                                       cd);
        StreamInfo sinfo = new StreamInfo(streamKey, streamId, stream, itemStream);

        // finish initialization of aock, if non-null
        if (aock != null)
        {
          // infinite timeout, since don't want to close the ConsumerKey if RME is inactive for
          // a while. Only close this ConsumerKey when start flushing this stream.
          aock.init(stream, "", new ConsumableKey[]{ck}, 0L, mp.getAlarmManager(), null);

          stream.setConsumerKey(aock);
        }

        boolean startFlushing;
        synchronized (streamTable)
        {
          if (closed)
          { // this callback can occur after the AOH is closed. We don't want to initialize any non-persistent
            // stream state in this case.
            stream.close();
            stream = null;
            if (ck != null)
              ck.detach();
          }
          else
          {
            // replace the placeholder in streamTable
            StreamInfo sinfo2 = streamTable.put(streamKey, sinfo);
            if (sinfo2.stream != null)
            { // should have been the placeholder
              // return it back to the original state
              streamTable.put(streamKey, sinfo);
              // throw ProbableBugException
              SIErrorException e = new SIErrorException(
                nls.getFormattedMessage(
                  "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                  new Object[] {
                    "com.ibm.ws.sib.processor.impl.CreatePersistentStream",
                    "1:1924:1.89.4.1" },
                  null));

              SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.CreatePersistentStream",
                "1:1930:1.89.4.1" });
              throw e;
            }
          }
          startFlushing = startedCloseAndFlush; // read it in the synchronized block
        } // end synchronized (streamTable)
        if (stream != null)
        {
          stream.start();
          if (startFlushing)
            stream.processRequestFlush();
          else
            stream.processAreYouFlushed(requestId, parentDest); // so that stream sends NotFlushed message to RME
        }
      }
      catch (Exception e)
      {
        FFDCFilter.processException(e,
          "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.CreatePersistentStream.committed",
          "1:1949:1.89.4.1",this);
        SibTr.exception(tc, e);
        // schedule RemovePersistentStrean
        try
        {
          if (ck != null)
          {
            ck.detach();
          }
          AsyncUpdate update =
            new RemovePersistentStream(streamKey, streamId, itemStream, null);
          msUpdateThread.enqueueWork(update);
        }
        catch (ClosedException e1)
        {
          // No FFDC code needed
          // ignore
        }
/*        catch (SIException e1)
        {
          // FFDC
          FFDCFilter.processException(e1,
                "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.CreatePersistentStream.committed",
                PROBE_ID_350,this);
          SibTr.exception(tc, e1);
        }*/
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "committed");
    }
  } // end inner class CreatePersistentStream

  /**
   * Helper method used to dispatch a message received for a particular stream.
   * Handles its own synchronization
   * @param remoteMEId
   * @param streamId
   * @return the StreamInfo, if there is one that matches the parameters, else null
   */
  private final StreamInfo getStreamInfo(String streamKey, SIBUuid12 streamId)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getStreamInfo", new Object[]{streamKey, streamId});
    StreamInfo sinfo = streamTable.get(streamKey);
    if ((sinfo != null) && sinfo.streamId.equals(streamId))
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getStreamInfo", sinfo);
      return sinfo;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getStreamInfo", null);
    return null;
  }

  /**
   * Callback from a stream, that it has been flushed
   * @param stream
   */
  public final void streamIsFlushed(AOStream stream)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "streamIsFlushed", stream);

    // we schedule an asynchronous removal of the persistent data
    synchronized (streamTable)
    {
      String key = SIMPUtils.getRemoteGetKey(stream.getRemoteMEUuid(), stream.getGatheringTargetDestUuid());
      StreamInfo sinfo = streamTable.get(key);
      if ((sinfo != null) && sinfo.streamId.equals(stream.streamId))
      {
        RemovePersistentStream update = null;
        synchronized (sinfo)
        { // synchronized since reading sinfo.item
          update = new RemovePersistentStream(key, sinfo.streamId, sinfo.itemStream, sinfo.item);
        }
        doEnqueueWork(update);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "streamIsFlushed");
  }

  /**
   * Asynchronously removed the persistent state for a protocol stream
   */
  class RemovePersistentStream extends AsyncUpdateWithRetry
  {
    final String streamKey;
    final SIBUuid12 streamId;
    final ItemStream itemStream;
    final Item item;
    RemovePersistentStream(String streamKey, SIBUuid12 streamId, ItemStream itemStream, Item item)
    {
      super(SIMPConstants.MS_WRITE_REPETITION_THRESHOLD, msUpdateThread, destName);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "RemovePersistentStream",
          new Object[]{streamKey,streamId, itemStream, item});

      this.streamKey = streamKey;
      this.streamId = streamId;
      this.itemStream = itemStream;
      this.item = item;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "RemovePersistentStream", this);
    }
    public void execute(TransactionCommon t) throws Throwable
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "execute", t);

      Transaction msTran = mp.resolveAndEnlistMsgStoreTransaction(t);

      // item can be null when some serious error caused the stream to be removed
      if (item != null)
      {
        item.lockItemIfAvailable(controlItemLockID);
        item.remove(msTran, controlItemLockID);
      }
      itemStream.lockItemIfAvailable(controlItemLockID);
      itemStream.remove(msTran, controlItemLockID);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "execute");
    }

    public void committed()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "committed");

      // now we remove the stream from the streamTable
      // NOTE: we do this after the removal of persistent storage instead of before since the latter can
      // cause two AOProtocolItemStreams for the same remoteMEId to be in the containerItemStream, and we
      // want to ensure that never happens.
      synchronized (streamTable)
      {
        StreamInfo sinfo = streamTable.get(streamKey);
        if ((sinfo != null) && sinfo.streamId.equals(streamId))
        {
          streamTable.remove(streamKey);
          if (startedCloseAndFlush)
          {
            if (streamTable.size() == 0)
            { // no streams exist and none are being asynchronously created
              finishedCloseAndFlush = true;
              if (redriveDeletionThread)
              {
                mp.getDestinationManager().startAsynchDeletion();
              }
              else
              {
                // delete the containerItemStream here
                try
                {
                  LocalTransaction siTran = getLocalTransaction();
                  ((SIMPItemStream) containerItemStream).removeAll((Transaction) siTran);
                  siTran.commit();
                }
                catch (Exception e)
                {
                  FFDCFilter.processException(e,
                    "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.RemovePersistentStream.committed",
                    "1:2115:1.89.4.1",
                    this);
                  SibTr.exception(tc, e);
                }
              } // end else
            } // end if (streamTable.size() == 0)
          } // end if startedCloseAndFlush
        } // end if ((sinfo != null) && sinfo.streamId.equals(streamId))
      } // end synchronized (streamTable)

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "committed");
    }
  }

  private final void initializeControlMessage(ControlMessage msg, SIBUuid8 remoteMEId, SIBUuid12 gatheringTargetDestUuid, SIBUuid12 streamId)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "initializeControlMessage",
        new Object[]{msg, remoteMEId, streamId});

    msg.setPriority(SIMPConstants.CONTROL_MESSAGE_PRIORITY);
    msg.setReliability(SIMPConstants.CONTROL_MESSAGE_RELIABILITY);
    
    SIMPUtils.setGuaranteedDeliveryProperties(msg,
        mp.getMessagingEngineUuid(), 
        remoteMEId,
        streamId,
        gatheringTargetDestUuid,
        destUuid,
        ProtocolType.ANYCASTINPUT,
        GDConfig.PROTOCOL_VERSION);  

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "initializeControlMessage");
  }

  public final void sendBrowseData(JsMessage msg,
                                   SIBUuid8 remoteME,
                                   SIBUuid12 gatheringTargetDestUuid,
                                   SIBUuid8 remoteMEId,
                                   long browseId,
                                   long seqNum)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendBrowseData",
      new Object[] { msg, remoteME, gatheringTargetDestUuid, remoteMEId, Long.valueOf(browseId), Long.valueOf(seqNum)});

    try
    {

      //make a copy of the jsMsg
      JsMessage jsMsg = msg.getReceived();

      jsMsg.clearGuaranteedRemoteGet();
      
      SIMPUtils.setGuaranteedDeliveryProperties(jsMsg,
          mp.getMessagingEngineUuid(), 
          remoteMEId,
          null,
          gatheringTargetDestUuid,
          destUuid,
          ProtocolType.ANYCASTINPUT,
          GDConfig.PROTOCOL_VERSION); 

      jsMsg.setGuaranteedRemoteBrowseID(browseId);
      jsMsg.setGuaranteedRemoteBrowseSequenceNumber(seqNum);

      // Send message
      sendToMe(remoteME,1,jsMsg.getPriority().intValue(),jsMsg);
    }
    catch (MessageCopyFailedException e)
    {
      FFDCFilter.processException(e,
        "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.sendBrowseData",
        "1:2189:1.89.4.1",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendBrowseData");
  }

  public final void sendBrowseEnd(SIBUuid8 remoteME, SIBUuid12 gatheringTargetDestUuid, long browseId, int errorCode)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendBrowseEnd",new Object[] {remoteME, gatheringTargetDestUuid, Long.valueOf(browseId), Integer.valueOf(errorCode)});
    try
    {
      ControlMessageFactory cmf;
      ControlBrowseEnd msg;
      // Create message
      cmf = MessageProcessor.getControlMessageFactory();
      msg = cmf.createNewControlBrowseEnd();
      initializeControlMessage(msg, remoteME, gatheringTargetDestUuid, null);

      msg.setBrowseID(browseId);
      msg.setExceptionCode(errorCode);

      // Send message
      sendToMe(remoteME, 1,
        SIMPConstants.CONTROL_MESSAGE_PRIORITY, msg);
    }
    catch (MessageCreateFailedException e)
    {
      FFDCFilter.processException(e,
        "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.sendBrowseEnd",
        "1:2222:1.89.4.1",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendBrowseEnd");
  }

  public final void sendRequestHighestGeneratedTick(SIBUuid8 remoteME, SIBUuid12 gatheringTargetDestUuid, SIBUuid12 streamId, long requestId)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendRequestHighestGeneratedTick", new Object[] {remoteME, gatheringTargetDestUuid, streamId, Long.valueOf(requestId)});
    try
    {
      ControlMessageFactory cmf;
      ControlRequestHighestGeneratedTick msg;
      // Create message
      cmf = MessageProcessor.getControlMessageFactory();
      msg = cmf.createNewControlRequestHighestGeneratedTick();
      initializeControlMessage(msg, remoteME, gatheringTargetDestUuid, streamId);

      msg.setRequestID(requestId);

      // Send message
      sendToMe(remoteME, 1,
        SIMPConstants.CONTROL_MESSAGE_PRIORITY, msg);
    }
    catch (MessageCreateFailedException e)
    {
      FFDCFilter.processException(e,
        "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.sendRequestHighestGeneratedTick",
        "1:2254:1.89.4.1",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendRequestHighestGeneratedTick");

  }

  public final void sendRemoteGetData(SIMPMessage msg,
                                      SIBUuid8 remoteMEId,
                                      SIBUuid12 gatheringTargetDestUuid,
                                      SIBUuid12 streamId,
                                      long prevTick,
                                      long startTick,
                                      long tick,
                                      long waitTime)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendRemoteGetData",
        new Object[] { msg,
                       remoteMEId,
                       gatheringTargetDestUuid,
                       streamId,
                       Long.valueOf(prevTick),
                       Long.valueOf(startTick),
                       Long.valueOf(tick),
                       Long.valueOf(waitTime) });

    // Retrieve the JsMessage from the root message but take a lazy copy
    // of it before we start modifying it (defect 517468) just in case we're
    // sharing it with someone else.
    JsMessage jsMsg = null;
    try
    {
      jsMsg = msg.getMessage().getReceived();
    }
    catch (MessageCopyFailedException e)
    {
      FFDCFilter.processException(e,
           "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.sendRemoteGetData",
           "1:2296:1.89.4.1",
           this);
      SibTr.exception(tc, e);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "sendRemoteGetData", "MessageCopyFailedException");

      // Not much else we can do but throw a runtime exception as we should never
      // have a problem here
      throw new SIErrorException(e);
    }
    
    jsMsg.clearGuaranteedRemoteBrowse();
    
    SIMPUtils.setGuaranteedDeliveryProperties(jsMsg,
        mp.getMessagingEngineUuid(), 
        remoteMEId,
        streamId,
        gatheringTargetDestUuid,
        destUuid,
        ProtocolType.ANYCASTINPUT,
        GDConfig.PROTOCOL_VERSION); 

    jsMsg.setRedeliveredCount(msg.guessRedeliveredCount());

    jsMsg.setGuaranteedRemoteGetPrevTick(prevTick);
    jsMsg.setGuaranteedRemoteGetStartTick(startTick);
    jsMsg.setGuaranteedRemoteGetValueTick(tick);
    jsMsg.setGuaranteedRemoteGetWaitTime(waitTime);

    // Update the message wait time so that the time spent waiting in this message
    // engine is available to remote message engines gathering consumers

    jsMsg.setMessageWaitTime(msg.updateStatisticsMessageWaitTime());

    // Send message
    sendToMe(remoteMEId,1,jsMsg.getPriority().intValue(),jsMsg);
    
    if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())    
      SibTr.debug(UserTrace.tc_mt,       
         nls_mt.getFormattedMessage(
         "REMOTE_MESSAGE_SENT_CWSJU0031",
         new Object[] {
           getDestName(),
           mp.getMessagingEngineUuid(),
           remoteMEId,
           gatheringTargetDestUuid},
         null));

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendRemoteGetData");
  }

  public final void sendCompleted(SIBUuid8 remoteME,
                                  SIBUuid12 gatheringTargetDestUuid,
                                  SIBUuid12 streamId,
                                  List tickRangeList)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendCompleted", new Object[] {remoteME, gatheringTargetDestUuid, streamId, tickRangeList});

    long[] startTicks, endTicks;
    startTicks = new long[tickRangeList.size()];
    endTicks = new long[tickRangeList.size()];
    for (int i=0; i < startTicks.length; i++)
    {
      TickRange r = (TickRange) tickRangeList.get(i);
      startTicks[i] = r.startstamp;
      endTicks[i] = r.endstamp;
    }

    sendCompletedInternal(remoteME, gatheringTargetDestUuid, streamId, startTicks, endTicks);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendCompleted");
  }

  public final void sendCompleted(SIBUuid8 remoteME, SIBUuid12 gatheringTargetDestUuid, SIBUuid12 streamId, TickRange r)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendCompleted", new Object[] {remoteME, gatheringTargetDestUuid, streamId, r});

    long[] startTicks, endTicks;
    startTicks = new long[1];
    endTicks = new long[1];
    startTicks[0] = r.startstamp;
    endTicks[0] = r.endstamp;

    sendCompletedInternal(remoteME, gatheringTargetDestUuid, streamId, startTicks, endTicks);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendCompleted");
  }

  public final void sendCompletedInternal(SIBUuid8 remoteME, SIBUuid12 gatheringTargetDestUuid,
    SIBUuid12 streamId, long[] startTicks, long[] endTicks)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendCompletedInternal", new Object[] {remoteME, gatheringTargetDestUuid, streamId, Arrays.toString(startTicks), Arrays.toString(endTicks)});
    try
    {
      ControlMessageFactory cmf;
      ControlCompleted msg;
      // Create message
      cmf = MessageProcessor.getControlMessageFactory();
      msg = cmf.createNewControlCompleted();
      initializeControlMessage(msg, remoteME, gatheringTargetDestUuid, streamId);

      msg.setStartTick(startTicks);
      msg.setEndTick(endTicks);

      // Send message
      sendToMe(remoteME, 1,
        SIMPConstants.CONTROL_MESSAGE_PRIORITY, msg);
    }
    catch (MessageCreateFailedException e)
    {
      FFDCFilter.processException(e,
        "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.sendCompletedInternal",
        "1:2415:1.89.4.1",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendCompletedInternal");
  }

  /**
   *
   * @param remoteME
   * @param streamId
   * @param valueTicks Contains AOValue ticks
   */
  public final void sendDecisionExpected(SIBUuid8 remoteME,
                                         SIBUuid12 gatheringTargetDestUuid,
                                         SIBUuid12 streamId,
                                         List valueTicks)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendDecisionExpected", new Object[] {remoteME, gatheringTargetDestUuid, streamId, valueTicks});
    try
    {
      ControlMessageFactory cmf;
      ControlDecisionExpected msg;
      // Create message
      cmf = MessageProcessor.getControlMessageFactory();
      msg = cmf.createNewControlDecisionExpected();
      initializeControlMessage(msg, remoteME, gatheringTargetDestUuid, streamId);

      long[] ticks = new long[valueTicks.size()];
      for (int i=0; i<ticks.length; i++)
      {
        AOValue value = (AOValue) valueTicks.get(i);
        ticks[i] = value.getTick();
      }
      msg.setTick(ticks);

      // Send message
      sendToMe(remoteME, 1,
        SIMPConstants.CONTROL_MESSAGE_PRIORITY, msg);
    }
    catch (MessageCreateFailedException e)
    {
      FFDCFilter.processException(e,
        "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.sendDecisionExpected",
        "1:2462:1.89.4.1",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendDecisionExpected");
  }

  public final void sendFlushed(SIBUuid8 remoteME, SIBUuid12 gatheringTargetDestUuid, SIBUuid12 streamId)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendFlushed", new Object[] {remoteME, gatheringTargetDestUuid, streamId});
    try
    {
      ControlMessageFactory cmf;
      ControlFlushed msg;
      // Create message
      cmf = MessageProcessor.getControlMessageFactory();
      msg = cmf.createNewControlFlushed();
      initializeControlMessage(msg, remoteME, gatheringTargetDestUuid, streamId);


      // Send message
      sendToMe(remoteME, 1,
        SIMPConstants.CONTROL_MESSAGE_PRIORITY, msg);
    }
    catch (MessageCreateFailedException e)
    {
      FFDCFilter.processException(e,
        "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.sendFlushed",
        "1:2493:1.89.4.1",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendFlushed");
  }

  public final void sendCardinalityInfo(SIBUuid8 remoteMEId, SIBUuid12 gatheringTargetDestUuid, long requestId,
                                        int value, SIBUuid12 parentDest)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendCardinalityInfo", new Object[]{remoteMEId, gatheringTargetDestUuid, Long.valueOf(requestId),Integer.valueOf(value),
                                                          parentDest});
    try
    {
      ControlMessageFactory cmf;
      ControlCardinalityInfo msg;
      // Create message
      cmf = MessageProcessor.getControlMessageFactory();
      msg = cmf.createNewControlCardinalityInfo();
      initializeControlMessage(msg, remoteMEId, gatheringTargetDestUuid, null);

      msg.setRequestID(requestId);
      msg.setCardinality(value);

      // If parentDest is non-null then we're handling a durable request
      if (parentDest != null)
        msg.setGuaranteedProtocolType(ProtocolType.DURABLEINPUT);

      // Send message
      sendToMe(remoteMEId, 1,
        SIMPConstants.CONTROL_MESSAGE_PRIORITY, msg);
    }
    catch (MessageCreateFailedException e)
    {
      FFDCFilter.processException(e,
        "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.sendCardinalityInfo",
        "1:2532:1.89.4.1",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendCardinalityInfo");

  }
  public final void sendNotFlushed(SIBUuid8 remoteME, SIBUuid12 gatheringTargetDestUuid, SIBUuid12 streamId, long requestId,
                                   long completedPrefix, long duplicatePrefix, SIBUuid12 parentDest)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendNotFlushed", new Object[] {remoteME, gatheringTargetDestUuid, streamId, Long.valueOf(requestId), Long.valueOf(completedPrefix), Long.valueOf(duplicatePrefix), parentDest});
    try
    {
      ControlMessageFactory cmf;
      ControlNotFlushed msg;
      // Create message
      cmf = MessageProcessor.getControlMessageFactory();
      msg = cmf.createNewControlNotFlushed();
      initializeControlMessage(msg, remoteME, gatheringTargetDestUuid, streamId);

      msg.setRequestID(requestId);

      // setting the priority and QoS value even though we don't need
      // to, since we want to make these arrays the same length
      int[] junk = new int[1];
      junk[0] = 0;
      msg.setCompletedPrefixPriority(junk);
      msg.setCompletedPrefixQOS(junk);
      msg.setDuplicatePrefixPriority(junk);
      msg.setDuplicatePrefixQOS(junk);
      long[] cp = new long[1];
      cp[0] = completedPrefix;
      msg.setCompletedPrefixTicks(cp);
      long[] dp = new long[1];
      dp[0] = duplicatePrefix;
      msg.setDuplicatePrefixTicks(dp);

      // If parentDest is non-null then we're handling a durable request
      if (parentDest != null)
        msg.setGuaranteedProtocolType(ProtocolType.DURABLEINPUT);

      // Send message
      sendToMe(remoteME, 1,
        SIMPConstants.CONTROL_MESSAGE_PRIORITY, msg);
    }
    catch (MessageCreateFailedException e)
    {
      FFDCFilter.processException(e,
        "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.sendNotFlushed",
        "1:2584:1.89.4.1",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendNotFlushed");
  }

  public final void sendRequestAck(SIBUuid8 remoteME, SIBUuid12 gatheringTargetDestUuid, SIBUuid12 streamId,
    long dmeVersion, long[] ticks)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendRequestAck", new Object[] {remoteME, gatheringTargetDestUuid, streamId, Long.valueOf(dmeVersion), Arrays.toString(ticks)});
    try
    {
      ControlMessageFactory cmf;
      ControlRequestAck msg;
      // Create message
      cmf = MessageProcessor.getControlMessageFactory();
      msg = cmf.createNewControlRequestAck();
      initializeControlMessage(msg, remoteME, gatheringTargetDestUuid, streamId);

      msg.setDMEVersion(dmeVersion);
      msg.setTick(ticks);

      // Send message
      sendToMe(remoteME, 1,
        SIMPConstants.CONTROL_MESSAGE_PRIORITY, msg);
    }
    catch (MessageCreateFailedException e)
    {
      FFDCFilter.processException(e,
        "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.sendRequestAck",
        "1:2618:1.89.4.1",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendRequestAck");
  }

  public final void sendResetRequestAck(SIBUuid8 remoteME, SIBUuid12 gatheringTargetDestUuid, SIBUuid12 streamId, long dmeVersion)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendResetRequestAck", new Object[] {remoteME, gatheringTargetDestUuid, streamId, Long.valueOf(dmeVersion)});
    try
    {
      ControlMessageFactory cmf;
      ControlResetRequestAck msg;
      // Create message
      cmf = MessageProcessor.getControlMessageFactory();
      msg = cmf.createNewControlResetRequestAck();
      initializeControlMessage(msg, remoteME, gatheringTargetDestUuid, streamId);

      msg.setDMEVersion(dmeVersion);

      // Send message
      sendToMe(remoteME, 1,
        SIMPConstants.CONTROL_MESSAGE_PRIORITY, msg);
    }
    catch (MessageCreateFailedException e)
    {
      FFDCFilter.processException(e,
        "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.sendResetRequestAck",
        "1:2650:1.89.4.1",
        this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendResetRequestAck");
  }

  /**
   * Helper method called by the AOStream when to persistently lock a message and create a persistent
   * tick in the protocol stream
   * @param t the transaction
   * @param stream the stream making this call
   * @param tick the tick in the stream
   * @param msg the message to be persistently locked (it is already non-persitently locked)
   * @param waitTime the time for this request to be satisfied
   * @param prevTick the previous tick of the same priority and QoS in the stream
   * @return The item representing the persistent tick
   * @throws Exception
   */
  public final AOValue persistLockAndTick(TransactionCommon t, AOStream stream, long tick,
      SIMPMessage msg, int storagePolicy, long waitTime, long prevTick) throws Exception
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "persistLockAndTick", 
          new Object[] {t,
                        stream,
                        Long.valueOf(tick),
                        msg,
                        Integer.valueOf(storagePolicy),
                        Long.valueOf(waitTime),
                        Long.valueOf(prevTick)});

    AOValue retvalue = null;
    try
    {
      Transaction msTran = mp.resolveAndEnlistMsgStoreTransaction(t);
      msg.persistLock(msTran);
      long plockId = msg.getLockID();
      retvalue = new AOValue(tick, msg, msg.getID(), storagePolicy,
        plockId, waitTime, prevTick);
      stream.itemStream.addItem(retvalue, msTran);
    }
    catch (Exception e)
    {
      // No FFDC code needed
      retvalue = null;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "persistLockAndTick", e);
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "persistLockAndTick", retvalue);

    return retvalue;
  }

  /**
   * Helper method called by the AOStream when a persistent tick representing a persistently locked
   * message should be removed since we are flushing or cleaning up state.
   * @param t the transaction
   * @param sinfo the stream this msgs is on
   * @param storedTick the persistent tick
   * @throws SIResourceException
   * @throws Exception
   */
  public final void cleanupTicks(StreamInfo sinfo, TransactionCommon t, ArrayList valueTicks) throws MessageStoreException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "cleanupTicks", new Object[]{sinfo, t, valueTicks});

    try {

      int length = valueTicks.size();
      for (int i=0; i<length; i++)
      {
        AOValue storedTick = (AOValue) valueTicks.get(i);
        
        // If we are here then we do not know which consumerDispatcher originally
        // persistently locked the message. We therefore have to use the meUuid in 
        // the AOValue to find/reconstitute the consumerDispatcher associated with it. This
        // potentially involves creating AIHs which is not ideal.
        
        ConsumerDispatcher cd = null;
        if (storedTick.getSourceMEUuid()==null || 
            storedTick.getSourceMEUuid().equals(getMessageProcessor().getMessagingEngineUuid()))
        {
          cd = (ConsumerDispatcher)destinationHandler.getLocalPtoPConsumerManager();
        }
        else
        {
          AnycastInputHandler aih = 
            destinationHandler.getAnycastInputHandler(storedTick.getSourceMEUuid(), null, true);
          
          cd = aih.getRCD();
        }
        
        SIMPMessage msg = null;
        synchronized(storedTick)
        {          
          msg = (SIMPMessage) cd.getMessageByValue(storedTick);
          if (msg == null)
          {
            storedTick.setToBeFlushed();
          }
        }
  
        Transaction msTran = mp.resolveAndEnlistMsgStoreTransaction(t);
        if (msg!=null && msg.getLockID()==storedTick.getPLockId())
          msg.unlockMsg(storedTick.getPLockId(), msTran, true);
        storedTick.lockItemIfAvailable(controlItemLockID); // should always be successful
        storedTick.remove(msTran, controlItemLockID);
      }
    }
    catch (MessageStoreException e)
    {
      // No FFDC code needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "cleanupTicks", e);

      throw e;
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "cleanupTicks");
  }

  /**
   * Helper method used by AOStream to persistently record that flush has been started
   * @param t the transaction
   * @param stream the stream making this call
   * @return the Item written
   * @throws Exception
   */
  public final Item writeStartedFlush(TransactionCommon t, AOStream stream) throws Exception
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "writeStartedFlush");

    String key = 
      SIMPUtils.getRemoteGetKey(stream.getRemoteMEUuid(), stream.getGatheringTargetDestUuid());
    StreamInfo sinfo = streamTable.get(key);
    if ((sinfo != null) && sinfo.streamId.equals(stream.streamId))
    {
      AOStartedFlushItem item = new AOStartedFlushItem(key, stream.streamId);
      Transaction msTran = mp.resolveAndEnlistMsgStoreTransaction(t);
      this.containerItemStream.addItem(item, msTran);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "writeStartedFlush", item);

      return item;
    }

    // this should not occur
    // log error and throw exception
    SIErrorException e = new SIErrorException(
      nls.getFormattedMessage(
        "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] { "com.ibm.ws.sib.processor.impl.AnycastOutputHandler", "1:2810:1.89.4.1" },
        null));

    // FFDC
    FFDCFilter.processException(
      e,
      "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.writeStartedFlush",
      "1:2817:1.89.4.1",
      this);

    SibTr.exception(tc, e);
    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
    new Object[] { "com.ibm.ws.sib.processor.impl.AnycastOutputHandler", "1:2822:1.89.4.1" });

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "writeStartedFlush", e);
    throw e;
  }

  /**
   * Callback when the Item that records that flush has been started has been committed to persistent storage
   * @param stream The stream making this call
   * @param startedFlushItem The item written
   */
  public final void writtenStartedFlush(AOStream stream, Item startedFlushItem)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "writtenStartedFlush");

    String key = 
      SIMPUtils.getRemoteGetKey(stream.getRemoteMEUuid(), stream.getGatheringTargetDestUuid());
    StreamInfo sinfo = streamTable.get(key);
    if ((sinfo != null) && sinfo.streamId.equals(stream.streamId))
    {
      synchronized (sinfo)
      {
        sinfo.item = (AOStartedFlushItem) startedFlushItem;
      }
    }
    else
    {
      // this should not occur
      // log error and throw exception
      SIErrorException e = new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
            "1:2858:1.89.4.1" },
          null));

      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.writtenStartedFlush",
        "1:2865:1.89.4.1",
        this);

      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
      new Object[] {
        "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
        "1:2872:1.89.4.1" });

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "writtenStartedFlush", e);
      throw e;
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "writtenStartedFlush");
  }

  /**
   * Helper method to generate a unique long value. Used for request id's in various message exchanges.
   * @return The unique value
   */
  public final long generateUniqueValue() throws SIResourceException
  {
    // use tick generator to generate a unique value
    return mp.nextTick();
  }

  private final SIBUuid12 generateUniqueStreamName()
  {
    return new SIBUuid12();
  }

  public final LocalTransaction getLocalTransaction()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getLocalTransaction");

    SIMPTransactionManager txManager = mp.getTXManager();
    //note that the Anycast code is trusted not to use PEV resources in this context!!
    LocalTransaction tran = txManager.createLocalTransaction(true);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getLocalTransaction", tran);
    return tran;
  }

  public final MessageProcessor getMessageProcessor()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getMessageProcessor");
      SibTr.exit(tc, "getMessageProcessor", mp);
    }
    return mp;
  }

  public final ConsumerDispatcher getPubSubConsumerDispatcher()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getPubSubConsumerDispatcher");
      SibTr.exit(tc, "getPubSubConsumerDispatcher", pubSubConsumerDispatcher);
    }
    return (ConsumerDispatcher)pubSubConsumerDispatcher;
  }

  public final SelectionCriteria createSelectionCriteria(String discriminator,
                                                         String selector,
                                                         SelectorDomain domain)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "createSelectionCriteria", new Object[]{discriminator, selector, domain});
      SibTr.exit(tc, "createSelectionCriteria");
    }
    return mp.getSelectionCriteriaFactory().createSelectionCriteria(discriminator, selector, domain);
  }

  private final void doEnqueueWork(AsyncUpdate asyncUpdate)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "doEnqueueWork", asyncUpdate);

    try
    {
      msUpdateThread.enqueueWork(asyncUpdate);
    }
    catch (ClosedException e)
    {
      // should not occur!
      FFDCFilter.processException(e,"com.ibm.ws.sib.processor.impl.AnycastOutputHandler.doEnqueueWork","1:2955:1.89.4.1",this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "doEnqueueWork");
  }

  public void sendToMe(SIBUuid8 rme, int srcId, int priority, JsMessage msg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendToMe",
        new Object[]{rme, Integer.valueOf(srcId), Integer.valueOf(priority), msg});
    MPIO msgTran = mp.getMPIO();
    msgTran.sendToMe(rme, priority, msg);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "sendToMe");
  }

  public void sendToMe(SIBUuid8 rme, int srcId, int priority, ControlMessage msg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendToMe",
        new Object[]{rme, Integer.valueOf(srcId), Integer.valueOf(priority), msg});
    MPIO msgTran = mp.getMPIO();
    msgTran.sendToMe(rme, priority, msg);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "sendToMe");
  }


  // for unit tests
  public int getCountOfBrowseSessions()
  {
    return browserSessionTable.size();
  }

  /**
   * Return the SIMPItemStream associated with this AOH.  This method is needed
   * for proper cleanup of remote durable subscriptions since the usual
   * item stream owned by the DestinationHandler is not used.
   *
   * @return The SIMPItemStream passed to this AOH during instantiation.
   */
  public SIMPItemStream getItemStream()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getItemStream");

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getItemStream", containerItemStream);

    return (SIMPItemStream) containerItemStream;
  }

  /**
   * Return the destination name which this AOH is associated with.
   *
   * @return The destination name passed to this AOH during instantiation.
   */
  public final String getDestName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getDestName");

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getDestName", destName);

    return destName;
  }

  /**
   * Return the destination UUID which this AOH is associated with.  This method
   * is needed for proper cleanup of remote durable subscriptions since pseudo
   * destinations are used, rather than the destination normally associated with the
   * DestinationHandler.
   *
   * @return The destination name passed to this AOH during instantiation.
   */
  public final SIBUuid12 getDestUUID()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getDestUUID");
      SibTr.exit(tc, "getDestUUID", destName);
    }

    return destUuid;
  }

  public Iterator<ControlAdapter> getAOControlAdapterIterator()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getAOControlAdapterIterator", new Object[] {});

    List<ControlAdapter> aoStreams = new ArrayList<ControlAdapter>();
    Enumeration streamInfos = streamTable.elements();

    while (streamInfos.hasMoreElements())
    {
      StreamInfo streamInfo = (StreamInfo)streamInfos.nextElement();
      if (streamInfo.stream != null)
        aoStreams.add(streamInfo.stream.getControlAdapter());
    }

    Iterator<ControlAdapter> itr = aoStreams.iterator();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getAOControlAdapterIterator", itr);

    return itr;
  }

  public final String convertSelectionCriteriasToString(String[] discriminators,
                                                        int[] selectorDomains,
                                                        String[] selectors)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "convertSelectionCriteriasToString",
        new Object[]{discriminators, selectorDomains, selectors});

    String selectionCriteriasAsString = "";

    // return "" if the selector or discriminator is effectively absent
    if (discriminators.length == 1)
    {
      boolean discEmpty = true;
      if  ((discriminators[0] != null) && (!discriminators[0].equals("")))
        discEmpty = false;
      if (discEmpty)
      {
        if ((selectors[0] != null) && (!selectors[0].equals("")))
        {
        }
        else
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "convertSelectionCriteriasToString", selectionCriteriasAsString);

          return selectionCriteriasAsString;
        }
      }
    }

    for (int i=0; i < discriminators.length; i++)
    {
      if (discriminators[i] == null)
        discriminators[i] = "";
      if (selectors[i] == null)
        selectors[i] = "";

      selectionCriteriasAsString += ":" + discriminators[i] + ":" +
                            SelectorDomain.getSelectorDomain(selectorDomains[i]).toString() + ":" +
                            selectors[i];
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "convertSelectionCriteriasToString", selectionCriteriasAsString);

    return selectionCriteriasAsString;
  }

  public Hashtable getBrowserSessions()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getBrowserSessions");

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getBrowserSessions", browserSessionTable);
    return browserSessionTable;
  }

  /**
   * If the RME has been deleted then the stream needs to be flushed to unlock
   * any messaqes on this DME.
   *
   * @param remoteME the uuid of the remote ME
   */
  public void forceFlushAtSource(SIBUuid8 remoteUuid, SIBUuid12 gatheringTargetDestUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "forceFlushAtSource", new Object[] {remoteUuid, gatheringTargetDestUuid});

    StreamInfo sinfo;
    boolean success = false;
    String key = 
      SIMPUtils.getRemoteGetKey(remoteUuid, gatheringTargetDestUuid);
    synchronized (streamTable)
    {
      sinfo = streamTable.get(key);
    }
    if (sinfo == null)
    { // need to do nothing as a stream for this RME does not exist
      success = true;
    }
    else if (sinfo.stream == null)
    {
      // stream for this RME is being created, which means that the RME just sent a request
      // to create a stream. How are we claiming that this RME has been deleted?
      // We should throw some sort of Exception.
      SIErrorException e =
        new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
              "1:3159:1.89.4.1" },
            null));

      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.forceFlushAtSource",
        "1:3165:1.89.4.1",
        this);
      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
          "1:3171:1.89.4.1" });

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "forceFlushAtSource");
      throw e;
    }
    else
    {
      try
      {
        AOStream stream = sinfo.stream;
        // first close() the stream.
        stream.close();
        // We are now guaranteed that all asynchronous writed to the Message Store initiated by that stream are done.

        // Now get all the value ticks from the itemStream
        ArrayList<AOValue> valueTicks = new ArrayList<AOValue>();
        NonLockingCursor cursor = sinfo.itemStream.newNonLockingItemCursor(null);
        AOValue tick;
        while ((tick = (AOValue) cursor.next()) != null)
        {
          valueTicks.add(tick);
        }
        cursor.finished();
        // now delete them and the item stream etc.
        deleteAndUnlockPersistentStream(sinfo, valueTicks);

        // now remove from the streamTable
        synchronized (streamTable)
        {
          streamTable.remove(key);
        }

        success = true;
      }
      catch (Exception e)
      {
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.AnycastOutputHandler.forceFlushAtSource",
          "1:3211:1.89.4.1",
          this);
        SibTr.exception(tc, e);
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
            "1:3217:1.89.4.1" });

        // we should throw an exception
        SIErrorException e2 =
          new SIErrorException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0001",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.AnycastOutputHandler",
                "1:3226:1.89.4.1"},
              null));

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "forceFlushAtSource");
        throw e2;

      }
    } // end else

    if (success)
    {
      // Log message to console saying we have finished flush at anycast DME for this destination and RME
      SibTr.info(
        tc,
        "FLUSH_COMPLETE_CWSIP0452",
        new Object[] {destName,
                      mp.getMessagingEngineName(),
                      key});
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "forceFlushAtSource");
  }

  // Internal helper function. Will unlock all the persistent locks in valueTicks, and
  // remove the persistent ticks and the itemstream and started-flush item
  private void deleteAndUnlockPersistentStream(StreamInfo sinfo, ArrayList valueTicks) throws MessageStoreException, SIRollbackException, SIConnectionLostException, SIIncorrectCallException, SIResourceException, SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deleteAndUnlockPersistentStream", new Object[]{sinfo, valueTicks});

    // create a transaction, and delete all AOValue ticks, unlock the persistent locks,
    // and then remove sinfo.itemStream, and sinfo.item

    LocalTransaction tran = getLocalTransaction();
    cleanupTicks(sinfo, tran, valueTicks);

    Transaction msTran = mp.resolveAndEnlistMsgStoreTransaction(tran);

    sinfo.itemStream.lockItemIfAvailable(controlItemLockID); // will always be available
    sinfo.itemStream.remove(msTran, controlItemLockID);
    if (sinfo.item != null)
    {
      sinfo.item.lockItemIfAvailable(controlItemLockID);  // will always be available
      sinfo.item.remove(msTran, controlItemLockID);
    }
    tran.commit();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "deleteAndUnlockPersistentStream");
  }

  public void notifyReceiveExclusiveChange(boolean newValue)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "notifyReceiveExclusiveChange",
        new Object[] { Boolean.valueOf(newValue) });

    // Only need to set the value.
    // No need to eagerly start flushing the streams since RMEs will also see the change in
    // receiveExclusive and will initiate the flush. Also, JSRemoteConsumerPoints will automatically
    // be closed beacuse the ConsumerDispatcher will close all the ConsumerKeys.
    // All this makes this method trivial.
    setCardinalityOne(newValue);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "notifyReceiveExclusiveChange");
  }

  public DestinationHandler getDestinationHandler() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getDestinationHandler");
      SibTr.exit(tc, "getDestinationHandler", destinationHandler);
    }
    return destinationHandler;
  }

  public long handleControlMessageWithReturnValue(SIBUuid8 sourceMEUuid,
		ControlMessage cMsg) throws SIIncorrectCallException,
		SIResourceException, SIConnectionLostException, SIRollbackException {
	return 0;
  }
}
