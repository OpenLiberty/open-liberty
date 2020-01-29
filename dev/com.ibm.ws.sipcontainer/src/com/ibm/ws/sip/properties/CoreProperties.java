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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

/**
 * Class that collect all properties for the Sip Container Core
 * @author Assya Azrieli
 */
public class CoreProperties
{
	/**
	 * Class Logger. 
	 */
	private static final transient LogMgr c_logger = Log
			.get(CoreProperties.class);
	/**
	 * Maximum sipAppSessions allowed. 
	 */
	 public static final String MAX_APP_SESSIONS = "maxAppSessions";
	 public static final int MAX_APP_SESSIONS_DEFAULT = 120000;
	 
	 /**
	  * Maximum traffic allowed per averaging period.
	  */
	 public static final String MAX_MESSAGE_RATE = "maxMessageRate";
	 public static final int MAX_MESSAGE_RATE_DEFAULT  = 5000;
	 	 
	 /**
	  * Maximum response time allowed in milliseconds. 
	  * When the value is 0, it is mean that there is no limit.
	  */
	 public static final String MAX_RESPONSE_TIME = "maxResponseTime";
	 public static final int MAX_RESPONSE_TIME_DEFAULT = 0;
	 
	 /**
	  * The interval in milliseconds at which the container calculates averages
	  * and publishes statistics to PMI.
	  * in tWas="com.ibm.pmi.updateTempo"
	  */ 
	 public static final String STAT_UPDATE_RANGE = "pmiUpdateRange";
	 public static final int STAT_UPDATE_RANGE_DEFAULT = 10000;
	 
	 /**
	  * The time period in milliseconds over which averages are calculated. 
	  * in tWas="com.ibm.pmi.updatePeriod"
	  */
	 public static final String STAT_AVERAGE_PERIOD = "pmiUpdatePeriod"; 
	 public static final int STAT_AVERAGE_PERIOD_DEFAULT = 1000;
	 
	 /**
	  * The time period in milliseconds over which the timer, which is responsible 
	  * for updating LoadMgr with new server Weight, will be executed
	  * in tWas="com.ibm.load.updateWeightPeriod"
	  */
	 public static final String LOAD_UPDATE_PERIOD = "loadUpdateWeightPeriod";
	 public static final int LOAD_UPDATE_PERIOD_DEFAULT = 1000;
	 	
	/**
	 * Defines the maximum size of the queue in the each DispatcherWorkingThread
	 */
	public static final String MAX_MSG_QUEUE_SIZE = "dispatcherMessageQueueSize";
	public static final int MAX_MSG_QUEUE_SIZE_DEFAULT = 1000;
	
	/**
     * Constant that used for enabling a FIS that makes PMI count all container incoming and outgoing messages,
     * including proxy incoming messages and container automatic messages, except 100.
     */
	public static final String PMI_COUNT_ALL_MESSAGES = "pmiCountAllMessages";
	public static final boolean PMI_COUNT_ALL_MESSAGES_DEFAULT = false;

	/**
	 * Constant that enables a load monitoring used for overload protection
    */
	public static final String ENABLE_LOAD_MONITORING = "enableLoadMonitoring";
	public static final boolean ENABLE_LOAD_MONITORING_DEFAULT = true;

		 
	/**
     * Variable that identify if PMI info should be printed into the
     * trace file or specific PMI file
     * in tWas="pmi.print.to.trace"
     */
	public static final String PMI_PRINT_TO_TRACE = "pmiPrintToTrace";
	public static final boolean PMI_PRINT_TO_TRACE_DEFAULT = false;
	
	/**
     * Constant that is used for enabling a fix in which SIP container detects a situation
     * where 2 proxy responses on different branches has the same to-tag, and returns 408 instead
     * in tWas="generated.container.error.on.to.tag.duplication"
     */
	public static final String GENERATED_CONTAINER_ERROR_ON_TO_TAG_DUPLICATION = "generatedContainerErrorOnToTagDuplication";
	public static final int GENERATED_CONTAINER_ERROR_ON_TO_TAG_DUPLICATION_DEFAULT = 0;
    
    /**
     * custom property to set s_queueSizeInDispatcher
     * in tWas="thread.message.queue.size"
     */
    public static final String MSG_QUEUE_INITIAL_SIZE = "threadMessageQueueSize";
    public static final int MSG_QUEUE_INITIAL_SIZE_DEFAULT = 100;
    
    /**
     * Liberty Property: concurrent tasks - defines the number of SAS-hashed queues in the default implementation
     */
    public static final String CONCURRENT_CONTAINER_TASKS = "concurrentContainerTasks";
    public static final int CONCURRENT_CONTAINER_TASKS_DEFAULT = 15;
    
    /**
	 * This property affects on the _shouldPrintQueueState flag.
	 * When 0 (default) - no statistic will be printed.
	 * When 2 - print only in case of queue overloaded and throwing the message.
	 * When 1 - periodically this MessageDispatcher will call each thread to print it's 
	 * state.
	 * in tWas="sip.container.print.queue.statistic"
	 */
	public final static String TO_PRINT_QUEUE_STATE = "printQueueStatistic";
	public final static int TO_PRINT_QUEUE_STATE_DEFAULT = 0;
	
	/**
	 * The time period in seconds over which MessageDispatcher will call 
	 * each thread to print it's state
	 * in tWas="sip.container.print.queue.statistic.tempo"
	 */
	public final static String PRINTING_TEMPO = "printQueueStatisticTempo";
	public final static int PRINTING_TEMPO_DEFAULT = 1000;
	
