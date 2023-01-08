/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.processor.exceptions;


public class SIMPControllableNotFoundException extends SIMPException {
  /** The serial version UID, for version to version compatability */
  private static final long serialVersionUID = -2286782716163250456L;

  public SIMPControllableNotFoundException(String msg) {
    super(msg);
  }
  
  public SIMPControllableNotFoundException(Throwable t) {
    super(t);
  }
}
