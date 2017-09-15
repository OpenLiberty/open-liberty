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
 * SIMessageParseException is used when the system needs to throw an
 * unchecked exception to indicate a parse failure. The other (checked)
 * SIException classes should be used where possible.
 * <p>
 * This exception does not use any new reason code or inserts, so it inherits
 * the SIRCConstants.SIRC0001_DELIVERY_ERROR behaviour from SIErrorException.
 * @ibm-was-base
 * @ibm-api
 */
public class SIMessageParseException extends SIErrorException {

  private static final long serialVersionUID = -5177496595904095444L;


  /**
   * Constructor for when the Exception is to be thrown because another
   * Exception has been caught.
   *
   * @param cause The original Throwable which has caused this to be
   * thrown.
   */
  public SIMessageParseException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructor for when the Exception is to be thrown for a reason other than
   * that an Exception has been caught during the parse.
   *
   * @param message A String giving information about the problem which caused
   * this to be thrown.
   */
  public SIMessageParseException(String message) {
    super(message);
  }

  /**
   * Constructor for when the Exception is to be thrown because another
   * Exception has been caught during the copy and additional information
   * is to be included.
   *
   * @param message A String giving information about the problem which
   * caused this to be thrown.
   * @param cause The original Throwable which has caused this to be
   * thrown.
   */
  public SIMessageParseException(String message, Throwable cause) {
    super(message, cause);
  }
}
