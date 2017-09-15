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

import com.ibm.websphere.sib.SIDestinationAddressFactory;

import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.utils.SIBUuid8;

/**
 * A singleton JsDestinationAddressFactory is created at static initialization
 * and is subsequently used for the creation of all JsDestinationAddresss.
 * This factory extends SIDestinationAddressFactory and is for use by Jetstream
 * components only.
 */
public abstract class JsDestinationAddressFactory extends SIDestinationAddressFactory {

  /**
   *  Create a JsDestinationAddress from the given parameters
   *
   *  @param destinationName  The name of the SIBus Destination
   *  @param localOnly        Indicates that the Destination should be limited
   *                          to only the queue or mediation point on the Messaging
   *                          Engine that the application is connected to, if one
   *                          exists. If no such message point exists then the option
   *                          is ignored.
   *  @param meId             The Id of the Message Engine where the destination is localized
   *
   *  @return JsDestinationAddress The JsDestinationAddress corresponding to the buffer contents.
   *
   *  @exception NullPointerException Thrown if the destinationName parameter is null.
   */
  public abstract JsDestinationAddress createJsDestinationAddress(String   destinationName
                                                                 ,boolean  localOnly
                                                                 ,SIBUuid8 meId
                                                                 )
                                                                 throws NullPointerException;

  /**
   *  Create a JsDestinationAddress from the given parameters
   *
   *  @param destinationName  The name of the SIBus Destination
   *  @param localOnly        Indicates that the Destination should be limited
   *                          to only the queue or mediation point on the Messaging
   *                          Engine that the application is connected to, if one
   *                          exists. If no such message point exists then the option
   *                          is ignored.
   *  @param meId             The Id of the Message Engine where the destination is localized
   *  @param busName          The name of the Bus where the destination is localized
   *
   *  @return JsDestinationAddress The JsDestinationAddress corresponding to the buffer contents.
   *
   *  @exception NullPointerException Thrown if the destinationName parameter is null.
   */
  public abstract JsDestinationAddress createJsDestinationAddress(String   destinationName
                                                                 ,boolean  localOnly
                                                                 ,SIBUuid8 meId
                                                                 ,String   busName
                                                                 )
                                                                 throws NullPointerException;

 
  /**
   *  Create a System JsDestinationAddress from the given parameters
   *
   *  @param prefix  The prefix to be used to generate the destination name
   *  @param meId    The Id to be used to generate the destination name
   *
   *  @return JsDestinationAddress The JsDestinationAddress corresponding to the buffer contents.
   *
   *  @exception NullPointerException Thrown if the meId parameter is null.
   */
  public abstract JsDestinationAddress createJsSystemDestinationAddress(String   prefix
                                                                       ,SIBUuid8 meId
                                                                       )
                                                                       throws NullPointerException;

  /**
   *  Create a System JsDestinationAddress from the given parameters
   *
   *  @param prefix   The prefix to be used to generate the destination name
   *  @param meId     The Id to be used to generate the destination name
   *  @param busName  The name of the Bus where the destination is localized
   *
   *  @return JsDestinationAddress The JsDestinationAddress corresponding to the buffer contents.
   *
   *  @exception NullPointerException Thrown if both the meId and BusName parameters are null.
   */
  public abstract JsDestinationAddress createJsSystemDestinationAddress(String   prefix
                                                                       ,SIBUuid8 meId
                                                                       ,String   busName
                                                                       )
                                                                       throws NullPointerException;

}
