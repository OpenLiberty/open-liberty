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
 * Causes an object to be notified when it is bound to or unbound from
 * a SipSession. The object is notified by an {@link SipSessionBindingEvent}
 * object. This may be as a result of a servlet programmer explicitly
 * unbinding an attribute from a session, due to a session being invalidated,
 * or die to a session timing out.
 * 
 * @see SipSession
 * @see SipSessionBindingEvent
 */
public interface SipSessionBindingListener extends EventListener {
    /**
     * Notifies the object that it is being bound to a session and
     * identifies the session.
     * 
     * @param event the event that identifies the session
     */
    void valueBound(SipSessionBindingEvent event);
    
    /**
     * Notifies the object that it is being unbound from a session and
     * identifies the session.
     * 
     * @param event the event that identifies the session
     */
    void valueUnbound(SipSessionBindingEvent event);
}