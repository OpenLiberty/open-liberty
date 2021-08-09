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
 * Class that collect all properties for the HA
 */
public class HAProperties {
	
	/**
	 * Class Logger. 
	 */
	private static final transient LogMgr c_logger = Log
			.get(HAProperties.class);
	
	/* consts */
	/**
	 * Is enable measures for operation duration statistics for debug and 
	 * performance optimization purposes
	 */
	public static final String ENABLE_FAILOVER_OPERATION_MEASUREMENTS = "enable.failover.operation.measurements";
	public static final boolean ENABLE_FAILOVER_OPERATION_MEASUREMENTS_DEFAULT = false;
	
	/**
	 * Will allow the start of a background process of failover sessions activation after
	 * failover
	 */
	public static final String ENABLE_BACKGROUND_ACTIVATION_PROCESS = "enable.background.activation.operation.process";
	public static final boolean ENABLE_BACKGROUND_ACTIVATION_PROCESS_DEFAULT = true;

	/**
	 * Will allow the container thread to access the replicatables tree to activate a failed-over
	 * session if it was not found on the Transaction-User table
	 */
	public static final String ENABLE_ON_DEMAND_ACTIVATION_PROCESS = "enable.on.demand.activation.operation.process";
	public static final boolean ENABLE_ON_DEMAND_ACTIVATION_PROCESS_DEFAULT = true;

	/**
	 *  constants that defined if failover enabled or not in container.properties file
	 */
	public static final String ENABLE_FAILOVER = "enable.failover";
	public static final boolean ENABLE_FAILOVER_DEFAULT = true;

	/**
	 *  Property name for enabling the replication feature
	 */
	public static final String ENABLE_REPLICATION = "enable.replication";
	public static final boolean ENABLE_REPLICATION_DEFAULT = false;

	/**
	 *  constants that defined if call-Id should be logged during the failover
	 */
	public static final String LOG_CALL_ID= "log.callid.during.failover";
	public static final boolean LOG_CALL_ID_DEFAULT = false;

	/**
	 * If this mode is on, replication debug messages will be logged
	 */
	public static final String ENABLE_REPLICATION_DEBUG_MODE ="enable.replication.debug.mode";
	public static final boolean ENABLE_REPLICATION_DEBUG_MODE_DEFAULT = false;

	/**
	 * property entry that sets _enableDynamicWeight value
	 */
	public static final String ENABLE_LOAD_BALANCING_DYNAMIC_WEIGHTS ="enable.load.balancing.dynamic.weights";
	public static final boolean ENABLE_LOAD_BALANCING_DYNAMIC_WEIGHTS_DEFAULT = true;

	/**
	 * Indicates is HeartbeatMonitor feature is enabled
	 */
	public static final String HEARTBEAT_ENABLED = "sip.container.heartbeat.enabled";
	public static final boolean HEARTBEAT_ENABLED_DEFAULT = true;
	
	 /**
	  * The number of LogicalNames in the AppServer. 
	  * This will be important when we allow "number of replicas". 
	  * Each Sip replicable object (for sample Sip Session) is associated to a Logical Name. 
	  * all objects of the same logical name get replicated to the same backup container. 
	  * the Proxy can route messages to the correct container using LogicalName? found in the message. 
	  * Range of valid values >1
	  * 
	  * TODO is it possible that the default value is 30 ?
	  */ 	 
	public static final String NUMBER_OF_LOGICAL_NAME = "com.ibm.sip.sm.lnm.size";
	public static final short NUMBER_OF_LOGICAL_NAME_DEFAULT = 1;
	
	public static final String REPLICATOR_POOL_SIZE = "com.ibm.sip.ha.replicator.poolsize";
	public static final int REPLICATOR_POOL_SIZE_DEFAULT = 3;
	
	/**
	 *  Time in milliseconds
	 */ 
	public static final String REPLICA_TIMER_PERIOD_PRORP = "com.ibm.sip.replica.period";
	public static final int REPLICA_TIMER_PERIOD_PRORP_DEFAULT = 1000;

	public static final String REPLICATOR_INITIAL_BUFFER_SIZE = "com.ibm.sip.ha.replicator.buffersize.init";
	public static final int REPLICATOR_INITIAL_BUFFER_SIZE_DEFAULT = -1;
	
