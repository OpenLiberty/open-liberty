/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// NOTE: This is a generated file. Do not edit it directly.
package com.ibm.ws.javaee.ddmodel.ejbext;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.app.Module;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.ejbext.EJBJarExt;

import org.osgi.service.component.annotations.*;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
    service = ContainerAdapter.class,
    property = { "service.vendor=IBM",
                 "toType=com.ibm.ws.javaee.dd.ejbext.EJBJarExt" })
public class EJBJarExtAdapter implements ContainerAdapter<EJBJarExt> {
    private static final TraceComponent tc = Tr.register(EJBJarExtAdapter.class);

    public static final String XMI_EXT_IN_EJB_MOD_NAME = "META-INF/ibm-ejb-jar-ext.xmi";
    public static final String XML_EXT_IN_EJB_MOD_NAME = "META-INF/ibm-ejb-jar-ext.xml";
    public static final String XMI_EXT_IN_WEB_MOD_NAME = "WEB-INF/ibm-ejb-jar-ext.xmi";
    public static final String XML_EXT_IN_WEB_MOD_NAME = "WEB-INF/ibm-ejb-jar-ext.xml";

    private static final String MODULE_NAME_INVALID = "module.name.invalid";
    private static final String MODULE_NAME_NOT_SPECIFIED = "module.name.not.specified";

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    volatile List<EJBJarExt> configurations;

