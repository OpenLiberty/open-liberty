/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.util;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

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

    public static boolean parseMethodSecurity(Method method, Principal principal, Predicate<String> isUserInRoleFunction) throws UnauthenticatedException {

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
                checkAuthentication(principal);
                return Stream.of(theseroles).anyMatch(isUserInRoleFunction);
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
    private static boolean parseClassSecurity(Class<?> cls, Principal principal, Predicate<String> isUserInRoleFunction) throws UnauthenticatedException {

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
                    LOG.log(Level.FINEST, "found RolesAllowed in class level: {} " + cls.getName(),
                            new Object[] { theseroles });
                }
                checkAuthentication(principal);
                return Stream.of(theseroles).anyMatch(isUserInRoleFunction);
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
