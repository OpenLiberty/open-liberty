/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.gd;

// Import required classes.

public final class GDConfig
{

 /**
  * Version number of protocol
  */
  public static byte PROTOCOL_VERSION = 1;
  
  /**
   * Timer related parameters needed for Guaranteed delivery.
   */
  /**
   * The timer granularity in milliseconds
   */
  public static int GD_TIMER_TICK = 50;
  /**
   * The number of timers expected to be active at any instant of time.
   */
  public static int GD_EXPECTED_TIMERS = 500;
  /**
   * The number of threads to dispatch the expiry function for timers.
   */
  public static int GD_MAX_TIMER_THREADS = 5;
  /**
   * GD timer values
   */
  public static int GD_ACK_PROPAGATION_THRESHOLD = 200;
  public static int GD_RELEASE_PROPAGATION_THRESHOLD = 500;
  public static int GD_NACK_REPETITION_THRESHOLD = 6000;
  public static int GD_MAX_NACK_REPETITION_THRESHOLD = 600000;
  public static int GD_REQUESTED_FORGETTING_THRESHOLD = 500;

  // Currently the FPT is only used for pubend streams. FPT < DCT (should be)
  public static long GD_FINALITY_PROPAGATION_THRESHOLD = 550000;
  // Currently the AckExp is only used for Source streams.
  public static int GD_ACK_EXPECTED_THRESHOLD = 3000;
  public static int GD_ACK_EXPECTED_THRESHOLD_MAX = 48000;
 
  // The receiving window for messages in a SHB, in milliseconds.
  public static int GD_RECV_WINDOW = 4000;
  // the SHB will remember knowledge GD_PAST_WINDOW ms before oack, to satisfy durable subs
  public static int GD_PAST_WINDOW = 2000;
  // the maximum number of V ticks stored in a KnVTickTable in an output stream
  // and the maximum number of V ticks in a KnStream in an InternalOutputStream
  public static int MAX_V_TICKS = 1000;

  // the following is a temporary parameter for recv window nacking.
  public static long GD_RWA_CURIOSITY_THRESHOLD = 100;
  // the following is a temporary parameter for nacking ticks that have fallen into recv window
  // only if they are greater than this length (in ms)
  public static int GD_MIN_RW_NACK_SIZE = 100;
  // The maximum size of a nack sent on a broker-broker link. units in milliseconds.
  public static int GD_NACK_CHUNK_SIZE = 600;
  
  // Arbitrary constant which indicates the most messages we'll keep
  // around while waiting to determine the status of a stream.
  public static final int FLUSH_CACHE_LENGTH = 10;

  // Number of "are you flushed" rounds we'll attempt before giving up.
  public static final int FLUSH_QUERY_ATTEMPTS = 3;

  // Milliseconds between "are you flushed" queries.
  public static long FLUSH_QUERY_INTERVAL = 3000;

  // Number of "request flush" rounds we'll attempt before giving up.
  public static final int REQUEST_FLUSH_ATTEMPTS = 10;

  // Milliseconds between "request flush" rounds
  public static final long REQUEST_FLUSH_INTERVAL = 10000;

  // Milliseconds between health checks on streams
  public static final int BLOCKED_STREAM_HEALTH_CHECK_INTERVAL = 10000;

}
