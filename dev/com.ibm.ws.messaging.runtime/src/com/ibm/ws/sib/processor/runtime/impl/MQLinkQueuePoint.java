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
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.exceptions.SIMPInvalidRuntimeIDException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPLocalMsgsItemStream;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPMQLinkQueuePointControllable;
import com.ibm.ws.sib.processor.runtime.SIMPMQLinkTransmitMessageControllable;

import com.ibm.ws.sib.utils.ras.SibTr;

public class MQLinkQueuePoint
  extends LocalQueuePoint implements SIMPMQLinkQueuePointControllable
{
  private static TraceComponent tc =
    SibTr.register(
      MQLinkQueuePoint.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  /* Output source info */
  static {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "Source info: 1:49:1.3");
  }

  public MQLinkQueuePoint(MessageProcessor messageProcessor, PtoPLocalMsgsItemStream itemStream)
  {
    super(messageProcessor, itemStream);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMQLinkQueuePointControllable#getTransmitMessageIterator()
   */
  public SIMPIterator getTransmitMessageIterator()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTransmitMessageIterator");

    // TODO : This method needs to throw a runtime invalid exception to comms
    SIMPIterator msgItr = null;
    try
    {
      msgItr = getQueuedMessageIterator();
    }
    catch (SIMPRuntimeOperationFailedException e)
    {
      // No FFDC code needed
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTransmitMessageIterator", msgItr);

    return msgItr;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMQLinkQueuePointControllable#getTransmitMessageByID(java.lang.String)
   */
  public SIMPMQLinkTransmitMessageControllable getTransmitMessageByID( String id )
    throws SIMPInvalidRuntimeIDException,
           SIMPControllableNotFoundException,
           SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTransmitMessageByID", id );
    SIMPMQLinkTransmitMessageControllable msgCon = (SIMPMQLinkTransmitMessageControllable)getQueuedMessageByID(id);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTransmitMessageByID", msgCon);
    return msgCon;
  }
}
