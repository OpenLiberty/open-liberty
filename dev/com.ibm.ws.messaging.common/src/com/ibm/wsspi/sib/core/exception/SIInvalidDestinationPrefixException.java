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

package com.ibm.wsspi.sib.core.exception;

import com.ibm.websphere.sib.exception.SIIncorrectCallException;

/**
 This is an SIIncorrectCallException thrown on createTemporaryDestination if the 
 destinationPrefix is not valid. It is necessary because the validation is 
 non-trivial, as evidenced by SICoreUtils.isDestinationPrefixValid. 
 <p>
 This class has no security implications.
 */
public class SIInvalidDestinationPrefixException extends SIIncorrectCallException
{

  private static final long serialVersionUID = 6712576188966498101L;

  public SIInvalidDestinationPrefixException(String msg)
  {
    super(msg);
  }

}
