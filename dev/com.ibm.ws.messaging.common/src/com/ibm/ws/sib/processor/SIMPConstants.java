/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/*
 * Note: Only put STATIC FINAL constants in this class, variables set from custom properties have
 * been moved into MessageProcessor.java so that they are MP scoped, not process scoped.
 */

package com.ibm.ws.sib.processor;

import com.ibm.websphere.sib.Reliability;

public class SIMPConstants
{
  public static final long PUBLICATION_BATCH_TIMEOUT = 1000;
  public static final int PUBLICATION_BATCH_SIZE = 100;
  public static final int NO_LOCAL_BATCH_SIZE = 100;
  public static final int EXCEPTION_RETRY_TIMEOUT = 1000;
  public static final int MAX_EXCEPTION_RETRY_TIMEOUT = 30000;

  //The percentage by which an alarm can be fired late by the MPAlarmManager
  public static final int MPAM_PERCENT_LATE = 20;

  //The size of the object pool used by the MPAlarmManager
  public static final int MPAM_ALARM_POOL_SIZE = 200;

  //The size of the object pool used by BatchedTimeoutManagers
  public static final int BTM_ENTRY_POOL_SIZE = 200;

  public static final int REALLOCATION_BATCH_SIZE = 100;

  public static final long API_MAJOR_VERSION = 6;
  public static final long API_MINOR_VERSION = 0;
  public static final String API_LEVEL_DESCRIPTION = API_MAJOR_VERSION + "." + API_MINOR_VERSION;
  public static final String RESOURCE_BUNDLE = "com.ibm.ws.sib.processor.CWSIPMessages";  // 170902
  public static final String CWSIR_RESOURCE_BUNDLE = "com.ibm.wsspi.sib.core.CWSIRMessages";
  public static final String CWSIK_RESOURCE_BUNDLE = "com.ibm.websphere.sib.CWSIKMessages";
  public static final String MP_TRACE_GROUP = com.ibm.ws.sib.utils.TraceGroups.TRGRP_PROCESSOR;
  public static final String MP_MECOMMS_TRACE_GROUP = com.ibm.ws.sib.utils.TraceGroups.TRGRP_MESSAGETRACEMECOMMS;
  public static final String TRACE_MESSAGE_RESOURCE_BUNDLE = "com.ibm.ws.sib.processor.CWSJUMessages";
  public static final String TEMPORARY_PUBSUB_DESTINATION_PREFIX = "_T";
  public static final String TEMPORARY_QUEUE_DESTINATION_PREFIX = "_Q";
  public static final String SYSTEM_DESTINATION_PREFIX = "_P";
  public static final char SYSTEM_DESTINATION_SEPARATOR = '_';
  public static final String PROXY_SYSTEM_DESTINATION_PREFIX = "SIMP.PROXY.QUEUE";
  public static final String TDRECEIVER_SYSTEM_DESTINATION_PREFIX = "SIMP.TDRECEIVER";

  // Be careful, these 2 constants MUST be changed in tandem.
  public static final String SYSTEM_DEFAULT_EXCEPTION_DESTINATION = "_SYSTEM.Exception.Destination."; // <- Careful NEEDS A DOT ON THE END
  public static final String SYSTEM_DEFAULT_EXCEPTION_DESTINATION_PREFIX = "_SYSTEM.Exception.Destination"; // <- Careful NO DOT ON THE END

  // The highest-permissable "non comms" message priority which
  // is also legal to use on an assurred or express stream.
  // This is dictated by the data structures stored in
  // classes like AbstractStreamArrayMap.
  public static final int MSG_HIGH_PRIORITY = 9;

  // The routing priority to use for control messages
  // NOTE: this is the priority passed to TRM ONLY!
  // do NOT stamp this priority into the message itself.
  public static final int CTRL_MSG_PRIORITY = 11;

  /** The threshold at which a producer becomes considered long-lived,
      when the consumer dispatcher is choosing specific consumers. */
  public static int LONG_LIVED_PRODUCER_THRESHOLD = 5;

