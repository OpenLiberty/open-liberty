/*******************************************************************************
 * Copyright (c) 2003,2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.properties;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

/**
 * Class that collect all properties for the Stack
 */
public class StackProperties {
	
	/**
	 * Class Logger. 
	 */
	private static final transient LogMgr c_logger = Log
			.get(StackProperties.class);
	
	/**
	* cache constants maybe overridden by some properties file
	* in tWas="IP_CACHE_INIT_SIZE"
	*/
	public static final String IP_CACHE_INIT_SIZE = "ipCacheInitSize";
	public static final  int IP_CACHE_INIT_SIZE_DEFAULT = 100;	
	
	/**
	 * in tWas="IP_CACHE_MAX_SIZE"
	 */
	public static final String IP_CACHE_MAX_SIZE = "ipCacheMaxSize";
	public static final int IP_CACHE_MAX_SIZE_DEFAULT = 1000;

	/** 
	 * number of threads to be run in the jain stack 
	 * in tWas="javax.sip.TransactionStack.dispachingThreadNumber"
	 */
	public final static String AGENT_KEY_DISPACHING_THREAD_NUMBER = "transactionStackDispachingThreadNumber";
	public final static int AGENT_KEY_DISPACHING_THREAD_NUMBER_DEFAULT = 1;
	
	/** 
	 * Print incoming messages to system-out 
	 * in tWas="javax.sip.trace.msg.in"
	 */
	public final static String TRACE_IN_MESSAGES = "traceMsgIn";
	public final static boolean TRACE_IN_MESSAGES_DEFAULT = false; 
	
	/** 
	 * Print outbound messages to system-out  
	 * in tWas="javax.sip.trace.msg.out"
	 */
	public final static String TRACE_OUT_MESSAGES = "traceMsgOut";
	public final static boolean TRACE_OUT_MESSAGES_DEFAULT = false;

	/** 
	 * hide message content in logs
	 */
	public static final String HIDE_MESSAGE_BODY = "hideMessageBody";
	public final static boolean HIDE_MESSAGE_BODY_DEFAULT = false;
	
	/** comma-separated list of header fields that should hide the value in the log */
	public static final String HIDE_MESSAGE_HEADERS = "hideMessageHeaders";
	private static final String[] HIDE_MESSAGE_HEADERS_DEFAULT = {"Authorization","Proxy-Authorization"};
	public static final String HIDE_MESSAGE_HEADERS_EMPTY_VALUE = "None";

	/** hide the request URI */
	public static final String HIDE_REQUEST_URI = "hideMessageReqUri";
	public static final boolean HIDE_REQUEST_URI_DEFAULT = false;
	
	/**
	 * number of dispatch threads. default is 0, meaning no dispatching (all
	 * events are executed directly from the network/timer threads).
	 * in tWas="javax.sip.dispatch.threads"
	 */
	public static final String NUMBER_OF_DISPATCH_THREADS = "dispatchThreads";
	public static final int NUMBER_OF_DISPATCH_THREADS_DEFAULT = 0;
	
	/**
	 * number of application threads. default is 0, meaning no application threads (all
	 * events are executed directly from the stack threads).
	 * in tWas="javax.sip.app.threads"
	 */
	public static final String NUMBER_OF_APPLICATION_THREADS = "appThreads";
	public static final int NUMBER_OF_APPLICATION_THREADS_DEFAULT = 0;
	
	/** should we send the 100 automatically on receiving invite */
	public final static String AUTO_100_ON_INVITE = "auto100OnInvite";
	public final static boolean AUTO_100_ON_INVITE_DEFAULT = true;
	
	/** should the stack respond with 482 automatically when receiving merged requests */
	public final static String AUTO_482_ON_MERGED_REQUESTS = "auto482OnMergedRequests";
	public final static boolean AUTO_482_ON_MERGED_REQUESTS_DEFAULT = false;
	
	/** should connection reuse ignore the alias parameter */
	public final static String FORCE_CONNECTION_REUSE = "forceConnectionReuse";
	public final static boolean FORCE_CONNECTION_REUSE_DEFAULT = true;

	/** 
	 * should only send stream requests from the specified local port 
	 * in tWas="javax.sip.strict.outbound.local.port"
	 */
	public static final String STRICT_OUTBOUND_LOCAL_PORT = "strictOutLocalPort";
	public static final boolean STRICT_OUTBOUND_LOCAL_PORT_DEFAULT = false;

	/** should accept non-standard byte sequences */
	public static final String ACCEPT_NON_UTF8_BYTES = "acceptNonUtf8Bytes";
	public static final boolean ACCEPT_NON_UTF8_BYTES_DEFAULT = false;
	
	/** 
	 * timeout in milliseconds for creating outbound connections 
	 * in tWas="javax.sip.connect.timeout"
	 */
	public final static String CONNECT_TIMEOUT = "connectTimeout";
	public static final int CONNECT_TIMEOUT_DEFAULT = 0;
	
	/** 
	 * prevent re-escaping of pre-escaped parameters 
	 * in tWas="javax.sip.detect.pre.escaped.params"
	 */
	public static final String DETECT_PRE_ESCAPED_PARAMS = "detectPreEscapedParams";
	public final static boolean DETECT_PRE_ESCAPED_PARAMS_DEFAULT = false;
	
