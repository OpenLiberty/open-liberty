/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.servlet;

import javax.servlet.http.HttpServletResponse;

/**
 * This class is used by the FragmentComposer to remember a header
 * as part of the state that is remembered just
 * prior to the execution of a JSP so that it can be executed
 * again without executing its parent JSP.
 */
public class HeaderSideEffect implements ResponseSideEffect
{
    private static final long serialVersionUID = -8447588126318396355L;
    private String name = null;
    private String value = null;
    private boolean set = true;

    public String toString() {
       StringBuffer sb = new StringBuffer("Header side effect: \n\t");
       sb.append("Name: ").append(name).append("\n\t");
       sb.append("Value: ").append(value).append("\n");
       return sb.toString();
    }

    /**
     * Constructor with parameter.
     *
     * @param name The header name.
     * @param value The header value.
     */
    public HeaderSideEffect(String name, String value,boolean set)
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
           response.setHeader(name, value);
        else
           response.addHeader(name, value);
    }
}
