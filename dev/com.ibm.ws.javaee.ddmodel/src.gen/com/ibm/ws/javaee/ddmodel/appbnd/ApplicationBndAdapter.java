/*******************************************************************************
 * Copyright (c) 2017,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.appbnd;

import java.util.List;
import org.osgi.service.component.annotations.*;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
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
public class ApplicationBndAdapter extends BndExtAdapter<ApplicationBnd> {

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    volatile List<ApplicationBnd> configurations;    

    @Override
    public List<? extends ApplicationBnd> getConfigurations() {
        return configurations;
    }

    //

    @Override
    public ApplicationBnd adapt(
        Container ddRoot,
        OverlayContainer ddOverlay,
        ArtifactContainer ddArtifactRoot,
        Container ddAdaptRoot) throws UnableToAdaptException {

        String appVersion = getAppVersion(ddAdaptRoot);
        boolean xmi = ( "1.2".equals(appVersion) ||
                        "1.3".equals(appVersion) ||
                        "1.4".equals(appVersion) );
        String ddPath = ( xmi ? ApplicationBnd.XMI_BND_NAME : ApplicationBnd.XML_BND_NAME );  

        return process(
                ddRoot, ddOverlay, ddArtifactRoot, ddAdaptRoot,
                ddPath, xmi);
    }

    @Override
    protected ApplicationBnd getConfigOverrides(
            OverlayContainer ddOverlay,
            ArtifactContainer ddArtifactRoot) throws UnableToAdaptException {
        
        List<? extends ApplicationBnd> useConfigurations = getConfigurations();        
        if ( (useConfigurations == null) || useConfigurations.isEmpty() ) {
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
        for ( ApplicationBnd appBnd : useConfigurations ) {
             String parentPid = getParentPid(appBnd);
             if ( appServicePid.equals(parentPid) || parentPid.equals(appExtendsPid)) {
                 return appBnd;
             }
        }
        return null;
    }

    //

    @Override
    protected ApplicationBnd parse(Container ddAdaptRoot, Entry ddEntry, boolean xmi)
            throws ParseException {
        return ( new ApplicationBndDDParser(ddAdaptRoot, ddEntry, xmi) ).parse();                
    }
    
    //

    @Override    
    protected String getParentPid(ApplicationBnd appBnd) {
        ApplicationBndComponentImpl appBndImpl = (ApplicationBndComponentImpl) appBnd;
        return (String) appBndImpl.getConfigAdminProperties().get("config.parentPID");        
    }

    @Override    
    protected String getModuleName(ApplicationBnd appBnd) {
        return null; // Unused
    }

    @Override    
    protected String getElementTag() {
        return "application-bnd"; // Unused
    }

    @Override    
    protected Class<?> getCacheType() {
        return ApplicationBndAdapter.class; // Unused
    }
    
    @Override    
    protected void setDelegate(ApplicationBnd appBnd, ApplicationBnd appBndDelegate) {
        ((ApplicationBndComponentImpl) appBnd).setDelegate(appBndDelegate);
    }
}
