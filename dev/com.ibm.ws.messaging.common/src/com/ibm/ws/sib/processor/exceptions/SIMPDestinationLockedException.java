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
 package com.ibm.ws.sib.processor.exceptions;

import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;

/**
 * @author gatfora
 *
 * Class used to provide custom SIDestinationLockedException properties
 */
public class SIMPDestinationLockedException extends
    SIDestinationLockedException
{
  /**
   * 
   */
  private static final long serialVersionUID = 4296862552917302653L;
  
  public static final int CONSUMERS_ATTACHED = 0;
  public static final int UNCOMMITTED_MESSAGES = 1;
  
  private int _type;

  /**
   * @param arg0
   */
  public SIMPDestinationLockedException(String arg0)
  {
    super(arg0);
  }
  
  /**
   * @param arg0
   * @param type  The reason that this destination is locked.
   */
  public SIMPDestinationLockedException(String arg0, int type)
  {
    super(arg0);
    
    _type = type;
  }
  
  public int getType()
  {
    return _type;
  }
}
