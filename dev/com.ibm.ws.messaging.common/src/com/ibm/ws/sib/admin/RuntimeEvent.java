/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 
package com.ibm.ws.sib.admin;

/**
 * Holds an event which occurs at run-time, which can be propogated
 * to the systems operator. 
 * 
 * @see com.ibm.ws.sib.admin.RuntimeEventListener
 */
public interface RuntimeEvent
{
	/**
	 * Obtains an internatialised text message describing the event.
	 * @return
	 */
	public String getMessage();

	/**
	 * Sets an internatialised text message describing the event.
	 */
	public void setMessage(String newMessage);
  
	/**
	 * Gets the type of the message. 
	 * @return
	 */
	public String getType();
  
	/**
	 * Sets the type of the message. 
	 * <p>
	 * For example: sib.processor.mediation.StateChanged
	 * <p>
	 * Use the full package and class name of the class which generated the 
	 * event, with one extra string on the end to indicate which event it is.
	 */
	public void setType(String newType );
  
	/**
	 * Gets the user data if there is any.
	 * <p>
	 * User data is often an object supporting the properties interface. 
	 * 
	 * @return null if there is no userdata, or an object in which there is 
	 * some userdata. 
	 */
	public Object getUserData();
  
	/**
	 * Sets the user data if there is any.
	 * <p>
	 * Setting the user data twice over-writes the first setting.
	 * 
	 * @param newUserData The user data is over-written with this new user data.
	 * If null is supplied, it indicates that there is no user data.
	 */
	public void setUserData(Object newUserData);
}

