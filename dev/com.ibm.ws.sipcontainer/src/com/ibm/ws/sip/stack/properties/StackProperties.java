/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.properties;

import jain.protocol.ip.sip.ListeningPoint;
import jain.protocol.ip.sip.SipProvider;
import jain.protocol.ip.sip.address.AddressFactory;
import jain.protocol.ip.sip.header.HeaderFactory;
import jain.protocol.ip.sip.message.MessageFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jain.protocol.ip.sip.ListeningPointImpl;

/**
 * @author Amir Perlman, Feb 19, 2003
 *
 * Contains pointers to all Jain Stack related properties 
 */
public class StackProperties
{
	 /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(StackProperties.class);
    
    /**
     * The one and only jain sip properties 
     */
    private static StackProperties c_instance = new StackProperties();

    /**
     * If a request from the code for listening point comes to this class and there aren't any set yet, this timeout defines
     * the period the call blocks in case this might be a race condition with the channel framework service.
     * After this timeout, the operation should fail with an exception
     */
    private static long WAIT_FOR_PROVIDERS_TIMEOUT_MS = 10*1000;//10 seconds
    
    /**
     * port wildcard
     */
    private static final int ANY_PORT = -1; 
    
    /**
     * The Jain Sip Message Factory associated with this message. 
     */
    private MessageFactory m_messageFactory;

    /**
     * The Jain Sip Header Factory associated with this message. 
     */
    private HeaderFactory m_headerFactory;

    /**
     * The Jain Sip Address Factory associated with this message. 
     */
    private AddressFactory m_addressFactory;

    /**
     * List of jain sip providers. 
     * CopyOnWriteArrayList makes sure that the list can be iterated over in a thread safe manner.
     * The performance hit of Copy-on-write should not be substantial since adding or removing of listening points is a rare operation, compared to 
     * traversing the list.
     */
    private CopyOnWriteArrayList<SipProvider> m_providers = new CopyOnWriteArrayList<SipProvider>();

    /**
     * A lock used for synchronizing the block operation on an empty providers list and releasing when its filled with a provider.
     */
    private Object waitForProvidersLock = new Object() {};
    
    /**
     * map of jain udp sip providers
     * the key is the sip provider port, a list of udp sip providers set to use this port
     */
    private Map<Integer, CopyOnWriteArrayList<SipProvider>> m_udpProviders = new HashMap<Integer, CopyOnWriteArrayList<SipProvider>>();
    
    /**
     * map of jain tcp sip providers
     * the key is the sip provider port, a list of tcp sip providers set to use this port
     */
    private Map<Integer, CopyOnWriteArrayList<SipProvider>> m_tcpProviders = new HashMap<Integer, CopyOnWriteArrayList<SipProvider>>();

    /**
     * map of jain tls sip providers
     * the key is the sip provider port, a list of tls sip providers set to use this port
     */
    private Map<Integer, CopyOnWriteArrayList<SipProvider>> m_tlsProviders = new HashMap<Integer, CopyOnWriteArrayList<SipProvider>>();
    
	/**
	 * UDP provider. Kept in also in a separate member for fast access. 
	 */	
	private SipProvider m_udpProvider;

	/**
	 * TCP provider. Kept in also in a separate member for fast access. 
	 */	
	private SipProvider m_tcpProvider;

	/**
	  * TLS provider. Kept in also in a separate member for fast access. 
	  */	
	private SipProvider m_tlsProvider;
 
    /**
     * Constructor
     */
    protected StackProperties()
    {

    }

    /**
     * 
     * @param provider
     */
    public void setFactories(
				        MessageFactory messageFactory,
				        HeaderFactory headerFactory,
				        AddressFactory addressFactory)
    {
        m_messageFactory = messageFactory;
        m_headerFactory = headerFactory;
        m_addressFactory = addressFactory;
    }

    /**
     * Check whether the container have any provider for listening points available
     */
    private boolean anyProviderExists( CopyOnWriteArrayList<SipProvider> providers){
    	return providers != null && providers.size() > 0;
    }

