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
package io.openliberty.security.oidcclientcore.storage;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.common.crypto.HashUtils;

import io.openliberty.security.oidcclientcore.utils.Utils;

public class OidcStorageUtils {

    public static String getStateStorageKey(String state) {
        return OidcClientStorageConstants.WAS_OIDC_STATE_KEY + Utils.getStrHashCode(state);
    }

    public static String getNonceStorageKey(String clientId, String state) {
        return getStorageKey(OidcClientStorageConstants.WAS_OIDC_NONCE, clientId, state);
    }

    public static String getOriginalReqUrlStorageKey(String state) {
        return OidcClientStorageConstants.WAS_REQ_URL_OIDC + Utils.getStrHashCode(state);
    }

    @Trivial
    public static String createStateStorageValue(String state, @Sensitive String clientSecret) {
        String newValue = state + clientSecret; // state already has a timestamp in it
        String hashedStateValue = HashUtils.digest(newValue);
        String timestamp = state.substring(0, Utils.TIMESTAMP_LENGTH);
        return timestamp + hashedStateValue;
    }

    public static String createNonceStorageValue(String nonceValue, String state, @Sensitive String clientSecret) {
        return HashUtils.digest(nonceValue + state + clientSecret);
    }

    @Sensitive
    @Trivial
    public static String getStorageKey(String prefix, String configId, String state) {
        String newValue = state + configId;
        String newName = Utils.getStrHashCode(newValue);
        return prefix + newName;
    }

}
