/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.expectations;

import java.util.List;

import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;

public class ResponseHeaderExpectation extends Expectation {

    protected static Class<?> thisClass = ResponseHeaderExpectation.class;

    public static final String HEADER_DELIMITER = "|";

    public ResponseHeaderExpectation(String checkType, String searchFor, String failureMsg) {
        this(null, checkType, searchFor, failureMsg);
    }

    public ResponseHeaderExpectation(String testAction, String checkType, String searchFor, String failureMsg) {
        super(testAction, Constants.RESPONSE_HEADER, checkType, searchFor, failureMsg);
    }

    public ResponseHeaderExpectation(String testAction, String checkType, String headerName, String headerValue, String failureMsg) {
        super(testAction, Constants.RESPONSE_HEADER, checkType, headerName, headerValue, failureMsg);
    }

    @Override
    protected void validate(Object contentToValidate) throws Exception {
        try {
            String responseHeaders = getResponseHeaderString(contentToValidate);
            validationUtils.validateStringContent(this, responseHeaders);
        } catch (Exception e) {
            throw new Exception(failureMsg + " Failed to validate response headers: " + e.getMessage());
        }
    }

    String getResponseHeaderString(Object contentToValidate) throws Exception {
        if (contentToValidate instanceof String) {
            return (String) contentToValidate;
        }
        if (validationKey != null) {
            return getSpecificHeaderValue(contentToValidate);
        }
        return getAllHeaderValues(contentToValidate);
    }

    String getSpecificHeaderValue(Object contentToValidate) throws Exception {
        return WebResponseUtils.getResponseHeaderField(contentToValidate, validationKey);
    }

    String getAllHeaderValues(Object contentToValidate) throws Exception {
        List<NameValuePair> headerList = WebResponseUtils.getResponseHeaderList(contentToValidate);
        return buildHeadersString(headerList);
    }

    String buildHeadersString(List<NameValuePair> headers) {
        if (headers == null) {
            Log.info(thisClass, "getResponseHeadersString", "No headers found in response");
            return null;
        }
        String headersString = "";
        for (NameValuePair header : headers) {
            if (!headersString.isEmpty()) {
                headersString += " " + HEADER_DELIMITER + " ";
            }
            headersString += header.getName() + ": " + header.getValue();
        }
        return headersString;
    }

}
