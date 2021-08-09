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

/**
 This subclass of SIConnectionUnavailableException is thrown when the connection 
 was closed by the bus, typically as a result of a Comms failure. (Note: this 
 exception is not used to indicate that a comms failure occurred during the 
 execution of the method invoked; SIConnectionLostException is used for that 
 purpose, although SIConnectionDroppedException will be thrown on subsequent 
 invocations.)
 <p>
 This class has no security implications.
 */
public class SIConnectionDroppedException
	extends SIConnectionUnavailableException 
{
  private static final long serialVersionUID = 8704309876012415509L;
  
  public SIConnectionDroppedException(String msg) {
    super(msg);
  }

  public SIConnectionDroppedException(String msg, Throwable t) {
    super(msg, t);
  }

}
