/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.annotations.*;
import org.osgi.service.event.EventAdmin;

import com.ibm.sip.util.log.*;
import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.channelfw.osgi.ChannelFactoryProvider;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.sip.container.internal.SipContainerComponent;
import com.ibm.ws.sip.stack.transport.chfw.GenericChannelProvider;
import com.ibm.ws.sip.stack.transport.virtualhost.SipVirtualHostAdapter;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.channelfw.ChannelConfiguration;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.kernel.service.utils.*;

import io.openliberty.netty.internal.NettyFramework;
import io.openliberty.netty.internal.tls.NettyTlsProvider;

//We need the immediate=true flag because of the race condition between the point when Channel Framework calls to the "started"
//callback and point when we take listening point when Container starts. 
@Component(configurationPid = "com.ibm.ws.sip.endpoint", configurationPolicy = ConfigurationPolicy.OPTIONAL, service = { GenericEndpointImpl.class }, immediate = false, property = { "service.vendor=IBM" })
public class GenericEndpointImpl {
    
	final public static String s_UDP_PORT = "sipUDPPort";
	final public static String s_TCP_PORT = "sipTCPPort";
	final public static String s_TLS_PORT = "sipTLSPort";

	private ConfigurationAdmin configAdminRef;
	
	final public static String s_defaultSipEndpointid = "defaultSipEndpoint";
	final public static String s_sipEndpointFactoryId = "com.ibm.ws.sip.endpoint";
	
	/**
	 * flag that the deactivate called as results of forcibly removing
	 * of defaultSipEndpoint endpoint configuration from configurationAdmin
	 */
	private boolean isForcedDefaultEndpointIdDeactivate = false;
	
	private static GenericChannelProvider channelProvider = null;
	
	/** number of retries in case of bind failure */
	// public static final String BIND_RETRIES = "javax.sip.bind.retries";
	// public static final int BIND_RETRIES_DEFAULT = 60;
	private static final String RETRIES = "bindRetries";

	/** delay between retries, in milliseconds */
	// public static final String BIND_RETRY_DELAY =
	// "javax.sip.bind.retry.delay";
	// public static final int BIND_RETRY_DELAY_DEFAULT = 5000;
	private static final String RETRY_DELAY = "bindRetryDelay";

	/** Required, dynamic tcpOptions: unmodifiable map, multiple present */
	private static ChannelConfiguration tcpOptions = null;

	/** Required, dynamic httpOptions: unmodifiable map, multiple present */
	private static ChannelConfiguration udpOptions = null;

	/**
	 * Required, dynamic reference to an executor service to schedule chain
	 * operations
	 */
	private ExecutorService executorService = null;

	/**
	 * Optional, dynamic reference to an SSL channel factory provider: used to
	 * start/stop SSL chains
	 */
	private ChannelFactoryProvider sslSupport = null;
	
	/** Optional, dynamic reference to NettyTlsProvider service */
	private static NettyTlsProvider tlsProviderService = null;

	/** Optional, dynamic reference to sslOptions: multiple present. */
	private static ChannelConfiguration sslOptions = null;

	/** Event service reference -- required */
	private static EventAdmin eventService = null;
	
        /** Class Logger */
        private static final LogMgr c_logger = Log.get(GenericEndpointImpl.class);
	
	private static final int DEACTIVATED = 1;
	private static final int ENABLED = 2;
	private static final int DISABLED = 4;

	/**
	 * Both this endpoint and the managed chains use something like a state
	 * machine to control/ order their operations: these flags ensure any
	 * activity queued to other threads does not proceed if the target state
	 * changed in the meanwhile.
	 */
	private final AtomicInteger endpointState = new AtomicInteger(DISABLED);

	/** Required, static Channel framework reference */
	private static NettyFramework m_nettyBundle = null;
	
        /** Required, static Channel framework reference */
        private static CHFWBundle m_chfw = null;

	/** Current endpoint configuration */
	private volatile Map<String, Object> endpointConfig = null;

	/** "True" when this endpoint is started */
	private volatile boolean endpointStarted = false;

	/** "localhost" value defined in the configuration */
	private volatile String host = "localhost";

