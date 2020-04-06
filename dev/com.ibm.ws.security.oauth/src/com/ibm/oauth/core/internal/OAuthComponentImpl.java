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

import com.ibm.oauth.core.api.OAuthComponent;
import com.ibm.oauth.core.api.OAuthComponentInstance;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.statistics.OAuthStatistics;
import com.ibm.oauth.core.internal.config.OAuthComponentConfigurationWrapper;
import com.ibm.oauth.core.internal.statistics.OAuthStatisticsImpl;

public class OAuthComponentImpl implements OAuthComponent {
    protected OAuthComponentConfiguration _config;
    protected OAuthComponentInstance _parent;
    protected OAuthStatisticsImpl _stats;

    protected OAuthComponentImpl(OAuthComponentInstance parent,
            OAuthComponentConfiguration config) {
        _parent = parent;
        _stats = new OAuthStatisticsImpl();
        _config = new OAuthComponentConfigurationWrapper(config, _stats);
    }

    public OAuthComponentConfiguration getConfiguration() {
        return _config;
    }

    public OAuthComponentInstance getParentComponentInstance() {
        return _parent;
    }

    public OAuthStatistics getStatistics() {
        return _stats;
    }

    public OAuthStatisticsImpl getOAuthStatisticsImpl() {
        return _stats;
    }
}
