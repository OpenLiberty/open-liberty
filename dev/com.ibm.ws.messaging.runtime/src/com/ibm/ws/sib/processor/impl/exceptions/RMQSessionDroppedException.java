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

import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;

public class RMQSessionDroppedException extends SISessionDroppedException
{
  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  /**
   *
   */
  private static final long serialVersionUID = -1897549409505172873L;

  /**
   * @param arg0
   */
  public RMQSessionDroppedException(String arg0)
  {
    super(arg0);
  }

  /**
   * @param arg0
   * @param arg1
   */
  public RMQSessionDroppedException(String arg0, Throwable arg1)
  {
    super(arg0, arg1);
  }

  private int exceptionReason = -1;
  private String[] exceptionInserts = null;

  /* (non-Javadoc)
   * @see com.ibm.websphere.sib.exception.SIException#getExceptionInserts()
   */
  public String[] getExceptionInserts()
  {
    if (exceptionInserts == null)
      return super.getExceptionInserts();

    return copyof(exceptionInserts);
  }

  /* (non-Javadoc)
   * @see com.ibm.websphere.sib.exception.SIException#getExceptionReason()
   */
  public int getExceptionReason()
  {
    if (exceptionReason < 0)
      return super.getExceptionReason();

    return exceptionReason;
  }

  /**
   * Set the exception inserts
   * @param exceptionInserts
   */
  public void setExceptionInserts(String[] exceptionInserts)
  {
    this.exceptionInserts = copyof(exceptionInserts);
  }

  /**
   * Set the exception reason code
   * @param exceptionReason
   */
  public void setExceptionReason(int exceptionReason)
  {
    this.exceptionReason = exceptionReason;
  }

  private String[] copyof(String[] arg)
  {
    if (arg == null) return null;
    if (arg.length == 0) return EMPTY_STRING_ARRAY;

    String[] copy = new String[arg.length];
    System.arraycopy(arg,0,copy,0,arg.length);

    return copy;
  }
}
