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
package com.ibm.ws.sib.processor.impl.interfaces;

import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;

/**
 * A cursor used to browse messages on either an MQ queue or a
 * MessageStore ItemStream.
 */
public interface BrowseCursor
{
  
  /**
   * Reply the next {@link JsMessage} that matches the filter specified when
   * the cursor was created.
   * Method next.
   * @return the next matching {@link JsMessage}, or null if there is none.
   * @throws SISessionDroppedException 
   */
  public JsMessage next() throws SIResourceException, SISessionDroppedException;
  
  /**
   * Declare that this cursor is no longer required.  This allows the underlying
   * resource to release resources.  
   * Once the cursor has been released it should not be used again.
   */
  public void finished() throws SISessionDroppedException;
  

}
