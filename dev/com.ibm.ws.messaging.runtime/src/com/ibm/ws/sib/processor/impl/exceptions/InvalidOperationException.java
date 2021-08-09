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
package com.ibm.ws.sib.processor.impl.exceptions;

import com.ibm.websphere.sib.exception.SIErrorException;

/**
 * @author caseyj
 *
 * This exception should be thrown when code attempts to execute a method on 
 * an interface which is invalid for the particular implementation referenced.
 * <p>
 * It is a runtime exception so that it will cause unit test errors without
 * needing try/catching, but can be caught if it is expected.
 */
public class InvalidOperationException extends SIErrorException
{
  /** The serial version UID, for version to version compatability */
  private static final long serialVersionUID = 2005588020713364517L;

  /**
   * Empty constructor
   */
  public InvalidOperationException()
  {
    super();
  }

  /**
   * @param message  The message text
   */
  public InvalidOperationException(String message)
  {
    super(message);
  }

  /**
   * @param message  The message text.
   * @param cause  The initial exception
   */
  public InvalidOperationException(String message, Throwable cause)
  {
    super(message, cause);
  }

  /**
   * @param cause The initial exception
   */
  public InvalidOperationException(Throwable cause)
  {
    super(cause);
  }
}
