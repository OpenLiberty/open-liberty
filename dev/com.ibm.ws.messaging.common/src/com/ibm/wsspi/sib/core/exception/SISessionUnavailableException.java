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
 This exception is thrown when attempt is made to use a destination session that 
 is not available, for example when a method is invoked on a session that has 
 been closed, or when createBifurcatedConsumerSession is invoked on an id that 
 does not correspond to a live consumer. 
 <p>
 This class has no security implications.
 */
public class SISessionUnavailableException
	extends SINotPossibleInCurrentStateException 
{

  private static final long serialVersionUID = 887189624894252171L;
  public SISessionUnavailableException(String msg) {
    super(msg);
  }

  public SISessionUnavailableException(String msg, Throwable t) {
    super(msg, t);
  }

}
