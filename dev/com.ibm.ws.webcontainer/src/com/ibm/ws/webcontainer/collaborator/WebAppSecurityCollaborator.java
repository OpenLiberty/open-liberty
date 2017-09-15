/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.collaborator;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppSecurityCollaborator;
import com.ibm.wsspi.webcontainer.extension.ExtensionProcessor;
import com.ibm.wsspi.webcontainer.security.SecurityViolationException;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

public class WebAppSecurityCollaborator implements IWebAppSecurityCollaborator {

    public Object preInvoke(HttpServletRequest req, HttpServletResponse resp,
                            String servletName, boolean enforceSecurity)
                        throws SecurityViolationException, IOException {

        //don't allow default "TRACE" request by default - even when security is disabled
        
        String defaultMethod = (String) req.getAttribute("com.ibm.ws.webcontainer.security.checkdefaultmethod");
        
        if ("TRACE".equals(defaultMethod) && !WCCustomProperties.ENABLE_TRACE_REQUESTS) {
            //in the security code, the cause exception is a deny reply
            Exception exceptionSentToHandleException = new Exception("IBMWebContainerTraceRequestException");
            SecurityViolationException secVE = new SecurityViolationException("Illegal request. Default implementation of TRACE not allowed.", HttpServletResponse.SC_FORBIDDEN);
            secVE.initCause(exceptionSentToHandleException);
            throw secVE;
        }
        return null;
    }

    public Object preInvoke(String servletName)
                        throws SecurityViolationException, IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public Object preInvoke() throws SecurityViolationException {
        // TODO Auto-generated method stub
        return null;
    }

    public void postInvoke(Object secObject) throws ServletException {
    // TODO Auto-generated method stub

    }

    public void handleException(HttpServletRequest req,
                                HttpServletResponse rsp, Throwable wse)
                        throws ServletException, IOException {
        //this is typically overridden within the security code.
        if (wse!=null && "IBMWebContainerTraceRequestException".equals(wse.getMessage())) {
            //this message is the same non-translated message that security reports when security is enabled
            rsp.sendError(HttpServletResponse.SC_FORBIDDEN, "Illegal request. Default implementation of TRACE not allowed.");
        }
    }

    public Principal getUserPrincipal() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isUserInRole(String role, IExtendedRequest req) {
        // TODO Auto-generated method stub
        return false;
    }

    public ExtensionProcessor getFormLoginExtensionProcessor(
                                                             IServletContext webapp) {
        // TODO Auto-generated method stub
        return null;
    }

    public ExtensionProcessor getFormLogoutExtensionProcessor(
                                                              IServletContext webapp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean authenticate(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void login(HttpServletRequest req, HttpServletResponse resp, String username, String password) throws ServletException {
    // TODO Auto-generated method stub

    }

    @Override
    public void logout(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
    // TODO Auto-generated method stub

    }

    @Override
    public List<String> getURIsInSecurityConstraints(String appName,
                                                     String contextRoot, String host, List<String> URIs) {
        return null;
    }
}
