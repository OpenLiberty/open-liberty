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

package com.ibm.websphere.sib.management;

/** 
 * Constants for applications which listen to notifications emitted from 
 * Websphere Embedded Messaging MBeans.  
 *  
 * <p>For any particular notification (e.g. TYPE_SIB_MESSAGING_ENGINE_START, 
 * which is "SIB.messaging.engine.start"), there may be a set of specific
 * properties that will be emitted together with the common properties
 * 
 * <p> The common properties comprise the following and are issued by all SIB Notifications.
 * <ul>
 * <li> KEY_THIS_BUS_NAME </li>
 * <li> KEY_THIS_BUS_UUID </li>
 * <li> KEY_THIS_MESSAGING_ENGINE_NAME </li>
 * <li> KEY_THIS_MESSAGING_ENGINE_UUID </li>
 * </ul>
 */ 

public class SibNotificationConstants
{
  /* This first group of properties is issued by all SIB Notifications */
  
  /**
   * <p> "this.bus.name".
   * 
   * <p> The value of this property is the name of the bus generating the Notification.
   */
  public static final String KEY_THIS_BUS_NAME = "this.bus.name";
  
  /**
   * <p> "this.bus.uuid".
   * 
   * <p> The value of this property is the Uuid of the bus generating the Notification.
   */  
  public static final String KEY_THIS_BUS_UUID = "this.bus.uuid";
  
  /**
   * <p> "this.messaging.engine.name".
   * 
   * <p> The value of this property is the name of the messaging engine generating the Notification.
   */  
  
  public static final String KEY_THIS_MESSAGING_ENGINE_NAME =
                            "this.messaging.engine.name";
                            
  /**
   * <p> "this.messaging.engine.uuid".
   * 
   * <p> The value of this property is the Uuid of the messaging engine generating the Notification.
   */  
  public static final String KEY_THIS_MESSAGING_ENGINE_UUID = 
                             "this.messaging.engine.uuid";

  /* This next group of properties is common to more than 1 type of
     SIB Notification */

  /**
   * <p> "remote.messaging.engine.name".
   * 
   * <p> The value of this property is the name of a remote messaging engine.
   */    
  public static final String KEY_REMOTE_MESSAGING_ENGINE_NAME = 
    "remote.messaging.engine.name";

  /**
   * <p> "remote.messaging.engine.UUID".
   * 
   * <p> The value of this property is the UUID of a remote messaging engine.
   */    
  public static final String KEY_REMOTE_MESSAGING_ENGINE_UUID = 
    "remote.messaging.engine.UUID";

  /**
   * <p> "remote.queue.manager.name".
   * 
   * <p> The value of this property is the name of the Queue Manager at the other end of the communications link.
   */     
  public static final String KEY_REMOTE_QUEUE_MANAGER_NAME = 
    "remote.queue.manager.name";      
                                                      
  /**
   * <p> "mqlink.sender.name".
   * 
   * <p> The value of this property is the name of the MQ link sender channel.
   */  
  public static final String KEY_MQLINK_SENDER_NAME = 
                             "mqlink.sender.name";                                                                  
  /**
   * <p> "mqlink.receiver.channel.name".
   * 
   * <p> The value of this property is the name of the MQ link receiver channel.
   */  
  public static final String KEY_MQLINK_RECEIVER_NAME = 
                             "mqlink.receiver.channel.name";                           
  /**
   * <p> "stop.reason".
   * 
   * <p> The value of this property is the reason code for the stop action. 
   */                                  
  public static final String KEY_STOP_REASON = "stop.reason";

  /**
   * <p> "communications.terminated".
   * 
   * <p> A reason code for a communications stop notification. 
   */      
  public static final String STOP_REASON_COMMUNICATIONS_TERMINATED = 
    "communications.terminated";

  /**
   * <p> "local.me.shutdown".
   * 
   * <p> A reason code for a communications stop notification. 
   */     
  public static final String STOP_REASON_LOCAL_ME_SHUTDOWN = 
    "local.me.shutdown";

  /**
   * <p> "requested.by.remote.qm".
   * 
   * <p> A reason code for a MQLink stop notification. 
   */  
  public static final String STOP_REASON_REQUESTED_BY_REMOTE_QM =
                             "requested.by.remote.qm";                                 

  /**
   * <p> "foreign.bus.name".
   * 
   * <p> The value of this property is the name of a foreign bus. 
   */  
  public static final String KEY_FOREIGN_BUS_NAME = "foreign.bus.name";
  
  /* TYPE_SIB_MESSAGING_ENGINE_START */
  
  /**
   * <p> "SIB.messaging.engine.start".
   * <p> This Notification type is used when the Messaging Engine is starting
   * and is associated with the SIBMain and MessagingEngine MBeans.
   * <p> In addition to the common properties, 
   * TYPE_SIB_MESSAGING_ENGINE_START has the following specific property. 
   * <ul>
   * <li> KEY_START_TYPE. </li>
   * </ul>
   * 
   * @see #KEY_START_TYPE    
   */ 
  public static final String TYPE_SIB_MESSAGING_ENGINE_START = 
                             "SIB.messaging.engine.start";
  /**
   * <p> "start.type". 
   * 
   * <p> The value of this property may be either START_TYPE_WARM or 
   * START_TYPE_FLUSH. 
   * 
   * @see #START_TYPE_WARM
   * @see #START_TYPE_FLUSH 
   */        
  public static final String KEY_START_TYPE = "start.type";

  /**
   * <p> "start.type.warm".
   * 
   * <p> A start type for a TYPE_SIB_MESSAGING_ENGINE_START notification.
   * 
   * @see #TYPE_SIB_MESSAGING_ENGINE_START 
   */  
  public static final String START_TYPE_WARM = "start.type.warm";
  
  /**
   * <p> "start.type.flush".
   * 
   * <p> A start type for a TYPE_SIB_MESSAGING_ENGINE_START notification. 
   * 
   * @see #TYPE_SIB_MESSAGING_ENGINE_START 
   */    
  public static final String START_TYPE_FLUSH= "start.type.flush";
  
  /* TYPE_SIB_MESSAGING_ENGINE_STOP */
  
  /**
   * <p> "SIB.messaging.engine.stop".
   * <p> This Notification type is used when the Messaging Engine is stopped
   * and is associated with the SIBMain and MessagingEngine MBeans.
   * <p> In addition to the common properties, 
   * TYPE_SIB_MESSAGING_ENGINE_STOP has the following specific property. 
   * <ul>
   * <li> KEY_STOP_REASON. </li>
   * </ul>  
   * 
   * @see #KEY_STOP_REASON
   */ 
  
  public static final String TYPE_SIB_MESSAGING_ENGINE_STOP = 
                             "SIB.messaging.engine.stop";

  // KEY_STOP_REASON
  
  /**
   * <p> "stop.reason.administrator.immediate".
   * 
   * <p> A stop reason for a Messaging Engine stop notification. 
   */  
  public static final String STOP_REASON_ADMINISTRATOR_IMMEDIATE = 
    "stop.reason.administrator.immediate";
    
  /**
   * <p> "stop.reason.administrator.force".
   * 
   * <p> A stop reason for a Messaging Engine stop notification. 
   */      
  public static final String STOP_REASON_ADMINISTRATOR_FORCE =
    "stop.reason.administrator.force"; 
  
