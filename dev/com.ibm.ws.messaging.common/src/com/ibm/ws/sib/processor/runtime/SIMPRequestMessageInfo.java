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

import com.ibm.wsspi.sib.core.SelectionCriteria;

/**
 * Object containing information about a message request that we have sent
 * to a remote ME
 */
public interface SIMPRequestMessageInfo extends SIMPControllable
{
  long getIssueTime();
  
  long getTimeout();
  
  SelectionCriteria[] getCriterias();
  
  long getACKingDME();
  
  /**
   * @return a long for the time of completion/expiration of this 
   * message.
   * SIMPConstants.INFINITE_TIMEOUT if this message does not expire
   * @author tpm
   */
  long getCompletionTime();
}
