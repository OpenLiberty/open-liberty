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
 * This class is used by the FragmentComposer to remember the 
 * status code with comment
 * as part of the state that is remembered just
 * prior to the execution of a JSP so that it can be executed 
 * again without executing its parent JSP.
 */
public class StatusSideEffect implements ResponseSideEffect
{
    private static final long serialVersionUID = 6532739036952813391L;
    private int statusCode = 0;
    private String comment = null;

    public String toString() {
       StringBuffer sb = new StringBuffer("Status side effect: \n\t");
       sb.append("Status code: ").append(statusCode).append("\n");
       if (comment != null) {
          sb.append("\tComment: ").append(comment).append("\n");
       }
       return sb.toString();
    }


    /**
     * Constructor with parameter.
     * 
     * @param statusCode The status code.
     * @param comment The comment.
     */
    public
    StatusSideEffect(int statusCode,
                     String comment)
    {
        this.statusCode = statusCode;
        this.comment = comment;
    }

	/**
	 * This resets the state of an HTTP response object to be just 
	 * as it was prior to executing a JSP.
	 *
	 * @param response The response object.
	 *    
	 *  */
	public void performSideEffect(HttpServletResponse response) {
		response.setStatus(statusCode, comment);
	}
}
