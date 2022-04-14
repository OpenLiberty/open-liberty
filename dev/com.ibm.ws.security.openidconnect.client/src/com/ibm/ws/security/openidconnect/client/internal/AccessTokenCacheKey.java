/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.internal;

import java.util.Objects;

import com.ibm.websphere.ras.annotation.Sensitive;

public class AccessTokenCacheKey {

    @Sensitive
    private final String accessToken;
    private final String configId;

    public AccessTokenCacheKey(@Sensitive String accessToken, String configId) {
        this.accessToken = accessToken;
        this.configId = configId;
    }

    @Sensitive
    public String getAccessToken() {
        return accessToken;
    }

    public String getConfigId() {
        return configId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessToken, configId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AccessTokenCacheKey other = (AccessTokenCacheKey) obj;
        return Objects.equals(accessToken, other.getAccessToken()) && Objects.equals(configId, other.getConfigId());
    }

}
