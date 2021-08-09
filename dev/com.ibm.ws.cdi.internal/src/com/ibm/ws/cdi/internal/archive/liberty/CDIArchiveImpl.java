/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
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
import java.util.Collections;
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
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ClientModuleInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo.Type;
import com.ibm.ws.container.service.app.deploy.InjectionClassList;
import com.ibm.ws.container.service.app.deploy.ModuleClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
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

import com.ibm.ws.container.service.annocache.Annotations;
import com.ibm.ws.container.service.annocache.AnnotationsBetaHelper;
import com.ibm.ws.container.service.annocache.CDIContainerAnnotations;
import com.ibm.wsspi.annocache.classsource.ClassSource_Factory;

public class CDIArchiveImpl extends AbstractCDIArchive implements CDIArchive {
    private static final String CLASS_NAME = CDIArchiveImpl.class.getSimpleName();

    // CDIArchives are created ...
    //
    // As module archives of an application:
    //
    // com.ibm.ws.cdi.internal.archive.liberty.ApplicationImpl.initModuleArchives()
    //   -- 'application' is the enclosing application
    //   -- 'containerInfo' is module container information
    //
    // As library archives of an application:
    //
    // com.ibm.ws.cdi.internal.archive.liberty.ApplicationImpl.initLibraryArchives()
    //
    // As extension archives:
    //
    // com.ibm.ws.cdi.internal.archive.liberty.ExtensionArchiveImpl.ExtensionArchiveImpl()
    //   -- 'application' is null
    //   -- 'containerInfo' is extension container information
    //
    // As module library archives:
    //
    // com.ibm.ws.cdi.internal.archive.liberty.CDIArchiveImpl.initModuleLibraryArchives()
    
    public CDIArchiveImpl(
        ApplicationImpl application,
        ContainerInfo containerInfo,
        ArchiveType archiveType,
        ClassLoader classLoader,
        RuntimeFactory factory) {

        super( containerInfo.getName(), factory.getServices() );

        this.factory = factory;

        this.application = application; // Null for an extension archive.

        this.containerInfo = containerInfo;
        this.type = archiveType;

        this.classLoader = classLoader;

        this.moduleLibraryArchives = initModuleLibraryArchives();
    }

    //

    private final RuntimeFactory factory;

    public RuntimeFactory getFactory() {
        return factory;
    }

    //

    private final ApplicationImpl application;

    @Override
    public Application getApplication() {
        return application;
    }

    //

    // Could be static ... but leave as instance so to keep the
    // trace associated with an instance.

    @Trivial
    private Container getContainer(Entry entry) {
        try {
            return entry.adapt(Container.class); // throws UnableToAdaptException
        } catch ( Throwable th ) {
            return null; // FFDC
        }
    }

    private Container getContainer(Container container, String path) {
        Entry startEntry = container.getEntry(path);
        if ( startEntry == null ) {
            return null;
        } else {
            return getContainer(startEntry);
        }
    }

    //

    private final ContainerInfo containerInfo;
    private String path; // The path to the root-of-roots.

    public ContainerInfo getContainerInfo() {
        return containerInfo;
    }

    public Container getContainer() {
        return containerInfo.getContainer();
    }

    @Override
    public String getPath() throws CDIException {
        if ( path == null ) {
            path = getPath( getContainer() );
        }
        return path;
    }

    private String getPath(Container useContainer) throws CDIException {
        StringBuilder pathBuilder = new StringBuilder();

        try {
            Entry entry;
            while ( (entry = useContainer.adapt(Entry.class)) != null ) {
                // 'adapt' throws UnableToAdaptException
                pathBuilder.insert(0,  entry.getPath() );
                useContainer = entry.getRoot();
            }
        } catch ( UnableToAdaptException e ) {
            // FFDC
            throw new CDIException(e);
        }

        return pathBuilder.toString();
    }

    @Override
    public Resource getResource(String path) {
        Entry entry = getContainer().getEntry(path);
        return ((entry == null) ? null : new ResourceImpl(entry));
    }

