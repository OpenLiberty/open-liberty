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
package com.ibm.websphere.sib.api.jms;

import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIProperties;
import com.ibm.ws.sib.utils.TraceGroups;
import com.ibm.wsspi.sib.core.trm.SibTrmConstants;

/**
 * This file contains constants which can be used by applications as parameters
 * to provider specific JMS extension methods.<p>
 *
 * It also contains some system constants which are required for trace and NLS
 * support.
 *
 * @ibm-api
 * @ibm-was-base
 */
public interface ApiJmsConstants
{

  // ***************************** TRACE CONSTANTS *****************************

  /**
   * This constant defines the message group used when tracing methods which form
   * part of the public JMS specification.<p>
   *
   * There is a separate message group for methods which are implementation internal
   * or defined on the provider specific extensions to the JMS objects.<p>
   *
   * <i>This is not intended for use by customer applications.</i>
   *
   * <!-- Javadoc'd: matrober 030903 -->
   *
   * @see #MSG_GROUP_INT
   * @see #MSG_BUNDLE_EXT
   */
  public final static String MSG_GROUP_EXT = TraceGroups.TRGRP_JMS_EXT;

  /**
   * This constant defines the NLS message file used for messages which occur in
   * response to the public JMS specification.<p>
   *
   * <i>This is not intended for use by customer applications.</i>
   *
   * <!-- Javadoc'd: matrober 030903 -->
   *
   * @see #MSG_GROUP_EXT
   * @see #MSG_GROUP_INT
   */
  public final static String MSG_BUNDLE_EXT =
    "com.ibm.websphere.sib.api.jms.CWSIAJMSMessages";

  /**
   * This constant defines the message group used when tracing methods which are
   * implementation internal or defined on the provider specific extensions to the
   * JMS objects.<p>
   *
   * <i>This is not intended for use by customer applications.</i>
   *
   * <!-- Javadoc'd: matrober 030903 -->
   *
   * @see #MSG_BUNDLE_INT
   */
  public final static String MSG_GROUP_INT = TraceGroups.TRGRP_JMS_INT;

  /**
   * This constant defines the NLS message file used for messages which occur in
   * response to provider specific events.<p>
   *
   * <i>This is not intended for use by customer applications.</i>
   *
   * <!-- Javadoc'd: matrober 030903 -->
   *
   * @see #MSG_GROUP_INT
   * @see #MSG_GROUP_EXT
   */
  public final static String MSG_BUNDLE_INT =
    "com.ibm.websphere.sib.api.jms.CWSIAJMSMessages";


  // *********************** THREAD POOL CONSTANTS *****************************

 /**
   * This constant defines the name of the thread pool created to dispatch
   * JMS Exceptions to applications. <p>
   *
   * <i>This is not intended for use by customer applications.</i>
   *
   * <!-- Javadoc'd: pbroad 250108 -->
   */
  public final static String EXCEPTION_THREADPOOL_NAME_INT = "JMS Exception Callback";

  /**
   * This constant defines name of a tuning parameter.<br/>
   * This tuning parameter specifies how many threads may be active
   * concurrently, invoking application provided JMS exception listeners
   * on different connections.
   * A maximum of one thread is ever active per connection.<p>
   *
   * <i>This is not intended for use by customer applications.</i>
   *
   * <!-- Javadoc'd: pbroad 250108 -->
   */
  public final static String EXCEPTION_MAXTHREADS_NAME_INT = "sib.apijms.maxExceptionThreads";

  /**
   * This constant defines the default number of threads which may
   * be active concurrently, invoking JMS exception listeners.
   * <p>
   *
   * <i>This is not intended for use by customer applications.</i>
   *
   * <!-- Javadoc'd: pbroad 250108 -->
   *
   * @see #EXCEPTION_MAXTHREADS_NAME_INT
   */
  public final static String EXCEPTION_MAXTHREADS_DEFAULT_INT = "10";

  // ***************************** FACTORY CONSTANTS ***************************

  /**
   * This constant contains the name of the class which implements the
   * JmsFactoryFactory interface. This constant is used to dynamically instantiate
   * the implementation class in order to provide separation between component
   * interfaces and implementation.
   *
   * <i>This is not intended for use by customer applications.</i>
   *
   * <!-- Javadoc'd: matrober 030903 -->
   *
   * @see JmsFactoryFactory
   */
  public final static String CONNFACTORY_FACTORY_CLASS =
    "com.ibm.ws.sib.api.jms.impl.JmsFactoryFactoryImpl";


  // ****************** CONSTANTS FOR MESSAGE QOS ***********************

  /**
   * Used to indicate that JMS messages should be sent with
   * Reliability.BEST_EFFORT_NONPERSISTENT.<p>
   *
   * @see JmsConnectionFactory#setNonPersistentMapping(String)
   * @see JmsConnectionFactory#setPersistentMapping(String)
   * @see #MAPPING_EXPRESS_NONPERSISTENT
   * @see #MAPPING_RELIABLE_NONPERSISTENT
   * @see #MAPPING_RELIABLE_PERSISTENT
   * @see #MAPPING_ASSURED_PERSISTENT
   * @see #MAPPING_AS_SIB_DESTINATION
   */

  public static final String MAPPING_BEST_EFFORT_NONPERSISTENT = Reliability.BEST_EFFORT_NONPERSISTENT.toString();

  /**
   * Used to indicate that JMS messages should be sent with
   * Reliability.EXPRESS_NONPERSISTENT.<p>
   *
   * This is the default value for the nonPersistentMapping property.
   *
   * @see JmsConnectionFactory#setNonPersistentMapping(String)
   * @see JmsConnectionFactory#setPersistentMapping(String)
   * @see #MAPPING_BEST_EFFORT_NONPERSISTENT
   * @see #MAPPING_RELIABLE_NONPERSISTENT
   * @see #MAPPING_RELIABLE_PERSISTENT
   * @see #MAPPING_ASSURED_PERSISTENT
   * @see #MAPPING_AS_SIB_DESTINATION
   */
  public static final String MAPPING_EXPRESS_NONPERSISTENT     = Reliability.EXPRESS_NONPERSISTENT.toString();

  /**
   * Used to indicate that JMS messages should be sent with
   * Reliability.RELIABLE_NONPERSISTENT.<p>
   *
   * @see JmsConnectionFactory#setNonPersistentMapping(String)
   * @see JmsConnectionFactory#setPersistentMapping(String)
   * @see #MAPPING_BEST_EFFORT_NONPERSISTENT
   * @see #MAPPING_EXPRESS_NONPERSISTENT
   * @see #MAPPING_ASSURED_PERSISTENT
   * @see #MAPPING_AS_SIB_DESTINATION
   */
  public static final String MAPPING_RELIABLE_NONPERSISTENT    = Reliability.RELIABLE_NONPERSISTENT.toString();