  public static final String JS_DESTINATION_ADDRESS_FACTORY = "JsDestinationAddressFactory";
  public static final String SI_DESTINATION_ADDRESS_FACTORY = "SIDestinationAddressFactory";
  public static final String CONTROL_MESSAGE_FACTORY = "ControlMessageFactory";
  public static final String JS_MESSAGE_FACTORY = "JsMessageFactory";
  public static final String JS_MESSAGE_HANDLE_FACTORY = "JsMessageHandleFactory";
  public static final String JS_ADMIN_SERVICE = "JsAdminService";
  public static final String JS_ADMIN_FACTORY = "JsAdminFactory";
  public static final String JS_MBEAN_FACTORY = "JsMBeanFactory";

  // Access control factory
  public static final String JS_ACCESS_CHECKER_FACTORY = "SIBAccessControlFactory";

  // The AuthUtilsFactory instance for getting the AuthUtils class.
  public static final String JS_AUTH_UTILS_FACTORY = "AuthUtilsFactory";

  // The AuthUtilsFactory instance for getting the AuthUtils instance.
  public static final String JS_AUTH_UTILS_INSTANCE = "AuthUtilsInstance";

  public static final String MATCHING_INSTANCE = "Matching";

  // Selection Criteria factory
  public static final String JS_SELECTION_CRITERIA_FACTORY = "SelectionCriteriaFactory";
  public static final String UNKNOWN_UUID = "0000000000000001";

   // Since we assign data messages of all priorities and reliability in a single stream,
   // we need to pick a reliability and priority value for control messages on this stream.

  public static final Reliability CONTROL_MESSAGE_RELIABILITY = Reliability.BEST_EFFORT_NONPERSISTENT;
  public static final int CONTROL_MESSAGE_PRIORITY = 11;

  // A null value for the DME version
  public static final long NULL_DME_VERSION = -1L;
  // value to determine if put time should be set
  public static final long TIMESTAMP_REQUIRED = -1L;

  // constants representing error codes in BrowseEnd messages
  public static final int BROWSE_OK = 0; // the browse ended because there were no remaining messages in the cursor
  public static final int BROWSE_STORE_EXCEPTION = 1; // there is a problem with the Message Store
  public static final int BROWSE_OUT_OF_ORDER = 2; // the BrowseGet message was received out of order
  public static final int BROWSE_BAD_FILTER = 3; // The BrowseGet indicated a bad selector

  // constants representing status code in BrowseStatus messages
  // the browse is still alive at the remote ME
  public static final int BROWSE_ALIVE = 0;

  // the browse session has been closed at the remote ME
  public static final int BROWSE_CLOSE = 1;

  // Remote Get constants
  // The value of prevTick indicating that there is no known previous tick in priority and qos order
  public static final long UNKNOWN_PREV_TICK = -1L;

  // An infinite timeout value. A zero timeout is represented as 0L
  public static final long INFINITE_TIMEOUT = -1L;

  // If the round trip time calculated increases above this value then revert to the max value
  public static final long ROUND_TRIP_TIME_HIGH_LIMIT = 30000;

  // Other RCD prefetch tuning parameters
  // Let W be the size of the prefetch window (See the RCD code and comments for how W is computed).
  // This is the total number of messages we want to be in the process of prefetching or have already prefetched but
  // not yet locked (i.e. the consumer has not begun consuming them). Let x be the current value of this sum. When
  // x < W we could prefetch more messages. Instead of prefetching as soon as x < W, we prefetch only
  // when (W - x)/W > MIN_PREFETCH_SIZE. This is to batch prefetch (ControlRequest) messages.
  public static final double MIN_PREFETCH_SIZE = 0.25;

  // Other parameters at the AIH

  // Decision events for RemoteConsumerDispatcher.resolve
  // The message for this tick was lost
  public static final int LOST_DATA = 0;

  // There was no available message
  public static final int NO_DATA = 1;

  // repetition threshold for unsuccessful writes to the Message Store
  public static final int MS_WRITE_REPETITION_THRESHOLD = 4;

