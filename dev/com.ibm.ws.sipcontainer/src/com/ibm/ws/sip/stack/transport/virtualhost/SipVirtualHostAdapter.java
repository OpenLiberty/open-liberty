/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.virtualhost;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.sip.SipURI;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.container.matching.SipServletsMatcher;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.virtualhost.VirtualHostAlias;
import com.ibm.ws.sip.container.virtualhost.VirtualHostAliasImpl;
import com.ibm.ws.sip.stack.transport.chfw.GenericEndpointImpl;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;

/**
 * Adapter between SIP endpoint service to virtual host service.
 * {@link GenericEndpointImpl} service life cycle methods (activate, deactivate, modified) will be
 * reflected to the VirtualHostImpl service by run-time configuration update of host aliases
 * using {@link ConfigurationAdmin}.
 * 
 * Exceptional is default_host virtual host which won't be updated with endpoint aliases because it can't be updated in run-time.
 * Host aliases of SIP endpoints which referenced to default_host will be maintained in dedicated list
 * for the use of {@link SipServletsMatcher}.
 * 
 * @author Lior Luker
 * 
 */
public class SipVirtualHostAdapter {
	
	private static final LogMgr c_logger = Log.get(SipVirtualHostAdapter.class);
	
	/** Trace service */
	private static final TraceComponent tc = Tr
			.register(SipVirtualHostAdapter.class);
	
	/** default virtual host id name */
	public  static final String DEFAULT_VH_ID  = "default_host";
	
	/** sip connector virtual host id name */
	private static final String SIP_CONNECTOR_VH_ID = "sipUA_VH";
	
	/** virtual host factory pid */
	private static final String VH_FACTORY_PID = "com.ibm.ws.http.virtualhost";
	
	/** rtcomm connector factory pid */
	private static final String RTCOMM_CONNECTOR_FACTORY_PID = "com.ibm.ws.rtcomm.connector";
	
	/** host alias attribute name */
	private static final String HOST_ALIAS_ATTRIBUTE = "hostAlias";
	
	/** host attribute name */
	private static final String HOST_ATTRIBUTE = "host";
	
	/** service pid attribute name  */
	private static final String SERVICE_PID_ATTRIBUTE = "service.pid";
	
	/** virtual host reference attribute name  */
	private static final String VIRTUAL_HOST_REF_ATTRIBUTE = "virtualhostRef";
	
	/** sip connector flat configuration attribute prefix  */
	private static final String SIP_CONNECTOR_PREFIX = "gateway.";
	
	/** sip connector flat configuration attribute suffix  */
	private static final String ALLOW_FROM_SIPENDPOINT_REF_SUFFIX = ".allowFromSipEndpointRef";
	
	/** default virtual host aliases thread-safe list*/
	private static List<VirtualHostAlias> s_defaultVH_hostAliases = Collections.synchronizedList(new LinkedList<VirtualHostAlias>());
	
	/** sip connector virtual host aliases thread-safe list*/
	private static List<VirtualHostAlias> s_sipConnectorVH_hostAliases = Collections.synchronizedList(new LinkedList<VirtualHostAlias>());
	
	/**Will hold a map in the form of map(host, map(Transport, [ports....]))*/
	private static Map<InetAddress, Map<String, HashSet<Integer>>> sipConnectorEndpoint = new HashMap<InetAddress, Map<String, HashSet<Integer>>>(); 
	
	/**List of Virtual Hosts that applications installed on will never be part of application composition*/
	private static HashSet<String> vhExcludedFromComposition = new HashSet<String>();
	static {
		vhExcludedFromComposition.add(SIP_CONNECTOR_VH_ID);
	}
	
	/**
	 * Adds SIP endpoint host aliases to its virtual host.
	 * 
	 * @param sipEndpointProperties configuration properties of SIP endpoint.
	 * @param isSslEnabled is ssl enabled flag.
	 * @param configAdminRef ConfigurationAdmin reference.
	 * @throws InvalidSyntaxException 
	 * @throws IOException 
	 */
	public static void addSipEndpointHostAliasesToVH(Map<String, Object> sipEndpointProperties, boolean isSslEnabled, ConfigurationAdmin configAdminRef) throws IOException, InvalidSyntaxException{
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Object[] params = {sipEndpointProperties, isSslEnabled};
			Tr.debug(tc, "Add SIP endpoint host aliases to virtual host", params);
		}
		
