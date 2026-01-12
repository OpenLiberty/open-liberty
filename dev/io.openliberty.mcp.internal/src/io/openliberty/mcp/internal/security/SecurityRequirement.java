/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;

/**
 * Stores the authorization requirements data needed to authorize access to a MCP tool
 *
 * @param securityAnnotation the security annotation found that will be used for authorization decisions
 * @param roleList the list of roles allowed to access the tool. Having any of these roles is sufficient to grant access. This will be empty unless a RolesAllowed annotation is
 *     used to determine the authorization decision
 */
public record SecurityRequirement(SecurityAnnotation securityAnnotation, List<String> roleList) {

    public static enum SecurityAnnotation {
        DENY, PERMIT, ROLES, NONE
    }

    /**
     *
     * Return the rolelist for the annotation
     *
     * @param a the annotation - This can be a method or class annotation
     * @return the roleList for the given annotation
     */
    private static List<String> getRoleList(RolesAllowed rolesAllowed) {
        List<String> roleList = new ArrayList<>();
        Collections.addAll(roleList, rolesAllowed.value());
        return roleList;
    }

    /**
     *
     * Return the annotation type based on where the annotation resides.
     * This method enforces the order of permission i.e. least permissive to most permissive.
     * Methods are tested first (Deny is least permissive, next PermitAll then RolesAllowed)
     * Classes are tested in the same fashion.
     * Method annotations override Class annotations.
     *
     * @param m the annotated method
     * @return the Authorization data calculated
     */
    public static SecurityRequirement createFrom(AnnotatedMethod<?> m) {

        // Method-level checks (higher precedence)
        if (m.isAnnotationPresent(DenyAll.class))
            return new SecurityRequirement(SecurityAnnotation.DENY, Collections.emptyList());
        if (m.isAnnotationPresent(PermitAll.class))
            return new SecurityRequirement(SecurityAnnotation.PERMIT, Collections.emptyList());
        if (m.isAnnotationPresent(RolesAllowed.class))
            return new SecurityRequirement(SecurityAnnotation.ROLES, getRoleList(m.getAnnotation(RolesAllowed.class)));

        AnnotatedType<?> c = m.getDeclaringType();
        // for io.openliberty.mcp.internal.test.schema.SchemaTest tests that use annotated mocks methods
        if (Objects.isNull(c))
            return new SecurityRequirement(SecurityAnnotation.NONE, Collections.emptyList());

        // Class-level checks (lower precedence)
        if (c.isAnnotationPresent(DenyAll.class))
            return new SecurityRequirement(SecurityAnnotation.DENY, Collections.emptyList());
        if (c.isAnnotationPresent(PermitAll.class))
            return new SecurityRequirement(SecurityAnnotation.PERMIT, Collections.emptyList());
        if (c.isAnnotationPresent(RolesAllowed.class))
            return new SecurityRequirement(SecurityAnnotation.ROLES, getRoleList(c.getAnnotation(RolesAllowed.class)));

        // No security annotations
        return new SecurityRequirement(SecurityAnnotation.NONE, Collections.emptyList());
    }
}
