/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.pmi;

/**
 * @author Assya Azrieli, November, 2006
 */
public interface ApplicationModuleInterface{

	/**
    * @return Request Module
    *  
    */
   public SessionInterface getSessionModule() ;
	   
	/**
    * @return Request Module
    *  
    */
   public RequestModuleInterface getRequestModule() ;
   
   /**
    * @return Response Module
    *  
    */
   public ResponseModuleInterface getResponseModule() ;
   
   /**
    * @return Application Task Duration Module 
    *  
    */
   public ApplicationTaskDurationModuleInterface getApplicationTaskDurationModule() ;
   
   /**
    * Update counters that were countered till now
    *
    */
   public void updateCounters();
   
   /**
    * Unregister module 
    */
   public void destroy();
}