    @Override
    public boolean isModule() {
        return ( containerInfo instanceof ModuleContainerInfo );
    }

    //

    private final ArchiveType type;

    @Override
    public ArchiveType getType() {
        return type;
    }

    //

    private final ClassLoader classLoader;

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    //

    private final Collection<CDIArchive> moduleLibraryArchives;

    @Override
    public Collection<CDIArchive> getModuleLibraryArchives() {
        return moduleLibraryArchives;
    }

    private Collection<CDIArchive> initModuleLibraryArchives() {
        Map<ContainerInfo, CDIArchive> moduleLibraryArchives = new HashMap<ContainerInfo, CDIArchive>();

        if ( containerInfo instanceof ModuleClassesContainerInfo ) {
            ModuleClassesContainerInfo moduleClassesContainerInfo = (ModuleClassesContainerInfo) containerInfo;
            List<ContainerInfo> containerInfos = moduleClassesContainerInfo.getClassesContainerInfo();
            for ( ContainerInfo containerInfo : containerInfos ) {
                Type containerType = containerInfo.getType();

                if ( (containerType == Type.WEB_INF_LIB) ||
                     (containerType == Type.MANIFEST_CLASSPATH) ||
                     (containerType == Type.JAR_MODULE) ) {
                    ArchiveType archiveType = ContainerInfoTypeUtils.getType(containerType);
                    CDIArchive childArchive = factory.newArchive(application, containerInfo, archiveType, classLoader);
                    moduleLibraryArchives.put(containerInfo, childArchive);
                }
            }
        }

        return moduleLibraryArchives.values();
    }

    //

    private ExtendedModuleInfo extendedModuleInfo;
    private String appCallbackHandlerName;

    public ExtendedModuleInfo getModuleInfo() throws CDIException {
        if ( !isModule() ) {
            return null;
        }

        if ( extendedModuleInfo == null ) {
            NonPersistentCache moduleCache;

            ModuleContainerInfo moduleContainerInfo = (ModuleContainerInfo) containerInfo;
            Container moduleContainer = moduleContainerInfo.getContainer();
            try {
                moduleCache = moduleContainer.adapt(NonPersistentCache.class);
            } catch ( UnableToAdaptException e ) {
                // FFDC
                throw new CDIException(e);
            }

            extendedModuleInfo = (ExtendedModuleInfo) moduleCache.getFromCache(ModuleInfo.class);
        }

        return extendedModuleInfo;
    }

    private ClientModuleInfo getClientModuleInfo() throws CDIException {
        ModuleInfo moduleInfo = getModuleInfo(); // throws CDIException
        if ( !(moduleInfo instanceof ClientModuleInfo) ) {
            return null;
        } else {
            return (ClientModuleInfo) moduleInfo;
        }
    }
    
    public ModuleMetaData getModuleMetaData() throws CDIException {
        ExtendedModuleInfo useModuleInfo = getModuleInfo();
        return ( (useModuleInfo == null) ? null : useModuleInfo.getMetaData() );
    }

    @Override
    public MetaData getMetaData() throws CDIException {
        if ( isModule() ) {
            return getModuleMetaData();
        } else {
            return application.getApplicationMetaData();
        }
    }

    @Override
    public J2EEName getJ2EEName() throws CDIException {
        // TODO Can we come up with a J2EEName for libraries as well as modules?

        ModuleMetaData moduleMetaData = getModuleMetaData();
        return ( (moduleMetaData == null) ? null : moduleMetaData.getJ2EEName() );
    }

    @Override
    public String getClientModuleMainClass() throws CDIException {
        ClientModuleInfo clientModuleInfo = getClientModuleInfo(); // throws CDIException
        return ( (clientModuleInfo == null) ? null : clientModuleInfo.getMainClassName() );
    }