		boolean isSipConnectorVH = isSipConnectorReferenecedEndpoint(sipEndpointProperties, configAdminRef);
		if(isSipConnectorVH){
			boolean isExistSipUaVH = isExistSipUaVirtualHost(configAdminRef);
			if(!isExistSipUaVH){
				createSipConnectorVirtualHost(sipEndpointProperties, isSslEnabled, configAdminRef);
			}
			else{
				updateSipEndpointVirtualHostAliases(true, sipEndpointProperties, isSslEnabled, configAdminRef);
			}
			addConnectorSipEndpointToList(sipEndpointProperties);
		}
		else{
			updateSipEndpointVirtualHostAliases(true, sipEndpointProperties, isSslEnabled, configAdminRef);
		}
	}

	/**
	 * Storing SIP Connector endpoint for reference
	 * @param sipEndpointProperties
	 * @throws UnknownHostException 
	 */
	private static void addConnectorSipEndpointToList(Map<String, Object> sipEndpointProperties) throws UnknownHostException{
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(null, "addConnectorSipEndpointToList", sipEndpointProperties);
		}
		
		String endpointHost = (String) sipEndpointProperties.get(HOST_ATTRIBUTE);
		
		String tcpPort = String.valueOf(sipEndpointProperties.get(GenericEndpointImpl.s_TCP_PORT));
		addHostTransportPortToConnectorList(endpointHost, "TCP", tcpPort);
		String udpPort = String.valueOf(sipEndpointProperties.get(GenericEndpointImpl.s_UDP_PORT));
		addHostTransportPortToConnectorList(endpointHost, "UDP", udpPort);
		String tlsPort = String.valueOf(sipEndpointProperties.get(GenericEndpointImpl.s_TLS_PORT));
		addHostTransportPortToConnectorList(endpointHost, "TLS", tlsPort);
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(null, "addConnectorSipEndpointToList");
		}
	}
	
	/**
	 * Adding SIP Connector endpoint host/transport/port mapping
	 * @param host
	 * @param transport
	 * @param port
	 * @throws UnknownHostException 
	 */
	private static void addHostTransportPortToConnectorList(String host, String transport, String port) throws UnknownHostException{
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(null, "addHostTransportPortToConnectorList", host, transport, port);
		}
		try{
			if(port == null || port.length() <=0){
				return;
			}
			Integer portInt = Integer.decode(port);
			synchronized(sipConnectorEndpoint){
				InetAddress iaHost = normlizeHost(host);
				Map<String, HashSet<Integer>> transports = sipConnectorEndpoint.get(iaHost);
				if(transports == null){
					transports = new HashMap<String, HashSet<Integer>>();
					sipConnectorEndpoint.put(iaHost, transports);
				}
				
				HashSet<Integer> ports = transports.get(transport);
				if(ports == null){
					ports = new HashSet<Integer>();
					transports.put(transport, ports);
				}
				ports.add(portInt);
			}
		}
		finally{
			if(c_logger.isTraceEntryExitEnabled()){
				c_logger.traceExit(null, "addHostTransportPortToConnectorList");
			}
		}
	}
	
	/**
	 * removing SIP Connector endpoint from reference
	 * @throws UnknownHostException 
	 */
	private static void removeConnectorSipEndpointFromList(Map<String, Object> sipEndpointProperties) throws UnknownHostException{
		String endpointHost = (String) sipEndpointProperties.get(HOST_ATTRIBUTE);
		
		String tcpPort = String.valueOf(sipEndpointProperties.get(GenericEndpointImpl.s_TCP_PORT));
		removeHostTransportPortFromConnectorList(endpointHost, "TCP", tcpPort);
		String udpPort = String.valueOf(sipEndpointProperties.get(GenericEndpointImpl.s_UDP_PORT));
		removeHostTransportPortFromConnectorList(endpointHost, "UDP", udpPort);
		String tlsPort = String.valueOf(sipEndpointProperties.get(GenericEndpointImpl.s_TLS_PORT));
		removeHostTransportPortFromConnectorList(endpointHost, "TLS", tlsPort);
	}
	
	/**
	 * removing SIP Connector endpoint host/transport/port mapping
	 * @param host
	 * @param transport
	 * @param port
	 * @throws UnknownHostException 
	 */
	private static void removeHostTransportPortFromConnectorList(String host, String transport, String port) throws UnknownHostException{
		if(port == null || port.length() <=0){
			return;
		}
		Integer portInt = Integer.decode(port);
		synchronized(sipConnectorEndpoint){
			InetAddress iaHost = normlizeHost(host);
			Map<String, HashSet<Integer>> transports = sipConnectorEndpoint.get(iaHost);
			if(transports == null){
				return;
			}
			HashSet<Integer> ports = transports.get(transport);
			if(ports == null){
				return;
			}
			ports.remove(portInt);
		}
	}
	
	
	/**
	 * Gets a host and returns a normalized inetAddress, such that *, loopback address or localhost will always resolve the same
	 * @param host
	 * @return
	 * @throws UnknownHostException
	 */
	private static InetAddress normlizeHost(String host) throws UnknownHostException{
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(null, "normlizeHost", host);
		}
		InetAddress iaHost = host.equals("*") || host.equals("localhost") ? InetAddress.getLocalHost() : InetAddress.getByName(host);
		if(iaHost.isLoopbackAddress()){
			iaHost = InetAddress.getLocalHost();
		}
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(null, "normlizeHost", iaHost);
		}
		return iaHost;
	}
	/**
	 * Checks if a given SipURI is a SIP connector interface
	 * @param uri
	 * @return
	 * @throws UnknownHostException 
	 */
	public static boolean isSipUriAConnectorInterface(SipURI uri) throws UnknownHostException{
		synchronized(sipConnectorEndpoint){
			if(c_logger.isTraceEntryExitEnabled()){
				c_logger.traceEntry(null, "isSipUriAConnectorInterface", uri);
			}
			boolean result = false;
			try{
				InetAddress iaHost = normlizeHost(uri.getHost());
				Map<String, HashSet<Integer>> transports = sipConnectorEndpoint.get(iaHost);
				if(transports !=null){
					String transport = uri.getTransportParam();
					if(transport == null || transport.length() <=0){
						transport = "UDP";
					}else{
						transport = transport.toUpperCase();
					}
					
					HashSet<Integer> ports = transports.get(transport);
					if(ports != null){
						return result = ports.contains(uri.getPort());
					}
				}
				return result = false;
			}finally{
				if(c_logger.isTraceEntryExitEnabled()){
					c_logger.traceExit(null, "isSipUriAConnectorInterface", result);
				}
			}
		}
	}
	
	/**
	 * Removes SIP endpoint host aliases to its virtual host.
	 * 
	 * @param sipEndpointProperties configuration properties of SIP endpoint.
	 * @param isSslEnabled is ssl enabled flag.
	 * @param configAdminRef ConfigurationAdmin reference.
	 * @throws InvalidSyntaxException 
	 * @throws IOException 
	 */
	public static void removeSipEndpointHostAliasesFromVH(Map<String, Object> sipEndpointProperties, boolean isSslEnabled, ConfigurationAdmin configAdminRef) throws IOException, InvalidSyntaxException{
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Object[] params = {sipEndpointProperties, isSslEnabled};
			Tr.debug(tc, "Remove SIP endpoint host aliases from virtual host", params);
		}
		
		updateSipEndpointVirtualHostAliases(false, sipEndpointProperties, isSslEnabled, configAdminRef);
		removeConnectorSipEndpointFromList(sipEndpointProperties);
	}

	/**
	 * @param prevProperties previous configuration properties of SIP endpoint to remove.
	 * @param newProperties	 new configuration properties of SIP endpoint to add.
	 * @param isSslEnabled is ssl enabled flag.
	 * @param configAdminRef ConfigurationAdmin reference.
	 * @throws InvalidSyntaxException 
	 * @throws IOException 
	 */
	public static void updateSipEndpointHostAliasesToVH(Map<String, Object> prevProperties, Map<String, Object> newProperties, boolean isSslEnabled, ConfigurationAdmin configAdminRef) throws IOException, InvalidSyntaxException{
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Object[] params = {prevProperties, newProperties, isSslEnabled};
			Tr.debug(tc, "Update SIP endpoint host aliases to virtual host", params);
		}
		
		removeSipEndpointHostAliasesFromVH(prevProperties,isSslEnabled, configAdminRef);
		addSipEndpointHostAliasesToVH(newProperties, isSslEnabled, configAdminRef);
	}
	
	/**
	 * Updates SIP endpoint host aliases in virtual host.
	 * The update action (add/remove) is determined by add flag.
	 * 
	 * @param add flag to indicate whether to add or remove host aliases from virtual host.
	 * @param properties configuration properties of SIP endpoint.
	 * @param isSslEnabled is ssl enabled flag.
	 * @param configAdminRef ConfigurationAdmin reference.
	 * 
	 * @throws IOException
	 * @throws InvalidSyntaxException
	 */
	private static void updateSipEndpointVirtualHostAliases(boolean add, Map<String, Object> properties, boolean isSslEnabled, ConfigurationAdmin configAdminRef) throws IOException, InvalidSyntaxException{
		String virtualHostPid = getVirtualHostPidForSipEndpoint(properties, configAdminRef);
		List<String> hostAliasesFromEndpoint = createHostAliasesFromEndpoint(properties, isSslEnabled);
		String defaultVirtualHostPid= getVirtualHostPidForId(DEFAULT_VH_ID, configAdminRef);
		
		if(defaultVirtualHostPid.equals(virtualHostPid)){
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
				Tr.debug(tc, virtualHostPid + " is " + DEFAULT_VH_ID);
			}
			
			updateDefaultVirtualHost(add, hostAliasesFromEndpoint);
		}
		else{
			updateNonDefaultVirtualHost(add, virtualHostPid, hostAliasesFromEndpoint, configAdminRef);
		}
	}
	
	/**
	 * Updates default virtual host(default_host). Because default_host can't be updated, the host aliases aren't added
	 * to default_host itself, but to dedicated list that will be used in {@link SipServletsMatcher}.
	 * 
	 * @param add flag to indicate whether to add or remove host aliases from virtual host.
	 * @param hostAliasesFromEndpoint host aliases list of SIP endpoint.
	 */
	private static void updateDefaultVirtualHost(boolean add, List<String> hostAliasesFromEndpoint){
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Object[] params = {add, hostAliasesFromEndpoint};
			Tr.debug(tc, "updateDefaultVirtualHost", params);
		}
		
		for(String ha : hostAliasesFromEndpoint){
			VirtualHostAliasImpl vhai = new VirtualHostAliasImpl();
			vhai.init(ha);
			if(add){
				if(!s_defaultVH_hostAliases.contains(vhai)){
					s_defaultVH_hostAliases.add(vhai);
				}
			}
			else{
				if(s_defaultVH_hostAliases.contains(vhai)){
					s_defaultVH_hostAliases.remove(vhai);
				}
			}
		}
	}
	
	/**
	 * Filling the given list with default_host aliases
	 * @param virtualHostAliases
	 */
	public static void fillWithDefaultHostAliases(List<VirtualHostAlias> virtualHostAliases){
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(null, "fillWithDefaultHostAliases",
					"Adding to virtualHostAliases=" + Arrays.toString(virtualHostAliases.toArray()) + ", s_defaultVH_hostAliases="
							+ Arrays.toString(s_defaultVH_hostAliases.toArray()));
		}
		virtualHostAliases.addAll(s_defaultVH_hostAliases);
	}
	/**
	 * Updates non-default virtual host.
	 * 
	 * @param add flag to indicate whether to add or remove host aliases from virtual host.
	 * @param virtualHostPid virtual host pid.
	 * @param hostAliasesFromEndpoint host aliases list of SIP endpoint.
	 * @param configAdminRef ConfigurationAdmin reference.
	 * 
	 * @throws IOException
	 * @throws InvalidSyntaxException
	 */
	private static void updateNonDefaultVirtualHost(boolean add, String virtualHostPid,List<String> hostAliasesFromEndpoint, ConfigurationAdmin configAdminRef) throws IOException, InvalidSyntaxException{
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Object[] params = {add, virtualHostPid, hostAliasesFromEndpoint};
			Tr.debug(tc, "updateNonDefaultVirtualHost", params);
		}
		
		Configuration[] configs = getVirtualHostConfiguration(virtualHostPid, configAdminRef);
        if(configs != null){
        	for (Configuration configuration : configs) {
	            String[] vhHostAliases = (String[]) configuration.getProperties().get(HOST_ALIAS_ATTRIBUTE);
	            List<String> vhHostAliasesAsList = createVirtualHostAliasesToUpdate(add, vhHostAliases, hostAliasesFromEndpoint);
	            if(vhHostAliasesAsList != null){
	            	updateVirtualHostAlisesConfiguration(configuration, vhHostAliasesAsList);
	            }
	        }
        }
        else{
    		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
    			Object[] params = {add, virtualHostPid, hostAliasesFromEndpoint};
    			Tr.debug(tc, "updateNonDefaultVirtualHost didn't update virtual host becasue it didn't find any configuration for SIP endpoint", params);
    		}
        }
	}
	
	/**
	 * Creates host aliases list to update virtual host.
	 * 
	 * @param add flag to indicate whether to add or remove host aliases from virtual host.
	 * @param vhHostAliases current virtual host aliases
	 * @param hostAliasesFromEndpoint host aliases list of SIP endpoint.
	 * @return virtual host aliases list to update.
	 */
	private static List<String> createVirtualHostAliasesToUpdate(boolean add, String[] vhHostAliases, List<String> hostAliasesFromEndpoint){
		List<String> vhHostAliasesAsList = null;
		if(vhHostAliases == null){
        	if(add){
        		vhHostAliasesAsList = new ArrayList<String>();	
        	}
        }
        else{
        	vhHostAliasesAsList = new LinkedList<String>(Arrays.asList(vhHostAliases));
        }
        
    	for(String hafe: hostAliasesFromEndpoint){
    		if(add){
        		if(!vhHostAliasesAsList.contains(hafe)){
        			vhHostAliasesAsList.add(hafe);
        		}
    		}
    		else{
        		if(vhHostAliasesAsList.contains(hafe)){
        			vhHostAliasesAsList.remove(hafe);
        		}
    		}
    	}
    	
    	return vhHostAliasesAsList;
	}
	
	/**
	 * Gets virtual host pid for specific id.
	 * @param id virtual host id.
	 * @param configAdminRef ConfigurationAdmin reference.
	 * @return virtual host pid.
	 * 
	 * @throws IOException
	 * @throws InvalidSyntaxException
	 */
	private static String getVirtualHostPidForId(String id, ConfigurationAdmin configAdminRef) throws IOException, InvalidSyntaxException{
		String virtualHostPid = null;
		String specificVHFilter = "(&" + FilterUtils.createPropertyFilter(ConfigurationAdmin.SERVICE_FACTORYPID, VH_FACTORY_PID)
                + FilterUtils.createPropertyFilter("id", id) + ")";
		
		Configuration[] sipConnectorConf = configAdminRef.listConfigurations(specificVHFilter);
		if(sipConnectorConf != null && sipConnectorConf.length == 1){
			Dictionary<String, Object> properties = sipConnectorConf[0].getProperties();
			virtualHostPid = (String) properties.get(SERVICE_PID_ATTRIBUTE);
		}
		
		return virtualHostPid;
	}
	
	/**
	 * Gets virtual host pid for SIP endpoint.
	 * 
	 * 
	 * @param sipEndpointProperties configuration properties of SIP endpoint.
	 * @param configAdminRef ConfigurationAdmin reference.
	 * @return virtual host pid for SIP endpoint.
	 * 
	 * @throws IOException
	 * @throws InvalidSyntaxException
	 */
	private static String getVirtualHostPidForSipEndpoint(Map<String, Object> sipEndpointProperties, ConfigurationAdmin configAdminRef) throws IOException, InvalidSyntaxException{
		String virtualHostPid = null;
		boolean isSipConnectorVH = isSipConnectorReferenecedEndpoint(sipEndpointProperties, configAdminRef);
		if(isSipConnectorVH){
	        virtualHostPid= getVirtualHostPidForId(SIP_CONNECTOR_VH_ID, configAdminRef);
		}
		
		if(virtualHostPid == null){
			virtualHostPid = (String) sipEndpointProperties.get(VIRTUAL_HOST_REF_ATTRIBUTE);
		}
		
		return virtualHostPid;
	}

	/**
	 * Checks whether a given virtual host name is excluded from application composition
	 * @param vhName
	 * @return
	 */
	public static boolean isVirtualHostExcludedFromComposition(String vhName){
		return vhExcludedFromComposition.contains(vhName);
	}
	
	/**
	 * Creates host aliases list from SIP endpoint.
	 * @param sipEndpointProperties configuration properties of SIP endpoint.
	 * @param isSslEnabled is ssl enabled flag.
	 * @return host aliases list from SIP endpoint.
	 */
	private static List<String> createHostAliasesFromEndpoint(Map<String, Object> sipEndpointProperties, boolean isSslEnabled){
		List<String> hostAliasesList = new ArrayList<String>();
		String endpointHost = (String) sipEndpointProperties.get(HOST_ATTRIBUTE);
		String tcpPort = String.valueOf(sipEndpointProperties.get(GenericEndpointImpl.s_TCP_PORT));
		String tcpHostAlias = VirtualHostAliasImpl.createHostAliasString(endpointHost, tcpPort);
		hostAliasesList.add(tcpHostAlias);
		
		String udpPort = String.valueOf(sipEndpointProperties.get(GenericEndpointImpl.s_UDP_PORT));
		String udpHostAlias = VirtualHostAliasImpl.createHostAliasString(endpointHost, udpPort);
		if(!tcpHostAlias.equals(udpHostAlias)){
			hostAliasesList.add(udpHostAlias);
		}
		
		if(isSslEnabled){
			String tlsPort = String.valueOf(sipEndpointProperties.get(GenericEndpointImpl.s_TLS_PORT));
			String tlsHostAlias = VirtualHostAliasImpl.createHostAliasString(endpointHost, tlsPort);
			if(!tlsHostAlias.equals(tcpHostAlias) &&
			   !tlsHostAlias.equals(udpHostAlias)	){
				hostAliasesList.add(tlsHostAlias);
			}
		}
		
		return hostAliasesList;
	}
	
	/**
	 * Checks whether the SIP endpoint is reference to SipConnector.
	 * @param sipEndpointProperties configuration properties of SIP endpoint.
	 * @param configAdminRef ConfigurationAdmin reference.
	 * @return true whether the SIP endpoint is reference to SipConnector
	 * @throws IOException
	 * @throws InvalidSyntaxException
	 */
	private static boolean isSipConnectorReferenecedEndpoint(Map<String, Object> sipEndpointProperties, ConfigurationAdmin configAdminRef) throws IOException, InvalidSyntaxException{
		String endpointPid = String.valueOf(sipEndpointProperties.get(SERVICE_PID_ATTRIBUTE));
		String allVHFilter = "(&" + FilterUtils.createPropertyFilter(SERVICE_PID_ATTRIBUTE, RTCOMM_CONNECTOR_FACTORY_PID)+ ")";
		
		Configuration[] configs = configAdminRef.listConfigurations(allVHFilter);
		if(configs != null){
			for (Configuration configuration : configs) {
				// sipConnector configuration is flat and therefore we need to extract its configuration from its parent.
				Set<String> flatSipConnectorConfPropeties = getFlatSipConnectorReferenceProperties(configuration.getProperties());
				for(String property : flatSipConnectorConfPropeties){
					String[] sipEndpointPidRefs = (String[]) configuration.getProperties().get(property);
		        	if(sipEndpointPidRefs != null){
		        		for(String sipEndpointPidRef : sipEndpointPidRefs){
		            		if(sipEndpointPidRef.equals(endpointPid)){
		            			return true;
		            		}
		            	}	
		        	}
				}
	        }	
		}
		
		return false;
	}
	
	/**
	 * Gets flat sipConnector reference properties.
	 * @param rtcommConnectorProperties rtcommConnector flat configuration properties
	 * @return flat sipConnector reference properties.
	 */
	private static Set<String> getFlatSipConnectorReferenceProperties(Dictionary<String, Object> rtcommConnectorProperties){
		Set<String> allowFromProperties = new HashSet<String>();
		Enumeration<String> iter = rtcommConnectorProperties.keys();
		
		while(iter.hasMoreElements()){
			String property = iter.nextElement();
			// flat configuration looks like: sipConnector.0.allowFromSipEndpointRef
			if(property.startsWith(SIP_CONNECTOR_PREFIX) && 
			   property.endsWith(ALLOW_FROM_SIPENDPOINT_REF_SUFFIX)){
				allowFromProperties.add(property);
			}
		}
		
		return allowFromProperties;
	}

	/**
	 * Gets virtual host configuration for specific pid.
	 * @param virtualHostPid virtual host pid
	 * @param configAdminRef ConfigurationAdmin reference.
	 * @return virtual host configuration for specific pid.
	 * 
	 * @throws IOException
	 * @throws InvalidSyntaxException
	 */
	private static Configuration[] getVirtualHostConfiguration(String virtualHostPid, ConfigurationAdmin configAdminRef) throws IOException, InvalidSyntaxException{
		String specificVHFilter = "(&" + FilterUtils.createPropertyFilter(ConfigurationAdmin.SERVICE_FACTORYPID, VH_FACTORY_PID)
                + FilterUtils.createPropertyFilter(SERVICE_PID_ATTRIBUTE, virtualHostPid) + ")";
		
		return configAdminRef.listConfigurations(specificVHFilter);
	}
	
	/**
	 * Updates virtual host aliases.
	 * 
	 * @param configuration virtual host configuration.
	 * @param vhHostAliasesAsList virtual host aliases list
	 * 
	 * @throws IOException
	 */
	private static void updateVirtualHostAlisesConfiguration(Configuration configuration, List<String> vhHostAliasesAsList) throws IOException{
    	Object[] listToArray = vhHostAliasesAsList.toArray();
    	String[] vhHostAliases = Arrays.copyOf(listToArray, listToArray.length, String[].class);
    
    	Dictionary<String,Object> dict = configuration.getProperties();
    	dict.put(HOST_ALIAS_ATTRIBUTE, vhHostAliases);
    	configuration.update(dict);
	}
	
	/**
	 * Creates SIP connector virtual host.
	 * 
	 * @param sipEndpointProperties sip endpoint properties.
	 * @param isSslEnabled is ssl enabled flag.
	 * @param configAdminRef ConfigurationAdmin reference.
	 * 
	 * @throws IOException
	 * @throws InvalidSyntaxException
	 */
	private static void createSipConnectorVirtualHost(Map<String, Object> sipEndpointProperties, boolean isSslEnabled, ConfigurationAdmin configAdminRef) throws IOException, InvalidSyntaxException{
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Object[] params = {sipEndpointProperties, isSslEnabled};
			Tr.debug(tc, "Creates SIP connector virtual host for SIP endpoint", params);
		}
		
		List<String> hostAliasesFromEndpoint = createHostAliasesFromEndpoint(sipEndpointProperties, isSslEnabled);
		Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("id", SIP_CONNECTOR_VH_ID);
        props.put("config.displayId", "virtualHost[" + SIP_CONNECTOR_VH_ID + "]");
        props.put("config.id", VH_FACTORY_PID + "[" + SIP_CONNECTOR_VH_ID + "]");
        props.put("enabled", "true");
        
        String virtualHostBundle = getVirtualHostBundleLocation(configAdminRef);
        Configuration vhConfig = configAdminRef.createFactoryConfiguration(VH_FACTORY_PID, virtualHostBundle);
    	
        Object[] listToArray = hostAliasesFromEndpoint.toArray();
    	String[] vhHostAliases = Arrays.copyOf(listToArray, listToArray.length, String[].class);
  
    	for (String ha : hostAliasesFromEndpoint){
			VirtualHostAliasImpl vhai = new VirtualHostAliasImpl();
			vhai.init(ha);
			if (!s_sipConnectorVH_hostAliases.contains(vhai)){
				s_sipConnectorVH_hostAliases.add(vhai);
			}
		}
    
    	props.put(HOST_ALIAS_ATTRIBUTE, vhHostAliases);
    	vhConfig.update(props);
    	
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Tr.debug(tc, "Finish to create SIP connector virtual host for SIP endpoint", props);
		}
	}
	
	/**
	 * Gets virtual host configuration bundle location.
	 * 
	 * @param configAdminRef ConfigurationAdmin reference.
	 * @return virtual host bundle location.
	 * 
	 * @throws IOException
	 * @throws InvalidSyntaxException
	 */
	private static String getVirtualHostBundleLocation(ConfigurationAdmin configAdminRef) throws IOException, InvalidSyntaxException{
		String defaultVirtualHostBundleLocation = null;
		String specificVHFilter = "(&" + FilterUtils.createPropertyFilter(ConfigurationAdmin.SERVICE_FACTORYPID, VH_FACTORY_PID)
                + FilterUtils.createPropertyFilter("id", DEFAULT_VH_ID) + ")";
		
		Configuration[] sipConnectorConf = configAdminRef.listConfigurations(specificVHFilter);
		if(sipConnectorConf != null && sipConnectorConf.length == 1){
			defaultVirtualHostBundleLocation = sipConnectorConf[0].getBundleLocation();
		}
		
		return defaultVirtualHostBundleLocation;
	}
	
	/**
	 * Checks whether sipUa virtual host exist.
	 * 
	 * @param configAdminRef ConfigurationAdmin reference.
	 * @return true whether sipUa virtual host exist, otherwise false.
	 * 
	 * @throws IOException
	 * @throws InvalidSyntaxException
	 */
	private static boolean isExistSipUaVirtualHost(ConfigurationAdmin configAdminRef) throws IOException, InvalidSyntaxException{
		String vhPid = getVirtualHostPidForId(SIP_CONNECTOR_VH_ID, configAdminRef);
		
		return  vhPid != null;
	}
	
	/**
	 * Checks whether host alias matches application virtual host.
	 * 
	 * @param vha Virtual host alias.
	 * @param app Sip application descriptor.
	 * @return true whether host alias matches application virtual host, otherwise false.
	 */
	public static boolean isHostAliasMatchVirtualHost(VirtualHostAlias vha, SipAppDesc app){
		String vhName = app.getVirtualHostName();
        List<VirtualHostAlias> virtualHostAliasesList;
        
        if(DEFAULT_VH_ID.equals(vhName)){
        	virtualHostAliasesList = s_defaultVH_hostAliases;
        }
        else{
        	virtualHostAliasesList = app.getVirtualHostAliases();
        	if (virtualHostAliasesList == null || virtualHostAliasesList.size() == 0) {
        		// in case the VH configuration update, which is called by createSipConnectorVirtualHost, 
        		// has not finished yet, then use the locally created list.
        		virtualHostAliasesList = s_sipConnectorVH_hostAliases;
        		
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
        			Tr.debug(tc, "Using locally generated rtcomm gateway's hosts alias list");
                }
        	}
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Tr.debug(tc, "Searching for alias in VH " + app.getVirtualHostName() +
	                " to match host=" + vha.getHost() + ", port=" + vha.getPort());
		}
        
        // virtual host configuration can be modified in run time.
        synchronized (virtualHostAliasesList) {
            for( int i=0; i < virtualHostAliasesList.size(); i++){
            	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
            		Tr.debug(tc, "Module virtual host = " + virtualHostAliasesList.get(i));
                }
                
                if( ((VirtualHostAlias)virtualHostAliasesList.get(i)).match( vha))
                    return true;
            }
		}

		return false;
	}
}
