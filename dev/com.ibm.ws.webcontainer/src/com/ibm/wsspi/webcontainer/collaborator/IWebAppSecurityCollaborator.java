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
package com.ibm.wsspi.webcontainer.collaborator;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.wsspi.webcontainer.extension.ExtensionProcessor;
import com.ibm.wsspi.webcontainer.security.SecurityViolationException;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

public interface IWebAppSecurityCollaborator {

    public Object preInvoke(HttpServletRequest req, HttpServletResponse resp, String servletName, boolean enforceSecurity)
                    throws SecurityViolationException, IOException;

    public boolean authenticate(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException;

    public void login(HttpServletRequest req, HttpServletResponse resp, String username, String password)
                    throws ServletException;

    public void logout(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException;

    public Object preInvoke(String servletName)
                    throws SecurityViolationException, IOException;

    public Object preInvoke() throws SecurityViolationException;

    public void postInvoke(Object secObject) throws ServletException;

    public void handleException(HttpServletRequest req, HttpServletResponse rsp,
                                        Throwable wse) throws ServletException, IOException;

    public java.security.Principal getUserPrincipal();

    public boolean isUserInRole(String role, IExtendedRequest req);

    public ExtensionProcessor getFormLoginExtensionProcessor(IServletContext webapp);

    public ExtensionProcessor getFormLogoutExtensionProcessor(IServletContext webapp);

    public List<String> getURIsInSecurityConstraints(String appName, String contextRoot, String vHost, List<String> URIs);
}
