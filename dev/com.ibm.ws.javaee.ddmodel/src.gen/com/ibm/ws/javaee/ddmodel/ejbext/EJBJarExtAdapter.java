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
package com.ibm.ws.javaee.ddmodel.ejbext;

import java.util.List;

import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.ejbext.EJBJarExt;

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

/**
 * Top level processing for EJB extensions.
 * 
 * The core implementation is shared between all four of
 * EJB and Web bindings and extensions.
 * 
 * A simplified pattern is used by application client bindings,
 * and by application bindings and extensions.  Those patterns
 * are implemented directly.
 * 
 * See {@link com.ibm.ws.javaee.ddmodel.common.BndExtAdapter} for
 * more information.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
    service = ContainerAdapter.class,
    property = { "service.vendor=IBM",
                 "toType=com.ibm.ws.javaee.dd.ejbext.EJBJarExt" })
public class EJBJarExtAdapter
    extends BndExtAdapter<EJBJarExt>
    implements ContainerAdapter<EJBJarExt> {

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY)
    volatile List<EJBJarExt> configurations;

    //

    public static final String XMI_EXT_IN_EJB_MOD_NAME = "META-INF/ibm-ejb-jar-ext.xmi";
    public static final String XML_EXT_IN_EJB_MOD_NAME = "META-INF/ibm-ejb-jar-ext.xml";
    public static final String XMI_EXT_IN_WEB_MOD_NAME = "WEB-INF/ibm-ejb-jar-ext.xmi";
    public static final String XML_EXT_IN_WEB_MOD_NAME = "WEB-INF/ibm-ejb-jar-ext.xml";

    @Override
    @FFDCIgnore(ParseException.class)
    public EJBJarExt adapt(
        Container ddRoot,
        OverlayContainer ddOverlay,
        ArtifactContainer ddArtifactRoot,
        Container ddAdaptRoot) throws UnableToAdaptException {

        EJBJar ejbJar = ddAdaptRoot.adapt(EJBJar.class);
        boolean xmi = ( (ejbJar != null) && ejbJar.getVersionID() < EJBJar.VERSION_3_0 );

        WebModuleInfo webInfo = (WebModuleInfo)
            ddOverlay.getFromNonPersistentCache(ddArtifactRoot.getPath(), WebModuleInfo.class);

        String ddPath;
        if ( webInfo == null ) {
            ddPath = ( xmi ? XMI_EXT_IN_EJB_MOD_NAME : XML_EXT_IN_EJB_MOD_NAME );
        } else {
            ddPath = ( xmi ? XMI_EXT_IN_WEB_MOD_NAME : XML_EXT_IN_WEB_MOD_NAME );
        }

        EJBJarExt ddConfig = getConfigOverrides(configurations, ddOverlay, ddArtifactRoot);

        Entry ddEntry = ddAdaptRoot.getEntry(ddPath);
        if ( ddEntry == null ) { 
            return ddConfig;
        }

        EJBJarExt ddModule;
        try {
            ddModule = new EJBJarExtDDParser(ddAdaptRoot, ddEntry, xmi).parse();
        } catch ( ParseException e ) {
            throw new UnableToAdaptException(e);
        }

        if ( ddConfig == null ) {
            return ddModule;
        } else {  
            setDelegate(ddConfig, ddModule);
            return ddConfig;
        }
    }

    //

    protected String getParentPid(EJBJarExt ejbExt) {
        EJBJarExtComponentImpl ejbExtImpl = (EJBJarExtComponentImpl) ejbExt;
        return (String) ejbExtImpl.getConfigAdminProperties().get("config.parentPID");        
    }

    protected String getModuleName(EJBJarExt ejbExt) {
        return (String) ((EJBJarExtComponentImpl) ejbExt).getConfigAdminProperties().get("moduleName");    
    }

    protected String getElementTag() {
        return "ejb-jar-ext";
    }

    protected Class<?> getCacheType() {
        return EJBJarExtAdapter.class;
    }

    protected void setDelegate(EJBJarExt ejbExt, EJBJarExt ejbExtDelegate) {
        ((EJBJarExtComponentImpl) ejbExt).setDelegate(ejbExtDelegate);
    }    
}
