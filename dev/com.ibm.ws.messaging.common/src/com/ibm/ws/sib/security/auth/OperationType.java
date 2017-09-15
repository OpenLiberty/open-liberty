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
 
package com.ibm.ws.sib.security.auth;

/**
 * OperationType is an type-safe enum that is used with checkDestinationAccess
 * to determine whether a particular Subject has authorization to perform a
 * particular operation on a particular destination 
 */
public class OperationType
{
  /** Integer value of the CREATE Type */
  public final static int CREATE_INT   = 0;
  /** Integer value of the SEND Type */
  public final static int SEND_INT  = 1;
  /** Integer value of the RECEIVE Type */
  public final static int RECEIVE_INT    = 2;
  /** Integer value of the BROWSE Type */
  public final static int BROWSE_INT = 3;
  /** Integer value of the IDENTITY_ADOPTER Type */
  public final static int IDENTITY_ADOPTER_INT = 4;
  /** Integer value of the INQUIRE Type */
  public final static int INQUIRE_INT = 5;

  /** CREATE a temporary destination **/
  public static final OperationType CREATE              = new OperationType("CREATE", CREATE_INT);
  /** SEND a message to a destination, including publishing on a topic. **/
  public static final OperationType SEND                = new OperationType("SEND", SEND_INT);
  /** RECEIVE a message from a destination, including receiving on a topic. **/
  public static final OperationType RECEIVE             = new OperationType("RECEIVE", RECEIVE_INT);
  /** BROWSE a message on a destination **/
  public static final OperationType BROWSE              = new OperationType("BROWSE", BROWSE_INT);
  /** IDENTITY_ADOPTER a message on a destination **/
  public static final OperationType IDENTITY_ADOPTER    = new OperationType("IDENTITY_ADOPTER", IDENTITY_ADOPTER_INT);
  /** INQUIRE the configuration of a destination */
  public static final OperationType INQUIRE             = new OperationType("INQUIRE", INQUIRE_INT);
  
  /** The name of this OperationType */
  private final String _name;
  /** The int value for this OperationType */
  private final int _value;

  /* ------------------------------------------------------------------------ */
  /* OperationType method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * Use a private constructor to ensure that operation types can only be
   * defined from within this class.
   * 
   * @param name  The name of the type (for debug purposes)
   * @param value The int value of the type (for use with switch statements)
   */
  private OperationType(String name, int value)
  {
    this._name = name;
    this._value = value;
  }

  /** The set of OperationTypes in int order */
  private static final OperationType[] set = {CREATE,
                                              SEND,
                                              RECEIVE,
                                              BROWSE,
                                              IDENTITY_ADOPTER,
                                              INQUIRE};

  /**
   * Provide a String representation of an instance of this class.
   * 
   * @return String a String representing this object
   */
  public String toString()
  {
    return _name;
  }

  /**
   * Method toInt.
   * @return int
   */
  public final int toInt () {
    return _value;
  }
  
  /**
   * Method getOperationType
   * @param aValue
   * @return OperationType
   */
  public final static OperationType getOperationType(int aValue) {
    return set[aValue];
  }
}