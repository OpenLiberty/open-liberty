/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws390.ola.jca;

public class ProxyEJBException extends Exception
{
  static final long serialVersionUID = -8086689648641734710L;

  /**
   * The stack trace for the cause exception.  We don't store the actual
   * exception because of serialization problems between distributed and
   * z/OS.
   */
  private StackTraceElement[] _causeStackTrace = null;

  /**
   * Constructor
   */
  public ProxyEJBException(String message, Throwable cause)
  {
    /*----------------------------------------------------------------------*/
    /* When initializing the superclass, don't save the cause exception.    */
    /*----------------------------------------------------------------------*/
    super(message);

    _causeStackTrace = cause.getStackTrace();
  }

  @Override
  public void printStackTrace()
  {
    printStackTrace(System.err);
  }

  @Override
  public void printStackTrace(java.io.PrintStream s)
  {
    super.printStackTrace(s);

    if (_causeStackTrace != null)
    {
      s.println("Stack trace from original exception:");
      for (int x = 0; x < _causeStackTrace.length; x++)
      {
        if (_causeStackTrace[x] != null)
        {
          s.print("  ");
          s.println(_causeStackTrace[x].toString());
        }
      }
    }
  }

  @Override
  public void printStackTrace(java.io.PrintWriter s)
  {
    super.printStackTrace(s);

    if (_causeStackTrace != null)
    {
      s.println("Stack trace from original exception:");
      for (int x = 0; x < _causeStackTrace.length; x++)
      {
        if (_causeStackTrace[x] != null)
        {
          s.print("  ");
          s.println(_causeStackTrace[x].toString());
        }
      }
    }
  }
}