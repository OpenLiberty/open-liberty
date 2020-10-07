/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.api.error.oauth20;

import java.util.Locale;

import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Represents an authorization exception to access protected resources.
 */
public class OAuth20AccessDeniedException extends OAuth20Exception {

    private static final TraceComponent tc = Tr.register(OAuth20AccessDeniedException.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final long serialVersionUID = 1L;
    private int httpStatusCode = HttpServletResponse.SC_MOVED_TEMPORARILY;

    /**
     * Creates a OAuth20AccessDeniedException.
     */
    public OAuth20AccessDeniedException() {
        super(ACCESS_DENIED, "You are not authorized to access this protected resource.", null);
    }

    // Liberty
    public OAuth20AccessDeniedException(String msgKey) {
        super(ACCESS_DENIED, Tr.formatMessage(tc, msgKey, new Object[] {}), null);
        _msgKey = msgKey;
    }

    // Liberty
    @Override
    public String formatSelf(Locale locale, String encoding) {
        return Tr.formatMessage(tc, OAuth20ExceptionUtil.getResourceBundle(locale).getString(_msgKey));
    }

    public void setHttpStatusCode(int iHttpStatusCode) {
        httpStatusCode = iHttpStatusCode;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }
}
