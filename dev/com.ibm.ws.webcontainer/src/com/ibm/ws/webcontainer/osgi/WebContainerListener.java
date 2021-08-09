/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi;
 
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.AllServiceListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.ModuleMetaDataListener;
import com.ibm.ws.container.service.metadata.extended.DeferredMetaDataFactory;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
/**
 * This service keeps track of started web applications.
 * The service listens for WebModuleMetaData to be created, and associates the WebModuleMetaData with the application name.
 * When a web application starts successfully, the saved WebModuleMetaData is moved to the list of started modules.
 * When a web application is stopped the saved WebModuleMetaData is removed from our data structures.
 */
@Component(service = {ModuleMetaDataListener.class,
                      AllServiceListener.class,
                      ApplicationStateListener.class,
                      ServerQuiesceListener.class,
                      DeferredMetaDataFactory.class },
           property = { "service.vendor=IBM", "deferredMetaData=WEB", "supportsDeferredInit:Boolean=true" })
public class WebContainerListener implements ModuleMetaDataListener, 
                                             ApplicationStateListener, 
                                             AllServiceListener, 
                                             ServerQuiesceListener,
                                             DeferredMetaDataFactory {
    
    private static final TraceComponent tc = Tr.register(WebContainerListener.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    private J2EENameFactory jeeNameFactory;

    // Map from the application name to the set of WebModuleMetaData objects for each of the web modules in the application
    ConcurrentHashMap<String,ConcurrentHashMap<J2EEName,WebModuleMetaData>> webModulesInStartingApps = new ConcurrentHashMap<String,ConcurrentHashMap<J2EEName,WebModuleMetaData>>();
    ConcurrentHashMap<String,ConcurrentHashMap<J2EEName,WebModuleMetaData>> webModulesInStartedApps = new ConcurrentHashMap<String,ConcurrentHashMap<J2EEName,WebModuleMetaData>>();
    private Object webModulesInStartingApps_lock = new Object() {};
    
    public WebContainerListener(){
    }
    
    /* (non-Javadoc)
     * @see com.ibm.ws.container.service.metadata.ModuleMetaDataListener#moduleMetaDataCreated(com.ibm.ws.container.service.metadata.MetaDataEvent)
     * This method listens for when WebModuleMetaData is first created. We use this as the indication that an application is "starting".
     * Any WebModuleMetaData created here will be added to webModulesInStartingApps.
     */
    @Override
    public void moduleMetaDataCreated(MetaDataEvent<ModuleMetaData> event) throws MetaDataException {
        // We only save meta data if it is for a web module
        if (event.getMetaData() instanceof WebModuleMetaData) {
            String appName = event.getMetaData().getJ2EEName().getApplication();
            // Synchronization needed here, so only the first web module in an app initializes the data structures.
            // The if condition and the logic in the else clause must be run without any other threads changing webModulesInStartingApps
            synchronized (webModulesInStartingApps_lock) {
                if (webModulesInStartingApps.containsKey(appName)) {
                    // Some other web module was already found for this app, so just get the HashMap for this app and add the new metadata  to it 
                    webModulesInStartingApps.get(appName).put(event.getMetaData().getJ2EEName(),(WebModuleMetaData)event.getMetaData());
                }
                else {
                    // First web module for this app, so initialize the set of WebModuleMetaDatas
                    // Store the WebModuleMetaData in webModulesInStartingApps
                    ConcurrentHashMap<J2EEName,WebModuleMetaData>moduleMetadatas = new ConcurrentHashMap<J2EEName,WebModuleMetaData>();
                    moduleMetadatas.put(event.getMetaData().getJ2EEName(),(WebModuleMetaData)event.getMetaData());
                    webModulesInStartingApps.put(appName, moduleMetadatas);
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "WebModule metadata saved for " + event.getMetaData().getJ2EEName());
            }
        }        
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.container.service.state.ApplicationStateListener#applicationStarted(com.ibm.ws.container.service.app.deploy.ApplicationInfo)
     * When an application is started, we must move the metadata from webModulesInStartingApps to webModulesInStartedApps.
     */
    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {       
        // If we can't get the J2EEName of the application, then we don't care about this application.
        String appName = appInfo.getDeploymentName();
        if (null == appName) return;

        // We only care about applications that have had web module metadata created.
        // The remove operation returns null if we don't find this app in webModulesInStartingApps
        ConcurrentHashMap<J2EEName,WebModuleMetaData> modules = webModulesInStartingApps.remove(appName);
        if (null != modules) {
            // We found the metadata for this app, and it has been removed from webModulesInStartingApps.
            // Now take the modules we removed from webModulesInStartingApps, and add them to webModulesInStartedApps
            webModulesInStartedApps.put(appName, modules);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Web application started - " + appName);
            }

        }
    }


    /* (non-Javadoc)
     * @see com.ibm.ws.container.service.state.ApplicationStateListener#applicationStopped(com.ibm.ws.container.service.app.deploy.ApplicationInfo)
     */
    @Override
    @FFDCIgnore(NullPointerException.class)
    public void applicationStopped(ApplicationInfo appInfo) {
      
        // If we can't get the J2EEName of the application, then we don't care about this application.     
        String appName = appInfo.getDeploymentName();
        if (null == appName) return;
        // Handle an app being stopped
        webModulesInStartingApps.remove(appName);
        webModulesInStartedApps.remove(appName);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Web application stopped- " + appName);
        }
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
    }
    
    @Activate
    protected void activate(ComponentContext context) {
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.container.service.state.ApplicationStateListener#applicationStarting(com.ibm.ws.container.service.app.deploy.ApplicationInfo)
     */
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.container.service.state.ApplicationStateListener#applicationStopping(com.ibm.ws.container.service.app.deploy.ApplicationInfo)
     */
    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.container.service.metadata.ModuleMetaDataListener#moduleMetaDataDestroyed(com.ibm.ws.container.service.metadata.MetaDataEvent)
     */
    @Override
    public void moduleMetaDataDestroyed(MetaDataEvent<ModuleMetaData> event) {   
    }

    /**
     * @see com.ibm.ws.container.service.metadata.extended.DeferredMetaDataFactory#createComponentMetaData(java.lang.String)
     */
    @Override
    public ComponentMetaData createComponentMetaData(String identifier) {
        String[] parts = identifier.split("#");

        J2EEName jeeName = jeeNameFactory.create(parts[1], parts[2], null); // ignore parts[0] which is the prefix: WEB

        Map<J2EEName, WebModuleMetaData> wmmdMap = webModulesInStartedApps.get(parts[1]);
        if (wmmdMap == null)
            return null;

        WebModuleMetaData wmmd = wmmdMap.get(jeeName);
        if (wmmd == null)
            return null;

        WebAppConfiguration webAppConfiguration = (WebAppConfiguration) wmmd.getConfiguration();
        return webAppConfiguration.getDefaultComponentMetaData();
    }

    /**
     * @see com.ibm.ws.container.service.metadata.extended.DeferredMetaDataFactory#initialize(com.ibm.ws.runtime.metadata.ComponentMetaData)
     */
    @Override
    public void initialize(ComponentMetaData metadata) {
        WebModuleMetaData wmmd = (WebModuleMetaData) metadata.getModuleMetaData();
        WebAppConfiguration webAppConfiguration = (WebAppConfiguration) wmmd.getConfiguration();
        WebApp webApp = webAppConfiguration.getWebApp();
        if (!webApp.isInitialized())
            try {
                webApp.initialize();
            } catch (Error x) {
                throw x;
            } catch (RuntimeException x) {
                throw x;
            } catch (Throwable x) {
                throw new IllegalStateException(x);
            }
    }

    // Declarative services bind method
    @Reference
    protected void setJEENameFactory(J2EENameFactory svc) {
        jeeNameFactory = svc;
    }

    protected void unsetJ2EENameFactory(J2EENameFactory svc) {
        jeeNameFactory = null;
    }

    /**
     * @return the persistent identifier for the given web app and module name
     */
    public static String getPersistentIdentifierImpl(String app, String module) {
        return "WEB#" + app + "#" + module;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @return "WEB#{appName}#{moduleName}"
     */
    @Override
    public String getMetaDataIdentifier(String appName, String moduleName, String componentName) {
        return getPersistentIdentifierImpl(appName, moduleName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassLoader getClassLoader(ComponentMetaData metaData) {

        ModuleMetaData moduleMetaData = (metaData != null) ? metaData.getModuleMetaData() : null;
        
        if (moduleMetaData instanceof WebModuleMetaData) {
            WebAppConfiguration webAppConfiguration = (WebAppConfiguration) ((WebModuleMetaData)moduleMetaData).getConfiguration();
            return webAppConfiguration.getWebApp().getClassLoader();
            
        } else {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener#serverStopping()
     */
    @Override
    public void serverStopping() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Server is stopping");
        }
        WebContainer wc = (WebContainer) WebContainer.getWebContainer();
        WebContainer.setServerStopping(true);
        if (wc!=null) wc.waitForApplicationInitialization();
    }
}
