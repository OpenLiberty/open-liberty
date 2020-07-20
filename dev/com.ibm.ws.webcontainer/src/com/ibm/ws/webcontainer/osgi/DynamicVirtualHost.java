/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Jan 1, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.ibm.ws.webcontainer.osgi;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.websphere.servlet.response.IResponse;
import com.ibm.ws.container.DeployedModule;
import com.ibm.ws.webcontainer.exception.WebAppNotLoadedException;
import com.ibm.ws.webcontainer.internalRuntimeExport.srt.IPrivateRequestAttributes;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer.osgi.webapp.WebApp;
import com.ibm.ws.webcontainer.osgi.webapp.WebGroup;
import com.ibm.ws.webcontainer.osgi.webapp.WebGroupConfiguration;
import com.ibm.ws.webcontainer.util.URIMatcher;
import com.ibm.ws.webcontainer.util.WSURLDecoder;
import com.ibm.wsspi.http.HttpContainer;
import com.ibm.wsspi.http.HttpInboundConnection;
import com.ibm.wsspi.http.channel.values.ConnectionValues;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.StatusCodes;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.WCCustomProperties;

/**
 * Thin shim over the base webcontainer VirtualHost to satisfy the HttpContainer
 * contract (including registering context roots with the http transport virtual host.
 * <p>
 * The bridge between this VirtualHost and the http transport object is provided via
 * DynamicVirtualHostConfiguration & DynamicVirtualHostManager.
 */
public class DynamicVirtualHost extends com.ibm.ws.webcontainer.VirtualHost implements HttpContainer {
    private static final TraceComponent tc = Tr.register(DynamicVirtualHost.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

// logger is defined in com.ibm.ws.webcontainer.VirtualHost
//  protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.osgi");
    private static final String CLASS_NAME = "com.ibm.ws.webcontainer.osgi.DynamicVirtualHost";

    private final WebContainer webContainerParent;
    private final DynamicVirtualHostConfiguration dhostConfig;

    private URIMatcher predefinedMatcher;
    
    private static boolean normalizeRequestURI = WCCustomProperties.NORMALIZE_REQUEST_URI; //PI05525

    interface Bridge extends RequestProcessor, Runnable {};
    
    /**
     * @param name
     * @param parent
     */
    DynamicVirtualHost(DynamicVirtualHostConfiguration vHostConfig, WebContainer parent) {
        super(vHostConfig, parent);
        dhostConfig = vHostConfig;
        webContainerParent = parent;
    }

    /**
     * Package protected method for returning the virtual host configuration.
     * 
     * @return
     */
    public DynamicVirtualHostConfiguration getHostConfiguration() {
        return dhostConfig;
    }

    /**
     * Method addWebApplication.
     * 
     * @param deployedModule
     * @param extensionFactories
     */
    //BEGIN: NEVER INVOKED BY WEBSPHERE APPLICATION SERVER (Common Component Specific)
    @SuppressWarnings("unchecked")
    @Override
    public void addWebApplication(DeployedModule deployedModule, List extensionFactories) throws WebAppNotLoadedException {
       
        com.ibm.ws.webcontainer.osgi.container.DeployedModule deployedModuleImpl = (com.ibm.ws.webcontainer.osgi.container.DeployedModule) deployedModule;
        String ct = deployedModuleImpl.getProperContextRoot();
        String contextRoot = deployedModuleImpl.getMappingContextRoot();

        String displayName = deployedModule.getDisplayName();
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
            logger.logp(Level.FINE, CLASS_NAME,"addWebApplication",  "enter ["+ displayName +"]");
        }


        WebGroup webGroup = (WebGroup) requestMapper.map(contextRoot);

        if (webGroup != null && ct.equalsIgnoreCase(webGroup.getConfiguration().getContextRoot())) {
            // begin 296368 Nested exceptions lost for problems during
            // application startup WAS.webcontainer
            List list = webGroup.getWebApps();
            String originalName = "";
            if (list != null && (list.size() > 0)) {
                WebApp originalWebApp = (WebApp) list.get(0);
                originalName = originalWebApp.getWebAppName();
            }
            logger.logp(Level.SEVERE, CLASS_NAME, "addWebApplication", "context.root.already.in.use", new Object[] { displayName, contextRoot, originalName, displayName });
            throw new WebAppNotLoadedException("Context root " + contextRoot + " is already bound. Cannot start application " + displayName);
            // end 296368 Nested exceptions lost for problems during application
            // startup WAS.webcontainer
        }
        // The following is used by/for Liberty: requred to create the webgroup & 
        // webgroup configuration
        webGroup = new WebGroup(contextRoot, this);
        WebGroupConfiguration wgConfig = new WebGroupConfiguration(deployedModule.getName());
        wgConfig.setContextRoot(deployedModule.getContextRoot());
        wgConfig.setVersionID(deployedModule.getWebAppConfig().getVersion());
        // begin LIDB2356.1: WebContainer work for incorporating SIP

        // Liberty: if this is one of the "new" WebAppConfiguration objects,
        // add this as the virtual host for better delegation (and less object copying)
        com.ibm.ws.webcontainer.webapp.WebAppConfiguration baseAppConfig = deployedModule.getWebAppConfig();
        try {
            ((com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration) baseAppConfig).setVirtualHost(this);
        } catch (ClassCastException cce) {
            baseAppConfig.setVirtualHostName(getName());
        }

        wgConfig.setWebAppHost(this);
        // end LIDB2356.1: WebContainer work for incorporating SIP
        webGroup.initialize(wgConfig);
        try {
            webGroup.addWebApplication(deployedModule, extensionFactories);
            Object[] args = { displayName, vHostConfig.toString() };
            logger.logp(Level.INFO, CLASS_NAME, "addWebApplication", "module.[{0}].successfully.bound.to.virtualhost.[{1}]", args);
        } catch (Throwable t) {
            //PI58875
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                logger.logp(Level.FINE, CLASS_NAME,"addWebApplication",  "error adding web app ["+ displayName +"]");
            }
            webGroup.destroy();  //preventing the classLoader memory leak
            webGroup = null;
            //PI58875 end 

            // requestMapper.removeMapping(contextRoot);
            // Do not need to remove mapping because we wait until we're sure we should add it!
            // PK67698 removeMapping(contextRoot);
            // 296368 added rootCause to newly created exception.
            throw new WebAppNotLoadedException(t.getMessage(), t);
        }

