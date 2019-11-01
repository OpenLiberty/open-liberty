/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.jmock.Expectations;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

import test.common.SharedOutputManager;

public class CommonTestClass extends com.ibm.ws.security.test.common.CommonTestClass {

    protected final String uniqueId = "myUniqueId";

    protected final ComponentContext cc = mockery.mock(ComponentContext.class);
    protected final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    protected final HttpServletResponse response = mockery.mock(HttpServletResponse.class);
    protected final PrintWriter writer = mockery.mock(PrintWriter.class);
    protected final WebAppSecurityConfig webAppSecConfig = mockery.mock(WebAppSecurityConfig.class);
    protected final HttpEntity httpEntity = mockery.mock(HttpEntity.class);

    protected final static String MSG_BASE = "CWWKS";
    protected final static String MSG_BASE_ERROR_WARNING = "CWWKS[0-9]{4}(E|W)";
    protected final static String CWWKS5371E_OPENSHIFT_ERROR_GETTING_USER_INFO = "CWWKS5371E";
    protected final static String CWWKS5372E_OPENSHIFT_ACCESS_TOKEN_MISSING = "CWWKS5372E";
    protected final static String CWWKS5373E_OPENSHIFT_USER_API_BAD_STATUS = "CWWKS5373E";
    protected final static String CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED = "CWWKS5400I";
    protected final static String CWWKS5403E_SOCIAL_LOGIN_SERVER_INTERNAL_LOG_ERROR = "CWWKS5403E";
    protected final static String CWWKS5405E_SOCIAL_LOGIN_NO_SUCH_PROVIDER = "CWWKS5405E";
    protected final static String CWWKS5406E_SOCIAL_LOGIN_INVALID_URL = "CWWKS5406E";
    protected final static String CWWKS5407I_SOCIAL_LOGIN_ENDPOINT_SERVICE_ACTIVATED = "CWWKS5407I";
    protected final static String CWWKS5416W_OUTGOING_REQUEST_MISSING_PARAMETER = "CWWKS5416W";
    protected final static String CWWKS5417E_EXCEPTION_INITIALIZING_URL = "CWWKS5417E";
    protected final static String CWWKS5430W_SELECTION_PAGE_URL_NOT_VALID = "CWWKS5430W";
    protected final static String CWWKS5431E_SELECTION_PAGE_URL_NOT_HTTP = "CWWKS5431E";
    protected final static String CWWKS5432W_CUSTOM_SELECTION_INITED_MISSING_WEBAPP_CONFIG = "CWWKS5432W";
    protected final static String CWWKS5433E_REDIRECT_NO_MATCHING_CONFIG = "CWWKS5433E";
    protected final static String CWWKS5434E_ERROR_PROCESSING_REDIRECT = "CWWKS5434E";
    protected final static String CWWKS5435E_USERNAME_NOT_FOUND = "CWWKS5435E";
    protected final static String CWWKS5436E_REALM_NOT_FOUND = "CWWKS5436E";
    protected final static String CWWKS5437E_TWITTER_ERROR_CREATING_RESULT = "CWWKS5437E";
    protected final static String CWWKS5438E_ERROR_GETTING_ENCRYPTED_ACCESS_TOKEN = "CWWKS5438E";
    protected final static String CWWKS5442E_TWITTER_STATE_MISSING = "CWWKS5442E";
    protected final static String CWWKS5443E_TWITTER_ORIGINAL_REQUEST_URL_MISSING_OR_EMPTY = "CWWKS5443E";
    protected final static String CWWKS5447E_FAILED_TO_REDIRECT_TO_AUTHZ_ENDPOINT = "CWWKS5447E";
    protected final static String CWWKS5448E_STATE_IS_NULL = "CWWKS5448E";
    protected final static String CWWKS5449E_REDIRECT_URL_IS_NULL = "CWWKS5449E";
    protected final static String CWWKS5450E_AUTH_CODE_ERROR_SSL_CONTEXT = "CWWKS5450E";
    protected final static String CWWKS5451E_AUTH_CODE_ERROR_GETTING_TOKENS = "CWWKS5451E";
    protected final static String CWWKS5452E_USER_API_RESPONSE_NULL_OR_EMPTY = "CWWKS5452E";
    protected final static String CWWKS5453E_AUTH_CODE_FAILED_TO_CREATE_JWT = "CWWKS5453E";
    protected final static String CWWKS5454E_AUTH_CODE_ERROR_CREATING_RESULT = "CWWKS5454E";
    protected final static String CWWKS5455E_ACCESS_TOKEN_MISSING = "CWWKS5455E";
    protected final static String CWWKS5456E_USER_PROFILE_ACCESS_TOKEN_NULL = "CWWKS5456E";
    protected final static String CWWKS5457E_ACCESS_TOKEN_NOT_IN_CACHE = "CWWKS5457E";
    protected final static String CWWKS5458E_CONFIG_FOR_CACHED_TOKEN_NOT_FOUND = "CWWKS5458E";
    protected final static String CWWKS5459E_SOCIAL_LOGIN_RESULT_MISSING_ACCESS_TOKEN = "CWWKS5459E";
    protected final static String CWWKS5460W_NO_USER_API_CONFIGS_PRESENT = "CWWKS5460W";
    protected final static String CWWKS5462E_TOKEN_ENDPOINT_NULL_OR_EMPTY = "CWWKS5462E";
    protected final static String CWWKS5463E_FAILED_TO_GET_SSL_CONTEXT = "CWWKS5463E";
    protected final static String CWWKS5464E_SERVICE_NOT_FOUND_JWT_CONSUMER_NOT_AVAILABLE = "CWWKS5464E";
    protected final static String CWWKS5465E_INVALID_CONTEXT_PATH_CHARS = "CWWKS5465E";
    protected final static String CWWKS5466E_ERROR_LOADING_SSL_PROPS = "CWWKS5466E";
    protected final static String CWWKS5467E_KEYSTORE_SERVICE_NOT_FOUND = "CWWKS5467E";
    protected final static String CWWKS5468E_ERROR_LOADING_KEYSTORE_CERTIFICATES = "CWWKS5468E";
    protected final static String CWWKS5469E_ERROR_LOADING_CERTIFICATE = "CWWKS5469E";
    protected final static String CWWKS5470E_ERROR_LOADING_GETTING_PUBLIC_KEYS = "CWWKS5470E";
    protected final static String CWWKS5471E_ERROR_LOADING_SPECIFIC_PRIVATE_KEY = "CWWKS5471E";
    protected final static String CWWKS5472E_ERROR_LOADING_PRIVATE_KEY = "CWWKS5472E";
    protected final static String CWWKS5473E_ERROR_LOADING_SECRET_KEY = "CWWKS5473E";
    protected final static String CWWKS5475E_NULL_OR_EMPTY_REQUEST_URL = "CWWKS5475E";
    protected final static String CWWKS5476E_ERROR_EXECUTING_REQUEST = "CWWKS5476E";
    protected final static String CWWKS5477E_RESPONSE_STATUS_MISSING_OR_ERROR = "CWWKS5477E";
    protected final static String CWWKS5478E_RESPONSE_STATUS_UNSUCCESSFUL = "CWWKS5478E";
    protected final static String CWWKS5479E_CONFIG_REQUIRED_ATTRIBUTE_NULL = "CWWKS5479E";
    protected final static String CWWKS5480E_STATE_NULL_OR_MISMATCHED = "CWWKS5480E";
    protected final static String CWWKS5481E_REQUEST_URL_NULL_OR_EMPTY = "CWWKS5481E";
    protected final static String CWWKS5486W_POST_RESPONSE_NULL = "CWWKS5486W";
    protected final static String CWWKS5487W_ENDPOINT_RESPONSE_NOT_JSON = "CWWKS5487W";
    protected final static String CWWKS5488W_URI_CONTAINS_INVALID_CHARS = "CWWKS5488W";
    protected final static String CWWKS5490E_USERAPI_RESP_INVALID_STATUS = "CWWKS5490E";
    protected final static String CWWKS5491E_USERAPI_ERROR_RESPONSE = "CWWKS5491E";
    protected final static String CWWKS5492E_USERAPI_RESP_PROCESS_ERR = "CWWKS5492E";
    protected final static String CWWKS5493E_USERAPI_NULL_RESP_STR = "CWWKS5493E";
    protected final static String CWWKS5494E_CODE_PARAMETER_NULL_OR_EMPTY = "CWWKS5494E";
    protected final static String CWWKS5495E_REDIRECT_REQUEST_CONTAINED_ERROR = "CWWKS5495E";
    protected final static String CWWKS5496W_HTTP_URI_DOES_NOT_START_WITH_HTTP = "CWWKS5496W";
    protected final static String CWWKS5497E_FAILED_TO_CREATE_JWT_FROM_USER_API = "CWWKS5497E";
    protected final static String CWWKS5498E_FAILED_TO_CREATE_JWT_FROM_ID_TOKEN = "CWWKS5498E";
    protected final static String CWWKS5499E_REQUEST_URL_NOT_VALID = "CWWKS5499E";

