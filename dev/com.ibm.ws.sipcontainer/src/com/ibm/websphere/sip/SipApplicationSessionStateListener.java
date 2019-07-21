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
package com.ibm.websphere.sip;

import java.util.EventListener;

import javax.servlet.sip.SipApplicationSessionEvent;

/**
 * This listener will receive notification on application session passivation and 
 * activation events. A container that migrates session between VMs or
 * persists sessions must notify this listener.
 * Normally, when migrating the session to a different container on another VM,
 * the sessionWillPassivate will be called on the source container, 
 * and when the session is activated on the target container, sessionDidActivate
 * will be called there. 
 * Users of this listener must bear in mind that on case
 * of a server unexpected shutdown or connectivity failure, the failover
 * mechanism will activate the session on another container in the cluster and
 * sessionDidActivate will be called, but sessionWillPassivate might not be
 * called on the failed server. 
 * @deprecated SIP Servlet 1.1 introduced standard API {@link javax.servlet.sip.SipApplicationSessionActivationListener} 
 * @author Nitzan
 */
public interface SipApplicationSessionStateListener extends EventListener {
	/**
     * Notification that the session is about to be passivated.
     * 
     * @param applicationSessionEvent  event identifying the session about to be persisted
     */
    void sessionWillPassivate(SipApplicationSessionEvent applicationSessionEvent);
    
    /**
     * Notification that the session has just been activated.
     * 
     * @param applicationSessionEvent event identifying the activated session
     */
    void sessionDidActivate(SipApplicationSessionEvent applicationSessionEvent);
}
