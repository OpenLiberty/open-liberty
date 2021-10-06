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
package com.ibm.ws.javaee.ddmodel.clientbnd;

import java.util.List;
import org.osgi.service.component.annotations.*;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.javaee.dd.clientbnd.ApplicationClientBnd;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.common.BndExtAdapter;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 * Top level processing for application client bindings.
 * 
 * This uses a subset of the pattern used by EJB and Web
 * bindings and extensions.  This implementation is a
 * simplified copy of the later pattern.
 * 
 * See {@link com.ibm.ws.javaee.ddmodel.common.BndExtAdapter} for
 * more information.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
    service = ContainerAdapter.class,
    property = { "service.vendor=IBM",
                 "toType=com.ibm.ws.javaee.dd.clientbnd.ApplicationClientBnd" })
public class ApplicationClientBndAdapter extends BndExtAdapter<ApplicationClientBnd> {

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    volatile List<ApplicationClientBnd> configurations;

    @Override    
    public List<? extends ApplicationClientBnd> getConfigurations() {
        return configurations;
    }
    
    //

    @Override
    public ApplicationClientBnd adapt(
        Container ddRoot,
        OverlayContainer ddOverlay,
        ArtifactContainer ddArtifactRoot,
        Container ddAdaptRoot) throws UnableToAdaptException {

        String ddVersion = getAppClientVersion(ddAdaptRoot);
        boolean xmi = ( "1.2".equals(ddVersion) ||
                        "1.3".equals(ddVersion) ||
                        "1.4".equals(ddVersion) );
        String ddPath =
                ( xmi ? ApplicationClientBnd.XMI_BND_NAME
                      : ApplicationClientBnd.XML_BND_NAME );  

        return process(
                ddRoot, ddOverlay, ddArtifactRoot, ddAdaptRoot,
                ddPath, xmi);
    }
    
    //

    /**
     * Local operation to retrieve configuration overrides.
     * 
     * Other cases have a module name which is used to match
     * overrides.  Application clients do not have a module name.
     *
     * @param ddOverlay The root overlay container of the descriptor.  This will be
     *     a module overlay.
     * @param ddArtifactRoot The root artifact container of the descriptor.  This will
     *     be a module container.
     *
     * @return The selected configuration override.  Null if none is available.
     */
    @Override
    protected ApplicationClientBnd getConfigOverrides(
        OverlayContainer ddOverlay,
        ArtifactContainer ddArtifactRoot) {

        List<? extends ApplicationClientBnd> useConfigurations = getConfigurations();                
        if ( (useConfigurations == null) || useConfigurations.isEmpty() ) {
            return null;
        }

        ApplicationInfo appInfo = (ApplicationInfo)
            ddOverlay.getFromNonPersistentCache(ddArtifactRoot.getPath(), ApplicationInfo.class);

        NestedConfigHelper configHelper = null;
        if ( (appInfo != null) && (appInfo instanceof ExtendedApplicationInfo) ) {
            configHelper = ((ExtendedApplicationInfo) appInfo).getConfigHelper();
        }
        if ( configHelper == null ) {
            return null;
        }
        String appServicePid = (String) configHelper.get("service.pid");
        String appExtendsPid = (String) configHelper.get("ibm.extends.source.pid");

        for ( ApplicationClientBnd appClientBnd : useConfigurations ) {
            String parentPid = getParentPid(appClientBnd);
            if ( appServicePid.equals(parentPid) || parentPid.equals(appExtendsPid) ) {
                return appClientBnd;
            }
        }
        return null;
    }

    //
    
    @Override    
    protected ApplicationClientBnd parse(Container ddAdaptRoot, Entry ddEntry, boolean xmi)
            throws ParseException {
        return ( new ApplicationClientBndDDParser(ddAdaptRoot, ddEntry, xmi) ).parse();                
    }
    
    //

    @Override    
    protected String getParentPid(ApplicationClientBnd appClientBnd) {
        ApplicationClientBndComponentImpl appClientBndImpl =
            (ApplicationClientBndComponentImpl) appClientBnd;
        return (String) appClientBndImpl.getConfigAdminProperties().get("config.parentPID");        
    }

    @Override    
    protected String getModuleName(ApplicationClientBnd appClientBnd) {
        return null; // Unused
    }

    @Override    
    protected String getElementTag() {
        return "application-client-bnd"; // Unused
    }

    @Override    
    protected Class<?> getCacheType() {
        return ApplicationClientBndAdapter.class; // Unused
    }
    
    @Override    
    protected void setDelegate(
            ApplicationClientBnd appClientBnd,
            ApplicationClientBnd appClientBndDelegate) {

        ((ApplicationClientBndComponentImpl) appClientBnd).setDelegate(appClientBndDelegate);
    }
}
