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

public class OLAUOWStateException extends OLANativeException
{
  /**
   * Constructor.  Private so can only be initialized through JNI.
   */
  private OLAUOWStateException(String errString)
  {
    super(errString);
  }
}