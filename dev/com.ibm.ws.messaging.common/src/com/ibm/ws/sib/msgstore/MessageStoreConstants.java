/* ==============================================================================
 * Copyright (c) 2012, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ==============================================================================
 */
package com.ibm.ws.sib.msgstore;


import com.ibm.ws.sib.utils.RuntimeInfo;

/**
 * This class just contains component-wide constants.
 * It does not include any constants used by other components.
 * 
 * DO NOT PUT ANY METHOD DECLARATIONS IN HERE (PLEASE).
 */
public interface MessageStoreConstants
{
    /*************************************************************************/
    /* Trace System Properties */
    /*************************************************************************/

    public final static String MSG_GROUP = com.ibm.ws.sib.utils.TraceGroups.TRGRP_MSGSTORE;
    public final static String MSG_BUNDLE = "com.ibm.ws.sib.msgstore.CWSISMessages";

    /*************************************************************************/
    /* Configuration properties */
    /*************************************************************************/

    // Since feature 220097, all MS properties use the prefix "sib.msgstore.".
    // I'm counting on the compiler gluing together these fixed strings intelligently :-)
    public final static String STANDARD_PROPERTY_PREFIX =
                    RuntimeInfo.SIB_PROPERTY_PREFIX + "msgstore" + RuntimeInfo.SIB_PROPERTY_SEPARATOR;

    /*************************************************************************/
    /* Transaction Limit Properties */
    /*************************************************************************/

    // Maximum number of user actions per transaction
    public final static String PROP_TRANSACTION_SEND_LIMIT = "transactionSendLimit";
    public final static String PROP_TRANSACTION_SEND_LIMIT_DEFAULT = "100";

    /*************************************************************************/
    /* Caching Properties */
    /*************************************************************************/

    // Variable name for setting size of memory cache for storedItems
    // 538096 multiply by 4 to cater for using inMemorySize
    public static final String PROP_STORED_CACHE_SIZE = "cachedDataBufferSize";
    public static final String PROP_STORED_CACHE_SIZE_DEFAULT = "40000000";

    // Variable name for setting maximum size of items in memory cache for storedItems
    // set to -1 to use old style cache
    // 538096 multiply by 4 to cater for using inMemorySize
    public static final String PROP_STORED_CACHE_MAXIMUM_ITEM_SIZE = "cachedDataItemMaximumSize";
    public static final String PROP_STORED_CACHE_MAXIMUM_ITEM_SIZE_DEFAULT = "400000";

    // Variable name for setting size of memory cache for unstoredItems
    // 538096 multiply by 4 to cater for using inMemorySize
    public static final String PROP_UNSTORED_CACHE_SIZE = "discardableDataBufferSize";
    public static final String PROP_UNSTORED_CACHE_SIZE_DEFAULT = "1280000";

    // Variable name for setting type and size of the id-to-itemlink map
    public static final String PROP_ITEM_MAP_TYPE = "itemMapType";
    // multiMap is a collection of individually synchronized Hashmaps, size recommended = 20;
    public static final String PROP_ITEM_MAP_TYPE_MULTIMAP = "multimap";
    // fastMap is a bespoke hashmap
    public static final String PROP_ITEM_MAP_TYPE_FASTMAP = "fastMap";
    public static final String PROP_ITEM_MAP_TYPE_DEFAULT = PROP_ITEM_MAP_TYPE_FASTMAP;
    public static final String PROP_ITEM_MAP_SIZE = "itemMapSize";
    public static final String PROP_ITEM_MAP_SIZE_DEFAULT = "18";

    public static final String PROP_ITEM_MAP_PARALLELISM = "itemMapParallelism";
    public static final String PROP_ITEM_MAP_PARALLELISM_DEFAULT = "7";

    public final static String PROP_JDBC_DISABLE_BATCHING = "disableJdbcBatching";//PM49953
    public final static String PROP_JDBC_DISABLE_BATCHING_DEFAULT = "false";//PM49953

    /*************************************************************************/
    /* Spilling Properties */
    /*************************************************************************/

    // Parameter for spill upper limit
    public static final String PROP_SPILL_UPPER_LIMIT = "spillUpperLimit";
    public static final String PROP_SPILL_UPPER_LIMIT_DEFAULT = "20";