	/**
	 * in tWas="SIPURL_CACHE_INIT_SIZE"
	 */
	public static final String SIPURL_CACHE_INIT_SIZE = "sipurlCacheInitSize";
	public static final int SIPURL_CACHE_INIT_SIZE_DEFAULT = 1000;
	
	/**
	 * in tWas="SIPURL_CACHE_MAX_SIZE"
	 */
	public static final String SIPURL_CACHE_MAX_SIZE = "sipurlCacheMaxSize";
	public static final int SIPURL_CACHE_MAX_SIZE_DEFAULT = 3000;
	
	/**
	 * number of times to try binding during initialization in case the port is taken
	 * by some other process. default is 60 times.
	 * in tWas="javax.sip.bind.retries"
	 */
	public static final String BIND_RETRIES = "bindRetries";
	public static final int BIND_RETRIES_DEFAULT = 60;
	
	/** 
	 * delay, in milliseconds, between bind retries. default is 5000 milliseconds 
	 * in tWas="javax.sip.bind.retry.delay"
	 */
	public static final String BIND_RETRY_DELAY = "bindRetryDelay";
	public static final int BIND_RETRY_DELAY_DEFAULT = 5000;
	
	/** Timer T1 - RTT Estimate */
	public static final String TIMER_T1 = "timerT1";
	public static final int TIMER_T1_DEFAULT = 500;
	
	/** Timer T2 - The maximum retransmit interval for non-INVITE requests and INVITE responses */
	public static final String TIMER_T2 = "timerT2";
	public static final int TIMER_T2_DEFAULT = 4000;
	
	/** Timer T4 - Maximum duration a message will remain in the network */
	public static final String TIMER_T4 = "timerT4";
	public static final int TIMER_T4_DEFAULT = 5000;
	
	/** Timer A - Initial INVITE request retransmit interval, for UDP only */
	public static final String TIMER_A = "timerA";
	public static final int TIMER_A_DEFAULT = TIMER_T1_DEFAULT;
	
	/** Timer B - INVITE client transaction timeout timer */
	public static final String TIMER_B = "timerB";
	public static final String TIMER_B_DEPRECATED = "javax.sip.transaction.timerb";
	public static final int TIMER_B_DEFAULT = 64*TIMER_T1_DEFAULT;
	
	/** Timer D - Wait time for INVITE response retransmits */
	public static final String TIMER_D = "timerD";
	public static final int TIMER_D_DEFAULT = 32000;
	
	/** Timer E - Initial Non-INVITE request retransmit interval, UDP only */
	public static final String TIMER_E = "timerE";
	public static final int TIMER_E_DEFAULT = TIMER_T1_DEFAULT;
	
	/** Timer F - Non-INVITE transaction timeout timer */
	public static final String TIMER_F = "timerF";
	public static final int TIMER_F_DEFAULT = 64*TIMER_T1_DEFAULT;
	
	/** Timer G - Initial INVITE response retransmit interval */
	public static final String TIMER_G = "timerG";
	public static final int TIMER_G_DEFAULT = TIMER_T1_DEFAULT;
	
	/** Timer H - Wait time for ACK receipt */
	public static final String TIMER_H = "timerH";
	public static final int TIMER_H_DEFAULT = 64*TIMER_T1_DEFAULT;
	
	/** Timer I - Wait time for ACK retransmits */
	public static final String TIMER_I = "timerI";
	public static final int TIMER_I_DEFAULT = TIMER_T4_DEFAULT;
	
	/** Timer J - Wait time for non-INVITE request retransmits */
	public static final String TIMER_J = "timerJ";
	public static final int TIMER_J_DEFAULT = 64*TIMER_T1_DEFAULT;
	
	/** Timer K - Wait time for non-INVITE response retransmits */
	public static final String TIMER_K = "timerK";
	public static final int TIMER_K_DEFAULT = TIMER_T4_DEFAULT;
	
	/**
	 * API timer for the application to respond to a non-INVITE request.
	 * Affects the non-INVITE server transaction. This timer is not defined in the RFC.
	 * It is needed for terminating the transaction in case the application never generates
	 * a final response to the request. It starts when the request arrives in the stack,
	 * and cancelled when a response is generated by the application. If no response is
	 * generated by the time it goes off, it takes care of terminating the transaction.
	 * in tWas="javax.sip.transaction.timer.non.invite.server"
	 */
	public static final String NON_INVITE_SERVER_TRANSACTION_TIMER = "transactionTimerNonInviteServer";
	// default to a little more than timer F on the other side
	public static final int NON_INVITE_SERVER_TRANSACTION_TIMER_DEFAULT = 34000;
	
	/** 
	 * timer to keep the INVITE server transaction in terminated state 
	 * in tWas="javax.sip.transaction.timer.invite.server"
	 */
	public static final String INVITE_SERVER_TRANSACTION_TIMER = "transactionTimerInviteServer";
	public static final int INVITE_SERVER_TRANSACTION_TIMER_DEFAULT = 32000;
	
	/**
	 * timer to keep the cancelled client transaction in the "proceeding"
	 * state before terminating the cancelled transaction.
	 * see RFC 3261 9.1
	 * in tWas="javax.sip.transaction.timer.cancel"
	 */
	public static final String CANCEL_TIMER = "transactionTimerCancel";
	public static final int CANCEL_TIMER_DEFAULT = 64*TIMER_T1_DEFAULT;
	
	/**
	 * Config key for logging internal queue length for monitoring queue size. 
	 * in tWas="javax.sip.trace.queue.size.mod"
	 */
	public final static String  TRACE_Q_SIZE_MODULUS = "traceQueueSizeMod";
	public static final int TRACE_Q_SIZE_MODULUS_DEFAULT = -1;
	
