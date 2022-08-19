/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
