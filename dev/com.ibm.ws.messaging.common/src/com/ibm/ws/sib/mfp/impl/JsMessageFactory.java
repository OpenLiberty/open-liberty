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

package com.ibm.ws.sib.mfp.impl;

import java.util.List;

import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.MessageRestoreFailedException;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * A singleton JsMessageFactory is created at static initialization
 * and is subsequently used for the creation of all inbound JsMessages
 * of any sub-type.
 * It may also be used for the creation of new 'vanilla' JsMessages but
 * not for new instances of sub-types, which are created by more specific
 * factories.
 *
 */
public abstract class JsMessageFactory {

  private static TraceComponent tc = SibTr.register(JsMessageFactory.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  private static JsMessageFactory instance = null;
  private static Exception  createException = null;

  static {
    
    /* Create the singleton factory instance                                  */
    try {
      createFactoryInstance();
    }
    catch (Exception e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.JsMessageFactory.<clinit>", "95");
      createException = e;
    }
  }

  /**
   * Get the singleton JsMessageFactory which is to be used for
   * creating JsMessage instances.
   *
   * @return The JsMessageFactory
   *
   * @exception Exception The method rethrows any Exception caught during
   *                       creaton of the singleton factory.
   */
  public static JsMessageFactory getInstance() throws Exception {

    /* If instance creation failed, throw on the Exception                    */
    if (instance == null) {
      throw createException;
    }

    /* Otherwise, return the singleton                                        */
    return instance;
  }

  /**
   * Create a JsMessage to represent an inbound message.
   * (To be called by the Communications component.)
   *
   * @param rawMessage  The inbound byte array containging a complete message
   * @param offset      The offset within the byte array at which the message begins
   * @param length      The length of the message within the byte array
   *
   * @return The new JsMessage
   *
   * @exception MessageDecodeFailedException Thrown if the inbound message could not be decoded
   */
  public abstract JsMessage createInboundJsMessage(byte rawMessage[], int offset, int length) throws MessageDecodeFailedException;

  /**
   * Create a JsMessage to represent an inbound message.
   * (To be called by the Communications component.)
   *
   * @param rawMessage      The inbound byte array containging a complete message
   * @param offset          The offset within the byte array at which the message begins
   * @param length          The length of the message within the byte array
   * @param commsConnection The CommsConnection object, if any, which is associated with this message
   *
   * @return The new JsMessage
   *
   * @exception MessageDecodeFailedException Thrown if the inbound message could not be decoded
   */
  public abstract JsMessage createInboundJsMessage(byte rawMessage[], int offset, int length, Object commsConnection) throws MessageDecodeFailedException;

  /**
   *  Create a JsMessage to represent an inbound message.
   *  (To be called by the Communications component.)
   *
   *  @param  slices         The List of DataSlices representing the inbound message.
   *
   *  @return The new JsMessage
   *
   *  @exception MessageDecodeFailedException Thrown if the inbound message could not be decoded
   */
  public abstract JsMessage createInboundJsMessage(List<DataSlice> slices) throws MessageDecodeFailedException;

  /**
   * Create a JsMessage to represent an inbound message.
   * (To be called by the Communications component.)
   *
   * @param  slices         The List of DataSlices representing the inbound message.
   * @param commsConnection The CommsConnection object, if any, which is associated with this message
   *
   * @return The new JsMessage
   *
   * @exception MessageDecodeFailedException Thrown if the inbound message could not be decoded
   */
  public abstract JsMessage createInboundJsMessage(List<DataSlice> slices, Object commsConnection) throws MessageDecodeFailedException;

 
  /**
   * Create a JsMessage to represent an inbound Web client message (to be called by the
   * Communications component).
   *
   * @param data the simple text encoding of a JMS message
   *
   * @exception MessageDecodeFailedException if the inbound message cannot be decoded
   */
  public abstract JsMessage createInboundWebMessage(String data) throws MessageDecodeFailedException;

  /**
   * Restore a JsMessage of any specializaton from a 'flattened' copy.
   *
   * @param  slices  The List of DataSlices representing the flattened message.
   * @param  store   The MessageStore from which the message is being recovered.
   *
   * @return The new JsMessage of appropriate specialization
   *
   * @exception MessageRestoreFailedException Thrown if the message could not be restored
   */
  public abstract JsMessage restoreJsMessage(List<DataSlice> slices, Object store) throws MessageRestoreFailedException;  // SIB0112b.mfp.1

  /**
   * Create the singleton Factory instance.
   *
   * @exception Exception The method rethrows any Exception caught during
   *                      creaton of the singleton factory.
   */
  private static void createFactoryInstance() throws Exception {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createFactoryInstance");
    try {
      Class cls = Class.forName(MfpConstants.JS_MESSAGE_FACTORY_CLASS);
      instance = (JsMessageFactory) cls.newInstance();
    }
    catch (Exception e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.JsMessageFactory.createFactoryInstance", "134");
      SibTr.error(tc,"UNABLE_TO_CREATE_FACTORY_CWSIF0001",e);
      throw e;
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createFactoryInstance");
  }
}