	/**
	 * Maximum number of messages allowed on queue. When queue reaches its limit
	 * messages will be dropped. In SIP UDP messages are retransmitted so no 
	 * session should be lost as a result of dropping some packets. We need to 
	 * protected ourself for cases where the queue gets full due to some network 
	 * failure which might result in an out of memory situation. 
	 * in tWas="javax.sip.udp.sender.queue.size"
	 */
	public final static String  MAX_UDP_QUEUE_SIZE = "udpSenderQueueSize";
	public static final int MAX_UDP_QUEUE_SIZE_DEFAULT = 500;
	
	/**
	 * Config key for setting the max queue size for the internal dispatcher
	 * above this size the queue will be considered overloaded. 
	 * in tWas="javax.sip.max.dispatch.queue.size"
	 */
	public static final String MAX_DISPATCH_Q_SIZE = "maxDispatchQueueSize";
	public static final int MAX_DISPATCH_Q_SIZE_DEFAULT = 3200;
	
	/** 
	 * interval in milliseconds for reporting performance-related statistics to SystemOut */
	public static final String TIMER_STAT_REPORT_INTERVAL = CoreProperties.TIMER_STAT_REPORT_INTERVAL;
	public static final int TIMER_STAT_REPORT_INTERVAL_DEFAULT = CoreProperties.TIMER_STAT_REPORT_INTERVAL_DEFAULT;
	
	
	/** Maximum transmission unit for outbound UDP requests. See RFC 3261-18.1.1 */
	public final static String PATH_MTU = "pathMtu";
	public final static int PATH_MTU_DEFAULT = 1500;
	
	/** When should the stack use compact headers when encoding message */
	public final static String COMPACT_HEADERS = "compactHeaders";
	public final static String COMPACT_HEADERS_DEFAULT = "MtuExceeds";
	
	/**
	 * Use a default buffer size of 2MB as it shows great performance 
	 * improvement especially in linux environment
	 * in tWas="javax.sip.udp.ReceiveBufferSize"
	 */
	public final static String UDP_RECEIVE_BUFFER_SIZE = "receiveBufferSize";
	public final static int UDP_RECEIVE_BUFFER_SIZE_DEFAULT = 2097152;
	
	/**
	 * in tWas="javax.sip.OUTBOUND_PROXY"
	 */
	public final static String OUTBOUND_PROXY =  "javax.sip.OUTBOUND_PROXY";
	public final static String OUTBOUND_PROXY_DEFAULT = "";
	
    /**
     * The default level of debug which is used for all debug prints
     * which do not have an assigned level. 
     * in tWas="javax.sip.trace.level"
     */
    public final static String AGENT_TRACE_LEVEL = "traceLevel";
    public final static int AGENT_TRACE_LEVEL_DEFAULT = 4;
    
//  Default values for SSL/TLS configuration 
    /**
     * in tWas="com.ibm.ssl.protocol"
     */
    public final static String SSL_PROTOCOL = "sslProtocol";
    public final static String SSL_PROTOCOL_DEFAULT = "SSL";
    
    /**
     * in tWas="com.ibm.ssl.contextProvider"
     */
    public final static String SSL_CONTEXT_PROVIDER = "sslContextProvider";
    public final static String SSL_CONTEXT_PROVIDER_DEFAULT = "IBMJSSE";
    
    /**
     * in tWas="com.ibm.ssl.keyStoreProvider"
     */
    public final static String SSL_KEY_STORE_PROVIDER = "sslKeyStoreProvider";
    public final static String SSL_KEY_STORE_PROVIDER_DEFAULT = "IBMJCE";
    
    /**
     * in tWas="com.ibm.ssl.trustStoreProvider"
     */
    public final static String SSL_TRUST_STORE_PROVIDER = "sslTrustStoreProvider";
    public final static String SSL_TRUST_STORE_PROVIDER_DEFAULT = "IBMJCE";
    
    /**
     * in tWas="com.ibm.ssl.clientAuthentication"
     */
    public final static String SSL_CLIENT_AUTHENTICATION = "sslClientAuthentication";
    public final static boolean SSL_CLIENT_AUTHENTICATION_DEFAULT = false;

    /**
     * in tWas="com.ibm.ssl.keyStore"
     */
    public final static String SSL_KEY_STORE = "sslKeyStore";
    public final static String SSL_KEY_STORE_DEFAULT = "/etc/DummyServerKeyFile.jks";
    
    /**
     * in tWas="com.ibm.ssl.keyStorePassword"
     */
    public final static String SSL_KEY_STORE_PASSWORD = "sslKeyStorePassword";
    public final static String SSL_KEY_STORE_PASSWORD_DEFAULT = "WebAS";
    
    /**
     * in tWas="com.ibm.ssl.keyManager"
     */
    public final static String SSL_KEY_MANAGER = "sslKeyManager";
    public final static String SSL_KEY_MANAGER_DEFAULT = "IbmX509";

    /**
     * in tWas="com.ibm.ssl.keyStoreType"
     */
    public final static String SSL_KEY_STORE_TYPE = "sslKeyStoreType";
    public final static String SSL_KEY_STORE_TYPE_DEFAULT = "JKS";
    
