/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.runtime;

import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.sib.admin.ForeignBusDefinition;

/**
 *
 */
public interface SIMPForeignBusControllable extends SIMPMessageHandlerControllable
{
  /**
   * Locates the inter bus link associated with this bus 
   *
   * @return SIMPVirtualLinkControllable  An control adapter for the link
   */
  SIMPVirtualLinkControllable getVirtualLinkControlAdapter();
  
  /**
   * Retrieves the ForeignBusDefinition object for this foreign bus 
   *
   * @return ForeignBusDefinition The foreignBusDefinition
   */
  ForeignBusDefinition getForeignBusDefinition();
  
  /**
   * Retrieves the default priority for the foreign bus 
   *
   * @return int The default priority
   */
  public int getDefaultPriority();
  
  /**
   * Retrieves the default reliability for the foreign bus 
   *
   * @return int The default reliability
   */
  public Reliability getDefaultReliability();
  
  /**
   * Determines whether messages can be sent to the foreign bus
   * @return
   */
  public boolean isSendAllowed();
}
