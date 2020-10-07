/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.platform;

import java.util.Map;

import com.ibm.ws.security.oauth20.exception.OAuthProviderException;

/* 
 * JUnit platform
 */
public class TestPlatformService implements PlatformService {

    public void init() {
        // nothing needed for now
    }

    public boolean skipInit() {
        return false;
    }

    public boolean isDistributedCapable() {
        return false;
    }

    public String getRewrite(String key) throws OAuthProviderException {
        throw new OAuthProviderException(new UnsupportedOperationException());
    }

    public String getConfigFolder() {
        String folder = "./OOBConfig"; // was.oauth/OOBconfig
        return folder;
    }

    public <K, V> Map<K, V> getDistributedMap(String jndiName, final K[] arg0, final V[] arg1) {
        // no cluster, never needed
        throw new UnsupportedOperationException();
    }

}
