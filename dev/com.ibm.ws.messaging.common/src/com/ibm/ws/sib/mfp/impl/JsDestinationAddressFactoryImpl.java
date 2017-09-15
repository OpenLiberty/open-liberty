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

import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.ws.sib.mfp.JsDestinationAddressFactory;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.processor.SIMPConstants;

import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;

/**
 *  This class extends the abstract com.ibm.ws.sib.mfp.JsDestinationAddressFactory
 *  class and provides the concrete implementations of the methods for
 *  creating SIDestinationAddresses and JsDestinationAddresss.
 *  <p>
 *  The class must be public so that the abstract class static
 *  initialization can create an instance of it at runtime.
 *
 */
public final class JsDestinationAddressFactoryImpl extends JsDestinationAddressFactory {

  private static TraceComponent tc = SibTr.register(JsDestinationAddressFactoryImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /* Constants for creating the System Destination name */
  private static final String SYSTEM_PREFIX = SIMPConstants.SYSTEM_DESTINATION_PREFIX;
  private static final String SYSTEM_SEPARATOR = String.valueOf(SIMPConstants.SYSTEM_DESTINATION_SEPARATOR);
  private static final int SYSTEM_LENGTH = SYSTEM_PREFIX.length()
                                         + SYSTEM_SEPARATOR.length()
                                         + (new SIBUuid8()).getStringLength();

  /**
   *  Create a new SIDestinationAddress to represent an SIBus Destination.
   *
   *  @param destinationName  The name of the SIBus Destination
   *  @param localOnly        Indicates that the Destination should be limited
   *                          to only the queue or mediation point on the Messaging
   *                          Engine that the application is connected to, if one
   *                          exists. If no such message point exists then the option
   *                          is ignored.
   *
   *  @return SIDestinationAddress The new SIDestinationAddress.
   *
   *  @exception NullPointerException Thrown if the destinationName parameter is null.
   */
  public final SIDestinationAddress createSIDestinationAddress(String  destinationName
                                                              ,boolean localOnly
                                                              )
                                                              throws NullPointerException {
    if (destinationName == null)  {
      throw new NullPointerException("destinationName");
    }
    return new JsDestinationAddressImpl(destinationName, localOnly, null, null, false);
  }


  /**
   *  Create a new SIDestinationAddress to represent an SIBus Destination.
   *
   *  @param destinationName  The name of the SIBus Destination
   *  @param busName          The name of the bus on which this SIBus Destination
   *                          exists.
   *
   *  @return SIDestinationAddress The new SIDestinationAddress.
   *
   *  @exception NullPointerException Thrown if the destinationName parameter is null.
   */
  public final SIDestinationAddress createSIDestinationAddress(String destinationName
                                                              ,String busName
                                                              )
                                                              throws NullPointerException {
    if (destinationName == null)  {
      throw new NullPointerException("destinationName");
    }
    return new JsDestinationAddressImpl(destinationName, false, null, busName, false);
  }


  /**
   *  Create a new SIDestinationAddress to represent an SIBus Destination.
   *
   *  @param destinationName  The name of the SIBus Destination
   *  @param localOnly        Indicates that the Destination should be limited
   *                          to only the queue or mediation point on the Messaging
   *                          Engine that the application is connected to, if one
   *                          exists. If no such message point exists then the option
   *                          is ignored.
   *  @param busName          The name of the bus on which this SIBus Destination
   *                          exists.
   *
   *  @return SIDestinationAddress The new SIDestinationAddress.
   *
   *  @exception NullPointerException Thrown if the destinationName parameter is null.
   */
  public final SIDestinationAddress createSIDestinationAddress(String destinationName
                                                              ,boolean localOnly
                                                              ,String busName
                                                              )
                                                              throws NullPointerException {
    if (destinationName == null)  {
      throw new NullPointerException("destinationName");
    }
    return new JsDestinationAddressImpl(destinationName, localOnly, null, busName, false);
  }

  /**
   *  Create a JsDestinationAddress from the given parameters
   *
   *  @param destinationName  The name of the SIBus Destination
   *  @param localOnly        Indicates that the Destination should be limited
   *                          to only the queue or mediation point on the Messaging
   *                          Engine that the application is connected to, if one
   *                          exists. If no such message point exists then the option
   *                          is ignored.
   *  @param localOnly        Indicates that the Destination should be localized
   *                          to the local Messaging Engine.
   *  @param meId             The Id of the Message Engine where the destination is localized
   *
   *  @return JsDestinationAddress The JsDestinationAddress corresponding to the buffer contents.
   *
   *  @exception NullPointerException Thrown if the destinationName parameter is null.
   */
  public final JsDestinationAddress createJsDestinationAddress(String   destinationName
                                                              ,boolean  localOnly
                                                              ,SIBUuid8 meId
                                                              )
                                                              throws NullPointerException {
    if (destinationName == null)  {
      throw new NullPointerException("destinationName");
    }
    return new JsDestinationAddressImpl(destinationName, localOnly, meId, null, false);
  }


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
  public final JsDestinationAddress createJsDestinationAddress(String   destinationName
                                                              ,boolean  localOnly
                                                              ,SIBUuid8 meId
                                                              ,String   busName
                                                              )
                                                              throws NullPointerException {
    if (destinationName == null)  {
      throw new NullPointerException("destinationName");
    }
    return new JsDestinationAddressImpl(destinationName, localOnly, meId, busName, false);
  }

 

  /**
   *  Create a System JsDestinationAddress from the given parameters
   *
   *  @param prefix           The prefix to be used to generate the destination name
   *  @param meId             The Id to be used to generate the destination name
   *
   *  @return JsDestinationAddress The JsDestinationAddress corresponding to the buffer contents.
   *                               The generated destination name will be _S + prefix + _ + meId.toString().
   *
   *  @exception NullPointerException Thrown if the meId parameter is null.
   */
  public final JsDestinationAddress createJsSystemDestinationAddress(String   prefix
                                                                    ,SIBUuid8 meId
                                                                    )
                                                                    throws NullPointerException {
    /* meId should never be null if busName is not specified                  */
    if (meId == null)  {
      throw new NullPointerException("MEId");
    }
    return createJsSystemDestinationAddress(prefix, meId, null);
  }


  /**
   *  Create a System JsDestinationAddress from the given parameters
   *
   *  @param prefix           The prefix to be used to generate the destination name
   *  @param meId             The Id to be used to generate the destination name
   *  @param busName          The name of the Bus where the destination is localized
   *
   *  @return JsDestinationAddress The JsDestinationAddress corresponding to the buffer contents.
   *                               The generated destination name will be _S + prefix + _ + meId.toString(),
   *                               however, if the busName is not null, a zero MeId will be substituted
   *                               if null is passed in as meId.
   *
   *  @exception NullPointerException Thrown if both the meId and BusName parameters are null.
   */
  public final JsDestinationAddress createJsSystemDestinationAddress(String   prefix
                                                                    ,SIBUuid8 meId
                                                                    ,String   busName
                                                                    )
                                                                    throws NullPointerException {

    /* BusName and meId should never both be null */
    if ((meId == null) && (busName == null))  {
      throw new NullPointerException("BusName and MEId");
    }

    StringBuilder s;

    if (prefix != null)  {
      s = new StringBuilder(SYSTEM_LENGTH + prefix.length());
      s.append(SYSTEM_PREFIX);
      s.append(prefix);
    }
    /* If the prefix is null, it should be treated as a zero length String    */
    else {
      s = new StringBuilder(SYSTEM_LENGTH);
      s.append(SYSTEM_PREFIX);
    }

    s.append(SYSTEM_SEPARATOR);

    /* Null MeId is OK if we have a BusName. The zero Uuid string is used.    */
    if (meId != null)  {
      s.append(meId.toString());
    }
    else {
      s.append(SIBUuid8.toZeroString());
    }

    return new JsDestinationAddressImpl(new String(s), false, null, busName, false);
  }

}
