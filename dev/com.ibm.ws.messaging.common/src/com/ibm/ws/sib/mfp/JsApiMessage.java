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
 * JsApiMessage extends the general JsMessage interface and additionally
 * provides get/set methods for all the common API Meta-Data fields.
 * It may be used for accessing and processing any Jetstream API message.
 * <p>
 * All API message types (e.g. JMS) are specializations of
 * JsApiMessage and can be 'made' from an existing JsApiMessage of the
 * appropriate type.
 *
 */
public interface JsApiMessage extends JsMessage {

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /**
   *  Get the contents of the ApiMessageId field from the message header.
   *
   *  @return A String containing the ApiMessageId in the form ID:xxxx
   *          where xxxx is the character representation of the binary value.
   *          Null is returned if the field was not set.
   */
  public String getApiMessageId();

  /**
   *  Get the contents of the CorrelationId field from the message header.
   *
   *  @return A String containing the CorrelationId - either an
   *          arbitrary String or one of the form ID:xxxx
   *          where xxxx is the character representation of the binary value.
   *          Null is returned if the field was not set.
   */
  public String getCorrelationId();

  /**
   *  Get the contents of the Userid field from the message header.
   *
   *  @return A String containing the Userid.
   *          Null is returned if the field was not set.
   */
  public String getUserid();

  /**
   *  Get the contents of the Format field from the message header.
   *
   *  @return A String containing the Format.
   *          Null is returned if the field was not set.
   */
  public String getFormat();


  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /**
   *  Set the contents of the ApiMessageId field in the message header.
   *
   *  @param value A String containing the ApiMessageId in the form ID:xxxx
   *               where x is in the range 0-9.A-F,a-f. There must be an
   *               even number of xs.
   *  @exception   IllegalArgumentException thrown if the value parameter is invalid.
   */
  public void setApiMessageId(String value);

  /**
   *  Set the contents of the CorrelationId field in the message header.
   *
   *  @param value Either an arbitrary String not starting with "ID:" or
   *               a String of the form ID:xxxx where x is in the
   *               range 0-9.A-F,a-f. There must be an even number of xs.
   */
  public void setCorrelationId(String value);

  /**
   *  Set the contents of the Userid field in the message header.
   *
   *  @param value A String containing the Userid.
   */
  public void setUserid(String value);

  /**
   *  Set the contents of the Format field in the message header.
   *
   *  @param value A String containing the Format.
   */
  public void setFormat(String value);

  /* **************************************************************************/
  /* UserProperty methods                                                     */
  /* **************************************************************************/

  /**
   *  Return the User Property stored in the Message under the given name.
   *  <p>
   *  Message Properties are stored as name-value pairs where the value may be any
   *  Object which implements java.io.Serializable.
   *  This method may only be used to retrieve user properties, and not to
   *  retrieve system (SI_) or JMS properties.
   *  The property name passed into the method should not include the "user."
   *  prefix.
   *  <p>
   *  The reference returned is to a copy of the Object stored in the
   *  Message, so any changes made to it will not be reflected in the Message.
   *
   *  @param name  The name of the Property to be returned.
   *
   *  @return Serializable A reference to the Message Property.
   *               Null is returned if there is no such item.
   *
   *  @exception IOException De-serialization of the Property failed.
   *  @exception ClassNotFoundException De-serialization of the Property failed
   *             because a necessary Java Class could not be found.
   */
  public Serializable getUserProperty(String name) throws IOException, ClassNotFoundException;

  /**
   *  Return a boolean indicating whether a User Property with the given name
   *  exists in the message.
   *  <p>
   *  This method may only be determine the existence of  user properties, and
   *  not to determine the existence of system (SI_) or JMS properties.
   *  The property name passed into the method should not include the "user."
   *  prefix.
   *
   *  @return True if the property exists in the message, otherwise false.
   */
  public boolean userPropertyExists(String name);


  /* **************************************************************************/
  /* SystemContextItem methods                                                */
  /* **************************************************************************/

  /**
   *  This method is used by a SystemContextHandler to place some context in
   *  a message. The key should be unique and this feature recommends using
   *  the name of the class implementing the SystemContextHandler. If null
   *  is passed in as a value it has the effect of removing the specified key
   *  from the SystemContext.
   *  <p>
   *  It is not valid for the key to be null and if an attempt is made to use
   *  a null key then a IllegalArgumentException will be thrown.
   *  <p>
   *  If an error occurs during serialization an IOException will be thrown</p>
   *
   * @param key   the name of the system context item
   * @param value the Serializable system context item
   * @throws IllegalArgumentException if the key is null
   * @throws IOException if the specified value cannot be serialized.
   */
  public void putSystemContextItem(String key, Serializable value)
    throws IllegalArgumentException, IOException;

  /**
   *  This method returns some piece of context to the SystemContextHandler.
   *  If there is no entry associated with the specified key null will be
   *  returned.
   *  <p>
   *  It is not valid for the key to be null and if an attempt is made to use
   *  a null key then a IllegalArgumentException will be thrown.
   *  <p>
   *  If an error occurs during de-serialization an IOException will be
   *  thrown, if the serialized objects class cannot be found a
   *  ClassNotFoundException will be thrown.
   *
   * @param key the name of the system context item
   * @return Serializable the system context item
   * @throws IllegalArgumentException if the key is null
   * @throws IOException if the system context item could not be de-serialized
   * @throws ClassNotFoundException if the system context items class could not be found.
   */
  public Serializable getSystemContextItem(String key)
    throws IllegalArgumentException, IOException, ClassNotFoundException;


  /* **************************************************************************/
  /* Pub-sub bridge-related methods                                           */
  /* **************************************************************************/

 
 
  /* **************************************************************************/
  /* Methods for Message Control Classification                               */
  /* **************************************************************************/

  /**
   *  Get the Message Control Classification from the message header.
   *
   *  @return A String containing the Message Control Classification
   *          Null is returned if the field was not set.
   */
  public String getMessageControlClassification();

  /**
   *  Set the Message Control Classification into the message header.
   *
   *  @param  value A String containing the Message Control Classification
   */
  public void setMessageControlClassification(String value);

}
