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
package com.ibm.ws.sib.processor.runtime;

/**
 * A type-safe enumeration for the delivery stream.
 * 
 */
public class DeliveryStreamType
{
  /**
   * A source delivery stream of type unicast is indicated 
   * by the value DeliveryStreamType.UNICAST_SOURCE
   */
  public final static DeliveryStreamType UNICAST_SOURCE = new DeliveryStreamType("Unicast_Source", 0);

  /**
  * A target delivery stream of type unicast is indicated 
  * by the value DeliveryStreamType.UNICAST_TARGET
   */
  public final static DeliveryStreamType UNICAST_TARGET = new DeliveryStreamType("Unicast_Target", 1);

  /**
  * A source delivery stream of type anycast is indicated 
  * by the value DeliveryStreamType.ANYCAST_SOURCE
  */
  public final static DeliveryStreamType ANYCAST_SOURCE = new DeliveryStreamType("Anycast_Source", 2);

  /**
  * A target delivery stream of type anycast is indicated 
  * by the value DeliveryStreamType.ANYCAST_TARGET
  */
  public final static DeliveryStreamType ANYCAST_TARGET = new DeliveryStreamType("Anycast_Target", 3);

  /**
   * A source delivery stream of type pub/sub is indicated 
   * by the value DeliveryStreamType.PUBSUB_SOURCE
  */
  public final static DeliveryStreamType PUBSUB_SOURCE = new DeliveryStreamType("PubSub_Source", 4);

  /**
   * A target delivery stream of type pub/sub is indicated 
   * by the value DeliveryStreamType.PUBSUB_TARGET
  */
  public final static DeliveryStreamType PUBSUB_TARGET = new DeliveryStreamType("PubSub_Target", 5);

  /**
   Returns a string representing the DeliveryStreamType value 
  
   @return a string representing the DeliveryStreamType value
  */
  public final String toString()
  {
    return name;
  }

  /**
   * Returns an integer value representing this DeliveryStreamType
   * 
   * @return an integer value representing this DeliveryStreamType
   */
  public final int toInt()
  {
    return value;
  }

  /**
   * Get the DeliveryStreamType represented by the given integer value;
   * 
   * @param value the integer representation of the required DeliveryStreamType
   * @return the DeliveryStreamType represented by the given integer value
   */
  public final static DeliveryStreamType getDeliveryStreamType(int value)
  {
    return set[value];
  }

  private final String name;
  private final int value;
  private final static DeliveryStreamType[] set = { 
                                                    UNICAST_SOURCE,
                                                    UNICAST_TARGET,
                                                    ANYCAST_SOURCE, 
                                                    ANYCAST_TARGET, 
                                                    PUBSUB_SOURCE, 
                                                    PUBSUB_TARGET 
                                                   };

  private DeliveryStreamType(String name, int value)
  {
    this.name = name;
    this.value = value;
  }
}
