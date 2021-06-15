/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.impl;

import java.util.Hashtable;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.gd.GDConfig;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.kernel.service.util.CpuInfo;

public final class CustomProperties
{
  

  private static final TraceComponent tc = SibTr.register(CustomProperties.class,SIMPConstants.MP_TRACE_GROUP,SIMPConstants.RESOURCE_BUNDLE);



  // The default search results Object Pool size

  private int _searchResultsPoolSize = 20;

  // 368006
  // The anycast batching was disabled a long time ago, however, the alarm that completes batches
  // was not disabled at the same time. Without batching it serves no purpose other than to drown
  // trace in pointless information. So the batch timeout are also set to zero.
  
  // The batch size and maximum time delay for the asynchronous persistent updates in
  //  the anycast (remote get) protocol. Applies to both the DME and RME. The ANYCAST_BATCH_TIMEOUT
  //  + round trip time should be less than the CREATE_STREAM_REPETITION_INTERVAL, ACCEPT_REPETITION_INTERVAL,
  //  REJECT_REPETITION_INTERVAL, EAGER_GET_REQUEST_INTERVAL. Some variables moved to MessageProcessor.java
  private long _anycast_batch_timeout = 0;
  private int _anycast_batch_size = 0;
  
  // The batch size and maximum time delay for the asynchronous persistent updates in
  // the anycast (remote get) protocol. Applies only for persisting locks and value ticks in the DME
  private long _anycast_lock_batch_timeout = 0;
  private int _anycast_lock_batch_size = 0;

  // Value to indicate the maximum size of the Consumer Thread Pool

  private int _max_consumer_threadpool_size = 100;

  // Value to indicate the maximum size of the System Thread Pool

  private int _max_system_threadpool_size = 5;
  // Value to indicate the maximum size of the Reconstitute Thread Pool
  //The deault value is numberofcores*2
  private int _max_reconstitute_threadpool_size = CpuInfo.getAvailableProcessors().get();

  // Browse Protocol Liveness Parameters. Should be configurable

  // The timeout for a browse at the DME.
  // The remote ME should send BrowseStatus messages with status=BROWSE_ALIVE more frequently than this.

  private long _browse_expiry_timeout = 10000;

  // The timeout for browse liveness at the RME.
  // After the defined amount of time of inactivity, send a BrowseStatus with BROWSE_ALIVE to the DME.

  private long _browse_liveness_timeout = 9000;

  // The timeout for a browse get at the RME.
  // A message satisfying a browse get should arrive sooner than this.

  private long _browse_get_timeout = 4000;

  // Remote Get Protocol Liveness Parameters. Should be configurable

  // The interval for repeating RequestHighestGeneratedTick.

  private long _init_repetition_interval = 3000;

  // The interval for repeating ResetRequestAck

  private long _reset_repetition_interval = 3000;

  // The interval for repeating DecisionExpected

  private long _decision_expected_repetition_interval = 5000;

  // The AOH inactivity timeout after which the AIH sends ControlAreYouFlushed

  private long _sender_inactivity_timeout = 5000;

  // The interval for eagerly repeating Request

  private long _eager_get_request_interval = 5000;

  // The threshold for sending initial Accept

  private long _accept_initial_threshold = 500;

  // The interval for repeating Accept

  private long _accept_repetition_interval = 5000;

  // The interval for repeating Reject

  private long _reject_repetition_interval = 5000;

  // The interval for repeating CreateStream

  private long _create_stream_repetition_interval = 5000;

  // This is a rough estimate; 0L is not good because when it's assigned to the timeout in the NO_WAIT case at the
  // remote consumer key it yields an infinite timeout.

  private long _init_round_trip_time = 2000;

  // parameters at the RCD that interact with liveness parameters
  // this is the time interval for which a message has to be in the unlocked state, before it is rejected.
  // It should be less than DECISION_EXPECTED_REPETITION_THRESHOLD + RTT to avoid wasteful DecisionExpected messages.
  // Note that RTT is not configurable, so ideally these should be adjusted based on the observed RTT. That is TBD.
  // Also note that this parameter interacts with prefetching, as described next

  private long _unlocked_reject_interval = 3000;

  // This is the upper bound on the size of the prefetch window

  private int _max_prefetch_window = 20;

  // Non-protocol Performance parameters at the AOH
  // The idle time after which an AOConsumerKey will be closed