	//NAPTER 
	/**
	 * Converted custom property 'SIP_RFC3263_nameserver'. 
	 * This allows a SIP URI to be resolved through DNS into the IP address, 
	 * port, and transport protocol of the next hop to contact. 
	 * SIP also uses DNS to allow a server to send a response to a backup client
	 * if the primary client has failed. 
	 * Value is a string containing exactly one or two address and port tuples, 
	 * where two tuples are separated by a space. 
	 * The tuples are either: 
	 * .d.o.t.t.e.d.D.e.c.i.m.a.l.A.d.d.r.e.s.s.@.p.o.r.t ,
	 * hostname.domain@port , 
	 * .I.P.V.6.a.d.d.r.e.s.s.@.p.o.r.t. 
	 * For example, the tuples could be: 1.2.3.4@53 or fred.raleigh.ibm.com@53 
	 * or a:b:c::d@53. 
	 * Some example Values could be: 1.2.3.4@53 or 1.2.3.4@53 ethel.raleigh.com@53
	 * ,a:b:c::d@53 9.37.241.24@53. WCCM attribute "dnsServerName" 
	 * This property should be used by sipChannel
	 */
	public static final String DNSSERVERNAMES = "dnsServers";
	public static final String[] DNSSERVERNAMES_DEFAULT = {};

	
	public static final String DNS_SERVER_AUTO_RESOLVE = "dnsAutoResolve";
	public static final boolean DNS_SERVER_AUTO_RESOLVE_DEFAULT = true;
	
	
	/**
	 * Set to N to use TCP for DNS lookups for RFC 3263 SIP URI resolution.
	 * This properties is loaded for Resolver Service component usage
	 */
	public static final String DNS_EDNS  = "dnsEdns";
	public static final String DNS_EDNS_DEFAULT = "Y";
	
	/**
	 * This properties is loaded for Resolver Service component usage.
	 * Possible values: 512 - 32767
	 */
	public static final String DNS_UDP_PAYLOAD_SIZE = "dnsUdpPayloadSize";
	public static final short DNS_UDP_PAYLOAD_SIZE_DEFAULT = 1280;

	
	/**
	 * This property sets the timeout in minutes for cached DNS queries.
	 */
	public static final String DNS_REQUEST_CACHE_TIMEOUT_MIN = "dnsRequestCacheTimeoutMin";
	public static final int DNS_REQUEST_CACHE_TIMEOUT_MIN_DEFAULT = 10;
	
	/**
	 * According to PI09754 this custom property should be set in order to determine
	 * the timeout duration in milliseconds for each domain resolver query.
	 */
	public static final String SIP_DNS_QUERY_TIMEOUT = "SIP_DNS_QUERY_TIMEOUT";
	public static final long   SIP_DNS_QUERY_TIMEOUT_DEFAULT = 31 * 1000; //31 Seconds


	/**
	 * When set to true, the result URIs from the DNS lookup will have an ibmttl parameter with the uri time to live.
	 */
	public static final String SIP_RFC3263_ADD_TTL = "addTtl";
	public static final boolean SIP_RFC3263_ADD_TTL_DEFAULT = false;
	
	/**
	 * These properties are related to the DNS failover solution
	 */
	public static final String SIP_RFC3263_DNS_FAILURE_DETECTION_SINGLE_QUERY_TIMEOUT_SEC = "dnsSingleQueryTimeoutSec";
	public static final int SIP_RFC3263_DNS_FAILURE_DETECTION_SINGLE_QUERY_TIMEOUT_SEC_DEFAULT = 5;

	public static final String SIP_RFC3263_DNS_FAILURE_DETECTION_ALLOWED_FAILURES = "dnsAllowedFailures";
	public static final int SIP_RFC3263_DNS_FAILURE_DETECTION_ALLOWED_FAILURES_DEFAULT = 5;

	public static final String SIP_RFC3263_DNS_FAILURE_DETECTION_WINDOW_SIZE_MIN = "dnsWindowSizeMin";
	public static final int SIP_RFC3263_DNS_FAILURE_DETECTION_WINDOW_SIZE_MIN_DEFAULT = 10 * 60; // 10 minutes

	public static final String SIP_RFC3263_DNS_FAILURE_DETECTION_WINDOW_SIZE_INTERVAL_SEC = "dnsWindowSizeInterval";
	public static final int SIP_RFC3263_DNS_FAILURE_DETECTION_WINDOW_SIZE_INTERVAL_SEC_DEFAULT = 10;
	
	
	/**
     * When true, an exception will be thrown when a replication is issued 
     * out side of the Container application threads
     * in tWas="check.for.replication.concurrency"
     */
    public static final String CHECK_FOR_REPLICATION_CONCURRENCY = "checkForReplicationConcurrency";
    public static final boolean CHECK_FOR_REPLICATION_CONCURRENCY_DEFAULT = true;
    
    /**
     * Property name for immediate replication mode 
     * Will perform replication on same thread issuing the command. 
	 * Used when replication transport is synchronous. 
	 * From WAS7.0+ default replication mode is END_OF_SERVICE_REPLICATION
	 * in tWas="immediate.replication"
     */
    public static final String IMMEDIATE_REPLICATION = "immediateReplication";
    public static final boolean IMMEDIATE_REPLICATION_DEFAULT = false;
    
    /**
     * Property name for EOS replication mode
     * When this mode is on, replication will be done on end of service for 
     * all replicatables changed during each service
	 * From WAS7.0+ default replication mode is END_OF_SERVICE_REPLICATION
	 * in tWas="end.of.service.replication"
     */
    public static final String END_OF_SERVICE_REPLICATION = "endOfServiceReplication";
    public static final boolean END_OF_SERVICE_REPLICATION_DEFAULT = true;
    
    /**
     *  Replication will be done on the service performing thread, 
     *  and not on replication handlers thread this mode should be used for 
     *  synchronous replication
     *  in tWas="replicate.on.serving.thread"
     */
    public static final String REPLICATE_ON_SERVING_THREAD = "replicateOnServingThread";
    public static final boolean REPLICATE_ON_SERVING_THREAD_DEFAULT = true;
    
    /**
     * Property name for on send replication mode 
     * in tWas="on.outgoing.message.replication"
     */
    public static final String ON_OUTGOING_MESSAGE_SEND_REPLICATION = "onOutgoingMessageReplication"; 
    public static final boolean ON_OUTGOING_MESSAGE_SEND_REPLICATION_DEFAULT = false;
    
    /**
     * Property name for replication on application call for proprietary sync method
     * in tWas="application.call.replication"
     */
    public static final String APPLICATION_CALL_REPLICATION = "applicationCallReplication";
    public static final boolean APPLICATION_CALL_REPLICATION_DEFAULT = false;
    
    /**
     * The interval in which the replication handler will wake to perform replication.
     * in tWas="replication.interval.ms" 
     */
    public static final String REPLICATION_INTERVAL_PROP = "replicationIntervalMs";
    public static final long REPLICATION_INTERVAL_PROP_DEFAULT= 0; 
    
    /**
     * Property name for number of replication handlers 
     * in tWas="replication.handlers.count"
     */
    public static final String REPLICATION_HANDLERS_COUNT = "replicationHandlersCount";
    public static final int REPLICATION_HANDLERS_COUNT_DEFAULT = 3; 
    
