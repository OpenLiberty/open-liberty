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

import com.ibm.ws.sib.processor.exceptions.SIMPResourceException;

public class RMQResourceException extends SIMPResourceException
{
  private static final long serialVersionUID = 7937371154987272905L;

  /**
   * 
   */
  public RMQResourceException()
  {
    super();
  }

  /**
   * @param arg0
   */
  public RMQResourceException(Throwable arg0)
  {
    super(arg0);
  }

  /**
   * @param arg0
   */
  public RMQResourceException(String arg0)
  {
    super(arg0);
  }

  /**
   * @param arg0
   * @param arg1
   */
  public RMQResourceException(String arg0, Throwable arg1)
  {
    super(arg0, arg1);
  }
}
