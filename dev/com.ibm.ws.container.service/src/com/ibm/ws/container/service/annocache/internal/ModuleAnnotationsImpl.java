/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.annocache.internal;

import com.ibm.websphere.ras.Tr;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

import com.ibm.wsspi.annocache.classsource.ClassSource_Factory;

import com.ibm.ws.container.service.app.deploy.ApplicationClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.ModuleClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;

import com.ibm.ws.container.service.annocache.ModuleAnnotations;

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

    // The application name must be carefully obtained:
    //
    // In practice, there are up to several names which are available:
    //
    // A name attribute supplied by the application element of the server configuration.
    //
    // A name supplied by the application descriptor.
    //
    // The base name of the application archive (as specified by the location attribute
    //   of the application element).
    //
    // The J2EE name of the application, which is:
    //    The name attribute from the application element, or, if that is absent,
    //    The base name of the application archive.
    //
    // The disambiguated name of the application, which is:
    //    The name from the application descriptor, or, if that is absent,
    //    The base name of the application archive,
    // And to which a number may be added if the resulting name is not
    // unique to the server runtime.
    //
    // This implementation uses the J2EE name.
    //
    // The other location which generates an application name for use by the
    // cache is:
    //   com.ibm.ws.app.manager.ear.internal.EARDeployedAppInfo.
    //      hasAnnotations(Container, Collection<String>)
    // That location uses (in effect) the J2EE name of the application.

    public static String getAppName(ModuleInfo moduleInfo) {
        String methodName = "getAppName";

        ApplicationInfo appInfo = moduleInfo.getApplicationInfo();
        String appName = appInfo.getName();
        String appDepName = appInfo.getDeploymentName();
        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, methodName + ": AppName [ " + appName + " ] AppDepName [ " + appDepName + " ] (using AppDepName)");
        }

        // System.out.println(methodName + ": ApplicationInfo [ " + appInfo + " ] [ " + appInfo.getClass().getName() + " ] [ " + appDepName + " ]");

        return appDepName;
    }

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

//    public void debug(String text) {
//        System.out.println("ModuleAnnotations.<init>: " + text);
//    }
//
//    public void debug(String text, Object value) {
//        System.out.println("ModuleAnnotations.<init>: " + text + ": [ " + value + " ]");
//    }

    @SuppressWarnings("unused")
    public ModuleAnnotationsImpl(
        AnnotationsAdapterImpl annotationsAdapter,
        Container rootContainer, OverlayContainer rootOverlayContainer,
        ArtifactContainer rootArtifactContainer, Container rootAdaptableContainer,
        ModuleInfo moduleInfo, String modCatName) throws UnableToAdaptException {

        super( annotationsAdapter,
               rootContainer, rootOverlayContainer,
               rootArtifactContainer, rootAdaptableContainer,
               ModuleAnnotationsImpl.getAppName(moduleInfo),
               !ClassSource_Factory.IS_UNNAMED_MOD,
               moduleInfo.getName(),
               // AnnotationsImpl.getPath( moduleInfo.getContainer() ), // 'getPath' throws UnableToAdaptException
               modCatName );

        this.moduleInfo = moduleInfo;
//        debug("Module info", this.moduleInfo);
//        debug("Module info class", this.moduleInfo.getClass().getName());
//        debug("Module info name", this.moduleInfo.getName());
//        debug("Module info URI", this.moduleInfo.getURI());
//        debug("Module info container", this.moduleInfo.getContainer());

        this.classLoader = moduleInfo.getClassLoader();
        
        this.appInfo = moduleInfo.getApplicationInfo();
//        debug("App info", this.appInfo);
//        debug("App info class", this.appInfo.getClass().getName());
//        debug("App info name", this.appInfo.getName());
//        debug("App info deployment name", this.appInfo.getDeploymentName());
//        debug("App info container", this.appInfo.getContainer());
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

    /*
    [2/9/19 13:36:03:791 EST] 00000036 id=4016f9d5 ainer.service.app.deploy.internal.ApplicationInfoFactoryImpl > 
    createEARApplicationInfo Entry
    EJB_500
    HugeEJBs_500
    com.ibm.ws.adaptable.module.internal.InterpretedContainerImpl@fb995bd0
    com.ibm.ws.app.manager.ear.internal.EARDeployedAppInfo@59933dcd
    com.ibm.ws.app.manager.module.ApplicationNestedConfigHelper@e065e7a1
    com.ibm.ws.app.manager.internal.ApplicationInstallInfo@9806c854
    null
    com.ibm.ws.app.manager.ear.internal.EARDeployedAppInfo@59933dcd

  -- "EJB_500", which is labelled as "appName", is the name from server.xml
  -- "HugeEJBs_500", which is labelled as "preferredName" is either the name from application.xml or from the application archive.

  Within the factory method:

      J2EEName j2eeName = j2eeNameFactory.getService().create(appName, null, null);
      String name = reserveName(preferredName);

      ExtendedApplicationInfo appInfo = new ApplicationInfoImpl(name, j2eeName, container, configHelper, applicationInformation);

  -- "appName" is used to generate the J2EE name of the application.
  -- "preferredName" is disambiguated and is used as the name of the application.

  [2/9/19 13:36:03:791 EST] 00000036 id=00000000 ws.container.service.app.deploy.internal.ApplicationInfoImpl >
    <init> Entry  
    HugeEJBs_500
    EJB_500
    com.ibm.ws.adaptable.module.internal.InterpretedContainerImpl@fb995bd0
    com.ibm.ws.app.manager.module.ApplicationNestedConfigHelper@e065e7a1
    com.ibm.ws.app.manager.internal.ApplicationInstallInfo@9806c854

  -- "HugeEjbs_500", which was obtained from application.xml or from the application archive,
     is set as the "name" of the application information.
  -- "EJB_500" is used to create metadata for the application.

    ApplicationInfoImpl(String appName, J2EEName j2eeName, ...
      this.appName = appName;
      this.appMetaData = new ApplicationMetaDataImpl(j2eeName);
      */    
}
