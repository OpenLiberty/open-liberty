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
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.runtime.SIMPMQLinkTransmitMessageControllable;
import com.ibm.ws.sib.utils.ras.SibTr;

public class MQLinkQueuedMessage
  extends QueuedMessage implements SIMPMQLinkTransmitMessageControllable
{
  private static TraceComponent tc =
    SibTr.register(
        MQLinkQueuedMessage.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  /* Output source info */
  static {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "Source info: 1:46:1.3");
  }
  String targetQMgr = null;
  String targetQueue = null;
  String state = null;

  public MQLinkQueuedMessage(SIMPMessage message, DestinationHandler destination, ItemStream is)
  throws SIResourceException
  {
    super(message, destination, is);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMQLinkTransmitMessageControllable#getTargetQMgr()
   */
  public String getTargetQMgr()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getTargetQMgr");
      SibTr.exit(tc, "getTargetQMgr", targetQMgr);
    }
    return targetQMgr;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMQLinkTransmitMessageControllable#getTargetQueue()
   */
  public String getTargetQueue()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getTargetQueue");
      SibTr.exit(tc, "getTargetQueue", targetQueue);
    }
    return targetQueue;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMQLinkTransmitMessageControllable#setState(java.lang.String)
   */
  public void setState(String state)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setState", state);

    this.state = state;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setState");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueuedMessageControllable#getState()
   */
  public String getState() throws SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getState");

    String currentState = null;

    // Has the state of this message been set externally (by MQLink). If not then
    // return the MP state associated with the message
    if(this.state != null)
    {
      currentState = this.state;
    }
    else
    {
      currentState = super.getState();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getState", currentState);
    return currentState;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMQLinkTransmitMessageControllable#setTargetQMgr(java.lang.String)
   */
  public void setTargetQMgr(String qMgr)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setTargetQMgr", qMgr);

    targetQMgr = qMgr;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setTargetQMgr");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMQLinkTransmitMessageControllable#setTargetQueue(java.lang.String)
   */
  public void setTargetQueue(String queue)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setTargetQueue", queue);

    targetQueue = queue;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setTargetQueue");
  }

}
