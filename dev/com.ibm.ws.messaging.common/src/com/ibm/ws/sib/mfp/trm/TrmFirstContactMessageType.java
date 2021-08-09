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

import com.ibm.ws.sib.mfp.*;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * TrmFirstContactMessageType is a type-safe enumeration which indicates the
 * type of a TRM First Contact message.
 */
public final class TrmFirstContactMessageType implements IntAble {

  private static TraceComponent tc = SibTr.register(TrmFirstContactMessageType.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /**  Constant denoting a Client Bootstrap Request  */
  public  final static TrmFirstContactMessageType CLIENT_BOOTSTRAP_REQUEST    = new TrmFirstContactMessageType("CLIENT_BOOTSTRAP_REQUEST"    ,0);

  /**  Constant denoting a Client Bootstrap Reply  */
  public  final static TrmFirstContactMessageType CLIENT_BOOTSTRAP_REPLY      = new TrmFirstContactMessageType("CLIENT_BOOTSTRAP_REPLY"      ,1);

  /**  Constant denoting a Client Attach Request  */
  public  final static TrmFirstContactMessageType CLIENT_ATTACH_REQUEST       = new TrmFirstContactMessageType("CLIENT_ATTACH_REQUEST"       ,2);

  /**  Constant denoting a Client Attach Request 2 */
  public  final static TrmFirstContactMessageType CLIENT_ATTACH_REQUEST2      = new TrmFirstContactMessageType("CLIENT_ATTACH_REQUEST2"      ,3);

  /**  Constant denoting a Client Attach Reply  */
  public  final static TrmFirstContactMessageType CLIENT_ATTACH_REPLY         = new TrmFirstContactMessageType("CLIENT_ATTACH_REPLY"         ,4);

  /**  Constant denoting a ME Bootstrap Request  */
  public  final static TrmFirstContactMessageType ME_CONNECT_REQUEST          = new TrmFirstContactMessageType("ME_CONNECT_REQUEST"          ,5);

  /**  Constant denoting a ME Bootstrap Reply  */
  public  final static TrmFirstContactMessageType ME_CONNECT_REPLY            = new TrmFirstContactMessageType("ME_CONNECT_REPLY"            ,6);

  /**  Constant denoting a ME Link Request  */
  public  final static TrmFirstContactMessageType ME_LINK_REQUEST             = new TrmFirstContactMessageType("ME_LINK_REQUEST"             ,7);

  /**  Constant denoting a ME Link Reply  */
  public  final static TrmFirstContactMessageType ME_LINK_REPLY               = new TrmFirstContactMessageType("ME_LINK_REPLY"               ,8);

  /**  Constant denoting a ME Bridge Request  */
  public  final static TrmFirstContactMessageType ME_BRIDGE_REQUEST           = new TrmFirstContactMessageType("ME_BRIDGE_REQUEST"           ,9);

  /**  Constant denoting a ME Bridge Reply  */
  public  final static TrmFirstContactMessageType ME_BRIDGE_REPLY             = new TrmFirstContactMessageType("ME_BRIDGE_REPLY"             ,10);

  /**  Constant denoting a ME Bridge Bootstrap Request */
  public  final static TrmFirstContactMessageType ME_BRIDGE_BOOTSTRAP_REQUEST = new TrmFirstContactMessageType("ME_BRIDGE_BOOTSTRAP_REQUEST", 11);

  /**  Constant denoting a ME Bridge Bootstrap Reply */
  public  final static TrmFirstContactMessageType ME_BRIDGE_BOOTSTRAP_REPLY   = new TrmFirstContactMessageType("ME_BRIDGE_BOOTSTRAP_REPLY",   12);

  /*  Array of defined TrmFirstContactMessageTypes - needed by getTrmFirstContactMessageType  */
  private final static TrmFirstContactMessageType[] set = {CLIENT_BOOTSTRAP_REQUEST
                                                          ,CLIENT_BOOTSTRAP_REPLY
                                                          ,CLIENT_ATTACH_REQUEST
                                                          ,CLIENT_ATTACH_REQUEST2
                                                          ,CLIENT_ATTACH_REPLY
                                                          ,ME_CONNECT_REQUEST
                                                          ,ME_CONNECT_REPLY
                                                          ,ME_LINK_REQUEST
                                                          ,ME_LINK_REPLY
                                                          ,ME_BRIDGE_REQUEST
                                                          ,ME_BRIDGE_REPLY
                                                          ,ME_BRIDGE_BOOTSTRAP_REQUEST
                                                          ,ME_BRIDGE_BOOTSTRAP_REPLY
                                                          };

  private String name;
  private int    value;

  /* Private constructor - ensures the 'constants' defined here are the total set. */
  private TrmFirstContactMessageType(String aName, int aValue) {
    name  = aName;
    value = aValue;
  }

  /**
   * Returns the corresponding TrmFirstContactMessageType for a given integer.
   * This method should NOT be called by any code outside the MFP component.
   * It is only public so that it can be accessed by sub-packages.
   *
   * @param  aValue         The integer for which an TrmFirstContactMessageType is required.
   *
   * @return The corresponding TrmFirstContactMessageType
   */
  public final static TrmFirstContactMessageType getTrmFirstContactMessageType(int aValue) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc,"Value = " + aValue);
    return set[aValue];
  }

  /**
   * Returns the integer representation of the TrmFirstContactMessageType.
   * This method should NOT be called by any code outside the MFP component.
   * It is only public so that it can be accessed by sub-packages.
   *
   * @return  The int representation of the instance.
   */
  public final int toInt() {
    return value;
  }

  /**
   * Returns the name of the TrmFirstContactMessageType.
   *
   * @return  The name of the instance.
   */
  public final String toString() {
    return name;
  }

}
