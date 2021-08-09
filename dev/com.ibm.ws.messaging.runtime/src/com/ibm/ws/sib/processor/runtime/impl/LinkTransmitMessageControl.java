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
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.gd.Stream;
import com.ibm.ws.sib.processor.impl.interfaces.DownstreamControl;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.runtime.SIMPLinkTransmitMessageControllable;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.utils.ras.SibTr;

public class LinkTransmitMessageControl extends TransmitMessage implements
    SIMPLinkTransmitMessageControllable {

  private static final TraceComponent tc =
    SibTr.register(
      LinkTransmitMessageControl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);


  public LinkTransmitMessageControl(long msgStoreid, Stream stream, DownstreamControl downControl)
    throws SIResourceException
  {
    super(msgStoreid, stream, downControl);
  }

  public LinkTransmitMessageControl(SIMPMessage msg, Stream stream)
  {
    super(msg, stream);
  }

  public long getApproximateLength() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getApproximateLength");
    
    long length = 0;
    
    try 
    {
      length = getJsMessage().getApproximateLength();
    } catch(Exception e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.runtime.LinkTransmitMessageControl.getApproximateLength",
        "1:78:1.5",
        this);    
        
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] { "com.ibm.ws.sib.processor.runtime.LinkTransmitMessageControl.getApproximateLength", 
                       "1:83:1.5", 
                       SIMPUtils.getStackTrace(e) }); 
      SibTr.exception(tc, e); 
    } 
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getApproximateLength", new Long(length));      
    return length;
  }

  public String getTargetDestination() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getTargetDestination");
    
    String destination= null;
    
    try 
    {
      destination = getJsMessage().getRoutingDestination().getDestinationName();
    } catch(Exception e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.runtime.LinkTransmitMessageControl.getTargetDestination",
        "1:107:1.5",
        this);    
        
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] { "com.ibm.ws.sib.processor.runtime.LinkTransmitMessageControl.getTargetDestination", 
                       "1:112:1.5", 
                       SIMPUtils.getStackTrace(e) }); 
      SibTr.exception(tc, e); 
    } 
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getTargetDestination", destination);      
    return destination;
  }

  public String getTargetBus() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getTargetBus");
    
    String bus= null;
    
    try 
    {
      bus = getJsMessage().getRoutingDestination().getBusName();
    } catch(Exception e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.runtime.LinkTransmitMessageControl.getTargetBus",
        "1:136:1.5",
        this);    
        
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] { "com.ibm.ws.sib.processor.runtime.LinkTransmitMessageControl.getTargetBus", 
                       "1:141:1.5", 
                       SIMPUtils.getStackTrace(e) }); 
      SibTr.exception(tc, e); 
    } 
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getTargetBus", bus);      
    return bus;
  }

}
