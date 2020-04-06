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
 * Represents an invalid request scope exception in an OAuth request.
 */
public class OAuth20InvalidScopeException extends OAuth20Exception {

    private static final TraceComponent tc = Tr.register(OAuth20InvalidScopeException.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final long serialVersionUID = 1L;
    private String[] _requestedScope;
    private String[] _approvedScope;
    private String _reqType;
    private String _clientId;

    /**
     * Creates a OAuth20InvalidScopeException.
     * @param requestedScope An array of requested scopes in an OAuth request.
     * @param approvedScope An array of authorized scopes for an OAuth request.
     */
    public OAuth20InvalidScopeException(String[] requestedScope, String[] approvedScope) {
        super(INVALID_SCOPE, "The requested scope: ["
                + arrayToString(requestedScope)
                + "] exceeds the scope granted by the resource owner: ["
                + arrayToString(approvedScope) + "].", null);
        _requestedScope = requestedScope;
        _approvedScope = approvedScope;
    }

    // Liberty
    public OAuth20InvalidScopeException(String msgKey, String[] requestedScope, String[] approvedScope) {
        super(INVALID_SCOPE, Tr.formatMessage(tc, msgKey, new Object[] { arrayToString(requestedScope),
                arrayToString(approvedScope) }), null);
        _msgKey = msgKey;
        _requestedScope = requestedScope;
        _approvedScope = approvedScope;
    }

    public OAuth20InvalidScopeException(String msgKey, String[] requestedScope, String[] approvedScope, String clientId) {
        super(INVALID_SCOPE, Tr.formatMessage(tc, msgKey, new Object[] { arrayToString(requestedScope),
                arrayToString(approvedScope),
                clientId }), null);
        _msgKey = msgKey;
        _requestedScope = requestedScope;
        _approvedScope = approvedScope;
        _clientId = clientId;
    }

    /**
     * @param msgKey
     * @param requestType
     */
    public OAuth20InvalidScopeException(String msgKey, String requestType) {
        super(INVALID_SCOPE, Tr.formatMessage(tc, msgKey, new Object[] { requestType }), null);
        _msgKey = msgKey;
        _reqType = requestType;
    }

    /**
     * @param msgKey
     * @param requestType
     * @param clientId
     */
    public OAuth20InvalidScopeException(String msgKey, String requestType, String clientId) {
        super(INVALID_SCOPE, Tr.formatMessage(tc, msgKey, new Object[] { clientId }), null);
        _msgKey = msgKey;
        _reqType = requestType;
        _clientId = clientId;
    }

    // Liberty
    @Override
    public String formatSelf(Locale locale, String encoding) {
        if (_msgKey.contains("missing.scope")) {
            return MessageFormat.format(OAuth20ExceptionUtil.getResourceBundle(locale).getString(_msgKey),
                    new Object[] { WebUtils.encode(_reqType, locale, encoding) });
        }
        else if (_msgKey.contains("missing.registered.scope")) {
            return MessageFormat.format(OAuth20ExceptionUtil.getResourceBundle(locale).getString(_msgKey),
                    new Object[] { WebUtils.encode(_clientId, locale, encoding) });
        }
        else if (_msgKey.contains("empty.scope")) {
            return MessageFormat.format(OAuth20ExceptionUtil.getResourceBundle(locale).getString(_msgKey),
                    new Object[] { WebUtils.encode(arrayToString(_requestedScope), locale, encoding),
                            WebUtils.encode(arrayToString(_approvedScope), locale, encoding),
                            WebUtils.encode(_clientId, locale, encoding) });
        }
        else {
            return MessageFormat.format(OAuth20ExceptionUtil.getResourceBundle(locale).getString(_msgKey),
                    new Object[] { WebUtils.encode(arrayToString(_requestedScope), locale, encoding),
                            WebUtils.encode(arrayToString(_approvedScope), locale, encoding) });
        }

    }

    /**
     * @return an array of requested scopes.
     */
    public String[] getRequestedScope() {
        return _requestedScope;
    }

    /**
     * @return an array of approved scopes.
     */
    public String[] getApprovedScope() {
        return _approvedScope;
    }

    static String arrayToString(String[] strs) {
        StringBuffer sb = new StringBuffer();
        if (strs != null && strs.length > 0) {
            for (int i = 0; i < strs.length; i++) {
                sb.append(strs[i]);
                if (i < (strs.length - 1)) {
                    sb.append(",");
                }
            }
        }
        else {
            return "";
        }
        return sb.toString();
    }

}
