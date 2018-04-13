/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.security.AuthResult;

/**
 * Web reply to send a HTTP Basic Auth (401) challenge to get
 * the userid/password information
 */
public class ChallengeReply extends WebReply {
    boolean taiChallengeReply = false;
    public static final String AUTHENTICATE_HDR = "WWW-Authenticate";

    public static final String REALM_HDR_PREFIX = "Basic realm=\"";
    public static final String REALM_HDR_SUFFIX = "\"";

    public ChallengeReply(String realm) {
        this(realm, HttpServletResponse.SC_UNAUTHORIZED, AuthResult.UNKNOWN);
    }

    public ChallengeReply(String realm, int code, AuthResult status) {
        super(code, null);
        message = REALM_HDR_PREFIX + realm + REALM_HDR_SUFFIX;

        if (status == AuthResult.TAI_CHALLENGE)
            taiChallengeReply = true;
        else
            taiChallengeReply = false;
    }

    @Override
    public void writeResponse(HttpServletResponse rsp) throws IOException {
        rsp.setStatus(responseCode);
        //DO NOT set the header for TAI challenge reply.
        if (!taiChallengeReply) {
            rsp.setHeader(AUTHENTICATE_HDR, message);
        }
    }
}
