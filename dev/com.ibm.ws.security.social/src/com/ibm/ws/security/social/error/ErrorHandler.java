/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2016
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.security.social.error;

import javax.servlet.http.HttpServletResponse;

import com.ibm.wsspi.security.tai.TAIResult;

/**
 *
 */
public interface ErrorHandler {

    /**
     * See an implement in ErrorHandlerImpl
     */

    /**
     * Handles setting the response error status and displaying an error message for an end user in the browser.
     *
     * @param response
     */
    public void handleErrorResponse(HttpServletResponse response);

    /**
     * Handles setting the response error status and displaying an error message for an end user in the browser.
     *
     * @param response
     * @param result
     * @return The same {@code TAIResult} object provided to the method.
     */
    public TAIResult handleErrorResponse(HttpServletResponse response, TAIResult result);

    /**
     * Handles setting the response error status and displaying an error message for an end user in the browser.
     *
     * @param response
     * @param httpStatusCode
     */
    public void handleErrorResponse(HttpServletResponse response, int httpStatusCode);

}