  /**
   * Used to indicate that JMS messages should be sent with
   * Reliability.RELIABLE_PERSISTENT.<p>
   *
   * @see JmsConnectionFactory#setNonPersistentMapping(String)
   * @see JmsConnectionFactory#setPersistentMapping(String)
   * @see #MAPPING_BEST_EFFORT_NONPERSISTENT
   * @see #MAPPING_EXPRESS_NONPERSISTENT
   * @see #MAPPING_RELIABLE_NONPERSISTENT
   * @see #MAPPING_ASSURED_PERSISTENT
   * @see #MAPPING_AS_SIB_DESTINATION
   */
  public static final String MAPPING_RELIABLE_PERSISTENT       = Reliability.RELIABLE_PERSISTENT.toString();

  /**
   * Used to indicate that JMS messages should be sent with
   * Reliability.ASSURED_PERSISTENT.<p>
   *
   * @see JmsConnectionFactory#setNonPersistentMapping(String)
   * @see JmsConnectionFactory#setPersistentMapping(String)
   * @see #MAPPING_BEST_EFFORT_NONPERSISTENT
   * @see #MAPPING_EXPRESS_NONPERSISTENT
   * @see #MAPPING_RELIABLE_NONPERSISTENT
   * @see #MAPPING_RELIABLE_PERSISTENT
   * @see #MAPPING_AS_SIB_DESTINATION
   */
  public static final String MAPPING_ASSURED_PERSISTENT        = Reliability.ASSURED_PERSISTENT.toString();

  /**
   * Used to indicate that JMS messages should be sent with
   * the default reliability configured on the SIB destination. This is
   * a synonym for MAPPING_NONE.
   *
   * @see JmsConnectionFactory#setNonPersistentMapping(String)
   * @see JmsConnectionFactory#setPersistentMapping(String)
   * @see #MAPPING_BEST_EFFORT_NONPERSISTENT
   * @see #MAPPING_EXPRESS_NONPERSISTENT
   * @see #MAPPING_RELIABLE_NONPERSISTENT
   * @see #MAPPING_RELIABLE_PERSISTENT
   * @see #MAPPING_ASSURED_PERSISTENT
   */
  public static final String MAPPING_AS_SIB_DESTINATION        = "AsSIBDestination";

  /**
   * Used to indicate that JMS messages should be sent with
   * the default reliability configured on the SIB destination. This is a
   * synonym for MAPPING_AS_SIB_DESTINATION
   *
   * @see JmsConnectionFactory#setNonPersistentMapping(String)
   * @see JmsConnectionFactory#setPersistentMapping(String)
   * @see #MAPPING_BEST_EFFORT_NONPERSISTENT
   * @see #MAPPING_EXPRESS_NONPERSISTENT
   * @see #MAPPING_RELIABLE_NONPERSISTENT
   * @see #MAPPING_RELIABLE_PERSISTENT
   * @see #MAPPING_ASSURED_PERSISTENT
   */
  public static final String MAPPING_NONE = Reliability.NONE.toString();

  //************** CONSTANTS FOR CONFIGURATION OF CONNECTIONPROXIMITY *******************

  /**
   * Value for the connectionProximity property of a JmsConnectionFactory that
   * indicates that the application wishes to connect to any messaging engine
   * in the same server process.<p>
   *
   * @see JmsConnectionFactory#setConnectionProximity(String)
   * @see #CONNECTION_PROXIMITY_HOST
   * @see #CONNECTION_PROXIMITY_BUS
   * @see #CONNECTION_PROXIMITY_CLUSTER
   */
  public static final String CONNECTION_PROXIMITY_SERVER    = SibTrmConstants.CONNECTION_PROXIMITY_SERVER;

  /**
   * Value for the connectionProximity property of a JmsConnectionFactory that
   * indicates that the application wishes to connect to any messaging engine
   * on the same host as the client application.<p>
   *
   * @see JmsConnectionFactory#setConnectionProximity(String)
   * @see #CONNECTION_PROXIMITY_SERVER
   * @see #CONNECTION_PROXIMITY_BUS
   * @see #CONNECTION_PROXIMITY_CLUSTER
   */
  public static final String CONNECTION_PROXIMITY_HOST      = SibTrmConstants.CONNECTION_PROXIMITY_HOST;

  /**
   * Default value for the connectionProximity property of a JmsConnectionFactory that
   * indicates that the application wishes to connect to any messaging engine
   * in the specified bus.<p>
   *
   * @see JmsConnectionFactory#setConnectionProximity(String)
   * @see #CONNECTION_PROXIMITY_SERVER
   * @see #CONNECTION_PROXIMITY_HOST
   * @see #CONNECTION_PROXIMITY_CLUSTER
   */
  public static final String CONNECTION_PROXIMITY_BUS       = SibTrmConstants.CONNECTION_PROXIMITY_BUS;

  /**
   * Value for the connectionProximity property of a JmsConnectionFactory that
   * indicates that the application wishes to connect to any messaging engine
   * on the same cluster as the client application.<p>
   *
   * @see JmsConnectionFactory#setConnectionProximity(String)
   * @see #CONNECTION_PROXIMITY_SERVER
   * @see #CONNECTION_PROXIMITY_BUS
   * @see #CONNECTION_PROXIMITY_HOST
   */
  public static final String CONNECTION_PROXIMITY_CLUSTER   = SibTrmConstants.CONNECTION_PROXIMITY_CLUSTER;

  // ************** CONSTANTS FOR CONFIGURATION OF TARGET_TYPE *******************

  /**
   * Value for the targetType property of a JmsConnectionFactory that
   * indicates that the information specified in the targetGroup is
   * the name of a bus.<p>
   *
   * The messaging engine chosen to connect to should be a member of this bus.
   *
   * @see JmsConnectionFactory#setTargetType(String)
   * @see #TARGET_TYPE_CUSTOM
   * @see #TARGET_TYPE_ME

   */
  public static final String TARGET_TYPE_BUSMEMBER   = SibTrmConstants.TARGET_TYPE_BUSMEMBER;

  /**
   * Value for the targetType property of a JmsConnectionFactory that
   * indicates that the information specified in the targetGroup is
   * a user specified group name.<p>
   *
   * The messaging engine chosen to connect to should be a member of this
   * user specified group.
   *
   * @see JmsConnectionFactory#setTargetType(String)
   * @see #TARGET_TYPE_BUSMEMBER
   * @see #TARGET_TYPE_ME
   */
  public static final String TARGET_TYPE_CUSTOM      = SibTrmConstants.TARGET_TYPE_CUSTOM;

  /**
   * Value for the targetType property of a JmsConnectionFactory that
   * indicates that the information specified in the targetGroup is
   * the name of a messaging engine.<p>
   *
   * @see JmsConnectionFactory#setTargetType(String)
   * @see #TARGET_TYPE_BUSMEMBER
   * @see #TARGET_TYPE_CUSTOM
   */
  public static final String TARGET_TYPE_ME          = SibTrmConstants.TARGET_TYPE_ME;

