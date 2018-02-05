/*******************************************************************************
 * Copyright (c) 2011, 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.builtin.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialExpiredException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.CredentialDestroyedException;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.authorization.AccessDecisionService;
import com.ibm.ws.security.authorization.AuthorizationService;
import com.ibm.ws.security.authorization.AuthorizationTableService;
import com.ibm.ws.security.authorization.FeatureAuthorizationTableService;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;

/**
 * Built-in authorization service which implements a role-based authorization
 * model.
 */
public class BuiltinAuthorizationService implements AuthorizationService {
    private static final TraceComponent tc = Tr.register(BuiltinAuthorizationService.class);

    protected static final String KEY_ACCESS_DECISION_SERVICE = "accessDecisionService";
    protected static final String KEY_AUTHORIZATION_TABLE_SERVICE = "authorizationTableService";
    protected static final String KEY_USE_ROLE_AS_GROUP_NAME = "useRoleAsGroupName";
    private final AtomicServiceReference<AccessDecisionService> accessDecisionServiceRef = new AtomicServiceReference<AccessDecisionService>(KEY_ACCESS_DECISION_SERVICE);
    private final ConcurrentServiceReferenceSet<AuthorizationTableService> authorizationTables = new ConcurrentServiceReferenceSet<AuthorizationTableService>(KEY_AUTHORIZATION_TABLE_SERVICE);
    private final SubjectManager subjManager = new SubjectManager();
    static final String KEY_FEATURE_SECURITY_AUTHZ_SERVICE = "featureAuthzTableService";
    private final AtomicServiceReference<FeatureAuthorizationTableService> featureAuthzTableServiceRef = new AtomicServiceReference<FeatureAuthorizationTableService>(KEY_FEATURE_SECURITY_AUTHZ_SERVICE);
    protected ConcurrentServiceReferenceMap<String, AuthorizationTableService> appBndAuthorizations = new ConcurrentServiceReferenceMap<String, AuthorizationTableService>(KEY_AUTHORIZATION_TABLE_SERVICE);

    private boolean useRoleAsGroupName = false;

    private static final String MGMT_AUTHZ_ROLES = "com.ibm.ws.management";

    static final String KEY_COMPONENT_NAME = "component.name";
    static final String KEY_FEATURE_AUTHORIZATION_TABLE = "com.ibm.ws.webcontainer.security.feature.internal.FeatureAuthorizationTable";
    static final String KEY_MANAGMENT_AUTHORIZATION_TABLE = "com.ibm.ws.management.security.authorizationTable";
    //    private static final String ADMIN_RESOURCE_NAME = "com.ibm.ws.management.security.resource";
    private final List<String> useRoleAsGroupNameForApps = new ArrayList<String>();

    protected void setAccessDecisionService(ServiceReference<AccessDecisionService> ref) {
        accessDecisionServiceRef.setReference(ref);
    }

    protected void unsetAccessDecisionService(ServiceReference<AccessDecisionService> ref) {
        accessDecisionServiceRef.unsetReference(ref);
    }

