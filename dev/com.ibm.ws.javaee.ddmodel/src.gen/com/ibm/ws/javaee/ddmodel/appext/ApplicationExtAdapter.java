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
package com.ibm.ws.javaee.ddmodel.appext;

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
    property = { "service.vendor=IBM", "toType=com.ibm.ws.javaee.dd.appext.ApplicationExt" })
public class ApplicationExtAdapter implements ContainerAdapter<com.ibm.ws.javaee.dd.appext.ApplicationExt> {

     private static final String MODULE_NAME_INVALID = "module.name.invalid";
     private static final String MODULE_NAME_NOT_SPECIFIED = "module.name.not.specified";
     private static final TraceComponent tc = Tr.register(ApplicationExtAdapter.class);

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
volatile List<com.ibm.ws.javaee.dd.appext.ApplicationExt> configurations;

    @Override
    @FFDCIgnore(ParseException.class)
    public com.ibm.ws.javaee.dd.appext.ApplicationExt adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {
        com.ibm.ws.javaee.dd.app.Application primary = containerToAdapt.adapt(com.ibm.ws.javaee.dd.app.Application.class);
        String primaryVersion = primary == null ? null : primary.getVersion();
        String ddEntryName;
        boolean xmi = "1.2".equals(primaryVersion) || "1.3".equals(primaryVersion) || "1.4".equals(primaryVersion);
        if (xmi) {
            ddEntryName = com.ibm.ws.javaee.dd.appext.ApplicationExt.XMI_EXT_NAME;
        } else {
            ddEntryName = com.ibm.ws.javaee.dd.appext.ApplicationExt.XML_EXT_NAME;
        }

        Entry ddEntry = containerToAdapt.getEntry(ddEntryName);
com.ibm.ws.javaee.ddmodel.appext.ApplicationExtComponentImpl fromConfig = getConfigOverrides(rootOverlay, artifactContainer);
if (ddEntry == null && fromConfig == null)
    return null;
        if (ddEntry != null) {
            try {
                com.ibm.ws.javaee.dd.appext.ApplicationExt fromApp = 
              new com.ibm.ws.javaee.ddmodel.appext.ApplicationExtDDParser(containerToAdapt, ddEntry, xmi).parse();
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
private com.ibm.ws.javaee.ddmodel.appext.ApplicationExtComponentImpl getConfigOverrides(OverlayContainer rootOverlay, ArtifactContainer artifactContainer) {
     if (configurations == null || configurations.isEmpty())
          return null;

     ApplicationInfo appInfo = (ApplicationInfo) rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), ApplicationInfo.class);
     NestedConfigHelper configHelper = null;
     if (appInfo != null && appInfo instanceof ExtendedApplicationInfo)
          configHelper = ((ExtendedApplicationInfo) appInfo).getConfigHelper();
      if (configHelper == null)
          return null;

     String servicePid = (String) configHelper.get("service.pid");
     String extendsPid = (String) configHelper.get("ibm.extends.source.pid");
     for (com.ibm.ws.javaee.dd.appext.ApplicationExt config : configurations) {
          com.ibm.ws.javaee.ddmodel.appext.ApplicationExtComponentImpl configImpl = (com.ibm.ws.javaee.ddmodel.appext.ApplicationExtComponentImpl) config;
          String parentPid = (String) configImpl.getConfigAdminProperties().get("config.parentPID");
          if ( servicePid.equals(parentPid) || parentPid.equals(extendsPid)) {
                    return configImpl;
          }
     }
     return null;
}
}