    protected final static String CWWKS6102E_SUBJECT_MAPPING_MISSING_ATTR = "CWWKS6102E";
    protected final static String CWWKS6104W_CONFIG_REQUIRED_ATTRIBUTE_NULL = "CWWKS6104W";

    /****************************************** Helper methods ******************************************/

    protected void handleErrorExpectations() throws IOException {
        handleErrorExpectations(HttpServletResponse.SC_UNAUTHORIZED);
    }

    protected void handleErrorExpectations(final int statusCode) throws IOException {
        mockery.checking(new Expectations() {
            {
                one(response).isCommitted();
                will(returnValue(false));
                one(response).setStatus(statusCode);
                one(response).getWriter();
                will(returnValue(writer));
                allowing(writer).println(with(any(String.class)));
                one(writer).flush();
            }
        });
    }

    protected void savePostParameterHelperExpectations() {
        mockery.checking(new Expectations() {
            {
                one(request).getMethod();
                will(returnValue("GET"));
            }
        });
    }

    protected void getAndClearCookieExpectations(final Cookie cookie, final String cookieName, final String cookieValue, final boolean isSecure, final HttpServletResponse response) {
        mockery.checking(new Expectations() {
            {
                allowing(cookie).getName();
                will(returnValue(cookieName));
                allowing(cookie).getValue();
                will(returnValue(cookieValue));
                one(cookie).getPath();
                one(cookie).getSecure();
                will(returnValue(isSecure));
                one(response).addCookie(with(any(Cookie.class)));
            }
        });
    }

