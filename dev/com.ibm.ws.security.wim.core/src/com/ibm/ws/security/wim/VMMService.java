/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.wim.Service;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.ws.bnd.metatype.annotation.Ext;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryChangeListener;
import com.ibm.wsspi.security.wim.CustomRepository;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Root;

@ObjectClassDefinition(factoryPid = "com.ibm.wsspi.security.wim.CustomRepository", name = Ext.INTERNAL, description = Ext.INTERNAL_DESC, localization = Ext.LOCALIZATION)
@Ext.Alias("customRepository")
@Ext.ObjectClassClass(CustomRepository.class)
@interface CustomRepositoryMarker {}

@ObjectClassDefinition(pid = "com.ibm.ws.security.wim.VMMService", name = Ext.INTERNAL, description = Ext.INTERNAL_DESC, localization = Ext.LOCALIZATION)
@interface VMMServiceConfig {

    //included only to force the need for a server.xml element to create the configuration.
    @AttributeDefinition(name = Ext.INTERNAL, description = Ext.INTERNAL_DESC)
    String name();

    @AttributeDefinition(name = Ext.INTERNAL, description = Ext.INTERNAL_DESC, defaultValue = "*")
    @Ext.Service("com.ibm.wsspi.security.wim.CustomRepository")
    @Ext.Final
    String[] customRepository();

    @AttributeDefinition(name = Ext.INTERNAL, description = Ext.INTERNAL_DESC, defaultValue = "${count(customRepository)}")
    String CustomRepository_cardinality_minimum();

    @AttributeDefinition(name = Ext.INTERNAL, description = Ext.INTERNAL_DESC, defaultValue = "*")
    @Ext.Service("com.ibm.ws.security.wim.ConfiguredRepository")
    @Ext.Final
    String[] configuredRepository();

    @AttributeDefinition(name = Ext.INTERNAL, description = Ext.INTERNAL_DESC, defaultValue = "${count(configuredRepository)}")
    String ConfiguredRepository_cardinality_minimum();

}

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = { VMMService.class },
           property = "service.vendor=IBM")
public class VMMService implements Service, RealmConfigChangeListener {

    private static final TraceComponent tc = Tr.register(VMMService.class);

    static final String KEY_FACTORY = "Factory";
    static final String KEY_CONFIGURATION = "Configuration";
    static final String KEY_TYPE = "com.ibm.ws.security.wim.repository.type";
    static final String KEY_ID = "config.id";

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private volatile List<UserRegistryChangeListener> listeners = Collections.emptyList();

    @Reference
    ConfigManager configMgr;

    private Set<String> registryRealmNames = null;

    private ProfileManager profileManager = null;
    private RepositoryManager repositoryManager = null;