    /**
     * in tWas="com.ibm.ssl.trustStore"
     */
    public final static String SSL_TRUST_STORE = "sslTrustStore";
    public final static String SSL_TRUST_STORE_DEFAULT = "/etc/DummyServerTrustFile.jks";
    
    /**
     * in tWas="com.ibm.ssl.trustStorePassword"
     */
    public final static String SSL_TRUST_STORE_PASSWORD = "sslTrustStorePassword";
    public final static String SSL_TRUST_STORE_PASSWORD_DEFAULT = "WebAS";
    
    /**
     * in tWas="com.ibm.ssl.trustManager"
     */
    public final static String SSL_TRUST_MANAGER = "sslTrustManager";
    public final static String SSL_TRUST_MANAGER_DEFAULT = "IbmX509";
    
    /**
     * in tWas="com.ibm.ssl.trustStoreType"
     */
    public final static String SSL_TRUST_STORE_TYPE = "sslTrustStoreType";
    public final static String SSL_TRUST_STORE_TYPE_DEFAULT = "JKS";
    
    /**
     * in tWas="com.ibm.ssl.providerClassName"
     */
    public final static String SSL_PROVIDER_CLASS_NAME = "sslProviderClassName";
    public final static String SSL_PROVIDER_CLASS_NAME_DEFAULT =  "com.ibm.jsse.IBMJSSEProvider";    
    
	/**
	 * "Number of threads" key name in the SIP stack definitions
	 * use SIPStackConfiguration.AGENT_KEY_DISPACHING_THREAD_NUMBER
	 * in tWas="javax.sip.TransactionStack.dispachingThreadNumber"
	 */
    public static final String THREADS_NUMBER_STACK_KEY =
	   						"transactionStackDispachingThreadNumber";
//  Just make sure that the stack uses only a single dispatching thread
    //into the container. There is no performance benefit of using more
    //then one and the container implementation assumes a single 
    //calling thread from the stack. 
    public static final int THREADS_NUMBER_STACK_KEY_DEFAULT = 1;
    
	/**
	 * TODO why do we need it ?
	 * Router path key name in the SIP stack definitions
	 * in tWas="javax.sip.ROUTER_PATH"
	 */
	public static final String ROUTER_PATH_STACK_KEY = "routerPath";
	/**
	 * Default external router setting for the SIP Stack.
	 */
	public static final String DEFAULT_EXTERNAL_STACK_ROUTER = 
		"com.ibm.ws.sip.stack.transaction.transport.routers.SLSPRouter";

	//Specific logger used by the SIP Stack
	/**
	 * in tWas="javax.sip.trace.logclass"
	 */
	public static final String TRACE_LOG_CLASS = "traceLogclass"; 
	public static final String TRACE_LOG_CLASS_DEFAULT = "com.ibm.ws.sip.container.stackext.SIPStackWasLogFactoryImpl";
    
	/**
	 * in tWas="javax.sip.channelframework.ws"
	 */
	public static final String USE_CHANNEL_FRAMEWORK = "channelframeworkWs";
	public static final boolean USE_CHANNEL_FRAMEWORK_DEFAULT = true;
	
	/**
	 * in tWas="LRUStringCache.size"
	 */
	public static final String LRU_STRING_CACHE = "lruStringChacheSize";
	public static final int LRU_STRING_CACHE_DEFAULT = 1000; 
	
	/**
     * Maximum pool size, above this size returned objects will not be put
     * into the pool and would be left for garbage collection. Any value less then 
     * 1 indicates unlimited size.  
     * in tWas="javax.sip.max.object.pool.size"
     */
	public static final String OBJECT_POOL_SIZE = "maxObjectPoolSize";
	public static final int OBJECT_POOL_SIZE_DEFAULT = 5000;
	
	/** outbound - receive buffer size */
	public final static String RECEIVE_BUFFER_SIZE_SOCKET = "receiveBufferSizeSocket";
    public final static String RECEIVE_BUFFER_SIZE_SOCKET_DEFAULT =  "";  

	/** outbound - send buffer size */
	public final static String SEND_BUFFER_SIZE_SOCKET = "sendBufferSizeSocket";
    public final static String SEND_BUFFER_SIZE_SOCKET_DEFAULT =  ""; 

	/** outbound - receive buffer size of the channel */
	public final static String RECEIVE_BUFFER_SIZE_CHANNEL = "receiveBufferSizeChannel";
    public final static String RECEIVE_BUFFER_SIZE_CHANNEL_DEFAULT =  "";

    /**
     * Parameter that can change the number of pooled objects in the
     * naptrSenderPool.
     * in tWas="max.naptr.sender.pool.size"
     */
    public final static String MAX_NAPTR_SENDER_POOL_SIZE = "maxNaptrSenderPoolSize";
    public final static int MAX_NAPTR_SENDER_POOL_SIZE_DEFAULT = 5000;

	/**
	 * Parameter that can change the number of pooled objects
	 * in tWas="max.message.context.pool.size"
	 */
	public final static String MAX_MESSAGE_CONTEXT_POOL_SIZE = "maxMsgContextPoolSize";
	public final static int MAX_MESSAGE_CONTEXT_POOL_SIZE_DEFAULT = 5000;
	
	/** the time to keep a cached InetAddress entry in cache */
	public static final String DNSSERVERNAMES = CoreProperties.DNSSERVERNAMES;
	public static final String[] DNSSERVERNAMES_DEFAULT = CoreProperties.DNSSERVERNAMES_DEFAULT;
	
