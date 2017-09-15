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

import java.io.UnsupportedEncodingException;

/**
 *  JsJmsTextMessage extends JsJmsMessage and adds the get/set methods specific
 *  to a JMS TextMessage.
 */
public interface JsJmsTextMessage extends JsJmsMessage {

  /**
   *  Get the body (payload) of the message.
   *
   *  @return The String representing the body of the message.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public String getText() throws UnsupportedEncodingException;

  /**
   *  Set the body (payload) of the message.
   *
   *  @param payload  The String containing the payload to be included in the message.
   */
  public void setText(String payload);

}
