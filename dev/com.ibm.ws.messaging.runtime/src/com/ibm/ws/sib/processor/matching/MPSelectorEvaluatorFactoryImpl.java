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

import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.sib.processor.MPSelectorEvaluator;
import com.ibm.ws.sib.processor.MPSelectorEvaluatorFactory;

/**
 * @author Neil Young
 */
public class MPSelectorEvaluatorFactoryImpl extends MPSelectorEvaluatorFactory 
{
  //trace
  private static final TraceComponent tc =
    SibTr.register(
      MPSelectorEvaluatorFactoryImpl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.MPSelectorEvaluatorFactory#createMPSelectorEvaluator()
   */  
  public MPSelectorEvaluator createMPSelectorEvaluator()
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "createMPSelectorEvaluator" );        
    
    MPSelectorEvaluator mpSelectorEvaluator = new MPSelectorEvaluatorImpl();
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "createMPSelectorEvaluator", mpSelectorEvaluator);
      
    return mpSelectorEvaluator;
  } 

}
