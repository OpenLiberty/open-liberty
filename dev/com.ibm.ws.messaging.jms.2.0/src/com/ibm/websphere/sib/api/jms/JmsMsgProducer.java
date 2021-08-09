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

import javax.jms.*;

/**
 * Contains provider specific methods relating to the javax.jms.MessageProducer interface.
 * 
 * <p>
 * Methods for which the security implications are not explicitly documented do not have 
 * any security considerations, do not check security permissions and do not affect any 
 * secured resources. 
 * 
 * @ibm-api
 * @ibm-was-base 
 */
public interface JmsMsgProducer extends MessageProducer
{

  /**
   * Set a default timeToLive for messages sent using this message producer.
   * 
   * @param timeToLive the default timeToLive value in ms to be used in
   * the send methods which don't provide a more specific value as a parameter. 
   * Must be in
   * the range 0 to ApiJmsConstants.MAX_TIME_TO_LIVE inclusive. 0 is interpreted
   * as unlimited. 
   * @see ApiJmsConstants#MAX_TIME_TO_LIVE
   * @see javax.jms.MessageProducer#setTimeToLive(long)
   * @throws JMSException if the supplied value is out of range
   */
  public void setTimeToLive(long timeToLive) throws JMSException;

  /**
   * Send a message to the Destination that was specified when the message producer
   * was created.
   * 
   * <p>A JMSSecurityException will be thrown if the user ID is not in the sender role for the destination.
   * 
   * @see javax.jms.MessageProducer#send(Message, int, int, long)
   * @param message the Message to be sent
   * @param deliveryMode one of DeliveryMode.PERSISTENT or DeliveryMode.NON_PERSISTENT.
   * These will be mapped to a quality of service using the settings in the connection
   * factory from which the MessageProducer was derived.
   * @param priority the priority with which to send the message, in the range 0 through 9
   * @param timeToLive the length of time in milliseconds before the message should be expired.
   * Must be in
   * the range 0 to ApiJmsConstants.MAX_TIME_TO_LIVE inclusive. 0 is interpreted
   * as unlimited. 
   * @see ApiJmsConstants#MAX_TIME_TO_LIVE
   * @throws JMSSecurityException with linked SINotAuthorizedException if the userID is not
   * in the sender role for the destination.
   * @throws JMSException if the provider is unable to send the message
   */
  public void send(Message message, int deliveryMode, int priority, long timeToLive)
    throws JMSException;

  /**
   * Send a message to the specified destination.
   * 
   * <p>A JMSSecurityException will be thrown if the user ID is not in the sender role for the destination.
   * 
   * @see javax.jms.MessageProducer#send(Destination, Message, int, int, long)
   * 
   * @param destination the Destination the message should be sent to.    
   * @param message the Message to be sent
   * @param deliveryMode one of DeliveryMode.PERSISTENT or DeliveryMode.NON_PERSISTENT.
   * These will be mapped to a quality of service using the settings in the connection
   * factory from which the MessageProducer was derived.
   * @param priority the priority with which to send the message, in the range 0 through 9
   * @param timeToLive the length of time in milliseconds before the message should be expired.
   * Must be in
   * the range 0 to ApiJmsConstants.MAX_TIME_TO_LIVE inclusive. 0 is interpreted
   * as unlimited. 
   * @see ApiJmsConstants#MAX_TIME_TO_LIVE
   * @throws JMSSecurityException with linked SINotAuthorizedException if the userID is not
   * in the sender role for the destination.
   * @throws JMSException if the provider is unable to send the message   
   */
  public void send(Destination destination, Message message, int deliveryMode,
                   int priority, long timeToLive)
    throws JMSException;

}
