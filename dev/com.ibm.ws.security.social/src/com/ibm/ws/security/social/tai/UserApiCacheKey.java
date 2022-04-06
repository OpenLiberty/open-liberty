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
package com.ibm.ws.security.social.tai;

import java.util.Objects;

public class UserApiCacheKey {

    private final String token;
    private final String configId;

    public UserApiCacheKey(String token, String configId) {
        this.token = token;
        this.configId = configId;
    }

    public String getToken() {
        return token;
    }

    public String getConfigId() {
        return configId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, configId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        UserApiCacheKey other = (UserApiCacheKey) obj;
        return Objects.equals(token, other.getToken()) && Objects.equals(configId, other.getConfigId());
    }

}
