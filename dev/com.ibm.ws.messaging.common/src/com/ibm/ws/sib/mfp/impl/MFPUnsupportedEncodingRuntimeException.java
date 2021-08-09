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

package com.ibm.ws.sib.mfp.impl;

/**
 * MFPUnsupportedEncodingRuntimeException is thrown when MQ Message Encapsulation
 * code needs to percolate an UnsupportedEncodingException up through the JMF
 * layers (which don't expect it) so that we can ultimately throw a suitable
 * Exception to the end user.
 * <p>
 * The UnsupportedEncodingException is passed in as the cause.
 * Whenever this exception is thrown it MUST be caught at a higher-level of MFP
 * code and the UnsupportedEncodingException extracted and thrown on.
 */
public class MFPUnsupportedEncodingRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public MFPUnsupportedEncodingRuntimeException() {
    super();
  }

  /**
   * Constructor for when the Exception is to be thrown because another
   * Exception has been caught.
   *
   * @param cause The original Throwable which has caused this to be thrown.
   */
  public MFPUnsupportedEncodingRuntimeException(Throwable cause) {
    super(cause);
  }

}