    @Override
    @FFDCIgnore(ParseException.class)
    public EJBJarExt adapt(
            Container ddRoot,
            OverlayContainer rootOverlay, ArtifactContainer artifactContainer,
            Container ddAdaptRoot) throws UnableToAdaptException {

        EJBJar ejbJar = ddAdaptRoot.adapt(EJBJar.class);
        boolean xmi = ( (ejbJar != null) && ejbJar.getVersionID() < EJBJar.VERSION_3_0 );

        String ddEntryName;
        if ( rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), WebModuleInfo.class) == null) {
            ddEntryName = xmi ? XMI_EXT_IN_EJB_MOD_NAME : XML_EXT_IN_EJB_MOD_NAME;
        } else {
            ddEntryName = xmi ? XMI_EXT_IN_WEB_MOD_NAME : XML_EXT_IN_WEB_MOD_NAME;
        }

        EJBJarExtComponentImpl fromConfig = getConfigOverrides(rootOverlay, artifactContainer, ddEntryName);

        Entry ddEntry = ddAdaptRoot.getEntry(ddEntryName);
        if ( ddEntry == null ) { 
            return fromConfig;
        }

        EJBJarExt fromApp;
        try {
            fromApp = new EJBJarExtDDParser(ddAdaptRoot, ddEntry, xmi).parse();
        } catch ( ParseException e ) {
            throw new UnableToAdaptException(e);
        }
        
        if ( fromConfig == null ) {
            return fromApp;
        } else {  
            fromConfig.setDelegate(fromApp);
            return fromConfig;
        }
    }

    private EJBJarExtComponentImpl getConfigOverrides(
            OverlayContainer rootOverlay,
            ArtifactContainer artifactContainer,
            String ddEntryPath) throws UnableToAdaptException {

        if ( (configurations == null) || configurations.isEmpty() ) {
            return null;
        }

        String cachePath = artifactContainer.getPath();

        ApplicationInfo appInfo = (ApplicationInfo)
            rootOverlay.getFromNonPersistentCache(cachePath, ApplicationInfo.class);
        ModuleInfo moduleInfo = null;

        if ( (appInfo == null) && (rootOverlay.getParentOverlay() != null) ) {
            moduleInfo = (ModuleInfo) rootOverlay.getFromNonPersistentCache(cachePath, ModuleInfo.class);
            if ( moduleInfo == null ) {
                return null;
            }
            appInfo = moduleInfo.getApplicationInfo();
        }
        if ( (appInfo == null) || !(appInfo instanceof ExtendedApplicationInfo) ) {
            return null;
        }

        // Note: The app overlay is not null if the module info is not null:
        //
        //    appInfo is null; moduleInfo is null: Pruned
        //      appOverlay may or may not be null        
        //    appInfo is null; moduleInfo not is null: Pruned
        //      appOverlay is not null
        //    appInfo is not null; moduleInfo is null: Proceed
        //      appOverlay may or may not be null
        //    appInfo is not null; moduleInfo is not null: Proceed
        //      appOverlay is not null

        NestedConfigHelper configHelper = ((ExtendedApplicationInfo) appInfo).getConfigHelper();
        if ( configHelper == null ) {
            return null;
        }
        String servicePid = (String) configHelper.get("service.pid");
        String extendsPid = (String) configHelper.get("ibm.extends.source.pid");

        Set<String> configModuleNames = new HashSet<String>();
        for ( EJBJarExt config : configurations ) {
            EJBJarExtComponentImpl configImpl = (EJBJarExtComponentImpl) config;
            
            // The configuration must match the service PID or the extends PID.
            String parentPid = (String) configImpl.getConfigAdminProperties().get("config.parentPID");            
            if ( !servicePid.equals(parentPid) && !parentPid.equals(extendsPid) ) {
                continue;
            }

            // If no module information is specified, simply take the first
            // matching configuration.
            if ( moduleInfo == null ) {
                return configImpl;
            }

            // Otherwise, only take the configuration if module names match.
            String configModuleName = (String) configImpl.getConfigAdminProperties().get("moduleName");
            if ( configModuleName == null ) {
                // A configuration is supposed to have a module name.
                // Issue a warning if none is present.  But, only issue
                // the warning once.
                warnModuleNameUnspecified(rootOverlay, moduleInfo, ddEntryPath);
            } else {
                configModuleName = stripExtension(configModuleName);
                if ( moduleInfo.getName().equals(configModuleName) ) {
                    // The names match.  Answer this configuration.
                    return configImpl;
                } else {
                    configModuleNames.add(configModuleName);
                }
            }
        }

        if ( (moduleInfo != null) && !configModuleNames.isEmpty() ) {
            warnModuleNameInvalid(rootOverlay, ddEntryPath, moduleInfo, configModuleNames);
        }

        return null;
    }

    private String stripExtension(String moduleName) {
        if ( moduleName.endsWith(".war") || moduleName.endsWith(".jar") ) {
            return moduleName.substring(0, moduleName.length() - 4);
        } else {
            return moduleName;
        }
    }

    private void warnModuleNameInvalid(
        OverlayContainer rootOverlay, String ddEntryName,
        ModuleInfo moduleInfo, Set<String> configModuleNames) throws UnableToAdaptException {

        OverlayContainer appOverlay = rootOverlay.getParentOverlay();

        if ( appOverlay.getFromNonPersistentCache(MODULE_NAME_INVALID, EJBJarExtAdapter.class) != null) {
            return;
        }
        appOverlay.addToNonPersistentCache(MODULE_NAME_INVALID, EJBJarExtAdapter.class, MODULE_NAME_INVALID);

        HashSet<String> appModuleNames = new HashSet<String>();
        Application app = moduleInfo.getApplicationInfo().getContainer().adapt(Application.class);
        for ( Module module : app.getModules() ) {
            appModuleNames.add( stripExtension( module.getModulePath() ) );
        }

        configModuleNames.removeAll(appModuleNames);
        if ( !configModuleNames.isEmpty() ) {
            // CWWKC2277E: One or more module names on the {1} element are invalid.
            // The invalid module name or names are {0}.            
            Tr.error(tc, "module.name.invalid", configModuleNames, "ejb-jar-ext");

            // TODO:
            // Error encountered while processing extension data {0}:
            // The module name {1} does not match any of configured extension module names {2}.
            // Tr.error(tc, "module.name.not.found",
            //     DDParser.describeEntry(null, moduleInfo.getContainer(), ddEntryName),
            //     moduleInfo.getName(), configModuleNames);
        }
     }

     private void warnModuleNameUnspecified(OverlayContainer rootOverlay, ModuleInfo moduleInfo, String ddEntryPath) {
         OverlayContainer appOverlay = rootOverlay.getParentOverlay();
         if ( appOverlay.getFromNonPersistentCache(MODULE_NAME_NOT_SPECIFIED, EJBJarExtAdapter.class) != null) {
             return;
         }
         appOverlay.addToNonPersistentCache(MODULE_NAME_NOT_SPECIFIED, EJBJarExtAdapter.class, MODULE_NAME_NOT_SPECIFIED);

         // Error encountered while processing extension data {0}:
         // A configured EJB extension is missing a module name.
         Tr.error(tc, "module.name.not.specified",
                      DDParser.describeEntry(null, moduleInfo.getContainer(), ddEntryPath));
     }
}
