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
package com.ibm.ws.sib.processor.runtime.impl;

import java.util.Iterator;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;
import com.ibm.ws.sib.processor.gd.InternalOutputStream;
import com.ibm.ws.sib.processor.gd.InternalOutputStreamManager;
import com.ibm.ws.sib.processor.gd.StreamSet;
import com.ibm.ws.sib.processor.impl.exceptions.InvalidOperationException;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.itemstreams.ProtocolItemStream;
import com.ibm.ws.sib.processor.runtime.DeliveryStreamType;
import com.ibm.ws.sib.processor.runtime.HealthState;
import com.ibm.ws.sib.processor.runtime.IndoubtAction;
import com.ibm.ws.sib.processor.runtime.SIMPDeliveryTransmitControllable;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPPubSubOutboundTransmitControllable;
import com.ibm.ws.sib.processor.runtime.SIMPTransmitMessageControllable;
import com.ibm.ws.sib.processor.runtime.SIMPTransmitMessageControllable.State;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 *
 */
public class InternalOutputStreamSetControl
  extends AbstractControlAdapter
    implements SIMPPubSubOutboundTransmitControllable
{
  private static final  TraceComponent tc =
    SibTr.register(
      InternalOutputStreamSetControl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  

  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  protected StreamSet _streamSet;
  protected InternalOutputStreamManager _ioStreamManager;
  protected HealthStateTree _healthState;

  protected RemoteTopicSpaceControl parent;

  public static final String STREAM_SET_UNINITIALISED_STRING = "STREAM_SET_UNINITIALISED";

  /**
   * Use this inner class to iterate over messages on all of the
   * streams in this set.
   * Returns objects of type TransmitMessage.
   */
  private class InternalOutputStreamSetXmitMessageIterator implements SIMPIterator
  {
    Iterator streamSetIterator = null;
    InternalOutputStream currentStream=null;
    Iterator<SIMPTransmitMessageControllable> currentStreamMsgIterator=null;
    int index, max;
    boolean allMsgs;

    public InternalOutputStreamSetXmitMessageIterator(int maxMsgs) throws SIResourceException
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "InternalOutputStreamSetXmitMessageIterator", Integer.valueOf(maxMsgs));

      index = 0; // reset position
      this.max = maxMsgs;
      allMsgs = (maxMsgs == SIMPConstants.SIMPCONTROL_RETURN_ALL_MESSAGES);

      streamSetIterator = _streamSet.iterator();
      //move tot he first stream
      moveToNextStream();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "InternalOutputStreamSetXmitMessageIterator", this);
    }

    /**
     * Attempt to move the currentMsgIterator to point to the next
     * stream with messages
     * @author tpm
     */
    private void moveToNextStream()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "moveToNextStream");
      //we keep looking at the streams until we find a stream
      //with some messages or until we reach the end
      boolean keepSearchingStreams = true;
      while(keepSearchingStreams)
      {
        if(streamSetIterator.hasNext())
        {
          //see if this stream has messages
          currentStream = (InternalOutputStream)streamSetIterator.next();
          currentStreamMsgIterator =
            currentStream.getControlAdapter().getTransmitMessagesIterator(SIMPConstants.SIMPCONTROL_RETURN_ALL_MESSAGES);
          if(currentStreamMsgIterator.hasNext())
          {
            //yes it does, we can stop looking
            keepSearchingStreams = false;
          }
        }
        else
        {
          //we are at the last stream - we cannot trundle further
          keepSearchingStreams = false;
        }
      }//end while
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "moveToNextStream");
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    public Object next()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "next");

      SIMPTransmitMessageControllable returnMessage = null;
      if (allMsgs || index < max)
      {
        if(!currentStreamMsgIterator.hasNext())
        {
          moveToNextStream();
        }
        //if we did not successfully move on to the next stream
        //then there are no more messages - we behave the same as a normal
        //iterators when next() is called when all elements have been
        //iterated.
        returnMessage = currentStreamMsgIterator.next();
        index++;
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "next", returnMessage);
      return returnMessage;
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "hasNext");
      if(currentStreamMsgIterator == null || !currentStreamMsgIterator.hasNext())
      {
        moveToNextStream();
      }
      boolean returnValue = (allMsgs || (index < max)) && currentStreamMsgIterator != null && currentStreamMsgIterator.hasNext() ;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "hasNext", new Boolean(returnValue));
      return returnValue;
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#remove()
     */
    public void remove()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "remove");

      InvalidOperationException finalE =
        new InvalidOperationException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {"InternalOutputStreamSetControl.InternalOutputStreamSetXmitMessageIterator.remove",
                          "1:218:1.30",
                          this},
            null));

      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "remove", finalE);
      throw finalE;
    }

    public void finished()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "finished");
      streamSetIterator = null;
      currentStream=null;
      currentStreamMsgIterator=null;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "finished");
    }

  }

  public InternalOutputStreamSetControl(StreamSet streamSet)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "InternalOutputStreamSetControl", new Object[]{streamSet});

    this._streamSet = streamSet;
    this._healthState = new HealthStateTree();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "InternalOutputStreamSetControl", this);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPPtoPOutboundTransmitControllable#getTransmitMessagesIterator()
   */
  public SIMPIterator getTransmitMessagesIterator(int maxMsgs) throws SIMPRuntimeOperationFailedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTransmitMessagesIterator");

    SIMPIterator msgItr = null;

    try
    {
      msgItr = new InternalOutputStreamSetXmitMessageIterator(maxMsgs);
    }
    catch (SIResourceException e)
    {
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.InternalOutputStreamSetControl.getTransmitMessagesIterator",
          "1:272:1.30",
          this);

        SIMPRuntimeOperationFailedException finalE =
          new SIMPRuntimeOperationFailedException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0002",
              new Object[] {"InternalOutputStreamSetControl.getTransmitMessagesIterator",
                  "1:280:1.30",
                            e},
              null), e);

        SibTr.exception(tc, finalE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getTransmitMessagesIterator", finalE);
        throw finalE;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTransmitMessagesIterator", msgItr);

    return msgItr;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPPubSubOutboundTransmitControllable#getTransmitMessageByID(java.lang.String)
   */
  public SIMPTransmitMessageControllable getTransmitMessageByID(String id)
    throws SIMPControllableNotFoundException, SIMPRuntimeOperationFailedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTransmitMessageByID", id);

    SIMPTransmitMessageControllable msg = null;
    SIMPIterator it = getStreams();
    while (it.hasNext() && msg==null)
    {
      msg = ((InternalOutputStreamControl)it.next()).getTransmitMessageByID(id);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTransmitMessageByID", msg);
    return msg;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetControllable#getType()
   */
  public DeliveryStreamType getType()
  {
    return DeliveryStreamType.PUBSUB_SOURCE;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getId()
   */
  public String getId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getId");

    SIBUuid12 streamID = _streamSet.getStreamID();
    String streamIDStr = (streamID != null) ? streamID.toString() : STREAM_SET_UNINITIALISED_STRING;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getId", streamIDStr);
    return streamIDStr;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getName()
   */
  public String getName()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#checkValidControllable()
   */
  public void assertValidControllable() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "assertValidControllable");

    if(_streamSet == null || _ioStreamManager == null)
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {"InternalOutputStreamControl.assertValidControllable",
                          "1:363:1.30",
                          _streamSet},
            null));

      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exception(tc, finalE);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "assertValidControllable", finalE);

      throw finalE;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "assertValidControllable");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#dereferenceControllable()
   */
  public void dereferenceControllable()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "dereferenceControllable");

    _streamSet = null;
    _ioStreamManager = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "dereferenceControllable");

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#registerControlAdapterAsMBean()
   */
  public void registerControlAdapterAsMBean()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#deregisterControlAdapterMBean()
   */
  public void deregisterControlAdapterMBean()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#runtimeEventOccurred(com.ibm.ws.sib.admin.RuntimeEvent)
   */
  public void runtimeEventOccurred(RuntimeEvent event)
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetTransmitControllable#clearMessagesAtSource(byte)
   *
   *Function Name:clearMessagesAtSource
   *
   *Parameters:  indoubtAction determines how indoubt messages are handled.
   *             INDOUBT_EXCEPTION causes indoubt messages to be sent to the exception destination,
   *             this gives the possibility that the messages are duplicated.
   *             INDOUBT_REALLOCATE causes the messages to be reallocated,
   *             possbly to the exception destination, and possibly duplicated.
   *             INDOUBT_DELETE causes indoubt messages to be discarded, risking their loss.
   *             INDOUBT_LEAVE means no action is taken for indoubt messages, so that
   *             the target system must recover for progress to be made.
   *
   *Description: If the target system has been deleted this routine will reallocate the
   *             not indoubt messages and then deal with the indoubts according to the indoubtAction.
   *             It will then send a Flushed message to the target and remove the sourcestream
   *
   *Throws: invalidStreamTypeException if the stream is not a source stream.
   *
   */
  public void clearMessagesAtSource(IndoubtAction indoubtAction)
    throws SIMPControllableNotFoundException, SIMPRuntimeOperationFailedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.entry(tc, "clearMessagesAtSource");

    assertValidControllable();

    synchronized(_streamSet)
    {
      boolean foundUncommittedMsgs = false;
      //we do not reallocate for an individual InternalOutputStreamSet

      // Next delete all indoubt messages from the Message store
      // Unless we have been told to leave them there
      // Since any reallocated messages have been replace by Silence
      // in the streams we can assume we want to work on all messages which
      // remain in the stream
      if( indoubtAction != IndoubtAction.INDOUBT_LEAVE)
      {
        // This is an iterator over all messages in all the source streams
        // in this streamSet. There may be Uncommitted messages in this list
        // but getSIMPMessage will return null for these as they don't have a
        // valid ItemStream id so we won't try to delete them
        Iterator itr = this.getTransmitMessagesIterator(SIMPConstants.SIMPCONTROL_RETURN_ALL_MESSAGES);

        TransmitMessage xmitMsg = null;
        String state=null;

        while(itr != null && itr.hasNext())
        {
          xmitMsg = (TransmitMessage)itr.next();

          state = xmitMsg.getState();

          if (state.equals( State.COMMITTING.toString() ) )
          {
            foundUncommittedMsgs = true;
          }
          else
          {
            try
            {
              // We can operate on this message
              SIMPMessage msg = xmitMsg.getSIMPMessage();
              if( msg != null)
              {

                //this will also check whether the msg should ber emoved
                //from the store
                _ioStreamManager.removeMessage(_streamSet, msg);
              }
            }
            catch (SIResourceException e)
            {
              FFDCFilter.processException(
                  e,
                  "com.ibm.ws.sib.processor.runtime.InternalOutputStreamSetControl.clearMessagesAtSource",
                  "1:496:1.30",
                  this);

                SIMPRuntimeOperationFailedException finalE =
                  new SIMPRuntimeOperationFailedException(
                    nls.getFormattedMessage(
                      "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                      new Object[] {"InternalOutputStreamSetControl.clearMessagesAtSource",
                          "1:504:1.30",
                                    e},
                      null), e);

                SibTr.exception(tc, finalE);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "clearMessagesAtSource", finalE);
                throw finalE;
            }
          }
        }
      }

      // Now send a Flushed message and remove streamSet from ioStreamManager
      if( foundUncommittedMsgs == false)
      {
        _ioStreamManager.forceFlush(_streamSet.getStreamID());
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.exit(tc, "clearMessagesAtSource");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetTransmitControllable#containsGuesses()
   */
  public boolean containsGuesses()
  {
    return false;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetTransmitControllable#forceFlushAtSource()
   */
  public void forceFlushAtSource()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetTransmitControllable#getCurrentMaxIndoubtMessages(int, int)
   */
  public int getCurrentMaxIndoubtMessages(int priority, int COS)
  {
    return 0;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetTransmitControllable#getStreams
   */
  public SIMPIterator getStreams() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getStreams");
    Iterator it = null;
    try
    {
      it = _streamSet.iterator();
    }
    catch (SIResourceException e)
    {
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.InternalOutputStreamSetControl.getStreams",
          "1:567:1.30",
          this);

        SIMPRuntimeOperationFailedException finalE =
          new SIMPRuntimeOperationFailedException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0002",
              new Object[] {"InternalOutputStreamSetControl.getStreams",
                  "1:575:1.30",
                            e},
              null), e);

        SibTr.exception(tc, finalE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getStreams", finalE);
        //throw finalE;
    }
    SIMPIterator returnIterator=new InternalOutputStreamControllableIterator(it);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getStreams", returnIterator);
    return returnIterator;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPPubSubOutboundTransmitControllable#getAttatchedRemoteSubscribers
   */
  public SIMPIterator getAttatchedRemoteSubscribers()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getAttatchedRemoteSubscribers");

    // TODO : This method needs to throw runtime invalid to admin
    SIMPIterator itr = null;
    try
    {
      ProtocolItemStream protocolItemStream =
        ((ProtocolItemStream)_streamSet.getItemStream());
      DestinationHandler destHandler = protocolItemStream.getDestinationHandler();

      itr =
        new BasicSIMPIterator(destHandler.getAOControlAdapterIterator());
    }
    catch (MessageStoreException e)
    {
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.InternalOutputStreamSetControl.getAttatchedRemoteSubscribers",
          "1:613:1.30",
          this);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getAttatchedRemoteSubscribers", itr);
    return itr;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetTransmitControllable#getDepth
   */
  public long getDepth() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getDepth");

    long count=0;
    SIMPIterator iterator = getStreams();
    while(iterator.hasNext())
    {
      InternalOutputStreamControl ios =
        (InternalOutputStreamControl)iterator.next();
      count+=ios.getNumberOfActiveMessages();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getDepth", Long.valueOf(count));
    return count;
  }

  public String getRemoteEngineUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getRemoteEngineUuid");

    String remoteMEUuid = _streamSet.getRemoteMEUuid().toString();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getRemoteEngineUuid", remoteMEUuid);
    return remoteMEUuid;
  }


  public void setParentControlAdapter(ControlAdapter parent)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setParentControlAdapter", parent);

    this.parent = (RemoteTopicSpaceControl)parent;
    this._ioStreamManager = this.parent.getOutputHandler().getInternalOutputStreamManager();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setParentControlAdapter");
  }

  public long getNumberOfMessagesSent() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getNumberOfMessagesSent");

    long count=0;
    SIMPIterator iterator = getStreams();
    while(iterator.hasNext())
    {
      InternalOutputStreamControl srcStreamControl =
        (InternalOutputStreamControl)iterator.next();
      count+=srcStreamControl.getNumberOfMessagesSent();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getNumberOfMessagesSent", new Long(count));
    return count;
  }

  public long getNumberOfUnacknowledgedMessages() {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getNumberOfUnacknowledgedMessages");

    long count=0;
    try
    {
      SIMPIterator iterator = getTransmitMessagesIterator(SIMPConstants.SIMPCONTROL_RETURN_ALL_MESSAGES);
      while(iterator.hasNext())
      {
        TransmitMessage msg =
          (TransmitMessage)iterator.next();
        if (msg.getState().equals(State.PENDING_ACKNOWLEDGEMENT.toString()))
          count++;
      }

    }
    catch(Exception e)
    {
      // FFDC
      FFDCFilter
          .processException(
              e,
              "com.ibm.ws.sib.processor.runtime.InternalOutputStreamSetControl.getNumberOfUnacknowledgedMessages",
              "1:712:1.30", this);

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] { "com.ibm.ws.sib.processor.runtime.SourceStreamSetControl.getNumberOfUnacknowledgedMessages",
                       "1:716:1.30",
                       SIMPUtils.getStackTrace(e) });
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getNumberOfUnacknowledgedMessages", Long.valueOf(count));

    return count;
  }

  public void moveMessages(boolean discard)
    throws SIMPRuntimeOperationFailedException, SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "moveMessages", Boolean.valueOf(discard));

    // Moving to exception destination makes no sense for pubsub
    clearMessagesAtSource(IndoubtAction.INDOUBT_DELETE);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "moveMessages");
  }

  public HealthState getHealthState() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getHealthState");

    // Iterate over the streams - get the worst health state
    Iterator it = null;
    try
    {
      it = getStreams();
    }
    catch (SIMPControllableNotFoundException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.runtime.InternalOutputStreamSetControl.getHealthState",
        "1:756:1.30",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getHealthState", e);
      return null;
    }
    while (it.hasNext())
    {
        SIMPDeliveryTransmitControllable control =
            (SIMPDeliveryTransmitControllable) it.next();
        _healthState.addHealthStateNode(control.getHealthState());
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getHealthState", _healthState);
    return _healthState;
  }


}

