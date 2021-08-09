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
 * Class to represent the various lookups for different destination types
 */ 
public class DeleteDefered extends Invisible
{  
  /** 
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      DeleteDefered.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
  public boolean isDeleteDefered()
  {
    return true;
  }

  public State delete()
  {
    return State.DELETE_PENDING; 
  }

  public State deferDelete()
  {
    return this; 
  }

  public State cleanup()
  {
    return State.DELETE_PENDING; 
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.indexes.statemodel.State#toString()
   */
  public String toString()
  {
    return "DELETE_DEFERED";
  }
}
