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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.EJBMethodMetaData;
import com.ibm.ws.security.authorization.jacc.MethodInfo;
import com.ibm.ws.security.authorization.jacc.RoleInfo;

/**
 ** This class is for restructure the contents of MetaData to the
 ** contents for JACC provider.
 */
public class JaccUtil {
    private static TraceComponent tc = Tr.register(JaccUtil.class);

    public JaccUtil() {
    }

    public static Map<RoleInfo, List<MethodInfo>> convertMethodInfoList(List<EJBMethodMetaData> methodInfos) {
        Map<RoleInfo, List<MethodInfo>> methodMap = null;
        if (methodInfos != null && !methodInfos.isEmpty()) {
            methodMap = new HashMap<RoleInfo, List<MethodInfo>>();
            for (EJBMethodMetaData mi : methodInfos) {
                String methodName = mi.getMethodName();
                String methodIntName = mi.getEJBMethodInterface().specName();
                Method method = mi.getMethod();
                List<String> params = null;
                if (method != null) {
                    params = new ArrayList<String>();
                    for (Class<?> cl : method.getParameterTypes()) {
                        params.add(cl.getName());
                    }
                }
                List<String> roles = mi.getRolesAllowed();
                MethodInfo methodInfo = new MethodInfo(methodName, methodIntName, params);
                if (mi.isDenyAll() || mi.isPermitAll()) {
                    RoleInfo ri = null;
                    if (mi.isDenyAll()) {
                        ri = RoleInfo.DENY_ALL;
                    } else {
                        ri = RoleInfo.PERMIT_ALL;
                    }
                    putMethodInfo(methodMap, ri, methodInfo);
                } else if (roles != null && !roles.isEmpty()) {
                    for (String role : roles) {
                        putMethodInfo(methodMap, new RoleInfo(role), methodInfo);
                    }
                }
                if (tc.isDebugEnabled()) {
                    StringBuffer buf = new StringBuffer();
                    for (Entry<RoleInfo, List<MethodInfo>> entry : methodMap.entrySet()) {
                        RoleInfo ri = entry.getKey();
                        List<MethodInfo> miList = entry.getValue();
                        buf.append("\n-----------\nRoleInfo: ").append(ri.toString());
                        buf.append("MethodInfo : number of methods : ").append(miList.size());
                        for (MethodInfo minfo : miList) {
                            buf.append("\n").append(minfo.toString());
                        }
                    }
                    Tr.debug(tc, buf.toString());
                }
            }
        }
        return methodMap;
    }

    static public List<EJBMethodMetaData> mergeMethodInfos(BeanMetaData bmd) {
        List<EJBMethodMetaData> miList = new ArrayList<EJBMethodMetaData>();
        EJBMethodInfoImpl list[][] = { bmd.homeMethodInfos, bmd.localHomeMethodInfos, bmd.methodInfos, bmd.localMethodInfos, bmd.timedMethodInfos, bmd.wsEndpointMethodInfos,
                                       bmd.lifecycleInterceptorMethodInfos };

        for (EJBMethodMetaData[] miArray : list) {
            if (miArray != null && miArray.length > 0) {
                miList.addAll(Arrays.asList(miArray));
            }
        }
        if (miList.isEmpty()) {
            miList = null;
        }
        return miList;
    }

    static protected void putMethodInfo(Map<RoleInfo, List<MethodInfo>> methodMap, RoleInfo ri, MethodInfo methodInfo) {
        List<MethodInfo> methodInfoList = methodMap.get(ri);
        if (methodInfoList == null) {
            methodInfoList = new ArrayList<MethodInfo>();
            methodInfoList.add(methodInfo);
            methodMap.put(ri, methodInfoList);
        } else if (!methodInfoList.contains(methodInfo)) {
            methodInfoList.add(methodInfo);
        }
    }
}