	public static final String REPLICATOR_MAX_BUFFER_SIZE= "com.ibm.sip.ha.replicator.buffersize.max";
	public static final int REPLICATOR_MAX_BUFFER_SIZE_DEFAULT = 3000;
	
	/**
	 * replica type - can be FS(file system) or DB or DRS
	 */
	public static final String CACHE_TYPE="com.ibm.sip.ha.replica.type";
	public static final String CACHE_TYPE_FS="FS";
	public static final String CACHE_TYPE_DRS="DRS";
	public static final String CACHE_TYPE_DB="DB";
	public static final String CACHE_TYPE_OG="OG";
	public static final String CACHE_TYPE_DEFAULT = CACHE_TYPE_DRS;
	
	public static final String DATA_SOURCE_JNDI_NAME="com.ibm.sip.ha.ds";
	private static final String DATA_SOURCE_JNDI_NAME_DEFAULT = "jdbc/siphaDB2DS";
	
	/**
	 * replicator type - can be HA or DRS or NONE (when using DB)
	 * this is actually extarnalized to the WAS admin console
	 * under SIP Container custom property.
	 * With this parameter we determine what replication service
	 * we intend to use.
	 */
	public static final String REPLICATOR_TYPE = "com.ibm.sip.ha.replicator.type";
	public static final String REPLICATOR_TYPE_DRS= "DRS";
	public static final String REPLICATOR_TYPE_OBJECTGRID= "OBJECTGRID";

	/**
	 * a parameter which let you change the implementation class of the SIP HA callback
	 * method.
	 */
	public static final String HACALLBACK_OBJGRID_PROP= "com.ibm.sip.ha.objgrid.callback";
	public static final String HACALLBACK_OBJECTGRID_PROP_DEFAULT= "com.ibm.ws.session.sip.ussm.XDSIPHAGroupCallback";


	/**
	 * This property is actually not used anymore with the new UnifiedSIPSessionMgr
	 */
	public static final String OBJGRID_ALG_CACHE_PROP= "com.ibm.sip.ha.objgrid.cache";
	public static final String OBJGRID_ALG_CACHE_PROP_DEFAULT= "com.ibm.ws.session.sip.ussm.XDLogicalNameAlgCache";

	/**
	 * This property is actually not used anymore with the new UnifiedSIPSessionMgr
	 * (well it might appear in the code , but that code never really executed).
	 */
	public static final String OBJGRID_REPLICATOR_PROP= "com.ibm.sip.ha.objgrid.replicator";
	public static final String OBJGRID_REPLICATOR_PROP_DEFAULT= "com.ibm.ws.session.sip.ussm.EOSSIPSessionStorer";

	
	/**
	 * a replication service that uses database to store/retreive SIP object
	 * usually the DB is fronted by ObjectGrid 
	 */
	public static final String REPLICATOR_TYPE_JDBC= "JDBC";
	
	/**
	 * type HA is depricated since no one is using this property.
	 * @deprecated
	 */ 
	public static final String REPLICATOR_TYPE_HA= "HA";
	
	public static final String REPLICATOR_TYPE_DEFAULT = REPLICATOR_TYPE_DRS;
	
	/**
	 *  replication domain
	 */
	public static final String REPLICATION_DOMAIN_NAME = "com.ibm.sip.ha.rep.domain";
	public static final String REPLICATION_DOMAIN_NAME_DEFAULT = "sipha";
	
	public static final String DB_REPLICATOR_WORKERS_NUMBER= "com.ibm.sip.ha.db.worker.number";
	public static final int DB_REPLICATOR_WORKERS_NUMBER_DEFAULT = 5;
	
	// TODO Do we need it ?
	public static final String LOGICAL_NAME_CLEANUP = "com.ibm.sip.ha.logicalname.cleanup";
	
	public static final String TASK_DELAY_PROP = "SlspClusterObserver.ucf.delay";
	public static final long TASK_DELAY_PROP_DEFAULT = 6000L;
		
	
	/**
	 * Unified Sip Session repository ctor.
	 */
	public static final String SAS_ATTR_OGv2_NAME = "SASATTROG";
	public static final String SAS_ATTR_JDBC_NAME = "SASATTRJDBC";
    public static final String SASATTROG_className = "com.ibm.ws.sip.container.failover.repository.objgrid.ObjGrid_SASAttrMgr";
    public static final String SASATTRJDBC_className = "com.ibm.ws.sip.container.failover.repository.objgrid.remote.Rm_SASAttrMgr";

