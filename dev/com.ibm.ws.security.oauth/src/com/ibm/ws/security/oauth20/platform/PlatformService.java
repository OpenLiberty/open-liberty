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
 * Platform-specific handling, for differences between WAS and Liberty
 */
public interface PlatformService {

    // called by OAuth20ProviderFactory.init()
    public void init() throws OAuthProviderException;

    // check to see if there's a reason not to run init
    public boolean skipInit();

    // to use mbeans or single server
    public boolean isDistributedCapable();

    // for oauth20.client.uri.substitutions
    public String getRewrite(String key) throws OAuthProviderException;

    // location to store the config file and file-based tokens
    public String getConfigFolder();

    // get dynacache or other distributed cache, if supported
    public <K, V> Map<K, V> getDistributedMap(String jndiName, final K[] arg0, final V[] arg1);

}
