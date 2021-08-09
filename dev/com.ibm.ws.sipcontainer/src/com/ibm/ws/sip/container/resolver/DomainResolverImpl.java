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
package com.ibm.ws.sip.container.resolver;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sip.resolver.DomainResolver;
import com.ibm.websphere.sip.resolver.DomainResolverListener;
import com.ibm.websphere.sip.resolver.exception.SipURIResolveException;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.util.SipUtil;
import com.ibm.ws.sip.container.was.ThreadLocalStorage;
import com.ibm.ws.sip.properties.CoreProperties;
import com.ibm.ws.sip.properties.SipPropertiesMap;
import com.ibm.ws.sip.stack.internalapi.NaptrRequestListener;
import com.ibm.ws.sip.stack.internalapi.SipStackDomainResolver;
import com.ibm.ws.sip.stack.transaction.SIPTransactionConstants;
//TODO Liberty change to Liberty channel framework classes
import com.ibm.ws.sip.channel.resolver.impl.SipResolverService;
import com.ibm.wsspi.sip.channel.resolver.SIPUri;
import com.ibm.wsspi.sip.channel.resolver.SipURILookup;
import com.ibm.wsspi.sip.channel.resolver.SipURILookupException;

@Component(service = DomainResolverImpl.class,
configurationPolicy = ConfigurationPolicy.OPTIONAL,
configurationPid = "com.ibm.ws.sip.container.resolver.DomainResolverImpl",
name = "com.ibm.ws.sip.container.resolver.DomainResolverImpl",
property = {"service.vendor=IBM"} )
public class DomainResolverImpl implements DomainResolver, SipStackDomainResolver {
	
	/** trace variable */
	private static final TraceComponent tc = Tr.register(DomainResolverImpl.class);
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(DomainResolverImpl.class);
	
	private boolean _initialized = false;
	
	private boolean _naptrAutoResolve = false;
	private CHFWBundle m_chfw;
	
