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
package com.ibm.ws.sip.container.osgi;

/**
 * The <code>AsynchronousWorkHolder</code> handles an instance of <code>AsynchronousTaskFactoryInterface</code> object  
 * created by sip container on start.
 * 
 * <p>
 * 
 * The <code>AsynchronousTaskFactoryInterface</code> is used by <code>AsynchronousWork</code> 
 * to create an <code>AsynchronousWorkDispatcher</code> object to dispatch the asynchronous work. 
 *  
 * <p>
 * @author Galina Rubinshtein, Dec 2008
 *
 */
public class AsynchronousWorkHolder {
	
	private static AsynchronousTaskFactoryInterface asynchWorkInstance = null;
	
	/**
	 * The sip container creates on start an instance of AsynchronousTaskFactoryInterface
	 * and sets it here.
	 * 
	 * @param instance AsynchronousTaskFactoryInterface
	 */
	public static void setAsynchWorkInstance(AsynchronousTaskFactoryInterface instance){
		AsynchronousWorkHolder.asynchWorkInstance = instance;
	}
	
	/**
	 * This method returns an instance of AsynchronousTaskFactoryInterface object created by sip container.
	 * 
	 * @return AsynchronousTaskCreatorInterface object
	 */
	public static AsynchronousTaskFactoryInterface getAsynchWorkInstance(){
		return asynchWorkInstance; 
	}
}
