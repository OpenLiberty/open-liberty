/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.jacc.internal.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Tracks operations that happen on PolicyConfigurationFactory, PolicyConfiguration, PolicyFactory
 * and Policy test implementations, so we validate that the right things are happening on the
 * underlying authorization modules as actions are happening on the JakartaPolicyConfigProxy and
 * JakartaPolicyConfigFactoryProxy.
 */
public class AuthzModuleTracker {

    enum ModuleType {
        POLICY_CONFIG_FACTORY, POLICY_CONFIG,
        POLICY_FACTORY, POLICY,
        DELEGATING_POLICY_CONFIG_FACTORY,
        WRAPPING_POLICY_CONFIG_FACTORY, WRAPPING_POLICY_CONFIG;
    }

    static class MethodInfo {

        final ModuleType type;
        final String methodName;

        MethodInfo(ModuleType type, String methodName) {
            this.type = type;
            this.methodName = methodName;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            MethodInfo other = (MethodInfo) obj;
            return Objects.equals(methodName, other.methodName) && type == other.type;
        }

        @Override
        public String toString() {
            return "MethodInfo [type=" + type + ", methodName=" + methodName + "]";
        }
    }

    static Map<String, List<MethodInfo>> trackerMap = new HashMap<>();

    public static void addOperation(String contextID, ModuleType type, String methodName) {
        List<MethodInfo> methodInfos = trackerMap.get(contextID);
        if (methodInfos == null) {
            methodInfos = new ArrayList<>();
            trackerMap.put(contextID, methodInfos);
        }
        methodInfos.add(new MethodInfo(type, methodName));
    }

    public static void remove(String contextID) {
        trackerMap.remove(contextID);
    }

    public static void clear() {
        trackerMap.clear();
    }

    public static boolean hasTrackedData() {
        return !trackerMap.isEmpty();
    }

    public static boolean hasTrackedData(String contextID) {
        return trackerMap.containsKey(contextID);
    }

    public static boolean hasOperation(String contextID, ModuleType type, String methodName) {
        List<MethodInfo> methodInfos = trackerMap.get(contextID);
        if (methodInfos != null) {
            return methodInfos.contains(new MethodInfo(type, methodName));
        }
        return false;
    }

    public static int getOperationCount(String contextID) {
        List<MethodInfo> methodInfos = trackerMap.get(contextID);
        return methodInfos == null ? 0 : methodInfos.size();
    }

    public static int getOperationCount(String contextID, ModuleType type, String methodName) {
        List<MethodInfo> methodInfos = trackerMap.get(contextID);
        int count = 0;
        if (methodInfos != null) {
            MethodInfo compareMethodInfo = new MethodInfo(type, methodName);
            for (MethodInfo methodInfo : methodInfos) {
                if (methodInfo.equals(compareMethodInfo)) {
                    count++;
                }
            }
        }
        return count;
    }

    public static String getTrackerMapString() {
        return trackerMap.toString();
    }
}
