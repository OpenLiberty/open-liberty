/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.filter;

import javax.servlet.http.HttpServletRequest;

import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

public class RealRequestInfo implements IRequestInfo {
    private static final String REFERRER = "Referer";
    private HttpServletRequest req;

    public RealRequestInfo(HttpServletRequest req) {
        this.req = req;
    }

    public String getHeader(String name) {
        return req.getHeader(name);
    }

    public StringBuffer getRequestURL() {
        return req.getRequestURL();
    }

    public String getRequestURI() {
        return req.getRequestURI();
    }

    public String getQueryString() {
        return req.getQueryString();
    }

    public String getRemoteAddr() {
        return req.getRemoteAddr();
    }

    public String getReferer() {
        return req.getParameter(REFERRER);
    }

    public String getApplicationName() {
        ComponentMetaData wcmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        String appName = null;
        if (wcmd != null)
        {
            appName = wcmd.getModuleMetaData().getApplicationMetaData().getName();
        }
        return appName;
    }

}
