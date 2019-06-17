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
import java.util.List;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.jaxrs20.utils.JaxRsUtils;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;

/**
 * JaxWsModuleMetaData holds all the runtime data for the webservice engine, e.g. Container, classloader and etc.
 */
public class JaxRsModuleMetaData {

    private static final TraceComponent tc = Tr.register(JaxRsModuleMetaData.class);

    private volatile JaxRsServerMetaData serverMetaData;

    private String name;

    private final ClassLoader appContextClassLoader;

    private final J2EEName j2EEName;

    private final Container moduleContainer;

    private final ModuleInfo moduleInfo;

    private Object managedAppRef;

    public static MetaDataSlot jaxrsModuleSlot = null;

    public static void setJaxRsModuleMetaData(ModuleMetaData mmd, JaxRsModuleMetaData moduleMetaData) {
        if (mmd != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Setting ModuleMetaData on ModuleMetaData instance: " + mmd);
            }
            mmd.setMetaData(jaxrsModuleSlot, moduleMetaData);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Could not set ModuleMetaData because ModuleMetaData " + "is null");
            }
        }
    }

    public static JaxRsModuleMetaData getJaxRsModuleMetaData(ModuleMetaData mmd) {
        if (mmd != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Getting JaxRs module metadata from module metadata instance: " + mmd);
            }

            return (JaxRsModuleMetaData) mmd.getMetaData(jaxrsModuleSlot);
        }
        return null;
    }

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
        this.appContextClassLoader = appContextClassLoader;
    }

    /**
     * @return the moduleInfo
     */
    public ModuleInfo getModuleInfo() {
        return moduleInfo;
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
    }
}
