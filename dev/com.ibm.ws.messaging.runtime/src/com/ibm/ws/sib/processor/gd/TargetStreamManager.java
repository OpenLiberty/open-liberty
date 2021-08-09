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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.util.am.Alarm;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.AbstractMessage;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.control.ControlAck;
import com.ibm.ws.sib.mfp.control.ControlAckExpected;
import com.ibm.ws.sib.mfp.control.ControlFlushed;
import com.ibm.ws.sib.mfp.control.ControlMessage;
import com.ibm.ws.sib.mfp.control.ControlNotFlushed;
import com.ibm.ws.sib.mfp.control.ControlSilence;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.BatchListener;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.MessageDeliverer;
import com.ibm.ws.sib.processor.impl.interfaces.UpstreamControl;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.impl.store.itemstreams.ProtocolItemStream;
import com.ibm.ws.sib.processor.runtime.SIMPInboundReceiverControllable;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.impl.BasicSIMPIterator;
import com.ibm.ws.sib.processor.runtime.impl.TargetStreamSetControl;
import com.ibm.ws.sib.processor.utils.am.MPAlarmManager;
import com.ibm.ws.sib.processor.utils.index.Index;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;


public class TargetStreamManager implements AlarmListener, BatchListener
{

  private static final TraceComponent tc =
    SibTr.register(
      TargetStreamManager.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

 
  //map from streamID to sourceCellule (upstream route info)
  private HashMap<SIBUuid12, SIBUuid8> sourceMap;
  //map from streamID to streamSet
  private Index streamSets;
  private List<StreamSet> flushedStreamSets;

  //Map from stream UUID to instances of FlushQueryRecord.  If a
  // mapping exists, then an "are you flushed" or "request flush" has
  // been sent for the specified stream UUID.
  private HashMap<SIBUuid12, FlushQueryRecord> flushMap;

  private MessageDeliverer deliverer;
  private UpstreamControl upControl;

  private SIMPTransactionManager txManager;
  protected ProtocolItemStream protocolItemStream = null;
  private DestinationHandler destination = null;

  private MPAlarmManager am;
  private MessageProcessor messageProcessor;

  public TargetStreamManager(MessageProcessor messageProcessor,
                              DestinationHandler destination,
                              MessageDeliverer deliverer,
                              UpstreamControl upControl,
                              ProtocolItemStream protocolItemStream,
                              SIMPTransactionManager txManager)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "TargetStreamManager",
        new Object[] {
          messageProcessor,
          destination,
          deliverer,
          upControl,
          protocolItemStream,
          txManager });

    this.messageProcessor = messageProcessor;
    this.am = messageProcessor.getAlarmManager();
    this.deliverer  = deliverer;
    this.upControl  = upControl;
    this.txManager = txManager;
    this.protocolItemStream = protocolItemStream;
    this.destination = destination;

