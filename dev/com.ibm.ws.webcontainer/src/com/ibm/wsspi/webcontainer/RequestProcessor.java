/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


/**
 * 
 * Interface that indicates that the implementation class is capable of processing
 * ServletRequests.
 * @ibm-private-in-use
 */
public interface RequestProcessor 
{
   
   /**
    * @param Request req
    * @param Response res@param req
    * @param res
    */
   public void handleRequest(ServletRequest req, ServletResponse res) throws Exception;
   
   /**
    * 
    * @return boolean Returns true if this request processor is for internal use only
    */
   public boolean isInternal();

   public String getName();
}