	/** TCP port value defined in the configuration */
	private volatile int tcpPort = -1;

	/** UDP port value defined in the configuration */
	private volatile int udpPort = -1;

	/** TLS port value defined in the configuration */
	private volatile int tlsPort = -1;

	/** number of retries in case of bind failure, default is 60 */
	public volatile int retries = 60;

	/** delay between retries, in milliseconds, default is 5000 */
	public volatile int retry_delay = 5000;

	/** topicString */
	private volatile String topicString = null;

	/** Name of this Endpoint */
	private volatile String name = null;
	
	/** Reference to the TCP chain relates to this Endpoint */
	private GenericChainBase _genericTCPChain;

	/** Reference to the UDP chain relates to this Endpoint */
	private GenericChainBase _genericUDPChain;

	/** Reference to the TLS chain relates to this Endpoint */
	private GenericChainBase _genericTLSChain;

	/** lock which is used to run synchronized runnable actions */
	private final Object actionLock = new Object() {
	};

	/** queue that holds the actions to run */
	private final LinkedList<Runnable> actionQueue = new LinkedList<Runnable>();

	/** Reference to the Future action */
	private Future<?> actionFuture = null;

        /**
         * 
         * @return ChannelFramework reference
         */
        public static ChannelFramework getChannelFramework() {
            return m_chfw.getFramework();
        }

        /**
         * 
         * @return WsByteBufferPoolManager related to this channel Framework
         */
        public static WsByteBufferPoolManager getBufferManager() { 
            return m_chfw.getBufferManager();
        }

	/**
	 * action runner
	 */
	private final Runnable actionsRunner = new Runnable() {
		@Override
		@Trivial
		public void run() {
			Runnable r = null;
			for (;;) {
				synchronized (actionQueue) {
					r = actionQueue.poll();
					if (r == null) {
						actionFuture = null;
						return;
					}
				}

				r.run();
			}
		}
	};

	/**
	 * Perform the stopAction
	 */
	private final Runnable stopAction = new Runnable() {
		@Override
		@Trivial
		public void run() {
			synchronized (actionLock) {
				// Always allow stops.
				if (c_logger.isTraceDebugEnabled())
					c_logger.traceDebug("EndpointAction: stopping chains");
			}
		}

		public String toString() {
			return "Stop";
		}
	};

	/**
	 * Perform the stopSipsOnlyAction
	 */
	private final Runnable stopSipsOnlyAction = new Runnable() {
		@Override
		@Trivial
		public void run() {
			synchronized (actionLock) {
				// Always allow stops.
				if (c_logger.isTraceDebugEnabled())
					c_logger.traceDebug("EndpointAction: stopping sips chain");

			}
		}

		public String toString() {
			return "Stop sips";
		}
	};

	/**
	 * Perform the stopSipsOnlyAction Called when this Endpoint was modified or
	 * when Chain was started.
	 */
	private final Runnable updateAction = new Runnable() {
		@Override
		@Trivial
		public void run() {
			synchronized (actionLock) {
				// only try to update the chains if the endpoint is
				// enabled/started and framework is good
				if (endpointStarted && endpointState.get() == ENABLED
						&& FrameworkState.isValid()) {
					if (c_logger.isTraceDebugEnabled())
						c_logger.traceDebug("EndpointAction: updating chains "
								+ GenericEndpointImpl.this, _genericUDPChain,
								_genericTCPChain, _genericTLSChain);

						_genericUDPChain.update();
						_genericTCPChain.update();
						_genericTLSChain.update();
						
					}
				}
		}

		public String toString() {
			return "update";
		}
	};