  /**
   * Value for the targetSignificance property of a JmsConnectionFactory
   * that indicates that selected messaging engine must be in the targetGroup.
   *
   * @see JmsConnectionFactory#setTargetSignificance(String)
   * @see #TARGET_SIGNIFICANCE_PREFERRED
   */
  public static final String TARGET_SIGNIFICANCE_REQUIRED = SibTrmConstants.TARGET_SIGNIFICANCE_REQUIRED;
  /**
   * Value for the targetSignificance property of a JmsConnectionFactory
   * that indicates that it is preferred that the selected messaging engine be
   * in the targetGroup.
   *
   * @see JmsConnectionFactory#setTargetSignificance(String)
   * @see #TARGET_SIGNIFICANCE_REQUIRED
   */
  public static final String TARGET_SIGNIFICANCE_PREFERRED = SibTrmConstants.TARGET_SIGNIFICANCE_PREFERRED;

  // Constants relating to multicast

  /**
   * Value for <code>Unicast</code> mode.
   * Unicast protocol is used for subscriptions.
   */
  public static final String SUBSCRIPTION_PROTOCOL_UNICAST = SibTrmConstants.SUBSCRIPTION_PROTOCOL_UNICAST;

  /**
   * Value for <code>Multicast</code> mode.
   * Multicast protocol is used for subscriptions.
   */
  public static final String SUBSCRIPTION_PROTOCOL_MULTICAST = SibTrmConstants.SUBSCRIPTION_PROTOCOL_MULTICAST;

  /**
   * Value for <code>UnicastAndMulticast</code> mode.
   * Means both unicast and multicast protocols may be used for subscriptions.
   */
  public static final String SUBSCRIPTION_PROTOCOL_UNICAST_AND_MULTICAST = SibTrmConstants.SUBSCRIPTION_PROTOCOL_UNICAST_AND_MULTICAST;


  /** Value for multicastInterface <code>None</code> mode.
   *  Means use the default local IP address.
   */
  public static final String MULTICAST_INTERFACE_NONE = SibTrmConstants.MULTICAST_INTERFACE_NONE;



  // ************** CONSTANTS FOR CONFIGURATION OF READAHEAD *******************

  /**
   * Default value for the ReadAhead property of a JmsConnectionFactory.<p>
   *
   * The behaviour controlled by ReadAhead is described in the documentation
   * for {@link JmsConnectionFactory#setReadAhead(String)}.<br><br>
   *
   * The defaults applied by specifying this value are;
   *
   * <table border=1>
   * <tr>
   * <th align=left>Situation</th>
   * <th align=left>ReadAhead</th>
   * </tr>
   *
   * <tr>
   * <td>Consumers attached to a queue type destination (point-to-point)</td>
   * <td>false</td>
   * </tr>
   *
   * <tr>
   * <td>Non-durable consumers of a topic type destination (publish and subscribe)</td>
   * <td>true</td>
   * </tr>
   *
   * <tr>
   * <td>Durable subscribers in a non-cloned environment</td>
   * <td>true</td>
   * </tr>
   *
   * <tr>
   * <td>Durable subscribers in a cloned environment</td>
   * <td>false</td>
   * </tr>
   *
   * </table>
   * <br>
   *
   * <b>Deviation from this default behaviour should be carefully considered.</b>
   *
   * <!-- Javadoc'd: matrober 160903 -->
   *
   * @see JmsConnectionFactory#setReadAhead(String)
   * @see #READ_AHEAD_ON
   * @see #READ_AHEAD_OFF
   */
  public static final String READ_AHEAD_DEFAULT = "Default";


  /**
   * Value for the ReadAhead property of a JmsConnectionFactory and JmsDestination
   * which defines that all consumers created using this object will have
   * ReadAhead turned on.<p>
   *
   * Please see {@link JmsConnectionFactory#setReadAhead(String)} for information
   * on the effect of the ReadAhead property.<br><br>
   *
   * Note that specifying this value on a JmsDestination object will override
   * the value specified on the ConnectionFactory.
   *
   * <!-- Javadoc'd: matrober 160903 -->
   *
   * @see JmsConnectionFactory#setReadAhead(String)
   * @see JmsDestination#setReadAhead(String)
   * @see #READ_AHEAD_DEFAULT
   * @see #READ_AHEAD_OFF
   */
  public static final String READ_AHEAD_ON = "AlwaysOn";

  /**
   * Value for the ReadAhead property of a JmsConnectionFactory and JmsDestination
   * which defines that all consumers created using this object will have
   * ReadAhead turned off.<p>
   *
   * Please see {@link JmsConnectionFactory#setReadAhead(String)} for information
   * on the effect of the ReadAhead property.<br><br>
   *
   * Note that specifying this value on a JmsDestination object will override
   * the value specified on the ConnectionFactory.
   *
   * <!-- Javadoc'd: matrober 160903 -->
   *
   * @see JmsConnectionFactory#setReadAhead(String)
   * @see JmsDestination#setReadAhead(String)
   * @see #READ_AHEAD_DEFAULT
   * @see #READ_AHEAD_ON
   */
  public static final String READ_AHEAD_OFF = "AlwaysOff";

  /**
   * Default value for the ReadAhead property of a JmsDestination which defines that
   * the consumers created using this Destination should inherit their readAhead
   * behaviour from the value in effect on the ConnectionFactory at the time
   * that the Connection was created.<p>
   *
   * Please see {@link JmsConnectionFactory#setReadAhead(String)} for information
   * on the effect of the ReadAhead property.<br><br>
   *
   * <!-- Javadoc'd: matrober 160903 -->
   *
   * @see JmsDestination#setReadAhead(String)
   * @see #READ_AHEAD_ON
   * @see #READ_AHEAD_OFF
   */
  public static final String READ_AHEAD_AS_CONNECTION = "AsConnection";


  /**
   * Constant that represents the default value for the shareDurableSubscriptions
   * property of a JmsConnectionFactory object.<p>
   *
   * Applications running in a non-cloned environment will be subject to the JMS
   * specification requirement that there be at most one active consumer for a
   * durable subscription. Non-cloned environments include both non-cloned application
   * servers and remote client (J2EE AppClient) cases.<p>
   *
   * Applications running in a cloned application server will be automatically
   * detected, and access to any durable subscriptions will be shared across any
   * applications supplying the same clientID and subscription name (ie the clones
   * of the application).
   *
   * @see JmsConnectionFactory#setShareDurableSubscriptions(String)
   * @see #SHARED_DSUBS_ALWAYS
   */
  public static final String SHARED_DSUBS_IN_CLUSTER = "InCluster";

