/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.appbnd.internal.delegation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.security.auth.Subject;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.javaee.dd.appbnd.RunAs;
import com.ibm.ws.javaee.dd.appbnd.SecurityRole;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.appbnd.internal.TraceConstants;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.IdentityStoreHandlerService;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.helper.AuthenticateUserHelper;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.delegation.DelegationProvider;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * This class defines the interface for creating
 * the run-as subject during delegation
 */
public class DefaultDelegationProvider implements DelegationProvider {
    private static final TraceComponent tc = Tr.register(DefaultDelegationProvider.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private final ConcurrentHashMap<String, Collection<SecurityRole>> appToSecurityRolesMap = new ConcurrentHashMap<String, Collection<SecurityRole>>();
    private final Map<String, Map<String, RunAs>> roleToRunAsMappingPerApp = new HashMap<String, Map<String, RunAs>>();
    private final Map<String, Map<String, Boolean>> roleToWarningMappingPerApp = new HashMap<String, Map<String, Boolean>>();
    private SecurityService securityService;
    private AtomicServiceReference<IdentityStoreHandlerService> identityStoreHandlerServiceRef = null;
    public String delegationUser = "";

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setIdentityStoreHandlerService(AtomicServiceReference<IdentityStoreHandlerService> identityStoreHandlerServiceRef) {
        this.identityStoreHandlerServiceRef = identityStoreHandlerServiceRef;
    }

    @Override
    public Subject getRunAsSubject(String roleName, String appName) throws AuthenticationException {
        Subject runAsSubject = null;
        RunAs runAs = getRunAs(roleName, appName);
        if (isValidRunAs(runAs)) {
            setDelegationUser(runAs);
            runAsSubject = authenticateRunAsUser(runAs);
        } else {
            if (!isWarningAlreadyIssued(roleName, appName)) {
                Tr.warning(tc, "RUNAS_INVALID_CONFIG", roleName, appName);
                markWarningAlreadyIssued(roleName, appName);
            }
        }
        return runAsSubject;
    }

    private RunAs getRunAs(String roleName, String appName) {
        RunAs runAs = getRunAsFromCache(roleName, appName);
        if (runAs == null) {
            runAs = getRunAsFromConfig(roleName, appName);
            addRunAsToCache(roleName, appName, runAs);
        }
        return runAs;
    }

    private RunAs getRunAsFromCache(String roleName, String appName) {
        RunAs runAs = null;
        Map<String, RunAs> roleToRunAsMap = roleToRunAsMappingPerApp.get(appName);
        if (roleToRunAsMap != null) {
            runAs = roleToRunAsMap.get(roleName);
        }
        return runAs;
    }

    private RunAs getRunAsFromConfig(String roleName, String appName) {
        RunAs runAs = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Using the hashmap: " + appToSecurityRolesMap.toString());
        }
        Collection<SecurityRole> securityRoles = appToSecurityRolesMap.get(appName);
        if (securityRoles != null) {
            for (SecurityRole securityRole : securityRoles) {
                String roleNameFromConfig = securityRole.getName();
                if (roleName.equals(roleNameFromConfig)) {
                    runAs = securityRole.getRunAs();
                }
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The app " + appName + " was not found in the map, " + appToSecurityRolesMap);
            }
        }

        if (runAs == null) {
            runAs = new com.ibm.ws.security.appbnd.internal.delegation.NoRunAs();
        }
        return runAs;
    }

    private void addRunAsToCache(String roleName, String appName, RunAs runAs) {
        Map<String, RunAs> roleToRunAsMap = getRoleToRunAsMap(appName);
        roleToRunAsMap.put(roleName, runAs);
    }

    private Map<String, RunAs> getRoleToRunAsMap(String appName) {
        Map<String, RunAs> roleToRunAsMap = roleToRunAsMappingPerApp.get(appName);
        if (roleToRunAsMap == null) {
            roleToRunAsMap = new HashMap<String, RunAs>();
            roleToRunAsMappingPerApp.put(appName, roleToRunAsMap);
        }
        return roleToRunAsMap;
    }

    public void setDelegationUser(RunAs runAs) {
        delegationUser = runAs.getUserid();
    }

    public String getDelegationUser() {
        return delegationUser;
    }

