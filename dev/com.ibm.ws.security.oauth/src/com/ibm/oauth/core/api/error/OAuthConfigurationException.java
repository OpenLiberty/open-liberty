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
package com.ibm.oauth.core.api.error;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.web.WebUtils;

/**
 * Represents an exception while processing OAuth component configuration.
 */
public class OAuthConfigurationException extends OAuthException {

    private static final long serialVersionUID = 1L;
    private static final String RESOURCE_BUNDLE = "com.ibm.ws.security.oauth20.resources.ProviderMsgs";
    private static final TraceComponent tc = Tr.register(OAuthConfigurationException.class, "OAUTH20", RESOURCE_BUNDLE);

    public static final String ERROR_TYPE = "configuration_error";

    private String _configProperty;
    private String _value;
    private String _msgKey;
    private Throwable _cause;

    /**
     * Creates a OAuthConfigurationException.
     * 
     * @param configProperty A configuration property for the error message.
     * @param value A configuration property value for the message resource.
     * @param cause A root exception.
     */
    public OAuthConfigurationException(String configProperty, String value, Throwable cause) {
        super("Error with configuration property: " + configProperty + " value: " + value, cause);
        _configProperty = configProperty;
        _value = value;
        _cause = cause;
    }

    public OAuthConfigurationException(String msgKey, String configProperty, String value, Throwable cause) {
        super(Tr.formatMessage(tc, msgKey, new Object[] { configProperty, value, cause }), cause);
        _msgKey = msgKey;
        _configProperty = configProperty;
        _value = value;
        _cause = cause;
    }

    /**
     * Returns A configuration property for the error message.
     * 
     * @return A configuration property for the error message.
     */
    public String getConfigProperty() {
        return _configProperty;
    }

    /**
     * Returns A configuration property value for the message resource.
     * 
     * @return A configuration property value for the message resource.
     */
    public String getValue() {
        return _value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.oauth.core.api.error.OAuthException#getError()
     */
    public String getError() {
        return ERROR_TYPE;
    }

    // Liberty
    @Override
    public String formatSelf(Locale locale, String encoding) {
        if (_msgKey != null) {
            Object[] params = null;
            if (_cause != null) {
                params = new Object[] { WebUtils.encode(_configProperty, locale, encoding), WebUtils.encode(_value, locale, encoding), _cause.getMessage() };
            } else {
                params = new Object[] { WebUtils.encode(_configProperty, locale, encoding), WebUtils.encode(_value, locale, encoding), "" };
            }

            return MessageFormat.format(ResourceBundle.getBundle(RESOURCE_BUNDLE, locale).getString(_msgKey), params);
        } else {
            return this.getMessage() + ((_cause != null) ? (" : " + _cause.getMessage()) : "");
        }
    }

}
