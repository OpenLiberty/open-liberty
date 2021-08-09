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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 *  A singleton JsJmsMessageFactory is created at static initialization
 *  and is subsequently used for the creation of all new JmsMessages
 *  of any type.
 *  <p>
 *  Inbound messages are not created using this factory as they are
 *  created from an existing JsMessage using the methods provided by
 *  the JsMessage interface.
 *
 */
public abstract class JsJmsMessageFactory {

  private static TraceComponent tc = SibTr.register(JsJmsMessageFactory.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  private static JsJmsMessageFactory instance = null;
  private static Exception  createException = null;

  static {
    
    /* Create the singleton factory instance                                  */
    try {
      createFactoryInstance();
    }
    catch (Exception e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.JsJmsMessageFactory.<clinit>", "62");
      createException = e;
    }
  }

  /**
   *  Get the singleton JsJmsMessageFactory which is to be used for
   *  creating JsJmsMessage instances.
   *
   *  @return The JsJmsMessageFactory
   *
   *  @exception Exception The method rethrows any Exception caught during
   *                       creaton of the singleton factory.
   */
  public static JsJmsMessageFactory getInstance() throws Exception {

    /* If instance creation failed, throw on the Exception                    */
    if (instance == null) {
      throw createException;
    }

    /* Otherwise, return the singleton                                        */
    return instance;
  }

  /**
   *  Create a new, empty null-bodied JMS Message.
   *  To be called by the API component.
   *
   *  @return The new JsJmsMessage
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract JsJmsMessage createJmsMessage() throws MessageCreateFailedException;

  /**
   *  Create a new, empty JMS BytesMessage.
   *  To be called by the API component.
   *
   *  @return The new JsJmsBytesMessage
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract JsJmsBytesMessage createJmsBytesMessage() throws MessageCreateFailedException;

  /**
   *  Create a new, empty JMS MapMessage.
   *  To be called by the API component.
   *
   *  @return The new JsJmsMapMessage
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract JsJmsMapMessage createJmsMapMessage() throws MessageCreateFailedException;

  /**
   *  Create a new, empty JMS ObjectMessage.
   *  To be called by the API component.
   *
   *  @return The new JsJmsObjectMessage
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract JsJmsObjectMessage createJmsObjectMessage() throws MessageCreateFailedException;

  /**
   *  Create a new, empty JMS StreamMessage.
   *  To be called by the API component.
   *
   *  @return The new JsJmsStreamMessage
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract JsJmsStreamMessage createJmsStreamMessage() throws MessageCreateFailedException;

  /**
   *  Create a new, empty JMS TextMessage.
   *  To be called by the API component.
   *
   *  @return The new JsJmsTextMessage
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract JsJmsTextMessage createJmsTextMessage() throws MessageCreateFailedException;

  /**
   *  Create the singleton Factory instance.
   *
   *  @exception Exception The method rethrows any Exception caught during
   *                       creaton of the singleton factory.
   */
  private static void createFactoryInstance() throws Exception {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createFactoryInstance");
    try {
      Class cls = Class.forName(MfpConstants.JS_JMS_MESSAGE_FACTORY_CLASS);
      instance = (JsJmsMessageFactory) cls.newInstance();
    }
    catch (Exception e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.JsJmsMessageFactory.createFactoryInstance", "133");
      SibTr.error(tc,"UNABLE_TO_CREATE_JMSFACTORY_CWSIF0011",e);
      throw e;
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createFactoryInstance");
  }

}
