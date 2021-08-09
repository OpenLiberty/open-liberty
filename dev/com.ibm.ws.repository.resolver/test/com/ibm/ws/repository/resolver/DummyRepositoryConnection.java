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
package com.ibm.ws.repository.resolver;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.repository.connections.internal.AbstractRepositoryConnection;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;
import com.ibm.ws.repository.transport.client.AbstractRepositoryClient;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;
import com.ibm.ws.repository.transport.exceptions.BadVersionException;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.Attachment;

/**
 * Dummy repository connection, useful for testing the resolver
 * <p>
 * NOTE: this class is limited in the extent to which it maches the semantics of a real repository. In particular, there's only one copy of an asset so don't retrieve something
 * from the repository and then change it.
 * <p>
 * Unfortunately, most of our tests do this all the time, but this class is still useful for testing the resolver.
 * <p>
 * SAFE USAGE:
 * <ol>
 * <li>Create the DummyRepositoryConnection</li>
 * <li>Add a bunch of resources to it</li>
 * <li>Read a bunch of resources from it</li>
 * <li>Don't modify anything or add any more resources at this point</li>
 * </ol>
 *
 */
@SuppressWarnings("restriction")
public class DummyRepositoryConnection extends AbstractRepositoryConnection {

    Map<String, Asset> assetMap = new HashMap<>();
    DummyRepositoryClient client = new DummyRepositoryClient();
    int nextId = 1;

    /** {@inheritDoc} */
    @Override
    public String getRepositoryLocation() {
        return "dummy location";
    }

    /** {@inheritDoc} */
    @Override
    public RepositoryReadableClient createClient() {
        return client;
    }

    public void addResource(RepositoryResource resource) {
        Asset asset = getAsset(resource);
        asset.set_id(Integer.toString(nextId));
        nextId++;
        assetMap.put(asset.get_id(), asset);
    }

    public void addResources(Collection<? extends RepositoryResource> resources) {
        for (RepositoryResource resource : resources) {
            addResource(resource);
        }
    }

    private Asset getAsset(RepositoryResource resource) {
        try {
            RepositoryResourceImpl impl = (RepositoryResourceImpl) resource;
            impl.updateGeneratedFields(false);
            Method m = RepositoryResourceImpl.class.getDeclaredMethod("getAsset");
            m.setAccessible(true);
            return (Asset) m.invoke(resource);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private class DummyRepositoryClient extends AbstractRepositoryClient {

        @Override
        public Asset getAsset(String assetId) throws IOException, BadVersionException, RequestFailureException {
            return assetMap.get(assetId);
        }

        @Override
        public Collection<Asset> getAllAssets() throws IOException, RequestFailureException {
            return assetMap.values();
        }

        @Override
        public InputStream getAttachment(Asset asset, Attachment attachment) throws IOException, BadVersionException, RequestFailureException {
            return null;
        }

        @Override
        public void checkRepositoryStatus() throws IOException, RequestFailureException {
            // Throw no exception -> repository ok
        }

    }

}
