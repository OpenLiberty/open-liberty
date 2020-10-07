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

import java.text.MessageFormat;
import java.util.Locale;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.web.WebUtils;

/**
 * Represents an invalid or not supported grant type in an OAuth request.
 */
public class OAuth20InvalidGrantTypeException extends OAuth20Exception {

    private static final TraceComponent tc = Tr.register(OAuth20InvalidGrantTypeException.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final long serialVersionUID = 1L;
    private String _grantType;

    /**
     * Creates a OAuth20InvalidGrantTypeException.
     * 
     * @param grantType A grant type in request.
     */
    public OAuth20InvalidGrantTypeException(String grantType) {
        super(UNSUPPORED_GRANT_TPE, "The grant_type parameter was invalid: " + grantType, null);
        _grantType = grantType;
    }

    // Liberty
    public OAuth20InvalidGrantTypeException(String msgKey, String grantType) {
        super(UNSUPPORED_GRANT_TPE, Tr.formatMessage(tc, msgKey, grantType), null);
        _msgKey = msgKey;
        _grantType = grantType;
    }

    // Liberty
    @Override
    public String formatSelf(Locale locale, String encoding) {
        return MessageFormat.format(OAuth20ExceptionUtil.getResourceBundle(locale).getString(_msgKey),
                new Object[] { WebUtils.encode(_grantType, locale, encoding) });
    }

    /**
     * @return the requested grant type.
     */
    public String getGrantType() {
        return _grantType;
    }

}
