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

public interface SIMPLinkTransmitMessageControllable extends
  SIMPTransmitMessageControllable {
  
  /**
   * The approximate length in bytes of this message
   * @return The length in bytes
   */
  public long getApproximateLength();
  
  /**
   * The name of the destination to which this message is being sent
   * @return Destination name
   */
  public String getTargetDestination();
  
  /**
   * The name of the bus to which this message is being sent
   * @return Bus Name
   */  
  public String getTargetBus();

}
