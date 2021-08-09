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
 This exception is thrown by the createDurableSubscription method if a durable 
 subscription already exists with the name given. It should not contain a linked 
 exception. The recovery action in this case is either to attach to the existing 
 subscription, or to delete it.
 <p>
 This class has no security implications.
 */
public class SIDurableSubscriptionAlreadyExistsException
  extends SINotPossibleInCurrentStateException
{

  private static final long serialVersionUID = -7722841239140825361L;
  public SIDurableSubscriptionAlreadyExistsException(String msg)
  {
    super(msg);
  }

}