    // Parameter for spill lower limit
    public static final String PROP_SPILL_LOWER_LIMIT = "spillLowerLimit";
    public static final String PROP_SPILL_LOWER_LIMIT_DEFAULT = "2";

    // Defect 484799
    // Parameter for spill upper size limit in bytes
    // 538096 multiply by 4 to cater for using inMemorySize
    public static final String PROP_SPILL_UPPER_SIZE_LIMIT = "spillUpperSizeLimit";
    public static final String PROP_SPILL_UPPER_SIZE_LIMIT_DEFAULT = "4000000";

    // Defect 484799
    // Parameter for spill lower size limit in bytes
    // 538096 multiply by 4 to cater for using inMemorySize
    public static final String PROP_SPILL_LOWER_SIZE_LIMIT = "spillLowerSizeLimit";
    public static final String PROP_SPILL_LOWER_SIZE_LIMIT_DEFAULT = "400000";

    /*************************************************************************/
    /* Connection Retry Properties */
    /*************************************************************************/

    // The number of times to retry connecting when a stale connection is encountered
    public final static String PROP_STALE_CONNECTION_RETRY_COUNT = "jdbcStaleConnectionRetryCount";
    public final static String PROP_STALE_CONNECTION_RETRY_COUNT_DEFAULT = "5";

    // The delay (in milliseconds) between retry attempts when a stale connection is encountered
    public final static String PROP_STALE_CONNECTION_RETRY_DELAY = "jdbcStaleConnectionRetryDelay";
    public final static String PROP_STALE_CONNECTION_RETRY_DELAY_DEFAULT = "2000";

    // The maximum period (in milliseconds) to retry a JDBC operation
    public final static String PROP_CONNECTION_RETRY_MAXIMUM_DURATION = "jdbcConnectionRetryMaximumDuration";
    public final static String PROP_CONNECTION_RETRY_MAXIMUM_DURATION_DEFAULT = "900000"; // 15 minutes

    // The maximum period (in milliseconds) to wait for the data source wrapper to be reenabled during startup
    public final static String PROP_INITIAL_DATASOURCE_WAIT_TIMEOUT = "jdbcInitialDatasourceWaitTimeout";
    public final static String PROP_INITIAL_DATASOURCE_WAIT_TIMEOUT_DEFAULT = "900000"; // 15 minutes

    // The maximum period (in milliseconds) to wait for the data source wrapper to be reenabled
    public final static String PROP_DISABLED_DATASOURCE_WAIT_TIMEOUT = "jdbcDisabledDatasourceWaitTimeout";
    public final static String PROP_DISABLED_DATASOURCE_WAIT_TIMEOUT_DEFAULT = "200";

    // The waiting period (in milliseconds) for the data source wrapper to be reenabled, during startup before checking for serverStopping notification
    public final static long PROP_DATASOURCE_WAIT_TIMEOUT = 30000; // 30 seconds

    /*************************************************************************/
    /* Persistence Layer Properties */
    /*************************************************************************/

    // Should we upgrade the quality of service of persistent items if possible?
    public final static String PROP_UPGRADE_RELIABLE_PERSISTENT_TO_ASSURED = "upgradeReliablePersistentToAssured";
    public final static String PROP_UPGRADE_RELIABLE_PERSISTENT_TO_ASSURED_DEFAULT = "false";

    // What authentication mechanism should we use when sharing connections?
    public final static String PROP_RES_AUTH_FOR_CONNECTIONS = "jdbcResAuthForConnections";
    public final static String PROP_RES_AUTH_FOR_CONNECTIONS_DEFAULT = "Container";

    // Should we check the table columns during startup?
    public final static String PROP_PERFORM_COLUMN_CHECKS = "jdbcPerformColumnChecks";
    public final static String PROP_PERFORM_COLUMN_CHECKS_DEFAULT = "true";

    /**
     * This property, if present, provides the name of the class used by the
     * PersistenceFactory to construct the PersistentMessageStore. This is the
     * mechanism used to enable plugging of alternative persistence mechanisms.
     */
    public static final String PROP_PERSISTENT_MESSAGE_STORE_CLASS = "persistentMessageStoreClass";
    public static final String PERSISTENT_MESSAGE_STORE_CLASS_DATABASE = "com.ibm.ws.sib.msgstore.persistence.impl.PersistentMessageStoreImpl";
    public static final String PERSISTENT_MESSAGE_STORE_CLASS_OBJECTMANAGER = "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistentMessageStoreImpl";
    public static final String PROP_PERSISTENT_MESSAGE_STORE_CLASS_DEFAULT = PERSISTENT_MESSAGE_STORE_CLASS_OBJECTMANAGER;

