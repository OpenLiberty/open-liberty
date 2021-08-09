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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;
import com.ibm.ws.sib.processor.gd.SourceStream;
import com.ibm.ws.sib.processor.gd.SourceStreamManager;
import com.ibm.ws.sib.processor.gd.StreamSet;
import com.ibm.ws.sib.processor.impl.exceptions.InvalidOperationException;
import com.ibm.ws.sib.processor.impl.interfaces.HealthStateListener;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.runtime.DeliveryStreamType;
import com.ibm.ws.sib.processor.runtime.HealthState;
import com.ibm.ws.sib.processor.runtime.IndoubtAction;
import com.ibm.ws.sib.processor.runtime.SIMPDeliveryTransmitControllable;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPPtoPOutboundTransmitControllable;
import com.ibm.ws.sib.processor.runtime.SIMPTransmitMessageControllable;
import com.ibm.ws.sib.processor.runtime.SIMPTransmitMessageControllable.State;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A Controllable for a set of source streams
 */
public class SourceStreamSetControl
  extends AbstractControlAdapter
    implements SIMPPtoPOutboundTransmitControllable
{
  private static final TraceComponent tc =
    SibTr.register(
      SourceStreamSetControl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  /* Output source info */

  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  protected StreamSet _streamSet;
  private SIBUuid12 _streamID;
  protected SourceStreamManager _sourceStreamManager;

  protected HealthState _healthState;

  /**
   * Use this inner class to iterate over messages on all of the
   * streams in this set.
   * Returns objects of type TransmitMessage.
   */
  protected class SourceStreamSetXmitMessageIterator implements SIMPIterator
  {
    protected Iterator streamSetIterator = null;
    protected SourceStream currentStream=null;
    protected Iterator<SIMPTransmitMessageControllable> currentStreamMsgIterator=null;
    int index, max;
    boolean allMsgs;

    public SourceStreamSetXmitMessageIterator(int maxMsgs) throws SIResourceException
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "SourceStreamSetXmitMessageIterator", Integer.valueOf(maxMsgs));

      index = 0; // reset position
      this.max = maxMsgs;
      allMsgs = (maxMsgs == SIMPConstants.SIMPCONTROL_RETURN_ALL_MESSAGES);

      streamSetIterator = _streamSet.iterator();
      //move tot he first stream
      moveToNextStream();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "SourceStreamSetXmitMessageIterator", this);
    }

    /**
     * Attempt to move the currentMsgIterator to point to the next
     * stream with messages
     * @author tpm
     */
    protected void moveToNextStream()
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
          currentStream = (SourceStream)streamSetIterator.next();
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
            new Object[] {"SourceStreamSetControl.SourceStreamSetXmitMessageIterator.remove",
                          "1:213:1.39",
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

  public SourceStreamSetControl(SIBUuid8 remoteMEUuid, StreamSet streamSet)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "SourceStreamSetControl", new Object[]{
        remoteMEUuid, streamSet});

    this._streamSet = streamSet;
    this._streamID = streamSet.getStreamID();
    this._sourceStreamManager = null;
    this._healthState = new HealthStateTree();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "SourceStreamSetControl", this);
  }

  public void setSourceStreamManager(SourceStreamManager sourceStreamManager)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setSourceStreamManager");

    this._sourceStreamManager = sourceStreamManager;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setSourceStreamManager");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPPtoPOutboundTransmitControllable#getTransmitMessagesIterator()
   */
  public SIMPIterator getTransmitMessagesIterator(int maxMsgs) throws SIMPControllableNotFoundException, SIMPRuntimeOperationFailedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTransmitMessagesIterator");

    assertValidControllable();

    SIMPIterator msgItr = null;
    try
    {
      msgItr = new SourceStreamSetXmitMessageIterator(maxMsgs);
    }
    catch (SIResourceException e)
    {
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.SourceStreamSetControl.getTransmitMessagesIterator",
          "1:283:1.39",
          this);

        SIMPRuntimeOperationFailedException finalE =
          new SIMPRuntimeOperationFailedException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0002",
              new Object[] {"SourceStreamSetControl.getTransmitMessagesIterator",
                  "1:291:1.39",
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
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetTransmitControllable#containsGuesses()
   */
  public boolean containsGuesses() throws SIMPControllableNotFoundException, SIMPRuntimeOperationFailedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "containsGuesses");

    assertValidControllable();

    boolean guess = false;

    Iterator itr = null;
    try
    {
      itr = _streamSet.iterator();
    }
    catch (SIResourceException e)
    {
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.SourceStreamSetControl.containsGuesses",
          "1:328:1.39",
          this);

        SIMPRuntimeOperationFailedException finalE =
          new SIMPRuntimeOperationFailedException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0002",
              new Object[] {"SourceStreamSetControl.containsGuesses",
                  "1:336:1.39",
                            e},
              null), e);

        SibTr.exception(tc, finalE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "containsGuesses", finalE);
        throw finalE;
    }
    while(!guess && itr.hasNext())
    {
      SourceStream stream = (SourceStream) itr.next();
      guess = stream.containsGuesses();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "containsGuesses", new Boolean(guess));

    return guess;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetTransmitControllable#getCurrentMaxIndoubtMessages(int, int)
   */
  public int getCurrentMaxIndoubtMessages(int priority, int COS)
  {
    return 0;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetTransmitControllable#forceFlushAtSource()
   */
  public void forceFlushAtSource()
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
  public void clearMessagesAtSource(IndoubtAction indoubtAction) throws SIMPControllableNotFoundException, SIMPRuntimeOperationFailedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.entry(tc, "clearMessagesAtSource");

    //This method used to be exposed by the MBeans,
    //but this is no longer the case. Instead, the exposed methods will be delete/moveAll
    //which will implicitly use this path

    assertValidControllable();

    synchronized(_streamSet)
    {
      boolean foundUncommittedMsgs = false;
      boolean reallocateAllMsgs = false;
      // Start by reallocating all messages which have not yet been sent
      // If inDoubtAction == INDOUBT_REALLOCATE then reallocate all messages
      // including those which have already been sent
      if( indoubtAction == IndoubtAction.INDOUBT_REALLOCATE)
      {
        reallocateAllMsgs = true;

        // This will be PtoP Only
        try
        {
          _sourceStreamManager.reallocate(reallocateAllMsgs);
        }
        catch (SIException e)
        {
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.runtime.SourceStreamSetControl.clearMessagesAtSource",
            "1:424:1.39",
            this);

          SIMPRuntimeOperationFailedException finalE =
            new SIMPRuntimeOperationFailedException(
              nls.getFormattedMessage(
                "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                new Object[] {"SourceStreamSetControl.clearMessagesAtSource",
                    "1:432:1.39",
                              e},
                null), e);

          SibTr.exception(tc, finalE);

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "clearMessagesAtSource", finalE);
          throw finalE;

        }
      }

      // Next delete all indoubt messages from the Message store
      // Unless we have been told to leave them there
      // Since any reallocated messages have been replace by Silence
      // in the streams we can assume we want to work on all messages which
      // remain in the stream
      if( indoubtAction != IndoubtAction.INDOUBT_LEAVE)
      {
        boolean discard = false;
        if( indoubtAction == IndoubtAction.INDOUBT_DELETE)
          discard = true;

        // This is an iterator over all messages in all the source streams
        // in this streamSet. There may be Uncommitted messages in this list
        // but getSIMPMessage will return null for these as they don't have a
        // valid ItemStream id so we won't try to delete them
        Iterator itr = this.getTransmitMessagesIterator(SIMPConstants.SIMPCONTROL_RETURN_ALL_MESSAGES);

        TransmitMessage xmitMsg = null;
        String state=null;

        while(itr.hasNext())
        {
          xmitMsg = (TransmitMessage)itr.next();

          state = xmitMsg.getState();

          if (state.equals( State.COMMITTING.toString() ) )
          {
            // Ignore committing messages. Theres nothing we can do with them.
            foundUncommittedMsgs = true;
          }
          else
          {
            try
            {
              // Try to delete it
              SIMPMessage msg = xmitMsg.getSIMPMessage();
              if( msg != null)
              {
                _sourceStreamManager.removeMessage(msg);
                QueuedMessage queuedMessage = (QueuedMessage)msg.getControlAdapter();
  
                // A null message implies it's already gone from the MsgStore
                if(queuedMessage != null)
                  queuedMessage.moveMessage(discard);
              }
            }
            catch (SIException e)
            {
              FFDCFilter.processException(
                e,
                "com.ibm.ws.sib.processor.runtime.SourceStreamSetControl.clearMessagesAtSource",
                "1:496:1.39",
                this);

              SIMPRuntimeOperationFailedException finalE =
                new SIMPRuntimeOperationFailedException(
                  nls.getFormattedMessage(
                    "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                    new Object[] {"SourceStreamSetControl.clearMessagesAtSource",
                        "1:504:1.39",
                                  e},
                    null), e);

              SibTr.exception(tc, finalE);

              if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "clearMessagesAtSource", finalE);
              throw finalE;
           }
          }
        }
      }