	/** the time to keep a cached InetAddress entry in cache */
	public static final String DNS_SERVER_AUTO_RESOLVE = CoreProperties.DNS_SERVER_AUTO_RESOLVE;
	public static final boolean DNS_SERVER_AUTO_RESOLVE_DEFAULT = CoreProperties.DNS_SERVER_AUTO_RESOLVE_DEFAULT;
	
	public static final String DNS_REQUEST_CACHE_TIMEOUT_MIN = CoreProperties.DNS_REQUEST_CACHE_TIMEOUT_MIN;
	public static final int DNS_REQUEST_CACHE_TIMEOUT_MIN_DEFAULT = CoreProperties.DNS_REQUEST_CACHE_TIMEOUT_MIN_DEFAULT;
	


	/** 
	 * the time to keep a cached InetAddress entry in cache 
	 * in tWas="javax.sip.networkaddress.cache.ttl"
	 */
	public static final String NETWORK_ADDRESS_CACHE_TTL = "networkaddressCacheTtl";
	public static final String NETWORK_ADDRESS_CACHE_TTL_DEFAULT = "";

	/**
	 * the local address to set as the Via sent-by host, or in the URI of
	 * self-generated Contact, Record-Route, and Path.
	 * applies to the Via in either standalone or cluster.
	 * applies to the Contact, Record-Route, and Path in standalone only.
	 * empty value obtains the local host automatically during initialization.
	 */
	public static final String SENT_BY_HOST = CoreProperties.SENT_BY_HOST;
	public static final String SENT_BY_HOST_DEFAULT = CoreProperties.SENT_BY_HOST_DEFAULT;

	/**
	 * Specifies the value to be added to the Call-ID after '@' 	 * 
	 * By default the Call-Id looks like the following:
	 * 		Call-ID: 35269872760098653@194.197.81.53 
	 * 
	 * If this property is set, its value will be applied in the Call-Id:
	 * 		Call-ID: 35269872760098653@$CALLID_VALUE$
	 * 
	 * empty value means not to add anything to the Call-ID header 
	 *   	Call-ID: 35269872760098653
	 */
	public static final String CALLID_VALUE = CoreProperties.CALLID_VALUE;
	public static final String CALLID_VALUE_DEFAULT = CoreProperties.CALLID_VALUE_DEFAULT;
	
	/**
	 * list of parameters that require quoting the value, that modifies, or
	 * adds to, the default list.
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParameterQuoter
	 */
	public static final String QUOTED_PARAMETERS = "sipQuotedParameters";
	public static final String[] QUOTED_PARAMETERS_DEFAULT = {};

	/** 
	 * maximum number of messages pending on the outbound queue, per connection 
	 * in tWas="max.outbound.pending.messages"
	 */
	public static final String MAX_OUTBOUND_PENDING_MESSAGES = "maxOutPendingMsgs";
	public static final int MAX_OUTBOUND_PENDING_MESSAGES_DEFAULT = 10000;

	/** 
	 * true if display names are always quoted, false if quoted only when necessary 
	 * in tWas="force.display.name.quoting"
	 */
	public static final String FORCE_DISPLAY_NAME_QUOTING = "forceDisplayNameQuoting";
	public static final boolean FORCE_DISPLAY_NAME_QUOTING_DEFAULT = false;

	/**
	 * true if the application is allowed to set a quoted display name.
	 * this flag serves misbehaving applications that insist on passing
	 * a quoted displayName string to the setDisplayName() method.
	 * in tWas="allow.display.name.quoting.by.app"
	 */
	public static final String ALLOW_DISPLAY_NAME_QUOTING_BY_APP = "allowDisplayNameQuotingByApp";
	public static final boolean ALLOW_DISPLAY_NAME_QUOTING_BY_APP_DEFAULT = false;


	/**
	 * true if Addresses are always serialized as name-addr (force <> around the URI)
	 * in tWas="force.name.addr"
	 */
	public static final String FORCE_NAME_ADDR = "forceNameAddr";
	public static final boolean FORCE_NAME_ADDR_DEFAULT = false;

	/** list of header fields that should be comma-separated */
	public static final String COMMA_SEPARATED_HEADERS = "commaSeparatedHeaders";
	public static final String[] COMMA_SEPARATED_HEADERS_DEFAULT = {};
	
	/**
	 * List of headers to be parsed as address headers (like To/From/Contact headers)
	 * separated by a comma (case-insensitive). Default value is an empty list (no headers).
	 * in tWas="headers.parsed.as.address"
	 */
	public static final String HEADERS_PARSED_AS_ADDRESS = "headersParsedAsAddress";
	public static final String HEADERS_PARSED_AS_ADDRESS_DEFAULT = "";
	
	/**
	 * true if we use MessageContext pooling mechanism debug
	 * in tWas="message.context.pooling.debug"
	 */
	public static final String MESSAGE_CONTEXT_POOLING_DEBUG = "msgContextPoolingDebug";
	public static final boolean MESSAGE_CONTEXT_POOLING_DEBUG_DEFAULT = false;
	
	/**
	 * true if we do not want to escape the hash character in tel-uri
	 * The default value is false
	 * in tWas="unescape.hash.char.in.teluri"
	 */
	public static final String UNESCAPE_HASH_CHAR_IN_TELURI = "unescapeHashCharInTeluri";
	public static final boolean UNESCAPE_HASH_CHAR_IN_TELURI_DEFAULT = false;
	
	
	/**
	 * true - if we want to send a "400 Bad Request" for wrong start line
	 * instead of dropping the message
	 * The default value is false
	 * in tWas="send.400.for.wrong.request.line"
	 */
	public static final String  SEND_400_FOR_WRONG_REQUEST_LINE = "send400ForWrongRequestLine";
	public static final boolean SEND_400_FOR_WRONG_REQUEST_LINE_DEFAULT = false;
	
