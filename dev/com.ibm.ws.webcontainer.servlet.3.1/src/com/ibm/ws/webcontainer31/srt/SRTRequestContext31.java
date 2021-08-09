/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer31.srt;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.srt.SRTRequestContext;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;

import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.session.impl.HttpSessionContext31Impl;

/**
 * SRTREquestContext specific to Servlet 3.1
 */
public class SRTRequestContext31 extends SRTRequestContext {
    private static final String CLASS_NAME="com.ibm.ws.webcontainer31.srt.SRTRequestContext31";
    
    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(SRTRequestContext31.class, 
                                                         WebContainerConstants.TR_GROUP, 
                                                         WebContainerConstants.NLS_PROPS );   
    

    /**
     * @param request
     */
    public SRTRequestContext31(SRTServletRequest request) {
        super(request);
    }
    
    // Added for support of HttpSessionIdListeners
    public HttpSession generateNewId(WebApp webapp) {
        HttpSession existingSession = (HttpSession) webappToSessionMap.get(webapp);
        if (existingSession != null) {
            if (!webapp.getSessionContext().isValid(existingSession, request, false)) {
                existingSession = null;
            }
        } else {
            // Looks like the session wasn't obtained during the preinvoke
            // call.  Should only happen if session doesn't exist at preinvoke.
            existingSession = webapp.getSessionContext().getIHttpSession(request, (HttpServletResponse) request.getResponse(), false);                
        }
        if ( existingSession == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled()) {  
                Tr.error(tc, "changeSessionId.no.session.associated.with.request", new Object[] {request.getRequestURI()});
            }
            throw new IllegalStateException(Tr.formatMessage(tc, "changeSessionId.no.session.associated.with.request", request.getRequestURI()));
        }
        
        HttpSession session = ((HttpSessionContext31Impl)webapp.getSessionContext()).generateNewId(request, (HttpServletResponse) request.getResponse(), existingSession);
        
        return session;
    }

}