    /*************************************************************************/
    /* Persistent Dispatcher Properties */
    /*************************************************************************/

    public final static String PROP_JDBC_WRITE_THREADS = "jdbcWriteThreads";
    public final static String PROP_JDBC_WRITE_THREADS_DEFAULT = "8";

    // The priority delta added to Thread.NORM_PRIORITY for Persistent dispatcher threads
    public final static String PROP_JDBC_WRITE_THREAD_PRIORITY_DELTA = "jdbcWriteThreadPriorityDelta";
    public final static String PROP_JDBC_WRITE_THREAD_PRIORITY_DELTA_DEFAULT = "0";

    public final static String PROP_JDBC_WRITE_TARGET_LATENCY_MILLIS = "jdbcWriteTargetLatencyMillis";
    public final static String PROP_JDBC_WRITE_TARGET_LATENCY_MILLIS_DEFAULT = "1000"; // 247513.3  (was 500)

    // 538096 multiply by 4 to cater for using inMemorySize
    public final static String PROP_JDBC_WRITE_MAX_BYTES_PER_BATCH = "jdbcWriteMaxBytesPerBatch";
    public final static String PROP_JDBC_WRITE_MAX_BYTES_PER_BATCH_DEFAULT = "4000000"; // 247513.3 (was 100000)

    public final static String PROP_JDBC_WRITE_MAX_TASKS_PER_BATCH = "jdbcWriteMaxTasksPerBatch";
    public final static String PROP_JDBC_WRITE_MAX_TASKS_PER_BATCH_DEFAULT = "64";

    public final static String PROP_JDBC_WRITE_RATE = "jdbcWriteRate";
    public final static String PROP_JDBC_WRITE_RATE_DEFAULT = "80"; // 247513.3 (was 50)

    // 538096 multiply by 4 to cater for using inMemorySize
    public final static String PROP_JDBC_WRITE_MAX_DISPATCHED_BYTES = "jdbcWriteMaxDispatchedBytes";
    public final static String PROP_JDBC_WRITE_MAX_DISPATCHED_BYTES_DEFAULT = "80000000";

    /*************************************************************************/
    /* Spill Dispatcher Properties */
    /*************************************************************************/

    // The number of threads used in the Spill dispatcher
    public final static String PROP_JDBC_SPILL_THREADS = "jdbcSpillThreads";
    public final static String PROP_JDBC_SPILL_THREADS_DEFAULT = "8";

    // The priority delta added to Thread.NORM_PRIORITY for Spill dispatcher threads
    public final static String PROP_JDBC_SPILL_THREAD_PRIORITY_DELTA = "jdbcSpillThreadPriorityDelta";
    public final static String PROP_JDBC_SPILL_THREAD_PRIORITY_DELTA_DEFAULT = "0";

    // The minimum number of bytes in a single Spill batch
    // 538096 multiply by 4 to cater for using inMemorySize
    public final static String PROP_JDBC_SPILL_MIN_BYTES_PER_BATCH = "jdbcSpillMinBytesPerBatch";
    public final static String PROP_JDBC_SPILL_MIN_BYTES_PER_BATCH_DEFAULT = "2000000";

    // The maximum number of bytes in a single Spill batch
    // 538096 multiply by 4 to cater for using inMemorySize
    public final static String PROP_JDBC_SPILL_MAX_BYTES_PER_BATCH = "jdbcSpillMaxBytesPerBatch";
    public final static String PROP_JDBC_SPILL_MAX_BYTES_PER_BATCH_DEFAULT = "4000000";

    // The maximum number of tasks in a single Spill batch
    public final static String PROP_JDBC_SPILL_MAX_TASKS_PER_BATCH = "jdbcSpillMaxTasksPerBatch";
    public final static String PROP_JDBC_SPILL_MAX_TASKS_PER_BATCH_DEFAULT = "64";

    // Tha maximum number of bytes dispatched by a single worker thread
    // 538096 multiply by 4 to cater for using inMemorySize
    public final static String PROP_JDBC_SPILL_MAX_DISPATCHED_BYTES_PER_THREAD = "jdbcSpillMaxDispatchedBytesPerThread";
    public final static String PROP_JDBC_SPILL_MAX_DISPATCHED_BYTES_PER_THREAD_DEFAULT = "10000000";

