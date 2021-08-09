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

import java.util.Properties;
import com.ibm.websphere.ras.TraceComponent;

import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.admin.RuntimeEventListener;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * 
 */
public class GatewayLinkControlAdapter extends AbstractControlAdapter
{
  // Reference to  comms RuntimeEventListener fo GatewayLink
  private RuntimeEventListener _rel;
  // Ref to JsMessagingEngine
  private JsMessagingEngine _jsme;   
  
  private static TraceComponent tc =
  SibTr.register(
    GatewayLinkControlAdapter.class,
    SIMPConstants.MP_TRACE_GROUP,
    SIMPConstants.RESOURCE_BUNDLE);

  public GatewayLinkControlAdapter(JsMessagingEngine jsme,
                                   RuntimeEventListener rel)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "GatewayLinkControlAdapter",
        new Object[] { jsme, rel});
          
    _jsme = jsme;
    _rel = rel;
          
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "GatewayLinkControlAdapter", this);   
  }
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#assertValidControllable()
   */
  public void assertValidControllable()
  {
  }
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#dereferenceControllable()
   */
  public void dereferenceControllable()
  {
  }
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#runtimeEventOccurred(com.ibm.ws.sib.admin.RuntimeEvent)
   */
  public void runtimeEventOccurred(RuntimeEvent event)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "runtimeEventOccurred",
        new Object[] { event});
          
    // Call out to the real GatewayLink MBean's RuntimeEventListener
    // if we determine it to be non-null.
    if(_rel != null)
    {
      _rel.runtimeEventOccurred(_jsme,
                                event.getType(),
                                event.getMessage(),
                                (Properties)event.getUserData());
    }
    else
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Null RuntimeEventListener, cannot fire event");         
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "runtimeEventOccurred", this);         
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getName()
   */
  public String getName()
  {
    return null;
  }
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getId()
   */
  public String getId()
  {
    return null;
  }

}
