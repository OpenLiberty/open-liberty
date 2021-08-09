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

package com.ibm.ws.sib.processor.exceptions;


public class SIMPInvalidRuntimeIDException extends SIMPException 
{
  /** The serial version UID, for version to version compatability */
  private static final long serialVersionUID = -35962071282560025L;

  public SIMPInvalidRuntimeIDException(String msg) {
    super(msg);
  }
  
  public SIMPInvalidRuntimeIDException(Throwable t) {
    super(t);
  }
  
  public SIMPInvalidRuntimeIDException(String msg, Throwable t) {
    super(msg, t);
  }
}
