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

import com.ibm.websphere.sib.SIApiConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * JmsBodyType is a type-safe enumeration for JmsBody types.
 */
public final class JmsBodyType implements IntAble {

  private static TraceComponent tc = SibTr.register(JmsBodyType.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /** Integer value of the NULL JMsBodyType                                   */
  public final static int NULL_INT   = 0;
  /** Integer value of the BYTES JMsBodyType                                  */
  public final static int BYTES_INT  = 1;
  /** Integer value of the MAP JMsBodyType                                    */
  public final static int MAP_INT    = 2;
  /** Integer value of the OBJECT JMsBodyType                                 */
  public final static int OBJECT_INT = 3;
  /** Integer value of the STREAM JMsBodyType                                 */
  public final static int STREAM_INT = 4;
  /** Integer value of the TEXT JMsBodyType                                   */
  public final static int TEXT_INT   = 5;


 
  /** Constant denoting a null-bodied JMS Message                   */
  public  final static JmsBodyType NULL   = new JmsBodyType("NULL"  ,(byte)NULL_INT);

  /** Constant denoting JmsBody Type of Bytes                       */
  public  final static JmsBodyType BYTES  = new JmsBodyType("BYTES" ,(byte)BYTES_INT);

  /** Constant denoting JmsBody Type of Map                         */
  public  final static JmsBodyType MAP    = new JmsBodyType("MAP"   ,(byte)MAP_INT);

  /** Constant denoting JmsBody Type of Object                      */
  public  final static JmsBodyType OBJECT = new JmsBodyType("OBJECT",(byte)OBJECT_INT);

  /** Constant denoting JmsBody Type of Stream                      */
  public  final static JmsBodyType STREAM = new JmsBodyType("STREAM",(byte)STREAM_INT);

  /** Constant denoting JmsBody Type of Text                        */
  public  final static JmsBodyType TEXT   = new JmsBodyType("TEXT"  ,(byte)TEXT_INT);

  /*  Array of defined JmsBodyTypes - needed by getJmsBodyType    */
  private final static JmsBodyType[] set = {NULL
                                           ,BYTES
                                           ,MAP
                                           ,OBJECT
                                           ,STREAM
                                           ,TEXT
                                           };

  private String name;
  private Byte   value;
  private int    intValue;

  /* Private constructor - ensures the 'constants' define here are the total set. */
  private JmsBodyType(String aName, byte aValue) {
    name  = aName;
    value = new Byte(aValue);
    intValue = (int)aValue;
  }

  /**
   * Return the appropriate JMSBodyType for a specific format string
   *
   * @param format  A String corresponding to the SDO format of a JMS Message
   *
   * @return JmsBodyType The JmsBodyType singleton which maps to the given format string
   */
  public static JmsBodyType getBodyType(String format) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getBodyType");

    JmsBodyType result = null;
    if (format.equals(SIApiConstants.JMS_FORMAT_BYTES))
      result = BYTES;
    else if (format.equals(SIApiConstants.JMS_FORMAT_TEXT))
      result = TEXT;
    else if (format.equals(SIApiConstants.JMS_FORMAT_OBJECT))
      result = OBJECT;
    else if (format.equals(SIApiConstants.JMS_FORMAT_STREAM))
      result = STREAM;
    else if (format.equals(SIApiConstants.JMS_FORMAT_MAP))
      result = MAP;
    else if (format.equals(SIApiConstants.JMS_FORMAT))
      result = NULL;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getBodyType");
    return result;
  }

  /**
   * Returns the corresponding JmsBodyType for a given Byte.
   * This method should NOT be called by any code outside the TEXT component.
   * It is only public so that it can be accessed by sub-packages.
   *
   * @param  aValue         The Byte for which an JmsBodyType is required.
   *
   * @return The corresponding JmsBodyType
   */
  public final static JmsBodyType getJmsBodyType(Byte aValue) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc,"Value = " + aValue);
    return set[aValue.intValue()];
  }

  /**
   * Returns the Byte representation of the JmsBodyType.
   * This method should NOT be called by any code outside the MFP component.
   * It is only public so that it can be accessed by sub-packages.
   *
   * @return The Byte representation of the instance.
   */
  public final Byte toByte() {
    return value;
  }

  /**
   * Returns the integer representation of the JmsBodyType.
   * This method should NOT be called by any code outside the SIBus.
   * It is only public so that it can be accessed by SIBus components.
   *
   * @return The int representation of the instance.
   */
  public final int toInt() {
    return intValue;
  }

  /**
   * Returns the name of the JmsBodyType.
   *
   * @return The name of the instance.
   */
  public final String toString() {
    return name;
  }

}
