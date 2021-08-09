/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins.custom;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.oauth20.store.OAuthClient;
import com.ibm.websphere.security.oauth20.store.OAuthStore;
import com.ibm.websphere.security.oauth20.store.OAuthStoreException;
import com.ibm.ws.security.oauth20.TraceConstants;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.error.impl.BrowserAndServerLogMessage;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClientValidator;
import com.ibm.ws.security.oauth20.util.HashSecretUtils;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;
import com.ibm.ws.security.oauth20.web.RegistrationEndpointServices;

/**
 * Client store that uses customer provided OAuthStore to manage client providers.
 */
public class OauthClientStore implements OidcOAuth20ClientProvider {

    private static TraceComponent tc = Tr.register(OauthClientStore.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private final OAuthStore oauthStore;
    private final String componentId;

    private final boolean updateXORtoHash = true;
    private String hashType = OAuth20Constants.XOR;
    private final int numIterations = HashSecretUtils.DEFAULT_ITERATIONS;
    private final int keylength = HashSecretUtils.DEFAULT_KEYSIZE;

    public OauthClientStore(String componentId, OAuthStore oauthStore) {
        this.componentId = componentId;
        this.oauthStore = oauthStore;
    }

    public OauthClientStore(String componentId, OAuthStore oauthStore, String hashType) {
        this.componentId = componentId;
        this.oauthStore = oauthStore;
        this.hashType = hashType;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Client secret hash type is " + hashType);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
    }

    /** {@inheritDoc} */
    @Override
    public void init(OAuthComponentConfiguration config) {
    }

    /** {@inheritDoc} */
    @Override
    public OidcBaseClient put(OidcBaseClient newClient) throws OidcServerException {
        OidcBaseClient returnClient = null;

        try {
            OAuthClient oauthClient = getOauthClient(newClient);
            // TODO: Determine if enabled must be always true when inserting a client into the store.
            oauthStore.create(oauthClient);
            returnClient = newClient;
        } catch (OAuthStoreException e) {
            logMessageAndThrowOidcServerException(e, "ERROR_PERFORMING_OAUTH_STORE_CREATE_CLIENT", newClient.getClientId());
        }

        return returnClient;
    }

    private OAuthClient getOauthClient(OidcBaseClient client) throws OidcServerException {
        JsonObject clientMetadataAsJson = OidcOAuth20Util.getJsonObj(client);

        String clientSecret = null;
        // The client secret is stored on the Object and in the metadata, hash both
        if (isPBKDF2WithHmacSHA512Configured()) {
            HashSecretUtils.processMetatypeForHashInfo(clientMetadataAsJson, client.getClientId(), hashType, numIterations, keylength);
            HashSecretUtils.hashClientMetaTypeSecret(clientMetadataAsJson, client.getClientId(), updateXORtoHash);
            clientSecret = HashSecretUtils.hashSecret(client.getClientSecret(), client.getClientId(), updateXORtoHash, clientMetadataAsJson);
        } else if (isXORConfigured()) {
            clientSecret = client.getClientSecret();
            if (clientSecret != null && !clientSecret.isEmpty()) {
                clientSecret = PasswordUtil.passwordEncode(clientSecret);
            }
            if (clientMetadataAsJson != null && clientMetadataAsJson.has(OAuth20Constants.CLIENT_SECRET)) {
                String metaClientSecret = clientMetadataAsJson.get(OAuth20Constants.CLIENT_SECRET).getAsString();
                if (metaClientSecret != null && !metaClientSecret.isEmpty()) {
                    clientMetadataAsJson.addProperty(OAuth20Constants.CLIENT_SECRET, PasswordUtil.passwordEncode(metaClientSecret));
                }
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The client secret type is unknown, attempt to hash. " + hashType);
            }
            // to do -- throw an exception here? We don't want to store something unexpected in the custom store.
            OAuthStoreException ie = new OAuthStoreException("Unknown hash type provided, " + hashType + ", the new client cannot be registered: " + client.getClientId());
            logMessageAndThrowOidcServerException(ie, "ERROR_PERFORMING_OAUTH_STORE_CREATE_CLIENT", client.getClientId());
        }
        String clientMetadata = clientMetadataAsJson.toString(); // TODO: Determine if client_id needs to be removed from metadata.

        return new OAuthClient(componentId, client.getClientId(), clientSecret, client.getClientName(), client.isEnabled(), clientMetadata);
    }

    /** {@inheritDoc} */
    @Override
    public OidcBaseClient update(OidcBaseClient newClient) throws OidcServerException {
        OidcBaseClient returnClient = null;

        try {
            OAuthClient oauthClient = getOauthClient(newClient);
            oauthStore.update(oauthClient);
            returnClient = newClient;
        } catch (OAuthStoreException e) {
            logMessageAndThrowOidcServerException(e, "ERROR_PERFORMING_OAUTH_STORE_UPDATE_CLIENT", newClient.getClientId());
        }

        return returnClient;
    }

    /** {@inheritDoc} */
    @Override
    public OidcBaseClient get(String clientIdentifier) throws OidcServerException {
        // TODO: Add validation code.
        OidcBaseClient client = null;
        try {
            client = createClient(oauthStore.readClient(componentId, clientIdentifier));
        } catch (OAuthStoreException e) {
            logMessageAndThrowOidcServerException(e, "ERROR_PERFORMING_OAUTH_STORE_READ_CLIENT", clientIdentifier);
        }
        return client;
    }

    /**
     * Get the client from the backend, leaving in the hash algorithm type and salt. Do not
     * return this to the client, this if for validation on the server side only.
     *
     * @param clientIdentifier
     * @return
     * @throws OidcServerException
     */
    private OidcBaseClient getInternal(String clientIdentifier) throws OidcServerException {
        OidcBaseClient client = null;
        try {
            client = createClient(oauthStore.readClient(componentId, clientIdentifier), false);
        } catch (OAuthStoreException e) {
            logMessageAndThrowOidcServerException(e, "ERROR_PERFORMING_OAUTH_STORE_READ_CLIENT", clientIdentifier);
        }
        return client;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<OidcBaseClient> getAll() throws OidcServerException {
        return getAll(null);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<OidcBaseClient> getAll(HttpServletRequest request) throws OidcServerException {
        Collection<OidcBaseClient> clients = new ArrayList<OidcBaseClient>();
        String attribute = ""; // TODO: Determine what the attribute will be
        Collection<OAuthClient> clientsFromStore;

        try {
            clientsFromStore = oauthStore.readAllClients(componentId, attribute);
            if (clientsFromStore != null) {
                for (OAuthClient element : clientsFromStore) {
                    OidcBaseClient client = createClient(element);
                    if (request != null) {
                        RegistrationEndpointServices.processClientRegistationUri(client, request);
                    }
                    clients.add(client);
                }
            }
        } catch (OAuthStoreException e) {
            logMessageAndThrowOidcServerException(e, "ERROR_PERFORMING_OAUTH_STORE_READ_ALL_CLIENTS");
        }

        return clients;
    }

    private OidcBaseClient createClient(OAuthClient oauthClient) {
        return createClient(oauthClient, true);
    }

    private OidcBaseClient createClient(OAuthClient oauthClient, boolean removeHashInfoFromMetadata) {
        OidcBaseClient client = null;
        if (oauthClient != null) {
            JsonObject clientAsJson = (new JsonParser()).parse(oauthClient.getClientMetadata()).getAsJsonObject();
            if (removeHashInfoFromMetadata) {
                clientAsJson.remove(OAuth20Constants.HASH_ALGORITHM);
                clientAsJson.remove(OAuth20Constants.SALT);
                clientAsJson.remove(OAuth20Constants.HASH_ITERATIONS);
                clientAsJson.remove(OAuth20Constants.HASH_LENGTH);
            }

            client = OidcOAuth20Util.GSON_RAW.fromJson(clientAsJson, OidcBaseClient.class);
            client.setComponentId(oauthClient.getProviderId());
            client.setEnabled(oauthClient.isEnabled());
            String storedSecret = client.getClientSecret();
            if (storedSecret != null && !storedSecret.isEmpty()) {
                String secretType = PasswordUtil.getCryptoAlgorithm(storedSecret);
                /*
                 * Originally, the secret was stored XOR, but now we're storing new secrets as a oneway hash.
                 * Only decode if it's still an XORd String
                 */
                if (secretType == null || secretType.equals(OAuth20Constants.XOR)) { // type is null on plain text, handled by passwordDecode
                    client.setClientSecret(PasswordUtil.passwordDecode(storedSecret));
                }
            }
            client = OidcBaseClientValidator.getInstance(client).setDefaultsForOmitted();
        }
        return client;
    }

    /** {@inheritDoc} */
    @Override
    public boolean exists(String clientIdentifier) throws OidcServerException {
        boolean exists = false;
        try {
            exists = oauthStore.readClient(componentId, clientIdentifier) != null;
        } catch (OAuthStoreException e) {
            logMessageAndThrowOidcServerException(e, "ERROR_PERFORMING_OAUTH_STORE_READ_CLIENT", clientIdentifier);
        }
        return exists;
    }

    /** {@inheritDoc} */
    @Override
    public boolean validateClient(String clientIdentifier, @Sensitive String clientSecret) throws OidcServerException {
        boolean result = false;

        if (clientSecret != null && !clientSecret.trim().isEmpty()) {
            OidcBaseClient client = getInternal(clientIdentifier);

            if (client != null && client.isConfidential()) {
                String storedSecret = client.getClientSecret();
                boolean update = updateXORtoHash;
                String secretType = PasswordUtil.getCryptoAlgorithm(storedSecret);
                if (secretType != null && secretType.equals(OAuth20Constants.HASH)) {
                    update = false; // already hashed, don't need to update
                    clientSecret = HashSecretUtils.hashSecret(clientSecret, clientIdentifier, false, client.getSalt(), client.getAlgorithm(), client.getIterations(), client.getLength());
                }

                if (storedSecret != null && storedSecret.equals(clientSecret)) {
                    result = true;

                    /*
                     * Optionally update the client secret from XOR to Hash
                     */
                    if (update && !isXORConfigured()) { // convert XOR to Hash
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Converting client secret for " + clientIdentifier + " to hash during a validateClient request");
                        }
                        update(client);
                    }
                }
            }
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean delete(String clientIdentifier) throws OidcServerException {
        boolean deleted = false;
        try {
            oauthStore.deleteClient(componentId, clientIdentifier);
            deleted = true;
        } catch (OAuthStoreException e) {
            logMessageAndThrowOidcServerException(e, "ERROR_PERFORMING_OAUTH_STORE_DELETE_CLIENT", clientIdentifier);
        }
        return deleted;
    }

    /**
     * TODO: Refactor with same method from CachedDBOidcClientProvider
     * Logs the exception message, logs an error message in the server log, and throws an OidcServerException using the message
     * key and message arguments provided.
     */
    void logMessageAndThrowOidcServerException(OAuthStoreException e, String msgKey, Object... msgArgs) throws OidcServerException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "There was a problem invoking the OAuthStore", e.getMessage(), e);
        }

        // The caught Exception message might contain sensitive information about database operations.
        // Log the message with the caught Exception message, but don't include the Exception message in the new exception that gets thrown.
        Object[] updatedMsgArgs = appendStringMessageToArgs(e.getLocalizedMessage(), msgArgs);

        if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled()) {
            Tr.error(tc, msgKey, updatedMsgArgs);
        }

        // Use an empty string as the last insert to avoid an unused insert (e.g. "{2}") showing up in the exception message
        updatedMsgArgs = appendStringMessageToArgs("", msgArgs);
        throw new OidcServerException(new BrowserAndServerLogMessage(tc, msgKey, updatedMsgArgs), OIDCConstants.ERROR_SERVER_ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);

    }

    Object[] appendStringMessageToArgs(String additionalInsert, Object... msgArgs) {
        Object[] updatedMsgArgs = new Object[1];
        if (msgArgs != null) {
            updatedMsgArgs = new Object[msgArgs.length + 1];
            for (int i = 0; i < msgArgs.length; i++) {
                updatedMsgArgs[i] = msgArgs[i];
            }
        }
        updatedMsgArgs[updatedMsgArgs.length - 1] = additionalInsert;
        return updatedMsgArgs;
    }

    private boolean isPBKDF2WithHmacSHA512Configured() {
        return hashType.equals(HashSecretUtils.PBKDF2WithHmacSHA512);
    }

    private boolean isXORConfigured() {
        return hashType.equals(OAuth20Constants.XOR);
    }
}
