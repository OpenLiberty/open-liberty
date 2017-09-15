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
package com.ibm.ws.sib.processor.impl.indexes;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.indexes.statemodel.State;
import com.ibm.ws.sib.processor.utils.index.Index;
import com.ibm.ws.sib.processor.utils.index.IndexFilter;
import com.ibm.ws.sib.utils.ras.SibTr;

public class AbstractDestinationTypeFilter implements IndexFilter
{
  public Boolean UNRECONCILED = null;
  public Boolean DELETE_PENDING = null;
  public Boolean DELETE_DEFERED = null;
  public Boolean ACTIVE = null;
  public Boolean CLEANUP_PENDING = null;
  public Boolean CLEANUP_DEFERED = null;
  public Boolean VISIBLE = null;
  public Boolean INVISIBLE = null;
  public Boolean INDOUBT = null;
  public Boolean CORRUPT = null;
  public Boolean RESET_ON_RESTART = null;
  // true = AND, false = OR
  public boolean and = true;
  
  /** 
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      AbstractDestinationTypeFilter.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
                  
  public boolean matches(Index.Type type)
  {
    if (tc.isEntryEnabled()) 
        SibTr.entry(tc, "matches", type);

    if(type == null) return false;
    if(type instanceof AbstractDestinationIndex.AbstractDestinationType)
    {
      AbstractDestinationIndex.AbstractDestinationType destType = (AbstractDestinationIndex.AbstractDestinationType) type;
      State state = destType.state;
      if (tc.isDebugEnabled())
        SibTr.debug(tc, "State: " + state); 

      if(and)
      {
        if((VISIBLE == null || VISIBLE.booleanValue() == state.isVisible()) &&
           (INVISIBLE == null || INVISIBLE.booleanValue() == state.isInvisible()) &&
           (UNRECONCILED == null || UNRECONCILED.booleanValue() == state.isUnreconciled()) &&
           (DELETE_PENDING == null || DELETE_PENDING.booleanValue() == state.isDeletePending()) &&
           (DELETE_DEFERED == null || DELETE_DEFERED.booleanValue() == state.isDeleteDefered()) &&
           (ACTIVE == null || ACTIVE.booleanValue() == state.isActive()) &&
           (CLEANUP_PENDING == null || CLEANUP_PENDING.booleanValue() == state.isCleanupPending()) &&
           (CLEANUP_DEFERED == null || CLEANUP_DEFERED.booleanValue() == state.isCleanupDefered()) &&
           (INDOUBT == null || INDOUBT.booleanValue() == state.isInDoubt()) &&
           (CORRUPT == null || CORRUPT.booleanValue() == state.isCorrupt()) &&
           (RESET_ON_RESTART == null || RESET_ON_RESTART.booleanValue() == state.isResetOnRestart()))
        {
          if (tc.isEntryEnabled()) 
             SibTr.exit(tc, "matches", new Boolean(true)); 

          return true;
        }
      }
      else
      {
        Boolean result = null;
        if(VISIBLE != null)
          result = new Boolean(VISIBLE.booleanValue() == state.isVisible());
        if(!(Boolean.TRUE.equals(result)) && INVISIBLE != null)
          result = new Boolean(INVISIBLE.booleanValue() == state.isInvisible());
        if(!(Boolean.TRUE.equals(result)) && UNRECONCILED != null)
          result = new Boolean(UNRECONCILED.booleanValue() == state.isUnreconciled());
        if(!(Boolean.TRUE.equals(result)) && DELETE_PENDING != null)
          result = new Boolean(DELETE_PENDING.booleanValue() == state.isDeletePending());
        if(!(Boolean.TRUE.equals(result)) && DELETE_DEFERED != null)
          result = new Boolean(DELETE_DEFERED.booleanValue() == state.isDeleteDefered());
        if(!(Boolean.TRUE.equals(result)) && ACTIVE != null)
          result = new Boolean(ACTIVE.booleanValue() == state.isActive());
        if(!(Boolean.TRUE.equals(result)) && CLEANUP_PENDING != null)
          result = new Boolean(CLEANUP_PENDING.booleanValue() == state.isCleanupPending());
        if(!(Boolean.TRUE.equals(result)) && CLEANUP_DEFERED != null)
          result = new Boolean(CLEANUP_DEFERED.booleanValue() == state.isCleanupDefered());
        if(!(Boolean.TRUE.equals(result)) && INDOUBT != null)
           result = new Boolean(INDOUBT.booleanValue() == state.isInDoubt());
        if(!(Boolean.TRUE.equals(result)) && CORRUPT != null)
           result = new Boolean(CORRUPT.booleanValue() == state.isCorrupt());
        if(!(Boolean.TRUE.equals(result)) && RESET_ON_RESTART != null)
           result = new Boolean(RESET_ON_RESTART.booleanValue() == state.isResetOnRestart());           
        
        if(result == null)
        { 
          if (tc.isEntryEnabled()) 
            SibTr.exit(tc, "matches", new Boolean(true));
          return true;
        }

        if (tc.isEntryEnabled()) 
             SibTr.exit(tc, "matches", result);         
        return result.booleanValue();              
      }            
    }      
    if (tc.isEntryEnabled()) 
           SibTr.exit(tc, "matches", new Boolean(false));

    return false;  
  }     
}
