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


/**
 * The Component being called is closed
 */
public class ClosedException extends Exception
{  
  /** The serial version UID, for version to version compatability */
  private static final long serialVersionUID = -7787063452957945077L;

  /**
   * Constructor
   */
  public ClosedException()
  {
    super();
  }

  /**
   * Constructor
   * @param arg extra information about the exception
   */
  public ClosedException(String arg)
  {
    super(arg);
  }
}
