/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.security.openidconnect.backchannellogout.BackchannelLogoutHelper;

/**
 * Servlet for OpenID Connect Back-Channel Logout (https://openid.net/specs/openid-connect-backchannel-1_0.html)
 */
public class OidcBackchannelLogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        BackchannelLogoutHelper logoutHelper = new BackchannelLogoutHelper(request, response);
        logoutHelper.handleBackchannelLogoutRequest();
    }

}