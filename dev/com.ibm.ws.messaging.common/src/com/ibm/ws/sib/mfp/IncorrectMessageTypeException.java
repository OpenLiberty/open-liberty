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
package com.ibm.ws.sib.mfp;

/**
 * IncorrectMessageTypeException is thrown if the type of the message was not
 * as expected so processing can not continue.
 * <p>
 * The Exception text details the expected and actual message types.
 */
public class IncorrectMessageTypeException extends AbstractMfpException {

  private static final long serialVersionUID = 9037856092705261428L;

  /**
  /**
   * Constructor for completeness
   *
   */
  public IncorrectMessageTypeException() {
    super();
  }

  /**
   * Constructor for when the Exception is to be thrown because another
   * Exception has been caught.
   *
   * @param cause The original Throwable which has caused this to be thrown.
   */
  public IncorrectMessageTypeException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructor for when the Exception is to be thrown for a reason other than
   * that an Exception has been caught.
   *
   * @param message A String giving information about the problem which caused this to be thrown.
   */
  public IncorrectMessageTypeException(String message) {
    super(message);
  }

  /**
   * Constructor for when the Exception is to be thrown because another
   * Exception has been caught and additional information is to be included.
   *
   * @param message A String giving information about the problem which caused this to be thrown.
   * @param cause The original Throwable which has caused this to be thrown.
   */
  public IncorrectMessageTypeException(String message, Throwable cause) {
    super(message, cause);
  }

}
