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
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;
import com.ibm.ws.sib.processor.gd.Stream;
import com.ibm.ws.sib.processor.impl.interfaces.DownstreamControl;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * THIS CLASS IS USED FOR NON-LINK PUBSUB MESSAGES TOO
 */
public class LinkPubSubTransmitMessageControl extends LinkTransmitMessageControl 
{

  private static final TraceComponent tc =
    SibTr.register(
      LinkPubSubTransmitMessageControl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  

  private InternalOutputStreamSetControl parent = null;

  public LinkPubSubTransmitMessageControl(long msgStoreId, Stream stream, DownstreamControl downControl, ControlAdapter parent)
    throws SIResourceException
  {
    super(msgStoreId, stream, downControl);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "LinkPubSubTransmitMessageControl", new Object[]{Long.valueOf(msgStoreId), stream, downControl, parent});
    this.parent = (InternalOutputStreamSetControl)parent;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "LinkPubSubTransmitMessageControl", this);
  }
  
  public LinkPubSubTransmitMessageControl(SIMPMessage msg, Stream stream, ControlAdapter parent) {
    super(msg, stream);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "LinkPubSubTransmitMessageControl", new Object[]{msg, stream, parent});
    this.parent = (InternalOutputStreamSetControl)parent;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "LinkPubSubTransmitMessageControl", this);
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.LinkTransmitMessageControl#getTargetDestination()
   */
  public String getTargetDestination() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getTargetDestination");
    
    String destination = null;
    if(parent instanceof LinkPublicationPointControl)
    {
      LinkPublicationPointControl lppc = (LinkPublicationPointControl)parent;
      destination = lppc.getTargetDestination();
    }
    else
    {
      // parent is not an instance of LinkPublicationPointControl   
      SIErrorException finalE =
        new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {"LinkPubSubTransmitMessageControl.getTargetDestination",
                "1:106:1.7",
                parent},
            null));
      
      // FFDC
      FFDCFilter.processException(
          finalE,
          "com.ibm.ws.sib.processor.runtime.LinkPubSubTransmitMessageControl.getTargetDestination",
          "1:114:1.7",
          this);
      
      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "getTargetDestination", finalE);
      throw finalE;      
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getTargetDestination", destination);      
    return destination;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.LinkTransmitMessageControl#getTargetBus()
   */
  public String getTargetBus() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getTargetBus");
    
    String bus = null;
    if(parent instanceof LinkPublicationPointControl)
    {
      LinkPublicationPointControl lppc = (LinkPublicationPointControl)parent;
      bus = lppc.getTargetBusName();
    }
    else
    {
      // parent is not an instance of LinkPublicationPointControl   
      SIErrorException finalE =
        new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {"LinkPubSubTransmitMessageControl.getTargetBus",
                "1:149:1.7",
                parent},
            null));
      
      // FFDC
      FFDCFilter.processException(
          finalE,
          "com.ibm.ws.sib.processor.runtime.LinkPubSubTransmitMessageControl.getTargetBus",
          "1:157:1.7",
          this);
      
      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "getTargetBus", finalE);
      throw finalE;      
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTargetBus", bus);
    return bus;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#moveMessage(boolean)
   */
  public void moveMessage(boolean discard) throws SIMPControllableNotFoundException, 
                                                    SIMPRuntimeOperationFailedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "moveMessage", Boolean.valueOf(discard));
      
    assertValidControllable();
    
    // This is a pubsub messages so we cannot remove the actual message from the ItemStream.
    // In fact, there is no need to remove the underlying message, we leave that to the stream
    // to call back to the PubSubInputHandler to remove the reference to the message if
    // possible (i.e. others are not also referencing it).
    try
    {
      Stream stream = getStream();
      if(stream != null)
        stream.writeSilenceForced(getStartTick());
    }
    catch (SIException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.runtime.LinkTransmitMessageControl.moveMessage",
        "1:184:1.5",
        this);
  
      SIMPRuntimeOperationFailedException finalE =
        new SIMPRuntimeOperationFailedException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {"TransmitMessage.moveMessage",
                "1:192:1.5",
                          e,
                          new Long(getStartTick())},
            null), e);

      SibTr.exception(tc, finalE);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "moveMessage", finalE);
      
      throw finalE;             
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "moveMessage");
  }  
}
