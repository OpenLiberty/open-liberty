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

import com.ibm.ws.sib.admin.DestinationForeignDefinition;

/**
 * 
 */
public interface SIMPForeignDestinationControllable extends SIMPMessageHandlerControllable
{
  /**
   * Locates the foreign destination definition relating to the queue. 
   *
   * @return DestinationForeignDefinition  A clone of the foreign destination definition as known at this time to the Message Processor. 
   */
  DestinationForeignDefinition getForeignDestinationDefinition();

  /**
   * Locates the local queues known to the MP and localized in this ME. 
   *
   * @return ForeignBus  An iterator over all of the LocalQueue objects. 
   */
  SIMPForeignBusControllable getTargetForeignBus();
}
