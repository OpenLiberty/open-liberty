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

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

/**
 * SIP Servlet specific context event.
 * 
 * @see SipServletListener
 * @since 1.1
 */
public class SipServletContextEvent extends ServletContextEvent {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private SipServlet servlet;
    
    /**
     * Constructs a new <code>SipServletContextEvent</code>.
     * 
     * @param context  the ServletContext
     * @param servlet the servlet, initialization of which triggered this event
     */
    public SipServletContextEvent(ServletContext context,
                         SipServlet servlet) {
        super(context);
        this.servlet = servlet;
    }
    
    /**
     * Returns the servlet associated with the event 
     * <code>SipServletContextEvent</code>.
     * 
     * @return request object associated with this <code>SipErrorEvent</code>
     */
    public SipServlet getSipServlet() {
        return servlet;
    }
}
