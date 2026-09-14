/*******************************************************************************
 * Copyright (c) 2013, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.rest.v1;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler;
import com.ibm.ws.ui.internal.rest.exceptions.RESTException;
import com.ibm.ws.ui.internal.rest.exceptions.UserNotAuthorizedException;
import com.ibm.ws.ui.internal.v1.pojo.DeployToolConfig;
import com.ibm.ws.ui.internal.v1.pojo.DeployToolSummary;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * <p>Defines the deploy validation API for the version 1 of the adminUI REST API.</p>
 *
 * <p>Maps to host:port/ibm/api/adminUI/v1/deployValidation</p>
 */
public class DeployValidation extends CommonJSONRESTHandler implements V1Constants {
    private static final TraceComponent tc = Tr.register(DeployValidation.class);

    /**
     * Default constructor.
     *
     * @param toolboxService
     */
    public DeployValidation() {
        super(DEPLOY_VALIDATION_PATH, true, true);
    }

    /**
     * Gets the user's authenticated ID.
     *
     * @param request The RESTRequest from handleRequest
     * @return The authenticated user's ID.
     */
    @Trivial
    private String getUserId(RESTRequest request) {
        return request.getUserPrincipal().getName();
    }

    /** {@inheritDoc} */
    @FFDCIgnore(IOException.class)
    @Override
    public POSTResponse postBase(final RESTRequest request, RESTResponse response) throws RESTException {
        // CWE-862: Require Administrator role for this operation; Reader role is not sufficient.
        if (!isAuthorizedDefault(request, response)) {
            throw new UserNotAuthorizedException();
        }

        DeployToolConfig deployToolToValidate = readJSONPayload(request, DeployToolConfig.class);
        String passwordValidation = "unknown";
        String uid = getUserId(request);
        URLConnection conn = null;
        try {

            URL url = AccessController.doPrivileged(new PrivilegedAction<URL>() {
                @Override
                public URL run() {
                    // CWE-918: Build the target URL using only the scheme and port derived from
                    // the parsed request URL, and lock the host to "localhost" so the outbound
                    // connection always resolves to the local JVM's loopback interface.
                    // Do NOT use the host segment of getCompleteURL(): it is derived from the
                    // attacker-controlled Host request header and enables SSRF to arbitrary hosts.
                    URL url = null;
                    try {
                        URL requestURL = new URL(request.getCompleteURL());
                        String scheme = requestURL.getProtocol();
                        int port = requestURL.getPort();
                        String authority = (port == -1) ? "localhost" : "localhost:" + port;
                        url = new URL(scheme + "://" + authority + "/ibm" + ADMIN_CENTER_ROOT_PATH + "/deploy-1.0/feature");
                    } catch (MalformedURLException mue) {
                        // Do nothing. We'll return an invalid response.
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Unexpected MalformedURLException: " + mue);
                        }
                    }
                    return url;
                }
            });

            if (url != null) {
                conn = url.openConnection();

                String uidPwd = uid + ":" + deployToolToValidate.getPassword();
                conn.setRequestProperty("Authorization", "Basic " + Base64Coder.base64Encode(uidPwd));
                conn.connect();
                conn.getInputStream();
                passwordValidation = "valid";
            }

        } catch (IOException ioe) {
            if (ioe.getCause() instanceof ClassNotFoundException) {
                String msg = ioe.getCause().getMessage();
                if (msg.indexOf("com.ibm.websphere.ssl.protocol.SSLSocketFactory") > -1) {
                    Tr.error(tc, "UNSUPPORTED_SSL_SOCKET_FACTORY", ioe);
                }
            }

            // NO-OP as we'll return an invalid pwd response.
            try {
                if (conn != null && conn instanceof HttpURLConnection) {
                    HttpURLConnection httpURLConnection = (HttpURLConnection) conn;
                    if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        // wrong password
                        passwordValidation = "invalid";
                    }
                } else {
                    Tr.error(tc, "PASSWORD_VALIDATION_EXCEPTION", ioe);
                }
            } catch (IOException e) {
                Tr.error(tc, "PASSWORD_VALIDATION_EXCEPTION", ioe);
            }
        }

        DeployToolSummary deployValidationSummary = new DeployToolSummary(passwordValidation);
        POSTResponse postResponse = new POSTResponse();
        postResponse.jsonPayload = deployValidationSummary;
        return postResponse;
    }
}
