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
public interface SIMPVirtualLinkControllable extends SIMPControllable
{  
  
  /**
   * Returns the Link receiver controllablea for the virtual link. These are the
   * target streams of msgs coming from a source bus and arriving over this link.
   *
   * @return SIMPIterator  The link receiver controllables
   *
   */
  SIMPIterator getLinkReceiverControllableIterator();
    
  /**
   * Locates the foreign bus control adapter associated with this link
   *
   * @return SIMPForeignBusControllable The foreign bus control adapter
   *
   */
  SIMPForeignBusControllable getForeignBusControllable();
  
  /**
   * Returns the Link Transmitter controllable associated with this link. This
   * is the transmission itemstream for messages being sent to the target bus
   * over this link.
   * 
   * @return SIMPLinkTransmitterControllable The queue point used to transmit messages
   *
   */
  SIMPIterator getLinkRemoteQueuePointControllableIterator();
  
  /**
   * Returns the bus name that this link is targetted at
   * @return String busname
   */
  String getTargetBus();
  
}
