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
 This exception is thrown when a specified temporary destination is not recognised.
 
 This class has no security implications.
 */
public class SITemporaryDestinationNotFoundException
  extends SINotPossibleInCurrentStateException 
{

  private static final long serialVersionUID = 5752938072435741910L;
  public SITemporaryDestinationNotFoundException(String msg) {
    super(msg);
  }

}
