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

import javax.jms.JMSException;

import com.ibm.websphere.sib.api.jms.JmsDestination;

/**
 * This interface provides utility methods that are used by the JMS implementation
 * to encode and decode a JmsDestination object to an efficient byte[] representation
 * for the purposes of transmission within the message.
 *
 * This class is specifically NOT tagged as ibm-spi because by definition it is not
 * intended for use by either customers or ISV's.
 *
 * @author matrober
 */
public interface MessageDestEncodingUtils
{

  /**
   * Returns the efficient byte[] representation of the parameter destination
   * that can be stored in the message for transmission. The boolean parameter
   * indicates whether a full (normal dest) or partial (reply dest) encoding
   * should be carried out.
   *
   * Throws a JMSException if there are problems during the serialization process, for
   *    example if the parameter is null.
   */
  public byte[] getMessageRepresentationFromDest(JmsDestination dest, EncodingLevel encodingLevel) throws JMSException;

  /**
   * Inflates the efficient byte[] representation from the message into a
   * JmsDestination object.
   *
   * Throws a JMSException if there are problems during the deserialization process, for
   *    example if the parameter is null.
   */
  public JmsDestination getDestinationFromMsgRepresentation(byte[] msgForm) throws JMSException;
}
