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
package com.ibm.websphere.ws.sib.unittest.ras;

/**
 * <p>This type safe enum indicates the severity. Note this is not the
 *   message code severity, but essentially the method on Tr (or SibTr) that
 *   was used to output the message. 
 * </p>
 */
public final class Severity
{
  /** Caused by a Tr.audit call */
  public static final Severity AUDIT = new Severity("audit");
  /** Caused by a Tr.error call */
  public static final Severity ERROR = new Severity("error");
  /** Caused by a Tr.fatal call */
  public static final Severity FATAL = new Severity("fatal");
  /** Caused by a Tr.info call */
  public static final Severity INFO = new Severity("info");
  /** Caused by a Tr.service call */
  public static final Severity SERVICE = new Severity("service");
  /** Caused by a Tr.warning call */
  public static final Severity WARNING = new Severity("warning");
  
  /** The name of this severity */
  private String _name;
  
  /* ---------------------------------------------------------------------- */
  /* Severity method                                    
  /* ---------------------------------------------------------------------- */
  /**
   * Constructor
   * 
   * @param name the login type name.
   */
  private Severity(String name)
  {
    _name = name;
  }
  
  /* ---------------------------------------------------------------------- */
  /* toString method                                    
  /* ---------------------------------------------------------------------- */
  /**
   * @see Object#toString()
   */
  public String toString()
  {
    return _name;
  }
}