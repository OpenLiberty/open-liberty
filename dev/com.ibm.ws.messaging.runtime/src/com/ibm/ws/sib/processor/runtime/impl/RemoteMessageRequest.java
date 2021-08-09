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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;
import com.ibm.ws.sib.processor.gd.AIRequestedTick;
import com.ibm.ws.sib.processor.gd.AIStream;
import com.ibm.ws.sib.processor.gd.AIValueTick;
import com.ibm.ws.sib.processor.gd.TickRange;
import com.ibm.ws.sib.processor.impl.exceptions.InvalidOperationException;
import com.ibm.ws.sib.processor.impl.store.itemstreams.AIProtocolItemStream;
import com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageRequestControllable;
import com.ibm.ws.sib.processor.runtime.SIMPRequestMessageInfo;
import com.ibm.ws.sib.processor.runtime.SIMPRequestedValueMessageInfo;
import com.ibm.ws.sib.processor.runtime.anycast.RequestMessageInfo;
import com.ibm.ws.sib.processor.runtime.anycast.RequestedValueMessageInfo;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * ControlAdapter for a message request that we have sent to a remote messaging
 * engine
 *
 * The object is created for each tick in the AIStream that represents the state of the
 * anycast protocol at the remote ME.
 */
public class RemoteMessageRequest extends AbstractControlAdapter implements SIMPRemoteMessageRequestControllable
{
  // The tick that this object represents
	private long _tick;
  // The AIStream
  private AIStream _aiStream;
  // The AIProtocolItemStream that is contained within the aiStream
  private AIProtocolItemStream _aiProtocolItemStream;
  	
	private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
	
	private static final TraceComponent tc =
		SibTr.register(
	    RemoteMessageRequest.class,
			SIMPConstants.MP_TRACE_GROUP,
			SIMPConstants.RESOURCE_BUNDLE);
	
  /**
   * This object is created for each tick on the AIStream when asked for it. i.e. iterator.next()
   *
   * @param tick
   * @param aiStream
   */
	public RemoteMessageRequest(long tick, AIStream aiStream)
	{
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "RemoteMessageRequest", new Object[]{new Long(tick), aiStream});

