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

/**
 * 
 */
public interface SIMPRemoteBrowserReceiverControllable extends SIMPControllable
{
  /**
   * Get the unique Id of the browse session
   * 
   * @return long the browse ID
   */
  long getBrowseID();
  
  /**
   * Get the expected sequence number of the next BrowseGet message
   * 
   * @return long the sequence number
   */
  long getExpectedSequenceNumber();
}