  // Time interval (in milliseconds) for recording a debug event indicating
  // we are waiting for a flush on a deleted destination to terminate.  We also
  // use this timer for repeating "are you flushed" queries when a localization
  // is waiting for a source to complete a flush.
  public static final int LOG_DELETED_FLUSH_WAIT = 3000;

  public static String PROBE_ID = "1:200:1.108";

  // Constant strings for Runtime States Transmit Messages
  public static final String COMPLETE_STRING = "Complete";
  public static final String COMMITTING_STRING = "Committing";
  public static final String PENDINGSEND_STRING = "Pending Send";
  public static final String PENDINGACK_STRING = "Pending Acknowledgement";

  // Queued Messages
  public static final String LOCKED_STRING = "LOCKED";
  public static final String UNLOCKED_STRING = "UNLOCKED";
  public static final String PENDING_RETRY_STRING = "PENDING_RETRY";
  public static final String BLOCKED_STRING = "BLOCKED";
  public static final String COMMIT_STRING = "COMMITTING";
  public static final String REMOVE_STRING = "REMOVING";
  public static final String REMOTE_LOCKED_STRING = "REMOTE_LOCKED";

  // Received Messages
  public static final String AWAITINGDEL_STRING = "Awaiting Delivery";

  // Transmit Message Requests and Remote Message Requests
  public static final String REQUEST_STRING = "Request";
  public static final String PENDINGACKMR_STRING = "Pending_Acknowledgement";
  public static final String ACKNOWLEDGED_STRING = "Acknowledged";
  public static final String REMOVING_STRING = "Removing";
  public static final String REJECT_STRING = "Reject";
  public static final String VALUE_STRING = "Value";
  public static final String LOCKED_MR_STRING = "Locked";
  public static final String COMPLETED_STRING = "Completed";

  // Runtime controllables getMessageIterator paramter constant
  // Provide this to receive all msgs as an iterator
  public static final int SIMPCONTROL_RETURN_ALL_MESSAGES = -1;

  public static final String PRODUCT_NAME = "Service_Integration_Bus_";
  
  public static final String HEALTH_STATE_MSG_KEY = "HEALTH_STATE_MESSAGE_CWSJU020";

  public static final long HEALTH_QHIGH_TIMEOUT = 5000;

  // Persisted target name of a ptop target stream 
  public static final String PTOP_TARGET_STREAM = "_PTOP_TARGET_STREAM";
  public static final String UNKNOWN_TARGET_DESTINATION = "UNKNOWN";
  public static final String DEFAULT_CONSUMER_SET = "_DEFAULT_CONSUMER_SET"; 
  
  // Constants required for SIB0163 - XD Integration. These are associated with
  // properties on a connection that describe the deployment target of the application
  // - the cell name, node name, server name and cluster name.
  public static final String DEPLOYMENT_TARGET_CELL_NAME = "CellName";
  public static final String DEPLOYMENT_TARGET_NODE_NAME = "NodeName"; 
  public static final String DEPLOYMENT_TARGET_APPLICATION_SERVER_NAME = "AppServerName"; 
  public static final String DEPLOYMENT_TARGET_CLUSTER_NAME = "ClusterName"; 
  
  // (510343) How quickly we repeat the warning about blocked messages
  public static final int BLOCKED_STREAM_REPEAT_INTERVAL = 300000;
  // (510343) How quickly we repeat the warning about excessive repeated messages
  public static final int REPEATED_VALUE_WARNING_INTERVAL = 300000;
  
  // 558352 - How often we attempt an attach to a DME which isnt responding (due to deadlock?)
  public static final long GATHERING_NO_RESPONSE_ATTACH_INTERVAL = 1000;
  public static final long GATHERING_ANYCAST_RESPONSE_INTERVAL = 10000;
  public static final long ANYCAST_RESPONSE_INTERVAL = 300000;
  
  //Liberty change: distinguishing types of connections.. in Liberty they might be need to closed separately.
  public static final int MP_INPROCESS_CONNECTION = 1;
  public static final int MP_VIACOMMS_CONNECTION = 2;
  public static final int MP_VIACOMMSSSL_CONNECTION = 3;
}
