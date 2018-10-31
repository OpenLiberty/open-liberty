/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.annotations.internal;

import com.ibm.websphere.ras.Tr;
import com.ibm.ws.container.service.annotations.ModuleAnnotations;
import com.ibm.ws.container.service.app.deploy.ApplicationClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.ModuleClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/*
 * Web module annotation service implementation.
 *
 * This implementation acts (in effect) as both a Future<AnnotationTargets_Targets>
 * and a Future<InfoStore>, with a three part resolution:
 *
 * 1) An initial adapt is performed on the root adaptable container of a module.
 *    Currently, the module must be a web module.
 *
 * 2) Completion parameters are assigned into the future: These are an application
 *    name, a module name, and a module root classloader.
 *
 * 3) The future is resolved through an appropriate getter.
 *
 * The implementation performs steps using web module rules.
 *
 * Note that the initial adapt call accepts four parameters.  The additional
 * parameters are accepted as debugging assists.
 *
 * The expected usage is for a target module to obtain an annotation services
 * object, and to retain a reference to that services object.
 *
 * The services object has retained state, which is shared between the two
 * obtainable objects.  That allows the class source (which has useful tables
 * of class lookup information) to be shared, and provides storage so that
 * multiple callers obtain the same target or info store objects.
 *
 * Current references are from:
 *
 * com.ibm.ws.webcontainer.osgi.DeployedModImpl.adapt(Class<T>)
 *
 * That adapt implementation provides three entries into the annotation
 * services:
 *
 * *) DeployedModule adapt to ClassSource_Aggregate
 * *) DeployedModule adapt to AnnotationTargets_Targets
 * *) DeployedModule adapt to ClassSource
 *
 * Notification plan:
 *
 * Adaptation to annotation targets requires a possibly time consuming scan.
 *
 * Informational messages are generated for the initiation of a scan, and for the
 * completion of a scan.
 */

// ClassesContainerInfo used by:
//
// com.ibm.ws.app.manager.ejb/src/com/ibm/ws/app/manager/ejb/internal/EJBDeployedAppInfo.java
// com.ibm.ws.app.manager.module/src/com/ibm/ws/app/manager/module/internal/DeployedAppInfoBase.java
// com.ibm.ws.app.manager.module/src/com/ibm/ws/app/manager/module/internal/SimpleDeployedAppInfoBase.java
// com.ibm.ws.app.manager.rar/src/com/ibm/ws/app/manager/rar/internal/RARDeployedAppInfo.java
// com.ibm.ws.app.manager.springboot/src/com/ibm/ws/app/manager/springboot/internal/SpringBootApplicationImpl.java
// com.ibm.ws.app.manager.springboot/src/com/ibm/ws/app/manager/springboot/support/SpringBootApplication.java
// com.ibm.ws.app.manager.war/src/com/ibm/ws/app/manager/ear/internal/EARDeployedAppInfo.java
// com.ibm.ws.app.manager.war/src/com/ibm/ws/app/manager/war/internal/WARDeployedAppInfo.java
// com.ibm.ws.cdi.internal/src/com/ibm/ws/cdi/internal/archive/liberty/ApplicationImpl.java
// com.ibm.ws.cdi.internal/src/com/ibm/ws/cdi/internal/archive/liberty/CDIArchiveImpl.java
// com.ibm.ws.classloading/src/com/ibm/ws/classloading/ClassLoaderConfigHelper.java
// com.ibm.ws.container.service/src/com/ibm/ws/container/service/annotations/internal/ModuleAnnotationsImpl.java
// com.ibm.ws.container.service/src/com/ibm/ws/container/service/app/deploy/ApplicationClassesContainerInfo.java
// com.ibm.ws.container.service/src/com/ibm/ws/container/service/app/deploy/extended/ApplicationInfoFactory.java
// com.ibm.ws.container.service/src/com/ibm/ws/container/service/app/deploy/extended/LibraryClassesContainerInfo.java
// com.ibm.ws.container.service/src/com/ibm/ws/container/service/app/deploy/internal/ApplicationInfoFactoryImpl.java
// com.ibm.ws.container.service/src/com/ibm/ws/container/service/app/deploy/ModuleClassesContainerInfo.java
// com.ibm.ws.ejbcontainer.remote/src/com/ibm/ws/ejbcontainer/remote/internal/EJBStubClassGeneratorImpl.java
// com.ibm.ws.jaxrs.2.0.server/src/com/ibm/ws/jaxrs20/server/component/JaxRsWebContainerManagerImpl.java
// com.ibm.ws.jaxws.webcontainer/src/com/ibm/ws/jaxws/webcontainer/JaxWsWebContainerManagerImpl.java
// com.ibm.ws.jpa.container/src/com/ibm/ws/jpa/container/osgi/internal/JPAComponentImpl.java
// com.ibm.ws.jsp/src/com/ibm/ws/jsp/taglib/SharedLibClassesContainerInfo.java
// com.ibm.ws.jsp/src/com/ibm/ws/jsp/taglib/SharedLibClassesContainerInfoAdapter.java
// com.ibm.ws.jsp/src/com/ibm/ws/jsp/taglib/TagLibraryCache.java
// com.ibm.ws.springboot.support.web.server/src/com/ibm/ws/springboot/support/web/server/internal/WebInstance.java

