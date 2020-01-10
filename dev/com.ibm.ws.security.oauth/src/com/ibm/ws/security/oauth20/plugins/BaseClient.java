/*******************************************************************************
 * Copyright (c) 1997, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.plugins;

import java.io.Serializable;

import com.google.gson.JsonArray;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.ibm.oauth.core.api.oauth20.client.OAuth20Client;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;

public class BaseClient implements OAuth20Client, Serializable {

    public static final String SN_CLIENT_ID = OAuth20Constants.CLIENT_ID;
    public static final String SN_CLIENT_SECRET = OAuth20Constants.CLIENT_SECRET;
    public static final String SN_CLIENT_NAME = "client_name";
    public static final String SN_REDIRECT_URIS = OIDCConstants.OIDC_CLIENTREG_REDIRECT_URIS;
    public static final String SN_ALLOW_REGEXP_REDIRECTS = "allow_regexp_redirects";

    private static final long serialVersionUID = -6749898820928856404L;

    String _componentId;

    // these affect the names used for json<-->java transformations in database backed clients,
    // so are like an API. Don't change them without careful thought.
    @Expose
    @SerializedName(SN_CLIENT_ID)
    String _clientId;

    @Expose
    @SerializedName(SN_CLIENT_SECRET)
    String _clientSecret;

    @Expose
    @SerializedName(SN_CLIENT_NAME)
    String _clientName;

    @Expose
    @SerializedName(SN_REDIRECT_URIS)
    JsonArray _redirectURIs = new JsonArray();

    @Expose
    @SerializedName(SN_ALLOW_REGEXP_REDIRECTS)
    Boolean _allowRegexpRedirects;

    boolean _isEnabled;

    public BaseClient(String componentId, String clientId, @Sensitive String clientSecret,
            String clientName, JsonArray redirectURIs, boolean isEnabled) {
        _componentId = componentId;
        _clientId = clientId;
        _clientSecret = clientSecret;
        _clientName = clientName;
        _redirectURIs = redirectURIs;
        _isEnabled = isEnabled;
        _allowRegexpRedirects = Boolean.valueOf(false);
    }

    public String getComponentId() {
        return _componentId;
    }

    @Override
    public String getClientId() {
        return _clientId;
    }

    @Override
    @Sensitive
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

    public void setComponentId(String componentId) {
        this._componentId = componentId;
    }

    public void setClientId(String clientId) {
        this._clientId = clientId;
    }

    public void setClientSecret(@Sensitive String clientSecret) {
        this._clientSecret = clientSecret;
    }

    public void setClientName(String clientName) {
        this._clientName = clientName;
    }

    public void setRedirectUris(JsonArray redirectURIs) {
        this._redirectURIs = redirectURIs;
    }

    public void setEnabled(boolean isEnabled) {
        this._isEnabled = isEnabled;
    }

    public boolean isConfidential() {
        return (_clientSecret != null && _clientSecret.length() > 0);
    }

    @Override
    public String[] getExtensionProperty(String propertyName) {
        // not used for bearer tokens
        return null;
    }

    @Override
    public void setAllowRegexpRedirects(boolean value) {
        this._allowRegexpRedirects = Boolean.valueOf(value);
    }

    @Override
    public boolean getAllowRegexpRedirects() {
        return _allowRegexpRedirects == null ? false : _allowRegexpRedirects.booleanValue();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[_componentId=" + _componentId);
        sb.append(" _clientId=" + _clientId);
        sb.append(" _clientSecret=" + "secret_removed");
        sb.append(" _displayName=" + _clientName);
        sb.append(" _redirectURIs=" + OidcOAuth20Util.getSpaceDelimitedString(_redirectURIs));
        sb.append(" _isEnabled=" + _isEnabled);
        sb.append(" _allowRegexpRepRedirects=" + _allowRegexpRedirects);
        sb.append("]");
        return sb.toString();
    }
}
