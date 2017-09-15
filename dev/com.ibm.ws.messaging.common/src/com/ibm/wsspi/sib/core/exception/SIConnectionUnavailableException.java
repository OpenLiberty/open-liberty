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
 This exception is thrown when attempt is made to use a connection that is not 
 available, ie when a method is invoked on a connection that has been closed. 
 The operation was not performed. The recovery action is to open a new 
 connection. 
 <p>
 This class has no security implications.
 */
public class SIConnectionUnavailableException
	extends SINotPossibleInCurrentStateException 
{

  private static final long serialVersionUID = -1542824145075787184L;
  public SIConnectionUnavailableException(String msg) {
    super(msg);
  }

  public SIConnectionUnavailableException(String msg, Throwable t) {
    super(msg, t);
  }

}
