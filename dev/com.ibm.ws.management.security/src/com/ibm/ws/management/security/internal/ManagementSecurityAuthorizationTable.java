/*******************************************************************************
 * Copyright (c) 2012, 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.management.security.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.management.security.ManagementRole;
import com.ibm.ws.management.security.ManagementSecurityConstants;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authorization.AuthorizationTableService;
import com.ibm.ws.security.authorization.RoleSet;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryChangeListener;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;

/**
 * Management security authorization table.
 */
public class ManagementSecurityAuthorizationTable implements AuthorizationTableService, UserRegistryChangeListener {
    private static final TraceComponent tc = Tr.register(ManagementSecurityAuthorizationTable.class);

    static final String KEY_SECURITY_SERVICE = "securityService";
    static final String KEY_MANAGEMENT_ROLE = "managementRole";
    static final String KEY_LDAP_REGISTRY = "(service.factoryPid=com.ibm.ws.security.registry.ldap.config)";
    static final String KEY_IGNORE_CASE = "ignoreCase";

    private final RoleSet ADMIN_ROLE_SET;
    private final AtomicServiceReference<SecurityService> securityServiceRef = new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);
    private final ConcurrentServiceReferenceSet<ManagementRole> managementRoles = new ConcurrentServiceReferenceSet<ManagementRole>(KEY_MANAGEMENT_ROLE);
    static final String KEY_CONFIG_ADMIN = "configurationAdmin";
    protected final AtomicServiceReference<ConfigurationAdmin> configAdminRef = new AtomicServiceReference<ConfigurationAdmin>(KEY_CONFIG_ADMIN);

    /**
     * Currently there is only one resource name. If this changes, this logic
     * needs to be updated.
     */
    private final Map<String, RoleSet> accessIdToRoles = new HashMap<String, RoleSet>();
    private final Map<String, String> userToAccessId = new HashMap<String, String>();
    private final Map<String, String> groupToAccessId = new HashMap<String, String>();
    private final Map<String, RoleSet> userToRoles = new HashMap<String, RoleSet>();
    private final Map<String, RoleSet> groupToRoles = new HashMap<String, RoleSet>();

    private static HashSet<String> ALL_AUTHENTICATED_USERS_SET = new HashSet<String>();
    static {
        ALL_AUTHENTICATED_USERS_SET.add(ManagementSecurityConstants.ALL_AUTHENTICATED_USERS_ROLE_NAME);
    }
    private static final RoleSet ALL_AUTHENTICATED_USERS_ROLESET = new RoleSet(ALL_AUTHENTICATED_USERS_SET);

    private boolean isIgnoreCaseSet = false;
    private boolean isIgnoreCase = false;

    public ManagementSecurityAuthorizationTable() {
        Set<String> adminRoles = new HashSet<String>();
        adminRoles.add(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME);
        ADMIN_ROLE_SET = new RoleSet(adminRoles);
    }

    protected void setSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.setReference(reference);
    }

    protected void unsetSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.unsetReference(reference);
    }

    protected synchronized void setManagementRole(ServiceReference<ManagementRole> reference) {
        managementRoles.addReference(reference);
        populateInitialAuthorizationTable();
    }

    protected synchronized void updatedManagementRole(ServiceReference<ManagementRole> reference) {
        populateInitialAuthorizationTable();
    }

    protected synchronized void unsetManagementRole(ServiceReference<ManagementRole> reference) {
        managementRoles.removeReference(reference);
        populateInitialAuthorizationTable();
    }

    protected void setConfigurationAdmin(ServiceReference<ConfigurationAdmin> reference) {
        configAdminRef.setReference(reference);
    }

    protected void unsetConfigurationAdmin(ServiceReference<ConfigurationAdmin> reference) {
        configAdminRef.unsetReference(reference);
    }

    protected synchronized void activate(ComponentContext cc) {
        securityServiceRef.activate(cc);
        managementRoles.activate(cc);
        configAdminRef.activate(cc);
        populateInitialAuthorizationTable();
    }

    protected synchronized void deactivate(ComponentContext cc) {
        clearAuthorizationTable();
        configAdminRef.deactivate(cc);
        securityServiceRef.deactivate(cc);
        managementRoles.deactivate(cc);
    }

    /**
     * {@inheritDoc}<p>
     * This is a hardcoded mapping of the special subject ALL_AUTHENTICATED_USERS to the
     * reserved internal role name "allAuthenticatedUsers"
     */
    @Override
    public RoleSet getRolesForSpecialSubject(String resName, String specialSubject) {
        if (ManagementSecurityConstants.ADMIN_RESOURCE_NAME.equals(resName)) {
            if (specialSubject.equals(AuthorizationTableService.ALL_AUTHENTICATED_USERS)) {
                return ALL_AUTHENTICATED_USERS_ROLESET;
            } else {
                return RoleSet.EMPTY_ROLESET;
            }
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public RoleSet getRolesForAccessId(String resName, String accessId) {
        if (ManagementSecurityConstants.ADMIN_RESOURCE_NAME.equals(resName)) {
            return rolesForAccessId(accessId);
        } else {
            return null;
        }

    }

    /** {@inheritDoc} */
    @Override
    public RoleSet getRolesForAccessId(String resName, String accessId, String realmName) {
        return getRolesForAccessId(resName, accessId);
    }

    /**
     * Clear the authorization table.
     */
    private void clearAuthorizationTable() {
        accessIdToRoles.clear();
        userToAccessId.clear();
        groupToAccessId.clear();
        userToRoles.clear();
        groupToRoles.clear();
    }

    /**
     * Construct the authorization table for the given management roles.
     * <p>
     * Right now, this is implemented to do a full rebuild every time we
     * get new information. This logic probably could be improved for
     * performance, some day.
     * <p>
     * This will take all of the known roles and their associated users
     * and groups and convert that mapping to all known users and groups
     * with their associated roles.
     * <p>
     * Access IDs are not computed at this time and are deferred until
     * the first request.
     */
    private void populateInitialAuthorizationTable() {
        clearAuthorizationTable();

        Map<String, Set<String>> userToRoleName = new HashMap<String, Set<String>>();
        Map<String, Set<String>> groupToRoleName = new HashMap<String, Set<String>>();
        Iterator<ManagementRole> itr = managementRoles.getServices();
        while (itr.hasNext()) {
            ManagementRole role = itr.next();
            String roleName = role.getRoleName();
            for (String user : role.getUsers()) {
                Set<String> assignedRoles = userToRoleName.get(user);
                if (assignedRoles == null) {
                    assignedRoles = new HashSet<String>();
                    userToRoleName.put(user, assignedRoles);
                }
                assignedRoles.add(roleName);
            }

            for (String group : role.getGroups()) {
                Set<String> assignedRoles = groupToRoleName.get(group);
                if (assignedRoles == null) {
                    assignedRoles = new HashSet<String>();
                    groupToRoleName.put(group, assignedRoles);
                }
                assignedRoles.add(roleName);
            }

        }

        for (Map.Entry<String, Set<String>> entry : userToRoleName.entrySet()) {
            userToRoles.put(entry.getKey(), new RoleSet(entry.getValue()));
        }

        for (Map.Entry<String, Set<String>> entry : groupToRoleName.entrySet()) {
            groupToRoles.put(entry.getKey(), new RoleSet(entry.getValue()));
        }

    }

    /**
     * Retrieve the roles, if any, for the given accessId.
     *
     * @param accessId
     * @return a non-null RoleSet
     */
    private RoleSet rolesForAccessId(String accessId) {
        if (AccessIdUtil.isServerAccessId(accessId)) {
            return ADMIN_ROLE_SET;
        }

        RoleSet roles = accessIdToRoles.get(accessId);
        if (roles == null) {
            return findRolesForAccessId(accessId);
        } else {
            return roles;
        }
    }

    /**
     * Parse the security-role entries to look for the specified accessId, and
     * update the accessId-to-roles mapping with the result. If the accessId was
     * not found, an empty list is added to the accessId-to-role map. Otherwise,
     * the list contains the set of roles mapped to the accessId.
     *
     * @param accessid the access id of the entity
     * @param resName the name of the application, this is the key used when
     *            updating the accessId-to-roles map
     *
     * @return the updated accessId-to-roles map
     */
    private RoleSet findRolesForAccessId(String accessId) {

        if (!AccessIdUtil.isAccessId(accessId)) {
            throw new IllegalArgumentException("Invalid accessId");
        }

        if (AccessIdUtil.isUserAccessId(accessId)) {
            // An user can be configured as user or an user-access-Id
            // Ex: bob or user:realm/bob
            for (String user : userToRoles.keySet()) {
                String userAccessId = userToAccessId.get(user);
                if (userAccessId == null) {
                    if (AccessIdUtil.isUserAccessId(user)) {
                        userAccessId = user;
                    } else {
                        // user is not accessId format, so we have to look up the user registry.
                        userAccessId = getUserAccessId(user);
                        if (userAccessId == null) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Unable to determine accessId of user " + user);
                            }
                            // We could not determine the access ID here, skip
                            // For all unknown IDs we try to re-compute each time
                            // we see it. This is probably the right thing to do
                            // as the user could add that entry in the future.
                            continue;
                        }
                    }
                    userToAccessId.put(user, userAccessId);
                }
                if (accessIdToRoles.get(userAccessId) == null) {
                    accessIdToRoles.put(userAccessId, userToRoles.get(user));
                }
                if (isMatch(accessId, userAccessId)) {
                    return accessIdToRoles.get(userAccessId);
                }
            }
        } else if (AccessIdUtil.isGroupAccessId(accessId)) {
            // A group can be configured as a group or group-access-id
            // Ex: group1 or group1:realm/group1
            for (String group : groupToRoles.keySet()) {
                String groupAccessId = groupToAccessId.get(group);
                if (groupAccessId == null) {

                    if (AccessIdUtil.isGroupAccessId(group)) {
                        groupAccessId = group;
                    } else {
                        // group is not accessId format, so we have to look up the user registry.
                        groupAccessId = getGroupAccessId(group);
                        if (groupAccessId == null) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Unable to determine accessId of group " + group);
                            }
                            // We could not determine the access ID here, skip
                            // For all unknown IDs we try to re-compute each time
                            // we see it. This is probably the right thing to do
                            // as the user could add that entry in the future.
                            continue;
                        }
                    }
                    groupToAccessId.put(group, groupAccessId);
                }
                if (accessIdToRoles.get(groupAccessId) == null) {
                    accessIdToRoles.put(groupAccessId, groupToRoles.get(group));
                }
                if (isMatch(accessId, groupAccessId)) {
                    return accessIdToRoles.get(groupAccessId);
                }
            }
        } else {
            // Do nothing
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unknown accessId");
            }
            return RoleSet.EMPTY_ROLESET;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "No roles mapped to accessId", accessId);
        }
        return RoleSet.EMPTY_ROLESET;
    }

    /**
     * Get the access id for a user by performing a looking up in the user registry.
     *
     * @param userName the user for which to create an access id
     * @return the access id of the userName specified,
     *         otherwise null when a registry error occurs or the entry is not found
     */
    private String getUserAccessId(String userName) {

        try {
            SecurityService securityService = securityServiceRef.getService();
            UserRegistryService userRegistryService = securityService.getUserRegistryService();
            UserRegistry userRegistry = userRegistryService.getUserRegistry();
            String realm = userRegistry.getRealm();
            String uniqueId = userRegistry.getUniqueUserId(userName);
            return AccessIdUtil.createAccessId(AccessIdUtil.TYPE_USER, realm, uniqueId);
        } catch (EntryNotFoundException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception getting the access id for "
                             + userName + ": " + e);
            }
        } catch (RegistryException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception getting the access id for "
                             + userName + ": " + e);
            }
        }
        return null;
    }

    /**
     * Get the access id for a group by performing a looking up in the user registry.
     *
     * @param groupName the user for which to create an access id
     * @return the access id of the groupName specified,
     *         otherwise null when a registry error occurs or the entry is not found
     */
    private String getGroupAccessId(String groupName) {
        try {
            SecurityService securityService = securityServiceRef.getService();
            UserRegistryService userRegistryService = securityService.getUserRegistryService();
            UserRegistry userRegistry = userRegistryService.getUserRegistry();
            String realm = userRegistry.getRealm();
            String groupUniqueId = userRegistry.getUniqueGroupId(groupName);
            return AccessIdUtil.createAccessId(AccessIdUtil.TYPE_GROUP, realm, groupUniqueId);
        } catch (EntryNotFoundException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception getting the access id for "
                             + groupName + ": " + e);
            }
        } catch (RegistryException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception getting the access id for "
                             + groupName + ": " + e);
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void notifyOfUserRegistryChange() {
        isIgnoreCaseSet = false;
        accessIdToRoles.clear();
        userToAccessId.clear();
        groupToAccessId.clear();
    }

    protected boolean isIgnoreCase() {
        if (!isIgnoreCaseSet) {
            isIgnoreCase = getIgnoreCase();
            isIgnoreCaseSet = true;
        }
        return isIgnoreCase;
    }

    protected boolean isMatch(String a, String b) {
        boolean match = false;
        if (isIgnoreCase() /* && areLDAPNames(a, b) */) {
            match = a.equalsIgnoreCase(b);
        } else {
            match = a.equals(b);
        }
        return match;
    }

    // in order to decouple the dependency to com.ibm.ws.security.authorization.builtin bundle, the getIgnoreCase method
    // and some other methods are copied from com.ibm.ws.security.authorization.builtin.BaseAuthorizationTableService class.
    protected boolean getIgnoreCase() {
        boolean value = false;
        if (securityServiceRef != null && configAdminRef != null) {
            try {
                SecurityService securityService = securityServiceRef.getService();

                if (securityService != null) {
                    UserRegistryService userRegistryService = securityService.getUserRegistryService();
                    if (userRegistryService != null && userRegistryService.isUserRegistryConfigured()) {
                        // now user registry is available. now check user registry type.e
                        String type = userRegistryService.getUserRegistryType();
                        if ("LDAP".equalsIgnoreCase(type) || "WIM".equalsIgnoreCase(type)) {
                            // now ldap user registry is used. set default value as true.
                            value = true;
                            // now, examine whether ignoreCase attribute is set.
                            ConfigurationAdmin configAdmin = configAdminRef.getService();
                            if (configAdmin != null) {
                                Configuration ldapRegistryConfigs[] = configAdmin.listConfigurations(KEY_LDAP_REGISTRY);
                                if (ldapRegistryConfigs != null) {
                                    for (int i = 0; i < ldapRegistryConfigs.length; i++) {
                                        Dictionary<String, Object> props = ldapRegistryConfigs[i].getProperties();
                                        if (props != null) {
                                            Object ignoreCaseObject = props.get(KEY_IGNORE_CASE);
                                            if (ignoreCaseObject != null) {
                                                if (ignoreCaseObject instanceof Boolean) {
                                                    value = ((Boolean) ignoreCaseObject).booleanValue();
                                                } else if (ignoreCaseObject instanceof String) {
                                                    if ("false".equalsIgnoreCase((String) ignoreCaseObject)) {
                                                        value = false;
                                                    }
                                                }
                                            }
                                        }
                                        if (!value) {
                                            // if value is false, we can break, since if there are multiple ldapRegistry configurations, if there is at least
                                            // one configuration which ignoreCase is set as false, then Authorization code needs to go to case sensitive comparison.
                                            break;
                                        }
                                    }
                                } else {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                        Tr.debug(tc, "The Ldap Configuration object is null, use the default value which is true.");
                                    }
                                }
                            } else {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "The ConfigurationAdmin object is null, use the default value which is true.");
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception is caught while accessing the user registry configuration information. The default value " + value + " is used.", e);
                }
            }
        }
        return value;
    }

    /*
     * if it is a management app, return true. Otherwise, return false.
     * Note: The management apps do not support useRoleAsGroupName for authorization
     */
    @Override
    public boolean isAuthzInfoAvailableForApp(String appName) {
        return (ManagementSecurityConstants.ADMIN_RESOURCE_NAME.equals(appName) == true ? true : false);
    }

}
