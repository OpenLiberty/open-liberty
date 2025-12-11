/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;

import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

public interface WebJaccService {

    /**
     * Propagates web constraints information to JACC.
     *
     * @param applicationName Application name
     * @param moduleName Module name
     * @param webAppConfig WebAppConfig object.
     */
    public void propagateWebConstraints(String applicationName,
                                        String moduleName,
                                        WebAppConfig webAppConfig);

    /**
     * Validates whether SSL is required for web inbound transport.
     *
     * @param applicationName Application name
     * @param moduleName Module name
     * @param uriName Uri
     * @param methodName method Name
     * @param req HttpServletObject of this request.
     * @return true if SSL is required.
     */
    public boolean isSSLRequired(String applicationName,
                                 String moduleName,
                                 String uriName,
                                 String methodName,
                                 HttpServletRequest req);

    /**
     * Validates whether the http request is excluded.
     *
     * @param applicationName Application name
     * @param moduleName Module name
     * @param uriName Uri
     * @param methodName method Name
     * @param req HttpServlet Object of this request.
     * @return true if access is excludeed.
     */
    public boolean isAccessExcluded(String applicationName,
                                    String moduleName,
                                    String uriName,
                                    String methodName,
                                    HttpServletRequest req);

    /**
     * Validates whether given Subject is granted to access the specified resource.
     *
     * @param applicationName Application name
     * @param moduleName Module name
     * @param uriName Uri
     * @param methodName method Name
     * @param req HttpServletObject of this request.
     * @param subject Subject object to be authorized.
     * @return true if access is granted.
     */
    public boolean isAuthorized(String applicationName,
                                String moduleName,
                                String uriName,
                                String methodName,
                                HttpServletRequest req,
                                Subject subject);

    /**
     * Validates whether given Subject is granted to access the specified resource.
     *
     * @param applicationName Application name
     * @param moduleName Module name
     * @param uriName Uri
     * @param req HttpServletObject of this request.
     * @param role role name to be examined.
     * @param subject Subject object to be authorized.
     * @return true if the specified subject has the specified role.
     */
    public boolean isSubjectInRole(String applicationName,
                                   String moduleName,
                                   String servletName,
                                   String role,
                                   HttpServletRequest req,
                                   Subject subject);

    /**
     * Reset the policyContext Handler as per JACC specification
     */
    public void resetPolicyContextHandlerInfo();
}
