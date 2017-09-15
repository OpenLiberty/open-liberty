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

import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;

/**
 * Exception which indicates a destination was corrupt.
 */
public class SIMPMQLinkCorruptException extends SINotPossibleInCurrentConfigurationException
{
  /** The serial version UID, for version to version compatability */
  private static final long serialVersionUID = -7517653780546136093L;

  /**
   * Empty constructor
   */
  public SIMPMQLinkCorruptException()
  {
    super();
  }

  /**
   * @param message  The message text
   */
  public SIMPMQLinkCorruptException(String message)
  {
    super(message);
  }

  /**
   * @param message  The message text.
   * @param cause  The initial exception
   */
  public SIMPMQLinkCorruptException(String message, Throwable cause)
  {
    super(message, cause);
  }

  /**
   * @param cause The initial exception
   */
  public SIMPMQLinkCorruptException(Throwable cause)
  {
    super(cause);
  }
}