    @Override
    public String getClientAppCallbackHandlerName() throws CDIException {
        if ( appCallbackHandlerName == null ) {
            ClientModuleInfo clientModuleInfo = getClientModuleInfo(); // throws CDIException
            if ( clientModuleInfo != null ) {
                ApplicationClient appClient;
                try {
                    appClient = getContainer().adapt(ApplicationClient.class);
                } catch ( UnableToAdaptException e ) {
                    // FFDC
                    // This should never happen unless there's a parse error
                    // in the application-client.xml in which case the container
                    // should catch it first.
                    throw new CDIException(e);
                }
                if ( appClient != null ) {
                    appCallbackHandlerName = appClient.getCallbackHandler();
                }
            }
        }
        return appCallbackHandlerName;
    }

    //

    private Set<String> classNames;

    @Override
    public Set<String> getClassNames() {
        if ( classNames == null ) {
            Set<String> storage = new TreeSet<String>();

            Container container = containerInfo.getContainer();
            if ( type == ArchiveType.WEB_MODULE ) {
                container = getContainer(container, CDIUtils.WEB_INF_CLASSES);
            }
            if ( container != null ) {
                collectClassNames(container, null, storage);
            }

            classNames = storage;
        }

        return classNames;
    }

    @Trivial
    private void collectClassNames(Container container, String packageName, Set<String> storage) {
        for ( Entry entry : container ) {
            String entryName = entry.getName();

            if ( !entryName.endsWith(CDIUtils.CLASS_EXT) ) {
                // TODO: Is this correct for RAR files?
                //       A RAR can have nested JARs, which will be picked up
                //       by this loop.
                //       Conversion to a *local* container might be correct.

                Container entryContainer = getContainer(entry);
                if ( entryContainer != null ) {
                    String subPackageName;
                    if ( packageName == null ) {
                        subPackageName = entryName;
                    } else {
                        subPackageName = packageName + CDIUtils.DOT + entryName;
                    }
                    collectClassNames(entryContainer, subPackageName, storage);
                }

            } else {
                int classNameLength = entryName.length() - CDIUtils.CLASS_EXT_LENGTH;
                String className = entryName.substring(0, classNameLength);

                String qualifiedClassName;
                if ( packageName == null ) {
                    qualifiedClassName = className;
                } else {
                    qualifiedClassName = packageName + CDIUtils.DOT + className;
                }

                storage.add(qualifiedClassName);
            }
        }
    }

    //

    private List<String> jeeComponentClassNames;

    @Override
    public List<String> getInjectionClassList() throws CDIException {
        if ( jeeComponentClassNames == null ) {
            InjectionClassList injectionClassList;
            try {
                injectionClassList = getContainer().adapt(InjectionClassList.class);
            } catch ( UnableToAdaptException e ) {
                throw new CDIException(e);
            }
            jeeComponentClassNames = injectionClassList.getClassNames();
        }
        return jeeComponentClassNames;
    }

    //

    // TODO: Should an 'isSetAllBindings' flag be used?
    //       If no ManagedBeanBnd instance is available, 'allBindings' is
    //       left null, in which case subsequent calls will re-attempt to
    //       initialize the bindings.

    private ResourceInjectionBag allBindings;

    @Override
    public ResourceInjectionBag getAllBindings() throws CDIException {
        if ( allBindings == null ) {
            ManagedBeanBnd managedBeanBnd;
            try {
                managedBeanBnd = getContainer().adapt(ManagedBeanBnd.class);
            } catch (UnableToAdaptException e) {
                throw new CDIException(e);
            }
            
            if ( managedBeanBnd != null ) {
                ResourceRefConfigFactory resourceRefConfigFactory = factory.getServices().getResourceRefConfigFactory();

                allBindings = new ResourceInjectionBag(resourceRefConfigFactory.createResourceRefConfigList());

                List<ManagedBean> managedBeans = managedBeanBnd.getManagedBeans();
                if ( managedBeans != null ) {
                    for ( ManagedBean managedBean : managedBeans ) {
                        OSGiJNDIEnvironmentRefBindingHelper.processBndAndExt(
                            allBindings.allBindings,
                            allBindings.envEntryValues,
                            allBindings.resourceRefConfigList,
                            managedBean,
                            null);
                    }
                }
            }
        }
        return allBindings;
    }

