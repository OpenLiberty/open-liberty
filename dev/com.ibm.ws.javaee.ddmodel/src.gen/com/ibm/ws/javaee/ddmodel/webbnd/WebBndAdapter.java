/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// NOTE: This is a generated file. Do not edit it directly.
package com.ibm.ws.javaee.ddmodel.webbnd;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.app.Module;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.webbnd.WebBnd;

import org.osgi.service.component.annotations.*;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.web.WebAppDDParser;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
    service = ContainerAdapter.class,
    property = { "service.vendor=IBM", "toType=com.ibm.ws.javaee.dd.webbnd.WebBnd" })
public class WebBndAdapter implements ContainerAdapter<WebBnd> {
    private static final TraceComponent tc = Tr.register(WebBndAdapter.class);

    private static final String MODULE_NAME_INVALID = "module.name.invalid";
    private static final String MODULE_NAME_NOT_SPECIFIED = "module.name.not.specified";

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    volatile List<WebBnd> configurations;

    private WebBndComponentImpl getConfigOverrides(OverlayContainer overlay, ArtifactContainer artifactContainer) throws UnableToAdaptException {
        if ( (configurations == null) || configurations.isEmpty() ) {
            return null;
        }

        String cachePath = artifactContainer.getPath();

        ApplicationInfo appInfo = (ApplicationInfo)
            overlay.getFromNonPersistentCache(cachePath, ApplicationInfo.class);
        ModuleInfo moduleInfo = null;
        if ( appInfo == null ) {
            moduleInfo = (ModuleInfo) overlay.getFromNonPersistentCache(cachePath, ModuleInfo.class);
            if ( moduleInfo != null ) {
                appInfo = moduleInfo.getApplicationInfo();
            }
        }
        if ( (appInfo == null) || !(appInfo instanceof ExtendedApplicationInfo) ) {
            return null;
        }

        NestedConfigHelper configHelper =
            ((ExtendedApplicationInfo) appInfo).getConfigHelper();
        if ( configHelper == null ) {
            return null;
        }
         
        OverlayContainer rootOverlay = overlay;
        if (overlay.getParentOverlay() != null)
           rootOverlay = overlay.getParentOverlay();
         
        Set<String> configuredModuleNames = new HashSet<String>();
        String servicePid = (String) configHelper.get("service.pid");
        String extendsPid = (String) configHelper.get("ibm.extends.source.pid");
        for (WebBnd config : configurations) {
             WebBndComponentImpl configImpl = (WebBndComponentImpl) config;
             String parentPid = (String) configImpl.getConfigAdminProperties().get("config.parentPID");
             if ( servicePid.equals(parentPid) || parentPid.equals(extendsPid)) {
                  if (moduleInfo == null)
                       return configImpl;
                  String moduleName = (String) configImpl.getConfigAdminProperties().get("moduleName");
                  if (moduleName == null) {
                       if (rootOverlay.getFromNonPersistentCache(MODULE_NAME_NOT_SPECIFIED, WebBndAdapter.class) == null) {
                       Tr.error(tc, "module.name.not.specified", "web-bnd" );
                       rootOverlay.addToNonPersistentCache(MODULE_NAME_NOT_SPECIFIED, WebBndAdapter.class, MODULE_NAME_NOT_SPECIFIED);
                       }
                       continue;
                  }
                  moduleName = stripExtension(moduleName);
                  configuredModuleNames.add(moduleName);
                  if (moduleInfo.getName().equals(moduleName))
                       return configImpl;
        }
        }
        if (moduleInfo != null && !configuredModuleNames.isEmpty()) {
         if (rootOverlay.getFromNonPersistentCache(MODULE_NAME_INVALID, WebBndAdapter.class) == null) {
             HashSet<String> moduleNames = new HashSet<String>();
             Application app = appInfo.getContainer().adapt(Application.class);
             for (Module m : app.getModules()) {
                  moduleNames.add(stripExtension(m.getModulePath()));
             }
             configuredModuleNames.removeAll(moduleNames);
             if ( !configuredModuleNames.isEmpty() )
                  Tr.error(tc, "module.name.invalid", configuredModuleNames, "web-bnd");
             rootOverlay.addToNonPersistentCache(MODULE_NAME_INVALID, WebBndAdapter.class, MODULE_NAME_INVALID);
             }
        }
        return null;
   }
    
    private String stripExtension(String moduleName) {
         if (moduleName.endsWith(".war") || moduleName.endsWith(".jar")) {
              return moduleName.substring(0, moduleName.length() - 4);
         }
         return moduleName;
    }
    
    @Override
    @FFDCIgnore(ParseException.class)
    public WebBnd adapt(
            Container ddRoot,
            OverlayContainer rootOverlay, ArtifactContainer artifactContainer,
            Container ddAdaptRoot) throws UnableToAdaptException {
        
        WebApp webApp = ddAdaptRoot.adapt(WebApp.class);
        String webAppVersion = ((webApp == null) ? null : webApp.getVersion() );

        String ddPath;
        boolean xmi = "2.2".equals(webAppVersion) || "2.3".equals(webAppVersion) || "2.4".equals(webAppVersion);
        if ( xmi ) {
            ddPath = WebBnd.XMI_BND_NAME;
        } else {
            ddPath = WebBnd.XML_BND_NAME;
        }

        WebBndComponentImpl fromConfig = getConfigOverrides(rootOverlay, artifactContainer);
        
        Entry ddEntry = ddAdaptRoot.getEntry(ddPath);
        if ( ddEntry == null ) {
            return fromConfig;
        }

        WebBnd fromApp;
        try {
            WebBndDDParser parser = new WebBndDDParser(ddAdaptRoot, ddEntry, xmi);
            fromApp = parser.parse();
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
}
