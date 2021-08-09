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

/**
 * A super-interface to indicate that implementors are dynamically 
 * controllable objects, and can be registered and de-registered.
 * 
 * @see com.ibm.ws.sib.admin.ControllableRegistrationService
 */
public interface Controllable {

  /**
   * Returns the UUID for this object. For objects formed from WCCM configuration
   * objects, then the UUID should be obtained from the class instance previously
   * received. If the type of object which implements this interface does not possess
   * a UUID, then null is returned.
   * @return
   */
  public String getUuid();
  
	/**
	 * Returns the name of this controllable object. This value is used as part of 
	 * the name by which the object is registered. In the current implementation,
	 * this value is used as the "name" of the JMX MBean to represent the external
	 * configuration name of this object. A controllable object must have a name, 
	 * and it must be unique within the type of object, as specified by the class
	 * ControllableType. 
	 * 
	 * The only currently supported Controllable objects are those which represent 
	 * "Message Points" (or "localization points"). The value returned by this method
	 * should be either be the name of the destination for which this object is the 
	 * Message Point or the mediation name. 
	 * @return
	 */
	public String getName();

  /**
   * Returns a "Configuration ID" to be used as the identification of the JMX MBean
   * which will subsequently be created. For objects formed from WCCM configuration
   * objects, then this value should be obtained from the class instance previously
   * received. For example, the LocalizationDefinition has a getConfigId() method 
   * for this purpose. If the type of object which implements this interface does
   * not represent the runtime instantiation of a WCCM object, then null is returned.
   * @return
   */
  public String getConfigId();
  
  /**
   * Returns an internal component identifier for this object. If the object which
   * implements this interface does not use such internal identifiers, then null is
   * returned.
   * @return
   */
  public String getId();

  /**
   * Returns the UUID of the target Messaging Engine, for those controllable objects
   * which represent remote message points. If the object which implements this interface
   * is local, then null is returned.
   * @return
   */
  public String getRemoteEngineUuid();
  
 
}
