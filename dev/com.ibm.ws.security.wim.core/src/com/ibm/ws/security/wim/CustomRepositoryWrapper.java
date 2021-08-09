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

import java.util.List;
import java.util.Map;

import com.ibm.ws.security.wim.util.SchemaConstantsInternal;
import com.ibm.wsspi.security.wim.CustomRepository;
import com.ibm.wsspi.security.wim.SchemaConstants;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Control;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.IdentifierType;
import com.ibm.wsspi.security.wim.model.PropertyControl;
import com.ibm.wsspi.security.wim.model.Root;

/**
 * {@link RepositoryWrapper} implementation that wraps a {@link CustomRepository}.
 */
class CustomRepositoryWrapper extends AbstractRepositoryWrapper {

    private final CustomRepositoryAdapter repository;

    /**
     * Construct a new {@link CustomRepositoryWrapper} instance.
     *
     * @param repositoryId The repository ID.
     * @param customRepository The {@link CustomRepository} to wrap.
     */
    public CustomRepositoryWrapper(String repositoryId, CustomRepository customRepository) {
        super(repositoryId);
        this.repository = new CustomRepositoryAdapter(repositoryId, customRepository);
    }

    @Override
    public Repository getRepository() {
        return repository;
    }

    @Override
    protected RepositoryConfig getRepositoryConfig() {
        return repository;
    }

    /**
     * This class will map the {@link CustomRepository} to the {@link Repository} interface.
     */
    private static class CustomRepositoryAdapter implements Repository, RepositoryConfig {
        private final CustomRepository customRepository;
        private final String repositoryId;

        /**
         * Construct a new {@link CustomRepositoryAdapter} instance.
         *
         * @param repositoryId The repository ID.
         * @param customRepository The {@link CustomRepository} to wrap.
         */
        public CustomRepositoryAdapter(String repositoryId, CustomRepository customRepository) {
            this.repositoryId = repositoryId;
            this.customRepository = customRepository;
        }

        @Override
        public Root create(Root root) throws WIMException {
            return setRepositoryId(customRepository.create(root));
        }

        @Override
        public Root delete(Root root) throws WIMException {
            return setRepositoryId(customRepository.delete(root));
        }

        @Override
        public Root get(Root root) throws WIMException {

            /*
             * Replace internal properties so they don't make their way down to the CustomRepository.
             */
            for (Control control : root.getControls()) {
                if (control instanceof PropertyControl) {
                    PropertyControl pControl = (PropertyControl) control;
                    List<String> properties = pControl.getProperties();
                    if (properties != null && !properties.isEmpty()) {
                        if (properties.contains(SchemaConstantsInternal.PROP_DISPLAY_BRIDGE_PRINCIPAL_NAME)) {
                            properties.remove(SchemaConstantsInternal.PROP_DISPLAY_BRIDGE_PRINCIPAL_NAME);
                            properties.add(SchemaConstants.PROP_PRINCIPAL_NAME);
                        }
                        if (properties.contains(SchemaConstantsInternal.PROP_DISPLAY_BRIDGE_CN)) {
                            properties.remove(SchemaConstantsInternal.PROP_DISPLAY_BRIDGE_CN);
                            properties.add("cn");
                        }
                    }
                    break;
                }
            }

            return setRepositoryId(customRepository.get(root));
        }

        @Override
        public String getRealm() {
            return customRepository.getRealm();
        }

        @Override
        public String getReposId() {
            return repositoryId;
        }

        //TODO WHAT IS THIS SUPPOSED TO MEAN?
        @Override
        public String[] getRepositoriesForGroups() {
            String[] repos = customRepository.getRepositoriesForGroups();
            if (repos == null) {
                repos = new String[] { repositoryId };
            }
            return repos;
        }

        @Override
        public Map<String, String> getRepositoryBaseEntries() {
            return customRepository.getRepositoryBaseEntries();
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public Root login(Root root) throws WIMException {
            return setRepositoryId(customRepository.login(root));
        }

        @Override
        public void resetConfig() {
            //TODO: customRepository.resetConfig();
        }

        @Override
        public Root search(Root root) throws WIMException {
            return setRepositoryId(customRepository.search(root));
        }

        /**
         * For the {@link Root} object, set the repository ID on all {@link Entity}'s
         * {@link IdentifierType} instances.
         *
         * @param root The {@link Root} instance to set the repository IDs on.
         * @return The {@link Root} instance that was passed in, now with the repository IDs set.
         */
        private Root setRepositoryId(Root root) {
            for (Entity entity : root.getEntities()) {
                IdentifierType identifier = entity.getIdentifier();
                if (identifier != null) {
                    identifier.setRepositoryId(repositoryId);
                }
            }
            return root;
        }

        @Override
        public Root update(Root root) throws WIMException {
            return setRepositoryId(customRepository.update(root));
        }
    }
}
