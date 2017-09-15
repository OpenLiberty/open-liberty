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

package com.ibm.wsspi.sib.core.exception;

import com.ibm.websphere.sib.exception.SINotPossibleInCurrentStateException;

/**
 This exception is thrown by the createConsumerSessionForDurableSubscription 
 method if there is no subscription with the name given. It should not contain 
 a linked exception. The recovery action in this case is to create a new 
 subscription. 
 <p>
 This class has no security implications.
*/
public class SIDurableSubscriptionNotFoundException
  extends SINotPossibleInCurrentStateException
{

  private static final long serialVersionUID = 6627264191727321850L;
  public SIDurableSubscriptionNotFoundException(String msg)
  {
    super(msg);
  }

}
