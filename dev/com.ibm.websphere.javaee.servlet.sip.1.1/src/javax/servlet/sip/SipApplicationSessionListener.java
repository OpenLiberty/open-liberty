/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javax.servlet.sip;

import java.util.EventListener;

/**
 * Implementations of this interface receive notifications about
 * invalidated <code>SipApplicationSession</code> objects in the
 * SIP application they are part of. To receive notification events,
 * the implementation class must be configured in the deployment
 * descriptor for the servlet application.
 */
public interface SipApplicationSessionListener extends EventListener {
	
    /**
     * Notification that a session was created.
     * 
     * @param sipApplicationSessionEvent   the notification event
     */
    void sessionCreated(SipApplicationSessionEvent sipApplicationSessionEvent);
    
    /**
     * Notification that a session was invalidated. Either it timed out
     * or it was explicitly invalidated. It is not possible to extend the
     * application sessions lifetime.
     * 
     * @param sipApplicationSessionEvent    the notification event
     */
    void sessionDestroyed(SipApplicationSessionEvent sipApplicationSessionEvent);
    
    /**
     * Notification that an application session has expired. The
     * application may request an extension of the lifetime of the
     * application session by invoking
     * {@link SipApplicationSession#setExpires}.
     * 
     * @param sipApplicationSessionEvent    the notification event
     */
    void sessionExpired(SipApplicationSessionEvent sipApplicationSessionEvent);
    
    
    /**
     * Notification that a SipApplicationSession is in the ready-to-invalidate 
     * state. The container will invalidate this session upon completion of this 
     * callback unless the listener implementation calls 
     * SipApplicationSessionEvent.getApplicationSession().setInvalidateWhenReady(false)
     * 
     * @param sipApplicationSessionEvent - the notification event
     * 
     * @see SipApplicationSession#isReadyToInvalidate()
     */
    void sessionReadyToInvalidate(SipApplicationSessionEvent sipApplicationSessionEvent);
}