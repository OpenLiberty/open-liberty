/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jaspi;

import javax.security.auth.message.config.AuthConfigFactory;

/**
 * Bridge to create the AuthConfigProvider, AuthConfig, AuthContext, and ServerAuthModule needed for JSR-375.
 */
public interface BridgeBuilderService {

    /**
     * @param appContext
     * @param providerFactory
     *
     */
    void buildBridgeIfNeeded(String appContext, AuthConfigFactory providerFactory);

}