    // Whether message references should be sized using the size of the message they reference
    public final static String PROP_JDBC_SPILL_SIZE_MSG_REFS_BY_MSG_SIZE = "jdbcSpillSizeMsgRefsByMsgSize";
    public final static String PROP_JDBC_SPILL_SIZE_MSG_REFS_BY_MSG_SIZE_DEFAULT = "false";

    /*************************************************************************/
    /* Data Store Locking Properties */
    /*************************************************************************/

    // Parameter for DB locking enabled flag
    public static final String PROP_LOCK_DISABLED = "jdbcDisableDataStoreLock";
    public static final String PROP_LOCK_DISABLED_DEFAULT = "false";

    // Parameter for timeout delay when acquiring DB lock
    public static final String PROP_LOCK_WAIT_TIMEOUT = "jdbcDataStoreLockPatienceDelay";
    public static final String PROP_LOCK_WAIT_TIMEOUT_DEFAULT = "5000"; // Milliseconds

    // Parameter to make ME wait before failover, number of seconds to wait before retrying
    public static final String PROP_DB_LOCK_RETRY_INTERVAL = "jdbcDataStoreDBLockRetryInterval";
    public static final String PROP_DB_LOCK_RETRY_INTERVAL_DEFAULT = "20000"; // Milliseconds

    // It will be unlikely to consider the db lock retry interval to be set less than 10 seconds,
    // since this will make the lock and update to run very frequently. Hence setting a value
    // to minimum of 10 seconds
    public static final int PROP_DB_LOCK_RETRY_INTERVAL_MINIMUM = 10000; // Milliseconds - 724624

    // Number of times to retry
    public static final String PROP_DB_LOCK_RETRY_COUNT = "jdbcDataStoreDBLockRetryCount";
    public static final String PROP_DB_LOCK_RETRY_COUNT_DEFAULT = "3"; // Milliseconds

    // Parameter for delay when refreshing DB lock
    public static final String PROP_KEEP_ALIVE_WAIT = "jdbcDataStoreLockRefreshDelay";
    public static final String PROP_KEEP_ALIVE_WAIT_DEFAULT = "20000"; // Milliseconds

    // Defect 549495
    // If this property is set to true then the DB locking thread
    // will log a local error when connection to the database is lost
    // instead of attempting to reconnect. As of v7.0 by default this
    // is set to true to failover as soon as we lose our connection to
    // the database.
    public static final String PROP_FAILOVER_ON_DB_CONNECTION_LOST = "jdbcFailoverOnDBConnectionLoss";
    public static final String PROP_FAILOVER_ON_DB_CONNECTION_LOST_DEFAULT = "true";

    // Defect 572575
    // This property is used to protect against the datastore lock being
    // inadvertantly disabled. In order to disable the datastore lock
    // this property must be set to true AND a call must be made to the
    // messaging engine MBean.
    public static final String PROP_DATASTORE_LOCK_CAN_BE_DISABLED = "jdbcDataStoreLockCanBeDisabled";
    public static final String PROP_DATASTORE_LOCK_CAN_BE_DISABLED_DEFAULT = "false";

    /*************************************************************************/
    /* Expirer Properties */
    /*************************************************************************/

    public static final String PROP_EXPIRY_INTERVAL = "expiryInterval";
    public static final String PROP_EXPIRY_INTERVAL_DEFAULT = "1000"; // Milliseconds

    public static final String PROP_CACHELOADER_INTERVAL = "cacheLoaderInterval";
    public static final String PROP_CACHELOADER_INTERVAL_DEFAULT = "60"; // Seconds

    public static final String PROP_MAX_STREAMS_PER_CYCLE = "maxStreamsPerCycle";
    public static final String PROP_MAX_STREAMS_PER_CYCLE_DEFAULT = "10";

    /*************************************************************************/
    /* DeliveryDelayManager Properties */
    /*************************************************************************/

    public static final String PROP_DELIVERY_DELAY_SCAN_INTERVAL = "deliveryDelayScanInterval";
    public static final String PROP_DELIVERY_DELAY_SCAN_INTERVAL_DEFAULT = "1000"; // Milliseconds
    
