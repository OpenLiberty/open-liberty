/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.error;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.sso20.internal.utils.RequestUtil;

/**
 *
 */
public class ErrorHandlerImpl implements ErrorHandler {
    @SuppressWarnings("unused")
    private static final TraceComponent tc = Tr.register(ErrorHandlerImpl.class,
                                                         TraceConstants.TRACE_GROUP,
                                                         TraceConstants.MESSAGE_BUNDLE);
    protected final static ErrorHandler instance = new ErrorHandlerImpl();

    public static ErrorHandler getInstance() {
        return instance;
    }

    /**
     * This handle the error response to show the messages and error status
     * in the browser of end-user
     * 
     * @param request
     * @param response
     * @param e
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void handleException(HttpServletRequest request,
                                HttpServletResponse response,
                                SamlException e) throws ServletException, IOException {
        // Handle the error in some special case
        ErrorHandler errorHandler = e.getErrorHanlder();
        if (errorHandler != null) {
            // Allow specific error handling. 
            // In case, it need some specific error handling, such as: json  
            errorHandler.handleException(request, response, e);
            return;
        }

        if (!e.ffdcAlready()) { // handle the FFDC earlier because, sometimes, it shows on the file system too late.
            com.ibm.ws.ffdc.FFDCFilter.processException(e,
                                                        "com.ibm.ws.security.saml20.sso20.util.ErrorHandlerImpl",
                                                        "78758",
                                                        this);
            e.setFfdcAlready(true);
        }

        int httpErrorCode = e.getHttpErrorCode();
        if (httpErrorCode == HttpServletResponse.SC_OK) {
            httpErrorCode = HttpServletResponse.SC_FORBIDDEN;
        }
        response.setStatus(httpErrorCode);

        Throwable cause = e.getCause();
        if (cause instanceof ServletException)
            throw (ServletException) cause;
        if (cause instanceof IOException)
            throw (IOException) cause;
        handleErrorResponse(request, response);
    }

    public void handleErrorResponse(HttpServletRequest request,
                                    HttpServletResponse response) throws IOException {
        String errorPageUrl = getErrorPageUrl(request);

        if (errorPageUrl == null) {
            SsoRequest samlRequest = (SsoRequest) request.getAttribute(Constants.ATTRIBUTE_SAML20_REQUEST);
            SsoConfig samlConfig = samlRequest == null ? null : samlRequest.getSsoConfig();
            errorPageUrl = RequestUtil.getCtxRootUrl(request, Constants.SAML20_CONTEXT_PATH, samlConfig)
                           + Constants.DEFAULT_ERROR_MSG_JSP;
        }
        response.sendRedirect(errorPageUrl);
    }

    /**
     * @param request
     * @return
     */
    private String getErrorPageUrl(HttpServletRequest request) {
        String result = null;
        SsoRequest samlRequest = (SsoRequest) request.getAttribute(Constants.ATTRIBUTE_SAML20_REQUEST);
        if (samlRequest != null) {
            SsoSamlService samlService = samlRequest.getSsoSamlService();
            if (samlService != null) {
                SsoConfig samlConfig = samlService.getConfig();
                if (samlConfig != null) {
                    String errorPageUrl = samlConfig.getErrorPageURL();
                    result = errorPageUrl == null ? null :
                                    (errorPageUrl.isEmpty() ? null : errorPageUrl);
                }
            }
        }
        return result;
    }
}
