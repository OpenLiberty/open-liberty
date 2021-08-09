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
package com.ibm.ws.sip.container.appqueue;

/**
 * Describes exceptions that rise throughout the 
 * usage of a MessageDispatchingHandler 
 * 
 * @author Nitzan
 */
public class MessageDispatchingException extends Exception{
	public MessageDispatchingException( String msg){
		super( msg);
	}
	
	public MessageDispatchingException( String msg, Exception e){
		super( msg, e);
	}
	
	public MessageDispatchingException( Exception e){
		super( e);
	}
}
