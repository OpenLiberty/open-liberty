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
package com.ibm.ws.sib.ffdc;

import junit.framework.AssertionFailedError;

/* ************************************************************************** */
/**
 * An FFDC AssertionError is a subclass of the JUnit AssertionError that allows
 * for easy identification that it results from a call to FFDC to process an
 * exception (and that that FFDC call was unexpected by the test)
 *
 */
/* ************************************************************************** */
public class FFDCAssertionError extends AssertionFailedError
{
  /** serialVersionUID for this class */
  private static final long serialVersionUID = -1496890900028442936L;

  /** The FFDCEntry for this error (if any) */
  private transient FFDCEntry _entry = null;
  
  /* -------------------------------------------------------------------------- */
  /* FFDCAssertionError constructor
   /* -------------------------------------------------------------------------- */
  /**
   * Construct a new FFDCAssertionError.
   *
   * 
   */
  public FFDCAssertionError()
  {
    super();
  }
  /* -------------------------------------------------------------------------- */
  /* FFDCAssertionError constructor
   /* -------------------------------------------------------------------------- */
  /**
   * Construct a new FFDCAssertionError.
   *
   * @param arg0
   */
  public FFDCAssertionError(String arg0)
  {
    super(arg0);
  }
  
  /* -------------------------------------------------------------------------- */
  /* FFDCAssertionError constructor
  /* -------------------------------------------------------------------------- */
  /**
   * Construct a new FFDCAssertionError.
   *
   * @param cause The cause of the FFDC
   * @param arg1  A text string describing the FFDC invocation
   */
  public FFDCAssertionError(Throwable cause, String arg1)
  {
    super(arg1);
    initCause(cause);
  }

  /* -------------------------------------------------------------------------- */
  /* FFDCAssertionError constructor
   /* -------------------------------------------------------------------------- */
  /**
   * Construct a new FFDCAssertionError.
   *
   * @param entry The FFDCEntry that describes this FFDC
   */
  public FFDCAssertionError(FFDCEntry entry)
  {
    super();
    _entry = entry;
  }
  /* -------------------------------------------------------------------------- */
  /* FFDCAssertionError constructor
   /* -------------------------------------------------------------------------- */
  /**
   * Construct a new FFDCAssertionError.
   *
   * @param arg0  The description of this problem
   * @param entry The FFDCEntry that describes this FFDC
   */
  public FFDCAssertionError(String arg0, FFDCEntry entry)
  {
    super(arg0);
    _entry = entry;
  }
  
  /* -------------------------------------------------------------------------- */
  /* FFDCAssertionError constructor
  /* -------------------------------------------------------------------------- */
  /**
   * Construct a new FFDCAssertionError.
   *
   * @param cause The cause of the FFDC
   * @param arg1  A text string describing the FFDC invocation
   * @param entry The FFDCEntry that describes this FFDC
   */
  public FFDCAssertionError(Throwable cause, String arg1, FFDCEntry entry)
  {
    super(arg1);
    initCause(cause);
    _entry = entry;
  }
  
  /* -------------------------------------------------------------------------- */
  /* getFFDCEntry method
  /* -------------------------------------------------------------------------- */
  /**
   * @return The FFDCEntry (if any) recorded for this FFDC (null if the entry didn't get created!)
   */
  public FFDCEntry getFFDCEntry()
  {
    return _entry;
  }
  
  /* -------------------------------------------------------------------------- */
  /* toString method
  /* -------------------------------------------------------------------------- */
  /**
   * @see java.lang.Throwable#toString()
   * @return A string representation of this FFDCEntry (actually the incident stream)
   */
  public String toString()
  {
    String intro = super.toString();
    if (_entry == null)
      return intro;
    else
      return intro+"\n"+_entry;
  }
}
