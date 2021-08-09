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
 * Exception thrown when a 'get' method of JMFMessageData attempts to access something
 *  not actually present in a message.
 */

public class JMFUninitializedAccessException extends JMFException {

  private final static long serialVersionUID = -7113228093650330453L;

  public JMFUninitializedAccessException(String reason) {
    super(reason);
  }
  public JMFUninitializedAccessException(String reason, Throwable cause) {
    super(reason, cause);
  }
}
