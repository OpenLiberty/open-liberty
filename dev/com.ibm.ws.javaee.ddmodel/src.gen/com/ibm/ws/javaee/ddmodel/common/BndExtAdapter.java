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
import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.app.Module;
import com.ibm.ws.javaee.ddmodel.DDParser;
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
public abstract class BndExtAdapter<ConfigType> {
    private static final TraceComponent tc = Tr.register(BndExtAdapter.class);

    //
    
    protected abstract String getParentPid(ConfigType config);
    protected abstract String getModuleName(ConfigType config);
    
    protected abstract String getElementTag();
    protected abstract Class<?> getCacheType();

    protected abstract void setDelegate(ConfigType config, ConfigType configDelegate);

    //

    /**
     * Retrieve the configuration override for a module descriptor.
     * 
     * Answer null if no override is available.
     * 
     * Set the module descriptor as the delegate of the configuration
     * override, if both are present.
     * 
     * @param configurations The available configuration overrides.
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
    public <CacheType> ConfigType getConfigOverrides(
            List<ConfigType> configurations,
            OverlayContainer ddOverlay,
            ArtifactContainer ddArtifactRoot) throws UnableToAdaptException {

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

        NestedConfigHelper configHelper = ((ExtendedApplicationInfo) appInfo).getConfigHelper();
        if ( configHelper == null ) {
            return null;
        }
        String appServicePid = (String) configHelper.get("service.pid");
        String appExtendsPid = (String) configHelper.get("ibm.extends.source.pid");

        if ( moduleInfo == null ) {
            return getFirstConfig(
                    configurations,
                    appInfo, appServicePid, appExtendsPid);

        } else {
            Map<String, ConfigType> configs = getConfigOverrides(
                    configurations,
                    appInfo,
                    ddOverlay, appServicePid, appExtendsPid);

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
     * @param configurations The available configuration overrides.
     * @param appInfo Current application information.
     * @param appServicePid The application service PID.
     * @param appExtendsPid The application extends PID.
     *
     * @return The first matching configuration.  Null if no matching configuration
     *     is available.
     */
    public ConfigType getFirstConfig(
            List<ConfigType> configurations,
            ApplicationInfo appInfo,
            String appServicePid, String appExtendsPid) {

        for ( ConfigType config : configurations ) {
            String parentPid = getParentPid(config);
            if ( appServicePid.equals(parentPid) || parentPid.equals(appExtendsPid) ) {
                return config;
            }
        }
        return null;
    }
    
    private static final String MODULE_NAME_CHECKS = "module.name.checks";
    private static final String MODULE_NAME_NOT_SPECIFIED = "module.name.not.specified";
    private static final String MODULE_NAME_NOT_FOUND = "module.name.not.found";
    private static final String MODULE_NAME_DUPLICATED = "module.name.duplicated";

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
     * @param configurations The available configuration overrides.
     * @param appInfo Current application information.
     * @param ddOverlay The module overlay.  Note that message recording is done
     *     in the application overlay, which is the parent of this module overlay. 
     * @param appServicePid The application service PID.
     * @param appExtendsPid The application extends PID.
     *
     * @return The matching configuration.  Null if no matching configuration is found.
     */    
    public Map<String, ConfigType> getConfigOverrides(
            List<ConfigType> configurations,
            ApplicationInfo appInfo, OverlayContainer ddOverlay,
            String appServicePid, String appExtendsPid)
        throws UnableToAdaptException {

        Map<String, ConfigType> selectedConfigs = null;

        Set<String> dupModuleNames = null;
        int absentModuleNames = 0;

        for ( ConfigType config : configurations ) {
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
                Tr.warning(tc, MODULE_NAME_NOT_SPECIFIED, appName, elementTag);
            }

            if ( dupModuleNames != null ) {
                if ( appName == null ) {
                    appName = getSimpleName(appInfo);
                }
                Tr.warning(tc, MODULE_NAME_DUPLICATED, appName, elementTag, dupModuleNames);
            }

            if ( selectedConfigs != null ) {
                Set<String> appModuleNames = getModuleNames(appInfo);

                Set<String> missingModuleNames = null;
                for ( String moduleName : selectedConfigs.keySet() ) {
                    if ( !appModuleNames.contains(moduleName) ) {
                        if ( missingModuleNames == null ) {
                            missingModuleNames = new HashSet<String>(1);
                        }
                        missingModuleNames.add(moduleName);
                    }
                }
                if ( missingModuleNames != null ) {
                    if ( appName == null ) {
                        appName = getSimpleName(appInfo);
                    }
                    Tr.warning(tc, MODULE_NAME_NOT_FOUND, appName, elementTag, missingModuleNames, appModuleNames);
                }
            }
        }

        return selectedConfigs;
    }

    //

    private String getSimpleName(ApplicationInfo appInfo) {
        return DDParser.getSimpleName( appInfo.getContainer() );
    }

    private Set<String> getModuleNames(ApplicationInfo appInfo) throws UnableToAdaptException {
        Application app = appInfo.getContainer().adapt(Application.class);
        List<Module> modules = app.getModules();        
        Set<String> moduleNames = new HashSet<String>( modules.size() );
        for ( Module module : modules ) {
            moduleNames.add( stripExtension( module.getModulePath() ) );
        }
        return moduleNames;
    }

    private String stripExtension(String moduleName) {
        if ( moduleName.endsWith(".war") || moduleName.endsWith(".jar") ) {
            return moduleName.substring(0, moduleName.length() - 4);
        } else {
            return moduleName;
        }
    }

    private boolean alreadyRecorded(OverlayContainer ddOverlay, String overlayMessage) {
        Class<?> cacheType = getCacheType();
        
        OverlayContainer appOverlay = ddOverlay.getParentOverlay();

        if ( appOverlay.getFromNonPersistentCache(overlayMessage, cacheType) != null) {
            return true;

        } else {
            appOverlay.addToNonPersistentCache(overlayMessage, cacheType, overlayMessage);
            return false;
        }
    }
}
