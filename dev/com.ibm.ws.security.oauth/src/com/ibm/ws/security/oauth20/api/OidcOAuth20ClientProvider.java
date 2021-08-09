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
package com.ibm.ws.security.oauth20.api;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;

/**
 * The OAuth20ClientProvider is an extension interface which allow component
 * consumers to provide registered OAuth 2.0 client configuration data.
 * Component consumers must provide a class implementing this interface.
 */
public interface OidcOAuth20ClientProvider {

    /**
     * 
     * This method is called by a factory when an instance of this object is
     * created. The configuration object will allow the client provider to
     * initialize itself.
     * 
     * @param config a OAuthComponentConfiguration entity for the component instance.
     */
    public void init(OAuthComponentConfiguration config);

    /**
     * Returns true if the client exists and is enabled, otherwise returns
     * false.
     * 
     * @param clientIdentifier a unique key that identifies the client
     * @return TRUE if the client exists, otherwise FALSE
     * @throws OidcServerException 
     */
    public boolean exists(String clientIdentifier) throws OidcServerException;

    /**
     * Returns a client identified by the given clientIdentifier. If no client
     * with the specified id exists, this method should return null
     * 
     * @param clientIdentifier a unique key that identifies the client
     * @return The registered client, or null if there is no client registered
     *         with that identifier.
     */
    public OidcBaseClient get(String clientIdentifier) throws OidcServerException;

    /**
     * Validates a client id and client secret. This method should return false
     * for public clients.
     * 
     * @param clientIdentifier a unique key that identifies the client
     * @param clientSecret a client's secret
     * 
     * @return TRUE if the clientIdentifier and clientSecret match a registered
     *         and enabled confidential client, FALSE otherwise.
     * @throws OidcServerException 
     */
    public boolean validateClient(String clientIdentifier, String clientSecret) throws OidcServerException;

    public boolean delete(String clientIdentifier) throws OidcServerException;

    // returns null on error
    public Collection<OidcBaseClient> getAll() throws OidcServerException;

    // returns null on error
    public Collection<OidcBaseClient> getAll(HttpServletRequest request) throws OidcServerException;

    public OidcBaseClient put(OidcBaseClient newClient) throws OidcServerException;

    public OidcBaseClient update(OidcBaseClient newClient) throws OidcServerException;

    public void initialize();
}
