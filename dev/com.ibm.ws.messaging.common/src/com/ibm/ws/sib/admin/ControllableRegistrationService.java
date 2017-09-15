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

package com.ibm.ws.sib.admin;

import javax.management.StandardMBean;

import com.ibm.ws.sib.admin.exception.AlreadyRegisteredException;
import com.ibm.ws.sib.admin.exception.NotRegisteredException;
import com.ibm.ws.sib.admin.exception.ParentNotFoundException;
import com.ibm.ws.sib.admin.exception.ParentRegisteredException;

/**
 * This interface provides registration calls to allow objects to register
 * themselves as dynamic controllable objects.
 * <p>
 * @param controllable The object which was is to be registered as 
 * a controllable.
 * @param type The type of object which is being de-registered.
 * 
 * @return A reference to a runtime event listener to which events can be 
 * passed when they are available.
 */
public interface ControllableRegistrationService {

  /**
   * Registers a controllable object for dynamic operations.
   * 
   * @param controllable The object which is dynamically controllable.
   * @param type The type of controllable object being registered.
   * @return An event listener to which run-time events and alarms can 
   * be posted.
    * @throws AlreadyRegisteredException If the passed Controllable 
   * object has already been registered as controlling this type of 
   * resource.
   * @throws SIBExceptionInvalidValue 
   */
  public RuntimeEventListener register(Controllable controllable, ControllableType type)
    throws AlreadyRegisteredException, SIBExceptionInvalidValue;

  /**
   * Registers a hierarchic controllable object for dynamic operations.
   * 
   * @param controllable The object which is dynamically controllable.
   * @param parent An existing registered object which is the parent of 
   * the new object to register.
   * @param type The type of controllable object being registered.
   * @return An event listener to which run-time events and alarms can 
   * be posted.
   * @throws AlreadyRegisteredException If the passed Controllable 
   * object has already been registered as controlling this type of 
   * resource.
   * @throws ParentNotFoundException The specified registered parent object
   * could not be found.
   * @throws SIBExceptionInvalidValue
   */
  public RuntimeEventListener register(Controllable controllable, Controllable parent, ControllableType type)
    throws AlreadyRegisteredException, ParentNotFoundException, SIBExceptionInvalidValue;

  /**
   * Registers a hierarchic controllable object for dynamic operations.
   * 
   * @param controllable The object which is dynamically controllable.
   * @param parent An existing registered object which is the parent of 
   * the new object to register.
   * @param type The type of controllable object being registered.
   * @return An event listener to which run-time events and alarms can 
   * be posted.
   * @throws AlreadyRegisteredException If the passed Controllable 
   * object has already been registered as controlling this type of 
   * resource.
   * @throws ParentNotFoundException The specified registered parent object
   * could not be found.
   * @throws SIBExceptionInvalidValue
   */
  public RuntimeEventListener register(Controllable controllable, StandardMBean parent, ControllableType type)
    throws AlreadyRegisteredException, ParentNotFoundException, SIBExceptionInvalidValue;

  /**
   * Removes a previously registered controllable object.
   * 
   * @param controllable The object which was previously registered 
   * as a controllable.
   * @param type The type of object which is being de-registered.
   * @throws NotRegisteredException if the Controllable object has not 
   * previously been registered a controlling an object of the specified 
   * type.
   * @throws ParentRegisteredException The object being deregistered was found
   * to have a registered parent. The parent object must be deregistered first.
   * @throws SIBExceptionInvalidValue
   */
  public void deregister(Controllable controllable, ControllableType type)
    throws NotRegisteredException, ParentRegisteredException, SIBExceptionInvalidValue;

}
