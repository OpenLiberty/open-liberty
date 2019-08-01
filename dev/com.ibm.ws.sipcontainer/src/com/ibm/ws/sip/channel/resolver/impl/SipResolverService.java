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
package com.ibm.ws.sip.channel.resolver.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Timer;
import java.util.Vector;
import java.util.regex.Pattern;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.ws.sip.properties.CoreProperties;
import com.ibm.wsspi.sip.channel.resolver.SIPUri;
import com.ibm.wsspi.sip.channel.resolver.SipURILookup;
import com.ibm.wsspi.sip.channel.resolver.SipURILookupCallback;

public class SipResolverService {
	
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipResolverService.class);
	
	static private boolean								initialized = false;
	static private SipResolver							sipResolver = null;
	static private Timer								resolverTimer = null;
	static private int				timerCounter = 0;						
	static private boolean								_addTTL = false;
	static private boolean								usePreciseSystemTimer = false;
	static private Hashtable<SIPUri, SipURILookupImpl>	lookupCache = new Hashtable<SIPUri, SipURILookupImpl>();
	
	static private long	lookupCacheTimeout = 10 * 60 * 1000; // default cache storage is 10 minutes (used to be 12 hours, 43200*1000)
	
	private static final String SIP_RFC3263_NAMESERVER = "SIP_RFC3263_nameserver";
	private static final String SIP_RFC3263_EDNS = "SIP_RFC3263_edns";
	private static final String SIP_RFC3263_TIMEOUT = "SIP_RFC3263_REQUEST_CACHE_TIMEOUT_MIN";
	private static final String SIP_RFC3263_ADD_TTL = "add_ttl";
	private static final String SIP_DNS_QUERY_TIMEOUT = "SIP_DNS_QUERY_TIMEOUT";
	private static final String SIP_USE_PRECISE_SYSTEM_TIMER = "SIP_USE_PRECISE_SYSTEM_TIMER";
    // not used   private static final String SIP_RFC3263_UDP_PAYLOAD_SIZE = "SIP_RFC3263_udp_payload_size";
	
	
	   

        synchronized static public void initialize(Properties containerProps,CHFWBundle chfwb)
        {
                 if (c_logger.isTraceEntryExitEnabled())
                         c_logger.traceEntry(SipResolverService.class, "SipResolverService: initialize(Properties): entry"); 

                 
                 if (initialized == false)
                 {
                         initialized = true;

                         /*
                          * SIP_MESSAGE_TIMEOUT is an optional config property.
                          */
                         Object tempObject = containerProps.get(CoreProperties.SIP_DNS_QUERY_TIMEOUT);
                         if (tempObject != null) {
                        	 Long messageTimeout = tempObject instanceof Long ? (Long) tempObject : 0;
	                         if (messageTimeout != 0) {
	                       		if (c_logger.isTraceDebugEnabled())
	                       			c_logger.traceDebug("SipResolverService: initialize: " + CoreProperties.SIP_DNS_QUERY_TIMEOUT + " = " + messageTimeout);
	                       		SipURILookupImpl.setMessageTimeoutValue(messageTimeout);
	                         } 
	                         else {
                        		if (c_logger.isTraceDebugEnabled())
                           			c_logger.traceDebug("SipResolverService: initialize:  <" + CoreProperties.SIP_DNS_QUERY_TIMEOUT + ">  is the wrong data type");                        	 
	                         }
                         }
                         else {
                       		if (c_logger.isTraceDebugEnabled())
                       			c_logger.traceDebug("SipResolverService: initialize:  <" + CoreProperties.SIP_DNS_QUERY_TIMEOUT + ">  is not set");
                         }
                         
                    /*     //read the use precise system timer value
                         tempObject = containerProps.get(SIP_USE_PRECISE_SYSTEM_TIMER);
                         if (tempObject != null) {
                        	 usePreciseSystemTimer = tempObject instanceof Boolean ? (Boolean) tempObject : false; 
                       		 if (c_logger.isTraceDebugEnabled()) {
                       			if (tempObject instanceof Boolean)
                       				 c_logger.traceDebug("SipResolverService: initialize:  <" + SIP_USE_PRECISE_SYSTEM_TIMER + ">  is a Boolean");
                       			else
                      				 c_logger.traceDebug("SipResolverService: initialize:  <" + SIP_USE_PRECISE_SYSTEM_TIMER + ">  is NOT a Boolean");
                                c_logger.traceDebug("SipResolverService: initialize:  <" + SIP_USE_PRECISE_SYSTEM_TIMER + ">  is set to " +
                            			tempObject);
                       		 }
                       		 SipURILookupImpl.setUsePreciseSystemTimer(usePreciseSystemTimer);
                         }
                         else {
                        	 if (c_logger.isTraceDebugEnabled())
                         		c_logger.traceDebug("SipResolverService: initialize:  <" + SIP_USE_PRECISE_SYSTEM_TIMER + ">  is not set");
                         }
                     */    
                         //read the add TTL parameter value
                         _addTTL = (Boolean) containerProps.get(CoreProperties.SIP_RFC3263_ADD_TTL);
						 
                         String serverString = containerProps.getProperty(CoreProperties.DNSSERVERNAMES);
                         // If the custome property isn't set, return
                         if (serverString == null) {
                                 if (c_logger.isInfoEnabled()) c_logger.info("info.sip.resolver.not.initialized");
                                 initialized = false;
                                 return;
                        } 

                         //	Create the SIP Resolver
                         Vector<InetSocketAddress> nameServers = serverStringtoVector(serverString);

                         if (!nameServers.isEmpty()){

                             if (c_logger.isTraceDebugEnabled()) c_logger.traceDebug("nameServers is not empty");
                                    
                             String sEDNS = containerProps.getProperty(CoreProperties.DNS_EDNS);
                                 
                             if (sEDNS != null){
                                	 
                            	 /** custom property used to turn of EDNS in the resolver */
                                 if (c_logger.isTraceDebugEnabled())
                                     c_logger.traceDebug("SipResolverService: sEDNS is: " + sEDNS);
                                 sEDNS = sEDNS.trim();
                                 if (sEDNS.equalsIgnoreCase("off")){
                                     SipResolver.setEDNS(false);
                                 }
                                 // "on" is default, we don't really need this check.
                                 // but put it here in case off becomes default
                                 else if (sEDNS.equalsIgnoreCase("on")){
                                     SipResolver.setEDNS(true);
                                 }
                             } else {
                                 if (c_logger.isTraceDebugEnabled())
                                     c_logger.traceDebug("SipResolverService: sEDNS property is: null ");
                             }

                             sipResolver = SipResolver.createResolver(nameServers, containerProps, chfwb);
                             
                             Short sPayload = (Short) containerProps.get(CoreProperties.DNS_UDP_PAYLOAD_SIZE);
                              
                             if (sPayload != null){
                            	 if (512 <= sPayload && sPayload <= 32767){
                            		 sipResolver.setUdpPayloadSize(sPayload);
                                }
                                else {
                                	if (c_logger.isTraceDebugEnabled()){
                                            c_logger.traceDebug("SipResolverService: initialize: invalid SIP_RFC3263_udp_payload_size" + sPayload);
                                     }
                                 }
                             }
                             
                             
                             String sTimeout = containerProps.getProperty(SIP_RFC3263_TIMEOUT);
                          
                             if (sTimeout != null && sTimeout.length() > 0) {
                            	 try {
                                	 long l = Long.parseLong(sTimeout);
                                	 if (l >= 10) {
                                		 lookupCacheTimeout = l * 60 * 1000;
                                	 }
                            	 } catch (NumberFormatException e) {
                                	 if (c_logger.isTraceDebugEnabled())
                                         c_logger.traceDebug("SipResolverService: initialize: invalid SIP_RFC3263_TIMEOUT " + sTimeout);
                            	 }
                            	 if (c_logger.isTraceDebugEnabled())
                            		 c_logger.traceDebug("SipResolverService: initialize: SIP_RFC3263_TIMEOUT " + lookupCacheTimeout / 1000);
                             }
                         }
                         else {
                             if (c_logger.isTraceDebugEnabled()) c_logger.traceDebug("nameServers is empty");
                             
                             if (c_logger.isInfoEnabled()) c_logger.info("info.sip.resolver.not.initialized");

                             initialized = false;
                             return;
                         }

                         resolverTimer = new Timer();

                         if (c_logger.isInfoEnabled()) {
                                 //X CR 4564
                                 InetSocketAddress socket1 = (InetSocketAddress)nameServers.firstElement();
                                 //System.out.println(s1.getAddress().getHostAddress()+"@"+s1.getPort());
                                 c_logger.info("info.sip.resolver.initialized", null, socket1.getAddress().getHostAddress() +"@"+ socket1.getPort());
                                 if (nameServers.size() > 1){
                                         InetSocketAddress socket2 = (InetSocketAddress)nameServers.elementAt(1);
                                         if (socket2 != null) {
                                                 c_logger.info("info.sip.resolver.initialized", null, socket2.getAddress().getHostAddress()+"@"+ socket2.getPort());
                                         }
                                 }
                         }		 
                 }
                 if (c_logger.isTraceEntryExitEnabled())
                                c_logger.traceExit(SipResolverService.class, "SipResolverService: initialize: exit");
         }

        /**
         * SIP_RFC3263_nameserver key to sip container custom property for setting nameserver address
         * 
         * Custom property is a string containing exactly one or two address/port tuples, with each 
         *  tuple seperated by a space
         * 
         * The tuple may consist of 
         * 	[Literal IPv4 | Literal IPv6 | hostname]@port
         * 		Example
         * 		10.1.1.1@53 10.1.1.2@53
         * 		foo.bar@53
         * 		fec0::1@6053
         * 
         * @param serverString
         * @return
         */
        private static Vector<InetSocketAddress> serverStringtoVector(String serverString){
                Vector<InetSocketAddress> nameServers = new Vector<InetSocketAddress>();

                /** split the string with space delimiter */
                Pattern pattern = Pattern.compile("\\s+");
                String [] split = pattern.split(serverString);

                if (c_logger.isTraceDebugEnabled())
                        c_logger.traceDebug("SipResolverService: serverStringtoVector split.length" + split.length);


                /** while there are tuples */
                for (int i = 0; i < split.length; i++){
                        /** split the tuple into host and port */
                        String [] tuple = split[i].split("\\@");

                        String host = tuple[0];
                        int port = 53;

                        /** if a port was set, get it */
                        if (tuple.length > 1){
                                try {
                                        port = Integer.parseInt(tuple[1]);
                                }
                                catch (NumberFormatException e) {
                                        if (c_logger.isTraceDebugEnabled())
                                                c_logger.traceDebug("SipResolverService: serverStringtoVector " + e);
                                }
                        }

                        try {
                        	if (host !=null && host != ""){
                                nameServers.add(new InetSocketAddress(InetAddress.getByName(host), port));
                        	}

                        }
                        catch (UnknownHostException e) {
                                if (c_logger.isTraceDebugEnabled())
                                        c_logger.traceDebug("SipResolverService: serverStringtoVector " + e);
                        }

                }

                return nameServers;
        }

	 synchronized static public void shutdown() 
	 {
	 	if (c_logger.isTraceEntryExitEnabled())
	 		c_logger.traceEntry(SipResolverService.class, "SipResolverService: shutdown: entry");
	 
		 if (initialized == true)
		 {
			 initialized = false;
			 
			 resolverTimer.cancel();
			 
			 //	Create the SIP Resolver
			 sipResolver.shutdown();
		 }
		 if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(SipResolverService.class, "SipResolverService: shutdown: exit");
	 }

	/**
	 * public method for getting an instance of SipURILookupImpl
	 */
	 static public  SipURILookup getInstance(SipURILookupCallback sll, SIPUri suri)
	 {
		 if (c_logger.isTraceEntryExitEnabled())
			 c_logger.traceEntry(SipResolverService.class, "SipResolverService: getInstance: entry");

         if (c_logger.isTraceDebugEnabled()) {
             if (suri != null) {
                 c_logger.traceDebug("  suri.getTransport(): " + suri.getTransport());
                 c_logger.traceDebug("  suri.getPortInt(): " + suri.getPortInt());
                 c_logger.traceDebug("  suri.getMaddr(): " + suri.getMaddr());
                 c_logger.traceDebug("  suri.getHost(): " + suri.getHost());
                 c_logger.traceDebug("  suri.getScheme(): " + suri.getScheme());
             } else {
                    c_logger.traceDebug("suri is null"); 
             }
         }
         
		 if (initialized == true)
		 {
			 
			 // purge timers every 10000 timers
             timerCounter++;
             
             if (timerCounter > 5000) {
            	 synchronized (resolverTimer) {
            		 if (timerCounter > 10000) {
        				 if (c_logger.isTraceDebugEnabled())
     						c_logger.traceDebug("SipResolverService: Timer task purge " + timerCounter);
            			 
            			 resolverTimer.purge();
            			 timerCounter = 0;
            		 }
				}
             }

			 
			 //	First check to see if the lookup object is already stored in the cache.
			 // X CR4564 
			 SipURILookup lookUp = lookupCache.get(suri);

			 //	If not, then create a new one.
			 if (lookUp == null){
				 if (c_logger.isTraceDebugEnabled())
						c_logger.traceDebug("SipResolverService: getInstance: new SipURILookupImpl for " + 
								suri.getBaseSIPUri());
				 lookUp = new SipURILookupImpl(sll, suri, resolverTimer, lookupCache, lookupCacheTimeout);
			 }
			 else {
				 if (c_logger.isTraceDebugEnabled())
						c_logger.traceDebug("SipResolverService: getInstance: cached SipURILookupImpl for: " +
								lookUp.getSipURI().getHost() + ":" + lookUp.getSipURI().getPort());
			 }
			 if (c_logger.isTraceEntryExitEnabled())
				 c_logger.traceExit(SipResolverService.class, "SipResolverService: getInstance: exit");
			 return lookUp;
		 }
		 else {
			 if (c_logger.isTraceDebugEnabled())
				 c_logger.traceDebug("SipResolverService: getInstance: Illegal State: exit");
			 throw new IllegalStateException("SipResolverService not initialized.");
		 }

	 }
	 
	 //X CR 4564 
	 static public SipResolver getResolver(){
		 if (sipResolver == null)
			 throw new IllegalStateException("Sip Resolver has not been created.");
		 return sipResolver;
	 }
	 
	 static boolean isAddTTL(){
		 return _addTTL; 
	 }
}
