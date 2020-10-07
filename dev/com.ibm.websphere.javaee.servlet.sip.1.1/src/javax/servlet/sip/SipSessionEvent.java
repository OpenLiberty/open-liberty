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
 * This is the class representing event notifications for changes to
 * <code>SipSession</code>s within a SIP servlet application.
 */
public class SipSessionEvent extends EventObject {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
     * Construct a session event from the given source.
     * 
     * @param source    the affected <code>SipSession</code> object
     */
    public SipSessionEvent(SipSession source) {
        super(source);
    }
    
    /**
     * Returns the session that changed.
     * 
     * @return  the affected <code>SipSession</code> object
     */
    public SipSession getSession() {
        return (SipSession) getSource();
    }
}