        // PK67698 Start
        try {
            addMapping(contextRoot, webGroup);

            webGroup.notifyStart();
        } catch (Exception exc) {
            // begin 296368 Nested exceptions lost for problems during
            // application startup WAS.webcontainer
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "addWebApplication", "error adding mapping ", exc); /* @283348.1 */
            }
            webGroup.destroy();
            throw new WebAppNotLoadedException("Context root " + contextRoot + " mapping unable to be bound. Application " + displayName + " unavailable.", exc);
            // end 296368 Nested exceptions lost for problems during application
            // startup WAS.webcontainer
        }
        // PK67698 End
    }

    @SuppressWarnings("unchecked")
    public Iterator<WebApp> getWebApps() {
        Iterator mappings = requestMapper.targetMappings();
        ArrayList<WebApp> webApps = new ArrayList<WebApp>();
        while (mappings.hasNext()) {
            WebGroup wg = (WebGroup) mappings.next();
            if (wg!=null) {
                webApps.addAll(wg.getWebApps());
                if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "getWebApps", "add apps ", wg.getWebApps()); 
                }
            }
        }
        return webApps.iterator();
    }

    /** Allow this to be invoked directly with a pre-constructed IRequest/IResponse and HttpConnection.
     *  For most inbound requests, this method is invoked via {@see #createRunnableHandler(final HttpInboundConnection inboundConnection) },
     *   the SIP container invokes this method directly */
    public Runnable createRunnableHandler(final IRequest ireq ,final IResponse ires,final HttpInboundConnection inboundConnection) {

        String requestUri = ireq.getRequestURI();
        
        //PI05525
        // needs to make sure we normalize and then map it to an webapp. In case the request is http://host/contextroot/../ , map to / 
        if(normalizeRequestURI){            
            requestUri = WebApp.normalize(requestUri); // normalize is a helper method not mapped to any webapp
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc,"normalized request uri --> ", requestUri);
        }
               
        WebApp sc = (WebApp) findContext(requestUri);            
        if ((sc == null) && (requestUri.indexOf("%") >= 0)) {
            // context could contain double byte char, so decode it now.
            // for performance reasons this was not decoded before doing the first look up, since it is not
            // common to have double byte chars in the context.  
            try {
                if (WCCustomProperties.DECODE_URL_PLUS_SIGN) {
                    requestUri = URLDecoder.decode(requestUri, this.webContainerParent.getURIEncoding());
                } else {
                    requestUri = WSURLDecoder.decode(requestUri, this.webContainerParent.getURIEncoding());
                }
            } catch (IOException e) {
                // unexpected - log FFDC and leave.
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, CLASS_NAME, "222", this);
                return null;
            }

            sc = (WebApp) findContext(requestUri);
        }

        final WebApp webApp = sc;

        if (webApp == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { 
                Tr.debug(this, tc, "request for--> ["+ requestUri+"], inboundConnection --> [" + inboundConnection + "], this --> " + this);
                Tr.debug(tc, "Context not found, return null");
            }
            
            // check if we need to send back a 503 since its predefined
            if (predefinedMatcher != null && predefinedMatcher.match(requestUri) != null) {
                return new Runnable() {

                    @Override
                    public void run() {
                        sendError(inboundConnection, ireq, ires);
                    }

                };
            }

            return null;
        } else if (!webApp.isInitialized()) {
            if (startWebApp(webApp) == false) {
                return null;
            }
        }

        return new Bridge() {
            HttpInboundConnection httpInboundConnection = inboundConnection;
            public void run() {
                Exception error = null;
                try {
                    // give a way to speed up finding the request processor: 
                    // we know where it has to come back to.. 
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { 
                        Tr.debug(this, tc, "Webcontainer handleRequest start for--> ["+ireq.getRequestURI()+"], mapped webApp context ["+ webApp + "], inboundConnection --> [" + inboundConnection + "], this --> " + this);
                    }
                    webContainerParent.handleRequest(ireq, ires, DynamicVirtualHost.this, this);
                    
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { 
                        Tr.debug(this, tc, "Webcontainer handleRequest complete for--> ["+ireq.getRequestURI()+"], mapped webApp context ["+ webApp + "], inboundConnection --> [" + inboundConnection + "], this --> " + this);
                    }
                    
                } catch (IOException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Error during request handling; " + e, inboundConnection, ireq);
                    }
                    error = e;
                } finally {
                    // If not async, then finish.  Else, finish will need to be done by the last thread working on the response  
                    if (ireq.isStartAsync() == false)
                        inboundConnection.finish(error);
                }
            }

            @Override
            public String getName() {
                return DynamicVirtualHost.this.getName();
            }

            /**
             * Invocation of this is via the webContainerParent.handleRequest, and
             * from other sources that look up this vhost by name..
             * In this case, we're looking in our requestMapper by the request URI
             * to see which processor should be used to handle the request.
             */
            @Override
            public void handleRequest(ServletRequest req, ServletResponse res) throws Exception {
                String hostAlias = WebContainer.getHostAliasKey(req.getServerName(), req.getServerPort());

                addSecureRedirect(req, hostAlias);

                webApp.handleRequest(req, res,  httpInboundConnection);
            }

            @Override
            public boolean isInternal() {
                return DynamicVirtualHost.this.isInternal();
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public Runnable createRunnableHandler(final HttpInboundConnection inboundConnection) {

        final IRequest ireq = webContainerParent.getRequestFactory().createRequest(inboundConnection);
        final IResponse ires = webContainerParent.getResponseFactory().createResponse(ireq, inboundConnection);
        return createRunnableHandler(ireq, ires,inboundConnection);


    }
    @Override
    protected void addSecureRedirect(ServletRequest req, String hostAlias) {
        int secureRedirect = vHostConfig.getSecureHttpPort(hostAlias);
        if (secureRedirect > 0) {
            try {
                ((IPrivateRequestAttributes) req).setPrivateAttribute("SecurityRedirectPort", Integer.valueOf(secureRedirect));
            } catch (ClassCastException cce) {
                //for performance reasons
                //the hreq could not be cast to an IPrivateRequestAttribute ... do nothing
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "failed to set the security redirect port", getName(), hostAlias);
                }
            }
        }
    }

    protected synchronized void addMapping(String contextRoot, com.ibm.ws.webcontainer.webapp.WebGroup group) throws Exception {
        super.addMapping(contextRoot, group);
        dhostConfig.addContextRoot(contextRoot, this);
    }

    protected synchronized void removeMapping(String contextRoot) {
        dhostConfig.removeContextRoot(contextRoot, this);
        super.removeMapping(contextRoot);
    }

    protected synchronized Object removeMappedObject(String contextRoot) {
        dhostConfig.removeContextRoot(contextRoot, this);
        return super.removeMappedObject(contextRoot);
    }

    public String getMimeType(String extension) {
        return dhostConfig.getMimeType(extension);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
               + "[name=" + name
               + "," + dhostConfig
               + "]";
    }

    private void sendError(HttpInboundConnection inboundConnection, IRequest ireq, IResponse irsp) {
        Exception error = null;
        StatusCodes code = StatusCodes.UNAVAILABLE;

        irsp.setStatusCode(code.getIntCode());
        irsp.setHeader(HttpHeaderKeys.HDR_CONNECTION.getByteArray(), ConnectionValues.CLOSE.getByteArray());
        irsp.resetBuffer();

        if (code.isBodyAllowed()) {
            try {
                ServletOutputStream body = irsp.getOutputStream();
                final byte bits[][] = new byte[][] { "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">".getBytes(),
                                                     "<html><head><title>".getBytes(),
                                                     "</title></head><body><h1>".getBytes(),
                                                     "</h1><p>".getBytes(),
                                                     "</p><hr /><address>".getBytes(),
                                                     "</address></body></html>".getBytes() };

                byte[] msg;

                body.write(bits[0]); // doctype
                body.write(bits[1]); // header-> title

                msg = code.getStatusWithPhrase();
                body.write(msg); // - title
                body.write(bits[2]); // title, body, h1

                msg = code.getDefaultPhraseBytes();
                body.write(msg); // - status phrase as header
                body.write(bits[3]); // h1, p 

                msg = ireq.getRequestURI().getBytes();
                body.write(msg); // - detail as body

                body.write(bits[4]); // p, address 
                body.write(bits[5]); // address, body, html 
            } catch (IOException e) {
                error = e;
            }
        }

        inboundConnection.finish(error);
    }

    /**
     * @param predefinedCtxRoots
     */
    void setPredefinedContextRoots(Set<String> ctxRoots) {
        URIMatcher newMatcher = null;
        if (ctxRoots != null && !ctxRoots.isEmpty()) {
            newMatcher = new URIMatcher(true, true);
            for (String ctxRoot : ctxRoots) {
                try {
                    newMatcher.put(ctxRoot, ctxRoot);
                } catch (Exception e) {
                }
            }
        }

        URIMatcher oldMatcher = predefinedMatcher;
        predefinedMatcher = newMatcher;

        // decide if we need to register or deregister
        if (oldMatcher == null && newMatcher != null) {
            dhostConfig.addContextRoot(null, this);
        } else if (oldMatcher != null && newMatcher == null) {
            dhostConfig.removeContextRoot(null, this);
        }
    }

    /**
     * @param dm
     */
    public boolean startWebApplication(DeployedModule dm) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
            logger.logp(Level.FINE, CLASS_NAME,"startWebApplication",  "Enter ");
        }


        String ct = makeProperContextRoot(dm.getContextRoot()); // proper
        WebApp webApp = (WebApp) findContext(ct);
        if (webApp == null) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                logger.logp(Level.FINE, CLASS_NAME,"startWebApplication",  "No webapp mapping found for contextroot ->"+ ct);
            }
            return false;
        }

        return startWebApp(webApp);
    }

    private boolean startWebApp(WebApp webApp) {
        try {
            webApp.initialize();
            return !webApp.getDestroyed();
        } catch (Throwable th) {
            stopWebApp(webApp);

            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, CLASS_NAME, "startWebApp", new Object[] { this, webApp });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Error starting web app: " + webApp + "; " + th);
            }
        }
        return false;
    }

    public boolean stopWebApplication(DeployedModule dm) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
            logger.logp(Level.FINE, CLASS_NAME,"stopWebApplication",  "Enter ");
        }

        String ct = makeProperContextRoot(dm.getContextRoot()); // proper
        WebApp webApp = (WebApp) findContext(ct);
        if (webApp != null) {
            return stopWebApp(webApp);
        }

        return false;
    }

    private boolean stopWebApp(WebApp webApp) {
        webApp.failed();
        webApp.destroy();
        return true;
    }

}
