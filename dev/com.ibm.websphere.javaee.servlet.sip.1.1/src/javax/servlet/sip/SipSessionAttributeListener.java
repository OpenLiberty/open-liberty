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
 * This listener interface can be implemented in order to get
 * notifications of changes to the attribute lists of sessions
 * within this SIP servlet application. 
 */
public interface SipSessionAttributeListener extends EventListener {
    /**
     * Notification that an attribute has been added to a session.
     * Called after the attribute is added.
     * 
     * @param ev    event identifying the affected <code>SipSession</code>
     */
    void attributeAdded(SipSessionBindingEvent ev);
    
    /**
     * Notification that an attribute has been removed from a session.
     * Called after the attribute is removed.
     * 
     * @param ev    event identifying the affected <code>SipSession</code>
     */
    void attributeRemoved(SipSessionBindingEvent ev);
    
    /**
     * Notification that an attribute has been replaced in a session.
     * Called after the attribute is replaced.
     * 
     * @param ev    event identifying the affected <code>SipSession</code>
     */
    void attributeReplaced(SipSessionBindingEvent ev);
}