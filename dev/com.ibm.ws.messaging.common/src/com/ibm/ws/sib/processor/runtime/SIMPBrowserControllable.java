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

import com.ibm.wsspi.sib.core.OrderingContext;
/**
 *
 */
public interface SIMPBrowserControllable extends SIMPControllable
{
  /**
   * Locates the Connection relating to the Browser. 
   *
   * @return SIMPConnectionControllable The connection object. 
   *
   */
  SIMPConnectionControllable getConnection();
  
  /**
   * Locates the administration destination that the browser is browsing.  
   *
   * @return Object  A Queue. 
   *
   */
  Object getDestinationObject();
  
  /**
   * Locates the ordering context for the browser.  
   *
   * @return OrderingContext  An OrderingContext or null if there is none. 
   */
  OrderingContext getOrderingContext();
  
  /**
   * Gets the remote messaging engine name that is browsing
   * 
   * @return String of the remote ME
   */
  String getRemoteMEName();
  
}
