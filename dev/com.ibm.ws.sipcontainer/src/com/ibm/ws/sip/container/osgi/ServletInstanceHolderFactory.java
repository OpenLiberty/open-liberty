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

public class ServletInstanceHolderFactory {
	
	private static ServletInstanceHolderInterface holderInstance;
	

	/**
	 * 
	 * @param holderInstance
	 */
	public static void setInstanceHolder(ServletInstanceHolderInterface holderInstance){
		ServletInstanceHolderFactory.holderInstance = holderInstance;
	}
	
	/**
	 * @return ServletInstanceHolderInterface
	 */
	public static ServletInstanceHolderInterface getInstanceHolder(){
		return holderInstance; 
	}

}
