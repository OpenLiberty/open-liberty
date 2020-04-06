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
 * Represents an OAuth service provider internal exception while processing OAuth request.
 */
public class OAuth20InternalException extends OAuth20Exception {

    private static final TraceComponent tc = Tr.register(OAuth20InternalException.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final long serialVersionUID = 1L;
    String _clientId;
    private String[] unencodedTraceArguments;

    /**
     * Creates a OAuth20InternalException.
     * 
     * @param cause A root exception.
     */
    public OAuth20InternalException(Throwable cause) {
        /*
         * SERVER_ERROR is not valid for token and resource endpoints but it
         * best describes this exception. Component consumers can always handle
         * this exception differently for each endpoint in order to be spec
         * compliant.
         */
        super(SERVER_ERROR,
                "In internal error has occurred processing an OAuth 2.0 request.",
                cause);
    }

    // Liberty
    public OAuth20InternalException(String msgKey, Throwable cause, String... unencodedTraceArguments) {
        super(SERVER_ERROR, Tr.formatMessage(tc, msgKey, (Object[]) unencodedTraceArguments), cause);
        _msgKey = msgKey;
        this.unencodedTraceArguments = unencodedTraceArguments;
    }

    // Liberty
    // public OAuth20InternalException(String msgKey, Throwable cause) {
    // super(SERVER_ERROR, Tr.formatMessage(tc, msgKey), cause);
    // _msgKey = msgKey;
    // this.unencodedTraceArguments = new String[] {};
    // }

    // Liberty
    @Override
    public String formatSelf(Locale locale, String encoding) {
        String message = OAuth20ExceptionUtil.getResourceBundle(locale).getString(_msgKey);
        if (this.unencodedTraceArguments != null) {
            Object[] encodedTraceArguments = getEncodedTraceArguments(locale, encoding);
            return MessageFormat.format(message, encodedTraceArguments);
        }
        else {
            return MessageFormat.format(message, new Object[] {});
        }

    }

    // Liberty
    private Object[] getEncodedTraceArguments(Locale locale, String encoding) {
        Object[] encodedTraceArguments = new Object[unencodedTraceArguments.length];
        for (int i = 0; i < unencodedTraceArguments.length; i++) {
            encodedTraceArguments[i] = WebUtils.encode(unencodedTraceArguments[i], locale, encoding);
        }
        return encodedTraceArguments;
    }

}