  /* TYPE_SIB_SECURITY_NOT_AUTHENTICATED */
  
  /**
   * <p> "SIB.security.not.authenticated".
   * <p> This Notification type is used when the Messaging Engine cannot
   * authenticate a user. It is associated with the MessagingEngine MBean.
   * <p> In addition to the common properties, 
   * TYPE_SIB_SECURITY_NOT_AUTHENTICATED has the following specific properties. 
   * <ul>
   * <li> KEY_OPERATION. </li>
   * <li> KEY_SECURITY_USERID. </li>
   * <li> KEY_SECURITY_REASON. </li>
   * </ul>  
   * 
   * @see #KEY_OPERATION
   * @see #KEY_SECURITY_USERID
   * @see #KEY_SECURITY_REASON
   */ 
  public static final String TYPE_SIB_SECURITY_NOT_AUTHENTICATED = 
                             "SIB.security.not.authenticated";
  /**
   * <p> "operation". 
   * 
   * <p> The value of this property is a type of operation. 
   */    
  public static final String KEY_OPERATION = "operation";
  
  /**
   * <p> "operation.connect".
   * 
   * <p> A type of operation for a security notification. 
   */  
  public static final String OPERATION_CONNECT = "operation.connect";

  /**
   * <p> "security.userid". 
   * 
   * <p> The value of this property is the user identifier attempting the connection.. 
   */    
  public static final String KEY_SECURITY_USERID = "security.userid";
  
  /**
   * <p> "security.reason". 
   * 
   * <p> The value of this property may be either SECURITY_REASON_NOT_AUTHENTICATED or 
   * SECURITY_REASON_NO_USERID.
   * 
   * @see #SECURITY_REASON_NOT_AUTHENTICATED
   * @see #SECURITY_REASON_NO_USERID
   */    
  public static final String KEY_SECURITY_REASON = "security.reason";

  /**
   * <p> "security.reason.not.authenticated".
   * 
   * <p> A security reason for a TYPE_SIB_SECURITY_NOT_AUTHENTICATED notification.
   * 
   *  @see #TYPE_SIB_SECURITY_NOT_AUTHENTICATED  
   */  
  public static final String SECURITY_REASON_NOT_AUTHENTICATED =
                             "security.reason.not.authenticated";
  /**
   * <p> "security.reason.no.userid".
   * 
   * <p> A security reason for a TYPE_SIB_SECURITY_NOT_AUTHENTICATED notification.
   * 
   * @see #TYPE_SIB_SECURITY_NOT_AUTHENTICATED 
   */                               
  public static final String SECURITY_REASON_NO_USERID =
                             "security.reason.no.userid";

  /* TYPE_SIB_SECURITY_NOT_AUTHORIZED */ 
  
  /**
   * <p> "SIB.security.not.authorized".
   * <p> This Notification type is used when the Messaging Engine cannot
   * authorize a user to perform a specified operation. It is associated 
   * with the MessagingEngine MBean.
   * <p> In addition to the common properties, 
   * TYPE_SIB_SECURITY_NOT_AUTHORIZED has the following specific properties. 
   * <ul>
   * <li> KEY_SECURITY_RESOURCE_TYPE. </li>
   * <li> KEY_SECURITY_RESOURCE_NAME. </li>
   * <li> KEY_OPERATION. </li>
   * <li> KEY_SECURITY_USERID. </li>
   * <li> KEY_SECURITY_REASON. </li>
   * </ul>  
   * 
   * @see #KEY_SECURITY_RESOURCE_TYPE
   * @see #KEY_SECURITY_RESOURCE_NAME
   * @see #KEY_OPERATION
   * @see #KEY_SECURITY_USERID
   * @see #KEY_SECURITY_REASON  
   */ 
  
  public static final String TYPE_SIB_SECURITY_NOT_AUTHORIZED = 
                             "SIB.security.not.authorized";
  // KEY_SECURITY_USERID

  /**
   * <p> "security.resource.type". 
   * 
   * <p> The value of this property is either SECURITY_RESOURCE_TYPE_DESTINATION
   * or SECURITY_RESOURCE_TYPE_BUS.
   * 
   * @see #SECURITY_RESOURCE_TYPE_DESTINATION
   * @see #SECURITY_RESOURCE_TYPE_BUS
   */   
  public static final String KEY_SECURITY_RESOURCE_TYPE = "security.resource.type";

  public static final String SECURITY_RESOURCE_TYPE_DESTINATION =
                             "security.resource.type.destination";
  public static final String SECURITY_RESOURCE_TYPE_BUS =
                             "security.resource.type.bus";

  /**
   * <p> "security.resource.name". 
   * 
   * <p> The value of this property is the Name of the bus/destination.
   */   
  public static final String KEY_SECURITY_RESOURCE_NAME = 
                             "security.resource.name";

  // KEY_OPERATION
  
  // OPERATION_CONNECT
  /**
   * <p> "operation.send".
   * 
   * <p> A type of operation for a TYPE_SIB_SECURITY_NOT_AUTHORIZED notification.
   * 
   * @see #TYPE_SIB_SECURITY_NOT_AUTHORIZED 
   */    
  public static final String OPERATION_SEND = "operation.send";
  
  /**
   * <p> "operation.receive".
   * 
   * <p> A type of operation for a TYPE_SIB_SECURITY_NOT_AUTHORIZED notification.
   * 
   * @see #TYPE_SIB_SECURITY_NOT_AUTHORIZED  
   */      
  public static final String OPERATION_RECEIVE = "operation.receive";

  /**
   * <p> "operation.browse".
   * 
   * <p> A type of operation for a TYPE_SIB_SECURITY_NOT_AUTHORIZED notification.
   * 
   * @see #TYPE_SIB_SECURITY_NOT_AUTHORIZED  
   */      
  public static final String OPERATION_BROWSE = "operation.browse";
  
  /**
   * <p> "operation.create".
   * 
   * <p> A type of operation for a TYPE_SIB_SECURITY_NOT_AUTHORIZED notification.
   * 
   * @see #TYPE_SIB_SECURITY_NOT_AUTHORIZED  
   */     
  public static final String OPERATION_CREATE = "operation.create";
  
  /**
   * <p> "operation.identity.adoption".
   * 
   * <p> A type of operation for a TYPE_SIB_SECURITY_NOT_AUTHORIZED notification.
   * 
   * @see #TYPE_SIB_SECURITY_NOT_AUTHORIZED  
   */     
  public static final String OPERATION_IDENTITY_ADOPTION = 
                             "operation.identity.adoption";

  /**
   * <p> "operation.inquire".
   * 
   * <p> A type of operation for a TYPE_SIB_SECURITY_NOT_AUTHORIZED notification.
   * 
   * @see #TYPE_SIB_SECURITY_NOT_AUTHORIZED  
   */     
  public static final String OPERATION_INQUIRE = 
                             "operation.inquire";
  
  // KEY_SECURITY_REASON

  /**
   * <p> "security.reason.not.authorized".
   * 
   * <p> A security reason for a TYPE_SIB_SECURITY_NOT_AUTHORIZED notification.
   * 
   * @see #TYPE_SIB_SECURITY_NOT_AUTHORIZED  
   */  
  public static final String SECURITY_REASON_NOT_AUTHORIZED =
                             "security.reason.not.authorized";
  
