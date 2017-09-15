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
import java.util.Iterator;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.control.ControlAck;
import com.ibm.ws.sib.mfp.control.ControlAreYouFlushed;
import com.ibm.ws.sib.mfp.control.ControlNack;
import com.ibm.ws.sib.mfp.control.ControlNotFlushed;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.exceptions.FlushAlreadyInProgressException;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DownstreamControl;
import com.ibm.ws.sib.processor.impl.interfaces.FlushComplete;
import com.ibm.ws.sib.processor.impl.interfaces.Reallocator;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.impl.store.itemstreams.ProtocolItemStream;
import com.ibm.ws.sib.processor.runtime.impl.SourceStreamSetControl;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;


public class SourceStreamManager
{
  private boolean pointTopoint = false;
  private SIMPTransactionManager txManager;
  private ProtocolItemStream protocolItemStream = null;
  private SIBUuid8 targetMEUuid = null;
  private DestinationHandler destinationHandler;
  private MessageProcessor messageProcessor;
  //NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private static final TraceComponent tc =
    SibTr.register(
      SourceStreamManager.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);


  //The DownstreamControl is used to send control messages to the
  //target
  private DownstreamControl downControl;

  //The StreamSet contains an array of Streams, in this case SourceStreams
  private StreamSet streamSet;

  private Reallocator reallocator = null;

  /**
   * A reference to the flush callback we need to make when the current
   * stream is finally flushed.
   */
  protected FlushComplete flushInProgress = null;

