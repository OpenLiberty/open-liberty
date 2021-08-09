/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.requestContext;

/**
 * This interface provides a standard way for requests to encapsulate
 * their context information in segments that can be parsed by request
 * probe extensions.  Implementations of this interface should still
 * extend the toString() method of java.lang.Object so that the context
 * information can be displayed in traces.
 */
public interface ContextInfoArray {
	/**
	 * Return the context information broken down into segments that can
	 * be parsed by other components such as request probe extensions.
	 */
	public String[] getContextInfoArray();
}
