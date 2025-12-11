/*******************************************************************************
 * Copyright (c) 2015, 2024 IBM Corporation and others.
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
package com.ibm.ws.security.authorization.jacc.ejb.impl;

import java.security.Permissions;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.security.jacc.EJBMethodPermission;
import javax.security.jacc.EJBRoleRefPermission;
import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyConfigurationFactory;
import javax.security.jacc.PolicyContextException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authorization.jacc.MethodInfo;
import com.ibm.ws.security.authorization.jacc.PolicyConfigurationManager;
import com.ibm.ws.security.authorization.jacc.RoleInfo;
import com.ibm.ws.security.authorization.jacc.ejb.EJBSecurityPropagator;

public class EJBSecurityPropagatorImpl implements EJBSecurityPropagator {
    private static String STARSTAR = "**";
    private static final TraceComponent tc = Tr.register(EJBSecurityPropagatorImpl.class);
    private static Map<String, Set<ModuleRoleInfo>> moduleRoleInfoMap = new ConcurrentHashMap<String, Set<ModuleRoleInfo>>();

    public EJBSecurityPropagatorImpl() {
    }

    @Override
    public void propagateEJBRoles(String contextId,
                                  String appName,
                                  String beanName,
                                  Map<String, String> roleLinkMap,
                                  Map<RoleInfo, List<MethodInfo>> methodMap,
                                  PolicyConfigurationManager policyConfigManager) {
        Set<ModuleRoleInfo> mris = moduleRoleInfoMap.get(contextId);
        if (mris == null) {
            mris = Collections.newSetFromMap(new ConcurrentHashMap<ModuleRoleInfo, Boolean>());
            moduleRoleInfoMap.put(contextId, mris);
        }
        mris.add(new ModuleRoleInfo(appName, beanName, roleLinkMap, methodMap));
        policyConfigManager.addEJB(appName, contextId);
    }

    @Override
    public void processEJBRoles(PolicyConfigurationFactory pcf, String contextId, PolicyConfigurationManager policyConfigManager) {
        Set<ModuleRoleInfo> mris = moduleRoleInfoMap.get(contextId);
        if (mris == null) {
            //nothing to do.
            return;
        }
        // construct allRoles.
        Set<String> allRoles = getAllRoles(mris);

        PolicyConfiguration ejbPC = null;
        String appName = mris.iterator().next().appName;
        boolean exist = policyConfigManager.containModule(appName, contextId);
        try {
            ejbPC = pcf.getPolicyConfiguration(contextId, !exist);
        } catch (PolicyContextException pce) {
            Tr.error(tc, "JACC_EJB_GET_POLICYCONFIGURATION_FAILURE", new Object[] { contextId, pce });
            return;
        }
        try {
            // for loop with bean name.
            for (ModuleRoleInfo mri : mris) {
                processRoleRefs(ejbPC, mri.beanName, mri.roleLinkMap, allRoles);
                processMethodPermissions(ejbPC, mri.beanName, mri.methodMap, allRoles);
                // commit will be invoked in PolicyCOnfigurationManager class.
            }
            policyConfigManager.linkConfiguration(appName, ejbPC);
            moduleRoleInfoMap.remove(contextId);
        } catch (PolicyContextException e) {
            Tr.error(tc, "JACC_EJB_PERMISSION_PROPAGATION_FAILURE", new Object[] { contextId, e });
        }
    }

    private Set<String> getAllRoles(Set<ModuleRoleInfo> mris) {
        Set<String> allRoles = new HashSet<String>();
        for (ModuleRoleInfo mri : mris) {
            if (mri.methodMap != null && mri.methodMap.size() > 0) {
                for (RoleInfo ri : mri.methodMap.keySet()) {
                    String roleName = ri.getRoleName();
                    if (roleName != null) {
                        allRoles.add(roleName);
                    }
                }
            }
        }
        if (allRoles.isEmpty()) {
            allRoles = null;
        }
        return allRoles;
    }

    private void processMethodPermissions(PolicyConfiguration ejbPC, String beanName, Map<RoleInfo, List<MethodInfo>> methodMap,
                                          Set<String> allRoles) throws PolicyContextException {
        if (methodMap != null && methodMap.size() > 0) {
            Permissions ejbRolePerms = null;
            Permissions ejbUncheckedPerms = null;
            Permissions ejbExcludedPerms = null;
            for (Entry<RoleInfo, List<MethodInfo>> entry : methodMap.entrySet()) {
                RoleInfo ri = entry.getKey();
                List<MethodInfo> miList = entry.getValue();

                if (ri.isPermitAll()) {
                    ejbUncheckedPerms = getEJBPermCollection(beanName, miList);
                    if (ejbUncheckedPerms != null) {
                        ejbPC.addToUncheckedPolicy(ejbUncheckedPerms);
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "addToUncheckedPolicy permission : " + ejbUncheckedPerms);
                    }
                } else if (ri.isDenyAll()) {
                    ejbExcludedPerms = getEJBPermCollection(beanName, miList);
                    if (ejbExcludedPerms != null) {
                        ejbPC.addToExcludedPolicy(ejbExcludedPerms);
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "addToExcludedPolicy permission : " + ejbExcludedPerms);
                    }
                } else {
                    ejbRolePerms = getEJBPermCollection(beanName, miList);
                    if (ejbRolePerms != null) {
                        String roleName = ri.getRoleName();
                        ejbPC.addToRole(roleName, ejbRolePerms);
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "addToRole(MethodPermisson) role : " + roleName + " permission : " + ejbRolePerms);
                    }
                }
            }
        }
    }

    private Permissions getEJBPermCollection(String beanName, List<MethodInfo> miList) {
        Permissions permCollection = new Permissions();
        if (miList != null && miList.size() > 0) {
            for (MethodInfo mi : miList) {
                String methodName = mi.getMethodName();
                String methodInfName = mi.getMethodInterfaceName();

                if (methodName.equals("*")) {
                    methodName = null;
                }

                if (methodInfName != null && methodInfName.equals("Unspecified")) {
                    methodInfName = null;
                }
                String[] paramArray = null;
                List<String> paramList = mi.getParamList();
                if (paramList != null) {
                    paramArray = paramList.toArray(new String[paramList.size()]);
                }

                if (tc.isDebugEnabled()) {
                    StringBuffer buf = new StringBuffer("addingEJBPermCollection: ejbName = ");
                    buf.append(beanName).append(", methodName = ").append(methodName).append(", methodInfName = ").append(methodInfName);
                    if (paramArray != null) {
                        buf.append(" # of params : " + paramArray.length);
                        for (String p : paramArray) {
                            buf.append(" param : " + p);
                        }
                    }
                    Tr.debug(tc, buf.toString());
                }
                EJBMethodPermission ejbMethodPerm = new EJBMethodPermission(beanName, methodName, methodInfName, paramArray);
                permCollection.add(ejbMethodPerm);
            }
        } else {
            permCollection = null;
        }
        return permCollection;
    }

    private void processRoleRefs(PolicyConfiguration ejbPC, String beanName, Map<String, String> roleLinkMap, Set<String> allRoles) throws PolicyContextException {
        if (roleLinkMap != null) {
            for (Entry<String, String> entry : roleLinkMap.entrySet()) {
                String refName = entry.getKey();
                String refLink = entry.getValue();
                EJBRoleRefPermission ejbRolePerm = new EJBRoleRefPermission(beanName, refName);
                ejbPC.addToRole(refLink, ejbRolePerm);
                EJBRoleRefPermission ejbRolePermLink = new EJBRoleRefPermission(beanName, refLink);
                ejbPC.addToRole(refLink, ejbRolePermLink);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "addToRole(RefName) role : " + refLink + " permission : " + ejbRolePerm);
                    Tr.debug(tc, "addToRole(RefLink) role : " + refLink + " permission : " + ejbRolePermLink);
                }
            }
        }
        // add additional role refs.
        if (allRoles == null || !allRoles.contains(STARSTAR)) {
            EJBRoleRefPermission ejbRolePerm = new EJBRoleRefPermission(beanName, STARSTAR);
            ejbPC.addToRole(STARSTAR, ejbRolePerm);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "addToRole(DeclaredRefLink) role : ** permission : " + ejbRolePerm);
        }
        if (allRoles != null) {
            for (String roleName : allRoles) {
                if (roleLinkMap == null || !roleLinkMap.containsValue(roleName)) {
                    EJBRoleRefPermission ejbRolePerm = new EJBRoleRefPermission(beanName, roleName);
                    ejbPC.addToRole(roleName, ejbRolePerm);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "addToRole(DeclaredRefLink) role : " + roleName + " permission : " + ejbRolePerm);
                }
            }
        }
    }
}

class ModuleRoleInfo {
    String appName;
    String beanName;
    Map<String, String> roleLinkMap;
    Map<RoleInfo, List<MethodInfo>> methodMap;

    ModuleRoleInfo(String appName, String beanName, Map<String, String> roleLinkMap, Map<RoleInfo, List<MethodInfo>> methodMap) {
        this.appName = appName;
        this.beanName = beanName;
        this.roleLinkMap = roleLinkMap;
        this.methodMap = methodMap;
    }
}
