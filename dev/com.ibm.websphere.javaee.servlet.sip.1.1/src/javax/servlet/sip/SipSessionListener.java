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
 * Implementations of this interface are notified of changes to the list
 * of active <code>SipSessions</code> in a SIP servlet application. To
 * recieve notification events, the implementation class must be configured
 * in the deployment descriptor for the SIP application.
 */
public interface SipSessionListener extends EventListener {
    /**
     * Notification that a <code>SipSession</code> was created.
     * 
     * @param se    the notification event
     */
    void sessionCreated(SipSessionEvent se);
    
    /**
     * Notification that a <code>SipSession</code> was destroyed.
     * 
     * @param se    the notification event
     */
    void sessionDestroyed(SipSessionEvent se);
    
    /**
     * 
     * Notification that a SipSession is in the ready-to-invalidate state. 
     * The container will invalidate this session upon completion of this callback 
     * unless the listener implementation calls 
     * SipSessionEvent.getSession().setInvalidateWhenReady(false)
     * 
     * @param se - the notification event
     * 
     * @see SipSession#isReadyToInvalidate()
     */
    void sessionReadyToInvalidate(SipSessionEvent se);
}