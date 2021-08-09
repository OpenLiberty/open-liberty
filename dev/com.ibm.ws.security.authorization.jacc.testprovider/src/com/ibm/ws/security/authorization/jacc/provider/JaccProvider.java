/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/*
 *  This is where the actual authorization decision takes place. The implies
 *  method here will be called to return true (if access is allowed) or false
 *  (if access is not permitted) during the J2EE authorization decisions.
 *
 *  Here is how the implies method makes the access decision.
 *  1) checks excludedList. If the permission is in that list, access is denied.
 *  2) checks uncheckedList.If the permission is in that list, access is granted.
 *  3) gets the required roles for the permission passed.
 *       3a) If the required roles are none, access is granted. else
 *       3b) Gets the authorization table associated with this application.
 *       3c) Gets the roles associated with the Everyone subject. If there are
 *           any and the required roles contain these roles, access is granted.
 *       3d) Uses the policyContext helper class to get the subject invoking the
 *           resource.
 *       3e) If the Subject is null or the cred is unauthenticated, access is
 *           denied.
 *       3f) Gets the roles associated with the AllAuthenticated subject. If
 *           there are any and if the required roles contain anf of these roles,
 *           access is granted.
 *       3g) Gets the granted roles for the Subject(first users and then groups)
 *       3h) if any of the granted roles belong to the required role list, access
 *           is granted, else access is denied.
 *
 *  The policy provider will then use this information to make the access
 *  decisions during the authorization checks.
 *
 */

package com.ibm.ws.security.authorization.jacc.provider;

import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialExpiredException;
import javax.security.jacc.EJBRoleRefPermission;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.security.jacc.WebResourcePermission;
import javax.security.jacc.WebRoleRefPermission;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.CredentialDestroyedException;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authorization.jacc.role.FileRoleMapping;

public class JaccProvider {
    private static final TraceComponent tc = Tr.register(JaccProvider.class);
    private static JaccProvider jaccProvider;
    private static boolean initialized = false;
    private static boolean ignoreCase = false;
    private static FileRoleMapping roleMapping = null;
    private static final String ALL_AUTHENTICATED_ROLE = "**";

    public static JaccProvider getInstance() {
        if (!initialized) {
            jaccProvider = new JaccProvider();
            roleMapping = FileRoleMapping.getInstance();
            initialized = true;
        }
        return jaccProvider;
    }

    private JaccProvider() {}

