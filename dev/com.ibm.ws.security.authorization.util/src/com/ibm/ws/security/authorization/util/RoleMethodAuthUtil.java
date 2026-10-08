/*******************************************************************************
 * Copyright (c) 2020, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.util;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import com.ibm.ws.security.authorization.AuthorizationService;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;

public class RoleMethodAuthUtil {
    private static final Logger LOG = Logger.getLogger(RoleMethodAuthUtil.class.getName());

    // Set by RoleMethodAuthUtilService OSGi component via @Reference injection.
    private static volatile AuthorizationService authorizationService = null;

    /* package */ static void setAuthorizationService(AuthorizationService service) {
        authorizationService = service;
    }

    public static void checkAuthentication(Principal principal) throws UnauthenticatedException {
        if (principal == null) {
            throw new UnauthenticatedException("principal is null");
        }
        if ("UNAUTHENTICATED".equals(principal.getName())) {
            throw new UnauthenticatedException("principal is UNAUTHENTICATED");
        }
    }

    /**
     * Returns true if the given role is mapped to the Everyone special subject for
     * the current application. Returns false if the AuthorizationService is not
     * available or the application name cannot be determined.
     */
    private static boolean isEveryoneGranted(Collection<String> requiredRoles) {
        AuthorizationService authzService = authorizationService;
        if (authzService == null) {
            return false;
        }
        try {
            com.ibm.ws.runtime.metadata.ComponentMetaData cmd =
                ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            if (cmd == null) {
                return false;
            }
            com.ibm.ws.runtime.metadata.ModuleMetaData mmd = cmd.getModuleMetaData();
            if (!(mmd instanceof WebModuleMetaData)) {
                return false;
            }
            WebModuleMetaData wmmd = (WebModuleMetaData) mmd;
            String appName = wmmd.getConfiguration().getApplicationName();
            return authzService.isEveryoneGranted(appName, requiredRoles);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean parseMethodSecurity(Method method, Supplier<Principal> principal,
                                              Predicate<String> isUserInRoleFunction) throws UnauthenticatedException {

        boolean denyAll = getDenyAll(method);
        if (denyAll) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("Found DenyAll for method: {} " + method.getName()
                           + ", Injection Processing for web service is ignored");
            }
            return false;

        } else { // try RolesAllowed
            RolesAllowed rolesAllowed = getRolesAllowed(method);
            if (rolesAllowed != null) {
                String[] theseroles = rolesAllowed.value();
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.log(Level.FINEST, "found RolesAllowed in method: {} " + method.getName(),
                            new Object[] { theseroles });
                }
                // Check Everyone special subject first — no authentication required.
                // On z/OS this prevents a SAF EJBROLE lookup for roles mapped to Everyone.
               if (isEveryoneGranted(Arrays.asList(theseroles))) {
                   return true;
               }
                // No Everyone match — require authentication before calling isUserInRole.
                checkAuthentication(principal.get());
                for (String role : theseroles) {
                    if (isUserInRoleFunction.test(role)) {
                        return true;
                    }
                }
                return false; // authenticated, but not authorized
            } else {
                boolean permitAll = getPermitAll(method);
                if (permitAll) {
                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.finest("Found PermitAll for method: {}" + method.getName());
                    }
                    return true;
                } else { // try class level annotations
                    Class<?> cls = method.getDeclaringClass();
                    return parseClassSecurity(cls, principal, isUserInRoleFunction);
                }
            }
        }
    }

    // parse security JSR250 annotations at the class level
    private static boolean parseClassSecurity(Class<?> cls, Supplier<Principal> principal,
                                              Predicate<String> isUserInRoleFunction) throws UnauthenticatedException {

        // try DenyAll
        DenyAll denyAll = cls.getAnnotation(DenyAll.class);
        if (denyAll != null) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("Found class level @DenyAll - authorization denied for " + cls.getName());
            }
            return false;
        } else { // try RolesAllowed
            RolesAllowed rolesAllowed = cls.getAnnotation(RolesAllowed.class);
            if (rolesAllowed != null) {
                String[] theseroles = rolesAllowed.value();
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.log(Level.FINEST, "found RolesAllowed in class: {} " + cls.getName(),
                            new Object[] { theseroles });
                }
                // Check Everyone special subject first — no authentication required.
                if (isEveryoneGranted(Arrays.asList(theseroles))) {
                    return true;
                }
                // No Everyone match — require authentication before calling isUserInRole.
                checkAuthentication(principal.get());
                for (String role : theseroles) {
                    if (isUserInRoleFunction.test(role)) {
                        return true;
                    }
                }
                return false; // authenticated, but not authorized
            } else {
                // if no annotations on method or class (or if class has @PermitAll), return true;
                return true;
            }
        }
    }

    private static RolesAllowed getRolesAllowed(Method method) {
        return method.getAnnotation(RolesAllowed.class);
    }

    private static boolean getPermitAll(Method method) {
        return method.isAnnotationPresent(PermitAll.class);
    }

    private static boolean getDenyAll(Method method) {
        return method.isAnnotationPresent(DenyAll.class);
    }
}
