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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import com.ibm.wsspi.security.wim.exception.EntityNotInRealmScopeException;
import com.ibm.wsspi.security.wim.exception.InitializationException;
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

    /**
     * The stack of realmNames stored in stack for this thread.
     *
     * Technically, there probably would never be a need to have a stack, as the realm
     * name for an operation is unlikely to change, but it operates as a counter for how
     * many times the realm name has been set so that reentrant calls don't end up clearing
     * the realm name prematurely.
     */
    private static ThreadLocal<LinkedList<String>> realmNameTLStack = new ThreadLocal<LinkedList<String>>() {
        @Override
        protected LinkedList<String> initialValue() {
            return new LinkedList<String>();
        }
    };

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
        numRepos = getNumberOfRepositories();
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
        numRepos = getNumberOfRepositories();
    }

    /**
     * {@inheritDoc} This lookup is done for all registered RepositoryFactory
     * instances. Please note that this method does not use the configuration
     * data for the RepositoryService for this lookup.
     */
    public Repository getRepository(String instanceId) {
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

    /**
     * Returns the ID of the repository to which the uniqueName belongs to.
     *
     * @param uniqueName
     *            The uniqueName to retrieve the repository ID for.
     * @return The repository that best matches the uniqueName.
     * @throws WIMException
     *             If the uniqueName is not found in any of the repositories.
     */
    protected String getRepositoryIdByUniqueName(String uniqueName) throws WIMException {

        boolean isDn = UniqueNameHelper.isDN(uniqueName) != null;
        if (isDn) {
            uniqueName = UniqueNameHelper.getValidUniqueName(uniqueName).trim();
        }

        String repo = null;
        int repoMatch = -1;
        int bestMatch = -1;
        String realmName = getRealmOnThread();

        /*
         * Restrict matches to the repositories that are participating in the
         * specified realm.
         */
        Collection<Entry<String, RepositoryWrapper>> baseEntries = new HashSet<Entry<String, RepositoryWrapper>>();
        RealmConfig realmConfig = vmmService.getConfigManager().getRealmConfig(realmName);
        if (realmConfig != null) {
            List<String> participatingBaseEntries = Arrays.asList(realmConfig.getParticipatingBaseEntries());

            for (Entry<String, RepositoryWrapper> entry : repositories.entrySet()) {
                for (String repoBaseEntry : entry.getValue().getRepositoryBaseEntries().keySet()) {
                    if (participatingBaseEntries.contains(repoBaseEntry)) {
                        baseEntries.add(entry);
                    }
                }
            }
        } else {
            /*
             * There is no explicitly configured realm. Use all base entries.
             */
            baseEntries = repositories.entrySet();
        }

        for (Entry<String, RepositoryWrapper> entry : baseEntries) {
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
                    realmName, null, Integer.valueOf("204"));

        String msg = Tr.formatMessage(tc, WIMMessageKey.ENTITY_NOT_IN_REALM_SCOPE, WIMMessageHelper.generateMsgParms(uniqueName, realmName));
        throw new EntityNotInRealmScopeException(WIMMessageKey.ENTITY_NOT_IN_REALM_SCOPE, msg);
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

    public List<String> getRepoIds() {
        return new ArrayList<String>(repositories.keySet());
    }

    public int getNumberOfRepositories() {
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
    public List<String> getFederationUREntityType(String uniqueName) {
        List<String> result = null;

        for (RepositoryWrapper rw : repositories.values()) {
            if (rw instanceof UserRegistryWrapper) {
                URBridge bridge = (URBridge) ((UserRegistryWrapper) rw).getRepository();
                result = bridge.getEntityType(uniqueName);
                if (result != null) {
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Get the realm set for the current thread.
     *
     * @return The realm set for the current thread.
     */
    public static String getRealmOnThread() {
        return realmNameTLStack.get().peek();
    }

    /**
     * Set the realm for the current thread.
     *
     * This should always be used in conjunction with the
     * {@link #clearRealmOnThread()} method in a finally block that ensures that
     * the realm name gets cleared.
     *
     * @param realmName
     *            The realm to set on the current thread.
     * @see #clearRealmOnThread()
     */
    public static void setRealmOnThread(String realmName) {
        final String METHODNAME = "setRealmOnThread";

        LinkedList<String> stack = realmNameTLStack.get();
        stack.push(realmName);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " realmName=" + realmName + ", size=" + stack.size());
        }
    }

    /**
     * Clear the realm set on the current thread. Whenever the method
     * {@link #setRealmOnThread(String)} is called this should be called in a
     * finally block to ensure the realm name is unset.
     *
     * @see #setRealmOnThread(String)
     */
    public static void clearRealmOnThread() {
        final String METHODNAME = "clearRealmOnThread";

        LinkedList<String> stack = realmNameTLStack.get();
        String realmName = stack.pop();

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " Realm popped from stack= " + realmName + ", size=" + stack.size());
        }
    }
}