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
 * Describes information about a message that has been sent to us
 * in response to a message request
 */
public interface SIMPRequestedValueMessageInfo
{
  long getTimeout();
  
  boolean isDelivered();
  
  long getIssueTime();
  
  int getMessagePriority();
  
  Reliability getMessageReliability();
}
