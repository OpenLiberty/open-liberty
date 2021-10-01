/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
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

import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
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
    // Service configuration ...

    @Activate
    protected void activate(ComponentContext context) {
        String processType = context.getBundleContext().getProperty("wlp.process.type");
        isServer = "server".equals(processType);
    }

    protected void deactivate(ComponentContext cc) {
        isServer = DEFAULT_IS_SERVER;
    }

    //

    private static final boolean DEFAULT_IS_SERVER = true;

    private boolean isServer = DEFAULT_IS_SERVER;

    public boolean getIsServer() { 
        return isServer;
    }

    //
    
    @Reference( cardinality = ReferenceCardinality.MULTIPLE,
                policy = ReferencePolicy.DYNAMIC,
                policyOption = ReferencePolicyOption.GREEDY )
    volatile List<WebservicesBnd> configurations;

    //

    @FFDCIgnore(ParseException.class)
    public WebservicesBnd adapt(
        Container ddRoot,
        OverlayContainer ddOverlay,
        ArtifactContainer ddArtifactRoot, Container ddAdaptRoot)
        throws UnableToAdaptException {

        String ddPath;
        if ( getIsServer() ) {
            WebModuleInfo webInfo = (WebModuleInfo)
                ddOverlay.getFromNonPersistentCache(ddArtifactRoot.getPath(), WebModuleInfo.class);
            if ( webInfo != null ) {
                ddPath = WebservicesBnd.WEB_XML_BND_URI;
            } else {
                ddPath = WebservicesBnd.EJB_XML_BND_URI;
            }
        } else {
            ddPath = WebservicesBnd.EJB_XML_BND_URI;
        }

        WebservicesBnd fromConfig = getConfigOverrides(configurations, ddOverlay, ddArtifactRoot);

        Entry ddEntry = ddAdaptRoot.getEntry(ddPath);
        if ( ddEntry == null ) { 
            return fromConfig;
        }

        WebservicesBnd fromModule;
        try {
            fromModule = new WsClientBndParser(ddAdaptRoot, ddEntry).parse();
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

    private static final class WsClientBndParser extends DDParserBndExt {
        public WsClientBndParser(Container ddRootContainer, Entry ddEntry) throws ParseException {
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
