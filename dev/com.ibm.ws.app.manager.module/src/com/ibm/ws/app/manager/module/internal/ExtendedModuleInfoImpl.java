/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.module.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.AltDDEntryGetter;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.metadata.extended.MetaDataGetter;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 *
 */
public class ExtendedModuleInfoImpl implements ExtendedModuleInfo, MetaDataGetter<ModuleMetaData>, AltDDEntryGetter {
    private static final TraceComponent tc = Tr.register(ExtendedModuleInfoImpl.class, "app.manager", "com.ibm.ws.app.manager.module.internal.resources.Messages");

    private final ApplicationInfo appInfo;
    private final String moduleName;
    private final String path;
    private final Container moduleContainer;
    private final Entry altDDEntry;
    private final List<ContainerInfo> moduleClassesContainers;
    private final ModuleClassLoaderFactory classLoaderFactory;
    private final ContainerInfo.Type containerInfoType;
    private ClassLoader classLoader;
    private ModuleMetaData moduleMetaData;
    private List<String> nestedModuleMetaDataTypes;
    private List<ModuleMetaData> nestedModuleMetaDataValues;

    public ExtendedModuleInfoImpl(ApplicationInfo appInfo, String moduleName, String path,
                                  Container moduleContainer, Entry altDDEntry, List<ContainerInfo> moduleClassesContainers,
                                  ModuleClassLoaderFactory classLoaderFactory,
                                  ContainerInfo.Type containerInfoType, Class<? extends ModuleInfo> subtype) throws UnableToAdaptException {

        this.appInfo = appInfo;
        this.moduleName = moduleName;
        this.path = path;
        this.moduleContainer = moduleContainer;
        this.altDDEntry = altDDEntry;
        this.moduleClassesContainers = moduleClassesContainers;
        this.classLoaderFactory = classLoaderFactory;
        this.containerInfoType = containerInfoType;

        NonPersistentCache overlayCache = moduleContainer.adapt(NonPersistentCache.class);
        if (overlayCache != null) {
            overlayCache.addToCache(ModuleInfo.class, this);
            overlayCache.addToCache(subtype, this);
        } else {
            //the overlayCache should never be null, if it is then the only real explanation
            //is that the containers are being torn down, check if the server is shutting down
            if (!!!FrameworkState.isStopping()) {
                //so the server isn't shutting down, but the overlayCache was null
                //this is a bad place, thrown an unable to adapt as that is a good
                //escape from the constructor
                if (containerInfoType == ContainerInfo.Type.RAR_MODULE) {
                    throw new UnableToAdaptException(Tr.formatMessage(tc, "error.cache.adapt.connector", moduleName));
                } else {
                    throw new UnableToAdaptException(Tr.formatMessage(tc, "error.cache.adapt", moduleName));
                }
            }
        }
    }

    public void setMetaData(ModuleMetaData mmd) {
        this.moduleMetaData = mmd;
    }

    @Override
    public String getName() {
        return this.moduleName;
    }

    @Override
    public String getURI() {
        return this.path;
    }

    @Override
    public Container getContainer() {
        return this.moduleContainer;
    }

    @Override
    public Entry getAltDDEntry(ContainerInfo.Type type) {
        return type == this.containerInfoType ? this.altDDEntry : null;
    }

    @Override
    public ModuleMetaData getMetaData() {
        return this.moduleMetaData;
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        return this.appInfo;
    }

    @Override
    public synchronized ClassLoader getClassLoader() {
        if (this.classLoader == null && this.classLoaderFactory != null) {
            this.classLoader = this.classLoaderFactory.createModuleClassLoader(this, this.moduleClassesContainers);
        }
        return this.classLoader;
    }

    @Override
    public synchronized void putNestedMetaData(String moduleType, ModuleMetaData nestedMetaData) {
        if (this.nestedModuleMetaDataTypes == null) {
            this.nestedModuleMetaDataTypes = new ArrayList<String>(2);
            this.nestedModuleMetaDataValues = new ArrayList<ModuleMetaData>(2);
        }
        this.nestedModuleMetaDataTypes.add(moduleType);
        this.nestedModuleMetaDataValues.add(nestedMetaData);
    }

    @Override
    public synchronized ModuleMetaData getNestedMetaData(String moduleType) {
        if (this.nestedModuleMetaDataTypes != null) {
            int index = this.nestedModuleMetaDataTypes.indexOf(moduleType);
            if (index >= 0) {
                return this.nestedModuleMetaDataValues.get(index);
            }
        }
        return null;
    }

    @Override
    public synchronized List<ModuleMetaData> getNestedMetaData() {
        if (this.nestedModuleMetaDataTypes != null) {
            return Collections.unmodifiableList(nestedModuleMetaDataValues);
        } else {
            return Collections.emptyList();
        }
    }
}
