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

package com.ibm.ws.sib.mfp.jmf;

/**
 * Base for all JMF exceptions.
 */

public class JMFException extends Exception {

  private final static long serialVersionUID = -162901135129217885L;

  public JMFException(String reason) {
    super(reason);
  }
  public JMFException(String reason, Throwable cause) {
    super(reason, cause);
  }
}
