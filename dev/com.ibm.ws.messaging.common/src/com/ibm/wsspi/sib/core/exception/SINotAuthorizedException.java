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

import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;

/**
 Those clients, such as the JMS API layer, that have a requirement to 
 discriminate permission failures from other config errors (such as "destination 
 not found", can do so by catching this subclass of 
 SINotPossibleInConfigurationException. Other clients are encouraged to avoid 
 leaking information to unauthorized end users by just catching the parent 
 exception.
 <p>
 This class has no security implications.
*/
public class SINotAuthorizedException 
    extends SINotPossibleInCurrentConfigurationException
{
	
  private static final long serialVersionUID = -3528137123062357016L;
  public SINotAuthorizedException(String msg) {
    super(msg);
  }
  
}

