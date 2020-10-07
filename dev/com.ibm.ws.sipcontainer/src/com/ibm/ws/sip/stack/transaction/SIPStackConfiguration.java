/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transaction;

import java.security.Security;
import java.util.HashSet;
import java.util.StringTokenizer;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.jain.protocol.ip.sip.address.AddressFactoryImpl;
import com.ibm.ws.sip.properties.StackProperties;
import com.ibm.ws.sip.stack.transaction.util.ApplicationProperties;
import com.ibm.ws.sip.stack.transaction.util.SIPStackUtil;

public class SIPStackConfiguration
{
	/** class Logger */
	private static final LogMgr s_logger = Log.get(SIPStackConfiguration.class);

	/** 
	 * on how many threads should we dispatch the events 
	 **/
	private int m_uaDispachingThreads;
	
	/**
	 * log all out messages
	 */
	public boolean m_logOutMessages;  
	
	/**
	 * log all in messages
	 */
	public boolean m_logInMessages;

	/** hide message content in logs */
	private boolean m_hideMessageContent;

	/** set of header fields that should hide the value in the log */
	private final HashSet<String> m_hideHeaders = new HashSet<String>();

	/** hide the request URI in logs */
	private boolean m_hideRequestUri;

	/**
	 * number of dispatch threads. default is 0, meaning no dispatching (all
	 * events are executed directly from the network/timer threads).
	 */
	private int m_numberOfDispatchThreads;

	/**
	 * number of application threads. default is 0, meaning no application threads (all
	 * events are executed directly from the stack threads).
	 */
	private int m_numberOfApplicationThreads;

    /**
	 * should we send the 100 automatically on receiving invite
	 */
	public boolean m_autoSendProvisionResponseOnInvite = StackProperties.AUTO_100_ON_INVITE_DEFAULT;
	
	/**
	 * set to true if the stack should act as a UAS core
	 * when it receives merged requests per rfc3261 8.2.2.2
	 */
	public boolean m_auto482ResponseToMergedRequests;
	
	/**
	 * @see #forceConnectionReuse()
	 */
	private boolean m_forceConnectionReuse;

	/**
	 * @see #strictOutboundLocalPort()
	 */
	private boolean m_strictOutboundLocalPort;
	
	/** timeout in milliseconds for creating outbound connections */
	private int m_connectTimeout;

	/** prevent re-escaping of pre-escaped parameters */
	private boolean m_detectPreEscapedParams;
	
	/**
	 * number of times to try binding during initialization in case the port is taken
	 * by some other process
	 */
	private int m_bindRetries;
	
	/** delay, in milliseconds, between bind retries */
	private int m_bindRetryDelay;

	/** Timer T1 - RTT Estimate */
	private int m_timerT1;

	/** Timer T2 - The maximum retransmit interval for non-INVITE requests and INVITE responses */
	private int m_timerT2;

	/** Timer T4 - Maximum duration a message will remain in the network */
	private int m_timerT4;

	/** Timer A - Initial INVITE request retransmit interval, for UDP only */
	private int m_timerA;

	/** Timer B - INVITE client transaction timeout timer */
	private int m_timerB;

	/** Timer D - Wait time for INVITE response retransmits */
	private int m_timerD;

	/** Timer E - Initial Non-INVITE request retransmit interval, UDP only */
	private int m_timerE;

	/** Timer F - Non-INVITE transaction timeout timer */
	private int m_timerF;

	/** Timer G - Initial INVITE response retransmit interval */
	private int m_timerG;

	/** Timer H - Wait time for ACK receipt */
	private int m_timerH;

	/** Timer I - Wait time for ACK retransmits */
	private int m_timerI;

	/** Timer J - Wait time for non-INVITE request retransmits */
	private int m_timerJ;

	/** Timer K - Wait time for non-INVITE response retransmits */
	private int m_timerK;
	
	/** API timer for the application to respond to a non-INVITE request */
	private int m_nonInviteServerTransactionTimer;

	/**
	 * timer to keep the INVITE server transaction in terminated state.
	 * this is needed when returning a 2xx response without returning a provisional response.
	 * if the response is lost, the client might retransmit the INVITE.
	 * in this scenario the retransmitted INVITE might create a new server transaction.
	 * to avoid this, we keep the terminated server transaction around for a while,
	 * absorbing INVITE retransmissions.
	 * This timer is started when the transaction transitions to the "terminated" state.
	 * When it goes off, the transaction is removed.
	 */
	private int m_inviteServerTransactionTimer;
	