  /* TYPE_SIB_COMMUNICATIONS_START */
  
  /**
   * <p> "SIB.communications.connection.start".
   * <p> This Notification type is used when an intra-bus messaging engine to
   * messaging engine connection is started. This notification is    
   * associated with the MessagingEngine MBean.
   * <p> In addition to the common properties, 
   * TYPE_SIB_COMMUNICATIONS_START has the following specific properties. 
   * <ul>
   * <li> KEY_REMOTE_MESSAGING_ENGINE_NAME. </li>
   * <li> KEY_REMOTE_MESSAGING_ENGINE_UUID. </li>
   * </ul>
   * 
   * @see #KEY_REMOTE_MESSAGING_ENGINE_NAME
   * @see #KEY_REMOTE_MESSAGING_ENGINE_UUID   
   */  

  public static final String TYPE_SIB_COMMUNICATIONS_START = 
                      "SIB.communications.connection.start";

  // KEY_REMOTE_MESSAGING_ENGINE_NAME

  // KEY_REMOTE_MESSAGING_ENGINE_UUID

  /* TYPE_SIB_COMMUNICATIONS_STOP */
  
  /**
   * <p> "SIB.communications.connection.stop".
   * <p> This Notification type is used when an intra-bus messaging engine to
   * messaging engine connection is stopped. This notification is    
   * associated with the MessagingEngine MBean.
   * <p> In addition to the common properties, 
   * TYPE_SIB_COMMUNICATIONS_STOP has the following specific properties. 
   * <ul>
   * <li> KEY_REMOTE_MESSAGING_ENGINE_NAME. </li>
   * <li> KEY_REMOTE_MESSAGING_ENGINE_UUID. </li>
   * <li> KEY_STOP_REASON. </li>
   * </ul> 
   * 
   * @see #KEY_REMOTE_MESSAGING_ENGINE_NAME
   * @see #KEY_REMOTE_MESSAGING_ENGINE_UUID
   * @see #KEY_STOP_REASON 
   */  

  public static final String TYPE_SIB_COMMUNICATIONS_STOP = 
                      "SIB.communications.connection.stop";

  // KEY_REMOTE_MESSAGING_ENGINE_NAME

  // KEY_REMOTE_MESSAGING_ENGINE_UUID

  // KEY_STOP_REASON

  // Values for stop reason:
  // STOP_REASON_COMMUNICATIONS_TERMINATED

  // STOP_REASON_LOCAL_ME_SHUTDOWN 

  /* TYPE_SIBLINK_START */
  
  /**
   * <p> "SIB.link.start".
   * <p> This Notification type is used when an inter-bus messaging engine to
   * messaging engine connection is started. This notification is    
   * associated with the GatewayLink MBean.
   * <p> In addition to the common properties, 
   * TYPE_SIBLINK_START has the following specific properties. 
   * <ul>
   * <li> KEY_REMOTE_MESSAGING_ENGINE_NAME. </li>
   * <li> KEY_REMOTE_MESSAGING_ENGINE_UUID. </li>
   * <li> KEY_FOREIGN_BUS_NAME. </li>
   * </ul> 
   * 
   * @see #KEY_REMOTE_MESSAGING_ENGINE_NAME
   * @see #KEY_REMOTE_MESSAGING_ENGINE_UUID
   * @see #KEY_FOREIGN_BUS_NAME
   */  

  public static final String TYPE_SIBLINK_START = "SIB.link.start";

  // KEY_FOREIGN_BUS_NAME
  
  // KEY_REMOTE_MESSAGING_ENGINE_NAME

  // KEY_REMOTE_MESSAGING_ENGINE_UUID

  /* TYPE_SIBLINK_STOP */
  
  /**
   * <p> "SIB.link.stop".
   * <p>` This Notification type is used when an inter-bus messaging engine to
   * messaging engine connection is started. This notification is    
   * associated with the GatewayLink MBean.
   * <p> In addition to the common properties, 
   * TYPE_SIBLINK_STOP has the following specific properties. 
   * <ul>
   * <li> KEY_REMOTE_MESSAGING_ENGINE_NAME. </li>
   * <li> KEY_REMOTE_MESSAGING_ENGINE_UUID. </li>
   * <li> KEY_FOREIGN_BUS_NAME. </li>
   * <li> KEY_STOP_REASON. </li>
   * </ul>
   *  
   * @see #KEY_REMOTE_MESSAGING_ENGINE_NAME
   * @see #KEY_REMOTE_MESSAGING_ENGINE_UUID
   * @see #KEY_FOREIGN_BUS_NAME
   * @see #KEY_STOP_REASON
   */  

  public static final String TYPE_SIBLINK_STOP = "SIB.link.stop";

  // KEY_FOREIGN_BUS_NAME

  // KEY_REMOTE_MESSAGING_ENGINE_NAME

  // KEY_REMOTE_MESSAGING_ENGINE_UUID

  // KEY_STOP_REASON

  // Values for stop reason:

  // STOP_REASON_COMMUNICATIONS_TERMINATED

  // STOP_REASON_LOCAL_ME_SHUTDOWN

  /**
   * <p> "stop.reason.administrator.command".
   * 
   * <p> A reason code for a TYPE_SIBLINK_STOP notification. 
   * 
   * @see #TYPE_SIBLINK_STOP
   */  
  public static final String STOP_REASON_ADMINISTRATOR_COMMAND = 
                             "stop.reason.administrator.command";

  /* TYPE_SIB_MESSAGEPOINT_SEND_ALLOWED_STATE */
  
  /**
   * <p> "SIB.messagepoint.send.allowed.state".
   * <p> This Notification type is used when a Message Point changes its
   * send allowed state. It is associated with a QueuePoint or 
   * PublicationPoint MBean.
   * <p> In addition to the common properties, 
   * TYPE_SIB_MESSAGEPOINT_SEND_ALLOWED_STATE has the following specific properties. 
   * <ul>
   * <li> KEY_DESTINATION_NAME. </li>
   * <li> KEY_DESTINATION_UUID. </li>
   * <li> KEY_SEND_ALLOWED_STATE. </li>
   * </ul>
   *  
   * @see #KEY_DESTINATION_NAME
   * @see #KEY_DESTINATION_UUID
   * @see #KEY_SEND_ALLOWED_STATE
   */
  
  public static final String TYPE_SIB_MESSAGEPOINT_SEND_ALLOWED_STATE = 
                             "SIB.messagepoint.send.allowed.state";
  /**
   * <p> "destination.name". 
   * 
   * <p> The value of this property is the Name of the destination.
   */   
  public static final String KEY_DESTINATION_NAME = "destination.name";
  
  /**
   * <p> "destination.UUID". 
   * 
   * <p> The value of this property is the UUID of the destination.
   */   
  public static final String KEY_DESTINATION_UUID = "destination.UUID";
  
  /**
   * <p> "send.allowed.state". 
   * 
   * <p> The value of this property may be either SEND_ALLOWED_TRUE or 
   * SEND_ALLOWED_FALSE. 
   * 
   * @see #SEND_ALLOWED_TRUE
   * @see #SEND_ALLOWED_FALSE 
   */   
  public static final String KEY_SEND_ALLOWED_STATE = "send.allowed.state";

