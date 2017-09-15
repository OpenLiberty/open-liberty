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
 * PersistenceType is a type-safe enumeration for JMSDeliveryMode persistence values.
 * Note that the name returned by toString must match the valid JMS Message
 * Selector values for the two real values.
 */
public final class PersistenceType {

  private static TraceComponent tc = SibTr.register(PersistenceType.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);


  /**  Constant denoting a currently unknown persistence type           */
  public  final static PersistenceType UNKNOWN        = new PersistenceType("UNKNOWN"       , (byte)0);

  /** Constant denoting Persistence Type - Non-persistent               */
  public  final static PersistenceType NON_PERSISTENT = new PersistenceType("NON_PERSISTENT", (byte)1);  /*173771*/

  /** Constant denoting Persistence Type - Persistent                   */
  public  final static PersistenceType PERSISTENT     = new PersistenceType("PERSISTENT"    , (byte)2);

  /*  Array of defined PersistenceTypes - needed by getPersistenceType  */
  private final static PersistenceType[] set = {UNKNOWN
                                               ,NON_PERSISTENT
                                               ,PERSISTENT
                                                };

  private String name;
  private Byte   value;

  /* Private constructor - ensures the 'constants' defined here are the total set. */
  private PersistenceType(String aName, byte aValue) {
    name  = aName;
    value = Byte.valueOf(aValue);
  }

  /**
   * Returns the corresponding PersistenceType for a given Byte.
   * This method should NOT be called by any code outside the MFP component.
   * It is only public so that it can be accessed by sub-packages.
   *
   * @param  aValue         The Byte for which an PersistenceType is required.
   *
   * @return The corresponding PersistenceType
   */
  public final static PersistenceType getPersistenceType(Byte aValue) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc,"Value = " + aValue);
    return set[aValue.intValue()];
  }

  /**
   * Returns the Byte representation of the PersistenceType.
   * This method should NOT be called by any code outside the MFP component.
   * It is only public so that it can be accessed by sub-packages.
   *
   * @return  The Byte representation of the instance.
   */
  public final Byte toByte() {
    return value;
  }

  /**
   * Returns the name of the PersistenceType.
   *
   * @return  The name of the instance.
   */
  public final String toString() {
    return name;
  }

}
