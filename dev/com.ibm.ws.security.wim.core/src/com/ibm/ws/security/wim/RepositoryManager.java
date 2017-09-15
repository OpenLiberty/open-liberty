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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.wim.ras.WIMMessageHelper;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.wim.util.UniqueNameHelper;
import com.ibm.wsspi.security.wim.CustomRepository;
import com.ibm.wsspi.security.wim.SchemaConstants;
import com.ibm.wsspi.security.wim.exception.InitializationException;
import com.ibm.wsspi.security.wim.exception.InvalidUniqueNameException;
import com.ibm.wsspi.security.wim.exception.WIMException;

/**
 * Single point of contact for core to interact with different repositories
 *
 */
public class RepositoryManager {
    public final static String CLASSNAME = RepositoryManager.class.getName();
    public static final String ACTION_READ = "READ";
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";

    private static final TraceComponent tc = Tr.register(RepositoryManager.class);

    private final VMMService vmmService;

    private final Map<String, RepositoryWrapper> repositories = new ConcurrentHashMap<String, RepositoryWrapper>();

    public RepositoryManager(VMMService service) {
        vmmService = service;
    }

    void addConfiguredRepository(String repositoryId, ConfiguredRepository configuredRepository) {
        RepositoryWrapper repositoryHolder = new ConfiguredRepositoryWrapper(repositoryId, configuredRepository);
        repositories.put(repositoryId, repositoryHolder);
    }

    void addCustomRepository(String repositoryId, CustomRepository customRepository) {
        RepositoryWrapper repositoryHolder = new CustomRepositoryWrapper(repositoryId, customRepository);
        repositories.put(repositoryId, repositoryHolder);
    }

    void addUserRegistry(UserRegistry userRegistry) {
        try {
            UserRegistryWrapper repositoryHolder = new UserRegistryWrapper(userRegistry, vmmService.getConfigManager());
            repositories.put(userRegistry.getRealm(), repositoryHolder);
        } catch (InitializationException e) {
            //TODO will occur on lookup when this is made lazy.
        }
    }

    void removeRepositoryHolder(String id) {
        RepositoryWrapper repositoryHolder = repositories.remove(id);
        if (repositoryHolder != null) {
            repositoryHolder.clear();
        }
    }

    /**
     * {@inheritDoc} This lookup is done for all registered RepositoryFactory
     * instances. Please note that this method does not use the configuration
     * data for the RepositoryService for this lookup.
     */
    public Repository getRepository(String instanceId) throws WIMException {
        RepositoryWrapper repositoryHolder = repositories.get(instanceId);
        if (repositoryHolder != null) {
            return repositoryHolder.getRepository();
        }
        return null;
//        ConcurrentServiceReferenceMap<String, RepositoryConfiguration> configs = vmmService.getRepositoryConfigurations();
//
//        if (tc.isDebugEnabled())
//            Tr.debug(tc, "Config = " + configs);
//        RepositoryConfig config = configs.getService(instanceId);
//
//        if (config != null) {
//            RepositoryFactory factory = getRepositoryFactory(config.getType()); //LATEST ISSUE HERE
//
//            // If the RepositoryConfiguration is a URRepositoryConfiguration, set the config manager
//            // if (config instanceof URRepositoryConfiguration)
//            //     ((URRepositoryConfiguration) config).setConfigManager(vmmService.getConfigManager());
//            Repository repos = config.getRepository(factory);
//            return repos;
//        } else {
//            Set<Object> userRegistries = vmmService.getUserRegistries();
//
//            Iterator<Object> URIterator = userRegistries.iterator();
//            while (URIterator.hasNext()) {
//                Object ur = URIterator.next();
//                String realm = getRealm(ur);
//
//                if (realm != null && realm.equals(instanceId)) {
//                    // Check if we have a cached instance.
//                    if (cachedRepository.containsKey(instanceId)) {
//                        return cachedRepository.get(instanceId);
//                    } else {
//                        Map<String, Object> properties = new HashMap<String, Object>();
//                        properties.put(KEY_REGISTRY, ur);
////                        properties.put(KEY_CONFIG_MANAGER, vmmService.getConfigManager());
//                        properties.put(VMMService.KEY_ID, realm);
//                        properties.put(BASE_ENTRY, "o=" + realm);
//
//                        Repository repos = new URBridge(properties, (UserRegistry) ur, vmmService.getConfigManager());
//                        cachedRepository.put(instanceId, repos);
//                        return repos;
//                    }
//                }
//            }
//        }
//        return null;
    }

