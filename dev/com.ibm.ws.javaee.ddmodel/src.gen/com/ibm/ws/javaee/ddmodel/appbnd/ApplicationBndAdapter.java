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
package com.ibm.ws.javaee.ddmodel.appbnd;

import java.util.List;
import org.osgi.service.component.annotations.*;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.appbnd.ApplicationBnd;
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
                 "toType=com.ibm.ws.javaee.dd.appbnd.ApplicationBnd" })
public class ApplicationBndAdapter implements ContainerAdapter<ApplicationBnd> {
    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)

    volatile List<ApplicationBnd> configurations;

    private ApplicationBndComponentImpl getConfigOverrides(OverlayContainer rootOverlay, ArtifactContainer artifactContainer) {
        if ( (configurations == null) || configurations.isEmpty() ) {
             return null;
        }
        
        ApplicationInfo appInfo = (ApplicationInfo)
            rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), ApplicationInfo.class);
        if ( (appInfo == null) || !(appInfo instanceof ExtendedApplicationInfo) ) {
            return null;
        }

        NestedConfigHelper configHelper = ((ExtendedApplicationInfo) appInfo).getConfigHelper();
         if ( configHelper == null ) {
             return null;
         }
    
        String servicePid = (String) configHelper.get("service.pid");
        String extendsPid = (String) configHelper.get("ibm.extends.source.pid");
        for ( ApplicationBnd config : configurations ) {
             ApplicationBndComponentImpl configImpl = (ApplicationBndComponentImpl) config;
             String parentPid = (String) configImpl.getConfigAdminProperties().get("config.parentPID");
             if ( servicePid.equals(parentPid) || parentPid.equals(extendsPid)) {
                 return configImpl;
             }
        }
        return null;
   }    
    
    @Override
    @FFDCIgnore(ParseException.class)
    public ApplicationBnd adapt(
        Container root,
        OverlayContainer rootOverlay,
        ArtifactContainer artifactContainer,
        Container containerToAdapt) throws UnableToAdaptException {

        Application application = containerToAdapt.adapt(Application.class);
        String appSchemaVersion = ((application == null) ? null : application.getVersion());
        boolean xmi = ( "1.2".equals(appSchemaVersion) ||
                        "1.3".equals(appSchemaVersion) ||
                        "1.4".equals(appSchemaVersion) );

        String ddEntryName;
        if ( xmi ) {
            ddEntryName = ApplicationBnd.XMI_BND_NAME;
        } else {
            ddEntryName = ApplicationBnd.XML_BND_NAME;
        }

        Entry ddEntry = containerToAdapt.getEntry(ddEntryName);
        ApplicationBndComponentImpl fromConfig =
            getConfigOverrides(rootOverlay, artifactContainer);
        if ( (ddEntry == null) && (fromConfig == null) ) {
            return null;
        }

        if ( ddEntry == null ) {
            return fromConfig;
        }

        ApplicationBnd fromApp;
        try {
            ApplicationBndDDParser parser =
                new ApplicationBndDDParser(containerToAdapt, ddEntry, xmi);                
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