  private long _ck_idle_timeout = 5000;

  // If the waitTime of a message is found to be less than this value
  // then the waitTime field is not actually set.
  // This is because setting the wait time in a message has a significant
  // cost and therefore should only be performed if the value is significant.
  // This value can be tuned via the MessageProcessor custom properties.

  private long _message_wait_time_granularity = 500;

  // Default Multicast Properties

  private boolean _multicastEnabled = false;
  private String _multicastGroupAddress = "234.6.17.92";
  private String _multicastInterfaceAddress = "none";
  private int _multicastPort = 34343;
  private int _multicastPacketSize = 7000;
  private int _multicastTTL = 1;
  private boolean _multicastUseReliableRMM = true;

  //248030.1.1 need to be able to set this
  //value for admin testing purposes

  private long _target_batch_timeout = 100;
  private int _target_batch_size = 10;
  private long _source_batch_timeout = 100;
  private int _source_batch_size = 10;
  
  // If the round trip time calculated falls below this value then revert to the
  // initialized value - INIT_ROUND_TRIP_TIME

  private long _round_trip_time_low_limit = 50;

  // When ConsumerCardinality=1, if the DME does not receive messages on an existing stream for this interval,
  // and MPIO states that this RME is unreachable, the DME will initiate the stream to be flushed and allow
  // another consumer to connect.

  private long _remote_consumer_cardinality_inactivity_interval = 20000;

  // Used when a message has been rolled back beyond its retry count
  // and no exception destination is available to redirect to. The message
  // will retry the consumer every x milliseconds, where x is the timeout.

  private long _blocked_retry_timeout = 5000;
  
  // The length of time that a message may remain hidden. After this time it will become unhidden and 
  // therefore re-tried by the MDB. This protects us from having messages hidden indefinitely if a 
  // message has been hidden and then no more messages come into the system to allow that message to 
  //be unhidden or moved to the exception destination. 

  private long _pending_retry_timeout = 5000;
  
  // The defined send window for all source streams on this MP

  // Changed under defect 535484
  private int _definedSendWindow = 50;

  // The percentage value increase that pub sub remoteQueueHighLimit
  // is compared to the queueHighLimit.
  // See defect 281311.

  private double _remote_queue_high_percentage_excess = 0.10;

  // If the dest low limit is now set then this will be set to
  // %DEST_LOW_UNSET_PERCENTAGE_DECREASE lower than the dest
  // high limit
  // This value must be between 0 and 0.99

  private double _dest_low_unset_percentage_decrease = 0.10;

  // Initial value of the message reference expiry timeout in milli seconds.
  //
  // This timeout indicates when we will remove the cached jsmessage
  // from the MP objects that hold it (currently the MessageItem and the
  // LMEMessage).
  //
  // A value of 0 (zero) means the timeout is turned off.

  private long _message_reference_expiry_value = 10000;

  //if _force_pev is not null and TRUE then MP will behave as if it always has MQ Locs
  //if _force_pev is not null and FALSE then MP will behave as if it never has MQ Locs
  //if _force_pev is null then MP will behave as normal

  private Boolean _force_pev = null;

  // Remote Get constants

  // The value of prevTick indicating that there is no known previous tick in priority and qos order

  private long _unknown_prev_tick = -1L;

  // An infinite timeout value. A zero timeout is represented as 0L

  private long _infinite_timeout = -1L;

  // The interval for slowly repeating Request

  private long _slowed_get_request_interval = 10 * _eager_get_request_interval;

  // If the round trip time calculated increases above this value then revert to the
  // max value

  private long _round_trip_time_high_limit = 30000;

  // We will not prefetch in the RCD if the average consumption
  // interval (time between consuming two messages, if messages were immediately available) of the consumer is greater
  // than MAX_INTERVAL_FOR_PREFETCH. This value is less than UNLOCK_REJECT_INTERVAL
  // because we don't want to usually timeout and reject prefetched messages.

  private int _max_interval_for_prefetch = (int) (0.75*_unlocked_reject_interval);

  // Let W be the size of the prefetch window (See the RCD code and comments for how W is computed).
  // This is the total number of messages we want to be in the process of prefetching or have already prefetched but
  // not yet locked (i.e. the consumer has not begun consuming them). Let x be the current value of this sum. When
  // x < W we could prefetch more messages. Instead of prefetching as soon as x < W, we prefetch only
  // when (W - x)/W > MIN_PREFETCH_SIZE. This is to batch prefetch (ControlRequest) messages.

