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

import com.ibm.websphere.sib.exception.SIResourceException;

/**
 This exception is thrown by the commit method if the transaction is rolled 
 back. The transaction object can no longer be used, and a new transaction must 
 be created. 
 <p>
 This class has no security implications.
 */
public class SIRollbackException extends SIResourceException 
{
	
  private static final long serialVersionUID = -2378844529328141711L;
  public SIRollbackException(String msg) {
    super(msg);
  }
  
  public SIRollbackException(String msg, Throwable t) {
    super(msg, t);
  }

}

