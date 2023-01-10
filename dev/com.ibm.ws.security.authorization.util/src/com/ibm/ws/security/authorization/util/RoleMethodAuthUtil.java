/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

public class RoleMethodAuthUtil {
    private static final Logger LOG = Logger.getLogger(RoleMethodAuthUtil.class.getName());

    public static void checkAuthentication(Principal principal) throws UnauthenticatedException {
        if (principal == null) {
            throw new UnauthenticatedException("principal is null");
        }
        if ("UNAUTHENTICATED".equals(principal.getName())) {
            throw new UnauthenticatedException("principal is UNAUTHENTICATED");
        }
    }

    public static boolean parseMethodSecurity(Method method, Supplier<Principal> principal, Predicate<String> isUserInRoleFunction) throws UnauthenticatedException {

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
                for (String role : theseroles) {
                    if (isUserInRoleFunction.test(role)) {
                        return true;
                    }
                }
                checkAuthentication(principal.get()); // throws UnauthenticatedException if not authenticated
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
    private static boolean parseClassSecurity(Class<?> cls, Supplier<Principal> principal, Predicate<String> isUserInRoleFunction) throws UnauthenticatedException {

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
                for (String role : theseroles) {
                    if (isUserInRoleFunction.test(role)) {
                        return true;
                    }
                }
                checkAuthentication(principal.get()); // throws UnauthenticatedException if not authenticated
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
