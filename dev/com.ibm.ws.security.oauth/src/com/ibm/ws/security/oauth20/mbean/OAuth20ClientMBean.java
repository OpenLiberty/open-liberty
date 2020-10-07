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
package com.ibm.ws.security.oauth20.mbean;

import com.ibm.ws.security.oauth20.exception.OAuthProviderException;
import com.ibm.ws.security.oauth20.plugins.BaseClient;

public interface OAuth20ClientMBean {

    public static final String BEAN_TYPE = "OAuth20ClientMBean";

    /**
     * Add a client
     * @throws OAuthProviderException
     */
    public void addClient(BaseClient newClient) throws OAuthProviderException;

    /**
     * Remove a client
     * @throws OAuthProviderException
     */
    public void removeClient(String providerName, String clientId) throws OAuthProviderException;

}
