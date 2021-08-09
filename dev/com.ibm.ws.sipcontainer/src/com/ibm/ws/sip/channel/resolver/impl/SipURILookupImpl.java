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

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.channel.resolver.dns.impl.AAAARecord;
import com.ibm.ws.sip.channel.resolver.dns.impl.ARecord;
import com.ibm.ws.sip.channel.resolver.dns.impl.Dns;
import com.ibm.ws.sip.channel.resolver.dns.impl.DnsMessage;
import com.ibm.ws.sip.channel.resolver.dns.impl.NAPTRRecord;
import com.ibm.ws.sip.channel.resolver.dns.impl.Name;
import com.ibm.ws.sip.channel.resolver.dns.impl.ResourceRecord;
import com.ibm.ws.sip.channel.resolver.dns.impl.SRVRecord;
import com.ibm.wsspi.sip.channel.SIPLogKeys;
import com.ibm.wsspi.sip.channel.resolver.SIPUri;
import com.ibm.wsspi.sip.channel.resolver.SipURILookup;
import com.ibm.wsspi.sip.channel.resolver.SipURILookupCallback;
import com.ibm.wsspi.sip.channel.resolver.SipURILookupException;

/**
 * Class to provide inteface for and implement RFC 3263 support.
 * <p>
 * In order to lookup a SIPUri via DNS NAPTR/SRV, a user 
 * need merely implement the SipURILookupCallback interface, 
 * contstuct a SipURILookup object with SipURILookupCallback and SIPUri
 * as parameters, and invoke the lookup()
 * method.
 * <p>
 * The result may be returned synchronously or asynchronously, 
 * and will be contained as a member of this object.
 * <p>
 * Once the SIPUri has been resolved via the underlying naming 
 * service, the user invokes getAnswer(), which returns an 
 * ArrayList of SIPUri objects.  These SIPUri objects have there host, 
 * port, and transport members filled in, thereby identifying a contactable
 * node on the internet.
 * 
 *
 */
