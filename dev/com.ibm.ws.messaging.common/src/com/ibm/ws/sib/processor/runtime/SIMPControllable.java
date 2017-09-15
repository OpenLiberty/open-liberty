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
package com.ibm.ws.sib.processor.runtime;

import com.ibm.ws.sib.admin.Controllable;

/**
 * All controllable sub-interfaces from the mesasge processor component
 * are sub-interfaces of this super-interface.
 */
public interface SIMPControllable extends Controllable
{
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
   * Message Point
   * @return
   */
  public String getName();

  /**
   * Returns an internal component identifier for this object. If the object which
   * implements this interface does not use such internal identifiers, then null is
   * returned.
   * @return
   */
  public String getId();
}
