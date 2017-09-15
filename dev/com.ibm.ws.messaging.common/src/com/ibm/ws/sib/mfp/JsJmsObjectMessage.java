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

import java.io.IOException;
import java.io.Serializable;

/**
 *  JsJmsObjectMessage extends JsJmsMessage and adds the get/set methods specific
 *  to a JMS ObjectMessage.
 */
public interface JsJmsObjectMessage extends JsJmsMessage {

  /**
   *  Get the byte array containing the serialized object which forms the
   *  payload of the message.
   *  The default value is null.
   *
   *  @return A byte array containing the serialized Object representing the
   *  payload of the message.
   *  @throws ObjectFailedToSerializeException if the real version of the object cannot be serialized
   */
  public byte[] getSerializedObject() throws ObjectFailedToSerializeException;

  /**
   *  Set the body (payload) of the message.
   *
   *  @param payload A byte array containing the serialized Object representing
   *  the payload of the message.
   */
  public void setSerializedObject(byte[] payload);


  /**
   *  Get the real object which forms the payload of the message.
   *  The default value is null.
   *
   *  @return the Serializable object that is the payload of the message.
   *  @throws IOException if the serialized version of the object cannot be deserialized
   *  @throws ClassNotFoundException if the class for the serialized object cannot be found
   */
  public Serializable getRealObject() throws IOException, ClassNotFoundException;

  /**
   *  Set the body (payload) of the message.
   *
   *  @param payload A serializable Object that is the payload of the message.
   */
  public void setRealObject(Serializable payload);

}
