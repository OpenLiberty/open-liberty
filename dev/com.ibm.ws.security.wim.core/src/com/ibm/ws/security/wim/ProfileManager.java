/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.audit.context.AuditManager;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.websphere.security.wim.ProfileServiceLite;
import com.ibm.websphere.security.wim.Service;
import com.ibm.websphere.security.wim.ras.WIMMessageHelper;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;
import com.ibm.websphere.security.wim.util.PasswordUtil;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.audit.Audit;
import com.ibm.ws.security.wim.env.ICacheUtil;
import com.ibm.ws.security.wim.util.AuditConstants;
import com.ibm.ws.security.wim.util.ControlsHelper;
import com.ibm.ws.security.wim.util.SchemaConstantsInternal;
import com.ibm.ws.security.wim.util.SortHandler;
import com.ibm.ws.security.wim.util.UniqueNameHelper;
import com.ibm.ws.security.wim.xpath.FederationLogicalNode;
import com.ibm.ws.security.wim.xpath.FederationParenthesisNode;
import com.ibm.ws.security.wim.xpath.ParenthesisNode;
import com.ibm.ws.security.wim.xpath.ParseException;
import com.ibm.ws.security.wim.xpath.TokenMgrError;
import com.ibm.ws.security.wim.xpath.WIMXPathInterpreter;
import com.ibm.ws.security.wim.xpath.mapping.datatype.LogicalNode;
import com.ibm.ws.security.wim.xpath.mapping.datatype.PropertyNode;
import com.ibm.ws.security.wim.xpath.mapping.datatype.XPathNode;
import com.ibm.wsspi.security.wim.SchemaConstants;
import com.ibm.wsspi.security.wim.exception.AttributeNotSupportedException;
import com.ibm.wsspi.security.wim.exception.CertificateMapFailedException;
import com.ibm.wsspi.security.wim.exception.CertificateMapNotSupportedException;
import com.ibm.wsspi.security.wim.exception.ChangeControlException;
import com.ibm.wsspi.security.wim.exception.DefaultParentNotFoundException;
import com.ibm.wsspi.security.wim.exception.DuplicateLogonIdException;
import com.ibm.wsspi.security.wim.exception.EntityIdentifierNotSpecifiedException;
import com.ibm.wsspi.security.wim.exception.EntityNotFoundException;
import com.ibm.wsspi.security.wim.exception.EntityNotInRealmScopeException;
import com.ibm.wsspi.security.wim.exception.EntityTypeNotSupportedException;
import com.ibm.wsspi.security.wim.exception.InvalidIdentifierException;
import com.ibm.wsspi.security.wim.exception.InvalidUniqueIdException;
import com.ibm.wsspi.security.wim.exception.MaxResultsExceededException;
import com.ibm.wsspi.security.wim.exception.MissingSearchControlException;
import com.ibm.wsspi.security.wim.exception.NoUserRepositoriesFoundException;
import com.ibm.wsspi.security.wim.exception.OperationNotSupportedException;
import com.ibm.wsspi.security.wim.exception.PasswordCheckFailedException;
import com.ibm.wsspi.security.wim.exception.SearchControlException;
import com.ibm.wsspi.security.wim.exception.SortControlException;
import com.ibm.wsspi.security.wim.exception.WIMApplicationException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.exception.WIMSystemException;
import com.ibm.wsspi.security.wim.model.CacheControl;
import com.ibm.wsspi.security.wim.model.ChangeControl;
import com.ibm.wsspi.security.wim.model.ChangeResponseControl;
import com.ibm.wsspi.security.wim.model.CheckGroupMembershipControl;
import com.ibm.wsspi.security.wim.model.CheckPointType;
import com.ibm.wsspi.security.wim.model.Context;
import com.ibm.wsspi.security.wim.model.Control;
import com.ibm.wsspi.security.wim.model.DeleteControl;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.ExternalNameControl;
import com.ibm.wsspi.security.wim.model.Group;
import com.ibm.wsspi.security.wim.model.GroupControl;
import com.ibm.wsspi.security.wim.model.GroupMemberControl;
import com.ibm.wsspi.security.wim.model.GroupMembershipControl;
import com.ibm.wsspi.security.wim.model.IdentifierType;
import com.ibm.wsspi.security.wim.model.LoginAccount;
import com.ibm.wsspi.security.wim.model.LoginControl;
import com.ibm.wsspi.security.wim.model.PageControl;
import com.ibm.wsspi.security.wim.model.PageResponseControl;
import com.ibm.wsspi.security.wim.model.Root;
import com.ibm.wsspi.security.wim.model.SearchControl;
import com.ibm.wsspi.security.wim.model.SearchResponseControl;
import com.ibm.wsspi.security.wim.model.SortControl;
import com.ibm.wsspi.security.wim.model.SortKeyType;

public class ProfileManager implements ProfileServiceLite {

    /**
     * Register the class to trace service.
     */
    private final static TraceComponent tc = Tr.register(ProfileManager.class);

    /*
     * Constant for the CREATE method.
     */
    private static final char GET = 'g';

    /*
     * Constant for the SEARCH method.
     */
    private static final char SEARCH = 's';

    /*
     * Constant for the LOGIN method.
     */
    private static final char LOGIN = 'l';

    /**
     * Constant for the DELETE method
     */
    private static final char DELETE = 'd';

    /**
     * Constant for the CREATE method
     */
    private static final char CREATE = 'c';

    /**
     * Constant for the UPDATE method
     */
    private static final char UPDATE = 'u';

    /**
     * Constant for delete method
     */
    private static final String DELETE_EMITTER = "delete";

    /*
     * Constant for REPOS
     */
    private static final String REPOS = "REPOS";

    /*
     * Constants for LA
     */
    private static final String LA = "LA";

    private ICacheUtil pagingSearchCache = null;

    private int maxTotalPagingSearchResults = 1000;

    // Timeout is 30 secs
    private long pagingSearchResultsCacheTimeOut = 30000;

    private ConfigManager configMgr;
    private final RepositoryManager repositoryManager;

    //TODO For now it doesn't make any diff. Could be be implemented as service only LA comes in picture
    PropertyManager propMgr = new PropertyManager();

    /**
     * @param repositoryManager
     */
    ProfileManager(RepositoryManager repositoryManager) {
        this.repositoryManager = repositoryManager;
    }

    public void setConfigManager(ConfigManager configManager) {
        configMgr = configManager;
    }

    private ConfigManager getConfigManager() {
        return configMgr;
    }

    private RepositoryManager getRepositoryManager() {
        return repositoryManager;
    }

    @Override
    @Trivial
    public Root get(Root root) throws WIMException {
        //Please DONOT UPDATE or CHANGE this wrapper.
        //If a need arises, please updated the getImpl method call.

        final String METHODNAME = "get";
        return genericProfileManagerMethod(METHODNAME, ProfileManager.GET, root);
    }

    @Override
    @Trivial
    public Root login(Root root) throws WIMException {
        //Please DONOT UPDATE or CHANGE this wrapper.
        //If a need arises, please updated the loginImpl method call.

        final String METHODNAME = "login";
        return genericProfileManagerMethod(METHODNAME, ProfileManager.LOGIN, root);
    }

    @Override
    @Trivial
    public Root search(Root root) throws WIMException {
        //Please DONOT UPDATE or CHANGE this wrapper.
        //If a need arises, please updated the searchImpl method call.

        final String METHODNAME = "search";
        return genericProfileManagerMethod(METHODNAME, ProfileManager.SEARCH, root);
    }

    @Trivial
    @FFDCIgnore(WIMException.class)
    private Root genericProfileManagerMethod(String METHODNAME, char METHODTYPE, Root root) throws WIMException {

        if (repositoryManager != null && repositoryManager.getNumberOfRepositoriesVolatile() < 1) {
            if (repositoryManager.getNumberOfRepositories() < 1) { // double check that we're at 0 repos
                throw new NoUserRepositoriesFoundException(WIMMessageKey.MISSING_REGISTRY_DEFINITION, Tr.formatMessage(
                                                                                                                       tc,
                                                                                                                       WIMMessageKey.MISSING_REGISTRY_DEFINITION,
                                                                                                                       null));
            }
        }

        // Please DONOT UPDATE or CHANGE this wrapper.
        // If a need arises, please updated the required method call.

        Root adapterRoot = null;
        try {
            //Calling Implementation
            switch (METHODTYPE) {
                case ProfileManager.LOGIN:
                    adapterRoot = loginImpl(root);
                    break;
                case ProfileManager.SEARCH:
                    adapterRoot = searchImpl(root);
                    break;
                case ProfileManager.DELETE:
                    adapterRoot = deleteImpl(root);
                    break;
                case ProfileManager.GET:
                    adapterRoot = getImpl(root);
                    break;
                case ProfileManager.CREATE:
                    adapterRoot = createImpl(root);
                    break;
                case ProfileManager.UPDATE:
                    adapterRoot = updateImpl(root);
                    break;
                default:
                    break;
            }

        } catch (WIMException wime) {
            throw wime;
        }
        return adapterRoot;
    }

    private Root getImpl(Root inRoot) throws WIMException {
        String METHODNAME = "getImpl(Root inRoot)";
        String targetReposId = null;
        String uniqueName = null;

        if (inRoot == null) {
            return null;
        }

        AuditManager auditManager = new AuditManager();

        // for audit reporting purposes, only if we have one repository configured, else we have to rely on a valid parentDN to get to the
        // correct repositoryId
        if (repositoryManager.getNumberOfRepositories() == 1) {
            targetReposId = repositoryManager.getRepoIds().get(0);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "targetReposID = ", targetReposId);
        }
        auditManager.setRepositoryId(targetReposId);
        auditManager.setRequestType(AuditConstants.GET_AUDIT);

        String realmName = getRealmName(inRoot);

        // Extract the controls
        Map<String, Control> ctrlMap = ControlsHelper.getControlMap(inRoot);

        boolean isAllowOperationIfReposDown = false;
        boolean trustEntityType = false;

        List<Context> contexts = inRoot.getContexts();
        for (Context contextInput : contexts) {
            String key = contextInput.getKey();

            if (key != null && Service.CONFIG_PROP_ALLOW_OPERATION_IF_REPOS_DOWN.equals(key)) {
                isAllowOperationIfReposDown = Boolean.parseBoolean(String.valueOf(contextInput.getValue()));
            }
            if (key != null && Service.VALUE_CONTEXT_TRUST_ENTITY_TYPE_KEY.equals(key)) {
                trustEntityType = Boolean.parseBoolean(String.valueOf(contextInput.getValue())) /* && ProfileSecurityManager.singleton().isCallerSuperUser() */;
            }
        }

        Set<String> failureRepositoryIds = new HashSet<String>();

        CheckGroupMembershipControl chkGrpMembershipCtl = (CheckGroupMembershipControl) ctrlMap.get(DO_CHECK_GROUP_MEMBERSHIP_CONTROL);
        Root retRoot = new Root();
        List<Entity> entities = inRoot.getEntities();

        HashMap<String, Root> inputDataGraphes = new HashMap<String, Root>();
        HashMap<String, Root> specialDataGraphes = new HashMap<String, Root>();
        HashMap<String, Root> returnedDataGraphes = new HashMap<String, Root>();

        int index = 0;

        for (Entity entity : entities) {
            index++;
            String entityType = entity.getTypeName();
            IdentifierType identifier = entity.getIdentifier();

            if (identifier == null) {
                Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.GET_AUDIT, targetReposId, null, getRealmName(inRoot),
                            inRoot,
                            Integer.valueOf("203"));

                throw new EntityIdentifierNotSpecifiedException(WIMMessageKey.ENTITY_IDENTIFIER_NOT_SPECIFIED, Tr.formatMessage(
                                                                                                                                tc,
                                                                                                                                WIMMessageKey.ENTITY_IDENTIFIER_NOT_SPECIFIED,
                                                                                                                                null));
            }

            String uniqueId = identifier.getUniqueId();
            uniqueName = identifier.getUniqueName();
            String federationEntityType = null;
            if (uniqueName != null) {
                if (UniqueNameHelper.isDN(uniqueName) != null) {
                    uniqueName = UniqueNameHelper.formatUniqueName(uniqueName);
                    identifier.setUniqueName(uniqueName);
                } else {
                    List<String> entityData = repositoryManager.getFederationUREntityType(uniqueName);
                    if (entityData != null) {
                        trustEntityType = true;
                        federationEntityType = entityData.get(0);
                        uniqueName = entityData.get(1);
                        entity.getIdentifier().setUniqueName(uniqueName);
                    } else
                        return new Root(); // If this is not a federation UR entity nor uniqueName in DN Format then return nothing
                }
            }

