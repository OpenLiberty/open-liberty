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
 * SIDataGraphFormatMismatchException is thrown when an attempt is made to interpret
 * a message payload in a format into which can the data can not be translated.
 * The recovery action for an application is to read the message in a simpler
 * format, for example as a String.
 *
 * @ibm-was-base
 * @ibm-api
 */
public class SIDataGraphFormatMismatchException extends SINotPossibleInCurrentStateException {

  private static final long serialVersionUID = -1163813360283366515L;

  public SIDataGraphFormatMismatchException() {
    super();
  }

  /**
   * Constructor for when the Exception is to be thrown because another
   * Exception has been caught during the copy.
   *
   * @param cause The original Throwable which has caused this to be thrown.
   */
  public SIDataGraphFormatMismatchException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructor for when the Exception is to be thrown for a reason other than
   * that an Exception has been caught during the copy.
   *
   * @param message A String giving information about the problem which caused this to be thrown.
   */
  public SIDataGraphFormatMismatchException(String message) {
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
  public SIDataGraphFormatMismatchException(String message, Throwable cause) {
    super(message, cause);
  }
}