    //

    @Override
    public Set<String> getAnnotatedClasses(Set<String> annotationClassNames) throws CDIException {
        if ( AnnotationsBetaHelper.getLibertyBeta() ) {
            return getAnnotatedClassesPostBeta(annotationClassNames);
        } else {
            return getAnnotatedClassesPreBeta(annotationClassNames);
        }
    }

    /**
     * Answer the application name for retrieving annotations.
     * 
     * This is the deployment name from the associated application information.
     * 
     * If no application information is available, default to use the name from 
     * the CDI application.
     * 
     * If no CDI application is available, answer the 'unnamed application' value.
     *
     * @return The application name for retrieving annotations.
     *
     * @throws CDIException Thrown if an error occurred while determining the
     *     application name.
     */
    public String getAnnoAppName() throws CDIException {
        String methodName = "getAnnoAppName";

        ApplicationInfo appInfo;
        ExtendedModuleInfo moduleInfo = getModuleInfo(); // throws CDIException
        if ( moduleInfo != null ) { // Null if not a module
            appInfo = moduleInfo.getApplicationInfo();
        } else if ( application != null ) { // Null if an extension archive
            appInfo = application.getApplicationInfo();
        } else {
            appInfo = null;
        }

        String annoAppName;
        if ( appInfo != null ) {
            annoAppName = appInfo.getDeploymentName();
            // System.out.println(methodName + ": ApplicationInfo [ " + appInfo + " ] [ " + appInfo.getClass().getName() + " ] [ " + annoAppName + " ]");
        } else {
            annoAppName = ClassSource_Factory.UNNAMED_APP;
            // System.out.println(methodName + ": ApplicationInfo [ unavailable ] [ " + annoAppName + " ]");
        }

        return annoAppName;
    }