	/**
	 * timer to keep the cancelled client transaction in the "proceeding"
	 * state before terminating the cancelled transaction
	 */
	private int m_cancelTimer;

	/** the time to keep a cached InetAddress entry in cache */
	private int m_networkAddressCacheTtl;

	/**
	 * the local address to set as the Via sent-by host, or in the URI of
	 * self-generated Contact, Record-Route, and Path.
	 * applies to the Via in either standalone or cluster.
	 * applies to the Contact, Record-Route, and Path in standalone only.
	 * null value obtains the local host automatically during initialization.
	 */
	private String m_sentByHost;
	
	/**
	 * Specifies the value to be added to the Call-ID after '@' 
	 */	
	private String m_callIdValue;

	/** maximum number of messages pending on the outbound queue, per connection */
	private int m_maxOutboundPendingMessages;

	/** true if display names are always quoted, false if quoted only when necessary */
	private boolean m_forceDisplayNameQuoting;

	/**
	 * true if the application is allowed to set a quoted display name.
	 * this flag serves misbehaving applications that insist on passing
	 * a quoted displayName string to the setDisplayName() method.
	 */
	private boolean m_allowDisplayNameQuotingByApp;
	
	/**
	 * true if Addresses are always serialized as name-addr (force <> around the URI)
	 */
	private boolean m_forceNameAddr;

	/** list of header fields that should be comma-separated */
	private String[] m_commaSeparatedHeaders;
	
	/**
	 * true if MessageContext pooling debug enabled
	 */
	private boolean m_messageContextPoolingDebug;
	
	/**
	 * Set of header names to be parsed as address headers (like To/From/Contact).
	 */
	private HashSet<String> m_addressHeaders = new HashSet<String>();
	
	/** remove empty comma-separated headers */
	private boolean m_removeEmptyCommaSeparatedHeaders;


	/** constructor */
	SIPStackConfiguration()
	{
		init();
	}
	
