/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
// import com.ibm.ws.javaee.dd.app.Application;
// import com.ibm.ws.javaee.dd.app.Module;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 * Common implementation for bindings and extensions adapters. 
 * 
 * Adaptation has two steps: Retrieving the descriptor from the module
 * or application, and retrieving overrides from the server configuration.
 * 
 * Answer whichever is retrieved.  If both are retrieved, answer the descriptor
 * obtained from the server configuration, with the descriptor obtained from
 * the module or application set as the delegate.
 * 
 * Two patterns are implemented by bindings and extensions adapters:
 * 
 * Managed beans, ejb, and web perform matching using configuration PID and
 * module name.
 * 
 * Application client, and application perform matching on configuration PID.
 * Module name is not available for these cases.
 *
 * Validation is performed when performing name matching.  Name based matching
 * expects the configuration element overrides to have unique module names which
 * match actual modules of the application.
 *
 * @param <ConfigType> The type of configuration element which is being adapted.
 */
public abstract class BndExtAdapter<ConfigType>
    implements ContainerAdapter<ConfigType> {

    protected static final TraceComponent tc = Tr.register(BndExtAdapter.class);

    // Subclass APIs ...

    /**
     * Each adapter is supplied with a collection of overrides which are read
     * out of the server configuration.
     */
    public abstract List<? extends ConfigType> getConfigurations();

    /**
     * Control parameter for {@link #parse(Container, Entry, boolean)}:
     * Use this as the <code>xmi</code> parameter value to document
     * that the parameter value is expected to be ignored.
     */
    protected static final boolean XMI_UNUSED = false;

    /**
     * Common parse step: Parse the specified entry.  Implementers
     * are expected to specialize to create a parser for the type
     * processed by this adapter.
     * 
     * Null should never be returned.  Any parsing failure should be
     * thrown as a {@link ParseException}.
     */
    protected abstract ConfigType parse(Container ddAdaptRoot, Entry ddEntry, boolean xmi)
            throws ParseException;

    //

    protected abstract String getParentPid(ConfigType config);
    protected abstract String getModuleName(ConfigType config);

    protected abstract String getElementTag();
    protected abstract Class<?> getCacheType();

    protected abstract void setDelegate(ConfigType config, ConfigType configDelegate);
    
    // Common processing steps ...

    /**
     * Standard processing step: Obtain the configuration override and
     * parse the bindings and extensions entry.  Answer one or the other.
     * When both are available, wire the entry data as the delegate of
     * the override data and answer the override data.
     */
    protected ConfigType process(
            Container ddRoot,
            OverlayContainer ddOverlay,
            ArtifactContainer ddArtifactRoot,
            Container ddAdaptRoot,
            String ddPath,
            boolean xmi) throws UnableToAdaptException {

        // Always try to get both the override and the parsed data.
        ConfigType fromConfig = getConfigOverrides(ddOverlay, ddArtifactRoot);
        ConfigType fromModule = parse(ddAdaptRoot, ddPath, xmi);

        // Answer whichever was obtained.  If both were obtained, set
        // the parsed data as the delegate of the override, and answer
        // the override.

        if ( fromConfig == null ) {
            return fromModule;
        } else {
            if ( fromModule != null ) {
                setDelegate(fromConfig, fromModule);
            }
            return fromConfig;
        }
    }

    /**
     * Retrieve the configuration override for a module descriptor.
     * 
     * Answer null if no override is available.
     * 
     * Set the module descriptor as the delegate of the configuration
     * override, if both are present.
     * 
     * @param ddOverlay The root overlay container of the descriptor.  This will be
     *     a module overlay.
     * @param ddArtifactRoot The root artifact container of the descriptor.  This will
     *     be a module container.
     *     
     * @return The selected configuration override.  Null if none is available.
     *
     * @throws UnableToAdaptException Thrown if application or module information
     *     cannot be retrieved.
     */
    protected ConfigType getConfigOverrides(
            OverlayContainer ddOverlay,
            ArtifactContainer ddArtifactRoot) throws UnableToAdaptException {

        List<? extends ConfigType> configurations = getConfigurations();
        if ( (configurations == null) || configurations.isEmpty() ) {
            return null;
        }

        String cachePath = ddArtifactRoot.getPath();

        // The logic, below, is convoluted:
        //
        // If the current container is an application container, application
        // information will be directly available, and no module information
        // will be available.
        //
        // If the current container is a module container, module information
        // should be directly available.
        //
        // Having neither application nor module information is unexpected.
        // Answer null in this case.
        //
        // Having obtained module information, if the module is within an
        // application, application info should be available from the module
        // info.
        //
        // Having no application info from the module info may be an error,
        // or may be usual for a stand-alone module.  In either case, since
        // overrides are obtained from the application configuration elements,
        // having no application information means no overrides can possibly
        // be available.

        ApplicationInfo appInfo = (ApplicationInfo)
            ddOverlay.getFromNonPersistentCache(cachePath, ApplicationInfo.class);        
        ModuleInfo moduleInfo = null;
        if ( (appInfo == null) && (ddOverlay.getParentOverlay() != null) ) {
            moduleInfo = (ModuleInfo)
                ddOverlay.getFromNonPersistentCache(cachePath, ModuleInfo.class);
            if ( moduleInfo == null ) {
                return null;
            }
            appInfo = moduleInfo.getApplicationInfo();
        }
        if ( (appInfo == null) || !(appInfo instanceof ExtendedApplicationInfo) ) {
            return null;
        }

        ExtendedApplicationInfo extAppInfo = (ExtendedApplicationInfo) appInfo;
        NestedConfigHelper configHelper = extAppInfo.getConfigHelper();
        if ( configHelper == null ) {
            return null;
        }
        String appServicePid = (String) configHelper.get("service.pid");
        String appExtendsPid = (String) configHelper.get("ibm.extends.source.pid");

        if ( moduleInfo == null ) {
            return getFirstConfig(appInfo, appServicePid, appExtendsPid);

        } else {
            Map<String, ConfigType> configs =
                getConfigOverrides(appInfo, ddOverlay, appServicePid, appExtendsPid);
            return ( (configs == null) ? null : configs.get( moduleInfo.getName() ) );
        }
    }

    /**
     * Retrieve the configuration override for a module descriptor.
     * 
     * Answer null if no override is available.
     * 
     * Case for when no module information is available.  No name matching
     * is performed: The first available matching configuration is used.
     * 
     * Matching is of the configuration parent PID against the application
     * service PID or extends PID.  There is a match if the parent PID equals
     * either of the application PIDs.
     *
     * @param appInfo Current application information.
     * @param appServicePid The application service PID.
     * @param appExtendsPid The application extends PID.
     *
     * @return The first matching configuration.  Null if no matching configuration
     *     is available.
     */
    protected ConfigType getFirstConfig(
            ApplicationInfo appInfo,
            String appServicePid, String appExtendsPid) {

        for ( ConfigType config : getConfigurations() ) {
            String parentPid = getParentPid(config);
            if ( appServicePid.equals(parentPid) || parentPid.equals(appExtendsPid) ) {
                return config;
            }
        }
        return null;
    }
  
     // TFB:
    //
    // Retrieval of module names, as implemented, is unreliable.
    // The implementation relies on the application descriptor, which
    // may be absent.  
    //
    // The completed modules list is computed by the deployed application. See:
    //
    // open-liberty/dev/com.ibm.ws.app.manager.module/src/
    //         com/ibm/ws/app/manager/module/internal/
    //             DeployedAppInfoBase.java
    //             SimpleDeployedAppInfoBase.java
    //
    // open-liberty/dev/com.ibm.ws.app.manager.war/src/
    //         com/ibm/ws/app/manager/ear/internal/
    //             EARDeployedAppInfo.java
    //
    // In particular, see:
    //         DeployedAppInfoBase.moduleContainerInfos
    //         EARDeployedAppInfo.createModuleContainerInfo
    //
    //         SimpleDeployedAppInfoBase.preDeployApp
    //         SimpleDeployedAppInfoBase$ModuleContainerInfoBase.createModuleMetaData
    //         ModuleHandlerBase.createModuleMetaData
    //
    // The current implementation fails with a NullPointerException for an application which
    // does not have a application deployment descriptor (META-INF/application.xml):
    //
    // Stack Dump = java.lang.NullPointerException
    //         at com.ibm.ws.javaee.ddmodel.common.BndExtAdapter.getModuleNames(BndExtAdapter.java:369)
    //         at com.ibm.ws.javaee.ddmodel.common.BndExtAdapter.getConfigOverrides(BndExtAdapter.java:344)
    //         at com.ibm.ws.javaee.ddmodel.common.BndExtAdapter.getConfigOverrides(BndExtAdapter.java:214)
    //         at com.ibm.ws.javaee.ddmodel.common.BndExtAdapter.process(BndExtAdapter.java:119)
    //         at com.ibm.ws.javaee.ddmodel.ejbbnd.EJBJarBndAdapter.adapt(EJBJarBndAdapter.java:81)
    //         at com.ibm.ws.javaee.ddmodel.ejbbnd.EJBJarBndAdapter.adapt(EJBJarBndAdapter.java:41)
    //         at com.ibm.ws.adaptable.module.internal.AdapterFactoryServiceImpl.adapt(AdapterFactoryServiceImpl.java:200)
    //         at com.ibm.ws.adaptable.module.internal.AdaptableContainerImpl.adapt(AdaptableContainerImpl.java:174)
    //         at com.ibm.ws.adaptable.module.internal.InterpretedContainerImpl.adapt(InterpretedContainerImpl.java:203)
    //         at com.ibm.ws.ejbcontainer.osgi.internal.ModuleInitDataFactory.createModuleInitData(ModuleInitDataFactory.java:559)
    //         at com.ibm.ws.ejbcontainer.osgi.internal.ModuleInitDataAdapter.createModuleInitData(ModuleInitDataAdapter.java:181)
    //         at com.ibm.ws.ejbcontainer.osgi.internal.ModuleInitDataAdapter.adapt(ModuleInitDataAdapter.java:122)
    //         at com.ibm.ws.ejbcontainer.osgi.internal.ModuleInitDataAdapter.adapt(ModuleInitDataAdapter.java:41)
    //         at com.ibm.ws.adaptable.module.internal.AdapterFactoryServiceImpl.adapt(AdapterFactoryServiceImpl.java:200)
    //         at com.ibm.ws.adaptable.module.internal.AdaptableContainerImpl.adapt(AdaptableContainerImpl.java:174)
    //         at com.ibm.ws.adaptable.module.internal.InterpretedContainerImpl.adapt(InterpretedContainerImpl.java:203)
    //         at com.ibm.ws.ejbcontainer.osgi.internal.EJBModuleRuntimeContainerImpl.createModuleMetaData(EJBModuleRuntimeContainerImpl.java:103)
    //         at com.ibm.ws.app.manager.module.internal.ModuleHandlerBase.createModuleMetaData(ModuleHandlerBase.java:63)
    //         at com.ibm.ws.app.manager.module.internal.SimpleDeployedAppInfoBase$ModuleContainerInfoBase.createModuleMetaData(SimpleDeployedAppInfoBase.java:189)
    //         at com.ibm.ws.app.manager.module.internal.SimpleDeployedAppInfoBase.preDeployApp(SimpleDeployedAppInfoBase.java:532)
    //         at com.ibm.ws.app.manager.module.internal.SimpleDeployedAppInfoBase.installApp(SimpleDeployedAppInfoBase.java:508)
    //         at com.ibm.ws.app.manager.module.internal.DeployedAppInfoBase.deployApp(DeployedAppInfoBase.java:349)
    //         at com.ibm.ws.app.manager.ear.internal.EARApplicationHandlerImpl.install(EARApplicationHandlerImpl.java:77)
    //         at com.ibm.ws.app.manager.internal.statemachine.StartAction.execute(StartAction.java:149)
    //         at com.ibm.ws.app.manager.internal.statemachine.ApplicationStateMachineImpl.enterState(ApplicationStateMachineImpl.java:1352)
    //
    // A correct implementation would provide the complete modules list.  That would introduce a new type
    // and new dependencies, and is not done at this time.
    
    public static final String MODULE_NAME_CHECKS = "module.name.checks";
    public static final String MODULE_NAME_NOT_SPECIFIED = "module.name.not.specified";
    public static final String MODULE_NAME_DUPLICATED = "module.name.duplicated";

    // public static final String MODULE_NAME_NOT_FOUND = "module.name.not.found";
    
    /**
     * Retrieve the configuration override for a module descriptor.
     * 
     * Answer null if no override is available.
     *
     * Case for when module information is available.  Name matching
     * is performed.
     * 
     * The first step of matching is of the configuration parent PID against
     * the application service PID or extends PID.  There is a match if the
     * parent PID equals either of the application PIDs.
     * 
     * In addition, the module name of the configuration must match the module name
     * obtained from the module information.
     *
     * As a side effect, validate the names of configuration elements that pass the
     * first step of matching (based on PID).  There are three validation tests:
     * First, the element must have a module name.  Second, the module name must match
     * a module name of the application.  Third, the module name must be unique.
     *
     * A record is made of whether validation has been performed.  That is necessary
     * to prevent the same error or warning messages being displayed multiple times.
     * The scope of validation is the configuration elements of a particular type for
     * a particular application.  Lookups are performed for each module of the
     * application, which means modules of the same type will perform the same
     * validations.
     *
     * @param appInfo Current application information.
     * @param ddOverlay The module overlay.  Note that message recording is done
     *     in the application overlay, which is the parent of this module overlay. 
     * @param appServicePid The application service PID.
     * @param appExtendsPid The application extends PID.
     *
     * @return The matching configuration.  Null if no matching configuration is found.
     */    
    @SuppressWarnings("unused")
    protected Map<String, ConfigType> getConfigOverrides(
            ApplicationInfo appInfo, OverlayContainer ddOverlay,
            String appServicePid, String appExtendsPid)
        throws UnableToAdaptException {

        Map<String, ConfigType> selectedConfigs = null;

        Set<String> dupModuleNames = null;
        int absentModuleNames = 0;

        for ( ConfigType config : getConfigurations() ) {
            String parentPid = getParentPid(config);
            if ( !appServicePid.equals(parentPid) && !parentPid.equals(appExtendsPid) ) {
                continue;
            }

            String moduleName = getModuleName(config);
            if ( moduleName == null ) {
                absentModuleNames++;
                continue;
            }

            moduleName = stripExtension(moduleName);
            if ( selectedConfigs == null ) {
                selectedConfigs = new HashMap<String, ConfigType>(1);
            }
            if ( selectedConfigs.put(moduleName, config) != null ) {
                if ( dupModuleNames == null ) {
                    dupModuleNames = new HashSet<String>(1);
                }
                dupModuleNames.add(moduleName);
            }
        }

        String elementTag = getElementTag();
        
        if ( !alreadyRecorded(ddOverlay, MODULE_NAME_CHECKS + elementTag) ) {
            String appName = null;

            if ( absentModuleNames != 0 ) {
                appName = getSimpleName(appInfo);
                Tr.error(tc, MODULE_NAME_NOT_SPECIFIED, appName, elementTag);
            }

            if ( dupModuleNames != null ) {
                if ( appName == null ) {
                    appName = getSimpleName(appInfo);
                }
                Tr.error(tc, MODULE_NAME_DUPLICATED, appName, elementTag, dupModuleNames);
            }

// TFB:See the comment, above.
//            
//            if ( selectedConfigs != null ) {
//                Set<String> appModuleNames = getModuleNames(appInfo);
//
//                Set<String> missingModuleNames = null;
//                for ( String moduleName : selectedConfigs.keySet() ) {
//                    if ( !appModuleNames.contains(moduleName) ) {
//                        if ( missingModuleNames == null ) {
//                            missingModuleNames = new HashSet<String>(1);
//                        }
//                        missingModuleNames.add(moduleName);
//                    }
//                }
//                if ( missingModuleNames != null ) {
//                    if ( appName == null ) {
//                        appName = getSimpleName(appInfo);
//                    }
//                    Tr.error(tc, MODULE_NAME_NOT_FOUND, appName, elementTag, missingModuleNames, appModuleNames);
//                }
//            }
        }

        return selectedConfigs;
    }

//    protected Set<String> getModuleNames(ApplicationInfo appInfo) throws UnableToAdaptException {
//        Application app = appInfo.getContainer().adapt(Application.class);
//        if ( app == null ) {
//            Tr.error(tc,  "BndExtAdapter.getModuleNames: Null application [ " + appInfo.getName() + " ]");
//            return new HashSet<>();
//        }
//        List<Module> modules = app.getModules();        
//        Set<String> moduleNames = new HashSet<String>( modules.size() );
//        for ( Module module : modules ) {
//            moduleNames.add( stripExtension( module.getModulePath() ) );
//        }
//        return moduleNames;
//    }

    protected String stripExtension(String moduleName) {
        if ( moduleName.endsWith(".war") || moduleName.endsWith(".jar") ) {
            return moduleName.substring(0, moduleName.length() - 4);
        } else {
            return moduleName;
        }
    }

    protected String getSimpleName(ApplicationInfo appInfo) {
        return DDParser.getSimpleName( appInfo.getContainer() );
    }

    protected boolean alreadyRecorded(OverlayContainer ddOverlay, String overlayMessage) {
        Class<?> cacheType = getCacheType();
        
        OverlayContainer appOverlay = ddOverlay.getParentOverlay();

        if ( appOverlay.getFromNonPersistentCache(overlayMessage, cacheType) != null) {
            return true;

        } else {
            appOverlay.addToNonPersistentCache(overlayMessage, cacheType, overlayMessage);
            return false;
        }
    }

    /**
     * Common parse step: Retrieve and parse a specified entry.
     * Answer null if the entry is not available.
     */
    @FFDCIgnore(ParseException.class)    
    protected ConfigType parse(Container ddAdaptRoot, String ddPath, boolean xmi)
            throws UnableToAdaptException {

        Entry ddEntry = ddAdaptRoot.getEntry(ddPath);
        if ( ddEntry == null ) {
            return null;
        }

        try {
            return parse(ddAdaptRoot, ddEntry, xmi);
        } catch ( ParseException e ) {
            // Don't process through FFDC: This exception will
            // be handled as a part of the UnableToAdaptException.
            throw new UnableToAdaptException(e);
        }
    }
    
    // 
    
    public String getAppVersion(Container ddAdaptRoot) throws UnableToAdaptException {
        return AppStructureHelper.getAppVersion(ddAdaptRoot);
    }

    public String getAppClientVersion(Container ddAdaptRoot) throws UnableToAdaptException {
        return AppStructureHelper.getAppClientVersion(ddAdaptRoot);
    }

    public boolean isWebModule(OverlayContainer ddOverlay, ArtifactContainer ddArtifactRoot) {
        return AppStructureHelper.isWebModule(ddOverlay, ddArtifactRoot);
    }    

    public String getWebVersion(Container ddAdaptRoot) throws UnableToAdaptException {
        return AppStructureHelper.getWebVersion(ddAdaptRoot);
    }

    public Integer getEJBVersion(Container ddAdaptRoot) throws UnableToAdaptException {
        return AppStructureHelper.getEJBVersion(ddAdaptRoot);
    }    
}
