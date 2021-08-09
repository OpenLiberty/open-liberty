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
package com.ibm.ws.webcontainer.core;

import java.util.Iterator;

import com.ibm.wsspi.webcontainer.*;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

/**
 * A RequestMapper is an optimized data structure that serves up the target 
 * or intermediate target in the request processing delegation chain.
 */
public interface RequestMapper 
{
   
   /**
    * @param reqURI
    * @return RequestProcessor
    */
   public RequestProcessor map(String reqURI);
   
   /**
    * @param req
    * @return RequestProcessor
    */
   public RequestProcessor map(IExtendedRequest req);
   
   /**
    * @param path
    * @param target
    */
   public void addMapping(String path, Object target) throws Exception;
   
   /**
    * @param path
    */
   public void removeMapping(String path);
   
   /**
    * Returns an Iterator of all the target mappings added
    * to this mapper
    */
   @SuppressWarnings("unchecked")
   public Iterator targetMappings();
   
   public Object replaceMapping(String path, Object target) throws Exception;
   
   public boolean exists(String path);
   
   
}
