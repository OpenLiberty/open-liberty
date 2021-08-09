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
 * Those components that require to participate in the Event Management infrastructure
 * need to implement this interface rather than the JsEngineComponent interface.
 * 
 * Such components can support a RuntimeEventListener (set into them by Admin)
 * through which they can emit run-time events.
 */
public interface JsEngineComponentWithEventListener extends JsEngineComponent
{
  /**
   * Set the Engine Component's RuntimeEventListener.
   *  
   * @param listener A RuntimeEventListener
   */
  public void setRuntimeEventListener(RuntimeEventListener listener);
  /**
   * Get the Engine Component's RuntimeEventListener.
   */  
  public RuntimeEventListener getRuntimeEventListener();
}
