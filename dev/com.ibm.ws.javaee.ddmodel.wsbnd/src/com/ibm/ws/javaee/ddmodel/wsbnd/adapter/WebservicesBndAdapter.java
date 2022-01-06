/*******************************************************************************
 * Copyright (c) 2012,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.wsbnd.adapter;

import java.util.List;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.DDParserBndExt;
import com.ibm.ws.javaee.ddmodel.common.BndExtAdapter;
import com.ibm.ws.javaee.ddmodel.wsbnd.WebservicesBnd;
import com.ibm.ws.javaee.ddmodel.wsbnd.impl.WebservicesBndComponentImpl;
import com.ibm.ws.javaee.ddmodel.wsbnd.impl.WebservicesBndType;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = ContainerAdapter.class,
           property = { "service.vendor=IBM",
                        "toType=com.ibm.ws.javaee.ddmodel.wsbnd.WebservicesBnd" })
public final class WebservicesBndAdapter extends BndExtAdapter<WebservicesBnd> {

    @Reference( cardinality = ReferenceCardinality.MULTIPLE,
                policy = ReferencePolicy.DYNAMIC,
                policyOption = ReferencePolicyOption.GREEDY )
    volatile List<WebservicesBnd> configurations;

    @Override
    public List<? extends WebservicesBnd> getConfigurations() {
        return configurations;
    }    
    
    @Activate
    protected void activate(ComponentContext context) {
        String processType = context.getBundleContext().getProperty("wlp.process.type");
        isServer = "server".equals(processType);
    }

    protected void deactivate(ComponentContext cc) {
        isServer = DEFAULT_IS_SERVER;
    }

    private static final boolean DEFAULT_IS_SERVER = true;

    private boolean isServer = DEFAULT_IS_SERVER;

    public boolean isServer() { 
        return isServer;
    }

    //

    public WebservicesBnd adapt(
        Container ddRoot,
        OverlayContainer ddOverlay,
        ArtifactContainer ddArtifactRoot,
        Container ddAdaptRoot) throws UnableToAdaptException {

        String ddPath;
        if ( isServer() ) {
            ddPath = ( isWebModule(ddOverlay, ddArtifactRoot)
                    ? WebservicesBnd.WEB_XML_BND_URI
                    : WebservicesBnd.EJB_XML_BND_URI );
        } else {
            ddPath = WebservicesBnd.EJB_XML_BND_URI;
        }

        return process(
                ddRoot, ddOverlay, ddArtifactRoot, ddAdaptRoot,
                ddPath, XMI_UNUSED);
    }    
    
    //
    
    @Override        
    protected String getParentPid(WebservicesBnd wsBnd) {
        return (String) ((WebservicesBndComponentImpl) wsBnd)
                .getConfigAdminProperties().get("config.parentPID");
    }

    @Override        
    protected String getModuleName(WebservicesBnd wsBnd) {
        return (String) ((WebservicesBndComponentImpl) wsBnd)
                .getConfigAdminProperties().get("moduleName");    
    }

    public static final String WEBSERVICES_BND_ELEMENT_NAME = "webservices-bnd";

    @Override        
    protected String getElementTag() {
        return WEBSERVICES_BND_ELEMENT_NAME;
    }

    @Override        
    protected Class<?> getCacheType() {
        return WebservicesBndAdapter.class;
    }

    @Override    
    protected void setDelegate(WebservicesBnd wsBnd, WebservicesBnd wsBndDelegate) {
        ((WebservicesBndComponentImpl) wsBnd).setDelegate(wsBndDelegate);
    }
    
    //

    @Override    
    protected WebservicesBnd parse(Container ddAdaptRoot, Entry ddEntry, boolean xmi)
            throws ParseException {
        return ( new WebservicesBndDDParser(ddAdaptRoot, ddEntry) ).parse();                
    }

    private static final class WebservicesBndDDParser extends DDParserBndExt {
        public WebservicesBndDDParser(Container ddRootContainer, Entry ddEntry) throws ParseException {
            super(ddRootContainer, ddEntry,
                    UNUSED_CROSS_COMPONENT_TYPE,
                    !IS_XMI, WEBSERVICES_BND_ELEMENT_NAME, UNUSED_XMI_NAMESPACE,
                    XML_VERSION_MAPPINGS_10_10,
                    10);
        }

        @Override
        public WebservicesBndType parse() throws ParseException {
            super.parseRootElement();
            return (WebservicesBndType) rootParsable;
        }

        @Override
        protected WebservicesBndType createRootParsable() throws ParseException {
            return (WebservicesBndType) super.createRootParsable();
        }

        @Override
        protected WebservicesBndType createRoot() {
            return new WebservicesBndType( getDeploymentDescriptorPath() );            
        }
    }
}
