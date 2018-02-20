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
package com.ibm.ws.cdi.internal.archive.liberty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.archive.AbstractCDIArchive;
import com.ibm.ws.cdi.internal.interfaces.Application;
import com.ibm.ws.cdi.internal.interfaces.ArchiveType;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.CDIUtils;
import com.ibm.ws.cdi.internal.interfaces.Resource;
import com.ibm.ws.cdi.internal.interfaces.ResourceInjectionBag;
import com.ibm.ws.container.service.annotations.ContainerAnnotations;
import com.ibm.ws.container.service.app.deploy.ClientModuleInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo.Type;
import com.ibm.ws.container.service.app.deploy.InjectionClassList;
import com.ibm.ws.container.service.app.deploy.ModuleClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ModuleContainerInfo;
import com.ibm.ws.injectionengine.osgi.util.OSGiJNDIEnvironmentRefBindingHelper;
import com.ibm.ws.javaee.dd.client.ApplicationClient;
import com.ibm.ws.javaee.dd.managedbean.ManagedBean;
import com.ibm.ws.javaee.dd.managedbean.ManagedBeanBnd;
import com.ibm.ws.resource.ResourceRefConfigFactory;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

public class CDIArchiveImpl extends AbstractCDIArchive implements CDIArchive {

    private final ContainerInfo containerInfo;
    private final RuntimeFactory factory;
    private final ArchiveType type;
    private final ClassLoader classLoader;
    private final ApplicationImpl application;
    private Set<String> classNames;
    private List<String> jeeComponentClassNames;
    private ExtendedModuleInfo moduleInfo;
    private String appCallbackHandlerName;
    private ResourceInjectionBag allBindings;
    private String path;
    private final Collection<CDIArchive> moduleLibraryArchives;

    public CDIArchiveImpl(ApplicationImpl application,
                          ContainerInfo containerInfo,
                          ArchiveType archiveType,
                          ClassLoader classLoader,
                          RuntimeFactory factory) {
        super(containerInfo.getName(), factory.getServices());
        this.application = application;
        this.containerInfo = containerInfo;
        this.factory = factory;
        this.type = archiveType;
        this.classLoader = classLoader;
        this.moduleLibraryArchives = initModuleLibraryArchives();
    }

    /**
     * {@inheritDoc}
     *
     * @throws CDIException
     */
    @Override
    public J2EEName getJ2EEName() throws CDIException {
        //TODO can we come up with a J2EEName for libraries as well as modules?
        J2EEName j2eeName = null;
        ModuleMetaData moduleMetaData = getModuleMetaData();
        if (moduleMetaData != null) {
            j2eeName = moduleMetaData.getJ2EEName();
        }
        return j2eeName;
    }

    /** {@inheritDoc} */
    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getClassNames() {
        if (this.classNames == null) {
            Container container = containerInfo.getContainer();

            if (type == ArchiveType.WEB_MODULE) {
                container = getSubContainer(container, CDIUtils.WEB_INF_CLASSES);
            }

            classNames = scanForAllClassNames(container, null);
        }

        return classNames;
    }

    @Trivial
    private Set<String> scanForAllClassNames(Container container, String packageName) {
        Set<String> classNames = new TreeSet<String>();
        String entryPath = null;

        if (container != null) {
            for (Entry entry : container) {
                String entryName = entry.getName();

                if (packageName == null) {
                    entryPath = entryName;
                } else {
                    entryPath = packageName + CDIUtils.DOT + entryName;
                }
                //if the entry has a .class extension include it
                if (entryPath.endsWith(CDIUtils.CLASS_EXT)) {
                    int classNameLength = entryPath.length() - CDIUtils.CLASS_EXT_LENGTH;
                    String className = entryPath.substring(0, classNameLength);
                    classNames.add(className);
                } else {
                    Container entryContainer = getContainer(entry);
                    if (entryContainer != null) {
                        classNames.addAll(scanForAllClassNames(entryContainer, entryPath));
                    }
                }
            }
        }

        return classNames;

    }

