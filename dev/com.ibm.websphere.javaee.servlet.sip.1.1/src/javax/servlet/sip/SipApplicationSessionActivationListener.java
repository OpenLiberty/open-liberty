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
 * Objects that are bound to a SipApplicationSession may 
 * listen to container events notifying them when the application 
 * session to which they are bound will be passivated or activated. 
 * A container that migrates application sessions between VMs or persists 
 * them is required to notify all attributes implementing this listener and 
 * that are bound to those application sessions of the events. 
 */
public interface SipApplicationSessionActivationListener extends EventListener
{

   /**
	* Notification that the application session has just been passivated.
	* 
	* @param sipApplicationSessionevent event identifying the 
	* 					activated application session
	*/
    public void sessionDidActivate(SipApplicationSessionEvent sipApplicationSessionevent);

   /**
	* Notification that the application session has just been activated.
	* 
	* @param sipApplicationSessionevent event identifying the application session 
	* 					about to be persisted
	*/
	public void sessionWillPassivate(SipApplicationSessionEvent sipApplicationSessionevent);
	
	
	
}