    /**
     ** check for Unchecked permissions
     */
    public boolean checkUncheckedPerm(WSPolicyConfigurationImpl pc, Permission p) {
        if (isUnProtectedResource(pc))
            return true;
        List<Permission> uncheckedList = pc.getUncheckedList();
        if (uncheckedList != null) {
            for (Permission unchkPerm : uncheckedList) {
                if (unchkPerm.implies(p)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     ** check for Excluded permissions
     */
    public boolean checkExcludedPerm(WSPolicyConfigurationImpl pc, Permission p) {
        List<Permission> excludedList = pc.getExcludedList();
        if (excludedList != null) {
            for (Permission excludedPerm : excludedList) {
                if (excludedPerm.implies(p)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     ** check for Everyone role permissions
     */
    public boolean isEveryoneGranted(WSPolicyConfigurationImpl pc, Permission p, String contextId) {
        //let's get the required roles.
        List<String> requiredRoleList = getRequiredRoleList(pc.getRoleToPermMap(), p);

        if (requiredRoleList != null && requiredRoleList.size() > 0) {
            // get the authorization table.
            String appName = getAppName(contextId);

            if (tc.isDebugEnabled())
                Tr.debug(tc, "application name is: " + appName);

            if (roleMapping == null) {
                Tr.error(tc, "roleMappingTable is null. Authorization cannot complete.");
                return false;
            }
            List<String> roles = roleMapping.getRolesForSpecialSubject(appName, FileRoleMapping.EVERYONE);
            if (roles != null) {
                for (String role : requiredRoleList) {
                    if (roles.contains(role)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Everyone is granted access.");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     ** check for role permissions
     */
    public boolean checkRolePerm(WSPolicyConfigurationImpl pc, Permission p, String contextId) {
        if (isUnProtectedResource(pc))
            return true;
        //let's get the required roles.
        List<String> requiredRoleList = getRequiredRoleList(pc.getRoleToPermMap(), p);

        // If native JACC provider will be used eventually, the following
        // logic should be changed/moved to improve performance.
        if (!(p instanceof WebRoleRefPermission || p instanceof EJBRoleRefPermission)) { //do not check for RoleRef's
            if (requiredRoleList.size() == 0) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "No required roles. exit value:true");
                return true;
            }
        }
        if (roleMapping == null) {
            Tr.error(tc, "Role Mapping table is null. Authorization cannot complete.");
            return false;
        }

        //format of contextID : localhost#C:/WASLiberty/oidc10/wlp/usr/#jacc#snoop#snoop
        String appName = getAppName(contextId);

        if (tc.isDebugEnabled())
            Tr.debug(tc, "application name is: " + appName);

        if (!(p instanceof WebResourcePermission)) { //already checked
            List<String> roles = roleMapping.getRolesForSpecialSubject(appName, FileRoleMapping.EVERYONE);
            if (roles != null) {
                for (String role : requiredRoleList) {
                    if (roles.contains(role)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Everyone is granted access.");
                        return true;
                    }
                }
            }
        }
        Object obj = null;
        try {
            obj = PolicyContext.getContext("javax.security.auth.Subject.container");
        } catch (PolicyContextException pce) {
            Tr.error(tc, "the JACC provider cannot access javax.security.auth.Subject.container : " + pce.getMessage());
            obj = null;
        }
        if (obj == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Cannot get the subject from the policy context.");
            return false;
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Object returned from the policy context is: " + obj.getClass());
        javax.security.auth.Subject subject = null;
        if (obj instanceof javax.security.auth.Subject) {
            subject = (javax.security.auth.Subject) obj;
        }
        WSCredential cred = getWSCredentialFromSubject(subject);
        if ((subject == null) || cred.isUnauthenticated()) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Subject is null or Unauthenticated.");
            return false;
        }
        // The tWAS code handles basic auth credential here. Since Liberty doesn't use basicAuth (even it's defined),
        // skip this part. Note that the tWAS code create a real Subject by logging in by using basic auth cred.

        // check for "**" (all authenticated)
        for (String role : requiredRoleList) {
            if (ALL_AUTHENTICATED_ROLE.equals(role)) {
                Tr.debug(tc, "granted access to the authenticated user (**).");
                return true;
            }
        }

        // check for all authenticated users
        List<String> roles = roleMapping.getRolesForSpecialSubject(appName, FileRoleMapping.ALL_AUTHENTICATED_USERS);
        if (roles != null && !roles.isEmpty()) {
            for (String role : requiredRoleList) {
                if (roles.contains(role)) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "granted access to the authenticated user.");
                    return true;
                }
            }
        }
        //now check for specific user
        String accessId = getAccessId(cred);
        roles = roleMapping.getRolesForUser(appName, accessId);
        if (roles != null && !roles.isEmpty()) {
            for (String role : requiredRoleList) {
                if (ALL_AUTHENTICATED_ROLE.equals(role) || roles.contains(role)) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "granted access for the user. Role matches.");
                    return true;
                }
            }
        }
        roles = null;
        List<String> groupIds = getGroupIds(cred);
        if (groupIds != null && !groupIds.isEmpty()) {
            for (String groupId : groupIds) {
                roles = roleMapping.getRolesForGroup(appName, groupId);
                if (roles != null && !roles.isEmpty()) {
                    for (String role : requiredRoleList) {
                        if (ALL_AUTHENTICATED_ROLE.equals(role) || roles.contains(role)) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "granted access for group. Role matches");
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private List<String> getRequiredRoleList(Map<String, List<Permission>> roleToPermMap, Permission p) {
        List<String> requiredRoleList = null;
        if (!roleToPermMap.isEmpty()) {
            requiredRoleList = new ArrayList<String>();
            for (Entry<String, List<Permission>> e : roleToPermMap.entrySet()) {
                List<Permission> rList = e.getValue();
                for (Permission perm : rList) {
                    if (perm.implies(p)) {
                        String role = e.getKey();
                        requiredRoleList.add(role);
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Added role: " + role + " to the requiredRoleList for Permission: " + p + " granted by : " + perm);
                    }
                }
            }
        }
        return requiredRoleList;
    }

    private WSCredential getWSCredentialFromSubject(Subject subject) {
        if (subject != null) {
            java.util.Collection<Object> publicCreds = subject.getPublicCredentials();

            if (publicCreds != null && publicCreds.size() > 0) {
                java.util.Iterator<Object> publicCredIterator = publicCreds.iterator();
                while (publicCredIterator.hasNext()) {
                    Object cred = publicCredIterator.next();
                    if (cred instanceof WSCredential) {
                        return (WSCredential) cred;
                    }
                }
            }
        }
        return null;
    }

    private String getAccessId(WSCredential cred) {
        String accessId = null;
        try {
            accessId = cred.getAccessId();
        } catch (CredentialExpiredException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Caught exception getting the access id: " + e);
        } catch (CredentialDestroyedException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Caught exception getting the access id: " + e);
        }
        if (accessId != null && accessId.length() > 0) {
            if (ignoreCase) {
                accessId = accessId.toLowerCase();
            }
        }
        return accessId;
    }

    @SuppressWarnings("unchecked")
    private List<String> getGroupIds(WSCredential cred) {
        Collection<String> ids = null;
        try {
            ids = cred.getGroupIds();
        } catch (CredentialExpiredException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Caught exception getting the group access ids: " + e);
        } catch (CredentialDestroyedException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Caught exception getting the group access ids: " + e);
        }
        List<String> groupIds = null;
        if (ids != null) {
            if (ignoreCase) {
                groupIds = new ArrayList<String>();
                for (String id : ids) {
                    groupIds.add(id.toLowerCase());
                }
            } else {
                groupIds = new ArrayList<String>(ids);
            }
        }
        return groupIds;
    }

    private String getAppName(String contextId) {
        int end = contextId.lastIndexOf("#");
        int begin = contextId.lastIndexOf("#", end - 1);
        String appName = null;
        if (begin != -1 && end != -1) {
            appName = contextId.substring(begin + 1, end);
        }
        return appName;
    }

    private boolean isUnProtectedResource(WSPolicyConfigurationImpl pc) {
        if ((pc.getExcludedList() == null || !pc.getExcludedList().isEmpty()) && (pc.getUncheckedList() == null || pc.getUncheckedList().isEmpty())
            && (pc.getRoleToPermMap() == null || pc.getRoleToPermMap().isEmpty())) {
            return true;
        } else
            return false;
    }
}
