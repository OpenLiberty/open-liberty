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

import java.util.Locale;

import javax.servlet.http.HttpServletResponse;

/**
 * This class is used by the FragmentComposer to remember the content type
 * as part of the state that is remembered just
 * prior to the execution of a JSP so that it can be executed
 * again without executing its parent JSP.
 */
public class LocaleSideEffect implements ResponseSideEffect
{
    private static final long serialVersionUID = -1259252338439044960L;
    private Locale locale = null;


    public String toString() {
       StringBuffer sb = new StringBuffer("Locale side effect: \n\t");
       sb.append("Locale: ").append(locale).append("\n");
       return sb.toString();
    }


    /**
     * Constructor with parameter.
     *
     * @param locale The locale.
     */
    public LocaleSideEffect(Locale locale)
    {
        this.locale = locale;
    }

    /**
     * This resets the state of an HTTP response object to be just
     * as it was prior to executing a JSP.
     *
     * @param response The response object.
     */
    public void performSideEffect(HttpServletResponse response)
    {
       try {
           response.setLocale(locale);
       } catch (IllegalStateException ex) {
         com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.servlet.LocaleSideEffect.performSideEffect", "69", this);
       }
    }
}
