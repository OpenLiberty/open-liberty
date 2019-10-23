/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMMessageHelper;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.ws.config.xml.internal.nester.Nester;
import com.ibm.ws.security.wim.util.StringUtil;
import com.ibm.ws.security.wim.util.UniqueNameHelper;
import com.ibm.wsspi.security.wim.SchemaConstants;
import com.ibm.wsspi.security.wim.exception.InvalidArgumentException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Group;
import com.ibm.wsspi.security.wim.model.PersonAccount;

/**
 * Manage and retrieve configuration information for core and realm configuration
 */

@Component(configurationPid = "com.ibm.ws.security.wim.core.config",
           configurationPolicy = ConfigurationPolicy.OPTIONAL,
           service = { ConfigManager.class },
           property = { "service.vendor=IBM", "com.ibm.ws.security.registry.type=WIM", "config.id=default-WIM" })
public class ConfigManager {

    private static final TraceComponent tc = Tr.register(ConfigManager.class);

    public static final String SUPPORTED_ENTITY_TYPE = "supportedEntityType";
    public static final String MAX_SEARCH_RESULTS = "maxSearchResults";
    public static final String SEARCH_TIME_OUT = "searchTimeout";
    public static final String RDN_PROPERTY = "rdnProperty";
    public static final String ENTITY_TYPE_NAME = "name";
    public static final String PRIMARY_REALM = "primaryRealm";
    public static final String REALM = "realm";
    public static final String REALM_NAME = "name";
    static final String KEY_CONFIG_ADMIN = "configurationAdmin";
    public static final String ENTITY_NAME = "name";
    public static final String DEFAULT_PARENT = "defaultParent";

    public static final String EXTENDED_PROPERTY = "extendedProperty";
    public static final String PROPERTY_NAME = "name";
    public static final String DATA_TYPE = "dataType";
    public static final String MULTI_VALUED = "multiValued";
    public static final String ENTITY_TYPE_NAMES = "entityType";
    public static final String DEFAULT_VALUE = "defaultValue";
    public static final String DEFAULT_ATTRIBUTE = "defaultAttribute";

    public static final Integer DEFAULT_MAX_SEARCH_RESULTS = new Integer(4500);

    public static final Integer DEFAULT_SEARCH_TIMEOUT = new Integer(600000);

    private static final Object PAGE_CACHE_SIZE = "pageCacheSize";

    private static final Integer DEFAULT_PAGE_CACHE_SIZE = new Integer(1000);

    private static final Object PAGE_CACHE_TIMEOUT = "pageCacheTimeout";

    private static final Long DEFAULT_PAGE_CACHE_TIMEOUT = new Long(30000);

    private volatile Map<String, Object> originalConfig;
    private volatile Map<String, Object> config;

    private volatile Map<String, RealmConfig> realmNameToRealmConfigMap;

    private volatile Map<String, SupportedEntityConfig> entityTypeMap;

    private final ArrayList<RealmConfigChangeListener> listeners = new ArrayList<RealmConfigChangeListener>();

    /**
     * Map to store the default RDN Mappings
     */
    private Map<String, Map<String, String[]>> defaultEntityMap = null;