    public static final String PROP_MAXIMUM_ALLOWED_DELIVERY_DELAY_INTERVAL = "maximumAllowedDeliveryDelayInterval";
    public static final String PROP_MAXIMUM_ALLOWED_DELIVERY_DELAY_INTERVAL_DEFAULT = "-1"; // Milliseconds
    
    public static enum MaximumAllowedDeliveryDelayAction {
        warn , unlock , exception;
    }
    public static final String PROP_MAXIMUM_ALLOWED_DELIVERY_DELAY_ACTION = "maximumAllowedDeliveryDelayAction";
    public static final String PROP_MAXIMUM_ALLOWED_DELIVERY_DELAY_ACTION_DEFAULT = MaximumAllowedDeliveryDelayAction.warn.toString();
   
    /*************************************************************************/
    /* Object Manager Properties */
    /*************************************************************************/

    // Defect 496893
    public static final String PROP_OBJECT_MANAGER_RETRY_WAIT_TIME = "omStartupRetryWaitTime";
    public static final String PROP_OBJECT_MANAGER_RETRY_WAIT_TIME_DEFAULT = "5000"; // Milliseconds

    // Defect 496893
    public static final String PROP_OBJECT_MANAGER_RETRY_TIME_LIMIT = "omStartupRetryTimeLimit";
    public static final String PROP_OBJECT_MANAGER_RETRY_TIME_LIMIT_DEFAULT = "900000"; // 15 minutes

    public static final String PROP_OBJECT_MANAGER_LOG_FILE_PREFIX = "omLogFilePrefix";
    public static final String PROP_OBJECT_MANAGER_LOG_FILE_PREFIX_DEFAULT = ".";

    public static final String PROP_OBJECT_MANAGER_LOG_FILE_NAME = "omLogFileName";
    public static final String PROP_OBJECT_MANAGER_LOG_FILE_NAME_DEFAULT = "Log";

    public static final String PROP_OBJECT_MANAGER_LOG_FILE_SIZE = "omLogFileSize";
    public static final String PROP_OBJECT_MANAGER_LOG_FILE_SIZE_DEFAULT = "10485760"; //10MB

    public static final String PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_NAME = "omPermanentStoreFileName";
    public static final String PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_NAME_DEFAULT = "PermanentStore";

    public static final String PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_PREFIX = "omPermanentStoreFilePrefix";
    public static final String PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_PREFIX_DEFAULT = ".";

    public static final String PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_MINIMUM_SIZE = "omPermanentStoreFileSizeMin";
    public static final String PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_MINIMUM_SIZE_DEFAULT = "20971520"; //20MB

    public static final String PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_MAXIMUM_SIZE = "omPermanentStoreFileSizeMax";
    public static final String PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_MAXIMUM_SIZE_DEFAULT = "20971520"; //20MB

    public static final String PROP_OBJECT_MANAGER_TEMPORARY_STORE_FILE_NAME = "omTemporaryStoreFileName";
    public static final String PROP_OBJECT_MANAGER_TEMPORARY_STORE_FILE_NAME_DEFAULT = "TemporaryStore";

    public static final String PROP_OBJECT_MANAGER_TEMPORARY_STORE_FILE_PREFIX = "omTemporaryStoreFilePrefix";
    public static final String PROP_OBJECT_MANAGER_TEMPORARY_STORE_FILE_PREFIX_DEFAULT = ".";

    public static final String PROP_OBJECT_MANAGER_TEMPORARY_STORE_FILE_MINIMUM_SIZE = "omTemporaryStoreFileSizeMin";
    public static final String PROP_OBJECT_MANAGER_TEMPORARY_STORE_FILE_MINIMUM_SIZE_DEFAULT = "20971520"; //20MB

    public static final String PROP_OBJECT_MANAGER_TEMPORARY_STORE_FILE_MAXIMUM_SIZE = "omTemporaryStoreFileSizeMax";
    public static final String PROP_OBJECT_MANAGER_TEMPORARY_STORE_FILE_MAXIMUM_SIZE_DEFAULT = "20971520"; //20MB

    public static final String PROP_OBJECT_MANAGER_STORE_FULL_WAIT_FOR_CHECKPOINT = "storeFullWaitForCheckPoint";
    public static final String PROP_OBJECT_MANAGER_STORE_FULL_WAIT_FOR_CHECKPOINT_DEFAULT = "false";

