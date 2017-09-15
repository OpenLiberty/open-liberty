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
 This exception is thrown when an attempt is made to create a ConsumerSession 
 for a destination configured with ReceiveExclusive=true that already has a 
 consumer attached. Single-threaded applications will not typically be able to 
 recover from this condition; multi-threaded apps may be able to recover if the 
 existing consumer is part of the same application.
 <p>
 This class has no security implications.
 */
public class SIDestinationLockedException 
    extends SINotPossibleInCurrentStateException 
{
  private static final long serialVersionUID = 2348582570323085062L;

  public SIDestinationLockedException(String msg) {
    super(msg);
  }
  
}

