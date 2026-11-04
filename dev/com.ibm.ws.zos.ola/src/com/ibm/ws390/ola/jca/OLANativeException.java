/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws390.ola.jca;

public class OLANativeException extends Exception
{
  /**
   * Constructor.  Protected so can only be initialized through JNI or
   * subclasses.
   */
  protected OLANativeException(String errString)
  {
    super(errString);
  }
}