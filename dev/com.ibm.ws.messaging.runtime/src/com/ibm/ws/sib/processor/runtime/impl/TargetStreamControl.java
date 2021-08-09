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
import java.util.LinkedList;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.gd.TargetStream;
import com.ibm.ws.sib.processor.gd.TargetStreamManager;
import com.ibm.ws.sib.processor.impl.interfaces.HealthStateListener;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamReceiverControllable;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPReceivedMessageControllable;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A Controllable view of an individual target stream
 *
 * @author tpm100
 */
public class TargetStreamControl extends AbstractControlAdapter
                                      implements SIMPDeliveryStreamReceiverControllable
{
  private static final TraceComponent tc =
    SibTr.register(
      TargetStreamControl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private TargetStream _targetStream;
  private SIBUuid12 _streamSetID;
  private Reliability _targetStreamReliability;
  private int _priority;
  private SIBUuid8 _remoteEngineUUID;
  private TargetStreamManager _tsm;

  private HealthStateListener _healthState;

  public TargetStreamControl(SIBUuid8 remoteEngineUUID,
    TargetStream targetStream, SIBUuid12 streamSetID,
    Reliability targetStreamReliability, int priority)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "TargetStreamControl", new Object[]
      {targetStream,  streamSetID, targetStreamReliability, priority});

    this._remoteEngineUUID = remoteEngineUUID;
    this._targetStream = targetStream;
    this._streamSetID = streamSetID;
    this._targetStreamReliability = targetStreamReliability;
    this._priority = priority;
    this._healthState = new HealthStateTree();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "TargetStreamControl", this);
  }


  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryReceiverControllable#getStreamState
   */
  public StreamState getStreamState()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getStreamState");
    StreamState returnValue = _targetStream.getStreamState();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getStreamState", returnValue);
    return returnValue;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryReceiverControllable#getStreamID
   */
  public SIBUuid12 getStreamID()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getStreamID");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getStreamID", _streamSetID);
    return _streamSetID;
  }


  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamReceiverControllable#getReliability
   */
  public Reliability getReliability()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getReliability");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getReliability", _targetStreamReliability);
    return _targetStreamReliability;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamReceiverControllable#getPriority
   */
  public int getPriority()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getPriority");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPriority", new Integer(_priority));
    return _priority;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamReceiverControllable#getNumberOfActiveMessages
   */
  public int getNumberOfActiveMessages()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getNumberOfActiveMessages");
    int returnValue = (int)_targetStream.countAllMessagesOnStream();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getNumberOfActiveMessages", new Integer(returnValue));
    return returnValue;
  }

  public String getRemoteEngineUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getRemoteEngineUuid");
      SibTr.exit(tc, "getRemoteEngineUuid", _remoteEngineUUID);
    }
    return _remoteEngineUUID.toString();
  }


  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getName()
   */
  public String getName()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#dereferenceControllable()
   */
  public void dereferenceControllable()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "dereferenceControllable");

    _targetStream = null;
    _streamSetID = null;
    _priority = -1;
    _targetStreamReliability = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "dereferenceControllable");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#runtimeEventOccurred(com.ibm.ws.sib.admin.RuntimeEvent)
   */
  public void runtimeEventOccurred(RuntimeEvent event)
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#assertValidControllable()
   */
  public void assertValidControllable() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "assertValidControllable");

    if(_targetStream == null ||
       _streamSetID == null  ||
       _targetStreamReliability == null )
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {"TargetStreamControl.assertValidControllable",
                          "1:228:1.32",
                          _streamSetID},
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
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getId()
   */
  public String getId()
  {
    String returnValue = _targetStream.getID();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getId", returnValue);
    return returnValue;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamReceiverControllable#getQueuedMessageIterator
   */
  public SIMPIterator getReceivedMessageIterator(int maxMsgs)
  {
    List receivedMessages = new LinkedList();
    Iterator<MessageItem> streamMessages = 
      _targetStream.getAllMessagesOnStream().iterator();
    boolean allMsgs = (maxMsgs == SIMPConstants.SIMPCONTROL_RETURN_ALL_MESSAGES);
    int index = 0;
    while((allMsgs || (index < maxMsgs)) && streamMessages.hasNext())
    {
      MessageItem msgItem = (MessageItem)streamMessages.next();

      SIMPReceivedMessageControllable receivedMessage =
        new LinkReceivedMessageControl(msgItem, _tsm.getDestinationHandler().getMessageProcessor(), _tsm.getDestinationHandler());
      receivedMessages.add(receivedMessage);
      index++;
    }
    return new BasicSIMPIterator(receivedMessages.iterator());
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamReceiverControllable#getQueuedMessageByID
   */
  public SIMPReceivedMessageControllable getReceivedMessageByID(String id)
  {
    SIMPReceivedMessageControllable returnMessage=null;
    SIMPIterator iterator = getReceivedMessageIterator(SIMPConstants.SIMPCONTROL_RETURN_ALL_MESSAGES);
    while(iterator.hasNext())
    {
      SIMPReceivedMessageControllable receivedMessage = (SIMPReceivedMessageControllable)iterator.next();
      String msgID = receivedMessage.getId();
      if(msgID.equals(id))
      {
        returnMessage = receivedMessage;
        break;
      }
    }
    return returnMessage;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamReceiverControllable#getLastDeliveredMessageSequenceId
   */
  public long getLastDeliveredMessageSequenceId()
  {
  	return _targetStream.getLastKnownTick();    	
  }


  public long getNumberOfMessagesReceived() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getNumberOfMessagesReceived");
    long returnValue = _targetStream.getNumberOfMessagesReceived();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getNumberOfMessagesReceived", new Long(returnValue));
    return returnValue;
  }

  public long getLastMsgReceivedTime()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getHealthState");

    long time = _targetStream.getLastMsgReceivedTimestamp();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getHealthState", time);

    return time;
  }

  public HealthStateListener getHealthState() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getHealthState");
      SibTr.exit(tc, "getHealthState", _healthState);
    }
    return (HealthStateListener)_healthState;
  }


  public void setTSM(TargetStreamManager tsm) {
    this._tsm = tsm;
  }

}
