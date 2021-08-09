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
 * SINotPossibleInCurrentConfigurationException is thrown by the SIBus when
 * the operation being attempted can not be performed because the current runtime
 * configuration does not permit it.
 * <p>
 * The exception message describes the precise way in which the operation is
 * incompatible with the configuration.
 *
 * @ibm-was-base
 * @ibm-api
 */
public class SINotPossibleInCurrentConfigurationException extends SIException {

  private static final long serialVersionUID = 4790393311934744169L;

  public SINotPossibleInCurrentConfigurationException() {
    super();
  }

  /**
   * Constructor for when the Exception is to be thrown because another
   * Exception has been caught during the copy.
   *
   * @param cause The original Throwable which has caused this to be thrown.
   */
  public SINotPossibleInCurrentConfigurationException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructor for when the Exception is to be thrown for a reason other than
   * that an Exception has been caught during the copy.
   *
   * @param message A String giving information about the problem which caused this to be thrown.
   */
  public SINotPossibleInCurrentConfigurationException(String message) {
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
  public SINotPossibleInCurrentConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
