/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.asynch;

import com.ibm.ws.sip.container.osgi.AsynchronousTaskFactoryInterface;
import com.ibm.ws.sip.container.osgi.AsynchronousWorkDispatcher;

/**
 * This class implements AsynchronousTaskFactoryInterface. 
 * Sip container creates an instance of the factory in 
 * <code> CommonWebsphereAppLoadListener#startSipContainer() </code>
 * 
 * @author Galina
 *
 */
public class AsynchronousWorkTaskFactory implements AsynchronousTaskFactoryInterface {

	public AsynchronousWorkDispatcher getAsynchWorkTaskObject(String sessionId) {
		return new AsynchronousWorkTask(sessionId);
	}

}