    /**
     * Property name for replication queue capacity 
     * in tWas="replication.queue.capcity"
     */
    public static final String REPLICATION_QUEUE_CAPACITY = "replicationQueueCapcity";
    public static final int REPLICATION_QUEUE_CAPACITY_DEFAULT= 10000;  
    
    //Servlet
    /**
     * Determine whether multiple virtual host support is enabled
     * in tWas="enable.multiple.vh"
     */
	 public static final String ENABLE_MULTIPLE_VH = "enableMultipleVh";
	 public static final boolean ENABLE_MULTIPLE_VH_DEFAULT = false;
	 
	 /**
	  * in tWas="use.extension.processor"
	  */
	 public static final String USE_EXTENSION_PROCESSOR = "useExtensionProcessor";
	 public static final boolean USE_EXTENSION_PROCESSOR_DEFAULT = false;
	 
	 //LoadManager
	 /**
	 * Defines the size of low water mark . By default is 50% of the stepSize.
	 * Used By LoadCounterAbs class.
	 * in tWas="low.water.mark.size.percent"
	 */
	 public final static String LOW_WATER_MARK_SIZE = "lowWaterMarkSizePercent";
	 public final static int LOW_WATER_MARK_SIZE_DEFAULT = 50;
		
	/**
	 * Defines the weight which will switch the container to the "overload" state.
	 * Meaning - when the weight will be 0 - the attribute "overloaded" will be
	 * passed to UCF. After the overload state only when the 3 weight will be 
	 * reached - the "overloaded" attribute will be removed.
	 * in tWas="weight.overload.watermark"
	 */
	public final static String WEIGHT_OVERLOAD_MARK = "weightOverloadWatermark";
	public final static int WEIGHT_OVERLOAD_MARK_DEFAULT = 3;
	
	/**
     * Constant used in configuration to enable PMI tracing.
     * in tWas="sip.container.trace.sessions.modulus"
     */
	public static final String TRACE_PMI_MODULUS = "traceSessionsModulus";
	public final static int TRACE_PMI_MODULUS_DEFAULT = -1;
	
	//Security
	/**
	 * Hold the IPs that can bypass security.
	 * should be looked like something as :
	 *	<property>
	 *		<name>com.ibm.ws.sip.security.trusted.iplist</name>
	 *		<value>121.55.44.33,               // ip
	 *             192.168.12.1/255.255.255.0, //ip/subnet
	 *             192.161.12.3/255.255.0.0,
	 *             10.10.1.1,
	 *				oakland.haifa.ibm.com      //full dns name
	 *		</value>
	 *	</property>
	 */
	public static final String IP_LIST_PROPERTY = "trustedIpList";
	public static final String IP_LIST_PROPERTY_DEFAULT = "";
	
	//Replication
	/**
	 * If this flag is true, the AppSession will not be replicated when an attribute is set.
	 * The attribute itself will not be replicated, unless the app session was already replicated
	 * for another reason.
	 * in tWas="no.mandatory.replication.when.attr.set"
	 */
	public static final String  NO_MANDATORY_REPLICATION_WHEN_ATTRIBUTE_SET = "noMandatoryReplicationWhenAttrSet";
	public static final boolean NO_MANDATORY_REPLICATION_WHEN_ATTRIBUTE_SET_DEFAULT = false;
	
	/**
	 * If this flag is true, the AppSession will not be replicated when an attribute is set.
	 * The attribute itself will not be replicated, unless the app session was already replicated
	 * for another reason.
	 * in tWas="mandatory.replication.when.http.sessions.set"
	 */
	public static final String  MANDATORY_REPLICATION_WHEN_HTTP_SESSIONS_SET = "mandatoryReplicationWhenHttpSessionsSet";
	public static final boolean MANDATORY_REPLICATION_WHEN_HTTP_SESSIONS_SET_DEFAULT = false;


	/**
	 * If this flag is true, the AppSession will be replicated also when the dialog is not confirmed
	 * or not exists
	 * in tWas="replicate.with.confirmed.dialog.only"
	 */
	public static final String REPLICATE_WITH_CONFIRMED_DIALOG_ONLY = "replicateWithConfirmedDialogOnly";
	public static final boolean REPLICATE_WITH_CONFIRMED_DIALOG_ONLY_DEFAULT = true;

	/**
     * Constant property key for enabling/disabling application composition
     * routing mode. 
     * in tWas="app.composition.enabled"
     */
	public static final String ENABLE_APP_COMPOSITION = "appCompositionEnabled";
	public static final boolean ENABLE_APP_COMPOSITION_DEFAULT = true;
	
    /**
     * Constants used for enabling disabling sequence session logger. 
     * in tWas="javax.sip.session.seq.log.level"
     */
	public static final String SIP_SESSION_SEQ_LOG_LEVEL = "sessionSeqLogLevel";
	public static final String SIP_SESSION_SEQ_LOG_LEVEL_DEFAULT = "";
	
	//Timers
	/**
	 * statistics reporting - interval, in milliseconds, for reporting statistics
	 * in tWas="javax.sip.stat.report.interval"
	 */
	public static final String TIMER_STAT_REPORT_INTERVAL = "statReportInterval";
	public static final int TIMER_STAT_REPORT_INTERVAL_DEFAULT = 0;
	
	/**
     * Used for debug environment and provide and option to disable timers
     * in tWas="enable.timers"
     */
	public static final String ENABLE_TIMERS = "enableTimers";
	public static final boolean ENABLE_TIMERS_DEFAULT = true;
	
	//Core
    /**
     * TODO ????
     * This flag is used by LWP admin to control the activation of the sip
     * container
     * in tWas="sip.container.enabled"
     */
	public static final String SIP_CONTAINER_ENABLED = "sipContainerEnabled";
	public static final boolean SIP_CONTAINER_ENABLED_DEFAULT = true;
	
	/**
     *  Defined if container should allow to application to add/set
     *  system headers
     *  in tWas="enable.system.headers.modify"
     */
	public static final String SYSTEM_HEADERS_MODIFY= "enableSystemHeadersModify";
	public static final boolean SYSTEM_HEADERS_MODIFY_DEFAULT = false;
	
	//DefaultApplicationRouter
	/**
	 * Location for the property file by which repository will be managed 
	 */
	public static final String DAR_CONFIG_LOCATION = "sipDarConfiguration";
	public static final String DAR_CONFIG_LOCATION_DEFAULT = "";
	
