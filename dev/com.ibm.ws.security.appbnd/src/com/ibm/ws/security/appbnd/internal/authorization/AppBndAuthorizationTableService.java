/*******************************************************************************
 * Copyright (c) 2011, 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.appbnd.internal.authorization;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.metadata.ApplicationMetaDataListener;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.security.SecurityRoles;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.appbnd.Group;
import com.ibm.ws.javaee.dd.appbnd.SecurityRole;
import com.ibm.ws.javaee.dd.appbnd.SpecialSubject;
import com.ibm.ws.javaee.dd.appbnd.User;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.IdentityStoreHandlerService;
import com.ibm.ws.security.authorization.AuthorizationTableService;
import com.ibm.ws.security.authorization.RoleSet;
import com.ibm.ws.security.authorization.builtin.BaseAuthorizationTableService;
import com.ibm.ws.security.delegation.DefaultDelegationProvider;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryChangeListener;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.ws.security.token.internal.TraceConstants;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * WebAppAuthorizationTableService handles the creation
 * of the authorization table when an application is deployed, and its
 * destruction when the application is undeployed.
 * <p>
 * If security is enabled dynamically, then the applications will be
 * re-deployed, which will trigger this listener to be called.
 */
@Component(service = { ApplicationMetaDataListener.class, AuthorizationTableService.class, UserRegistryChangeListener.class },
           name = "com.ibm.ws.security.appbnd.AppBndAuthorizationTableService",
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM", "com.ibm.ws.security.authorization.table.name=WebApp" })
public class AppBndAuthorizationTableService extends BaseAuthorizationTableService implements ApplicationMetaDataListener, AuthorizationTableService, UserRegistryChangeListener {
    private static final TraceComponent tc = Tr.register(AppBndAuthorizationTableService.class);

    static final String KEY_IDENTITY_STORE_HANDLER_SERVICE = "identityStoreHandlerService";
    static final String KEY_DEFAULT_DELEGATION_PROVIDER_SERVICE = "defaultDelegationProviderService";
    private final AtomicServiceReference<IdentityStoreHandlerService> identityStoreHandlerServiceRef = new AtomicServiceReference<IdentityStoreHandlerService>(KEY_IDENTITY_STORE_HANDLER_SERVICE);
    private final AtomicServiceReference<DefaultDelegationProvider> defaultDelegationProviderServiceRef = new AtomicServiceReference<DefaultDelegationProvider>(KEY_DEFAULT_DELEGATION_PROVIDER_SERVICE);
    DefaultDelegationProvider defaultDelegationProvider = null;

    /**
     * Invalid access ID used to indicate an attempt was made to compute the
     * accessID but the ID was invalid (due to no such entry or some other
     * reason). If the registry changes, we'll purge our cache and re-compute.
     */
    private final static String INVALID_ACCESS_ID = "";

    /**
     * Security roles from the application bindings (server.xml or ear),
     * one entry per application. Access to the backing Collection must be
     * concurrent, however the backing Collection need not be.
     * <p>
     * Currently, application deployment tends to run serially (one thread
     * doing all app deployments) so having the concurrencyLevel set to 1
     * is sufficient. At the time of this writing (2012/04/25) this map is
     * only modified during application deployment and undeployment.
     */
    private final ConcurrentMap<String, AuthzInfo> resourceToAuthzInfoMap = new ConcurrentHashMap<String, AuthzInfo>(16, 0.7f, 1);

    @Reference(service = IdentityStoreHandlerService.class, name = KEY_IDENTITY_STORE_HANDLER_SERVICE,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setIdentityStoreHandlerService(ServiceReference<IdentityStoreHandlerService> reference) {
        identityStoreHandlerServiceRef.setReference(reference);
    }

    protected void unsetIdentityStoreHandlerService(ServiceReference<IdentityStoreHandlerService> reference) {
        identityStoreHandlerServiceRef.unsetReference(reference);
    }

    @Reference(service = DefaultDelegationProvider.class, name = KEY_DEFAULT_DELEGATION_PROVIDER_SERVICE,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    public void setDefaultDelegationProvider(ServiceReference<DefaultDelegationProvider> ref) {
        defaultDelegationProviderServiceRef.setReference(ref);
    }

    public void unsetDefaultDelegationProvider(ServiceReference<DefaultDelegationProvider> ref) {
        defaultDelegationProviderServiceRef.unsetReference(ref);
    }

    private static final class AuthzInfo {
        final Collection<SecurityRole> securityRoles;
        volatile AuthzTableContainer authzTableContainer;
        boolean hasSecurityRole = false;

        /**
         * @param securityRoles
         * @param authzTableContainer
         */
        public AuthzInfo(String resourceName, Collection<SecurityRole> securityRoles) {
            this.securityRoles = securityRoles;
            this.authzTableContainer = new AuthzTableContainer(resourceName);
            if (securityRoles != null && !securityRoles.isEmpty()) {
                hasSecurityRole = true;
            }
        }

        public boolean hasRoles() {
            return hasSecurityRole;
        }

    }

    /**
     * Container class for the computed authorization table.
     * <p>
     * The AuthzTableContainer will be updated during application access and
     * this access needs to be thread-safe. The concurrencyLevel of 1 was chosen
     * as the frequency of updates relative to the lifetime of the map is, on
     * average, so low that the contention over the write lock versus the size
     * of the table is a reasonable trade-off.
     */
    private static final class AuthzTableContainer {
        final String resourceName;
        final ConcurrentMap<String, RoleSet> specialSubjectMap = new ConcurrentHashMap<String, RoleSet>(16, 0.7f, 1);
        final ConcurrentMap<String, RoleSet> accessIdToRolesMap = new ConcurrentHashMap<String, RoleSet>(16, 0.7f, 1);
        final ConcurrentMap<String, String> userToAccessIdMap = new ConcurrentHashMap<String, String>(16, 0.7f, 1);
        final ConcurrentMap<String, String> groupToAccessIdMap = new ConcurrentHashMap<String, String>(16, 0.7f, 1);

        AuthzTableContainer(String resourceName) {
            this.resourceName = resourceName;
        }
    }

    @Override
    protected void activate(ComponentContext cc) {
        super.activate(cc);
        identityStoreHandlerServiceRef.activate(cc);
        defaultDelegationProviderServiceRef.activate(cc);
    }

    @Override
    protected void deactivate(ComponentContext cc) {
        super.deactivate(cc);
        identityStoreHandlerServiceRef.deactivate(cc);
        defaultDelegationProviderServiceRef.deactivate(cc);
    }

    /**
     * Establishes the basic information that comprises an authorization table,
     * but all real work is deferred until later. This has some benefits:<p>
     * <ol>
     * <li>Faster application deployment</li>
     * <li>If the authorization table is not used, such as for SAF authorization,
     * we do not incur much cost to establish the table.</li>
     * </ol>
     * <p>
     * If a table already exists for the application name, issue an error. Two
     * applications with the same name is an error, and we should not be going
     * through appDeployed without having gone through appUndeployed.
     *
     * @param appName
     * @param securityRoles
     * @return {@code true} if the (new) table was created, {@code false} if there was an existing table.
     */
    private boolean establishInitialTable(String appName, Collection<SecurityRole> secRoles) {
        // Create and add a new table if we don't have a cached copy
        if (resourceToAuthzInfoMap.putIfAbsent(appName, new AuthzInfo(appName, secRoles)) == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Created initial authorization tables for " + appName);
            }
            return true;
        }
        return false;
    }

    @Override
    public void applicationMetaDataCreated(MetaDataEvent<ApplicationMetaData> event) throws MetaDataException {
        String appName = event.getMetaData().getJ2EEName().getApplication();
        try {
            Collection<SecurityRole> securityRoles = event.getContainer().adapt(SecurityRoles.class).getSecurityRoles();
            if (!establishInitialTable(appName, securityRoles)) {
                Tr.error(tc, "AUTHZ_TABLE_DUPLICATE_APP_NAME", appName);
                throw new MetaDataException(TraceNLS.getFormattedMessage(
                                                                         this.getClass(),
                                                                         TraceConstants.MESSAGE_BUNDLE,
                                                                         "AUTHZ_TABLE_DUPLICATE_APP_NAME",
                                                                         new Object[] { appName },
                                                                         "CWWKS9110E: Multiple applications have the name {0}. Security authorization policies requires that names be unique."));
            }
            defaultDelegationProvider = defaultDelegationProviderServiceRef.getService();
            if (defaultDelegationProvider != null)
                defaultDelegationProvider.createAppToSecurityRolesMapping(appName, securityRoles);
        } catch (UnableToAdaptException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "There was a problem setting the security meta data for application " + appName + ".", e);
            }
            Tr.error(tc, "AUTHZ_TABLE_NOT_CREATED", appName);
        }
    }

    @Override
    public void applicationMetaDataDestroyed(MetaDataEvent<ApplicationMetaData> event) {
        String appName = event.getMetaData().getJ2EEName().getApplication();
        removeTable(appName);
        defaultDelegationProvider = defaultDelegationProviderServiceRef.getService();
        if (defaultDelegationProvider != null)
            defaultDelegationProvider.removeRoleToRunAsMapping(appName);
    }

    /**
     * Remove the tables for the given appName.
     *
     * @param appName
     */
    private void removeTable(String appName) {
        resourceToAuthzInfoMap.remove(appName);
    }

    /**
     * Parse the security-role entries to look for the specified special
     * subject, and update the subject-to-roles mapping with the result.
     * If the special subject was not found, an empty list is added to
     * the subject-to-roles map. Otherwise, the list contains the set of
     * roles mapped to the special subject.
     *
     * @param appName the name of the application, this is the key used when
     *            updating the subject-to-role map
     * @param specialSubjectToRolesMap the subject-to-role mapping,
     *            key: appName, value: list of roles (possibly empty)
     * @param specialSubjectName the string representing the special subject
     *            to look for. It can be one of these values:
     *            EVERYONE
     *            ALL_AUTHENTICATED_USERS
     *            ALL_AUTHENTICATED_IN_TRUSTED_REALMS
     * @param secRoles the security-role entries, previously read either
     *            from server.xml or ibm-application.bnd.xmi/xml
     * @return the updated subject-to-roles map
     */
    private Map<String, RoleSet> updateMapForSpecialSubject(String appName,
                                                            Map<String, RoleSet> specialSubjectToRolesMap,
                                                            String specialSubjectName) {
        RoleSet computedRoles = RoleSet.EMPTY_ROLESET;
        Set<String> rolesForSubject = new HashSet<String>();

        //TODO what if the appName is not present in the map?
        for (SecurityRole role : resourceToAuthzInfoMap.get(appName).securityRoles) {
            String roleName = role.getName();

            for (SpecialSubject specialSubject : role.getSpecialSubjects()) {
                String specialSubjectNameFromRole = specialSubject.getType().toString();
                if (specialSubjectName.equals(specialSubjectNameFromRole)) {
                    rolesForSubject.add(roleName);
                }
            }
        }

        if (!rolesForSubject.isEmpty()) {
            computedRoles = new RoleSet(rolesForSubject);
        }

        specialSubjectToRolesMap.put(specialSubjectName, computedRoles);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Added the following subject to role mapping for application: " + appName + ".",
                     specialSubjectName, computedRoles);
        }

        return specialSubjectToRolesMap;
    }

    /** {@inheritDoc} */
    @Override
    public RoleSet getRolesForSpecialSubject(String appName, String specialSubject) {
        AuthzInfo authzInfo = resourceToAuthzInfoMap.get(appName);
        if (authzInfo != null) {
            Map<String, RoleSet> subjectToRolesMap = authzInfo.authzTableContainer.specialSubjectMap;
            if (subjectToRolesMap.get(specialSubject) == null) {
                subjectToRolesMap = updateMapForSpecialSubject(appName, subjectToRolesMap, specialSubject);
            }
            return subjectToRolesMap.get(specialSubject);
        } else {
            return null;
        }
    }

    /**
     * Get the access id for a user or group in the bindings config by looking up
     * the user registry.
     *
     * @param subject the Subject object in the bindings config, can be User or Group
     * @return the access id of the Subject specified in the bindings config,
     *         otherwise null when a registry error occurs or the entry is not found
     */
    @FFDCIgnore(EntryNotFoundException.class)
    private String getMissingAccessId(com.ibm.ws.javaee.dd.appbnd.Subject subjectFromArchive) {
        String subjectType = null;
        try {
            SecurityService securityService = securityServiceRef.getService();
            UserRegistryService userRegistryService = securityService.getUserRegistryService();
            if (!userRegistryService.isUserRegistryConfigured())
                return null;
            UserRegistry userRegistry = userRegistryService.getUserRegistry();
            String realm = userRegistry.getRealm();
            if (subjectFromArchive instanceof Group) {
                subjectType = "group";
                String groupUniqueId = userRegistry.getUniqueGroupId(subjectFromArchive.getName());
                return AccessIdUtil.createAccessId(AccessIdUtil.TYPE_GROUP, realm, groupUniqueId);
            } else if (subjectFromArchive instanceof User) {
                subjectType = "user";
                String uniqueId = userRegistry.getUniqueUserId(subjectFromArchive.getName());
                return AccessIdUtil.createAccessId(AccessIdUtil.TYPE_USER, realm, uniqueId);
            }
        } catch (EntryNotFoundException e) {
            if (TraceComponent.isAnyTracingEnabled()) {
                if (tc.isEventEnabled()) {
                    Tr.event(tc, "No entry found for " + subjectType + " "
                                 + subjectFromArchive.getName() + " found in user registry. Unable to create access ID.");
                }
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "EntryNotFoundException details:", e);
                }
            }
        } catch (RegistryException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unexpected exception getting the accessId for "
                             + subjectFromArchive.getName() + ": " + e);
            }
        }
        return null;
    }

    /**
     * Update the map for the specified user name. If the accessID is
     * successfully computed, the map will be updated with the accessID.
     * If the accessID can not be computed due to the user not being found,
     * INVALID_ACCESS_ID will be stored.
     *
     * @param maps
     * @param user
     * @param userNameFromRole
     * @return
     */
    private String updateMissingUserAccessId(AuthzTableContainer maps, User user, String userNameFromRole) {
        String accessIdFromRole;
        accessIdFromRole = getMissingAccessId(user);
        if (accessIdFromRole != null) {
            maps.userToAccessIdMap.put(userNameFromRole, accessIdFromRole);
        } else {
            // Unable to compute the accessId, store an invalid access ID indicate this
            maps.userToAccessIdMap.put(userNameFromRole, INVALID_ACCESS_ID);
        }
        return accessIdFromRole;
    }

    /**
     * Update the map for the specified group name. If the accessID is
     * successfully computed, the map will be updated with the accessID.
     * If the accessID can not be computed due to the user not being found,
     * INVALID_ACCESS_ID will be stored.
     *
     * @param maps
     * @param group
     * @param groupNameFromRole
     * @return
     */
    private String updateMissingGroupAccessId(AuthzTableContainer maps, Group group, String groupNameFromRole) {
        String accessIdFromRole;
        accessIdFromRole = getMissingAccessId(group);
        if (accessIdFromRole != null) {
            maps.groupToAccessIdMap.put(groupNameFromRole, accessIdFromRole);
        } else {
            // Unable to compute the accessId, store an invalid access ID indicate this
            // and avoid future attempts
            maps.groupToAccessIdMap.put(groupNameFromRole, INVALID_ACCESS_ID);
        }
        return accessIdFromRole;
    }

    /**
     * Parse the security-role entries to look for the specified accessId, and
     * update the accessId-to-roles mapping with the result. If the accessId was
     * not found, an empty list is added to the accessId-to-role map. Otherwise,
     * the list contains the set of roles mapped to the accessId.
     *
     * @param accessid the access id of the entity
     * @param realmName the realm name of the entity (this value suppoes to get from wscredential)
     * @param appName the name of the application, this is the key used when updating the
     *            accessId-to-roles map
     * @param secRoles the security-role entries, previously read either
     *            from server.xml or ibm-application.bnd.xmi/xml
     * @return the updated accessId-to-roles map
     */
    private Map<String, RoleSet> updateMapsForAccessId(String appName, String accessId, String realmName) {
        RoleSet computedRoles = RoleSet.EMPTY_ROLESET;
        Set<String> rolesForSubject = new HashSet<String>();

        AuthzInfo authzInfo = resourceToAuthzInfoMap.get(appName);
        AuthzTableContainer maps = authzInfo.authzTableContainer;

        for (SecurityRole role : authzInfo.securityRoles) {
            String roleName = role.getName();

            if (AccessIdUtil.isUserAccessId(accessId)) {
                Iterator<User> users = role.getUsers().iterator();
                while (users.hasNext()) {
                    User user = users.next();
                    String userNameFromRole = user.getName();
                    String accessIdFromRole = user.getAccessId();
                    if (accessIdFromRole == null || accessIdFromRole.isEmpty()) {
                        accessIdFromRole = maps.userToAccessIdMap.get(userNameFromRole);
                        if (accessIdFromRole == null) {
                            accessIdFromRole = updateMissingUserAccessId(maps, user, userNameFromRole);
                        }
                    } else if (!AccessIdUtil.isUserAccessId(accessIdFromRole)) {
                        accessIdFromRole = getCompleteAccessId(accessId, accessIdFromRole, AccessIdUtil.TYPE_USER, realmName);
                        maps.userToAccessIdMap.put(userNameFromRole, accessIdFromRole);
                    }

                    if (isMatch(accessId, accessIdFromRole)) {
                        rolesForSubject.add(roleName);
                    }
                }
            } else if (AccessIdUtil.isGroupAccessId(accessId)) {
                Iterator<Group> groups = role.getGroups().iterator();
                while (groups.hasNext()) {
                    Group group = groups.next();
                    String groupNameFromRole = group.getName();
                    String accessIdFromRole = group.getAccessId();
                    if (accessIdFromRole == null || accessIdFromRole.isEmpty()) {
                        accessIdFromRole = maps.groupToAccessIdMap.get(groupNameFromRole);
                        if (accessIdFromRole == null) {
                            accessIdFromRole = updateMissingGroupAccessId(maps, group, groupNameFromRole);
                        }
                    } else if (!AccessIdUtil.isGroupAccessId(accessIdFromRole)) {
                        accessIdFromRole = getCompleteAccessId(accessId, accessIdFromRole, AccessIdUtil.TYPE_GROUP, realmName);
                        maps.groupToAccessIdMap.put(groupNameFromRole, accessIdFromRole);
                    }

                    if (isMatch(accessId, accessIdFromRole)) {
                        rolesForSubject.add(roleName);
                    }
                }
            }
        }

        if (!rolesForSubject.isEmpty()) {
            computedRoles = new RoleSet(rolesForSubject);
        }

        maps.accessIdToRolesMap.put(accessId, computedRoles);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Added the following subject to role mapping for application: " + appName + ".",
                     accessId, computedRoles);
        }

        return maps.accessIdToRolesMap;
    }

    private String getCompleteAccessId(String accessIdFromSubject, String accessIdFromRole, String type, String realmName) {
        String tempAccessId = type + AccessIdUtil.TYPE_SEPARATOR + accessIdFromRole;
        if (AccessIdUtil.isAccessId(tempAccessId)) {
            return tempAccessId;
        } else {
            if (realmName == null) {
                realmName = AccessIdUtil.getRealm(accessIdFromSubject);
            }
            return AccessIdUtil.createAccessId(type, realmName, accessIdFromRole);
        }
    }

    /** {@inheritDoc} */
    @Override
    public RoleSet getRolesForAccessId(String appName, String accessId) {
        return getRolesForAccessId(appName, accessId, null);
    }

    /** {@inheritDoc} */
    @Override
    public RoleSet getRolesForAccessId(String appName, String accessId, String realmName) {
        AuthzInfo authzInfo = resourceToAuthzInfoMap.get(appName);
        if (authzInfo != null) {
            Map<String, RoleSet> accessIdToRolesMap = authzInfo.authzTableContainer.accessIdToRolesMap;
            if (accessIdToRolesMap.get(accessId) == null) {
                accessIdToRolesMap = updateMapsForAccessId(appName, accessId, realmName);
            }
            return accessIdToRolesMap.get(accessId);
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAuthzInfoAvailableForApp(String appName) {
        AuthzInfo authzInfo = resourceToAuthzInfoMap.get(appName);
        return (authzInfo != null && authzInfo.hasRoles() == true ? true : false);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyOfUserRegistryChange() {
        super.notifyOfUserRegistryChange();
        for (AuthzInfo authzInfo : resourceToAuthzInfoMap.values()) {
            authzInfo.authzTableContainer = new AuthzTableContainer(authzInfo.authzTableContainer.resourceName);
        }
    }
}