public class ModuleAnnotationsImpl extends AnnotationsImpl implements ModuleAnnotations {
    private static final String CLASS_NAME = ModuleAnnotationsImpl.class.getSimpleName();

    public ModuleAnnotationsImpl(
        AnnotationsAdapterImpl annotationsAdapter,
        Container rootContainer, OverlayContainer rootOverlayContainer,
        ArtifactContainer rootArtifactContainer, Container rootAdaptableContainer,
        ModuleInfo moduleInfo) throws UnableToAdaptException {

    	this( annotationsAdapter,
              rootContainer, rootOverlayContainer,
              rootArtifactContainer, rootAdaptableContainer,
              moduleInfo, ClassSource_Factory.UNSET_CATEGORY_NAME );
    }

    public ModuleAnnotationsImpl(
        AnnotationsAdapterImpl annotationsAdapter,
        Container rootContainer, OverlayContainer rootOverlayContainer,
        ArtifactContainer rootArtifactContainer, Container rootAdaptableContainer,
        ModuleInfo moduleInfo, String modCatName) throws UnableToAdaptException {

        super( annotationsAdapter,
               rootContainer, rootOverlayContainer,
               rootArtifactContainer, rootAdaptableContainer,
               moduleInfo.getApplicationInfo().getName(),
               AnnotationsImpl.getPath( moduleInfo.getContainer() ), // 'getPath' throws UnableToAdaptException
               modCatName );

        this.moduleInfo = moduleInfo;
        this.classLoader = moduleInfo.getClassLoader();

        this.appInfo = moduleInfo.getApplicationInfo();
    }

    //

    private final ModuleInfo moduleInfo;

    @Override
    public ModuleInfo getModuleInfo() {
        return moduleInfo;
    }

    //

    @Deprecated
    @Override
    public void addAppClassLoader(ClassLoader appClassLoader) {
        setClassLoader(appClassLoader);
    }

    //

    private final ApplicationInfo appInfo;

    @Override
    public ApplicationInfo getAppInfo() {
        return appInfo;
    }

    @Override
    public Container getAppContainer() {
        return getAppInfo().getContainer();
    }

    /**
     * Override: Retrieve the 'useJandex' setting from
     * the application information.
     * 
     * @return True or false telling if Jandex indexes are to be read.
     */
    @Override
    public boolean getUseJandex() {
        return getAppInfo().getUseJandex();
    }

    private ModuleClassesContainerInfo getModuleClassesContainerInfo() {
        Container container = getContainer();

        NonPersistentCache cache;
        try {
            cache = getAppContainer().adapt(NonPersistentCache.class);
            // 'adapt' throws UnableToAdaptException
        } catch ( UnableToAdaptException e ) {
            return null; // FFDC
        }
        ApplicationClassesContainerInfo appClassesInfo = (ApplicationClassesContainerInfo)
            cache.getFromCache(ApplicationClassesContainerInfo.class);

        // AppClassesInfo -> { ModuleClassesInfo -> { ContainerInfo -> Container } }
        // Find the module classes information which has a container which is the
        // same as the module container.

        for ( ModuleClassesContainerInfo moduleClassesInfo : appClassesInfo.getModuleClassesContainerInfo() ) {
            for ( ContainerInfo containerInfo : moduleClassesInfo.getClassesContainerInfo() ) {
                if ( containerInfo.getType() == ContainerInfo.Type.MANIFEST_CLASSPATH ) {
                    continue;
                }
                if ( !containerInfo.getContainer().equals(container) ) {
                    continue;
                }
                return moduleClassesInfo;
            }
        }

        return null; // Unexpected
    }

    //

