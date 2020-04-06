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
 * Notification that a {@link SipApplicationSession} has expired.
 * 
 * <p>A {@link SipApplicationSessionListener} receiving this notification
 * may attempt to extend the lifetime of the application instance
 * corresponding to the expiring application session by invoking
 * {@link SipApplicationSession#setExpires}.
 */
public class SipApplicationSessionEvent extends EventObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new <code>SipApplicationSessionEvent</code> object.
	 * 
	 * @param appSession    the expired application session
	 */
    public SipApplicationSessionEvent(SipApplicationSession appSession) {
        super(appSession);
    }
    
    /**
	 * Returns the expired session object.
           * 
           * @return the expired session object
           */
    public SipApplicationSession getApplicationSession() {
        return (SipApplicationSession) getSource();
    }
}