	/** init configuration parameters */
	private void init()
	{
		m_uaDispachingThreads = ApplicationProperties.getProperties().
			getInt(StackProperties.AGENT_KEY_DISPACHING_THREAD_NUMBER );
		
		//log properties
		m_logInMessages = ApplicationProperties.getProperties().
			getBoolean( StackProperties.TRACE_IN_MESSAGES);
		m_logOutMessages = ApplicationProperties.getProperties().
			getBoolean( StackProperties.TRACE_OUT_MESSAGES );
		m_hideMessageContent = ApplicationProperties.getProperties().
			getBoolean(StackProperties.HIDE_MESSAGE_BODY);
		
		String[] hiddenHeaders = (String[])ApplicationProperties.getProperties().getObject(StackProperties.HIDE_MESSAGE_HEADERS);
		for (String header : hiddenHeaders) {
			m_hideHeaders.add(header);
		}
		
		m_hideRequestUri = ApplicationProperties.getProperties().
			getBoolean(StackProperties.HIDE_REQUEST_URI);
		
		m_numberOfDispatchThreads = ApplicationProperties.getProperties().
			getInt(StackProperties.NUMBER_OF_DISPATCH_THREADS);
		m_numberOfApplicationThreads = ApplicationProperties.getProperties().
			getInt(StackProperties.NUMBER_OF_APPLICATION_THREADS);
		
		m_autoSendProvisionResponseOnInvite = ApplicationProperties.
			getProperties().getBoolean( StackProperties.AUTO_100_ON_INVITE );
		m_auto482ResponseToMergedRequests = ApplicationProperties.
			getProperties().getBoolean(StackProperties.AUTO_482_ON_MERGED_REQUESTS);
		m_forceConnectionReuse = ApplicationProperties.
			getProperties().getBoolean(StackProperties.FORCE_CONNECTION_REUSE);
		m_strictOutboundLocalPort = ApplicationProperties.
			getProperties().getBoolean(StackProperties.STRICT_OUTBOUND_LOCAL_PORT);
		m_connectTimeout = ApplicationProperties.getProperties().
			getInt(StackProperties.CONNECT_TIMEOUT);

		m_detectPreEscapedParams = ApplicationProperties.
			getProperties().getBoolean(StackProperties.DETECT_PRE_ESCAPED_PARAMS);
		
		AddressFactoryImpl.s_sipurl_cache_init_size = ApplicationProperties.
			getProperties().getInt(StackProperties.SIPURL_CACHE_INIT_SIZE);

		AddressFactoryImpl.s_sipurl_cache_max_size = ApplicationProperties.
			getProperties().getInt(StackProperties.SIPURL_CACHE_MAX_SIZE);

		SIPStackUtil.s_ipCacheInitSize = ApplicationProperties.getProperties().
			getInt(StackProperties.IP_CACHE_INIT_SIZE);
		
		SIPStackUtil.s_ipCacheMaxSize = ApplicationProperties.getProperties().
			getInt(StackProperties.IP_CACHE_MAX_SIZE);

		// Note: It's never used. The value is taken from the metatype property.
		m_bindRetries = ApplicationProperties.getProperties().
			getInt(StackProperties.BIND_RETRIES);

		// Note: It's never used. The value is taken from the metatype property.
		m_bindRetryDelay = ApplicationProperties.getProperties()
			.getInt(StackProperties.BIND_RETRY_DELAY);

		m_timerT1 = ApplicationProperties.getProperties().getDuration(StackProperties.TIMER_T1);
		if (m_timerT1 != StackProperties.TIMER_T1_DEFAULT) {
			if (m_timerT1 == -1) {
				m_timerT1 = StackProperties.TIMER_T1_DEFAULT;
			}
			if (s_logger.isInfoEnabled()) {
				Object[] params = { "T1", Integer.valueOf(m_timerT1) };
				s_logger.info("info.sip.stack.timer", Situation.SITUATION_CONFIGURE, params);
			}
		}
		m_timerT2 = ApplicationProperties.getProperties().getDuration(StackProperties.TIMER_T2);
		if (m_timerT2 != StackProperties.TIMER_T2_DEFAULT) {
			if (m_timerT2 == -1) {
				m_timerT2 = StackProperties.TIMER_T2_DEFAULT;
			}
			if (s_logger.isInfoEnabled()) {
				Object[] params = { "T2", Integer.valueOf(m_timerT2) };
				s_logger.info("info.sip.stack.timer", Situation.SITUATION_CONFIGURE, params);
			}
		}
		m_timerT4 = ApplicationProperties.getProperties().getDuration(StackProperties.TIMER_T4);
		if (m_timerT4 != StackProperties.TIMER_T4_DEFAULT) {
			if (m_timerT4 == -1) {
				m_timerT4 = StackProperties.TIMER_T4_DEFAULT;
			}
			if (s_logger.isInfoEnabled()) {
				Object[] params = { "T4", Integer.valueOf(m_timerT4) };
				s_logger.info("info.sip.stack.timer", Situation.SITUATION_CONFIGURE, params);
			}
		}
		m_timerA = ApplicationProperties.getProperties().getDuration(StackProperties.TIMER_A);
		if (m_timerA != StackProperties.TIMER_A_DEFAULT) {
			if (m_timerA == -1) {
				m_timerA = StackProperties.TIMER_A_DEFAULT;
			}
			if (s_logger.isInfoEnabled()) {
				Object[] params = { "Timer A", Integer.valueOf(m_timerA) };
				s_logger.info("info.sip.stack.timer", Situation.SITUATION_CONFIGURE, params);
			}
		}
		m_timerB = ApplicationProperties.getProperties().getDuration(StackProperties.TIMER_B);
		if (m_timerB == -1) {
			m_timerB = StackProperties.TIMER_B_DEFAULT;
		}
		if (m_timerB == StackProperties.TIMER_B_DEFAULT) {
			m_timerB = ApplicationProperties.getProperties().getInt(StackProperties.TIMER_B_DEPRECATED);
			if (m_timerB != StackProperties.TIMER_B_DEFAULT) {
				if (s_logger.isWarnEnabled()) {
					s_logger.warn("Configuration property is deprecated. Use [" + StackProperties.TIMER_B
					+ "] instead of [" + StackProperties.TIMER_B_DEPRECATED + ']', null);
				}
			}
		}else{
			if (s_logger.isInfoEnabled()) {
				Object[] params = { "Timer B", Integer.valueOf(m_timerB) };
				s_logger.info("info.sip.stack.timer", Situation.SITUATION_CONFIGURE, params);
			}
		}
		m_timerD = ApplicationProperties.getProperties().getDuration(StackProperties.TIMER_D);
		if (m_timerD != StackProperties.TIMER_D_DEFAULT) {
			if (m_timerD == -1) {
				m_timerD = StackProperties.TIMER_D_DEFAULT;
			}
			if (s_logger.isInfoEnabled()) {
				Object[] params = { "Timer D", Integer.valueOf(m_timerD) };
				s_logger.info("info.sip.stack.timer", Situation.SITUATION_CONFIGURE, params);
			}
		}
		m_timerE = ApplicationProperties.getProperties().getDuration(StackProperties.TIMER_E);
		if (m_timerE != StackProperties.TIMER_E_DEFAULT) {
			if (m_timerE == -1) {
				m_timerE = StackProperties.TIMER_E_DEFAULT;
			}
			if (s_logger.isInfoEnabled()) {
				Object[] params = { "Timer E", Integer.valueOf(m_timerE) };
				s_logger.info("info.sip.stack.timer", Situation.SITUATION_CONFIGURE, params);
			}
		}
		m_timerF = ApplicationProperties.getProperties().getDuration(StackProperties.TIMER_F);
		if (m_timerF != StackProperties.TIMER_F_DEFAULT) {
			if (m_timerF == -1) {
				m_timerF = StackProperties.TIMER_F_DEFAULT;
			}
			if (s_logger.isInfoEnabled()) {
				Object[] params = { "Timer F", Integer.valueOf(m_timerF) };
				s_logger.info("info.sip.stack.timer", Situation.SITUATION_CONFIGURE, params);
			}
		}
		m_timerG = ApplicationProperties.getProperties().getDuration(StackProperties.TIMER_G);
		if (m_timerG != StackProperties.TIMER_G_DEFAULT) {
			if (m_timerG == -1) {
				m_timerG = StackProperties.TIMER_G_DEFAULT;
			}
			if (s_logger.isInfoEnabled()) {
				Object[] params = { "Timer G", Integer.valueOf(m_timerG) };
				s_logger.info("info.sip.stack.timer", Situation.SITUATION_CONFIGURE, params);
			}
		}
		m_timerH = ApplicationProperties.getProperties().getDuration(StackProperties.TIMER_H);
		if (m_timerH != StackProperties.TIMER_H_DEFAULT) {
			if (m_timerH == -1) {
				m_timerH = StackProperties.TIMER_H_DEFAULT;
			}
			if (s_logger.isInfoEnabled()) {
				Object[] params = { "Timer H", Integer.valueOf(m_timerH) };
				s_logger.info("info.sip.stack.timer", Situation.SITUATION_CONFIGURE, params);
			}
		}
		m_timerI = ApplicationProperties.getProperties().getDuration(StackProperties.TIMER_I);
		if (m_timerI != StackProperties.TIMER_I_DEFAULT) {
			if (m_timerI == -1) {
				m_timerI = StackProperties.TIMER_I_DEFAULT;
			}
			if (s_logger.isInfoEnabled()) {
				Object[] params = { "Timer I", Integer.valueOf(m_timerI) };
				s_logger.info("info.sip.stack.timer", Situation.SITUATION_CONFIGURE, params);
			}
		}
		m_timerJ = ApplicationProperties.getProperties().getDuration(StackProperties.TIMER_J);
		if (m_timerJ != StackProperties.TIMER_J_DEFAULT) {
			if (m_timerJ == -1) {
				m_timerJ = StackProperties.TIMER_J_DEFAULT;
			}
			if (s_logger.isInfoEnabled()) {
				Object[] params = { "Timer J", Integer.valueOf(m_timerJ) };
				s_logger.info("info.sip.stack.timer", Situation.SITUATION_CONFIGURE, params);
			}
		}
		m_timerK = ApplicationProperties.getProperties().getDuration(StackProperties.TIMER_K);
		if (m_timerK != StackProperties.TIMER_K_DEFAULT) {
			if (m_timerK == -1) {
				m_timerK = StackProperties.TIMER_K_DEFAULT;
			}
			if (s_logger.isInfoEnabled()) {
				Object[] params = { "Timer K", Integer.valueOf(m_timerK) };
				s_logger.info("info.sip.stack.timer", Situation.SITUATION_CONFIGURE, params);
			}
		}
		
		m_nonInviteServerTransactionTimer = ApplicationProperties.getProperties()
			.getInt(StackProperties.NON_INVITE_SERVER_TRANSACTION_TIMER);
		if (m_nonInviteServerTransactionTimer != StackProperties.NON_INVITE_SERVER_TRANSACTION_TIMER_DEFAULT) {
			if (s_logger.isInfoEnabled()) {
				Object[] params = { "NonInviteServerTransaction", Integer.valueOf(m_nonInviteServerTransactionTimer) };
				s_logger.info("info.sip.stack.timer", Situation.SITUATION_CONFIGURE, params);
			}
		}
		m_inviteServerTransactionTimer = ApplicationProperties.getProperties().getInt(StackProperties.INVITE_SERVER_TRANSACTION_TIMER);
		if (m_inviteServerTransactionTimer != StackProperties.INVITE_SERVER_TRANSACTION_TIMER_DEFAULT) {
			if (s_logger.isInfoEnabled()) {
				Object[] params = { "InviteServerTransaction", Integer.valueOf(m_inviteServerTransactionTimer) };
				s_logger.info("info.sip.stack.timer", Situation.SITUATION_CONFIGURE, params);
			}
		}
		m_cancelTimer = ApplicationProperties.getProperties().getInt(StackProperties.CANCEL_TIMER);
		if (m_cancelTimer  != StackProperties.CANCEL_TIMER_DEFAULT) {
			if (s_logger.isInfoEnabled()) {
				Object[] params = { "CancelTimer", Integer.valueOf(m_cancelTimer) };
				s_logger.info("info.sip.stack.timer", Situation.SITUATION_CONFIGURE, params);
			}
		}

		// read from config the duration to cache DNS lookups
		String networkAddressCacheTtl = ApplicationProperties.getProperties().getString(StackProperties.NETWORK_ADDRESS_CACHE_TTL);
		if (networkAddressCacheTtl == null || networkAddressCacheTtl.length() == 0) {
			// if not configured specifically, use the JRE's setting.
			networkAddressCacheTtl = Security.getProperty("networkaddress.cache.ttl");
			if (networkAddressCacheTtl == null) {
				networkAddressCacheTtl = "-1";
			}
		}
		try {
			m_networkAddressCacheTtl = Integer.parseInt(networkAddressCacheTtl);
		}
		catch (NumberFormatException e) {
			m_networkAddressCacheTtl = -1;
		}
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug(this, "init", "InetAddress cache TTL: " + m_networkAddressCacheTtl);
		}

