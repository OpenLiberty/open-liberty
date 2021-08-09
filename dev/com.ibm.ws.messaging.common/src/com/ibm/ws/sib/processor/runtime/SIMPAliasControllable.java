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

import com.ibm.websphere.sib.Reliability;

/**
 * The interface presented by a queue to perform dynamic
 * control operations.
 * <p>
 * The operations in this interface are specific to a queueing point.
 */
public interface SIMPAliasControllable extends SIMPMessageHandlerControllable
{
  /**
   * Get the target message handler as known to the MP. 
   *
   * @return The target SIMPMessageHandlerControllable object. 
   */
  public SIMPMessageHandlerControllable getTargetMessageHandler();

  public String getBus();
  public int getDefaultPriority();
  public Reliability getMaxReliability();
  public Reliability getDefaultReliability();
  public boolean isProducerQOSOverrideEnabled();
  public boolean isReceiveAllowed();
  public boolean isSendAllowed();
}
