/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.servlet.event;


import javax.servlet.*;
import java.util.Enumeration;

/**
 * @ibm-api
 * 
 * This is the event class that is furnished to the listeners that register to 
 * listen to Application related events.
 * 
 * @see ApplicationListener
 */
@SuppressWarnings("unchecked")
public class ApplicationEvent extends java.util.EventObject
{
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3546927969739289392L;
	private ServletContext _context;
    private Enumeration    _servletNames;

    /**
     * ApplicationEvent contructor.
     * @param source the object that triggered this event.
     * @param context the application's ServletContext
     * @param servletNames an enumeration of the names of all of the servlets in the application
     */
    public ApplicationEvent(Object source, ServletContext context, Enumeration servletNames)
    {
        super(source);
        _context = context;
        _servletNames = servletNames;
    }

    /**
     * Return the ServletContext that this event is associated with.
     */
    public ServletContext getServletContext()
    {
        return _context;
    }

    /**
     * Return the list of servlet names associated with this application
     **/
    public Enumeration getServletNames()
    {
        return _servletNames;
    }
}
