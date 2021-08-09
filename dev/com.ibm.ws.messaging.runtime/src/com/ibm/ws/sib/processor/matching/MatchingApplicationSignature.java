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
package com.ibm.ws.sib.processor.matching;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.messagecontrol.ApplicationSignature;

public class MatchingApplicationSignature extends MessageProcessorMatchTarget
{
  // Standard trace boilerplate

  private static final TraceComponent tc =
    SibTr.register(
        MatchingApplicationSignature.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);  
  private ApplicationSignature applicationSig;

  MatchingApplicationSignature(ApplicationSignature applicationSig)
  {
    super(APPLICATION_SIG_TYPE);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "MatchingApplicationSignature", applicationSig);

    this.applicationSig = applicationSig;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "MatchingApplicationSignature");    
    
  }

  public boolean equals(Object o)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "equals", o);
    
    boolean areEqual = false;
    if (o instanceof MatchingApplicationSignature)
    {
      ApplicationSignature otherAS = ((MatchingApplicationSignature) o).applicationSig;

      if(applicationSig.equals(otherAS))
        areEqual = true;
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "equals", Boolean.valueOf(areEqual));        
    return areEqual;
  }

  public int hashCode()
  {
    return applicationSig.hashCode();
  }
  
  /**
   * Returns the ApplicationSignature.
   * @return ApplicationSignature
   */
  public ApplicationSignature getApplicationSignature()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getApplicationSignature");
      SibTr.exit(tc, "getApplicationSignature", applicationSig);
    }
    return applicationSig;
  }
}