	/**
	 * DS method to activate this component.
	 * 
	 * @param	context 	: Component context of the component 
	 * @param 	properties 	: Map containing service & config properties
	 *            populated/provided by config admin
	 */
	protected void activate(Map<String, Object> properties) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, "DomainResolverImpl activated", properties);
		PropertiesStore.getInstance().getProperties().updateProperties(properties);
		init();
	}
	
	

	/**
	 * This is a required static reference, this won't be called until the
	 * component has been deactivated
	 * 
	 * @param bundle
	 *            CHFWBundle instance to unset
	 */
	protected void unsetChfwBundle(CHFWBundle bundle) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, "unsetChfwBundle");
		}
		m_chfw = null;
		
	}   

	/**
	 * This is a required static reference, this won't be called until the
	 * component has been deactivated
	 * 
	 * @param bundle
	 *            CHFWBundle instance to unset
	 */
	@Reference( policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY)
	protected void setChfwBundle(CHFWBundle bundle) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, "setChfwBundle");
		}
		m_chfw = bundle;
		
	}

	
	/**
	 * DS method to modify this component.
	 * 
	 * @param 	properties 	: Map containing service & config properties
	 *            populated/provided by config admin
	 * @throws IOException 
	 */
	@Modified
	public void modified(ComponentContext context, Map<String, Object> properties) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "DomainResolverImpl modified", properties);
     // TODO Liberty - note configuration changes actions to be done
    }
	
	/**
	 * DS method to deactivate this component.
	 * 
	 * @param reason int - representation of reason the component is stopping
	 */
	public void deactivate(int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "DomainResolverImpl deactivated, reason="+reason);
    }
	

	private void init() {
		SipPropertiesMap sipProp = PropertiesStore.getInstance().getProperties();
		
		String[] dnsServersArray = (String[])sipProp.getObject(CoreProperties.DNSSERVERNAMES);
		String dnsServers = "";
		for (String s : dnsServersArray) {
			dnsServers = dnsServers.concat(s);
			dnsServers = dnsServers.concat(" ");
		}
		
		if (dnsServers != null && !dnsServers.isEmpty()) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "DomainResolverImpl", "DNS found: " + dnsServers);
			}
			
			int cacheTimeout = sipProp.getDuration(CoreProperties.DNS_REQUEST_CACHE_TIMEOUT_MIN);
			
			if (cacheTimeout < CoreProperties.DNS_REQUEST_CACHE_TIMEOUT_MIN_DEFAULT) {
				cacheTimeout = CoreProperties.DNS_REQUEST_CACHE_TIMEOUT_MIN_DEFAULT;
			}
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "DomainResolverImpl", "DNS Cache timeout: " + cacheTimeout);
			}
			

			int singleQueryTimeout = sipProp.getDuration(CoreProperties.SIP_RFC3263_DNS_FAILURE_DETECTION_SINGLE_QUERY_TIMEOUT_SEC);
			if (singleQueryTimeout == -1) {
				singleQueryTimeout = CoreProperties.SIP_RFC3263_DNS_FAILURE_DETECTION_SINGLE_QUERY_TIMEOUT_SEC_DEFAULT;
			}
			
			int allowedFailures = sipProp.getInt(CoreProperties.SIP_RFC3263_DNS_FAILURE_DETECTION_ALLOWED_FAILURES);
			int windowSize = sipProp.getInt(CoreProperties.SIP_RFC3263_DNS_FAILURE_DETECTION_WINDOW_SIZE_MIN);
			int interval = sipProp.getInt(CoreProperties.SIP_RFC3263_DNS_FAILURE_DETECTION_WINDOW_SIZE_INTERVAL_SEC);
			boolean addTTL = sipProp.getBoolean(CoreProperties.SIP_RFC3263_ADD_TTL);
			long queryTimeoutDuration = sipProp.getLong(CoreProperties.SIP_DNS_QUERY_TIMEOUT);

			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "DomainResolverImpl", "DNS Failover parameters. single query timeout " + singleQueryTimeout +
						" allowed failures: " + allowedFailures + " window size: " + windowSize + " interval: " + interval);
			}

			
			Properties dsProps = new Properties();
			dsProps.put(CoreProperties.DNSSERVERNAMES, dnsServers);

			//we only pass those properties, to provide those values to Resolver 
			// Service from Sip Container custom properties.
			dsProps.put(CoreProperties.DNS_EDNS, sipProp.getString(CoreProperties.DNS_EDNS,true));
			dsProps.put(CoreProperties.DNS_UDP_PAYLOAD_SIZE, sipProp.getShort(CoreProperties.DNS_UDP_PAYLOAD_SIZE));
			dsProps.put(CoreProperties.DNS_REQUEST_CACHE_TIMEOUT_MIN, cacheTimeout);
			dsProps.put(CoreProperties.SIP_RFC3263_DNS_FAILURE_DETECTION_SINGLE_QUERY_TIMEOUT_SEC, singleQueryTimeout);
			dsProps.put(CoreProperties.SIP_RFC3263_DNS_FAILURE_DETECTION_ALLOWED_FAILURES, allowedFailures);
			dsProps.put(CoreProperties.SIP_RFC3263_DNS_FAILURE_DETECTION_WINDOW_SIZE_MIN, windowSize);
			dsProps.put(CoreProperties.SIP_RFC3263_DNS_FAILURE_DETECTION_WINDOW_SIZE_INTERVAL_SEC, interval);
			dsProps.put(CoreProperties.SIP_RFC3263_ADD_TTL, addTTL);
			dsProps.put(CoreProperties.SIP_DNS_QUERY_TIMEOUT, queryTimeoutDuration);
		
			SipResolverService.initialize(dsProps,m_chfw);
			
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "DomainResolverImpl", "SipResolverService initialized.");
			}
			
			_naptrAutoResolve = sipProp.getBoolean(CoreProperties.DNS_SERVER_AUTO_RESOLVE);
			
			_initialized = true;
		} else {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "DomainResolverImpl", "SipResolverService not initialized.");
			} 
		}
	}
	public DomainResolverImpl() {
		
	}
	
	/**
	 * @see DomainResolver#locate(SipURI)
	 */
	public List<SipURI> locate(SipURI sipUri) throws SipURIResolveException {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "locate", sipUri);
		}		
		
		if (!_initialized) {
			throw new SipURIResolveException("Resolver service not initialized."); 
		}
		
		SipURILookupCallbackImpl callback = ThreadLocalStorage.getURILookupCallback();
		
		SIPUri sipUrlToSend = convertURI(sipUri);
		boolean fix = fixNonStandardURI(sipUrlToSend);
		callback.init(sipUri, fix);
		
		List<SipURI> results = null;
		try {
			lookupDestination( sipUrlToSend, callback);
			
			callback.waitForResults();
			
			if (!callback.isErrorResponse()) {
				results = callback.getResults();
				if (c_logger.isTraceDebugEnabled()) {
					int num = (results != null) ? results.size() : 0;
					c_logger.traceDebug(this, "locate", "Retrieved " + num + " results.");
				}
			} else {
				throw new SipURIResolveException("Failed to retrieve DNS result"/*TODO Liberty , callback.getErrorException()*/);
			}
			
			return results;
		} catch (InterruptedException e) {
			throw new SipURIResolveException("Failed to retrieve DNS result", e);
		} catch (IllegalArgumentException e) {
			throw new SipURIResolveException("Failed to retrieve DNS result", e);
		} finally {
			callback.reset();
		}
	}
	
	/**
	 * @see DomainResolver#locate(SipURI, DomainResolverListener, SipSession)
	 */
	public void locate(SipURI sipUri, DomainResolverListener drListener, SipSession sipSession) throws SipURIResolveException {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "locate", new Object[] {sipUri, drListener, sipSession});
		}		
		
		if (!_initialized) {
			throw new SipURIResolveException("Resolver service not initialized."); 
		}
		
		SIPUri sipUrlToSend = convertURI(sipUri);
		boolean fix = fixNonStandardURI(sipUrlToSend);
		
		SipURILookupCallbackImpl callback = new SipURILookupCallbackImpl(sipUri, fix, drListener, sipSession);
		
		try {
			lookupDestination(sipUrlToSend, callback);
		} catch (IllegalArgumentException e) {
			throw new SipURIResolveException("Failed to retrieve DNS result", e);
		}
	}
	
	/**
	 * Create and 'com.ibm.wsspi.sip.channel.protocol.SIPUri' object which is used by sipChannel resolver.
	 * @param sipUrlToSend
	 * @return
	 */
	private final SIPUri convertURI(SipURI sipUrlToSend) {
		SIPUri suri = SIPUri.createSIPUri(sipUrlToSend.toString());
		suri.setHost(sipUrlToSend.getHost());
		suri.setScheme(sipUrlToSend.getScheme());
		suri.setPortInt(sipUrlToSend.getPort());
		suri.setTransport(sipUrlToSend.getTransportParam());
		
		return suri;
	}
	
	/**
	 * Utility method used for both query API's
	 * 
	 * @param sipUri
	 * @param callback
	 */
	private void lookupDestination(SIPUri sipUri, SipURILookupCallbackImpl callback) throws SipURIResolveException{
		SipURILookup request = SipResolverService.getInstance(callback, sipUri);
		
		try {
			if (request.lookup()){
				List<SIPUri> response = request.getAnswer();
				
				if (response == null || response.size() < 1) {
					throw new SipURIResolveException("Failed to retrieve DNS result");
				}
				
				callback.complete(request, true);
			}
		} catch (SipURILookupException e){
			throw new SipURIResolveException("Failed to retrieve DNS result", e);
		}
	}
	
	public void lookupDestination(SIPUri suri, NaptrRequestListener listener) {
		if (!_naptrAutoResolve) {
			listener.error(new SipURIResolveException("Resolver service not initialized."));
			return;
		}
		
		boolean fix = fixNonStandardURI(suri);
		
		SipURILookupCallbackImpl callback = new SipURILookupCallbackImpl(listener, fix);

		// create the SipURILookup object //
		SipURILookup request = SipResolverService.getInstance(callback, suri);
		
		if (c_logger.isTraceDebugEnabled()) {
			StringBuilder buff = new StringBuilder();
			buff.append(" Requested Uri = <").append(suri.toString()).append(">")
				.append(" NaptrRequestListener = <").append(listener).append(">")
				.append(" SipURILookup object = <").append(request.getSipURI().toString()).append(">");
			
			c_logger.traceDebug(this, "lookupDestination", buff.toString());
		}
		
		try {
			if (request.lookup()){				
				//Get the answer sync 
				List<SIPUri> response = request.getAnswer();
				
				if (response == null || response.size() < 1) {
					callback.error(request, new SipURILookupException());
				} else {
					callback.complete(request);	
				}
			}
		}
		catch (SipURILookupException e){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "lookupDestination", 
						"SipURILookupException when lookup = " + e.getMessage());
			}
			callback.error(request, e);
		}
	}
	
	/**
	 * In case of of sip:---:tls URI which is no longer standard we need to modify the sip to sips and
	 * the TLS to TCP.
	 * When resultset will return we need to fix the resultset as well.
	 * We cannot send the sip channel resolver the TLS transport so we have to modify it to TCP.
	 * 
	 * @param suri
	 * @return true if user sent a sip:---:tls URI
	 */
	private boolean fixNonStandardURI(SIPUri suri) {
		boolean corrected = false;
		if (SipUtil.SIP_SCHEME.equalsIgnoreCase(suri.getScheme()) && SipUtil.TLS_TRANSPORT.equalsIgnoreCase(suri.getTransport())) { 
			corrected = true;
			suri.setScheme(SipUtil.SIPS_SCHEME);
			suri.setTransport(SIPTransactionConstants.TCP);
			
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "fixMalformedURI", "correcting target to [" + suri + ']');
			}
		} else {
			if (SipUtil.TLS_TRANSPORT.equalsIgnoreCase(suri.getTransport())){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "fixMalformedURI", "Modifying transport to TCP.");
				}
				suri.setTransport(SIPTransactionConstants.TCP);
			}
			
		}
		
		return corrected;
	}
	
	/**
	 * @return if NAPRT is enabled.
	 */
	public boolean isNaptrAutoResolveEnabled() {
		return _naptrAutoResolve;
	}	
	
}
