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
package com.ibm.ws.sib.api.jms;

/**
 * This file contains constants which are not to be used by customers, but may
 * be used inside the JMS component itself or by other product components. 
 * 
 * This class is specifically NOT tagged as ibm-spi because by definition it is not
 * intended for use by either customers or ISV's.
 * 
 * @author matrober
 */
public interface JmsInternalConstants
{
  
  // ***************************** STATE CONSTANTS *****************************

  /**
   * Used to indicate the stopped state for JMS objects.<p>
   * (Connection, Session, MessageProducer, MessageConsumer etc).
   */
  public static final int STOPPED = 1;
  
  /**
   * Used to indicate the started state for JMS objects.<p>
   * (Connection, Session, MessageProducer, MessageConsumer etc).
   */
  public static final int STARTED = 2;
  
  /**
   * Used to indicate the closed state for JMS objects.<p>
   * (Connection, Session, MessageProducer, MessageConsumer etc).
   */
  public static final int CLOSED = 3;


  // ******************* DESTINATION FACTORY PARAMETER CONSTANTS ****************
    
  /**
   * Used as the key value for the destination name property of a Destination.
   */
  public static final String DEST_NAME = "DEST_NAME";
  
  /**
   * Used as the key value for the discriminator property of a Destination.
   */ 
  public static final String DEST_DISCRIM = "DEST_DISCRIM";
  
  /**
   * Used as the key value for the producer mediation property of a Destination.
   *
  public static final String P_MEDIATION = "producerMediation";
   */
  
  /**
   * Used as the key value for the consumer mediation property of a Destination.
   *
  public static final String C_MEDIATION = "consumerMediation";
   */
  
  /**
   * Used as the key value for the browser mediation property of a Destination.
   *
  public static final String B_MEDIATION = "browserMediation";
   */
  
  /**
   * Used as the key value for the delivery mode property of a Destination.
   */
  public static final String DELIVERY_MODE = "deliveryMode";
  public static final String DELIVERY_MODE_DEFAULT = "deliveryModeDefault";
  
  /**
   * Used as the key value for the priority property of a Destination.
   */
  public static final String PRIORITY = "priority";
  public static final String PRIORITY_DEFAULT = "priorityDefault";
  
  /**
   * Used as the key value for the time to live property of a Destination.
   */
  public static final String TIME_TO_LIVE = "timeToLive";
  public static final String TIME_TO_LIVE_DEFAULT = "timeToLiveDefault";
  
  /**
   * Used as the key value for the forward routing path property of a Destination.
   */
  public static final String FORWARD_ROUTING_PATH = "forwardRP";
  
  /**
   * Used as the key value for the reverse routing path property of a Destination.
   */
  public static final String REVERSE_ROUTING_PATH = "reverseRP";
  
  /**
   * Used as the key value for the property which decides whether to set the
   * JMSDestination attribute of a message.
   */
  public static final String INHIBIT_DESTINATION = "inhibitJMSDest";
  
  /**
   * Used as the key value for the property which indicates which bus 
   * a Destination resides on.
   */
  public static final String BUS_NAME = "busName";
  
  /**
   * Used as the key value for the CF property which decides whether to share
   * durable subscriptions.
   */
  public static final String SHARE_DSUBS = "shareDurableSubscriptions";
  
  /**
    * Used as the key value for the property which indicates which bus 
    * a Destination resides on.
    */
  public static final String BLOCKED_DESTINATION = "blockedDestinationCode";
    
  /**
   * Used as the key value for the property which indicate whether access to
   * a queue is scoped to the local queue point.
   */
  public static final String SCOPE_TO_LOCAL_QP = "scopeToLocalQP";

  /**
   * Used as the key value for the property which indicate whether a producer
   * using the queue will have a preference for the local partition.
   */
  public static final String PRODUCER_PREFER_LOCAL = "producerPreferLocal";
  
  /**
   * Used as the key value for the property which indicate whether a producer
   * using this JMS Queue will bind to a single partition.
   */
  public static final String PRODUCER_BIND = "producerBind";
  
  /**
   * Used as the key value for the property which indicate whether a consumer
   * using this JMS Queue will gather messages from all the available partitions
   * of the destination.
   */
  public static final String GATHER_MESSAGES = "gatherMessages";
  
  /**
   * Used as the blocked destination code when a message is received by PSB
   * from MA88 and the broker input stream is not specified.
   */
  public static final Integer PSB_REPLY_DATA_MISSING = new Integer(1);


  
  // **************** MESSAGE TYPE CONSTANTS FOR toString **********************
  public static final String CLASS_NONE   = "jms_none";     // Null message
  public static final String CLASS_TEXT   = "jms_text";     // Text message
  public static final String CLASS_OBJECT = "jms_object";   // Object message
  public static final String CLASS_MAP    = "jms_map";      // Map message
  public static final String CLASS_STREAM = "jms_stream";   // Stream message
  public static final String CLASS_BYTES  = "jms_bytes";    // Bytes message

  
  
}