  private double _min_prefetch_size = 0.25;

  // Maximum frp depth. Can be customized.

  private int _max_frp_depth = 20;

  // SIB0113
  // When an IME restarts, we may have AOValues that refer to AIMessageItems - these will have
  // been removed after restart. We have a timer that uses the following value to repeat
  // the creation of a request msg to retrieve the AIMsgItem from the DME
  private long _restore_repetition_interval = 1000L;

  // Interval for repeating AnycastInputHandler flushes
  private volatile long _request_flush_repetition_interval = 5000;
  private volatile long _request_flush_slow_repetition_interval = 120000;
  
  // A gathering consumer will attempt to retrieve messages from a queue point on the
  // ME it is connected to, plus any remote queue points on the cluster. The following 
  // value is a percentage of how many messages it attempts to retrieve from the local
  // queue point over the remote ones. This could be increased if messages are known to always
  // be available on the local queue point or if performance was suffering due to too many
  // attempts to retrieve messages from remote MEs.
  private int _gathering_local_weighting = 80; //percent
  
  // If TRM tells us that a remote ME has become available in a cluster and we already
  // have a gathering consumer receiving from the cluster then the consumer needs to 
  // attach to the new partition and start reading from it. If this attach fails (even
  // though TRM told us it was up) we retry with the following interval.
  private long _gathering_reattach_interval = 5000;  // ms
  
  // By default we'll wait one minute before warning about prepared transactions blocking messages
  // But we'll still check every 10 seconds so that the link state is up to date.
  // (510343)
  private int BLOCKED_STREAM_INTERVAL = 60000;
  private int BLOCKED_STREAM_CHECK_INTERVAL = GDConfig.BLOCKED_STREAM_HEALTH_CHECK_INTERVAL; // 10 seconds
  private int BLOCKED_STREAM_RATIO = BLOCKED_STREAM_INTERVAL / BLOCKED_STREAM_CHECK_INTERVAL; // 6

  // By default we warn in the log when a link's xmitq or a destination's (remote) message point
  // is full
  // (510343)
  private boolean OUTPUT_LINK_THRESHOLD_EVENTS_TO_LOG = true;
  private boolean OUTPUT_DESTINATION_THRESHOLD_EVENTS_TO_LOG = true; 
  
  // We don't automatically spit out informationals when queues/links reach certain depths, but
  // it can be enabled
  // (510343)
  private int LOG_ALL_MESSAGE_DEPTH_INTERVALS = 0;
  // A list of individual destinations can be enabled/disabled/set to a different interval, rather
  // than all destinations being the same
  private Hashtable<String, Long> _logMessageDepthIntervals = new Hashtable<String, Long>();
  
  // Unanswered nacks are not reported by default
  // (510343)
  private long NACK_LOG_INTERVAL = 0;
  
  // Any number of re-sent messages are not reported by default
  // (510343)
  private int REPEATED_VALUE_PERCENTAGE = 0;
  // ...if they were to be reported we'd use a default interval of 2000 messages as our sample period
  private int REPEATED_VALUE_INTERVAL = 2000;

  // Delay (in miliseconds) before a new gap is NACKed by a GuaranteedTargetStream
  // (moved from GDConfig in 510343)
  private int GD_GAP_CURIOSITY_THRESHOLD = 200;

  // (F001731)
  // Period before a blocked consumer warning is re-issued (in seconds)
  public int MSG_BLOCK_WARNING_REPEAT_INTERVAL = 300; // 5 minutes
  
