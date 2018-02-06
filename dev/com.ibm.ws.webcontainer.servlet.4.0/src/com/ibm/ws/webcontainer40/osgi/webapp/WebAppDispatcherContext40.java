/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer40.osgi.webapp;

import javax.servlet.http.HttpServletMapping;
import javax.servlet.http.MappingMatch;

import com.ibm.ws.webcontainer.osgi.webapp.WebApp;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppDispatcherContext;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;

/**
 *
 */
public class WebAppDispatcherContext40 extends WebAppDispatcherContext {

    private HttpServletMapping _mapping;
    private MappingMatch _match;

    public WebAppDispatcherContext40() {
        super();
    }

    public WebAppDispatcherContext40(WebApp webapp) {
        super(webapp);
    }

    public WebAppDispatcherContext40(IExtendedRequest req) {
        super(req);
    }

    @Override
    public String getMappingValue() {
        if (_mapping == null) {
            return null;
        }
        return _mapping.getMatchValue();
    }

    public void setServletMapping() {
        _mapping = _request.getHttpServletMapping();
    }

    public HttpServletMapping getServletMapping() {
        return _mapping;
    }

    @Override
    public void pushServletReference(IServletWrapper wrapper) {
        super.pushServletReference(wrapper);

        //Set the mapping to the parent's mapping if this is an include or an async dispatch
        // or _match == null (when using a named dispatcher)
        if (this.isInclude() || this.isAsync() || (_mapping == null && _match == null))
            _mapping = ((WebAppDispatcherContext40) this.getParentContext()).getServletMapping();
        else
            setServletMapping();
    }

    @Override
    public void setPathElements(String servletPath, String pathInfo) {
        _mapping = null;
        super.setPathElements(servletPath, pathInfo);
    }

    public void setMappingMatch(MappingMatch match) {
        _match = match;
    }

    public MappingMatch getMappingMatch() {
        return _match;
    }

    public String getServletPathForMapping() {
        return _servletPath;
    }

    public String getPathInfoForMapping() {
        return _pathInfo;
    }

}
