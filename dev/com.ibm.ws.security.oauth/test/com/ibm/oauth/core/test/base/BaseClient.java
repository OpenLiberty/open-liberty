/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.test.base;

import com.google.gson.JsonArray;
import com.ibm.oauth.core.api.oauth20.client.OAuth20Client;

public class BaseClient implements OAuth20Client {

    String _clientId;
    String _clientSecret;
    String _clientName;
    JsonArray _redirectURIs;
    boolean _isEnabled;
    boolean _allowRegex;

    public BaseClient(String clientId, String clientSecret, String clientName,
                      JsonArray redirectURIs, boolean isEnabled) {
        _clientId = clientId;
        _clientSecret = clientSecret;
        _clientName = clientName;
        _redirectURIs = redirectURIs;
        _isEnabled = isEnabled;
    }

    @Override
    public String getClientId() {
        return _clientId;
    }

    @Override
    public String getClientSecret() {
        return _clientSecret;
    }

    @Override
    public String getClientName() {
        return _clientName;
    }

    @Override
    public JsonArray getRedirectUris() {
        return _redirectURIs;
    }

    @Override
    public boolean isEnabled() {
        return _isEnabled;
    }

    @Override
    public String[] getExtensionProperty(String propertyName) {
        // No extension properties are expected
        return null;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("{");
        sb.append("_clientId: " + _clientId);
        sb.append(" _clientSecret: " + _clientSecret);
        sb.append(" _displayName: " + _clientName);
        sb.append(" _redirectURI: " + _redirectURIs);
        sb.append(" _isEnabled: " + _isEnabled);
        sb.append("}");
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.oauth.core.api.oauth20.client.OAuth20Client#setAllowRegexpRedirects(boolean)
     */
    @Override
    public void setAllowRegexpRedirects(boolean value) {
        _allowRegex = value;

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.oauth.core.api.oauth20.client.OAuth20Client#getAllowRegexpRedirects()
     */
    @Override
    public boolean getAllowRegexpRedirects() {
        return _allowRegex;
    }
}