	public static final String CAR_PROVIDER = "carProvider";
	public static final String CAR_PROVIDER_DEFAULT = "";
	public static final String CAR_SKIP_CUSTOM_PROVIDER = "com.ibm.sip.ar.DefaultApplicationRouterProvider";
	
	/**
	 * whether to use the custom application router if it exists.
	 */
	public static final String ENABLE_CAR = "enable.car";
	public static final boolean ENABLE_CAR_DEFAULT = true;
	
	
	
	public static final String INVALIDATE_SESSION_ON_SHUTDOWN = "invalidateSessionOnShutdown";
	public static final boolean INVALIDATE_SESSION_ON_SHUTDOWN_DEFAULT = false;
	
	/**
	 * Enabling purging of canceled timers 
	 * The only reason to disable this flag was due to a JDK bug in the purge mechanism. 
	 * We will do our performance tests with this one set to false, until they fix the problem, but 
	 * under normal consequences it should stay untouched, and should not be exposed to users. 
	 * in tWas="enable.canceled.timers.purge"
	 */
	public static final String ENABLE_CANCELED_TIMERS_PURGE = "enableCanceledTimersPurge";
	public static final boolean ENABLE_CANCELED_TIMERS_PURGE_DEFAULT = true;
	
	/**
	 * Bootstrap is a process that initiated whenever a new server is detected
	 * in a clustered environment (with DRS). it uses its own thread and can be a heavy
	 * duty process (to send all those SIP sessions over the network).
	 * This parameter will slow the bootstrap process a bit .
	 * This can prevent several load issues like DRSCongestionException (if 
	 * DRS is used as the replication mechanism).
	 * Bootstrap is not really needed when using ObjectGrid as replication mechanism. 
	 * in tWas="bootstrap.batch.size"
	 * @author moti
	 */
	public static final String BOOTSTRAP_BATCH_SIZE = "bootstrapBatchSize";
	public static final int DEFAULT_BOOTSTRAP_BATCH_SIZE = 4000;
	
	/**
	 * JSR 289 allows application to modify 'From' and 'To' fields, this requirement contradict
	 * other supported RFC's. Due to that this custom property is added and enabled by default
	 * to the container. This property controls the 'from_change' extension added to servlet
	 * context.
	 * in tWas="jsr289.support.legacy.client"
	 */
	public static final String JSR289_SUPPORT_LEGACY_CLIENT = "jsr289SupportLegacyClient";
	public static final boolean JSR289_SUPPORT_LEGACY_CLIENT_DEFAULT = true; 
	
	/**
	 * This custom property will enable/disable digest TAI. Currently Digest TAI is loaded
	 * and enabled automatically. There is no way to disable it other than this flag.
	 * in tWas="com.ibm.ws.sip.security.enable.digest.tai"
	 */
	public static final String ENABLE_DIGEST_TAI_PROPERTY = "securityEnableDigestTai";
	public static final String ENABLE_DIGEST_TAI_PROPERTY_DEFAULT = "true";
	
	
	/**
	 * This custom property defines which error to send in case no servlet mapping exists or
	 * all applications are down. 
	 */
	public static final String SIP_NO_ROUTE_ERROR_CODE_PROPERTY = "sipNoRouteErrorCode";
	public static final int SIP_NO_ROUTE_ERROR_CODE_PROPERTY_DEFAULT = 403;	

	/**
     * flag specifying whether or not to clone the To and From header values
     * when creating ACK for 2xx response.
     * A value of true is safer, in case the application modifies those
     * headers in the incoming 2xx response.
     * A value of false conserves memory, but requires the application does
     * not touch the To and From headers in the incoming 2xx response. 
     * in tWas="sip.clone.to.from.in.ack"
     */
    public static final String CLONE_TO_FROM_IN_ACK = "sipCloneToFromInAck";
    public static final boolean CLONE_TO_FROM_IN_ACK_DEFAULT = true;
    
    /**
	 * This custom property defines whether to parse address according to the JSR 289 rules or not
	 * the default is true, so when an address string will be used without <> parameters will  be considered as 
	 * address parameters and not URL parameters  
	 * in tWas="sip.jsr289.parse.address"
	 */
	public static final String SIP_JSR289_PARSE_ADDRESS = "sipJsr289ParseAddress";
	public static final boolean SIP_JSR289_PARSE_ADDRESS_DEFAULT = true;
	
	/**
	 * This custom property indicates whether an Object attribute on the SIP session
	 * will be replicated using read/writeObject() instead of read/writeUnshared().
	 * When <tt>true</tt> - read/writeObject() will be used,
	 * otherwise (default) - read/writeUnshared().
	 * 
	 * @see {@link ObjectInputStream#readUnshared()}
	 * @see {@link ObjectInputStream#readObject()}
	 * @see {@link ObjectOutputStream#writeUnshared(Object)}
	 * @see {@link ObjectOutputStream#writeObject(Object)}
	 * 
	 * in tWas="replicate.sip.object.attribute"
	 */
	public static final String REPLICATE_SIP_OBJECT_ATTRIBUTE = "replicateSipObjectAttribute";
	public static final boolean REPLICATE_SIP_OBJECT_ATTRIBUTE_DEFAULT = false;

	/**
	 * the local address to set as the Via sent-by host, or in the URI of
	 * self-generated Contact, Record-Route, and Path.
	 * applies to the Via in either standalone or cluster.
	 * applies to the Contact, Record-Route, and Path in standalone only.
	 * empty value obtains the local host automatically during initialization.
	 * in tWas="com.ibm.ws.sip.sent.by.host"
	 */
	public static final String SENT_BY_HOST = "sentByHost";
	public static final String SENT_BY_HOST_DEFAULT = "";
	
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
	 * in tWas="com.ibm.ws.sip.callid.value"
	 */
	public static final String CALLID_VALUE = "sipCallidValue";
	public static final String CALLID_VALUE_DEFAULT = "";
	
	/**
	 * This custom property indicates whether the container does a schema validation on the sip.xml
	 * during startup to make sure if the application is indeed jsr289 application. if the application is found
	 * to be a jsr289, sip annotations will be scanned and added to the sip.xml
	 * in tWas="enable.jsr289.schema.validation"
	 */
	public static final String  ENABLE_JSR289_SCHEMA_VALIDATION = "enableJsr289SchemaValidation";
	public static final boolean ENABLE_JSR289_SCHEMA_VALIDATION_DEFAULT = true;

