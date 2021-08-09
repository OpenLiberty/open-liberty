/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.error;

import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.mp.jwt.TraceConstants;
import com.ibm.wsspi.security.tai.TAIResult;

/**
 *
 */
public class ErrorHandlerImpl implements ErrorHandler {

    private static final TraceComponent tc = Tr.register(ErrorHandlerImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final String AUTH_HEADER = "WWW-Authenticate";
    private static final String REALM = "MP-JWT";
    private static final String ERROR_CODE = "error=\"invalid_token\"";
    private static final String BEARER = "Bearer realm=\"" + REALM + "\"";

    public static ErrorHandler getInstance() {
        return new ErrorHandlerImpl();
    }

    public ErrorHandlerImpl() {
    }

    @Override
    public TAIResult handleErrorResponse(HttpServletResponse response, TAIResult result) {
        handleErrorResponse(response, result.getStatus());
        return result;
    }

    @Override
    public void handleErrorResponse(HttpServletResponse response, int httpErrorCode) {
        if (!response.isCommitted()) {
            response.setStatus(httpErrorCode);
        }
        String errorMessage = getErrorMessage();
        response.setHeader(AUTH_HEADER, errorMessage);
    }

    String getErrorMessage() {
        String message = getRealmMessage();
        message += ", " + ERROR_CODE;
        return message;
    }

    String getRealmMessage() {
        return BEARER;
    }

}