            if ((uniqueId == null || uniqueId.trim().length() == 0)
                && (uniqueName == null || uniqueName.trim().length() == 0)) {
                String externalName = identifier.getExternalName();
                if (externalName != null && externalName.length() > 0) {
                    ExternalNameControl extNameCtrl = (ExternalNameControl) ctrlMap.get(Service.DO_EXTERNAL_NAME_CONTROL);
                    if (extNameCtrl != null) {
                        Root sRoot = null;
                        String key = Service.DO_EXTERNAL_NAME_CONTROL + "-" + index;
                        if (!specialDataGraphes.containsKey(key)) {
                            sRoot = new Root();
                            sRoot.getEntities().add(entity);
                            sRoot.getControls().addAll(inRoot.getControls());

                            specialDataGraphes.put(key, sRoot);
                        }
                        continue;
                    }
                    Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.GET_AUDIT, targetReposId, uniqueName, realmName,
                                inRoot,
                                Integer.valueOf("210"));

                    throw new WIMApplicationException(WIMMessageKey.EXTERNAL_NAME_CONTROL_NOT_FOUND, Tr.formatMessage(
                                                                                                                      tc,
                                                                                                                      WIMMessageKey.EXTERNAL_NAME_CONTROL_NOT_FOUND,
                                                                                                                      WIMMessageHelper.generateMsgParms(externalName)));
                }

                Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.GET_AUDIT, targetReposId, uniqueName, realmName, inRoot,
                            Integer.valueOf("211"));

                throw new InvalidIdentifierException(WIMMessageKey.INVALID_IDENTIFIER, Tr.formatMessage(
                                                                                                        tc,
                                                                                                        WIMMessageKey.INVALID_IDENTIFIER,
                                                                                                        WIMMessageHelper.generateMsgParms(uniqueId, uniqueName)));
            }

            String repositoryId;
            String realEntityType;
            if (trustEntityType) {
                // Trust the entity type specified by the client
                // This eliminates an extra call to LDAP for performance
                realEntityType = (federationEntityType == null) ? entity.getTypeName() : federationEntityType;
                repositoryId = getRepositoryManager().getRepositoryId(uniqueName);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, METHODNAME + " Client entity type will be trusted: " + realEntityType);
            } else {
                // Don't trust the entity type specified by the client
                if (tc.isDebugEnabled())
                    Tr.debug(tc, METHODNAME + " Client entity type will NOT be trusted: " + entityType);
                Entity realEntity = retrieveEntity(null, identifier, isAllowOperationIfReposDown, failureRepositoryIds);
                realEntityType = realEntity.getTypeName();
                uniqueName = identifier.getUniqueName();
                repositoryId = identifier.getRepositoryId();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, METHODNAME + " Entity type retrieved from repository: " + realEntityType);
            }

            if (chkGrpMembershipCtl != null) {
                Group group = null;
                try {
                    group = (Group) entity;
                } catch (ClassCastException e) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, METHODNAME + " Entity is not a group or its subtype");
                }

                if (group != null) {
                    List<Entity> mbrs = group.getMembers();
                    setExtIdAndRepositoryIdForEntities(mbrs, repositoryId, isAllowOperationIfReposDown, failureRepositoryIds);
                }
            }

            // realm support
            realmName = getRealmName(inRoot);
            if (realmName != null && !getConfigManager().isUniqueNameInRealm(uniqueName, realmName) &&
                UniqueNameHelper.isDN(uniqueName) != null) {
                Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.GET_AUDIT, repositoryId, uniqueName, realmName, inRoot,
                            Integer.valueOf("204"));

                throw new EntityNotInRealmScopeException(WIMMessageKey.ENTITY_NOT_IN_REALM_SCOPE, Tr.formatMessage(
                                                                                                                   tc,
                                                                                                                   WIMMessageKey.ENTITY_NOT_IN_REALM_SCOPE,
                                                                                                                   WIMMessageHelper.generateMsgParms(uniqueName, realmName)));
            }

            Root sRoot = null;
            if (!inputDataGraphes.containsKey(repositoryId)) {
                sRoot = new Root();
            } else {
                sRoot = inputDataGraphes.get(repositoryId);
            }

            sRoot.getEntities().add(entity);
            sRoot.getControls().addAll(inRoot.getControls());
            sRoot.getContexts().addAll(inRoot.getContexts());

            inputDataGraphes.put(repositoryId, sRoot);
        }

        // handling externalName operation
        //Iterator<String> keys = specialDataGraphes.keySet().iterator();
        Set<Map.Entry<String, Root>> specialDataGraphesEntrySet = specialDataGraphes.entrySet();
        for (Map.Entry<String, Root> entry : specialDataGraphesEntrySet) {
            String key = entry.getKey();
            if (key.startsWith(Service.DO_EXTERNAL_NAME_CONTROL)) {
                Root rRoot = entry.getValue();
                realmName = getRealmName(rRoot);
                List<Entity> extEntities = rRoot.getEntities();
                Entity entity = extEntities.get(0);
                IdentifierType identifier = entity.getIdentifier();
                String externalName = identifier.getExternalName();
                List<String> potentialRepositories = getReposForExternalName(externalName, realmName);
                // For each repository in the list call get() SPI
                Root retSRoot = null;
                String reposId = null;
                // RepositoryManager reposMgr = reposMgrRef.getService();
                for (int i = 0; i < potentialRepositories.size(); i++) {
                    try {
                        reposId = potentialRepositories.get(i);
                        retSRoot = repositoryManager.getRepository(reposId).get(rRoot);

                        // TODO:: Un-comment when adding LA repository
                        /*
                         * if (retSRoot != null && reposMgr.isPropertyJoin()) {
                         * // Get the returned dataObject and the controls
                         * List controls = inRoot.getControls();
                         *
                         * // If there are any controls in the original call, copy them here.
                         * try {
                         * if(controls.size() != 0)
                         * {
                         * if (hasLaProps) {
                         * retSRoot.getList(Service.DO_CONTROLS).clear();
                         * retSRoot.getList(Service.DO_CONTROLS).addAll(controls);
                         * RepositoryManager.singleton().getLookasideRepository().get(retSRoot);
                         * }
                         * }
                         * else {
                         * RepositoryManager.singleton().getLookasideRepository().get(retSRoot);
                         * }
                         * }catch (WIMException we){
                         * if (!isAllowOperationIfReposDown){
                         * throw we;
                         * }else{
                         * trcLogger.logp(Level.FINER, CLASSNAME, METHODNAME,
                         * "IGNORE: exception [" + we.getMessage() + "] on LA repository ");
                         * failureRepositoryIds.add(reposMgr.getLookasideRepositoryID());
                         * }
                         * }
                         * }
                         */
                    } catch (EntityNotFoundException e) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, METHODNAME + " IGNORE: exception [" + e.getMessage() + "] on repository [" + reposId + "]");
                    } catch (WIMException we) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, METHODNAME + " IGNORE: exception [" + we.getMessage() + "] on repository [" + reposId + "]");
                        failureRepositoryIds.add(reposId);
                        continue;
                    }

                    if (retSRoot != null) {
                        prepareDataGraphForCaller(retSRoot, null, null, isAllowOperationIfReposDown, failureRepositoryIds);
                        returnedDataGraphes.put(key, retSRoot);
                        break;
                    }
                }
                if (retSRoot == null) {
                    Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.GET_AUDIT, targetReposId, uniqueName, realmName,
                                inRoot,
                                Integer.valueOf("212"));

                    throw new EntityNotFoundException(WIMMessageKey.ENTITY_NOT_FOUND, Tr.formatMessage(
                                                                                                       tc,
                                                                                                       WIMMessageKey.ENTITY_NOT_FOUND,
                                                                                                       WIMMessageHelper.generateMsgParms(externalName)));
                }

            }
        }

        // distribute to repository for get
        Set<Map.Entry<String, Root>> inputDataGraphesEntrySet = inputDataGraphes.entrySet();

        for (Map.Entry<String, Root> entry : inputDataGraphesEntrySet) {
            String key = entry.getKey();
            Root dgRoot = entry.getValue();
            Root retDgRoot = null;
            try {
                retDgRoot = getRepositoryManager().getRepository(key).get(dgRoot);
            } catch (Exception e) {
                if (!isAllowOperationIfReposDown) {
                    if (e instanceof WIMException) {
                        throw (WIMException) e;
                    }
                    throw new WIMException(e);
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, METHODNAME + " IGNORE: exception [" + e.getMessage() + "] on repository [" + key + "]");
                    failureRepositoryIds.add(key);
                    continue;
                }
            }

            if (ctrlMap.containsKey(Service.DO_GROUP_MEMBERSHIP_CONTROL)) {
                GroupMembershipControl ctrl = (GroupMembershipControl) ctrlMap.get(Service.DO_GROUP_MEMBERSHIP_CONTROL);

                // If a group membership control is passed, check if there is also a clear cache control
                CacheControl clearCacheCtrl = null;

                // If the control is found then extract it and pass it to the repository while getting groups
                if (ctrlMap.containsKey(Service.DO_CACHE_CONTROL)) {
                    clearCacheCtrl = (CacheControl) ctrlMap.get(Service.DO_CACHE_CONTROL);
                }

                groupMembershipLookup(retDgRoot, key, ctrl, isAllowOperationIfReposDown, failureRepositoryIds, clearCacheCtrl);

            }
            if (ctrlMap.containsKey(Service.DO_GROUP_MEMBER_CONTROL)) {
                GroupMemberControl ctrl = (GroupMemberControl) ctrlMap.get(Service.DO_GROUP_MEMBER_CONTROL);

                // If a group member control is passed, check if there is also a clear cache control
                CacheControl clearCacheCtrl = null;

                // If the control is found then extract it and pass it to the repository while getting members
                if (ctrlMap.containsKey(Service.DO_CACHE_CONTROL)) {
                    clearCacheCtrl = (CacheControl) ctrlMap.get(Service.DO_CACHE_CONTROL);
                }

                groupMembershipLookup(retDgRoot, key, ctrl, isAllowOperationIfReposDown, failureRepositoryIds, clearCacheCtrl);

            }

            prepareDataGraphForCaller(retDgRoot, null, null, isAllowOperationIfReposDown, failureRepositoryIds);

            returnedDataGraphes.put(key, retDgRoot);
        }

        // merge entities
        List<Entity>[] retEntities = new List[returnedDataGraphes.size()];
        index = 0;
        for (Root dgRoot : returnedDataGraphes.values()) {
            retEntities[index++] = dgRoot.getEntities();
            List<Control> ctrls = dgRoot.getControls();
            List<Context> contxs = dgRoot.getContexts();

            retRoot.getControls().addAll(ctrls);
            retRoot.getContexts().addAll(contxs);
        }

        List<Entity> retEntityList = mergeEntitiesList(retEntities);

        //Sorting
        SortControl sortCtrl = (SortControl) ctrlMap.get(Service.DO_SORT_CONTROL);
        if (sortCtrl != null) {
            List<SortKeyType> sortKeys = sortCtrl.getSortKeys();

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, WIMTraceHelper.printObjectArray(new Object[] {
                                                                            sortKeys
                }));
            }

            if (sortKeys == null || sortKeys.size() == 0) {
                Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.GET_AUDIT, targetReposId, uniqueName, realmName, inRoot,
                            Integer.valueOf("213"));

                throw new SortControlException(WIMMessageKey.MISSING_SORT_KEY, Tr.formatMessage(
                                                                                                tc,
                                                                                                WIMMessageKey.MISSING_SORT_KEY,
                                                                                                null));
            }

            SortHandler sortHandler = new SortHandler(sortCtrl);
            retEntityList = sortHandler.sortEntities(retEntityList);
        }

        retRoot.getEntities().clear();
        retRoot.getEntities().addAll(retEntityList);

        unsetExternalId(retRoot);

        if (isAllowOperationIfReposDown) {
            Context context = new Context();
            retRoot.getContexts().add(context);
            context.setKey(Service.VALUE_CONTEXT_FAILURE_REPOSITORY_IDS_KEY);
            context.setValue(failureRepositoryIds);
        }

        Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.GET_AUDIT, targetReposId, uniqueName,
                    realmName != null ? realmName : getRealmNameOrFirstBest(retRoot), retRoot, Integer.valueOf("200"));

        return retRoot;
    }

    @Trivial
    private void unsetExternalId(Root root) {
        if (root != null) {
            List<Entity> entities = root.getEntities();
            if (entities != null) {
                for (Entity entity : entities) {
                    IdentifierType identifier = entity.getIdentifier();

                    if (identifier != null) {
                        identifier.setExternalId(null);
                    }
                }
            }
        }
    }

    private Root searchImpl(Root inRoot) throws WIMException {
        String METHODNAME = "searchImpl(Root inRoot)";
        String targetReposId = null;
        String uniqueName = null;

        // boolean firstCall = true;
        List<Entity> mergedEnts = null;
        Root retRootDO = null;
        boolean bFirstChangeSearchCall = false;
        // Holds the list of change response controls from each adapter
        ChangeResponseControl[] changeResponseCtrls = null;
        boolean isURBrigeResult = false;

        if (getRepositoryManager() == null) {
            throw new WIMException("No Repositories found");
        }
        int numOfRepos = getRepositoryManager().getNumberOfRepositories();
        Map<String, List<String>> reposSearchBases = new HashMap<String, List<String>>();

        AuditManager auditManager = new AuditManager();

        // for audit reporting purposes, only if we have one repository configured, else we have to rely on a valid parentDN to get to the
        // correct repositoryId
        if (numOfRepos == 1) {
            try {
                targetReposId = repositoryManager.getRepoIds().get(0);
            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "numOfRepos was " + numOfRepos, e);
                }
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "targetReposID = ", targetReposId);
        }
        auditManager.setRepositoryId(targetReposId);
        auditManager.setRequestType(AuditConstants.SEARCH_AUDIT);

        List<Entity> entitys = inRoot.getEntities();
        if (entitys != null && !entitys.isEmpty()) {
            Entity entitee = entitys.get(0);
            if (entitee != null) {
                IdentifierType identifier = entitee.getIdentifier();
                if (identifier != null)
                    uniqueName = identifier.getUniqueName();
            }
        }

        String realmName = getRealmName(inRoot);

        Root returnDO = null;

        Map<String, Control> ctrlMap = ControlsHelper.getControlMap(inRoot);
        int searchCountLimit = 0;
        int searchLimit = 0;
        int pageSize = 0;
        int startIndex = 0;
        String cacheKey = null;

        boolean isAllowOperationIfReposDown = false;
        boolean setByContext = false;
        Set<String> failureRepositoryIds = new HashSet<String>();

        List<Context> contexts = inRoot.getContexts();
        if (contexts != null && contexts.size() > 0) {
            for (Context contextInput : contexts) {
                String key = contextInput.getKey();
                if (key != null && Service.CONFIG_PROP_ALLOW_OPERATION_IF_REPOS_DOWN.equals(key)) {
                    isAllowOperationIfReposDown = ((Boolean) contextInput.getValue()).booleanValue();
                    setByContext = true;
                }
            }
        }

        // Identify whether this is a search for changed entities or a normal search
        boolean bChangeSearch = true;
        SearchControl searchControl = (SearchControl) ctrlMap.get(DO_CHANGE_CONTROL);
        if (searchControl == null) {
            bChangeSearch = false;
            searchControl = (SearchControl) ctrlMap.get(DO_SEARCH_CONTROL);
        } else {
            List<CheckPointType> checkpoint = ((ChangeControl) searchControl).getCheckPoint();
            if (checkpoint.size() == 0) {
                bFirstChangeSearchCall = true;
            }
        }
        PageControl pageControl = (PageControl) ctrlMap.get(DO_PAGE_CONTROL);
        SortControl sortControl = (SortControl) ctrlMap.get(DO_SORT_CONTROL);

        if (pageControl != null && searchControl != null)
            cacheKey = getPageCacheKey(searchControl, sortControl);

        if (searchControl == null && pageControl == null) {
            Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.SEARCH_AUDIT, targetReposId, uniqueName, realmName, inRoot,
                        Integer.valueOf("214"));

            throw new MissingSearchControlException(WIMMessageKey.MISSING_SEARCH_CONTROL, Tr.formatMessage(
                                                                                                           tc,
                                                                                                           WIMMessageKey.MISSING_SEARCH_CONTROL,
                                                                                                           null));
        } else if (pageControl != null && searchControl != null && pagingSearchCache != null && pagingSearchCache.containsKey(cacheKey)) {
            pageSize = pageControl.getSize();
            startIndex = pageControl.getStartIndex();

            PageCacheEntry entry = (PageCacheEntry) pagingSearchCache.get(cacheKey);
            int numEntities = 0;

            Root cachedRootDO = null;
            List<Entity> entities = null;
            if (entry != null) {
                cachedRootDO = entry.getDataObject();
                if (cachedRootDO != null) {
                    entities = cachedRootDO.getEntities();
                    if (entities != null) {
                        numEntities = entities.size();
                    }
                }
            }
            if (pageSize == 0) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, METHODNAME + " clean up the paging cache entry");
                }
                pagingSearchCache.invalidate(cacheKey);
                returnDO = new Root();
            } else {
                returnDO = new Root();
                List<Entity> retDOEntities = null;

                /*
                 * Return all of the entities unless the allowed page size is less than
                 * the number of returned entities or if the starting index is not 0.
                 */
                if (numEntities > pageSize || startIndex > 0) {
                    /*
                     * Iterate only if startIndex is less than total search result size.
                     */
                    if (startIndex < numEntities) {
                        /*
                         * Determine the end of the entities to include in this request.
                         */
                        int toIndex = startIndex + pageSize;
                        toIndex = (toIndex < numEntities) ? toIndex : numEntities;

                        /*
                         * Copy the range of entities into the entities to return.
                         */
                        retDOEntities = entities.subList(startIndex, toIndex);
                    }
                } else {
                    retDOEntities = entities;
                    pagingSearchCache.invalidate(cacheKey);
                }

                if (retDOEntities != null) {
                    returnDO.getEntities().addAll(retDOEntities);
                }

                PageResponseControl respPageCtrl = new PageResponseControl();
                returnDO.getControls().add(respPageCtrl);
                respPageCtrl.setTotalSize(numEntities);
            }

            unsetExternalId(returnDO);

            return returnDO;
        } else if (searchControl != null) {
            if (sortControl != null) {
                List<SortKeyType> sortKeys = sortControl.getSortKeys();
                List<String> propNames = searchControl.getProperties();

                for (SortKeyType sortKey : sortKeys) {
                    String propName = sortKey.getPropertyName();

                    if (!propNames.contains(propName)) {
                        propNames.add(propName);
                    }
                }
            }

            searchCountLimit = searchControl.getCountLimit();
            if (searchCountLimit < 0) {
                // New:: Modified to remove exception for backward compatibility with Stand-alone ldap
                return new Root();
                /*
                 * throw new SearchControlException(WIMMessageKey.INCORRECT_COUNT_LIMIT, TraceNLS.getFormattedMessage(
                 * this.getClass(),
                 * TraceConstants.MESSAGE_BUNDLE,
                 * WIMMessageKey.INCORRECT_COUNT_LIMIT,
                 * WIMMessageHelper.generateMsgParms(Integer.valueOf(searchCountLimit)),
                 * WIMDefaultMessage.INCORRECT_COUNT_LIMIT));
                 */
            } else if (searchCountLimit > 0) {
                searchControl.setCountLimit(searchCountLimit + 1);
            } else {
                searchControl.setCountLimit(getConfigManager().getMaxSearchResults() + 1);
            }
            searchLimit = searchControl.getSearchLimit();
            if (searchLimit < 0) {
                Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.SEARCH_AUDIT, targetReposId, uniqueName, realmName,
                            inRoot,
                            Integer.valueOf("215"));

                throw new SearchControlException(WIMMessageKey.INCORRECT_SEARCH_LIMIT, Tr.formatMessage(
                                                                                                        tc,
                                                                                                        WIMMessageKey.INCORRECT_SEARCH_LIMIT,
                                                                                                        WIMMessageHelper.generateMsgParms(Integer.valueOf(searchLimit))));
            }
            long timeLimit = searchControl.getTimeLimit();

            if (searchCountLimit > 0 && pageControl != null) {
                Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.SEARCH_AUDIT, targetReposId, uniqueName, realmName,
                            inRoot,
                            Integer.valueOf("216"));

                throw new SearchControlException(WIMMessageKey.CANNOT_SPECIFY_COUNT_LIMIT, Tr.formatMessage(
                                                                                                            tc,
                                                                                                            WIMMessageKey.CANNOT_SPECIFY_COUNT_LIMIT,
                                                                                                            null));
            }
            if (timeLimit <= 0) {
                searchControl.setTimeLimit(getConfigManager().getSearchTimeOut());
            }
            List<String> searchBases = searchControl.getSearchBases();

            if (bChangeSearch) {
                List<String> changeTypes = ((ChangeControl) searchControl).getChangeTypes();
                // Validating ChangeTypes
                validateChangeTypes(changeTypes);
            }
            String realm = getRealmName(inRoot);
            if (!setByContext)
                isAllowOperationIfReposDown = getConfigManager().isAllowOpIfRepoDown(realm);

            boolean isSearchBaseSet = false;
            if (searchBases.size() > 0) {
                reposSearchBases = divideSearchBases(searchBases, realm, reposSearchBases);
                isSearchBaseSet = true;
            } else {
                reposSearchBases = getSearchBasesFromRealm(realm, reposSearchBases);
            }

            mergedEnts = null;

            // check if there are more than one profile repositories
            if (getRepositoryManager().isPropertyJoin()) {
                String searchExpr = searchControl.getExpression();
                if (!bFirstChangeSearchCall) {
                    if (searchExpr == null || searchExpr.length() == 0) {
                        Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.SEARCH_AUDIT, targetReposId, uniqueName,
                                    realmName, inRoot,
                                    Integer.valueOf("217"));

                        throw new SearchControlException(WIMMessageKey.MISSING_SEARCH_EXPRESSION, Tr.formatMessage(
                                                                                                                   tc,
                                                                                                                   WIMMessageKey.MISSING_SEARCH_EXPRESSION,
                                                                                                                   null));
                    }
                }

                List<String> props = searchControl.getProperties();
                boolean returnSubTypes = searchControl.isReturnSubType();
                List<String> entityTypes = getConfigManager().getSupportedEntityTypes();

                List<Entity>[] reposEntities = new List[numOfRepos];

                changeResponseCtrls = new ChangeResponseControl[numOfRepos];

                String exceptionMessage = "";
                Boolean propFound = false;
                List<String> reposIds = getRepositoryManager().getRepoIds();
                for (int i = 0; i < reposIds.size(); i++) {
                    String reposId = reposIds.get(i);
                    if (reposSearchBases != null) {
                        List<String> srchBases = reposSearchBases.get(reposId);
                        if (srchBases != null && srchBases.size() > 0) {
                            searchControl.getSearchBases().clear();
                            searchControl.getSearchBases().addAll(srchBases);
                            if (isSearchBaseSet) {
                                Context context = new Context();
                                inRoot.getContexts().add(context);
                                context.setKey(PROP_REALM);
                                context.setValue("n/a"); // Indicates search bases were set by client
                            }
                        } else if (numOfRepos == 1) {
                            searchControl.getSearchBases().clear();
                        } else {
                            continue;
                        }
                    }

                    Root search_resultDO = null;
                    XPathNode node = null;
                    List<String> entTypes = null;

                    if (searchExpr != null) {
                        WIMXPathInterpreter parser = new WIMXPathInterpreter(new StringReader(searchExpr));
                        ProfileManagerMetadataMapper mapper = new ProfileManagerMetadataMapper(reposId, entityTypes);

                        try {
                            node = parser.parse(mapper);
                            entTypes = parser.getEntityTypes();
                            propFound = true;
                        } catch (AttributeNotSupportedException anse) {
                            exceptionMessage = anse.getMessage();
                            continue;
                        } catch (ParseException pe) {
                            Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.SEARCH_AUDIT, targetReposId, uniqueName,
                                        realmName, inRoot,
                                        Integer.valueOf("218"));

                            throw new SearchControlException(WIMMessageKey.SEARCH_EXPRESSION_ERROR, Tr.formatMessage(
                                                                                                                     tc,
                                                                                                                     WIMMessageKey.SEARCH_EXPRESSION_ERROR,
                                                                                                                     WIMMessageHelper.generateMsgParms(pe.getMessage())));
                        } catch (TokenMgrError e) {
                            Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.SEARCH_AUDIT, targetReposId, uniqueName,
                                        realmName, inRoot,
                                        Integer.valueOf("219"));

                            throw new SearchControlException(WIMMessageKey.INVALID_SEARCH_EXPRESSION, Tr.formatMessage(
                                                                                                                       tc,
                                                                                                                       WIMMessageKey.INVALID_SEARCH_EXPRESSION,
                                                                                                                       WIMMessageHelper.generateMsgParms(searchExpr)));
                        }
                    } else {
                        propFound = true;
                    }
                    HashMap<String, List<String>> returnProps = validateAndDivideReturnProperties(entTypes, props, reposId, returnSubTypes);

                    short nodeType;
                    if (node != null
                        && ((nodeType = node.getNodeType()) == XPathNode.NODE_FED_LOGICAL || nodeType == XPathNode.NODE_FED_PARENTHESIS)) {
                        search_resultDO = splitSearch(reposId, entTypes, node, inRoot, returnProps, isAllowOperationIfReposDown, failureRepositoryIds);
                    } else {
                        search_resultDO = propertyJoinSearch(reposId, node, inRoot, returnProps, isAllowOperationIfReposDown, failureRepositoryIds);
                    }

                    if (search_resultDO != null) {
                        reposEntities[i] = search_resultDO.getEntities();
                        Map<String, Control> responseCtrlMap = ControlsHelper.getControlMap(search_resultDO);
                        changeResponseCtrls[i] = (ChangeResponseControl) responseCtrlMap.get(DO_CHANGE_RESPONSE_CONTROL);
                    }
                }
                //If the attribute in the search expression is not supported by any repository where search is to be performed,then
                // SearhControlException is thrown.
                if (!propFound) {
                    Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.SEARCH_AUDIT, targetReposId, uniqueName, realmName,
                                inRoot,
                                Integer.valueOf("218"));

                    throw new SearchControlException(WIMMessageKey.SEARCH_EXPRESSION_ERROR, Tr.formatMessage(
                                                                                                             tc,
                                                                                                             WIMMessageKey.SEARCH_EXPRESSION_ERROR,
                                                                                                             WIMMessageHelper.generateMsgParms(exceptionMessage)));
                }

                mergedEnts = mergeRepositoryEntities(reposEntities, isAllowOperationIfReposDown, failureRepositoryIds);
            } else {
                List<Entity>[] reposEntities = new List[numOfRepos];
                changeResponseCtrls = new ChangeResponseControl[numOfRepos];
                List<String> reposIds = getRepositoryManager().getRepoIds();
                for (int i = 0; i < reposIds.size(); i++) {
                    String reposId = reposIds.get(i);
                    if (reposSearchBases != null) {
                        List<String> srchBases = reposSearchBases.get(reposId);
                        if (srchBases != null && srchBases.size() > 0) {
                            //srchBases.size() will be one every time for change log request
                            searchControl.getSearchBases().clear();
                            searchControl.getSearchBases().addAll(srchBases);
                            if (isSearchBaseSet) {
                                Context context = new Context();
                                inRoot.getContexts().add(context);
                                context.setKey(PROP_REALM);
                                context.setValue("n/a"); // Indicates search bases were set by client
                            }
                            Root result = searchRepository(reposId, inRoot, null, isAllowOperationIfReposDown, failureRepositoryIds);
                            isURBrigeResult = isURBridgeResult(result);
                            if (result != null) {
                                List<Entity> ents = result.getEntities();
                                if (ents != null) {
                                    reposEntities[i] = ents;
                                }
                                Map<String, Control> responseCtrlMap = ControlsHelper.getControlMap(result);
                                changeResponseCtrls[i] = (ChangeResponseControl) responseCtrlMap.get(DO_CHANGE_RESPONSE_CONTROL);
                            }
                        } else if (numOfRepos == 1) {
                            searchControl.getSearchBases().clear();
                            Root result = searchRepository(reposId, inRoot, null, isAllowOperationIfReposDown, failureRepositoryIds);
                            isURBrigeResult = isURBridgeResult(result);
                            if (result != null) {
                                List<Entity> ents = result.getEntities();
                                if (ents != null) {
                                    reposEntities[i] = ents;
                                }
                                Map<String, Control> responseCtrlMap = ControlsHelper.getControlMap(result);
                                changeResponseCtrls[i] = (ChangeResponseControl) responseCtrlMap.get(DO_CHANGE_RESPONSE_CONTROL);
                            }
                        }
                    } else {
                        Root result = searchRepository(reposId, inRoot, null, isAllowOperationIfReposDown, failureRepositoryIds);
                        isURBrigeResult = isURBridgeResult(result);
                        if (result != null) {
                            List<Entity> ents = result.getEntities();
                            if (ents != null) {
                                reposEntities[i] = ents;
                            }
                            Map<String, Control> responseCtrlMap = ControlsHelper.getControlMap(result);
                            changeResponseCtrls[i] = (ChangeResponseControl) responseCtrlMap.get(DO_CHANGE_RESPONSE_CONTROL);
                        }
                    }
                } // for (numOfReposs)
                mergedEnts = mergeRepositoryEntities(reposEntities, isAllowOperationIfReposDown, failureRepositoryIds);
            }
        }

        retRootDO = new Root();

        int reEntitySize = mergedEnts.size();
        if (searchLimit <= 0) {
            searchLimit = getConfigManager().getMaxSearchResults();
        } else if (getConfigManager().getMaxSearchResults() > 0) {
            searchLimit = (getConfigManager().getMaxSearchResults() > searchLimit ? searchLimit : getConfigManager().getMaxSearchResults());
        }
        if (searchLimit > 0 && reEntitySize > searchLimit) {
            Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.SEARCH_AUDIT, targetReposId, uniqueName, realmName, inRoot,
                        Integer.valueOf("220"));

            throw new MaxResultsExceededException(WIMMessageKey.EXCEED_MAX_TOTAL_SEARCH_LIMIT, Tr.formatMessage(
                                                                                                                tc,
                                                                                                                WIMMessageKey.EXCEED_MAX_TOTAL_SEARCH_LIMIT,
                                                                                                                WIMMessageHelper.generateMsgParms(Integer.toString(reEntitySize),
                                                                                                                                                  Integer.toString(searchLimit))));
        }

        //checking the no. of "wim:ChangeResponseControl" retrieved after searching and clubing in all the check point inside them into one change "wim:ChangeResponseControl"
        if (bChangeSearch && (changeResponseCtrls != null) && (changeResponseCtrls.length > 0)) {
            // Consolidate change response controls from each adapter
            ChangeResponseControl changeResponseCtrl = new ChangeResponseControl();
            retRootDO.getControls().add(changeResponseCtrl);
            for (int i = 0; i < changeResponseCtrls.length; i++) {
                if (changeResponseCtrls[i] != null) {
                    // Note: Only one checkpoint expected in the response control from each adapter
                    CheckPointType reposCheckPoint = changeResponseCtrls[i].getCheckPoint().get(0);
                    if (reposCheckPoint.getRepositoryCheckPoint() != null) {
                        changeResponseCtrl.getCheckPoint().add(reposCheckPoint);
                    }
                }
            }
        }

        if (searchCountLimit > 0 && searchCountLimit < reEntitySize) {
            reEntitySize = searchCountLimit;
            SearchResponseControl srchResponseCtrl = new SearchResponseControl();
            srchResponseCtrl.setHasMoreResults(true);
            retRootDO.getControls().add(srchResponseCtrl);
        }

        List<Entity> processedEnts = new ArrayList<Entity>();
        for (Entity entity : mergedEnts) {
            String qualifiedEntityType = entity.getTypeName();
            processReferenceProperty(entity, qualifiedEntityType, true, isAllowOperationIfReposDown, failureRepositoryIds);
            processedEnts.add(entity);
        }

        List<Entity> returnEntities = null;
        if (sortControl != null) {
            List<SortKeyType> sortKeys = sortControl.getSortKeys();

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, WIMTraceHelper.printObjectArray(new Object[] {
                                                                            sortKeys
                }));
            }
            if (sortKeys == null || sortKeys.size() == 0) {
                Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.SEARCH_AUDIT, targetReposId, uniqueName, realmName,
                            inRoot,
                            Integer.valueOf("213"));

                throw new SortControlException(WIMMessageKey.MISSING_SORT_KEY, Tr.formatMessage(
                                                                                                tc,
                                                                                                WIMMessageKey.MISSING_SORT_KEY,
                                                                                                null));
            }
            SortHandler sortHandler = new SortHandler(sortControl);
            returnEntities = sortHandler.sortEntities(processedEnts);
        } else {
            returnEntities = processedEnts;
        }

        if (pageControl != null) {
            pageSize = pageControl.getSize();
            startIndex = pageControl.getStartIndex();
        }
        if (pageSize > 0 && FactoryManager.getCacheUtil().isCacheAvailable()) {
            if (pagingSearchCache == null) {
                maxTotalPagingSearchResults = getConfigManager().getPageCacheSize();
                pagingSearchResultsCacheTimeOut = getConfigManager().getPageCacheTimeOut();
                pagingSearchCache = FactoryManager.getCacheUtil().initialize("PagingSearchCache", maxTotalPagingSearchResults, maxTotalPagingSearchResults,
                                                                             pagingSearchResultsCacheTimeOut);
            }
            if (pagingSearchCache != null) {
                Root cachedRootDO = new Root();

                if (reEntitySize > pageSize || startIndex > 0) {

                    /*
                     * Iterate only if the start index is less than total result size
                     */
                    if (startIndex < returnEntities.size()) {
                        /*
                         * Determine the end of the entities to include in this request.
                         */
                        int toIndex = startIndex + pageSize;
                        toIndex = (toIndex < returnEntities.size()) ? toIndex : returnEntities.size();

                        /*
                         * Copy the range of entities into the entities to return.
                         */
                        List<Entity> retDOEntities = retRootDO.getEntities();
                        retDOEntities.addAll(returnEntities.subList(startIndex, toIndex));
                    }
                } else {
                    retRootDO.getEntities().addAll(returnEntities);
                }

                cachedRootDO.getEntities().addAll(returnEntities);

                PageCacheEntry entry = new PageCacheEntry(reEntitySize, cachedRootDO);

                pagingSearchCache.put(cacheKey, entry);

                PageResponseControl respPageCtrl = new PageResponseControl();
                retRootDO.getControls().add(respPageCtrl);
                respPageCtrl.setTotalSize(reEntitySize);
            }
        } else if (pageControl != null && pageSize == 0) {
            returnEntities.clear();
        }

        // If no page control is present then add all entities
        if (pageControl == null)
            retRootDO.getEntities().addAll(returnEntities);

        unsetExternalId(retRootDO);

        if (isAllowOperationIfReposDown) {
            Context context = new Context();
            retRootDO.getContexts().add(context);
            context.setKey(Service.VALUE_CONTEXT_FAILURE_REPOSITORY_IDS_KEY);
            context.setValue(failureRepositoryIds);
        }

        Context context = null;
        if (isURBrigeResult) {
            // Add context for URBridge
            context = new Context();
            context.setKey(SchemaConstantsInternal.IS_URBRIDGE_RESULT);
            context.setValue("true");
            retRootDO.getContexts().add(context);
        }

        if (uniqueName == null) {
            List<Context> ctxs = inRoot.getContexts();
            for (Context c : ctxs) {
                if ("useUserFilterForSearch".equals(c.getKey()) || "useGroupFilterForSearch".equals(c.getKey())) {
                    uniqueName = (String) c.getValue();
                }
            }
        }

        Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.SEARCH_AUDIT, targetReposId, uniqueName,
                    realmName != null ? realmName : getRealmNameOrFirstBest(retRootDO), retRootDO,
                    Integer.valueOf("200"));

        return retRootDO;
    }

    private Root searchRepository(String reposId, Root inputRootDO, HashMap<String, List<String>> returnProps,
                                  boolean isAllowOperationIfReposDown, Set<String> failureRepositoryIds) throws WIMException {
        String METHODNAME = "searchRepository";
        Root returnDO = null;
        List<Entity> ents = null;
        List<String> reposProps = null;
        List<String> laProps = null;
        boolean bChangeSearch = true;
        Map<String, Control> ctrlMap = ControlsHelper.getControlMap(inputRootDO);
        SearchControl srchCtrl = (SearchControl) ctrlMap.get(DO_CHANGE_CONTROL);

        if (srchCtrl == null) {
            srchCtrl = (SearchControl) ctrlMap.get(DO_SEARCH_CONTROL);
            bChangeSearch = false;
        }

        if (returnProps != null) {
            reposProps = returnProps.get(REPOS);
            // laProps = returnProps.get(LA);
            if (reposProps != null) {
                srchCtrl.getProperties().addAll(reposProps);
            } else {
                srchCtrl.getProperties().clear();
            }
        }

        try {

            //If search for changed entities
            if (bChangeSearch) {
                //Check whether flag ChangeLogSupport is true or false
                boolean bChangeLogSupport = isConfigChangeLogSupportEnabled(reposId);
                //If search for changed entities
                if (bChangeLogSupport) {
                    //Search Repository
                    Root searchDO = keepCheckPointForReposOnly(inputRootDO, reposId);
                    returnDO = getRepositoryManager().getRepository(reposId).search(searchDO);
                } else {
                    //Create a ChangeResponseControl without any checkpoint for this repository
                    returnDO = new Root();
                    ChangeResponseControl changeResponseCtrl = new ChangeResponseControl();
                    returnDO.getControls().add(changeResponseCtrl);
                    CheckPointType checkPointDO = new CheckPointType();
                    checkPointDO.setRepositoryId(reposId);
                    changeResponseCtrl.getCheckPoint().add(checkPointDO);
                }
            } else {
                returnDO = getRepositoryManager().getRepository(reposId).search(inputRootDO);
            }
        } catch (Exception e) {
            if (!isAllowOperationIfReposDown) {
                if (e instanceof WIMException) {
                    throw (WIMException) e;
                }
                throw new WIMException(e);
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, METHODNAME + " IGNORE: exception [" + e.getMessage() + "] on repository [" + reposId + "]");
                failureRepositoryIds.add(reposId);
            }
        }
        if (returnDO != null) {
            ents = returnDO.getEntities();
        }

        if (returnDO != null) {
            ents = returnDO.getEntities();
            if (ents != null && ents.size() > 0) {
                // set uuid and reposUUID
                for (int j = 0; j < ents.size(); j++) {
                    IdentifierType entIdDO = ents.get(j).getIdentifier();
                    String extId = entIdDO.getExternalId();
                    entIdDO.setRepositoryId(reposId);
                    if (!getRepositoryManager().isEntryJoin()) {
                        entIdDO.setUniqueId(extId);
                    }
                }
            }

        }
        if (tc.isDebugEnabled()) {
            if (ents != null)
                Tr.debug(tc, METHODNAME + " returning " + ents.size() + " entities");
            else
                Tr.debug(tc, METHODNAME + " returning null");
        }
        return returnDO;
    }

    /**
     * This method is invoked by searchImpl() to make sure that each
     * adapter receives only its corresponding checkpoint in the change
     * control.
     *
     * @param searchDO Input search DataObject
     * @param reposId Identifier for the repository whose checkpoint needs to be retained
     * @return Object for search containing checkpoint corresponding to reposId only
     */
    @Trivial
    private Root keepCheckPointForReposOnly(Root searchDO, String reposId) {
        Root retDO = new Root();
        Map<String, Control> ctrlsMap = ControlsHelper.getControlMap(searchDO);
        ChangeControl changeCtrl = (ChangeControl) ctrlsMap.get(DO_CHANGE_CONTROL);
        ChangeControl returnChangeControl = new ChangeControl();
        CheckPointType returnCheckPoint = new CheckPointType();
        returnChangeControl.getCheckPoint().add(returnCheckPoint);
        retDO.getControls().add(returnChangeControl);
        List<CheckPointType> checkPointList = changeCtrl.getCheckPoint();
        if (checkPointList != null) {
            for (CheckPointType checkPointDO : checkPointList) {
                if (checkPointDO.getRepositoryId().equals(reposId)) {
                    returnCheckPoint.setRepositoryCheckPoint(checkPointDO.getRepositoryCheckPoint());
                    returnCheckPoint.setRepositoryId(checkPointDO.getRepositoryId());
                }
            }
        }
        return retDO;
    }

    @Trivial
    private List<Entity> mergeRepositoryEntities(List<Entity>[] reposEntities, boolean isAllowOperationIfReposDown,
                                                 Set<String> failureRepositoryIds) {
        List<Entity> mergedEntities = null;
        if (reposEntities != null && reposEntities.length > 0) {
            mergedEntities = new ArrayList<Entity>();
            for (int j = 0; j < reposEntities.length; j++) {
                if (reposEntities[j] != null) {
                    if (!getRepositoryManager().isEntryJoin()) {
                        mergedEntities.addAll(reposEntities[j]);
                    }
                }
            }
        }
        return mergedEntities;
    }

    private Root propertyJoinSearch(String reposId, XPathNode node, Root inputRootDO, HashMap<String, List<String>> returnProps,
                                    boolean isAllowOperationIfReposDown, Set<String> failureRepositoryIds) throws WIMException {
        boolean inRepos = true;
        if (node != null) {
            short nodeType = node.getNodeType();
            switch (nodeType) {
                case XPathNode.NODE_LOGICAL:
                    inRepos = ((LogicalNode) node).isPropertyInRepository();
                    break;
                case XPathNode.NODE_PARENTHESIS:
                    inRepos = ((ParenthesisNode) node).isPropertyInRepository();
                    break;
                case XPathNode.NODE_PROPERTY:
                    inRepos = ((PropertyNode) node).isPropertyInRepository();
                    break;
                default:
                    break;
            }
        }

        Root result = null;
        if (inRepos) {
            result = searchRepository(reposId, inputRootDO, returnProps, isAllowOperationIfReposDown, failureRepositoryIds);
        }
        return result;
    }

    private Root splitSearch(String reposId, List<String> entityTypes, XPathNode node, Root inputRootDO,
                             HashMap<String, List<String>> returnProps, boolean isAllowOperationIfReposDown,
                             Set<String> failureRepositoryIds) throws WIMException {
        Root search_resultDO = null;
        short nodeType = node.getNodeType();
        switch (nodeType) {
            case XPathNode.NODE_FED_PARENTHESIS:
                node = (XPathNode) ((FederationParenthesisNode) node).getChild();
                search_resultDO = splitSearch(reposId, entityTypes, node, inputRootDO, returnProps,
                                              isAllowOperationIfReposDown, failureRepositoryIds);
                break;
            case XPathNode.NODE_FED_LOGICAL:
                FederationLogicalNode fNode = (FederationLogicalNode) node;
                XPathNode leftChild = (XPathNode) fNode.getLeftChild();
                XPathNode rightChild = (XPathNode) fNode.getRightChild();
                String operator = fNode.getOperator();
                Root leftDO = splitSearch(reposId, entityTypes, leftChild, inputRootDO, returnProps,
                                          isAllowOperationIfReposDown, failureRepositoryIds);
                Root rightDO = splitSearch(reposId, entityTypes, rightChild, inputRootDO, returnProps,
                                           isAllowOperationIfReposDown, failureRepositoryIds);
                if (operator.equals("or")) {
                    search_resultDO = leftDO;
                    // Iterate over the left DO entities and create a list of unique names. This list would be used to check for duplicates
                    List<Entity> leftEnts = leftDO.getEntities();
                    List<String> leftUniqueNames = new ArrayList<String>(leftEnts.size());
                    for (Entity ent : leftEnts) {
                        IdentifierType id = ent.getIdentifier();
                        if (id != null) {
                            String uniqueName = id.getUniqueName();
                            if (uniqueName != null) {
                                leftUniqueNames.add(uniqueName.toLowerCase());
                            }
                        }
                    }

                    List<Entity> rightEnts = rightDO.getEntities();

                    for (Entity ent : rightEnts) {
                        IdentifierType id = ent.getIdentifier();
                        if (id != null) {
                            String uniqueName = id.getUniqueName();
                            if (uniqueName != null) {
                                // If this entity is not already considered then add it to search results
                                if (!leftUniqueNames.contains(uniqueName.toLowerCase())) {
                                    search_resultDO.getEntities().add(ent);
                                }
                            }
                        }
                    }
                } else { // AND
                    List<Entity> leftEnts = leftDO.getEntities();
                    List<String> leftUniqueNames = new ArrayList<String>(leftEnts.size());
                    for (int i = 0; i < leftEnts.size(); i++) {
                        Entity ent = leftEnts.get(i);
                        IdentifierType id = ent.getIdentifier();
                        if (id != null) {
                            String uniqueName = id.getUniqueName();
                            if (uniqueName != null) {
                                leftUniqueNames.add(uniqueName.toLowerCase());
                            } else {
                                leftEnts.remove(i); // remove useless entries from the list!
                                i--; // Do not want to skip the next item!
                            }
                        }
                    }
                    List<Entity> rightEnts = rightDO.getEntities();
                    List<String> rightUniqueNames = new ArrayList<String>(rightEnts.size());
                    for (int i = 0; i < rightEnts.size(); i++) {
                        Entity ent = rightEnts.get(i);
                        IdentifierType id = ent.getIdentifier();
                        if (id != null) {
                            String uniqueName = id.getUniqueName();
                            if (uniqueName != null) {
                                rightUniqueNames.add(uniqueName.toLowerCase());
                            } else {
                                rightEnts.remove(i);// remove useless entries from the list!
                                i--; // Do not want to skip the next item!
                            }
                        }
                    }
                    if (leftEnts.size() < rightEnts.size()) {
                        search_resultDO = leftDO;

                        List<Entity> searchEnts = search_resultDO.getEntities();
                        for (int j = 0; j < searchEnts.size(); j++) {
                            Entity ent = searchEnts.get(j);
                            IdentifierType id = ent.getIdentifier();
                            if (id != null) {
                                String uniqueName = id.getUniqueName();
                                if (!rightUniqueNames.contains(uniqueName.toLowerCase())) // check for it's relation with right List
                                {
                                    searchEnts.remove(j); // Remove as entry is not there in LEFTDO
                                    j--; // don;t skip entry after this
                                }
                            }
                        }
                    } else {
                        search_resultDO = rightDO;
                        List<Entity> searchEnts = search_resultDO.getEntities();
                        for (int j = 0; j < searchEnts.size(); j++) {
                            Entity ent = searchEnts.get(j);
                            IdentifierType id = ent.getIdentifier();
                            if (id != null) {
                                String uniqueName = id.getUniqueName();
                                if (!leftUniqueNames.contains(uniqueName.toLowerCase())) {
                                    searchEnts.remove(j); // Remove as entry is not there in RIGHTDO
                                    j--; // don;t skip entry after this
                                }
                            }
                        }
                    }
                }

                break;
            default:
                inputRootDO = prepareSearchExpression(entityTypes, node, inputRootDO);
                search_resultDO = propertyJoinSearch(reposId, node, inputRootDO, returnProps, isAllowOperationIfReposDown, failureRepositoryIds);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, WIMTraceHelper.printObjectArray(new Object[] {
                                                                        search_resultDO
            }));
        }
        return search_resultDO;
    }

    private Root prepareSearchExpression(List<String> entityTypes, XPathNode node, Root inputRootDO) {
        String searchEntityTypeTemplate = "@xsi:type='%ENTITYTYPE%'";
        String leftBracket = "(";
        String rightBracket = ")";
        String and = " and ";
        String or = " or ";
        String ExpressionTemplate = "(%SEARCH_STRING%)";
        StringBuffer srchExpr = new StringBuffer();
        StringBuffer entExpr = new StringBuffer();
        StringBuffer condExpr = new StringBuffer();
        if (inputRootDO != null && node != null && entityTypes != null) {

            if (entityTypes.size() == 1) {
                String entType = searchEntityTypeTemplate.replaceFirst("%ENTITYTYPE%", entityTypes.get(0));
                entExpr.append(entType);
            } else if (entityTypes.size() > 1) {
                entExpr.append(leftBracket);
                String entType = searchEntityTypeTemplate.replaceFirst("%ENTITYTYPE%", entityTypes.get(0));
                entExpr.append(entType);
                for (int i = 1; i < entityTypes.size(); i++) {
                    entExpr.append(or);
                    entType = searchEntityTypeTemplate.replaceFirst("%ENTITYTYPE%", entityTypes.get(i));
                    entExpr.append(entType);
                }
                entExpr.append(rightBracket);
            }

            short nodeType = node.getNodeType();
            switch (nodeType) {
                case XPathNode.NODE_PARENTHESIS:
                    node = (XPathNode) ((ParenthesisNode) node).getChild();
                default:
                    nodeToString(condExpr, node);
            }
            srchExpr.append(entExpr);
            srchExpr.append(and);
            String cExpr = ExpressionTemplate.replaceFirst("%SEARCH_STRING%", condExpr.toString());
            srchExpr.append(cExpr);

            Map<String, Control> ctrlMap = ControlsHelper.getControlMap(inputRootDO);
            SearchControl searchControl = (SearchControl) ctrlMap.get(DO_SEARCH_CONTROL);
            searchControl.setExpression(srchExpr.toString());
        }
        return inputRootDO;
    }

    private StringBuffer nodeToString(StringBuffer searchExpr, XPathNode node) {
        if (node != null && searchExpr != null) {
            short nodeType = node.getNodeType();
            switch (nodeType) {
                case XPathNode.NODE_LOGICAL:
                    searchExpr = nodeToString(searchExpr, (XPathNode) ((LogicalNode) node).getLeftChild());
                    searchExpr.append(" " + ((LogicalNode) node).getOperator() + " ");
                    searchExpr = nodeToString(searchExpr, (XPathNode) ((LogicalNode) node).getRightChild());
                    break;
                case XPathNode.NODE_PARENTHESIS:
                    searchExpr = nodeToString(searchExpr, (XPathNode) ((ParenthesisNode) node).getChild());
                    break;
                case XPathNode.NODE_PROPERTY:
                    searchExpr.append(node.toString());
                    break;
                default:
                    break;
            }
        }
        return searchExpr;
    }

    private HashMap<String, List<String>> validateAndDivideReturnProperties(List<String> entityTypes, List<String> props, String reposId,
                                                                            boolean returnSubTypes) {
        String METHODNAME = "validateAndDivideReturnProperties";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, WIMTraceHelper.printObjectArray(new Object[] {
                                                                        entityTypes, props, reposId
            }));
        }
        HashMap<String, List<String>> returnProps = new HashMap<String, List<String>>();
        List<String> reposProps = new ArrayList<String>(props);
        List<String> laProps = new ArrayList<String>(props);

        if (entityTypes != null && returnSubTypes) {
            List<String> subTypes = new ArrayList<String>();
            for (int k = 0; k < entityTypes.size(); k++) {
                Set<String> types = Entity.getSubEntityTypes(entityTypes.get(k));
                if (types != null) {
                    subTypes.addAll(types);
                }
            }
            entityTypes.addAll(subTypes);
        }
        Set<String> laSupportedPropNames = propMgr.getLookAsidePropertyNameSet(entityTypes);
        Set<String> reposSupportedPropNames = propMgr.getRepositoryPropertyNameSet(reposId, entityTypes);

        List<String> reposRemovePropNames = new ArrayList<String>();
        for (int i = 0; i < reposProps.size(); i++) {
            String propName = reposProps.get(i);
            if (!reposSupportedPropNames.contains(propName) && !propName.equals(VALUE_ALL_PROPERTIES)) {
                reposRemovePropNames.add(propName);
            } else if (propName.equals(VALUE_ALL_PROPERTIES)) {
                reposProps.clear();
                reposProps.add(VALUE_ALL_PROPERTIES);
                laProps.clear();
                laProps.add(VALUE_ALL_PROPERTIES);
                returnProps.put(REPOS, reposProps);
                returnProps.put(LA, laProps);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, METHODNAME + " propName=" + VALUE_ALL_PROPERTIES + ", returnProps=" + returnProps);
                }
                return returnProps;
            }
        }
        reposProps.removeAll(reposRemovePropNames);

        List<String> laRemovePropNames = new ArrayList<String>();
        for (int i = 0; i < laProps.size() && laSupportedPropNames != null; i++) {
            String propName = laProps.get(i);
            if (!laSupportedPropNames.contains(propName)) {
                laRemovePropNames.add(propName);
            }
        }
        laProps.removeAll(laRemovePropNames);

        returnProps.put(REPOS, reposProps);
        returnProps.put(LA, laProps);
        return returnProps;
    }

    @Trivial
    private static void validateChangeTypes(List<String> changeTypes) throws ChangeControlException {
        Object curChangeType = null;
        for (int i = 0; i < changeTypes.size(); i++) {
            curChangeType = changeTypes.get(i);
            if (!((CHANGETYPE_ADD.equals(curChangeType)) ||
                  (CHANGETYPE_DELETE.equals(curChangeType)) ||
                  (CHANGETYPE_RENAME.equals(curChangeType)) ||
                  (CHANGETYPE_MODIFY.equals(curChangeType)) || (CHANGETYPE_ALL.equals(curChangeType)))) {
                throw new ChangeControlException(WIMMessageKey.INVALID_CHANGETYPE, Tr.formatMessage(
                                                                                                    tc,
                                                                                                    WIMMessageKey.INVALID_CHANGETYPE,
                                                                                                    WIMMessageHelper.generateMsgParms(curChangeType)));
            }
        }
    }

    @FFDCIgnore({ PasswordCheckFailedException.class, CertificateMapNotSupportedException.class, Exception.class })
    private Root loginImpl(Root inRoot) throws WIMException {
        final String METHODNAME = "loginImpl";

        if (inRoot == null) {
            return null;
        }

        Root result = null;

        Root root = inRoot;
        Map<String, Integer> exceptions = new HashMap<String, Integer>();
        WIMException exp = null;
        LoginControl ctrl = null;
        String reposId = null;
        String principalName = null;
        byte[] pwd = null;
        boolean isAllowOperationIfReposDown = false;
        Set<String> failureRepositoryIds = new HashSet<String>();
        int certExceptionCount = 0;

        List<Entity> entities = root.getEntities();

        if (entities.size() == 0) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.MISSING_ENTITY_DATA_OBJECT, (Object) null);
            throw new EntityNotFoundException(WIMMessageKey.MISSING_ENTITY_DATA_OBJECT, msg);
        } else if (entities.size() > 1) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.ACTION_MULTIPLE_ENTITIES_SPECIFIED, (Object) null);
            throw new OperationNotSupportedException(WIMMessageKey.ACTION_MULTIPLE_ENTITIES_SPECIFIED, msg);
        }

        LoginAccount personAccount = (LoginAccount) entities.get(0);
        principalName = personAccount.getPrincipalName();
        pwd = personAccount.getPassword();

        if (personAccount.getCertificate().size() == 0 && principalName == null) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.MISSING_OR_EMPTY_PRINCIPAL_NAME, (Object) null);
            throw new PasswordCheckFailedException(WIMMessageKey.MISSING_OR_EMPTY_PRINCIPAL_NAME, msg);
        }

        List<Context> contexts = root.getContexts();
        if (contexts != null && contexts.size() > 0) {
            for (Context contextInput : contexts) {
                String key = contextInput.getKey();
                if (key != null && Service.CONFIG_PROP_ALLOW_OPERATION_IF_REPOS_DOWN.equals(key)) {
                    isAllowOperationIfReposDown = ((Boolean) contextInput.getValue()).booleanValue();
                }
            }
        }

        Map<String, Control> ctrlMap = ControlsHelper.getControlMap(inRoot);
        ctrl = (LoginControl) ctrlMap.get(DO_LOGIN_CONTROL);
        if (ctrl == null) {
            ctrl = new LoginControl();
            inRoot.getControls().add(ctrl);
        }

        List<String> searchBases = ctrl.getSearchBases();
        //int numOfRepos = getRepositoryManager().getNumberOfRepositories();
        String realm = getRealmName(root);
        Map<String, List<String>> reposSearchBases = new HashMap<String, List<String>>();

        if (searchBases.size() > 0) {
            reposSearchBases = divideSearchBases(searchBases, realm, reposSearchBases);
        } else {
            reposSearchBases = getSearchBasesFromRealm(realm, reposSearchBases);
        }

        try {
            List<String> reposIds = getRepositoryManager().getRepoIds();
            for (int i = 0; i < reposIds.size(); i++) {
                reposId = reposIds.get(i);
                if (reposSearchBases != null) {
                    List<String> srchBases = reposSearchBases.get(reposId);
                    if (srchBases != null && srchBases.size() > 0) {
                        try {
                            Root inputRoot = inRoot;
                            Map<String, Control> cMap = ControlsHelper.getControlMap(inputRoot);
                            LoginControl lCtrl = (LoginControl) cMap.get(DO_LOGIN_CONTROL);
                            lCtrl.getSearchBases().clear();
                            lCtrl.getSearchBases().addAll(srchBases);

                            Root retRoot = getRepositoryManager().getRepository(reposId).login(inputRoot);

                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, METHODNAME + " after calling adapter ["
                                             + reposId + "]");
                            }
                            if (result == null) {
                                if (retRoot.getEntities().size() > 0) {
                                    result = retRoot;
                                }
                            } else {
                                if (retRoot.getEntities().size() > 0) {
                                    if (tc.isErrorEnabled()) {
                                        Tr.error(tc, WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, WIMMessageHelper.generateMsgParms(principalName));
                                    }
                                    String msg = Tr.formatMessage(tc, WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, WIMMessageHelper.generateMsgParms(principalName));
                                    throw new DuplicateLogonIdException(WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, msg);
                                }
                            }
                        } catch (PasswordCheckFailedException e) {
                            String msgKey = e.getMessageKey();
                            if (WIMMessageKey.PRINCIPAL_NOT_FOUND.equals(msgKey)) {
                                continue;
                            } else if (WIMMessageKey.MISSING_OR_EMPTY_PRINCIPAL_NAME.equals(msgKey)) {
                                throw e;
                            } else if (WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND.equals(msgKey)) {
                                throw e;
                            }

                            exp = e;
                            recordLoginException(e, exceptions);
                        } catch (CertificateMapFailedException e) {
                            exp = e;
                            recordLoginException(e, exceptions);
                        } catch (CertificateMapNotSupportedException e) {
                            /* Don't record these as we will only fail if all repos threw these. */
                            certExceptionCount++;
                            continue;
                        } catch (Exception e) {
                            exp = new WIMException(e);

                            // If it's truly a duplicate login exception from above, rethrow it.
                            if (e instanceof DuplicateLogonIdException)
                                throw (DuplicateLogonIdException) e;
                            else {
                                if (!isAllowOperationIfReposDown) {
                                    throw exp;
                                } else {
                                    // Otherwise log it as strange and continue
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, METHODNAME + " IGNORE: exception ["
                                                     + exp.getMessage() + "] on repository ["
                                                     + reposId + "]");
                                    failureRepositoryIds.add(reposId);

                                }
                            }
                        }
                    } else {
                        continue;
                    }
                }
            }
        } finally {
            PasswordUtil.erasePassword(pwd);
        }

        // handling exceptions
        int countedException = exceptions.size();
        if (result == null && countedException == 0) {
            /*
             * No user was authenticated, and there were no exceptions recorded. This can only happen if there
             * were PasswordCheckFailedException's and / or CertificateMapNotSupportedException's.
             */
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " result == null && countedException == 0, certExceptionCount = " + certExceptionCount);
            }
            if (certExceptionCount != 0 && certExceptionCount == getRepositoryManager().getNumberOfRepositories()) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.AUTHENTICATION_WITH_CERT_NOT_SUPPORTED);
                throw new CertificateMapNotSupportedException(WIMMessageKey.AUTHENTICATION_WITH_CERT_NOT_SUPPORTED, msg);
            } else {
                // If login was using certificate set principalName to "extracted from certificate"
                if (personAccount.getCertificate().size() > 0 && principalName == null) {
                    principalName = "extracted from certificate";
                }
                String msg = Tr.formatMessage(tc, WIMMessageKey.PRINCIPAL_NOT_FOUND, WIMMessageHelper.generateMsgParms(principalName));
                throw new PasswordCheckFailedException(WIMMessageKey.PRINCIPAL_NOT_FOUND, msg);
            }
        } else if (countedException == 1) {
            /*
             * There was a single recorded exception. Throw it up the stack.
             */
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " countedException == 1");
            }

            throw exp;
        } else if (countedException > 1) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " countedException > 1 [" + countedException + "]");
            }
            String msg = Tr.formatMessage(tc, WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, WIMMessageHelper.generateMsgParms(principalName));
            throw new DuplicateLogonIdException(WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, msg);
        } else {
            /*
             * There were no recorded exceptions and we have a result.
             */
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " login successful.");
            }
        }

        prepareDataGraphForCaller(result, null, null, isAllowOperationIfReposDown, failureRepositoryIds);

        if (isAllowOperationIfReposDown) {
            Context context = new Context();
            result.getContexts().add(context);
            context.setKey(Service.VALUE_CONTEXT_FAILURE_REPOSITORY_IDS_KEY);
            context.setValue(failureRepositoryIds);
        }

        return result;
    }

    @Trivial
    private void recordLoginException(WIMException e, Map<String, Integer> container) {
        String METHODNAME = "recordLoginException";
        //String key = e.getClass().getName();
        String key = e.getMessageKey();
        if (container.containsKey(key)) {
            int value = container.get(key).intValue() + 1;
            container.put(key, Integer.valueOf(value));
        } else {
            container.put(key, Integer.valueOf(1));
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " record exception [" + key + "]");
        }
    }

    Map<String, List<String>> getSearchBasesFromRealm(String realmName, Map<String, List<String>> reposSearchBases) throws WIMException {
        Map<String, List<String>> baseMap = null;
        //first get all baseEntries for the realm
        if (realmName != null && getConfigManager().getRealmConfig(realmName) != null) {
            String[] realmBaseEntries = getConfigManager().getRealmConfig(realmName).getParticipatingBaseEntries();
            if (realmBaseEntries == null || realmBaseEntries.length == 0) {
                throw new WIMException(WIMMessageKey.MISSING_BASE_ENTRY_IN_REALM, Tr.formatMessage(
                                                                                                   tc,
                                                                                                   WIMMessageKey.MISSING_BASE_ENTRY_IN_REALM,
                                                                                                   WIMMessageHelper.generateMsgParms(realmName)));
            }
            // now separate same basedn repository id
            baseMap = getRepositoryManager().getBaseEntriesForRepos(realmBaseEntries);
        }
        // if no matching realm participating base entries, get all baseEntries of repository
        if (baseMap == null) {
            reposSearchBases = getRepositoryManager().getRepositoriesBaseEntries();
        } else {
            reposSearchBases = baseMap;
        }

        return reposSearchBases;
    }

    private Map<String, List<String>> divideSearchBases(List<String> searchBases, String vrealmName,
                                                        Map<String, List<String>> reposSearchBases) throws WIMException {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, WIMTraceHelper.printObjectArray(new Object[] { searchBases, vrealmName, reposSearchBases }));
        }

        for (String searchBase : searchBases) {
            if (searchBase != null) {
                if (!getConfigManager().isUniqueNameInRealm(searchBase, vrealmName)) {
                    throw new WIMApplicationException(WIMMessageKey.NON_EXISTING_SEARCH_BASE, Tr.formatMessage(
                                                                                                               tc,
                                                                                                               WIMMessageKey.NON_EXISTING_SEARCH_BASE,
                                                                                                               WIMMessageHelper.generateMsgParms(searchBase)));
                }
                String reposId = getRepositoryManager().getRepositoryIdByUniqueName(searchBase);
                List<String> searchBaseList = reposSearchBases.get(reposId);
                if (searchBaseList == null) {
                    searchBaseList = new ArrayList<String>();
                }
                searchBaseList.add(searchBase);
                reposSearchBases.put(reposId, searchBaseList);
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, WIMTraceHelper.printObjectArray(new Object[] { reposSearchBases }));
        }
        return reposSearchBases;
    }

    private List<Entity> mergeEntitiesList(List<Entity>[] retEntities) {

        List<Entity> mergedEntities = new ArrayList<Entity>();
        for (int j = 0; j < retEntities.length; j++) {
            if (retEntities[j] != null) {
                mergedEntities.addAll(retEntities[j]);
            }
        }
        return mergedEntities;
    }

    private Entity retrieveEntity(String repositoryId, IdentifierType identifier,
                                  boolean isAllowOperationIfReposDown, Set<String> failureRepositoryIds) throws WIMException {
        String METHODNAME = "retrieveEntity";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " (" + repositoryId + ", " + identifier + ", " + isAllowOperationIfReposDown + ", " + failureRepositoryIds + ")");
        }

        Entity retEntDO = null;

        String uniqueId = identifier.getUniqueId();
        String uniqueName = identifier.getUniqueName();

        if ((uniqueId == null || uniqueId.length() == 0) && (uniqueName == null || uniqueName.length() == 0)) {
            AuditManager auditManager = new AuditManager();
            Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), auditManager.getRequestType(), repositoryId, uniqueName,
                        auditManager.getRepositoryRealm(), null, Integer.valueOf("211"));
            throw new InvalidIdentifierException(WIMMessageKey.INVALID_IDENTIFIER, Tr.formatMessage(
                                                                                                    tc,
                                                                                                    WIMMessageKey.INVALID_IDENTIFIER,
                                                                                                    WIMMessageHelper.generateMsgParms(uniqueId, uniqueName)));
        }
        if (uniqueName != null) {
            uniqueName = UniqueNameHelper.formatUniqueName(uniqueName);
            identifier.setUniqueName(uniqueName);
        }

        retEntDO = retrieveEntityFromRepository(repositoryId, identifier, isAllowOperationIfReposDown,
                                                failureRepositoryIds);

        return retEntDO;
    }

    @FFDCIgnore(ClassCastException.class)
    private void prepareDataGraphForCaller(Root root, String uid, String uName, boolean isAllowOperationIfReposDown,
                                           Set<String> failureRepositoryIds) throws WIMException {
        String METHODNAME = "prepareDataGraphForCaller";
        if (root != null) {
            List<Entity> entities = root.getEntities();
            if (entities != null) {
                for (Entity entity : entities) {
                    String type = entity.getTypeName();
                    IdentifierType identifier = entity.getIdentifier();
                    if (identifier != null) {
                        prepareForCaller(identifier, type, uid, uName, isAllowOperationIfReposDown, failureRepositoryIds);
                    }
                    processReferenceProperty(entity, type, true, isAllowOperationIfReposDown, failureRepositoryIds);

                    Group group = null;
                    try {
                        group = (Group) entity;
                    } catch (ClassCastException e) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, METHODNAME + " Entity is not a group or its subtype");
                    }

                    if (group != null) {
                        List<Entity> mbrs = group.getMembers();
                        if (mbrs != null) {
                            for (Entity mbr : mbrs) {
                                String mbrType = entity.getTypeName();
                                IdentifierType id = mbr.getIdentifier();
                                prepareForCaller(id, mbrType, null, null, isAllowOperationIfReposDown, failureRepositoryIds);
                                processReferenceProperty(mbr, mbrType, true, isAllowOperationIfReposDown, failureRepositoryIds);
                            }
                        }
                    }

                    List<Group> grps = entity.getGroups();
                    if (grps != null) {
                        for (Group grp : grps) {
                            String grpType = entity.getTypeName();
                            IdentifierType id = grp.getIdentifier();
                            prepareForCaller(id, grpType, null, null, isAllowOperationIfReposDown, failureRepositoryIds);
                            if (propMgr.getReferencePropertyNameSet(grpType) != null) {
                                processReferenceProperty(grp, grpType, true, isAllowOperationIfReposDown, failureRepositoryIds);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * prepare the identifier DataObject for caller.
     *
     * @param id the identifier DataObject
     * @param qualifiedEntityType the qualified entity type of the identifier represented entity
     * @param uid unique ID of the entity before updating operation (the should be only specified for update operation, for other operations, set the uid to null)
     * @param uName unique name of the entity before updating operation (the should be only specified for update operation, for other operations, set the uName to null)
     * @throws WIMException
     */
    private void prepareForCaller(IdentifierType id, String qualifiedEntityType, String uid, String uName,
                                  boolean isIgnoreRepositoryErrors, Set<String> failureRepositoryIds) throws WIMException {
        String METHODNAME = "prepareForCaller";
        if (id != null) {
            String externalId = id.getExternalId();

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " prepare identifier for caller, set [uniqueId="
                             + externalId + "]");
            }

            if (externalId != null) {
                id.setUniqueId(externalId);
            }

            id.setExternalId(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void processReferenceProperty(Entity entity, String qualifiedEntityType, boolean read,
                                          boolean isAllowOperationIfReposDown, Set<String> failureRepositoryIds) throws WIMException {

        Set<String> refPropNames = propMgr.getReferencePropertyNameSet(qualifiedEntityType);

        if (refPropNames != null) {
            Iterator<String> iter = refPropNames.iterator();
            while (iter.hasNext()) {
                String qualifiedPropName = iter.next();
                String prop = entity.getDataType(qualifiedPropName);
                if (prop != null && entity.isSet(prop)) {
                    if (entity.get(qualifiedPropName) instanceof List) {
                        List<IdentifierType> ids = (List<IdentifierType>) entity.get(prop);
                        for (int i = 0; i < ids.size(); i++) {
                            processIdentifier(ids.get(i), read, isAllowOperationIfReposDown, failureRepositoryIds);
                        }
                    } else {
                        IdentifierType id = entity.getIdentifier();
                        processIdentifier(id, read, isAllowOperationIfReposDown, failureRepositoryIds);
                    }
                }
            }
        }
        if (tc.isDebugEnabled()) {
            //Tr.debug(tc, entity);
        }
    }

    private void processIdentifier(IdentifierType id, boolean fromSPI, boolean isAllowOperationIfReposDown,
                                   Set<String> failureRepositoryIds) throws WIMException {

        if (tc.isDebugEnabled()) {
            //Tr.debug(tc, id);
        }
        String extId = id.getExternalId();

        if (fromSPI) {
            if (extId != null) {
                id.setUniqueId(extId);
            }
            id.setExternalId(null);
        } else {
            retrieveEntityFromRepository(null, id, isAllowOperationIfReposDown, failureRepositoryIds);
        }

        if (tc.isDebugEnabled()) {
            //Tr.debug(tc, id);
        }
    }

    @FFDCIgnore(EntityNotFoundException.class)
    private Entity retrieveEntityFromRepository(String repositoryId, IdentifierType identifier, boolean isAllowOperationIfReposDown,
                                                Set<String> failureRepositoryIds) throws WIMException {
        String METHODNAME = "retrieveEntityFromRepository";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " (" + repositoryId + ", " + identifier + ", " + isAllowOperationIfReposDown + ", " + failureRepositoryIds + ")");
        }

        Entity retEntDO = null;

        Root root = new Root();

        String extId = identifier.getExternalId();
        String extName = identifier.getExternalName();
        String uniqueId = identifier.getUniqueId();
        String uniqueName = identifier.getUniqueName();

        if ((uniqueId == null || uniqueId.length() == 0) && (uniqueName == null || uniqueName.length() == 0)) {
            AuditManager auditManager = new AuditManager();
            Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), auditManager.getRequestType(), repositoryId, uniqueName,
                        auditManager.getRepositoryRealm(), null, Integer.valueOf("211"));
            throw new InvalidIdentifierException(WIMMessageKey.INVALID_IDENTIFIER, Tr.formatMessage(
                                                                                                    tc,
                                                                                                    WIMMessageKey.INVALID_IDENTIFIER,
                                                                                                    WIMMessageHelper.generateMsgParms(uniqueId, uniqueName)));
        }

        try {
            if (retEntDO == null) {
                Entity inEntDO = new Entity();
                root.getEntities().add(inEntDO);

                IdentifierType idDO = new IdentifierType();
                inEntDO.setIdentifier(idDO);

                if (extId != null)
                    idDO.setExternalId(extId);
                else
                    idDO.setExternalId(uniqueId);
                idDO.setExternalName(extName);
                idDO.setUniqueName(uniqueName);

                if (uniqueId != null && repositoryId == null) {
                    retEntDO = innerRetrieveEntityFromRepository(root, retEntDO, uniqueId, isAllowOperationIfReposDown, failureRepositoryIds);
                } else {
                    if (repositoryId == null) {
                        repositoryId = getRepositoryManager().getRepositoryId(uniqueName);
                    }

                    // from the specified repository
                    Root retRoot = getRepositoryManager().getRepository(repositoryId).get(root);

                    if (retRoot != null) {
                        List<Entity> entList = retRoot.getEntities();

                        if (entList.size() >= 1) {
                            retEntDO = entList.get(0);
                        }
                    }
                }
            }
        } catch (EntityNotFoundException nfe) {
            // No FFDC here -- only FFDC from calling innerRetrieveEntityFromRepository
            AuditManager auditManager = new AuditManager();
            Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), auditManager.getRequestType(),
                        repositoryId == null ? auditManager.getRepositoryId() : repositoryId, uniqueName,
                        auditManager.getRepositoryRealm(), null, Integer.valueOf("212"));
            throw nfe;
        }

        if (retEntDO == null) {
            String id = null;
            if (uniqueId != null) {
                id = uniqueId;
            } else {
                id = uniqueName;
            }
            AuditManager auditManager = new AuditManager();
            Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), auditManager.getRequestType(),
                        repositoryId == null ? auditManager.getRepositoryId() : repositoryId, uniqueName,
                        auditManager.getRepositoryRealm(), null, Integer.valueOf("212"));
            throw new EntityNotFoundException(WIMMessageKey.ENTITY_NOT_FOUND, Tr.formatMessage(
                                                                                               tc,
                                                                                               WIMMessageKey.ENTITY_NOT_FOUND,
                                                                                               WIMMessageHelper.generateMsgParms(id)));
        }

        IdentifierType idDO = retEntDO.getIdentifier();
        identifier.setExternalId(idDO.getExternalId());
        identifier.setExternalName(idDO.getExternalName());
        identifier.setUniqueId(idDO.getUniqueId());
        identifier.setUniqueName(idDO.getUniqueName());
        identifier.setRepositoryId(idDO.getRepositoryId());

        return retEntDO;
    }

    /**
     * Method created so we can ffdc the inner EntityNotFoundException
     */
    private Entity innerRetrieveEntityFromRepository(Root root, Entity retEntDO, String uniqueId, boolean isAllowOperationIfReposDown,
                                                     Set<String> failureRepositoryIds) throws WIMException {
        String METHODNAME = "retrieveEntityFromRepository";

        List<String> reposIds = getRepositoryManager().getRepoIds();
        for (int i = 0; (i < reposIds.size() && retEntDO == null); i++) {
            String reposId = reposIds.get(i);

            try {
                Root retRoot = getRepositoryManager().getRepository(reposId).get(root);
                if (retRoot != null) {
                    List<Entity> entList = retRoot.getEntities();

                    if (entList.size() >= 1) {
                        retEntDO = entList.get(0);
                    }
                }
            } catch (EntityNotFoundException e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, METHODNAME + " EntityNotFoundException[reposId=" + reposId + "] - " + uniqueId);
                }
            } catch (WIMSystemException wse) {
                String message = wse.getMessage();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, METHODNAME + " WSE message = " + message);
                }
                if (message != null && message.contains("CWIML4520E") && message.contains("javax.naming.InvalidNameException")) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, METHODNAME + " WIMSystemException [reposId=" + reposId + "] - " + message);
                    }
                } else
                    throw wse;
            } catch (Exception e) {
                if (!isAllowOperationIfReposDown) {
                    if (e instanceof WIMException) {
                        throw (WIMException) e;
                    }
                    throw new WIMException(e);
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, METHODNAME + " IGNORE: exception [" + e.getMessage()
                                     + "] when retrieve entity from repository [" + reposId + "]");
                    failureRepositoryIds.add(reposId);

                }
            }
        }
        return retEntDO;
    }

    private List<? extends Entity> setExtIdAndRepositoryIdForEntities(List<? extends Entity> members, String targetReposId,
                                                                      boolean isAllowOperationIfReposDown, Set<String> failureRepositoryIds) throws WIMException {
        if (members == null) {
            return null;
        } else {
            for (Entity mbrEntDO : members) {
                IdentifierType mbrEntIdDO = mbrEntDO.getIdentifier();
                if (mbrEntIdDO == null) {
                    throw new EntityIdentifierNotSpecifiedException(WIMMessageKey.ENTITY_IDENTIFIER_NOT_SPECIFIED, Tr.formatMessage(tc,
                                                                                                                                    WIMMessageKey.ENTITY_IDENTIFIER_NOT_SPECIFIED));
                }
                String uniqueId = mbrEntIdDO.getUniqueId();
                String uniqueName = mbrEntIdDO.getUniqueName();

                String extId = null;
                String reposUUID = null;
                String uName = null;

                if (getRepositoryManager().getNumberOfRepositories() == 1) {
                    if (uniqueId != null) {
                        extId = uniqueId;
                        reposUUID = targetReposId;
                    }
                } else {
                    IdentifierType idDO = getIdentifierByUniqueIdOrUniqueName(uniqueId, uniqueName,
                                                                              isAllowOperationIfReposDown, failureRepositoryIds);
                    if (idDO != null) {
                        extId = idDO.getExternalId();
                        reposUUID = idDO.getRepositoryId();
                        uName = idDO.getUniqueName();
                    }
                }

                if (extId != null) {
                    mbrEntIdDO.setExternalId(extId);
                }
                if (reposUUID != null) {
                    mbrEntIdDO.setRepositoryId(reposUUID);
                }
                if (uName != null) {
                    mbrEntIdDO.setUniqueName(uName);
                }
            }

            return members;
        }
    }

    private IdentifierType getIdentifierByUniqueIdOrUniqueName(String uniqueId, String uniqueName, boolean isAllowOperationIfReposDown,
                                                               Set<String> failureRepositoryIds) throws WIMException {
        final String METHODNAME = "getIdentifierByUniqueIdOrUniqueName";
        IdentifierType idDO = null;
        WIMException ex = null;

        boolean found = false;

        Root tempDO = new Root();
        Entity tempEntityDO = new Entity();
        IdentifierType tempIdDO = new IdentifierType();
        tempEntityDO.setIdentifier(tempIdDO);
        tempDO.getEntities().add(tempEntityDO);

        if (uniqueName != null) {
            String reposId = getRepositoryManager().getRepositoryIdByUniqueName(uniqueName);
            /*
             * if (reposId == null) {
             * throw new EntityNotFoundException(WIMMessageKey.ENTITY_NOT_FOUND, Tr.formatMessage(
             * tc,
             * WIMMessageKey.ENTITY_NOT_FOUND,
             * WIMMessageHelper.generateMsgParms(uniqueName)
             * ));
             * }
             */
            tempIdDO.setUniqueName(uniqueName);
            Root retRootDO = getRepositoryManager().getRepository(reposId).get(tempDO);
            if (retRootDO != null) {
                List<Entity> pes = retRootDO.getEntities();
                if (pes != null) {
                    Entity ent = pes.get(0);
                    if (ent != null) {
                        idDO = ent.getIdentifier();
                        if (idDO != null) {
                            found = true;
                        }
                    }
                } else {
                    AuditManager auditManager = new AuditManager();
                    Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), auditManager.getRequestType(),
                                reposId == null ? auditManager.getRepositoryId() : reposId, uniqueName,
                                auditManager.getRepositoryRealm(), null, Integer.valueOf("212"));
                    throw new EntityNotFoundException(WIMMessageKey.ENTITY_NOT_FOUND, Tr.formatMessage(
                                                                                                       tc,
                                                                                                       WIMMessageKey.ENTITY_NOT_FOUND,
                                                                                                       WIMMessageHelper.generateMsgParms(uniqueName)));
                }
            }
        } else {
            int i = 0;
            List<String> reposIds = getRepositoryManager().getRepoIds();
            String reposId = null;
            while (i < reposIds.size() && !found) {
                reposId = reposIds.get(i);
                tempDO = new Root();
                tempEntityDO = new Entity();
                tempIdDO = new IdentifierType();
                tempEntityDO.setIdentifier(tempIdDO);
                tempDO.getEntities().add(tempEntityDO);

                if (uniqueId != null) {
                    tempIdDO.setUniqueId(uniqueId);
                }
                try {
                    Root retRootDO = getRepositoryManager().getRepository(reposId).get(tempDO);
                    if (retRootDO != null) {
                        List<Entity> pes = retRootDO.getEntities();
                        if (pes != null) {
                            Entity ent = pes.get(0);
                            if (ent != null) {
                                idDO = ent.getIdentifier();
                                if (idDO != null) {
                                    found = true;
                                }
                            }
                        }
                    }
                } catch (EntityNotFoundException e) {
                    i++;
                    ex = e;
                } catch (Exception e) {
                    WIMException we = null;
                    if (e instanceof WIMException) {
                        we = (WIMException) e;
                    } else {
                        we = new WIMException(e);
                    }

                    if (!isAllowOperationIfReposDown) {
                        throw we;
                    } else {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, METHODNAME + " IGNORE: exception [" + e.getMessage()
                                         + "] on repository [" + reposId + "]");
                        failureRepositoryIds.add(reposId);
                        ex = we;
                    }

                }
            }
        }
        if (!found && ex != null) {
            throw ex;
        }
        return idDO;
    }

    private String getRealmName(Root root) {
        String value = null;
        List<Context> contexts = root.getContexts();
        if (contexts == null || contexts.size() == 0) {
            if (getConfigManager() != null)
                value = getConfigManager().getDefaultRealmName();
        } else {

            int i = 0;
            while (i < contexts.size() && (value == null || value.length() == 0)) {
                Context context = contexts.get(i);
                String key = context.getKey();
                if (key != null && VALUE_CONTEXT_REALM_KEY.equals(key)) {
                    value = (String) context.getValue();
                }
                i++;
            }
            if (value == null) {
                value = getConfigManager().getDefaultRealmName();
            }
        }
        return value;
    }

    /**
     * First try to get the default or primary realm defined.
     * If not found, then use the realm name from one of the
     * registries.
     * Added for populating audit records.
     *
     * @param root
     * @return
     */
    private String getRealmNameOrFirstBest(Root root) {
        String value = null;
        value = getRealmName(root);
        if (value == null) {
            try {
                value = getRealmName();
            } catch (Exception e) {
                // leave realm at null
            }
        }
        return value;
    }

    private void groupMembershipLookup(Root root, String repositoryId, GroupControl ctrl,
                                       boolean isAllowOperationIfReposDown, Set<String> failureRepositoryIds, CacheControl clearCacheCtrl) throws WIMException {
        String METHODNAME = "groupMembershipLookup";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " repositoryId=" + repositoryId);
        }
        int level = ctrl.getLevel();

        if (root != null) {
            List<Entity> entities = root.getEntities();
            for (Entity entity : entities) {
                IdentifierType id = entity.getIdentifier();
                if (ctrl instanceof GroupMembershipControl) {
                    List<Group> groups = entity.getGroups();
                    ArrayList<IdentifierType> groupIDS = new ArrayList<IdentifierType>();
                    groupIDS.add(id);

                    if (level == 0) {
                        for (int j = 0; j < groups.size(); j++) {
                            Group grp = groups.get(j);
                            groupIDS.add(grp.getIdentifier());
                        }
                    }

                    Iterator<String> reposIds = getRepositoryManager().getRepositoriesForGroupMembership(repositoryId).iterator();
                    while (reposIds.hasNext()) {
                        String targetId = reposIds.next();
                        if (!targetId.equals(repositoryId)) {
                            boolean duplicateGroup;
                            for (IdentifierType tempGrp : groupIDS) {
                                Root newRoot = new Root();
                                IdentifierType newId = new IdentifierType();
                                Entity newEntity = new Entity();
                                newEntity.setIdentifier(newId);
                                newRoot.getEntities().add(newEntity);

                                newId.setUniqueId(tempGrp.getUniqueId());
                                newId.setUniqueName(tempGrp.getUniqueName());
                                newId.setRepositoryId(tempGrp.getRepositoryId());
                                newId.setExternalId(tempGrp.getExternalId());
                                newId.setExternalName(tempGrp.getExternalName());

                                newRoot.getControls().add(ctrl);

                                if (clearCacheCtrl != null)
                                    newRoot.getControls().add(clearCacheCtrl);

                                try {
                                    Root retRoot = getRepositoryManager().getRepository(targetId).get(newRoot);

                                    if (retRoot != null) {
                                        List<Entity> retEntities = retRoot.getEntities();
                                        Entity retEntity = retEntities.get(0);
                                        List<Group> grps = retEntity.getGroups();
                                        List<Group> totalGroups = entity.getGroups();

                                        for (Group newGroup : grps) {
                                            duplicateGroup = false;
                                            IdentifierType newIdentifier = newGroup.getIdentifier();
                                            for (Group group : totalGroups) {
                                                IdentifierType identifier = group.getIdentifier();
                                                if ((isIdentifierEqual(identifier, newIdentifier))) {
                                                    duplicateGroup = true;
                                                    break;
                                                }
                                            }
                                            if (!duplicateGroup) {
                                                totalGroups.add(newGroup);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    if (!isAllowOperationIfReposDown) {
                                        if (e instanceof WIMException) {
                                            throw (WIMException) e;
                                        }
                                        throw new WIMException(e);
                                    } else {
                                        if (tc.isDebugEnabled())
                                            Tr.debug(tc, METHODNAME + " IGNORE: exception ["
                                                         + e.getMessage() + "] on repository [" + targetId + "]");
                                        failureRepositoryIds.add(targetId);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isIdentifierEqual(IdentifierType targetId, IdentifierType id) {
        boolean result = false;

        if (targetId.getUniqueId() != null
            && targetId.getUniqueId().equals(id.getUniqueId())) {
            result = true;
        } else if (targetId.getUniqueName() != null
                   && targetId.getUniqueName().equals(id.getUniqueName())) {
            result = true;
        } else if (targetId.getExternalId() != null
                   && targetId.getExternalId().equals(id.getExternalId())) {
            result = true;
        } else if (targetId.getExternalName() != null
                   && targetId.getExternalName().equals(id.getExternalName())) {
            result = true;
        }

        return result;
    }

    private boolean isConfigChangeLogSupportEnabled(String reposId) throws WIMException {
        // TODO::
        return false;
    }

    public String getRealmName() throws WIMException {
        String realmName = null;

        @SuppressWarnings("unchecked")
        List<String> repoIds = getRepositoryManager().getRepoIds();
        if (repoIds != null && repoIds.size() > 0) {
            Repository repository = getRepositoryManager().getRepository(repoIds.get(0));
            realmName = repository.getRealm();
        }

        return realmName;
    }

    /**
     * PM33575: get a list of all the potential repositories based on external name. The list will return repository IDs where user can exist.
     * Repositories for which nameInRepository = "", will be always be added at the end of the list
     *
     * @param externalName
     * @return
     * @throws WIMException
     */
    public List<String> getReposForExternalName(String externalName, String realmName) throws WIMException {
        externalName = UniqueNameHelper.getValidUniqueName(externalName);
        if (externalName == null || externalName.trim().equals(""))
            return Collections.emptyList();

        int uLength = externalName.length();

        // Get the list of configured baseEntries for the realm
        String[] realmBaseEntries = null;

        if (getConfigManager() != null && getConfigManager().getRealmConfig(realmName) != null)
            realmBaseEntries = getConfigManager().getRealmConfig(realmName).getParticipatingBaseEntries();

        // Iterate over the repositories.
        // RepositoryManager reposMgr = reposMgrRef.getService();
        List<String> reposIds = repositoryManager.getRepoIds();

        if (reposIds == null || realmBaseEntries == null)
            return Collections.emptyList();

        List<String> potentialRepos = new ArrayList<String>(); // keep a list of all the potential repository ids where user can exist
        List<String> realmBases = Arrays.asList(realmBaseEntries);
        String repo = null;
        int bestMatch = -1;
        for (String repoId : reposIds) {
            // read the list of base entries for a given repository which are defined in realm
            Map<String, String> baseEntryies = repositoryManager.getRepositoryBaseEntries(repoId);

            for (Map.Entry<String, String> entry : baseEntryies.entrySet()) {
                // check if the repository base entry is present in  realm
                if (realmBases.contains(entry.getKey())) {
                    String baseDN = entry.getValue();
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "baseDN = " + baseDN);
                    if (baseDN != null) {
                        String reposNodeName = baseDN;

                        if (reposNodeName.trim().equals("")) {
                            potentialRepos.add(repoId);
                        }
                        reposNodeName = UniqueNameHelper.getValidUniqueName(reposNodeName);
                        //handling the case of empty string "" returned from repository.
                        if (reposNodeName != null && reposNodeName.length() > 0) {
                            int nodeLength = reposNodeName.length();
                            if ((uLength == nodeLength) && externalName.equalsIgnoreCase(reposNodeName)) {
                                repo = repoId;
                                break;
                            }
                            if ((uLength > nodeLength)
                                && (com.ibm.ws.security.wim.util.StringUtil.endsWithIgnoreCase(externalName, "," + reposNodeName))) {
                                if (nodeLength > bestMatch) {
                                    repo = repoId;
                                    bestMatch = nodeLength;
                                }
                            }
                        }
                    }
                }
            }
            if (repo != null) {
                potentialRepos.add(0, repo);
            }

        }
        return potentialRepos;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.security.wim.ProfileServiceLite#delete(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root delete(Root root) throws WIMException {
        //Please DONOT UPDATE or CHANGE this wrapper.
        //If a need arises, please updated the searchImpl method call.

        final String METHODNAME = "delete";
        return genericProfileManagerMethod(METHODNAME, ProfileManager.DELETE, root);
    }

    /**
     * The underlying implementation of the delete method. This in-turn calls the relevant delete method
     * on the repository.
     *
     * @param root
     * @return
     * @throws WIMException
     */
    @FFDCIgnore({ EntityNotFoundException.class })
    public Root deleteImpl(Root root) throws WIMException {
        final String METHODNAME = "deleteImpl";

        if (root == null)
            return null;

        AuditManager auditManager = new AuditManager();
        auditManager.setRequestType(AuditConstants.DELETE_AUDIT);
        if (repositoryManager.getNumberOfRepositories() == 1) {
            auditManager.setRepositoryId(repositoryManager.getRepoIds().get(0));
        }

        // Check if this is logged in user.
        // Logged-in User cannot be deleted. isReferenceToLoggedInUser will throw exception in case
        // the user being deleted is logged in User.
        this.isReferenceToLoggedInUser(root);

        boolean returnDeleted = false;

        List<Entity> entities = root.getEntities();

        if (entities.size() == 0) {
            Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.DELETE_AUDIT, null, null, getRealmName(root), root,
                        Integer.valueOf("201"));

            throw new EntityNotFoundException(WIMMessageKey.MISSING_ENTITY_DATA_OBJECT, Tr.formatMessage(tc,
                                                                                                         WIMMessageKey.MISSING_ENTITY_DATA_OBJECT,
                                                                                                         WIMMessageHelper.generateMsgParms(DELETE_EMITTER)));
        } else if (entities.size() > 1) {
            Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.DELETE_AUDIT, null, null, getRealmName(root), root,
                        Integer.valueOf("202"));

            throw new OperationNotSupportedException(WIMMessageKey.ACTION_MULTIPLE_ENTITIES_SPECIFIED, Tr.formatMessage(tc,
                                                                                                                        WIMMessageKey.ACTION_MULTIPLE_ENTITIES_SPECIFIED,
                                                                                                                        WIMMessageHelper.generateMsgParms(RepositoryManager.ACTION_DELETE)));
        }

        Entity entity = entities.get(0);
        Map<String, Control> ctrlMap = ControlsHelper.getControlMap(root);

        DeleteControl deleteCtrl = (DeleteControl) ctrlMap.get(DO_DELETE_CONTROL);
        //boolean deleteDesc = false;
        if (deleteCtrl != null) {
            //deleteDesc = deleteCtrl.isDeleteDescendants();
            returnDeleted = deleteCtrl.isReturnDeleted();
        }

        /* Ensure the principal is authorized to delete the entity */
        // profileSecManager.checkPermission_DELETE(new EntityResource(inRoot, entity), deleteDesc);

        IdentifierType identifier = entity.getIdentifier();
        if (identifier == null) {
            Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.DELETE_AUDIT, null, null, getRealmName(root), root,
                        Integer.valueOf("203"));

            throw new EntityIdentifierNotSpecifiedException(WIMMessageKey.ENTITY_IDENTIFIER_NOT_SPECIFIED, Tr.formatMessage(tc, WIMMessageKey.ENTITY_IDENTIFIER_NOT_SPECIFIED));
        }

        // If repository id is set in the identifier then use it, else determine it from the uniqueName
        String repositoryId = identifier.getRepositoryId();
        String uniqueName = identifier.getUniqueName();

        if (repositoryId == null)
            repositoryId = getRepositoryManager().getRepositoryId(uniqueName);

        // check realm
        String realmName = getRealmName(root);
        if (realmName != null && !getConfigManager().isUniqueNameInRealm(uniqueName, realmName)) {
            Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.DELETE_AUDIT, repositoryId, uniqueName, realmName, root,
                        Integer.valueOf("204"));

            throw new EntityNotInRealmScopeException(WIMMessageKey.ENTITY_NOT_IN_REALM_SCOPE, Tr.formatMessage(tc, WIMMessageKey.ENTITY_NOT_IN_REALM_SCOPE,
                                                                                                               WIMMessageHelper.generateMsgParms(uniqueName, realmName)));
        }

        // String realEntityType = realEntity.getTypeName();
        // checkAccessibility(DELETE, RepositoryManager.ACTION_DELETE, repositoryId, realEntityType);

        auditManager.setRepositoryId(repositoryId);
        auditManager.setRepositoryRealm(realmName);
        auditManager.setRepositoryUniqueName(uniqueName);

        Root retRoot = null;
        try {
            retRoot = repositoryManager.getRepository(repositoryId).delete(root);
        } catch (EntityNotFoundException ex) {
            Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.DELETE_AUDIT, repositoryId, uniqueName, realmName, root,
                        Integer.valueOf("212"));
            throw ex;
        }

        if (retRoot != null) {
            retRoot = postDelete(retRoot, repositoryId, returnDeleted);
        }

        Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.DELETE_AUDIT, repositoryId, uniqueName,
                    realmName != null ? realmName : getRealmNameOrFirstBest(retRoot), retRoot, Integer.valueOf("200"));

        return retRoot;
    }

    private Root postDelete(Root retRoot, String repositoryId, boolean returnDeleted) throws WIMException {
        // TODO:: Code to be added
        return retRoot;
    }

    @Override
    public Root create(Root root) throws WIMException {
        //Please DONOT UPDATE or CHANGE this wrapper.
        //If a need arises, please updated the createImpl method call.

        final String METHODNAME = "create";
        return genericProfileManagerMethod(METHODNAME, ProfileManager.CREATE, root);
    }

    public Root createImpl(Root root) throws WIMException {
        Root created = null;
        Root inRoot = root;
        if (root == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "createImpl, root is null");
        }

        String targetReposId = null;

        String realmName = getRealmName(root);

        List<Entity> entities = root.getEntities();
        if (entities == null) {
            return null;
        }

        AuditManager auditManager = new AuditManager();

        // for audit reporting purposes, only if we have one repository configured, else we have to rely on a valid parentDN to get to the
        // correct repositoryId
        if (repositoryManager.getNumberOfRepositories() == 1) {
            targetReposId = repositoryManager.getRepoIds().get(0);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "targetRepostID = ", targetReposId);
        }
        auditManager.setRepositoryId(targetReposId);
        auditManager.setRepositoryRealm(realmName);
        auditManager.setRequestType(AuditConstants.CREATE_AUDIT);

        if (entities.size() == 0) {
            Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.CREATE_AUDIT, targetReposId, null, getRealmName(root), root,
                        Integer.valueOf("201"));

            throw new EntityNotFoundException(WIMMessageKey.MISSING_ENTITY_DATA_OBJECT, Tr.formatMessage(tc, WIMMessageKey.MISSING_ENTITY_DATA_OBJECT,
                                                                                                         WIMMessageHelper.generateMsgParms(RepositoryManager.ACTION_CREATE)));
        } else if (entities.size() > 1) {
            Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.CREATE_AUDIT, targetReposId, null, getRealmName(root), root,
                        Integer.valueOf("202"));

            throw new OperationNotSupportedException(WIMMessageKey.ACTION_MULTIPLE_ENTITIES_SPECIFIED, Tr.formatMessage(tc, WIMMessageKey.ACTION_MULTIPLE_ENTITIES_SPECIFIED,
                                                                                                                        WIMMessageHelper.generateMsgParms(RepositoryManager.ACTION_CREATE)));
        }

        Entity entity = entities.get(0);
        String qualifiedEntityType = entity.getTypeName();

        /* Ensure the principal is authorized to create the entity */
        //TODO::
        //profileSecManager.checkPermission_CREATE(new EntityResource(newRootDO, entity));

        // TODO:: Is this needed?
        checkCreateAndModifyTimeStamp(qualifiedEntityType, entity);

        // Check if view-specific operation need to be performed
        // Map<String, Control> controlMap = ControlsHelper.getControlMap(root);

        Entity parent = null;
        String parentDN = null;

        parent = entity.getParent();

        if (parent != null) {
            if (parent.isSetIdentifier()) {
                parentDN = parent.getIdentifier().getUniqueName();
                if (parentDN == null) {
                    String parentID = parent.getIdentifier().getUniqueId();
                    if (parentID != null) {
                        // get parent unique name
                        parentDN = getUniqueNameByUniqueId(parentID, false, null);
                        if (parentDN == null || parentDN.length() == 0) {
                            if (entity != null && entity.getIdentifier() != null) {
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "entity not null, parentDN null");
                                Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.CREATE_AUDIT, targetReposId,
                                            entity.getIdentifier().getUniqueName(),
                                            realmName, root,
                                            Integer.valueOf("205"));
                            } else {
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "entity null, parentDN null");
                                Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.CREATE_AUDIT, targetReposId, null,
                                            realmName, root,
                                            Integer.valueOf("205"));
                            }
                            throw new InvalidUniqueIdException(WIMMessageKey.INVALID_PARENT_UNIQUE_ID, Tr.formatMessage(tc, WIMMessageKey.INVALID_PARENT_UNIQUE_ID,
                                                                                                                        WIMMessageHelper.generateMsgParms(parentID)));
                        }
                    }
                }
            } else {
                if (entity != null && entity.getIdentifier() != null) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "parent.isSetIdentifier null, entity not null");
                    Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.CREATE_AUDIT, targetReposId,
                                entity.getIdentifier().getUniqueName(),
                                realmName,
                                inRoot, Integer.valueOf("205"));
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "parent.isSetIdentifier null, entity null");
                    Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.CREATE_AUDIT, targetReposId, null, realmName, inRoot,
                                Integer.valueOf("205"));
                }
                throw new InvalidUniqueIdException(WIMMessageKey.INVALID_PARENT_UNIQUE_ID, Tr.formatMessage(tc, WIMMessageKey.INVALID_PARENT_UNIQUE_ID,
                                                                                                            WIMMessageHelper.generateMsgParms(null)));
            }
        }
        if (parentDN == null) {
            parentDN = configMgr.getDefaultParentForEntityInRealm(qualifiedEntityType, realmName);
        }

        if (parentDN == null) {
            // Check if we have only one repository configured. If yes, use its baseEntry as parentDN
            List<String> repos = repositoryManager.getRepoIds();
            if (repos != null && repos.size() == 1) {
                List<String> baseEntries = repositoryManager.getRepositoriesBaseEntries().get(repos.get(0));
                // Check that it has only one baseEntry
                if (baseEntries != null && baseEntries.size() == 1) {
                    parentDN = baseEntries.get(0);
                }
            }
        }

        if (parentDN == null) {
            if (entity != null && entity.getIdentifier() != null)
                Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.CREATE_AUDIT, targetReposId,
                            entity.getIdentifier().getUniqueName(), realmName,
                            root, Integer.valueOf("206"));
            else
                Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.CREATE_AUDIT, targetReposId, null, realmName, root,
                            Integer.valueOf("206"));

            throw new DefaultParentNotFoundException(WIMMessageKey.DEFAULT_PARENT_NOT_FOUND, Tr.formatMessage(tc, WIMMessageKey.DEFAULT_PARENT_NOT_FOUND,
                                                                                                              WIMMessageHelper.generateMsgParms(qualifiedEntityType, realmName)));
        }

        if (!getConfigManager().isUniqueNameInRealm(parentDN, realmName)) {
            if (entity != null && entity.getIdentifier() != null)
                Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.CREATE_AUDIT, targetReposId,
                            entity.getIdentifier().getUniqueName(), realmName,
                            root, Integer.valueOf("204"));
            else
                Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.CREATE_AUDIT, targetReposId, null, realmName, root,
                            Integer.valueOf("204"));

            throw new EntityNotInRealmScopeException(WIMMessageKey.ENTITY_NOT_IN_REALM_SCOPE, Tr.formatMessage(tc, WIMMessageKey.ENTITY_NOT_IN_REALM_SCOPE,
                                                                                                               WIMMessageHelper.generateMsgParms(parentDN, realmName)));
        }

        // Format the parentDN
        parentDN = UniqueNameHelper.formatUniqueName(parentDN);
        if (parent == null) {
            parent = new Entity();
            entity.setParent(parent);
            IdentifierType id = new IdentifierType();
            parent.setIdentifier(id);
        }

        // Check if the entity is being created under the right parent.
        if (entity.getIdentifier() != null) {
            String entityUniqueName = entity.getIdentifier().getUniqueName();
            if (entityUniqueName != null && parentDN != null) {
                if (entityUniqueName.length() < parentDN.length()
                    || !entityUniqueName.endsWith(parentDN)) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "entity not being created under the right parent");
                    Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.CREATE_AUDIT, targetReposId,
                                entity.getIdentifier().getUniqueName(),
                                realmName,
                                inRoot, Integer.valueOf("205"));

                    throw new InvalidUniqueIdException(WIMMessageKey.INVALID_PARENT_UNIQUE_ID, Tr.formatMessage(tc, WIMMessageKey.INVALID_PARENT_UNIQUE_ID,
                                                                                                                WIMMessageHelper.generateMsgParms(parentDN)));
                }
            }
        }

        parent.getIdentifier().setUniqueName(parentDN);
        entity.setParent(parent);

        if (repositoryManager.getNumberOfRepositories() > 1) {
            if (targetReposId == null) {
                targetReposId = repositoryManager.getRepositoryIdByUniqueName(parentDN);
            }
        } else {
            targetReposId = repositoryManager.getRepoIds().get(0);
        }

        // TODO:: Permissions code
        // checkAccessibility(CREATE, RepositoryManager.ACTION_CREATE, targetReposId, qualifiedEntityType);

        // set extId, repositoryId for members if it has
        if (qualifiedEntityType.equals(TYPE_GROUP) || entity.getSuperTypes().contains(TYPE_GROUP)) {
            Group group = (Group) entity;
            List<Entity> members = group.getMembers();
            if (members != null && members.size() > 0) {
                members = (List<Entity>) setExtIdAndRepositoryIdForEntities(members, targetReposId, false, null);
                if (members != null && members.size() > 0) {
                    for (int i = 0; i < members.size(); i++) {
                        Entity mbrDO = members.get(i);
                        IdentifierType memberId = mbrDO.getIdentifier();
                        String reposId = retrieveTargetRepository(memberId);
                        boolean crossRepos = repositoryManager.canGroupAcceptMember(targetReposId, reposId);
                        if (!(crossRepos || targetReposId.equalsIgnoreCase(reposId))) {
                            Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.CREATE_AUDIT, targetReposId,
                                        entity.getIdentifier().getUniqueName(),
                                        realmName, root,
                                        Integer.valueOf("207"));
                            throw new OperationNotSupportedException(WIMMessageKey.MISSING_REPOSITORIES_FOR_GROUPS_CONFIGURATION, Tr.formatMessage(tc,
                                                                                                                                                   WIMMessageKey.MISSING_REPOSITORIES_FOR_GROUPS_CONFIGURATION,
                                                                                                                                                   WIMMessageHelper.generateMsgParms(RepositoryManager.ACTION_CREATE)));
                        }
                    }
                }
            }
        }

        // set extId, repositoryId for groups if it has
        List<Group> groups = entity.getGroups();
        List<Group> realGroups = null;
        if (groups != null && groups.size() > 0) {
            realGroups = separateGroups(groups, targetReposId);
        }
        /*
         * if (reposMgr.isAsyncModeSupported(targetReposId) && (groups != null) && (groups.size() > 0)) {
         * // input DO contains groups which are stored in another repositoty and the target repository is async: not supported
         * throw new OperationNotSupportedException(
         * WIMMessageKey.ASYNC_CALL_WITH_MULTIPLE_REPOSITORIES_NOT_SUPPORTED, CLASSNAME, METHODNAME);
         * }
         */
        if (realGroups != null) {
            entity.unset(DO_GROUPS);
            entity.getGroups().addAll(realGroups);
        }

        // Form the DN of the entity
