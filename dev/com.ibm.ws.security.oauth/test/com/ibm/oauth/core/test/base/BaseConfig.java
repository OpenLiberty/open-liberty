/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.oauth.core.test.base;

import com.ibm.oauth.core.api.config.OAuthComponentConfigurationConstants;
import com.ibm.oauth.core.api.config.SampleComponentConfiguration;

public class BaseConfig extends SampleComponentConfiguration {

    public static final String[] CLIENT_PROVIDER_CLASSNAME = { "com.ibm.oauth.core.test.base.OidcBaseClientProvider" };
    public static final String[] TOKEN_CACHE_CLASSNAME = { "com.ibm.oauth.core.test.base.BaseCache" };

    protected static int uniqueIDSeed = 100;

    String uniqueID = null;

    public BaseConfig() {
        super();
        _config.put(OAuthComponentConfigurationConstants.OAUTH20_CLIENT_PROVIDER_CLASSNAME, CLIENT_PROVIDER_CLASSNAME);
        _config.put(OAuthComponentConfigurationConstants.OAUTH20_TOKEN_CACHE_CLASSNAME, TOKEN_CACHE_CLASSNAME);

        uniqueID = generateUniqueID();
    }

    protected static synchronized String generateUniqueID() {
        uniqueIDSeed++;
        return uniqueIDSeed + "";
    }

    @Override
    public String getUniqueId() {
        return uniqueID;
    }

    public void setUniqueId(String id) {
        uniqueID = id;
    }

}