  /**
   * <p> "send.allowed.true".
   * 
   * <p> A value for the KEY_SEND_ALLOWED_STATE property.
   * 
   *  @see #KEY_SEND_ALLOWED_STATE  
   */  
  public static final String SEND_ALLOWED_TRUE =
                             "send.allowed.true";
  /**
   * <p> "send.allowed.false".
   * 
   * <p> A value for the KEY_SEND_ALLOWED_STATE property.
   * 
   *  @see #KEY_SEND_ALLOWED_STATE  
   */                               
  public static final String SEND_ALLOWED_FALSE =
                             "send.allowed.false";

  /* TYPE_SIB_MESSAGEPOINT_RECEIVE_ALLOWED_STATE */
  
  /**
   * <p> "SIB.messagepoint.receive.allowed.state".
   * <p> This Notification type is used when a Message Point changes its
   * receive allowed state. It is associated with a QueuePoint or 
   * PublicationPoint MBean.
   * <p> In addition to the common properties, 
   * TYPE_SIB_MESSAGEPOINT_RECEIVE_ALLOWED_STATE has the following specific properties. 
   * <ul>
   * <li> KEY_DESTINATION_NAME. </li>
   * <li> KEY_DESTINATION_UUID. </li>
   * <li> KEY_RECEIVE_ALLOWED_STATE. </li>
   * </ul>
   *  
   * @see #KEY_DESTINATION_NAME
   * @see #KEY_DESTINATION_UUID
   * @see #KEY_RECEIVE_ALLOWED_STATE
   */ 
  
  public static final String TYPE_SIB_MESSAGEPOINT_RECEIVE_ALLOWED_STATE = 
                             "SIB.messagepoint.receive.allowed.state";
  /** 
   * TYPE_SIB_MESSAGEPOINT_RECEIVE_ALLOWED_STATE has the following specific
   * properties. 
   */ 

  // KEY_DESTINATION_NAME
  
  // KEY_DESTINATION_UUID

  /**
   * <p> "receive.allowed.state". 
   * 
   * <p> The value of this property may be either RECEIVE_ALLOWED_TRUE or 
   * RECEIVE_ALLOWED_FALSE. 
   * 
   * @see #RECEIVE_ALLOWED_TRUE
   * @see #RECEIVE_ALLOWED_FALSE 
   */     
  public static final String KEY_RECEIVE_ALLOWED_STATE = "receive.allowed.state";

  /**
   * <p> "receive.allowed.true".
   * 
   * <p> A value for the KEY_RECEIVE_ALLOWED_STATE property.
   * 
   *  @see #KEY_RECEIVE_ALLOWED_STATE  
   */  
  public static final String RECEIVE_ALLOWED_TRUE =
                             "receive.allowed.true";
  /**
   * <p> "receive.allowed.false".
   * 
   * <p> A value for the KEY_RECEIVE_ALLOWED_STATE property.
   * 
   *  @see #KEY_RECEIVE_ALLOWED_STATE  
   */                               
  public static final String RECEIVE_ALLOWED_FALSE =
                             "receive.allowed.false";

  /* TYPE_SIB_MESSAGEPOINT_DEPTH_THRESHOLD_REACHED */
  
  /**
   * <p> "SIB.messagepoint.depth.threshold.reached".
   * <p> This Notification type is used when the number of message stored at a 
   * Message Point makes a depth change that causes messages to either
   * start flowing or stop flowing into it. It is associated with a QueuePoint
   * or PublicationPoint MBean.
   * <p> In addition to the common properties, 
   * TYPE_SIB_MESSAGEPOINT_DEPTH_THRESHOLD_REACHED has the following specific properties. 
   * <ul>
   * <li> KEY_DESTINATION_NAME. </li>
   * <li> KEY_DESTINATION_UUID. </li>
   * <li> KEY_DEPTH_THRESHOLD_REACHED. </li>
   * <li> KEY_MESSAGES. </li>
   * </ul>
   *  
   * @see #KEY_DESTINATION_NAME
   * @see #KEY_DESTINATION_UUID
   * @see #KEY_DEPTH_THRESHOLD_REACHED
   * @see #KEY_MESSAGES
   */ 
  
  public static final String TYPE_SIB_MESSAGEPOINT_DEPTH_THRESHOLD_REACHED = 
                             "SIB.messagepoint.depth.threshold.reached";

  // KEY_DESTINATION_NAME
  
  // KEY_DESTINATION_UUID

  /**
   * <p> "depth.threshold.reached". 
   * 
   * <p> The value of this property may be either DEPTH_THRESHOLD_REACHED_HIGH or 
   * DEPTH_THRESHOLD_REACHED_LOW. 
   * 
   * @see #DEPTH_THRESHOLD_REACHED_HIGH
   * @see #DEPTH_THRESHOLD_REACHED_LOW 
   */       
  public static final String KEY_DEPTH_THRESHOLD_REACHED = "depth.threshold.reached";

  /**
   * <p> "depth.threshold.reached.high".
   * 
   * <p> A value for the KEY_DEPTH_THRESHOLD_REACHED property.
   * 
   *  @see #KEY_DEPTH_THRESHOLD_REACHED  
   */  
  public static final String DEPTH_THRESHOLD_REACHED_HIGH =
                             "depth.threshold.reached.high";

  /**
   * <p> "depth.threshold.reached.low".
   * 
   * <p> A value for the KEY_DEPTH_THRESHOLD_REACHED property.
   * 
   *  @see #KEY_DEPTH_THRESHOLD_REACHED  
   */                               
  public static final String DEPTH_THRESHOLD_REACHED_LOW =
                             "depth.threshold.reached.low"; 

  /**
   * <p> "messages". 
   * 
   * <p> The value of this property represents the count of messages at the message point. 
   */        
  public static final String KEY_MESSAGES = "messages";

  /* TYPE_SIB_REMOTE_MESSAGEPOINT_DEPTH_THRESHOLD_REACHED */
  
  /**
   * <p> "SIB.remote.messagepoint.depth.threshold.reached".
   * <p> This Notification type is used when the number of message stored at a 
   * Remote Message Point makes a depth change that causes messages to either
   * start flowing or stop flowing into it. It is associated with a RemoteQueuePoint
   * or PublicationPoint MBean.
   * <p> In addition to the common properties, 
   * TYPE_SIB_REMOTE_MESSAGEPOINT_DEPTH_THRESHOLD_REACHED has the following specific properties. 
   * <ul>
   * <li> KEY_DESTINATION_NAME. </li>
   * <li> KEY_DESTINATION_UUID. </li>
   * <li> KEY_LOCALIZING_MESSAGING_ENGINE_UUID. </li>
   * <li> KEY_DEPTH_THRESHOLD_REACHED. </li>
   * <li> KEY_MESSAGES. </li>
   * </ul>
   *  
   * @see #KEY_DESTINATION_NAME
   * @see #KEY_DESTINATION_UUID
   * @see #KEY_LOCALIZING_MESSAGING_ENGINE_UUID
   * @see #KEY_DEPTH_THRESHOLD_REACHED
   * @see #KEY_MESSAGES
   */  
  
  public static final String TYPE_SIB_REMOTE_MESSAGEPOINT_DEPTH_THRESHOLD_REACHED = 
                             "SIB.remote.messagepoint.depth.threshold.reached";

