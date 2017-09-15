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


import javax.jms.JMSException;
import javax.resource.Referenceable;


/**
 * Contains provider specific methods relating to the javax.jms.ConnectionFactory
 * interface.
 *
 * @ibm-api
 * @ibm-was-base
 */
public interface JmsConnectionFactory
  extends JmsManagedConnectionFactory, Referenceable
{

  /**
   * Set the name of the messaging bus to which you wish to connect when
   * creating Connections using this ConnectionFactory object.<p>
   *
   * The default value for this property is null, however this must be
   * altered by the administrator/user to define the actual bus name. It
   * will not be possible to create a connection with this property at
   * its default value.
   *
   * <!-- Javadoc'd: matrober 030903 -->
   *
   * @param busName The name of the bus to connect to.
   * @throws JMSException If the supplied argument is not valid for this property - for example
   *         the busName parameter is null.
   */
  public void setBusName(String busName) throws JMSException;



  /**
   * Sets the clientID for this ConnectionFactory.<p>
   *
   * The clientID property must be set to a non-null and non-empty value if
   * durable subscriptions will be created or used from Connections created using
   * this ConnectionFactory. Note that it is possible to set the clientID
   * programmatically on the JMS Connection object.<p>
   *
   * Note the getClientID method is inherited from the JmsManagedConnectionFactory
   * interface.<p>
   *
   * The default value for this property is null, however his must be changed
   * by the administrator/user if durable subscriptions will be used.
   *
   * <!-- Javadoc'd: matrober 030903 -->
   *
   * @param clientID the client ID
   * @throws JMSException If the supplied clientID is not valid.
   *
   * @see JmsManagedConnectionFactory#getClientID
   * @see javax.jms.Connection#setClientID(String)
   */
  public void setClientID(String clientID) throws JMSException;

  /**
   * Define the Reliability that should be used for non persistent messages.<p>
   *
   * Applications may choose from any of the reliability options
   * for messages sent with a JMS delivery mode of NonPersistent. The meaning of
   * these reliability options can be found in the WAS documentation.<p>
   *
   * The following constants are accepted as parameters for this method;<br>
   * <ul>
   * <li>{@link ApiJmsConstants#MAPPING_AS_SIB_DESTINATION}
   * <li>{@link ApiJmsConstants#MAPPING_NONE}
   * <li>{@link ApiJmsConstants#MAPPING_BEST_EFFORT_NONPERSISTENT}
   * <li>{@link ApiJmsConstants#MAPPING_EXPRESS_NONPERSISTENT} (default)
   * <li>{@link ApiJmsConstants#MAPPING_RELIABLE_NONPERSISTENT}
   * <li>{@link ApiJmsConstants#MAPPING_RELIABLE_PERSISTENT}
   * <li>{@link ApiJmsConstants#MAPPING_ASSURED_PERSISTENT}
   * </ul>
   * <p>
   *
   * Notes:
   * <ol>
   * <li>MAPPING_AS_SIB_DESTINATION is a synonym for MAPPING_NONE, both of which will
   * result in messages being sent with the default quality of service defined on the
   * SIB destination.
   * <li>The getNonPersistentMapping method is inherited from
   * the JmsManagedConnectionFactory interface.
   * </ol>
   *
   * <!-- Javadoc'd: matrober 030903 -->
   *
   * @param nonPersistentMapping The value required for this property, expressed as
   *              one of the constants referenced above.
   * @throws JMSException if parameter is not one of the allowed values from ApiJmsConstants
   * @see JmsManagedConnectionFactory#getNonPersistentMapping
   */
  public void setNonPersistentMapping (String nonPersistentMapping)
    throws JMSException;

  /**
   * Define the Reliability that should be used for persistent messages.<p>
   *
   * Applications may choose from any of the reliability values
   * for messages sent with a JMS delivery mode of Persistent. The meaning of
   * these reliability options can be found in the WAS documentation.<p>
   *
   * The following constants are accepted as parameters for this method;<br>
   * <ul>
   * <li>{@link ApiJmsConstants#MAPPING_AS_SIB_DESTINATION}
   * <li>{@link ApiJmsConstants#MAPPING_NONE}
   * <li>{@link ApiJmsConstants#MAPPING_BEST_EFFORT_NONPERSISTENT}
   * <li>{@link ApiJmsConstants#MAPPING_EXPRESS_NONPERSISTENT}
   * <li>{@link ApiJmsConstants#MAPPING_RELIABLE_NONPERSISTENT}
   * <li>{@link ApiJmsConstants#MAPPING_RELIABLE_PERSISTENT} (default)
   * <li>{@link ApiJmsConstants#MAPPING_ASSURED_PERSISTENT}
   * </ul>
   * <p>
   *
   * Notes:
   * <ol>
   * <li>MAPPING_AS_SIB_DESTINATION is a synonym for MAPPING_NONE, both of which will
   * result in messages being sent with the default quality of service defined on the
   * SIB destination.
   * <li>The getPersistentMapping method is inherited from
   * the JmsManagedConnectionFactory interface.
   * </ol>
   *
   * @param persistentMapping The value required for this property, expressed as
   *              one of the constants referenced above.
   * @throws JMSException if parameter is not one of the allowed values from ApiJmsConstants
   * @see JmsManagedConnectionFactory#getPersistentMapping
   */
  public void setPersistentMapping (String persistentMapping)
    throws JMSException;

  /**
   * Sets the default user name that will be used to create Connections
   * from this ConnectionFactory when none is specified by the application
   * or container.<p>
   *
   * This property is null by default, but must be altered by the administrator
   * or user if security is turned on, and the user name cannot be inherited
   * from the container.
   *
   * <!-- Javadoc'd: matrober 030903 -->
   *
   * @param userName the default user name
   * @throws JMSException If the user name specified is violates any conditions
   * (reserved for later use).
   */
  public void setUserName(String userName)
    throws JMSException;



  /**
   * Sets the default password that will be used to create Connections
   * from this ConnectionFactory when none is specified by the application
   * or container.<p>
   *
   * This property is null by default, but must be altered by the administrator
   * or user if security is turned on, and the password cannot be inherited
   * from the container.
   *
   * <!-- Javadoc'd: matrober 030903 -->
   *
   * @param password the default password
   * @throws JMSException If the password specified is violates any conditions
   * (reserved for later use).
   */
  public void setPassword(String password) throws JMSException;



  /**
   * Set the required value for ReadAhead on all consumers created from
   * this JmsConnectionFactory.<p>
   *
   * The ReadAhead property defines whether messages may be pre-emptively
   * streamed to remote client consumers of messages for the benefit of
   * performance.<br><br>
   *
   * Messages which are streamed to a consumer are locked on the server and
   * may not be consumed by any other consumers of that destination.
   * Messages which are streamed to the consumer but not consumed before the
   * consumer is closed are subsequently unlocked on the server and
   * available for receipt by other consumers.<br><br>
   *
   * Permitted values for the ReadAhead property of a JmsConnectionFactory are
   * as follows;
   *
   * <ul>
   * <li>{@link ApiJmsConstants#READ_AHEAD_DEFAULT} - The default (recommended)
   *     behaviour will be used. Follow this link for details on what this means.
   *     (this is the default for this property).
   * <li>{@link ApiJmsConstants#READ_AHEAD_ON} - All consumers created through
   *     use of this JmsConnectionFactory will have ReadAhead turned on.
   * <li>{@link ApiJmsConstants#READ_AHEAD_OFF} - All consumers created through
   *     use of this JmsConnectionFactory will have ReadAhead turned off.
   * </ul>
   * <br><br>
   *
   * Note that the value specified here may be overridden on a per Destination
   * basis by use of the {@link JmsDestination#setReadAhead(String)} method.
   *
   * @param value The required value for ReadAhead on this JmsConnectionFactory
   * @throws JMSException If the value specified is not one of the supported constants.
   *
   * @see ApiJmsConstants#READ_AHEAD_DEFAULT
   * @see ApiJmsConstants#READ_AHEAD_ON
   * @see ApiJmsConstants#READ_AHEAD_OFF
   * @see JmsDestination#setReadAhead(String)
   */
  public void setReadAhead(String value) throws JMSException;


  /**
   * Defines the name of the messaging engine used to store messages delivered to durable
   * subscriptions created or used by objects created from this JmsConnectionFactory.<p>
   *
   * Note that applications wishing to connect to a particular durable subscription
   * must specify the same value for this property as was used when the
   * durable subscription was created.<br><br>
   *
   * This property must be specified on the ConnectionFactory before the Connection
   * is created. Failure to do so will cause a javax.jms.IllegalStateException to be
   * thrown by Session.createDurableSubscriber.
   *
   * The default value for this property is null, but this default does not allow
   * applications to use durable subscriptions for the reasons described above.
   *
   * @param home The name of the messaging engine which is used to store
   *             durable subscriptions created through use of this JmsConnectionFactory.
   * @throws JMSException Reserved for later use.
   */
  public void setDurableSubscriptionHome(String home) throws JMSException;



  /**
   * Set the target, which is the name of a target that resolves
   * to a group of messaging engines.<p>
   *
   * The target is a bus member, custom user cluster or a messaging
   * engine name.<p>
   *
   * There is no default value for this property, which may be null.<p>
   *
   * @exception JMSException if the value is badly formed
   * @param value The required value for targetGroup on this JmsConnectionFactory
   *
   * @see #setTargetSignificance(String)
   * @see #setTargetType(String)
   */
  public void setTarget (String value) throws JMSException;



  /**
   * Set the target type, specifying the type of information specified
   * in the Target Group property.<p>
   *
   * Permitted values for the targetType property of a JmsConnectionFactory are
   * as follows;
   *
   * <ul>
   * <li>{@link ApiJmsConstants#TARGET_TYPE_BUSMEMBER}
   *     - The default behaviour will be used. A messaging engine will be selected that is
   *     a member of the named bus member group.
   * <li>{@link ApiJmsConstants#TARGET_TYPE_CUSTOM}
   *     - A messaging engine in the required user specified custom group will be selected.
   * <li>{@link ApiJmsConstants#TARGET_TYPE_ME}
   *     - When this constant is specified the Target Group must be a messaging engine name.
   * </ul>
   * <br><br>
   *
   * The default for this property is TARGET_TYPE_BUSMEMBER.<p>
   *
   * @param value The required value for TargetType on this JmsConnectionFactory
   * @exception JMSException if the value is badly formed
   *
   * @see ApiJmsConstants#TARGET_TYPE_BUSMEMBER
   * @see ApiJmsConstants#TARGET_TYPE_CUSTOM
   * @see ApiJmsConstants#TARGET_TYPE_ME
   * @see #setTarget(String)
   * @see #setTargetSignificance(String)
   */
  public void setTargetType (String value) throws JMSException;

  /**
   * Specifies the significance of the target group.
   * The Target Significance values are:
   * <ul>
   * <li>Required - it is required that the selected messaging engine be in the Target Group
   * <li>Preferred - it is preferred that the selected messaging engine be in the Target Group.
   * </ul>
   *
   * @param value The required value for targetSignificance
   * @throws JMSException if the value is not accepted.
   *
   * @see ApiJmsConstants#TARGET_SIGNIFICANCE_REQUIRED
   * @see ApiJmsConstants#TARGET_SIGNIFICANCE_PREFERRED
   *
   * @see #setTarget(String)
   * @see #setTargetType(String)
   */
  public void setTargetSignificance(String value) throws JMSException;


  /**
   * Set the target transport chain, the name of the transport chain that should
   * be used wjen connecting to a remote messaging engine.<p>
   *
   * This is not the chain name specification string, but a short
   * name that represents a particular chain.<p>
   *
   * A null value is allowed, and results in the default value being used.<p>
   *
   * @exception JMSException if the value is badly formed
   * @param value The required value for chain name on this JmsConnectionFactory
   */
  public void setTargetTransportChain (String value) throws JMSException;



  /**
   * Set the provider endpoints, which are a comma separated list of end point
   * triples of the the form <i>&lt;host&gt;:&lt;port&gt;:&lt;chain&gt;</i>.
   *
   * If the host name is not specified a default of "localhost" will be used.<p>
   *
   * If the port number is not specified then 7276 will be used as a default value.<p>
   *
   * If the chain name is not specified for a triplet it will be defaulted
   * to a predefined chain, eg "tcp/jfap".<p>
   *
   * The default value for this property is null.<p>
   *
   * @exception JMSException if the value is badly formed
   * @param value The provider endpoints to use
   */
  public void setProviderEndpoints (String value) throws JMSException;

 
  /**
   * Sets the target transport
   *
   * @param targetTransport
   *            the target transport
 * @throws JMSException 
   */
  public void setTargetTransport(final String targetTransport) throws JMSException;



  /**
   * Set the connection proximity, which specifies the proximity of
   * acceptable messaging engines.<p>
   *
   * Only messaging engines which fall into the acceptable proximity will be
   * considered as candidate messaging engines for attachment of the client.<p>
   *
   * Permitted values for the ConnectionProximity property of a JmsConnectionFactory
   * are as follows;
   *
   * <ul>
   * <li>{@link ApiJmsConstants#CONNECTION_PROXIMITY_SERVER}
   *     - Only messaging engines in the same server are acceptable.
   * <li>{@link ApiJmsConstants#CONNECTION_PROXIMITY_HOST}
   *     - Only messaging engines in the same host are acceptable.
   * <li>{@link ApiJmsConstants#CONNECTION_PROXIMITY_CLUSTER}
   *     - Only messaging engines in the same cluster are acceptable.
   * <li>{@link ApiJmsConstants#CONNECTION_PROXIMITY_BUS}
   *     - All messaging eninges in the Bus are acceptable. This is the default value.
   * </ul>
   * <br><br>
   *
   * @param value The connection proximity to use
   * @exception JMSException if the value is badly formed
   *
   * @see ApiJmsConstants#CONNECTION_PROXIMITY_BUS
   * @see ApiJmsConstants#CONNECTION_PROXIMITY_HOST
   * @see ApiJmsConstants#CONNECTION_PROXIMITY_SERVER
   * @see ApiJmsConstants#CONNECTION_PROXIMITY_CLUSTER
   */
  public void setConnectionProximity (String value) throws JMSException;

  /**
   * Allows the user to supply a prefix of up to twelve characters of text that will
   * be used as the beginning of the temporary destination name.<p>
   *
   * There is a 12 character limit on the length of the prefix, which will result
   * in a JMSException being thrown if this length is exceeded.<p>
   *
   * The default for this property is null, which will result in no prefix being
   * applied to temporary queue names.<p>
   *
   * @param prefix The temporary queue prefix to be set.
   * @throws JMSException if the prefix length limit is breached.
   */
  public void setTemporaryQueueNamePrefix(String prefix) throws JMSException;

  /**
   * Allows the user to supply a prefix of up to twelve characters of text that will
   * be used as the beginning of the temporary destination name.<P>
   *
   * There is a 12 character limit on the length of the prefix, which will result
   * in a JMSException being thrown if this length is exceeded.<P>
   *
   * The default for this property is null, which will result in no prefix being
   * applied to temporary topic names.<p>
   *
   * @param prefix The temporary topic prefix to be set.
   * @throws JMSException if the prefix length limit is breached.
   */
  public void setTemporaryTopicNamePrefix(String prefix) throws JMSException;


  /**
   * Defines whether durable subscriptions accessed by an application using this
   * ConnectionFactory should be used in an exclusive or shared way.<p>
   *
   * The default for this value is {@link com.ibm.websphere.sib.api.jms.ApiJmsConstants#SHARED_DSUBS_IN_CLUSTER}
   * which provides sensible behaviour in cloned and non-cloned application servers
   * and J2EE AppClient environments as described on the property description.<p>
   *
   * The valid options for this property are as follows - more details can found by
   * clicking on each option.
   * <ul>
   * <li>{@link com.ibm.websphere.sib.api.jms.ApiJmsConstants#SHARED_DSUBS_IN_CLUSTER}
   *     - The default value. The decision is made autonomically.
   * <li>{@link com.ibm.websphere.sib.api.jms.ApiJmsConstants#SHARED_DSUBS_ALWAYS}
   *     - Durable subscriptions are always shared.
   * <li>{@link com.ibm.websphere.sib.api.jms.ApiJmsConstants#SHARED_DSUBS_NEVER}
   *     - Durable subscriptions are never shared.
   * </ul>
   * <p>
   *
   * @see com.ibm.websphere.sib.api.jms.ApiJmsConstants#SHARED_DSUBS_IN_CLUSTER
   * @see com.ibm.websphere.sib.api.jms.ApiJmsConstants#SHARED_DSUBS_ALWAYS
   * @param sharePolicy Constant representing the required behaviour.
   * @throws JMSException If an unsupported constant is supplied.
   */
  public void setShareDurableSubscriptions(String sharePolicy) throws JMSException;


  /**
   * Determines the network adapter to use for multicast transmissions on a 
   * multi-homed system. If not set, the default adapter will be used.
   * @param multicastInterface
   * @throws JMSException
   */
  public void setMulticastInterface(String multicastInterface) throws JMSException;
  
  /**
   * Used to indicate the protocols that the client will accept for receiving messages.
   * Only applicable to remote (tcp/ip) connections.
   * @param protocol
   * @throws JMSException
   */
  public void setSubscriptionProtocol(String protocol) throws JMSException;
  
  /** 
   * Sets the property that indicates if the producer will modify the payload after setting it.
   * 
   * @param propertyValue containing the property value.  
   * @throws JMSException In the event of an invalid value
   */
  public void setProducerDoesNotModifyPayloadAfterSet(String propertyValue) throws JMSException;
  
  /** 
   * Sets the property that indicates if the consumer will modify the payload after getting it.
   * 
   * @param propertyValue containing the property value.  
   * @throws JMSException In the event of an invalid value
   */
  public void setConsumerDoesNotModifyPayloadAfterGet(String propertyValue) throws JMSException; 
}
