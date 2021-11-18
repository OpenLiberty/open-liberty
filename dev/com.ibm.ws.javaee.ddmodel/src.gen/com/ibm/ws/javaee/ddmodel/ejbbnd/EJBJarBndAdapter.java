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
package com.ibm.ws.javaee.ddmodel.ejbbnd;

import java.util.List;

import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.ejbbnd.EJBJarBnd;

import org.osgi.service.component.annotations.*;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.common.BndExtAdapter;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 * Top level processing for EJB bindings.
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
                 "toType=com.ibm.ws.javaee.dd.ejbbnd.EJBJarBnd" })
public class EJBJarBndAdapter extends BndExtAdapter<EJBJarBnd> {

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    volatile List<EJBJarBnd> configurations;

    @Override
    public List<? extends EJBJarBnd> getConfigurations() {
        return configurations;
    }
    
    //

    public static final String XMI_BND_IN_EJB_MOD_NAME = "META-INF/ibm-ejb-jar-bnd.xmi";
    public static final String XML_BND_IN_EJB_MOD_NAME = "META-INF/ibm-ejb-jar-bnd.xml";
    public static final String XMI_BND_IN_WEB_MOD_NAME = "WEB-INF/ibm-ejb-jar-bnd.xmi";
    public static final String XML_BND_IN_WEB_MOD_NAME = "WEB-INF/ibm-ejb-jar-bnd.xml";
    
    @Override
    public EJBJarBnd adapt(
            Container ddRoot,
            OverlayContainer ddOverlay,
            ArtifactContainer ddArtifactRoot,
            Container ddAdaptRoot) throws UnableToAdaptException {

        Integer ejbVersion = getEJBVersion(ddAdaptRoot);
        boolean xmi = ( (ejbVersion != null) && (ejbVersion.intValue() < EJBJar.VERSION_3_0) );

        String ddPath;
        if ( isWebModule(ddOverlay, ddArtifactRoot) ) {
            ddPath = ( xmi ? XMI_BND_IN_WEB_MOD_NAME : XML_BND_IN_WEB_MOD_NAME );
        } else {
            ddPath = ( xmi ? XMI_BND_IN_EJB_MOD_NAME : XML_BND_IN_EJB_MOD_NAME );
        }

        return process(
                ddRoot, ddOverlay, ddArtifactRoot, ddAdaptRoot,
                ddPath, xmi);
    }

    //
    
    @Override    
    protected EJBJarBnd parse(Container ddAdaptRoot, Entry ddEntry, boolean xmi)
            throws ParseException {
        return ( new EJBJarBndDDParser(ddAdaptRoot, ddEntry, xmi) ).parse();                
    }    
    
    //

    @Override    
    protected String getParentPid(EJBJarBnd ejbBnd) {
        EJBJarBndComponentImpl ejbBndImpl = (EJBJarBndComponentImpl) ejbBnd;
        return (String) ejbBndImpl.getConfigAdminProperties().get("config.parentPID");        
    }

    @Override    
    protected String getModuleName(EJBJarBnd ejbBnd) {
        return (String) ((EJBJarBndComponentImpl) ejbBnd).getConfigAdminProperties().get("moduleName");    
    }
    
    @Override    
    protected String getElementTag() {
        return "ejb-jar-bnd";
    }

    @Override    
    protected Class<?> getCacheType() {
        return EJBJarBndAdapter.class;
    }

    @Override    
    protected void setDelegate(EJBJarBnd ejbBnd, EJBJarBnd ejbBndDelegate) {
        ((EJBJarBndComponentImpl) ejbBnd).setDelegate(ejbBndDelegate);
    }
}