  public void setProperty (String key, String value) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "setProperty","key="+key+",value="+value);

    try
    {
      if (key.equals("sib.processor.searchResultsPoolSize"))
      {
        _searchResultsPoolSize = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.anycastLockBatchTimeout"))
      {
        _anycast_lock_batch_timeout = Long.parseLong(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.anycastLockBatchSize"))
      {
        _anycast_lock_batch_size = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.anycastBatchTimeout"))
      {
        _anycast_batch_timeout = Long.parseLong(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.anycastBatchSize"))
      {
        _anycast_batch_size = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.maxThreadPoolSize"))
      {
        _max_consumer_threadpool_size = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.maxSystemThreadPoolSize"))
      {
        _max_system_threadpool_size = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if(key.equals("sib.processor.maxReconstituteThreadpoolSize"))
      {
        _max_reconstitute_threadpool_size = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.browseExpiryTimeout"))
      {
        _browse_expiry_timeout = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.browseLivenessTimeout"))
      {
        _browse_liveness_timeout = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.browseGetTimeout"))
      {
        _browse_get_timeout = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.initRepetitionInterval"))
      {
        _init_repetition_interval = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.resetRepetitionInterval"))
      {
        _reset_repetition_interval = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.decisionExpectedRepetitionInterval"))
      {
        _decision_expected_repetition_interval = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.senderInactivityTimeout"))
      {
        _sender_inactivity_timeout = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.eagerGetRequestInterval"))
      {
        _eager_get_request_interval = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.slowed_get_request_interval"))
      {
        _slowed_get_request_interval = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.acceptInitialThreshold"))
      {
        _accept_initial_threshold = Long.parseLong(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.acceptRepetitionInterval"))
      {
        _accept_repetition_interval = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.rejectRepetitionInterval"))
      {
        _reject_repetition_interval = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.createStreamRepetitionInterval"))
      {
        _create_stream_repetition_interval = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.initRoundTripTime"))
      {
        _init_round_trip_time = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.unlockedRejectInterval"))
      {
        _unlocked_reject_interval = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.maxPrefetchWindow"))
      {
        _max_prefetch_window = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.idleTimeout"))
      {
        _ck_idle_timeout = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.messageWaitTimeGranularity"))
      {
        _message_wait_time_granularity = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      // Multicast Properties
      else if (key.equals("multicastEnabled"))
      {
        _multicastEnabled = Boolean.valueOf(value).booleanValue();
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("multicastInterface"))
      {
        _multicastInterfaceAddress = value;
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("multicastPort"))
      {
        _multicastPort = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("multicastPacketSize"))
      {
        _multicastPacketSize = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("multicastTTL"))
      {
        _multicastTTL = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("multicastGroupAddress"))
      {
        _multicastGroupAddress = value;
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("multicastUseReliableRMM"))
      {
        _multicastUseReliableRMM = Boolean.valueOf(value).booleanValue();;
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.targetBatchTimeout"))
      {
        _target_batch_timeout = Long.parseLong(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.targetBatchSize"))
      {
        _target_batch_size = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.sourceBatchTimeout"))
      {
        _source_batch_timeout = Long.parseLong(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.sourceBatchSize"))
      {
        _source_batch_size = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.roundTripTimeLowLimit"))
      {
        _round_trip_time_low_limit = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.remoteConsumerInactivityTimeout"))
      {
        _remote_consumer_cardinality_inactivity_interval = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.remoteConsumerTransmitterInactivityTimeout"))
      {
        _sender_inactivity_timeout = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.blockedRetryTimeout"))
      {
        _blocked_retry_timeout = Long.parseLong(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.pendingRetryTimeout"))
      {
        long pendingRetryTimeout = Long.parseLong(value);
        // Change from the default value as long as its greater than 0.
        if(pendingRetryTimeout > 0) 
        {
          _pending_retry_timeout = pendingRetryTimeout;
           com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
        }
      }    
      else if (key.equals("sib.processor.protocolSendWindow"))
      {
        _definedSendWindow = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.remoteTopicSpaceHighExcess"))
      {
        double remoteQueueHighExcess = Double.parseDouble(value);
        if(remoteQueueHighExcess>=0){
          _remote_queue_high_percentage_excess = remoteQueueHighExcess;
          com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
        }
      }
      else if (key.equals("sib.processor.destLowUnsetPercentageDecrease"))
      {
        double destQueueLowDecrease = Double.parseDouble(value);
        if(destQueueLowDecrease>=0 && destQueueLowDecrease<1){
          _dest_low_unset_percentage_decrease = destQueueLowDecrease;
          com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
        }
      }

      
      else if (key.equals("sib.processor.maxForwardRoutingPathDepth"))
      {
        _max_frp_depth = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.messageCacheTimeout"))
      {
        _message_reference_expiry_value = Long.parseLong(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.forcePEV"))
      {
        _force_pev = Boolean.parseBoolean(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.minPrefetchSsize"))
      {
        _min_prefetch_size = Double.parseDouble(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.maxIntervalForPrefetch"))
      {
        _max_interval_for_prefetch = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.roundTripTimeHighLimit"))
      {
        _round_trip_time_high_limit = Long.parseLong(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.infiniteTimeout"))
      {
        _infinite_timeout = Long.parseLong(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.unknownPrevTick"))
      {
        _unknown_prev_tick = Long.parseLong(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.anycastRequestFlushRepetitionInterval"))
      {
        _request_flush_repetition_interval = Long.parseLong(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.anycastRequestFlushSlowRepetitionInterval"))
      {
        _request_flush_slow_repetition_interval = Long.parseLong(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.gatheringLocalPercentage"))
      {
        _gathering_local_weighting = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      else if (key.equals("sib.processor.gatheringReattachInterval"))
      {
        _gathering_reattach_interval = Long.parseLong(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      // blockedCommittingMessageInterval defines the period of time (in milliseconds)
      // that a transaction can remain in 'committing' state (the period after a
      // prepare and before a commit or rollback) before a warning is issued identifying
      // the transaction that appears to be blocked.
      // The default is a period of one minute, a value of zero disables the warnings.
      // If the message remains in this state a repeat message will be issued at most 
      // every 5 minutes (not configurable) after the first report.
      else if (key.equals("sib.processor.blockedCommittingMessageInterval"))
      {
        BLOCKED_STREAM_INTERVAL = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
        
        // Now we've changed the reporting interval we re-calculate the check interval,
        // we modify the check interval so that the report interval is a multiple of
        // the check interval. We do this by allowing the check interval to vary by
        // +-50% of the GDConfig.BLOCKED_STREAM_HEALTH_CHECK_INTERVAL value (10 seconds).
        if(BLOCKED_STREAM_INTERVAL > (GDConfig.BLOCKED_STREAM_HEALTH_CHECK_INTERVAL))
        {
          BLOCKED_STREAM_RATIO = Math.round(BLOCKED_STREAM_INTERVAL /
                                            (float)GDConfig.BLOCKED_STREAM_HEALTH_CHECK_INTERVAL);
          BLOCKED_STREAM_CHECK_INTERVAL = (BLOCKED_STREAM_INTERVAL / BLOCKED_STREAM_RATIO);
        }
        else
        {
          BLOCKED_STREAM_CHECK_INTERVAL = BLOCKED_STREAM_INTERVAL;
          BLOCKED_STREAM_RATIO = 1;
        }
      }
      // Enabling logDepthThresholdEvents with result in notification events for
      // destinations or links being written to the output log of the Application Server
      // even when JMX event notification is not enabled.
      // Possible setting are:
      //    links - Log high/low transitions for any link transmission streams
      //            (SIB or WMQ)
      //    on    - Log high/low transitions for any message points, remote message points
      //            or links (SIB or WMQ) (default value)
      //    off   - No high/log events written to the log
      else if (key.equals("sib.processor.logDepthThresholdEvents"))
      {
        if(value.compareToIgnoreCase("links") == 0)
        {
          OUTPUT_LINK_THRESHOLD_EVENTS_TO_LOG = true; 
          OUTPUT_DESTINATION_THRESHOLD_EVENTS_TO_LOG = false; 
          com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
        }
        else if(value.compareToIgnoreCase("on") == 0)
        {
          OUTPUT_LINK_THRESHOLD_EVENTS_TO_LOG = true; 
          OUTPUT_DESTINATION_THRESHOLD_EVENTS_TO_LOG = true; 
          com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
        }
        else if(value.compareToIgnoreCase("off") == 0)
        {
          OUTPUT_LINK_THRESHOLD_EVENTS_TO_LOG = false; 
          OUTPUT_DESTINATION_THRESHOLD_EVENTS_TO_LOG = false; 
          com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
        }
      }
      // (510343)
      // Write a log message for every message point, remote message point or link for
      // every increment of the defined message depth.
      // For example, if set to 1000 a message will be written every time the message
      // depth of a queue point reaches a multiple of 1000 (including zero if 1000 was
      // previously reached), so an expected sequence may be: 1000, 2000, 1000, 0, 1000, ...

      // A value of zero or less does not enable messages to be logged (although
      // individual message points, remote message points, links can be enabled using
      // the next property, sib.processor.logMessageDepthIntervals.XXX).
      
      // The default setting is zero (disabled)
      else if (key.equals("sib.processor.logAllMessageDepthIntervals"))
      {
        LOG_ALL_MESSAGE_DEPTH_INTERVALS = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      // (510343)
      // sib.processor.logMessageDepthIntervals.XXX, where XXX is a destination/foreign bus
      // name, enables/disables message depth log messages to be output to the application server
      // log every increment of the defined message depth (as defined by
      // sib.processor.logAllMessageDepthIntervals but for individual destinations/foreign buses).
      
      // To monitor messages being transmitted from this ME via a link (SIB or WMQ) the
      // name of the DIRECT foreign bus should be used here.
      
      // Set to zero to explicitly disable logging for a particular destination/foreign bus
      
      // The setting of sib.processor.logAllMessageDepthIntervals applies to all destination/
      // foreign buses that have not been explicitly configured using this property.
      else if (key.startsWith("sib.processor.logMessageDepthIntervals."))
      {
        String destName = key.substring("sib.processor.logMessageDepthIntervals.".length());
        _logMessageDepthIntervals.put(destName, new Long(value));
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      // (510343)
      // Write a log record if a gap remains in the arriving message stream from another
      // messaging engine or bus after approximately this time (in seconds).
      
      // A value of zero or less disables this logging (disabled by default)
      else if (key.equals("sib.processor.logUnresolvedGapsInTransmissionStreams"))
      {
        NACK_LOG_INTERVAL = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      // (510343)
      // It's acceptable to receive the odd repeated value, for example a message
      // is sent from the source slightly out of order with subsequent messages
      // due to it's transaction taking longer to complete than others). So we (the
      // target) see a gap appear, nack the gap (after a few milliseconds) then the
      // message arrives anyway so we add it to the stream. Then the source receives
      // the nack and re-sends the message. When that arrives at us we just ignore it
      // becuase we already have it.
      // You'd expect this to happen occasionally but if it happens too often, especially after
      // an initial re-start period, it will add unnecessary traffic, reducing performance.
      // There are a couple of reasons this can occur:
      //  1) the initial nack is sent too fast to allow for slightly out of order
      //     streams. This is set by sib.processor.transmissionStreamGapEagerness (defaults to
      //     200 msecs)
      //  2) The end-to-end transmission time of messages is excessively long, so
      //     once a gap is introduced (by the sender overloading the socket) the gap
      //     is nacked multiple times. This is due to the nack repetition timer (NRT)
      //     popping multiple times before the re-sent message (from the first nack) is
      //     actually received and the NRT cancelled. This end-to-end delay has been seen due
      //     to excessive buffering of inbound messages in the Comms layer before being
      //     processed by us. This buffer in Comms can be configured using
      //     com.ibm.ws.sib.jfapchanel.RL_DISPATCHER_MAXQUEUESIZE. The excessive buffering
      //     is still only a symptom of the socket being overloaded with work, the thing
      //     sending all the messages needs to back off (although this is to be expected when
      //     a connection/ME is restarted)

      // This property will result in a warning message being logged if the percentage of re-sent
      // messages over other messages for any particular destination or link exceeds the setting in
      // the defined message interval (see sib.processor.repeatedMessageInterval for definition of
      // the interval).
      
      // Valid settings are 0-100, where 0 disables the checking and 100 represents 100%
      // resent messages. The default is set to 0 (disabled).
      
      // Once a warning is issued for a particulat destination or link it will not be re-issued
      // within 5 minutes even if the percentage of re-sent messages exceeds this setting during
      // that time.
      else if (key.equals("sib.processor.repeatedMessagePercentage"))
      {
        REPEATED_VALUE_PERCENTAGE = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      // (510343)
      // Define the sample size of incoming messages that the sib.processor.repeatedMessagePercentage
      // check is applied to.
      
      // For example, a percentage of 25 and an interval of 2000 would require at least 500 resent
      // messages to be received in a sample of 2000 incoming messages to produce a warning message.
      
      // The default interval is 2000 (although the default percentage is zero, so checking is disabled).
      else if (key.equals("sib.processor.repeatedMessageInterval"))
      {
        REPEATED_VALUE_INTERVAL = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      // (510343)
      // If a target stream receives a message and detects that it hasn't yet received the previous
      // message(s) it will send a NACK to the sending ME after this period, re-requesting this
      // message. This is intended to resolve gaps introduced when messages are dropped from the
      // connection (e.g. when an ME-ME connection is recycled, or when the Comms buffers are exhausted).
      
      // Due to threading and differences in the time it takes to complete the commit of a transaction
      // (this is NOT related to the time between sending a message and committing the transaction
      // though), it is legitimate to receive messages out of order on a stream. This is why the delay
      // between detecting a gap and NACKing it exists.
      
      // However, if the NACKing is too eager it will result in messages that haven't been dropped but
      // are just a 'little slow' being NACKed, this results in multiple copies of the same message
      // being sent. This is inefficient and should be tuned out of the system.
      
      // WARNING: The longer the delay, the longer it will take to fill genuine gaps in the stream, possibly
      // degrading performance.
      
      // The default delay is 200 milliseconds
      else if (key.equals("sib.processor.transmissionStreamGapEagerness"))
      {
        GD_GAP_CURIOSITY_THRESHOLD = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
      // (F001731)
      // If a destination has been enabled to report warnings when a consumer has been blocked
      // on its maxActiveMessages setting for a configured period of time (using the destination context
      // property of 'MsgBlockWarningInterval'), this custom property sets the frequency that the
      // warning is re-issued while the block still exists (in seconds).

      // The default period is 300 seconds (5 minutes)
      else if (key.equals("sib.processor.msgBlockWarningRepeatInterval"))
      {
        MSG_BLOCK_WARNING_REPEAT_INTERVAL = Integer.parseInt(value);
        com.ibm.ws.sib.utils.Runtime.changedPropertyValue(key, value);
      }
    }
    catch(Exception e)
    {
      // No FFDC code needed
      
      // We've had a problem setting a custom property, probably due to an invalid value
      // (e.g. trying to fit a long into an int).
      
      // TODO This shouldn't be an 'internal error', as it's probably user error, but we're
      // at a stage where we can't add new warning messages, so this'll have to do for now.

      // Log to console
      SibTr.error(
        tc,
        "INTERNAL_MESSAGING_ERROR_CWSIP0008",
        new Object[] {
            "com.ibm.ws.sib.processor.impl.CustomProperties.setProperty",
            "1:944:1.8.1.16",
            "(Key: " + key + " Value: " + value + ")",
            e});
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "setProperty");
  }

  public int get_searchResultsPoolSize () {
    return _searchResultsPoolSize;
  }

  public int get_anycast_lock_batch_size () {
    return _anycast_lock_batch_size;
  }

  public long get_anycast_lock_batch_timeout () {
    return _anycast_lock_batch_timeout;
  }

  public int get_anycast_batch_size () {
    return _anycast_batch_size;
  }

  public long get_anycast_batch_timeout () {
    return _anycast_batch_timeout;
  }

  public long get_blocked_retry_timeout () {
    return _blocked_retry_timeout;
  }
  
  public long get_pending_retry_timeout () {
    return _pending_retry_timeout;
  }
  
  public long get_infinite_timeout () {
    return _infinite_timeout;
  }

  public long get_unlocked_reject_interval () {
    return _unlocked_reject_interval;
  }

  public double get_dest_low_unset_percentage_decrease () {
    return _dest_low_unset_percentage_decrease;
  }

  public double get_remote_queue_high_percentage_excess () {
    return _remote_queue_high_percentage_excess;
  }

  public long get_create_stream_repetition_interval () {
    return _create_stream_repetition_interval;
  }

  public long get_init_round_trip_time ()  {
    return _init_round_trip_time;
  }

  public long get_sender_inactivity_timeout () {
    return _sender_inactivity_timeout;
  }

  public long get_round_trip_time_low_limit () {
    return _round_trip_time_low_limit;
  }

  public long get_round_trip_time_high_limit () {
    return  _round_trip_time_high_limit;
  }

  public long get_unknown_prev_tick () {
    return _unknown_prev_tick;
  }

  public long get_eager_get_request_interval () {
    return _eager_get_request_interval;
  }

  public long get_slowed_get_request_interval () {
    return _slowed_get_request_interval;
  }

  public long get_accept_initial_threshold () {
    return _accept_initial_threshold;
  }

  public long get_accept_repetition_interval () {
    return _accept_repetition_interval;
  }

  public long get_reject_repetition_interval () {
    return _reject_repetition_interval;
  }

  public long get_decision_expected_repetition_interval () {
    return _decision_expected_repetition_interval;
  }

  public long get_init_repetition_interval () {
   return _init_repetition_interval;
  }

  public long get_reset_repetition_interval () {
    return _reset_repetition_interval;
  }

  public int get_max_interval_for_prefetch () {
    return _max_interval_for_prefetch;
  }

  public int get_max_prefetch_window () {
    return _max_prefetch_window;
  }

  public long get_remote_consumer_cardinality_inactivity_interval () {
    return _remote_consumer_cardinality_inactivity_interval;
  }

  public long get_ck_idle_timeout () {
    return _ck_idle_timeout;
  }

  public long get_message_wait_time_granularity () {
    return _message_wait_time_granularity;
  }

  public long get_message_reference_expiry_value () {
    return _message_reference_expiry_value;
  }

  public int get_max_frp_depth () {
    return _max_frp_depth;
  }

  public long get_target_batch_timeout () {
    return _target_batch_timeout;
  }

  public int get_target_batch_size () {
    return _target_batch_size;
  }

  public long get_source_batch_timeout () {
    return _source_batch_timeout;
  }

  public int get_source_batch_size () {
    return _source_batch_size;
  }

  public int get_max_consumer_threadpool_size () {
    return _max_consumer_threadpool_size;
  }

  public int get_max_system_threadpool_size () {
    return _max_system_threadpool_size;
  }
  
  public int get_max_reconstitute_threadpool_size () {
    return _max_reconstitute_threadpool_size;
  }

  public long get_browse_expiry_timeout () {
    return _browse_expiry_timeout;
  }

  public long get_browse_liveness_timeout () {
    return _browse_liveness_timeout;
  }

  public long get_browse_get_timeout () {
    return _browse_get_timeout;
  }

  public boolean get_multicastEnabled () {
    return _multicastEnabled;
  }

  public String get_multicastGroupAddress () {
    return _multicastGroupAddress;
  }

  public String get_multicastInterfaceAddress () {
    return _multicastInterfaceAddress;
  }

  public int get_multicastPort () {
    return _multicastPort;
  }

  public int get_multicastPacketSize () {
    return _multicastPacketSize;
  }

  public int get_multicastTTL () {
    return _multicastTTL;
  }

  public boolean get_multicastUseReliableRMM () {
    return _multicastUseReliableRMM;
  }

  public int get_definedSendWindow () {
    return _definedSendWindow;
  }

  public Boolean get_force_pev () {
    return _force_pev;
  }

  public double get_min_prefetch_size () {
    return _min_prefetch_size;
  }

  public long get_restore_repetition_interval() {
    return _restore_repetition_interval ;
  }

  public long get_request_flush_repetition_interval () {
    return _request_flush_repetition_interval;
  }
  
  public long get_request_flush_slow_repetition_interval () {
    return _request_flush_slow_repetition_interval;
  }

  public int get_gathering_local_weighting () {
    return _gathering_local_weighting;
  }
  
  public long get_gathering_reattach_interval() {
    return _gathering_reattach_interval;
  }
  
  public int getBlockedStreamFirstReportInterval()
  {
    return BLOCKED_STREAM_INTERVAL;
  }
  
  public int getBlockedStreamHealthCheckInterval()
  {
    return BLOCKED_STREAM_CHECK_INTERVAL;
  }
  
  public int getBlockedStreamRatio()
  {
    return BLOCKED_STREAM_RATIO;
  }
  
  public boolean getOutputLinkThresholdEventsToLog()
  {
    return OUTPUT_LINK_THRESHOLD_EVENTS_TO_LOG;
  }
  
  public boolean getOutputDestinationThresholdEventsToLog()
  {
    return OUTPUT_DESTINATION_THRESHOLD_EVENTS_TO_LOG;
  }
  
  public int getLogAllMessageDepthIntervals()
  {
    return LOG_ALL_MESSAGE_DEPTH_INTERVALS;
  }
  
  public Hashtable<String, Long> getLogMessageDepthIntervalsTable()
  {
    return _logMessageDepthIntervals;
  }
  
  public long getNackLogInterval()
  {
    return NACK_LOG_INTERVAL;
  }
  
  public int getRepeatedValuePercentage()
  {
    return REPEATED_VALUE_PERCENTAGE;
  }
  
  public int getRepeatedValueInterval()
  {
    return REPEATED_VALUE_INTERVAL;
  }
  
  public int getGapCuriosityThreshold()
  {
    return GD_GAP_CURIOSITY_THRESHOLD;
  }
  
  public int getMsgBlockWarningRepeat()
  {
    return MSG_BLOCK_WARNING_REPEAT_INTERVAL;
  }
}
