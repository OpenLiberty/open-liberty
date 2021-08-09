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
package com.ibm.wsspi.jms;
 
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

/**
 * <p>This class provides support for setting a MessageListener on a 
 *   MessageConsumer, despite this not being allowed in a container. The intent
 *   is that this is only used by system code. It does not constitute an API.
 * </p>
 * 
 * <p>This class provides extenability points as the mechanism of setting a
 *   message listener may change based on provider and not all providers will
 *   be available at the time this code is written.
 * </p>
 *
 * <p>WAS build component: messaging</p>
 *
 * @author nottinga
 * @version 1.4
 * @since 1.0
 * @ibm-spi
 */
public class JmsMessageListenerSupport
{
  /**
   * <p>JMS providers implement this interface if they wish to allow 
   *   setMessageListener to be called by container services.
   * </p>
   * 
   * @ibm-spi
   */
  public interface MessageListenerSetter
  {
    /* ---------------------------------------------------------------------- */
    /* setMessageListener method                                    
    /* ---------------------------------------------------------------------- */
    /**
     * This method sets the supplied message listener on the supplied consumer.
     * 
     * @param consumer The consumer to set the message listener on.
     * @param listener The message listener to set on the conusmer.
     * @throws JMSException If the message listener cannot be set.
     */
    public void setMessageListener(MessageConsumer consumer, MessageListener listener) throws JMSException;
  }
  }