	public static final String SAS_OGv2_NAME = "SASOG";
	public static final String SAS_JDBC_NAME = "SASJDBC";
    public static final String SASOG_className = "com.ibm.ws.sip.container.failover.repository.objgrid.ObjGrid_SASMgr";
    public static final String SASJDBC_className = "com.ibm.ws.sip.container.failover.repository.objgrid.remote.Rm_SASMgr";

	public static final String SS_ATTR_OGv2_NAME = "SSATTROG";
	public static final String SS_ATTR_JDBC_NAME = "SSATTRJDBC";
	public static final String SSATTROG_className = "com.ibm.ws.sip.container.failover.repository.objgrid.ObjGrid_SSAttrMgr";
	public static final String SSATTRJDBC_className = "com.ibm.ws.sip.container.failover.repository.objgrid.remote.Rm_SSAttrMgr";

	public static final String SS_OGv2_NAME = "SSOG";
	public static final String SS_JDBC_NAME = "SSJDBC";
	public static final String SSOG_className = "com.ibm.ws.sip.container.failover.repository.objgrid.ObjGrid_SSMgr";
	public static final String SSJDBC_className = "com.ibm.ws.sip.container.failover.repository.objgrid.remote.Rm_SSMgr";

	public static final String TIMERS_OGv2_NAME = "TIMERSOG";
	public static final String TIMERS_JDBC_NAME = "TIMERSJDBC";
	public static final String TIMERS_OGv2_className = "com.ibm.ws.sip.container.failover.repository.objgrid.ObjGrid_TimerMgr";
	public static final String TIMERS_JDBC_className = "com.ibm.ws.sip.container.failover.repository.objgrid.remote.Rm_TimerMgr";

	
	public static final String TUB_OGv2_NAME = "TUBOG";
	public static final String TUB_JDBC_NAME = "TUBJDBC";
    private static final String TUB_OGv2_className = "com.ibm.ws.sip.container.failover.repository.objgrid.ObjGrid_TuBaseMgr";
    private static final String TUB_JDBC_className = "com.ibm.ws.sip.container.failover.repository.objgrid.remote.Rm_TuBaseMgr";

	public static final String TUI_OGv2_NAME = "TUIOG";
	public static final String TUI_JDBC_NAME = "TUIJDBC";
    private static final String TUI_OGv2_className = "com.ibm.ws.sip.container.failover.repository.objgrid.ObjGrid_TuImplMgr";
    private static final String TUI_JDBC_className = "com.ibm.ws.sip.container.failover.repository.objgrid.remote.Rm_TuImplMgr";

	
	/**
     * Load default properties and store them in properties.
     * This is the first properties that are loaded and some properties might be overridden later
     * by the WCCM configuration
     */
    static public void loadDefaultProperties(SipPropertiesMap properties)
    {
    	if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(HAProperties.class.getName(),
					"loadDefaultProperties");
		}
    	
