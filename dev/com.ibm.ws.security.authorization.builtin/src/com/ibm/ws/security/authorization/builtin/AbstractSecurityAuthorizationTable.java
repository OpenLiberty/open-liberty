/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.builtin;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authorization.AuthorizationTableConfigService;
import com.ibm.ws.security.authorization.AuthorizationTableService;
import com.ibm.ws.security.authorization.RoleSet;
import com.ibm.ws.security.authorization.SecurityRole;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryChangeListener;
import com.ibm.ws.security.registry.UserRegistryService;

/**
 * Subclasses must implement ConfigurationListener as the modified actions appear to be different.
 */
public abstract class AbstractSecurityAuthorizationTable extends BaseAuthorizationTableService implements AuthorizationTableService, UserRegistryChangeListener, AuthorizationTableConfigService, ConfigurationListener {
    private static final TraceComponent tc = Tr.register(AbstractSecurityAuthorizationTable.class);

    public static final String DEFAULT_ROLE_ELEMENT_NAME = "security-role";

    static final String CFG_KEY_ID = "id";
    static final String CFG_KEY_REALM = "realm";
    static final String CFG_KEY_USER = "user";
    static final String CFG_KEY_GROUP = "group";
    static final String CFG_KEY_SPECIAL_SUBJECT = "special-subject";
    static final String CFG_KEY_MEMBER = "member";
    static final String CFG_KEY_NAME = "name";
    static final String CFG_KEY_PASSWORD = "password";

    protected final Set<String> pids = new HashSet<String>();

    private final Map<String, RoleSet> specialSubjectToRoles = new HashMap<String, RoleSet>();
    private final Map<String, String> userToAccessId = new HashMap<String, String>();
    private final Map<String, String> groupToAccessId = new HashMap<String, String>();
    private final Map<String, RoleSet> userToRoles = new HashMap<String, RoleSet>();
    private final Map<String, RoleSet> groupToRoles = new HashMap<String, RoleSet>();
    private final Map<String, RoleSet> accessIdToRoles = new HashMap<String, RoleSet>();
    private final Map<String, Set<String>> explicitAccessIdToRoles = new HashMap<String, Set<String>>();
    protected String roleElementName = getRoleElementName();
    protected boolean populated = false;
    protected Set<SecurityRole> roles = new HashSet<SecurityRole>();

    @Override
    protected synchronized void activate(ComponentContext cc) {
        super.activate(cc);
    }

