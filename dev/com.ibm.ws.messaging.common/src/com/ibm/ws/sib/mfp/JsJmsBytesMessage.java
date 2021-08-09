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
package com.ibm.ws.sib.mfp;

/**
 *  JsJmsBytesMessage extends JsJmsMessage and adds the get/set methods specific
 *  to a JMS BytesMessage.
 */
public interface JsJmsBytesMessage extends JsJmsMessage {

  /**
   *  Get the body (payload) of the message.
   *
   *  @return The byte array representing the body of the message.
   */
  public byte[] getBytes();

  /**
   *  Set the body (payload) of the message.
   *  The act of setting the payload will cause a copy of the byte array to be
   *  made, in order to ensure that the payload sent matches the byte array
   *  passed in. If no copy was made it would be possible for the content to
   *  changed before the message was transmitted or delivered.
   *
   *  @param payload  The byte arraycontaining the payload to be included in the message.
   */
  public void setBytes(byte[] payload);

}