    private Boolean isWarningAlreadyIssued(String roleName, String appName) {
        Boolean warningIssued = false;
        Map<String, Boolean> roleToWarningMap = roleToWarningMappingPerApp.get(appName);
        if (roleToWarningMap != null) {
            warningIssued = roleToWarningMap.get(roleName);
            if (warningIssued == null)
                warningIssued = false;
        }
        return warningIssued;
    }

    private void markWarningAlreadyIssued(String roleName, String appName) {
        Map<String, Boolean> roleToWarningMap = getRoleToWarningMap(appName);
        roleToWarningMap.put(roleName, true);
    }

    private Map<String, Boolean> getRoleToWarningMap(String appName) {
        Map<String, Boolean> roleToWarningMap = roleToWarningMappingPerApp.get(appName);
        if (roleToWarningMap == null) {
            roleToWarningMap = new HashMap<String, Boolean>();
            roleToWarningMappingPerApp.put(appName, roleToWarningMap);
        }
        return roleToWarningMap;
    }

    private boolean isValidRunAs(RunAs runAs) {
        return (runAs != null && runAs.getUserid() != null);
    }

    private Subject authenticateRunAsUser(RunAs runAs) throws AuthenticationException {
        String username = runAs.getUserid();
        String password = PasswordUtil.passwordDecode(runAs.getPassword());
        IdentityStoreHandlerService identityStoreHandlerService = getIdentityStoreHandlerService();
        if (identityStoreHandlerService != null && identityStoreHandlerService.isIdentityStoreHanderAvailable()) {
            Subject inSubject;
            if (password != null) {
                inSubject= identityStoreHandlerService.createHashtableInSubject(username, password);
            } else {
                inSubject= identityStoreHandlerService.createHashtableInSubject(username);
            }
            return securityService.getAuthenticationService().authenticate(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, inSubject);
        }
        else if (password != null) {
            AuthenticationData authenticationData = createAuthenticationData(username, password);
            return securityService.getAuthenticationService().authenticate(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, authenticationData, null);
        } else {
            AuthenticateUserHelper authHelper = new AuthenticateUserHelper();
            return authHelper.authenticateUser(securityService.getAuthenticationService(), username, JaasLoginConfigConstants.SYSTEM_WEB_INBOUND);
        }
    }

    /**
     * Creates the application to security roles mapping for a given application.
     *
     * @param appName the name of the application for which the mappings belong to.
     * @param securityRoles the security roles of the application.
     */
    public void createAppToSecurityRolesMapping(String appName, Collection<SecurityRole> securityRoles) {
        //only add it if we don't have a cached copy
        appToSecurityRolesMap.putIfAbsent(appName, securityRoles);
    }

    /**
     * Removes the role to RunAs mappings for a given application.
     *
     * @param appName the name of the application for which the mappings belong to.
     */
    public void removeRoleToRunAsMapping(String appName) {
        Map<String, RunAs> roleToRunAsMap = roleToRunAsMappingPerApp.get(appName);
        if (roleToRunAsMap != null) {
            roleToRunAsMap.clear();
        }

        appToSecurityRolesMap.remove(appName);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Updated the appToSecurityRolesMap: " + appToSecurityRolesMap.toString());
        }
        removeRoleToWarningMapping(appName);

    }

    /**
     * Removes the role to warning mappings for a given application.
     *
     * @param appName the name of the application for which the mappings belong to.
     */
    public void removeRoleToWarningMapping(String appName) {
        Map<String, Boolean> roleToWarningMap = roleToWarningMappingPerApp.get(appName);
        if (roleToWarningMap != null) {
            roleToWarningMap.clear();
        }
        roleToWarningMappingPerApp.remove(appName);
    }

    @Trivial
    protected AuthenticationData createAuthenticationData(String username, String password) {
        AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.USERNAME, username);
        if (password != null) {
            authenticationData.set(AuthenticationData.PASSWORD, password.toCharArray());
        }
        return authenticationData;
    }

    private IdentityStoreHandlerService getIdentityStoreHandlerService() {
        if (identityStoreHandlerServiceRef != null) {
            return identityStoreHandlerServiceRef.getService();
        }
        return null;
    }
}