    	properties.setBoolean(ENABLE_FAILOVER_OPERATION_MEASUREMENTS, ENABLE_FAILOVER_OPERATION_MEASUREMENTS_DEFAULT,CustPropSource.DEFAULT);
    	properties.setBoolean(ENABLE_BACKGROUND_ACTIVATION_PROCESS, ENABLE_BACKGROUND_ACTIVATION_PROCESS_DEFAULT,CustPropSource.DEFAULT);
		properties.setBoolean(ENABLE_ON_DEMAND_ACTIVATION_PROCESS, ENABLE_ON_DEMAND_ACTIVATION_PROCESS_DEFAULT,CustPropSource.DEFAULT);
		properties.setBoolean(ENABLE_FAILOVER, ENABLE_FAILOVER_DEFAULT,CustPropSource.DEFAULT);
		properties.setBoolean(ENABLE_REPLICATION, ENABLE_REPLICATION_DEFAULT,CustPropSource.DEFAULT);
		properties.setBoolean(LOG_CALL_ID, LOG_CALL_ID_DEFAULT,CustPropSource.DEFAULT);
		properties.setBoolean(ENABLE_REPLICATION_DEBUG_MODE, ENABLE_REPLICATION_DEBUG_MODE_DEFAULT,CustPropSource.DEFAULT);
		properties.setBoolean(ENABLE_LOAD_BALANCING_DYNAMIC_WEIGHTS, ENABLE_LOAD_BALANCING_DYNAMIC_WEIGHTS_DEFAULT,CustPropSource.DEFAULT);
		properties.setBoolean(HEARTBEAT_ENABLED, HEARTBEAT_ENABLED_DEFAULT,CustPropSource.DEFAULT);
		properties.setShort(NUMBER_OF_LOGICAL_NAME, NUMBER_OF_LOGICAL_NAME_DEFAULT,CustPropSource.DEFAULT);
		properties.setInt(REPLICA_TIMER_PERIOD_PRORP, REPLICA_TIMER_PERIOD_PRORP_DEFAULT,CustPropSource.DEFAULT);
		properties.setInt(REPLICATOR_POOL_SIZE, REPLICATOR_POOL_SIZE_DEFAULT, CustPropSource.DEFAULT);
		properties.setInt(REPLICATOR_INITIAL_BUFFER_SIZE, REPLICATOR_INITIAL_BUFFER_SIZE_DEFAULT,CustPropSource.DEFAULT);
		properties.setInt(REPLICATOR_MAX_BUFFER_SIZE, REPLICATOR_MAX_BUFFER_SIZE_DEFAULT,CustPropSource.DEFAULT);
		properties.setString(CACHE_TYPE,CACHE_TYPE_DEFAULT,CustPropSource.DEFAULT);
		properties.setString(DATA_SOURCE_JNDI_NAME, DATA_SOURCE_JNDI_NAME_DEFAULT,CustPropSource.DEFAULT);
		properties.setString(REPLICATOR_TYPE, REPLICATOR_TYPE_DEFAULT, CustPropSource.DEFAULT);
		properties.setString(REPLICATION_DOMAIN_NAME, REPLICATION_DOMAIN_NAME_DEFAULT, CustPropSource.DEFAULT);
		properties.setInt(DB_REPLICATOR_WORKERS_NUMBER,DB_REPLICATOR_WORKERS_NUMBER_DEFAULT,CustPropSource.DEFAULT);
		properties.setLong(TASK_DELAY_PROP, TASK_DELAY_PROP_DEFAULT, CustPropSource.DEFAULT);
		properties.setString(HACALLBACK_OBJGRID_PROP, HACALLBACK_OBJECTGRID_PROP_DEFAULT, CustPropSource.DEFAULT);
		properties.setString(OBJGRID_ALG_CACHE_PROP,OBJGRID_ALG_CACHE_PROP_DEFAULT, CustPropSource.DEFAULT);
		properties.setString(OBJGRID_REPLICATOR_PROP,OBJGRID_REPLICATOR_PROP_DEFAULT,CustPropSource.DEFAULT);
		
		properties.setString(SAS_ATTR_OGv2_NAME,SASATTROG_className, CustPropSource.DEFAULT);
		properties.setString(SAS_ATTR_JDBC_NAME,SASATTRJDBC_className,CustPropSource.DEFAULT);
		properties.setString(SAS_OGv2_NAME,SASOG_className, CustPropSource.DEFAULT);
		properties.setString(SAS_JDBC_NAME,SASJDBC_className,CustPropSource.DEFAULT);
		properties.setString(SS_ATTR_OGv2_NAME,SSATTROG_className, CustPropSource.DEFAULT);
		properties.setString(SS_ATTR_JDBC_NAME,SSATTRJDBC_className,CustPropSource.DEFAULT);
		properties.setString(SS_OGv2_NAME,SSOG_className, CustPropSource.DEFAULT);
		properties.setString(SS_JDBC_NAME,SSJDBC_className,CustPropSource.DEFAULT);
		properties.setString(TIMERS_OGv2_NAME,TIMERS_OGv2_className, CustPropSource.DEFAULT);
		properties.setString(TIMERS_JDBC_NAME,TIMERS_JDBC_className,CustPropSource.DEFAULT);
		properties.setString(TUB_OGv2_NAME,TUB_OGv2_className, CustPropSource.DEFAULT);
		properties.setString(TUB_JDBC_NAME,TUB_JDBC_className,CustPropSource.DEFAULT);
		properties.setString(TUI_OGv2_NAME,TUI_OGv2_className, CustPropSource.DEFAULT);
		properties.setString(TUI_JDBC_NAME,TUI_JDBC_className,CustPropSource.DEFAULT);

    }
}
