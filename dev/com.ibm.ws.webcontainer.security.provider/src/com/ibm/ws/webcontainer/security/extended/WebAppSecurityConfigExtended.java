/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.extended;

import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.openidconnect.OidcClient;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServer;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Encapsulate the web application security settings.
 */
public interface WebAppSecurityConfigExtended extends WebAppSecurityConfig {

    /**
     * @param oidcServerRef
     * @param oidcClientRef
     */
    void setSsoCookieName(AtomicServiceReference<OidcServer> oidcServerRef, AtomicServiceReference<OidcClient> oidcClientRef);

}