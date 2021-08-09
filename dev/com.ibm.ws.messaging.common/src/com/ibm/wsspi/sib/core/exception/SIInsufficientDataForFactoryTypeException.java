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
 * <p>This exception is thrown if more information is required.</p>
 * 
 * <p>SIB build component: sib.core.selector</p>
 * 
 * <p>This exception is thrown by the SICoreConnectionFactorySelector if the FactoryType
 *   passed in to the getSICoreConnectionFactory methods requires some additional information
 *   in order to obtain the SICoreConnectionFactory the FactoryType refers to.
 * </p>
 */
public class SIInsufficientDataForFactoryTypeException
  extends SIIncorrectCallException
{
  private static final long serialVersionUID = 3983956926597066625L;
  /* ------------------------------------------------------------------------ */
  /* SIInsufficientDataForFactoryTypeException constructor                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This constructor creates the Exception with an explanatory error message.
   * @param message The associated error message.
   */
  public SIInsufficientDataForFactoryTypeException(String message)
  {
    super(message);
  }

}