	/**
	 * The number of allowed parse errors before closing a reliable connection.
	 * -1 indicates never close the reliable connection on any parse error.
	 * 0 indicates closing the reliable connection on any parse error i.e. at least 1 parse error.
	 * 1 indicates closing the reliable connection on at least 2 parse errors.
	 * In general, (n) indicates closing the reliable connection on at least (n+1) parse errors. 
	 * default is 0 times.
	 */
	public static final String NUMBER_OF_PARSE_ERRORS_ALLOWED = "numberOfParseErrorsAllowed";
	public static final int NUMBER_OF_PARSE_ERRORS_ALLOWED_DEFAULT = 0;
	
	/**
	 * An interval (in milliseconds) in which the parse errors are counted per a reliable connection.
	 * 0 and below indicates the parse errors are counted globally per the reliable connection.
	 * If the interval is out-of-time then the number of parse errors, per the reliable connection, is initialized.
	 * default is 0 milliseconds.
	 */
	public static final String TIMER_PARSE_ERRORS_INTERVAL = "parseErrorsTimerInterval";
	public static final int TIMER_PARSE_ERRORS_INTERVAL_DEFAULT = 0;
	
	/**
	 * The maximum content-length (in bytes) for a message's body.
	 * default is 65536 bytes (64 KBytes).
	 */
	public static final String MAX_CONTENT_LENGTH = "maxContentLength";
	public static final int MAX_CONTENT_LENGTH_DEFAULT = 65536; //64 Kbytes
	
	
	/**
	  * IFix for PI51393.
	  * Create connection from selected outbound instead of related ListeningPoint. 
	  */
	public static final String CREATE_CONNECTION_USE_LP_FROM_OUTBOUND = "use.listening.point.from.outbound";
	public static final boolean CREATE_CONNECTION_USE_LP_FROM_OUTBOUND_DEFAULT = false;
	
	/** fix for PI56387 
	 *  If the SIP Stack receives a header w/ an empty value, and the next header has the same key w/ values, 
	 *  we should disregard the empty header.    The default behavior in the Stack led to some confusing headers.
	 *  For example, if an incoming message contained these two headers:
	 *    "Supported:"
	 *    "Supported:  abc"
	 *  This resulted in this combined header --->   "Supported: , abc", which is wrong.
	 *  I discussed this w/ Avshalom and Lior, they both think this is not accurate.
	 */
	public final static String REMOVE_EMPTY_COMMA_SEPARATED_HEADERS = "removeEmptyCommaSeparatedHeaders";
	public final static boolean REMOVE_EMPTY_COMMA_SEPARATED_HEADERS_DEFAULT = false;

	public static final String  ENABLE_SET_OUTBOUND_INTERFACE = "enableSetOutboundIF";
	public static final boolean  ENABLE_SET_OUTBOUND_INTERFACE_DEFAULT = false;
	/**
     * Load default properties and store them in properties.
     * This is the first properties that are loaded and some properties might be overridden later
     * by the server.xml configuration
     */
    static public void loadDefaultProperties(SipPropertiesMap properties)
    {
    	if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(StackProperties.class.getName(),
					"loadDefaultProperties");
		}
    	
    	properties.setInt(IP_CACHE_INIT_SIZE, IP_CACHE_INIT_SIZE_DEFAULT,CustPropSource.DEFAULT);
    	properties.setInt(IP_CACHE_MAX_SIZE, IP_CACHE_MAX_SIZE_DEFAULT, CustPropSource.DEFAULT);
    	
