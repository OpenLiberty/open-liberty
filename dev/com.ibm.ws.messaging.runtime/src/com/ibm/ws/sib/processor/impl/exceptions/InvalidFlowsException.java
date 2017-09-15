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

public class InvalidFlowsException extends Exception
{
  private static final long serialVersionUID = 3283070569718292269L;

  /**
   * InvalidFlowsException is thrown when the set of flows that are being applied to a ConsumerSet are invalid.
   */
  public InvalidFlowsException()
  {
    super();
  }

  /**
   * InvalidFlowsException is thrown when the set of flows that are being applied to a ConsumerSet are invalid.
   *
   * @param arg0  Exception text
   */
  public InvalidFlowsException(String arg0)
  {
    super(arg0);
  }
}
