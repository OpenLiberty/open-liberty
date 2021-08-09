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

/**
 * Thrown if an attempt is made to flush a stream when a flush
 * is already in progress.
 */
public class FlushAlreadyInProgressException extends Exception 
{
  /** The serial version UID, for version to version compatability */
  private static final long serialVersionUID = -5677438044925077005L;

  /**
   * FlushAlreadyInProgressException is thrown if an attempt
   * is made to flush a stream for which a flush is already
   * in progress.
   */
  public FlushAlreadyInProgressException()
  {
    super();
  }

  /**
   * FlushAlreadyInProgressException is thrown if an attempt
   * is made to flush a stream for which a flush is already
   * in progress.
   *
   * @param arg0  Exception text
   */
  public FlushAlreadyInProgressException(String arg0)
  {
    super(arg0);
  }
}
