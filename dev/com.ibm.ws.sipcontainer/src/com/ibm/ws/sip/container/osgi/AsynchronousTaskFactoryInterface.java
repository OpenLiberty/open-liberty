/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.sip.container.osgi;

/**
 * The <code>AsynchronousTaskFactoryInterface</code> provides an interface for getting  <code>AsynchronousWorkDispatcher</code> object.
 * <p>
 * This interface is for the sip container internal usage only.
 * 
 * @author Galina Rubinshtein, Dec 2008
 *
 */
public interface AsynchronousTaskFactoryInterface {	

	/**
	 * This method returns a new or existing AsynchronousWorkDispatcher object
	 * 
	 * @param sessionId String
	 * @return AsynchronousWorkDispatcher
	 */
	public AsynchronousWorkDispatcher getAsynchWorkTaskObject(String sessionId);

}