    private Container getSubContainer(Container container, String path) {
        Container subContainer = null;
        Entry startEntry = container.getEntry(path);
        if (startEntry != null) {
            subContainer = getContainer(startEntry);
        }
        return subContainer;
    }

    @Trivial
    private Container getContainer(Entry entry) {
        Container container = null;
        try {
            container = entry.adapt(Container.class);
        } catch (Throwable thrown) {
            //ffdc and carry on
        }
        return container;
    }

    /** {@inheritDoc} */
    @Override
    public Resource getResource(String path) {

        Entry entry = getContainer().getEntry(path);
        Resource resource = entry == null ? null : new ResourceImpl(entry);

        return resource;
    }

    /** {@inheritDoc} */
    @Override
    public ArchiveType getType() {
        return type;
    }

    /**
     * @return
     */
    public ContainerInfo getContainerInfo() {
        return containerInfo;
    }

    /**
     * @return
     */
    public RuntimeFactory getFactory() {
        return factory;
    }

    /**
     * @return
     */
    public Container getContainer() {
        return containerInfo.getContainer();
    }

    /**
     * @return
     * @throws CDIException
     */
    public ModuleMetaData getModuleMetaData() throws CDIException {

        ModuleMetaData moduleMetaData = null;

        if (isModule()) {
            ExtendedModuleInfo moduleInfo = getModuleInfo();
            if (moduleInfo != null) {
                moduleMetaData = moduleInfo.getMetaData();
            }
        }

        return moduleMetaData;
    }