    public VMMService() {
        repositoryManager = new RepositoryManager(this);
        profileManager = new ProfileManager(repositoryManager);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void setConfiguredRepository(ConfiguredRepository configuredRepository, Map<String, Object> props) {
        String repositoryId = (String) props.get(KEY_ID);
        repositoryManager.addConfiguredRepository(repositoryId, configuredRepository);
        notifyListeners();
    }

    protected void updatedConfiguredRepository(ConfiguredRepository configuredRepository) {
        notifyListeners();
    }

    protected void unsetConfiguredRepository(Map<String, Object> props) {
        String repositoryId = (String) props.get(KEY_ID);
        repositoryManager.removeRepositoryHolder(repositoryId);
        notifyListeners();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void setCustomRepository(CustomRepository customRepository, Map<String, Object> props) {
        String repositoryId = (String) props.get(KEY_ID);
        repositoryManager.addCustomRepository(repositoryId, customRepository);
        notifyListeners();
    }

    protected void updatedCustomRepository(CustomRepository customRepository) {
        notifyListeners();
    }

    protected void unsetCustomRepository(Map<String, Object> props) {
        String repositoryId = (String) props.get(KEY_ID);
        repositoryManager.removeRepositoryHolder(repositoryId);
        notifyListeners();
    }

    ConfigManager getConfigManager() {
        return configMgr;
    }

    @Activate
    protected void activate(ComponentContext cc) {
        profileManager.setConfigManager(configMgr);

        // Register for realm configuration change callback
        getConfigManager().registerRealmConfigChangeListener(this);

        // Verify participating base entry
        //this is totally bogus without forcing all repositories to be registered using a minimum cardinality.
        //Presumably this would break the current ability to have misconfigured repositories/adapters that can't possibly start.
        //The problem is that repositories can be registered at any time after the VMMService starts, whereas  the configmanager is
        //configured directly with the other half of the info used in this check.
//        verifyParticipatingBaseEntries();

        // Activate and notify of repository change to all listeners
//        UserRegistryService URService = securityService.getUserRegistryService();
//        if (URService != null)
//            URService.notifyRepositoryChange();
        notifyListeners();
        if (tc.isInfoEnabled())
            Tr.info(tc, "FEDERATED_MANAGER_SERVICE_READY");
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {

        if (tc.isInfoEnabled())
            Tr.info(tc, "FEDERATED_MANAGER_SERVICE_STOPPED");
        notifyListeners();
    }

    /**
     * Notify the registered listeners of the change to the UserRegistry
     * configuration.
     */
    private void notifyListeners() {
        for (UserRegistryChangeListener listener : listeners) {
            listener.notifyOfUserRegistryChange();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.security.wim.SchemaService#createRootObject()
     */
    @Override
    public Root createRootObject() throws WIMException {
        final String METHODNAME = "createRootObject";

        Root result = null;
        result = new Root();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " " + result);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.security.wim.ProfileServiceLite#get(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root get(Root root) throws WIMException {
        final String METHODNAME = "get";
        Root result = null;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " " + root);
        }
        result = profileManager.get(root);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " " + result);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.security.wim.ProfileServiceLite#search(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root search(Root root) throws WIMException {
        final String METHODNAME = "search";
        Root result = null;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " " + root);
        }
        result = profileManager.search(root);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " " + result);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.security.wim.ProfileServiceLite#login(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root login(Root root) throws WIMException {
        final String METHODNAME = "login";
        Root result = null;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " " + root);
        }
        result = profileManager.login(root);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " " + result);
        }
        return result;
    }

    public String getRealmName() {
        return profileManager.getDefaultRealmName();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.wim.RealmConfigChangeListener#notifyRealmConfigChange()
     */
    @Override
    public void notifyRealmConfigChange() {
        // Verify that all participating base entries are present in some repository.
        verifyParticipatingBaseEntries();
    }

    /**
     *
     */
    private void verifyParticipatingBaseEntries() {
        Map<String, List<String>> repositoryBaseEntrymap = repositoryManager.getRepositoriesBaseEntries();
        Set<String> baseEntries = new HashSet<String>();
        for (List<String> entries : repositoryBaseEntrymap.values()) {
            baseEntries.addAll(entries);
        }

        // Iterate over all configured realms and for each realm verify the participating base entry
        // against repository base entries.
        Set<String> realmNames = configMgr.getConfiguredRealmNames();

        if (realmNames != null) {
            for (String realmName : realmNames) {
                RealmConfig config = configMgr.getRealmConfig(realmName);
                String[] participatingEntries = config.getParticipatingBaseEntries();

                if (participatingEntries != null) {
                    for (String baseEntry : participatingEntries) {
                        if (!baseEntries.contains(baseEntry))
                            Tr.error(tc, WIMMessageKey.INVALID_PARTICIPATING_BASE_ENTRY_DEFINITION, baseEntry);
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.security.wim.ProfileServiceLite#delete(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root delete(Root root) throws WIMException {
        final String METHODNAME = "delete";
        Root result = null;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " " + root);
        }
        result = profileManager.delete(root);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " " + result);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.security.wim.ProfileServiceLite#create(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root create(Root root) throws WIMException {
        final String METHODNAME = "create";
        Root result = null;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " " + root);
        }
        result = profileManager.create(root);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " " + result);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.security.wim.ProfileServiceLite#update(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root update(Root root) throws WIMException {
        return profileManager.update(root);
    }

    /**
     * @param registries
     */
    public void addFederationRegistries(List<UserRegistry> registries) {
        if (registryRealmNames == null)
            registryRealmNames = new HashSet<String>();

        for (UserRegistry ur : registries) {
            registryRealmNames.add(ur.getRealm());
            repositoryManager.addUserRegistry(ur);
        }
        configMgr.processConfig();
    }

    /**
     * @param registry
     */
    public void removeAllFederatedRegistries() {
        if (registryRealmNames != null) {
            for (String realm : registryRealmNames) {
                repositoryManager.removeRepositoryHolder(realm);
            }
            registryRealmNames.clear();
        }

        repositoryManager.clearAllCachedURRepositories();
    }

}