    public Repository getTargetRepository(String uniqueName) throws WIMException {
        String reposId = getRepositoryIdByUniqueName(uniqueName);
        Repository repos = getRepository(reposId);
        return repos;
    }

    public String getRepositoryId(String uniqueName) throws WIMException {
        String reposId = getRepositoryIdByUniqueName(uniqueName);
        return reposId;
    }

    /**
     * Returns the id of the repository to which the uniqueName belongs to.
     *
     * @throws InvalidUniqueNameException
     */
    protected String getRepositoryIdByUniqueName(String uniqueName) throws WIMException {
        boolean isDn = UniqueNameHelper.isDN(uniqueName) != null;
        if (isDn)
            uniqueName = UniqueNameHelper.getValidUniqueName(uniqueName).trim();

        String repo = null;
        int repoMatch = -1;
        int bestMatch = -1;

        for (Map.Entry<String, RepositoryWrapper> entry : repositories.entrySet()) {
            repoMatch = entry.getValue().isUniqueNameForRepository(uniqueName, isDn);
            if (repoMatch == Integer.MAX_VALUE) {
                return entry.getKey();
            } else if (repoMatch > bestMatch) {
                repo = entry.getKey();
                bestMatch = repoMatch;
            }
        }
        if (repo != null) {
            return repo;
        }

        throw new InvalidUniqueNameException(WIMMessageKey.ENTITY_NOT_IN_REALM_SCOPE, Tr.formatMessage(
                                                                                                       tc,
                                                                                                       WIMMessageKey.ENTITY_NOT_IN_REALM_SCOPE,
                                                                                                       WIMMessageHelper.generateMsgParms(uniqueName, "defined")));
    }

    public Map<String, List<String>> getRepositoriesBaseEntries() {
        Map<String, List<String>> reposNodesMap = new HashMap<String, List<String>>();

        for (Map.Entry<String, RepositoryWrapper> entry : repositories.entrySet()) {
            reposNodesMap.put(entry.getKey(), new ArrayList<String>(entry.getValue().getRepositoryBaseEntries().keySet()));
        }
        return reposNodesMap;

//        ConcurrentServiceReferenceMap<String, RepositoryConfiguration> configs = vmmService.getRepositoryConfigurations();
//        Set<Object> userRegistries = vmmService.getUserRegistries();
//        if ((configs == null || configs.isEmpty()) && (userRegistries == null || userRegistries.isEmpty())) {
//            return reposNodesMap;
//        }
//
//        if (configs != null && !configs.isEmpty()) {
//            Iterator<RepositoryConfiguration> itr = configs.getServices();
//            while (itr.hasNext()) {
//                RepositoryConfig config = itr.next();
//                List<String> baseEntries = new ArrayList<String>();
//                baseEntries.addAll(config.getRepositoryBaseEntries().keySet());
//                reposNodesMap.put(config.getReposId(), baseEntries); //TODO handle null baseEntries
//            }
//        }
//
//        if (userRegistries != null && !userRegistries.isEmpty()) {
//            Iterator<Object> URIterator = userRegistries.iterator();
//            while (URIterator.hasNext()) {
//                Object ur = URIterator.next();
//                String realm = getRealm(ur);
//
//                if (realm != null) {
//                    String baseEntry = "o=" + realm;
//                    List<String> baseEntries = new ArrayList<String>();
//                    baseEntries.add(baseEntry);
//                    String reposId = realm;
//                    reposNodesMap.put(reposId, baseEntries);
//                }
//            }
//        }
//
//        return reposNodesMap;
    }

