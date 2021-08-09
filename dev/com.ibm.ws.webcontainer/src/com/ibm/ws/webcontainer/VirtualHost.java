/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.Container;
import com.ibm.ws.container.DeployedModule;
import com.ibm.ws.webcontainer.core.BaseContainer;
import com.ibm.ws.webcontainer.exception.WebAppNotLoadedException;
import com.ibm.ws.webcontainer.exception.WebGroupVHostNotFoundException;
import com.ibm.ws.webcontainer.session.IHttpSessionContext;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.ws.webcontainer.webapp.WebGroup;
import com.ibm.ws.webcontainer.webapp.WebGroupConfiguration;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.util.URIMapper;

public abstract class VirtualHost extends BaseContainer {
    protected final static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer");
    private static final String CLASS_NAME = "com.ibm.ws.webcontainer.VirtualHost";

    protected final VirtualHostConfiguration vHostConfig;

    public VirtualHost(VirtualHostConfiguration vHostConfig, Container parent) {
        super(vHostConfig.getName(), parent);
        this.vHostConfig = vHostConfig;
        requestMapper = new URIMapper(true);
    }

    public List<String> getAliases() {
        return vHostConfig.getAliases();
    }

    /**
     * Method addWebApplication.
     * 
     * @param deployedModule
     * @param extensionFactories
     */
    //BEGIN: NEVER INVOKED BY WEBSPHERE APPLICATION SERVER (Common Component Specific)
    @SuppressWarnings("unchecked")
    public void addWebApplication(DeployedModule deployedModule, List extensionFactories) throws WebAppNotLoadedException {
        WebGroupConfiguration wgConfig = deployedModule.getWebGroupConfig();
        WebGroup wg = deployedModule.getWebGroup();
        
        String contextRoot = deployedModule.getContextRoot();
        String ct = makeProperContextRoot(contextRoot); // proper
        String mapRoot = makeMappingContextRoot(ct);
        
        String displayName = deployedModule.getDisplayName();

        WebGroup webGroup = (WebGroup) requestMapper.map(mapRoot);

        if (webGroup != null && ct.equalsIgnoreCase(webGroup.getConfiguration().getContextRoot())) {
            //begin 296368    Nested exceptions lost for problems during application startup    WAS.webcontainer    
            List list = webGroup.getWebApps();
            String originalName = "";
            if (list != null && (list.size() > 0)) {
                WebApp originalWebApp = (WebApp) list.get(0);
                originalName = originalWebApp.getWebAppName();
            }
            logger.logp(Level.SEVERE, CLASS_NAME, "addWebApplication", "context.root.already.in.use", new Object[] { displayName, contextRoot, originalName, displayName });
            throw new WebAppNotLoadedException("Context root " + contextRoot + " is already bound. Cannot start application " + displayName);
            // end 296368    Nested exceptions lost for problems during application startup    WAS.webcontainer    

        } else {
            webGroup = wg;
            // begin LIDB2356.1:	WebContainer work for incorporating SIP
            deployedModule.getWebAppConfig().setVirtualHostName(this.getName());

            wgConfig.setWebAppHost(this);
            // end LIDB2356.1:	WebContainer work for incorporating SIP 
            webGroup.initialize(wgConfig);
        }

        try {
            webGroup.addWebApplication(deployedModule, extensionFactories);
            
            Object[] args = { displayName, vHostConfig.toString() };
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.INFO, CLASS_NAME, "addWebApplication", "module.[{0}].successfully.bound.to.virtualhost.[{1}]", args);
            }
        } catch (Throwable t) {
            //PI58875
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                logger.logp(Level.FINE, CLASS_NAME,"addWebApplication",  "error adding web app ["+ displayName +"]");
            }
            webGroup.destroy();
            webGroup = null;
            //PI58875 end 

            //requestMapper.removeMapping(contextRoot);
            //Do not need to remove mapping because we wait until we're sure we should add it!
            //PK67698 removeMapping(contextRoot); 
            throw new WebAppNotLoadedException(t.getMessage(), t); // 296368 added rootCause to newly created exception.
        }

        //PK67698 Start
        try {
            addMapping(contextRoot, webGroup);
            webGroup.notifyStart();
        } catch (Exception exc) {
            // begin 296368    Nested exceptions lost for problems during application startup    WAS.webcontainer    
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "addWebApplication", "error adding mapping ", exc); /* @283348.1 */
            }
            webGroup.destroy();
            throw new WebAppNotLoadedException("Context root " + contextRoot + " mapping unable to be bound. Application " + displayName + " unavailable.", exc);
            // end 296368    Nested exceptions lost for problems during application startup    WAS.webcontainer    
        }
        //PK67698 End
    }
    
    public static String makeMappingContextRoot(String contextRoot) {
        String cRoot = contextRoot;
        if (cRoot.endsWith("/") && !cRoot.equals("/"))
            cRoot = contextRoot.substring(0, cRoot.length() - 1);

        if (cRoot.equals("/"))
            cRoot += "*";
        else
            cRoot += "/*";

        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "makeMapingContextRoot", "original-> " + contextRoot + ", matcher->" + cRoot);
        }
        return cRoot;
    }

    public static String makeProperContextRoot(String contextRoot) {
        String cRoot = contextRoot;
        if (cRoot.endsWith("/") && !cRoot.equals("/"))
            cRoot = contextRoot.substring(0, cRoot.length() - 1);

        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "makeProperContextRoot", "original-> " + contextRoot + ", matcher->" + cRoot);
        }
        return cRoot;
    }

    //END: NEVER INVOKED BY WEBSPHERE APPLICATION SERVER (Common Component Specific)

    /**
     * Method getMimeType.
     * 
     * @param file
     * @return String
     */
    public String getMimeType(String withDot, String withoutDot) {
        String type = vHostConfig.getMimeType(withoutDot);

        if (type == null)
            type = vHostConfig.getMimeType(withDot);

        return type;
    }

    /**
     * Method getSessionContext.
     * 
     * @param moduleConfig
     * @param webApp
     * @return IHttpSessionContext
     */
    @SuppressWarnings("unchecked")
    public IHttpSessionContext getSessionContext(DeployedModule moduleConfig, WebApp webApp, ArrayList[] listeners) throws Throwable {
        //System.out.println("Vhost createSC");
        return ((WebContainer) parent).getSessionContext(moduleConfig, webApp, this.vHostConfig.getName(), listeners);
    }

    /**
     * Method findContext.
     * 
     * @param path
     * @return ServletContext
     */
    public ServletContext findContext(String path) {
        WebGroup g = (WebGroup) requestMapper.map(path);
        if (g != null) {
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "findContext", "WebGroup ->" + g.getName());
            }
            return g.getContext();
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "findContext", "WebGroup mapping not found ");
            }
            return null;
        }
    }

    //PK37449 synchronizing method
    public synchronized void destroy() {
        super.destroy();
        requestMapper = null;
    }

    public void handleRequest(ServletRequest req, ServletResponse res)
                    throws Exception {
        //Begin 280335, Context-roots with dbcs characters are failed to be resolved.
        //Begin 293696    ServletRequest.getPathInfo() fails    WASCC.web.webcontainer
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) ((IExtendedRequest) req).getWebAppDispatcherContext();
        String reqURI = dispatchContext.getDecodedReqUri();
