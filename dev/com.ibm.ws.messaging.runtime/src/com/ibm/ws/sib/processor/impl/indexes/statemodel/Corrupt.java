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
 * Class to represent the Corrupt state.
 */ 
public class Corrupt extends Visible
{
  /** 
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      Corrupt.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
      
   

  public boolean isCorrupt()
  {
    return true;
  }
  
  public State reset()
  {
    return State.RESET_ON_RESTART; 
  } 
  
  public State cleanup()
  {
    return State.CLEANUP_PENDING; 
  }  

  /**
   * Delete added as it should be possible to delete a corrupt destination
   */
  public State delete()
  {
    return State.DELETE_PENDING; 
  }
        
  public String toString()
  {
    return "CORRUPT";
  }
}
