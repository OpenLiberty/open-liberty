/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.util;

/**
 * This class stores variables statically in a thread scope 
 * (same utility as we have in the container core).
 * Used for logging the call ID and SAS ID.
 * 
 * @author hagit
 * @since Sep 24, 2012
 */
public class ThreadLocalStorage {
	
	/**
	 * The call ID.
	 */
	private static ThreadLocal<String> _callID = new ThreadLocal<String>();
	
	/**
	 * The SIP Application Session (SAS) ID.
	 */
	private static ThreadLocal<String>_sasID = new ThreadLocal<String>();
    
	/**
     * @return the call ID.
     */
    public static String getCallID() {
    	return _callID.get();
    }
    
    /**
     * Sets a new value to the call ID.
     * 
     * @param id the new call ID to set.
     */
    public static void setCallID(String id) {
    	_callID.set(id);
    }
    
    /**
     * @return the SAS ID.
     */
    public static String getSasID() {
    	return _sasID.get();
    }
    
    /**
     * Sets a new value to the SAS ID.
     * 
     * @param id the new SAS ID to set.
     */
    public static void setSasID(String id) {
    	_sasID.set(id);
    }   
}
