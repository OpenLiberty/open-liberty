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

import com.gargoylesoftware.htmlunit.util.Cookie;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;

public class WsSubjectExpectationHelpers {

    public static void getWsSubjectExpectations(String action, Expectations expectations, String requester, ResponseValues rspValues) throws Exception {

        String updatedRequester = requester + ServletMessageConstants.WSSUBJECT;
        // remove check for BASIC once 22940 is resolved - we should always get JAKARTA_OIDC from both the callback and test servlet.
        // TODO this should always return JAKARTA_OIDC - fix when issue is resolved  TODO
//        if (requester.equals(ServletMessageConstants.SERVLET) && (!RepeatTestFilter.getRepeatActionsAsString().contains("useRedirectToOriginalResource"))) {
//            expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, updatedRequester + ServletMessageConstants.GET_AUTH_TYPE
//                                                                                                       + ServletMessageConstants.BASIC, "Did not find the correct auth type in the WSSubject."));
//        } else {
//            expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, updatedRequester + ServletMessageConstants.GET_AUTH_TYPE
//                                                                                                       + ServletMessageConstants.JAKARTA_OIDC, "Did not find the correct auth type in the WSSubject."));
//        }

        getWSSubjectSubjectExpectations(action, expectations, updatedRequester, rspValues);
        getWSSubjectCookieExpectations(action, expectations, updatedRequester, rspValues);

    }

    public static void getWSSubjectSubjectExpectations(String action, Expectations expectations, String requester, ResponseValues rspValues) throws Exception {

        if (rspValues.getSubject() != null) {
            expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.GET_USER_PRINCIPAL_GET_NAME
                                                                                                       + rspValues.getSubject(), "Did not find the correct principal in the WSSubject."));
        } else {
            expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_DOES_NOT_CONTAIN, requester
                                                                                                               + ServletMessageConstants.GET_USER_PRINCIPAL_GET_NAME, "Found a principal name in the WSSubject but should not have."));
        }
    }

    public static void getWSSubjectCookieExpectations(String action, Expectations expectations, String requester, ResponseValues rspValues) throws Exception {

        // get extra cookies that test may have added
        List<Cookie> cookies = rspValues.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.COOKIE + c.getName()
                                                                                                           + " " + ServletMessageConstants.VALUE
                                                                                                           + c.getValue(), "Did not find the " + c.getName()
                                                                                                                           + " cookie in the  WSSubject."));
            }

        }

        // Make sure that some standard cookies exist (can't check the value, but just make sure they exist)
        if (rspValues.getUseSession()) {
            if (!rspValues.getUseAuthApp()) {
                expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.COOKIE + "clientJSESSIONID "
                                                                                                           + ServletMessageConstants.VALUE, "Did not find the clientJSESSIONID cookie in the  WSSubject."));
            }

            expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.COOKIE + "OPJSESSIONID "
                                                                                                       + ServletMessageConstants.VALUE, "Did not find the OPJSESSIONID cookie in the  WSSubject."));
        } else {
            expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.COOKIE + "JSESSIONID "
                                                                                                       + ServletMessageConstants.VALUE, "Did not find the JSESSIONID cookie in the  WSSubject."));
        }
        expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.COOKIE
                                                                                                   + "WAS_", "Did not find the WAS_* cookie in the  WSSubject."));

    }

}
