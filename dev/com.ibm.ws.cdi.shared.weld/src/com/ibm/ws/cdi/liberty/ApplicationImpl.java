/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.liberty;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.CDIRuntimeException;
import com.ibm.ws.cdi.interfaces.Application;
import com.ibm.ws.cdi.interfaces.ApplicationType;
import com.ibm.ws.cdi.interfaces.ArchiveType;
import com.ibm.ws.cdi.interfaces.CDIArchive;
import com.ibm.ws.container.service.app.deploy.ApplicationClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo.Type;
import com.ibm.ws.container.service.app.deploy.EARApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ModuleClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.app.deploy.extended.LibraryClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.extended.LibraryContainerInfo.LibraryType;
import com.ibm.ws.container.service.app.deploy.extended.ModuleContainerInfo;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 *
 */
public class ApplicationImpl implements Application {

    private static final TraceComponent tc = Tr.register(ApplicationImpl.class);

    private final ExtendedApplicationInfo applicationInfo;
    private final EARApplicationInfo earApplicationInfo;
    private final RuntimeFactory factory;
    private final ApplicationType type;
    private final Collection<CDIArchive> moduleArchives;
    private final Collection<CDIArchive> libraryArchives;

    /**
     * @param applicationInfo
     * @throws CDIException
     */
    public ApplicationImpl(ApplicationInfo applicationInfo, RuntimeFactory factory) throws CDIException {
        this.applicationInfo = (ExtendedApplicationInfo) applicationInfo;
        this.factory = factory;
        if (applicationInfo instanceof EARApplicationInfo) {
            this.type = ApplicationType.EAR;
            this.earApplicationInfo = (EARApplicationInfo) applicationInfo;
        } else {
            //if it isn't an EAR then it must be a WAR, we don't support WABs etc yet
            this.type = ApplicationType.WAR;
            this.earApplicationInfo = null;
        }

        //initialize archive caches ... must be last section
        //initModuleArchives() must be run before initLibraryArchives()
        this.moduleArchives = initModuleArchives();
        this.libraryArchives = initLibraryArchives();
    }

    @SuppressWarnings("unchecked")
    private <T> T getFromCache(Class<T> clazz) throws CDIException {
        T data = null;
        try {
            NonPersistentCache cache = applicationInfo.getContainer().adapt(NonPersistentCache.class);
            data = (T) cache.getFromCache(clazz);
        } catch (UnableToAdaptException e) {
            throw new CDIException(e);
        }
        return data;
    }

    private ApplicationClassesContainerInfo getApplicationClassesContainerInfo() throws CDIException {
        ApplicationClassesContainerInfo applicationClassesContainerInfo = getFromCache(ApplicationClassesContainerInfo.class);
        return applicationClassesContainerInfo;
    }

    /** {@inheritDoc} */
    @Override
    public J2EEName getJ2EEName() {
        return applicationInfo.getMetaData().getJ2EEName();
    }

    @Override
    public ApplicationMetaData getApplicationMetaData() {
        return applicationInfo.getMetaData();
    }

    /**
     * {@inheritDoc}
     *
     * @throws CDIException
     */
    @Override
    public boolean hasModules() throws CDIException {
        return getApplicationClassesContainerInfo() != null;
    }

    private ClassLoader getApplicationClassLoader() throws CDIException {
        ClassLoader classLoader;
        if (this.type == ApplicationType.EAR) {
            classLoader = this.earApplicationInfo.getApplicationClassLoader();
        } else {
            // this should be a singleton module, so we want to use that module's class loader
            Collection<CDIArchive> modules = getModuleArchives();
            if (modules.size() == 1) {
                CDIArchive firstModule = modules.iterator().next();
                classLoader = firstModule.getClassLoader();
            } else {
                //we should NEVER ever reach this code
                //if we get here then the application type was not EAR so must be WAR, which should only be a single module
                throw new CDIRuntimeException("There was more than one module in an application of type: " + this.type);
            }
        }
        return classLoader;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<CDIArchive> getLibraryArchives() {
        return this.libraryArchives;
    }

    private Collection<CDIArchive> initLibraryArchives() throws CDIException {
        Map<ContainerInfo, CDIArchive> libraryArchives = new HashMap<ContainerInfo, CDIArchive>();

        ApplicationClassesContainerInfo appClassesCntrInfo = getApplicationClassesContainerInfo();
        if (appClassesCntrInfo != null) {
            List<ContainerInfo> original = appClassesCntrInfo.getLibraryClassesContainerInfo();

            for (ContainerInfo containerInfo : original) {
                Type ciType = containerInfo.getType();

                if (ciType == Type.EAR_LIB) {
                    CDIArchive archive = libraryArchives.get(containerInfo);
                    if (archive == null) {
                        ArchiveType type = ArchiveType.EAR_LIB;
                        ClassLoader classLoader = getApplicationClassLoader();
                        archive = factory.newArchive(this, containerInfo, type, classLoader);
                        libraryArchives.put(containerInfo, archive);
                    }
                } else if (ciType == Type.SHARED_LIB) {
                    LibraryClassesContainerInfo libraryClassesContainerInfo = (LibraryClassesContainerInfo) containerInfo;
                    LibraryType libType = libraryClassesContainerInfo.getLibraryType();
                    ArchiveType type = null;
                    ClassLoader classLoader = null;

                    if (libType == LibraryType.PRIVATE_LIB) {
                        type = ArchiveType.EAR_LIB;
                        classLoader = getApplicationClassLoader();
                    } else {
                        type = ArchiveType.SHARED_LIB;
                        classLoader = libraryClassesContainerInfo.getClassLoader();
                    }
                    List<ContainerInfo> classesContainerInfos = libraryClassesContainerInfo.getClassesContainerInfo();
                    for (ContainerInfo ci : classesContainerInfos) {
                        CDIArchive archive = libraryArchives.get(ci);
                        if (archive == null) {
                            archive = factory.newArchive(this, ci, type, classLoader);
                            libraryArchives.put(ci, archive);
                        }
                    }
                }
            }
        }

        return libraryArchives.values();
    }

    /** {@inheritDoc} */
    @Override
    public Collection<CDIArchive> getModuleArchives() {
        return this.moduleArchives;
    }

    private Collection<CDIArchive> initModuleArchives() throws CDIException {
        Map<ContainerInfo, CDIArchive> moduleArchives = new HashMap<ContainerInfo, CDIArchive>();
        ApplicationClassesContainerInfo appClassesCntrInfo = getApplicationClassesContainerInfo();
        if (appClassesCntrInfo != null) {
            List<ModuleClassesContainerInfo> moduleClassesContainerInfos = appClassesCntrInfo.getModuleClassesContainerInfo();
            for (ModuleClassesContainerInfo ci : moduleClassesContainerInfos) {
                ModuleContainerInfo rootContainerInfo = (ModuleContainerInfo) ci;
                ArchiveType type = ContainerInfoTypeUtils.getType(rootContainerInfo.getType());
                ClassLoader classLoader = rootContainerInfo.getClassLoader();
                CDIArchive archive = factory.newArchive(this, rootContainerInfo, type, classLoader);
                moduleArchives.put(rootContainerInfo, archive);
            }
        }

        return moduleArchives.values();
    }

    /** {@inheritDoc} */
    @Override
    public ClassLoader getClassLoader() throws CDIException {
        return getApplicationClassLoader();
    }

    /** {@inheritDoc} */
    @Override
    public ApplicationType getType() {
        return type;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return applicationInfo.getName();
    }

    @Override
    public String toString() {
        return "ApplicationImpl: " + applicationInfo.getName();
    }

}