    protected void entityUtilsToStringExpectations(String response) throws IOException {
        final InputStream is = (response == null) ? null : new ByteArrayInputStream(response.getBytes());
        mockery.checking(new Expectations() {
            {
                allowing(httpEntity).getContent();
                will(returnValue(is));
                allowing(httpEntity).getContentLength();
                will(returnValue(1L));
                allowing(httpEntity).getContentType();
            }
        });
    }

    public void verifyPattern(String input, Pattern regexPattern) {
        Matcher m = regexPattern.matcher(input);
        assertTrue("Input did not match expected expression. Expected: [" + regexPattern.toString() + "]. Value was: [" + input + "]", m.find());
    }

    public void verifyNoLogMessage(SharedOutputManager outputMgr, String messageRegex) {
        assertFalse("Found message [" + messageRegex + "] in log but should not have.", outputMgr.checkForMessages(messageRegex));
    }

    public void verifyLogMessage(SharedOutputManager outputMgr, String messageRegex) {
        assertTrue("Did not find message [" + messageRegex + "] in log.", outputMgr.checkForMessages(messageRegex));
    }

    public void verifyExceptionWithInserts(Exception e, String msgKey, String... inserts) {
        String errorMsg = e.getLocalizedMessage();
        verifyStringWithInserts(errorMsg, msgKey, inserts);
    }

    public void verifyStringWithInserts(String searchString, String msgKey, String... inserts) {
        String fullPattern = buildStringWithInserts(msgKey, inserts).toString();
        Pattern pattern = Pattern.compile(fullPattern);
        Matcher m = pattern.matcher(searchString);
        assertTrue("Provided string did not contain [" + fullPattern + "] as expected. Full string was: [" + searchString + "]", m.find());
    }

    protected StringBuilder buildStringWithInserts(String msgKey, String... inserts) {
        // Expects inserts to be in square brackets '[]'
        StringBuilder regexBuilder = new StringBuilder(msgKey).append(".*");
        for (String insert : inserts) {
            regexBuilder.append("\\[" + insert + "\\]").append(".*");
        }
        return regexBuilder;
    }

}
