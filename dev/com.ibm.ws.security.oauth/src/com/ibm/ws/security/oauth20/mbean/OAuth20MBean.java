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

public interface OAuth20MBean {

    public static final String BEAN_TYPE = "OAuth20MBean";

    /**
     * Reloads every OAuth provider definition file
     * @throws OAuthProviderException
     */
    public void reloadAllProviders() throws OAuthProviderException;

    /**
     * Reloads the given provider definition file
     * @param providerName the provider ID
     * @throws OAuthProviderException
     */
    public void reloadProvider(String providerName)
            throws OAuthProviderException;

    /**
     * Reloads the given provider definition file
     * @param providerName the provider ID
     * @throws OAuthProviderException
     */
    public void reloadProvider(String providerName, boolean persist)
            throws OAuthProviderException;

    /**
     * Updates an OAuth provider based on a list of parameters
     * @param providerName the provider ID
     * @param parameters the OAuth params
     * @throws OAuthProviderException
     */
    public void updateProvider(String providerName,
            List<OAuth20Parameter> parameters) throws OAuthProviderException;

    /**
     * Updates an OAuth provider based on a list of parameters
     * @param providerName the provider ID
     * @param parameters the OAuth params
     * @param persistChanges true = save changes to file, false = only update runtime
     * @throws OAuthProviderException
     */
    public void updateProvider(String providerName,
            List<OAuth20Parameter> parameters,
            Boolean persistChanges) throws OAuthProviderException;

    /**
     * Updates a single OAuth provider parameter. If the parameter exists it will
     * be updated with new values. If the parameter does not exist it will be created.
     * @param providerName the provider ID
     * @param parameter the OAuth parameter
     * @param persistChanges true = save changes to file, false = only update runtime
     * @throws OAuthProviderException
     */
    public void updateProviderParameter(String providerName,
            OAuth20Parameter parameter,
            Boolean persistChanges) throws OAuthProviderException;

    /**
     * Creates an OAuth provider based on an XML configuration file
     * @param providerName the provider ID
     * @param parameters XML configuration parameters
     * @throws OAuthProviderException
     */
    public void createProvider(String providerName, List<OAuth20Parameter> parameters)
            throws OAuthProviderException;

    /**
     * Deletes an OAuth provider from the system
     * @param providerName the provider ID
     * @throws OAuthProviderException
     */
    public void deleteProvider(String providerName)
            throws OAuthProviderException;

    /**
     * Deletes a single OAuth provider parameter. The name and type of the supplied
     * OAuth20Parameter are used to identify the parameter to be deleted.
     * @param providerName the provider ID
     * @param parameter the OAuth parameter to delete
     * @param persistChanges true = save changes to file, false = only update runtime
     * @throws OAuthProviderException
     */
    public void deleteProviderParameter(String providerName,
            OAuth20Parameter parameter,
            Boolean persistChanges) throws OAuthProviderException;

    /**
     * Remove the token for a user and a client. If providerName is null
     * remove the token for all providers, else only for the given provider
     *
     * @param providerName
     * @param clientId
     * @param userName
     */
    public void removeOAuthTokenByClientID(String providerName,
            String clientId,
            String userName) throws OAuthProviderException;

    /**
     * Remove the token for a user and a unique key. If providerName is null
     * remove the token for all providers, else only for the given provider
     *
     * @param providerName
     * @param uniqueKey
     * @param userName
     */
    public void removeOAuthTokenByUniqueKey(String providerName,
            String uniqueKey,
            String userName) throws OAuthProviderException;

    /**
     * Remove all tokens issued to a client. If providerName is null
     * remove the tokens for all providers, else only for the given provider
     *
     * @param providerName
     * @param clientId
     */
    public void removeAllOAuthTokenByClientID(String providerName,
            String clientId) throws OAuthProviderException;

    /**
     * Remove all tokens issued for a user. If providerName is null
     * remove the tokens for all providers, else only for the given provider
     *
     * @param providerName
     * @param userName
     */
    public void removeAllOAuthTokenByUser(String providerName,
            String userName) throws OAuthProviderException;

    /**
     * Return authorizations for a user and a client. If providerName is null then get
     * authorizations for all providers, else only for the given provider
     *
     * @param providerName
     * @param userName
     */
    public String queryAuthorizationsByClientID(String providerName,
            String clientId,
            String userName) throws OAuthProviderException;

    /**
     * Return authorizations for a user and a unique key. If providerName is null then
     * get authorizations for all providers, else only for the given provider
     *
     * @param providerName
     * @param userName
     */
    public String queryAuthorizationsByUniqueKey(String providerName,
            String uniqueKey,
            String userName) throws OAuthProviderException;

    /**
     * Return all authorizations for a client. If providerName is null then get
     * authorizations for all providers, else only for the given provider
     *
     * @param providerName
     * @param userName
     */
    public String queryAllAuthorizationsByClientID(String providerName,
            String clientId) throws OAuthProviderException;

    /**
     * Return all authorizations for a user. If providerName is null then get
     * authorizations for all providers, else only for the given provider
     *
     * @param providerName
     * @param userName
     */
    public String queryAllAuthorizationsByUser(String providerName,
            String userName) throws OAuthProviderException;
}
