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


import java.io.Serializable;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

/**
 * Interface which provides read only access to some of the properties of
 * a ConnectionFactory for use in the managed environment.<p>
 * 
 * Note that this is a super interface of JmsConnectionFactory which
 * provides setters for the getters defined here, and also getters and
 * setters for other properties of the JmsConnectionFactory.<p>
 * 
 * The ability of applications to use the other properties defined on
 * JmsConnectionFactory is dependent on the environment in which it is
 * being run.  
 * 
 * @ibm-api
 * @ibm-was-base
 */
public interface JmsManagedConnectionFactory
  extends ConnectionFactory, Serializable
{	
  
  /**
   * Returns the default clientID for this Connections created from
   * this ConnectionFactory.<p>
   * 
   * <!-- Javadoc'd: matrober 030903 -->
   * 
   * @return The clientID string
   *  
   * @see JmsConnectionFactory#setClientID(String)   
   */
  public String getClientID();
   
  /**
   * Retrieve the Reliability that should be used for non persistent messages.<p>
   * 
   * <!-- Javadoc'd: matrober 030903 -->
   * 
   * @return The current setting for reliability of non persistent messages.
   *    
   * @see JmsConnectionFactory#setNonPersistentMapping(String)
   */
  public String getNonPersistentMapping();
   
  /**
   * Retrieve the Reliability that should be used for persistent messages.<p>
   * 
   * @return The current setting for reliability of persistent messages.
   *    
   * @see JmsConnectionFactory#setPersistentMapping(String)
   */
  public String getPersistentMapping();

  /**
   * Is this connection factory in a managed environment?<p>
   * 
   * <!-- Javadoc'd: amardeep 050903 -->
   * 
   * @return true if in a managed environment, false otherwise.
   */
  public boolean isManaged();
  
  /**
   * Retrieve the current setting for the ReadAhead property for this
   * JmsConnectionFactory.<p>
   * 
   * @return The current setting for ReadAhead.
   * 
   * @see JmsConnectionFactory#setReadAhead(String)
   */
  public String getReadAhead();
  
  
  /**
   * Retrieves the current value of the durable subscription home property.<p>
   * 
   * @return String The current durable subscription home property.
   * 
   * @see JmsConnectionFactory#setDurableSubscriptionHome(String)
   */
  public String getDurableSubscriptionHome();
  
  /**
   * Gets the temporary queue name prefix.<P>
   * 
   * Will return null if the prefix has not been set.<P>
   * 
   * @return String The temporary queue name prefix.
   */
  public String getTemporaryQueueNamePrefix();

  /**
   * Gets the temporary topic name prefix.<P>
   * 
   * Will return null if the prefix has not been set.<P>
   * 
   * @return The temporary queue name prefix.
   */
  public String getTemporaryTopicNamePrefix();
  
  /**
   * Retrieves the name of the bus to which Connections created from this
   * ConnectionFactory will be connected.<p>
   *
   * <!-- Javadoc'd: matrober 030903 -->
   *
   * @return The name of the bus to connect to.
   */
  public String getBusName();
  
  /**
   * Returns the default uesr name that will be used to create Connections
   * when none is specified by the application or container.<p>
   *
   * <!-- Javadoc'd: matrober 030903 -->
   *
   * @return the default user name
   */
  public String getUserName();
  
//  /**
//   * Returns the default password that will be used to create Connections
//   * when none is specified by the application or container.<p>
//   *
//   * <!-- Javadoc'd: matrober 030903 -->
//   *
//   * @return the default password
//   */
//  public String getPassword();
  
  /**
   * Gets the target, the name of a target that resolves
   * to a group of messaging engines.<p>
   *
   * May return null if value has not been set.<p>
   *
   * @return the target
   */
  public String getTarget();
  
  /**
   * Gets the target type, specifying the type of name in the
   * Target Group property.
   *
   * Will be set to {@link ApiJmsConstants#TARGET_TYPE_BUSMEMBER} by default.
   *
   * @return the target type
   *
   * @see ApiJmsConstants#TARGET_TYPE_BUSMEMBER
   * @see ApiJmsConstants#TARGET_TYPE_CUSTOM
   * @see ApiJmsConstants#TARGET_TYPE_ME   
   */
  public String getTargetType ();
  
  /**
   * Gets the target significance, which indicates the significance of
   * the target group.
   * 
   * @return the target significance
   * 
   * @see ApiJmsConstants#TARGET_SIGNIFICANCE_PREFERRED
   * @see ApiJmsConstants#TARGET_SIGNIFICANCE_REQUIRED
   */
  public String getTargetSignificance();
  
  /**
   * Gets the target transport chain, which is the name of the chain that should
   * be used when connecting to a remote messaging engine.<p>
   *
   * @return the target transport chain
   */
  public String getTargetTransportChain ();
  
  /**
   * Gets the provider endpoints, which are a comma separated list of end point
   * triples of the the form {&lt;host&gt;:&lt;port&gt;:&lt;chain&gt;}.<p>
   *
   * The default value for this property is null.<p>
   *
   * @return the provider endpoints
   */
  public String getProviderEndpoints ();
  
  /**
   * Gets the connection proximity, which specifies the proximity of
   * acceptable messaging engines.
   *
   * Will be set to {@link ApiJmsConstants#CONNECTION_PROXIMITY_BUS} by default.
   *
   * @return the connection proximity
   *
   * @see ApiJmsConstants#CONNECTION_PROXIMITY_BUS
   */
  public String getConnectionProximity ();
  
  
  /**
   * Gets the current setting for the policy towards sharing of durable subscriptions.
   *
   * @see JmsConnectionFactory#setShareDurableSubscriptions(String)
   */
  public String getShareDurableSubscriptions();

  /**
   * Determines the network adapter to use for multicast transmissions on a 
   * multi-homed system. If not set, the default adapter will be used.
   * @param the multicast interface property
   * @throws JMSException
   */
  public String getMulticastInterface();
  
  /**
   * Used to indicate the protocols that the client will accept for receiving messages.
   * Only applicable to remote (tcp/ip) connections.
   * @param the subscription protocol property
   * @throws JMSException
   */
  public String getSubscriptionProtocol();
  
  /** 
   * Returns the property indicating if the producer will modify the payload after setting it.
   * 
   * @return String containing the property value.  
   */
  public String getProducerDoesNotModifyPayloadAfterSet();  

  /** 
   * Gets the property indicating if the consumer will modify the payload after getting it.
   * 
   * @return String containing the property value.  
   */
  public String getConsumerDoesNotModifyPayloadAfterGet();  
}
