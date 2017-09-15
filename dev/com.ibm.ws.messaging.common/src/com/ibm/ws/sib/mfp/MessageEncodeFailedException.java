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
 * MessageEncodeFailedException is thrown if the component is unable
 * to encode the Message for transmission or hardening.
 * <p>
 * An FFDC record will already have been written detailing what could not be
 * encoded. This problem is usually caused by required fields not being set.
 */
public class MessageEncodeFailedException extends AbstractMfpException {

  private static final long serialVersionUID = -2135140680132858765L;

  /**
   * Constructor for completeness
   *
   */
  public MessageEncodeFailedException() {
    super();
  }

  /**
   * Constructor for when the Exception is to be thrown because another
   * Exception has been caught during the copy.
   *
   * @param cause The original Throwable which has caused this to be thrown.
   */
  public MessageEncodeFailedException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructor for when the Exception is to be thrown for a reason other than
   * that an Exception has been caught during the copy.
   *
   * @param message A String giving information about the problem which caused this to be thrown.
   */
  public MessageEncodeFailedException(String message) {
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
  public MessageEncodeFailedException(String message, Throwable cause) {
    super(message, cause);
  }

}