	/**
	 * in tWas="b2buahelper.use.inbound.call.id.for.outbound.request"
	 */
	public static final String B2BUAHELPER_USE_INBOUND_CALL_ID_FOR_OUTBOUND_REQUEST="b2bUuseInCallidForOutRequest";
	public static final boolean B2BUAHELPER_USE_INBOUND_CALL_ID_FOR_OUTBOUND_REQUEST_DEFAULT = false;
	/**
	 * This custom property indicates whether to support the amm annotation reading during deployment.
	 * when it is set to true the sip annotations will be added to the web.xml, and the sip annotations will
	 * be added to the sip.xml during startup and not during deployment
	 * in tWas="sip.support.amm.annotation.reading"
	 */
	public static final String  SUPPORT_AMM_ANNOTATION_READING = "supportAmmAnnotationReading";
	public static final boolean SUPPORT_AMM_ANNOTATION_READING_DEFAULT = false;
	
	/**
	 * This custom property indicates whether to support the amm annotation reading during deployment.
	 * when it is set to true the sip annotations will be added to the web.xml, and the sip annotations will
	 * be added to the sip.xml during startup and not during deployment
	 * in tWas="sip.support.sar.to.war"
	 */
	public static final String  SUPPORT_SAR_TO_WAR = "supportSarToWar";
	public static final boolean SUPPORT_SAR_TO_WAR_DEFAULT = true;
	
	/**
	 * indicate whether SIP container forward SIP cancel to the application doCancel method
	 * in case of wrong Cancel message (Cancel that is being received after the initial Invite was already completed)
	 * in tWas="com.ibm.ws.sip.forward.bad.cancel.to.app"
	 */
	public static final String FORWARD_BAD_CANCEL_TO_APP = "forwardBadCancelToApp";
	public static final boolean FORWARD_BAD_CANCEL_TO_APP_DEFAULT = true;
	
	/**
	 * Indicates whether UCF messages will be ignored when monitoring
	 * the communication with a front-end proxy.
	 * This custom property is for DMZ Proxy FIS, therefore it should
	 * be enabled in a configuration with multiple front-end DMX proxies.
	 * The default is false, meaning UCF messages are not ignored.
	 * in tWas="ignore.ucf.messages.from.proxy"
	 */
	public static final String IGNORE_UCF_MESSAGES_FROM_PROXY = "ignoreUcfMessagesFromProxy";
	public static final boolean IGNORE_UCF_MESSAGES_FROM_PROXY_DEFAULT = false;
	
	/**
	 * Indicates whether when the container detects all connections with proxies are down,
	 * it will suicide (shut itself down) or not.
	 * The default is false, meaning the container will shut itself down when it
	 * detects all its connections with the SIP Proxy are down.
	 * in tWas="disable.failover.suicide"
	 */
	public static final String DISABLE_FAILOVER_SUICIDE = "disableFailoverSuicide";
	public static final boolean DISABLE_FAILOVER_SUICIDE_DEFAULT = false;
	
	/**
	 * indicate whether the IPAuthenticator will check ip addresses and hostnames or only ip addresses
	 * this can make a difference if the server can not access DNS and hostname resolving is not possible
	 * In such cases the DNS access can block the server for some time.
	 */
	public static final String IPAUTHENTICATOR_CHECK_HOST_NAMES = "ipAuthenticatorCheckHostName";
	public static final boolean IPAUTHENTICATOR_CHECK_HOST_NAMES_DEFAULT = true;
	
	/**
	 * If the application did not explicitly created sessions, but still wants to get listener notification for 
	 * each session (SAS and SS), the container will create the session before invoking a listener
	 * in tWas="create.sessions.when.listeners.exist"
	 */
	public static final String CREATE_SESSIONS_WHEN_LISTENERS_EXIST = "createSessionsWhenListenersExist";
	public static final boolean CREATE_SESSIONS_WHEN_LISTENERS_EXIST_DEFAULT = false;
	
	/**
	 * The burst factor for the message queue size.
	 * The message queue size will be set to the specified size * the burst factor,
	 * in order to allow some additional space in the queue before we really fail
	 * new messages (we do not want the queue to be blocked on capacity).
	 */
	public static final String MESSAGE_QUEUE_BURST_FACTOR = "messageQueueBurstFactor";
	public static final int MESSAGE_QUEUE_BURST_FACTOR_DEFAULT = 10;
	
	/**
	 * Time granularity of the cached timer service used for PMI task duration measurement
	 * in tWas="pmi.time.granularity.of.timer.service"
	 */
	public static final String TIME_GRANULARITY_OF_CHACHED_TIMER_SERVICE = "pmiTimerServiceGranularity";
	public static final int TIME_GRANULARITY_OF_CHACHED_TIMER_SERVICE_DEFAULT = 10;
	
	/**
	 * Defines the size of the pool of reusable byte buffers to be used during replication process.
	 * When the number of buffers in the pool reaches this number, buffers will not
	 * be allowed to return to the pool, and they'll be garbage collected instead. 
	 * A value of 0 means that the pool is disabled and all buffers will be garbage collected. 
	 * Default is unlimited pool size (-1).
	 * in tWas="max.byte.buffer.pool.size"
	 */
	public static final String MAX_BYTE_BUFFER_POOL_SIZE = "maxByteBufferPoolSize";
	public static final int MAX_BYTE_BUFFER_POOL_SIZE_DEFAULT = -1;
	
	/**
	 * Enables the HPEL log extension for SIP information.
	 * This extension adds SIP information (such as the SIP Application Session, Call and SIP Session IDs)
	 * to HPEL logging. The information will be appended to any message in the log.
	 * Default is true - meaning the log extension is enabled. 
	 * in tWas="enable.hpel.sip.log.extension"
	 */
	public static final String ENABLE_HPEL_SIP_LOG_EXTENSION = "enableHpelSipLogExtension";
	public static final boolean ENABLE_HPEL_SIP_LOG_EXTENSION_DEFAULT = true;
	
	
	/**
	 * Whether the container will save the message arrival time a an attribute of the message or not
	 * in tWas="message.arrival.time.attribute"
	 */
	public static final String SAVE_MESSAGE_ARRIVAL_TIME_ATTRIBUTE = "msgArrivalTimeAttr";
	public static final boolean SAVE_MESSAGE_ARRIVAL_TIME_ATTRIBUTE_DEFAULT = false;		
	