    public Set<String> getAnnotatedClassesPostBeta(Set<String> annotationClassNames) throws CDIException {
        // ArchiveType:
        //   MANIFEST_CLASSPATH
        //   EAR_LIB
        //   WEB_INF_LIB
        //
        //   JAR_MODULE
        //   WEB_MODULE
        //   EJB_MODULE
        //   CLIENT_MODULE
        //   RAR_MODULE
        //
        //   SHARED_LIB
        //   ON_DEMAND_LIB
        //   RUNTIME_EXTENSION

    	String methodName = "getAnnotatedClassesPostBeta";

    	String appName = getAnnoAppName(); // throws CDIException
    	// System.out.println(methodName + " App: " + appName);
    	// System.out.println(methodName + " Type: " + type);

        // Handle WEB-INF/classes by providing an entry prefix to the
        // annotations information.  Keep the archive container as the
        // target container.

        Container archiveContainer = getContainer();
    	// System.out.println(methodName + " Container: " + archiveContainer);

        // When the archive is a root-of-roots, and the archive type
        // is a module type, change the archive path to the application name.
        //
        // Leave the archive path unassigned for other types.
        //
        // (Cases other than modules duplicate the application name,
        // which causes data from different containers to be mapped
        // to the same location in the archive.)

        String archivePath = getPath(archiveContainer);
    	// System.out.println(methodName + " Container Path: " + archivePath);

        if ( archivePath.isEmpty() ) { // Root-of-roots
            if ( (type == ArchiveType.WEB_MODULE) ||
                 (type == ArchiveType.WEB_MODULE) ||
                 (type == ArchiveType.EJB_MODULE) ||
                 (type == ArchiveType.CLIENT_MODULE) ||
                 (type == ArchiveType.RAR_MODULE) ) {
                archivePath = appName;
            } else {
                archivePath = null;
            }
        }

        String archiveEntryPrefix;
        if ( type == ArchiveType.WEB_MODULE ) {
            if ( getContainer(archiveContainer, CDIUtils.WEB_INF_CLASSES) == null ) {
                return Collections.emptySet();
            } else {
                archiveEntryPrefix = CDIUtils.WEB_INF_CLASSES;
            }
        } else {
            archiveEntryPrefix = null;
        }

        CDIContainerAnnotations cdiContainerAnnotations;
        try {
            cdiContainerAnnotations = archiveContainer.adapt(CDIContainerAnnotations.class);
        } catch ( UnableToAdaptException e ) {
            throw new CDIException(e);
        }

        // Supply persistence information, but only when the archive is associated
        // with an application (currently, only when the archive is an extension archive).
        //
        // Use the path as the module name, even for cases when the archive is not a module
        // type archive.
        //
        // Three general cases are generated?
        //
        // The archive is a module archive;
        // The archive is a non-module archive of the application;
        // The archive is a library archive of a web module.
        //
        // For all cases, the container path (from the root-of-roots, which should
        // be the application root) is the second half of the key to the results.
        // The first half of the key is the "CDI" category name, which is necessary
        // because CDI obtains module results somewhat differently than it obtains
        // JavaEE obtains module results.

        // The category name is necessary to distinguish processing driven by CDI from
        // processing driven by JavaEE.  In particular, CDI web module results contain
        // data from only WEB-INF/classes, not from the entire web module.

        // CDI annotations are always a single tier above their container data.
        // There is no point to caching the module level data.
        //
        // Also, CDI data occurs with the same names in different class loading contexts.
        // That is the result of turning manifest jars into CDI archives.  Each manifest
        // JAR can occur on the class path of more than one module.  For each module, the
        // manifest JAR has a different class loader, which means each manifest JAR as
        // as CDI archive must have its own scan results.
        //
        // The caching framework does not have a way to distinguish these results.

        cdiContainerAnnotations.setIsLightweight(Annotations.IS_LIGHTWEIGHT);

        if ( application != null ) {
            cdiContainerAnnotations.setUseJandex( application.getUseJandex() );

            cdiContainerAnnotations.setAppName(appName);
            if ( archivePath == null ) {
                cdiContainerAnnotations.setIsUnnamedMod(true);
            } else {
                cdiContainerAnnotations.setModName(archivePath);
            }

            if ( archiveEntryPrefix != null ) {
                cdiContainerAnnotations.setEntryPrefix(archiveEntryPrefix);
            }
        }

        // Complete inheritance information requires a class loader.
        ClassLoader useClassLoader = getClassLoader();
        if ( useClassLoader == null ) {
            String message =
                "CDI archive [ " + appName + " : " + archivePath + " ]:" +
                " Null class loader during query for inherited annotations [ " + annotationClassNames + " ]";
            throw new IllegalArgumentException(message);
        }
        cdiContainerAnnotations.setClassLoader(useClassLoader);

        return cdiContainerAnnotations.getClassesWithSpecifiedInheritedAnnotations(annotationClassNames);
    }

    //

    public Set<String> getAnnotatedClassesPreBeta(Set<String> annotationClassNames) throws CDIException {
        Set<String> annotatedClassNames = new HashSet<String>();

        Container container = getContainer();
        if ( getType() == ArchiveType.WEB_MODULE ) {
            container = getContainer(container, CDIUtils.WEB_INF_CLASSES);
            if ( container == null ) {
                return annotatedClassNames;
            }
        }

        com.ibm.ws.container.service.annotations.ContainerAnnotations containerAnnotations;
        try {
            containerAnnotations = container.adapt(com.ibm.ws.container.service.annotations.ContainerAnnotations.class);
        } catch ( UnableToAdaptException e ) {
            throw new CDIException(e);
        }

        boolean useJandex = false;
        if ( application != null ) {
            useJandex = application.getUseJandex();
        }

        List<String> useAnnotationClassNames = new ArrayList<String>(annotationClassNames);

        annotatedClassNames.addAll( containerAnnotations.getClassesWithSpecifiedInheritedAnnotations(useAnnotationClassNames, useJandex) );

        return annotatedClassNames;
    }
}
