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

package com.ibm.ws.sib.mfp.trm;

import com.ibm.ws.sib.mfp.trm.TrmMessageFactory;

import com.ibm.ws.sib.mfp.*;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * A singleton TrmMessageFactory is created at static initialization
 * and is subsequently used for the creation of all new and inbound TRM Messages
 * of any sub-type.
 */
public abstract class TrmMessageFactory {

  private final static String TRM_MESSAGE_FACTORY_CLASS = "com.ibm.ws.sib.mfp.impl.TrmMessageFactoryImpl";

  private static TraceComponent tc = SibTr.register(TrmMessageFactory.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  //Liberty COMMS change
  // Making TrmMessageFactoryImpl as singleton
  volatile private static TrmMessageFactory _instance=null;

  /**
   *  Get the singleton TrmMessageFactory which is to be used for
   *  creating TRM Message instances.
   *
   *  @return The TrmMessageFactory
   *
  */
  public static TrmMessageFactory getInstance()  {
	  if (_instance == null) {
		  synchronized(TrmMessageFactory.class) {
			  if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createFactoryInstance");
			    try {
			      Class cls = Class.forName(TRM_MESSAGE_FACTORY_CLASS);
			      _instance = (TrmMessageFactory) cls.newInstance();
			    }
			    catch (Exception e) {
			      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.TrmMessageFactory.createFactoryInstance", "112");
			      SibTr.error(tc,"UNABLE_TO_CREATE_TRMFACTORY_CWSIF0021",e);
			    }
			    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createFactoryInstance",_instance);
			  }
		   }

	  return _instance;
  }

  /**
   *  Create a new, empty TrmClientBootstrapRequest message
   *
   *  @return The new TrmClientBootstrapRequest.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract TrmClientBootstrapRequest createNewTrmClientBootstrapRequest() throws MessageCreateFailedException;

  /**
   *  Create a new, empty TrmClientBootstrapReply message
   *
   *  @return The new TrmClientBootstrapReply.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract TrmClientBootstrapReply createNewTrmClientBootstrapReply() throws MessageCreateFailedException;

  /**
   *  Create a new, empty TrmClientAttachRequest message
   *
   *  @return The new TrmClientAttachRequest.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract TrmClientAttachRequest createNewTrmClientAttachRequest() throws MessageCreateFailedException;

  /**
   *  Create a new, empty TrmClientAttachRequest2 message
   *
   *  @return The new TrmClientAttachRequest2.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract TrmClientAttachRequest2 createNewTrmClientAttachRequest2() throws MessageCreateFailedException;

  /**
   *  Create a new, empty TrmClientAttachReply message
   *
   *  @return The new TrmClientAttachReply.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract TrmClientAttachReply createNewTrmClientAttachReply() throws MessageCreateFailedException;

  /**
   *  Create a new, empty TrmMeConnectRequest message
   *
   *  @return The new TrmMeConnectRequest.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract TrmMeConnectRequest createNewTrmMeConnectRequest() throws MessageCreateFailedException;

  /**
   *  Create a new, empty TrmMeConnectReply message
   *
   *  @return The new TrmMeConnectReply.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract TrmMeConnectReply createNewTrmMeConnectReply() throws MessageCreateFailedException;

  /**
   *  Create a new, empty TrmMeLinkRequest message
   *
   *  @return The new TrmMeLinkRequest.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract TrmMeLinkRequest createNewTrmMeLinkRequest() throws MessageCreateFailedException;

  /**
   *  Create a new, empty TrmMeLinkReply message
   *
   *  @return The new TrmMeLinkReply.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract TrmMeLinkReply createNewTrmMeLinkReply() throws MessageCreateFailedException;

  /**
   *  Create a new, empty TrmMeBridgeRequest message
   *
   *  @return The new TrmMeBridgeRequest.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract TrmMeBridgeRequest createNewTrmMeBridgeRequest() throws MessageCreateFailedException;

  /**
   *  Create a new, empty TrmMeBridgeReply message
   *
   *  @return The new TrmMeBridgeReply.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract TrmMeBridgeReply createNewTrmMeBridgeReply() throws MessageCreateFailedException;

  /**
   *  Create a new, empty TrmMeBridgeBootstrapRequest message
   *
   *  @return The new TrmMeBridgeBootstrapRequest.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract TrmMeBridgeBootstrapRequest createNewTrmMeBridgeBootstrapRequest() throws MessageCreateFailedException;

  /**
   *  Create a new, empty TrmMeBridgeBootstrapReply message
   *
   *  @return The new TrmMeBridgeBootstrapReply.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract TrmMeBridgeBootstrapReply createNewTrmMeBridgeBootstrapReply() throws MessageCreateFailedException;

  /**
   *  Create a TrmFirstContactMessage to represent an inbound message.
   *
   *  @param rawMessage  The inbound byte array containging a complete message
   *  @param offset      The offset within the byte array at which the message begins
   *  @param length      The length of the message within the byte array
   *
   *  @return The new TrmFirstContactMessage
   *
   *  @exception MessageDecodeFailedException Thrown if the inbound message could not be decoded
   */
  public abstract TrmFirstContactMessage createInboundTrmFirstContactMessage(byte rawMessage[], int offset, int length)
                                                                            throws MessageDecodeFailedException;

  /**
   *  Create a TrmRouteData message
   *
   *  @return The new TrmRouteData.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract TrmRouteData createTrmRouteData() throws MessageCreateFailedException;

}
