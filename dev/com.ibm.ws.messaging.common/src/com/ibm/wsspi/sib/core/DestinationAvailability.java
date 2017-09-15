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
package com.ibm.wsspi.sib.core;

/**
 * Type-Safe Enumeration indicating the Destination Availiability
 * <p>
 * This class has no security implications.
 */
public class DestinationAvailability
{
  /**
   * Indicates that the listener is only interested in SEND from an destination.
   *
   * The destination listener will be called if the destination is a non-mediated
   *  destination with a local QueuePoint or a mediated destination with a local Mediation Point.
   */
  public final static DestinationAvailability SEND = new DestinationAvailability("SEND", 0);

  /**
   * Indicates that the listener is only interested in RECEIVE from an destination.
   *
   * The destination must have a QueuePoint on the ME to which the connection is connected.
   */
  public final static DestinationAvailability RECEIVE = new DestinationAvailability("RECEIVE", 1);

  /**
   * Indicates that the listener is interested in both SEND and RECEIVE from an destination.
   */
  public final static DestinationAvailability SEND_AND_RECEIVE = new DestinationAvailability("SEND_AND_RECEIVE", 2);

  /** The value for this constant */
  private int value;

  /** The string for this constant */
  private transient String name = null;

  /**
   * Private constructor.
   */
   private DestinationAvailability(String name, int value)
   {
     this.name = name;
     this.value = value;
   }

  /**
   * @return a string representing the DestinationAvailability value
   */
  public final String toString()
  {
    return name;
  }

  /**
   * @return an integer value representing this DestinationAvailability
   */
  public final int toInt()
  {
    return value;
  }

  /**
   * Get the DestinationAvailability represented by the given integer value;
   *
   * @param value the integer representation of the required DestinationAvailability
   * @return the DestinationAvailability represented by the given integer value
   */
  private final static DestinationAvailability[] set = {SEND, RECEIVE, SEND_AND_RECEIVE};              //SIB0137.comms.2

  public final static DestinationAvailability getDestinationAvailability (final int value) {           //SIB0137.comms.2
    return set[value];                                                                                 //SIB0137.comms.2
  }                                                                                                    //SIB0137.comms.2

}
