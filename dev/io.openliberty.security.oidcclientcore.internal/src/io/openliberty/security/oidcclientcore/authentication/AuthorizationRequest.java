/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.authentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.oidcclientcore.exceptions.OidcClientConfigurationException;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;
import io.openliberty.security.oidcclientcore.storage.OidcClientStorageConstants;
import io.openliberty.security.oidcclientcore.storage.OidcStorageUtils;
import io.openliberty.security.oidcclientcore.storage.Storage;
import io.openliberty.security.oidcclientcore.storage.StorageProperties;
import io.openliberty.security.oidcclientcore.utils.Utils;

public abstract class AuthorizationRequest {

    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected String clientId;

    protected Storage storage;

    protected AuthorizationRequestUtils requestUtils = new AuthorizationRequestUtils();
    protected OidcStorageUtils storageUtils = new OidcStorageUtils();

    public AuthorizationRequest(HttpServletRequest request, HttpServletResponse response, String clientId) {
        this.request = request;
        this.response = response;
        this.clientId = clientId;
    }

    public ProviderAuthenticationResult sendRequest() throws OidcClientConfigurationException, OidcDiscoveryException {
        getAuthorizationEndpoint();

        createSessionIfNecessary();

        String state = requestUtils.generateStateValue(request);
        storeStateValue(state);

        String redirectUrl = getRedirectUrl();
        return redirectToAuthorizationEndpoint(state, redirectUrl);
    }

    protected abstract String getAuthorizationEndpoint() throws OidcClientConfigurationException, OidcDiscoveryException;

    protected abstract String getRedirectUrl() throws OidcClientConfigurationException;

    protected abstract boolean shouldCreateSession();

    protected abstract String createStateValueForStorage(String state);

    protected abstract String createNonceValueForStorage(String nonce, String state);

    protected abstract ProviderAuthenticationResult redirectToAuthorizationEndpoint(String state, String redirectUrl);

    protected StorageProperties getStateStorageProperties() {
        StorageProperties props = new StorageProperties();
        props.setStorageLifetimeSeconds(OidcClientStorageConstants.DEFAULT_STATE_STORAGE_LIFETIME_SECONDS);
        return props;
    }

    protected StorageProperties getNonceStorageProperties() {
        StorageProperties props = new StorageProperties();
        return props;
    }

    protected StorageProperties getOriginalRequestUrlStorageProperties() {
        StorageProperties props = new StorageProperties();
        props.setStorageLifetimeSeconds(OidcClientStorageConstants.DEFAULT_REQ_URL_STORAGE_LIFETIME_SECONDS);
        return props;
    }

    protected void storeStateValue(String state) {
        String storageName = OidcClientStorageConstants.WAS_OIDC_STATE_KEY + Utils.getStrHashCode(state);
        String storageValue = createStateValueForStorage(state);
        StorageProperties stateStorageProperties = getStateStorageProperties();
        storage.store(storageName, storageValue, stateStorageProperties);
    }

    protected void storeNonceValue(String nonce, String state) {
        String storageName = OidcStorageUtils.getStorageKey(OidcClientStorageConstants.WAS_OIDC_NONCE, clientId, state);
        String storageValue = createNonceValueForStorage(nonce, state);
        StorageProperties nonceStorageProperties = getNonceStorageProperties();
        storage.store(storageName, storageValue, nonceStorageProperties);
    }

    protected void storeOriginalRequestUrl(String state) {
        String storageName = OidcClientStorageConstants.WAS_REQ_URL_OIDC + Utils.getStrHashCode(state);
        String storageValue = requestUtils.getRequestUrl(request);
        StorageProperties reqUrlStorageProperties = getOriginalRequestUrlStorageProperties();
        storage.store(storageName, storageValue, reqUrlStorageProperties);
    }

    void createSessionIfNecessary() {
        if (shouldCreateSession()) {
            try {
                request.getSession(true);
            } catch (Exception e) {
                // ignore it. Session exists
            }
        }
    }

}
