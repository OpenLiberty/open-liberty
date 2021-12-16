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
    property = { "service.vendor=IBM", "toType=com.ibm.ws.javaee.dd.ejbext.EJBJarExt" })
public class EJBJarExtAdapter implements ContainerAdapter<com.ibm.ws.javaee.dd.ejbext.EJBJarExt> {
    public static final String XMI_EXT_IN_EJB_MOD_NAME = "META-INF/ibm-ejb-jar-ext.xmi";
    public static final String XML_EXT_IN_EJB_MOD_NAME = "META-INF/ibm-ejb-jar-ext.xml";
    public static final String XMI_EXT_IN_WEB_MOD_NAME = "WEB-INF/ibm-ejb-jar-ext.xmi";
    public static final String XML_EXT_IN_WEB_MOD_NAME = "WEB-INF/ibm-ejb-jar-ext.xml";


     private static final String MODULE_NAME_INVALID = "module.name.invalid";
     private static final String MODULE_NAME_NOT_SPECIFIED = "module.name.not.specified";
     private static final TraceComponent tc = Tr.register(EJBJarExtAdapter.class);

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
volatile List<com.ibm.ws.javaee.dd.ejbext.EJBJarExt> configurations;

    @Override
    @FFDCIgnore(ParseException.class)
    public com.ibm.ws.javaee.dd.ejbext.EJBJarExt adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {
        com.ibm.ws.javaee.dd.ejb.EJBJar primary = containerToAdapt.adapt(com.ibm.ws.javaee.dd.ejb.EJBJar.class);
        boolean xmi = primary != null && primary.getVersionID() < com.ibm.ws.javaee.dd.ejb.EJBJar.VERSION_3_0;
        String ddEntryName;
        if (rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), com.ibm.ws.container.service.app.deploy.WebModuleInfo.class) == null) {
            ddEntryName = xmi ? XMI_EXT_IN_EJB_MOD_NAME : XML_EXT_IN_EJB_MOD_NAME;
        } else {
            ddEntryName = xmi ? XMI_EXT_IN_WEB_MOD_NAME : XML_EXT_IN_WEB_MOD_NAME;
        }

        Entry ddEntry = containerToAdapt.getEntry(ddEntryName);
com.ibm.ws.javaee.ddmodel.ejbext.EJBJarExtComponentImpl fromConfig = getConfigOverrides(rootOverlay, artifactContainer);
if (ddEntry == null && fromConfig == null)
    return null;
        if (ddEntry != null) {
            try {
                com.ibm.ws.javaee.dd.ejbext.EJBJarExt fromApp = 
              new com.ibm.ws.javaee.ddmodel.ejbext.EJBJarExtDDParser(containerToAdapt, ddEntry, xmi).parse();
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
private com.ibm.ws.javaee.ddmodel.ejbext.EJBJarExtComponentImpl getConfigOverrides(OverlayContainer rootOverlay, ArtifactContainer artifactContainer) throws UnableToAdaptException {
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
     for (com.ibm.ws.javaee.dd.ejbext.EJBJarExt config : configurations) {
          com.ibm.ws.javaee.ddmodel.ejbext.EJBJarExtComponentImpl configImpl = (com.ibm.ws.javaee.ddmodel.ejbext.EJBJarExtComponentImpl) config;
          String parentPid = (String) configImpl.getConfigAdminProperties().get("config.parentPID");
          if ( servicePid.equals(parentPid) || parentPid.equals(extendsPid)) {
               if (moduleInfo == null)
                    return configImpl;
               String moduleName = (String) configImpl.getConfigAdminProperties().get("moduleName");
               if (moduleName == null) {
                    if (rootOverlay.getParentOverlay().getFromNonPersistentCache(MODULE_NAME_NOT_SPECIFIED, EJBJarExtAdapter.class) == null) {
                    Tr.error(tc, "module.name.not.specified", "ejb-jar-ext" );
                    rootOverlay.getParentOverlay().addToNonPersistentCache(MODULE_NAME_NOT_SPECIFIED, EJBJarExtAdapter.class, MODULE_NAME_NOT_SPECIFIED);
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
      if (rootOverlay.getParentOverlay().getFromNonPersistentCache(MODULE_NAME_INVALID, EJBJarExtAdapter.class) == null) {
          HashSet<String> moduleNames = new HashSet<String>();
          Application app = appInfo.getContainer().adapt(Application.class);
          for (Module m : app.getModules()) {
               moduleNames.add(stripExtension(m.getModulePath()));
          }
          configuredModuleNames.removeAll(moduleNames);
          if ( !configuredModuleNames.isEmpty() )
               Tr.error(tc, "module.name.invalid", configuredModuleNames, "ejb-jar-ext");
          rootOverlay.getParentOverlay().addToNonPersistentCache(MODULE_NAME_INVALID, EJBJarExtAdapter.class, MODULE_NAME_INVALID);
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
