/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.jwt;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.token.propagation.TokenPropagationHelper;
import com.ibm.ws.security.jwt.internal.TraceConstants;

/**
 *
 * <p>
 * These jax-rs client request filter APIs retrieve the JWT token from the
 * subject on the thread or self issue a JWT token and then adds the token to
 * the jax-rs client request header to propagate the JWT token. <br>
 * <br>
 * The code snippet that is shown here demonstrate how to use these APIs to
 * propagate the token. <br>
 * <dl>
 * <dt><B>Sample code to propagate the JWT Token</B>
 * <dd>
 *
 * <pre>
 *        // 1. Create a jax-rs client
 *         javax.ws.rs.client.Client client = javax.ws.rs.client.ClientBuilder.newClient();
 *
 *        // 2. Register the jwt client request filter api using one of the three ways shown here
 *         client.register(new JwtHeaderInjecter()); or
 *         client.register(new JwtHeaderInjecter("customHeader")); or
 *         client.register(new JwtHeaderInjecter("customHeader","jwtBuilder"));
 *
 *        // 3. Make REST request - The jwt token from the subject on the thread will be added to the default header "Authorization" or to the custom header
 *         String response = client.target(requesturl).request("text/plain").get(String.class)
 *
 *
 * </pre>
 * </dl>
 *
 * @author IBM Corporation
 *
 * @version 1.0
 *
 * @since 1.1
 *
 * @ibm-api
 */

public class JwtHeaderInjecter implements ClientRequestFilter {
    private static final TraceComponent tc = Tr.register(JwtHeaderInjecter.class, TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);

    String jwt = null;;
    String header = null;

    /**
     * Adds the JWT token to the Authorization header in the jax-rs client
     * requests to propagate the token.
     *
     * @param requestContext
     *            jax-rs client request context
     */
    @Override
    public void filter(ClientRequestContext requestContext) {
        //
        if (requestContext == null || jwt == null || requestContext.getHeaders().containsKey("Authorization")) {
            return;
        }
        if (header == null || header.equals("")) {
            header = "Authorization";
        }
        final String headerValue;
        if ("Authorization".equals(header)) {
            headerValue = "Bearer " + jwt;
        } else {
            headerValue = jwt;
        }

        requestContext.getHeaders().add(header, headerValue);
    }

    /**
     * Retrieves the JWT token from the Subject on the thread. The filter will
     * add the JWT token to the Authorization header using the default header
     * "Authorization".
     */
    public JwtHeaderInjecter() throws Exception {
        // inject jwt from Subject, default header
        jwt = TokenPropagationHelper.getJwtToken();
        if (jwt == null) {
            String msg = Tr.formatMessage(tc, "JWT_PROPAGATION_INVALID_TOKEN_ERR");
            Tr.error(tc, msg);
            throw new Exception(msg);
        }
    }

    /**
     * Retrieves the JWT token from the Subject on the thread. The filter will
     * add the JWT token to the header using the specified header name.
     *
     * @param headername
     *            custom header name to use.
     */

    public JwtHeaderInjecter(String headername) throws Exception {
        // inject jwt from Subject
        header = headername;
        jwt = TokenPropagationHelper.getJwtToken();
        if (jwt == null) {
            String msg = Tr.formatMessage(tc, "JWT_PROPAGATION_INVALID_TOKEN_ERR");
            Tr.error(tc, msg);
            throw new Exception(msg);
        }
    }

    /**
     * Self issues a JWT token using the specified JWT builder configuration.
     * The filter will add the JWT token to the header using the specified
     * header name.
     *
     * @param headername
     *            custom header name to use.
     * @param builder
     *            ID of a corresponding {@code jwtBuilder} element in the server
     *            configuration.
     */

    public JwtHeaderInjecter(String headername, String builder) throws Exception {
        // build new JWT based on the issuer
        header = headername;
        try {
            String sub = TokenPropagationHelper.getUserName();
            JwtToken token = JwtBuilder.create(builder).subject(sub).buildJwt();
            jwt = token.compact();

        } catch (InvalidBuilderException e) {
            throw e;
        } catch (JwtException e) {
            throw e;
        } catch (InvalidClaimException e) {
            throw e;
        }
    }

}
