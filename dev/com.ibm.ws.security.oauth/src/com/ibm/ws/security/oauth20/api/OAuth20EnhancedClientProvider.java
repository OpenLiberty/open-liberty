/*******************************************************************************
 * Copyright (c) 1997, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.api;

import java.util.Collection;

import com.ibm.oauth.core.api.oauth20.client.OAuth20ClientProvider;
import com.ibm.ws.security.oauth20.exception.OAuthDataException;
import com.ibm.ws.security.oauth20.plugins.BaseClient;

public interface OAuth20EnhancedClientProvider extends OAuth20ClientProvider {

    public void initialize();

    public boolean delete(String clientIdentifier);

    // returns null on error
    public Collection<BaseClient> getAll();

    // Returns -1 on error
    public int getCount();

    public BaseClient put(BaseClient newClient) throws OAuthDataException;

    public BaseClient update(BaseClient newClient) throws OAuthDataException;
}
