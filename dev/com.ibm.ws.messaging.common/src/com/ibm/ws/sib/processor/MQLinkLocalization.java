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

package com.ibm.ws.sib.processor;

import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.processor.runtime.SIMPMQLinkQueuePointControllable;

/**
 * Interface that supports access to MQLink resources that are managed by
 * the MessageProcessor component.
 *
 */
public interface MQLinkLocalization 
{
  /**
   * Retrieves the MQLink's State ItemStream
   * 
   * @return itemStream
   * @throws SIException
   */
  public ItemStream getMQLinkStateItemStream()
      throws SIException;
  
  /**
   * Sets the MQLink's State ItemStream
   * 
   * @param mqLinkStateItemStream
   * @throws SIException
   */
  public void setMQLinkStateItemStream(ItemStream mqLinkStateItemStream)
      throws SIException;  
  
  /**
   * Resources associated with the MQLink will be marked for deletion.
   * 
   * @throws SIResourceException
   * @throws SIException
   */
  public void delete()
      throws SIResourceException, SIException;
  
  
  /**
   * Retrieve a reference to the Controllable associated with the MQLink
   * Queue Point.
   * 
   * @return
   * @throws SIException
   */
  public SIMPMQLinkQueuePointControllable getSIMPMQLinkQueuePointControllable() 
    throws SIException;
  
}