  /**
   * <p> "destination.UUID". 
   * 
   * <p> The value of this property is the UUID of the Messaging Engine 
   * localizing the destination.
   */   
  public static final String KEY_LOCALIZING_MESSAGING_ENGINE_UUID = 
                            "localizing.messaging.engine.uuid";

  // KEY_DESTINATION_NAME
  
  // KEY_DESTINATION_UUID
  
  // KEY_DEPTH_THRESHOLD_REACHED 

  // DEPTH_THRESHOLD_REACHED_HIGH
  
  // DEPTH_THRESHOLD_REACHED_LOW
    
  // KEY_MESSAGES

  /* TYPE_SIB_CLIENT_CONNECTION_START */
  
  /**
   * <p> "SIB.client.connection.start".
   * <p> Produced whenever a remote client connects to this messaging engine. 
   * <p> In addition to the common properties, 
   * TYPE_SIB_CLIENT_CONNECTION_START has the following specific properties. 
   * <ul>
   * <li> KEY_CLIENT_USERID. </li>
   * <li> KEY_FAP_TYPE. </li>
   * <li> KEY_COMMUNICATIONS_ADDRESS. </li>
   * </ul>
   *  
   * @see #KEY_CLIENT_USERID
   * @see #KEY_FAP_TYPE
   * @see #KEY_COMMUNICATIONS_ADDRESS
   */   
  
  public static final String TYPE_SIB_CLIENT_CONNECTION_START = 
                             "SIB.client.connection.start";
                             
  /**
   * <p> "client.userid". 
   * 
   * <p> The value of this property is the user identifier making the connection. 
   */    
  public static final String KEY_CLIENT_USERID = 
                             "client.userid";
                             
  /**
   * <p> "fap.type". 
   * 
   * <p> The value of this property may be either FAP_TYPE_JFAP or FAP_TYPE_MQFAP.
   * 
   * @see #FAP_TYPE_JFAP
   * @see #FAP_TYPE_MQFAP 
   */  
  public static final String KEY_FAP_TYPE = "fap.type";
  
  /**
   * <p> "JFAP".
   * 
   * <p> A value for the KEY_FAP_TYPE property.
   * 
   *  @see #KEY_FAP_TYPE  
   */       
  public static final String FAP_TYPE_JFAP = "JFAP";
  
  /**
   * <p> "MQFAP".
   * 
   * <p> A value for the KEY_FAP_TYPE property.
   * 
   *  @see #KEY_FAP_TYPE  
   */      
  public static final String FAP_TYPE_MQFAP = "MQFAP";

  /**
   * <p> "communications.address". 
   * 
   * <p> The value of this property is the client's network address 
   * in dotted decimal form. This may not be the actual address of the client.
   * If, for example it uses a gateway.
   */    
  public static final String KEY_COMMUNICATIONS_ADDRESS =
                             "communications.address";  

  /* TYPE_SIB_CLIENT_CONNECTION_STOP */
  
  /**
   * <p> "SIB.client.connection.stop".
   * <p> Produced whenever a remote client disconnects from this messaging
   * engine. 
   * <p> In addition to the common properties, 
   * TYPE_SIB_CLIENT_CONNECTION_START has the following specific properties. 
   * <ul>
   * <li> KEY_CLIENT_USERID. </li>
   * <li> KEY_STOP_REASON. </li>
   * </ul>
   *  
   * @see #KEY_CLIENT_USERID
   * @see #KEY_STOP_REASON
   */   
  public static final String TYPE_SIB_CLIENT_CONNECTION_STOP = 
                             "SIB.client.connection.stop";

  // KEY_CLIENT_USERID 

  /* Value */
  /* The user identifier of the client that is disconnecting. */

  // KEY_STOP_REASON
  
  /**
   * <p> "communications.failure".
   * 
   * <p> A reason code for a TYPE_SIB_CLIENT_CONNECTION_STOP notification. 
   * 
   * @see #TYPE_SIB_CLIENT_CONNECTION_STOP
   */  
  public static final String STOP_REASON_COMMUNICATIONS_FAILURE =
                             "communications.failure";
                             
  /**
   * <p> "client.shutdown".
   * 
   * <p> A reason code for a TYPE_SIB_CLIENT_CONNECTION_STOP notification. 
   * 
   * @see #TYPE_SIB_CLIENT_CONNECTION_STOP
   */                               
  public static final String STOP_REASON_CLIENT_SHUTDOWN = 
                             "client.shutdown";

  /* TYPE_SIB_MESSAGE_EXCEPTIONED */
   
   /**
   * <p> "SIB.message.exceptioned".
   * <p> This Notification type is used when the messaging engine exceptions 
   * a message that could not be delivered to the intended destination.
   * 
   * It is associated with the Messaging Engine MBean.
   * <p> In addition to the common properties, 
   * TYPE_SIB_CLIENT_CONNECTION_START has the following specific properties. 
   * <ul>
   * <li> KEY_CLIENT_USERID. </li>
   * <li> KEY_STOP_REASON. </li>
   * </ul>
   *  
   * @see #KEY_CLIENT_USERID
   * @see #KEY_STOP_REASON
   */   
  public static final String TYPE_SIB_MESSAGE_EXCEPTIONED = 
                             "SIB.message.exceptioned";

  /**
   * <p> "exception.destination.name". 
   * 
   * <p> The value of this property is the name of the exception destination.
   */   
  public static final String KEY_EXCEPTION_DESTINATION_NAME = 
                             "exception.destination.name";
                             
  /**
   * <p> "exception.destination.UUID". 
   * 
   * <p> The value of this property is the UUID of the exception destination.
   */   
  public static final String KEY_EXCEPTION_DESTINATION_UUID = 
                             "exception.destination.UUID";
  /**
   * <p> "intended.destination.name". 
   * 
   * <p> The value of this property is the name of the intended destination.
   */   
  public static final String KEY_INTENDED_DESTINATION_NAME = 
                             "intended.destination.name";
  /**
   * <p> "intended.destination.UUID". 
   * 
   * <p> The value of this property is the UUID of the intended destination.
   * This is supplied if the intended destination was located
   */   
  public static final String KEY_INTENDED_DESTINATION_UUID = 
                             "intended.destination.UUID";
  /**
   * <p> "system.message.identifier". 
   * 
   * <p> The value of this property is the system message identifier of the
   * message that was exceptioned.
   */   
  public static final String KEY_SYSTEM_MESSAGE_IDENTIFIER = 
                             "system.message.identifier";
                             
  /**
   * <p> "message.exception.reason".
   * 
   * <p> The value of this property is the reason code for why the message
   * was exceptioned as stored in the message. 
   */    
  public static final String KEY_MESSAGE_EXCEPTION_REASON = 
                             "message.exception.reason";

  /* TYPE_SIB_MEDIATION_NEW_STATE */
  
