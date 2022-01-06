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
package com.ibm.ws.javaee.ddmodel.webext;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.javaee.dd.webext.WebExt;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.common.BndExtAdapter;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 * Top level processing for Web extensions.
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
                        "toType=com.ibm.ws.javaee.dd.webext.WebExt" })
public class WebExtAdapter extends BndExtAdapter<WebExt> {

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    volatile List<WebExt> configurations;

    @Override
    public List<? extends WebExt> getConfigurations() {
        return configurations;
    }
    
    //

    @Override
    public WebExt adapt(
        Container ddRoot,
        OverlayContainer ddOverlay,
        ArtifactContainer ddArtifactRoot,
        Container ddAdaptRoot) throws UnableToAdaptException {

        String webVersion = getWebVersion(ddAdaptRoot);
        boolean xmi = ( "2.2".equals(webVersion) ||
                        "2.3".equals(webVersion) ||
                        "2.4".equals(webVersion) );
        String ddPath = ( xmi ? WebExt.XMI_EXT_NAME : WebExt.XML_EXT_NAME ); 

        return process(
                ddRoot, ddOverlay, ddArtifactRoot, ddAdaptRoot,
                ddPath, xmi);
    }

    //
    
    @Override
    protected WebExt parse(Container ddAdaptRoot, Entry ddEntry, boolean xmi)
            throws ParseException {
        return ( new WebExtDDParser(ddAdaptRoot, ddEntry, xmi) ).parse();                
    }
    
    //

    @Override    
    protected String getParentPid(WebExt webExt) {
        WebExtComponentImpl WebExtImpl = (WebExtComponentImpl) webExt;
        return (String) WebExtImpl.getConfigAdminProperties().get("config.parentPID");        
    }

    @Override    
    protected String getModuleName(WebExt WebExt) {
        return (String) ((WebExtComponentImpl) WebExt).getConfigAdminProperties().get("moduleName");    
    }

    @Override
    protected String getElementTag() {
        return "web-ext";
    }

    @Override
    protected Class<?> getCacheType() {
        return WebExtAdapter.class;
    }

    @Override
    protected void setDelegate(WebExt WebExt, WebExt WebExtDelegate) {
        ((WebExtComponentImpl) WebExt).setDelegate(WebExtDelegate);
    }   
}