//    d477883 - COMMENT THE FOLLOWING CODE
//    We no longer flush the streams as part of a deleteAll/reallocateAll
//    This stream may still be used afterwards and therefore we dont want to
//    null out any resources.

//      // Now send a Flushed message and remove streamSet from sourcestreamManager
//      if( foundUncommittedMsgs == false)
//      {
//
//        try
//        {
//          _sourceStreamManager.forceFlush();
//        }
//        catch (SIException e)
//        {
//          FFDCFilter.processException(
//            e,
//            "com.ibm.ws.sib.processor.runtime.SourceStreamSetControl.clearMessagesAtSource",
//            "1:535:1.39",
//            this);
//
//          SIMPRuntimeOperationFailedException finalE =
//            new SIMPRuntimeOperationFailedException(
//              nls.getFormattedMessage(
//                "INTERNAL_MESSAGING_ERROR_CWSIP0003",
//                new Object[] {"SourceStreamSetControl.clearMessagesAtSource",
//                    "1:543:1.39",
//                              e},
//                null), e);
//
//          SibTr.exception(tc, finalE);
//
//          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "clearMessagesAtSource", finalE);
//          throw finalE;
//        }
//      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.exit(tc, "clearMessagesAtSource");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetControllable#getType()
   */
  public DeliveryStreamType getType()
  {
    return DeliveryStreamType.ANYCAST_SOURCE;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getId()
   */
  public String getId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getId");
    String returnString =  _streamID.toString();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getId", returnString);
    return returnString;
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

    if(_streamSet == null || _sourceStreamManager == null)
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {"SourceStreamSetControl.assertValidControllable",
                "1:603:1.39",
                          _streamID},
            null));

      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "assertValidControllable", finalE);
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
    _sourceStreamManager = null;

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
      msg = ((SourceStreamControl)it.next()).getTransmitMessageByID(id);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTransmitMessageByID", msg);
    return msg;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetTransmitControllable#getDepth
   */
  public long getDepth() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getDepth");
    //the number of tick ranges in the value state on the streams
    long depth=0;
    SIMPIterator iterator = this.getStreams();
    while(iterator.hasNext())
    {
      depth+=
        ((SourceStreamControl)iterator.next()).getNumberOfActiveMessages();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getDepth", new Long(depth));
    return depth;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetTransmitControllable#getStreams
   */
  public SIMPIterator getStreams() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getStreams");

    assertValidControllable();

    Iterator it = null;
    try
    {
      it = _streamSet.iterator();
    }
    catch (SIResourceException e)
    {
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.SourceStreamSetControl.getStreams",
          "1:713:1.39",
          this);

        SIMPRuntimeOperationFailedException finalE =
          new SIMPRuntimeOperationFailedException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0002",
              new Object[] {"SourceStreamSetControl.getStreams",
                  "1:721:1.39",
                            e},
              null), e);

        SibTr.exception(tc, finalE);
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getStreams", finalE);
//        throw finalE;
    }
    SIMPIterator returnIterator=new SourceStreamControllableIterator(it);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getStreams", returnIterator);
    return returnIterator;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetTransmitControllable#getAttatchedRemoteSubscribers
   */
  public SIMPIterator getAttatchedRemoteSubscribers()
  {
    //This is a NO-OP for SourceStreamSets - is only valid for
    //InternalOutputStreamSet controller
    throw new UnsupportedOperationException();
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

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPPtoPOutboundTransmitControllable#reallocateAllTransmitMessages
   */
  public void reallocateAllTransmitMessages()
    throws SIMPRuntimeOperationFailedException, SIMPControllableNotFoundException
  {
    // Reallocates all messages on the outbound streams so that they
    // are sent to a different localization.
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reallocateAllTransmitMessages");

    clearMessagesAtSource(IndoubtAction.INDOUBT_REALLOCATE);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reallocateAllTransmitMessages");
  }

  public long getNumberOfMessagesSent()
    throws SIMPControllableNotFoundException
  {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getNumberOfMessagesSent");

    long count=0;
    SIMPIterator iterator = getStreams();
    while(iterator.hasNext())
    {
      SourceStreamControl srcStreamControl =
        (SourceStreamControl)iterator.next();
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
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.runtime.SourceStreamSetControl.getNumberOfUnacknowledgedMessages",
        "1:817:1.39",
        this);

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] { "com.ibm.ws.sib.processor.runtime.SourceStreamSetControl.getNumberOfUnacknowledgedMessages",
                       "1:822:1.39",
                       SIMPUtils.getStackTrace(e) });
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getNumberOfUnacknowledgedMessages", new Long(count));

    return count;
  }

  public void moveMessages(boolean discard)
    throws SIMPRuntimeOperationFailedException, SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "moveMessages", new Boolean(discard));

    if (discard)
      clearMessagesAtSource(IndoubtAction.INDOUBT_DELETE);
    else
      clearMessagesAtSource(IndoubtAction.INDOUBT_EXCEPTION);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "moveMessages");
  }

  public HealthStateListener getHealthState()
  {
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
        "com.ibm.ws.sib.processor.runtime.SourceStreamSetControl.getHealthState",
        "1:865:1.39",
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
        ((HealthStateTree)_healthState).addHealthStateNode(control.getHealthState());
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getHealthState", _healthState);
    return (HealthStateListener)_healthState;
  }

}

