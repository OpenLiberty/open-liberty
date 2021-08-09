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
package javax.servlet.sip;

import java.util.EventObject;

/**
 * 
 * Events of this type are either sent to an object that implements 
 * SipApplicationSessionBindingListener when it is bound or unbound 
 * from an application session, or to a SipApplicationSessionAttributeListener 
 * that has been configured in the deployment descriptor when any attribute 
 * is bound, unbound or replaced in an application session.
 * 
 * The session binds the object by a call to 
 * SipApplicationSession.setAttribute(String, Object) and unbinds the object 
 * by a call to SipApplicationSession.removeAttribute(String). 
 *
 * @since 1.1
 */
public class SipApplicationSessionBindingEvent extends EventObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String name;
	
	/**
	 * Constructs an event that notifies an object that it has been bound to 
	 * or unbound from an application session. To receive the event, 
	 * the object must implement SipApplicationSessionBindingListener.
	 * @param 	session the application ession to which the object is bound or 
	 * 			unbound
	 * @param 	name the name with which the object is bound or unbound
	 */
	public SipApplicationSessionBindingEvent(SipApplicationSession session,
            String name){
		super(session);
		this.name = name;
	}
	
	/**
	 * Returns the application session to or from which the object is bound 
	 * or unbound.
	 * 
	 * @return 	the application session to which the object is bound or 
	 * 			from which the object is unbound
	 */
	public SipApplicationSession getApplicationSession() {
		return (SipApplicationSession) getSource();
	}
	
	/**
	 * Returns the name with which the object is bound to or unbound from 
	 * the application session.
	 *  
	 * @return 	a string specifying the name with which the object is bound 
	 * 			to or unbound from the session
	 */
	public String getName(){
		return this.name;
	}
}