    public Map<String, String> getRepositoryBaseEntries(String reposId) throws WIMException {
        RepositoryWrapper repositoryHolder = repositories.get(reposId);
        if (repositoryHolder != null) {
            return repositoryHolder.getRepositoryBaseEntries();
        }
        return Collections.emptyMap();
//        List<String> baseEntries = new ArrayList<String>();
//
//        RepositoryConfig config = vmmService.getRepositoryConfigurations().getService(reposId);
//        if (config != null)
//            baseEntries.addAll(config.getRepositoryBaseEntries().keySet());
//        else {
//            Set<Object> userRegistries = vmmService.getUserRegistries();
//
//            if (userRegistries != null && !userRegistries.isEmpty()) {
//                Iterator<Object> URIterator = userRegistries.iterator();
//                urSearch: while (URIterator.hasNext()) {
//                    Object ur = URIterator.next();
//                    String realm = getRealm(ur);
//
//                    if (realm != null && realm.equals(reposId)) {
//                        String baseEntry = "o=" + realm;
//                        baseEntries.add(baseEntry);
//                        break urSearch;
//                    }
//                }
//            }
//        }
//
//        return baseEntries;
    }

    public List<String> getRepoIds() throws WIMException {
        return new ArrayList<String>(repositories.keySet());
//        List<String> repoIds = new ArrayList<String>();
//        ConcurrentServiceReferenceMap<String, RepositoryConfiguration> configs = vmmService.getRepositoryConfigurations();
//
//        Set<Object> userRegistries = vmmService.getUserRegistries();
//
//        if ((configs == null || configs.isEmpty()) && (userRegistries == null || userRegistries.isEmpty())) {
//            return repoIds;
//        }
//
//        Iterator<RepositoryConfiguration> itr = configs.getServices();
//        while (itr.hasNext()) {
//            RepositoryConfig config = itr.next();
//            // If repository Id is not already added then add it to the list
//            if (!repoIds.contains(config.getReposId())) {
//                repoIds.add(config.getReposId());
//            }
//        }
//
//        Iterator<Object> URIterator = userRegistries.iterator();
//        while (URIterator.hasNext()) {
//            Object ur = URIterator.next();
//            // Default the repository Id to its configured realm
//            String repoId = getRealm(ur);
//
//            // If repository Id is not already added then add it to the list
//            if (!repoIds.contains(repoId)) {
//                repoIds.add(repoId);
//            }
//        }
//
//        return repoIds;
    }

    public int getNumberOfRepositories() throws WIMException {
        return getRepoIds().size();
    }

