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
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.runtime.SIMPReceivedMessageControllable;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * The adapter presented by a received message to perform dynamic
 * control operations.
 */
public class ReceivedMessage extends AbstractControlAdapter implements SIMPReceivedMessageControllable
{
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private static final TraceComponent tc =
    SibTr.register(
      ReceivedMessage.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  MessageItem msgItem;

  public ReceivedMessage(MessageItem item, MessageProcessor msgProcessor, DestinationHandler destHandler)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "ReceivedMessage",
      new Object[]{item, msgProcessor, destHandler});

    msgItem = item;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "ReceivedMessage", this);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getJsMessage()
   */
  public JsMessage getJsMessage() throws SIMPControllableNotFoundException, SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getJsMessage");

    assertValidControllable();
    JsMessage returnMessage =  msgItem.getMessage();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getJsMessage");
    return returnMessage;
  }


  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getState()
   *
   * Gets the state from the message on the item stream.
   * If the item is not returned or the item isn't locked then we return State.UNLOCKED.
   */
  public String getState() throws SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getState");

    assertValidControllable();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getState", State.AWAITING_DELIVERY);
    return State.AWAITING_DELIVERY;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getTransactionID()
   */
  public String getTransactionId() throws SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTransactionId");

    assertValidControllable();

    String id = null;
    PersistentTranId pTranId = msgItem.getTransactionId();
    if (pTranId != null)
    {
      id = pTranId.toTMString();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTransactionId", id);
    return id;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#removeMessage(boolean)
   */
  public void moveMessage(boolean discard)
    throws SIMPControllableNotFoundException,
           SIMPRuntimeOperationFailedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "moveMessage", new Object[] { new Boolean(discard)});

    // Should never get called
    SIMPRuntimeOperationFailedException e =
      new SIMPRuntimeOperationFailedException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0003",
          new Object[] {"ReceivedMessage.moveMessage",
                         "1:157:1.19",
                        null},
          null), null);

    SibTr.exception(tc, e);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "moveMessage", e);

    throw e;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getId()
   */
  public String getId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getId");
      SibTr.exit(tc, "getId", new Long(getSequenceID()));
    }
    return ""+getSequenceID();
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

    if(msgItem == null)
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {"ReceivedMessage.assertValidControllable",
                           "1:205:1.19",
                           null},
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

    msgItem=null;

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
   * @see com.ibm.ws.sib.processor.runtime.SIMPReceivedMessageControllable#getSequenceID()
   */
  public long getSequenceID()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getSequenceID");
    long sequenceID=0;
    sequenceID = msgItem.getMessage().getGuaranteedValueValueTick();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getSequenceID", new Long(sequenceID));
    return sequenceID;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPReceivedMessageControllable#getPreviousSequenceId()
   */
  public long getPreviousSequenceID()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getPreviousSequenceID");
    long previousSequenceID=0;
    previousSequenceID = msgItem.getMessage().getGuaranteedValueStartTick()-1;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPreviousSequenceID", new Long(previousSequenceID));
    return previousSequenceID;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getStartTick()
   */
  public long getStartTick()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getStartTick");
    long startTick=0;
    startTick = msgItem.getMessage().getGuaranteedValueStartTick();

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
    long endTick=0;
    endTick = msgItem.getMessage().getGuaranteedValueEndTick();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getEndTick", new Long(endTick));
    return endTick;
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

