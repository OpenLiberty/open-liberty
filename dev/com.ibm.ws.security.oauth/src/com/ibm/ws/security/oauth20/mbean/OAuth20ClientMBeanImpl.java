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
import com.ibm.ws.security.oauth20.plugins.BaseClientProvider;

/**
 * This class was imported from tWAS to make only those changes necessary to
 * run OAuth on Liberty. The mission was not to refactor, restructure, or
 * generally cleanup the code.
 */
public class OAuth20ClientMBeanImpl extends BaseClientProvider implements
        OAuth20ClientMBean {

    /** {@inheritDoc} */
    @Override
    public void addClient(BaseClient newClient) throws OAuthProviderException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void removeClient(String providerName, String clientId) throws OAuthProviderException {
        // TODO Auto-generated method stub

    }

}