//         String[] rdnProperties = configMgr.getRDNProperties(qualifiedEntityType);
//         if (rdnProperties == null) {
//            rdnProperties = configMgr.getDefaultRDNProperties(qualifiedEntityType);

//            if (rdnProperties == null)
//                throw new EntityTypeNotSupportedException(WIMMessageKey.ENTITY_TYPE_NOT_SUPPORTED,
//                                Tr.formatMessage(tc, WIMMessageKey.ENTITY_TYPE_NOT_SUPPORTED, WIMMessageHelper.generateMsgParms(qualifiedEntityType)));
//        }
//        String uniqueName = UniqueNameHelper.constructUniqueName(rdnProperties, entity, parentDN, true);

        if (!SchemaConstants.DO_PERSON_ACCOUNT.equalsIgnoreCase(qualifiedEntityType) && !SchemaConstants.DO_GROUP.equalsIgnoreCase(qualifiedEntityType)) {
            if (entity != null && entity.getIdentifier() != null)
                Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.CREATE_AUDIT, targetReposId,
                            entity.getIdentifier().getUniqueName(), realmName,
                            root, Integer.valueOf("208"));
            else
                Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.CREATE_AUDIT, targetReposId, null, realmName, root,
                            Integer.valueOf("208"));

            throw new EntityTypeNotSupportedException(WIMMessageKey.ENTITY_TYPE_NOT_SUPPORTED, Tr.formatMessage(tc, WIMMessageKey.ENTITY_TYPE_NOT_SUPPORTED,
                                                                                                                WIMMessageHelper.generateMsgParms(qualifiedEntityType)));
        }

        if (entity.getIdentifier() == null || entity.getIdentifier().getUniqueName() == null) {
            if (entity != null && entity.getIdentifier() != null)
                Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.CREATE_AUDIT, targetReposId,
                            entity.getIdentifier().getUniqueName(), realmName,
                            root, Integer.valueOf("203"));
            else
                Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.CREATE_AUDIT, targetReposId, null, realmName, root,
                            Integer.valueOf("203"));
            throw new EntityIdentifierNotSpecifiedException(WIMMessageKey.ENTITY_IDENTIFIER_NOT_SPECIFIED, Tr.formatMessage(tc, WIMMessageKey.ENTITY_IDENTIFIER_NOT_SPECIFIED));
        }

        String uniqueName = entity.getIdentifier().getUniqueName();

        processReferenceProperty(entity, qualifiedEntityType, false, false, null);

        if (uniqueName != null) {
            auditManager.setRepositoryId(targetReposId);
            auditManager.setRepositoryRealm(realmName);
            auditManager.setRepositoryUniqueName(uniqueName);

            created = repositoryManager.getRepository(targetReposId).create(root);
        }

        if (created == null) {
            return null;
        }

        Entity ent = created.getEntities().get(0);
        IdentifierType entId = ent.getIdentifier();
        String extId = entId.getExternalId();
        String uuid = extId;

        entId.setUniqueId(uuid);
        entId.setRepositoryId(targetReposId);
        if (groups != null && groups.size() > 0) {
            for (Group group : groups) {
                IdentifierType grpId = group.getIdentifier();
                String grpReposId = grpId.getRepositoryId();
                if (!targetReposId.equals(grpReposId)) {
                    //PK66383
                    if (repositoryManager.isCrossRepositoryGroupMembership(targetReposId)) {
                        Root newRoot = new Root();
                        Group grpEntity = new Group();
                        newRoot.getEntities().add(grpEntity);
                        grpEntity.setIdentifier(grpId);
                        Entity mbrEnt = new Entity();
                        grpEntity.getMembers().add(mbrEnt);
                        mbrEnt.setIdentifier(entId);
                        GroupMemberControl grpMbrCtrl = new GroupMemberControl();
                        newRoot.getControls().add(grpMbrCtrl);

                        update(newRoot);
                    } else {
                        Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.CREATE_AUDIT, targetReposId,
                                    entity.getIdentifier().getUniqueName(),
                                    realmName, created,
                                    Integer.valueOf("207"));
                        throw new OperationNotSupportedException(WIMMessageKey.MISSING_REPOSITORIES_FOR_GROUPS_CONFIGURATION, Tr.formatMessage(tc,
                                                                                                                                               WIMMessageKey.MISSING_REPOSITORIES_FOR_GROUPS_CONFIGURATION,
                                                                                                                                               WIMMessageHelper.generateMsgParms(targetReposId)));
                    }
                }
            }
        }

        /* Embed entitlements into the datagraph if requested */
        // profileSecManager.setEntitlements(root, created, new EntitlementRequest(rootDO));

        unsetExternalId(created);

        Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.CREATE_AUDIT, targetReposId, uniqueName,
                    realmName != null ? realmName : getRealmNameOrFirstBest(created), created, Integer.valueOf("200"));

        return created;
    }

    /**
     * Return if the uniqueName in delete api's DO is same as that of logged-in user
     * Defect# 37864
     *
     * @param Dataobject passed to delete API.
     * @return void
     * @throws WIMApplicationException
     */
    private void isReferenceToLoggedInUser(Root root) throws WIMApplicationException {
        // Get unique name
        if (root.getEntities().size() == 0)
            return;

        Entity entity = root.getEntities().get(0);
        IdentifierType identifier = entity.getIdentifier();

        if (identifier == null)
            return;

        String uName = identifier.getUniqueName();

        // Get unique name from subject
        String loggedInUniqueName = this.getCallerUniqueName();

        // Check subject uniquename is same as unique name of user being deleted
        // throw exception that user cannot be deleted
        if (uName != null && uName.equalsIgnoreCase(loggedInUniqueName)) {
            throw new WIMApplicationException(WIMMessageKey.CANNOT_DELETE_LOGGED_IN_USER, Tr.formatMessage(tc,
                                                                                                           WIMMessageKey.CANNOT_DELETE_LOGGED_IN_USER,
                                                                                                           WIMMessageHelper.generateMsgParms(uName)));
        }
        return;
    }

    /**
     * Helper function returns the caller's unique name.
     * Created to use for delete api to check if user being deleted is not logged-in user
     * Defect # 37864
     *
     * @return Caller's principalName
     * @throws WIMApplicationException
     */
    private String getCallerUniqueName() throws WIMApplicationException {
        String uniqueName = null;
        Subject subject = null;
        WSCredential cred = null;

        try {
            /* Get the subject */
            if ((subject = WSSubject.getRunAsSubject()) == null) {
                subject = WSSubject.getCallerSubject();
            }

            /* Get the credential */
            if (subject != null) {
                Iterator<WSCredential> iter = subject.getPublicCredentials(WSCredential.class).iterator();
                if (iter.hasNext()) {
                    cred = iter.next();
                }
                //check if credentials is null , then throw exception
                if (null == cred) {
                    throw new WIMApplicationException(WIMMessageKey.AUTH_SUBJECT_CRED_FAILURE, Tr.formatMessage(tc, WIMMessageKey.AUTH_SUBJECT_CRED_FAILURE));
                }
            }
            //throw exception when subject is null
            else {
                throw new WIMApplicationException(WIMMessageKey.AUTH_SUBJECT_FAILURE, Tr.formatMessage(tc, WIMMessageKey.AUTH_SUBJECT_FAILURE));
            }

            /* Get the unique name */
            uniqueName = cred.getUniqueSecurityName();
            if (null == uniqueName) {
                throw new WIMApplicationException(WIMMessageKey.AUTH_SUBJECT_CRED_FAILURE, Tr.formatMessage(tc, WIMMessageKey.AUTH_SUBJECT_CRED_FAILURE));
            }
        } //throw exception in case there is some issue while retrieving authentication details from subject
        catch (Exception excp) {
            //check if Auth subject failure OR auth credential failure to give relevant message in exception
            if (WIMMessageKey.AUTH_SUBJECT_FAILURE.equals(excp.getMessage())) {
                throw new WIMApplicationException(WIMMessageKey.AUTH_SUBJECT_FAILURE, Tr.formatMessage(tc, WIMMessageKey.AUTH_SUBJECT_FAILURE));
            } else {
                throw new WIMApplicationException(WIMMessageKey.AUTH_SUBJECT_CRED_FAILURE, Tr.formatMessage(tc, WIMMessageKey.AUTH_SUBJECT_CRED_FAILURE));
            }
        }

        //return unique name obtained from subject
        return uniqueName;
    }

    private String retrieveTargetRepository(IdentifierType id) throws WIMException {
        String result = null;

        String uniqueId = id.getUniqueId();
        String uniqueName = id.getUniqueName();

        if (!(uniqueId == null || uniqueId.trim().length() == 0)) {
            // uniqueId specified
            result = id.getRepositoryId();
            if (result == null) {
                retrieveEntity(null, id, false, null);
            }
            result = id.getRepositoryId();
        } else {
            // uniqueName specified
            result = repositoryManager.getRepositoryIdByUniqueName(uniqueName);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Group> separateGroups(List<Group> groups, String targetReposId) throws WIMException {
        groups = (List<Group>) setExtIdAndRepositoryIdForEntities(groups, targetReposId, false, null);
        List<Group> retGroups = new ArrayList<Group>();
        if (repositoryManager.getNumberOfRepositories() > 1 && groups != null) {
            for (int i = 0; i < groups.size(); i++) {
                Group grp = groups.get(i);
                IdentifierType grpId = grp.getIdentifier();
                if (grpId == null) {
                    throw new EntityIdentifierNotSpecifiedException(WIMMessageKey.ENTITY_IDENTIFIER_NOT_SPECIFIED, Tr.formatMessage(tc,
                                                                                                                                    WIMMessageKey.ENTITY_IDENTIFIER_NOT_SPECIFIED,
                                                                                                                                    WIMMessageHelper.generateMsgParms(grp)));
                }
                String uniqueName = grpId.getUniqueName();
                if (uniqueName != null) {
                    String reposId = repositoryManager.getRepositoryIdByUniqueName(uniqueName);
                    if (reposId.equals(targetReposId)) {
                        retGroups.add(grp);
                        groups.remove(i);
                        i--;
                    }
                }
            }
        } else {
            if (groups != null) {
                retGroups.addAll(groups);
                groups.clear();
            }
        }

        return retGroups;
    }

    private void checkCreateAndModifyTimeStamp(String qualifiedEntityType, Entity entity) throws WIMException {
        // STOP USER FROM UPDATING THE CREATE AND MODIFIED TIMESTAMP VALUES!
        // This is now done in the Entity Model class itself
    }

    /**
     * Get the uniqueName based on the specified uniqueId
     *
     * @param uniqueId the unique ID of an entity
     * @return the unique name of the entity which has the specified unique ID
     * @throws WIMException
     */
    @FFDCIgnore({ EntityNotFoundException.class, Exception.class })
    private String getUniqueNameByUniqueId(String uniqueId, boolean isAllowOperationIfReposDown, Set<String> failureRepositoryIds) throws WIMException {
        final String METHODNAME = "getUniqueNameByUniqueId";
        String uniqueName = null;

        boolean found = false;
        int i = 0;
        Root temp = new Root();
        Entity entity = new Entity();
        IdentifierType id = new IdentifierType();
        id.setExternalId(uniqueId);
        entity.setIdentifier(id);
        temp.getEntities().add(entity);

        List<String> repositoryIds = repositoryManager.getRepoIds();
        while (i < repositoryIds.size() && !found) {
            try {
                Root returnedRoot = repositoryManager.getRepository(repositoryIds.get(i)).get(temp);
                if (returnedRoot != null) {
                    List<Entity> pes = returnedRoot.getEntities();
                    if (pes != null) {
                        Entity ent = pes.get(0);
                        if (ent != null) {
                            IdentifierType entityId = ent.getIdentifier();
                            if (entityId != null) {
                                uniqueName = entityId.getUniqueName();
                                found = true;
                            }
                        }
                    }
                }
            } catch (EntityNotFoundException e) {
                // Move on to the next repository
                i++;
            } catch (Exception e) {
                if (!isAllowOperationIfReposDown) {
                    if (e instanceof WIMException) {
                        throw (WIMException) e;
                    }
                    throw new WIMException(e);
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, METHODNAME + " IGNORE: exception [" + e.getMessage()
                                     + "] on repository [" + repositoryIds.get(i) + "]");
                    failureRepositoryIds.add(repositoryIds.get(i));

                    // Move on to the next repository
                    i++;
                }
            }
        }
        return uniqueName;
    }

    @Override
    public Root update(Root rootDO) throws WIMException {
        //Please DONOT UPDATE or CHANGE this wrapper.
        //If a need arises, please updated the updateImpl method call.

        final String METHODNAME = "update";
        return genericProfileManagerMethod(METHODNAME, ProfileManager.UPDATE, rootDO);
    }

    @FFDCIgnore({ EntityNotFoundException.class, InvalidIdentifierException.class, OperationNotSupportedException.class })
    public Root updateImpl(Root root) throws WIMException {
        final String METHODNAME = "updateImpl";

        if (root == null) {
            return null;
        }

        AuditManager auditManager = new AuditManager();
        auditManager.setRequestType(AuditConstants.UPDATE_AUDIT);
        if (repositoryManager.getNumberOfRepositories() == 1) {
            auditManager.setRepositoryId(repositoryManager.getRepoIds().get(0));
        }

        Root retRoot = null;
        String repositoryId = null;
        Root laRetRoot = null;
        boolean updateCrossRepos = true;

        //If Cache Control is present then update operation is invoked for clearing the cache.
        Map<String, Control> ctrlMap = ControlsHelper.getControlMap(root);
        CacheControl cacheCtrl = (CacheControl) ctrlMap.get(DO_CACHE_CONTROL);
        if (cacheCtrl != null) {
            String cacheMode = cacheCtrl.getMode();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " Cache Control is passed with mode " + cacheMode);
            }

            if (cacheMode != null && CACHE_MODE_CLEARALL.equalsIgnoreCase(cacheMode)) {
                // Get the entities
                List<Entity> entities = root.getEntities();

                // Iterate over the entities and remove each one from the cache.
                for (int i = 0; i < entities.size(); i++) {
                    Entity entity = entities.get(i);

                    // Get the identifier
                    IdentifierType id = entity.getIdentifier();

                    // Get the repositoryID
                    repositoryId = id.getRepositoryId();

                    if (repositoryId != null && repositoryId.trim().length() > 0) {
                        // Check if the user specified LA repository
                        if (repositoryId.equalsIgnoreCase("LA")) {
                            // Clear the cache for the lookaside repository
                            // LookasideRepository laRepo = reposMgr.getLookasideRepository();
                            // laRepo.update(newRoot);
                        } else {
                            // Clear the cache of only specified repository
                            repositoryManager.getRepository(repositoryId).update(root);
                        }
                    } else {
                        // Clear the cache of all the repositories
                        int numberOfRepositories = repositoryManager.getNumberOfRepositories();

                        // Iterate over all configured repositories and clear the cache.
                        for (int reposIndex = 0; reposIndex < numberOfRepositories; reposIndex++) {
                            try {
                                Repository repo = repositoryManager.getRepository(repositoryManager.getRepoIds().get(reposIndex));
                                repo.update(root);
                            } catch (EntityNotFoundException e) {
                                // Consuming this exception
                                if (tc.isDebugEnabled()) {
                                    Tr.debug(tc, METHODNAME + " Exception in clear cache", WIMMessageHelper.generateMsgParms(e.getMessage()));
                                }
                            } catch (InvalidIdentifierException e) {
                                // Consuming this exception
                                if (tc.isDebugEnabled()) {
                                    Tr.debug(tc, METHODNAME + " Exception in clear cache", WIMMessageHelper.generateMsgParms(e.getMessage()));
                                }
                            } catch (OperationNotSupportedException e) {
                                // Consuming this exception, as this would be thrown by a custom read-only adapter.
                                if (tc.isDebugEnabled()) {
                                    Tr.debug(tc, METHODNAME + " Exception in clear cache", WIMMessageHelper.generateMsgParms(e.getMessage()));
                                }
                            } catch (WIMApplicationException e) {
                                String messageKey = e.getMessageKey();
                                // If this message is thrown by a read only adapter i.e. UR Bridge, then consume this exception.
                                if (WIMMessageKey.CANNOT_WRITE_TO_READ_ONLY_REPOSITORY.equalsIgnoreCase(messageKey)) {
                                    Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.UPDATE_AUDIT, repositoryId,
                                                id.getUniqueName(),
                                                getRealmName(root), root,
                                                Integer.valueOf("209"));
                                    // Consuming this exception
                                    if (tc.isDebugEnabled()) {
                                        Tr.debug(tc, METHODNAME + " Exception in clear cache", WIMMessageHelper.generateMsgParms(e.getMessage()));
                                    }
                                } else {
                                    throw e;
                                }
                            }
                        }

                        // Clear the cache for the lookaside repository
                        // LookasideRepository laRepo = reposMgr.getLookasideRepository();
                        // if (laRepo != null)
                        // laRepo.update(root);
                    }
                }
            } else if (cacheMode != null && CACHE_MODE_CLEAR_ENTITY.equalsIgnoreCase(cacheMode)) {
                // Get the entities
                List<Entity> entities = root.getEntities();

                // Iterate over the entities and remove each one from the cache.
                for (int i = 0; i < entities.size(); i++) {
                    Entity entity = entities.get(i);

                    // Get the identifier
                    IdentifierType id = entity.getIdentifier();

                    // Get the repositoryID
                    repositoryId = id.getRepositoryId();

                    if (repositoryId != null && repositoryId.trim().length() > 0) {
                        Root tempRoot = new Root();
                        tempRoot.getEntities().add(entity);
                        CacheControl ctlDO = new CacheControl();
                        ctlDO.setMode(CACHE_MODE_CLEAR_ENTITY);

                        // Check if the user is in LA repository
                        if (repositoryId.equalsIgnoreCase("LA")) {
                            // Clear the user form cache of lookaside repository
                            // LookasideRepository laRepo = reposMgr.getLookasideRepository();
                            // laRepo.update(tempRoot);
                        } else {
                            // Clear the user from cache of specified repository
                            repositoryManager.getRepository(repositoryId).update(tempRoot);
                        }

                        // Get the respositories that store the group membership for the passed repository.
                        Iterator reposIds = repositoryManager.getRepositoriesForGroupMembership(repositoryId).iterator();
                        while (reposIds.hasNext()) {
                            String targetId = (String) reposIds.next();
                            if (!targetId.equals(repositoryId)) {
                                repositoryManager.getRepository(targetId).update(tempRoot);
                            }
                        }
                    }
                }
            }

            return null;
        }

        List<Entity> entities = root.getEntities();

        if (entities.size() == 0) {
            Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.UPDATE_AUDIT, repositoryId, null, getRealmName(root), root,
                        Integer.valueOf("201"));
            throw new EntityNotFoundException(WIMMessageKey.MISSING_ENTITY_DATA_OBJECT, Tr.formatMessage(tc, WIMMessageKey.MISSING_ENTITY_DATA_OBJECT,
                                                                                                         WIMMessageHelper.generateMsgParms(RepositoryManager.ACTION_DELETE)));
        } else if (entities.size() > 1) {
            Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.UPDATE_AUDIT, repositoryId, null, getRealmName(root), root,
                        Integer.valueOf("202"));
            throw new OperationNotSupportedException(WIMMessageKey.ACTION_MULTIPLE_ENTITIES_SPECIFIED, Tr.formatMessage(tc, WIMMessageKey.ACTION_MULTIPLE_ENTITIES_SPECIFIED,
                                                                                                                        WIMMessageHelper.generateMsgParms(RepositoryManager.ACTION_DELETE)));
        }

        Entity entity = entities.get(0);
        String entityType = entity.getTypeName();

        /* Ensure the principal is authorized to update the entity */
        // profileSecManager.checkPermission_UPDATE(new EntityResource(inRoot, entity));

        IdentifierType identifier = entity.getIdentifier();
        if (identifier == null) {
            Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.UPDATE_AUDIT, repositoryId, null, getRealmName(root), root,
                        Integer.valueOf("203"));
            throw new EntityIdentifierNotSpecifiedException(WIMMessageKey.ENTITY_IDENTIFIER_NOT_SPECIFIED, Tr.formatMessage(tc, WIMMessageKey.ENTITY_IDENTIFIER_NOT_SPECIFIED));
        }

        // If properties of identifier are set, then do not need to retrieve the entity
        Entity realEntity = entity;
        if ((!identifier.isSet(PROP_EXTERNAL_ID)) || (!identifier.isSet(PROP_EXTERNAL_NAME))
            || (!identifier.isSet(PROP_REPOSITORY_ID))) {
            realEntity = retrieveEntity(identifier.getRepositoryId(), identifier, false, null);
        }

        repositoryId = identifier.getRepositoryId();

        String realEntityType = realEntity.getTypeName();

        String uniqueName = identifier.getUniqueName();

        String realmName = getRealmName(root);
        if (realmName != null && !getConfigManager().isUniqueNameInRealm(uniqueName, realmName)) {
            Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.UPDATE_AUDIT, repositoryId, uniqueName, realmName, root,
                        Integer.valueOf("204"));
            throw new EntityNotInRealmScopeException(WIMMessageKey.ENTITY_NOT_IN_REALM_SCOPE, Tr.formatMessage(tc, WIMMessageKey.ENTITY_NOT_IN_REALM_SCOPE,
                                                                                                               WIMMessageHelper.generateMsgParms(uniqueName, realmName)));
        }

        checkCreateAndModifyTimeStamp(realEntityType, entity);

        //reference type
        processReferenceProperty(entity, entityType, false, false, null);

        // process members if it is a group
        if (realEntity.getSuperTypes() != null && realEntity.getSuperTypes().contains(DO_GROUP)) {
            setExtIdAndRepositoryIdForEntities(((Group) entity).getMembers(), repositoryId, false, null);
            List<Entity> inputMbrs = ((Group) entity).getMembers();
            if (inputMbrs != null && inputMbrs.size() > 0) {
                for (int i = 0; i < inputMbrs.size(); i++) {
                    Entity member = inputMbrs.get(i);
                    IdentifierType memberId = member.getIdentifier();
                    String reposId = retrieveTargetRepository(memberId);
                    boolean crossRepos = repositoryManager.canGroupAcceptMember(repositoryId, reposId);
                    if (crossRepos || repositoryId.equalsIgnoreCase(reposId)) {
                        updateCrossRepos = updateCrossRepos && false;
                    } else {
                        Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.UPDATE_AUDIT, repositoryId, uniqueName, realmName,
                                    root,
                                    Integer.valueOf("207"));
                        throw new OperationNotSupportedException(WIMMessageKey.MISSING_REPOSITORIES_FOR_GROUPS_CONFIGURATION, Tr.formatMessage(tc,
                                                                                                                                               WIMMessageKey.MISSING_REPOSITORIES_FOR_GROUPS_CONFIGURATION,
                                                                                                                                               WIMMessageHelper.generateMsgParms(reposId)));
                    }
                }
            }
        }

        List<Group> inputGrps = entity.getGroups();
        if (inputGrps != null && inputGrps.size() > 0) {
            if (repositoryManager.isCrossRepositoryGroupMembership(repositoryId)) {
                updateCrossRepos = true;
                setExtIdAndRepositoryIdForEntities(entities, repositoryId, false, null);
                HashMap<String, List<Group>> grps = new HashMap<String, List<Group>>();
                for (int i = 0; i < inputGrps.size(); i++) {
                    Group group = inputGrps.get(i);
                    IdentifierType inputId = group.getIdentifier();
                    String reposId = retrieveTargetRepository(inputId);
                    List<Group> reposGrps = grps.get(reposId);
                    if (reposGrps == null) {
                        reposGrps = new ArrayList<Group>();
                    }
                    reposGrps.add(group);
                    grps.put(reposId, reposGrps);
                }
                Iterator<String> iter = grps.keySet().iterator();

                List<Group> reposGrps = grps.get(repositoryId);
                if (reposGrps == null) {
                    entity.unset(DO_GROUPS);
                } else {
                    entity.getGroups().addAll(reposGrps);
                }

                // checkAccessibility(UPDATE, RepositoryManager.ACTION_UPDATE, repositoryId, realEntityType);
                retRoot = repositoryManager.getRepository(repositoryId).update(root);

                Iterator<String> reposIds = repositoryManager.getRepositoriesForGroupMembership(repositoryId).iterator();
                while (reposIds.hasNext()) {
                    String targetId = reposIds.next();
                    if (!targetId.equals(repositoryId)) {
                        reposGrps = grps.get(targetId);
                        if (reposGrps != null && reposGrps.size() > 0) {
                            Root inputRoot = new Root();
                            entity.getGroups().addAll(reposGrps);
                            inputRoot.getEntities().add(entity);
                            // checkAccessibility(UPDATE, RepositoryManager.ACTION_UPDATE, targetId, realEntityType);
                            Root retDgForGrpRoot = repositoryManager.getRepository(targetId).update(inputRoot);
                        }
                    }
                }
            } else {
                for (int i = 0; i < inputGrps.size(); i++) {
                    Group group = inputGrps.get(i);
                    IdentifierType groupId = group.getIdentifier();
                    String reposId = retrieveTargetRepository(groupId);
                    if (!repositoryId.equalsIgnoreCase(reposId)) {
                        Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.UPDATE_AUDIT, repositoryId, uniqueName, realmName,
                                    root,
                                    Integer.valueOf("207"));
                        throw new OperationNotSupportedException(WIMMessageKey.MISSING_REPOSITORIES_FOR_GROUPS_CONFIGURATION, Tr.formatMessage(tc,
                                                                                                                                               WIMMessageKey.MISSING_REPOSITORIES_FOR_GROUPS_CONFIGURATION,
                                                                                                                                               WIMMessageHelper.generateMsgParms(reposId)));
                    } else {
                        updateCrossRepos = updateCrossRepos && false;
                    }
                }
            }
        } else {
            updateCrossRepos = false;
        }

        if (!updateCrossRepos) {
            auditManager.setRepositoryId(repositoryId);
            auditManager.setRepositoryRealm(realmName);
            auditManager.setRepositoryUniqueName(uniqueName);

            setUniqueName(inputGrps);
            // checkAccessibility(UPDATE, RepositoryManager.ACTION_UPDATE, repositoryId, realEntityType);

            retRoot = repositoryManager.getRepository(repositoryId).update(root);
        }

        Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), AuditConstants.UPDATE_AUDIT, repositoryId, uniqueName,
                    realmName != null ? realmName : getRealmNameOrFirstBest(retRoot), retRoot, Integer.valueOf("200"));

        return retRoot;
    }

    private List<Group> setUniqueName(List<Group> inputGrps) throws WIMException {
        if (inputGrps == null) {
            return null;
        } else {
            for (int i = 0; i < inputGrps.size(); i++) {
                Group mbrEntDO = inputGrps.get(i);
                IdentifierType mbrEntIdDO = mbrEntDO.getIdentifier();
                if (mbrEntIdDO == null) {
                    throw new EntityIdentifierNotSpecifiedException(WIMMessageKey.ENTITY_IDENTIFIER_NOT_SPECIFIED, Tr.formatMessage(tc,
                                                                                                                                    WIMMessageKey.ENTITY_IDENTIFIER_NOT_SPECIFIED,
                                                                                                                                    WIMMessageHelper.generateMsgParms(mbrEntDO)));
                }
                String uniqueId = mbrEntIdDO.getUniqueId();
                String uniqueName = mbrEntIdDO.getUniqueName();
                if (uniqueName == null) {
                    if (uniqueId == null) {
                        throw new EntityIdentifierNotSpecifiedException(WIMMessageKey.ENTITY_IDENTIFIER_NOT_SPECIFIED, Tr.formatMessage(tc,
                                                                                                                                        WIMMessageKey.ENTITY_IDENTIFIER_NOT_SPECIFIED,
                                                                                                                                        WIMMessageHelper.generateMsgParms(mbrEntDO)));
                    }
                    uniqueName = getUniqueNameByUniqueId(uniqueId, false, null);
                    if (uniqueName == null) {
                        AuditManager auditManager = new AuditManager();
                        Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), auditManager.getRequestType(), auditManager.getRepositoryId(), uniqueName,
                                    auditManager.getRepositoryRealm(), null, Integer.valueOf("212"));
                        throw new EntityNotFoundException(WIMMessageKey.ENTITY_NOT_FOUND, Tr.formatMessage(tc, WIMMessageKey.ENTITY_NOT_FOUND,
                                                                                                           WIMMessageHelper.generateMsgParms(uniqueId)));
                    } else {
                        mbrEntIdDO.setUniqueName(uniqueName);
                    }
                }
            }
            return inputGrps;
        }
    }

    /**
     * @param searchControl
     * @param sortControl
     * @return
     */
    private String getPageCacheKey(SearchControl searchControl, SortControl sortControl) {
        StringBuilder cacheKey = new StringBuilder();

        // Search Filter
        cacheKey.append(searchControl.getExpression());

        cacheKey.append("|");

        // Include Properties asked in return.
        List<String> properties = searchControl.getProperties();
        if (properties != null) {
            for (String property : properties) {
                cacheKey.append("|");
                cacheKey.append(property);
            }
        }

        cacheKey.append("|");

        // Include the Sort keys
        if (sortControl != null) {
            List<SortKeyType> sortKeys = sortControl.getSortKeys();
            for (SortKeyType sortKey : sortKeys) {
                cacheKey.append(sortKey.getPropertyName());
                cacheKey.append("|");
                cacheKey.append(sortKey.isAscendingOrder());
                cacheKey.append("|");
            }
        }

        return cacheKey.toString();
    }

    /**
     * @param returnRoot
     */
    private boolean isURBridgeResult(Root returnRoot) {
        // Check if there is a valid response
        if (returnRoot != null && !returnRoot.getEntities().isEmpty()) {
            // Determine if the return object to check if the context was set.
            List<Context> contexts = returnRoot.getContexts();
            for (Context context : contexts) {
                String key = context.getKey();

                if (key != null && SchemaConstantsInternal.IS_URBRIDGE_RESULT.equals(key)) {
                    if ("true".equalsIgnoreCase((String) context.getValue()))
                        return true;
                }
            }
        }
        return false;
    }

}
