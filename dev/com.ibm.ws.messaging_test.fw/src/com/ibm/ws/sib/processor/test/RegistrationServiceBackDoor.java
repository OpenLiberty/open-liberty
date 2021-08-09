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
package com.ibm.ws.sib.processor.test;

import com.ibm.ws.sib.admin.Controllable;
import com.ibm.ws.sib.admin.ControllableType;
import com.ibm.ws.sib.admin.RuntimeEventListener;
import com.ibm.ws.sib.admin.SIBExceptionInvalidValue;
import com.ibm.ws.sib.admin.exception.NotRegisteredException;

/**
 * Interface gives the unit test cases a "back door" into the 
 * controllable registration service.
 */
public interface RegistrationServiceBackDoor
{
  /**
   * If the unit test has a reference to the resource's controllable
   * implementatation object, he can add a runtime event listener of 
   * his own, to listen for specific events coming from that resource.
   * <p>
   * Calling it again forces the old event listener to be abandoned. 
   * @param listener
   * @param controllable
   */
  public void setControllableEventListener( 
    RuntimeEventListener listener,
    Controllable controllable,
    ControllableType type 
  ) throws SIBExceptionInvalidValue, NotRegisteredException ;
  
  /**
   * Allows the caller to look up a controllable by its ID.
   * @return null if not found, or the controllable if it is found.
   */
  public Controllable findControllableById( 
    String id , ControllableType type ) 
    throws SIBExceptionInvalidValue 
  ;
}