  /**
   * Constant that can be used as a parameter for the shareDurableSubscriptions
   * property of a JmsConnectionFactory object.<p>
   *
   * Applications using this value will always connect to durable subscriptions
   * in a non-exclusive way regardless of the environment in which they are exceuted.
   * This allows multiple active consumers for the durable subscription.<p>
   *
   * Note that use of this property should be carefully considered since the default
   * {@link com.ibm.websphere.sib.api.jms.ApiJmsConstants#SHARED_DSUBS_IN_CLUSTER} option
   * provides sensible behaviour in most environments.<p>
   *
   * @see JmsConnectionFactory#setShareDurableSubscriptions(String)
   * @see #SHARED_DSUBS_IN_CLUSTER
   * @see #SHARED_DSUBS_NEVER
   */
  public static final String SHARED_DSUBS_ALWAYS = "AlwaysShared";


  /**
   * Constant that can be used as a parameter for the shareDurableSubscriptions
   * property of a JmsConnectionFactory object.<p>
   *
   * Applications using this value will never connect to durable subscriptions
   * in a non-exclusive way regardless of the environment in which they are exceuted.
   * This ensures that at most one consumer instance may be active for a durable
   * subscription at any time.<p>
   *
   * Note that use of this property should be carefully considered since the default
   * {@link com.ibm.websphere.sib.api.jms.ApiJmsConstants#SHARED_DSUBS_IN_CLUSTER} option
   * provides sensible behaviour in most environments. For example if there are
   * multiple instances of an EJB running with this option then only one of them
   * will be able to access the durable subscription.<p>
   *
   * Applications wishing to have independent access to durable subscriptions from
   * many cloned application instances may be better served by deploying a
   * ConnectionFactory object at the server scope with different clientID
   * than share a single ConnectionFactory object with this option enabled.
   *
   * @see JmsConnectionFactory#setShareDurableSubscriptions(String)
   * @see #SHARED_DSUBS_IN_CLUSTER
   * @see #SHARED_DSUBS_ALWAYS
   */
  public static final String SHARED_DSUBS_NEVER = "NeverShared";


  // ******** CONSTANTS FOR CONFIGURATION OF MESSAGE CONTROL IN CLUSTERS ***********

  /**
   * Common constant to ensure consistency between multiple "On" value properties.
   */
  public static final String ON  = "On";

  /**
   * Common constant to ensure consistency between multiple "On" value properties.
   */
  public static final String OFF = "Off";

  /**
   * Constant used with a JMS queue to indicate that the SIB queue destination
   * identified by the JMSQueue is dynamically scoped to a single queue point
   * if one exists on the messaging engine that the application is connected to.
   *
   * @see JmsQueue#setScopeToLocalQP(String)
   * @since V7.0
   */
  public static final String SCOPE_TO_LOCAL_QP_ON = ON;


  /**
   * Constant used with a JMS queue to indicate that the SIB queue destination
   * identified by the JMS Queue is not scoped to a single queue point - the system
   * will choose which of the available queue points of the SIB destination to use,
   * which may include work load balancing of requests across different queue points.
   *
   * This is the default value for this property.
   *
   * @see JmsQueue#setScopeToLocalQP(String)
   * @since V7.0
   */
  public static final String SCOPE_TO_LOCAL_QP_OFF = OFF;


  /**
   * Constant used to indicate that a MessageProducer using a JMS Queue with this
   * property set should prefer a locally connected queue point of the queue
   * destination over any other queue points.
   *
   * This is the default value for this property.
   *
   * @see JmsQueue#setProducerPreferLocal(String)
   * @since V7.0
   */
  public static final String PRODUCER_PREFER_LOCAL_ON = ON;


  /**
   * Constant used to indicate that a MessageProducer using a JMS Queue with this
   * property does not have a preference for local queue points of the destination.
   *
   * @see JmsQueue#setProducerPreferLocal(String)
   * @since V7.0
   */
  public static final String PRODUCER_PREFER_LOCAL_OFF = OFF;


  /**
   * Constant used to indicate that a MessageProducer using a JMS Queue with this
   * property set should bind to a single queue point and send all messages to it.
   *
   * @see JmsQueue#setProducerBind(String)
   * @since V7.0
   */
  public static final String PRODUCER_BIND_ON = ON;


  /**
   * Constant used to indicate that a MessageProducer using a JMS Queue with this
   * property set should not bind to a single queue point, but instead allow different
   * messages to be sent to different queue points.
   *
   * This is the default for this property.
   *
   * @see JmsQueue#setProducerBind(String)
   * @since V7.0
   */
  public static final String PRODUCER_BIND_OFF = OFF;


  /**
   * Constant used to indicate that messages on all queue points of the SIB destination
   * referenced by the JMS Queue are visible to MessageConsumers and QueueBrowsers.
   *
   * @see JmsQueue#setGatherMessages(String)
   * @since V7.0
   */
  public static final String GATHER_MESSAGES_ON = ON;


  /**
   * Constant used to indicate that only messages on the local queue point (hosted by
   * the messaging engine to which the application is connected) of the SIB
   * destination are visible to MessageConsumers and QueueBrowsers.
   *
   * This is the default for this property.
   *
   * @see JmsQueue#setGatherMessages(String)
   * @since V7.0
   */
  public static final String GATHER_MESSAGES_OFF = OFF;


  // ****************** The list of JMS_IBM_* properties ***************************
  // These were copied from MA88 JMSC.java, version 1.33