    /**
     * Returns true if the baseEntries list contains the baseEntry. The
     * comparison is done using equalsIgnoreCase() to make sure that Turkish
     * locale I's are handled properly.
     **/
    public static boolean matchBaseEntryIgnoreCase(List<String> baseEntries, String baseEntry) {
        boolean result = false;
        if (baseEntries != null && baseEntry != null) {
            for (int i = 0; i < baseEntries.size(); i++) {
                if (baseEntry.equalsIgnoreCase(baseEntries.get(i))) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

    public boolean isPropertyJoin() {
        return false;
    }

    public boolean isEntryJoin() {
        return false;
    }

    /**
     * sorts a list of base entries into the repositories that provide them.
     *
     * @param realmBaseEntries
     * @return map of repository id to base entries that that repository provides.
     * @throws WIMException
     */
    //TODO doing it everytime instead of prepopulated might cause perf issue. look at alternatives where it can be done
    public Map<String, List<String>> getBaseEntriesForRepos(String[] realmBaseEntries) throws WIMException {
        Map<String, List<String>> reposBaseEntries = new HashMap<String, List<String>>();
        for (String baseEntryName : realmBaseEntries) {
            String reposId = getRepositoryIdByUniqueName(baseEntryName);
            List<String> baseEntryList = reposBaseEntries.get(reposId);
            if (baseEntryList == null)
                baseEntryList = new ArrayList<String>();
            baseEntryList.add(baseEntryName);
            reposBaseEntries.put(reposId, baseEntryList);
        }
        return reposBaseEntries;
    }

    //TODO not tested.
    public boolean isReadOnly(String reposId) throws WIMException {
        // return readOnlyMap.get(reposId).booleanValue();
        // As property is not defined in the metatype, always return false.
        return false;
    }

    //TODO not tested.
    public boolean isSortingSupported(String reposId) {
        // return sortSupportMap.get(reposId).booleanValue();
        // As property is not defined in the metatype, always return false.
        return false;
    }

    private Map<String, Set<String>> getRepositoriesForGroup() {
        //TODO this appears to be backwards, but also seems to match the original (untested) code.
        //Perhaps the map needs to be inverted?
        //On the other hand getRepositoriesForGroupMembers seems to return the inverted map.
        Map<String, Set<String>> repositoriesForGroup = new HashMap<String, Set<String>>();
        for (Map.Entry<String, RepositoryWrapper> entry : repositories.entrySet()) {
            repositoriesForGroup.put(entry.getKey(), entry.getValue().getRepositoryGroups());
        }
        return repositoriesForGroup;

//        ConcurrentServiceReferenceMap<String, RepositoryConfiguration> configs = vmmService.getRepositoryConfigurations();
//        Set<Object> userRegistries = vmmService.getUserRegistries();
//
//        if ((configs == null || configs.isEmpty()) && (userRegistries == null || userRegistries.isEmpty())) {
//            return repositoriesForGroup;
//        }
//
//        Iterator<RepositoryConfiguration> itr = configs.getServices();
//        while (itr.hasNext()) {
//            RepositoryConfig config = itr.next();
//            // TODO not tested
//            String[] reposForGroups = config.getRepositoriesForGroups();
//            repositoriesForGroup.put(config.getReposId(), new HashSet<String>());
//
//            if (reposForGroups != null && reposForGroups.length > 0) {
//                for (int k = 0; k < reposForGroups.length; k++) {
//                    String grpReposId = reposForGroups[k].trim();
//                    Set<String> grpReposIdSet = repositoriesForGroup.get(config.getReposId());
//
//                    grpReposIdSet.add(grpReposId);
//                    repositoriesForGroup.put(config.getReposId(), grpReposIdSet);
//                }
//            }
//        }
//
//        Iterator<Object> URIterator = userRegistries.iterator();
//        while (URIterator.hasNext()) {
//            Object ur = URIterator.next();
//            // Default the repository Id to its configured realm
//            String repoId = getRealm(ur);
//
//            HashSet<String> repositoryIdForGroup = new HashSet<String>();
//            repositoryIdForGroup.add(repoId);
//            repositoriesForGroup.put(repoId, repositoryIdForGroup);
//        }
//
//        return repositoriesForGroup;
    }

    //TODO not tested.
    public boolean isCrossRepositoryGroupMembership(String reposID) throws WIMException {

        Map<String, Set<String>> repositoriesForGroup = getRepositoriesForGroup();

        int numOfReposForGrp = repositoriesForGroup.get(reposID).size();
        if (numOfReposForGrp > 1) {
            return true;
        }
        if (numOfReposForGrp == 1) {
            String grpReposUUID = repositoriesForGroup.get(reposID).iterator().next();
            if (!reposID.equals(grpReposUUID)) {
                return true;
            }
        }

        return false;
    }

    //TODO not tested.
    public Set<String> getRepositoriesForGroupMembership(String repositoryId) throws WIMException {
        RepositoryWrapper repositoryHolder = repositories.get(repositoryId);
        if (repositoryHolder != null) {
            return repositoryHolder.getRepositoryGroups();
        }
        return null;
//        return getRepositoriesForGroup().get(reposID);
    }

    private Map<String, Set<String>> getRepositoriesForGroupMembers() {
        Map<String, Set<String>> groupToRepositoryId = new HashMap<String, Set<String>>();

        for (Map.Entry<String, RepositoryWrapper> entry : repositories.entrySet()) {
            String repositoryid = entry.getKey();
            Set<String> groups = entry.getValue().getRepositoryGroups();
            for (String group : groups) {
                Set<String> repositoryIds = groupToRepositoryId.get(group);
                if (repositoryIds == null) {
                    repositoryIds = new HashSet<String>();
                    groupToRepositoryId.put(group, repositoryIds);
                }
                repositoryIds.add(repositoryid);
            }
        }
        return groupToRepositoryId;
//        ConcurrentServiceReferenceMap<String, RepositoryConfiguration> configs = vmmService.getRepositoryConfigurations();
//        if (configs == null || configs.isEmpty()) {
//            return repositoriesForGroupMembers;
//        }
//
//        Iterator<RepositoryConfiguration> itr = configs.getServices();
//        while (itr.hasNext()) {
//            RepositoryConfig config = itr.next();
//            // TODO not tested
//            String[] reposForGroups = config.getRepositoriesForGroups();
//
//            if (reposForGroups != null && reposForGroups.length > 0) {
//                for (int k = 0; k < reposForGroups.length; k++) {
//                    String grpReposId = reposForGroups[k].trim();
//
//                    Set<String> mbrReposIdSet = repositoriesForGroupMembers.get(grpReposId);
//                    if (mbrReposIdSet == null) {
//                        mbrReposIdSet = new HashSet<String>();
//                    }
//                    mbrReposIdSet.add(config.getReposId());
//                    repositoriesForGroupMembers.put(grpReposId, mbrReposIdSet);
//                }
//            }
//        }
//        return repositoriesForGroupMembers;
    }

    //TODO not tested.
    public boolean canGroupAcceptMember(String grpReposId, String mbrReposId) {
        Map<String, Set<String>> repositoriesForGroupMembers = getRepositoriesForGroupMembers();

        if (repositoriesForGroupMembers != null) {
            Set<String> mbrReposIdSet = repositoriesForGroupMembers.get(grpReposId);
            if (mbrReposIdSet != null) {
                return mbrReposIdSet.contains(mbrReposId);
            }
        }
        return false;
    }

    /**
     *
     */
    public void clearAllCachedURRepositories() {
        for (RepositoryWrapper repositoryHolder : repositories.values()) {
            repositoryHolder.clear();
        }
    }

    /**
     * @param uniqueName
     * @return
     */
    @FFDCIgnore(Exception.class)
    public List<String> getFederationUREntityType(String data) {
        for (RepositoryWrapper rh : repositories.values()) {
            if (rh instanceof UserRegistryWrapper) {
                UserRegistry ur = ((UserRegistryWrapper) rh).getUserRegistry();

                try {
                    SearchResult result = ur.getUsers(data, 1);
                    if (result != null && result.getList().size() > 0) {
                        ArrayList<String> returnValue = new ArrayList<String>();
                        returnValue.add(SchemaConstants.DO_PERSON_ACCOUNT);
                        returnValue.add(data);
                        return returnValue;
                    }
                } catch (Exception e) {
                }

                try {
                    SearchResult result = ur.getGroups(data, 1);
                    if (result != null && result.getList().size() > 0) {
                        ArrayList<String> returnValue = new ArrayList<String>();
                        returnValue.add(SchemaConstants.DO_GROUP);
                        returnValue.add(data);
                        return returnValue;
                    }
                } catch (Exception e) {
                }

                try {
                    String result = ur.getUserSecurityName(data);
                    if (result != null) {
                        ArrayList<String> returnValue = new ArrayList<String>();
                        returnValue.add(SchemaConstants.DO_PERSON_ACCOUNT);
                        returnValue.add(result);
                        return returnValue;
                    }
                } catch (Exception e) {
                }

                try {
                    String result = ur.getGroupSecurityName(data);
                    if (result != null) {
                        ArrayList<String> returnValue = new ArrayList<String>();
                        returnValue.add(SchemaConstants.DO_GROUP);
                        returnValue.add(result);
                        return returnValue;
                    }
                } catch (Exception e) {
                }
            }
        }
        return null;
    }
}