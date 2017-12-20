/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.jaxrs20.support.JaxRsInstanceManager;
import com.ibm.ws.jaxrs20.utils.JaxRsUtils;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.injectionengine.ReferenceContext;

/**
 * JaxWsModuleMetaData holds all the runtime data for the webservice engine, e.g. Container, classloader and etc.
 */
public class JaxRsModuleMetaData {

    private volatile JaxRsServerMetaData serverMetaData;

    private volatile JaxRsClientMetaData clientMetaData;

    private String name;

    private final ClassLoader appContextClassLoader;

    private final J2EEName j2EEName;

    private final Container moduleContainer;

    private final ModuleInfo moduleInfo;

    private final Map<Class<?>, ReferenceContext> referenceContextMap;

    private JaxRsInstanceManager jaxRsInstanceManager;

    private Object managedAppRef;

    /**
     * @return the managedAppRef
     */
    public Object getManagedAppRef() {
        return managedAppRef;
    }

    /**
     * @param managedAppRef the managedAppRef to set
     */
    public void setManagedAppRef(Object managedAppRef) {
        this.managedAppRef = managedAppRef;
    }

    // the ModuleMetaDatas which contain the JaxRsModuleMetadata
    private final List<ModuleMetaData> enclosingModuleMetaDatas = new ArrayList<ModuleMetaData>(2);

    public JaxRsModuleMetaData(ModuleMetaData moduleMetaData, Container moduleContainer, ClassLoader appContextClassLoader) {
        this.moduleContainer = moduleContainer;
        this.enclosingModuleMetaDatas.add(moduleMetaData);
        this.j2EEName = moduleMetaData.getJ2EEName();
        this.moduleInfo = JaxRsUtils.getModuleInfo(moduleContainer);
        this.jaxRsInstanceManager = new JaxRsInstanceManager(moduleInfo.getClassLoader());
        this.appContextClassLoader = appContextClassLoader;
        this.referenceContextMap = new HashMap<Class<?>, ReferenceContext>();
    }

    /**
     * @return the referenceContextMap
     */
    public Map<Class<?>, ReferenceContext> getReferenceContextMap() {
        return referenceContextMap;
    }

    /**
     * @return the referenceContext of the injected class
     */
    public ReferenceContext getReferenceContext(Class<?> clazz) {
        return referenceContextMap.get(clazz);
    }

    /**
     * @param referenceContext the referenceContext to set
     */
    public void setReferenceContext(Class<?> clazz, ReferenceContext referenceContext) {
        this.referenceContextMap.put(clazz, referenceContext);
    }

    /**
     * @return the moduleInfo
     */
    public ModuleInfo getModuleInfo() {
        return moduleInfo;
    }

    /**
     * @return the jaxWsInstanceManager
     */
    public JaxRsInstanceManager getJaxRsInstanceManager() {
        return jaxRsInstanceManager;
    }

    /**
     * @return the serverMetaData
     */
    public JaxRsServerMetaData getServerMetaData() {
        if (serverMetaData == null) {
            synchronized (this) {
                if (serverMetaData == null) {
                    serverMetaData = new JaxRsServerMetaData(this);
                }
            }
        }
        return serverMetaData;
    }

    /**
     * @return the clientMetaData
     */
    public JaxRsClientMetaData getClientMetaData() {
        if (clientMetaData == null) {
            synchronized (this) {
                if (clientMetaData == null) {
                    clientMetaData = new JaxRsClientMetaData(this);
                }
            }
        }
        return clientMetaData;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the appContextClassLoader
     */
    public ClassLoader getAppContextClassLoader() {
        return appContextClassLoader;
    }

    /**
     * @return the j2EEName
     */
    public J2EEName getJ2EEName() {
        return j2EEName;
    }

    /**
     * @return the moduleContainer
     */
    public Container getModuleContainer() {
        return moduleContainer;
    }

    /**
     *
     * @return
     */
    public List<ModuleMetaData> getEnclosingModuleMetaDatas() {
        return this.enclosingModuleMetaDatas;
    }

    public void destroy() {
        if (serverMetaData != null) {
            serverMetaData.destroy();
        }
        if (clientMetaData != null) {
            clientMetaData.destroy();
        }

        jaxRsInstanceManager = null;
    }

}