  /**
   * Name of the Format property.
   * For use with MQ Interoperability, this property reflects the MQMD.format field
   * of incoming messages and can be used to provide a value for outgoing
   * messages.
   */
  public static final String FORMAT_PROPERTY            =  SIProperties.JMS_IBM_Format;
  /**
   * Name of the message type property.
   * For use with MQ interoperability, this property reflects the MQMD.MsgType field
   * of incoming messages and can be used to provide a value for outgoing messages.
   */
  public static final String MSG_TYPE_PROPERTY          =  SIProperties.JMS_IBM_MsgType;
  /**
   * Name of the feedback property.
   * With MQ interoperability, this maps to the MQMD.Feedback field. Natively
   * generated report messages also use this field to indicate the reason the
   * report was generated.
   */
  public static final String FEEDBACK_PROPERTY          =  SIProperties.JMS_IBM_Feedback;
  /**
   * Name of the PutApplType property.
   * With MQ interoperability, this reflects the MQMD.PutApplType field for
   * incoming messages. It is ignored on sent messages.
   */
  public static final String PUT_APPL_TYPE_PROPERTY     =  SIProperties.JMS_IBM_PutApplType;
  /**
   * Name of the Report_Exception property.
   * With MQ interoperability, this field reflects the relevent bits of the
   * MQMD.report field on incoming messages, and can be used to influence the
   * value of outgoing messages.
   * It is also used to control the generation of native report messages.
   */
  public static final String REPORT_EXCEPTION_PROPERTY  =  SIProperties.JMS_IBM_Report_Exception;
  /**
   * Name of the Report_Expiration property.
   * With MQ interoperability, this field reflects the relevent bits of the
   * MQMD.report field on incoming messages, and can be used to influence the
   * value of outgoing messages.
   * It is also used to control the generation of native report messages.
   */
  public static final String REPORT_EXPIRATION_PROPERTY =  SIProperties.JMS_IBM_Report_Expiration;
  /**
   * Name of the Report_COA property.
   * With MQ interoperability, this field reflects the relevent bits of the
   * MQMD.report field on incoming messages, and can be used to influence the
   * value of outgoing messages.
   * It is also used to control the generation of native report messages.
   */
  public static final String REPORT_COA_PROPERTY        =  SIProperties.JMS_IBM_Report_COA;
  /**
   * Name of the Report_COD property.
   * With MQ interoperability, this field reflects the relevent bits of the
   * MQMD.report field on incoming messages, and can be used to influence the
   * value of outgoing messages.
   * It is also used to control the generation of native report messages.
   */
  public static final String REPORT_COD_PROPERTY        =  SIProperties.JMS_IBM_Report_COD;
  /**
   * Name of the Report_PAN property.
   * With MQ interoperability, this field reflects the relevent bits of the
   * MQMD.report field on incoming messages, and can be used to influence the
   * value of outgoing messages.
   * This field is used to indicate desired behaviour of the receiving application,
   * and doesn't impact the behaviour of the messaging transport.
   */
  public static final String REPORT_PAN_PROPERTY        =  SIProperties.JMS_IBM_Report_PAN;
  /**
   * Name of the Report_NAN property.
   * With MQ interoperability, this field reflects the relevent bits of the
   * MQMD.report field on incoming messages, and can be used to influence the
   * value of outgoing messages.
   * This field is used to indicate desired behaviour of the receiving application,
   * and doesn't impact the behaviour of the messaging transport.
   */
  public static final String REPORT_NAN_PROPERTY        =  SIProperties.JMS_IBM_Report_NAN;
  /**
   * Name of the Report_Pass_Msg_ID property.
   * With MQ interoperability, this field reflects the relevent bits of the
   * MQMD.report field on incoming messages, and can be used to influence the
   * value of outgoing messages.
   */
  public static final String REPORT_MSGID_PROPERTY      =  SIProperties.JMS_IBM_Report_Pass_Msg_ID;
  /**
   * Name of the Report_Pass_Correl_ID property.
   * With MQ interoperability, this field reflects the relevent bits of the
   * MQMD.report field on incoming messages, and can be used to influence the
   * value of outgoing messages.
   */
  public static final String REPORT_CORRELID_PROPERTY   = SIProperties.JMS_IBM_Report_Pass_Correl_ID;
  /**
   * Name of the Report_Discard_Msg property.
   * With MQ interoperability, this field reflects the relevent bits of the
   * MQMD.report field on incoming messages, and can be used to influence the
   * value of outgoing messages.
   */
  public static final String REPORT_DISCARD_PROPERTY    =  SIProperties.JMS_IBM_Report_Discard_Msg;

  // The following properties are the MQRO constants that are used with
  // Report Messages.

