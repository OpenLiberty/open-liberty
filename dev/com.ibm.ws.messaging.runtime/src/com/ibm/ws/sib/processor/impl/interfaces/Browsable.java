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
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;

/**
 * Browsable should be implemented by any class which can provide
 * a BrowseCursor for its messages.
 * 
 * @author tevans
 * @see com.ibm.ws.sib.msgstore.NonLockingCursor
 * @see com.ibm.ws.sib.msgstore.ItemStream
 */
public interface Browsable {
  
  /**
   * Get a Cursor on the message point.
   * 
   * @param selectionCriteria Limits the messages returned based on
   * a selection criteria.
   * If the cursor should return all items, selectionCriteria should be null.
   * @return A Cursor
   * @throws SIResourceException Thrown if there is a problem getting a Cursor
   * from the messageStore or from MQ.
   * @throws SIDiscriminatorSyntaxException
   */
  public BrowseCursor getBrowseCursor(SelectionCriteria selectionCriteria) throws SIResourceException, SISelectorSyntaxException, SIDiscriminatorSyntaxException;
  
  
}
