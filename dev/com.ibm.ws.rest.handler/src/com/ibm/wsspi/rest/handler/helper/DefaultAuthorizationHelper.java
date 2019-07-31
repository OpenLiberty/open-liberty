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
package com.ibm.wsspi.rest.handler.helper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.management.security.ManagementSecurityConstants;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * This helper service performs the default authorization on the given user.
 *
 * <p/>
 * The following table shows the allowed operations for each administrative role.
 *
 * <table>
 * <thead>
 * <tr><th>Operation</th><th>Administrator Role</th><th>Reader Role</th></tr>
 * </thead>
 * <tbody>
 * <tr><td>GET</td><td align="center">X</td><td align="center">X</td></tr>
 * <tr><td>PUT</td><td align="center">X</td><td align="center"></td></tr>
 * <tr><td>POST</td><td align="center">X</td><td align="center"></td></tr>
 * <tr><td>DELETE</td><td align="center">X</td><td align="center"></td></tr>
 * <tr><td>OPTIONS</td><td align="center">X</td><td align="center"></td></tr>
 * </tbody>
 * </table>
 *
 * @ibm-spi
 */
@Component(service = { DefaultAuthorizationHelper.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM" })
public class DefaultAuthorizationHelper {

    /**
     * The set of roles that is sufficient by default to do most HTTP operations.
     */
    private static final Set<String> REQUIRED_ROLES_DEFAULT = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[] { ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME })));

    /**
     * The set of roles that is sufficient to perform a GET operation.
     */
    private static final Set<String> REQUIRED_ROLES_GET = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[] { ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME,
                                                                                                                                       ManagementSecurityConstants.READER_ROLE_NAME })));

    public boolean checkAdministratorRole(RESTRequest request, RESTResponse response) throws IOException {
        boolean isGetMethod = "GET".equals(request.getMethod());

        /*
         * Authorize:
         * 1) principals with the administrator role, or
         * 2) principals with the reader role if the HTTP request method is GET.
         */
        boolean isAuthorized = request.isUserInRole(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME)
                               || (isGetMethod && request.isUserInRole(ManagementSecurityConstants.READER_ROLE_NAME));

        if (!isAuthorized) {
            // Is not authorized, so build the error message.
            response.sendError(403, "Forbidden");
            response.setRequiredRoles(isGetMethod ? REQUIRED_ROLES_GET : REQUIRED_ROLES_DEFAULT);
        }

        return isAuthorized;
    }
}