  /**
   * Value used with REPORT_EXCEPTION_PROPERTY to indicate that a report message without data
   * is required.
   * @see ApiJmsConstants#REPORT_EXCEPTION_PROPERTY
   * @see ApiJmsConstants#MQRO_EXCEPTION_WITH_DATA
   * @see ApiJmsConstants#MQRO_EXCEPTION_WITH_FULL_DATA
   */
  public static final int MQRO_EXCEPTION                 = 0x01000000;
  /**
   * Value used with REPORT_EXCEPTION_PROPERTY to indicate that a report message with partial
   * data is required.
   * @see ApiJmsConstants#REPORT_EXCEPTION_PROPERTY
   * @see ApiJmsConstants#MQRO_EXCEPTION
   * @see ApiJmsConstants#MQRO_EXCEPTION_WITH_FULL_DATA
   */
  public static final int MQRO_EXCEPTION_WITH_DATA       = 0x03000000;
  /**
   * Value used with REPORT_EXCEPTION_PROPERTY to indicate that a report message with full
   * data is required.
   * @see ApiJmsConstants#REPORT_EXCEPTION_PROPERTY
   * @see ApiJmsConstants#MQRO_EXCEPTION
   * @see ApiJmsConstants#MQRO_EXCEPTION_WITH_DATA
   */
  public static final int MQRO_EXCEPTION_WITH_FULL_DATA  = 0x07000000;
  /**
   * Value used with REPORT_EXPIRATION_PROPERTY to indicate that a report message without data
   * is required.
   * @see ApiJmsConstants#REPORT_EXPIRATION_PROPERTY
   * @see ApiJmsConstants#MQRO_EXPIRATION_WITH_DATA
   * @see ApiJmsConstants#MQRO_EXPIRATION_WITH_FULL_DATA
   */
  public static final int MQRO_EXPIRATION                = 0x00200000;
  /**
   * Value used with REPORT_EXPIRATION_PROPERTY to indicate that a report message with partial
   * data is required.
   * @see ApiJmsConstants#REPORT_EXPIRATION_PROPERTY
   * @see ApiJmsConstants#MQRO_EXPIRATION
   * @see ApiJmsConstants#MQRO_EXPIRATION_WITH_FULL_DATA
   */
  public static final int MQRO_EXPIRATION_WITH_DATA      = 0x00600000;
  /**
   * Value used with REPORT_EXPIRATION_PROPERTY to indicate that a report message with full
   * data is required.
   * @see ApiJmsConstants#REPORT_EXPIRATION_PROPERTY
   * @see ApiJmsConstants#MQRO_EXPIRATION
   * @see ApiJmsConstants#MQRO_EXPIRATION_WITH_DATA
   */
  public static final int MQRO_EXPIRATION_WITH_FULL_DATA = 0x00e00000;
  /**
   * Value used with REPORT_COA_PROPERTY to indicate that a report message without
   * data is required.
   * @see ApiJmsConstants#REPORT_COA_PROPERTY
   * @see ApiJmsConstants#MQRO_COA_WITH_DATA
   * @see ApiJmsConstants#MQRO_COA_WITH_FULL_DATA
   */
  public static final int MQRO_COA                       = 0x00000100;
  /**
   * Value used with REPORT_COA_PROPERTY to indicate that a report message with partial
   * data is required.
   * @see ApiJmsConstants#REPORT_COA_PROPERTY
   * @see ApiJmsConstants#MQRO_COA
   * @see ApiJmsConstants#MQRO_COA_WITH_FULL_DATA
   */
  public static final int MQRO_COA_WITH_DATA             = 0x00000300;
  /**
   * Value used with REPORT_COA_PROPERTY to indicate that a report message with full
   * data is required.
   * @see ApiJmsConstants#REPORT_COA_PROPERTY
   * @see ApiJmsConstants#MQRO_COA
   * @see ApiJmsConstants#MQRO_COA_WITH_DATA
   */
  public static final int MQRO_COA_WITH_FULL_DATA        = 0x00000700;
  /**
   * Value used with REPORT_COD_PROPERTY to indicate that a report message without
   * data is required.
   * @see ApiJmsConstants#REPORT_COD_PROPERTY
   * @see ApiJmsConstants#MQRO_COD_WITH_DATA
   * @see ApiJmsConstants#MQRO_COD_WITH_FULL_DATA
   */
  public static final int MQRO_COD                       = 0x00000800;
  /**
   * Value used with REPORT_COD_PROPERTY to indicate that a report message with partial
   * data is required.
   * @see ApiJmsConstants#REPORT_COD_PROPERTY
   * @see ApiJmsConstants#MQRO_COD
   * @see ApiJmsConstants#MQRO_COD_WITH_FULL_DATA
   */
  public static final int MQRO_COD_WITH_DATA             = 0x00001800;
  /**
   * Value used with REPORT_COD_PROPERTY to indicate that a report message with full
   * data is required.
   * @see ApiJmsConstants#REPORT_COD_PROPERTY
   * @see ApiJmsConstants#MQRO_COD
   * @see ApiJmsConstants#MQRO_COD_WITH_DATA
   */
  public static final int MQRO_COD_WITH_FULL_DATA        = 0x00003800;
  /**
   * Value used with REPORT_CORRELID_PROPERTY to indicate that the messageID of
   * the request message should be copied to the correlationID of the report.
   *
   * @see ApiJmsConstants#REPORT_CORRELID_PROPERTY
   * @see ApiJmsConstants#MQRO_PASS_CORREL_ID
   */
  public static final int MQRO_COPY_MSG_ID_TO_CORREL_ID  = 0x00000000;
  /**
   * Value used with REPORT_CORRELID_PROPERTY to indicate that the correlationID of
   * the request message should be copied to the correlationID of the report.
   *
   * @see ApiJmsConstants#REPORT_CORRELID_PROPERTY
   * @see ApiJmsConstants#MQRO_COPY_MSG_ID_TO_CORREL_ID
   */
  public static final int MQRO_PASS_CORREL_ID            = 0x00000040;
  /**
   * Value used with the REPORT_MSGID_PROPERTY to indicate that a new messageID
   * should be generated for the report message.
   *
   * @see ApiJmsConstants#REPORT_MSGID_PROPERTY
   * @see ApiJmsConstants#MQRO_PASS_MSG_ID
   */
  public static final int MQRO_NEW_MSG_ID                = 0x00000000;
  /**
   * Value used with the REPORT_MSGID_PROPERTY to indicate that the messageID of
   * the request message should be passed as the messageID of the report message.
   *
   * @see ApiJmsConstants#REPORT_MSGID_PROPERTY
   * @see ApiJmsConstants#MQRO_NEW_MSG_ID
   */
  public static final int MQRO_PASS_MSG_ID               = 0x00000080;
  /**
   * Value used with the REPORT_DISCARD_PROPERTY to indicate that the request
   * message should be placed on an exception destination if it can not be
   * delivered to its original destination.
   *
   * @see ApiJmsConstants#REPORT_DISCARD_PROPERTY
   * @see ApiJmsConstants#MQRO_DISCARD_MSG
   */
  public static final int MQRO_DEAD_LETTER_Q             = 0x00000000;
  /**
   * Value used with the REPORT_DISCARD_PROPERTY to indicate that the request
   * message should be discarded if it can not be delivered to its original
   * destination.
   *
   * @see ApiJmsConstants#REPORT_DISCARD_PROPERTY
   * @see ApiJmsConstants#MQRO_DISCARD_MSG
   */
  public static final int MQRO_DISCARD_MSG               = 0x08000000;
  /**
   * Value used with the REPORT properties to indicate that no options
   * are required. Equivalent to not setting the property.
   */
  public static final int MQRO_NONE                      = 0x00000000;
  /**
   * Value used with the REPORT_NAN_PROPERTY to request an application specific
   * negative acknowledge report.
   *
   * @see ApiJmsConstants#REPORT_NAN_PROPERTY
   * @see ApiJmsConstants#MQRO_PAN
   */
  public static final int MQRO_NAN                       = 2;
  /**
   * Value used with the REPORT_PAN_PROPERTY to request an application specific
   * positive acknowledge report.
   *
   * @see ApiJmsConstants#REPORT_PAN_PROPERTY
   * @see ApiJmsConstants#MQRO_NAN
   */
  public static final int MQRO_PAN                       = 1;

