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
import com.ibm.ws.javaee.ddmodel.common.BndExtAdapter;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 * Top level processing for application bindings.
 * 
 * This uses the same pattern as application extensions.
 * 
 * The code has been left separate: Merging the two classes
 * doesn't seem quite worth the extra effort and complications
 * that would be introduced.
 * 
 * See {@link com.ibm.ws.javaee.ddmodel.common.BndExtAdapter} for
 * more information.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
    service = ContainerAdapter.class,
    property = { "service.vendor=IBM",
                 "toType=com.ibm.ws.javaee.dd.appbnd.ApplicationBnd" })
public class ApplicationBndAdapter
    extends BndExtAdapter<ApplicationBnd>
    implements ContainerAdapter<ApplicationBnd> {

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    volatile List<ApplicationBnd> configurations;    

    //

    @Override
    @FFDCIgnore(ParseException.class)
    public ApplicationBnd adapt(
        Container ddRoot,
        OverlayContainer ddOverlay,
        ArtifactContainer ddArtifactRoot,
        Container ddAdaptRoot) throws UnableToAdaptException {

        Application app = ddAdaptRoot.adapt(Application.class);
        String appVersion = ((app == null) ? null : app.getVersion());
        boolean xmi = ( "1.2".equals(appVersion) || "1.3".equals(appVersion) || "1.4".equals(appVersion) );
        String ddPath = ( xmi ? ApplicationBnd.XMI_BND_NAME : ApplicationBnd.XML_BND_NAME );  

        ApplicationBnd fromConfig = getConfigOverrides(ddOverlay, ddArtifactRoot);

        Entry ddEntry = ddAdaptRoot.getEntry(ddPath);
        if ( ddEntry == null ) {
            return fromConfig;
        }

        ApplicationBnd fromApp;
        try {
            ApplicationBndDDParser parser =
                new ApplicationBndDDParser(ddAdaptRoot, ddEntry, xmi);                
            fromApp = parser.parse(); 
        } catch ( ParseException e ) {
            throw new UnableToAdaptException(e);
        }
        
        if ( fromConfig == null ) {
            return fromApp;
        } else {  
            setDelegate(fromConfig, fromApp);
            return fromConfig;
        }
    }

    private ApplicationBnd getConfigOverrides(OverlayContainer ddOverlay, ArtifactContainer ddArtifactRoot) {
        if ( (configurations == null) || configurations.isEmpty() ) {
             return null;
        }

        ApplicationInfo appInfo = (ApplicationInfo)
            ddOverlay.getFromNonPersistentCache(ddArtifactRoot.getPath(), ApplicationInfo.class);
        if ( (appInfo == null) || !(appInfo instanceof ExtendedApplicationInfo) ) {
            return null;
        }

        NestedConfigHelper configHelper = ((ExtendedApplicationInfo) appInfo).getConfigHelper();
         if ( configHelper == null ) {
             return null;
         }
    
        String appServicePid = (String) configHelper.get("service.pid");
        String appExtendsPid = (String) configHelper.get("ibm.extends.source.pid");
        for ( ApplicationBnd appBnd : configurations ) {
             String parentPid = getParentPid(appBnd);
             if ( appServicePid.equals(parentPid) || parentPid.equals(appExtendsPid)) {
                 return appBnd;
             }
        }
        return null;
    }

    //

    protected String getParentPid(ApplicationBnd appBnd) {
        ApplicationBndComponentImpl appBndImpl = (ApplicationBndComponentImpl) appBnd;
        return (String) appBndImpl.getConfigAdminProperties().get("config.parentPID");        
    }

    protected String getModuleName(ApplicationBnd appBnd) {
        return null; // Unused
    }

    protected String getElementTag() {
        return "application-bnd"; // Unused
    }

    protected Class<?> getCacheType() {
        return ApplicationBndAdapter.class; // Unused
    }
    
    protected void setDelegate(ApplicationBnd appBnd, ApplicationBnd appBndDelegate) {
        ((ApplicationBndComponentImpl) appBnd).setDelegate(appBndDelegate);
    }
}
