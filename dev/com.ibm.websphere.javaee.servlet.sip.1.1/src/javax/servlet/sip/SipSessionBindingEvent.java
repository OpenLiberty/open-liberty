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
 * Events of this type are either sent to an object that implements
 * {@link SipSessionBindingListener} when it is bound or unbound from a
 * session, or to a {@link SipSessionAttributeListener} that has been
 * configured in the deployment descriptor when any attribute is bound,
 * unbound or replaced in a session. 
 * 
 * <p>The session binds the object by a call to
 * <code>SipSession.setAttribute</code> and unbinds the object by a call
 * to <code>SipSession.removeAttribute</code>.
 * 
 * @see SipSession
 * @see SipSessionBindingListener
 * @see SipSessionAttributeListener
 */
public class SipSessionBindingEvent extends EventObject {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/** The name that the Object is being bound to or unbound from. */
    private String name;
    
    /**
     * Constructs an event that notifies an object that it has been bound
     * to or unbound from a session. To receive the event, the object must
     * implement {@link SipSessionBindingListener}.
     * 
     * @param session   the session to which the object is bound or unbound
     * @param name      the name with which the object is bound or unbound
     */
    public SipSessionBindingEvent(SipSession session, String name) {
        super(session);
        this.name = name;
    }
    
    /**
     * Returns the name with which the object is bound to or unbound from
     * the session.
     * 
     * @return a string specifying the name with which the object is bound
     *         to or unbound from the session
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the session to or from which the object is bound or unbound.
     * 
     * @return the session to which the object is bound or from which the
     *         object is unbound
     */
    public SipSession getSession() {
        return (SipSession) getSource();
    }
}