    // Defect 533310
    // These properties will allow users to change the size of the object
    // cache used by the object stores to avoid reading from disk.
    public static final String PROP_OBJECT_MANAGER_PERMANENT_STORE_CACHE_SIZE = "omPermanentStoreCacheSize";
    public static final String PROP_OBJECT_MANAGER_PERMANENT_STORE_CACHE_SIZE_DEFAULT = "1000";

    public static final String PROP_OBJECT_MANAGER_TEMPORARY_STORE_CACHE_SIZE = "omTemporaryStoreCacheSize";
    public static final String PROP_OBJECT_MANAGER_TEMPORARY_STORE_CACHE_SIZE_DEFAULT = "1000";

    // Defect 601995
    // This property will allow the UUID checking in the file store to be
    // turned on/off so that a customers log file can be started up easily.
    public static final String PROP_OBJECT_MANAGER_DISABLE_UUID_CHECK = "omDisableUUIDCheck";
    public static final String PROP_OBJECT_MANAGER_DISABLE_UUID_CHECK_DEFAULT = "false";

    /*************************************************************************/
    /* Other Properties */
    /*************************************************************************/

    /**
     * This property, if present, will cause the message store to dump its raw xml
     * representation to a file at startup. This will be done before start is
     * complete, so may allow a dump of a malformed persistence before it confuses
     * the message store. The value of the parameter must specify a writable file.
     */
    public static final String PROP_DUMP_RAW_XML_ON_STARTUP = "dumpDataStoreOnStartup";

    /**
     * This property, if set to "true", will cause the DataStore to use DELETE rather
     * than TRUNCATE TABLE at startup.
     */
    public static final String PROP_USE_DELETE_INSTEAD_OF_TRUNCATE_AT_STARTUP = "jdbcUseDeleteInsteadOfTruncateAtStartup";
    public static final String PROP_USE_DELETE_INSTEAD_OF_TRUNCATE_AT_STARTUP_DEFAULT = "false";

    /**
     * PM11814
     * This property, if set to "true", will allow access to the database to use synonyms.
     */
    public static final String PROP_ALLOW_SYNONYM_USE = "jdbcAllowSynonymUse";
    public static final String PROP_ALLOW_SYNONYM_USE_DEFAULT = "false";

    /*************************************************************************/
    /* Persistence Layer Constants */
    /*************************************************************************/
    public static final String START_MODE = "STARTMODE"; // F008622--start
    public static final String DEFAULT_START_MODE = "NORMAL"; //F008622-end
    public static final String DEFAULT_DATABASE_NAME = "SIBDB";
    public static final String DEFAULT_STOGROUP_NAME = "SIBSG";
    public static final String DEFAULT_BUFPOOL_NAME = "BP1";
    public static final String DEFAULT_SCHEMA_NAME = "IBMWSSIB";
    public static final String DEFAULT_USER_NAME = "IBMUSER";
    public static final String DEFAULT_VCAT_NAME = "DSNSIBDS";
    public static final String DEFAULT_TS_PREFIX = "DUMMY";
    public static final String DEFAULT_CREATE_DB_STMT = "true";

    public static final String CLASSMAP_TABLE_NAME = "SIBCLASSMAP";
    public static final String LISTING_TABLE_NAME = "SIBLISTING";
    public static final String XACTS_TABLE_NAME = "SIBXACTS";
    public static final String KEYS_TABLE_NAME = "SIBKEYS";
    // PK35226
    // We now need two tables for the locking algorithm
    public static final String ME_INNER_TABLE_NAME = "SIBOWNER";
    public static final String ME_OUTER_TABLE_NAME = "SIBOWNERO";
    public static final String TABLE_PREFIX = "SIB";

    /**
     * The default number of {@link ItemTable#STREAM} tables that are allowed.
     */
    public static final int NUMBER_OF_STREAM_TABLES = 1;

    /**
     * The default number of {@link ItemTable#PERMANENT} tables that are allowed.
     */
    public final static int NUMBER_OF_PERMANENT_TABLES = 1;

    /**
     * The default number of {@link ItemTable#TEMPORARY} tables that are allowed.
     */
    public final static int NUMBER_OF_TEMPORARY_TABLES = 1;

    public final static String ME_STATUS_STOPPED = "STOPPED"; //R000382
    public final static String ME_STATUS_STARTED = "STARTED"; //R000382

}