  /**
   * <p> "SIB.mediation.new.state".
   * <p> This Notification type is used whenever a MediationPoint changes to
   * a new state. It is associated with a MediationPoint MBean.
   * <p> In addition to the common properties, 
   * TYPE_SIB_MEDIATION_NEW_STATE has the following specific properties. 
   * <ul>
   * <li> KEY_DESTINATION_NAME. </li>
   * <li> KEY_DESTINATION_UUID. </li>
   * <li> KEY_MEDIATION_NAME. </li>
   * <li> KEY_MEDIATION_STATE. </li>
   * <li> KEY_MEDIATION_STATE_REASON. </li>
   * </ul>
   *  
   * @see #KEY_DESTINATION_NAME
   * @see #KEY_DESTINATION_UUID
   * @see #KEY_MEDIATION_NAME
   * @see #KEY_MEDIATION_STATE
   * @see #KEY_MEDIATION_STATE_REASON
   */    
  public static final String TYPE_SIB_MEDIATION_NEW_STATE = 
                             "SIB.mediation.new.state";

  // KEY_DESTINATION_NAME
  
  // KEY_DESTINATION_UUID
  
  /**
   * <p> "mediation.name". 
   * 
   * <p> The value of this property is the name of the mediation.
   */   
  public static final String KEY_MEDIATION_NAME = 
                             "mediation.name";
  /**
   * <p> "mediation.state". 
   * 
   * <p> The value of this property is the new state of the mediation point.
   */   
  public static final String KEY_MEDIATION_STATE = "mediation.state";
  
  /**
   * <p> "mediation.state.started".
   * 
   * <p> A type of state for a TYPE_SIB_MEDIATION_NEW_STATE notification.
   * 
   * @see #TYPE_SIB_MEDIATION_NEW_STATE 
   */  
  public static final String MEDIATION_STATE_STARTED =
                             "mediation.state.started";
                             
  /**
   * <p> "mediation.state.stopping".
   * 
   * <p> A type of state for a TYPE_SIB_MEDIATION_NEW_STATE notification.
   * 
   * @see #TYPE_SIB_MEDIATION_NEW_STATE 
   */                               
  public static final String MEDIATION_STATE_STOPPING =
                             "mediation.state.stopping";
                             
  /**
   * <p> "mediation.state.stopped".
   * 
   * <p> A type of state for a TYPE_SIB_MEDIATION_NEW_STATE notification.
   * 
   * @see #TYPE_SIB_MEDIATION_NEW_STATE 
   */                                 
  public static final String MEDIATION_STATE_STOPPED =
                             "mediation.state.stopped";
                             
  /**
   * <p> "mediation.state.deleting".
   * 
   * <p> A type of state for a TYPE_SIB_MEDIATION_NEW_STATE notification.
   * 
   * @see #TYPE_SIB_MEDIATION_NEW_STATE 
   */                                 
  public static final String MEDIATION_STATE_DELETING =
                             "mediation.state.deleting";
                             
  /**
   * <p> "mediation.state.waiting".
   * 
   * <p> A type of state for a TYPE_SIB_MEDIATION_NEW_STATE notification.
   * 
   * @see #TYPE_SIB_MEDIATION_NEW_STATE 
   */                                   
  public static final String MEDIATION_STATE_WAITING =
                             "mediation.state.waiting";
  
  /**
   * <p> "mediation.state.reason".
   * 
   * <p> The value of this property is the reason code for the change to 
   * the new state if available, for example "MEDIATION_START_FAILURE_INVALID_SELECTOR_SIMP0663"
   */      
  public static final String KEY_MEDIATION_STATE_REASON = 
                             "mediation.state.reason";

  /* TYPE_SIB_MQLINK_DEPTH_THRESHOLD_REACHED */
  
  /**
   * <p> "SIB.mqlink.depth.threshold.reached". 
   * <p> This Notification type is used when the number of message stored at an
   * MQLink makes a depth change that causes messages to either
   * start flowing or stop flowing into it. It is associated with a MQLink
   * MBean.
   * <p> In addition to the common properties, 
   * TYPE_SIB_MQLINK_DEPTH_THRESHOLD_REACHED has the following specific properties. 
   * <ul>
   * <li> KEY_MQLINK_NAME. </li>
   * <li> KEY_MQLINK_UUID. </li>
   * <li> KEY_DEPTH_THRESHOLD_REACHED. </li>
   * <li> KEY_MESSAGES. </li>
   * </ul>
   *  
   * @see #KEY_MQLINK_NAME
   * @see #KEY_MQLINK_UUID
   * @see #KEY_DEPTH_THRESHOLD_REACHED
   * @see #KEY_MESSAGES
   */   
  
  public static final String TYPE_SIB_MQLINK_DEPTH_THRESHOLD_REACHED = 
                             "SIB.mqlink.depth.threshold.reached";

  /**
   * <p> "mqlink.name". 
   * 
   * <p> The value of this property is the name of the mqlink.
   */   
  public static final String KEY_MQLINK_NAME = "mqlink.name";
  
  /**
   * <p> "mqlink.UUID". 
   * 
   * <p> The value of this property is the UUID of the mqlink.
   */   
  public static final String KEY_MQLINK_UUID = "mqlink.UUID";
  
  // KEY_DEPTH_THRESHOLD_REACHED 

  // DEPTH_THRESHOLD_REACHED_HIGH
  
  // DEPTH_THRESHOLD_REACHED_LOW
    
  // KEY_MESSAGES
  
  /* TYPE_SIB_LINK_DEPTH_THRESHOLD_REACHED */
  
  /**
   * <p> "SIB.link.depth.threshold.reached".
   * <p> This Notification type is used when the number of message stored at a 
   * link to a remote bus makes a depth change that causes messages to either
   * start flowing or stop flowing into it. It is associated with a GatewayLink
   * MBean.
   * <p> In addition to the common properties, 
   * TYPE_SIB_LINK_DEPTH_THRESHOLD_REACHED has the following specific properties. 
   * <ul>
   * <li> KEY_MQLINK_NAME. </li>
   * <li> KEY_MQLINK_UUID. </li>
   * <li> KEY_FOREIGN_BUS_NAME. </li>
   * <li> KEY_REMOTE_MESSAGING_ENGINE_UUID. </li>
   * <li> KEY_DEPTH_THRESHOLD_REACHED. </li>
   * <li> KEY_MESSAGES. </li>
   * </ul>
   *  
   * @see #KEY_MQLINK_NAME
   * @see #KEY_MQLINK_UUID
   * @see #KEY_FOREIGN_BUS_NAME
   * @see #KEY_REMOTE_MESSAGING_ENGINE_UUID
   * @see #KEY_DEPTH_THRESHOLD_REACHED
   * @see #KEY_MESSAGES
   */    
  public static final String TYPE_SIB_LINK_DEPTH_THRESHOLD_REACHED = 
                             "SIB.link.depth.threshold.reached";

  /**
   * <p> "link.name". 
   * 
   * <p> The value of this property is the name of the link.
   */
  public static final String KEY_LINK_NAME = "link.name";
  
  /**
   * <p> "link.UUID". 
   * 
   * <p> The value of this property is the UUID of the link.
   */
  public static final String KEY_LINK_UUID = "link.UUID";
  
  // KEY_FOREIGN_BUS_NAME
    
  // KEY_REMOTE_MESSAGING_ENGINE_UUID
  
  // KEY_DEPTH_THRESHOLD_REACHED 

  // DEPTH_THRESHOLD_REACHED_HIGH
  
  // DEPTH_THRESHOLD_REACHED_LOW
    
  // KEY_MESSAGES  

  /* TYPE_SIB_MQLINK_START */
  
