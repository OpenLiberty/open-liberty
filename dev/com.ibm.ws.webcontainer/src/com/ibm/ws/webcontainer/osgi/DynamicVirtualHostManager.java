/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.wsspi.http.VirtualHost;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * The VirtualHostManager bridges between the {@link com.ibm.ws.webcontainer.osgi.DynamicVirtualHost} and
 * the dynamic {@link com.ibm.wsspi.http.VirtualHost}.
 * <p>
 * For app deployment and management, a {@link com.ibm.ws.webcontainer.osgi.DynamicVirtualHost} will
 * always be created (at first request for a virtual host of that name). Requests will not be served
 * to that virtual host until the {@link com.ibm.wsspi.http.VirtualHost} counterpart is
 * available.
 * <p>
 * When a {@link com.ibm.wsspi.http.VirtualHost} is set, all contexts roots managed by the
 * DynamicVirtualHost will be added to it.
 */
public class DynamicVirtualHostManager implements Runnable {

    private static final TraceComponent tc = Tr.register(DynamicVirtualHostManager.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    /** Concurrent map for thread safe iteration: synchronization should be used when bridging maps */
    private final ConcurrentHashMap<String, DynamicVirtualHost> hostMap = new ConcurrentHashMap<String, DynamicVirtualHost>();

    /** Concurrent map for thread safe iteration: synchronization should be used when bridging maps */
    private final ConcurrentHashMap<String, VirtualHost> transportMap = new ConcurrentHashMap<String, VirtualHost>();

    private volatile WsLocationAdmin locationService = null;
    
    private volatile ScheduledExecutorService schedExecutor = null;

    private Map<String, Set<String>> pluginCfgVhostUris = null;
    private final static String PLUGIN_CFG = "plugin-cfg.xml";

    public void activate(ComponentContext compcontext) {
        // read pluginCfg
        PluginParser pp = new PluginParser();
        WsResource pluginCfgXml = locationService.getServerResource(PLUGIN_CFG);
        if (pluginCfgXml != null) {
            try {
                pp.parse(pluginCfgXml);
                pluginCfgVhostUris = pp.getVhostUris();
            } catch (IOException e) {
            }
        }
        
        // Schedule a task to run after 30 seconds to see if any virtual hosts
        // that applications reference are missing.. 
        schedExecutor.schedule(this, 30, TimeUnit.SECONDS);
    }
    
    @Override
    public void run() {
        List<String> missingHosts = new ArrayList<String>();

        // runnable used for scheduled executor.. 
        for (DynamicVirtualHost host : hostMap.values()) {
            String name = host.getName();
            if ( transportMap.get(name) == null ) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Missing host = " + host.hashCode() + " : this = " + this);
                }
                if (!name.startsWith("springBootVirtualHost-")) {
                    // Ignore missing virtual hosts for Spring Boot.
                    // Spring Boot support will either use the existing configuration of
                    // a virtual host with the correct ID or generate a new one on the fly
                    // there is no need to post a warning for a missing one after some period
                    // of time.  This has proven to introduce a timing issue when virtual
                    // host configuration is dynamically created.
                    missingHosts.add(name);
                }
            }
        }
        