    @Override
    protected void addInternalToClassSource() {
        if ( rootClassSource == null ) {
            return; // Nothing yet to do.
        }

        ClassSource_Factory classSourceFactory = getClassSourceFactory();
        if ( classSourceFactory == null ) {
            return;
        }

        Container moduleContainer = getContainer();

        Tr.debug(tc, CLASS_NAME +
             ": Module [ " + getAppName() + ":" + getModName() + " ][ " + moduleContainer + " ]:" +
             " Building internal class sources");

        ModuleClassesContainerInfo moduleClassesContainerInfo = getModuleClassesContainerInfo();

        if ( moduleClassesContainerInfo == null ) {
            // Tr.info(tc, CLASS_NAME + ": No classes container info: Using the module container.");

            // When there is no module classes container information, use the module container
            // itself as the classes container.
            //
            // Use the full path of the module container as the container name, except,
            // when the module container is a root of roots, use the module name as the
            // container name, since the the full path of the module is empty.

            String containerPath = getContainerPath(moduleContainer);
            if ( containerPath == null ) {
                return; // FFDC in 'getContainerPath'
            }
            if ( !addContainerClassSource(containerPath, moduleContainer) ) {
                return; // FFDC in 'addContainerClassSource'
            }

        } else {
            // These are the possible values of ContainerInfo.Type:
            //
            //   MANIFEST_CLASSPATH: Should not be present
            //   EAR_LIB: Should not be present
            //   SHARED_LIB: Should not be present
            //   WEB_MODULE: Should not be present; should be WEB_INF_CLASSES or WEB_INF_LIB

            //   WEB_INF_CLASSES: Must be prefixed to enable finding the JANDEX index
            //   WEB_INF_LIB: Normal; should be a root container

            //   EJB_MODULE: Normal; should be a root container
            //   CLIENT_MODULE: Normal; should be a root container
            //   RAR_MODULE: Normal; should be a root container
            //   JAR_MODULE: ?? Maybe, a JAR in a RAR module

            for ( ContainerInfo nextInfo : moduleClassesContainerInfo.getClassesContainerInfo() ) {
                Container nextContainer = nextInfo.getContainer();
                ContainerInfo.Type nextType = nextInfo.getType();

                if ( (nextType == ContainerInfo.Type.MANIFEST_CLASSPATH) ||
                     (nextType == ContainerInfo.Type.EAR_LIB) ||
                     (nextType == ContainerInfo.Type.SHARED_LIB) ) {

                    // While manifest class path entries, EAR libraries, and shared libraries
                    // can be on a module's class path, none of these is ever scanned directly,
                    // and should not be put into the class source containers.
                    //
                    // These will be handled as a part of the module class loader.

                    Tr.debug(tc, "Ignoring container [ " + nextContainer + " ] [ " + nextType + " ]");
                    continue;

                } else if ( nextType == ContainerInfo.Type.WEB_MODULE ) {
                    // A web module should never be specified itself as a classes container.

                    Tr.warning(tc, "Ignoring container [ " + nextContainer + " ] [ " + nextType + " ]: " +
                                   "Web modules should use WEB_INF_CLASSES");
                    continue;
                }

                String nextPrefix;

                if ( nextType == ContainerInfo.Type.WEB_INF_CLASSES ) {
                    // Handle WEB-INF/classes by providing the module container and a prefix for
                    // locating the WEB-INF/classes folder.  The module container is provided to
                    // give the annotations visibility to META-INF, which is where jandex indexes
                    // are stored.

                    nextContainer  = nextContainer.getEnclosingContainer().getEnclosingContainer();
                    nextPrefix = "WEB-INF/classes/";

                    Tr.debug(tc, CLASS_NAME + ": Handling type [ " + nextType + " ] with prefix [ " + nextPrefix + " ]");

                } else if ( (nextType == ContainerInfo.Type.WEB_INF_LIB) ||
                            (nextType == ContainerInfo.Type.EJB_MODULE) ||
                            (nextType == ContainerInfo.Type.CLIENT_MODULE) ||
                            (nextType == ContainerInfo.Type.RAR_MODULE) ||
                            (nextType == ContainerInfo.Type.JAR_MODULE) ) {

                    // These are the most usual cases.

                    nextPrefix = ClassSource_Factory.UNUSED_ENTRY_PREFIX;

                    Tr.debug(tc, CLASS_NAME + ": Handling type [ " + nextType + " ]");

                } else {
                    Tr.warning(tc, "Ignoring container [ " + nextContainer + " ] [ " + nextType + " ]: " + "unknown type");
                    return;
                }

                // A root-of-roots classes container should only be possible for
                // particular container types.  Testing for all types should not
                // be a problem.

                String nextPath = getContainerPath(nextContainer);
                if ( nextPath == null ) {
                    return; // FFDC in 'getContainerPath'
                }

                if ( !addContainerClassSource(nextPath, nextContainer, nextPrefix) ) {
                    return; // FFDC in 'addContainerClassSource'
                }
            }
        }
    }
}
