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

import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * ProducerType is a type-safe enumeration for Producer types.
 */
public final class ProducerType {

  private static TraceComponent tc = SibTr.register(ProducerType.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /**  Constant denoting a currently unknown producer type           */
  public  final static ProducerType UNKNOWN  = new ProducerType("UNKNOWN" , (byte)0);

  /** Constant denoting Producer Type - Message Processor            */
  public  final static ProducerType MP       = new ProducerType("MP"      , (byte)1);

  /** Constant denoting Producer Type - Topology & Routing           */
  public  final static ProducerType TRM      = new ProducerType("TRM"     , (byte)2);

  /** Constant denoting Producer Type - API Component                */
  public  final static ProducerType API      = new ProducerType("API"     , (byte)3);

  /** Constant denoting Producer Type - Pub Sub Bridge               */
  public  final static ProducerType PSB      = new ProducerType("PSB"     , (byte)4);

  /*  Array of defined ProducerTypes - needed by getProducerType    */
  private final static ProducerType[] set = {UNKNOWN
                                            ,MP
                                            ,TRM
                                            ,API
                                            ,PSB
                                            };

  private String name;
  private Byte   value;

  /* Private constructor - ensures the 'constants' define here are the total set. */
  private ProducerType(String aName, byte aValue) {
    name  = aName;
    value = Byte.valueOf(aValue);
  }

  /**
   * Returns the corresponding ProducerType for a given Byte.
   * This method should NOT be called by any code outside the MFP component.
   * It is only public so that it can be accessed by sub-packages.
   *
   * @param  aValue         The Byte for which an ProducerType is required.
   *
   * @return The corresponding ProducerType
   */
  public final static ProducerType getProducerType(Byte aValue) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc,"Value = " + aValue);
    return set[aValue.intValue()];
  }

  /**
   * Returns the Byte representation of the ProducerType.
   * This method should NOT be called by any code outside the MFP component.
   * It is only public so that it can be accessed by sub-packages.
   *
   * @return  The Byte representation of the instance.
   */
  public final Byte toByte() {
    return value;
  }

  /**
   * Returns the name of the ProducerType.
   *
   * @return  The name of the instance.
   */
  public final String toString() {
    return name;
  }

}
