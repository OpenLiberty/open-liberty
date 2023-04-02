/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.internal;

import java.util.Objects;

import com.ibm.websphere.ras.annotation.Sensitive;

class JwtCacheKey {

    @Sensitive
    private final String jwt;
    private final String configId;

    public JwtCacheKey(@Sensitive String jwt, String configId) {
        this.jwt = jwt;
        this.configId = configId;
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
        return Objects.equals(jwt, other.jwt) && Objects.equals(configId, other.configId);
    }

}
