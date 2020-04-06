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

/**
 * Represents a missing request parameter exception.
 */
public class OAuth20MissingParameterException extends OAuth20Exception {

    private static final TraceComponent tc = Tr.register(OAuth20MissingParameterException.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final long serialVersionUID = 1L;
    private String _param;

    /**
     * Create OAuth20MissingParameterException.
     * 
     * @param param the missed required parameter name.
     * @param cause the root cause.
     */
    public OAuth20MissingParameterException(String param, Throwable cause) {
        super(INVALID_REQUEST, "A required runtime parameter was missing: " + param, cause);
        _param = param;
    }

    // Liberty
    public OAuth20MissingParameterException(String msgKey, String param, Throwable cause) {
        super(INVALID_REQUEST, Tr.formatMessage(tc, msgKey, new Object[] { param, cause }), null);
        _msgKey = msgKey;
        _param = param;
    }

    // Liberty
    @Override
    public String formatSelf(Locale locale, String encoding) {
        return MessageFormat.format(OAuth20ExceptionUtil.getResourceBundle(locale).getString(_msgKey),
                new Object[] { _param });
    }

    /**
     * @return the required parameter name.
     */
    public String getParam() {
        return _param;
    }

}
