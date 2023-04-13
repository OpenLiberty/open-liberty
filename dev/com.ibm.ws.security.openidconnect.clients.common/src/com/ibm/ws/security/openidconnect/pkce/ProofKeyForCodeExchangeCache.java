/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.pkce;

import com.ibm.ws.security.common.structures.SingleTableCache;

public class ProofKeyForCodeExchangeCache {

    // TODO - decide on an official caching approach
    private static SingleTableCache codeVerifierCache = new SingleTableCache(1000 * 60 * 60);

    public static void cacheCodeVerifier(String state, String codeVerifier) {
        codeVerifierCache.put(state, codeVerifier);
    }

    public static String getCachedCodeVerifier(String state) {
        return (String) codeVerifierCache.get(state);
    }

}