  /**
   * Value used with FEEDBACK_PROPERTY to indicate no feedback.
   *
   * @see ApiJmsConstants#FEEDBACK_PROPERTY
   */
  public static final int MQFB_NONE                      = 0;
  /**
   * Value used with FEEDBACK_PROPERTY to indicate the first value in the
   * range reserved for system use.
   *
   * @see ApiJmsConstants#FEEDBACK_PROPERTY
   */
  public static final int MQFB_SYSTEM_FIRST              = 1;
  /**
   * Value used with FEEDBACK_PROPERTY to request the application to finish.
   *
   * @see ApiJmsConstants#FEEDBACK_PROPERTY
   */
  public static final int MQFB_QUIT                      = 256;
  /**
   * Value used with FEEDBACK_PROPERTY to indicate that a message expired.
   *
   * @see ApiJmsConstants#FEEDBACK_PROPERTY
   * @see ApiJmsConstants#REPORT_EXPIRATION_PROPERTY
   */
  public static final int MQFB_EXPIRATION                = 258;
  /**
   * Value used with FEEDBACK_PROPERTY to indicate that a message arrived at
   * its destination.
   *
   * @see ApiJmsConstants#FEEDBACK_PROPERTY
   * @see ApiJmsConstants#REPORT_COA_PROPERTY
   */
  public static final int MQFB_COA                       = 259;
  /**
   * Value used with FEEDBACK_PROPERTY to indicate that a message was delivered
   * to an application.
   *
   * @see ApiJmsConstants#FEEDBACK_PROPERTY
   * @see ApiJmsConstants#REPORT_COD_PROPERTY
   */
  public static final int MQFB_COD                       = 260;
  /**
   * Value used with FEEDBACK_PROPERTY.
   *
   * @see ApiJmsConstants#FEEDBACK_PROPERTY
   */
  public static final int MQFB_CHANNEL_COMPLETED         = 262;
  /**
   * Value used with FEEDBACK_PROPERTY.
   *
   * @see ApiJmsConstants#FEEDBACK_PROPERTY
   */
  public static final int MQFB_CHANNEL_FAIL_RETRY        = 263;
  /**
   * Value used with FEEDBACK_PROPERTY.
   *
   * @see ApiJmsConstants#FEEDBACK_PROPERTY
   */
  public static final int MQFB_CHANNEL_FAIL              = 264;
  /**
   * Value used with FEEDBACK_PROPERTY.
   *
   * @see ApiJmsConstants#FEEDBACK_PROPERTY
   */
  public static final int MQFB_APPL_CANNOT_BE_STARTED    = 265;
  /**
   * Value used with FEEDBACK_PROPERTY.
   *
   * @see ApiJmsConstants#FEEDBACK_PROPERTY
   */
  public static final int MQFB_TM_ERROR                  = 266;
  /**
   * Value used with FEEDBACK_PROPERTY.
   *
   * @see ApiJmsConstants#FEEDBACK_PROPERTY
   */
  public static final int MQFB_APPL_TYPE_ERROR           = 267;
  /**
   * Value used with FEEDBACK_PROPERTY.
   *
   * @see ApiJmsConstants#FEEDBACK_PROPERTY
   */
  public static final int MQFB_STOPPED_BY_MSG_EXIT       = 268;
  /**
   * Value used with FEEDBACK_PROPERTY.
   *
   * @see ApiJmsConstants#FEEDBACK_PROPERTY
   */
  public static final int MQFB_XMIT_Q_MSG_ERROR          = 271;
  /**
   * Value used with FEEDBACK_PROPERTY to indicate an application specific
   * positive acknowledge report.
   *
   * @see ApiJmsConstants#FEEDBACK_PROPERTY
   * @see ApiJmsConstants#REPORT_PAN_PROPERTY
   */
  public static final int MQFB_PAN                       = 275;
  /**
   * Value used with FEEDBACK_PROPERTY to indicate an application specific
   * negative acknowledge report.
   *
   * @see ApiJmsConstants#FEEDBACK_PROPERTY
   * @see ApiJmsConstants#REPORT_NAN_PROPERTY
   */
  public static final int MQFB_NAN                       = 276;
  /**
   * Value used with FEEDBACK_PROPERTY to indicate the last value in the
   * range reserved for system use.
   *
   * @see ApiJmsConstants#FEEDBACK_PROPERTY
   */
  public static final int MQFB_SYSTEM_LAST               = 65535;
  /**
   * Value used with FEEDBACK_PROPERTY to indicate the first value in the
   * range reserved for application use.
   *
   * @see ApiJmsConstants#FEEDBACK_PROPERTY
   */
  public static final int MQFB_APPL_FIRST                = 65536;
  /**
   * Value used with FEEDBACK_PROPERTY to indicate the last value in the
   * range reserved for application use.
   *
   * @see ApiJmsConstants#FEEDBACK_PROPERTY
   */
  public static final int MQFB_APPL_LAST                 = 999999999;

  /**
   * Value used with MSG_TYPE_PROPERTY to indicate a request message.
   *
   * @see ApiJmsConstants#MSG_TYPE_PROPERTY
   */
  public static final int MQMT_REQUEST                   = 1;
  /**
   * Value used with MSG_TYPE_PROPERTY to indicate a report message.
   *
   * @see ApiJmsConstants#MSG_TYPE_PROPERTY
   */
  public static final int MQMT_REPORT                    = 4;
  /**
   * Value used with MSG_TYPE_PROPERTY to indicate a datagram message.
   *
   * @see ApiJmsConstants#MSG_TYPE_PROPERTY
   */
  public static final int MQMT_DATAGRAM                  = 8;
  /**
   * Value used with MSG_TYPE_PROPERTY to indicate the first value in
   * the range reserved for applications.
   *
   * @see ApiJmsConstants#MSG_TYPE_PROPERTY
   */
  public static final int MQMT_APPL_FIRST                = 65536;
  /**
   * Value used with MSG_TYPE_PROPERTY to indicate the last value in
   * the range reserved for applications.
   *
   * @see ApiJmsConstants#MSG_TYPE_PROPERTY
   */
  public static final int MQMT_APPL_LAST                 = 999999999;

  /**
   * Name of the Last_Msg_In_Group property.
   * This property is used in conjunction with the JMSXGroupID and JMSXGroupSeq properties,
   * and when set to true indicates that the message is the last of a group of messages.
   * When used with MQ interoperability, this property reflects/influences the appropriate
   * bit of the MQMD.MsgFlags field.
   */
  public static final String LAST_MSG_IN_GROUP_PROPERTY =  SIProperties.JMS_IBM_Last_Msg_In_Group;
  /**
   * Name of the PutDate property.
   * With MQ interoperability, this reflects the value of the MQMD.PutDate field
   * for incoming messages. It is ignored on outgoing messages.
   */
  public static final String PUT_DATE_PROPERTY          =  SIProperties.JMS_IBM_PutDate;
  /**
   * Name of the PutTime property.
   * With MQ interoperability, this reflects the value of the MQMD.PutTime field
   * for incoming messages. It is ignored on outgoing messages.
   */
  public static final String PUT_TIME_PROPERTY          =  SIProperties.JMS_IBM_PutTime;

  // These were copied from MA88 JMSMessage.java, version 1.58
  /**
   * Name of the encoding property.
   * This is used with BytesMessages to control the representation of numeric fields.
   * With MQ interoperability, this reflects the value of the MQMD.Encoding field
   * for incoming messages, and can be used to set the value on outgoing messages.
   */
  public static final String ENCODING_PROPERTY          =  SIProperties.JMS_IBM_Encoding;
  /**
   * Name of the character set property.
   * With MQ interoperability, this reflects the value of the MQMD.CodedCharSetId
   * field on incoming messages.
   */
  public static final String CHARSET_PROPERTY           =  SIProperties.JMS_IBM_Character_Set;

  /**
   * Name of the Exception Message property.
   * Generated when a message cannot be delivered to its final destination
   * and is instead rerouted to an exception destination.
   */
  public static final String EXCEPTION_MESSAGE          = SIProperties.JMS_IBM_ExceptionMessage;

  /**
   * Name of the Exception Timestamp property.
   * Generated when a message cannot be delivered to its final destination
   * and is instead rerouted to an exception destination.
   */
  public static final String EXCEPTION_TIMESTAMP        = SIProperties.JMS_IBM_ExceptionTimestamp;

  /**
   * Name of the Exception Reason property.
   * Generated when a message cannot be delivered to its final destination
   * and is instead rerouted to an exception destination.
   */
  public static final String EXCEPTION_REASON           = SIProperties.JMS_IBM_ExceptionReason;

  /**
   * Name of the system messageID property.
   */
  public static final String SYSTEM_MESSAGEID_PROPERTY  = SIProperties.JMS_IBM_System_MessageID;

  // d246220.3 Additional field for finding out where a message was headed before it was redirected
  /**
   * Name of the exception problem destination property.
   */
  public static final String EXCEPTION_PROBLEM_DESTINATION = SIProperties.JMS_IBM_ExceptionProblemDestination;

  // F001333.14611.1 Additional field for finding out what subscription a message was headed before it was redirected
  /**
   * Name of the exception problem subscription property.
   */
  public static final String EXCEPTION_PROBLEM_SUBSCRIPTION = SIProperties.JMS_IBM_ExceptionProblemSubscription;