		_tick = tick;
    _aiStream = aiStream;
		_aiProtocolItemStream = aiStream.getAIProtocolItemStream();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "RemoteMessageRequest", this);
	}
	
  private TickRange getTickRange()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTickRange");

    TickRange tr = _aiStream.getTickRange(_tick);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTickRange", tr);	
    return tr;
  }

  /**
   * No-op as the JsMessage is not present in the stream on the RME.
   *
   * @throws UnSupportedOperationException
   */
  public JsMessage getJsMessage()
  {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "getJsMessage");

    InvalidOperationException finalE =
      new InvalidOperationException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0005",
          new Object[] {"RemoteMessageRequest.getJsMessage",
                        "1:127:1.34",
                        this},
          null));

    SibTr.exception(tc, finalE);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getJsMessage");
    throw finalE;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getState()
   */
  public String getState() throws SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getState");

    String state = getState(getTickRange());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getState", state);

    return state;
  }

  private String getState(TickRange tickRange) throws SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getState", tickRange);

    String state = null;

    byte tickState = tickRange.type;

    switch ( tickState )
    {
      case TickRange.Requested :
      {
        state = State.REQUEST.toString();
        break;
      }
      case TickRange.Accepted :
      {
        state = State.ACKNOWLEDGED.toString();
        break;
      }
      case TickRange.Rejected :
      {
        state = State.REJECT.toString();
        break;
      }
      case TickRange.Value :
      {
        state = State.VALUE.toString();
        break;
      }
      case TickRange.Completed :
      {
        state = State.COMPLETED.toString();
        break;
      }
    }

    if (state == null)
    {
      SIMPException e =
        new SIMPException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {"RemoteMessageRequest.getState",
                          "1:199:1.34",
                          this},
            null));

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getState");
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getState", state);
    return state;
  }
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getStartTick()
   */
  public long getStartTick()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getStartTick");

    long startTick = getTickRange().startstamp;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getStartTick", new Long(startTick));

    return startTick;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getEndTick()
   */
  public long getEndTick()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getEndTick");

    long endTick = getTickRange().endstamp;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getEndTick", new Long(endTick));

    return endTick;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#removeMessage(boolean)
   */
  public void moveMessage(boolean discard)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeMessage", new Boolean(discard));

    InvalidOperationException finalE =
      new InvalidOperationException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0005",
          new Object[] {"RemoteMessageRequest.removeMessage",
                        "1:259:1.34",
                        this},
          null));

    SibTr.exception(tc, finalE);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeMessage", finalE);
    throw finalE;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getId()
   */
  public String getId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getId");
      SibTr.exit(tc, "getId", ""+getTick());
    }
    return ""+getTick();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getName()
   */
  public String getName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getName");
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "No implementation");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getName", null);
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#checkValidControllable()
   */
  public void assertValidControllable() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "assertValidControllable");

		if(getTick() == -1)
		{
			SIMPControllableNotFoundException finalE =
				new SIMPControllableNotFoundException(
					nls.getFormattedMessage(
						"INTERNAL_MESSAGING_ERROR_CWSIP0005",
						new Object[] {"RemoteMessageRequest.assertValidControllable",
                          "1:312:1.34",
													new Long(getTick())},
						null));

			SibTr.exception(tc, finalE);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "assertValidControllable");
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
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "dereferenceControllable", "No implementation");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#registerControlAdapterAsMBean()
   */
  public void registerControlAdapterAsMBean()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "registerControlAdapterAsMBean", "No implementation");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#deregisterControlAdapterMBean()
   */
  public void deregisterControlAdapterMBean()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "deregisterControlAdapterMBean", "No implementation");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#runtimeEventOccurred(com.ibm.ws.sib.admin.RuntimeEvent)
   */
  public void runtimeEventOccurred(RuntimeEvent event)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "runtimeEventOccurred", "No implementation");
  }
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getTransactionId()
   */
  public String getTransactionId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTransactionId");

    String tranID = null;
    PersistentTranId pTranID = _aiProtocolItemStream.getTransactionId();

    if (pTranID != null)
    {
      tranID = pTranID.toTMString();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTransactionId", tranID);
    return tranID;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageRequestControllable#getRequestMessageInfo()
   */
  public SIMPRequestMessageInfo getRequestMessageInfo() throws SIMPRuntimeOperationFailedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getRequestMessageInfo");

    SIMPRequestMessageInfo requestMessageInfo = null;
    try
    {
      if (State.REQUEST.toString().equals(getState()))
      {
        // This RemoteMessageRequest is in state request so lets get the info
        TickRange tickRange = _aiStream.getTickRange(_tick);
        requestMessageInfo = new RequestMessageInfo((AIRequestedTick)tickRange.value);
      }
    }
    catch(SIMPException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.runtime.RemoteMessageRequest.getRequestMessageInfo",
        "1:407:1.34",
        this);

      SIMPRuntimeOperationFailedException e1 =
        new SIMPRuntimeOperationFailedException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {"RemoteMessageRequest.getRequestMessageInfo",
                          "1:415:1.34",
                          e,
                          _aiStream.getStreamId()},
            null), e);

      SibTr.exception(tc, e1);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getRequestMessageInfo", e1);
      throw e1;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getRequestMessageInfo", requestMessageInfo);
    return requestMessageInfo;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageRequestControllable#getRequestMessageInfo()
   */
  public SIMPRequestedValueMessageInfo getRequestedValueMessageInfo() throws SIMPRuntimeOperationFailedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getRequestedValueMessageInfo");

    SIMPRequestedValueMessageInfo requestedValueMessageInfo = null;
    try
    {
      TickRange tickRange = getTickRange();
      synchronized(tickRange)
      {
        if (State.VALUE.toString().equals(getState(tickRange)))
        {
          // This RemoteMessageRequest is in state request so lets get the info
          requestedValueMessageInfo = new RequestedValueMessageInfo((AIValueTick)tickRange.value);
        }
      }
    }
    catch(SIMPException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.runtime.RemoteMessageRequest.getRequestedValueMessageInfo",
        "1:456:1.34",
        this);

      SIMPRuntimeOperationFailedException e1 =
        new SIMPRuntimeOperationFailedException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {"RemoteMessageRequest.getRequestedValueMessageInfo",
                          "1:464:1.34",
                          e,
                          _aiStream.getStreamId()},
            null), e);

      SibTr.exception(tc, e1);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getRequestedValueMessageInfo", e1);
      throw e1;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getRequestedValueMessageInfo", requestedValueMessageInfo);
    return requestedValueMessageInfo;
  }

  public long getTick()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getTick");
      SibTr.exit(tc, "getTick", new Long(_tick));
    }
    return _tick;
  }

  /** Return the Messaging engine that this request has been generated against */
  public String getRemoteEngineUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getRemoteEngineUuid");

    String remoteUUID = _aiStream.getAnycastInputHandler().getLocalisationUuid().toString();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getRemoteEngineUuid", remoteUUID);
    return remoteUUID;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getMEArrivalTimestamp()
   */
  public long getMEArrivalTimestamp() throws SIMPControllableNotFoundException, SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getMEArrivalTimestamp");

    long timestamp =  getJsMessage().getCurrentMEArrivalTimestamp();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getMEArrivalTimestamp", new Long(timestamp));
    return timestamp;
  }

}
