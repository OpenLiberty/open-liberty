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

package com.ibm.ws.sib.processor.matching;

import com.ibm.ws.sib.processor.impl.interfaces.DispatchableKey;

/**
 * @author Neil Young
 *
 * <p>The MessageProcessorMatchTarget class is a wrapper that holds a ConsumerPoint,
 * but allows a MatchTarget type to be associated with it for storage in the
 * MatchSpace.

 */
public class MatchingConsumerPoint extends MessageProcessorMatchTarget{

  private DispatchableKey consumerPointData;

  MatchingConsumerPoint(DispatchableKey cp)
  {
    super(JS_CONSUMER_TYPE);
    consumerPointData = cp;
  }

  public boolean equals(Object o)
  {
    boolean areEqual = false;
    if (o instanceof MatchingConsumerPoint)
    {
      DispatchableKey otherCP = ((MatchingConsumerPoint) o).consumerPointData;

      if(consumerPointData.equals(otherCP))
        areEqual = true;
    }
    return areEqual;
  }

  public int hashCode()
  {
    return consumerPointData.hashCode();
  }
  /**
   * Returns the consumerPoint.
   * @return ConsumerPoint
   */
  public DispatchableKey getConsumerPointData()
  {
    return consumerPointData;
  }

}
