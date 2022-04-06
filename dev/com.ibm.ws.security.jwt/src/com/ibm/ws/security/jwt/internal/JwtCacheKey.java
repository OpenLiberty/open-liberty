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
package com.ibm.ws.security.jwt.internal;

import java.util.Objects;

public class JwtCacheKey {

    private final String jwt;
    private final String configId;

    public JwtCacheKey(String jwt, String configId) {
        this.jwt = jwt;
        this.configId = configId;
    }

    public String getJwt() {
        return jwt;
    }

    public String getConfigId() {
        return configId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(jwt, configId);
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
        JwtCacheKey other = (JwtCacheKey) obj;
        return Objects.equals(jwt, other.getJwt()) && Objects.equals(configId, other.getConfigId());
    }

}
