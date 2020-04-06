/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.sip.resolver.exception;


/**
 * Thrown when a SIP Servlet application attempts resolve a
 * URI and fails.
 * @ibm-api
 */
public class SipURIResolveException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
     * Constructs a new <code>SipURIResolveException</code> exception, without any message.
     */
	public SipURIResolveException() {
		super();
	}
	
	/**
	 * Constructs a new SipURIResolveException exception with the specified message. 
	 * 
	 * @param msg
	 */
	public SipURIResolveException(java.lang.String msg){
		super(msg);
	}
	
	/**
	 * Constructs a new SipURIResolveException exception with the specified 
	 * detail message and cause.
	 * 
	 * Note that the detail message associated with cause is not automatically 
	 * incorporated in this exception's detail message.
	 *  
	 * @param message - the detail message (which is saved for later retrieval by 
	 * 					the Throwable.getMessage() method).
	 * @param cause - the cause (which is saved for later retrieval by the Throwable.getCause() 
	 * 					method). (A null value is permitted, and indicates that the cause is 
	 * 					nonexistent or unknown.)
	 */
	public SipURIResolveException(java.lang.String message, java.lang.Throwable cause){
		super(message, cause);
	}
	
	/**
	 * Constructs a new SipURIResolveException exception with the specified cause and a 
	 * detail message of (cause==null ? null : cause.toString()) (which typically contains 
	 * the class and detail message of cause). This constructor is useful for exceptions 
	 * that are little more than wrappers for other throwables.
	 *  
	 * @param cause - the cause (which is saved for later retrieval by the Throwable.getCause() 
	 * 				method). (A null value is permitted, and indicates that the cause is nonexistent 
	 * 				or unknown.)
	 */
	public SipURIResolveException(java.lang.Throwable cause){
		super(cause);
	}	
	
	

}
