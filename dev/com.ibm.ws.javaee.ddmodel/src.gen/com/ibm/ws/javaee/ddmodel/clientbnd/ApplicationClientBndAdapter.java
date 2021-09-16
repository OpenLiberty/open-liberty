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
package com.ibm.ws.javaee.ddmodel.clientbnd;

import java.util.List;
import org.osgi.service.component.annotations.*;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.client.ApplicationClient;
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
public class ApplicationClientBndAdapter
    extends BndExtAdapter<ApplicationClientBnd>
    implements ContainerAdapter<ApplicationClientBnd> {

    @Reference( cardinality = ReferenceCardinality.MULTIPLE,
                policy = ReferencePolicy.DYNAMIC,
                policyOption = ReferencePolicyOption.GREEDY )
    volatile List<ApplicationClientBnd> configurations;

    //

    @Override
    @FFDCIgnore(ParseException.class)
    public ApplicationClientBnd adapt(
        Container ddRoot,
        OverlayContainer ddOverlay,
        ArtifactContainer ddArtifactRoot,
        Container ddAdaptRoot) throws UnableToAdaptException {

        ApplicationClient appClient = ddAdaptRoot.adapt(ApplicationClient.class);
        String ddVersion = ( (appClient == null) ? null : appClient.getVersion() );
        boolean xmi = ( "1.2".equals(ddVersion) || "1.3".equals(ddVersion) || "1.4".equals(ddVersion) );
        String ddPath = ( xmi ? ApplicationClientBnd.XMI_BND_NAME : ApplicationClientBnd.XML_BND_NAME );  

        ApplicationClientBnd fromConfig = getConfigOverrides(ddOverlay, ddArtifactRoot);
        
        Entry ddEntry = ddAdaptRoot.getEntry(ddPath);
        if ( ddEntry == null ) {
            return fromConfig;
        }

        ApplicationClientBnd fromModule;
        try {
            fromModule = new ApplicationClientBndDDParser(ddAdaptRoot, ddEntry, xmi).parse();
        } catch ( ParseException e ) {
            throw new UnableToAdaptException(e);
        }

        if ( fromConfig == null ) {
            return fromModule;
        } else {  
            setDelegate(fromConfig, fromModule);
            return fromConfig;
        }
    }

    //
    
    private ApplicationClientBnd getConfigOverrides(
        OverlayContainer ddOverlay,
        ArtifactContainer ddArtifactRoot) {

        if ( (configurations == null) || configurations.isEmpty() ) {
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

        for ( ApplicationClientBnd appClientBnd : configurations ) {
            String parentPid = getParentPid(appClientBnd);
            if ( appServicePid.equals(parentPid) || parentPid.equals(appExtendsPid) ) {
                return appClientBnd;
            }
        }
        return null;
    }

    //

    protected String getParentPid(ApplicationClientBnd appClientBnd) {
        ApplicationClientBndComponentImpl appClientBndImpl =
            (ApplicationClientBndComponentImpl) appClientBnd;
        return (String) appClientBndImpl.getConfigAdminProperties().get("config.parentPID");        
    }

    protected String getModuleName(ApplicationClientBnd appClientBnd) {
        return null; // Unused
    }

    protected String getElementTag() {
        return "application-client-bnd"; // Unused
    }

    protected Class<?> getCacheType() {
        return ApplicationClientBndAdapter.class; // Unused
    }
    
    protected void setDelegate(ApplicationClientBnd appClientBnd, ApplicationClientBnd appClientBndDelegate) {
        ((ApplicationClientBndComponentImpl) appClientBnd).setDelegate(appClientBndDelegate);
    }
}
