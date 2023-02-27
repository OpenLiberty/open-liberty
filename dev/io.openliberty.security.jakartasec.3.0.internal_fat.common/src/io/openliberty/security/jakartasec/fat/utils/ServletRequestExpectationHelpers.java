/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;

import componenttest.custom.junit.runner.RepeatTestFilter;

public class ServletRequestExpectationHelpers {

    protected static Class<?> thisClass = ServletRequestExpectationHelpers.class;

    public static void getServletRequestExpectations(String action, Expectations expectations, String requester, ResponseValues rspValues) throws Exception {

        String updatedRequester = requester + ServletMessageConstants.REQUEST;

        if (RepeatTestFilter.getRepeatActionsAsString().contains("useRedirectToOriginalResource")) {
            getRequestHeaderExpectations(action, expectations, updatedRequester, rspValues.getHeaders());
        }
        getRequestParmsExpectations(action, expectations, updatedRequester, rspValues);
        getRequestCookieExpectations(action, expectations, updatedRequester, rspValues.getCookies());

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

    public static void getRequestParmsExpectations(String action, Expectations expectations, String requester, ResponseValues rspValues) throws Exception {

        Log.info(thisClass, "getRequestParmsExpectations", "Setting extra parm expectations");
        if (rspValues.getParms() != null) {
            for (NameValuePair parm : rspValues.getParms()) {
                Log.info(thisClass, "getRequestParmsExpectations", "Adding expectation for " + parm.getName());
                if (rspValues.getUseAuthApp()) {
                    expectations.addExpectation(new ServerMessageExpectation(rspValues.getRPServer(), Constants.STRING_CONTAINS, requester + ServletMessageConstants.PARMS
                                                                                                                                 + ServletMessageConstants.NAME
                                                                                                                                 + parm.getName() + " "
                                                                                                                                 + ServletMessageConstants.VALUE
                                                                                                                                 + parm.getValue(), "Did not find the expected parm name "
                                                                                                                                                    + parm.getName()
                                                                                                                                                    + " with the expected value "
                                                                                                                                                    + parm.getValue()));
                } else {
                    expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.PARMS
                                                                                                               + ServletMessageConstants.NAME
                                                                                                           + parm.getName() + " " + ServletMessageConstants.VALUE
                                                                                                           + parm.getValue(), "Did not find the expected parm name "
                                                                                                                              + parm.getName() + " with the expected value "
                                                                                                                              + parm.getValue()));
                }
            }
        }
    }

    public static void getRequestCookieExpectations(String action, Expectations expectations, String requester, List<Cookie> cookies) throws Exception {

        Log.info(thisClass, "getRequestParmsExpectations", "Setting extra cookie expectations");
        if (cookies != null && cookies.size() != 0) {
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
