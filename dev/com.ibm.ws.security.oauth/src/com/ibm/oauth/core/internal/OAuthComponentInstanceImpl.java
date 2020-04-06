/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.internal;

import com.ibm.oauth.core.api.OAuthComponentInstance;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.oauth20.OAuth20Component;
import com.ibm.oauth.core.internal.oauth20.OAuth20ComponentImpl;
import com.ibm.oauth.core.internal.oauth20.OAuth20ComponetImplWrapper;

/**
 * One and only implementation class for the OAuthComponentInstance interface.
 */
public class OAuthComponentInstanceImpl implements OAuthComponentInstance {

    OAuthComponentConfiguration _config = null;
    OAuth20Component _oauth20 = null;

    public OAuthComponentInstanceImpl(OAuthComponentConfiguration config)
            throws OAuthException {
        _config = config;
        buildComponentInstances();
    }

    public String getInstanceId() {
        return _config.getUniqueId();
    }

    public OAuth20Component getOAuth20Component() {
        return _oauth20;
    }

    void buildComponentInstances() throws OAuthException {
        OAuth20ComponentImpl realOAuth20Component = new OAuth20ComponentImpl(
                this, _config);
        _oauth20 = new OAuth20ComponetImplWrapper(realOAuth20Component,
                realOAuth20Component.getOAuthStatisticsImpl());
    }
}
