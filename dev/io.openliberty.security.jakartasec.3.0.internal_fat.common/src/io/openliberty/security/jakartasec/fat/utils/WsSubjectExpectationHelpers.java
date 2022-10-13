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

import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;

public class WsSubjectExpectationHelpers {

    public static void getWsSubjectExpectations(String action, Expectations expectations, String requester, ResponseValues rspValues) throws Exception {

        String updatedRequester = requester + ServletMessageConstants.WSSUBJECT;
        // remove check for BASIC once 22940 is resolved - we should always get JAKARTA_OIDC from both the callback and test servlet.
        if (requester.equals(ServletMessageConstants.SERVLET)) {
            expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, updatedRequester + ServletMessageConstants.GET_AUTH_TYPE
                                                                                                       + ServletMessageConstants.BASIC, "Did not find the correct auth type in the WSSubject."));
        } else {
            expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, updatedRequester + ServletMessageConstants.GET_AUTH_TYPE
                                                                                                       + ServletMessageConstants.JAKARTA_OIDC, "Did not find the correct auth type in the WSSubject."));
        }

        getWSSubjectExpectations(action, expectations, updatedRequester, rspValues);

    }

    public static void getWSSubjectExpectations(String action, Expectations expectations, String requester, ResponseValues rspValues) throws Exception {

        expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.GET_USER_PRINCIPAL_GET_NAME
                                                                                                   + rspValues.getSubject(), "Did not find the correct principal in the WSSubject."));

    }

}