  /**
   * The SourceStreamManager manages source streams. It handles all new GD messages
   * and replies to flush queries and requests.
   *
   * @param messageProcessor The Owning MessageProcessor
   * @param downControl The DownstreamControl used to send control messages to the target
   * @param destinationHandler The definition of this remote destination
   * @param targetCellule The target Cellule where the destination is localised
   */
  public SourceStreamManager(MessageProcessor messageProcessor,
                             DownstreamControl downControl,
                             DestinationHandler destinationHandler,
                             ProtocolItemStream protocolItemStream,
                             SIBUuid8 targetMEUuid,
                             Reallocator reallocator )
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "SourceStreamManager",
        new Object[]{messageProcessor,
                     downControl,
                     destinationHandler,
                     protocolItemStream,
                     targetMEUuid,
                     reallocator});

    //save our parameters
    this.messageProcessor = messageProcessor;
    this.downControl = downControl;
    this.destinationHandler = destinationHandler;
    this.protocolItemStream = protocolItemStream;
    this.txManager = messageProcessor.getTXManager();
    this.targetMEUuid = targetMEUuid;

    //for PubSub SourceStream sets the reallocator is always null
    this.reallocator = reallocator;
    if( reallocator != null )
    {
      pointTopoint = true;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "SourceStreamManager", this);
  }

  public synchronized StreamSet getStreamSet() throws SIResourceException
  {
    if(streamSet == null)
    {
      createNewPersistentStreamSet(null);
    }
    return streamSet;
  }

  public SourceStreamSetControl getStreamSetRuntimeControl()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getStreamSetRuntimeControl");
    SourceStreamSetControl control = null;
    
    try
    {
       control = (SourceStreamSetControl) getStreamSet().getControlAdapter();
      ((SourceStreamSetControl)control).setSourceStreamManager(this);
    }
    catch (SIResourceException e)
    {
      // SIResourceException shouldn't occur so FFDC.
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.gd.SourceStreamManager.getStreamSetRuntimeControl",
        "1:188:1.102",
        this);

      SibTr.exception(tc, e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getStreamSetRuntimeControl", control);

    return control;
  }

  private synchronized StreamSet getStreamSet(SIBUuid12 streamID) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getStreamSet", streamID);

    if(streamSet == null)
    {
      createNewPersistentStreamSet(streamID);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getStreamSet", streamSet);
    return streamSet;
  }

  private void createNewPersistentStreamSet(SIBUuid12 streamID) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createNewPersistentStreamSet", streamID);

    if (streamID == null)
      streamID = new SIBUuid12();

    try
    {
      LocalTransaction tran = txManager.createLocalTransaction(false);
      Transaction msTran = txManager.resolveAndEnlistMsgStoreTransaction(tran);

      // The sendWindow of the streams should be persisted from the start to the correct value,
      // otherwise, cnages in the defined sendWindow will go un-noticed over restarts.
      long sendWindow = Long.MAX_VALUE;
      if(pointTopoint)
        sendWindow = messageProcessor.getDefinedSendWindow();
      
      // Work out which type of source stream this is so that the correct controllable
      // object is created.
      StreamSet.Type type = StreamSet.Type.SOURCE;
      if(destinationHandler.isLink())
      {
        if(destinationHandler.hasLocal())
          type = StreamSet.Type.LINK_SOURCE;
        else
          type = StreamSet.Type.LINK_REMOTE_SOURCE;
      }
      
      //  create a new streamSet
      streamSet = new StreamSet(streamID,
                               targetMEUuid,
                               destinationHandler.getUuid(),
                               messageProcessor.getMessagingEngineBusUuid(),
                               protocolItemStream,
                               txManager,
                               sendWindow,
                               type,
                               tran,
                               null);

      // Add new protocolItem to the protocolItemStream
      protocolItemStream.addItem(streamSet, msTran);
      tran.commit();

    }
    catch (SIConnectionLostException e)
    {
      // No FFDC code needed
      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "createNewPersistentStreamSet", "SIResourceException");
      throw new SIResourceException(e);
    }
    catch (SIIncorrectCallException e)
    {
      // No FFDC code needed
      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "createNewPersistentStreamSet", "SIResourceException");
      throw new SIResourceException(e);
    }
    catch (OutOfCacheSpace e)
    {
      // No FFDC code needed
      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "createNewPersistentStreamSet", "SIResourceException");
      throw new SIResourceException(e);

    }
    catch (MessageStoreException e)
    {
      // MessageStoreException shouldn't occur so FFDC.
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.gd.SourceStreamManager.createNewPersistentStreamSet",
        "1:294:1.102",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "createNewPersistentStreamSet", e);

      throw new SIResourceException(e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewPersistentStreamSet");
  }

  /**
   * Add a message in to the appropriate source stream. This will create a stream
   * if one does not exist and set any appropriate fields in the message
   *
   * @param msgItem The message to add
   * @return Whether the message was added to a real stream or not
   */
  public boolean addMessage(SIMPMessage msgItem) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addMessage", new Object[] { msgItem });
  
    // Get the JsMessage as we need to update the Guaranteed fields
    JsMessage jsMsg = msgItem.getMessage();
  
    StreamSet streamSet = getStreamSet();
  
    // Stamp the message with the stream ID
    msgItem.setGuaranteedStreamUuid(streamSet.getStreamID());
  
    //  NOTE: no synchronization with flush necessary here since startFlush
    // is not supposed to be called until all competing producer threads have
    // exited.
    Reliability reliability = msgItem.getReliability();
  
    if(reliability == Reliability.BEST_EFFORT_NONPERSISTENT)
    {
      addBestEffortMessage(msgItem);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "addMessage", false);
      return false;
    }
    else
    {
      SourceStream sourceStream = null;
      int priority = msgItem.getPriority();
      synchronized(streamSet)
      {        
        sourceStream = (SourceStream) streamSet.getStream(priority, reliability);
    
        if(sourceStream == null)
        {
          sourceStream = createStream(streamSet,
                                      priority,
                                      reliability,
                                      streamSet.getPersistentData(priority, reliability),
                                      false );
        }
      }
    
      synchronized(sourceStream)
      {    
        long tick = this.messageProcessor.nextTick();
    
        //Set a unique id in the message if explicitly told to or
        //if one has not already been set
        if(msgItem.getRequiresNewId()  ||  jsMsg.getSystemMessageId()==null)
        {
          jsMsg.setSystemMessageSourceUuid(this.messageProcessor.getMessagingEngineUuid());
          jsMsg.setSystemMessageValue(tick);
          msgItem.setRequiresNewId(false);
        }
    
        jsMsg.setGuaranteedValueEndTick(tick);
        jsMsg.setGuaranteedValueValueTick(tick);
        jsMsg.setGuaranteedValueRequestedOnly(false);
    
        jsMsg.setGuaranteedValueStartTick(sourceStream.getLastMsgAdded() + 1);
        jsMsg.setGuaranteedValueCompletedPrefix(sourceStream.getCompletedPrefix());
    
        sourceStream.writeUncommitted(msgItem);
      } // end sync
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "addMessage", true);
    
      return true;
    }    
  }

  /**
   * Set any appropriate fields in the best effort message. The message does
   * not actually get added to any streams.
   *
   * @param msgItem The message to add
   */
  private void addBestEffortMessage(SIMPMessage msgItem) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addBestEffortMessage", new Object[] { msgItem });
    
    // Get the JsMessage as we need to update the Guaranteed fields
    JsMessage jsMsg = msgItem.getMessage();
 
    long tick = this.messageProcessor.nextTick();

    //Set a unique id in the message if explicitly told to or
    //if one has not already been set
    if(msgItem.getRequiresNewId()  ||  jsMsg.getSystemMessageId()==null)
    {
      jsMsg.setSystemMessageSourceUuid(this.messageProcessor.getMessagingEngineUuid());
      jsMsg.setSystemMessageValue(tick);
      msgItem.setRequiresNewId(false);
    }

    jsMsg.setGuaranteedValueEndTick(tick);
    jsMsg.setGuaranteedValueValueTick(tick);
    jsMsg.setGuaranteedValueRequestedOnly(false);

    // Keep MFP happy
    jsMsg.setGuaranteedValueStartTick(-1);
    jsMsg.setGuaranteedValueCompletedPrefix(-1);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addBestEffortMessage");
  }

  /**
   * Remove a message from the appropriate source stream.
   * This is done by replacing the message with Silence in the stream
   * Since it can only be called when a message is already in the stream the
   * stream must exist. If it doesn't we have an internal error
   *
   * @param msgItem The message to remove
   */
  public boolean removeMessage(SIMPMessage msgItem) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeMessage", new Object[] { msgItem });

    boolean msgRemoved = true;
    StreamSet streamSet = getStreamSet();

    int priority = msgItem.getPriority();
    Reliability reliability = msgItem.getReliability();

    SourceStream sourceStream = (SourceStream) streamSet.getStream(priority, reliability);

    if(sourceStream != null )
    {
      msgRemoved = sourceStream.writeSilenceForced(msgItem);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeMessage", Boolean.valueOf(msgRemoved));
    
    return msgRemoved;
  }



  /**
   * Put a message back into the appropriate source stream. This will create a stream
   * if one does not exist but will not change any fields in the message
   *
   * @param msgItem The message to be restored
   * @param commit  Boolean indicating whether message to be restored is in commit state
   */
  public void restoreMessage(SIMPMessage msgItem, boolean commit) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.entry(tc, "restoreMessage", new Object[] { msgItem });

    int priority = msgItem.getPriority();
    Reliability reliability = msgItem.getReliability();
    SIBUuid12 streamID = msgItem.getGuaranteedStreamUuid();
    StreamSet streamSet = getStreamSet(streamID);

    SourceStream sourceStream = null;
    synchronized(streamSet)
    {
      sourceStream = (SourceStream) streamSet.getStream(priority, reliability);

      if(sourceStream == null && reliability.compareTo(Reliability.BEST_EFFORT_NONPERSISTENT) > 0)
      {
        sourceStream = createStream(streamSet,
                                    priority,
                                    reliability,
                                    streamSet.getPersistentData(priority, reliability),
                                    true );
      }
    }

    // NOTE: sourceStream should only be null for express qos
    if(sourceStream != null)
    {
      if( !commit )
        sourceStream.restoreUncommitted(msgItem);
      else
        sourceStream.restoreValue(msgItem);

    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restoreMessage");
  }

  /**
   * Put a message back into the appropriate source stream. This will create a stream
   * if one does not exist but will not change any fields in the message
   *
   * @param msgItem The message to be restored
   */
  public void reallocate(boolean allMsgs) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.entry(tc, "reallocate");

    // There will not be a reallocator if this streamSet is used for PubSub
    if( pointTopoint )
    {
      // Force the reallocator to delete mesages from the ItemStream
      // when it finishes if it has removed them from the stream
      reallocator.reallocateMsgs(this.destinationHandler, allMsgs, true);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reallocate");
  }

  /**
   * Process an Ack control message. This method uses the completed prefix in the
   * ack to decide if the stream's completed prefix should be advanced.
   *
   * @param ackMsg The Ack control message
   * @return List of messages which are now complete and may be deleted.
   * @throws SIResourceException
   */
  public List processAck(ControlAck ackMsg) throws SIRollbackException, SIConnectionLostException, SIResourceException, SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processAck",
        new Object[] { ackMsg });

    // Get the ackPrefix from the message
    long ackPrefix = ackMsg.getAckPrefix();

    List indexList = processAck(ackMsg, ackPrefix);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processAck", indexList);

    return indexList;

  }

  /**
   * Process an Ack control message but use the specified ack prefix to decide if the
   * ack prefix can be advanced.
   *
   * @param ackMsg The Ack message
   * @param ackPrefix The new ack prefix
   * @return List of messages which are now completed and may be deleted
   * @throws SIResourceException
   */
  public List processAck(ControlAck ackMsg, long ackPrefix) throws SIRollbackException, SIConnectionLostException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processAck", new Object[] { ackMsg, new Long(ackPrefix) });

    List indexList = null;

    // Short circuit if incoming message has wrong stream ID
    if (!hasStream(ackMsg.getGuaranteedStreamUUID()))
    {
      // Bogus stream, ignore the message
      // Return a null indexList to ensure no further processing is
      // attempted by the caller	
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "processAck",new Object[] {"unknown stream ID - returning null array list(message ignored)"});
      return indexList;
    }

    int priority = ackMsg.getPriority().intValue();
    Reliability reliability = ackMsg.getReliability();

    // There is no stream for BestEffort non persistent messages
    if( reliability.compareTo(Reliability.BEST_EFFORT_NONPERSISTENT) > 0)
    {
      StreamSet streamSet = getStreamSet();
      SourceStream sourceStream = (SourceStream) streamSet.getStream(priority, reliability);

      // If there's no source stream then priority was bogus so ignore the ack
      if (sourceStream == null)
      {
        // Return a null indexList to ensure no further processing is
        // attempted by the caller	
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "processAck", new Object[] {"unknown priority - returning null array list(message ignored)"});
        return indexList;
      }

      // If this increases the finality prefix then
      // update it and delete the acked messages from the ItemStream
      long completedPrefix = sourceStream.getAckPrefix();
      if (ackPrefix > completedPrefix)
      {
        // Update the completedPrefix and the oack value for the stream
        // returns a lit of the itemStream ids of the newly Acked messages
        // which we can then remove from the itemStream
        indexList = sourceStream.writeAckPrefix(ackPrefix);
      }
    }
    else
    {
      // We didn't expect an Ack for a best effort message so write an entry
       // to the trace log indicating that it happened
       if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
         SibTr.debug(tc, "Unexpected Ack message for BEST_EFFORT_NONPERSISTENT message ");
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processAck", indexList);

    return indexList;

  }

  /**
   * Process a Nack control message. This should cause the source stream to resend
   * the original message.
   *
   * @param nackMsg The Nack control message
   */
  public void processNack(ControlNack nackMsg) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processNack", nackMsg);

    // Short circuit if incoming message has wrong stream ID
    if (!hasStream(nackMsg.getGuaranteedStreamUUID()))
    {
      // Bogus stream, ignore the message
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "processNack","unknown stream ID - message ignored");
      return;
    }

    int priority = nackMsg.getPriority().intValue();
    Reliability reliability = nackMsg.getReliability();

    // There is no stream for BestEffort non persistent messages
    if( reliability.compareTo(Reliability.BEST_EFFORT_NONPERSISTENT) > 0)
    {
      StreamSet streamSet = getStreamSet();
      // Get the outputStream to send the necessary messages
      SourceStream sourceStream = (SourceStream) streamSet.getStream(priority, reliability);

      // If there's no source stream then priority was bogus so ignore the nack
      if (sourceStream == null) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "processNack", "unknown priority - message ignored");
        return;
      }

      sourceStream.processNack(nackMsg);
    }
    else
    {
      // We didn't expect a Nack for this stream so throw it away but write an entry
      // to the trace log indicating that it happened
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
       SibTr.debug(tc, "Unexpected Nack message for BEST_EFFORT_NONPERSISTENT message ");
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processNack");
  }

  /**
   * returns true if a given set of streams are flushed
   *
   * @param streamID
   */
  public boolean isFlushed(SIBUuid12 streamID)
  {
    //note that although it is true that this method is currently
    //equivalent to !hadStream(streamID), it may not always be true in
    //the future.
    if(streamSet == null) return true;

    return !streamID.equals(streamSet.getStreamID());
  }

  /**
   * Returns true if the given message was uncommitted at the time of the
   * last reallocation of this stream. If so we need to reallocate the
   * stream again.
   * @param msg
   * @return boolean
   * @throws SIResourceException
   */
  public boolean isReallocationRequired(SIMPMessage msg)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isReallocationRequired", new Object[] { msg });

    boolean reallocate = false;

    JsMessage jsMsg = msg.getMessage();
    SIBUuid12 streamID = msg.getGuaranteedStreamUuid();
    StreamSet streamSet = getStreamSet();

    if(!streamID.equals(streamSet.getStreamID()))
    {
      SIResourceException e = new SIResourceException(
                                nls.getFormattedMessage(
                                  "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                  new Object[] {
                                    "com.ibm.ws.sib.processor.gd.SourceStreamManager",
                                    "1:717:1.102"},
                                  null));

      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.gd.SourceStreamManager.isReallocationRequired",
        "1:724:1.102",
        this);

      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.gd.SourceStreamManager",
          "1:731:1.102"});

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "isReallocationRequired", e);

      throw e;
    }

    int priority = msg.getPriority();
    Reliability reliability = msg.getReliability();

    // Get the stream. Note that no synchronization is required here
    // since we can't be here if a flush is in progress.
    SourceStream sourceStream = (SourceStream) streamSet.getStream(priority,reliability);

    if(sourceStream != null)
    {
      TickRange tr = sourceStream.getTickRange(jsMsg.getGuaranteedValueValueTick());

      // Need to check if it was in uncommitting state AND if it is still outside
      // the send window.
      if (tr.isReallocationRequired() && 
          sourceStream.isOutsideSendWindow(jsMsg.getGuaranteedValueValueTick()))
        reallocate = true;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isReallocationRequired", Boolean.valueOf(reallocate));

    return reallocate;
  }

  /**
   * returns true if a given set of streams exists
   *
   * @param streamID
   */
  public boolean hasStream(SIBUuid12 streamID)
  {
    //note that although it is true that this method is currently
    //equivalent to !isFlushed(streamID), it may not always be true in
    //the future.
    if(streamSet == null) return false;
    return streamID.equals(streamSet.getStreamID());
  }

  /**
   * Create a new Source Stream and store it in the given StreamSet
   *
   * @param streamSet
   * @param priority
   * @param reliability
   * @param sendWindow size
   * @parm  boolean indicating whether this was called during a restore
   * @return a new SourceStream
   * @throws SIResourceException
   */
  private SourceStream createStream(StreamSet streamSet,
                              int priority,
                              Reliability reliability,
                              long sendWindow,
                              boolean restore ) throws SIResourceException

  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createStream",
        new Object[]{streamSet, new Integer(priority), reliability, new Long(sendWindow), Boolean.valueOf(restore)});

    SourceStream stream = createStream(streamSet, priority, reliability);

    // We only use the sendWindow for PtoP in which case the SourceStreamManager
    // will be persistent as it perists the sendWindow for each stream
    if( pointTopoint)
    {
      stream.initialiseSendWindow(sendWindow, messageProcessor.getDefinedSendWindow());

      // If this stream is being recreated after a restore although we
      // may have restored a valid targetMEUuid we don't want to send any messages
      // until the link is started and we know both endpoint MEs
      // Note, defect 238709: An MQLinkHandler is deemed not to be a link
      if( restore && destinationHandler.isLink()&& !destinationHandler.isMQLink() )
      {
        // This will stop the stream sending messages
        stream.guessesInStream();
      }

    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createStream", stream);
    return stream;
  }

  /**
   * Create a new Source Stream and store it in the given StreamSet
   *
   * @param streamSet
   * @param priority
   * @param reliability
   * @return a new SourceStream
   * @throws SIResourceException
   */
  private SourceStream createStream(StreamSet streamSet,
                              int priority,
                              Reliability reliability) throws SIResourceException

  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createStream", new Object[] {streamSet, new Integer(priority), reliability});

    SourceStream stream = null;

    //there is no source stream for express messages
    if(reliability.compareTo(Reliability.BEST_EFFORT_NONPERSISTENT) > 0)
    {
      //Warning - this assumes that ASSURED is always the highest Reliability
      //and that UNKNOWN is always the lowest (0).
      stream = new SourceStream(priority, //priority
                        reliability, //reliability
                        downControl,
                        new ArrayList(),
                        streamSet,
                        messageProcessor.getAlarmManager(),
                        destinationHandler);
    }

    streamSet.setStream(priority, reliability, stream);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createStream", stream);

    return stream;
  }

  /**
   * Set the StreamSet to be the given one, which will probably have
   * been restored from the the message store
   *
   * @param newStreamSet
   * @throws SIStoreException
   */
  public void reconstituteStreamSet(StreamSet newStreamSet)

  throws SIRollbackException, SIConnectionLostException, SIResourceException, SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reconstituteStreamSet", streamSet);

    //remove the old one
    if(streamSet != null) streamSet.remove();
    //replace it with a new one
    streamSet = newStreamSet;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reconstituteStreamSet");
  }

  /**
   * Attach the appropriate completed and duplicate prefixes for the
   * stream stored in this array to a ControlNotFlushed message.
   *
   * @param msg The ControlNotFlushed message to stamp.
   */
  public ControlNotFlushed stampNotFlushed(ControlNotFlushed msg) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "stampNotFlushed", new Object[] { msg });

    int count = 0;

    //the maximum possible number of streams in a set
    int max = (SIMPConstants.MSG_HIGH_PRIORITY+1) * (Reliability.MAX_INDEX+1);
    //an array of priorities
    int[] ps = new int[max];
    //an array of reliabilities
    int[] qs = new int[max];
    //an array of prefixes
    long[] cs = new long[max];

    StreamSet streamSet = getStreamSet();

    //iterate through the non-null streams
    Iterator itr = streamSet.iterator();
    while(itr.hasNext())
    {
      SourceStream sourceStream = (SourceStream) itr.next();

      // for each stream, store it's priority, reliability and completed prefix
      ps[count] = sourceStream.getPriority();
      qs[count] = sourceStream.getReliability().toInt();
      cs[count] = sourceStream.getCompletedPrefix();
      count++;
    }

    //create some arrays which are of the correct size
    int[] realps = new int[count];
    int[] realqs = new int[count];
    long[] realcs = new long[count];

    //copy the data in to them
    System.arraycopy(ps, 0, realps, 0, count);
    System.arraycopy(qs, 0, realqs, 0, count);
    System.arraycopy(cs, 0, realcs, 0, count);

    //set the appropriate message fields
    msg.setCompletedPrefixPriority(realps);
    msg.setCompletedPrefixQOS(realqs);
    msg.setCompletedPrefixTicks(realcs);

    msg.setDuplicatePrefixPriority(realps);
    msg.setDuplicatePrefixQOS(realqs);
    msg.setDuplicatePrefixTicks(realcs);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "stampNotFlushed", msg);
    //return the message
    return msg;
  }

  /**
   * Consolidates the sourceStreams following restart recovery.
   * The streams may have scattered tick values derived from persisted
   * messages or references.
 * @param startMode
   */
  public List consolidateStreams(int startMode) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "consolidateStreams");

    List<List> sentMsgs = new ArrayList<List>();

    StreamSet streamSet = getStreamSet();

    //iterate over the non-null streams
    Iterator itr = streamSet.iterator();

    List temp = null;

    while(itr.hasNext())
    {
      SourceStream stream = (SourceStream) itr.next();
      // Get lsit of the mesages inside the sendWindow on this stream
      // and add this list onto end of sentMsgs list
      temp = stream.restoreStream(startMode);
      if( temp != null)
        sentMsgs.addAll(temp);

      // This is done after the restore as it may have been
      // changed by Admin and we need to work towards it as usual
      if( pointTopoint)
        stream.setDefinedSendWindow(messageProcessor.getDefinedSendWindow());
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "consolidateStreams", sentMsgs);

    return sentMsgs;
  }

  /**
   * Check whether a particular set of streams is flushable.  A stream is
   * flushable if the use count is zero and none of the component
   * streams contain a V/U or L/U tick
   *
   * @return true if the set of streams are all flushable, and false otherwise.
   */
  public boolean flushable() throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "flushable");

    // Short circuit if we've never created a stream
    if (streamSet == null)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "flushable", Boolean.TRUE);
      return true;
    }

    StreamSet streamSet = getStreamSet();

    //iterate over the non-null streams
    Iterator itr = streamSet.iterator();
    while(itr.hasNext())
    {
      SourceStream stream = (SourceStream) itr.next();
      if(!stream.flushable())
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "flushable", Boolean.FALSE);
        return false;
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "flushable", Boolean.TRUE);
    return true;
  }

  /**
   * Wait for the current stream to quiesce, remove it
   * from storage, then create a replacement stream ID.
   * We assume that all production has already been
   * stopped and that no new production will occur until
   * after the flush has been completed.  The reference
   * passed to this method contains the callback for
   * signalling when the flush has completed.
   *
   * @param complete An instance of the FlushComplete interface
   * which we'll invoke when the flush of the current stream
   * has completed.
   * @throws FlushAlreadyInProgressException if someone calls
   * this method but a flush is already in progress.
   */
  public void startFlush(FlushComplete complete)
    throws FlushAlreadyInProgressException, SIRollbackException, SIConnectionLostException, SIResourceException, SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "startFlush", new Object[] { complete });

    // Synchronize here to get a consistent view of "flushInProgress"
    synchronized (this)
    {
      if (flushInProgress != null)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "startFlush", "FlushAlreadyInProgressException");
        // Somebody already called us so bail
        throw new FlushAlreadyInProgressException();
      }

      // Otherwise, update flush in progress and
      // flush immediately if possible.  Note that
      // this means the caller may receive the callback
      // on their own thread.
      flushInProgress = complete;
    }

    attemptFlush();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "startFlush");
  }

  /**
   * If a flush is in progress, then give it a try.  This method
   * is exposed so that it may be invoked by higher level code
   * which knows when it's safe to try a flush.
   */
  public void attemptFlushIfNecessary()
    throws SIRollbackException, SIConnectionLostException, SIResourceException, SIErrorException
  {
    if (flushInProgress != null)
      attemptFlush();
  }

  /**
   * This method will be called periodically to check
   * if a stream is flushable, and if so will actually
   * flush it.
   */
  protected void attemptFlush() throws SIRollbackException, SIConnectionLostException, SIResourceException, SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc,"attemptFlush");

    //The FlushComplete callback
    FlushComplete callback = null;
    //The old set of streams which has just been flushed
    StreamSet oldStreamSet = null;

    // Actions for a flushable stream.  Note that we synchronize
    // up to step 4 to avoid races with threads handling control
    // messages.
    // 1. Cache and unset flushInProgress.
    // 2. Create a new stream ID
    // 3. Remove any data structures associated with the stream.
    // 4. Invoke the callback on the cached FlushComplete reference.
    synchronized (this)
    {
      // Resolve race between control message handlers who both
      // see a flushable stream.
      // flushInProgress was set by startFlush
      if (flushInProgress == null)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "attemptFlush");
        return;
      }

      // Also verify that the stream really is flushable in case
      // a thread handling a control message called us with a
      // stale source stream reference.
      if (!flushable())
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "attemptFlush");
        return;
      }

      // Now cache the callback.  When we exit the synchronize block,
      // the flush will have officially ended.
      callback = flushInProgress;
      flushInProgress = null;

      // Cleanup the data structures and null out the
      // existing streamSet so we force creation of a new
      // set on the next message.
      oldStreamSet = streamSet;
      streamSet = null;

      if(oldStreamSet != null)
      {
        oldStreamSet.remove();
      }
    }

    // Send out a flushed message.  If this fails, make sure we get
    // to at least invoke the callback.
    try
    {
      // The Cellule argument is null as it is ignored by our parent handler
      // this flush message should be broadcast downstream.
      if (oldStreamSet != null)
        downControl.sendFlushedMessage(null, oldStreamSet.getStreamID());
    }
    catch (Exception e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.gd.SourceStreamManager.attemptFlush",
        "1:1158:1.102",
        this);

      // Note that it doesn't make much sense to throw an exception here since
      // this is a callback from stream array map.
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "attemptFlush", e);
      return;
    }
    finally
    {
      // Invoke the calback in the finally clause so we don't holdup
      // whoever's waiting for the flush to complete.
      callback.flushComplete(destinationHandler);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "attemptFlush");
  }

