/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transaction.transport.connections;

/**
 * The interface defnining methid which Stream connection
 * listeners must implement, like accepting write errors/completions
 *  
 * @author nogat
 *
 */
public interface SipStreamConnectionWriteListener {
	
	/**
	 * called in case a write error occurs 
	 * 
	 * @param e - the exception that occurred
	 */
	public void writeError(Exception e);
	/**
	 * called in case a write complete occurs 
	 * 
	 * @param e - the exception that occurred
	 */
	public void writeComplete();

}
