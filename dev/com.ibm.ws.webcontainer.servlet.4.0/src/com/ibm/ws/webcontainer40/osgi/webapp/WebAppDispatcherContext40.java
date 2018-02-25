/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer40.osgi.webapp;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletMapping;
import javax.servlet.http.MappingMatch;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer.osgi.webapp.WebApp;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppDispatcherContext;
import com.ibm.ws.webcontainer40.srt.SRTServletRequest40;
import com.ibm.wsspi.webcontainer.servlet.AsyncContext;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;

/**
 *
 */
public class WebAppDispatcherContext40 extends WebAppDispatcherContext {

    private static final TraceComponent tc = Tr.register(WebAppDispatcherContext40.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    private HttpServletMapping _mapping, _currentMapping, _originalMapping;
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

        _currentMapping = ((SRTServletRequest40) _request).getCurrentHttpServletMapping(this);

        //Check if this is the original servlet in the chain and set _originalMapping accordingly
        if (this.getParentContext() == null || ((WebAppDispatcherContext40) this.getParentContext())._originalMapping == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "This is the original servlet in the chain. Saved the mapping.");
            }
            this._originalMapping = _currentMapping;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Found an original servlet in the chain. Using its mapping.");
            }
            this._originalMapping = ((WebAppDispatcherContext40) this.getParentContext())._originalMapping;
        }

        //These 2 values will be null only when using a named dispatcher
        boolean isNamedDispatcher = _mapping == null && _match == null;

        if (isNamedDispatcher) {
            //Set the mapping to the parent's mapping if using a named dispatcher
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "This is a named dispatch. Setting the HttpServletMapping to the parent context's mapping.");
            }
            _mapping = ((WebAppDispatcherContext40) this.getParentContext()).getServletMapping();
        } else if (this.isInclude() || this.isAsync()) {
            //Set the mapping to the mapping of the (original) first servlet in the invocation chain when this is an include or async dispatch
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "This is an include or async dispatch. Setting the HttpServletMapping to the original mapping.");
            }
            _mapping = _originalMapping;
        } else {
            //This is a forward or the original request, set the mapping to the current servlet's mapping
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Setting the HttpServletMapping to the current servlet's mapping.");
            }
            _mapping = _currentMapping;
        }

        /*
         * Set the HttpServletMapping related attributes. If this is a named dispatcher, do not set the forward
         * or include attributes. Do not set the forward or async attributes if they have been already set since
         * they will always reflect the original servlet's mapping
         */
        if (this.isForward() && !isNamedDispatcher && _request.getAttribute(RequestDispatcher.FORWARD_MAPPING) == null) {
            //Request attribute under which the original HttpServletMapping is made available to the target of a forward
            _request.setAttribute(RequestDispatcher.FORWARD_MAPPING, _originalMapping);
        } else if (this.isInclude() && !isNamedDispatcher) {
            //Request attribute under which the HttpServletMapping of the target of an include is stored
            _request.setAttribute(RequestDispatcher.INCLUDE_MAPPING, _currentMapping);
        } else if (this.isAsync() && _request.getAttribute(RequestDispatcher.FORWARD_MAPPING) == null) {
            //Request attribute under which the original HttpServletMapping is made available to the target of a dispatch(String) or dispatch(ServletContext,String)
            _request.setAttribute(AsyncContext.ASYNC_MAPPING, _originalMapping);
        }
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
