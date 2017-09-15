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
 * @author wallisgd
 *
 * JsMainImpl and JsMessagingEngineImpl each implement this interface to allow
 * JsProcessComponents or JsEngineComponents to report error conditions that need
 * to be reported to HAManager when hamanager calls the isAlive method on the
 * JsMessagingEngineImpl's HAGroupCallback.
 * 
 */
public interface JsHealthMonitor {

  /**
   * Report a local error - one which will result in a restart/failover of the
   * Messaging Engine. 
   */
  public void reportLocalError();

  /**
   * Report a global error - one which will not result in a restart/failover of the
   * Messaging Engine because it represents a common mode fault that would affect 
   * other servers also. 
   */
  public void reportGlobalError();

}
