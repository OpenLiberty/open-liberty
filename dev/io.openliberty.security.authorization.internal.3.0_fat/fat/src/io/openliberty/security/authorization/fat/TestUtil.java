/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.fat;

import java.io.PrintWriter;
import java.security.PermissionCollection;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import jakarta.security.enterprise.SecurityContext;
import jakarta.security.jacc.PolicyConfiguration;
import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyContext;
import jakarta.security.jacc.PrincipalMapper;

public class TestUtil {

    public static final Set<String> allRoles = Set.of("CDIRole1", "CDIRole2", "CDIRole3", "CDIRole4",
                                                      "WebRole1", "WebRole2", "WebRole3", "WebRole4",
                                                      "EJBRole1", "EJBRole2", "EJBRole3", "EJBRole4",
                                                      "RestRole1", "RestRole2", "RestRole3", "RestRole4",
                                                      "WebServicesRole1", "WebServicesRole2", "WebServicesRole3", "WebServicesRole4",
                                                      "AllAuthenticated");

    public static void printSecurityData(PrintWriter out, ComponentContextWrapper componentContext, SecurityContext secContext) {

        // Display user principal
        Principal userPrincipal = componentContext.getUserPrincipal();
        String principalName = null;
        if (userPrincipal == null) {
            principalName = "Not authenticated";
        } else {
            principalName = userPrincipal.getName();
            // EJB's getUserPrincipal returns a Principal with the name of UNAUTHENTICATED instead of null
            if ("UNAUTHENTICATED".equals(principalName)) {
                principalName = "Not authenticated";
            }
        }
        out.println("User Principal: " + principalName);

        if (secContext.getCallerPrincipal() != null) {
            out.println("Caller Principal: " + secContext.getCallerPrincipal().getName());
        } else {
            out.println("Caller Principal: Not authenticated");
        }

        // Check roles
        for (String role : allRoles) {
            checkRole(out, componentContext, role, secContext);
        }
        checkRole(out, componentContext, "**", secContext);

        Set<String> declaredRoles = secContext.getAllDeclaredCallerRoles();

        out.println("Declared Roles: " + setToString(declaredRoles));

        try {

            Subject subject = (Subject) PolicyContext.getContext(PolicyContext.SUBJECT);
            PrincipalMapper roleMapper = (PrincipalMapper) PolicyContext.getContext(PolicyContext.PRINCIPAL_MAPPER);
            boolean isStarStarMapped = roleMapper.isAnyAuthenticatedUserRoleMapped();
            Set<String> mappedRoles = roleMapper.getMappedRoles(subject);

            out.println("Is ** mapped: " + isStarStarMapped);
            out.println("PrincipalMapper Roles for subject: " + setToString(mappedRoles));

            PolicyConfiguration pc = PolicyConfigurationFactory.get().getPolicyConfiguration();

            if (pc == null) {
                out.println("PolicyConfiguration is null");
            } else {
                Map<String, PermissionCollection> rolePerms = pc.getPerRolePermissions();
                Set<String> calculatedDeclaredCallerRoles = new HashSet<>();
                for (String role : mappedRoles) {
                    PermissionCollection perms = rolePerms.get(role);
                    if (perms != null && perms.elements().hasMoreElements()) {
                        calculatedDeclaredCallerRoles.add(role);
                    }
                }
                out.println("Calculated declared roles: " + setToString(calculatedDeclaredCallerRoles));
            }
        } catch (Throwable e) {
            // Expected when no policy is defined or appAuthorizatoin feature is not enabled
        }
    }

    public static String setToString(Set<String> set) {
        StringBuilder builder = new StringBuilder();
        for (String element : set) {
            if (builder.length() != 0) {
                builder.append(", ");
            }
            builder.append(element);
        }
        return builder.toString();
    }

    private static void checkRole(PrintWriter out, ComponentContextWrapper componentContext, String role, SecurityContext secContext) {
        boolean inRole = componentContext.isUserInRole(role);
        out.println("User in role " + role + ": " + inRole);
        inRole = secContext.isCallerInRole(role);
        out.println("Caller in role " + role + ": " + inRole);
    }
}
