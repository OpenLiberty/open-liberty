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
 * Represents a bad parameter format exception presented in OAuth request.
 */
public class OAuth20BadParameterFormatException extends OAuth20Exception {

    private static final TraceComponent tc = Tr.register(OAuth20BadParameterFormatException.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final long serialVersionUID = 1L;
    private String _paramName;
    private String _paramValue;

    /**
     * Creates a OAuth20BadParameterFormatException.
     * @param paramName a parameter in an OAuth request.
     * @param paramValue a value of parameter in Oauth request.
     */
    public OAuth20BadParameterFormatException(String paramName, String paramValue) {
        super(INVALID_REQUEST, "The parameter: ["
                + paramName
                + "] contained an illegally formatted value: ["
                + paramValue + "].", null);
        _paramName = paramName;
        _paramValue = paramValue;
    }

    // Liberty
    public OAuth20BadParameterFormatException(String msgKey, String paramName, String paramValue) {
        super(INVALID_REQUEST, Tr.formatMessage(tc, msgKey, new Object[] { paramName, paramValue }), null);
        _msgKey = msgKey;
        _paramName = paramName;
        _paramValue = paramValue;
    }

    // Liberty
    @Override
    public String formatSelf(Locale locale, String encoding) {
        return MessageFormat.format(OAuth20ExceptionUtil.getResourceBundle(locale).getString(_msgKey),
                new Object[] { _paramName, WebUtils.encode(_paramValue, locale, encoding) });
    }

    /**
     * @return a parameter name in the request.
     */
    public String getParamName() {
        return _paramName;
    }

    /**
     * @return a parameter value in the request.
     */
    public String getParamValue() {
        return _paramValue;
    }

}
