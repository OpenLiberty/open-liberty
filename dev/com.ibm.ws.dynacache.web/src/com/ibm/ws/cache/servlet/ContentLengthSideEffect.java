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

import javax.servlet.http.HttpServletResponse;

/**
 * This class is used by the FragmentComposer to remember the content length
 * as part of the state that is remembered just
 * prior to the execution of a JSP so that it can be executed 
 * again without executing its parent JSP.
 */
public class ContentLengthSideEffect implements ResponseSideEffect
{
    private static final long serialVersionUID = 3327341212480328259L;
    
    private int length = 0;

    public String toString() {
       StringBuffer sb = new StringBuffer("Content length side effect: \n\t");
       sb.append("length: ").append(length).append("\n");
       return sb.toString();
    }




    /**
     * Constructor with parameter.
     * 
     * @param lenght The content lenght.
     */
    public
    ContentLengthSideEffect(int length)
    {
        this.length = length;
    }

    /**
     * This resets the state of an HTTP response object to be just 
     * as it was prior to executing a JSP. 
     *
     * @param response The response object.
     */
    public void
    performSideEffect(HttpServletResponse response)
    {
        response.setContentLength(length);
    }
}