    @Override
    protected synchronized void deactivate(ComponentContext cc) {
        super.deactivate(cc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoleSet getRolesForSpecialSubject(String appName, String specialSubject) {
        if (!populated) {
            return null;
        }
        if (getApplicationName().equals(appName)) {
            return specialSubjectToRoles.get(specialSubject);
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public RoleSet getRolesForAccessId(String appName, String accessId) {
        if (!populated) {
            return null;
        }
        if (getApplicationName().equals(appName)) {
            return rolesForAccessId(accessId);
        } else {
            return null;
        }
    }

    /**
     * Clear the authorization table.
     */
    private void clearAuthorizationTable() {
        userToAccessId.clear();
        groupToAccessId.clear();
        userToRoles.clear();
        groupToRoles.clear();
        specialSubjectToRoles.clear();
        accessIdToRoles.clear();
        explicitAccessIdToRoles.clear();
    }

    /**
     * Construct the authorization table for the given security roles.
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
    protected void populate() {
        clearAuthorizationTable();

        Map<String, Set<String>> userToRoleName = new HashMap<String, Set<String>>();
        Map<String, Set<String>> groupToRoleName = new HashMap<String, Set<String>>();
        Map<String, Set<String>> specialSubjectToRoleName = new HashMap<String, Set<String>>();
        Iterator<SecurityRole> itr = getRoles();
        while (itr.hasNext()) {
            SecurityRole role = itr.next();
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
            for (String specialSubject : role.getSpecialSubjects()) {
                Set<String> assignedRoles = specialSubjectToRoleName.get(specialSubject);
                if (assignedRoles == null) {
                    assignedRoles = new HashSet<String>();
                    specialSubjectToRoleName.put(specialSubject, assignedRoles);
                }
                assignedRoles.add(roleName);
            }
            for (String accessId : role.getAccessIds()) {
                Set<String> assignedRoles = getRoles(explicitAccessIdToRoles, accessId);
                if (assignedRoles == null) {
                    assignedRoles = new HashSet<String>();
                    explicitAccessIdToRoles.put(accessId, assignedRoles);
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
        for (Map.Entry<String, Set<String>> entry : specialSubjectToRoleName.entrySet()) {
            specialSubjectToRoles.put(entry.getKey(), new RoleSet(entry.getValue()));
        }
        populated = true;
    }

    /**
     * Retrieve the roles, if any, for the given accessId.
     *
     * @param accessId
     * @return a non-null RoleSet
     */
    private RoleSet rolesForAccessId(String accessId) {
        RoleSet roles = getRoleSet(accessIdToRoles, accessId);
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
            // First look for any <user> WITHOUT an explicit access id
            for (String user : userToRoles.keySet()) {
                String userAccessId = userToAccessId.get(user);
                if (userAccessId == null) {
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
                    userToAccessId.put(user, userAccessId);
                }
                if (getRoleSet(accessIdToRoles, userAccessId) == null) {
                    accessIdToRoles.put(userAccessId, new RoleSet(userToRoles.get(user), getRoles(explicitAccessIdToRoles, userAccessId)));
                }
                if (isMatch(accessId, userAccessId)) {
                    return getRoleSet(accessIdToRoles, userAccessId);
                }
            }
            // This access id was not for any <user> WITHOUT an explicit access id
            // See if there was a <user> with this explicit access id specified
            Set<String> explicitRoleSet = getRoles(explicitAccessIdToRoles, accessId);
            if (explicitRoleSet != null) {
                RoleSet set = new RoleSet(explicitRoleSet);
                accessIdToRoles.put(accessId, set);
                return set;
            }
        } else if (AccessIdUtil.isGroupAccessId(accessId)) {
            // First look for any <group> WITHOUT an explicit access id
            for (String group : groupToRoles.keySet()) {
                String groupAccessId = groupToAccessId.get(group);
                if (groupAccessId == null) {
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
                    groupToAccessId.put(group, groupAccessId);
                }
                if (getRoleSet(accessIdToRoles, groupAccessId) == null) {
                    accessIdToRoles.put(groupAccessId, new RoleSet(groupToRoles.get(group), getRoles(explicitAccessIdToRoles, groupAccessId)));
                }
                if (isMatch(accessId, groupAccessId)) {
                    return getRoleSet(accessIdToRoles, groupAccessId);
                }
            }
            // This access id was not for any <group> WITHOUT an explicit access id
            // See if there was a <group> with this explicit access id specified
            Set<String> explicitRoleSet = getRoles(explicitAccessIdToRoles, accessId);
            if (explicitRoleSet != null) {
                RoleSet set = new RoleSet(explicitRoleSet);
                accessIdToRoles.put(accessId, set);
                return set;
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
        super.notifyOfUserRegistryChange();
        accessIdToRoles.clear();
        userToAccessId.clear();
        groupToAccessId.clear();
    }

    /**
     * Return the name of the application that this authz table instance is for
     *
     * @return app name
     */
    abstract protected String getApplicationName();

    /**
     * Return the name of the xml element that defines a security role.
     * The default element is <security-role...>
     *
     * @return element name
     */
    protected String getRoleElementName() {
        return DEFAULT_ROLE_ELEMENT_NAME;
    }

    /**
     * Return the iterator for the collection of security roles for the application
     *
     * @return security roles iterator
     */
    protected Iterator<SecurityRole> getRoles() {
        return roles.iterator();
    }

    public void setFeatureRoleConfiguration(String[] rolePids, ConfigurationAdmin configAdmin) {
        roles.clear();
        pids.clear();
        Set<String> badConfigs = new HashSet<String>();
        Dictionary<String, Object> roleProps = null;
        String roleName = null;
        if (rolePids != null) {
            for (String rolePid : rolePids) {
                pids.add(rolePid);
                try {
                    Configuration config = configAdmin.getConfiguration(rolePid, bundleLocation);
                    roleProps = config.getProperties();
                    roleName = (String) roleProps.get(CFG_KEY_NAME);
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Role name " + roleName);
                    }
                } catch (IOException ioe) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Invalid role definition", new Object[] { rolePid, ioe.getMessage() });
                    }
                    continue;
                }
                SecurityRole role = new SecurityRoleImpl(configAdmin, bundleLocation, roleName, roleProps, pids);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Adding role", role);
                }
                if (!roles.add(role)) {
                    badConfigs.add(roleName);
                    Tr.error(tc, "AUTHZ_TABLE_DUPLICATE_ROLE_DEFINITION", role);
                    roles.remove(role);
                }
            }
        }
        populate();
    }

    @Override
    public void setConfiguration(String[] roleNames,
                                 ConfigurationAdmin configAdmin,
                                 Map<String, Object> properties) {
        // read the config, create a SecurityRoleImpl for each role, add it to the roles set
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Auth table configuration", properties);

        pids.clear();
        for (String roleName : roleNames) {
            processRole(roleName, configAdmin, properties);
        }
        populate();
    }

    private void processRole(String roleName,
                             ConfigurationAdmin configAdmin,
                             Map<String, Object> properties) {
        Set<String> badConfigs = new HashSet<String>();
        String[] roleDefs = (String[]) properties.get(roleName);
        if (roleDefs == null || roleDefs.length == 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No roles were defined with element: " + roleName);
            }
            return;
        }
        if (roleDefs.length > 1) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Multiple roles were defined with element: " + roleName + ". Using 1st definition");
            }
        }
        Configuration config = null;
        pids.add(roleDefs[0]);
        try {
            config = configAdmin.getConfiguration(roleDefs[0], bundleLocation);
        } catch (IOException ioe) {
            Tr.error(tc, "AUTHZ_TABLE_INVALID_ROLE_DEFINITION", roleDefs[0]);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Invalid role definition", new Object[] { roleDefs[0], ioe.getMessage() });
            }
            return;
        }
        if (config == null || config.getProperties() == null) {
            Tr.error(tc, "AUTHZ_TABLE_INVALID_ROLE_DEFINITION", roleDefs[0]);
            return;
        }

        SecurityRole role = new SecurityRoleImpl(configAdmin, bundleLocation, roleName, config.getProperties(), pids);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Adding role", role);
        }
        if (!roles.add(role)) {
            badConfigs.add(roleName);
            Tr.error(tc, "AUTHZ_TABLE_DUPLICATE_ROLE_DEFINITION", roleName);
            roles.remove(role);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.authorization.AuthorizationTableService#isAuthzInfoAvailableForApp(java.lang.String)
     */
    @Override
    public boolean isAuthzInfoAvailableForApp(String appName) {
        if (roles != null && !roles.isEmpty())
            return true;
        else
            return false;
    }
}
