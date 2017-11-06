/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.wim.adapter.urbridge.URBridge;
import com.ibm.wsspi.security.wim.exception.InitializationException;

/**
 * {@link RepositoryWrapper} implementation that wraps a {@link UserRegistry} contained within a {@link URBridge} instance.
 */
class UserRegistryWrapper implements RepositoryWrapper {

    private static final String BASE_ENTRY = "registryBaseEntry";
    private static final String KEY_REGISTRY = "userRegistry";
    private final Map<String, String> baseEntries;
    private final String baseEntry;
    private final URBridge urBridge;

    public UserRegistryWrapper(UserRegistry ur, ConfigManager configManager) throws InitializationException {
        String realm = ur.getRealm();
        this.baseEntry = "o=" + realm;
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(KEY_REGISTRY, ur);
        properties.put(VMMService.KEY_ID, realm);
        properties.put(BASE_ENTRY, baseEntry);
        baseEntries = Collections.singletonMap(baseEntry, realm);

        urBridge = new URBridge(properties, ur, configManager);
    }

    @Override
    public void clear() {
        /* Do nothing. */
    }

    @Override
    public Repository getRepository() {
        return urBridge;
    }

    @Override
    public Map<String, String> getRepositoryBaseEntries() {
        //TODO not clear what value should be????
        return baseEntries;
    }

    @Override
    public Set<String> getRepositoryGroups() {
        return Collections.singleton(urBridge.getRealm());
    }

    @Override
    public int isUniqueNameForRepository(String uniqueName, boolean isDn) {
        if (baseEntry.equals(uniqueName))
            return Integer.MAX_VALUE;
        if (urBridge.isEntityInRealm(uniqueName))
            return 0;
        return -1;
    }
}