//d477883 - We no longer flush the streams as part of a deleteAll/reallocateAll
//This stream may still be used afterwards and therefore we dont want to
//null out any resources. 
//  /**
//   * This method will be called to force flush of streamSet
//   */
//  public void forceFlush() throws SIRollbackException, SIConnectionLostException, SIResourceException, SIErrorException
//  {
//    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
//      SibTr.entry(tc,"forceFlush");
//
//    synchronized (this)
//    {
//      // Cleanup the data structures
//      streamSet.remove();
//      streamSet.dereferenceControlAdapter();
//    }
//
//    // Send out a flushed message.  If this fails, make sure we get
//    // to at least invoke the callback.
//    try
//    {
//      // The Cellule argument is null as it is ignored by our parent handler
//      // this flush message should be broadcast downstream.
//      downControl.sendFlushedMessage(null, streamSet.getStreamID());
//      streamSet = null;
//    }
//
//    catch (Exception e)
//    {
//      // FFDC
//      FFDCFilter.processException(
//        e,
//        "com.ibm.ws.sib.processor.gd.SourceStreamManager.forceFlush",
//        "1:1212:1.102",
//        this);
//
//      // Note that it doesn't make much sense to throw an exception here since
//      // this is a callback from stream array map.
//      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
//        SibTr.exit(tc, "forceFlush", e);
//      return;
//    }
//
//    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
//      SibTr.exit(tc, "forceFlush");
//  }


  public boolean updateSourceStream(SIMPMessage msg, boolean isSilence) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "updateSourceStream", new Object[] { msg });

    boolean sendMessage = false;

    SIBUuid12 streamID = msg.getGuaranteedStreamUuid();

    StreamSet streamSet = getStreamSet();

    if(!streamID.equals(streamSet.getStreamID()))
    {
      SIResourceException e = new SIResourceException(nls.getFormattedMessage(
        "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.gd.SourceStreamManager",
          "1:1244:1.102" },
        null));

      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.gd.SourceStreamManager.updateSourceStream",
        "1:1251:1.102",
        this);

      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.gd.SourceStreamManager",
          "1:1258:1.102" });

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "updateSourceStream", e);

      throw e;
    }

    int priority = msg.getPriority();
    Reliability reliability = msg.getReliability();

    // Get the stream. Note that no synchronization is required here
    // since we can't be here if a flush is in progress.
    SourceStream sourceStream = (SourceStream) streamSet.getStream(priority,reliability);

    if(sourceStream != null)
    {
      // Write the value message to the stream
      // This will also write a range of Completed ticks between
      // the previous Value tick and the new one
      if(isSilence)
        sendMessage = sourceStream.writeSilence(msg);
      else
        sendMessage = sourceStream.writeValue(msg);
    }
    else
    {
      // The message must be EXPRESS so we always want to send it
      sendMessage = true;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateSourceStream", Boolean.valueOf(sendMessage));

    return sendMessage;
  }

  /**
   * Handle a flush query.
   *
   * @param flushQuery The control message which elicited the query.
   * @throws SIResourceException I'm pretty sure this can't actually
   * be thrown.
   */
  public void processFlushQuery(ControlAreYouFlushed flushQuery) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processFlushQuery", new Object[] { flushQuery });

    SIBUuid12 streamID = flushQuery.getGuaranteedStreamUUID();

    try
    {
      //synchronize to give a consistent view of the flushed state
      synchronized(this)
      {
        SIBUuid8 requestor = flushQuery.getGuaranteedSourceMessagingEngineUUID();
        if(isFlushed(streamID))
        {
          // It's flushed.  Send a message saying as much.
          downControl.sendFlushedMessage(requestor, streamID);
        }
        else
        {
          // Not flushed.  Send a message saying as much.
          downControl.sendNotFlushedMessage(requestor, streamID, flushQuery.getRequestID());
        }
      }
    }
    catch (SIResourceException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.gd.SourceStreamManager.processFlushQuery",
        "1:1333:1.102",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "processFlushQuery", e);

      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processFlushQuery");

  }

  /**
   * This method should only be called when the PtoPOutputHandler was created
   * with an unknown targetCellule and WLM has now told us correct targetCellule.
   * This can only happen when the SourceStreamManager is owned by a
   * PtoPOutputHandler within a LinkHandler
   */
  public synchronized void updateTargetCellule( SIBUuid8 targetMEUuid ) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "updateTargetCellule", targetMEUuid);

    if( pointTopoint)
    {
      this.targetMEUuid = targetMEUuid;

      StreamSet streamSet = getStreamSet();

       // We may not have had any messages yet in which case there
       // is no streamSet to update
      if( streamSet != null)
      {
        //Update the cellule in the StreamSet
        //and persist this
        streamSet.updateCellule(targetMEUuid);
        Transaction tran = txManager.createAutoCommitTransaction();
        try
        {
          streamSet.requestUpdate(tran);
        }
        catch (MessageStoreException e)
        {
          // MessageStoreException shouldn't occur so FFDC.
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.gd.SourceStreamManager.updateTargetCellule",
            "1:1384:1.102",
            this);

          SibTr.exception(tc, e);

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "updateTargetCellule", e);

          throw new SIResourceException(e);
        }

        //iterate over the non-null streams
        Iterator itr = streamSet.iterator();
        while(itr.hasNext())
        {
          SourceStream stream = (SourceStream) itr.next();
          stream.noGuessesInStream();
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateTargetCellule");
  }

  /**
   *
   * @param  ackMsg The Ack message
   * @return SourceStream to be passed to batchHandler for batchListener callbacks
   * @throws SIResourceException
   */
  public SourceStream getBatchListener(ControlAck ackMsg) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getBatchListener", new Object[] { ackMsg });

    int priority = ackMsg.getPriority().intValue();
    Reliability reliability = ackMsg.getReliability();

    StreamSet streamSet = getStreamSet();

    SourceStream sourceStream = (SourceStream) streamSet.getStream(priority, reliability);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
         SibTr.exit(tc, "getBatchListener", sourceStream );

    return sourceStream;
  }

  public DestinationHandler getDestinationHandler() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getDestinationHandler");
      SibTr.exit(tc, "getDestinationHandler", destinationHandler);
    }
    return destinationHandler;
  }

}
