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
 * Represents an invalid response type exception in an OAuth request.
 */
public class OAuth20InvalidResponseTypeException extends OAuth20Exception {

    private static final TraceComponent tc = Tr.register(OAuth20InvalidResponseTypeException.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final long serialVersionUID = 1L;
    private String _responseType;
    private String _firstResponseType;
    private String _secondResponseType;

    /**
     * Creates a OAuth20InvalidResponseTypeException.
     * 
     * @param responseType A response type in a request.
     */
    public OAuth20InvalidResponseTypeException(String responseType) {
        super(UNSUPPORTED_RESPONSE_TPE, "The response_type parameter was invalid: " + responseType, null);
        _responseType = responseType;
    }

    // Liberty
    public OAuth20InvalidResponseTypeException(String msgKey, String responseType) {
        this(msgKey, responseType, null, null);
    }

    // Liberty
    public OAuth20InvalidResponseTypeException(String msgKey, String responseType, String firstResponseTypeOption, String secondResponseTypeOption) {
        super(UNSUPPORTED_RESPONSE_TPE, Tr.formatMessage(tc, msgKey, new Object[] { responseType, firstResponseTypeOption, secondResponseTypeOption }), null);
        _msgKey = msgKey;
        _responseType = responseType;
        _firstResponseType = firstResponseTypeOption;
        _secondResponseType = secondResponseTypeOption;
    }

    // Liberty
    @Override
    public String formatSelf(Locale locale, String encoding) {
        if (_firstResponseType == null || _secondResponseType == null) {
            return MessageFormat.format(OAuth20ExceptionUtil.getResourceBundle(locale).getString(_msgKey),
                    new Object[] { WebUtils.encode(_responseType, locale, encoding) });
        } else {
            return MessageFormat.format(OAuth20ExceptionUtil.getResourceBundle(locale).getString(_msgKey),
                    new Object[] { WebUtils.encode(_responseType, locale, encoding),
                            WebUtils.encode(_firstResponseType, locale, encoding),
                            WebUtils.encode(_secondResponseType, locale, encoding) });
        }
    }

    /**
     * @return response type.
     */
    public String getResponseType() {
        return _responseType;
    }

}
