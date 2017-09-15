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
package com.ibm.ws.javaee.ddmodel.webext;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.app.Module;
import org.osgi.service.component.annotations.*;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
    service = ContainerAdapter.class,
    property = { "service.vendor=IBM", "toType=com.ibm.ws.javaee.dd.webext.WebExt" })
public class WebExtAdapter implements ContainerAdapter<com.ibm.ws.javaee.dd.webext.WebExt> {

     private static final String MODULE_NAME_INVALID = "module.name.invalid";
     private static final String MODULE_NAME_NOT_SPECIFIED = "module.name.not.specified";
     private static final TraceComponent tc = Tr.register(WebExtAdapter.class);

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
volatile List<com.ibm.ws.javaee.dd.webext.WebExt> configurations;

    @Override
    @FFDCIgnore(ParseException.class)
    public com.ibm.ws.javaee.dd.webext.WebExt adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {
        com.ibm.ws.javaee.dd.web.WebApp primary = containerToAdapt.adapt(com.ibm.ws.javaee.dd.web.WebApp.class);
        String primaryVersion = primary == null ? null : primary.getVersion();
        String ddEntryName;
        boolean xmi = "2.2".equals(primaryVersion) || "2.3".equals(primaryVersion) || "2.4".equals(primaryVersion);
        if (xmi) {
            ddEntryName = com.ibm.ws.javaee.dd.webext.WebExt.XMI_EXT_NAME;
        } else {
            ddEntryName = com.ibm.ws.javaee.dd.webext.WebExt.XML_EXT_NAME;
        }

        Entry ddEntry = containerToAdapt.getEntry(ddEntryName);
com.ibm.ws.javaee.ddmodel.webext.WebExtComponentImpl fromConfig = getConfigOverrides(rootOverlay, artifactContainer);
if (ddEntry == null && fromConfig == null)
    return null;
        if (ddEntry != null) {
            try {
                com.ibm.ws.javaee.dd.webext.WebExt fromApp = 
              new com.ibm.ws.javaee.ddmodel.webext.WebExtDDParser(containerToAdapt, ddEntry, xmi).parse();
               if (fromConfig == null) {
                   return fromApp;
                } else {  
                   fromConfig.setDelegate(fromApp);
                    return fromConfig;
                }
            } catch (ParseException e) {
                throw new UnableToAdaptException(e);
            }
        }

        return fromConfig;
    }
private com.ibm.ws.javaee.ddmodel.webext.WebExtComponentImpl getConfigOverrides(OverlayContainer rootOverlay, ArtifactContainer artifactContainer) throws UnableToAdaptException {
     if (configurations == null || configurations.isEmpty())
          return null;

     ApplicationInfo appInfo = (ApplicationInfo) rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), ApplicationInfo.class);
     ModuleInfo moduleInfo = null;
     if (appInfo == null && rootOverlay.getParentOverlay() != null) {
          moduleInfo = (ModuleInfo) rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), ModuleInfo.class);
          if (moduleInfo == null)
               return null;
          appInfo = moduleInfo.getApplicationInfo();
     }
     NestedConfigHelper configHelper = null;
     if (appInfo != null && appInfo instanceof ExtendedApplicationInfo)
          configHelper = ((ExtendedApplicationInfo) appInfo).getConfigHelper();
      if (configHelper == null)
          return null;

     Set<String> configuredModuleNames = new HashSet<String>();
     String servicePid = (String) configHelper.get("service.pid");
     String extendsPid = (String) configHelper.get("ibm.extends.source.pid");
     for (com.ibm.ws.javaee.dd.webext.WebExt config : configurations) {
          com.ibm.ws.javaee.ddmodel.webext.WebExtComponentImpl configImpl = (com.ibm.ws.javaee.ddmodel.webext.WebExtComponentImpl) config;
          String parentPid = (String) configImpl.getConfigAdminProperties().get("config.parentPID");
          if ( servicePid.equals(parentPid) || parentPid.equals(extendsPid)) {
               if (moduleInfo == null)
                    return configImpl;
               String moduleName = (String) configImpl.getConfigAdminProperties().get("moduleName");
               if (moduleName == null) {
                    if (rootOverlay.getParentOverlay().getFromNonPersistentCache(MODULE_NAME_NOT_SPECIFIED, WebExtAdapter.class) == null) {
                    Tr.error(tc, "module.name.not.specified", "web-ext" );
                    rootOverlay.getParentOverlay().addToNonPersistentCache(MODULE_NAME_NOT_SPECIFIED, WebExtAdapter.class, MODULE_NAME_NOT_SPECIFIED);
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
      if (rootOverlay.getParentOverlay().getFromNonPersistentCache(MODULE_NAME_INVALID, WebExtAdapter.class) == null) {
          HashSet<String> moduleNames = new HashSet<String>();
          Application app = appInfo.getContainer().adapt(Application.class);
          for (Module m : app.getModules()) {
               moduleNames.add(stripExtension(m.getModulePath()));
          }
          configuredModuleNames.removeAll(moduleNames);
          if ( !configuredModuleNames.isEmpty() )
               Tr.error(tc, "module.name.invalid", configuredModuleNames, "web-ext");
          rootOverlay.getParentOverlay().addToNonPersistentCache(MODULE_NAME_INVALID, WebExtAdapter.class, MODULE_NAME_INVALID);
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
}
