/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.servlet;

import javax.servlet.http.HttpServletResponse;

/**
 * This class is used by the FragmentComposer to remember the content type
 * as part of the state that is remembered just
 * prior to the execution of a JSP so that it can be executed
 * again without executing its parent JSP.
 */
public class ContentTypeSideEffect implements ResponseSideEffect
{
    private static final long serialVersionUID = -522999048151768022L;
    
    private String contentType = null;

    public String toString() {
       StringBuffer sb = new StringBuffer("Content type side effect: \n\t");
       sb.append("Content type: ").append(contentType).append("\n");
       return sb.toString();
    }


    /**
     * Constructor with parameter.
     *
     * @param contentType The content type.
     */
    public
    ContentTypeSideEffect(String contentType)
    {
        this.contentType = contentType;
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
       try {                                        //@bkma
           response.setContentType(contentType);
       } catch (IllegalStateException ex) {         //@bkma
         com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.servlet.ContentTypeSideEffect.performSideEffect", "71", this);
       }                                            //@bkma
    }
}
