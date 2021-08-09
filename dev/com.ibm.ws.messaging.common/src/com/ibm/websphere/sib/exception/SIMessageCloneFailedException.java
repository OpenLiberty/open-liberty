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
 * SIMessageCloneFailedException is thrown if a the SIMessage clone was
 * unsuccessful for any reason.
 * <p>
 * The underlying problem is detailed in the linked Exception.
 * <p>
 * SIMessageCloneFailedException extends CloneNotSupportedException because
 * it is thrown by a clone method.
 *
 * @ibm-was-base
 * @ibm-api
 */
public class SIMessageCloneFailedException extends CloneNotSupportedException {

  private static final long serialVersionUID = 2591783634234605842L;

  /**
   * Constructor for completeness
   *
   */
  public SIMessageCloneFailedException() {
    super();
  }


  /**
   * Constructor for when the Exception is to be thrown because another
   * Exception has been caught during the copy.
   *
   * @param cause The original Throwable which has caused this to be thrown.
   */
  public SIMessageCloneFailedException(Throwable cause) {
    super(cause.getMessage());
  }


  /**
   * Constructor for when the Exception is to be thrown for a reason other than
   * that an Exception has been caught during the copy.
   *
   * @param message A String giving information about the problem which caused this to be thrown.
   */
  public SIMessageCloneFailedException(String message) {
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
  public SIMessageCloneFailedException(String message, Throwable cause) {
    super(message + " : " + cause.getMessage());
  }


}
