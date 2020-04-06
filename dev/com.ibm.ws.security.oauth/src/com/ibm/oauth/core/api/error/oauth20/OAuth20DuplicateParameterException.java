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
 * Represents a duplicated parameter exception in OAuth request.
 */
public class OAuth20DuplicateParameterException extends OAuth20Exception {

    private static final TraceComponent tc = Tr.register(OAuth20DuplicateParameterException.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final long serialVersionUID = 1L;
    private String _paramName;

    /**
     * Creates a OAuth20DuplicateParameterException.
     * 
     * @param paramName a parameter in an OAuth request.
     */
    public OAuth20DuplicateParameterException(String paramName) {
        super(INVALID_REQUEST, "The following OAuth parameter was provided more than once in the request: "
                + paramName, null);
        _paramName = paramName;
    }

    // Liberty
    public OAuth20DuplicateParameterException(String msgKey, String paramName) {
        super(INVALID_REQUEST, Tr.formatMessage(tc, msgKey, new Object[] { paramName }), null);
        _msgKey = msgKey;
        _paramName = paramName;
    }

    // Liberty
    @Override
    public String formatSelf(Locale locale, String encoding) {
        return MessageFormat.format(OAuth20ExceptionUtil.getResourceBundle(locale).getString(_msgKey),
                new Object[] { WebUtils.encode(_paramName, locale, encoding) });
    }

    /**
     * @return the duplicated parameter name in the request.
     */
    public String getParamName() {
        return _paramName;
    }

}
