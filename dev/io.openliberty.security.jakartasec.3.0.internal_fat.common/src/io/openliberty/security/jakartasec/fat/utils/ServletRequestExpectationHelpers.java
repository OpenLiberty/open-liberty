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

import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;

public class ServletRequestExpectationHelpers {

    protected static Class<?> thisClass = ServletRequestExpectationHelpers.class;

    public static void getServletRequestExpectations(String action, Expectations expectations, String requester, Map<String, String> headers,
                                                     List<NameValuePair> parms) throws Exception {

        String updatedRequester = requester + ServletMessageConstants.REQUEST;

//        getRequestHeaderExpectations(action, expectations, updatedRequester, headers);
        getRequestParmsExpectations(action, expectations, updatedRequester, parms);

    }

    public static void getRequestHeaderExpectations(String action, Expectations expectations, String requester, Map<String, String> headers) throws Exception {

        if (headers != null) {
            Log.info(thisClass, "getRequestHeaderExpectations", "Setting extra header expectations");
            for (Map.Entry<String, String> header : headers.entrySet()) {
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
                expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.PARMS + ServletMessageConstants.NAME
                                                                                                           + parm.getName() + " " + ServletMessageConstants.VALUE
                                                                                                           + parm.getValue(), "Did not find the expected parm name "
                                                                                                                              + parm.getName() + " with the expected value "
                                                                                                                              + parm.getValue()));
            }
        }
    }

}
