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
package com.ibm.ws.sib.processor.runtime.anycast;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.AnycastInputHandler;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author whitfiea
 *
 * Object represents the remoteDurableSubscribers
 */
public class RemoteDurableSubscribers
{
  private static TraceComponent tc =
    SibTr.register(
      RemoteDurableSubscribers.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  
  private String remoteDurableName;
  private AnycastInputHandler anycastInputHandler;
  
  public RemoteDurableSubscribers(String remoteDurableName, AnycastInputHandler aih)
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "RemoteDurableSubscribers", new Object[]{remoteDurableName, aih});
      
    this.remoteDurableName = remoteDurableName;
    anycastInputHandler = aih;
    
    if (tc.isEntryEnabled()) 
      SibTr.exit(tc, "RemoteDurableSubscribers", this);
  }
  
  public String getRemoteDurableName()
  {
    if (tc.isEntryEnabled()) SibTr.entry(tc, "getRemoteDurableName");
    if (tc.isEntryEnabled()) SibTr.exit(tc, "getRemoteDurableName", remoteDurableName);
    return remoteDurableName;
  }
  
  public AnycastInputHandler getAnycastInputHandler()
  {
    if (tc.isEntryEnabled()) SibTr.entry(tc, "getAnycastInputHandler");
    if (tc.isEntryEnabled()) SibTr.exit(tc, "getAnycastInputHandler", anycastInputHandler);
    return anycastInputHandler;
  }
}
