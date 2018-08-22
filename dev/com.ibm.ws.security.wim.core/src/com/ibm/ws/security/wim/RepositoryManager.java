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
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.audit.context.AuditManager;
import com.ibm.websphere.security.wim.ras.WIMMessageHelper;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.ws.security.audit.Audit;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.wim.adapter.urbridge.URBridge;
import com.ibm.ws.security.wim.util.UniqueNameHelper;
import com.ibm.wsspi.security.wim.CustomRepository;
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

    private volatile int numRepos = 0; // short cut for error checking on how many repos we have

    private final Map<String, RepositoryWrapper> repositories = new ConcurrentHashMap<String, RepositoryWrapper>();

    public RepositoryManager(VMMService service) {
        vmmService = service;
    }

    void addConfiguredRepository(String repositoryId, ConfiguredRepository configuredRepository) {
        RepositoryWrapper repositoryHolder = new ConfiguredRepositoryWrapper(repositoryId, configuredRepository);
        addRepository(repositoryId, repositoryHolder);
    }

    void addCustomRepository(String repositoryId, CustomRepository customRepository) {
        RepositoryWrapper repositoryHolder = new CustomRepositoryWrapper(repositoryId, customRepository);
        addRepository(repositoryId, repositoryHolder);
    }

    /**
     * Pair adding to the repositories map and resetting the numRepos int.
     *
     * @param repositoryId
     * @param repositoryHolder
     */
    private void addRepository(String repositoryId, RepositoryWrapper repositoryHolder) {
        repositories.put(repositoryId, repositoryHolder);
        try {
            numRepos = getNumberOfRepositories();
        } catch (WIMException e) {
            // okay
        }
    }

    void addUserRegistry(UserRegistry userRegistry) {
        try {
            UserRegistryWrapper repositoryHolder = new UserRegistryWrapper(userRegistry, vmmService.getConfigManager());
            addRepository(userRegistry.getRealm(), repositoryHolder);

        } catch (InitializationException e) {
            //TODO will occur on lookup when this is made lazy.
        }
    }

    void removeRepositoryHolder(String id) {
        RepositoryWrapper repositoryHolder = repositories.remove(id);
        if (repositoryHolder != null) {
            repositoryHolder.clear();
        }
        try {
            numRepos = getNumberOfRepositories();
        } catch (WIMException e) {
            // okay
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

        AuditManager auditManager = new AuditManager();
        Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), auditManager.getRequestType(), auditManager.getRepositoryId(), uniqueName,
                    vmmService.getConfigManager().getDefaultRealmName(), null, Integer.valueOf("204"));

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
    }

    public Map<String, String> getRepositoryBaseEntries(String reposId) throws WIMException {
        RepositoryWrapper repositoryHolder = repositories.get(reposId);
        if (repositoryHolder != null) {
            return repositoryHolder.getRepositoryBaseEntries();
        }
        return Collections.emptyMap();
    }

    public List<String> getRepoIds() throws WIMException {
        return new ArrayList<String>(repositories.keySet());
    }

    public int getNumberOfRepositories() throws WIMException {
        return getRepoIds().size();
    }

    /**
     * Gets the shortcut number of repositories, to do a quick check on the number of repos.
     * To get the actual map size, call getNumberOfRepositories().
     *
     * @return
     * @throws WIMException
     */
    @Trivial
    public int getNumberOfRepositoriesVolatile() throws WIMException {
        return numRepos;
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
    public List<String> getFederationUREntityType(String data) {
        List<String> result = null;

        for (RepositoryWrapper rw : repositories.values()) {
            if (rw instanceof UserRegistryWrapper) {
                URBridge bridge = (URBridge) ((UserRegistryWrapper) rw).getRepository();
                result = bridge.getEntityType(data);
                if (result != null) {
                    break;
                }
            }
        }

        return result;
    }
}