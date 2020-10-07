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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * This class handles the case when a cookie is added to a response
 * in a parent JSP.  It is cached with the child fragment, so it can
 * be reapplied prior to executing the child without executing the parent.
 */
public class AddCookieSideEffect implements ResponseSideEffect
{
    private static final long serialVersionUID = 3572236512314101134L;
    
    private String name = null;
    private String value = null;
    private String comment = null;
    private String domain = null;
    private String path = null;
    private boolean secure = false;
    private int maxAge = -1;
    private int version = 0;
    private transient Cookie cookie = null;

    public String toString() {
       StringBuffer sb = new StringBuffer("Add cookie side effect:\n\t");
       sb.append("name: ").append(name).append("\n\t");
       sb.append("value: ").append(value).append("\n");
       return sb.toString();
    }

    /**
     * Constructor with parameter.
     *
     * @param cookie The new cookie.
     */
    public 
    AddCookieSideEffect(Cookie cookie)
    {
        this.cookie = cookie;
        name = cookie.getName();
        value = cookie.getValue();
        comment = cookie.getComment();
        domain = cookie.getDomain();
        path = cookie.getPath();
        secure = cookie.getSecure();
        maxAge = cookie.getMaxAge();
        version = cookie.getVersion();        
    }

    /**
     * This implements the method in the ResponseSideEffect interface.
     * It is called by the FragmentComposerMomento.
     * 
     * @param response The response.
     */
    public void
    performSideEffect(HttpServletResponse response)
    {
        if (cookie == null) {
	    cookie = new Cookie(name, value);
	    if (comment != null) {
		cookie.setComment(comment);
	    }
	    if (domain != null) {
		cookie.setDomain(domain);
	    }
	    if (path != null) {
		cookie.setPath(path);
	    }
	    cookie.setMaxAge(maxAge);
	    cookie.setSecure(secure);
	    cookie.setVersion(version);     
        }
        response.addCookie(cookie);
    }    
}
