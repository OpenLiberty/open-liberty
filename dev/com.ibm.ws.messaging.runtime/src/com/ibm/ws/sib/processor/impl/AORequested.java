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

package com.ibm.ws.sib.processor.impl;

// Import required classes.

/**
 * The information kept for each value tick in a stream
 */
public final class AORequested
{
  public final JSRemoteConsumerPoint aock;
  public final long startTime; // used to calculate the waitTime
  public final long expiryInterval; // expiry occurs at approximately startTime+expiryInterval
  public final long tick;

  /** true if in the process of inserting a value message for this tick, else false
   * initial state: false
   * final state: false, true
   * possible state transitions: false -> true
   */
  public boolean inserting;

  public AORequested(JSRemoteConsumerPoint aock, long startTime, long expiryInterval, long tick)
  {
    this.aock = aock;
    this.startTime = startTime;
    this.inserting = false;
    this.expiryInterval = expiryInterval;
    this.tick = tick;
  }
}
