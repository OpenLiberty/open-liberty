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

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.exception.OAuthProviderException;

public class WASPlatformService implements PlatformService {

    private static TraceComponent tc = Tr.register(WASPlatformService.class,
            "OAuth20Provider",
            "com.ibm.ws.security.oauth20.resources.ProviderMsgs");

    private static Logger logger = Logger.getLogger(WASPlatformService.class
            .getName());
    private static ResourceBundle resBundle = ResourceBundle.getBundle(
            Constants.RESOURCE_BUNDLE, Locale.getDefault());

    private static String configFileDir = null;

    public void init() throws OAuthProviderException {

    }

    public boolean skipInit() {
        return false;
    }

    public String getRewrite(String key) throws OAuthProviderException {
        return null;
    }

    public boolean isDistributedCapable() {
        return true;
    }

    public String getConfigFolder() {
        return null;
    }

    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getDistributedMap(String jndiName, final K[] arg0, final V[] arg1) {
        return null;
    }

}
