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
 * 
 */
public interface SIMPDeliveryStreamSetControllable extends SIMPControllable
{
  /**
   * All of the streams in a set are the same type.
   *
   * @return DeliveryStreamType  The type of stream, source target p2p etc.
   */
  DeliveryStreamType getType();  
  
  /**
   * Return the health state of the stream set
   * 
   * @return HealthState    The state of the stream set
   */
  HealthState getHealthState();
}
