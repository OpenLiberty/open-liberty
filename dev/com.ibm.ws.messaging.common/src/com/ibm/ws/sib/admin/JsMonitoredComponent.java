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

import com.ibm.ws.sib.admin.JsHealthState;

/**
 * @author wallisgd
 *
 * Any JsProcessComponent or JsEngineComponent that additionally implements
 * JsMonitoredComponent will be polled when hamanager calls the isAlive 
 * method on the JsMessagingEngineImpl's HAGroupCallback.
 * 
 * If a component requires fault monitoring it should implement this interface.
 *   
 */
public interface JsMonitoredComponent {

  /**
   * Return an indication of whether the component is healthy or not.
   * "Healthy" in this context means that the component can continue to operate 
   * without requiring a restart of the Messaging Engine.
   * 
   * Within isAlive, the component can perform internal polling of the constituent
   * parts of its state - but it would be inadvisable to solicit input from large
   * numbers of objects, because of performance.
   *  
   * @return JsHealthState
   */
  public JsHealthState getHealthState();

}
