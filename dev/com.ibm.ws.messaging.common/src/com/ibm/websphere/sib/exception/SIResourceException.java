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

package com.ibm.websphere.sib.exception;

/**
 * SIResourceException thrown by the SIBus when an operation is unable to complete
 * due to a resource error, such as lose of connection to a database or system.
 * In many cases, if the operation is retried it may succeed.
 * <p>
 * The context and root cause of the problem are detailed in the chain of linked
 * Exceptions contained by the SIResourceException.
 *
 * @ibm-was-base
 * @ibm-api
 */
public class SIResourceException extends SIException {

  private static final long serialVersionUID = 7713697496638751309L;

  public SIResourceException() {
    super();
  }

  /**
   * Constructor for when the Exception is to be thrown because another
   * Exception has been caught during the copy.
   *
   * @param cause The original Throwable which has caused this to be thrown.
   */
  public SIResourceException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructor for when the Exception is to be thrown for a reason other than
   * that an Exception has been caught during the copy.
   *
   * @param message A String giving information about the problem which caused this to be thrown.
   */
  public SIResourceException(String message) {
    super(message);
  }

  /**
   * Constructor for when the Exception is to be thrown because another
   * Exception has been caught during the copy and additional information is
   * to be included.
   *
   * @param message A String giving information about the problem which caused this to be thrown.
   * @param cause The original Throwable which has caused this to be thrown.
   */
  public SIResourceException(String message, Throwable cause) {
    super(message, cause);
  }
}