    sourceMap = new HashMap<SIBUuid12, SIBUuid8>();
    streamSets = new Index();
    flushMap = new HashMap<SIBUuid12, FlushQueryRecord>();
    flushedStreamSets = new ArrayList<StreamSet>();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "TargetStreamManager", this);
  }
  
  private static final class TargetStreamSetControllableIterator extends BasicSIMPIterator
  {
    private TargetStreamManager tsm;
    
    /**
     * @param parent An iterator view of the streams in this stream set
     * @param tsm The TargetStreamManager
     */
    public TargetStreamSetControllableIterator(Iterator parent, TargetStreamManager tsm)
    {
      super(parent);
      this.tsm = tsm;
      
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "TargetStreamSetControllableIterator", new Object[]{parent, tsm});
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "TargetStreamSetControllableIterator", this);
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    public Object next()
    {
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "next");
      
      StreamSet targetStreamSet = (StreamSet)super.next();
      SIMPInboundReceiverControllable targetStreamSetControl = 
        (SIMPInboundReceiverControllable)targetStreamSet.getControlAdapter();
      if(targetStreamSetControl != null)
      {
        ((TargetStreamSetControl)targetStreamSetControl).setTargetStreamManager(tsm);
      }
      
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "next", targetStreamSetControl);
      return targetStreamSetControl;
    }
  }

  /**
   * Get any existing stream set for the stream UUID and destination UUID 
   * specified in the message. If an existing destination exists with the 
   * correct stream UUID, but an incorrect destination UUID (due to 
   * recreation of the virtual link on the remote side), the stream 
   * set will be updated before returning to the caller. 
   * @return The existing stream set, with a correct dest UUID, or null 
   */
  private StreamSet getStreamSetForMessage(AbstractMessage msg) throws SIResourceException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getStreamSetForMessage", new Object[] {msg});

    SIBUuid12 streamID = msg.getGuaranteedStreamUUID();
    StreamSet streamSet = null;

    // Synchronize on the stream set while getting it, and persisting
    // any change.
    synchronized(streamSets)
    {
      streamSet = (StreamSet) streamSets.get(streamID);
      if (streamSet != null)
      {
        // We have an existing stream set.
        // We need to ensure that the destination UUID is correct, as
        // it is possible for this to change on the remote side if the
        // foreign bus (and hence virtual link UUID) is change. We must
        // send back our reply stamped with the correct destination UUID.
        SIBUuid12 msgDest = msg.getGuaranteedTargetDestinationDefinitionUUID();
        if (msgDest != null && !msgDest.equals(streamSet.getDestUuid())) {
          // Update our in-memory destination UUID on the stream set.
          // Note that we don't attempt to persist any change here.
          // The change will be persisted the next time the stream set
          // is persisted for any other reason. There is no benefit
          // to updating the persisted representation here (and doing
          // so has been seen to fail in some cases).
          streamSet.setDestUuid(msgDest);
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getStreamSetForMessage", streamSet);
    return streamSet;
   }

  /**
   * Handle a new message by inserting it in to the appropriate target stream.
   * If the stream ID in the message is a new one, a flush query will be
   * sent to the source. If the ID has been seen before but the specific stream
   * is not found, a new one will be created and added to the stream set.
   *
   * @param msgItem
   */
  public void handleMessage(MessageItem msgItem) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "handleMessage", new Object[] { msgItem });

    JsMessage jsMsg = msgItem.getMessage();

    int priority = jsMsg.getPriority().intValue();
    Reliability reliability = jsMsg.getReliability();

    StreamSet streamSet = getStreamSetForMessage(jsMsg);

    if(streamSet == null)
    {
      handleNewStreamID(msgItem);
    }
    else
    {
      TargetStream targetStream = null;
      synchronized(streamSet)
      {
        targetStream = (TargetStream) streamSet.getStream(priority, reliability);
        if(targetStream == null)
        {
          targetStream = createStream(streamSet,
                                      priority,
                                      reliability,
                                      streamSet.getPersistentData(priority, reliability));
        }
      }

      // Update the stateStream with this message
      // The stateStream itself will do Gap detection

      // Note that the message cannot be written to the MsgStore or
      // delivered to the ConsumerDispatcher until the stream is
      // in order. The TargetStream calls back into this class
      // using the deliverOrderedMessages() method to do this once
      // it has an ordered stream
      targetStream.writeValue(msgItem);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleMessage");
  }

  /**
   * Handle a filtered message by inserting it in to the
   * appropriate target stream as Silence.
   * Since we only need to do this on exisiting TargetStreams, if the
   * streamSet or stream are null we give up
   *
   * @param msgItem
   * @throws SIResourceException
   */
  public void handleSilence(MessageItem msgItem) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "handleSilence", new Object[] { msgItem });

    JsMessage jsMsg = msgItem.getMessage();

    int priority = jsMsg.getPriority().intValue();
    Reliability reliability = jsMsg.getReliability();

    StreamSet streamSet = getStreamSetForMessage(jsMsg);

    if(streamSet != null)
    {
      TargetStream targetStream = null;
      synchronized(streamSet)
      {
        targetStream = (TargetStream) streamSet.getStream(priority, reliability);
      }
      if(targetStream != null)
      {
        // Update the stateStream with Silence
        targetStream.writeSilence(msgItem);
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleSilence");
  }

  /**
   * Handle a new stream ID from a control message
   *
   * @param jsMsg
   */
  private void handleNewStreamID(ControlMessage cMsg) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "handleNewStreamID", new Object[] { cMsg });

    handleNewStreamID(cMsg, null);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleNewStreamID");
  }

  /**
   * Handle a new stream ID from a Message Item (a value message)
   *
   * @param msgItem
   */
  private void handleNewStreamID(MessageItem msgItem) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "handleNewStreamID", new Object[] { msgItem });

    JsMessage jsMsg = msgItem.getMessage();

    handleNewStreamID(jsMsg, msgItem);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleNewStreamID");
  }

  /**
   * Handle a new stream ID and cache a MessageItem for replay later.
   * msgItem can be null, for example if this was triggered by a control message.
   *
   * @param msgItem
   */
  private void handleNewStreamID(AbstractMessage aMessage, MessageItem msgItem) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "handleNewStreamID", new Object[] { aMessage, msgItem });

    SIBUuid12 streamID = aMessage.getGuaranteedStreamUUID();

    // Synchronize to resolve race between multiple messages on same
    // stream.  Can't actually happen until multihop, e.g. messages on
    // same stream arrive simultaneously from multiple cellules.
    synchronized (flushMap)
    {
      // Otherwise, create a new flush query record and proceed
      FlushQueryRecord entry = flushMap.get(streamID);

      if ( (entry != null) && (msgItem != null) )
      {
        // Since the entry already exists, somebody else already
        // started the timer so all we have to do is cache the
        // message for replay later.
        entry.append(msgItem);
      }
      else
      {
        // Otherwise, new entry:
        // 1. Create a request ID and flush record, store it in the map.
        // 2. Create and send an "are you flushed".
        // 3. Start an alarm to repeat "are you flushed" a set number of
        //    times.

        SIBUuid8 sourceMEUuid = aMessage.getGuaranteedSourceMessagingEngineUUID();

        SIBUuid12 destID = aMessage.getGuaranteedTargetDestinationDefinitionUUID();
        SIBUuid8  busID  = aMessage.getGuaranteedCrossBusSourceBusUUID();

        // Create and store the request record
        long reqID = messageProcessor.nextTick();
        entry = new FlushQueryRecord(sourceMEUuid, destID, busID,
                                     msgItem, reqID);
        flushMap.put(streamID, entry);

        // Create and send the query
        // TODO: create a proper srcID here
        upControl.sendAreYouFlushedMessage(sourceMEUuid, destID, busID, reqID, streamID);

        // Start the alarm.  The context for the alarm is the stream id, which
        // we can use to look up the FlushQueryRecord if the alarm expires.
        entry.resend = am.create(GDConfig.FLUSH_QUERY_INTERVAL, this, streamID);
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleNewStreamID");
  }

  /**
   * Create a new StreamSet for a given streamID and sourceCellule.
   *
   * @param streamID
   * @param sourceCellule
   * @param remoteDestUuid    This may not always be the same as the
   *                          Uuid of the local destination
   * @param remoteBusUuid
   * @return A new StreamSet
   * @throws SIResourceException if the message store outofcache space exception is caught
   */
  private StreamSet addNewStreamSet(SIBUuid12 streamID,
                                    SIBUuid8  sourceMEUuid,
                                    SIBUuid12 remoteDestUuid,
                                    SIBUuid8  remoteBusUuid,
                                    String linkTarget)

  throws SIRollbackException,
         SIConnectionLostException,
         SIIncorrectCallException,
         SIResourceException,
         SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addNewStreamSet", new Object[]{streamID,
                                                      sourceMEUuid,
                                                      remoteDestUuid,
                                                      remoteBusUuid,
                                                      linkTarget});
    StreamSet streamSet = null;
    try
    {
      LocalTransaction tran = txManager.createLocalTransaction(false);
      Transaction msTran = txManager.resolveAndEnlistMsgStoreTransaction(tran);

      //create a persistent stream set
      streamSet = new StreamSet(streamID,
                                sourceMEUuid,
                                remoteDestUuid,
                                remoteBusUuid,
                                protocolItemStream,
                                txManager,
                                0,
                                destination.isLink() ? StreamSet.Type.LINK_TARGET : StreamSet.Type.TARGET,
                                tran,
                                linkTarget);
      protocolItemStream.addItem(streamSet, msTran);
      tran.commit();

      synchronized(streamSets)
      {
        streamSets.put(streamID, streamSet);
        sourceMap.put(streamID, sourceMEUuid);
      }
    }
    catch (OutOfCacheSpace e)
    {
      // No FFDC code needed

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "addNewStreamSet", e);

      throw new SIResourceException(e);
    }
    catch (MessageStoreException e)
    {
      // MessageStoreException shouldn't occur so FFDC.
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.gd.TargetStreamManager.addNewStreamSet",
        "1:471:1.69",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "addNewStreamSet", e);

      throw new SIResourceException(e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addNewStreamSet", streamSet);
    return streamSet;
  }

  /**
   * Create a new TargetStream and initialize it with a given completed prefix.
   * Always called with streamSet lock
   *
   * @param streamSet
   * @param priority
   * @param reliability
   * @param completedPrefix
   * @return A new TargetStream
   */
  private TargetStream createStream(StreamSet streamSet,
                                    int priority,
                                    Reliability reliability,
                                    long completedPrefix)

                                    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "createStream",
        new Object[] { streamSet, Integer.valueOf(priority), reliability, Long.valueOf(completedPrefix) });

    TargetStream stream = null;
    stream = createStream(streamSet, priority, reliability);
    stream.setCompletedPrefix(completedPrefix);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createStream");

    return stream;
  }

  /**
   * Create a new TargetStream in the given StreamSet
   *
   * @param streamSet
   * @param priority
   * @param reliability
   * @return a new TargetStream
   * @throws SIResourceException
   */
  private TargetStream createStream(StreamSet streamSet,
                              int priority,
                              Reliability reliability) throws SIResourceException

  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "createStream",
        new Object[] { streamSet, Integer.valueOf(priority), reliability });

    TargetStream stream = null;

    if(reliability.compareTo(Reliability.BEST_EFFORT_NONPERSISTENT) <= 0)
    {
      stream = new ExpressTargetStream(deliverer,
        streamSet.getRemoteMEUuid(), streamSet.getStreamID());
    }
    else
    {
      //Warning - this assumes that ASSURED is always the highest Reliability
      //and that UNKNOWN is always the lowest (0).
      stream = new GuaranteedTargetStream(
                        deliverer,
                        upControl,
                        am,
                        streamSet,
                        priority, //priority
                        reliability, //reliability
                        new ArrayList(),
                        messageProcessor);
    }

    streamSet.setStream(priority, reliability, stream);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createStream", stream);

    return stream;
  }

  /**
   * @param ack
   * @param min
   * @throws SIResourceException
   */
  public long checkAck(ControlAck ack, long min) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "checkAck", new Object[]{ack, Long.valueOf(min)});

    int priority = ack.getPriority().intValue();
    Reliability reliability = ack.getReliability();

    StreamSet streamSet = getStreamSetForMessage(ack);

    // It's possible that we've never seen any messages on this
    // stream due to filtering.  So if we don't have a stream set
    // for it, or a stream,  then just return the min.
    if (streamSet != null)
    {
      TargetStream targetStream = null;
      synchronized(streamSet)
      {
        targetStream = (TargetStream) streamSet.getStream(priority, reliability);
      }

      if(targetStream != null)
      {
        long ackPrefix = targetStream.getCompletedPrefix();
        if(ackPrefix < min) min = ackPrefix;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "checkAck", Long.valueOf(min));
    return min;
  }


  /**
   * Handle a ControlFlushed message. Flush any existing streams and
   * throw away any cached messages.
   *
   * @param cMsg
   * @throws SIResourceException
   */
  public void handleFlushedMessage(ControlFlushed cMsg)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "handleFlushedMessage", new Object[] { cMsg });

    SIBUuid12 streamID = cMsg.getGuaranteedStreamUUID();
    forceFlush(streamID);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleFlushedMessage");
  }

  /**
   * Flush any existing streams and throw away any cached messages.
   *
   * @param streamID
   * @throws SIResourceException
   * @throws SIException
   */
  public void forceFlush(SIBUuid12 streamID)
    throws SIResourceException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "forceFlush", new Object[] { streamID });

    // Synchronize to resolve racing messages.
    synchronized (flushMap)
    {
      FlushQueryRecord entry = flushMap.remove(streamID);

      // Remove the entry (we may not have even had
      // one), then clean up any existing stream state.  Also, make
      // sure we turn off the alarm if there IS an entry.  Note that
      // an alarm will always be present if an entry exists.
      if (entry != null)
         entry.resend.cancel();

      flush(streamID);

    }

    //If all the inbound stream sets are empty, queue the destination that the
    //inbound streams are for to the asynch deletion thread, incase any cleanup
    //of the destination is required.  If not, the asynch deletion thread will do
    //nothing.
 //   if (isEmpty())
 //   {
 //     DestinationManager destinationManager = messageProcessor.getDestinationManager();
 //     BaseDestinationHandler destinationHandler = protocolItemStream.getDestinationHandler();
 //     destinationManager.markDestinationAsCleanUpPending(destinationHandler);
 //   }


    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "forceFlush");
  }

  /**
   * Send a request to flush a stream.  The originator of the stream,
   * and the ID for the stream must be known.  This method is public
   * because it's not clear who's going to call this yet.
   *
   * @param source The originator of the stream (may be multiple hops
   * away).
   * @param stream The UUID of the stream to flush.
   */
  public void requestFlushAtSource(SIBUuid8 source,
                                   SIBUuid12 destID,
                                   SIBUuid8  busID,
                                   SIBUuid12 stream,
                                   boolean indoubtDiscard)
    throws SIException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "requestFlushAtSource", new Object[] { source, stream });

    // Synchronize here to avoid races (not that we expect any)
    synchronized (flushMap)
    {
      if (flushMap.containsKey(stream))
      {
        // Already a request on the way so ignore.
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "requestFlushAtSource");
        return;
      }

      // Ok, new entry:
      // 1. Create a request ID and flush record, store it in the map.
      // 2. Create and send an "request flush".
      // 3. Start an alarm to repeat "request flush" a set number of
      //    times.

      // Create and store the request record
      long             reqID = messageProcessor.nextTick();
      FlushQueryRecord entry = new FlushQueryRecord(source, destID, busID, reqID);
      flushMap.put(stream, entry);

      // Create and send the query
       upControl.sendRequestFlushMessage(source, destID, busID, reqID, stream, indoubtDiscard);

      // Start the alarm.  The context for the alarm is the stream ID,
      // which we can use to look up the FlushQueryRecord if the alarm
      // expires.
      entry.resend = am.create(GDConfig.REQUEST_FLUSH_INTERVAL, this, stream);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "requestFlushAtSource");
  }

  public void flush(SIBUuid12 streamID) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "flush", new Object[] { streamID, this });

    StreamSet streamSet = null;
    synchronized(streamSets)
    {
      //remove the streamID from the sourceMap
      sourceMap.remove(streamID);
      //remove the streamSet from the map of all streamSets
      streamSet = (StreamSet) streamSets.remove(streamID);

      //TODO do we need to synchronize on the streamSet here?
      if(streamSet != null)
      {          
        streamSet.dereferenceControlAdapter();
        
        //flush all the streams within the set
        Iterator itr = streamSet.iterator();
        while(itr.hasNext())
        {
          TargetStream stream = (TargetStream) itr.next();
          stream.flush();
        }
         
  //    remove the streamSet(protocolItem) from the message store in the
        // post-commit of the current batch
        synchronized(flushedStreamSets)
        {
          flushedStreamSets.add(streamSet);
        }
        deliverer.forceTargetBatchCompletion(this);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "flush", streamSets);
  }

  /**
   * Handle a ControlNotFlushed message. Flush and streams from the same source
   * and create a new set of streams for the new (not flushed) stream ID.
   *
   * @param cMsg
   * @throws SIResourceException
   */
  public void handleNotFlushedMessage(ControlNotFlushed cMsg)

  throws SIIncorrectCallException, SIErrorException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "handleNotFlushedMessage", new Object[] { cMsg });

    SIBUuid12 streamID = cMsg.getGuaranteedStreamUUID();
    //Synchronize to resolve racing messages.
    synchronized (flushMap)
    {
      FlushQueryRecord entry = flushMap.get(streamID);
      // make sure the request ID matches.
      long reqId = cMsg.getRequestID();
      //if there is no entry or the entry's request ID does not match then
      //it must have been a stale request and we ignore it.
      if ((entry != null) && (entry.requestID == reqId))
      {
        // ASSERT.isTrue(entry.cache != null)

        // The assertion basically means that we should only receive
        // NOTFLUSHED if the original query was AREYOUFLUSHED.  The
        // only legal reply to REQUESTFLUSH is, eventually, FLUSHED.

        // Our original query was for "are you flushed" and we now
        // have the reply, so:
        // 1) remove the entry from the flush map and disable the alarm
        // 2) handle the new prefixes and replay any cached data messages.
        flushMap.remove(streamID);
        entry.resend.cancel();

        StreamSet streamSet = getStreamSetForMessage(cMsg);

        if(streamSet == null)
        {
          //check that we don't already have a streamSet for this source, because there
          // should only be the one at any one time (except for links, see below...)
          SIBUuid8 sourceMEUuid = cMsg.getGuaranteedSourceMessagingEngineUUID();
          if(sourceMap.containsValue(sourceMEUuid))
          {
            //if we do, try to remove it
            Iterator<SIBUuid12> itr = sourceMap.keySet().iterator();
            while(itr.hasNext())
            {
              SIBUuid12 sid = itr.next();
              SIBUuid8 uuid = sourceMap.get(sid);
              if(sourceMEUuid.equals(uuid))
              {
                // 538529
                // If this is a link it is possible to have multiple streams of messages
                // coming in from the source ME concurrently. We have one for all point-to-point
                // messages and one per topicspace that is sending messages to us.
                // Ideally we need to check with the source before we flush any of our existing
                // streams, as they may still be valid. We would do this by asking the sender if
                // they are flushed (commented out code below). Unfortunately we can't do that because
                // the sender (the other end of the link) may not be the source of the messages
                // so it's not guaranteed to actually know about this stream even if it still
                // exists (for example the ME's just been restarted and lost the non-persistent
                // state for this stream). So, instead we simply keep all streams.
                // TODO This isn't ideal so maybe we can fix it in the future 
                if(destination.isLink())
                {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "WARNING: Existing Link stream, " + sid + ", not flushed");
                  
//                  StreamSet existingStreamSet = (StreamSet)streamSets.get(sid);
//
//                  // Create and store the new request record
//                  long reqID = messageProcessor.nextTick();
//                  FlushQueryRecord newEntry = new FlushQueryRecord(sourceMEUuid,
//                                               existingStreamSet.getDestUuid(),
//                                               existingStreamSet.getBusUuid(),
//                                               null,
//                                               reqID);
//                  flushMap.put(sid, newEntry);
//
//                  // Create and send the query
//                  upControl.sendAreYouFlushedMessage(sourceMEUuid,
//                                                     existingStreamSet.getDestUuid(),
//                                                     existingStreamSet.getBusUuid(),
//                                                     reqID,
//                                                     sid);
//
//                  // Start the alarm.  The context for the alarm is the stream id, which
//                  // we can use to look up the FlushQueryRecord if the alarm expires.
//                  newEntry.resend = am.create(GDConfig.FLUSH_QUERY_INTERVAL, this, sid);
                }
                // If we're not a link we only ever expect a single streamSet to be in use
                // by the source at any one time, so flush any that we already have.
                else
                {
                  flush(sid);
                  //there should only ever be at most one other streamSet found
                  //so stop looking if we found one.
                  break;
                }
              }
            }
          }
          
          // If this is a link, get the actual target destination 
          String linkTarget = SIMPConstants.PTOP_TARGET_STREAM;
          JsDestinationAddress routingDest = cMsg.getRoutingDestination();
          if (destination.isLink() &&  routingDest!= null)
          {
            // get the destination and check if it is a topicspace
            DestinationHandler dest = 
              destination.getDestinationManager().
                getDestinationInternal(routingDest.getDestinationName(),
                                       routingDest.getBusName(), true);
            if (dest!=null && dest.isPubSub())
              linkTarget = routingDest.getDestinationName();
          }           
          
          //create a new streamSet
          SIBUuid8 busId = cMsg.getGuaranteedCrossBusSourceBusUUID();
          if( busId == null)
          {
            busId = messageProcessor.getMessagingEngineBusUuid();
          }
          streamSet = addNewStreamSet(streamID,
                                      sourceMEUuid,
                                      cMsg.getGuaranteedTargetDestinationDefinitionUUID(),
                                      busId,
                                      linkTarget);
          //set all the completed prefixes
          long[] completedPrefixes = cMsg.getCompletedPrefixTicks();
          int[] priority  = cMsg.getCompletedPrefixPriority();
          int[] reliability = cMsg.getCompletedPrefixQOS();
          for(int i=0;i<completedPrefixes.length;i++)
          {
            streamSet.setPersistentData(priority[i],
                                                Reliability.getReliability(reliability[i]),
                                                completedPrefixes[i]);
          }
        }
        // Now replay any cached messages
        for(int i=0; i<entry.cacheIndex; i++)
        {
          handleMessage(entry.cache[i]);
        }
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleNotFlushedMessage");
  }

  /**
   * Handle an ControlAckExpected message. This will result in either a ControlAreYouFlushed
   * or a ControlNack being sent back to the source.
   *
   * @param cMsg
   * @throws SIException
   */
  public void handleAckExpectedMessage(ControlAckExpected cMsg) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "handleAckExpectedMessage", new Object[] { cMsg });

    int priority = cMsg.getPriority().intValue();
    Reliability reliability = cMsg.getReliability();
    SIBUuid12 streamID = cMsg.getGuaranteedStreamUUID();

    StreamSet streamSet = getStreamSetForMessage(cMsg);

    if(streamSet == null)
    {
      //if this is a new streamID, send a flush query
      handleNewStreamID(cMsg);
    }
    else
    {
      TargetStream targetStream = null;
      synchronized(streamSet)
      {
        targetStream = (TargetStream) streamSet.getStream(priority, reliability);
        if(targetStream == null)
        {
          //if the specific stream does not exist, create it
          targetStream = createStream(streamSet,
                                      priority,
                                      reliability,
                                      streamSet.getPersistentData(priority, reliability));
        }
      }

        // Get the tickValue from the message
        long tickValue = cMsg.getTick();
        // Get the underlying statestream to process this
        // it will call back to the UpstreamControl to send Nacks
        targetStream.processAckExpected(tickValue);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleAckExpectedMessage");
  }

  /**
   * Handle a ControlSilence message.
   *
   * @param cMsg
   * @throws SIException
   */
  public void handleSilenceMessage(ControlSilence cMsg) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "handleSilenceMessage", new Object[] { cMsg });

    int priority = cMsg.getPriority().intValue();
    Reliability reliability = cMsg.getReliability();
    SIBUuid12 streamID = cMsg.getGuaranteedStreamUUID();

    StreamSet streamSet = getStreamSetForMessage(cMsg);

    if(streamSet == null)
    {
      //if this is a new stream ID, send a flush query
      handleNewStreamID(cMsg);
    }
    else
    {
      TargetStream targetStream = null;
      synchronized(streamSet)
      {
        targetStream = (TargetStream) streamSet.getStream(priority, reliability);
        if(targetStream == null)
        {
          //if the specific stream does not exist, create it
          targetStream = createStream(streamSet,
                                      priority,
                                      reliability,
                                      streamSet.getPersistentData(priority, reliability));
        }
      }

      // Update the statestream with this information
      targetStream.writeSilence(cMsg);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleSilenceMessage");
  }

  /**
   * Restore a StreamSet to a previous state
   *
   * @param streamSet
   * @throws SIResourceException
   * @throws SIException
   */
  public void reconstituteStreamSet(StreamSet streamSet)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "reconstituteStreamSet", streamSet);

    synchronized(streamSets)
    {
      streamSets.put(streamSet.getStreamID(), streamSet);
      sourceMap.put(streamSet.getStreamID(), streamSet.getRemoteMEUuid());
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reconstituteStreamSet");
  }

  /**
   * This method is called when an alarm expires for an "are you
   * flushed" or "flush request" query.
   *
   * @param alarmContext the alarm context we passed into
   * the alarm scheduler.  Contains the stream ID we were querying.
   */
  public void alarm(Object alarmContext)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "alarm", alarmContext);

    // Alarm context should be an sid, make it so.
    SIBUuid12 sid = (SIBUuid12) alarmContext;

    // synchronized here in case we're racing with an answer to our
    // query.
    synchronized (flushMap)
    {
      // See if the query record is still around
      FlushQueryRecord entry = flushMap.get(sid);

      if (entry != null)
      {
        // Query still active, see if we need to restart.
        entry.attempts--;
        if (entry.attempts > 0)
        {
          // Yup, resend the query and reset the alarm.  If
          // entry.cached is null, then this is a "request flush"
          // rather than an "are you flushed".
          try
          {
            if (entry.cache == null)
            {
              upControl.sendRequestFlushMessage(entry.source,
                                                entry.destId,
                                                entry.busId,
                                                entry.requestID,
                                                sid,
                                                false);  //TODO check indoubtDiscard
            }
            else
            {
              upControl.sendAreYouFlushedMessage(entry.source,
                                                 entry.destId,
                                                 entry.busId,
                                                 entry.requestID, sid);
            }
          }
          catch (SIResourceException e)
          {
            // No FFDC code needed

            // If we run out of resources, then give up on the query
            // and log an error.  This is probably only important for
            // "request flush" queries.
            flushMap.remove(sid);

            // TODO: actually, this should be an admin message.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
              SibTr.event(tc, "Flush query failed for stream: " + sid);
          }

        }
        else
        {
          // Nope, didn't work.  Remove the query record, log the event,
          // and exit.
          flushMap.remove(sid);
          // TODO: actually, this should be an admin message.
          if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            SibTr.event(tc, "Flush query expired for stream: " + sid);
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "alarm");
  }

  /**
   * Determine if there are any unflushed target streams to the destination
   *
   * @return boolean
   */
  public boolean isEmpty()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "isEmpty");
      SibTr.exit(tc, "isEmpty", new Object[] {Boolean.valueOf(streamSets.isEmpty()), 
          Boolean.valueOf(flushedStreamSets.isEmpty()),streamSets, this});
    }

    // Don't report empty until any pending flushes have completed.
    // Otherwise we may run into a race with an async delete thread
    // for the destination.
    return (streamSets.isEmpty() && flushedStreamSets.isEmpty());
  }

  public SIMPIterator getTargetStreamSetControlIterator()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "getTargetStreamSetControlIterator");

    SIMPIterator itr = new TargetStreamSetControllableIterator(streamSets.iterator(), this);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTargetStreamSetControlIterator", itr);

    return itr;
  }

  /* Inner classes */

  /**
   * Instances of this class represent either an "are you flushed" or
   * "request flush" query.  These instances are stored in flushMap,
   * keyed to the UUID of the stream.
   */
  protected class FlushQueryRecord
  {
    // The ME to which we've sent the request
    public SIBUuid8 source;

    // The destination and bus from the original query message
    public SIBUuid12 destId;
    public SIBUuid8  busId;

    // If non-null, then this is an "are you flushed" query and this
    // vector contains data messages cached until we find out whether
    // the stream is flushed or not.  Otherwise, this is a "request
    // flush" query.
    public MessageItem[] cache;
    public int           cacheIndex;

    // The resend alarm for this request.
    public Alarm resend;
    public int   attempts;

    // The request ID associated with this request
    public long requestID;
    
    /**
     * Use this constructor for building a "request flush" query.
     *
     * @param C The ME from which the message originated (and hence
     * the target for our query).
     * @param I The requestID associated with the query message we're
     * about to send.
     */
    public FlushQueryRecord(SIBUuid8 C, SIBUuid12 D, SIBUuid8 B, long I)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "FlushQueryRecord", new Object[] { C,D,B, Long.valueOf(I) });

      source    = C;
      destId    = D;
      busId     = B;
      requestID = I;
      cache     = null;
      attempts  = GDConfig.REQUEST_FLUSH_ATTEMPTS;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "FlushQueryRecord", this);
    }

    /**
     * Use this constructor for building a "are you flushed" query.
     *
     * @param C The ME from which the message originated (and hence
     * the target for our query).
     * @param M The first MessageItem we'll add to the cache (usually
     * the first data message we show for which we didn't have stream
     * info).
     * @param I The requestID associated with the query message we're
     * about to send.
     */
    public FlushQueryRecord(SIBUuid8 C, SIBUuid12 D, SIBUuid8 B, MessageItem M, long I)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "FlushQueryRecord", new Object[] { C, D,B, M, Long.valueOf(I) });

      source     = C;
      destId     = D;
      busId      = B;
      cache      = new MessageItem[GDConfig.FLUSH_CACHE_LENGTH];
      cacheIndex = 0;
      requestID  = I;
      attempts   = GDConfig.FLUSH_QUERY_ATTEMPTS;

      // Don't add the message unless it's data
      if (M != null)
        append(M);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "FlushQueryRecord", this);
    }

    /**
     * Append a message to the cache for a stream with an outstanding
     * request.  Note that the cache is bounded.  If the bound is
     * exceeded, then this method will silently fail to add any
     * further messages.
     *
     * @param M The MessageItem to append.
     */
    public synchronized void append(MessageItem M)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(this, tc, "append", new Object[] { M });

      // ASSERT: cache != null
      if ((cacheIndex < cache.length) && (M != null))
        cache[cacheIndex++] = M;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "append");
    }
  }
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.BatchListener#batchPrecommit(com.ibm.ws.sib.msgstore.transactions.Transaction)
   */
  public void batchPrecommit(TransactionCommon currentTran)
  {

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.BatchListener#batchCommitted()
   */
  public void batchCommitted()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "batchCommitted");

    synchronized(flushedStreamSets)
    {
      Iterator<StreamSet> itr = flushedStreamSets.iterator();
      while(itr.hasNext())
      {
        StreamSet streamSet = itr.next();
        try
        {
          streamSet.remove();
          itr.remove();
        }
        catch (SIException e)
        {
          // FFDC
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.gd.TargetStreamManager.batchCommitted",
            "1:1297:1.69",
            this);

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "batchCommitted", "SIErrorException");
          throw new SIErrorException(e);
        }
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "batchCommitted");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.BatchListener#batchRolledBack()
   */
  public void batchRolledBack()
  {
    // No implementation
  }

  /**
   * Sends an "are you flushed" query to the source of any unflushed streams.
   * We use this to determine when it's safe to delete a destination with
   * possibly indoubt messages.
   */
  public void queryUnflushedStreams()
    throws SIResourceException
  {
    synchronized (streamSets) {
      for(Iterator i=streamSets.iterator(); i.hasNext();)
      {
        StreamSet next = (StreamSet) i.next();

        // Note the use of -1 for the request ID.  This guarantees
        // that we won't accidentally overlap with a request
        // in the local request map.
        upControl.sendAreYouFlushedMessage(next.getRemoteMEUuid(),
                                           next.getDestUuid(),
                                           next.getBusUuid(),
                                           -1,
                                           next.getStreamID());
      }
    }
  }

  public DestinationHandler getDestinationHandler() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getDestinationHandler");
      SibTr.exit(tc, "getDestinationHandler", destination);
    }

    return destination;
  }
}