public class SipURILookupImpl implements SipURILookup, SipResolverListener{
	
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipURILookupImpl.class);
	
	//X CR 4564
	/** Array to hold the SIPUri objects which the lookup resolves to */
	protected ArrayList<SIPUri>    _answerList;  
	/** SIPUri being resolved */
	protected final SIPUri               _suri;
	protected String               _target;
	protected int                  _port;
	protected String               _transport;
	protected String               _scheme;
	/** Listener for asynchronous call back */
	protected final SipURILookupCallback _sll;
	/** Error */
	
	private	Timer				_timer;
	
	protected StateMachine        _fsm;
	
	private static final short         TARGET_NUMERIC  = 0;
	private static final short         TARGET_HOSTNAME = 1;
	private static final short         TARGET_INVALID  = 255;
	
	private static final short         PORT_NOT_SET = 0;
	private static final short         PORT_SET = 1;
	
	private static final short         SIP_SCHEME = 0;
	private static final short         SIPS_SCHEME = 1;
	
	private static final short         TRANSPORT_NOT_SET = 0;
	private static final short         TRANSPORT_SET = 1;
	
	private static final int           DEFAULT_PORT     = 5060;
	private static final int           DEFAULT_SEC_PORT = 5061;
	
	public static final String TCP            = "tcp";
	public static final String UDP            = "udp";
	public static final String SCTP           = "sctp";
	public static final String SIP            = "sip";
	public static final String SIPS           = "sips";
	private static final String IBM_TTL_PARAM = "ibmttl";
	
	protected	RequestTimeoutTimerTask	_timeoutTask = null;
	//X CR 4564 SipURILookupCache has been removed
	protected final Hashtable <SIPUri, SipURILookupImpl>    	_lookupCache;
	protected final long _cacheTimeout;

    boolean doTCP = false;
    boolean didTCPLast = false;
	
	private static final int MAX_CACHE_ENTRIES = 5000;  
	private static long	messageTimeoutValue = 31 * 1000;	// Default timeout value is 31 seconds
	private static boolean usePreciseSystemTimer = false;
	
	/**
	 * Allow timeout to be configurable.
	 * @param localDNSTimeout
	 */
	public static void setMessageTimeoutValue(long localMessageTimeoutValue) {
		if (c_logger.isTraceDebugEnabled()) 
			c_logger.traceDebug("Setting Message timeout to " + localMessageTimeoutValue);
		messageTimeoutValue = localMessageTimeoutValue;
	}
	
	
	/**
	 * Allow user to set precise system timer.
	 * @param tempUsePreciseSystemTimer
	 */
	public static void setUsePreciseSystemTimer(boolean tempUsePreciseSystemTimer) {
		if (c_logger.isTraceDebugEnabled()) 
			c_logger.traceDebug("Setting usePreciseSystemTimer " + tempUsePreciseSystemTimer);
		usePreciseSystemTimer = tempUsePreciseSystemTimer;
	}
	
	/**
	 * Constructor 
	 * @param sll Listner for async resolution
	 * @param suri SIPUri being resolved
	 */
	protected SipURILookupImpl(SipURILookupCallback sll, SIPUri suri, Timer timer, Hashtable<SIPUri, SipURILookupImpl>	lookupCache, long cacheTimeout){
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipURILookupImpl: constructor: entry: id="+hashCode());
		
		/** Construct the ArrayList to hold the answer */
		_answerList = new ArrayList<SIPUri>();
		/** The SIPUri being resolved */
		_suri = suri;
		/** SipURILookupListener */
		_sll = sll;
		/** SipResolverLookupCache */
		_lookupCache = lookupCache;
		_cacheTimeout = cacheTimeout;
		/** Timer */
		_timer = timer;
		
		/** create the state machine */
		try {
		_fsm = new StateMachine(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipURILookupImpl: constructor: exit: id="+hashCode());
	}
	
	/** 
	 * getter for SIPUri being resolved
	 * @return SIPUri
	 */
	public SIPUri getSipURI(){
		return _suri;
	}
	
	/**
	 * method which invokes the naming service 
	 * @return true if answer is available right away, 
	 * 		   false if answer will be returned asynchronously
	 * @throws SipURILookupException if synchronous exception in resolving SIPUri
	 */
	public synchronized boolean lookup() throws SipURILookupException{
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipURILookupImpl: lookup: entry: id="+hashCode());
		
		if (c_logger.isTraceDebugEnabled()) 
			c_logger.traceDebug("A Sip URI Lookup is being performed for "+ _suri.getBaseSIPUri());
		
		boolean bool = false;
		
		if (_fsm.currentState == StateMachine.INITIAL)
		{
			/** validate the SIPUri which was passed in on the constructor */
			//X CR 4564
			validateSipURI();
			
			/** determine what action should be taken based on what is filled in the request SIPUri */
			short action = rulesMatrix();
			
			/** 424229.1 need to schedule the timer before we run the state machine */
			if ((action != StateMachine.FINISHED) && (action != StateMachine.NAPTR_ERROR)){
				// Here we need to set the timer to cancel this request if a timeout occurs
				_timeoutTask = new RequestTimeoutTimerTask();
					
				Date currentDate = new Date();	//	Get the current time
				
				// do we need to schedule this earlier? 424229
				//	Add the timeout interval to the current time and schedule the task to run.
				_timer.schedule(_timeoutTask, new Date(currentDate.getTime() + messageTimeoutValue));
			}
			
			/** run the state machine */
			_fsm.runMachine(action, null);
			
			/** check the state of this request */
			if (_fsm.currentState == StateMachine.COMPLETE) {
				bool = true;
				
				if (_lookupCache.size() < MAX_CACHE_ENTRIES) {
					//	Before notifying the listener, set the URI expiration timer and 
					//	update the lookup  cache with this latest result.
					UriExpirationTimerTask _expirationTask = new UriExpirationTimerTask();

					Date currentDate = new Date();	//	Get the current time

					//	Add the timeout interval to the current time and schedule the task to run.
					//  cache this lookup for 12 hrs 60*60*12
					_timer.schedule(_expirationTask, new Date(currentDate.getTime() + _cacheTimeout));
					_lookupCache.put(getSipURI(), this);
				}
				if (_timeoutTask != null) {
					_timeoutTask.cancel();
				}

			}	
			else if (_fsm.currentState == StateMachine.ERROR){
				if (_timeoutTask != null) {
					_timeoutTask.cancel();
				}
				
				bool = false;
				throw new SipURILookupException(SipURILookupException.DEFAULTMSG);
			}
			//else {
            //Here we need to set the timer to cancel this request if a timeout occurs
			//	_timeoutTask = new RequestTimeoutTimerTask();
					
			//	Date currentDate = new Date();	//	Get the current time
				
				// do we need to schedule this earlier? 424229
				//	Add the timeout interval to the current time and schedule the task to run.
			//	_timer.schedule(_timeoutTask, new Date(currentDate.getTime() + MESSAGE_TIMEOUT_INTERVAL));
			//}
		}
		else if (_fsm.currentState == StateMachine.COMPLETE){ // came from the cache
			bool = true;
		}
		else {
			_timeoutTask.cancel();
			throw new SipURILookupException(SipURILookupException.LOOKUP_IN_PROGRESS);
		}
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipURILookupImpl: lookup: exit: id="+hashCode());
		
		return bool;
	}
	
	private void validateSipURI() throws SipURILookupException{
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipURILookupImpl: validateSipUri: entry: id="+hashCode());

		if (_suri == null){
			throw new SipURILookupException(SipURILookupException.SIPURI_NULL);
		}
		/** per 3263 target is the maddr parameter of the URI if it is set or the host 
		 *  value of the host port component
		 */
		_target = _suri.getMaddr();
		if (_target == null){
			_target  = _suri.getHost();
			if (_target == null){
				throw new SipURILookupException(SipURILookupException.TARGET_UNDEFINED);
			}
		}
		
		//X CR 4564
		/**
		 * ensure the port is within the valid range 2^16 - 1 
		 *  0 or < 0 indicate the port is not set
		 */
		_port = _suri.getPortInt();
		if ( _port > 65535) {
			throw new SipURILookupException(SipURILookupException.PORT_INVALID + _port);
		}
		
		/**
		 * validate the transport
		 * null indicates the transport is not set
		 */
		_transport = _suri.getTransport();
		if (_transport != null){			
			if (!(_transport.equalsIgnoreCase(TCP) || 
				_transport.equalsIgnoreCase(UDP) ||
				_transport.equalsIgnoreCase(SCTP))){
				throw new SipURILookupException(SipURILookupException.TRANSPORT_INVALID + _transport);
			}
		}
		/**
		 * validate the scheme
		 */
		_scheme = _suri.getScheme();
		if (_scheme == null){
			throw new SipURILookupException(SipURILookupException.SCHEME_UNDEFINED);
		}
		else if(!_scheme.equalsIgnoreCase(SIP) && 
				!_scheme.equalsIgnoreCase(SIPS)) {
			throw new SipURILookupException(SipURILookupException.SCHEME_INVALID + _scheme);		
		}
		/**
		 * validate the transport and the scheme
		 */
		if (_transport != null && _scheme != null){
			if ((_scheme.equals(SIP) && _transport.equalsIgnoreCase(SCTP)) || 
			    (_scheme.equals(SIPS)) && (_transport.equalsIgnoreCase(UDP))) {
				throw new SipURILookupException(SipURILookupException.TRANSPORT_SCHEME_INVALID);									
			}
		}		
		
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipURILookupImpl: validateSipUri: exit: id="+hashCode());

	}

	/**
	 * rules for SIPUri as defined in RFC 3263
	 * 
	 * Matrix for determining whether a dns lookup is necessary; if lookup is unnecessary 
	 * default values as determined by RFC 3263 are used
	 * 
	 * row 0 has a value of 0 (0x0000)
	 * row 1 has a value of 1 (0x0001)
	 * row 2 has a value of 2 (0x0010)
	 * etc
	 * ------------------------------------------------------------------------------------------------
	 * | Value in SIPUri passed in 			   | Action and Result                                    |
	 * |----------------------------------------------------------------------------------------------|
	 * | Target	Transport	Port	Scheme	   | host	   Transport	 Port	 Scheme	  DnsLookup   |
	 * |----------------------------------------------------------------------------------------------|
	 * | numeric | !set    |  !set  |  SIP	   |  numeric |  UDP	    | 5060  |  SIP   | NONE       |
	 * | numeric | !set    |  !set  |  SIPS    |  numeric |  TCP        | 5061  |  SIPS  | NONE       |
	 * | numeric | !set    |   set  |  SIP     |  numeric |  UDP        | set   |  SIP   | NONE       |
	 * | numeric | !set    |   set  |  SIPS    |  numeric |  TCP        | set   |  SIPS  | NONE       |
	 * | numeric |  set    |  !set  |  SIP     |  numeric |  set        | 5060  |  SIP   | NONE       |
	 * | numeric |  set    |  !set  |  SIPS    |  numeric |  set        | 5061  |  SIPS  | NONE       |
	 * | numeric |  set    |   set  |  SIP     |  numeric |  set        | set   |  SIP   | NONE       |
	 * | numeric |  set    |   set  |  SIPS    |  numeric |  set        | set   |  SIPS  | NONE       |
	 * | hostname| !set    |  !set  |  SIP     |  TBD     |  TBD        | TBD   |  SIP   | NAPTR, SRV |
	 * | hostname| !set    |  !set  |  SIPS    |  TBD     |  TBD        | TBD   |  SIPS  | NAPTR, SRV |
	 * | hostname| !set    |   set  |  SIP     |  TBD     |  UDP        | set   |  SIP   | A,AAAA     |
	 * | hostname| !set    |   set  |  SIPS    |  TBD     |  TCP        | set   |  SIPS  | A,AAAA     |
	 * | hostname|  set    |  !set  |  SIP     |  TBD     |  set        | TBD   |  SIP   | SRV        |
	 * | hostname|  set    |  !set  |  SIPS    |  TBD     |  set        | TBD   |  SIPS  | SRV        |
	 * | hostname|  set    |   set  |  SIP     |  TBD     |  set        | set   |  SIP   | A,AAAA     |
	 * | hostname|  set    |   set  |  SIPS    |  TBD     |  set        | set   |  SIPS  | A,AAAA     |
	 * ------------------------------------------------------------------------------------------------
	 * 
	 * @return
	 */	
	private short rulesMatrix(){
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipURILookupImpl: rulesMatrix: entry: id="+hashCode());
		
		//X CR 4564
		short truthTableAction = 0;
		short fsmAction = 0;
		
		SIPUri answer = null;
		
		int bit3 = checkTarget();
		int bit2 = checkTransport();
		int bit1 = checkPort();
		int bit0 = checkScheme();
		
		if (bit3 == TARGET_INVALID){
		  fsmAction = StateMachine.NAPTR_ERROR; 
		  _fsm.initAction = fsmAction;
		  return fsmAction;
		}
		
		/** fully qualify the target if hostname */
		if (bit3 == TARGET_HOSTNAME){
			if (!new Character ('.').equals(_target.charAt(_target.length()-1))){
				_target = _target + ".";
			}
		}
		
		/** 4 boolean inputs bit3, bit2, bit1, bit0 produces a total of 16 combinations*/
	    truthTableAction = (short)(bit3 << 3 | bit2 << 2 | bit1 << 1 | bit0);
	    if (c_logger.isTraceDebugEnabled())
			c_logger.traceDebug("SipURILookupImpl: rulesMatrix: truthTableAction = "+truthTableAction);
		
	    switch (truthTableAction){
	    case 0:
	    	answer = SIPUri.createSIPUri(_suri.getURI());
	    	answer.setScheme(_scheme);
	    	answer.setHost(_target);
	    
	    	answer.setPortInt(DEFAULT_PORT); //5060
	    	answer.setTransport(UDP); //"udp"
	    	_answerList.add(answer);
	    	fsmAction = StateMachine.FINISHED;
	    	break;
	    case 1:
	    	answer = SIPUri.createSIPUri(_suri.getURI());
	    	answer.setScheme(_scheme);
	    	answer.setHost(_target);
	    	
	    	answer.setTransport(TCP); //"tcp"
	    	answer.setPortInt(DEFAULT_SEC_PORT); //5061
	    	_answerList.add(answer);
	    	fsmAction = StateMachine.FINISHED;
	    	break;
	    case 2:
	    	answer = SIPUri.createSIPUri(_suri.getURI());
	    	answer.setScheme(_scheme);
	    	answer.setHost(_target);
	    	answer.setPortInt(_port);
	    	
	    	answer.setTransport(UDP); //"udp"
	    	_answerList.add(answer);
	    	fsmAction = StateMachine.FINISHED;
	    	break;
	    case 3:
	    	answer = SIPUri.createSIPUri(_suri.getURI());
	    	answer.setScheme(_scheme);
	    	answer.setHost(_target);
	    	answer.setPortInt(_port);
	    
	    	answer.setTransport(TCP); //"tcp"
	    	_answerList.add(answer);
	    	fsmAction = StateMachine.FINISHED;
	    	break;
	    case 4:
	    	answer = SIPUri.createSIPUri(_suri.getURI());
	    	answer.setScheme(_scheme);
	    	answer.setHost(_target);
	    	answer.setTransport(_transport);
	    	
	    	answer.setPortInt(DEFAULT_PORT); //5060
	    	_answerList.add(answer);
	    	fsmAction = StateMachine.FINISHED;
	    	break;
	    case 5:
	    	answer = SIPUri.createSIPUri(_suri.getURI());
	    	answer.setScheme(_scheme);
	    	answer.setHost(_target);
	    	answer.setTransport(_transport);
	    	
	    	answer.setPortInt(DEFAULT_SEC_PORT); //"tcp"
	    	_answerList.add(answer);
	    	fsmAction = StateMachine.FINISHED;
	    	break;
	    case 6:
	    case 7:
	    	answer = SIPUri.createSIPUri(_suri.getURI());
	    	answer.setScheme(_scheme);
	    	answer.setHost(_target);
	    	answer.setPortInt(_port);
	    	answer.setTransport(_transport);

	    	_answerList.add(answer);
	    	fsmAction = StateMachine.FINISHED;
	    	break;
	    case 8:	 /** results in a NATPR,SRV query by this object */
	    case 9:
	    	fsmAction = StateMachine.SEND_NAPTR_REQUEST;
	    	break;
	    case 10: /** results in an A,AAAA query by this object; send the A query first */
	    	_transport = UDP;
	    	fsmAction = StateMachine.SEND_A_REQUEST;
	    	break;
	    case 11: /** results in an A,AAAA query by this object; send the A query first */
	    	_transport = TCP;
	    	fsmAction = StateMachine.SEND_A_REQUEST;
	    	break;
	    case 12: /** results in an SRV query by the resolver */
	    case 13:
	    	fsmAction = StateMachine.SEND_SRV_REQUEST;
	    	break;
	    case 14:	
	    case 15: /** results in an A,AAAA query by the resolver; send the A query first */
	    	fsmAction = StateMachine.SEND_A_REQUEST;
	    	break;
	        
	    }
	    if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipURILookupImpl: rulesMatrix: exit: id="+hashCode());
		_fsm.initAction = fsmAction;
		return fsmAction;
	}

	

	private int checkTarget(){
		int result = 0;
		
		/** check the target */
		String [] v6split = _target.split("\\:");
		
		if (v6split.length == 1){ // this must be literal Ipv4 or hostname
			/** check the target */
			String [] v4split = _target.split("\\.");
			if (v4split.length == 4){ 
				result = TARGET_NUMERIC; // probably a numeric IPv4 address, still could be host/domain name though
				for (int i = 0; i < v4split.length; i++){
					try {
						Integer.parseInt(v4split[i]);	
					}
					catch (NumberFormatException nfe){
						result = TARGET_HOSTNAME;
						break;
					}
				}
				try {
					//X CR 4564 Only validate the numeric hostname
					if (result == TARGET_NUMERIC)
						InetAddress.getByName(_target);
				}	
				catch(Exception e ){
					result = TARGET_INVALID;
				}
			}
			else {
				result = TARGET_HOSTNAME;
			}
		}
		else { // check literal IPv6
			/** check the first character of the string for hex value, : or [ */
			if (v6split[0].equals("") ||                           // compression ie ::ffff:a
				Character.digit(v6split[0].charAt(0), 16) <= 15 || // hex digit   ie a:b:c:d
				new Character('[').equals(v6split[0].charAt(0)) ){ // bracket     ie [a:b:c:d]
				try {
					InetAddress.getByName(_target);
				}
				catch(Exception e ){
					result = TARGET_INVALID;
				}	
				result = TARGET_NUMERIC;
			}
			else {
				result = TARGET_INVALID;
			}
		}
		
		return result;
	}
		
	private int checkPort(){
		int result = 0;
		
		if (_port < 0 || _port == 0){
			result = PORT_NOT_SET;
		}
		else {
			result = PORT_SET;
		}
		
		return result;
	}
	
	private int checkScheme(){
		int result = 0;
		
		if (_scheme.equalsIgnoreCase(SIP)) {
			result = SIP_SCHEME;
		}
		else if (_scheme.equalsIgnoreCase(SIPS)){
			result = SIPS_SCHEME;
		}
		
		return result;
	}
	
	private int checkTransport(){
		int result = 0;
		
		if (_transport != null && 
			(_transport.equalsIgnoreCase(TCP) ||
			_transport.equalsIgnoreCase(UDP) ||
			_transport.equalsIgnoreCase(SCTP))){
			result = TRANSPORT_SET;
		}
		else {
			result = TRANSPORT_NOT_SET;
		}
		
		return result;
	}

	/**
	 * getter to return the array list of SIPUri objects which the 
	 * original SIPUri resolved to
	 * 
	 * @return the array list of SIPUri objects
	 */
	public ArrayList<SIPUri> getAnswer(){
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipURILookupImpl: getAnswer: entry: id="+hashCode());
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipURILookupImpl: getAnswer: exit: id="+hashCode());

		return _answerList;
	}
	
		
			
		
	
    /**      
     * Method to receive callback for a {@link SipResolverEvent}        
     * @param event {@link SipResolverEvent} representing a response to 
	 */      
	public void handleSipResolverEvent(SipResolverEvent event){
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipURILookupImpl: handleSipResolverEvent: entry: id="+hashCode());
				
        if (event.getType() != SipResolverEvent.NAMING_TRY_TCP) {
            didTCPLast = false;
        }
        // didTCPLast will only be true here if this is the second time in a row
        // that NAMING_TRY_TCP was received.
        
		switch (event.getType()){
		case SipResolverEvent.NAMING_RESOLUTION:
		case SipResolverEvent.NAMING_EXCEPTION:
			
			_fsm.handleEvent(event);
			
			/** are we complete ?*/
			if (_fsm.currentState == StateMachine.COMPLETE){
				//fillSRVAnswer();
				//completeAnswerFromDns();
		
				_timeoutTask.cancel(); //	Kill the timer task associated with this lookup.
			
				if (_answerList.isEmpty()){
					if (c_logger.isTraceDebugEnabled()) 
						c_logger.traceDebug("SipURILookupImpl: handleSipResolverEvent: sll.error()");
					_sll.error(this, new SipURILookupException(SipURILookupException.NAMING_ERROR));
				} else {
					int ttl = _fsm.getTTL(); // in seconds
					if (ttl > 0) //	Only cache if the ttl is larger than 0
					{
						if (c_logger.isTraceDebugEnabled()) 
							c_logger.traceDebug("SipURILookupImpl: handleSipResolverEvent: ttl(seconds) = " + ttl + " caching results");

						if (_lookupCache.size() < MAX_CACHE_ENTRIES) {
							//	Before notifying the listener, set the URI expiration timer and 
							//	update the lookup  cache with this latest result.
							UriExpirationTimerTask _expirationTask = new UriExpirationTimerTask();

							Date currentDate = new Date();	//	Get the current time


							//	Add the timeout interval to the current time and schedule the task to run.
							_timer.schedule(_expirationTask, new Date(currentDate.getTime() + Math.min(_cacheTimeout, ttl*1000)));

							_lookupCache.put(getSipURI(), this);
						}
					} else {
						if (c_logger.isTraceDebugEnabled()) 
							c_logger.traceDebug("SipURILookupImpl: handleSipResolverEvent: ttl = 0, not caching lookup");
					}

					if (c_logger.isTraceDebugEnabled()) 
						c_logger.traceDebug("SipURILookupImpl: handleSipResolverEvent: sll.complete()");

					if (c_logger.isTraceDebugEnabled()) 
						c_logger.traceDebug("SipURILookupImpl: handleSipResolverEvent: sll.complete()");
					_sll.complete(this);

				}
			}
			else if (_fsm.currentState == StateMachine.ERROR){
				_timeoutTask.cancel();
				_sll.error(this, new SipURILookupException(SipURILookupException.NAMING_ERROR));
			}
			break;
            
        case SipResolverEvent.NAMING_FAILURE:    
		case SipResolverEvent.NAMING_TRANSPORT_FAILURE:
			/** cancel the original timeout task */
			_timeoutTask.cancel();
			_sll.error(this, new SipURILookupException(SipURILookupException.DEFAULTMSG));
			break;
            
		case SipResolverEvent.NAMING_TRANSPORT_RETRY:
            
			/** cancel the original timeout task */
			_timeoutTask.cancel();
			/** reset the state machine */
			_fsm.reset();
			
			/** start over */
			try {

                lookup();
                
			}
			catch (SipURILookupException e){
				_sll.error(this, e);
			}
			
			break;
            
        case SipResolverEvent.NAMING_TRY_TCP:
            
            /** cancel the original timeout task */
            _timeoutTask.cancel();
            /** reset the state machine */
            _fsm.reset();
            
            if (!didTCPLast) {
                didTCPLast = true;
                doTCP = true;

                try {
                    lookup();
                }
                catch (SipURILookupException e){
                    _sll.error(this, e);
                }
                
            } else {
                // TODO
                // what to do here, we already tried NAMING_TRY_TCP once before for
                // this request, should we go through the NAMING_EXCEPTION logic
                // above?
                
            }
            
            break;
            
            
            
            
		default:
			/** cancel the original timeout task */
			_timeoutTask.cancel();

			_sll.error(this, new SipURILookupException(SipURILookupException.NAMING_ERROR));
			break;
		}
		
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipURILookupImpl: handleSipResolverEvent: exit: id="+hashCode());
		
	}
		
	
	/**
	 * @author bpulito
	 */
	private class RequestTimeoutTimerTask extends TimerTask
	{
		public void run() {
			
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "RequestTimeoutTimerTask: run: entry: id="+hashCode());

			if (c_logger.isTraceDebugEnabled())
				c_logger.traceDebug("RequestTimeoutTimerTask: run: Message timeout occurred. Cancelling request for " +
						_suri.getBaseSIPUri());
			/** cancel outstanding dns requests */
			_fsm.cancelRequests();
			/** call the error callback with the timeout exception */
			SIPUri suri = getSipURI();
			_sll.error(SipURILookupImpl.this, new SipURILookupException(SipURILookupException.LOOKUP_TIMEOUT + 
					suri.getBaseSIPUri()));
			cancel();
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "RequestTimeoutTimerTask: run: exit: id="+hashCode());

		}
	}
	
	/**
	 * @author bpulito
	 */
	private class UriExpirationTimerTask extends TimerTask
	{
		public void run() {
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "UriExpirationTimerTask: run: entry: id="+hashCode());	
			
			if (c_logger.isTraceDebugEnabled())
				c_logger.traceDebug("UriExpirationTimerTask: run: Removing request " +
						_suri.getBaseSIPUri()+ " from the SipUResolverLookupCache");
			
			//	remove this entry from the cache
			_lookupCache.remove(getSipURI());
			
			cancel();
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "UriExpirationTimerTask: run: exit: id="+hashCode());
		}
	}
	
	static class StateMachine {

		private int previousState = INITIAL;
		protected int currentState = INITIAL;
		
		protected int initAction = NOOP;

		/** states for this machine */
		private static final int   INITIAL     = 0;
		private static final int   NAPTR_PEND  = 1;
		private static final int   SRV_PEND    = 2;
		private static final int   A_PEND      = 3;
		private static final int   AAAA_PEND   = 4;
		private static final int   COMPLETE    = 5;
		private static final int   ERROR       = 6;

		/** inputs for this state machine */
		private static final short FINISHED           = 0;
		private static final short SEND_NAPTR_REQUEST = 1;
		private static final short SEND_SRV_REQUEST   = 2;
		private static final short SEND_A_REQUEST     = 3;
		private static final short SEND_AAAA_REQUEST  = 4;
		private static final short NAPTR_RESP         = 5;
		private static final short NAPTR_ERROR        = 6;
		private static final short NOOP               = 13;
		
		/** supported services */
		private static final String SIP_D2T  = "SIP+D2T";
		private static final String SIPS_D2T = "SIPS+D2T";
		private static final String SIP_D2U  = "SIP+D2U";
		private static final String SIP_D2S  = "SIPS+D2S"; // sip channel currently does not support D2S


		/** if lookup_stateMachine inputs change, this array must change */
		private static final String mapper[] = {"PH0", "PH1", "PH2", "PH3", "PH4","NAPTR", "PH5",  "SRV", "PH6", "A", "PH7", "AAAA"};

		private final SipURILookupImpl _simpl;
		private static final Class<StateMachine> c = StateMachine.class;

		/** action methods */
		private static final Method CHKANS;
		private static final Method CHKERR;
		private static final Method SNAPTR;
		private static final Method SSRV;
		private static final Method SA;
		private static final Method SAAAA;
		private static final Method ANSWER;
		private static final Method NONE;

		/** vector for outstanding dns requests for this lookup */
		private final Vector<DnsMessage>            _dnsRequests;
		/** vector for completed NATPR responses for this lookup */
		private final Vector<NAPTRRecord>           _NAPTRResponses;
		/** array of vectors for completed SRV responses for this lookup */
		private Vector<SRVRecord> []          _SRVResponses;
		/** Hashtable for completed A responses for this lookup */
		private final Hashtable<String,Vector>      _AResponses;
		/** Hashtable for completed AAAA responses for this lookup */
		private final Hashtable<String,Vector>      _AAAAResponses;

		private static final e lookup_stateMachine[][];

		/** state machine element */
		static class e {
			final Method action;
			final int nextState;

			e(Method m, int s){
				action = m;
				nextState = s;
			}
		}

	
		static {
			if (c_logger.isInfoEnabled())
				c_logger.info("State machine, allocating static data");

			
			Method mCHKANS, mCHKERR, mSNAPTR, mSSRV, mSA, mSAAAA, mANSWER, mNONE;
			mCHKANS = mCHKERR = mSNAPTR = mSSRV = mSA = mSAAAA = mANSWER = mNONE = null;
			
			try {
				mCHKANS   = c.getMethod("checkAnswer", new Class[]{DnsMessage.class});
				mCHKERR   = c.getMethod("checkError",new Class[]{});
				mSNAPTR   = c.getMethod("sendNAPTR", new Class[]{});
				mSSRV     = c.getMethod("sendSRV", new Class[]{});
				mSA       = c.getMethod("sendA", new Class[]{});
				mSAAAA    = c.getMethod("sendAAAA", new Class[]{});
				mANSWER   = c.getMethod("constructAnswer", new Class[]{});
				mNONE     = c.getMethod("none", new Class[]{});
			}
			catch (Exception e ){
				if (c_logger.isTraceDebugEnabled())
					c_logger.traceDebug("StateMachine: StateMachine exception " + e);
			}

			CHKANS = mCHKANS;
			CHKERR = mCHKERR;
			SNAPTR = mSNAPTR;
			SSRV = mSSRV;
			SA = mSA;
			SAAAA = mSAAAA;
			ANSWER = mANSWER;
			NONE = mNONE;
			
			/** state machine to handle state of this lookup */
			/**                                              */
			/** input to the state machine can either be     */
			/** 1) action                                    */
			/** 2) dns message                               */
			/**                                              */
			/** state changes come from                      */
			/** 1) the lookup_stateMachine                   */
			/** 2) the action methods                        */
			lookup_stateMachine = new e [][]{
			/***********************************************************************************************************************************/
			/* Current State */
			/*      :	     
			/*      :         Input --> */
			/*      V     
			/* 		         FINISHED                SEND_NAPTR                SEND_SRV               SEND_A                SEND_AAAA               */
			/* INITIAL    */{new e(ANSWER,COMPLETE), new e(SNAPTR,NAPTR_PEND), new e(SSRV,SRV_PEND),  new e(SA,A_PEND),     new e(SAAAA,AAAA_PEND),
			/*               NAPTR_RESP				 NAPTR_ERROR               SRV_RESP               SRV_ERROR             A_RESP                */
			/* INITIAL    */ new e(NONE,ERROR), 	 new e(NONE,ERROR),        new e(NONE,ERROR),     new e(NONE,ERROR),    new e(NONE,ERROR),      
			/*               A_ERROR				 AAAA_RESP                 AAAA_ERROR             NOOP                                   */ 
			/* INITIAL    */ new e(NONE,ERROR),		 new e(NONE,ERROR),        new e(NONE,ERROR),     new e(NONE,INITIAL)},
			/************************************************************************************************************************************/
			/*               FINISHED                SEND_NAPTR                SEND_SRV               SEND_A                SEND_AAAA               */
			/* NAPTR_PEND */{new e(ANSWER,COMPLETE), new e(NONE,ERROR),        new e(SSRV,SRV_PEND),  new e(SA,SRV_PEND),   new e(SAAAA,AAAA_PEND),
			/*               NAPTR_RESPONSE          NAPTR_ERROR               SRV_RESP               SRV_ERROR             A_RESP                  */
			/* NAPTR_PEND */ new e(CHKANS,NAPTR_PEND),new e(SSRV,SRV_PEND),    new e(NONE,ERROR),     new e(NONE,ERROR),    new e(NONE,ERROR),   
			/*               A_ERROR                 AAAA_RESP                 AAAA_ERROR             NOOP                                       */ 
			/* NAPTR_PEND */ new e(NONE,ERROR),      new e(NONE,ERROR),        new e(NONE,ERROR),     new e(NONE,NAPTR_PEND)},
			/************************************************************************************************************************************/
			/*                FINISHED               SEND_NAPTR                SEND_SRV               SEND_A                SEND_AAAA               */
			/* SRV_PEND   */{new e(ANSWER,COMPLETE), new e(NONE,ERROR),        new e(NONE,ERROR),     new e(SA,A_PEND),     new e(SAAAA,AAAA_PEND),
			/*               NAPTR_RESP              NAPTR_ERROR               SRV_RESP               SRV_ERROR             A_RESP                  */
			/* SRV_PEND   */ new e(NONE,ERROR),      new e(NONE,ERROR),        new e(CHKANS,SRV_PEND),new e(CHKERR,SRV_PEND),new e(NONE,ERROR),      
			/*               A_ERROR                 AAAA_RESP                 AAAA_ERROR             NOOP                                       */ 
			/* SRV_PEND   */ new e(NONE,ERROR),      new e(NONE,ERROR),        new e(NONE,ERROR),     new e(NONE,SRV_PEND)},
			/************************************************************************************************************************************/
			/*                FINISHED               SEND_NAPTR                SEND_SRV               SEND_A                 SEND_AAAA              */
			/* A_PEND     */{new e(ANSWER,COMPLETE), new e(NONE,ERROR),        new e(NONE,ERROR),     new e(NONE,ERROR),     new e(SAAAA,AAAA_PEND),
			/*               NAPTR_RESP              NAPTR_ERROR               SRV_RESP               SRV_ERROR              A_RESP                 */
			/* A_PEND     */ new e(NONE,ERROR),      new e(NONE,ERROR),        new e(NONE,ERROR),     new e(NONE,ERROR),     new e(CHKANS,A_PEND),     
			/*               A_ERROR                 AAAA_RESP                 AAAA_ERROR             NOOP                                       */ 
			/* A_PEND     */ new e(CHKERR,A_PEND),   new e(NONE,ERROR),        new e(NONE,ERROR),     new e(NONE,A_PEND)},
			/************************************************************************************************************************************/
			/*                FINISHED               SEND_NAPTR                SEND_SRV               SEND_A                SEND_AAAA              */
			/* AAAA_PEND  */{new e(ANSWER,COMPLETE), new e(NONE,ERROR),        new e(NONE,ERROR),     new e(NONE,ERROR),    new e(NONE,ERROR),
			/*               NAPTR_RESP              NAPTR_ERROR               SRV_RESP               SRV_ERROR             A_RESP                 */
			/* AAAA_PEND  */ new e(NONE,ERROR),      new e(NONE,ERROR),        new e(NONE,ERROR),     new e(NONE,ERROR),    new e(NONE,ERROR),      
			/*               A_ERROR                 AAAA_RESP                 AAAA_ERROR             NOOP                                      			
			/* AAAA_PEND  */ new e(NONE,ERROR),      new e(CHKANS,AAAA_PEND),  new e(CHKERR,AAAA_PEND),new e(NONE,AAAA_PEND)},
			/************************************************************************************************************************************/
			/*                FINISHED               SEND_NAPTR                SEND_SRV               SEND_A                SEND_AAAA              */
			/* COMPLETE   */{new e(NONE,COMPLETE),   new e(NONE,ERROR),        new e(SSRV,SRV_PEND),  new e(SA,A_PEND),     new e(SAAAA, AAAA_PEND),
			/*               NAPTR_RESP              NAPTR_ERROR               SRV_RESP               SRV_ERROR             A_RESP                 */
			/* COMPLETE   */ new e(NONE,ERROR),      new e(NONE,ERROR),        new e(NONE,ERROR),     new e(NONE,ERROR),    new e(NONE,ERROR),      
			/*               A_ERROR                 AAAA_RESP                 AAAA_ERROR             NOOP                                      */ 
			/* COMPLETE   */ new e(NONE,ERROR),      new e(NONE,ERROR),        new e(NONE,ERROR),     new e(NONE,COMPLETE)},
			/************************************************************************************************************************************/
			/*                FINISHED               SEND_NAPTR                SEND_SRV               SEND_A                SEND_AAAA              */
			/* ERROR      */{new e(ANSWER,COMPLETE), new e(NONE,ERROR),        new e(NONE,ERROR),     new e(NONE,ERROR),    new e(NONE,ERROR),
			/*               NAPTR_RESP              NAPTR_ERROR               SRV_RESP               SRV_ERROR             A_RESP                 */
			/* ERROR      */ new e(NONE,ERROR),      new e(NONE,ERROR),        new e(NONE,ERROR),     new e(NONE,ERROR),    new e(NONE,ERROR),      
			/*               A_ERROR                 AAAA_RESP                 AAAA_ERROR             NOOP                                      */ 
			/* ERROR      */ new e(NONE,ERROR),      new e(NONE,ERROR),        new e(NONE,ERROR),     new e(NONE,ERROR)}
			/************************************************************************************************************************************/
			};
			
		};
		

		StateMachine(SipURILookupImpl simpl) throws Exception{
			_simpl = simpl;

			_dnsRequests    = new Vector<DnsMessage>();
			_NAPTRResponses = new Vector<NAPTRRecord>();
			_SRVResponses   = null;
			_AResponses     = new Hashtable<String,Vector>();
			_AAAAResponses  = new Hashtable<String,Vector>();
			

		}

		//X CR 4564 synchronize this method instead of handleSipResolverEvent
		protected synchronized void runMachine(int input, DnsMessage msg){
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: runMachine: entry: id="+hashCode());

			if (c_logger.isTraceDebugEnabled()) 
				c_logger.traceDebug("StateMachine: runMachine: before the state machine current state = "  + currentState
						+ " input = " + input);

			/** run the state machine */
			e element = lookup_stateMachine[currentState][input];
			/** assign the previous state */
			previousState = currentState;
			/** go to the next state */
			currentState = element.nextState;

			try {
				if (c_logger.isTraceDebugEnabled()) 
					c_logger.traceDebug("StateMachine: runMachine: after the state machine current action method = " + element.action.getName());

				if (msg != null && !msg.isNameError()){
					Object [] arg = {msg};
					element.action.invoke(this, arg);
				}
				else {
					element.action.invoke(this, (Object [])null);
				}
			}
			catch (Exception e){
				if (c_logger.isTraceDebugEnabled()) 
					c_logger.traceDebug("StateMachine: runMachine: state machine error "+ e);
			}

			if (c_logger.isTraceDebugEnabled()) 
				c_logger.traceDebug("StateMachine: runMachine: after state machine current state = "  + currentState);
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: runMachine: exit: id="+hashCode());

		}	

        protected boolean checkSRVAdditional(DnsMessage msg){
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: checkSRVAdditional: entry: id="+hashCode());
			boolean bool = false;
			checkSRV(msg.getAdditional());
			checkA(msg.getAdditional());
			checkAAAA(msg.getAdditional());

			/** if we found something worthwhile in additional let the caller know */
			if (haveSRVResponses() && (!_AResponses.isEmpty() || !_AAAAResponses.isEmpty()) && _dnsRequests.isEmpty()){
				bool = true;
			}
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: checkSRVAdditional: exit: id="+hashCode());
			return bool;
		}

        protected void checkAError(DnsMessage msg){
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: checkAError: entry: id="+hashCode());

			if (_dnsRequests.isEmpty()){
				/** try AAAA per the RFC */
				sendAAAA();
			}

			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: checkAError: exit: id="+hashCode());
		}

        protected boolean checkA(Vector rec){
			boolean bool = false;
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: checkA: entry: id="+hashCode());

			Vector vector1 = null;
			Vector vector2 = rec;
			if (c_logger.isTraceDebugEnabled())
				c_logger.traceDebug("StateMachine: checkAAnswer: answer size " + vector2.size());
			for (Enumeration e = vector2.elements(); e.hasMoreElements();){
				ResourceRecord rr = (ResourceRecord)e.nextElement();
				if (rr.getType() == Dns.A){
					if ((vector1 = _AResponses.get(rr.getName().getString())) == null){
						vector1 = new Vector();
					}

					/** avoid duplicate records */
					boolean dontAdd = false;
					for (Enumeration e1 = vector1.elements(); e1.hasMoreElements();){
						ARecord aRec = (ARecord)e1.nextElement();
                                                //433900.6 - duplicate A Records based on name and address
						//if (aRec.getName().toString().equals(rr.getName().toString())){
                                                if (aRec.getAddress().equals(((ARecord) rr).getAddress())){
							if (c_logger.isTraceDebugEnabled())
								c_logger.traceDebug("duplicate record found for " + rr.getName().toString());
							c_logger.traceDebug("duplicate record found for " + aRec.getAddress().getHostName());
							dontAdd = true;
							break;
						}
					}

					/** avoid duplicate records */
					if (!dontAdd) {
						if (c_logger.isTraceDebugEnabled())
							c_logger.traceDebug("adding record for " + rr.getName().toString());
						vector1.add((ARecord)rr);
						bool = true;
					}

					if (!_AResponses.contains(rr.getName().getString())){
						_AResponses.put(rr.getName().getString(), vector1);
					}
				}	
			}	

			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: checkA: exit: id="+hashCode());
			return bool;

		}

        protected boolean checkAAAA(Vector rec){		
			boolean bool = false;
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: checkAAAA: entry: id="+hashCode());

			Vector vector1 = null;
			Vector vector2 = rec;

			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: checkAAAA: answer size "+ vector2.size());
			for (Enumeration e = vector2.elements(); e.hasMoreElements();){
				ResourceRecord rr = (ResourceRecord)e.nextElement();
				if (rr.getType() == Dns.AAAA){
					if (c_logger.isTraceEntryExitEnabled())
						c_logger.traceEntry(this, "StateMachine: checkAAAA: found AAAA record");
					if ((vector1 = _AAAAResponses.get(rr.getName().getString())) == null){
						if (c_logger.isTraceEntryExitEnabled())
							c_logger.traceEntry(this, "StateMachine: checkAAAA: create new Vector");
						vector1 = new Vector();
					}

					boolean dontAdd = false;
					for (Enumeration e1 = vector1.elements(); e1.hasMoreElements();){
						AAAARecord aaaa = (AAAARecord)e1.nextElement();
                                                //433900.6 - duplicate A Records based on name and address
						//if (aaaa.getName().toString().equals(rr.getName().toString())){
                                                if (aaaa.getAddress().equals(((AAAARecord) rr).getAddress())){
							dontAdd = true;
							break;
						}
					}

					/** avoid duplicate records */
					if (!dontAdd){
						if (c_logger.isTraceEntryExitEnabled())
							c_logger.traceEntry(this, "StateMachine: checkAAAA: add record "+ rr.getName().getString());
						vector1.add((AAAARecord)rr);
						bool = true;
					}


					if (!_AAAAResponses.contains(rr.getName().getString())){
						_AAAAResponses.put(rr.getName().getString(), vector1);
					}
				}
			}

			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: checkAAAA: exit: id="+hashCode());
			return bool;

		}

        protected boolean checkAAAAAdditional(DnsMessage msg){
			boolean bool = false;
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: checkAAAAAdditional: entry: id="+hashCode());
			bool = checkAAAA(msg.getAdditional());
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: checkAAAAAdditional: exit: id="+hashCode());
			return bool;

		}

		public void sendReq(DnsMessage msg){
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: sendReq: entry: id="+hashCode());

			_dnsRequests.add(msg);
			//X CR 4564 
            
			SipResolverEvent event = SipResolverService.getResolver().resolve(msg, _simpl, _simpl.doTCP);

			if (event != null){
				if (!event.successfulResolution()){
					/** cancel the original timeout task */
					_simpl._timeoutTask.cancel();
  
					_simpl._sll.error(_simpl, new SipURILookupException(SipURILookupException.DEFAULTMSG));
				}
			}

			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: sendReq: exit: id="+hashCode());
		}

		public void sendNAPTR(){
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: sendNAPTR: entry: id="+hashCode());

			DnsMessage request = new DnsMessage(Dns.NAPTR, _simpl._target);
			sendReq(request);

			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: sendNAPTR: exit: id="+hashCode());
		}

		public void sendSRV(){
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: sendSRV: entry: id="+hashCode());

			/** if we have NAPTR responses; use for SRV requests */
			if (_NAPTRResponses.size() > 0) {
				/** send out the SRV request for each element in the NAPTR answer */
				//System.out.println("StateMachine:handleResponse + _NAPTRResponses " + request.getNAPTRResponses().size());
				for (Enumeration e = _NAPTRResponses.elements(); e.hasMoreElements();) {
					NAPTRRecord r = (NAPTRRecord) e.nextElement();
					//System.out.println("SipResolverLookupImpl:handleResponse send out SRV ");
					DnsMessage request = new DnsMessage(Dns.SRV, r.getReplacement().getString());
					/**  invoke the resolver */
					sendReq(request);
				}
			}
			else {
				/** NAPTR failed, try udp for sip and tcp for sips */
				if (previousState == NAPTR_PEND){
					String target1 = null;
					String target2 = null;
					if (_simpl._scheme.equalsIgnoreCase(SIP)){
						target1 = "_" + SIP + "._" + UDP + "." + _simpl._target;
						target2 = "_" + SIP + "._" + TCP + "." + _simpl._target;
					}
					else if (_simpl._scheme.equalsIgnoreCase(SIPS)){
						target1 = "_" + SIPS + "._" + TCP + "." + _simpl._target;
					}

					DnsMessage request = new DnsMessage(Dns.SRV, target1);
					sendReq(request);
					
					if (target2 != null){
						DnsMessage request2 = new DnsMessage(Dns.SRV, target2);
						sendReq(request2);
					}
				}
				else { // initial action was SEND_SRV_REQUEST
					/** target is based on the transport */
					String target = null;
					String transport = _simpl._suri.getTransport();
					if (transport.equalsIgnoreCase(UDP)){
						target = "_" + SIP + "._" + UDP + "." + _simpl._target;
					}
					else if (transport.equalsIgnoreCase(TCP)){
						String scheme = _simpl._suri.getScheme();
						if (scheme.equalsIgnoreCase(SIP)){
							target = "_" + SIP + "._" + TCP + "." + _simpl._target;
						}
						else if (scheme.equalsIgnoreCase(SIPS)){
							target = "_" + SIPS + "._" + TCP + "." + _simpl._target;
						}
					}
					else if (transport.equalsIgnoreCase(SCTP)){
						target = "_" + SIPS + "._" + SCTP + "." + _simpl._target;
					}

					DnsMessage request = new DnsMessage(Dns.SRV, target);
					sendReq(request);
				}
			}
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: sendSRV: exit: id="+hashCode());

		}

		public void sendA(){
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: sendA: entry: id="+hashCode());

			/** loop through _SRVResponses, sending A queries for the target */
			if (previousState == SRV_PEND && currentState == A_PEND && haveSRVResponses()){
				for (int i = 0; i < _SRVResponses.length; i ++){
					Vector vector2 = _SRVResponses[i];
					for (Enumeration e = vector2.elements(); e.hasMoreElements();){
						SRVRecord srv = (SRVRecord)e.nextElement();
						DnsMessage req = new DnsMessage(Dns.A, srv.getTarget().toString());
						sendReq(req);
					}
				}
			}
			else {
				DnsMessage request = new DnsMessage(Dns.A, _simpl._target);
				sendReq(request);
			}

			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: sendA: exit: id="+hashCode());
		}

		public void sendAAAA(){
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: sendAAAA: entry: id="+hashCode());

			/** loop through _SRVResponses, sending AAAA queries for the target */
			if (haveSRVResponses()){
				for (int i = 0; i < _SRVResponses.length; i ++){
					Vector vector2 = _SRVResponses[i];
					for (Enumeration e = vector2.elements(); e.hasMoreElements();){
						SRVRecord srv = (SRVRecord)e.nextElement();
						DnsMessage req = new DnsMessage(Dns.AAAA, srv.getTarget().toString());
						sendReq(req);
					}
				}
			}
			else {
				DnsMessage request = new DnsMessage(Dns.AAAA, _simpl._target);
				sendReq(request);
			}

			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: sendAAAA: exit: id="+hashCode());
		}

		public void none(){
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: none: entry: id="+hashCode());
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: none: exit: id="+hashCode());
		}

		protected void reset(){
			previousState = INITIAL;
			currentState = INITIAL;

			/** start this lookup over */
			for (Enumeration e = _dnsRequests.elements(); e.hasMoreElements();){
				DnsMessage msg = (DnsMessage)e.nextElement();
				//X CR 4564
				SipResolverService.getResolver().cancelRequest(msg, false);
			}


			/** clear the request vector */
			_dnsRequests.clear();

			/** reset the response members */
			_NAPTRResponses.clear();
			_SRVResponses = null;
			_AResponses.clear();
			_AAAAResponses.clear();

		}

        protected void fillSRVAnswer(){
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: fillSRVAnswer: entry: id="+hashCode());
			if (haveSRVResponses()){
				Vector [] tmp = _SRVResponses;
				for (int i = 0; i < tmp.length;i++){
					float j = calcTotalWeight((Vector)tmp[i]);
					for (Enumeration e = tmp[i].elements(); e.hasMoreElements(); ){
						SRVRecord r = (SRVRecord)e.nextElement();
						Vector vector2 = fillSipUri(r);

						if (!vector2.isEmpty()){
							/** weight the answer approximatley */
							float k = (float)r.getWeight();
							if(k > 0 && j - k !=0) { 
								for(float l = 0; l < k/(j-k); l++) {
									for (Enumeration e1 = vector2.elements();e1.hasMoreElements();){
										SIPUri suri = (SIPUri)e1.nextElement();
										_simpl._answerList.add(suri);
									}
								}	
							}
							else {
								for (Enumeration e1 = vector2.elements();e1.hasMoreElements();){
									SIPUri suri = (SIPUri)e1.nextElement();
									_simpl._answerList.add(suri);
								}
							}
						}
					}
				}
			}
			else { //coming here from the error path
				fillAError();
			}
			if (c_logger.isTraceDebugEnabled())
				c_logger.traceDebug("StateMachine: fillSRVAnswer: _answerList.size()"+ _simpl._answerList.size());
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: fillSRVAnswer: exit: id="+hashCode());

		}

        protected void fillAAnswer(){
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: fillAAnswer: entry: id="+hashCode());

			for (Enumeration e = _AResponses.elements(); e.hasMoreElements();){
				Vector vector2 = (Vector)e.nextElement();
				for (Enumeration e1 = vector2.elements();e1.hasMoreElements();){
					ARecord a = (ARecord)e1.nextElement();
					SIPUri answer = SIPUri.createSIPUri(_simpl._suri.getURI());
					answer.setScheme(_simpl._scheme);
					answer.setHost(a.getAddress().getHostAddress());

					answer.setTransport(_simpl._transport); //"tcp"
					answer.setPortInt(_simpl._port); //5060
					addUriToList(answer, _simpl._answerList, a);
				}
			}

			for (Enumeration e = _AAAAResponses.elements(); e.hasMoreElements();){
				Vector vector2 = (Vector)e.nextElement();
				for(Enumeration e1 = vector2.elements();e1.hasMoreElements();){
					AAAARecord aaaa = (AAAARecord)e1.nextElement();
					SIPUri answer = SIPUri.createSIPUri(_simpl._suri.getURI());
					answer.setScheme(_simpl._scheme);
					answer.setHost(aaaa.getAddress().getHostAddress());

					answer.setTransport(_simpl._transport); //"tcp"
					answer.setPortInt(_simpl._port); //5060
					addUriToList(answer, _simpl._answerList, aaaa);
				}
			}

			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: fillAAnswer: exit: id="+hashCode());

		}

        protected void fillAError(){
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: fillAError: entry: id="+hashCode());
			if (c_logger.isTraceDebugEnabled())
				c_logger.traceDebug("StateMachine: fillAError: _target = " + _simpl._target);
			
			Vector vector1 = null;
			if ((vector1 = _AResponses.get(_simpl._target)) != null){
				for (Enumeration e1 = vector1.elements();e1.hasMoreElements();){
					ARecord aRec = (ARecord)e1.nextElement();
					SIPUri answer = SIPUri.createSIPUri(_simpl._suri.getURI());
					answer.setScheme(_simpl._scheme);
					answer.setHost(aRec.getAddress().getHostAddress());

					if (_simpl._scheme.equals(SIP)){
						if (_simpl._transport == null)
							answer.setTransport(UDP); //"tcp"
						answer.setPortInt(5060); //5060
					}
					else {
						if (_simpl._transport == null)
							answer.setTransport(TCP); //"tcp"
						answer.setPortInt(5061); //5060

					}
					addUriToList(answer, _simpl._answerList, aRec);
				}
			}

			if ((vector1 = _AAAAResponses.get(_simpl._target)) != null){
				for(Enumeration e1 = vector1.elements();e1.hasMoreElements();){
					AAAARecord aaaa = (AAAARecord)e1.nextElement();
					SIPUri answer = SIPUri.createSIPUri(_simpl._suri.getURI());
					answer.setScheme(_simpl._scheme);
					answer.setHost(aaaa.getAddress().getHostAddress());

					if (_simpl._scheme.equals(SIP)){
						if (_simpl._transport == null)
							answer.setTransport(UDP); //"tcp"
						answer.setPortInt(5060); //5060
					}
					else {
						if (_simpl._transport == null)
							answer.setTransport(TCP); //"tcp"
						answer.setPortInt(5061); //5060

					}
					
					addUriToList(answer, _simpl._answerList, aaaa);
				}
			}

			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: fillAError: exit: id="+hashCode());

		}

        protected float calcTotalWeight(Vector vec){
			float f = 0;
			for (Enumeration e = vec.elements(); e.hasMoreElements();){
				SRVRecord r = (SRVRecord)e.nextElement();
				f += (float)r.getWeight();
			}
			return f;
		}

        protected Vector fillSipUri(SRVRecord r){
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: fillSipUri: entry: id="+hashCode());

			Vector<SIPUri> vector2 = new Vector<SIPUri>();
			Vector vector1 = null;
			/** map target to literal IPv4 Address */
			if ((vector1 = _AResponses.get(r.getTarget().toString())) != null){
			    for (Enumeration e = vector1.elements(); e.hasMoreElements();){
			         ARecord aRec = (ARecord)e.nextElement();
			         SIPUri suri = SIPUri.createSIPUri(_simpl._suri.getBaseSIPUri());

			         //s.setHost(r.getTarget().getString());
			         suri.setHost(aRec.getAddress().getHostAddress());
			         suri.setPort(new Integer(r.getPort()).toString());
			         suri.setTransport(r.getProtocol().substring(1));
			         suri.setScheme(r.getService().substring(1));

			         addUriToList(suri, vector2, aRec);
			    }	
			}

			/** map target to literal IPv6 Address */

			if ((vector1 = _AAAAResponses.get(r.getTarget().toString())) != null) {
				for (Enumeration e = vector1.elements(); e.hasMoreElements();){			
					AAAARecord aaaa = (AAAARecord)e.nextElement();

					SIPUri suri = SIPUri.createSIPUri(_simpl._suri.getBaseSIPUri());

					//s.setHost(r.getTarget().getString());
					suri.setHost(aaaa.getAddress().getHostAddress());
					suri.setPort(new Integer(r.getPort()).toString());
					suri.setTransport(r.getProtocol().substring(1));
					suri.setScheme(r.getService().substring(1));

					addUriToList(suri, vector2, aaaa);
				}	
			}
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: fillSipUri: exit: id="+hashCode());

			return vector2;
		
        }


		/**
		 * Method to loop through the array of Dns Records and return the lowest ttl
		 * @return ttl
		 */
        protected int getTTL(){
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: getTTL: entry: id="+hashCode());

			int i = Integer.MAX_VALUE;

			for (Enumeration e = _NAPTRResponses.elements(); e.hasMoreElements();){
				NAPTRRecord rec = (NAPTRRecord)e.nextElement();
				if ( rec.getTTL() < i){
					i = rec.getTTL();
				}
			}
			
			if (haveSRVResponses()){
				for (int j = 0; j < _SRVResponses.length; j++){
					if (!_SRVResponses[j].isEmpty()){
						SRVRecord rec = (SRVRecord)_SRVResponses[j].firstElement();
						if (rec.getTTL() < i){
							i = rec.getTTL();
						}
					}
				}
			}
			
			for (Enumeration e = _AResponses.elements(); e.hasMoreElements();){
				Vector vector2 = (Vector)e.nextElement();
				for (Enumeration e1 = vector2.elements(); e1.hasMoreElements();){	
					ARecord rec = (ARecord)e1.nextElement();
					if ( rec.getTTL() < i){
						i = rec.getTTL();
					}
				}
			}
			
			for (Enumeration e = _AAAAResponses.elements(); e.hasMoreElements();){
				Vector vector2 = (Vector)e.nextElement();
				for (Enumeration e1 = vector2.elements(); e1.hasMoreElements();){	
					AAAARecord rec = (AAAARecord)e1.nextElement();
					if ( rec.getTTL() < i){
						i = rec.getTTL();
					}
				}	
			}
			/** no DnsRecords found */
			if(i == Integer.MAX_VALUE){
				i = 0;
			}
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: getTTL: exit: id="+hashCode());

			return i;
		}

		public void checkAnswer(DnsMessage msg){
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: checkAnswer: entry: id="+hashCode());

			int input = NOOP;

			switch (currentState){
			case NAPTR_PEND:
				checkNAPTRAnswer(msg);
				checkAdditional(msg);

				/** send out the SRV requests if NAPTR complete */
				if (currentState != COMPLETE){
					input = SEND_SRV_REQUEST;
				}
				break;
			case SRV_PEND:
				checkSRV(msg.getAnswers());
				checkAdditional(msg);

				/** if all SRV Requests are satisfied */
				if (_dnsRequests.isEmpty() && (currentState != COMPLETE || _simpl._answerList.isEmpty())){
					input = SEND_A_REQUEST;
				}	

				break;
			case A_PEND:
				checkA(msg.getAnswers());
				checkAdditional(msg);
				if (_dnsRequests.isEmpty() && (currentState != COMPLETE || _simpl._answerList.isEmpty())){
					input = SEND_AAAA_REQUEST;
				}

				break;
			case AAAA_PEND:
				checkAAAA(msg.getAnswers());
				if (_dnsRequests.isEmpty() && currentState != COMPLETE){
					input = FINISHED;
				}
				break;
			case COMPLETE:
			case ERROR:
				break;//
			}

			/** run the state machine */
			runMachine(input, null);


			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: checkAnswer: exit: id="+hashCode());

		}

		public void checkAdditional(DnsMessage msg){
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: checkAdditional: entry: id="+hashCode());

			int input = NOOP;

			switch (currentState){
			case NAPTR_PEND:
				if (checkSRVAdditional(msg)){
					input = FINISHED;
				}
				break;
			case SRV_PEND:
				if (checkSRVAdditional(msg)){
					input = FINISHED;
				}
				break;
			case A_PEND:
				if (checkAAAAAdditional(msg)){
					input = FINISHED;
				}
				break;
			case AAAA_PEND:
				break;
			case COMPLETE:
			case ERROR:
				break;
			}

			/** run the state machine */
			runMachine(input, null);


			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: checkAdditional: exit: id="+hashCode());

		}

		public void checkError(){
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: checkError: entry: id="+hashCode());

			int input = NOOP;

			switch (currentState){
			case NAPTR_PEND:
				/** if NAPTR fails, try SRV for target */
				input = SEND_SRV_REQUEST;
				break;
			case SRV_PEND:
				/** if all SRV requests have failed, try A lookup on target */
				if (_dnsRequests.isEmpty()){
					input = SEND_A_REQUEST;
				}
				break;
			case A_PEND:
				/** if nothing else outstanding, try AAAA on target */
				if (_dnsRequests.isEmpty()){
					/** try AAAA per the RFC */
					input = SEND_AAAA_REQUEST;
				}
				break;
			case AAAA_PEND:
				/** if nothing else outstanding see if we have enough to complete the lookup */
				if (_dnsRequests.isEmpty()){
					input = FINISHED;
				}
				break;
			case COMPLETE:
			case ERROR:
				break;
			}

			/** run the state machine */
			runMachine(input, null);

			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: checkError: exit: id="+hashCode());

		}

		/**
		 * iterate through a dns message representing a NAPTR response, ordering the answers
		 * 
		 * @param msg
		 * @return true for valid NAPTR answer
		 */
		protected boolean checkNAPTRAnswer(DnsMessage msg){
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: checkNAPTRAnswer: entry: id="+hashCode());

			Vector vAnswer = msg.getAnswers();
			if (vAnswer != null){
				for (Enumeration e = vAnswer.elements();e.hasMoreElements();){
					if (c_logger.isTraceDebugEnabled())
						c_logger.traceDebug("StateMachine: checkNAPTRAnswer: v size " + vAnswer.size());
					NAPTRRecord r = (NAPTRRecord)e.nextElement();
					/** throw away non secure services for secure requests */
					//if (_scheme.startsWith(SIPS) && !(r.getService().startsWith("SIPS"))){
					//	if (c_logger.isTraceDebugEnabled())
					//		c_logger.traceDebug("StateMachine: checkNAPTRAnswer: looking for secure answer ");
					/** ensure we have a valid NAPTR Service */
					if (validateNAPTRService(r)){
						continue;
					}
					if (_NAPTRResponses.isEmpty()){
						if (c_logger.isTraceDebugEnabled())
							c_logger.traceDebug("StateMachine: checkNAPTRAnswer: insert into empty list");
						_NAPTRResponses.insertElementAt(r, 0);
					}
					else {
						int i = 0;
						for (Enumeration e1 = _NAPTRResponses.elements(); e1.hasMoreElements();){
							//NAPTRRecord existing = (NAPTRRecord)_NAPTRResponses.get(r.getOrder());
							NAPTRRecord existing = (NAPTRRecord)e1.nextElement();
							i = _NAPTRResponses.indexOf(existing);
							if (c_logger.isTraceDebugEnabled())
								c_logger.traceDebug("StateMachine: checkNAPTRAnswer: after existing ");
							/** must check preference if 2 elements have the same order */
							if (r.getOrder() == existing.getOrder()){  
								if ((r.getPreference() >= existing.getPreference())){
									if (c_logger.isTraceDebugEnabled())
										c_logger.traceDebug("StateMachine: checkNAPTRAnswer: answers same Order higer pref ");
									_NAPTRResponses.insertElementAt(r, i);
									break;
								}
								else {
									if (c_logger.isTraceDebugEnabled())
										c_logger.traceDebug("StateMachine: checkNAPTRAnswer: same Order lower pref ");
									_NAPTRResponses.insertElementAt(r, i + 1);
									break;
								}
							}
							else if (r.getOrder() < existing.getOrder()){
								if (c_logger.isTraceDebugEnabled())
									c_logger.traceDebug("StateMachine: checkNAPTRAnswer: lower order");
								_NAPTRResponses.insertElementAt(r, i);
								break;  // 416823
							}
							else {
								continue;
							}

						}
						/** if the record wasn't inserted add it to the end of the vector */
						if (!_NAPTRResponses.contains(r)){
							if (c_logger.isTraceDebugEnabled())
								c_logger.traceDebug("StateMachine: checkNAPTRAnswer: insert at the end of list");
							_NAPTRResponses.insertElementAt(r, i + 1);
						}
					}
				}
			}

			boolean bool = false;
			if (_NAPTRResponses.isEmpty()){
				bool = false;
			}
			else {
				if (c_logger.isTraceDebugEnabled())
					c_logger.traceDebug("StateMachine: checkNAPTRAnswer: setting up SRVResponses");
				/** only set up _SRVResponses when we have NAPTR here  419977.1*/
				_SRVResponses = (Vector<SRVRecord>[])new Vector[_NAPTRResponses.size()];
				for (int i = 0; i < _NAPTRResponses.size(); i ++){
					_SRVResponses[i] = new Vector<SRVRecord>();
				}
				bool = true;
			}
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: checkNAPTRAnswer: exit: id="+hashCode());

			return bool;
		}
		
        protected boolean validateNAPTRService(NAPTRRecord nr){
			boolean bool = false;
			
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: validateNAPTRService: entry: id="+hashCode());
			
			String service = nr.getService();
			
			if (_simpl._scheme.startsWith(SIPS) && !(service.startsWith("SIPS"))){
				
				if (c_logger.isTraceDebugEnabled())
					c_logger.traceDebug("StateMachine: validateNAPTRService: Non-Secure service");
				bool = true;	
			} 
			//X CR 4564 
			else if(!service.equals(SIP_D2T) &&
					!service.equals(SIPS_D2T) &&
					!service.equals(SIP_D2U)) {
				
				if (c_logger.isTraceDebugEnabled())
					c_logger.traceDebug("StateMachine: validateNAPTRService: Unsupported service " + service);
				
				bool = true;
			}
			
			/** now check the replacement field for service */
                        /** per RFC 2915, don't validate the replacement field for SRV */
                        
			//String replacement = nr.getReplacement().getString();
			//if  (!replacement.startsWith("_"+SIP)){
		        //		if (c_logger.isTraceDebugEnabled())
		        //			c_logger.traceDebug("StateMachine: validateNAPTRService: Unsupported replacment service " + replacement);
		        //		bool = true;
			//}
                        
       			/** now check the replacement field for protocol */
			
			//if  (!replacement.contains("_"+UDP+".") && 
			//	 !replacement.contains("_"+TCP+".")	&&
			//	 !replacement.contains("_"+SCTP+".")){
			//		if (c_logger.isTraceDebugEnabled())
			//			c_logger.traceDebug("StateMachine: validateNAPTRService: Unsupported replacment protocol " + replacement);
			//		bool = true;
			//	}
			
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: validateNAPTRService: exit: id="+hashCode());
			
			return bool;
		}
		
		// RWH DEFECT#424213
		private void mapNAPTRtransportToSRVprotocol(NAPTRRecord naptr, SRVRecord srv){
			String service = naptr.getService();
			if (service.equals(SIP_D2U))
				srv.setProtocol("_"+UDP);
			else if (service.equals(SIPS_D2T))
				srv.setProtocol("_"+TCP);
			else if (service.equals(SIP_D2T))
				srv.setProtocol("_"+TCP);
			else if (service.equals(SIP_D2S))
				srv.setProtocol("_"+SCTP);
			return;
		}
		
		/**
		 * iterate through a dns message representing a NAPTR response, looking for SRV, A, AAAA answers
		 * 
		 * @param msg
		 * @return true if we have enough to complete this lookup
		 */
		/**
		 * iterate through a dns message representing an SRV dns response, ordering the answers
		 * @param vAnswer
		 * @return true if there are no longer any dns requests outstanding for this lookup
		 */
		protected boolean checkSRV(Vector vec){
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: checkSRV: entry: id="+hashCode());
			Vector vAnswer = vec;
			/** if there was no NAPTR for this SRV, set up _SRVResponses as if there were 1 NAPTR response*/
			if (_SRVResponses == null){
				if (c_logger.isTraceEntryExitEnabled())
					c_logger.traceEntry(this, "StateMachine: checkSRV: setting up SRVResponses");
				/** set up _SRVResponses */
				_SRVResponses = (Vector<SRVRecord>[])new Vector[1];
				_SRVResponses[0] = new Vector<SRVRecord>();

			}

			if (c_logger.isTraceDebugEnabled())
				c_logger.traceDebug("StateMachine: checkSRV: vAnswer size " + vAnswer.size());
			for (Enumeration e = vAnswer.elements();e.hasMoreElements();){
				//SRVRecord srv = (SRVRecord)e.nextElement();
				ResourceRecord rr = (ResourceRecord)e.nextElement();
				if (c_logger.isTraceDebugEnabled())
					c_logger.traceDebug("StateMachine: checkSRV: type " + rr.getType());
				if (rr.getType() == Dns.SRV){
					SRVRecord srv = (SRVRecord)rr;
					if (!_NAPTRResponses.isEmpty()){
						if (c_logger.isTraceDebugEnabled())
							c_logger.traceDebug("StateMachine: checkSRV: have Naptr ");
						for (Enumeration e1 = _NAPTRResponses.elements();e1.hasMoreElements();){
							NAPTRRecord naptr = (NAPTRRecord)e1.nextElement();
							Name n  = naptr.getReplacement();

							String s = srv.getService() + "." + srv.getProtocol() + "." + srv.getSrvName();
							if (c_logger.isTraceDebugEnabled())
								c_logger.traceDebug("StateMachine: checkSRV: replacement " + n.getString());
							if (c_logger.isTraceDebugEnabled())
								c_logger.traceDebug("StateMachine: checkSRV: srv " + s);
							if(s.equals(n.getString())) {
								// RWH DEFECT#424213 if we have NAPTR use the transport from the service field
								mapNAPTRtransportToSRVprotocol(naptr, srv);
								if (_SRVResponses[_NAPTRResponses.indexOf(naptr)].isEmpty()){
									if (c_logger.isTraceDebugEnabled())
										c_logger.traceDebug("StateMachine: checkSRV: insert into empty list");
									_SRVResponses[_NAPTRResponses.indexOf(naptr)].insertElementAt(srv, 0);
									break;
								}
								else {
									int i = 0;
									for (Enumeration e2 = _SRVResponses[_NAPTRResponses.indexOf(naptr)].elements(); e2.hasMoreElements();){
										//SRVRecord existing = (SRVRecord)_SRVResponses.elementAt(srv.getPriority());
										SRVRecord existing = (SRVRecord)e2.nextElement();
										i = _SRVResponses[_NAPTRResponses.indexOf(naptr)].indexOf(existing);
										if (srv.getPriority() == existing.getPriority()){
											/** must check weight if two elements with the same priority */
											if (srv.getWeight() >= existing.getWeight() ){
												if (c_logger.isTraceDebugEnabled())
													c_logger.traceDebug("StateMachine: checkSRV: insert same pri higher weight");
												_SRVResponses[_NAPTRResponses.indexOf(naptr)].insertElementAt(srv, i);
												break;
											} 
											else {
												_SRVResponses[_NAPTRResponses.indexOf(naptr)].insertElementAt(srv, i + 1);
												if (c_logger.isTraceDebugEnabled())
													c_logger.traceDebug("StateMachine: checkSRV: insert same pri lower weight");
												break;
											}
										}
										else if (srv.getPriority() < existing.getPriority()){
											//System.out.println("StateMachine:checkSRVs lower priority");
											_SRVResponses[_NAPTRResponses.indexOf(naptr)].insertElementAt(srv, i);
											break;

										}
										else {
											continue;
										}
									}
									/** if the record wasn't inserted add it to the end of the vector */
									if (!_SRVResponses[_NAPTRResponses.indexOf(naptr)].contains(srv)){
										if (c_logger.isTraceDebugEnabled())
											c_logger.traceDebug("StateMachine: checkSRV: insert at the end of list");
										_SRVResponses[_NAPTRResponses.indexOf(naptr)].insertElementAt(srv, i + 1);
									}

								}
							}

						}
					}// there is no NAPTR
					else { /** no NAPTR for this SRV response */
						if (c_logger.isTraceDebugEnabled())
							c_logger.traceDebug("StateMachine: checkSRV: checking SRV with NO NAPTR");
						
						if (validateSRVServiceAndProtocol(srv)){
							continue;
						}
						
						if (_SRVResponses[0].isEmpty()){
							if (c_logger.isTraceDebugEnabled())
								c_logger.traceDebug("StateMachine: checkSRV: no NAPTR insert into empty list");
							_SRVResponses[0].insertElementAt(srv, 0);
						}
						else {
							int i = 0;
							for (Enumeration e2 = _SRVResponses[0].elements(); e2.hasMoreElements();){
								//SRVRecord existing = (SRVRecord)_SRVResponses.elementAt(srv.getPriority());
								SRVRecord existing = (SRVRecord)e2.nextElement();
								i = _SRVResponses[0].indexOf(existing);
								if (srv.getPriority() == existing.getPriority()){
									/** must check weight if two elements with the same priority */
									if (srv.getWeight() >= existing.getWeight() ){
										if (c_logger.isTraceDebugEnabled())
											c_logger.traceDebug("StateMachine: checkSRV: same pri higher weight");
										_SRVResponses[0].insertElementAt(srv, i);
										break;
									} 
									else {
										if (c_logger.isTraceDebugEnabled())
											c_logger.traceDebug("StateMachine: checkSRV: same pri lower weight");
										_SRVResponses[0].insertElementAt(srv, i + 1);
										break;
									}
								}
								else if (srv.getPriority() < existing.getPriority()){
									if (c_logger.isTraceDebugEnabled())
										c_logger.traceDebug("StateMachine: checkSRV: lower priority");
									_SRVResponses[0].insertElementAt(srv, i);
									break;

								}
								else {
									continue;
								}
							}
							/** if the record wasn't inserted add it to the end of the vector */
							if (!_SRVResponses[0].contains(srv)){
								if (c_logger.isTraceDebugEnabled())
									c_logger.traceDebug("StateMachine: checkSRV: insert at the end of list");
								_SRVResponses[0].insertElementAt(srv, i + 1);
							}
						}
					}
				}
			}

			boolean bool = false;
			if (_dnsRequests.isEmpty()){
				bool = true;
			}
			else{
				bool = false;
			}
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: checkSRV: exit: id="+hashCode());

			return bool;
		}
		
		private boolean validateSRVServiceAndProtocol(SRVRecord sr){
			boolean bool = false;
			
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: validateSRVServiceAndProtocol: entry: id="+hashCode());
			
			String service = sr.getService();
			/** service for 3263 must be _sip or _sips */
			if (!service.startsWith("_"+SIP) && !service.startsWith("_"+SIPS)){
				if (c_logger.isTraceDebugEnabled())
					c_logger.traceDebug("StateMachine: validateSRVServiceAndProtocol: Invalid service " + service);
				bool = true;
			}
			
			if (_simpl._scheme.startsWith(SIPS) && !(service.startsWith("_" + SIPS))){
				if (c_logger.isTraceDebugEnabled())
					c_logger.traceDebug("StateMachine: validateSRVServiceAndProtocol: Non-Secure service");
				bool = true;	
			} 
			
			String protocol = sr.getProtocol();
			if(!protocol.equals("_"+UDP) && 
			   !protocol.equals("_"+TCP) &&
			   !protocol.equals("_"+SCTP)){
				if (c_logger.isTraceDebugEnabled())
					c_logger.traceDebug("StateMachine: validateSRVServiceAndProtocol: Invalid protocol " + protocol);
				bool = true;
			}
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: validateSRVServiceAndProtocol: exit: id="+hashCode());
			
			return bool;
		}


		protected void handleEvent(SipResolverEvent event){
			DnsMessage dnsResponse = event.getResponse();
			DnsMessage dnsRequest = getDnsRequest(dnsResponse);
			/** remove the request from the request vector, else return */
			if (dnsRequest != null){
				_dnsRequests.remove(dnsRequest);
			}
			else {
				if (c_logger.isTraceEntryExitEnabled())
					c_logger.traceEntry(this, "StateMachine: handleSipResolverEvent: exit: No matching request id="+hashCode());
				return;
			}
			
			int input = 0;
			for (int i = 0; i < mapper.length; i ++){
				if ((Dns.TYPESTRING[dnsResponse.getQtype()]).equals(mapper[i])){
					input = i;
					break;
				}
			}
			/** if the dns response error, inc input by 1 */
			if (dnsResponse.isNameError()){
				input++;
			}
			
			runMachine(input, dnsResponse);
		}
		
		protected DnsMessage getDnsRequest(DnsMessage dnsResponse){
			DnsMessage dnsRequest = null;
			
			for (Enumeration e = _dnsRequests.elements(); e.hasMoreElements();){
				dnsRequest = (DnsMessage)e.nextElement();
				if (dnsRequest.getId() == dnsResponse.getId()){
					break;
				}
			}
			return dnsRequest;
		}
		
		protected void cancelRequests(){
			/** First, inform the resolver that this request is being canceled due to a timeout */
			for (Enumeration e = _dnsRequests.elements(); e.hasMoreElements();){
				DnsMessage msg = (DnsMessage)e.nextElement();
				SipResolverService.getResolver().cancelRequest(msg, true);
			}
		}
	
		public void constructAnswer(){
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceEntry(this, "StateMachine: constructAnswer: entry: id="+hashCode());
			
			/** what was the initial action for this lookup */
			switch (_simpl._fsm.initAction){
			case StateMachine.FINISHED:
				// answer was filled in in rulesMatrix()
				break;
			case StateMachine.SEND_NAPTR_REQUEST:
				_simpl._fsm.fillSRVAnswer();
				break;
			case StateMachine.SEND_SRV_REQUEST:
				_simpl._fsm.fillSRVAnswer();
				break;
			case StateMachine.SEND_A_REQUEST:
				_simpl._fsm.fillAAnswer();
				break;
			default:break;
			}
			
			if (c_logger.isTraceDebugEnabled()){
				for (ListIterator li = _simpl._answerList.listIterator(); li.hasNext();){
					SIPUri look = (SIPUri)li.next();
					c_logger.traceDebug("StateMachine: completeAnswer: SIPUri number  " + (li.nextIndex() - 1));
					c_logger.traceDebug("StateMachine: completeAnswer: SIPUri " + look.getScheme() + ":" + look.getUserInfo() 
							     + "@" + look.getHost() + ":" + look.getPort() + ";" + look.getTransport());
				}
			}
			
			if (c_logger.isTraceEntryExitEnabled())
				c_logger.traceExit(this, "StateMachine: completeAnswer: exit: id="+hashCode());
		}
		
		private boolean haveSRVResponses(){
			boolean bool = false;
			if (_SRVResponses != null && !_SRVResponses[0].isEmpty()){
				bool = true;
			}
			return bool;
		}
		
		/**
		 * add uri to the answer list, add ibmttl custom parameter if needed
		 * 
		 * @param uri
		 * @param uriList
		 * @param record
		 */
		private void addUriToList(SIPUri uri, List uriList, ResourceRecord record){
			if (SipResolverService.isAddTTL()){
				StringBuilder builder = new StringBuilder(uri.getAdditionalParms());
				builder.append(";");
				builder.append(IBM_TTL_PARAM);
				builder.append("=");
				builder.append(record.getTTL());
				builder.append("_");
				if (usePreciseSystemTimer) {
					// Convert nanoseconds to milliseconds
					builder.append(System.nanoTime() / 1000000L);
				}
				else {
					builder.append(System.currentTimeMillis());
				}
				
				uri.setAdditionalParms(builder.toString());
			}
			
			uriList.add(uri);
		}
	}/** end StateMachine class */
	
}/** end of SipURILookUpImpl class */
