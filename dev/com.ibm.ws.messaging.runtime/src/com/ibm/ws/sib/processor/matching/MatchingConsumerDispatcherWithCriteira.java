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
import com.ibm.ws.sib.processor.impl.ConsumerDispatcher;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;

/**
 * @author Neil Young
 *
 * <p>The MatchingConsumerDispatcher class is a wrapper that holds a ConsumerDispatcher,
 * but allows a MatchTarget type to be associated with it for storage in the
 * MatchSpace. 

 */
public class MatchingConsumerDispatcherWithCriteira extends MessageProcessorMatchTarget
{

  private static final TraceComponent tc = 
    SibTr.register(MatchingConsumerDispatcherWithCriteira.class, null, null);
  
  private ConsumerDispatcher consumerDispatcher;
  private SelectionCriteria selectionCriteria;
  
  MatchingConsumerDispatcherWithCriteira(ConsumerDispatcher cd, SelectionCriteria sc)
  {    
    super(JS_SUBSCRIPTION_TYPE);
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "MatchingConsumerDispatcherWithCriteira", new Object[] {
          cd, sc });
    consumerDispatcher = cd;
    selectionCriteria = sc;
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "MatchingConsumerDispatcherWithCriteira", this);
  }

  public boolean equals(Object o) 
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "equals", o);
    boolean areEqual = true;
    if (o instanceof MatchingConsumerDispatcherWithCriteira)
    {
      ConsumerDispatcher otherCD = ((MatchingConsumerDispatcherWithCriteira) o).consumerDispatcher;
      SelectionCriteria otherSC  = ((MatchingConsumerDispatcherWithCriteira) o).selectionCriteria;   
      if( !(consumerDispatcher.equals(otherCD)) )
        areEqual = false;
      if( selectionCriteria == null )
      {
        if (otherSC != null) 
          areEqual = false;
      }
      else
      {
         if( otherSC == null )
           areEqual = false;  
         else
           if (!selectionCriteria.equals(otherSC))
             areEqual = false; 
      }     
    }
    else
      areEqual = false;
     
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "equals", new Boolean(areEqual));
    return areEqual;
  }
  
  public int hashCode() 
  {
    return consumerDispatcher.hashCode();
  }  

  /**
   * Returns the consumerDispatcher.
   * @return ConsumerDispatcher
   */
  public ConsumerDispatcher getConsumerDispatcher() 
  {
    if (tc.isEntryEnabled()) 
    {
      SibTr.entry(tc, "getConsumerDispatcher");
      SibTr.exit(tc, "getConsumerDispatcher", consumerDispatcher);	
    }
    return consumerDispatcher;
  }

}
