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
package com.ibm.ws.security.openidconnect.backchannellogout.internal;

import java.util.Objects;

import com.ibm.websphere.ras.annotation.Sensitive;

class JtiCacheKey {

    private final String jti;
    private final String configId;

    public JtiCacheKey(@Sensitive String jti, String configId) {
        this.jti = jti;
        this.configId = configId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(jti, configId);
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
        JtiCacheKey other = (JtiCacheKey) obj;
        return Objects.equals(jti, other.jti) && Objects.equals(configId, other.configId);
    }

}
