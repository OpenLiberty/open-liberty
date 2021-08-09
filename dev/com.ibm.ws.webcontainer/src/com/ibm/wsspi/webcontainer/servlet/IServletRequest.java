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
package com.ibm.wsspi.webcontainer.servlet;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletRequest;

/**
 * 
 * 
 * This interface maybe be used by websphere components in situations where
 * they would like to do a parallel dispatch. In order to do this, they would have
 * to clone the request, and pass on the cloned copy to the new thread which 
 * does a dispatch to a resource.
 * @ibm-private-in-use
 */
public interface IServletRequest extends ServletRequest, Cloneable 
{
	/**
	 * Clones this request
	 * @return
	 * @throws CloneNotSupportedException
	 */
    public Object clone() throws CloneNotSupportedException;
    
    //used by the security component to get at information on the request
    public HashMap getInputStreamData() throws IOException;
}
