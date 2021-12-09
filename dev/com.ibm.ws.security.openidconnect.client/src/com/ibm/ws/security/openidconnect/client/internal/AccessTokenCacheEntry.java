/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.internal;

import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

public class AccessTokenCacheEntry {

    private final String uniqueID;
    private final ProviderAuthenticationResult result;

    public AccessTokenCacheEntry(String uniqueID, ProviderAuthenticationResult result) {
        this.uniqueID = uniqueID;
        this.result = result;
    }

    public String getUniqueID() {
        return uniqueID;
    }

    public ProviderAuthenticationResult getResult() {
        return result;
    }
}
