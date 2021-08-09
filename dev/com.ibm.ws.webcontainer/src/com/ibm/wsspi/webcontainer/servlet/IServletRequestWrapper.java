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

import javax.servlet.ServletRequest;
/**
 * 
 * Simple interface to allowing retrieving of a wrapped request object
 * without all the extra methods specified in the Servlet Specification
 * @ibm-private-in-use
 */
public interface IServletRequestWrapper {
	public ServletRequest getWrappedRequest();
}
