/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.mbeans; 

import java.io.File;
import java.util.Map;

import javax.management.StandardMBean;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.extended.ModuleRuntimeContainer;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;
import com.ibm.ws.webcontainer.httpsession.SessionManager;
import com.ibm.ws.webcontainer.osgi.DynamicVirtualHostManager;
import com.ibm.ws.webcontainer.osgi.WebContainer;
import com.ibm.ws.webserver.plugin.runtime.interfaces.PluginUtilityConfigGenerator;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.webcontainer.osgi.mbeans.GeneratePluginConfig;

/**
 * WebContainer admin task command class. This contains the subcommands that
 * are valid to call from the admin client.
 */
public class GeneratePluginConfigMBean extends StandardMBean implements GeneratePluginConfig, com.ibm.websphere.webcontainer.GeneratePluginConfigMBean, PluginUtilityConfigGenerator, ServerQuiesceListener  {
    private static final String DEFAULT_SERVER_NAME = "defaultServer";
    
    private static final TraceComponent tc = Tr.register(GeneratePluginConfigMBean.class,com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants.TR_GROUP,
                                                         com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants.NLS_PROPS);

    /** Required, static reference to the module runtime container */
    private ModuleRuntimeContainer webContainer;
    /** Required, static reference to the session manager */
    private SessionManager smgr;
    /** required, static reference to the virtual host manager */
    private DynamicVirtualHostManager dynVhostMgr;
    /** required, static reference to the location service */
    protected WsLocationAdmin locMgr;
        
    private Map<String, Object> config = null;
    private BundleContext bundleContext = null;
    
    private boolean serverIsStopping = false;
    private boolean generateInProgress = false;
    /**
     * @return the bundleContext 
     */
    public BundleContext getBundleContext() {
        return bundleContext;
    }
    

    private volatile PluginGenerator pluginGenerator = null;

    /** Configuration constants */
    static final String CFG_CONNECT_TIMEOUT = "connectTimeout";
    static final String CFG_SERVER_IO_TIMEOUT = "serverIOTimeout";
    static final String CFG_EXTENDED_HANDSHAKE = "extendedHandshake";
    static final String CFG_WAIT_FOR_CONTINUE = "waitForContinue";
    /** Attribute names */
    static final String ATTRIBUTE_NAME_CONNECT_TIMEOUT = "ConnectTimeout";
    static final String ATTRIBUTE_NAME_SERVER_IO_TIMEOUT = "IoTimeout";
    static final String ATTRIBUTE_NAME_EXTENDED_HANDSHAKE = "ExtendedHandshake";
    static final String ATTRIBUTE_NAME_WAIT_FOR_CONTINUE = "WaitForContinue";  
       
    /**
     * Constructor.
     */
    public GeneratePluginConfigMBean() {
        super(GeneratePluginConfig.class, false);
        generateInProgress = false;
        serverIsStopping= false;

    }

    /**
     * Activate this component.
     *
     * @param context
     */
    protected void activate(BundleContext context, Map<String, Object> config) {
        bundleContext = context;
        this.config = config;
        modified(config);
    }

    /**
     * Deactivate this component.
     *
     * @param context
     */
    protected void deactivate(ComponentContext context) {
        this.config = null;
    }


    /**
     * DS method for runtime updates to configuration without stopping and
     * restarting the component.
     *
     * @param properties
     */
    protected void modified(Map<String, Object> properties) {
        this.config = properties;
        this.pluginGenerator = null;
    }

    /**
     * Subcommand for creating the plugin-cfg.xml file at runtime.
     */
    public void generateDefaultPluginConfig() {
        generatePluginConfig(null, DEFAULT_SERVER_NAME);
    }
    
    /* (non-Javadoc)
     * @see com.ibm.wsspi.webcontainer.osgi.mbeans.GeneratePluginConfig#generatePluginConfig(java.lang.String, java.lang.String)
     */
    @Override
    public void generatePluginConfig(String root, String serverName) {
        generatePluginConfig(root,serverName,false,null);
        
    }
    
