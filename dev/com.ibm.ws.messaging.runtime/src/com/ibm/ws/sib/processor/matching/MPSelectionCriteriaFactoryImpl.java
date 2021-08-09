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

import java.util.Map;

import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.sib.processor.MPSelectionCriteria;
import com.ibm.ws.sib.processor.MPSelectionCriteriaFactory;
import com.ibm.wsspi.sib.core.SelectorDomain;

/**
 * @author Neil Young
 */
public class MPSelectionCriteriaFactoryImpl extends MPSelectionCriteriaFactory 
{
  //trace
  private static final TraceComponent tc =
    SibTr.register(
      MPSelectionCriteriaFactoryImpl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
  @Override
  public MPSelectionCriteria createSelectionCriteria(String discriminator,
      String selectorString, SelectorDomain selectorDomain,
      Map<String, Object> selectorProperties)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "createSelectionCriteria" );        
    
    MPSelectionCriteria mpSelectionCriteria = 
      new MPSelectionCriteriaImpl(discriminator,
                                  selectorString, 
                                  selectorDomain,
                                  selectorProperties);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createSelectionCriteria", mpSelectionCriteria);
      
    return mpSelectionCriteria;
  } 

}