    /**
     * Wait till the container will have available listening points. This is meant for making sure no
     * race condition occur when an application loaded on startup and sends out a message, but the stack is not ready yet to serve messages
     */
    private void waitForAnyProvider( CopyOnWriteArrayList<SipProvider> allProviders){
    	waitForProviders(null, ANY_PORT, allProviders);
    }
    
    /**
     * Wait till the container will have available listening points of specific transport and port. This is meant for making sure no
     * race condition occur when an application loaded on startup and sends out a message, but the stack is not ready yet to serve messages
     */
    private void waitForProviders( String transport, int port, CopyOnWriteArrayList<SipProvider> allProviders){
    	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc,"waitForProviders", transport, port, allProviders);
        }
    	try{
	    	synchronized(waitForProvidersLock){
	    		CopyOnWriteArrayList<SipProvider> providers = allProviders == null ? getMatchingProvidersList(transport, port) : allProviders; 
	    		long waitEnd, waitStart = System.nanoTime();
	    		waitEnd = waitStart;
		    	while( !anyProviderExists(providers)){
		    		try {
		    			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
		                    Tr.debug(this, tc,"waitForProviders", "List of SIP listening points providers is empty. Waiting for a listening points to be added.", transport, port, allProviders);
		                }
		    			waitForProvidersLock.wait(WAIT_FOR_PROVIDERS_TIMEOUT_MS);
		    			waitEnd = System.nanoTime();
					} catch (InterruptedException e) {
						//do nothing
					}
		    		providers = allProviders == null ? getMatchingProvidersList(transport, port) : allProviders;
		    		if(!anyProviderExists(providers) && (waitEnd-waitStart >= WAIT_FOR_PROVIDERS_TIMEOUT_MS*1000000)){
		    			throw new RuntimeException("An application is trying to access SIP endpoints, but none were set for this server. Transport="+
		    					(transport == null ? "Any" : transport) + " port="+port);
		    		}
		    	}
	    	}
    	}finally{
	    	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
	            Tr.exit(this, tc,"waitForProviders");
	        }
    	}
    }
    
    /**
     * Release any thread that is waiting for providers to be available
     */
    private void announceProviderAdded(){
    	synchronized(waitForProvidersLock){
    		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc,"announceProviderAdded", "New SIP listening point provider was added. Releasing any waiting resource.");
            }
    		waitForProvidersLock.notify();
    	}
    }
    
    /**
     * Add a provider to our list of providers
     * 
     * @param provider
     */
    public void addProvider(SipProvider provider)
    {
    	synchronized(waitForProvidersLock){
    		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc,"addProvider", provider);
            }
	        m_providers.add(provider);
	        
	        ListeningPoint lp = provider.getListeningPoint();
	        
	        Map<Integer, CopyOnWriteArrayList<SipProvider>> providersMap = null;
	        
	        if(lp.isSecure())
	        {
	        	m_tlsProvider = provider;
	        	providersMap = m_tlsProviders;
	        }
	        else if(lp.getTransport().equalsIgnoreCase(ListeningPoint.TRANSPORT_TCP))
	        {
	        	m_tcpProvider = provider;
	        	providersMap = m_tcpProviders;
	        }
	        else if(lp.getTransport().equalsIgnoreCase(ListeningPoint.TRANSPORT_UDP))
	        {
	        	m_udpProvider = provider;
	        	providersMap = m_udpProviders;
	        }
	        
	        //get list of providers set on this port
	        CopyOnWriteArrayList<SipProvider> providers = providersMap.get(lp.getPort());
	        if (providers == null){
	        	providers = new CopyOnWriteArrayList<SipProvider>();
	        	providersMap.put(lp.getPort(), providers);
	        }
	        providers.add(provider);
	        announceProviderAdded();
    	}
    }

    /**
     * Gets the appropriate providers for the specified transport 
     * @param transport Transport type should be either TCP/TLS/UDP
     * @param port
     * @return Iterator 
     */
    public Iterator<SipProvider> getProviders(String transport, int port){
    	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc,"getProviders", transport, port);
        }
    	CopyOnWriteArrayList<SipProvider> providers = null;
    	try{
	    	synchronized(waitForProvidersLock){
	    		waitForProviders(transport, port, null);
	    		providers = getMatchingProvidersList(transport, port);
		        if(providers != null){
		        	return providers.iterator();
		        }
		        
		        return null;
	    	}
    	}
    	finally{
    		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(this, tc,"getProviders", providers);
            }
    	}
    }
    
    /**
     * Gets the appropriate providers for the specified transport 
     * @param transport Transport type should be either TCP/TLS/UDP
     * @param port
     * @return
     */
    private CopyOnWriteArrayList<SipProvider> getMatchingProvidersList(String transport, int port){
    	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc,"getMatchingProvidersList", transport, port);
        }
    	CopyOnWriteArrayList<SipProvider> providers = null;
    	Map<Integer, CopyOnWriteArrayList<SipProvider>> providersMap = null;
        
        if(transport == null){
        	providersMap = m_udpProviders;
            if(null == providersMap){
            	providers = m_providers;
            }
        }
        else if(transport.equalsIgnoreCase(ListeningPointImpl.TRANSPORT_TLS)){
        	providersMap = m_tlsProviders; 
        }
        else if(transport.equalsIgnoreCase(ListeningPoint.TRANSPORT_TCP)){
        	providersMap = m_tcpProviders; 
        }
        else if(transport.equalsIgnoreCase(ListeningPoint.TRANSPORT_UDP)){
        	providersMap = m_udpProviders;
        }
        
        if (providersMap != null && !providersMap.isEmpty()){
        	providers = 
        			port == ANY_PORT ?  
        			providersMap.values().iterator().next() : // get any provider that matches host and port 
        				providersMap.get(port); // get specific provider for that port
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc,"getMatchingProvidersList", providers);
        }
        return providers;
    }
    
    /**
     * Gets the first provider from the list of available providers. 
     * @return
     */
    public SipProvider getFirstProvider()
    {
    	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc,"getFirstProvider");
        }
    	synchronized(waitForProvidersLock){
    		waitForAnyProvider(m_providers);
    		SipProvider provider = (SipProvider) m_providers.get(0);
    		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(this, tc,"getFirstProvider", provider);
            }
    		return provider;
    	}
    }

    /**
     * Return the message factory
     * 
     * @return MessageFactory
     */
    public MessageFactory getMessageFactory()
    {
        return m_messageFactory;
    }

    /**
     * Return the message factory
     * 
     * @return MessageFactory
     */
    public HeaderFactory getHeadersFactory()
    {
        return m_headerFactory;
    }

    /**
     * Return the message factory
     * 
     * @return MessageFactory
     */
    public AddressFactory getAddressFactory()
    {
        return m_addressFactory;
    }

    /**
     * Return an instance to the one and only jain sip properties
     */
    public static StackProperties getInstance()
    {
        return c_instance;
    }

    /**
     * Gets the appropriate provider for the specified transport 
     * @param transport Transport type should be either TCP/TLS/UDP
     * @return Best matching provider or the first one in the list in case
     * of no match. 
     */
    public SipProvider getProvider(String transport)
    {
    	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc,"getProvider", transport);
        }
    	synchronized (waitForProvidersLock) {
			
	    	waitForProviders(transport, ANY_PORT, null);
	        SipProvider provider = null;
	        if(transport == null)
	        {
	            provider = m_udpProvider;
	            if(null == provider)
	            {
	                //Still no provider, use the first available
	                provider = getFirstProvider();
	            }
	        }
	        else if(transport.equalsIgnoreCase(ListeningPointImpl.TRANSPORT_TLS))
	        {
	        	provider = m_tlsProvider; 
	        }
	        else if(transport.equalsIgnoreCase(ListeningPoint.TRANSPORT_TCP))
	        {
	        	provider = m_tcpProvider; 
	        }
	        else if(transport.equalsIgnoreCase(ListeningPoint.TRANSPORT_UDP))
	        {
	        	provider = m_udpProvider;
	        }
	        
	        if(provider == null)
	        {
	        	//Still no provider found - Use first one in the list.
	        	provider = getFirstProvider(); 
	        }
	        
	        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
	            Tr.exit(this, tc,"getProvider", provider);
	        }
	        
	        return provider;
    	}
    }

}
