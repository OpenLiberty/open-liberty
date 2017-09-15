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
 This exception is thrown whenever a Comms error occurs during the execution of 
 a method, resulting in the SICoreConnection being closed. Any subsequent 
 attempt to use the lost connection will result in SIConnectionDroppedException. 
 This exception indicates that the outcome of the operation is unknown. 
 <p>
 This class has no security implications.
 */
public class SIConnectionLostException extends SIResourceException 
{

  private static final long serialVersionUID = -9095554106264670028L;
  public SIConnectionLostException(String msg) {
    super(msg);
  }

  public SIConnectionLostException(String msg, Throwable t) {
    super(msg, t);
  }

}
