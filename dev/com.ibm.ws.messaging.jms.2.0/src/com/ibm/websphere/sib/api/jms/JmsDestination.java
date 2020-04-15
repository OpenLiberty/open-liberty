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
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.naming.Referenceable;

/**
 * Contains provider specific methods relating to the javax.jms.Destination interface.
 *
 * @ibm-api
 * @ibm-was-base
 */
public interface JmsDestination extends Destination, Serializable, Referenceable
{

  /**
   * Retrieves the name of the underlying destination to which this
   * javax.jms.Destination refers.<p>
   *
   * <!-- Javadoc'd: matrober 040903 -->
   *
   * @return The name of the underlying destination.
   */
  public String getDestName();

  /**
   * Retrieves the discriminator associated with this Destination.<p>
   *
   * Note that for Queue objects the returned value will always be null, while for
   * Topics it represents the name of the topic within the topic space.
   *
   * <!-- Javadoc'd: matrober 220803 -->
   *
   * @return The discriminator for this Destination.
   */
  public String getDestDiscrim();

  /**
   * Set the deliveryMode.<p>
   * The deliveryMode for sending messages may be overridden with this property.
   * Valid values are:
   * <table border="1">
   * <TR><TD>Value        <TD>Constant name in ApiJmsConstants<TD>Meaning
   * <TR><TD>Application  <TD>DELIVERY_MODE_APP               <TD>The deliveryMode is determined by the application (default)
   * <TR><TD>Persistent   <TD>DELIVERY_MODE_PERSISTENT        <TD>All messages will be sent persistent, irrespective of any
   *                                                              settings in the application.
   * <TR><TD>NonPersistent<TD>DELIVERY_MODE_NONPERSISTENT     <TD>All messages will be sent non-persistent, irrespective of
   *                                                              any settings in the application.
   * </table>
   * @param deliveryMode the deliveryMode to be used by MessageProducers of
   * this Destination.
   * @throws JMSException if the String is not one of the predefined values.
   * @see javax.jms.MessageProducer
   */
  public void setDeliveryMode(String deliveryMode) throws JMSException;
  public void setDeliveryModeDefault(String deliveryMode) throws JMSException;

  /**
   * Get the deliveryMode.<p>
   * @see JmsDestination#setDeliveryMode
   * @return a String representing the deliveryMode.
   */
  public String getDeliveryMode();
  public String getDeliveryModeDefault();

  /**
   * Set the timeToLive (in milliseconds) to be used for all messages sent
   * using this destination.<p>
   *
   * A value of 0 means that the message will never expire. The default for
   * this property is null, which allows the application to determine the
   * timeToLive.<p>
   *
   * For compatibility with MQJMS, the value of -2 is treated in the same
   * way as null.<p>
   *
   * The maximum value that will be accepted for timeToLive is defined in
   * ApiJmsConstants.MAX_TIME_TO_LIVE.
   *
   * @param timeToLive The time in milliseconds that the message should
   *           live before expiry.
   *
   * @see ApiJmsConstants#MAX_TIME_TO_LIVE
   * @throws JMSException if the value provided is not valid.
   */
  public void setTimeToLive(Long timeToLive) throws JMSException;
  public void setTimeToLiveDefault(Long timeToLive) throws JMSException;

  /**
   * Get the timeToLive that will be used for all messages sent using
   * this destination.<p>
   *
   * @see JmsDestination#setTimeToLive
   * @return Long The timeToLive for message sent using this destination.
   */
  public Long getTimeToLive();
  public Long getTimeToLiveDefault();

  /**
   * Set the priority to be used for all messages sent using this Destination.<p>
   *
   * The valid parameters to this method are integers 0 to 9 inclusive, which will
   * be used as the priority for messages sent using this destination.<p>
   *
   * The default value for this property is null, which indicates that the
   * priority of the message will be set by the application.<p>
   *
   * For compatibility with MQJMS, the value of -2 will be treated in the same
   * way as null.
   *
   * @param priority The priority to be used for messages sent using this Destination.
   * @throws JMSException If the value provided is not valid.
   */
  public void setPriority(Integer priority) throws JMSException;
  public void setPriorityDefault(Integer priority) throws JMSException;

  /**
   * Get the priority.
   * @return the priority
   * @see JmsDestination#setPriority
   */
  public Integer getPriority();
  public Integer getPriorityDefault();

  /**
   * Set the required value for ReadAhead on all consumers created using
   * this JmsDestination.<p>
   *
   * Please see {@link JmsConnectionFactory#setReadAhead(String)} for information
   * on the effect of the ReadAhead property.<br><br>
   *
   * Permitted values for the ReadAhead property of a JmsDestination are
   * as follows;
   *
   * <ul>
   * <li>{@link ApiJmsConstants#READ_AHEAD_AS_CONNECTION} - The default behaviour,
   *     where the value is inherited from the value set on the JmsConnectionFactory
   *     at the time that the Connection was created.
   * <li>{@link ApiJmsConstants#READ_AHEAD_ON} - All consumers created using
   *     this JmsDestination will have ReadAhead turned on.
   * <li>{@link ApiJmsConstants#READ_AHEAD_OFF} - All consumers created using
   *     this JmsDestination will have ReadAhead turned off.
   * </ul>
   * <br><br>
   *
   * Note that the value specified will override the value specified on the
   * JmsConnectionFactory if the AS_CONNECTION value is not specified.
   *
   * @param value The required value for ReadAhead on this JmsDestination
   * @throws JMSException If the value specified is not one of the supported constants.
   *
   * @see ApiJmsConstants#READ_AHEAD_AS_CONNECTION
   * @see ApiJmsConstants#READ_AHEAD_ON
   * @see ApiJmsConstants#READ_AHEAD_OFF
   * @see JmsConnectionFactory#setReadAhead(String)
   */
  public void setReadAhead(String value) throws JMSException;

  /**
   * Retrieve the current setting for the ReadAhead property for this
   * JmsDestination.<p>
   *
   * @return The current setting for ReadAhead.
   */
  public String getReadAhead();

  /**
   *  Get the name of the bus on which this JMS Destination resides.
   *
   *  @return String The name of the Bus.
   */
  public String getBusName();

  /**
   *  Set the name of the bus on which this JMS Destination resides.
   *  Setting this property defines the name of the bus on which the
   *  Destination is hosted. This enables applications to send messages to
   *  Destinations outside the local bus - for example remote MQ networks.
   *
   *  By default this property is set to null, indicating that the
   *  Destination resides on the local bus. The setting of this property is
   *  optional, and the value of empty String is taken to be equivalent
   *  to null.
   *
   *  @throws JMSException
   */
  public void setBusName(String busName)throws JMSException;

}
