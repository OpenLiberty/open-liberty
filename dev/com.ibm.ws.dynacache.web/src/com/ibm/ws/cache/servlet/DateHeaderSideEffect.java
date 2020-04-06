/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.servlet;

import java.util.Date;

import javax.servlet.http.HttpServletResponse;

/**
 * This class is used by the FragmentComposer to remember the date header
 * as part of the state that is remembered just
 * prior to the execution of a JSP so that it can be executed
 * again without executing its parent JSP.
 */
public class DateHeaderSideEffect implements ResponseSideEffect
{
    private static final long serialVersionUID = -1692052189061271100L;
    
    private String name = null;
    private long value = 0;
    private boolean set = true;

    public String toString() {
       StringBuffer sb = new StringBuffer("Date header side effect: \n\t");
       sb.append("Name: ").append(name).append("\n\t");
       sb.append("Date: ").append(new Date(value).toString()).append("\n");
       return sb.toString();
    }


    /**
     * Constructor with parameter.
     *
     * @param name The header name.
     * @param value The header value.
     */
    public DateHeaderSideEffect(String name, long value, boolean set)
    {
        this.name = name;
        this.value = value;
        this.set = set;
    }

    /**
     * This resets the state of an HTTP response object to be just
     * as it was prior to executing a JSP.
     *
     * @param response The response object.
     */
    public void performSideEffect(HttpServletResponse response)
    {
        if (set)
           response.setDateHeader(name, value);
        else
           response.addDateHeader(name, value);
    }
}