	/**
	 * indicate whether SIP container marks internally generated responses by adding
	 * property SipServletRequestImpl.INTERNAL_RESPONSE_ATTR = "com.ibm.websphere.sip.container.internal.message";
	 */
	public static final String MARK_INTERNAL_ERROR_RESPONSE = "markInternalResponse";
	public static final boolean MARK_INTERNAL_ERROR_RESPONSE_DEFAULT = false;
	
	/**
	 * Indicates whether an incoming request needs to be sent externally to Route header if there is no next application. 
	 * The default is true that means to route it as according to JSR 289 15.4.1. 
	 * If this property is set to false, we send an error defined in the "sip.no.route.error.code" CP.
	 * PI17820
	 */
	public static final String  WAS80_ROUTE_WHEN_NO_APPLICATION = "route.when.no.app";
	public static final boolean WAS80_ROUTE_WHEN_NO_APPLICATION_DEFAULT = true;
	
	/**
	 * This property sets a response code for session invalidated in underlying state
	 */
	public static final String WAS80_SESSION_INVALIDATE_RESPONSE = "session.invalidated.response.code";
	public static final int WAS80_SESSION_INVALIDATE_RESPONSE_DEFAULT = 408;
	
	/**
	 * Indicates whether 2xx/6xx response will be treated as best response
	 * (and therefore branch response won't be sent to the application for these responses
	 * when this property is set to true). 
	 */
	public static final String  WAS80_TREAT_2XX_6XX_AS_BEST_RESPONSE = "treat.2xx.6xx.as.best.response";
	public static final boolean WAS80_TREAT_2XX_6XX_AS_BEST_RESPONSE_DEFAULT = false;
	
	/**
	 * If set to true application can set the display name on system contact header, 
	 * According to JSR289 this is not allowed although according to 
	 *  the "spirit" of the JSR this should be allowed
	 */
	public static final String ALLOW_SETTING_SYSTEM_CONTACT_DISPLAY_NAME = "allow.setting.system.contact.display.name";
	public static final boolean ALLOW_SETTING_SYSTEM_CONTACT_DISPLAY_NAME_DEFAULT = false;
	
	/**
	 * CMVC defect 769290
	 * Fix AutomaticCancel tests. Send Automatic Cancel on INVITE 
	 * when invalidate TU as a result of explicit invalidation or expiration.
	 */
	public static final String  WAS855_AUTOMATIC_CANCEL_FIX = "automaticCancelFix";
	public static final boolean  WAS855_AUTOMATIC_CANCEL_FIX_DEFAULT = false;

	/**
	 * CMVC defect 769290
	 * Fix AutomaticCancel tests. Send Automatic Cancel on INVITE 
	 * when invalidate TU as a result of explicit invalidation or expiration.
	 */
	public static final String  WAS855_TU_COUNTER_TRANSACTION_FIX = "tuCounterTransactionFix";
	public static final boolean  WAS855_TU_COUNTER_TRANSACTION_FIX_DEFAULT = true;

	
	/**
	 * CMVC defect 769290
	 * Fix AutomaticCancel tests. Send Automatic Cancel on INVITE 
	 * when invalidate TU as a result of explicit invalidation or expiration.
	 */
	public static final String  WAS855_TU_COUNTER_TRANSACTION_FIX_SYS_OUT = "tuCounterSystemOut";
	public static final boolean  WAS855_TU_COUNTER_TRANSACTION_FIX_SYS_OUT_DEFAULT = false;
	
	/**
	 * PI62617
	 * SetOutBound interface was not working. Property added because it may change the behavior.
	 * If true setOutboundInterface() should work on proxy and proxyBranch.
	 */
	public static final String ENABLE_SET_OUTBOUND_INTERFACE = "enableSetOutboundIF";
	public static final boolean ENABLE_SET_OUTBOUND_INTERFACE_DEFAULT = false;
	/**
     * Load default properties and store them in properties.
     * This is the first properties that are loaded and some properties might be overridden later
     * by the server.xml configuration
     */
    static public void loadDefaultProperties(SipPropertiesMap properties)
    {
    	if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(CoreProperties.class.getName(),
					"loadDefaultProperties");
		}
    	
    	properties.setInt(MAX_APP_SESSIONS, MAX_APP_SESSIONS_DEFAULT,CustPropSource.DEFAULT);
		properties.setInt(MAX_MESSAGE_RATE, MAX_MESSAGE_RATE_DEFAULT,CustPropSource.DEFAULT);
		properties.setInt(MAX_RESPONSE_TIME, MAX_RESPONSE_TIME_DEFAULT,CustPropSource.DEFAULT);
		properties.setInt(STAT_UPDATE_RANGE, STAT_UPDATE_RANGE_DEFAULT,CustPropSource.DEFAULT);
		properties.setInt(STAT_AVERAGE_PERIOD, STAT_AVERAGE_PERIOD_DEFAULT,CustPropSource.DEFAULT);
		properties.setInt(LOAD_UPDATE_PERIOD, LOAD_UPDATE_PERIOD_DEFAULT,CustPropSource.DEFAULT);
		properties.setInt(MAX_MSG_QUEUE_SIZE, MAX_MSG_QUEUE_SIZE_DEFAULT,CustPropSource.DEFAULT);
		properties.setBoolean(PMI_COUNT_ALL_MESSAGES, PMI_COUNT_ALL_MESSAGES_DEFAULT,CustPropSource.DEFAULT);
		properties.setBoolean(ENABLE_LOAD_MONITORING, ENABLE_LOAD_MONITORING_DEFAULT,CustPropSource.DEFAULT);
		properties.setBoolean(PMI_PRINT_TO_TRACE, PMI_PRINT_TO_TRACE_DEFAULT, CustPropSource.DEFAULT);
		properties.setInt(GENERATED_CONTAINER_ERROR_ON_TO_TAG_DUPLICATION, GENERATED_CONTAINER_ERROR_ON_TO_TAG_DUPLICATION_DEFAULT, CustPropSource.DEFAULT);
		properties.setInt(MSG_QUEUE_INITIAL_SIZE, MSG_QUEUE_INITIAL_SIZE_DEFAULT,CustPropSource.DEFAULT);
		properties.setInt(TO_PRINT_QUEUE_STATE, TO_PRINT_QUEUE_STATE_DEFAULT,CustPropSource.DEFAULT);
		properties.setInt(PRINTING_TEMPO, PRINTING_TEMPO_DEFAULT,CustPropSource.DEFAULT);

