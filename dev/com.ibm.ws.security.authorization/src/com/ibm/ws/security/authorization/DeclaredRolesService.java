/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.authorization;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Each container implements this method to provide the declared roles that are configured
 * in the application.
 */
public class DeclaredRolesService {
    private final Map<String, Map<String, Set<String>>> declaredRolesMap = new ConcurrentHashMap<>();

    public Set<String> getDeclaredRoles(String appName) {
        Map<String, Set<String>> moduleRolesMap = declaredRolesMap.get(appName);
        if (moduleRolesMap != null && !moduleRolesMap.isEmpty()) {
            Set<String> declaredRoles = new HashSet<>();
            for (Set<String> moduleRoles : moduleRolesMap.values()) {
                declaredRoles.addAll(moduleRoles);
            }
            return declaredRoles;

        }
        return Collections.emptySet();
    }

    public void addDeclaredRoles(String appName, String moduleName, Collection<String> roles) {
        if (roles.size() > 0) {
            Map<String, Set<String>> moduleRolesMap = declaredRolesMap.get(appName);
            if (moduleRolesMap == null) {
                moduleRolesMap = new ConcurrentHashMap<>();
                Map<String, Set<String>> oldValue = declaredRolesMap.putIfAbsent(appName, moduleRolesMap);
                if (oldValue != null) {
                    moduleRolesMap = oldValue;
                }
            }
            Set<String> moduleRoles = moduleRolesMap.get(moduleName);
            if (moduleRoles == null) {
                moduleRoles = Collections.newSetFromMap(new ConcurrentHashMap<>());
                Set<String> oldValue = moduleRolesMap.putIfAbsent(moduleName, moduleRoles);
                if (oldValue != null) {
                    moduleRoles = oldValue;
                }
            }
            moduleRoles.addAll(roles);
        }
    }

    public void removeModule(String appName, String moduleName) {
        Map<String, Set<String>> moduleRolesMap = declaredRolesMap.get(appName);
        if (moduleRolesMap != null) {
            moduleRolesMap.remove(moduleName);
            if (moduleRolesMap.isEmpty()) {
                declaredRolesMap.remove(appName);
            }
        }
    }
}
