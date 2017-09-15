/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.ws.config.xml.internal.nester.Nester;
import com.ibm.ws.security.SecurityService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Manage and retrieve configuration information for repository configuration
 */
public class BaseRepository implements RepositoryConfig {
    private static final TraceComponent tc = Tr.register(BaseRepository.class);

    static final String KEY_ID = "config.id";
    static final String BASE_ENTRY_NAME = "name";
    static final String BASE_ENTRY = "baseEntry";
    static final String REGISTRY_BASE_ENTRY = "registryBaseEntry";
    static final String BASE_DN = "baseDN";
    static final String REPOSITORY_FOR_GROUPS = "repositoriesForGroups";
    public static final String KEY_SECURITY_SERVICE = "securityService";

    protected volatile Map<String, Object> config;
    protected String reposId = null;
    private Map<String, String> baseEntryMap = new HashMap<String, String>();

    private boolean readOnly = false;

    private String[] repositoriesForGroups;

    private final AtomicServiceReference<SecurityService> securityServiceRef = new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);

    protected void setSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.setReference(reference);
    }

    protected void unsetSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.unsetReference(reference);
    }

    protected void activate(Map<String, Object> properties, ComponentContext cc) {
        config = properties;
        securityServiceRef.activate(cc);
        initConfig();
    }

    protected void modify(Map<String, Object> newProperties) {
        config = newProperties;
        resetConfig();
        initConfig();
    }

    protected void deactivate(int reason, ComponentContext cc) {
        config = null;
        resetConfig();
        securityServiceRef.deactivate(cc);
    }

    private void initConfig() {
        reposId = (String) config.get(KEY_ID);

        Map<String, List<Map<String, Object>>> configMap = Nester.nest(config, BASE_ENTRY,
                                                                       REGISTRY_BASE_ENTRY);

        initBaseEntry(configMap.get(BASE_ENTRY));
        initBaseEntry(configMap.get(REGISTRY_BASE_ENTRY));
        if (config.containsKey(BASE_DN)) {
            String baseDN = (String) config.get(BASE_DN);
            String name = (String) config.get(BASE_ENTRY_NAME);
            if (name == null || name.length() == 0) {
                baseEntryMap.put(baseDN, baseDN);
            } else {
                baseEntryMap.put(name, baseDN);
            }
        }
        // TODO handle other configuration elements of a repository like
        // readOnly, supportSorting,CustomProperties when implementing
        // adapters story

        //These are repository ids.
        repositoriesForGroups = (String[]) config.get(REPOSITORY_FOR_GROUPS); // not tested

        if (baseEntryMap.size() == 0) {
            Tr.error(tc, WIMMessageKey.MISSING_BASE_ENTRY, reposId);
        }

    }

    private void initBaseEntry(List<Map<String, Object>> baseEntryList) {
        for (Map<String, Object> baseEntry : baseEntryList) {
            String baseDN = (String) baseEntry.get(BASE_DN);
            String name = (String) baseEntry.get(BASE_ENTRY_NAME);
            if (name == null || name.length() == 0) {
                //TODO correct error?
                Tr.error(tc, WIMMessageKey.INVALID_BASE_ENTRY_DEFINITION, baseDN);
            } else {
                baseEntryMap.put(name, baseDN);
            }
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.wim.RepositoryConfig#isReadOnly()
     */
    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.wim.RepositoryConfig#resetConfig()
     */
    @Override
    public void resetConfig() {
        reposId = null;
        baseEntryMap = new HashMap<String, String>();
        repositoriesForGroups = null;
        readOnly = false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.wim.RepositoryConfig#getReposId()
     */
    @Override
    public String getReposId() {
        return reposId;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.wim.RepositoryConfig#getRepositoryBaseEntries()
     */
    @Override
    public Map<String, String> getRepositoryBaseEntries() {
        return baseEntryMap;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.wim.RepositoryConfig#getRepositoriesForGroups()
     */
    @Override
    public String[] getRepositoriesForGroups() {
        return repositoriesForGroups;
    }
}