		properties.setObject(DNSSERVERNAMES, DNSSERVERNAMES_DEFAULT,CustPropSource.DEFAULT);
		properties.setString(DNS_EDNS, DNS_EDNS_DEFAULT,CustPropSource.DEFAULT);
		properties.setShort(DNS_UDP_PAYLOAD_SIZE, DNS_UDP_PAYLOAD_SIZE_DEFAULT,CustPropSource.DEFAULT);
		properties.setBoolean(DNS_SERVER_AUTO_RESOLVE, DNS_SERVER_AUTO_RESOLVE_DEFAULT, CustPropSource.DEFAULT);
		properties.setInt(DNS_REQUEST_CACHE_TIMEOUT_MIN, DNS_REQUEST_CACHE_TIMEOUT_MIN_DEFAULT, CustPropSource.DEFAULT);
		properties.setInt(SIP_RFC3263_DNS_FAILURE_DETECTION_SINGLE_QUERY_TIMEOUT_SEC, SIP_RFC3263_DNS_FAILURE_DETECTION_SINGLE_QUERY_TIMEOUT_SEC_DEFAULT, CustPropSource.DEFAULT);
		properties.setInt(SIP_RFC3263_DNS_FAILURE_DETECTION_ALLOWED_FAILURES, SIP_RFC3263_DNS_FAILURE_DETECTION_ALLOWED_FAILURES_DEFAULT, CustPropSource.DEFAULT);
		properties.setInt(SIP_RFC3263_DNS_FAILURE_DETECTION_WINDOW_SIZE_MIN, SIP_RFC3263_DNS_FAILURE_DETECTION_WINDOW_SIZE_MIN_DEFAULT, CustPropSource.DEFAULT);
		properties.setInt(SIP_RFC3263_DNS_FAILURE_DETECTION_WINDOW_SIZE_INTERVAL_SEC, SIP_RFC3263_DNS_FAILURE_DETECTION_WINDOW_SIZE_INTERVAL_SEC_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(SIP_RFC3263_ADD_TTL, SIP_RFC3263_ADD_TTL_DEFAULT, CustPropSource.DEFAULT);
		properties.setLong(SIP_DNS_QUERY_TIMEOUT, SIP_DNS_QUERY_TIMEOUT_DEFAULT, CustPropSource.DEFAULT);

		properties.setBoolean(CHECK_FOR_REPLICATION_CONCURRENCY, CHECK_FOR_REPLICATION_CONCURRENCY_DEFAULT,CustPropSource.DEFAULT);
		properties.setBoolean(IMMEDIATE_REPLICATION, IMMEDIATE_REPLICATION_DEFAULT,CustPropSource.DEFAULT);
		properties.setBoolean(END_OF_SERVICE_REPLICATION, END_OF_SERVICE_REPLICATION_DEFAULT,CustPropSource.DEFAULT);
		properties.setBoolean(REPLICATE_ON_SERVING_THREAD, REPLICATE_ON_SERVING_THREAD_DEFAULT,CustPropSource.DEFAULT);
		properties.setBoolean(ON_OUTGOING_MESSAGE_SEND_REPLICATION, ON_OUTGOING_MESSAGE_SEND_REPLICATION_DEFAULT,CustPropSource.DEFAULT);
		properties.setBoolean(APPLICATION_CALL_REPLICATION, APPLICATION_CALL_REPLICATION_DEFAULT,CustPropSource.DEFAULT);
		properties.setLong(REPLICATION_INTERVAL_PROP, REPLICATION_INTERVAL_PROP_DEFAULT,CustPropSource.DEFAULT);
		properties.setInt(REPLICATION_HANDLERS_COUNT, REPLICATION_HANDLERS_COUNT_DEFAULT,CustPropSource.DEFAULT);
		properties.setInt(REPLICATION_QUEUE_CAPACITY, REPLICATION_QUEUE_CAPACITY_DEFAULT,CustPropSource.DEFAULT);
		properties.setBoolean(ENABLE_MULTIPLE_VH, ENABLE_MULTIPLE_VH_DEFAULT,CustPropSource.DEFAULT);
		properties.setBoolean(USE_EXTENSION_PROCESSOR, USE_EXTENSION_PROCESSOR_DEFAULT,CustPropSource.DEFAULT);
		properties.setInt(LOW_WATER_MARK_SIZE, LOW_WATER_MARK_SIZE_DEFAULT,CustPropSource.DEFAULT);
		properties.setInt(WEIGHT_OVERLOAD_MARK, WEIGHT_OVERLOAD_MARK_DEFAULT,CustPropSource.DEFAULT);
		properties.setInt(TRACE_PMI_MODULUS, TRACE_PMI_MODULUS_DEFAULT, CustPropSource.DEFAULT);
		properties.setString(IP_LIST_PROPERTY, IP_LIST_PROPERTY_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(NO_MANDATORY_REPLICATION_WHEN_ATTRIBUTE_SET,NO_MANDATORY_REPLICATION_WHEN_ATTRIBUTE_SET_DEFAULT ,CustPropSource.DEFAULT);
		properties.setBoolean(MANDATORY_REPLICATION_WHEN_HTTP_SESSIONS_SET, MANDATORY_REPLICATION_WHEN_HTTP_SESSIONS_SET_DEFAULT ,CustPropSource.DEFAULT);
		properties.setBoolean(REPLICATE_WITH_CONFIRMED_DIALOG_ONLY, REPLICATE_WITH_CONFIRMED_DIALOG_ONLY_DEFAULT,CustPropSource.DEFAULT);
		properties.setBoolean(ENABLE_APP_COMPOSITION,ENABLE_APP_COMPOSITION_DEFAULT, CustPropSource.DEFAULT);
		properties.setString(SIP_SESSION_SEQ_LOG_LEVEL, SIP_SESSION_SEQ_LOG_LEVEL_DEFAULT, CustPropSource.DEFAULT);
		properties.setInt(TIMER_STAT_REPORT_INTERVAL, TIMER_STAT_REPORT_INTERVAL_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(ENABLE_TIMERS, ENABLE_TIMERS_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(SIP_CONTAINER_ENABLED, SIP_CONTAINER_ENABLED_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(SYSTEM_HEADERS_MODIFY, SYSTEM_HEADERS_MODIFY_DEFAULT, CustPropSource.DEFAULT);
		properties.setString(DAR_CONFIG_LOCATION, DAR_CONFIG_LOCATION_DEFAULT,CustPropSource.DEFAULT);
		properties.setString(CAR_PROVIDER,CAR_PROVIDER_DEFAULT,CustPropSource.DEFAULT);
		
		properties.setBoolean(INVALIDATE_SESSION_ON_SHUTDOWN, INVALIDATE_SESSION_ON_SHUTDOWN_DEFAULT, CustPropSource.DEFAULT);
		
		properties.setBoolean(ENABLE_CANCELED_TIMERS_PURGE, ENABLE_CANCELED_TIMERS_PURGE_DEFAULT, CustPropSource.DEFAULT);
		
		properties.setInt(BOOTSTRAP_BATCH_SIZE, DEFAULT_BOOTSTRAP_BATCH_SIZE, CustPropSource.DEFAULT);

		properties.setBoolean(JSR289_SUPPORT_LEGACY_CLIENT, JSR289_SUPPORT_LEGACY_CLIENT_DEFAULT, CustPropSource.DEFAULT);

		properties.setString(ENABLE_DIGEST_TAI_PROPERTY, ENABLE_DIGEST_TAI_PROPERTY_DEFAULT, CustPropSource.DEFAULT);		
		
		
		properties.setBoolean(CLONE_TO_FROM_IN_ACK, CLONE_TO_FROM_IN_ACK_DEFAULT, CustPropSource.DEFAULT);
		
		properties.setInt(SIP_NO_ROUTE_ERROR_CODE_PROPERTY, SIP_NO_ROUTE_ERROR_CODE_PROPERTY_DEFAULT, CustPropSource.DEFAULT);
		
		properties.setBoolean(SIP_JSR289_PARSE_ADDRESS, SIP_JSR289_PARSE_ADDRESS_DEFAULT, CustPropSource.DEFAULT);
		
		properties.setBoolean(REPLICATE_SIP_OBJECT_ATTRIBUTE, REPLICATE_SIP_OBJECT_ATTRIBUTE_DEFAULT, CustPropSource.DEFAULT);

		properties.setString(SENT_BY_HOST, SENT_BY_HOST_DEFAULT, CustPropSource.DEFAULT);
		properties.setString(CALLID_VALUE, CALLID_VALUE_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(ENABLE_JSR289_SCHEMA_VALIDATION, ENABLE_JSR289_SCHEMA_VALIDATION_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(SUPPORT_AMM_ANNOTATION_READING, SUPPORT_AMM_ANNOTATION_READING_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(SUPPORT_SAR_TO_WAR, SUPPORT_SAR_TO_WAR_DEFAULT, CustPropSource.DEFAULT);
	    properties.setBoolean(B2BUAHELPER_USE_INBOUND_CALL_ID_FOR_OUTBOUND_REQUEST, B2BUAHELPER_USE_INBOUND_CALL_ID_FOR_OUTBOUND_REQUEST_DEFAULT, CustPropSource.DEFAULT);
	    properties.setBoolean(FORWARD_BAD_CANCEL_TO_APP, FORWARD_BAD_CANCEL_TO_APP_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(MARK_INTERNAL_ERROR_RESPONSE, MARK_INTERNAL_ERROR_RESPONSE_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(IGNORE_UCF_MESSAGES_FROM_PROXY, IGNORE_UCF_MESSAGES_FROM_PROXY_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(DISABLE_FAILOVER_SUICIDE, DISABLE_FAILOVER_SUICIDE_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(IPAUTHENTICATOR_CHECK_HOST_NAMES, IPAUTHENTICATOR_CHECK_HOST_NAMES_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(CREATE_SESSIONS_WHEN_LISTENERS_EXIST, CREATE_SESSIONS_WHEN_LISTENERS_EXIST_DEFAULT, CustPropSource.DEFAULT);
		properties.setInt(MESSAGE_QUEUE_BURST_FACTOR, MESSAGE_QUEUE_BURST_FACTOR_DEFAULT, CustPropSource.DEFAULT);
		properties.setInt(TIME_GRANULARITY_OF_CHACHED_TIMER_SERVICE, TIME_GRANULARITY_OF_CHACHED_TIMER_SERVICE_DEFAULT, CustPropSource.DEFAULT);
		properties.setInt(MAX_BYTE_BUFFER_POOL_SIZE, MAX_BYTE_BUFFER_POOL_SIZE_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(ENABLE_HPEL_SIP_LOG_EXTENSION, ENABLE_HPEL_SIP_LOG_EXTENSION_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(SAVE_MESSAGE_ARRIVAL_TIME_ATTRIBUTE, SAVE_MESSAGE_ARRIVAL_TIME_ATTRIBUTE_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(WAS80_ROUTE_WHEN_NO_APPLICATION, WAS80_ROUTE_WHEN_NO_APPLICATION_DEFAULT, CustPropSource.DEFAULT);
		properties.setInt(WAS80_SESSION_INVALIDATE_RESPONSE, WAS80_SESSION_INVALIDATE_RESPONSE_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(WAS80_TREAT_2XX_6XX_AS_BEST_RESPONSE, WAS80_TREAT_2XX_6XX_AS_BEST_RESPONSE_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(ALLOW_SETTING_SYSTEM_CONTACT_DISPLAY_NAME, ALLOW_SETTING_SYSTEM_CONTACT_DISPLAY_NAME_DEFAULT, CustPropSource.DEFAULT);
		
		//New Liberty properties
		properties.setInt(CONCURRENT_CONTAINER_TASKS, CONCURRENT_CONTAINER_TASKS_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(ENABLE_CAR, ENABLE_CAR_DEFAULT, CustPropSource.DEFAULT);
		
		properties.setBoolean(WAS855_AUTOMATIC_CANCEL_FIX, WAS855_AUTOMATIC_CANCEL_FIX_DEFAULT, CustPropSource.DEFAULT);	
		properties.setBoolean(WAS855_TU_COUNTER_TRANSACTION_FIX, WAS855_TU_COUNTER_TRANSACTION_FIX_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(WAS855_TU_COUNTER_TRANSACTION_FIX_SYS_OUT, WAS855_TU_COUNTER_TRANSACTION_FIX_SYS_OUT_DEFAULT, CustPropSource.DEFAULT);
		properties.setBoolean(ENABLE_SET_OUTBOUND_INTERFACE,ENABLE_SET_OUTBOUND_INTERFACE_DEFAULT, CustPropSource.DEFAULT);
    }
}
