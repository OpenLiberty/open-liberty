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
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.exceptions.InvalidOperationException;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Class to represent the various lookups for different destination types
 */ 
public abstract class State
{
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);  

  /** 
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      State.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  public final static State UNRECONCILED = new Unreconciled();
  public final static State DELETE_PENDING = new DeletePending();
  public final static State DELETE_DEFERED = new DeleteDefered();
  public final static State ACTIVE = new Active();
  public final static State CLEANUP_PENDING = new CleanupPending();
  public final static State CLEANUP_DEFERED = new CleanupDefered();
  public final static State INDOUBT = new InDoubt();
  public final static State CORRUPT = new Corrupt();
  public final static State RESET_ON_RESTART = new ResetOnRestart();
  public final static State CREATE_IN_PROGRESS = new CreateInProgress();

  public boolean isVisible()
  {
    return false;
  }
  
  public boolean isInvisible()
  {
    return false;
  }
    
  public boolean isUnreconciled()
  {
    return false;
  }
    
  public boolean isCreateInProgress()
  {
    return false;
  }
    
  public boolean isDeletePending()
  {
    return false;
  }
    
  public boolean isDeleteDefered()
  {
    return false;
  }
    
  public boolean isActive()
  {
    return false;
  }
  
  public boolean isInDoubt()
  {
    return false;
  }
    
    
  public boolean isCleanupPending()
  {
    return false;
  }
    
  public boolean isCleanupDefered()
  {
    return false;
  }
  
  public boolean isCorrupt()
  {
    return false;
  }
  
  public boolean isResetOnRestart()
  {
    return false;
  }

  public State create()
  {
    throw new InvalidOperationException(
      nls.getFormattedMessage("INTERNAL_MESSAGING_ERROR_CWSIP0005",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.indexes.statemodel.State",
          "1:136:1.13",
          this},
        null)); 
  }
  
  public State delete()
  {
    throw new InvalidOperationException(
      nls.getFormattedMessage("INTERNAL_MESSAGING_ERROR_CWSIP0005",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.indexes.statemodel.State",
          "1:147:1.13",
          this},
        null)); 
  }
  
  public State defer()
  {
    throw new InvalidOperationException(
      nls.getFormattedMessage("INTERNAL_MESSAGING_ERROR_CWSIP0005",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.indexes.statemodel.State",
          "1:158:1.13",
          this},
        null)); 
  }
  
  public State putInDoubt()
  {
    throw new InvalidOperationException(
      nls.getFormattedMessage("INTERNAL_MESSAGING_ERROR_CWSIP0005",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.indexes.statemodel.State",
          "1:169:1.13",
          this},
        null)); 
  }
  
  public State putUnreconciled()
  {
    throw new InvalidOperationException(
      nls.getFormattedMessage("INTERNAL_MESSAGING_ERROR_CWSIP0005",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.indexes.statemodel.State",
          "1:180:1.13",
          this},
        null)); 
  }
    
  public State cleanup()
  {
    throw new InvalidOperationException(
      nls.getFormattedMessage("INTERNAL_MESSAGING_ERROR_CWSIP0005",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.indexes.statemodel.State",
          "1:191:1.13",
          this},
        null)); 
  }
  
  public State cleanupComplete()
  {
    throw new InvalidOperationException(
      nls.getFormattedMessage("INTERNAL_MESSAGING_ERROR_CWSIP0005",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.indexes.statemodel.State",
          "1:202:1.13",
          this},
        null)); 
  }
  
  public State corrupt()
  {
    // It is possible for any state to move to the corrupt state.
    return State.CORRUPT;
  }
  
  public State reset()
  {
    throw new InvalidOperationException(
      nls.getFormattedMessage("INTERNAL_MESSAGING_ERROR_CWSIP0005",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.indexes.statemodel.State",
          "1:219:1.13",
          this},
        null)); 
  }

  public State createInProgress()
  {
    throw new InvalidOperationException(
      nls.getFormattedMessage("INTERNAL_MESSAGING_ERROR_CWSIP0005",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.indexes.statemodel.State",
          "1:230:1.13",
          this},
        null)); 
  }
        
        
  public abstract String toString();
}
