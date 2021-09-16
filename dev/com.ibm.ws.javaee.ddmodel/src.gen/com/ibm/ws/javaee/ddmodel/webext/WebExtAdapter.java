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

// ************************************************************
// THIS FILE HAS BEEN UPDATED SINCE IT WAS GENERATED.
// ANY NEWLY GENERATED CODE MUST BE CAREFULLY MERGED WITH
// THIS CODE.
// ************************************************************

package com.ibm.ws.javaee.ddmodel.webext;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.web.WebApp;
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
public class WebExtAdapter
    extends BndExtAdapter<WebExt>
    implements ContainerAdapter<WebExt> {

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    volatile List<WebExt> configurations;

    //

    @Override
    @FFDCIgnore(ParseException.class)
    public WebExt adapt(
        Container ddRoot,
        OverlayContainer ddOverlay,
        ArtifactContainer ddArtifactRoot,
        Container ddAdaptRoot) throws UnableToAdaptException {

        WebApp webApp = ddAdaptRoot.adapt(WebApp.class);
        String webAppVersion = ((webApp == null) ? null : webApp.getVersion());
        boolean xmi = ( "2.2".equals(webAppVersion) ||
                        "2.3".equals(webAppVersion) ||
                        "2.4".equals(webAppVersion) );
        String ddPath = ( xmi ? WebExt.XMI_EXT_NAME : WebExt.XML_EXT_NAME ); 
    
        WebExt fromConfig = getConfigOverrides(configurations, ddOverlay, ddArtifactRoot);

        Entry ddEntry = ddAdaptRoot.getEntry(ddPath);
        if ( ddEntry == null ) {
            return fromConfig;
        }

        WebExt fromModule;
        try {
            WebExtDDParser parser = new WebExtDDParser(ddAdaptRoot, ddEntry, xmi);
            fromModule = parser.parse();
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

    protected String getParentPid(WebExt webExt) {
        WebExtComponentImpl WebExtImpl = (WebExtComponentImpl) webExt;
        return (String) WebExtImpl.getConfigAdminProperties().get("config.parentPID");        
    }

    protected String getModuleName(WebExt WebExt) {
        return (String) ((WebExtComponentImpl) WebExt).getConfigAdminProperties().get("moduleName");    
    }

    protected String getElementTag() {
        return "web-ext";
    }

    protected Class<?> getCacheType() {
        return WebExtAdapter.class;
    }

    protected void setDelegate(WebExt WebExt, WebExt WebExtDelegate) {
        ((WebExtComponentImpl) WebExt).setDelegate(WebExtDelegate);
    }   
}
