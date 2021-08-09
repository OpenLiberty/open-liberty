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

import java.util.List;

import com.ibm.ws.security.oauth20.exception.OAuthProviderException;
import com.ibm.ws.security.oauth20.util.OAuth20Parameter;

/**
 * This class was imported from tWAS to make only those changes necessary to
 * run OAuth on Liberty. The mission was not to refactor, restructure, or
 * generally cleanup the code.
 */
public class OAuth20MBeanImpl implements OAuth20MBean {

    /** {@inheritDoc} */
    @Override
    public void reloadAllProviders() throws OAuthProviderException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void reloadProvider(String providerName) throws OAuthProviderException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void reloadProvider(String providerName, boolean persist) throws OAuthProviderException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void updateProvider(String providerName, List<OAuth20Parameter> parameters) throws OAuthProviderException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void updateProvider(String providerName, List<OAuth20Parameter> parameters, Boolean persistChanges) throws OAuthProviderException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void updateProviderParameter(String providerName, OAuth20Parameter parameter, Boolean persistChanges) throws OAuthProviderException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void createProvider(String providerName, List<OAuth20Parameter> parameters) throws OAuthProviderException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void deleteProvider(String providerName) throws OAuthProviderException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void deleteProviderParameter(String providerName, OAuth20Parameter parameter, Boolean persistChanges) throws OAuthProviderException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void removeOAuthTokenByClientID(String providerName, String clientId, String userName) throws OAuthProviderException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void removeOAuthTokenByUniqueKey(String providerName, String uniqueKey, String userName) throws OAuthProviderException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void removeAllOAuthTokenByClientID(String providerName, String clientId) throws OAuthProviderException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void removeAllOAuthTokenByUser(String providerName, String userName) throws OAuthProviderException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public String queryAuthorizationsByClientID(String providerName, String clientId, String userName) throws OAuthProviderException {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String queryAuthorizationsByUniqueKey(String providerName, String uniqueKey, String userName) throws OAuthProviderException {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String queryAllAuthorizationsByClientID(String providerName, String clientId) throws OAuthProviderException {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String queryAllAuthorizationsByUser(String providerName, String userName) throws OAuthProviderException {
        // TODO Auto-generated method stub
        return null;
    }

}
