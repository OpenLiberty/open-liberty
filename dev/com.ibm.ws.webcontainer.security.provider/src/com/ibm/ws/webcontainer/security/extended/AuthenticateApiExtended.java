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
package com.ibm.ws.webcontainer.security.extended;

import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.security.collaborator.CollaboratorUtils;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticateApi;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.SSOCookieHelper;
import com.ibm.ws.webcontainer.security.UnprotectedResourceService;
import com.ibm.ws.webcontainer.security.WebAuthenticator;
import com.ibm.ws.webcontainer.security.internal.WebReply;
import com.ibm.ws.webcontainer.security.internal.extended.OAuthChallengeReply;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

public class AuthenticateApiExtended extends AuthenticateApi {

    public AuthenticateApiExtended(SSOCookieHelper ssoCookieHelper,
                                   AtomicServiceReference<SecurityService> securityServiceRef,
                                   CollaboratorUtils collabUtils,
                                   ConcurrentServiceReferenceMap<String, WebAuthenticator> webAuthenticatorRef,
                                   ConcurrentServiceReferenceMap<String, UnprotectedResourceService> unprotectedResourceServiceRef,
                                   UnauthenticatedSubjectService unauthSubjectService) {
        super(ssoCookieHelper, securityServiceRef, collabUtils, webAuthenticatorRef, unprotectedResourceServiceRef, unauthSubjectService);
    }

    @Override
    public WebReply createReplyForAuthnFailure(AuthenticationResult authResult, String realm) {
        if (authResult.getStatus() == AuthResult.OAUTH_CHALLENGE) {
            return new OAuthChallengeReply(authResult.getReason());
        } else {
            return super.createReplyForAuthnFailure(authResult, realm);
        }
    }
}
