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
 * Objects that are bound to a session may listen to container events
 * notifying them that sessions will be passivated and that session
 * will be activated. A container that migrates session between VMs or
 * persists sessions is required to notify all attributes bound to
 * sessions implementing <code>SipSessionActivationListener</code>.
 */
public interface SipSessionActivationListener extends EventListener {

	/**
     * Notification that the session has just been activated.
     * 
     * @param se    event identifying the activated session
     */
    void sessionDidActivate(SipSessionEvent se);
    
    /**
     * Notification that the session is about to be passivated.
     * 
     * @param se    event identifying the session about to be persisted
     */
    void sessionWillPassivate(SipSessionEvent se);
}