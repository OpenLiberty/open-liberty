/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins.jose4j;

import java.util.Map;

import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

/**
 * User claims for the id token, token introspection, identity assertion, and resource authorization.
 */
public interface OidcUserClaimsInterface {

    /**
     * @param oidcServerConfig
     */
    public void addExtraClaims(OidcServerConfig oidcServerConfig);

    public Map<String, Object> asMap();

}
