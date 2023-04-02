/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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
package com.ibm.ws.security.oauth20.error.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;

public class OAuth20AuthorizeRequestExceptionHandlerTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final HttpServletRequest req = mockery.mock(HttpServletRequest.class);
    private final HttpServletResponse rsp = mockery.mock(HttpServletResponse.class);
    private final OAuthResult result = mockery.mock(OAuthResult.class);
    private final String formattedErrorMessage = "Test formatted error message.";
    private final String errorMessage_zh_TW = "CWOAU0033E: \\u907a\\u6f0f\\u5fc5\\u8981\\u7684\\u57f7\\u884c\\u6642\\u671f\\u53c3\\u6578\\uff1aclient_id";

    @Test
    public void informResourceOwner() throws Exception {
        createInformResourceOwnerExpectations("en_us", formattedErrorMessage);
        OAuth20AuthorizeRequestExceptionHandler handler = createHandler();

        handler.handleResultException(req, rsp, result);
    }

    @Test
    public void informResourceOwner_differentLocale() throws Exception {
        createInformResourceOwnerExpectations("zh_TW", errorMessage_zh_TW);
        OAuth20AuthorizeRequestExceptionHandler handler = createHandler();

        handler.handleResultException(req, rsp, result);
    }

    private OAuth20AuthorizeRequestExceptionHandler createHandler() {
        String responseType = OAuth20Constants.RESPONSE_TYPE_CODE;
        String redirectUri = "https://localhost/oidcclient/redirect/client01";
        String templateUrl = "";
        return new OAuth20AuthorizeRequestExceptionHandler(responseType, redirectUri, templateUrl);
    }

    private void createInformResourceOwnerExpectations(String localeLanguage, String errorMessage) throws Exception {
        Locale locale = new Locale(localeLanguage);
        createRequestExpectations(locale);
        createResponseExpectations(locale, errorMessage);
        createResultExpectations(locale, errorMessage);
    }

    private void createRequestExpectations(final Locale locale) {
        mockery.checking(new Expectations() {
            {
                allowing(req).getCharacterEncoding();
                will(returnValue("utf-8"));
                allowing(req).getLocale();
                will(returnValue(locale));
            }
        });
    }

    private void createResultExpectations(final Locale locale, final String errorMessage) {
        final OAuthException oauthException = mockery.mock(OAuth20Exception.class);
        mockery.checking(new Expectations() {
            {
                one(result).getStatus();
                will(returnValue(OAuthResult.STATUS_FAILED));
                one(result).getCause();
                will(returnValue(oauthException));
                one(oauthException).getError();
                will(returnValue("unauthorized_client"));
                one(oauthException).getError();
                will(returnValue("unauthorized_client"));
                one(oauthException).formatSelf(with(locale), with(any(String.class)));
                will(returnValue(errorMessage));
            }
        });
    }

    private void createResponseExpectations(final Locale locale, final String errorMessage) throws IOException {
        final PrintWriter pw = mockery.mock(PrintWriter.class);
        mockery.checking(new Expectations() {
            {
                one(rsp).setLocale(locale);
                one(rsp).getWriter();
                will(returnValue(pw));
                one(pw).print(errorMessage);
            }
        });
    }

}
