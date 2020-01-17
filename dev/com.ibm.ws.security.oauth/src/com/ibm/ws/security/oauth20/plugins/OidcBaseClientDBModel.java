/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins;

import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.oauth20.TraceConstants;

/**
 *
 */
public class OidcBaseClientDBModel {
    private static final TraceComponent tc = Tr.register(OidcBaseClientDBModel.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private final static String CLASS = OidcBaseClientDBModel.class.getName();

    private String componentId;
    private String clientId;
    @Sensitive
    private String clientSecret;
    private String displayName;
    private String redirectUri;
    private int enabled;
    @Sensitive
    private JsonObject clientMetadata;

    public OidcBaseClientDBModel(String componentId,
            String clientId,
            String clientSecret,
            String displayName,
            String redirectUri,
            int enabled,
            JsonObject clientMetadata) {
        this.componentId = componentId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.displayName = displayName;
        this.redirectUri = redirectUri;
        this.enabled = enabled;
        this.clientMetadata = clientMetadata;
    }

    /**
     * @return the componentId
     */
    public String getComponentId() {
        return componentId;
    }

    /**
     * @param componentId the componentId to set
     */
    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    /**
     * @return the clientId
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * @param clientId the clientId to set
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * @return the clientSecret
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * @param clientSecret the clientSecret to set
     */
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     * @return the displayName
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @param displayName the displayName to set
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * @return the redirectUri
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * @param redirectUri the redirectUri to set
     */
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    /**
     * @return the enabled
     */
    public int getEnabled() {
        return enabled;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(int enabled) {
        this.enabled = enabled;
    }

    /**
     * @return the clientMetadata
     */
    @Sensitive
    public JsonObject getClientMetadata() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            Set<Map.Entry<String, JsonElement>> entries = clientMetadata.entrySet();
            for (Map.Entry<String, JsonElement> entry : entries) {
                String key = entry.getKey();

                if (key.equals(OAuth20Constants.CLIENT_SECRET)) {
                    sb.append(" " + key + "=secret_removed");
                } else {
                    sb.append(" " + key + "=" + entry.getValue());
                }

            }
            sb.append("]");
            Tr.debug(tc, CLASS, "getClientMetadata: " + sb.toString());
        }
        return clientMetadata;
    }

    /**
     * @param clientMetadata the clientMetadata to set
     */
    public void setClientMetadata(@Sensitive JsonObject clientMetadata) {
        this.clientMetadata = clientMetadata;
    }
}
