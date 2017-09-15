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
 * TrmMessageType is a type-safe enumeration which indicates the type of a TRM
 * message.
 */
public final class TrmMessageType implements IntAble {

  private static TraceComponent tc = SibTr.register(TrmMessageType.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /** Integer value of the Trm Message Types                                  */
  public final static int UNKNOWN_INT            = 0;
  public final static int ROUTE_DATA_INT         = 1;

  /**  Constant denoting an indeterminate TRM Message  */
  public  final static TrmMessageType UNKNOWN            = new TrmMessageType("UNKNOWN"            ,UNKNOWN_INT            );

  /**  Constant denoting a Route data Message */
  public  final static TrmMessageType ROUTE_DATA         = new TrmMessageType("ROUTE_DATA"         ,ROUTE_DATA_INT         );

  /*  Array of defined TrmMessageTypes - needed by getTrmMessageType  */
  private final static TrmMessageType[] set = {UNKNOWN
                                              ,ROUTE_DATA
                                              };

  private String name;
  private int    value;

  /* Private constructor - ensures the 'constants' defined here are the total set. */
  private TrmMessageType(String aName, int aValue) {
    name  = aName;
    value = aValue;
  }

  /**
   * Returns the corresponding TrmMessageType for a given integer.
   * This method should NOT be called by any code outside the MFP component.
   * It is only public so that it can be accessed by sub-packages.
   *
   * @param  aValue         The integer for which an TrmMessageType is required.
   *
   * @return The corresponding TrmMessageType
   */
  public final static TrmMessageType getTrmMessageType(int aValue) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc,"Value = " + aValue);
    return set[aValue];
  }

  /**
   * Returns the integer representation of the TrmMessageType.
   * This method should NOT be called by any code outside the MFP component.
   * It is only public so that it can be accessed by sub-packages.
   *
   * @return  The int representation of the instance.
   */
  public final int toInt() {
    return value;
  }

  /**
   * Returns the name of the TrmMessageType.
   *
   * @return  The name of the instance.
   */
  public final String toString() {
    return name;
  }

}