    public ExtendedModuleInfo getModuleInfo() throws CDIException {

        if (this.moduleInfo == null) {

            if (containerInfo instanceof ModuleContainerInfo) {
                try {
                    ModuleContainerInfo ci = (ModuleContainerInfo) containerInfo;
                    NonPersistentCache cache = ci.getContainer().adapt(NonPersistentCache.class);
                    moduleInfo = (ExtendedModuleInfo) cache.getFromCache(ModuleInfo.class);
                } catch (UnableToAdaptException e) {
                    throw new CDIException(e);
                }
            }
        }

        return moduleInfo;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isModule() {
        return containerInfo instanceof ModuleContainerInfo;
    }

    /** {@inheritDoc} */
    @Override
    public Application getApplication() {
        return application;
    }

    @Override
    public String getClientModuleMainClass() throws CDIException {
        String mainClassName = null;
        ModuleInfo moduleInfo = getModuleInfo();
        if (moduleInfo != null && moduleInfo instanceof ClientModuleInfo) {
            ClientModuleInfo clientModuleInfo = (ClientModuleInfo) moduleInfo;
            mainClassName = clientModuleInfo.getMainClassName();
        }
        return mainClassName;
    }

    @Override
    public String getClientAppCallbackHandlerName() throws CDIException {
        if (this.appCallbackHandlerName == null) {

            ModuleInfo moduleInfo = getModuleInfo();
            if (moduleInfo != null && moduleInfo instanceof ClientModuleInfo) {
                try {
                    ApplicationClient appClientXml = getContainer().adapt(ApplicationClient.class);
                    if (appClientXml != null) {
                        appCallbackHandlerName = appClientXml.getCallbackHandler();
                    }
                } catch (UnableToAdaptException e) {
                    // This should never happen unless there's a parse error in the application-client.xml
                    // in which case the Container should catch it first
                    throw new CDIException(e);
                }
            }
        }

        return appCallbackHandlerName;
    }

    @Override
    public List<String> getInjectionClassList() throws CDIException {
        if (this.jeeComponentClassNames == null) {
            try {
                InjectionClassList injectionClassList = getContainer().adapt(InjectionClassList.class);
                jeeComponentClassNames = injectionClassList.getClassNames();
            } catch (UnableToAdaptException e) {
                throw new CDIException(e);
            }
        }
        return jeeComponentClassNames;
    }

    /**
     * {@inheritDoc}
     *
     * @throws CDIException
     */
    @Override
    public MetaData getMetaData() throws CDIException {
        MetaData metaData = null;
        if (isModule()) {
            metaData = getModuleMetaData();
        } else {
            metaData = application.getApplicationMetaData();
        }
        return metaData;
    }

    @Override
    public ResourceInjectionBag getAllBindings() throws CDIException {
        if (this.allBindings == null) {

            try {
                ManagedBeanBnd managedBeanBnd = getContainer().adapt(ManagedBeanBnd.class);
                if (managedBeanBnd != null) {
                    ResourceRefConfigFactory resourceRefConfigFactory = factory.getServices().getResourceRefConfigFactory();
                    allBindings = new ResourceInjectionBag(resourceRefConfigFactory.createResourceRefConfigList());

                    List<ManagedBean> mbs = managedBeanBnd.getManagedBeans();
                    if (mbs != null) {
                        for (ManagedBean mb : mbs) {
                            OSGiJNDIEnvironmentRefBindingHelper.processBndAndExt(allBindings.allBindings,
                                                                                 allBindings.envEntryValues,
                                                                                 allBindings.resourceRefConfigList,
                                                                                 mb, null);
                        }
                    }
                }
            } catch (UnableToAdaptException e) {
                throw new CDIException(e);
            }
        }
        return allBindings;

    }

    /**
     * {@inheritDoc}
     *
     * @throws CDIException
     */
    @Override
    public String getPath() throws CDIException {
        if (this.path == null) {
            try {
                Entry entry = getContainer().adapt(Entry.class);
                path = "";
                while (entry != null) {
                    path = entry.getPath() + path;
                    entry = entry.getRoot().adapt(Entry.class);
                }
            } catch (UnableToAdaptException e) {
                throw new CDIException(e);
            }
        }
        return path;
    }

    /**
     * {@inheritDoc}
     *
     * @throws CDIException
     */
    @Override
    public Collection<CDIArchive> getModuleLibraryArchives() {
        return moduleLibraryArchives;
    }

    private Collection<CDIArchive> initModuleLibraryArchives() {

        Map<ContainerInfo, CDIArchive> moduleLibraryArchives = new HashMap<ContainerInfo, CDIArchive>();

        if (containerInfo instanceof ModuleClassesContainerInfo) {
            ModuleClassesContainerInfo moduleClassesContainerInfo = (ModuleClassesContainerInfo) containerInfo;
            List<ContainerInfo> containerInfos = moduleClassesContainerInfo.getClassesContainerInfo();
            for (ContainerInfo child : containerInfos) {
                Type childType = child.getType();

                if (childType == Type.WEB_INF_LIB ||
                    childType == Type.MANIFEST_CLASSPATH ||
                    childType == Type.JAR_MODULE) {
                    ArchiveType archiveType = ContainerInfoTypeUtils.getType(childType);
                    CDIArchive childArchive = factory.newArchive(application, child, archiveType, classLoader);
                    moduleLibraryArchives.put(child, childArchive);
                }
            }
        }

        return moduleLibraryArchives.values();
    }

    @Override
    public Set<String> getAnnotatedClasses(Set<String> annotations) throws CDIException {

        Set<String> classNames = new HashSet<String>();

        Container container = getContainer();
        if (type == ArchiveType.WEB_MODULE) {
            container = getSubContainer(container, CDIUtils.WEB_INF_CLASSES);
        }

        if (container != null) {
            Set<String> annotatedClasses = getClassesWithSpecifiedInheritedAnnotations(container, annotations);
            classNames.addAll(annotatedClasses);
        }

        return classNames;
    }

    private Set<String> getClassesWithSpecifiedInheritedAnnotations(Container container, Set<String> annotations) throws CDIException {
        ContainerAnnotations containerAnnotations;
        try {
            containerAnnotations = container.adapt(ContainerAnnotations.class);
        } catch (UnableToAdaptException e) {
            throw new CDIException(e);
        }
        Set<String> annotatedClasses = containerAnnotations.getClassesWithSpecifiedInheritedAnnotations(new ArrayList<String>(annotations), application.getUseJandex());
        return annotatedClasses;
    }
}
