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

package com.ibm.ws.sib.processor.impl.indexes.statemodel;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This state is not to be confused with corrupt destinations.
 * A state is put into inDoubt when WCCM fails to reconcile the destination
 * (in other words fails to call createDestLocalisation). We will then
 * check and if we find that:
 * 1) According to admin, the destination still exists
 * 2) Admin thinks the destination should be localised on our ME
 * 
 * This means that WCCM files may be corrupt, and so we do not want to delete
 * the destination but do not want to let messages be produced/consumed
 * from it either.
 * 
 * therefore this destination is not visible.
 * 
 * @author tpm100
 */
public class InDoubt extends Invisible
{

  /** 
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      InDoubt.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
    
  /* Output source info */
  static {
    if (tc.isDebugEnabled())
    SibTr.debug(tc, "Source info: @(#)ws/code/sib.processor.impl/src/com/ibm/ws/sib/processor/impl/indexes/statemodel/InDoubt.java, SIB.processor, jstream 1.2");
  }        

  public boolean isInDoubt()
  {
    return true;
  }
  
  public State putInDoubt()
  {
    return this;
  }
  
  public State putUnreconciled()
  {
    return State.UNRECONCILED;
  }
      
  public String toString()
  {
    return "INDOUBT";
  }
  
}


