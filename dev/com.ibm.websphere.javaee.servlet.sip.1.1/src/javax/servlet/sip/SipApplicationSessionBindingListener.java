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

import java.util.EventListener;

/**
 * 
 * Causes an object to be notified when it is bound to or unbound from a 
 * SipApplicationSession. The object is notified by an 
 * SipApplicationSessionBindingEvent  object. This may be as a result of a 
 * servlet programmer explicitly unbinding an attribute from an application 
 * session, due to an application session being invalidated, or due to an 
 * application session timing out.
 * 
 * @since 1.1
 * 
 * @see javax.servlet.sip.SipApplicationSession 
 * @see javax.servlet.sip.SipApplicationSessionBindingEvent 
 */
public interface SipApplicationSessionBindingListener extends EventListener{
	
	/**
	 * Notifies the object that it is being bound to an application 
	 * session and identifies the application session.
	 * 
	 * @param event the event that identifies the application session
	 */
	void valueBound(SipApplicationSessionBindingEvent event);
	
	/**
	 * Notifies the object that it is being unbound from an application session 
	 * and identifies the application session.
	 * 
	 * @param event the event that identifies the application session
	 */
	void valueUnbound(SipApplicationSessionBindingEvent event);

}