  /**
   * Constant for accessing the IBM ARM correlator property. Note that this IBM defined property
   * is a synonym of the TOG ARM correlator. Setting one property will change the value of the other.
   *
   * This is a String-type property.
   */
  public static final String IBM_ARM_CORRELATOR = SIProperties.JMS_IBM_ArmCorrelator;

  /**
   * Constant for accessing the The Open Group (TOG) ARM correlator property.
   *
   * Note that this property is a synonym of the IBM ARM correlator. Setting one property will
   * change the value of the other.
   *
   * This is a String-type property.
   */
  public static final String TOG_ARM_CORRELATOR = SIProperties.JMS_TOG_ARM_Correlator;

  /**
   * DeliveryMode constant for Destinations.
   * If the destination contains this value for DeliveryMode, then the deliveryMode
   * of sent messages is determined by the application.
   * @see JmsDestination#setDeliveryMode
   */
  public static final String DELIVERY_MODE_APP           = "Application";
  /**
   * DeliveryMode constant for Destinations.
   * This value will override all messages sent using the Destination
   * to be persistent.
   * @see JmsDestination#setDeliveryMode
   */
  public static final String DELIVERY_MODE_PERSISTENT    = "Persistent";
  /**
   * DeliveryMode constant for Destinations.
   * This value will override all messages sent using the Destination
   * to be non-persistent.
   * @see JmsDestination#setDeliveryMode
   */
  public static final String DELIVERY_MODE_NONPERSISTENT = "NonPersistent";

  /**
   * The maximum permitted value for timeToLive. This is equivalent to
   * approximately 291 million years. The imposition of such a limit may
   * appear arbitrary, but it prevents poorly defined behaviour which occurs
   * if we permit values up to Long.MAX_VALUE.
   *
   */
  public static final long MAX_TIME_TO_LIVE = MfpConstants.MAX_TIME_TO_LIVE;

  /**
   * Mask for the integer part of the JMS_IBM_Encoding field
   */
  public static final int ENC_INTEGER_MASK  = 0x0000000f;
  /**
   * Mask for the float part of the JMS_IBM_Encoding field
   */
  public static final int ENC_FLOAT_MASK    = 0x00000f00;

  /**
   * UNDEFINED value of integer encoding.
   */
  public static final int ENC_INTEGER_UNDEFINED = 0x00000000;
  /**
   * NORMAL value of integer encoding.
   * When sending a JMS BytesMessage, integer fields will be sent
   * using high byte first order when ENC_INTEGER_NORMAL is set
   * in the JMS_IBM_Encoding field. This is also the default order
   * used when JMS_IBM_Encoding is not set.
   */
  public static final int ENC_INTEGER_NORMAL    = 0x00000001;
  /**
   * REVERSED value of integer encoding.
   * When sending a JMS BytesMessage, integer fields will be sent
   * using low byte first order when ENC_INTEGER_REVERSED is set in
   * the JMS_IBM_Encoding field.
   */
  public static final int ENC_INTEGER_REVERSED  = 0x00000002;

  /** UNDEFINED value of floating point encoding.
   */
  public static final int ENC_FLOAT_UNDEFINED     = 0x00000000;
  /**
   * NORMAL value of floating point encoding.
   * When sending a JMS BytesMessage, floating point fields will be
   * sent using IEEE encoding in high byte first order when
   * ENC_FLOAT_IEEE_NORMAL is set
   * in the JMS_IBM_Encoding field. This is also the default order used
   * when JMS_IBM_Encoding is not set.
   */
  public static final int ENC_FLOAT_IEEE_NORMAL   = 0x00000100;
  /**
   * REVERSED value of floating point encoding.
   * When sending a JMS BytesMessage, floating point fields will be
   * sent using IEEE encoding in low byte first order when
   * ENC_FLOAT_IEEE_REVERSED is set
   * in the JMS_IBM_Encoding field.
   */
  public static final int ENC_FLOAT_IEEE_REVERSED = 0x00000200;
  /**
   * S390 value of floating point encoding.
   * When sending a JMS BytesMessage, floating point fields will be
   * sent using a S390 specific format when
   * ENC_FLOAT_IEEE_NORMAL is set
   * in the JMS_IBM_Encoding field.
   */
  public static final int ENC_FLOAT_S390          = 0x00000300;

  /**
   * A constant for the JMSXUserID property name.
   */
  public static final String JMSX_USERID         = SIProperties.JMSXUserID;
  /**
   * A constant for the JMSXDeliveryCount property name.
   */
  public static final String JMSX_DELIVERY_COUNT = SIProperties.JMSXDeliveryCount;
  /**
   * A constant for the JMSXAppID property name.
   */
  public static final String JMSX_APPID          = SIProperties.JMSXAppID;
  /**
   * A constant for the JMSXGroupID property name.
   */
  public static final String JMSX_GROUPID        = SIProperties.JMSXGroupID;
  /**
   * A constant for the JMSXGroupSeq property name.
   */
  public static final String JMSX_GROUPSEQ       = SIProperties.JMSXGroupSeq;

  /**
   * A constant for the JMS_IBM_MQMD_Persistence property name.
   */
  public static final String JMS_IBM_MQMD_PERSISTENCE = SIProperties.JMS_IBM_MQMD_Persistence;
  /**
   * A constant for the JMS_IBM_MQMD_MsgId property name.
   */
  public static final String JMS_IBM_MQMD_MSGID = SIProperties.JMS_IBM_MQMD_MsgId;
  /**
   * A constant for the JMS_IBM_MQMD_CorrelId property name.
   */
  public static final String JMS_IBM_MQMD_CORRELID = SIProperties.JMS_IBM_MQMD_CorrelId;
  /**
   * A constant for the JMS_IBM_MQMD_ReplyToQ property name.
   */
  public static final String JMS_IBM_MQMD_REPLYTOQ = SIProperties.JMS_IBM_MQMD_ReplyToQ;
  /**
   * A constant for the JMS_IBM_MQMD_ReplyToQMgr property name.
   */
  public static final String JMS_IBM_MQMD_REPLYTOQMGR = SIProperties.JMS_IBM_MQMD_ReplyToQMgr;

  /**
   * Constant - one of the possible values of the ConnectionFactory properties:
   * <ul>
   * <li>producerDoesNotModifyPayloadAfterSet</li>
   * <li>consumerDoesNotModifyPayloadAfterGet</li>
   * </ul>
   */
  public static final String MIGHT_MODIFY_PAYLOAD = "false";
  /**
   * Constant - one of the possible values of the ConnectionFactory properties:
   * <ul>
   * <li>producerDoesNotModifyPayloadAfterSet</li>
   * <li>consumerDoesNotModifyPayloadAfterGet</li>
   * </ul>
   */
  public static final String WILL_NOT_MODIFY_PAYLOAD = "true";

}