    protected void setAuthorizationTableService(ServiceReference<AuthorizationTableService> ref) {
        authorizationTables.addReference(ref);
        String cn = (String) ref.getProperty(KEY_COMPONENT_NAME);
        if (cn != null && !cn.equals(KEY_FEATURE_AUTHORIZATION_TABLE) && !cn.equals(KEY_MANAGMENT_AUTHORIZATION_TABLE)) {
            appBndAuthorizations.putReference(cn, ref);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.event(tc, "appBndAuthorizationTable service: " + ref);
            }
        }
    }

    protected void unsetAuthorizationTableService(ServiceReference<AuthorizationTableService> ref) {
        authorizationTables.removeReference(ref);
        String cn = (String) ref.getProperty(KEY_COMPONENT_NAME);
        if (cn != null && !cn.equals(KEY_FEATURE_AUTHORIZATION_TABLE) && !cn.equals(KEY_MANAGMENT_AUTHORIZATION_TABLE)) {
            appBndAuthorizations.removeReference(cn, ref);
        }
    }

    protected void setFeatureAuthzTableService(ServiceReference<FeatureAuthorizationTableService> ref) {
        featureAuthzTableServiceRef.setReference(ref);
    }

    protected void unsetFeatureAuthzTableService(ServiceReference<FeatureAuthorizationTableService> ref) {
        featureAuthzTableServiceRef.unsetReference(ref);
    }

    protected void activate(ComponentContext cc, Map<String, Object> properties) {
        accessDecisionServiceRef.activate(cc);
        authorizationTables.activate(cc);
        featureAuthzTableServiceRef.activate(cc);
        if (properties != null && properties.containsKey(KEY_USE_ROLE_AS_GROUP_NAME))
            useRoleAsGroupName = (Boolean) properties.get(KEY_USE_ROLE_AS_GROUP_NAME);
    }

    protected void deactivate(ComponentContext cc) {
        accessDecisionServiceRef.deactivate(cc);
        authorizationTables.deactivate(cc);
        featureAuthzTableServiceRef.deactivate(cc);
        useRoleAsGroupNameForApps.clear();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAuthorized(String resourceName,
                                Collection<String> requiredRoles, final Subject inSubject) {
        validateInput(resourceName, requiredRoles);

        Subject subject = inSubject;
        if (subject == null) {
            subject = subjManager.getCallerSubject();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Determining if Subject is authorized to access resource " + resourceName + ". Specified required roles are " + requiredRoles + ".", subject);
        }

        if (requiredRoles.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Subject is authorized to access resource " + resourceName + " as there are no required roles.", subject);
            }
            return true;
        }

        // first, check if Everyone is granted
        if (isEveryoneGranted(resourceName, requiredRoles)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Subject is authorized to access resource " + resourceName + " as everyone is authorized.", subject);
            }
            return true;
        }

        // make sure subject is valid and authenticated
        if (!isSubjectValid(subject)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Subject is NOT authorized to access resource " + resourceName + " as the subject is not valid.", subject);
            }
            return false;
        } else {
            // second, check if AllAuthenticated is granted
            if (isAllAuthenticatedGranted(resourceName, requiredRoles, subject)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Subject is authorized to access resource " + resourceName + " as all authenticated users are authorized.", subject);
                }
                return true;
            }

            // finally, check if the passed-in subject is authorized
            boolean authorized = isSubjectAuthorized(resourceName, requiredRoles, subject);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                if (authorized) {
                    Tr.event(tc, "Subject is authorized to access resource " + resourceName + " as the Subject possesses one of the required roles.", subject);
                } else {
                    List<String> rolesList = new ArrayList<String>();
                    Iterator<String> iter = requiredRoles.iterator();
                    while (iter.hasNext()) {
                        rolesList.add(iter.next());
                    }
                    Tr.event(tc, "Subject is NOT authorized to access resource " + resourceName + " as the Subject does not possess one of the required roles: " + rolesList,
                             subject);
                }
            }
            return authorized;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEveryoneGranted(String resourceName, Collection<String> requiredRoles) {
        validateInput(resourceName, requiredRoles);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Determining if Everyone is authorized to access resource " + resourceName + ". Specified required roles are " + requiredRoles + ".");
        }

        if (requiredRoles.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Everyone is granted access to resource " + resourceName + " as there are no required roles.");
            }
            return true;
        }

        Collection<String> roles = getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);

        AccessDecisionService accessDecisionService = accessDecisionServiceRef.getService();
        boolean granted = accessDecisionService.isGranted(resourceName, requiredRoles, roles, null);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            if (granted) {
                Tr.event(tc, "Everyone is granted access to resource " + resourceName + ".");
            } else {
                Tr.event(tc, "Everyone is NOT granted access to resource " + resourceName + ".");
            }
        }
        return granted;
    }

    /**
     * Check all of the authorization table services for the resourceName.
     * If no authorization table can be found for the resourceName, null
     * is returned. If more than one authorization table can be found for
     * the resourceName, null is returned.
     *
     * @param resourceName
     * @param specialSubject
     * @return
     */
    private Collection<String> getRolesForSpecialSubject(String resourceName, String specialSubject) {
        int found = 0;
        Collection<String> roles = null;
        FeatureAuthorizationTableService featureAuthzTableSvc = featureAuthzTableServiceRef.getService();
        String featureAuthzRoleHeaderValue = null;
        if (featureAuthzTableSvc != null) {
            featureAuthzRoleHeaderValue = featureAuthzTableSvc.getFeatureAuthzRoleHeaderValue();
        }
        if (featureAuthzRoleHeaderValue != null &&
            !featureAuthzRoleHeaderValue.equals(MGMT_AUTHZ_ROLES)) {
            roles = featureAuthzTableSvc.getRolesForSpecialSubject(resourceName, specialSubject);
        } else {
            Iterator<AuthorizationTableService> itr = authorizationTables.getServices();
            while (itr.hasNext()) {
                AuthorizationTableService authzTableSvc = itr.next();
                Collection<String> rolesFound = authzTableSvc.getRolesForSpecialSubject(resourceName, specialSubject);
                if (rolesFound != null) {
                    roles = rolesFound;
                    found++;
                }
            }
            // We must find one, and only one, Collection of roles
            if (found > 1) {
                Tr.error(tc, "AUTHZ_MULTIPLE_RESOURCES_WITH_SAME_NAME", resourceName);
                roles = null;
            }
        }
        return roles;
    }

    /**
     * Check all of the authorization table services for the resourceName.
     * If no authorization table can be found for the resourceName, null
     * is returned. If more than one authorization table can be found for
     * the resourceName, null is returned.
     *
     * @param resourceName
     * @param accessId
     * @return
     */
    private Collection<String> getRolesForAccessId(String resourceName, String accessId, String realmName) {
        int found = 0;
        Collection<String> roles = null;
        FeatureAuthorizationTableService featureAuthzTableSvc = featureAuthzTableServiceRef.getService();
        String featureAuthzRoleHeaderValue = null;
        if (featureAuthzTableSvc != null) {
            featureAuthzRoleHeaderValue = featureAuthzTableSvc.getFeatureAuthzRoleHeaderValue();
        }
        if (featureAuthzRoleHeaderValue != null &&
            !featureAuthzRoleHeaderValue.equals(MGMT_AUTHZ_ROLES)) {
            roles = featureAuthzTableSvc.getRolesForAccessId(resourceName, accessId, realmName);
        } else {
            Iterator<AuthorizationTableService> itr = authorizationTables.getServices();
            while (itr.hasNext()) {
                AuthorizationTableService authzTableSvc = itr.next();
                Collection<String> rolesFound = authzTableSvc.getRolesForAccessId(resourceName, accessId, realmName);
                if (rolesFound != null) {
                    roles = rolesFound;
                    found++;
                }
            }
            // We must find one, and only one, Collection of roles
            if (found > 1) {
                Tr.error(tc, "AUTHZ_MULTIPLE_RESOURCES_WITH_SAME_NAME", resourceName);
                roles = null;
            }
        }
        return roles;
    }

    private boolean isAuthzInfoAvailableForApp(String resourceName) {
        boolean found = false;
//        FeatureAuthorizationTableService featureAuthzTableSvc = featureAuthzTableServiceRef.getService();
//        if (featureAuthzTableSvc != null) {
//            found = featureAuthzTableSvc.isAuthzInfoAvailableForApp(resourceName);
//        }
//
//        if (!found) {
//            Iterator<AuthorizationTableService> itr = authorizationTables.getServices();
//            while (itr.hasNext() && !found) {
//                AuthorizationTableService authzTableSvc = itr.next()
//                found = authzTableSvc.isAuthzInfoAvailableForApp(resourceName);
//            }
//        }
        Iterator<AuthorizationTableService> itr = appBndAuthorizations.getServices();
        while (itr.hasNext() && !found) {
            AuthorizationTableService authzTableSvc = itr.next();
            found = authzTableSvc.isAuthzInfoAvailableForApp(resourceName);
        }
        return found;
    }

    /**
     * Check if the Subject is authorized to the required roles for a given
     * resource. The user is checked first, and if it's not authorized, then
     * each group is checked.
     *
     * @param resourceName
     *            the name of the application, used for looking up the correct
     *            authorization table
     * @param requiredRoles
     *            the roles required to access the resource
     * @param subject
     *            the subject to authorize
     * @return true if the subject is authorized, otherwise false
     */
    private boolean isSubjectAuthorized(String resourceName,
                                        Collection<String> requiredRoles, Subject subject) {

        AccessDecisionService accessDecisionService = accessDecisionServiceRef.getService();

        // check user access first
        boolean isGranted = false;
        WSCredential wsCred = getWSCredentialFromSubject(subject);
        String accessId = getAccessId(wsCred);

//        if (useRoleAsGroupName && !isAuthzInfoAvailableForApp(resourceName) && !resourceName.equalsIgnoreCase(ADMIN_RESOURCE_NAME)) {
        if (useRoleAsGroupName && !isAuthzInfoAvailableForApp(resourceName)) {
            isGranted = useRoleAsGroupNameForAccessDecision(resourceName, requiredRoles, subject, accessDecisionService, wsCred);
        } else {
            isGranted = useAppBndForAccessDecision(resourceName, requiredRoles, subject, accessDecisionService, wsCred, accessId);
        }

        return isGranted;
    }

    /**
     * @param resourceName
     * @param requiredRoles
     * @param subject
     * @param accessDecisionService
     * @param wsCred
     * @param accessId
     * @return
     */
    private boolean useAppBndForAccessDecision(String resourceName, Collection<String> requiredRoles, Subject subject, AccessDecisionService accessDecisionService,
                                               WSCredential wsCred, String accessId) {
        String realmName = getRealmName(wsCred);
        Collection<String> userRoles = getRolesForAccessId(resourceName, accessId, realmName);

        // check user access
        boolean isGranted = accessDecisionService.isGranted(resourceName, requiredRoles, userRoles, subject);

        // check group access
        if (!isGranted) {
            String[] groupIds = getGroupIds(wsCred);
            if (groupIds != null && groupIds.length > 0) {
                for (int i = 0; i < groupIds.length && !isGranted; i++) {
                    String groupId = groupIds[i];
                    Collection<String> assignedRoles = getRolesForAccessId(resourceName, groupId, realmName);
                    if (assignedRoles != null) {
                        isGranted = accessDecisionService.isGranted(resourceName, requiredRoles, assignedRoles, subject);
                    }
                }
            }
        }

        if (useRoleAsGroupNameForApps.contains(resourceName)) {
            useRoleAsGroupNameForApps.remove(resourceName);
        }

        return isGranted;
    }

    /**
     * @param resourceName
     * @param requiredRoles
     * @param subject
     * @param accessDecisionService
     * @param isGranted
     * @param wsCred
     * @return
     */
    private boolean useRoleAsGroupNameForAccessDecision(String resourceName, Collection<String> requiredRoles, Subject subject, AccessDecisionService accessDecisionService,
                                                        WSCredential wsCred) {
        boolean isGranted = false;
        String[] groupIds = getGroupIds(wsCred);
        String realmName = getRealmName(wsCred);
        if (groupIds != null && groupIds.length > 0) {
            Collection<String> assignedRoles = new ArrayList<String>();
            // Just include the group name and not the id
            for (int i = 0; i < groupIds.length; i++) {
                assignedRoles.add(AccessIdUtil.getUniqueId(groupIds[i], realmName));
            }
            isGranted = accessDecisionService.isGranted(resourceName, requiredRoles, assignedRoles, subject);
        }
        //Keep track the app that use role as group name for authorization decision
        if (!useRoleAsGroupNameForApps.contains(resourceName)) {
            Tr.info(tc, "AUTHZ_BASED_ON_ROLE_NAME_SAME_AS_GROUP_NAME", resourceName);
            useRoleAsGroupNameForApps.add(resourceName);
        }

        return isGranted;
    }

    /**
     * Get the WSCredential from the given Subject
     *
     * @param subject
     *            the subject to parse, must not be null
     * @return the WSCredential, or null if the Subject does not have one
     */
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

    /**
     * Get the access ID from the specified credential.
     *
     * @parm cred the WSCredential to search
     * @return the user access id of the credential, or null when the
     *         cred is expired or destroyed
     */
    private String getAccessId(WSCredential cred) {
        String accessId = null;
        try {
            accessId = cred.getAccessId();
        } catch (CredentialExpiredException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception getting the access id: " + e);
            }
        } catch (CredentialDestroyedException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception getting the access id: " + e);
            }
        }
        // TODO if ignoreCase is Collection, then lower case the accessid
        return accessId;
    }

    /**
     * Get the group IDs from the specified credential.
     *
     * @param cred
     *            the WSCredential to search, must not be null
     * @return an array of group access ids of the credential, or null when the
     *         cred is expired or destroyed
     */
    @SuppressWarnings("unchecked")
    private String[] getGroupIds(WSCredential cred) {
        Collection<String> ids = null;
        if (cred != null) {
            try {
                ids = cred.getGroupIds();
            } catch (CredentialExpiredException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught exception getting the group access ids: " + e);
                }
            } catch (CredentialDestroyedException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught exception getting the group access ids: " + e);
                }
            }
            if (ids != null) {
                return ids.toArray(new String[ids.size()]);
            }
        }
        return null;
        // TODO if ignoreCase is Collection, then lower case the groupids
    }

    /**
     * Validate that the input parameters are not null.
     *
     * @param resourceName
     *            the name of the resource
     * @param requiredRoles
     *            the Collection of required roles
     * @throws NullPointerException
     *             when either input is null
     */
    private void validateInput(String resourceName, Collection<String> requiredRoles) {
        if (requiredRoles == null) {
            throw new NullPointerException("requiredRoles cannot be null.");
        } else if (resourceName == null) {
            throw new NullPointerException("resourceName cannot be null.");
        }
    }

    /**
     * Check if the special subject ALL_AUTHENTICATED_USERS is mapped to the
     * requiredRole.
     *
     * @param resourceName
     *            the name of the resource being accessed, used to look up
     *            corresponding the authorization table, must not be null
     * @param requiredRoles
     *            the security constraints required to be authorized, must not
     *            be null or empty
     * @param subject
     *            the user who is trying to access the resource
     *
     * @throws NullPointerException
     *             when resourceName or requiredRoles is null
     */
    protected boolean isAllAuthenticatedGranted(String resourceName,
                                                Collection<String> requiredRoles,
                                                Subject subject) {
        Collection<String> roles = getRolesForSpecialSubject(resourceName, AuthorizationTableService.ALL_AUTHENTICATED_USERS);
        AccessDecisionService accessDecisionService = accessDecisionServiceRef.getService();
        return accessDecisionService.isGranted(resourceName, requiredRoles,
                                               roles, subject);
    }

    /**
     * Check if the subject has a WScredential, is authenticated, and is not a basic auth credential.
     *
     * @param subject
     *            the subject to check
     * @return true if the subject has a WSCredential that is not marked as
     *         unauthenticated and is not marked as basic auth, otherwise false
     */
    private boolean isSubjectValid(Subject subject) {
        final WSCredential wsCred = getWSCredentialFromSubject(subject);
        if (wsCred == null) {
            return false;
        } else {
            // TODO revisit this when EJBs are supported add additional
            // checks would be required
            return !wsCred.isUnauthenticated() && !wsCred.isBasicAuth();
        }
    }

    /**
     * Get the realm name from the specified credential.
     *
     * @param cred
     *            the WSCredential to search, must not be null
     * @return realm name.
     */
    private String getRealmName(WSCredential cred) {
        String realmName = null;
        if (cred != null) {
            try {
                realmName = cred.getRealmName();
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught exception getting the realm name: " + e);
                }
            }
        }
        return realmName;
    }

}