		m_sentByHost = ApplicationProperties.getProperties().getString(StackProperties.SENT_BY_HOST);
		if (m_sentByHost != null && m_sentByHost.trim().length() == 0) {
			m_sentByHost = null;
		}
		else if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug(this, "init", "sent-by host ["
				+ m_sentByHost + ']');
		}
		
		m_callIdValue = ApplicationProperties.getProperties().getString(StackProperties.CALLID_VALUE);
		if (m_callIdValue != null && m_callIdValue.trim().length() == 0) {
			m_callIdValue = null;
		}
		if (m_callIdValue != null) {
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug(this, "init", "callId value ["
						+ m_callIdValue + ']');
			}
		}
		m_maxOutboundPendingMessages = ApplicationProperties.getProperties().getInt(StackProperties.MAX_OUTBOUND_PENDING_MESSAGES);

		m_forceDisplayNameQuoting = ApplicationProperties.getProperties().getBoolean(StackProperties.FORCE_DISPLAY_NAME_QUOTING);
		m_allowDisplayNameQuotingByApp = ApplicationProperties.getProperties().getBoolean(StackProperties.ALLOW_DISPLAY_NAME_QUOTING_BY_APP);
		
		m_forceNameAddr = ApplicationProperties.getProperties().getBoolean(StackProperties.FORCE_NAME_ADDR);

		m_commaSeparatedHeaders = (String[])(ApplicationProperties.getProperties().getObject(StackProperties.COMMA_SEPARATED_HEADERS));
		
		m_messageContextPoolingDebug = ApplicationProperties.getProperties().getBoolean(StackProperties.MESSAGE_CONTEXT_POOLING_DEBUG);
		
		//Init the map of headers that will be parsed as address headers.
		String addressHeaders = ApplicationProperties.getProperties().getString(StackProperties.HEADERS_PARSED_AS_ADDRESS);
		if (!StackProperties.HEADERS_PARSED_AS_ADDRESS_DEFAULT.equals(addressHeaders)) {
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug(this, "init", "addressHeaders=" + addressHeaders);
			}
			StringTokenizer tokenizer = new StringTokenizer(addressHeaders, ",");
			while (tokenizer.hasMoreElements()) {
				//This custom property is case-insensitive
				String header = tokenizer.nextToken().trim().toLowerCase();
				m_addressHeaders.add(header);
			}
		}
		
		m_removeEmptyCommaSeparatedHeaders = ApplicationProperties.getProperties().getBoolean(StackProperties.REMOVE_EMPTY_COMMA_SEPARATED_HEADERS);
	}
	
	/** number of UA dispatching threads */
	public int getUaDispachingThreads()
	{
		return m_uaDispachingThreads;
	}
	
	/**
	 * @return true for reusing the inbound connections for sending outbound requests,
	 * irrespectively of the alias parameter in the Via header.
	 * false for creating a new outbound connection for every outbound request.
	 * this is the standard behavior.
	 * default is true. change using custom property javax.sip.force.connection.reuse=false.
	 */
	public boolean forceConnectionReuse()
	{
		return m_forceConnectionReuse;
	}
	
	/**
	 * only applies to sending out requests over stream transports.
	 * forces sending a request from the SipProvider instance that is selected
	 * by the application calling SipProvider.sendRequest().
	 * @return true if the stack should only send out the request from the
	 *  selected SipProvider. false if the stack may send the request from
	 *  any other SipProvider. if this is set to true, and there is no existing
	 *  connection from the selected SipProvider, a new outbound connection
	 *  will be created.
	 */
	public boolean strictOutboundLocalPort()
	{
		return m_strictOutboundLocalPort;
	}
	
	/**
	 * should we send 100 on receiving invite 
	 * @return boolean
	 */
	public boolean isAuto100OnInvite()
	{
		return m_autoSendProvisionResponseOnInvite;
	}
	
	/**
	 * @return true if the stack should act as a UAS core
	 * when it receives merged requests per rfc3261 8.2.2.2
	 */
	public boolean isAuto482ResponseToMergedRequests() {
		return m_auto482ResponseToMergedRequests;
	}
	
	/**
 	 * @return boolean - true if we should trace in messages
	 */
	public boolean isTraceInMsg()
	{
		return m_logInMessages;
	}


	/**
	 * @return boolean -  - true if we should trace out messages
	 */
	public boolean isTraceOutMsg()
	{
		return m_logOutMessages;
	}
	
	/**
	 * @return true if hiding message content in logs
	 */
	public boolean hideMessageContent() {
		return m_hideMessageContent;
	}

	/**
	 * @return set of header fields that should hide the value in the log
	 */
	public HashSet<String> getHiddenHeaders() {
		return m_hideHeaders;
	}
	
	/**
	 * @return true if hiding the request URI in logs
	 */
	public boolean hideRequestUri() {
		return m_hideRequestUri;
	}

	/**
	 * @return true if any message field is hidden out from the log
	 */
	public boolean hideAnything() {
		return m_hideMessageContent || m_hideRequestUri
			|| !m_hideHeaders.isEmpty();
	}

	/**
	 * @return number of dispatch threads
	 */
	public int getNumberOfDispatchThreads() {
		return m_numberOfDispatchThreads;
	}
	
	/**
	 * @return number of application threads
	 */
	public int getNumberOfApplicationThreads() {
		return m_numberOfApplicationThreads;
	}
	
	/**
	 * @return timeout in milliseconds for creating outbound connections
	 */
	public int getConnectTimeout() {
		return m_connectTimeout;
	}
	
	/**
	 * @return true if preventing re-escaping of pre-escaped parameters
	 */
	public boolean detectPreEscapedParams() {
		return m_detectPreEscapedParams;
	}
	
	/**
	 * @return number of times to try binding during initialization in case the port is taken
	 *   by some other process
	 */
	public int getBindRetries() {
		return m_bindRetries;
	}
	
	/**
	 * @return delay, in milliseconds, between bind retries
	 */
	public int getBindRetryDelay() {
		return m_bindRetryDelay;
	}

	/** @return Timer T1 - RTT Estimate */
	public int getTimerT1() {
		return m_timerT1;
	}

	/** @return Timer T2 - The maximum retransmit interval for non-INVITE requests and INVITE responses */
	public int getTimerT2() {
		return m_timerT2;
	}

	/** @return Timer T4 - Maximum duration a message will remain in the network */
	public int getTimerT4() {
		return m_timerT4;
	}

	/** @return Timer A - Initial INVITE request retransmit interval, for UDP only */
	public int getTimerA() {
		return m_timerA;
	}

	/** @return Timer B - INVITE client transaction timeout timer */
	public int getTimerB() {
		return m_timerB;
	}

	/** @return Timer D - Wait time for INVITE response retransmits */
	public int getTimerD() {
		return m_timerD;
	}

	/** @return Timer E - Initial Non-INVITE request retransmit interval, UDP only */
	public int getTimerE() {
		return m_timerE;
	}

	/** @return Timer F - Non-INVITE transaction timeout timer */
	public int getTimerF() {
		return m_timerF;
	}

	/** @return Timer G - Initial INVITE response retransmit interval */
	public int getTimerG() {
		return m_timerG;
	}

	/** @return Timer H - Wait time for ACK receipt */
	public int getTimerH() {
		return m_timerH;
	}

	/** @return Timer I - Wait time for ACK retransmits */
	public int getTimerI() {
		return m_timerI;
	}

	/** @return Timer J - Wait time for non-INVITE request retransmits */
	public int getTimerJ() {
		return m_timerJ;
	}

	/** @return Timer K - Wait time for non-INVITE response retransmits */
	public int getTimerK() {
		return m_timerK;
	}
	
	/** @return API timer for the application to respond to a non-INVITE request */
	public int getNonInviteServerTransactionTimer() {
		return m_nonInviteServerTransactionTimer;
	}
	
	/** @return timer to keep the INVITE server transaction in terminated state */
	public int getInviteServerTransactionTimer() {
		return m_inviteServerTransactionTimer;
	}
	
	/**
	 * @return timer to keep the cancelled client transaction in the "proceeding"
	 *  state before terminating the cancelled transaction
	 */
	public int getCancelTimer() {
		return m_cancelTimer;
	}

	/**
	 * @return the time to keep a cached InetAddress entry in cache
	 */
	public int getNetworkAddressCacheTtl() {
		return m_networkAddressCacheTtl;
	}

	/**
	 * @return the local address to set as the Via sent-by host, or in the URI of
	 * self-generated Contact, Record-Route, and Path.
	 * applies to the Via in either standalone or cluster.
	 * applies to the Contact, Record-Route, and Path in standalone only.
	 * null value obtains the local host automatically during initialization.
	 */
	public String getSentByHost() {
		return m_sentByHost;
	}
	
	/**
	 * @return the value to be added after '@' in Call-Id generation
	 */
	public String getCallIdValue() {
		return m_callIdValue;
	}

	/**
	 * @return the maximum number of messages pending on the outbound queue, per connection
	 */
	public int getMaxOutboundPendingMessages() {
		return m_maxOutboundPendingMessages;
	}

	/**
	 * @return true if display names are always quoted, false if quoted only when necessary
	 */
	public boolean forceDisplayNameQuoting() {
		return m_forceDisplayNameQuoting;
	}

	/**
	 * @return true if the application is allowed to set a quoted display name
	 */
	public boolean allowDisplayNameQuotingByApp() {
		return m_allowDisplayNameQuotingByApp;
	}

	/**
	 * @return true if the application is allowed to set a quoted display name
	 */
	public boolean forceNameAddr() {
		return m_forceNameAddr;
	}

	/**
	 * @return list of header fields that should be comma-separated
	 */
	public String[] getCommaSeparatedHeaders() {
		return m_commaSeparatedHeaders.clone();
	}
	
	/**
	 * @return true if message context debug enabled
	 */
	public boolean messageContextPoolingDebug() {
		return m_messageContextPoolingDebug;
	}
	
	/**
	 * @return set of header names to be parsed as address headers (like To/From/Contact).
	 */
	public HashSet<String> getAddressHeaders() {
		return m_addressHeaders;
	}
	
	/**
	 * @return true if we should skip empty comma-separated headers.
	 */
	public boolean removeEmptyCommaSeparatedHeaders() {
		return m_removeEmptyCommaSeparatedHeaders;
	}
}
