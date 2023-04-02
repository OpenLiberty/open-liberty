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
package com.ibm.ws.security.social.tai;

import java.util.Objects;

import com.ibm.websphere.ras.annotation.Sensitive;

class UserApiCacheKey {

    @Sensitive
    private final String token;
    private final String configId;

    public UserApiCacheKey(@Sensitive String token, String configId) {
        this.token = token;
        this.configId = configId;
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
        return Objects.equals(token, other.token) && Objects.equals(configId, other.configId);
    }

}
