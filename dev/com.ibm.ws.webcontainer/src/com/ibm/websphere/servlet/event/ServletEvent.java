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

import javax.servlet.ServletContext;

import com.ibm.ws.webcontainer.util.EmptyEnumeration;

/**
 * Generic servlet event.
 * 
 * @ibm-api
 */
public class ServletEvent extends ApplicationEvent{
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3906650803920713522L;
	private String _servletName;
    private String _servletClassName;

    /**
     * ServletEvent contructor.
     * @param source the object that triggered this event.
     * @param servletName the name of the servlet that triggered the event.
     */
    public ServletEvent(Object source, ServletContext context, String servletName, String servletClassName){
        super(source, context, EmptyEnumeration.instance());
        _servletName = servletName;
        _servletClassName = servletClassName;
    }

    /**
     * Get the name of the servlet that triggered this event.
     */
    public String getServletName(){
        return _servletName;
    }

    /**
     * Get the name of the servlet class that triggered this event.
     */
    public String getServletClassName(){
        return _servletClassName;
    }
}