  /**
   * <p> "SIB.mqlink.start". 
   * This Notification type is used when an MQ link is started. 
   * It is associated with the MQLink MBean. 
   * <p> In addition to the common properties, 
   * TYPE_SIB_MQLINK_START has the following specific property. 
   * <ul>
   * <li> KEY_MQLINK_NAME. </li>
   * </ul>
   * 
   * @see #KEY_MQLINK_NAME
   */  
  public static final String TYPE_SIB_MQLINK_START = 
                             "SIB.mqlink.start"; 
 
  // KEY_MQLINK_NAME  
    
  /* TYPE_SIB_MQLINK_STOP */
  
  /**
   * <p> "SIB.mqlink.stop". 
   * This Notification type is used when an MQ link is stopped. 
   * It is associated with the MQLink MBean.
   * TYPE_SIB_MQLINK_STOP has the following specific properties. 
   * <ul>
   * <li> KEY_MQLINK_NAME. </li>
   * <li> KEY_STOP_REASON. </li>
   * </ul>  
   *
   * @see #KEY_MQLINK_NAME 
   * @see #KEY_STOP_REASON
   */ 
  public static final String TYPE_SIB_MQLINK_STOP = 
                             "SIB.mqlink.stop"; 
 
  // KEY_MQLINK_NAME

  // KEY_STOP_REASON
  /* Values */ 
  // STOP_REASON_ADMINISTRATOR_COMMAND    

  /* TYPE_SIB_MQLINK_SENDER_START */
  
  /**
   * <p> "SIB.mqlink.sender.start".
   * <p> This Notification type is used when an MQ link sender channel is  
   * started. It is associated with the MQLink MBean. 
   * <p> In addition to the common properties, 
   * TYPE_SIB_MQLINK_SENDER_START has the following specific properties. 
   * <ul>
   * <li> KEY_MQLINK_SENDER_NAME. </li>
   * <li> KEY_MQLINK_NAME. </li>
   * </ul>
   * 
   * @see #KEY_MQLINK_SENDER_NAME
   * @see #KEY_MQLINK_NAME
   */   
  public static final String TYPE_SIB_MQLINK_SENDER_START = 
                             "SIB.mqlink.sender.start"; 
 
  // KEY_MQLINK_SENDER_NAME 
  
  // KEY_MQLINK_NAME  
    
  /* TYPE_SIB_MQLINK_SENDER_STOP */
  
  /**
   * <p> "SIB.mqlink.sender.stop".
   * <p> This Notification type is used when an MQ link sender channel is 
   * stopped. It is associated with the MQLink MBean.
   * <p> In addition to the common properties, 
   * TYPE_SIB_MQLINK_SENDER_STOP has the following specific properties. 
   * <ul>
   * <li> KEY_MQLINK_SENDER_NAME. </li>
   * <li> KEY_MQLINK_NAME. </li>
   * <li> KEY_STOP_REASON. </li>
   * </ul>
   * 
   * @see #KEY_MQLINK_SENDER_NAME
   * @see #KEY_MQLINK_NAME
   * @see #KEY_STOP_REASON
   */   
  public static final String TYPE_SIB_MQLINK_SENDER_STOP = 
                             "SIB.mqlink.sender.stop"; 
 
  // KEY_MQLINK_SENDER_NAME 

  // KEY_MQLINK_NAME
  
  // KEY_STOP_REASON
  /* Values */ 
  // STOP_REASON_ADMINISTRATOR_COMMAND

  /* TYPE_SIB_MQLINK_SENDER_SESSION_START */
  
  /**
   * <p> "SIB.mqlink.sender.session.start".
   * This Notification type is used when an MQ link sender channel is  
   * started and a session established with a receiver channel at the 
   * remote queue manager. It is associated with the MQLink MBean.
   * <p> In addition to the common properties, 
   * TYPE_SIB_MQLINK_SENDER_SESSION_START has the following specific properties. 
   * <ul>
   * <li> KEY_MQLINK_SENDER_NAME. </li>
   * <li> KEY_MQLINK_NAME. </li>
   * <li> KEY_REMOTE_QUEUE_MANAGER_NAME. </li>
   * </ul>
   * 
   * @see #KEY_MQLINK_SENDER_NAME
   * @see #KEY_MQLINK_NAME
   * @see #KEY_REMOTE_QUEUE_MANAGER_NAME
   */   
  public static final String TYPE_SIB_MQLINK_SENDER_SESSION_START = 
                             "SIB.mqlink.sender.session.start"; 
 
  // KEY_MQLINK_SENDER_NAME 
  
  // KEY_MQLINK_NAME 
    
  // KEY_REMOTE_QUEUE_MANAGER_NAME
  
  /* TYPE_SIB_MQLINK_SENDER_SESSION_STOP */
  
  /**
   * <p> "SIB.mqlink.sender.session.stop".
   * <p> This Notification type is used when an MQ link sender channel is  
   * stopped and a session with a partner receiver channel at the remote  
   * queue manager is ended. It is associated with the MQLink MBean.
   * <p> In addition to the common properties, 
   * TYPE_SIB_MQLINK_SENDER_SESSION_STOP has the following specific properties. 
   * <ul>
   * <li> KEY_MQLINK_SENDER_NAME. </li>
   * <li> KEY_MQLINK_NAME. </li>
   * <li> KEY_REMOTE_QUEUE_MANAGER_NAME. </li>
   * <li> KEY_STOP_REASON. </li>
   * </ul>
   * 
   * @see #KEY_MQLINK_SENDER_NAME
   * @see #KEY_MQLINK_NAME
   * @see #KEY_REMOTE_QUEUE_MANAGER_NAME
   * @see #KEY_STOP_REASON
   */    
  public static final String TYPE_SIB_MQLINK_SENDER_SESSION_STOP = 
                             "SIB.mqlink.sender.session.stop"; 

  // KEY MQLINK_SENDER_NAME 
  
  // KEY_MQLINK_NAME 
  
  // KEY_REMOTE_QUEUE_MANAGER_NAME
  
  // KEY_STOP_REASON 
                               
  /* Values */ 
  // STOP_REASON_COMMUNICATIONS_FAILURE
   
  // STOP_REASON_REQUESTED_BY_REMOTE_QM
    
  // STOP_REASON_ADMINISTRATOR_COMMAND  

  /* TYPE_SIB_MQLINK_RECEIVER_START */
  
  /**
   * <p> "SIB.mqlink.receiver.start".
   * <p> This Notification type is used when an MQ link receiver channel is  
   * started. It is associated with the MQLink MBean. 
   * <p> In addition to the common properties, 
   * TYPE_SIB_MQLINK_RECEIVER_START has the following specific properties. 
   * <ul>
   * <li> KEY_MQLINK_RECEIVER_NAME. </li>
   * <li> KEY_MQLINK_NAME. </li>
   * </ul>
   * 
   * @see #KEY_MQLINK_RECEIVER_NAME
   * @see #KEY_MQLINK_NAME
   */   
  public static final String TYPE_SIB_MQLINK_RECEIVER_START = 
                             "SIB.mqlink.receiver.start"; 
  
  // KEY_MQLINK_RECEIVER_NAME 
  
  // KEY_MQLINK_NAME  
    
  /* TYPE_SIB_MQLINK_RECEIVER_STOP */
  
