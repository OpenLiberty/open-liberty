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

import com.ibm.websphere.sib.exception.SIResourceException;


public class SIMPNoResponseException extends SIResourceException 
{

  /**
   * 
   */
  private static final long serialVersionUID = 5298893702048228363L;

  public SIMPNoResponseException() {
    super();
  }

  public SIMPNoResponseException(String message, Throwable cause) {
    super(message, cause);
  }

  public SIMPNoResponseException(String message) {
    super(message);
  }

  public SIMPNoResponseException(Throwable cause) {
    super(cause);
  }

}
