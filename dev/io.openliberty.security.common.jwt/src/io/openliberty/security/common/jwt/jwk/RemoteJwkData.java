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
package io.openliberty.security.common.jwt.jwk;

import com.ibm.wsspi.ssl.SSLSupport;

public class RemoteJwkData {

    private String jwksUri;
    private SSLSupport sslSupport;
    private int jwksConnectTimeout = 500;
    private int jwksReadTimeout = 500;

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public SSLSupport getSslSupport() {
        return sslSupport;
    }

    public void setSslSupport(SSLSupport sslSupport) {
        this.sslSupport = sslSupport;
    }

    public int getJwksConnectTimeout() {
        return jwksConnectTimeout;
    }

    public void setJwksConnectTimeout(int jwksConnectTimeout) {
        this.jwksConnectTimeout = jwksConnectTimeout;
    }

    public int getJwksReadTimeout() {
        return jwksReadTimeout;
    }

    public void setJwksReadTimeout(int jwksReadTimeout) {
        this.jwksReadTimeout = jwksReadTimeout;
    }

}
