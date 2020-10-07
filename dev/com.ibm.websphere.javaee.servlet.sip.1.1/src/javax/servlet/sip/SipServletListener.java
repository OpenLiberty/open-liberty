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
 * Containers are required to invoke init() on the servlets before
 * the servlets are ready for service. The servlet can only be used
 * after succesful initialization. Since SIP is a peer-to-peer protocol
 * and some servlets may act as UACs, the container is required to 
 * let the servlet know when it is succesfully initialized by invoking
 * <code>SipServletListener</code>. 
 * 
 * @see SipServletContextEvent
 * @since 1.1
 */
public interface SipServletListener extends EventListener {
    /**
     * Notification that the servlet was succesfully initialized
     * @param sipServletContextEvent event identifying the initialized servlet and associated context
     */
    void servletInitialized(SipServletContextEvent sipServletContextEvent);
}