        // If there are missing hosts, issue a warning message to indicate that virtual hosts
        // referenced by applications aren't present in the config. This only happens at startup, 
        // not w/ dynamically added applications later.. but at least there is _some_ indication
        // that something might be missing from the config.. 
        // For long lived server shutdowns, avoid throwing these exceptions since the Virtual 
        // Hosts might have been cleared. 
        if ( !missingHosts.isEmpty() && !FrameworkState.isStopping()) {
            Tr.warning(tc, "UNKNOWN_VIRTUAL_HOST", missingHosts);
        }
    }

    /**
     * Called when the webcontainer is deactivated. This will clean up
     * all maps within the host manager to facilitate garbage collection
     * and clean up.
     */
    public void purge() {

        synchronized(this){
            // Clear all references to VirtualHost objects
            for (DynamicVirtualHost host : hostMap.values()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Purge host : " + host.hashCode() + ", this : " +this);
                }
                host.getHostConfiguration().setConfiguration(null);
            }

            hostMap.clear();
        }
        //do not clear the transport map. 
        //DS looks after the transport map via set/unset/updateVirtualHost
        //transportMap.clear();
    }
    
    public void purgeHost(String hostName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "purgeHost","hostName ->["+ hostName+ "] ,this : " +this);
        }
        synchronized(this){
            DynamicVirtualHost host = hostMap.get(hostName);
            if (host!=null) {
                Iterator<String> contexts = host.getHostConfiguration().getActiveContext();
                if(!contexts.hasNext()) {
                    host.getHostConfiguration().setConfiguration(null);
                    hostMap.remove(hostName);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Remove host from map : " + hostName + ",host : " + host.hashCode() + ", this : " + this);
                    }
                }    
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "purgeHost");
        }

    }

    /**
     * Return a DynamicVirtualHost for the requested name.
     * <p>
     * This method will never return null. Created DynamicVirtualHost objects
     * will not be released until the WebContainer is deactivated in order to ensure
     * correct and predictable behavior when adding or configurating applications.
     * 
     * @param name VirtualHost name
     * @param instance WebContainer instance that this VirtualHost
     */
    public DynamicVirtualHost getVirtualHost(String name, WebContainer parent) {
        if (name == null) {
            name = "default_host";
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getVirtualHost","name ->["+ name+ "] , parent ->["+ parent + "] ,this : " +this);
        }        
        DynamicVirtualHost vhost = null;
        boolean setpredefinedCtxRoots = false;
        synchronized(this){
            // Always return a virtual host: construct one if it is not present.
            vhost = hostMap.get(name);
            if (vhost == null && parent != null) {
                DynamicVirtualHostConfiguration vhostCfg = new DynamicVirtualHostConfiguration(name);
                vhost = new DynamicVirtualHost(vhostCfg, parent);
                DynamicVirtualHost prev = hostMap.putIfAbsent(name, vhost);
                // keep previous value if one beat us to the punch
                if (prev != null)
                    vhost = prev;

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    if (prev==null)
                        Tr.debug(tc, "add host to map " + vhost.hashCode() + ", this :" + this );
                    else Tr.debug( tc, "found host in map " + vhost.hashCode() + ", this : " + this);
                }

                // Check/set the virtual host config.. 
                vhost.getHostConfiguration().setConfiguration(transportMap.get(name));
                setpredefinedCtxRoots = true;              
            }
        }
        
        if (setpredefinedCtxRoots && pluginCfgVhostUris != null) {          
            Set<String> predefinedCtxRoots = pluginCfgVhostUris.get(name);
            vhost.setPredefinedContextRoots(predefinedCtxRoots);
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Virtual Host created " + name, vhost);
            }
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getVirtualHost", vhost);
        }
        return vhost;
    }

    // Required reference
    protected void setLocationService(WsLocationAdmin locAdmin) {
        locationService = locAdmin;
    }

    protected void unsetLocationService(WsLocationAdmin locAdmin) {}
    
    // Required reference
    protected void setScheduledExececutor(ScheduledExecutorService schedExec) {
        schedExecutor = schedExec;
    }

    protected void unsetScheduledExececutor(ScheduledExecutorService schedExec) {}


    protected void setVirtualHost(com.ibm.wsspi.http.VirtualHost vhost) {
        String name = vhost.getName();

        // add virtual host to the transport map
        transportMap.put(name, vhost);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Virtual Host registered " + vhost);
        }

        // see if we should update a dynamic virtual host:
        DynamicVirtualHost dynamicHost = hostMap.get(name);
        if (dynamicHost != null) {
            updateDynamicHost(dynamicHost, vhost);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc,"Set host : " + dynamicHost.hashCode() + ", this : " + this);
            }

        }    
    }

    protected void updatedVirtualHost(com.ibm.wsspi.http.VirtualHost vhost) {
        String name = vhost.getName();

        // add virtual host to the transport map
        transportMap.put(name, vhost);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Virtual Host updated " + vhost);
        }

        // see if we should update a dynamic virtual host:
        DynamicVirtualHost dynamicHost = hostMap.get(name);
        if (dynamicHost != null) {
            updateDynamicHost(dynamicHost, vhost);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Update host : " + dynamicHost.hashCode() + ", this : " + this);
            }

        }    
    }

    private void updateDynamicHost(DynamicVirtualHost dynamicHost, VirtualHost vhost) {
        DynamicVirtualHostConfiguration config = dynamicHost.getHostConfiguration();
        config.setConfiguration(vhost);

        Iterator<String> activeContexts = config.getActiveContext();

        // add all known context roots to the virtual host
        while (activeContexts.hasNext())
            vhost.addContextRoot(activeContexts.next(), dynamicHost);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Update virtual host configuration", vhost, dynamicHost, activeContexts);
        }
    }

    protected void unsetVirtualHost(com.ibm.wsspi.http.VirtualHost vhost) {
        String name = vhost.getName();

        // only remove if the value associated with the name is the same
        // as the argument to this method.. 
        if (transportMap.remove(name, vhost)) {
            // there is some cleanup to do if the value was removed
            DynamicVirtualHost dynamicHost = hostMap.get(name);
            if (dynamicHost != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unset host : " + dynamicHost.hashCode() + ", this : " + this);
                }
                dynamicHost.getHostConfiguration().setConfiguration(null);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Removed virtual host configuration", vhost, dynamicHost);
            }
        }
    }

    public Iterator<DynamicVirtualHost> getVirtualHosts() {
        return hostMap.values().iterator();
    }
}