  /**
   * <p> "SIB.mqlink.receiver.stop" 
   * This Notification type is used when an MQ link receiver channel is 
   * stopped. It is associated with the MQLink MBean.
   * <p> In addition to the common properties, 
   * TYPE_SIB_MQLINK_RECEIVER_STOP has the following specific properties. 
   * <ul>
   * <li> KEY_MQLINK_RECEIVER_NAME. </li>
   * <li> KEY_MQLINK_NAME. </li>
   * <li> KEY_STOP_REASON. </li>
   * </ul>
   * 
   * @see #KEY_MQLINK_RECEIVER_NAME
   * @see #KEY_MQLINK_NAME
   * @see #KEY_STOP_REASON
   */   
  public static final String TYPE_SIB_MQLINK_RECEIVER_STOP = 
                             "SIB.mqlink.receiver.stop"; 

  // KEY_MQLINK_RECEIVER_NAME 

  // KEY_MQLINK_NAME
  
  // KEY_STOP_REASON
  /* Values */ 
  // STOP_REASON_ADMINISTRATOR_COMMAND

  /* TYPE_SIB_MQLINK_RECEIVER_SESSION_START */
  
  /**
   * <p> "SIB.mqlink.receiver.session.start".
   * <p> This Notification type is used when an MQLink receiver channel is  
   * started as a result of a request, from a partner sender channel on 
   * a remote queue manager, to establish a session. It is associated  
   * with the MQLink MBean.
   * <p> In addition to the common properties, 
   * TYPE_SIB_MQLINK_RECEIVER_SESSION_START has the following specific properties. 
   * <ul>
   * <li> KEY_MQLINK_RECEIVER_NAME. </li>
   * <li> KEY_MQLINK_NAME. </li>
   * <li> KEY_REMOTE_QUEUE_MANAGER_NAME. </li>
   * </ul>
   * 
   * @see #KEY_MQLINK_RECEIVER_NAME
   * @see #KEY_MQLINK_NAME
   * @see #KEY_REMOTE_QUEUE_MANAGER_NAME
   */   
  public static final String TYPE_SIB_MQLINK_RECEIVER_SESSION_START = 
                             "SIB.mqlink.receiver.session.start"; 

  // KEY_MQLINK_RECEIVER_NAME 
  
  // KEY_MQLINK_NAME 
    
  // KEY_REMOTE_QUEUE_MANAGER_NAME
  
  /* TYPE_SIB_MQLINK_RECEIVER_SESSION_STOP */
  
  /**
   * <p> "SIB.mqlink.receiver.session.stop".
   * This Notification type is used when an MQLink receiver channel is  
   * stopped and a session with a partner sender channel on a remote 
   * queue manager ended. It is associated with the MQLink MBean.
   * <p> In addition to the common properties, 
   * TYPE_SIB_MQLINK_RECEIVER_SESSION_STOP has the following specific properties. 
   * <ul>
   * <li> KEY_MQLINK_RECEIVER_NAME. </li>
   * <li> KEY_MQLINK_NAME. </li>
   * <li> KEY_REMOTE_QUEUE_MANAGER_NAME. </li>
   * <li> KEY_STOP_REASON. </li>
   * </ul>
   * 
   * @see #KEY_MQLINK_RECEIVER_NAME
   * @see #KEY_MQLINK_NAME
   * @see #KEY_REMOTE_QUEUE_MANAGER_NAME
   * @see #KEY_STOP_REASON
   */    
  public static final String TYPE_SIB_MQLINK_RECEIVER_SESSION_STOP = 
                             "SIB.mqlink.receiver.session.stop"; 

  // KEY_MQLINK_RECEIVER_NAME 
  
  // KEY_MQLINK_NAME 
  
  // KEY_REMOTE_QUEUE_MANAGER_NAME
  
  // KEY_STOP_REASON 
                               
  /* Values */ 
  // STOP_REASON_COMMUNICATIONS_FAILURE
   
  // STOP_REASON_REQUESTED_BY_REMOTE_QM 
  
  // STOP_REASON_ADMINISTRATOR_COMMAND  
  
  /* TYPE_SIB_MESSAGING_ENGINE_STARTING */
  
  /**
   * <p> "SIB.messaging.engine.starting".
   * <p> This Notification type is used when the Messaging Engine is starting,
   * and is associated with the SIBMain and MessagingEngine MBeans.
   * <p> In addition to the common properties, 
   * TYPE_SIB_MESSAGING_ENGINE_STARTING has the following specific property. 
   * <ul>
   * <li> KEY_START_TYPE. </li>
   * </ul>
   * 
   * @see #KEY_START_TYPE    
   */ 
  public static final String TYPE_SIB_MESSAGING_ENGINE_STARTING = 
                             "SIB.messaging.engine.starting";

  /* TYPE_SIB_MESSAGING_ENGINE_STOPPING */
  
  /**
   * <p> "SIB.messaging.engine.stopping".
   * <p> This Notification type is used when the Messaging Engine is stopping,
   * and is associated with the SIBMain and MessagingEngine MBeans.
   * <p> In addition to the common properties, 
   * TYPE_SIB_MESSAGING_ENGINE_STOPPING has the following specific property. 
   * <ul>
   * <li> KEY_STOP_REASON. </li>
   * </ul>
   * 
   * @see #KEY_STOP_REASON    
   */ 
  public static final String TYPE_SIB_MESSAGING_ENGINE_STOPPING = 
                             "SIB.messaging.engine.stopping";

  /* TYPE_SIB_MESSAGING_ENGINE_FAILED */
  
  /**
   * <p> "SIB.messaging.engine.failed".
   * <p> This Notification type is used when the Messaging Engine fails to
   * start or to stop, and is associated with the SIBMain and MessagingEngine
   * MBeans.
   * <p> In addition to the common properties, TYPE_SIB_MESSAGING_ENGINE_FAILED
   * has the following specific property.
   * <ul>
   * <li> KEY_FAIL_OPERATION. </li>
   * <li> KEY_FAIL_OPERATION_TYPE. </li>
   * </ul>
   * 
   * @see #KEY_FAIL_OPERATION
   * @see #KEY_FAIL_OPERATION_TYPE
   */ 
  public static final String TYPE_SIB_MESSAGING_ENGINE_FAILED = 
                             "SIB.messaging.engine.failed";
  
  /**
   * <p> "fail.operation". 
   * 
   * <p> The value of this property may be either
   * TYPE_SIB_MESSAGING_ENGINE_START or TYPE_SIB_MESSAGING_ENGINE_STOP. 
   * 
   * @see #TYPE_SIB_MESSAGING_ENGINE_START
   * @see #TYPE_SIB_MESSAGING_ENGINE_STOP
   */        
  public static final String KEY_FAIL_OPERATION = "fail.operation";

  /**
   * <p> "fail.operation.type". 
   * 
   * <p> The value of this property may be either START_TYPE_WARM,
   * START_TYPE_FLUSH, STOP_REASON_ADMINISTRATOR_IMMEDIATE or
   * STOP_REASON_ADMINISTRATOR_FORCE. 
   * 
   * @see #START_TYPE_WARM
   * @see #START_TYPE_FLUSH
   * @see #STOP_REASON_ADMINISTRATOR_IMMEDIATE
   * @see #STOP_REASON_ADMINISTRATOR_FORCE
   */        
  public static final String KEY_FAIL_OPERATION_TYPE = "fail.operation.type";

}