    /**
     * Subcommand for creating the plugin-cfg.xml file at runtime using the
     * user provided Plugin install root and Plugin server name.
     *
     * @param root Plugin install root directory
     * @param name The server name
     * @param utilityRequest True if plugin config creation is being done automatically or via the pluginUtility, False if it is being done because of a call to the mbean
     * @param writeDirectory The directory to write the plugin-cfg.xml file to
     */
    public synchronized void generatePluginConfig(String root, String serverName,boolean utilityRequest, File writeDirectory) {
            
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "generatePluginConfig", "server is stopping = " + serverIsStopping);
        }
    	try {
    		// Method is synchronized so only one generate can be in progress at a time.
    	    generateInProgress = true;
        	if (!serverIsStopping) {
    		
                PluginGenerator generator = pluginGenerator;
                if ( generator == null ) {
                    // Process the updated configuration
                    generator = pluginGenerator = new PluginGenerator(this.config, locMgr, bundleContext);
                }
                generator.generateXML(root, serverName, (WebContainer) webContainer, smgr, dynVhostMgr, locMgr,utilityRequest, writeDirectory); 
        	}   
         } catch (Throwable t) {
            FFDCFilter.processException(t, getClass().getName(), "generatePluginConfig");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Error generate plugin xml: " + t.getMessage());
            }
        } finally {
       	     generateInProgress = false;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "generatePluginConfig", "server is stopping = " + serverIsStopping);
        }

    }

    /** Required static reference: called before activate */
    public void setWebContainer(ModuleRuntimeContainer ref) {
        this.webContainer = ref;
    }

    /** Required static reference: will be called after deactivate. Avoid NPE */
    public void unsetWebContainer(ModuleRuntimeContainer ref) {}

    /** Required static reference: called before activate */
    public void setSessionManager(SessionManager ref) {
        this.smgr = ref;
    }

    /** Required static reference: will be called after deactivate. Avoid NPE */
    public void unsetSessionManager(SessionManager ref) {}
    

    /** Required static reference: called before activate */
    protected void setLocationService(WsLocationAdmin ref) {
        locMgr = ref;
    }

    /** Required static reference: will be called after deactivate. Avoid NPE */
    protected void unsetLocationService(WsLocationAdmin ref) {}

    /** Required static reference: called before activate */
    protected void setVirtualHostMgr(DynamicVirtualHostManager vhostMgr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setVirtualHostMgr : DynamicVirtualHost set : " + vhostMgr.toString());  
        dynVhostMgr = vhostMgr;
    }

    /** Required static reference: will be called after deactivate. Avoid NPE */
    protected void unsetVirtualHostMgr(DynamicVirtualHostManager vhostMgr) {}

    /* (non-Javadoc)
     * @see com.ibm.wsspi.webcontainer.osgi.mbeans.GeneratePluginConfig#getConnectTimeout()
     */
    @Override
    public long getConnectTimeout() {
        if (this.config != null) {
            return (Long) this.config.get(CFG_CONNECT_TIMEOUT);
        }
        return 0;
    }

    /* (non-Javadoc)
     * @see com.ibm.wsspi.webcontainer.osgi.mbeans.GeneratePluginConfig#getIoTimeout()
     */
    @Override
    public long getIoTimeout() {
        if (this.config != null) {
            return (Long) this.config.get(CFG_SERVER_IO_TIMEOUT);
        }
        return 0;
    }

    /* (non-Javadoc)
     * @see com.ibm.wsspi.webcontainer.osgi.mbeans.GeneratePluginConfig#getExtendedHandshake()
     */
    @Override
    public boolean getExtendedHandshake() {
        if (this.config != null) {
            return (Boolean) this.config.get(CFG_EXTENDED_HANDSHAKE);
        }
        return false;
    }

    /* (non-Javadoc)
     * @see com.ibm.wsspi.webcontainer.osgi.mbeans.GeneratePluginConfig#getWaitForContinue()
     */
    @Override
    public boolean getWaitForContinue() {
        if (this.config != null) {
            return (Boolean) this.config.get(CFG_WAIT_FOR_CONTINUE);
        }
        return false;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webserver.plugin.runtime.interfaces.PluginUtilityConfigGenerator#generatePluginConfig(java.lang.String, java.io.File)
     */
    @Override
    public void generatePluginConfig(String serverName, File writeDirectory) {
        // Pass true to utilityRequest since this method will be called from the pluginUtility
        // or by the GeneratePluginConfigListener not by a call to the mbean.
        generatePluginConfig(null,serverName,true,writeDirectory);
        
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webserver.plugin.runtime.interfaces.PluginUtilityConfigGenerator#getPluginConfigType()
     */
    @Override
    public PluginUtilityConfigGenerator.Types getPluginConfigType() {
        return PluginUtilityConfigGenerator.Types.WEBCONTAINER;
    }
    
    /* (non-Javadoc)
     * @see com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener#serverStopping()
     */
    @Override
    public void serverStopping() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "serverStopping", "generate in progress = " + generateInProgress);
        }
        serverIsStopping = true;
        // Wait for up to 20 seconds for plugin generation to complete.
        for (int  i=0 ; i<40 && generateInProgress; i++) {
             try {
                Thread.sleep(500);
             } catch (InterruptedException e) {
                // don't bother sleeping again.
                i=40;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "serverStopping");
        }

    }
}
