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

import com.ibm.websphere.sib.exception.SIIncorrectCallException;

/**
 This is an SIIncorrectCallException thrown when an invalid selector string is 
 passed. It is necessary because the validation is complex and expensive.
 <p>
 This class has no security implications.
 */
public class SISelectorSyntaxException extends SIIncorrectCallException
{
	
  private static final long serialVersionUID = 7353245508053935130L;
  public SISelectorSyntaxException(String msg) {
    super(msg);
  }
	
}
