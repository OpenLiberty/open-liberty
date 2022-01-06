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
package com.ibm.ws.javaee.ddmodel.webbnd;

import java.util.List;

import com.ibm.ws.javaee.dd.webbnd.WebBnd;

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
 * Top level processing for Web bindings.
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
                 "toType=com.ibm.ws.javaee.dd.webbnd.WebBnd" })
public class WebBndAdapter extends BndExtAdapter<WebBnd> {

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    volatile List<WebBnd> configurations;

    @Override
    public List<? extends WebBnd> getConfigurations() {
        return configurations;
    }
    
    //

    @Override
    public WebBnd adapt(
        Container ddRoot,
        OverlayContainer ddOverlay,
        ArtifactContainer ddArtifactRoot,
        Container ddAdaptRoot) throws UnableToAdaptException {

        String webVersion = getWebVersion(ddAdaptRoot);
        boolean xmi = ( "2.2".equals(webVersion) ||
                        "2.3".equals(webVersion) ||
                        "2.4".equals(webVersion) );
        String ddPath = ( xmi ? WebBnd.XMI_BND_NAME : WebBnd.XML_BND_NAME );  

        return process(
                ddRoot, ddOverlay, ddArtifactRoot, ddAdaptRoot,
                ddPath, xmi);
    }    

    //
    
    @Override    
    protected WebBnd parse(Container ddAdaptRoot, Entry ddEntry, boolean xmi)
            throws ParseException {
        return ( new WebBndDDParser(ddAdaptRoot, ddEntry, xmi) ).parse();                
    }

    //

    @Override    
    protected String getParentPid(WebBnd webBnd) {
        WebBndComponentImpl webBndImpl = (WebBndComponentImpl) webBnd;
        return (String) webBndImpl.getConfigAdminProperties().get("config.parentPID");        
    }

    @Override    
    protected String getModuleName(WebBnd webBnd) {
        return (String) ((WebBndComponentImpl) webBnd).getConfigAdminProperties().get("moduleName");    
    }

    @Override    
    protected String getElementTag() {
        return "web-bnd";
    }

    @Override    
    protected Class<?> getCacheType() {
        return WebBndAdapter.class;
    }
    
    @Override    
    protected void setDelegate(WebBnd webBnd, WebBnd webBndDelegate) {
        ((WebBndComponentImpl) webBnd).setDelegate(webBndDelegate);
    }    
}
