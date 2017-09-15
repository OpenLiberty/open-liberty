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
 This exception is thrown when an architected Jetstream limit prevents the 
 method from completing. The exception message should describe the specific 
 limit that would be exceeded were the method to be allowed to complete 
 normally, and give the configured value of the limit. SILimitExceededException 
 should not contain a linked exception. The application may choose to retry the 
 method call in the hope that resources have become available. 
 <p>
 This class has no security implications.
 */
public class SILimitExceededException extends SIResourceException 
{

  private static final long serialVersionUID = -9020374047684536734L;
  public SILimitExceededException(String msg) {
    super(msg);
  }

}
