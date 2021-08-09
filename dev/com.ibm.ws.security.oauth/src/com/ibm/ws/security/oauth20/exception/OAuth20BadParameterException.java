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
package com.ibm.ws.security.oauth20.exception;

import java.util.Locale;

import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.oauth.core.api.error.oauth20.TraceConstants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Represents a bad parameter format exception presented in OAuth request.
 */
public class OAuth20BadParameterException extends OAuth20Exception {

    private static final TraceComponent tc = Tr.register(OAuth20BadParameterException.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final long serialVersionUID = 1L;
    private final String _paramName;
    private final String _paramValue;
    private Object[] _params = null;

    // Liberty
    // for SECURITY.OAUTH20.ERROR.VALUE.NOT.IN.LIST at the writing time
    // TODO deal with OAuth20ExceptionUtil
    public OAuth20BadParameterException(String msgKey, Object[] params) {
        super(INVALID_REQUEST, Tr.formatMessage(tc, msgKey, params), null);
        _objs = params;
        _msgKey = msgKey;
        _paramName = (String) params[0];
        _paramValue = (String) params[1];
        this._params = params.clone();
    }

    // Liberty
    @Override
    public String formatSelf(Locale locale, String encoding) {
        return Tr.formatMessage(tc, _msgKey, _params);
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
