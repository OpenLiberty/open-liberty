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

import com.ibm.websphere.sib.exception.SIException;

/**
 This exception is thrown if the credentials supplied on the createConnection 
 call are not accepted. Instances of this exception should not contain a linked 
 exception. 
 <p>
 This class has no security implications.
*/
public class SIAuthenticationException extends SIException {
	 
  private static final long serialVersionUID = -458588735063364784L;
  
  public SIAuthenticationException(String msg) {
    super(msg);
  } 

}