	/**
	 * Activate this Endpoint
	 */
	@Activate
	public void activate(Map<String, Object> properties){
		Object cid = properties.get("component.id");
		name = (String) properties.get("id");

		if (name == null)
			name = "sipEndpoint-" + cid;

		// switch between chfw and netty implementations
		if (useNetty()) {
   	            _genericTCPChain = new com.ibm.ws.sip.stack.transport.netty.GenericTCPChain(this, false);
	            _genericUDPChain = new com.ibm.ws.sip.stack.transport.netty.GenericUDPChain(this);
	            _genericTLSChain = new com.ibm.ws.sip.stack.transport.netty.GenericTCPChain(this, true);
		} else {
                _genericTCPChain = new com.ibm.ws.sip.stack.transport.chfw.GenericTCPChain(this, false);
                _genericUDPChain = new com.ibm.ws.sip.stack.transport.chfw.GenericUDPChain(this);
                _genericTLSChain = new com.ibm.ws.sip.stack.transport.chfw.GenericTCPChain(this, true);
		}


		if(isNeedToActivateSipEndpoint(name)) {
			if (c_logger.isEventEnabled()) {
				c_logger.event("GEP activate: " + this + " properties="
						+ properties);
			}
			
			if (!useNetty() && channelProvider == null) {
				try {
					channelProvider = new GenericChannelProvider(
							SipContainerComponent.getContext().getBundleContext());
				} catch (Exception e) {
					if (c_logger.isEventEnabled()) {
						c_logger.event("Failed to create ChannelProvider: "
								+ this);
					}

					e.printStackTrace();
					return;
				}
			}
			
			if (c_logger.isEventEnabled()) {
				c_logger.event("activate sipEndpoint " + this);
			}
			
		    if (useNetty()) {
		            if (udpOptions != null) {
		                ((com.ibm.ws.sip.stack.transport.netty.GenericChain)_genericUDPChain).init(name, cid, m_nettyBundle, "InboundUDPChain");
		            }

		            if (tcpOptions != null) {
		                ((com.ibm.ws.sip.stack.transport.netty.GenericChain)_genericTCPChain).init(name, cid, m_nettyBundle, "InboundTCPChain");
		            }

		            if (sslOptions != null) {
		                ((com.ibm.ws.sip.stack.transport.netty.GenericChain)_genericTLSChain).init(name, cid, m_nettyBundle, "InboundTLSChain");
		            }
    	    } else {
    	            if (udpOptions != null) {
    	                ((com.ibm.ws.sip.stack.transport.chfw.GenericChain)_genericUDPChain).init(name, cid, m_chfw, "InboundUDPChain");
    	            }
    
    	            if (tcpOptions != null) {
    	                ((com.ibm.ws.sip.stack.transport.chfw.GenericChain)_genericTCPChain).init(name, cid, m_chfw, "InboundTCPChain");
    	            }
    
    	            if (sslOptions != null) {
    	                ((com.ibm.ws.sip.stack.transport.chfw.GenericChain)_genericTLSChain).init(name, cid, m_chfw, "InboundTLSChain");
    	            }
    	    }
			startChains(properties);
			
			try {
				SipVirtualHostAdapter.addSipEndpointHostAliasesToVH(properties,
														isSslEnabled(),
														configAdminRef);
			} catch (Exception e) {
				handleSipVirtualHostException("Adding SIP endpoint to virtual host failed.",
											  Situation.SITUATION_CREATE,
											  e);
			} 
		}
		else {
			isForcedDefaultEndpointIdDeactivate = true;
			
			if (c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("INFO: defaultSipEndpoint endpoint won't be activated as other sipendpoint is configured on the server!!!");
			}

			//Do not remove defaultSipEndpont ID from configuration
			//when other sipEndpoints are configured.
			//By removing the defaultSipEndpointId from configuretions we
			//also remove the ssl configuration, which results in
			//TLS endpoints not coming online
			//see open-liberty issue #30874
			
			//removeDefaultSipEndpointIdFromConfiguration();
		}
	}

	/**
	 * DS deactivate
	 * 
	 * @param ctx
	 * @param reason
	 */
	@Deactivate
	protected void deactivate(ComponentContext ctx, int reason){
		if(!isForcedDefaultEndpointIdDeactivate){
			if (c_logger.isEventEnabled()) {
				c_logger.event("deactivate SIP Endpoint " + this + ", reason="
						+ reason);
			}

			// the component is being deactivated.
			endpointState.set(DEACTIVATED);

			// Try to get the activity off of the scr deactivate thread
			performAction(stopAction);

			// TODO Liberty
			// sslFactoryProvider.deactivate(ctx);
			// sslOptions.deactivate(ctx);

			// Stop all Chains
			stopChains();
			
			try {
				SipVirtualHostAdapter.removeSipEndpointHostAliasesFromVH(getEndpointOptions(),
														   isSslEnabled(),
														   configAdminRef);
			} catch (Exception e) {
				handleSipVirtualHostException("Removing SIP endpoint from virtual host failed.",
											  Situation.SITUATION_DESTROY,
											  e);
			}
		}
		
	}