    private RepositoryManager repositoryManager = null;

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> properties) {
        this.originalConfig = properties;
        config = processConfig();
    }

    @Modified
    protected void modify(ComponentContext cc, Map<String, Object> newProperties) {
        this.originalConfig = newProperties;
        config = processConfig();
        if (listeners != null) {
            for (RealmConfigChangeListener listener : listeners)
                listener.notifyRealmConfigChange();
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext cc,
                              Map<String, Object> newProperties) {
        originalConfig = null;
        config = null;
        listeners.clear();
    }

    public Map<String, Object> getConfigurationProperties() {
        return originalConfig;
    }

    public int getMaxSearchResults() {
        if (config.get(MAX_SEARCH_RESULTS) != null)
            return (Integer) config.get(MAX_SEARCH_RESULTS);
        else
            return DEFAULT_MAX_SEARCH_RESULTS;
    }

    public int getSearchTimeOut() {
        if (config.get(SEARCH_TIME_OUT) != null) {
            long val = Long.parseLong(String.valueOf(config.get(SEARCH_TIME_OUT)));
            return (int) val;
            //return (Integer) config.get(SEARCH_TIME_OUT);
        } else
            return DEFAULT_SEARCH_TIMEOUT;
    }

    public String[] getRDNProperties(String entityTypeName) {
        SupportedEntityConfig entityConfig = entityTypeMap.get(entityTypeName);
        if (entityConfig != null) {
            return entityConfig.getRdnProperties();
        }
        return null;
    }

    public String[] getDefaultRDNProperties(String typeName) {
        String[] rdnProps = null;

        if (defaultEntityMap == null)
            initializeDefaultEntityMap();

        Map<String, String[]> specificEntityMap = defaultEntityMap.get(typeName);
        if (specificEntityMap != null) {
            rdnProps = specificEntityMap.get(RDN_PROPERTY);
        }
        return rdnProps;
    }

    /**
     *
     */
    private void initializeDefaultEntityMap() {
        if (defaultEntityMap != null)
            return;

        defaultEntityMap = new HashMap<String, Map<String, String[]>>();

        // PersonAccount
        {
            Map<String, String[]> rdn = new HashMap<String, String[]>();
            String[] rdns = { "uid" };
            rdn.put(RDN_PROPERTY, rdns);
            defaultEntityMap.put(SchemaConstants.DO_PERSON_ACCOUNT, rdn);
        }

        // Group
        {
            Map<String, String[]> rdn = new HashMap<String, String[]>();
            String[] rdns = { "cn" };
            rdn.put(RDN_PROPERTY, rdns);
            defaultEntityMap.put(SchemaConstants.DO_GROUP, rdn);
        }
    }

    //TODO see if you can change this to just return Set
    public List<String> getSupportedEntityTypes() {
        return new ArrayList<String>(entityTypeMap.keySet());
    }

    Map<String, Object> processConfig() {
        Map<String, Object> properties = this.originalConfig;
        Map<String, Object> updatedConfig = new HashMap<String, Object>(properties);
        Map<String, List<Map<String, Object>>> nested = Nester.nest(properties, SUPPORTED_ENTITY_TYPE, REALM, PRIMARY_REALM, EXTENDED_PROPERTY);

        Map<String, SupportedEntityConfig> entityTypeMap = new HashMap<String, SupportedEntityConfig>();
        List<Map<String, Object>> supportedEntityTypes = nested.get(SUPPORTED_ENTITY_TYPE);
        for (Map<String, Object> supportedEntityType : supportedEntityTypes) {
            String entityTypeName = ((String[]) supportedEntityType.get(ENTITY_TYPE_NAME))[0];
            String defaultParent = ((String[]) supportedEntityType.get(DEFAULT_PARENT))[0];
            String[] rdnProperties = (String[]) supportedEntityType.get(RDN_PROPERTY);
            entityTypeMap.put(entityTypeName, new SupportedEntityConfig(defaultParent, rdnProperties));
        }
        this.entityTypeMap = entityTypeMap;

        // Handle realm Config
        Map<String, RealmConfig> realmMap = new HashMap<String, RealmConfig>();
        List<Map<String, Object>> realms = nested.get(REALM);
        for (Map<String, Object> realm : realms) {
            String realmName = (String) realm.get(REALM_NAME);
            RealmConfig realmCfg = new RealmConfig(realm, false);
            realmMap.put(realmName, realmCfg);

        }
        // handle primaryRealm
        List<Map<String, Object>> primaryRealmList = nested.get(PRIMARY_REALM);
        if (!primaryRealmList.isEmpty()) {

            Map<String, Object> primaryRealm = primaryRealmList.get(0);
            RealmConfig realmCfg = new RealmConfig(primaryRealm, true);
            String realmName = (String) primaryRealm.get(REALM_NAME);
            realmMap.put(realmName, realmCfg);

        }
        realmNameToRealmConfigMap = realmMap;

        // Process schema extension
        PersonAccount.clearExtendedProperties();
        Group.clearExtendedProperties();
        List<Map<String, Object>> extendedProperties = nested.get(EXTENDED_PROPERTY);
        if (!extendedProperties.isEmpty()) {
            for (Map<String, Object> property : extendedProperties) {
                String propertyName = (String) property.get(PROPERTY_NAME);
                String dataType = (String) property.get(DATA_TYPE);
                String entityTypeName = (String) property.get(ENTITY_TYPE_NAMES);
                String defaultValue = (String) property.get(DEFAULT_VALUE);
                // String defaultAttribute = (String) Property.get(DEFAULT_ATTRIBUTE);
                Boolean multiValued = (Boolean) property.get(MULTI_VALUED);
                if (entityTypeName.equalsIgnoreCase("PersonAccount")) {
                    PersonAccount.addExtendedProperty(propertyName, dataType, multiValued, defaultValue/* , defaultAttribute */);
                } else if (entityTypeName.equalsIgnoreCase("Group")) {
                    Group.addExtendedProperty(propertyName, dataType, multiValued, defaultValue/* , defaultAttribute */);
                } else {
                    if (tc.isWarningEnabled()) {
                        Tr.warning(tc, WIMMessageKey.ENTITY_TYPE_NOT_SUPPORTED, entityTypeName);
                    }
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "The extended property definition for " + entityTypeName
                                     + " is ignored as this is either an unknown entity type or extension is not supported for it.");
                    }
                }
            }

            Group.reInitializePropertyNames();
            PersonAccount.reInitializePropertyNames();
        }
        return updatedConfig;
    }

    @Trivial
    public RealmConfig getRealmConfig(String realmName) {
        if (realmName != null)
            return realmNameToRealmConfigMap.get(realmName);
        else
            return null;
    }

    public boolean isAllowOpIfRepoDown(String realmName) {
        boolean isAllowOpIfRepoDown = false;
        if (realmName != null && getRealmConfig(realmName) != null) {
            isAllowOpIfRepoDown = getRealmConfig(realmName).isAllowOpIfRepoDown();
        }
        return isAllowOpIfRepoDown;
    }

    /**
     * Get the configured primary realm name. This will return a value only if there is a 'primaryRealm'
     * configured.
     *
     * @return The configured primary realm name, if one is configured. Otherwise; null.
     */
    public String getConfiguredPrimaryRealmName() {
        String defaultRealmName = null;
        if (realmNameToRealmConfigMap != null) {
            for (RealmConfig realmCfg : realmNameToRealmConfigMap.values()) {
                if (realmCfg.isDefaultRealm()) {
                    defaultRealmName = realmCfg.getName();
                    break;
                }
            }

        }

        return defaultRealmName;
    }

    @Trivial
    public RealmConfig getDefaultRealmConfig() {
        return getRealmConfig(getConfiguredPrimaryRealmName());
    }

    public boolean isUniqueNameInRealm(String uniqueName, String realmName) throws InvalidArgumentException {

        RealmConfig realmConfig = getRealmConfig(realmName);
        boolean inRealm = false;
        //To handle Liberty UR realm as default realm
        if (realmName == null || realmNameToRealmConfigMap.size() == 0) {
            inRealm = true;
        } else if (realmConfig != null && realmConfig.getParticipatingBaseEntries() != null) {
            validateRealmName(realmName);

            if (uniqueName != null) {
                uniqueName = UniqueNameHelper.getValidUniqueName(uniqueName);
                String[] baseEntryNames = realmConfig.getParticipatingBaseEntries();
                if (baseEntryNames != null) {
                    for (int i = 0; i < baseEntryNames.length && !inRealm; i++) {
                        String baseEntry = baseEntryNames[i];
                        if (StringUtil.endsWithIgnoreCase(uniqueName, baseEntry)) {
                            inRealm = true;
                        }
                    }
                }
            }
        }
        return inRealm;
    }

    private void validateRealmName(String realmName) throws InvalidArgumentException {
        Set<?> realms = getRealmNames();
        if (realmName != null && realms != null && !realms.contains(realmName)) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.INVALID_REALM_NAME, WIMMessageHelper.generateMsgParms(realmName));
            throw new InvalidArgumentException(WIMMessageKey.INVALID_REALM_NAME, msg);
        }
    }

    public String getDefaultDelimiter() {
        String delimiter = "/";
        if (getDefaultRealmConfig() != null)
            delimiter = getDefaultRealmConfig().getDelimiter();
        return delimiter;
    }

    public String getDelimiter(String realmName) {
        String delimiter = getDefaultDelimiter();
        if (realmName != null && getRealmConfig(realmName) != null)
            delimiter = getRealmConfig(realmName).getDelimiter();
        return delimiter;

    }

    /**
     * Get all the realm names. This will return a set of one of the following either:
     *
     * <ol>
     * <li>The configured realm names.</li>
     * <li>The first federated repository's realm name.</li>
     * <li>The default realm name.</li>
     * </ol>
     *
     * @return The set of valid realm names.
     * @see #getConfiguredRealmNames()
     */
    public Set<String> getRealmNames() {

        Set<String> results = null;

        /*
         * Were there any realms defined?
         */
        Set<String> configuredRealmNames = getConfiguredRealmNames();
        if (configuredRealmNames != null && !configuredRealmNames.isEmpty()) {
            results = configuredRealmNames;
        }

        /*
         * If no realms were defined, then use the first federated repository's realm name.
         */
        if (results == null) {
            String realmName = null;
            List<String> repoIds = repositoryManager.getRepoIds();
            if (repoIds != null && !repoIds.isEmpty()) {
                Repository repository = repositoryManager.getRepository(repoIds.get(0));
                if (repository != null) {
                    realmName = repository.getRealm();
                }
            }

            if (realmName != null) {
                results = new HashSet<String>(Arrays.asList(new String[] { realmName }));
            }
        }

        /*
         * If we still have no realms, use the default realm name.
         */
        if (results == null) {
            results = new HashSet<String>(Arrays.asList(new String[] { ProfileManager.DEFAULT_REALM_NAME }));
        }

        return results;
    }

    /**
     * Get only the configured realm names.
     *
     * @return The set of valid realm names.
     * @see #getRealmNames()
     */
    public Set<String> getConfiguredRealmNames() {
        return Collections.unmodifiableSet(realmNameToRealmConfigMap.keySet());
    }

    public void registerRealmConfigChangeListener(RealmConfigChangeListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    /**
     * Gets the default parent node that is for the specified entity type in specified realm.
     *
     * @param entType The entity type. e.g. Person, Group...
     * @param realmName The name of the realm
     * @return The default parent node.
     */
    public String getDefaultParentForEntityInRealm(String entType, String realmName) throws WIMException {
        String defaultParent = getDefaultParent(entType);
        if (realmName != null) {
            validateRealmName(realmName);
            String parent = null;
            RealmConfig realmConfig = getRealmConfig(realmName);
            if (realmConfig != null) {
                Map<String, String> defaultParentsMap = realmConfig.getDefaultParentMapping();
                if (defaultParentsMap != null) {
                    parent = defaultParentsMap.get(entType);
                    if (parent != null) {
                        defaultParent = parent;
                    }
                }
                if (parent == null && !isUniqueNameInRealm(defaultParent, realmName)) {
                    defaultParent = null;
                }
            }
        }
        return defaultParent;
    }

    /**
     * Returns the default parent for the given prefixed entity type.
     * Entity types under WIM package should not have any name space prefix. For example, "Person".
     *
     * @param entityTypeName The prefixed entity type.
     * @return The unique name of the default parent of this entity type. If the entity type is not supported, null will be returned.
     */
    public String getDefaultParent(String entityTypeName) {
        SupportedEntityConfig entityConfig = entityTypeMap.get(entityTypeName);
        if (entityConfig != null) {
            return entityConfig.getDefaultParent();
        }
        return null;
    }

    /**
     * @return
     */
    public int getPageCacheSize() {
        if (config.get(PAGE_CACHE_SIZE) != null)
            return (Integer) config.get(PAGE_CACHE_SIZE);
        else
            return DEFAULT_PAGE_CACHE_SIZE;
    }

    /**
     * @return
     */
    public long getPageCacheTimeOut() {
        if (config.get(PAGE_CACHE_TIMEOUT) != null)
            return (Long) config.get(PAGE_CACHE_TIMEOUT);
        else
            return DEFAULT_PAGE_CACHE_TIMEOUT;
    }

    /**
     * Set the {@link RepositoryManager} instance.
     *
     * @param repositoryManager The {@link RepositoryManager} instance.
     */
    void setRepositoryManager(RepositoryManager repositoryManager) {
        this.repositoryManager = repositoryManager;
    }
}
