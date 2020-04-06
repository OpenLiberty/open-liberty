/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.error;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.ws.security.oauth20.util.UtilConstants;

public interface OAuthExceptionHandler {

    public static final String WWW_AUTHENTICATE = UtilConstants.WWW_AUTHENTICATE;

    public static final String CACHE_CONTROL = "Cache-Control";

    public static final String CACHE_CONTROL_VALUE = "no-store";

    public static final String PRAGMA = "Pragma";

    public static final String PRAGMA_VALUE = "no-cache";

    public static final String CONTENT_TYPE = "Content-Type";

    public static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";

    public static final String X_FRAME_OPTIONS = "X-Frame-Options";

    public static final String SAMEORIGIN = "SAMEORIGIN";

    public static final String LOCATION = "Location";

    public static final String ERROR = "error";

    public static final String ERROR_DESCRIPTION = "error_description";

    public static final String ERROR_URI = "error_uri";

    public static final String SCOPE = "scope";

    /**
     * Handles the processing of an OAuthResult exception
     *
     * @param req
     *            The HTTP request
     * @param rsp
     *            The HTTP response
     * @param result
     *            The OAuthResult returned from a call to the OAuth component
     */
    public void handleResultException(HttpServletRequest req,
            HttpServletResponse rsp, OAuthResult result);

}