	/**
	 * DS Modify
	 * 
	 * @param config
	 */
	@Modified
	protected void modified(Map<String, Object> config) {
		if (c_logger.isEventEnabled()) {
			c_logger.event("GEP modified: " + this + " properties="
					+ config);
		}

		if (c_logger.isWarnEnabled()) {
			c_logger.warn("warn.change.in.endpoints", null);
		}
		
		Map<String, Object> previousConfig = getEndpointOptions();
		applyNewConfiguration(config);
		try {
			SipVirtualHostAdapter.updateSipEndpointHostAliasesToVH(previousConfig, 
															 config, 
															 isSslEnabled(), 
															 configAdminRef);
		} catch (Exception e) {
			handleSipVirtualHostException("Modifying SIP endpoint virtual host failed.",
										  null,
										  e);
		} 
	}

	/**
	 * Process new configuration: call updateChains to push out new
	 * configuration.
	 * 
	 * @param config
	 */
	protected void applyNewConfiguration(Map<String, Object> config) {
		boolean endpointEnabled = MetatypeUtils.parseBoolean(
				GenericServiceConstants.ENPOINT_FPID_ALIAS, "enabled",
				config.get("enabled"), true);
		// MAKE THE HOST NAME LOWER CASE
		host = (String) config.get("host");
		host = host == null ? "localhost" : host.toLowerCase();

		tcpPort = MetatypeUtils.parseInteger(
				GenericServiceConstants.ENPOINT_FPID_ALIAS, s_TCP_PORT,
				config.get(s_TCP_PORT), -1);

		udpPort = MetatypeUtils.parseInteger(
				GenericServiceConstants.ENPOINT_FPID_ALIAS, s_UDP_PORT,
				config.get(s_UDP_PORT), -1);

		tlsPort = MetatypeUtils.parseInteger(
				GenericServiceConstants.ENPOINT_FPID_ALIAS, s_TLS_PORT,
				config.get(s_TLS_PORT), -1);

		retries = MetatypeUtils.parseInteger(
				GenericServiceConstants.ENPOINT_FPID_ALIAS, RETRIES,
				config.get(RETRIES), -1);

		retry_delay = MetatypeUtils.parseInteger(
				GenericServiceConstants.ENPOINT_FPID_ALIAS, RETRY_DELAY,
				config.get(RETRY_DELAY), -1);

		String id = (String) config.get("id");
		Object cid = config.get("component.id");
		id = id == null ? "cid_" + cid : id;
		// vhostName = (String) config.get("virtualHost");

		// Notification Topics for chain start/stop
		String topicHost = host;
		if(topicHost.contains("*")){
			topicHost = "ALL";
		}
		else{
			try {
				InetAddress ipAddress = InetAddress.getByName(topicHost);
				if (ipAddress instanceof Inet6Address) {
					// Remove colon from ipv6 address before constructing topicString
					topicHost = topicHost.replace(":", "_");
				} 
				else if (ipAddress instanceof Inet4Address) {
					// Remove dots from ipv4 address before constructing topicString
					topicHost = topicHost.replace(".", "_");
				}
			} catch (UnknownHostException e) {
				if (c_logger.isTraceDebugEnabled()){
					c_logger.traceDebug("Error: GenericEndpointImpl applyNewConfiguration: " + e);}	
			}

		}
		topicString = GenericServiceConstants.TOPIC_PFX
				+ topicHost + "/" + id + "/" + cid;

		if (tcpPort < 0 && tlsPort < 0 && udpPort < 0) {
			endpointEnabled = false;
			c_logger.warn("missingPorts.endpointDisabled", id);
		}

		if (tcpOptions != null && tcpPort >= 0) {
			_genericTCPChain.enable();
		}

		if (udpOptions != null && udpPort >= 0) {
			_genericUDPChain.enable();
		}
		if (tlsPort >= 0 && ((sslSupport != null) || (tlsProviderService != null))) {
			_genericTLSChain.enable();
		}

		// Store the configuration
		endpointConfig = config;

		if (endpointEnabled) {
			// make sure this endpoint is enabled (unless deactivated).
			// it's ok if the endpoint is stopped, the config update will occur
			// @ next start
			endpointState.compareAndSet(DISABLED, ENABLED);

			// Use an update action so they pick up the new settings
			// (the updateAction will trigger activity only if the endpoint was
			// previously started)
			performAction(updateAction);
		} else {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("endpoint disabled: " + id);
			}

			// The endpoint has been disabled-- stop it now
			endpointState.set(DISABLED);
			performAction(stopAction);
		}
	}

	public String getEventTopic() {
		return topicString;
	}

	public Map<String, Object> getEndpointOptions() {
		return endpointConfig;
	}

	public String getHostName() {
		// the chain may not be active/listening yet.. if it is, will return the
		// configured host name
		String activeHost = _genericTCPChain.getActiveHost();
		if (activeHost == null)
			activeHost = _genericTLSChain.getActiveHost();
		if (activeHost == null)
			activeHost = _genericUDPChain.getActiveHost();

		return activeHost == null ? host : activeHost;
	}

	
    /**
     * Set/store the bound ConfigurationAdmin service.
     * it's used for connecting SIP endpoint to virtualhost(See {@link SipVirtualHostAdapter}). 
     * 
     * @param ca Configuration admin.
     */
    @Reference
    protected void setConfigurationAdmin(ConfigurationAdmin ca) {
        if (c_logger.isTraceDebugEnabled()) {
      	    c_logger.traceDebug("ConfigurationAdmin ", ca);
        }
        configAdminRef = ca;
    }

    protected void unsetConfigurationAdmin(ConfigurationAdmin ca) {
        configAdminRef = null;
    }
	
	
	/**
	 * Set/store the bound ConfigurationAdmin service. Also ensure that a
	 * default endpoint configuration exists.
	 * 
	 * "type=SSLChannel" means this will only match SSL channel factory
	 * providers
	 * 
	 * @param ref
	 */
	@Reference(name = "sslSupport", service = ChannelFactoryProvider.class, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL, target = "(type=SSLChannel)")
	protected void setSslSupport(ChannelFactoryProvider config) {
		if (c_logger.isEventEnabled()) {
			c_logger.event("enable ssl support ", this);
		}
		sslSupport = config;
	}

	/**
	 * This is an optional/dynamic reference: if this goes away, the CFW will
	 * eventually stop the SSL chain (factory will be removed)
	 * 
	 * @param ref
	 *            ConfigurationAdmin instance to unset
	 */
	protected void unsetSslSupport(ServiceReference<ChannelFactoryProvider> ref) {
		if (c_logger.isEventEnabled()) {
			c_logger.event("disable ssl support " + ref.getProperty("type"), this);
		}

		if (sslSupport != null) {
			sslSupport = null;
			//_genericTLSChain.disable();
			// removal of ssl support includes removal of the ssl channel
			// factory
			// the CFW is going to disable this chain once the factory goes
			// away.
		}
	}

	/**
	 * The specific sslOptions is selected by a filter set through metatype that
	 * matches a specific user-configured option set or falls back to a default.
	 * 
	 * @param service
	 */
	@Trivial
	@Reference(name = "sslOptions", service = ChannelConfiguration.class, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	protected void setSslOptions(ChannelConfiguration config) {
		if (c_logger.isEventEnabled()) {
			c_logger.event("set ssl options " + config.getProperty("id"),
					this);
		}
		sslOptions = config;
	}

	@Trivial
	protected void updatedSslOptions(
			ServiceReference<ChannelConfiguration> service) {
		if (c_logger.isEventEnabled()) {
			c_logger.event("update ssl options, not supported yet "
					+ service.getProperty("id"), this);
		}

		// TODO Liberty Anat : Currently we do not support update of the
		// Endpoint on the fly
		// if (endpointConfig != null) {
		// performAction(updateAction);
		// }
	}

	@Trivial
	protected void unsetSslOptions(ChannelConfiguration config) {
		if (c_logger.isEventEnabled()) {
			c_logger.event("unset ssl options = " + config, this);
		}
		if (sslOptions != null) {
			performAction(stopSipsOnlyAction);
			sslOptions = null;
		}
	}

	/**
	 * Return SSL options or null if doesn't exist.
	 * 
	 * @return
	 */
	public static Map<String, Object> getSslOptions() {
		return sslOptions == null ? null : sslOptions.getConfiguration();
	}

	/**
	 * The specific tcpOptions is selected by a filter set through metatype that
	 * matches a specific user-configured option set or falls back to a default.
	 * 
	 * @param config
	 */
	@Trivial
	@Reference(name = "tcpOptions", service = ChannelConfiguration.class, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setTcpOptions(ChannelConfiguration config, Map prop) {
		if (c_logger.isEventEnabled()) {
			c_logger.event("setTcpOptions " + config.getProperty("id"),
					this);
		}
		tcpOptions = config;
	}

	@Trivial
	protected void updatedTcpOptions(ChannelConfiguration config) {
		if (c_logger.isEventEnabled()) {
			c_logger.event("update tcp options  - not supported yet"
					+ config.getProperty("id"), this);
		}
	}

	protected void unsetTcpOptions(ChannelConfiguration config) {
		if (c_logger.isEventEnabled()) {
			c_logger.event("unsetTcpOptions, stoppint TCP and TLS chains "
					+ config.getProperty("id") + this);
		}
		if(!isForcedDefaultEndpointIdDeactivate){
			tcpOptions = null;	
		}
	}

	/**
	 * return TCP Options
	 * 
	 * @return
	 */
	public static Map<String, Object> getTcpOptions() {
		return tcpOptions == null ? null : tcpOptions.getConfiguration();
	}

	/**
	 * The specific httpOptions is selected by a filter set through metatype
	 * that matches a specific user-configured option set or falls back to a
	 * default.
	 * 
	 * @param service
	 */

	@Trivial
	@Reference(name = "udpOptions",// "UDPChannel",
	service = ChannelConfiguration.class, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setUdpOptions(ChannelConfiguration config, Map prop) {
		if (c_logger.isEventEnabled()) {
			c_logger.event("setUdpOptions " + config.getProperty("id"),
					this);
		}
		udpOptions = config;
	}

	protected void unsetUdpOptions(ChannelConfiguration config) {
		if (c_logger.isEventEnabled()) {
			c_logger.event("unsetUdpOptions and stop UDP related chain "
					+ config.getProperty("id"), this);
		}
		//_genericUDPChain.stop();
		
		if(!isForcedDefaultEndpointIdDeactivate){
			udpOptions = null;
		}
	}

	@Trivial
	protected void updatedUdpOptions(ChannelConfiguration config) {
		if (c_logger.isEventEnabled()) {
			c_logger.event("update udp options  - not supported yet"
					+ config.getProperty("id"), this);
		}
		// TODO Liberty Anat : Currently we do not support update of the
		// Endpoint on the fly
	}

	/**
	 * Return UDP Options
	 * 
	 * @return
	 */
	public static Map<String, Object> getUdpOptions() {

		return udpOptions == null ? null : udpOptions.getConfiguration();
	}
	
    @Reference(name = "NettyTlsProvider",
            service = NettyTlsProvider.class,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY,
            cardinality = ReferenceCardinality.OPTIONAL)
    protected void setNettyTlsProvider(NettyTlsProvider tls) {
        if (c_logger.isEventEnabled()) {
            c_logger.event("setNettyTlsProvider " + tls);
        }
        tlsProviderService = tls;
    }
    
    protected void unsetNettyTlsProvider(NettyTlsProvider tls) {
        if (c_logger.isEventEnabled()) {
            c_logger.event("unsetSipTlsProvider" + tls);
        }
        tlsProviderService = null;
    }

    @Reference(name = "nettyBundle",
            service = NettyFramework.class,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY,
            cardinality = ReferenceCardinality.OPTIONAL)
	protected void setNettyBundle(NettyFramework bundle) {
		if (c_logger.isEventEnabled()) {
			c_logger.event("setNettyBundle " + bundle);
		}
		m_nettyBundle = bundle;
	}
    
    public static NettyFramework getNettyBundle() {
    	return m_nettyBundle;
    }

	/**
	 * This is a required static reference, this won't be called until the
	 * component has been deactivated
	 * 
	 * @param bundle instance to unset
	 */
	protected void unsetNettyBundle(NettyFramework bundle) {
		if (c_logger.isEventEnabled()) {
			c_logger.event("unsetNettyBundle and stop all open chains"
					+ bundle);
		}
		if(!isForcedDefaultEndpointIdDeactivate){
			m_nettyBundle = null;	
		}
		
		performAction(stopAction);
	}

	/**
         * DS method for setting the required channel framework service.
         * 
         * @param bundle
         */
        @Reference(name = "chfwBundle",
            service = CHFWBundle.class,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY,
            cardinality = ReferenceCardinality.OPTIONAL)
        protected void setChfwBundle(CHFWBundle bundle) {
            if (c_logger.isEventEnabled()) {
                c_logger.event("setChfwBundle " + bundle);
            }
            m_chfw = bundle;
        }

        /**
         * This is a required static reference, this won't be called until the
         * component has been deactivated
         * 
         * @param bundle
         *            CHFWBundle instance to unset
         */
        protected void unsetChfwBundle(CHFWBundle bundle) {
            if (c_logger.isEventEnabled()) {
                c_logger.event("unsetChfwBundle and stop all open chains" + bundle);
            }

            if(!isForcedDefaultEndpointIdDeactivate){
                m_chfw = null;  
            }
        
            performAction(stopAction);
        
        }

        /**
         * Returns reference to ChannelFramework
         * 
         * @return
         */
        protected CHFWBundle getChfwBundle() {
            if (c_logger.isEventEnabled()) {
                c_logger.event("chfwBundle = " + m_chfw);
            }
            return m_chfw;
        }

	/**
	 * DS method for setting the required dynamic executor service reference.
	 * 
	 * @param bundle
	 */
	@Reference(name = "executorService", service = ExecutorService.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MANDATORY)
	protected void setExecutorService(ExecutorService service) {
		this.executorService = service;
	}

	/**
	 * DS method for clearing the required dynamic event admin reference. This
	 * is a required reference, but will be called if the dynamic reference is
	 * replaced
	 */
	protected void unsetExecutorService(
			ServiceReference<ExecutorService> executorService) {
		this.executorService = null;
	}

	/**
	 * setEventAdmin - @param service
	 */
	@Reference(name = "eventService", service = EventAdmin.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
	protected void setEventAdmin(EventAdmin service) {
		if (c_logger.isEventEnabled()) {
			c_logger.event("setEventAdmin " + this);
		}
		eventService = service;
	}

	/**
	 * unsetEventAdmin @param service
	 */
	protected void unsetEventAdmin(EventAdmin service) {
		if (c_logger.isEventEnabled()) {
			c_logger.event("unsetEventAdmin " + service);
		}
		
		if(!isForcedDefaultEndpointIdDeactivate){
			eventService = null;
		}
		
	}

	/**
	 * @return
	 */
	public static EventAdmin getEventAdmin() {
		return eventService;
	}

	/**
	 * If we can get the chain activity off the SCR action thread, we should
	 * 
	 * @param action
	 *            Runnable action to execute
	 */
	@Trivial
	private void performAction(Runnable action) {
		if (c_logger.isEventEnabled()) {
			c_logger.event("action = " + action);
		}
		if (executorService == null) {
			// If we can't find the executor service, we have to run it in
			// place.
			action.run();
		} else {
			// If we can find the executor service, we'll add the action to
			// the queue.
			// If the actionFuture is null (no pending actions), we'll
			// submit the actionsRunner to the executor service to drain the
			// queue.
			synchronized (actionQueue) {
				actionQueue.add(action);
				if (actionFuture == null) {
					actionFuture = executorService.submit(actionsRunner);
				}
			}
		}
	}

	/**
	 * Starting chains
	 */
	private void startChains(Map<String, Object> properties) {
		if (c_logger.isEventEnabled()) {
			c_logger.event("startChains");
		}
		endpointStarted = true;
		applyNewConfiguration(properties);
	}

	/**
	 * Called by VirtualHost to start chains associated with this endpoint.
	 */
	public void stopChains() {

		endpointStarted = false;
		performAction(stopAction);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[@"
				+ System.identityHashCode(this) + ",name=" + name + ",host="
				+ host + ",tcp=" + tcpPort + ",tls=" + tlsPort + ",udp="
				+ udpPort + ",retries=" + retries + ",retry_delay="
				+ retry_delay + ",state=" + endpointState.get() + "]";
	}

	/**
	 * 
	 * @return name of this endpoint
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Checks whether ssl is enabled.
	 * 
	 * @return true if ssl is enabled.
	 */
	private boolean isSslEnabled(){
		return sslOptions != null;
	}
	
	/**
	 * Handles SipVirtualHostAdapter exception.
	 * 
	 * @param message message for component exception.
	 * @param situation situation for the logger message.
	 * @param e occured exception.
	 */
	private void handleSipVirtualHostException(String message, String situation, Exception e){
        if(c_logger.isErrorEnabled()){
        	Object[] params = {this};
        	c_logger.error("error.exception", situation, params, e);
        }
		throw new ComponentException(message);
	}
	
	
	/**
	 * Checks whether need to activate sip endpoint id
	 * @param sipEndpointId sipEndpoint id
	 * @return true whether need to activate sip endpoint id, otherwise false.
	 */
	private boolean isNeedToActivateSipEndpoint(String sipEndpointId) {
		boolean isNeedToActivate;
		if(!name.equals(s_defaultSipEndpointid)){
			isNeedToActivate  = true;
		}
		else{//default sipEndpoint
			isNeedToActivate = !isExistNonDefaultSipEndpointId();
		}
		
		return isNeedToActivate;
	}
	
	/**
	 * Checks whether non default id sip endpoint configuration exist 
	 * @return true whether non default id sip endpoint configuration exist, otherwise false. 
	 */
	private boolean isExistNonDefaultSipEndpointId(){
		try {
			String filter = "(&" + FilterUtils.createPropertyFilter(ConfigurationAdmin.SERVICE_FACTORYPID, s_sipEndpointFactoryId)
	                + "(!" + FilterUtils.createPropertyFilter("id", s_defaultSipEndpointid) + "))";
			
			Configuration[] sipEndpointConf = configAdminRef.listConfigurations(filter);
			
			return sipEndpointConf != null && 
				   sipEndpointConf.length > 0;
		} catch (Exception e) {
			if (c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("isExistNonDefaultSipEndpointId failed", e);
			}
			
			return false;
		}

	}
	
	/**
	 * Removes defaultsipEndpoint configuration from configuationAdmin
	 */
	private void removeDefaultSipEndpointIdFromConfiguration(){
		try {
			String filter = "(&" + FilterUtils.createPropertyFilter(ConfigurationAdmin.SERVICE_FACTORYPID, s_sipEndpointFactoryId)
	                 + FilterUtils.createPropertyFilter("id", s_defaultSipEndpointid) + ")";
			
			Configuration[] sipEndpointConf = configAdminRef.listConfigurations(filter);
			
			if(sipEndpointConf != null && sipEndpointConf.length == 1){
				Configuration defaultIdSipEndpoint = sipEndpointConf[0];
				defaultIdSipEndpoint.delete();
				
				if (c_logger.isTraceDebugEnabled()){
					c_logger.traceDebug("defaultSipEndpoint endpoint was removed from ConfigurationAdmin");
				}
			}
		} catch (Exception e) {
			if (c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("Error occured while defaultSipEndpoint endpoint was removed from ConfigurationAdmin", e);
			}
		}
	}
	
	/**
	 * Query if netty has been enabled for this endpoint
	 * @return true if netty should be used for this endpoint
	 */
	public static boolean useNetty() {
	    return SipContainerComponent.useNetty();
	}
	
	public static NettyTlsProvider getTlsProvider() {
	    return tlsProviderService;
	}
}
