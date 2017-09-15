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

import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.exceptions.SIMPInvalidRuntimeIDException;

public interface SIMPMQLinkQueuePointControllable extends
  SIMPLocalQueuePointControllable {

  /**
   * Get the iterator over the messages on this MQLink transmit queue point.
   * @return Iterator over messages
   */
  SIMPIterator getTransmitMessageIterator();
  
  /**
   * Get a message with a given id from this MQLink transmit queue point
   * @param id
   * @return transmit message
   * @throws SIMPException 
   * @throws SIMPControllableNotFoundException 
   * @throws SIMPInvalidRuntimeIDException 
   */
  SIMPMQLinkTransmitMessageControllable getTransmitMessageByID( String id ) 
    throws SIMPInvalidRuntimeIDException, 
           SIMPControllableNotFoundException, 
           SIMPException;
}
