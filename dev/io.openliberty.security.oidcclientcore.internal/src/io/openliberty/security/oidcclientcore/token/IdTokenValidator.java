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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.token;

import com.ibm.websphere.ras.annotation.Sensitive;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.storage.OidcStorageUtils;
import io.openliberty.security.oidcclientcore.storage.Storage;

/**
 *
 */
public class IdTokenValidator extends TokenValidator {
    
    private String nonce;
    private String state;
    private Storage storage;
    private String secret;

    /**
     * @param clientConfig
     */
    public IdTokenValidator(OidcClientConfig clientConfig) {
        super(clientConfig);
    }
    
    public IdTokenValidator nonce(String nonce) {
        this.nonce = nonce;
        return this;
    }
    
    /**
     * @param string
     */
    public IdTokenValidator state(String state) {
        this.state = state;
        return this;
    }

    /**
     * @param storage
     */
    public IdTokenValidator storage(Storage storage) {
        this.storage = storage;
        return this;
    }

    /**
     * @param clientSecret
     */
    public IdTokenValidator secret(@Sensitive String clientSecret) {
        this.secret = clientSecret;
        return this;   
    }

    public void validateNonce() throws TokenValidationException {
        String cookieName = OidcStorageUtils.getNonceStorageKey(this.oidcConfig.getClientId(), state);
        String cookieValue = OidcStorageUtils.createNonceStorageValue(nonce, state, secret);
        String storedCookieValue = storage.get(cookieName);
        storage.remove(cookieName);
        if (!(cookieValue.equals(storedCookieValue))) {
            throw new TokenValidationException(this.oidcConfig.getClientId(), "The nonce [ " + this.nonce + " ]" + "in the token does not match the nonce that was specified in the request to the OpenID Connect provider");
        }
    }

}
