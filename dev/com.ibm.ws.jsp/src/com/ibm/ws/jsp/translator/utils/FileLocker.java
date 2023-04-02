/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package com.ibm.ws.jsp.translator.utils;


/**
 * The FileLocker Class should be extended by anyone wanting to lock Files.
 * A ZosFileLockerImpl is contained in JSP in SERV1.  JSPComponent
 * Update the JSPClassFactory with .updateMap() in order to add and then getInstanceOf()
 *    in order to load your implementation in the place of this default class.
 *    
 * @author dmeisenb, kennas
 *
 */
  public class FileLocker {
	
    public boolean obtainFileLock(String filename)
	{
    
    	return true;
	}

	public boolean releaseFileLock(String filename)
	{

		return true;
	}
	
	
}
