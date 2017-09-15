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
package com.ibm.ws.sib.processor;

/**
 * A Class representing the current Multicast properties for the Messaging engine.
 */
public interface MulticastProperties
{
  /**
   * Return the Multicast Group Address
   * 
   * 
   * Default 234.6.17.92
   */
  public String getMulticastGroupAddress();
  
  /**
   * Return the Multicast Interface Address
   * Determines the network adapter to use for multicast 
   * transmissions on a multi-homed system. A value of blank specified all adapters.
   * 
   * Default "none"
   */
  public String getMulticastInterfaceAddress();
  
  /**
   * Return the Multicast Port
   * 
   * Default 34343
   */
  public int getMulticastPort();
  
  /**
   * Return the Multicast Packet Size
   * 
   * Default 7000 (bytes)
   */
  public int getMulticastPacketSize();
  
  /**
   * Specifies the network range for multicast transmissions. 
   * Routers decrement the TTL and when the value reaches 0, the packet is discarded. 
   * The value of 1 therefore confines the packet to the local LAN subnet.
   * 
   * Default 1
   */
  public int getMulticastTTL();
  
  /**
   * Specifies whether RMM should operate in 'reliable' mode.
   * 
   * Default false
   */
  public boolean isReliable();  
  
}
