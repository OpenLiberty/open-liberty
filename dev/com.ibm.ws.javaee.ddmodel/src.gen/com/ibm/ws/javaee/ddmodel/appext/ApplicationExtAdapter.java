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
package com.ibm.ws.javaee.ddmodel.appext;

import java.util.List;
import org.osgi.service.component.annotations.*;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.appext.ApplicationExt;
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
public class ApplicationExtAdapter implements ContainerAdapter<ApplicationExt> {
    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    volatile List<ApplicationExt> configurations;

    private ApplicationExtComponentImpl getConfigOverrides(
            OverlayContainer rootOverlay, ArtifactContainer artifactContainer) {

        if ( (configurations == null) || configurations.isEmpty() ) {
            return null;
        }

        ApplicationInfo appInfo = (ApplicationInfo)
            rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), ApplicationInfo.class);
        if ( (appInfo == null) || !(appInfo instanceof ExtendedApplicationInfo ) ) {
            return null;
        }

        NestedConfigHelper configHelper = ((ExtendedApplicationInfo) appInfo).getConfigHelper();
        if ( configHelper == null ) {
            return null;
        }
            
        String servicePid = (String) configHelper.get("service.pid");
        String extendsPid = (String) configHelper.get("ibm.extends.source.pid");
        for ( ApplicationExt config : configurations ) {
            ApplicationExtComponentImpl configImpl = (ApplicationExtComponentImpl) config;
            String parentPid = (String) configImpl.getConfigAdminProperties().get("config.parentPID");
            if ( servicePid.equals(parentPid) || parentPid.equals(extendsPid)) {
                return configImpl;
            }
        }
        return null;
    }
    
    @Override
    @FFDCIgnore(ParseException.class)
    public ApplicationExt adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {
        Application app = containerToAdapt.adapt(Application.class);
        String appVersion = ( (app == null) ? null : app.getVersion() );
        boolean xmi = ( (appVersion != null) &&
                        ("1.2".equals(appVersion) ||
                         "1.3".equals(appVersion) ||
                         "1.4".equals(appVersion)) );

        String ddEntryName;
        if ( xmi ) {
            ddEntryName = ApplicationExt.XMI_EXT_NAME;
        } else {
            ddEntryName = ApplicationExt.XML_EXT_NAME;
        }
        Entry ddEntry = containerToAdapt.getEntry(ddEntryName);

        ApplicationExtComponentImpl fromConfig =
            getConfigOverrides(rootOverlay, artifactContainer);

        if ( ddEntry == null ) {
            return fromConfig;
        }

        ApplicationExt fromApp;
        try {
            fromApp = new ApplicationExtDDParser(containerToAdapt, ddEntry, xmi).parse();
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
