/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.security.javaeesec.cdi.extensions;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.security.javaeesec.properties.ModuleProperties;
import com.ibm.ws.threadContext.ModuleMetaDataAccessorImpl;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;

/**
 * TODO: Determine if this should be an OSGi service.
 */
public class HttpAuthenticationMechanismsTracker {

    private static final TraceComponent tc = Tr.register(HttpAuthenticationMechanismsTracker.class);

    private final Map<String, Map<String, ModuleProperties>> moduleMapsPerApplication = new HashMap<String, Map<String, ModuleProperties>>();

    public void initialize(String applicationName) {
        if (applicationName != null) {
            moduleMapsPerApplication.remove(applicationName);
            moduleMapsPerApplication.put(applicationName, createInitializedWebModuleMap());
        }
    }

    private Map<String, ModuleProperties> createInitializedWebModuleMap() {
        Map<String, ModuleProperties> moduleMap = new HashMap<String, ModuleProperties>();
        Map<String, URL> wml = getWebModuleMap();
        if (wml != null) {
            for (Map.Entry<String, URL> entry : wml.entrySet()) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "moduleName : " + entry.getKey() + ", location : " + entry.getValue());
                }
                moduleMap.put(entry.getKey(), new ModuleProperties(entry.getValue()));
            }
        }
        return moduleMap;
    }

    private Map<String, URL> getWebModuleMap() {
        Map<URL, ModuleMetaData> mmds = getModuleMetaDataMap();
        Map<String, URL> map = null;
        if (mmds != null) {
            map = new HashMap<String, URL>();
            for (Map.Entry<URL, ModuleMetaData> entry : mmds.entrySet()) {
                ModuleMetaData mmd = entry.getValue();
                if (mmd instanceof WebModuleMetaData) {
                    String j2eeModuleName = mmd.getJ2EEName().getModule();
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "j2ee module name  : " + j2eeModuleName);
                    }
                    map.put(j2eeModuleName, entry.getKey());
                }
            }
        }
        return map;
    }

    public static String getApplicationName() {
        String result = null;
        Map<URL, ModuleMetaData> mmds = getModuleMetaDataMapInternal();
        if (mmds != null && !mmds.isEmpty()) {
            for (Map.Entry<URL, ModuleMetaData> entry : mmds.entrySet()) {
                ModuleMetaData mmd = entry.getValue();
                if (mmd instanceof WebModuleMetaData) {
                    J2EEName j2eeName = mmd.getJ2EEName();
                    if (j2eeName != null) {
                        result = j2eeName.getApplication();
                        break;
                    }
                }
            }
        }
        return result;
    }

    private static Map<URL, ModuleMetaData> getModuleMetaDataMapInternal() {
        return ModuleMetaDataAccessorImpl.getModuleMetaDataAccessor().getModuleMetaDataMap();
    }

    // Protected to allow unit testing
    protected Map<URL, ModuleMetaData> getModuleMetaDataMap() {
        return getModuleMetaDataMapInternal();
    }

    public Map<String, ModuleProperties> getModuleMap(String applicationName) {
        return moduleMapsPerApplication.get(applicationName);
    }

    public void addAuthMech(String applicationName, Class<?> annotatedClass, Class<?> implClass, Set<Annotation> annotations, Properties props) {
        Map<String, ModuleProperties> moduleMap = moduleMapsPerApplication.get(applicationName);
        String moduleName = getModuleFromClass(annotatedClass, moduleMap);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "moduleName: " + moduleName);
        }

        if (moduleMap.containsKey(moduleName)) {
            moduleMap.get(moduleName).putToAuthMechMap(implClass, props);
        } else {
            // if there is no match in the module name, it should be a shared jar file.
            // so place the authmech to the all modules.
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Place the AuthMech to all modules since the module is not found  Module: " + moduleName);
            }

            for (Map.Entry<String, ModuleProperties> entry : moduleMap.entrySet()) {
                entry.getValue().putToAuthMechMap(implClass, props);
            }
        }
    }

    /**
     * Identify the module name from the class. If the class exists in the jar file, return war file name
     * if it is located under the war file, otherwise returning jar file name.
     **/
    private String getModuleFromClass(Class<?> annotatedClass, Map<String, ModuleProperties> moduleMap) {
        String file = getClassFileLocation(annotatedClass);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "File name : " + file);
        }
        String moduleName = null;
        for (Map.Entry<String, ModuleProperties> entry : moduleMap.entrySet()) {
            URL location = entry.getValue().getLocation();
            String filePath = location.getFile();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "location : " + filePath);
            }

            if (location.getProtocol().equals("file") && file.startsWith(filePath)) {
                moduleName = entry.getKey();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "module name from the list  : " + moduleName);
                }
                break;
            }
        }
        if (moduleName == null) {
            moduleName = file;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "no match. use filename as module name : " + moduleName);
            }
        }
        return moduleName;
    }

    protected String getClassFileLocation(Class<?> annotatedClass) {
        return annotatedClass.getProtectionDomain().getCodeSource().getLocation().getFile();
    }

    public boolean existAuthMech(String applicationName, Class<?> authMechToExist) {
        return (null != getExistingAuthMechClass(applicationName, authMechToExist));
    }

    public Class<?> getExistingAuthMechClass(String applicationName) {
        return getExistingAuthMechClass(applicationName, null);
    }

    public Class<?> getExistingAuthMechClass(String applicationName, Class<?> authMechToExist) {
        Map<Class<?>, Properties> authMechs = null;
        Map<String, ModuleProperties> moduleMap = moduleMapsPerApplication.get(applicationName);
        if (moduleMap != null) {
            for (Map.Entry<String, ModuleProperties> entry : moduleMap.entrySet()) {
                authMechs = entry.getValue().getAuthMechMap();
                for (Class<?> authMech : authMechs.keySet()) {
                    if (authMechToExist != null) {
                        if (authMech.equals(authMechToExist)) {
                            return authMech;
                        }
                    } else {
                        return authMech;
                    }
                }
            }
        }
        return null;
    }

    public Map<Class<?>, Properties> getAuthMechs(String applicationName, String moduleName) {
        Map<Class<?>, Properties> authMechs = null;

        if (applicationName != null && !applicationName.isEmpty()) {
            Map<String, ModuleProperties> moduleMap = moduleMapsPerApplication.get(applicationName);
            if (moduleMap != null && moduleMap.containsKey(moduleName)) {
                authMechs = moduleMap.get(moduleName).getAuthMechMap();
            }
        }

        return authMechs;
    }

    public boolean isEmptyModuleMap(String applicationName) {
        if (applicationName == null || applicationName.isEmpty()) {
            return true;
        }

        Map<String, ModuleProperties> moduleMap = moduleMapsPerApplication.get(applicationName);
        boolean result = moduleMap != null ? moduleMap.isEmpty() : true;
        if (!result) {
            // check ModuleProperties is empty.
            for (Map.Entry<String, ModuleProperties> entry : moduleMap.entrySet()) {
                if (entry.getValue().getAuthMechMap().isEmpty()) {
                    result = true;
                } else {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

}
