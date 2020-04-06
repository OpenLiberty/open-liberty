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
 * listeners must implement, like accepting read errors/completions
 *  
 * @author nogat
 *
 */
public interface SipStreamConnectionReadListener {
	
	/**
	 * called in case a read error occurs 
	 * 
	 * @param e - the exception that occurred
	 */
	public void readError(Exception e);
	/**
	 * called in case a read complete occurs 
	 * 
	 * @param e - the exception that occurred
	 */
	public void readComplete();

}
