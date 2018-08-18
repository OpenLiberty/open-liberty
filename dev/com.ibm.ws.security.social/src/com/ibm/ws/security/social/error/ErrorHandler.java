/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
