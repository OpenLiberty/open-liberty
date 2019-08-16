/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    @Test
    public void informResourceOwner() throws Exception {
        createRequestExpectations();
        createResponseExpectations();
        createResultExpectations();
        String responseType = OAuth20Constants.RESPONSE_TYPE_CODE;
        String redirectUri = "https://localhost/oidcclient/redirect/client01";
        String templateUrl = "";
        OAuth20AuthorizeRequestExceptionHandler handler = new OAuth20AuthorizeRequestExceptionHandler(responseType, redirectUri, templateUrl);

        handler.handleResultException(req, rsp, result);
    }

    private void createRequestExpectations() {
        mockery.checking(new Expectations() {
            {
                allowing(req).getCharacterEncoding();
                will(returnValue("utf-8"));
                allowing(req).getLocale();
                will(returnValue(new Locale("en_us")));
            }
        });
    }

    private void createResultExpectations() {
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
                one(oauthException).formatSelf(with(any(Locale.class)), with(any(String.class)));
                will(returnValue("Test formatted error message."));
            }
        });
    }

    private void createResponseExpectations() throws IOException {
        final PrintWriter pw = mockery.mock(PrintWriter.class);
        mockery.checking(new Expectations() {
            {
                one(rsp).getWriter();
                will(returnValue(pw));
                one(pw).print(formattedErrorMessage);
            }
        });
    }

}
