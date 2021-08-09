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

/**
 * @author jroots
 */
public class SIMPException extends Exception {

  /** The serial version UID, for version to version compatability */
  private static final long serialVersionUID = -4008780578072028380L;

  /**
   * @see java.lang.Throwable#Throwable(String)
   */
  public SIMPException(String msg) {
    super(msg);
  }
  
  /**
   * @see java.lang.Throwable#Throwable(Throwable)
   */
  public SIMPException(Throwable t) {
    super(t);
  }
  
  /**
   * @see java.lang.Throwable#Throwable(String, Throwable)
   */
  public SIMPException(String msg, Throwable t) {
    super(msg, t);
  }
}