//		String undecodedURI = ((Request) req).getRequestURI();
//		String reqURI = URLDecoder.decode(undecodedURI,WebContainer.getWebContainer().getURIEncoding());
//		((Request) req).getWebAppDispatcherContext().setDecodedReqUri(reqURI);
//		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
//			logger.logp(Level.FINE, CLASS_NAME,"handleRequest", "Looking for webgroup for --> (not decoded="+undecodedURI + "), (decoded=" + reqURI +")");
//		}
        //End 293696    ServletRequest.getPathInfo() fails    WASCC.web.webcontainer
        //End 280335
        RequestProcessor g = (RequestProcessor) requestMapper.map(reqURI);
        if (g != null) {
            g.handleRequest(req, res);
        } else {
            throw new WebGroupVHostNotFoundException(reqURI);
        }
    }

    /**
     * Method removeWebApplication.
     * 
     * @param deployedModule
     */
    public void removeWebApplication(DeployedModule deployedModule) {
        String contextRoot = deployedModule.getContextRoot();
        //begin 280649    SERVICE: clean up separation of core and shell    WASCC.web.webcontainer : reuse with other VirtualHost impls.
        removeWebApplication(deployedModule, contextRoot);
        //end 280649    SERVICE: clean up separation of core and shell    WASCC.web.webcontainer : reuse with other VirtualHost impls.

    }

    //begin 280649    SERVICE: clean up separation of core and shell    WASCC.web.webcontainer : reuse with other VirtualHost impls.   
    public void removeWebApplication(DeployedModule deployedModule, String contextRoot) {
        //boolean restarting = deployedModule.isRestarting();
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "removeWebApplication", "ContextRoot : " + contextRoot);
      
        String ct = makeProperContextRoot(deployedModule.getContextRoot()); // proper
        String mapRoot = makeMappingContextRoot(ct);

        //PK37449 adding synchronization block
        // removeMappedObject uses the mapped root (the same format that was used to add it)
        // removeMappedObject is synchronized, and returns the object that was removed from the map (if there was one)
        WebGroup webGroup = (WebGroup) removeMappedObject(mapRoot);

        if (webGroup != null) {
            // Begin 284644, reverse removal of web applications from map to prevent requests from coming in after app removed
            // Liberty: removed redundant call to remove mapping: call to removeMappedObject does what is needed
            
            webGroup.removeWebApplication(deployedModule);
            // End 284644, reverse removal of web applications from map to prevent requests from coming in after app removed

            //PK37449 adding trace and call to removeSubContainer() from AbstractContainer.  
            //        The call that was in WebGroup was removed.
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME, "removeWebApplication", "name: " + webGroup.getName());
            
            //if (!restarting)
            removeSubContainer(webGroup.getName());
            //PK37449 end
        }
    }

    //end  280649    SERVICE: clean up separation of core and shell    WASCC.web.webcontainer

    protected synchronized void addMapping(String contextRoot, WebGroup group) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "addMapping", " contextRoot -->" + contextRoot + " group -->" + group.getName() + " : " + this.hashCode());
        }
        requestMapper.addMapping(contextRoot, group);
    }

    protected synchronized void removeMapping(String contextRoot) {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "removeMapping", " contextRoot -->" + contextRoot + " : " + this.hashCode());
        }
        requestMapper.removeMapping(contextRoot);
    }
    
    protected synchronized Object removeMappedObject(String contextRoot) {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "removeMappedObject", " contextRoot -->" + contextRoot+ " : " + this.hashCode());
        }
        Object o = requestMapper.map(contextRoot);
        removeMapping(contextRoot);
        
        return o;
    }
    
    protected void addSecureRedirect(ServletRequest req, String hostAlias) {}
}
