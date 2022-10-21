/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.utils;

import java.util.List;
import java.util.Map;

import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;

import componenttest.custom.junit.runner.RepeatTestFilter;

public class ServletRequestExpectationHelpers {

    protected static Class<?> thisClass = ServletRequestExpectationHelpers.class;

    public static void getServletRequestExpectations(String action, Expectations expectations, String requester, Map<String, String> headers,
                                                     List<NameValuePair> parms, Cookie... cookies) throws Exception {

        String updatedRequester = requester + ServletMessageConstants.REQUEST;

        if (RepeatTestFilter.getRepeatActionsAsString().contains("useRedirectToOriginalResource")) {
            getRequestHeaderExpectations(action, expectations, updatedRequester, headers);
        }
        getRequestParmsExpectations(action, expectations, updatedRequester, parms);
        getRequestCookieExpectations(action, expectations, updatedRequester, cookies);

    }

    public static void getRequestHeaderExpectations(String action, Expectations expectations, String requester, Map<String, String> headers) throws Exception {

        if (headers != null) {
            Log.info(thisClass, "getRequestHeaderExpectations", "Setting extra header expectations");
            for (Map.Entry<String, String> header : headers.entrySet()) {
                Log.info(thisClass, "getRequestHeaderExpectations", "Adding expectation for " + header.getKey());
                expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.HEADER + ServletMessageConstants.NAME
                                                                                                           + header.getKey() + " " + ServletMessageConstants.VALUE
                                                                                                           + header.getValue(), "Did not find the expected header key "
                                                                                                                                + header.getKey() + " with the expected value "
                                                                                                                                + header.getValue()));
            }
        }
    }

    public static void getRequestParmsExpectations(String action, Expectations expectations, String requester, List<NameValuePair> parms) throws Exception {

        Log.info(thisClass, "getRequestParmsExpectations", "Setting extra parm expectations");
        if (parms != null) {
            for (NameValuePair parm : parms) {
                Log.info(thisClass, "getRequestParmsExpectations", "Adding expectation for " + parm.getName());
                expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.PARMS + ServletMessageConstants.NAME
                                                                                                           + parm.getName() + " " + ServletMessageConstants.VALUE
                                                                                                           + parm.getValue(), "Did not find the expected parm name "
                                                                                                                              + parm.getName() + " with the expected value "
                                                                                                                              + parm.getValue()));
            }
        }
    }

    public static void getRequestCookieExpectations(String action, Expectations expectations, String requester, Cookie... cookies) throws Exception {

        Log.info(thisClass, "getRequestParmsExpectations", "Setting extra cookie expectations");
        if (cookies != null && cookies.length != 0) {
            for (Cookie cookie : cookies) {
                Log.info(thisClass, "getRequestCookieExpectations", "Adding expectation for " + cookie.getName());
                expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_MATCHES, requester + ServletMessageConstants.HEADER + ServletMessageConstants.NAME
                                                                                                          + "Cookie " + ServletMessageConstants.VALUE + ".*"
                                                                                                          + cookie.getName() + "="
                                                                                                          + cookie.getValue(), "Did not find the expected cookie name "
                                                                                                                               + cookie.getName()
                                                                                                                               + " with the expected value " + cookie.getValue()));

                expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.COOKIE + ServletMessageConstants.NAME
                                                                                                           + cookie.getName() + " " + ServletMessageConstants.VALUE
                                                                                                           + cookie.getValue(), "Did not find the expected cookie name "
                                                                                                                                + cookie.getName() + " with the expected value "
                                                                                                                                + cookie.getValue()));
            }
        }
    }
}