    	properties.setInt(AGENT_KEY_DISPACHING_THREAD_NUMBER, AGENT_KEY_DISPACHING_THREAD_NUMBER_DEFAULT,CustPropSource.DEFAULT);
    	properties.setBoolean(TRACE_IN_MESSAGES, TRACE_IN_MESSAGES_DEFAULT, CustPropSource.DEFAULT);
    	properties.setBoolean(TRACE_OUT_MESSAGES, TRACE_OUT_MESSAGES_DEFAULT, CustPropSource.DEFAULT);
    	properties.setBoolean(HIDE_MESSAGE_BODY, HIDE_MESSAGE_BODY_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(NUMBER_OF_DISPATCH_THREADS, NUMBER_OF_DISPATCH_THREADS_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(NUMBER_OF_APPLICATION_THREADS, NUMBER_OF_APPLICATION_THREADS_DEFAULT, CustPropSource.DEFAULT);
    	properties.setBoolean(AUTO_100_ON_INVITE, AUTO_100_ON_INVITE_DEFAULT, CustPropSource.DEFAULT);
    	properties.setBoolean(AUTO_482_ON_MERGED_REQUESTS, AUTO_482_ON_MERGED_REQUESTS_DEFAULT, CustPropSource.DEFAULT);
    	properties.setBoolean(FORCE_CONNECTION_REUSE, FORCE_CONNECTION_REUSE_DEFAULT, CustPropSource.DEFAULT);
    	properties.setBoolean(STRICT_OUTBOUND_LOCAL_PORT, STRICT_OUTBOUND_LOCAL_PORT_DEFAULT, CustPropSource.DEFAULT);
    	properties.setBoolean(ACCEPT_NON_UTF8_BYTES, ACCEPT_NON_UTF8_BYTES_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(CONNECT_TIMEOUT, CONNECT_TIMEOUT_DEFAULT, CustPropSource.DEFAULT);
    	properties.setBoolean(DETECT_PRE_ESCAPED_PARAMS, DETECT_PRE_ESCAPED_PARAMS_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(SIPURL_CACHE_INIT_SIZE, SIPURL_CACHE_INIT_SIZE_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(BIND_RETRIES, BIND_RETRIES_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(BIND_RETRY_DELAY, BIND_RETRY_DELAY_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(SIPURL_CACHE_MAX_SIZE, SIPURL_CACHE_MAX_SIZE_DEFAULT, CustPropSource.DEFAULT);
    	
    	properties.setInt(TIMER_T1, TIMER_T1_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(TIMER_T2, TIMER_T2_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(TIMER_T4, TIMER_T4_DEFAULT, CustPropSource.DEFAULT);
    	
    	properties.setInt(TIMER_A, TIMER_A_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(TIMER_B, TIMER_B_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(TIMER_B_DEPRECATED, TIMER_B_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(TIMER_D, TIMER_D_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(TIMER_E, TIMER_E_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(TIMER_F, TIMER_F_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(TIMER_G, TIMER_G_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(TIMER_H, TIMER_H_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(TIMER_I, TIMER_I_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(TIMER_J, TIMER_J_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(TIMER_K, TIMER_K_DEFAULT, CustPropSource.DEFAULT);
    	
    	properties.setInt(NON_INVITE_SERVER_TRANSACTION_TIMER, NON_INVITE_SERVER_TRANSACTION_TIMER_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(INVITE_SERVER_TRANSACTION_TIMER, INVITE_SERVER_TRANSACTION_TIMER_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(CANCEL_TIMER, CANCEL_TIMER_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(TRACE_Q_SIZE_MODULUS, TRACE_Q_SIZE_MODULUS_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(MAX_UDP_QUEUE_SIZE, MAX_UDP_QUEUE_SIZE_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(MAX_DISPATCH_Q_SIZE, MAX_DISPATCH_Q_SIZE_DEFAULT, CustPropSource.DEFAULT);
    	properties.setInt(TIMER_STAT_REPORT_INTERVAL, TIMER_STAT_REPORT_INTERVAL_DEFAULT, CustPropSource.DEFAULT);
    	
    	properties.setInt(PATH_MTU, PATH_MTU_DEFAULT, CustPropSource.DEFAULT);
    	properties.setString(COMPACT_HEADERS, COMPACT_HEADERS_DEFAULT, CustPropSource.DEFAULT);

    	properties.setInt(UDP_RECEIVE_BUFFER_SIZE, UDP_RECEIVE_BUFFER_SIZE_DEFAULT, CustPropSource.DEFAULT);
    	properties.setString(OUTBOUND_PROXY, OUTBOUND_PROXY_DEFAULT, CustPropSource.DEFAULT);
    	
    	properties.setInt(AGENT_TRACE_LEVEL, AGENT_TRACE_LEVEL_DEFAULT, CustPropSource.DEFAULT);
    	
//    	 String installRoot = System.getProperty("was.install.root");
		// Yaron: SPR #DHAR642P4Y
		String installRoot = System.getProperty("user.install.root");
		properties.setString(SSL_KEY_STORE, installRoot + SSL_KEY_STORE_DEFAULT, CustPropSource.DEFAULT);
		properties.setString(SSL_TRUST_STORE, installRoot + SSL_TRUST_STORE_DEFAULT, CustPropSource.DEFAULT);
		properties.setString(SSL_PROTOCOL, SSL_PROTOCOL_DEFAULT, CustPropSource.DEFAULT);
		properties.setString(SSL_CONTEXT_PROVIDER, SSL_CONTEXT_PROVIDER_DEFAULT, CustPropSource.DEFAULT);
		properties.setString(SSL_KEY_STORE_PROVIDER, SSL_KEY_STORE_PROVIDER_DEFAULT, CustPropSource.DEFAULT);
		properties.setString(SSL_TRUST_STORE_PROVIDER, SSL_TRUST_STORE_PROVIDER_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(SSL_CLIENT_AUTHENTICATION, SSL_CLIENT_AUTHENTICATION_DEFAULT, CustPropSource.DEFAULT);
		properties.setString(SSL_KEY_STORE_PASSWORD, SSL_KEY_STORE_PASSWORD_DEFAULT, CustPropSource.DEFAULT);
		properties.setString(SSL_KEY_MANAGER, SSL_KEY_MANAGER_DEFAULT, CustPropSource.DEFAULT);
		properties.setString(SSL_KEY_STORE_TYPE, SSL_KEY_STORE_TYPE_DEFAULT, CustPropSource.DEFAULT);
		properties.setString(SSL_TRUST_STORE_PASSWORD, SSL_TRUST_STORE_PASSWORD_DEFAULT, CustPropSource.DEFAULT);
		properties.setString(SSL_TRUST_MANAGER, SSL_TRUST_MANAGER_DEFAULT, CustPropSource.DEFAULT);
		properties.setString(SSL_TRUST_STORE_TYPE, SSL_TRUST_STORE_TYPE_DEFAULT, CustPropSource.DEFAULT);
		properties.setString(SSL_PROVIDER_CLASS_NAME, SSL_PROVIDER_CLASS_NAME_DEFAULT, CustPropSource.DEFAULT);
		
		properties.setInt(THREADS_NUMBER_STACK_KEY, THREADS_NUMBER_STACK_KEY_DEFAULT, CustPropSource.DEFAULT);
		properties.setString(ROUTER_PATH_STACK_KEY, DEFAULT_EXTERNAL_STACK_ROUTER,CustPropSource.DEFAULT);
		properties.setString(TRACE_LOG_CLASS, TRACE_LOG_CLASS_DEFAULT,CustPropSource.DEFAULT);
		
		properties.setBoolean(USE_CHANNEL_FRAMEWORK, USE_CHANNEL_FRAMEWORK_DEFAULT, CustPropSource.DEFAULT);
		properties.setInt(LRU_STRING_CACHE, LRU_STRING_CACHE_DEFAULT, CustPropSource.DEFAULT);
		properties.setInt(OBJECT_POOL_SIZE, OBJECT_POOL_SIZE_DEFAULT, CustPropSource.DEFAULT);
		
		properties.setString(RECEIVE_BUFFER_SIZE_CHANNEL, RECEIVE_BUFFER_SIZE_CHANNEL_DEFAULT,CustPropSource.DEFAULT);
		properties.setString(SEND_BUFFER_SIZE_SOCKET, SEND_BUFFER_SIZE_SOCKET_DEFAULT,CustPropSource.DEFAULT);
		properties.setString(RECEIVE_BUFFER_SIZE_SOCKET, RECEIVE_BUFFER_SIZE_SOCKET_DEFAULT,CustPropSource.DEFAULT);
		properties.setInt(MAX_NAPTR_SENDER_POOL_SIZE, MAX_NAPTR_SENDER_POOL_SIZE_DEFAULT,CustPropSource.DEFAULT);
		properties.setInt(MAX_MESSAGE_CONTEXT_POOL_SIZE, MAX_MESSAGE_CONTEXT_POOL_SIZE_DEFAULT,CustPropSource.DEFAULT);
		properties.setObject(DNSSERVERNAMES, DNSSERVERNAMES_DEFAULT,CustPropSource.DEFAULT);
		properties.setString(NETWORK_ADDRESS_CACHE_TTL, NETWORK_ADDRESS_CACHE_TTL_DEFAULT, CustPropSource.DEFAULT);
		properties.setObject(HIDE_MESSAGE_HEADERS, HIDE_MESSAGE_HEADERS_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(HIDE_REQUEST_URI, HIDE_REQUEST_URI_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(DNS_SERVER_AUTO_RESOLVE, DNS_SERVER_AUTO_RESOLVE_DEFAULT, CustPropSource.DEFAULT);
		properties.setInt(DNS_REQUEST_CACHE_TIMEOUT_MIN, DNS_REQUEST_CACHE_TIMEOUT_MIN_DEFAULT, CustPropSource.DEFAULT);
		properties.setInt(MAX_OUTBOUND_PENDING_MESSAGES, MAX_OUTBOUND_PENDING_MESSAGES_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(FORCE_DISPLAY_NAME_QUOTING, FORCE_DISPLAY_NAME_QUOTING_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(ALLOW_DISPLAY_NAME_QUOTING_BY_APP, ALLOW_DISPLAY_NAME_QUOTING_BY_APP_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(FORCE_NAME_ADDR, FORCE_NAME_ADDR_DEFAULT, CustPropSource.DEFAULT);
		properties.setObject(COMMA_SEPARATED_HEADERS, COMMA_SEPARATED_HEADERS_DEFAULT, CustPropSource.DEFAULT);
		properties.setObject(QUOTED_PARAMETERS, QUOTED_PARAMETERS_DEFAULT, CustPropSource.DEFAULT);
		properties.setString(HEADERS_PARSED_AS_ADDRESS, HEADERS_PARSED_AS_ADDRESS_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(MESSAGE_CONTEXT_POOLING_DEBUG, MESSAGE_CONTEXT_POOLING_DEBUG_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(UNESCAPE_HASH_CHAR_IN_TELURI, UNESCAPE_HASH_CHAR_IN_TELURI_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(SEND_400_FOR_WRONG_REQUEST_LINE, SEND_400_FOR_WRONG_REQUEST_LINE_DEFAULT, CustPropSource.DEFAULT);
		properties.setInt(NUMBER_OF_PARSE_ERRORS_ALLOWED, NUMBER_OF_PARSE_ERRORS_ALLOWED_DEFAULT, CustPropSource.DEFAULT);
		properties.setInt(TIMER_PARSE_ERRORS_INTERVAL, TIMER_PARSE_ERRORS_INTERVAL_DEFAULT, CustPropSource.DEFAULT);
		properties.setInt(MAX_CONTENT_LENGTH, MAX_CONTENT_LENGTH_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(CREATE_CONNECTION_USE_LP_FROM_OUTBOUND, CREATE_CONNECTION_USE_LP_FROM_OUTBOUND_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(REMOVE_EMPTY_COMMA_SEPARATED_HEADERS, REMOVE_EMPTY_COMMA_SEPARATED_HEADERS_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(ENABLE_SET_OUTBOUND_INTERFACE, ENABLE_SET_OUTBOUND_INTERFACE_DEFAULT, CustPropSource.DEFAULT);
    }
}
