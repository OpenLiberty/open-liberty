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
package com.ibm.ws.javaee.ddmodel.managedbean;

import java.util.List;
import com.ibm.ws.javaee.dd.managedbean.ManagedBeanBnd;

import org.osgi.service.component.annotations.*;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.common.BndExtAdapter;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
    service = ContainerAdapter.class,
    property = { "service.vendor=IBM",
                 "toType=com.ibm.ws.javaee.dd.managedbean.ManagedBeanBnd" })
public class ManagedBeanBndAdapter
    extends BndExtAdapter<ManagedBeanBnd>
    implements ContainerAdapter<ManagedBeanBnd> {

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    volatile List<ManagedBeanBnd> configurations;

    //

    public static final String XML_BND_IN_EJB_MOD_NAME = "META-INF/ibm-managed-bean-bnd.xml";
    public static final String XML_BND_IN_WEB_MOD_NAME = "WEB-INF/ibm-managed-bean-bnd.xml";

    @Override
    @FFDCIgnore(ParseException.class)
    public ManagedBeanBnd adapt(
            Container ddRoot,
            OverlayContainer ddOverlay,
            ArtifactContainer ddArtifactRoot,
            Container ddAdaptRoot) throws UnableToAdaptException {

        WebModuleInfo webInfo = (WebModuleInfo)
            ddOverlay.getFromNonPersistentCache(ddArtifactRoot.getPath(), WebModuleInfo.class);
        String ddPath = ( (webInfo == null) ? XML_BND_IN_EJB_MOD_NAME : XML_BND_IN_WEB_MOD_NAME ); 

        ManagedBeanBnd fromModule = getConfigOverrides(configurations, ddOverlay, ddArtifactRoot);

        Entry ddEntry = ddAdaptRoot.getEntry(ddPath);
        if ( ddEntry == null ) {
            return fromModule;
        }

        ManagedBeanBnd fromApp;
        try {
            ManagedBeanBndDDParser parser = new ManagedBeanBndDDParser(ddAdaptRoot, ddEntry);
            fromApp = parser.parse();
        } catch ( ParseException e ) {
            throw new UnableToAdaptException(e);
        }

        if ( fromModule == null ) {
            return fromApp;
         } else {
             setDelegate(fromModule, fromApp);
             return fromModule;
         }
    }

    //

    protected String getParentPid(ManagedBeanBnd mBeanBnd) {
        ManagedBeanBndComponentImpl mBeanBndImpl = (ManagedBeanBndComponentImpl) mBeanBnd;
        return (String) mBeanBndImpl.getConfigAdminProperties().get("config.parentPID");        
    }

    protected String getModuleName(ManagedBeanBnd mBeanBnd) {
        return (String) ((ManagedBeanBndComponentImpl) mBeanBnd).getConfigAdminProperties().get("moduleName");    
    }

    protected String getElementTag() {
        return "managed-bean-bnd";
    }

    protected Class<?> getCacheType() {
        return ManagedBeanBndAdapter.class;
    }
    
    protected void setDelegate(ManagedBeanBnd mBeanBnd, ManagedBeanBnd mBeanBndDelegate) {
        ((ManagedBeanBndComponentImpl) mBeanBnd).setDelegate(mBeanBndDelegate);
    }
}
