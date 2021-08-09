/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.web;

import javax.servlet.http.HttpServletRequest;

import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.oauth.core.internal.oauth20.OAuthResultImpl;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.util.OIDCConstants;

public class Prompt {
    private static TraceComponent tc = Tr.register(Prompt.class);

    private static final String ATTR_PROMPT = "prompt";
    private String value = null;
    private boolean prompt = false;
    private boolean none = false;
    private boolean login = false;
    private boolean consent = false;

    Prompt(HttpServletRequest request) {
        String p = request.getParameter(ATTR_PROMPT);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "prompt=" + p);
        }
        if (p != null) {
            prompt = true;
            value = p;
            if (p.contains(Constants.PROMPT_NONE)) {
                none = true;
            }
            if (p.contains(Constants.PROMPT_LOGIN)) {
                login = true;
            }
            if (p.contains(Constants.PROMPT_CONSENT)) {
                consent = true;
            }
        }
    }

    // constrctor for oauth.
    Prompt() {
    }

    public OAuthResult errorLoginRequired() {
        OAuth20Exception oe = new OAuth20Exception(OIDCConstants.ERROR_LOGIN_REQUIRED, OIDCConstants.MESSAGE_LOGIN_REQUIRED, null);
        return new OAuthResultImpl(OAuthResult.STATUS_FAILED, new AttributeList(), oe);
    }

    public OAuthResult errorLoginRequired(AttributeList al) {
        OAuth20Exception oe = new OAuth20Exception(OIDCConstants.ERROR_LOGIN_REQUIRED, OIDCConstants.MESSAGE_LOGIN_REQUIRED, null);
        return new OAuthResultImpl(OAuthResult.STATUS_FAILED, al, oe);
    }

    public OAuthResult errorConsentRequired() {
        return errorConsentRequired(null);
    }

    public OAuthResult errorConsentRequired(AttributeList al) {
        OAuth20Exception oe = new OAuth20Exception(OIDCConstants.ERROR_CONSENT_REQUIRED, OIDCConstants.MESSAGE_CONSENT_REQUIRED, null);
        if (al == null) {
            al = new AttributeList();
        }
        return new OAuthResultImpl(OAuthResult.STATUS_FAILED, al, oe);
    }

    public String getValue() {
        return value;
    }

    boolean hasPrompt() {
        return prompt;
    }

    boolean hasNone() {
        return none;
    }

    /*
     * The Authorization Server SHOULD prompt the End-User for reauthentication.
     * If it cannot reauthenticate the End-User, it MUST return an error, typically login_required
     */
    boolean hasLogin() {
        return login;
    }

    /*
     * The Authorization Server SHOULD prompt the End-User for consent before returning
     * information to the Client.
     * If it cannot obtain consent, it MUST return an error, typically consent_required.
     */
    boolean hasConsent() {
        return consent;
    }
}