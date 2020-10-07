/*******************************************************************************
 * Copyright (c) 2006, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.channel.resolver.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.ws.sip.channel.resolver.dns.impl.Dns;
import com.ibm.ws.sip.channel.resolver.dns.impl.DnsMessage;
import com.ibm.ws.sip.channel.resolver.dns.impl.OPTRecord;
import com.ibm.ws.sip.channel.resolver.dns.impl.ResourceRecord;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.sip.channel.resolver.SipURILookupException;

/**
 * Singleton Instance of the Utility class which handles Sip Naming
 * Requests from WAS components
 * 
 */
public class SipResolver implements SipResolverTransportListener {

	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipResolver.class);


	/** Maximum size of request cache. If this is exceeded service flushes all outstanding
	 * request and starts over.
	 */
	public static final int MAX_REQUEST_CACHE_SIZE = 5000;
	
	private static final String SIP_RFC3263_DNS_FAILURE_DETECTION_SINGLE_QUERY_TIMEOUT_SEC = "SIP_RFC3263_DNS_FAILURE_DETECTION_SINGLE_QUERY_TIMEOUT_SEC";
	private static final String SIP_RFC3263_DNS_FAILURE_DETECTION_ALLOWED_FAILURES = "SIP_RFC3263_DNS_FAILURE_DETECTION_ALLOWED_FAILURES";
	private static final String SIP_RFC3263_DNS_FAILURE_DETECTION_WINDOW_SIZE_MIN = "SIP_RFC3263_DNS_FAILURE_DETECTION_WINDOW_SIZE_MIN";
	private static final String SIP_RFC3263_DNS_FAILURE_DETECTION_WINDOW_SIZE_INTERVAL_SEC = "SIP_RFC3263_DNS_FAILURE_DETECTION_WINDOW_SIZE_INTERVAL_SEC";
	private static final String SIP_USE_PRECISE_SYSTEM_TIMER = "SIP_USE_PRECISE_SYSTEM_TIMER";
	

	/** Results in DNS NS Record Query    */

	private static SipResolver _resolver;

	private static SipResolverTcpTransport _tcpTransport;
	private static SipResolverUdpTransport _udpTransport;
	/** Standard MAX Ethernet MTU 1500 octets (does not include the ethernet header 14 octets and 4 octets CRC) */
	/** 1500 - IPv4 Header 20 octets (no options) - 8 octet UDP Header = 1472                                   */
	/** 1500 - IPv6 Header 40 octets (no options  - 8 octet UDP Header = 1452                                   */
	/** recommendation by rfc 2671 is for a lower value to prevent ICMP from intermediate gateways              */
	private short _UDPpayloadSize = 1280;
    
    private static boolean _bEDNS = true;  
    
	private boolean	_shutdown = false;
    
    private static int UDP_TRANSPORT = 0;
    private static int TCP_TRANSPORT = 1;
    private static int ROLLOVER_TO_TCP = 2;
    private static int transportToTry = UDP_TRANSPORT;

    private int COUNT_BEFORE_TRANSPORT_ROLLBACK = 1000;
    private int requestCount = 0;

	private static CHFWBundle m_chfw;
    
    
    private static boolean _enableSecondaryDNS = false;
    private static SipResolverEventsCounter _eventsCounter;
    
    
    private static long _dnsRequestTimeout = 5 * 1000;
    private static int _allowed_failures = 5;
    private static int _window_size_min = 60 * 10;
    private static int _window_interval_sec = 10;
    private static boolean usePreciseSystemTimer = false;
   
    
 

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
    
    private class RequestPending {
    	private long _startTime;
    	private SipResolverListener _listener;
    	
    	public RequestPending(SipResolverListener listener) {
    		if (usePreciseSystemTimer) {
    			// Convert nanoseconds to milliseconds
    			_startTime = System.nanoTime() / 1000000L;
    		}
    		else {
    			_startTime = System.currentTimeMillis();
    		}
    		_listener = listener;
    	}
    	
    	public SipResolverListener getListener() {
    		return _listener;
    	}
    	
    	public long getStartTime() {
    		return _startTime;
    	}
    	
    }
	
	/** temporary cache for naming store */
	private Hashtable<Integer, RequestPending> _requestPending = new Hashtable<Integer, RequestPending>();

	/**
	 * Method to construct/get and instance of the resolver
	 */
	public static synchronized SipResolver createResolver(Vector<InetSocketAddress> nameServers, Properties containerProps , CHFWBundle chfwB) {
		m_chfw = chfwB;
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(SipResolver.class, "SipResolver: createResolver: entry");
		
		if (_resolver == null) {
			/** Construct the resolver */
			if (c_logger.isTraceDebugEnabled())
				c_logger.traceDebug("SipResolver: createResolver: instantiate the SipResolver");

			if (nameServers != null && nameServers.size() > 1) {
				_enableSecondaryDNS = true;
			} else {
                if (c_logger.isTraceDebugEnabled()){
                	c_logger.traceDebug("SipResolver: secondary DNS is disabled.");
                }
			}
			 
			
			_resolver = new SipResolver();
			
            if (_bEDNS) {
                if (c_logger.isTraceDebugEnabled()){
                	c_logger.traceDebug("SipResolver: initializing UdpTransport");
                }
			    _udpTransport = new SipResolverUdpTransport(nameServers, _resolver, m_chfw);
                transportToTry = UDP_TRANSPORT;
            } else {
                if (c_logger.isTraceDebugEnabled()){
                	c_logger.traceDebug("SipResolver: do NOT initialize UdpTransport");
                }
                transportToTry = TCP_TRANSPORT;
            }
            
			// First construct the TCP transport based on the passed in nameServers
            if (c_logger.isTraceDebugEnabled()){
            	c_logger.traceDebug("SipResolver: initializing TcpTransport");
           }
           _tcpTransport = new SipResolverTcpTransport(nameServers, _resolver, m_chfw);
           
           String val; 

           val = containerProps.getProperty(SIP_RFC3263_DNS_FAILURE_DETECTION_SINGLE_QUERY_TIMEOUT_SEC);

           if (val != null && val.length() > 0) {
        	   try {
        		   long l = Long.parseLong(val);
        		   if (l > 0) {
        			   _dnsRequestTimeout = l * 1000L;
        		   }
        	   } catch (NumberFormatException e) {
        		   if (c_logger.isTraceDebugEnabled())
        			   c_logger.traceDebug("SipResolverService: initialize: invalid SIP_RFC3263_DNS_FAILURE_DETECTION_SINGLE_QUERY_TIMEOUT_SEC " + val);
        	   }
        	   if (c_logger.isTraceDebugEnabled())
        		   c_logger.traceDebug("SipResolverService: initialize: SIP_RFC3263_DNS_FAILURE_DETECTION_SINGLE_QUERY_TIMEOUT_SEC " + _dnsRequestTimeout / 1000);
           }

           val = containerProps.getProperty(SIP_RFC3263_DNS_FAILURE_DETECTION_ALLOWED_FAILURES);

           if (val != null && val.length() > 0) {
        	   try {
        		   int i = Integer.parseInt(val);
        		   if (i > 0) {
        			   _allowed_failures = i;
        		   }
        	   } catch (NumberFormatException e) {
        		   if (c_logger.isTraceDebugEnabled())
        			   c_logger.traceDebug("SipResolverService: initialize: invalid SIP_RFC3263_DNS_FAILURE_DETECTION_ALLOWED_FAILURES " + val);
        	   }
        	   if (c_logger.isTraceDebugEnabled())
        		   c_logger.traceDebug("SipResolverService: initialize: SIP_RFC3263_DNS_FAILURE_DETECTION_ALLOWED_FAILURES " + _allowed_failures);
           }

           val = containerProps.getProperty(SIP_RFC3263_DNS_FAILURE_DETECTION_WINDOW_SIZE_MIN);

           if (val != null && val.length() > 0) {
        	   try {
        		   int i = Integer.parseInt(val);
        		   if (i > 0) {
        			   _window_interval_sec = i;
        		   }
        	   } catch (NumberFormatException e) {
        		   if (c_logger.isTraceDebugEnabled())
        			   c_logger.traceDebug("SipResolverService: initialize: invalid SIP_RFC3263_DNS_FAILURE_DETECTION_WINDOW_SIZE_MIN " + val);
        	   }
        	   if (c_logger.isTraceDebugEnabled())
        		   c_logger.traceDebug("SipResolverService: initialize: SIP_RFC3263_DNS_FAILURE_DETECTION_WINDOW_SIZE_MIN " + _window_interval_sec);
           }                            


           val = containerProps.getProperty(SIP_RFC3263_DNS_FAILURE_DETECTION_WINDOW_SIZE_INTERVAL_SEC);

           if (val != null && val.length() > 0) {
        	   try {
        		   int i = Integer.parseInt(val);
        		   if (i > 0) {
        			   _window_size_min = i;
        		   }
        	   } catch (NumberFormatException e) {
        		   if (c_logger.isTraceDebugEnabled())
        			   c_logger.traceDebug("SipResolverService: initialize: invalid SIP_RFC3263_DNS_FAILURE_DETECTION_WINDOW_SIZE_INTERVAL_SEC " + val);
        	   }
        	   if (c_logger.isTraceDebugEnabled())
        		   c_logger.traceDebug("SipResolverService: initialize: SIP_RFC3263_DNS_FAILURE_DETECTION_WINDOW_SIZE_INTERVAL_SEC " + _window_size_min);
           }
           
           //read the use precise system timer value
           Object tempObject = containerProps.get(SIP_USE_PRECISE_SYSTEM_TIMER);
           if (tempObject != null) {
        	   usePreciseSystemTimer = tempObject instanceof Boolean ? (Boolean) tempObject : false; 
        	   if (c_logger.isTraceDebugEnabled()) {
        		   if (tempObject instanceof Boolean)
        			   c_logger.traceDebug("SipResolver: createResolver:  <" + SIP_USE_PRECISE_SYSTEM_TIMER + ">  is a Boolean");
        		   else
        			   c_logger.traceDebug("SipResolver: createResolver:  <" + SIP_USE_PRECISE_SYSTEM_TIMER + ">  is NOT a Boolean");
         			c_logger.traceDebug("SipResolver: createResolver:  <" + SIP_USE_PRECISE_SYSTEM_TIMER + ">  is set to " +
              			tempObject);
        	   }
           }
           else {
          	 if (c_logger.isTraceDebugEnabled())
           		c_logger.traceDebug("SipResolver: createResolver:  <" + SIP_USE_PRECISE_SYSTEM_TIMER + ">  is not set");
           }
           
           SipResolverEventsCounter.setUsePreciseSystemTimer(usePreciseSystemTimer);
           _eventsCounter = new SipResolverEventsCounter(_window_size_min, _window_interval_sec);
           
        }  		
		
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(SipResolver.class, "SipResolver: createResolver: exit");

		return (_resolver);
	}
	
	/**
	 * 
	 */
	protected synchronized void shutdown() {
		if (_shutdown == false)
		{
			synchronized (_requestPending)
			{
                if (c_logger.isTraceDebugEnabled()){
                    c_logger.traceDebug("SipResolver: shutdown clear _requestPending");
                }
				_requestPending.clear();
			}
			_tcpTransport.shutdown();

            if (_bEDNS) {
                if (c_logger.isTraceDebugEnabled()){
                    c_logger.traceDebug("SipResolver: shutdown UdpTransport");
                }
                _udpTransport.shutdown();
            } else {
                if (c_logger.isTraceDebugEnabled()){
                    c_logger.traceDebug("SipResolver: do not shutdown UdpTransport");
                }
                
            }
            
            _resolver = null;
			_shutdown = true;
		}
	}

	/**
	 * Method to handle Sip specific naming request
	 */
	public SipResolverEvent resolve(DnsMessage dnsRequest, SipResolverListener listener, boolean doTCP) {
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipResolver: resolve: entry id="+hashCode());

		SipResolverEvent event = null;
		/** check the cache */
		
		/** send the DnsMessage out on the wire */
		try
		{
			/** add the SipResolverMsg to the requestCache */
			synchronized (_requestPending) {
				if (_requestPending.size() > MAX_REQUEST_CACHE_SIZE)
				{
					IllegalStateException e =  new IllegalStateException ("Request Cache overflowed. Shutdown SIP Resolver service.");
					transportFailed(e, null);
					throw e;
				}
				
                if (c_logger.isTraceDebugEnabled()){
                    c_logger.traceDebug("SipResolver: resolve put in _requestPending: " + new Integer(dnsRequest.getId()));
                }

				_requestPending.put(new Integer(dnsRequest.getId()), new RequestPending(listener));
			}
			
			if ( (transportToTry == UDP_TRANSPORT) && !doTCP) {
				OPTRecord opt = (OPTRecord)ResourceRecord.createRecord(Dns.OPT);
				opt.setUdpPayloadSize(_UDPpayloadSize);
				dnsRequest.addAdditional(opt);
                if (c_logger.isTraceDebugEnabled()){
                    c_logger.traceDebug("SipResolver: resolve: dns request using UDP");
                }
                _udpTransport.writeRequest(dnsRequest.toBuffer(m_chfw.getBufferManager()));
			} else {
                if (c_logger.isTraceDebugEnabled()){
                    c_logger.traceDebug("SipResolver: resolve: dns request using TCP");
                }
                _tcpTransport.writeRequest(dnsRequest.toBuffer(m_chfw.getBufferManager()));
            }
			
			if (c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("SipResolver: resolve: dns request = " + dnsRequest.toString());
			}
			

		}
		catch (IOException e)
		{
            /** add the SipResolverMsg to the requestCache */
			synchronized (_requestPending)
			{
                if (c_logger.isTraceDebugEnabled()){
                    c_logger.traceDebug("SipResolver: resolve remove from _requestPending: " + new Integer(dnsRequest.getId()));
                }
                _requestPending.remove(new Integer(dnsRequest.getId()));
			}
			transportError (e, null);
		}

		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipResolver: resolve: exit");
		
		return event;
	}

	
	public void responseReceived(WsByteBuffer byteBuffer) {
        SipResolverEvent event;
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipResolver: responseReceived: entry id="+hashCode());

		/** see if the response contains a valid dns message */
		try {
			DnsMessage dnsResponse = new DnsMessage(byteBuffer);
			if (c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("SipResolver: responseReceived: dns response = " + dnsResponse.toString());
			}
			
			/** remove the request from the cache */
			
			RequestPending request;
			synchronized (_requestPending)
			{
                if ( transportToTry == ROLLOVER_TO_TCP) {
                    requestCount++;
                    if (requestCount >= COUNT_BEFORE_TRANSPORT_ROLLBACK) {
                        transportToTry = UDP_TRANSPORT;
                    }
                }
                
                if (c_logger.isTraceDebugEnabled()){
                    c_logger.traceDebug("SipResolver: responseReceived: remove from _requestPending: " + new Integer(dnsResponse.getId()));
                }

                //listener = (SipResolverListener) _requestPending.remove(new Integer(dnsResponse.getId()));
                
                request = _requestPending.remove(new Integer(dnsResponse.getId()));
			}
			
			/** dispatch the event back to the listener */
			if (request != null) {
				long endTime = 0;
				if (usePreciseSystemTimer) {
					// Convert nanoseconds to milliseconds
					endTime = System.nanoTime() / 1000000L;
				}
				else {
					endTime = System.currentTimeMillis();
				}
				
				long startTime = request.getStartTime();
				
				if (endTime - startTime > _dnsRequestTimeout) {
	                if (c_logger.isTraceDebugEnabled()){
	                    c_logger.traceDebug("SipResolver: responseReceived: DNS responded after : " + (endTime - startTime) + " millisec");
	                }					
					int total = _eventsCounter.reportEvent();
					if (total > _allowed_failures && _enableSecondaryDNS) {
			            if (c_logger.isInfoEnabled()){
			            	c_logger.info("SipResolver: switching to secondary DNS");
			            }
			            _eventsCounter.reset();
						_udpTransport.destroyFromTimeout(new SipURILookupException("No Response from DNS Server") );
					}
				}
				SipResolverListener listener = request.getListener();
				
				int rcode = dnsResponse.getRCODE();

				/** check for truncation, if truncated try TCP unless NAME_ERROR */
				if (dnsResponse.getTC() && rcode != Dns.NAME_ERROR) {
						rcode = Dns.TRY_TCP;
				}

                if (c_logger.isTraceDebugEnabled()){
                    c_logger.traceDebug("SipResolver: responseReceived: RC received: " + rcode);
                }
                
				switch (rcode){
				case Dns.NO_ERROR:
					event = new SipResolverEvent();
					
                    if (c_logger.isTraceDebugEnabled())
                        c_logger.traceDebug("SipResolver: responseReceived: Good Response");
                        
                    event.setType(SipResolverEvent.NAMING_RESOLUTION);		
					event.setResponse(dnsResponse);
					listener.handleSipResolverEvent(event);
					break;

                case Dns.NAME_ERROR:
                    event = new SipResolverEvent();
                    
                    if (c_logger.isTraceDebugEnabled())
                        c_logger.traceDebug("SipResolver: responseReceived: notify listener with NAMING_EXCEPTION event");
                    
                    event.setType(SipResolverEvent.NAMING_EXCEPTION);
                    event.setResponse(dnsResponse);
                    listener.handleSipResolverEvent(event);
                    break;

                    
				case Dns.FORM_ERROR:
				case Dns.NOT_IMPL:
                case Dns.REFUSED:
                    event = new SipResolverEvent();
                    
                    if (c_logger.isTraceDebugEnabled())
                        c_logger.traceDebug("SipResolver: responseReceived: notify listener with NAMING_EXCEPTION event");
                    
                    event.setType(SipResolverEvent.NAMING_FAILURE);
                    event.setResponse(dnsResponse);
                    listener.handleSipResolverEvent(event);
                    break;
                    
                case Dns.TRY_TCP:    
					/** try TCP, UDP with EDNS failed or some other error */
                    
                    if (c_logger.isTraceDebugEnabled()){
                        c_logger.traceDebug("SipResolver: responseReceived: retry case received");
                        c_logger.traceDebug("SipResolver: responseReceived: return event with type: NAMING_TRY_TCP");
                    }

                    SipResolverEvent event2 = new SipResolverEvent();
                    event2.setType(SipResolverEvent.NAMING_TRY_TCP);      
                    event2.setResponse(dnsResponse);
                    listener.handleSipResolverEvent(event2);
					break;
				}
			}
			else
			{
				//	This could happen if a request times out and is canceled.
				if (c_logger.isTraceDebugEnabled()){
					c_logger.traceDebug("SipResolver: responseReceived: no matching dns request for response");
				}
			}
			
		} catch (Exception e) {
			c_logger.traceDebug("SipResolver: responseReceived: Error: " + e.getMessage());
		}
		
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipResolver: responseReceived: exit");
	}

	public void transportError(Exception exception, SipResolverTransport transport) {
        // need to synchronized this method with respect to the "resolve(...)"
        // method, or else new request (and/or retries) could be sent and either 
        // incorrectly cleared, or we could generate more calls into transportError
        
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipResolver: transportError: entry id="+hashCode());

		/** Now iterate over the requestCache and notify listener of event */
		try
		{
            // once NAMING_TRANSPORT_RETRY is called, the listener can (and does)
            // call back, using this thread,  into this object's resolve(....) method with the 
            // request to retry.  That thread has the synchronized locks, so the
            // retry request will be worked on.  But then
            // clearing the pending queue after calling the listener will
            // clear out the NEW request.   So first the pending queue needs to be
            // clear, then the transport is told to clear out its data structures,
            // then the retries can be issued.
			Hashtable<Integer, RequestPending> toRetry;
            synchronized (_requestPending) {
                
                if (c_logger.isTraceDebugEnabled()){
                    c_logger.traceDebug("SipResolver: transportError: clearing out pending events - # events: " + _requestPending.size());
                }
          
                toRetry = (Hashtable<Integer, RequestPending>) _requestPending.clone(); 

                if (c_logger.isTraceDebugEnabled()){
                    c_logger.traceDebug("SipResolver: transportError: clear _requestPending: ");
                }
                _requestPending.clear();

                if (transport != null) {
                    transport.prepareForReConnect();
                }
            }

            if (c_logger.isTraceDebugEnabled()){
                c_logger.traceDebug("SipResolver: transportError: sending NAMING_TRANSPORT_RETRY to listeners");
            }
            for(Enumeration e = toRetry.elements(); e.hasMoreElements();){
            	//SipResolverListener listener = (SipResolverListener)e.nextElement();
            	RequestPending request = (RequestPending)e.nextElement();
            	
            	SipResolverListener listener = request.getListener();
            		
                /** alert the listener there has been an error */
                /** this should cause all request to be retried */
                SipResolverEvent event =  new SipResolverEvent();
                event.setType(SipResolverEvent.NAMING_TRANSPORT_RETRY);
                event.setResponse(null);
                listener.handleSipResolverEvent(event);
		    }
		}
		catch(Exception e)
		{
			if (c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("SipResolver: transportError: Exception caught: " + e);
			}
			transportFailed(e, null);
		}
		
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipResolver: transportError: exit" );
	}
	
    
	public void cancelRequest(DnsMessage msg, boolean isTimeout) {
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipResolver: cancelRequest: entry id="+hashCode());

		RequestPending request;
		synchronized (_requestPending){
            if (c_logger.isTraceDebugEnabled()){
                c_logger.traceDebug("SipResolver: cancelRequest: remove from _requestPending: " + new Integer(msg.getId()));
            }
            request = _requestPending.remove(new Integer(msg.getId()));
		}
		
        if (isTimeout && request != null) {
        	long endTime = 0;
        	if (usePreciseSystemTimer) {
        		// Convert nanoseconds to milliseconds
        		endTime = System.nanoTime() / 1000000L;
        	}
        	else {
        		endTime = System.currentTimeMillis();
        	}
        	
			long startTime = request.getStartTime();
			
			if ((endTime - startTime) > _dnsRequestTimeout) {
                if (c_logger.isTraceDebugEnabled()){
                    c_logger.traceDebug("SipResolver: responseReceived: DNS responded after : " + (endTime - startTime) + " millisec");
                }				
				int total = _eventsCounter.reportEvent();
				if (total > _allowed_failures && _enableSecondaryDNS) {
		            if (c_logger.isInfoEnabled()){
		            	c_logger.info("SipResolver: switching to secondary DNS");
		            }
		            _eventsCounter.reset();
					_udpTransport.destroyFromTimeout(new SipURILookupException("No Response from DNS Server") );
				}
			}
        }
		
		
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipResolver: cancelRequest: exit");

	}

	public void transportFailed(Exception exception, SipResolverTransport transport) {
        // need to synchronized this method with respect to the "resolve(...)"
        // method, or else new request (and/or retries) could be sent and either 
        // incorrectly cleared, or we could generate more calls in transportError

        if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipResolver: transportFailed: entry id="+hashCode());

        // once NAMING_TRANSPORT_FAILURE is called, the listener could, using this thread
        // call back into this object's resolve(....) method with new 
        // requests.  That thread has the synchronized locks, so the
        // retry request will be worked on.  But then
        // clearing the pending queue after calling the listener will
        // clear out the NEW request.   So first the pending queue needs to be
        // clear, then the transport is told to clear out its data structures,
        // then the retries can be issued.
        Hashtable toFail;
        synchronized (_requestPending) {
            /** iterate over the requestCache, send errors and clear the cache */

            if (c_logger.isTraceDebugEnabled()){
                c_logger.traceDebug("SipResolver: transportFailed: clearing out pending events - # events: " + _requestPending.size());
            }

            if (transportToTry == UDP_TRANSPORT) {
                transportToTry = ROLLOVER_TO_TCP;
                requestCount = 0;
            } else {
                if (transportToTry == ROLLOVER_TO_TCP) {
                    transportToTry = UDP_TRANSPORT;
                    requestCount = 0;
                }
            }
            
            toFail = (Hashtable) _requestPending.clone(); 

            if (c_logger.isTraceDebugEnabled()){
                c_logger.traceDebug("SipResolver: transportFailed: clear _requestPending: ");
            }
            _requestPending.clear();

            if (transport != null) {
                if (c_logger.isTraceDebugEnabled()){
                    c_logger.traceDebug("SipResolver: transportFailed: tell transport to prepare for re-connect");
                }
                transport.prepareForReConnect();
            }
        }
            
        if (c_logger.isTraceDebugEnabled()){
            c_logger.traceDebug("SipResolver: transportFailed: sending NAMING_TRANSPORT_FAILURE to listeners");
        }
        for(Enumeration e = toFail.elements(); e.hasMoreElements();){
		    
        	//SipResolverListener listener = (SipResolverListener)e.nextElement();
		    RequestPending req = (RequestPending)e.nextElement();
		    SipResolverListener listener = req.getListener();
        	/** multiple dns requests outstanding */
			
		    /** alert the listener there has been an error */
            SipResolverEvent event =  new SipResolverEvent();
		    event.setType(SipResolverEvent.NAMING_TRANSPORT_FAILURE);
		    event.setResponse(null);
		    listener.handleSipResolverEvent(event);
	    }
		
		
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipResolver: transportFailed: exit id="+hashCode());

	}
	
	protected void setUdpPayloadSize(short size){
		_UDPpayloadSize = size;
	}
	
	protected static void setEDNS(boolean bool){
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(SipResolver.class, "SipResolver: setENDS(boolean) setter: " + bool);

        _bEDNS = bool;

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(SipResolver.class, "SipResolver: setENDS(boolean)");
    }
}
/** end of SipResolver class */
