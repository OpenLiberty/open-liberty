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
 * Exception thrown when a message is found to be corrupted.  The formatting service does
 * not guarantee that all forms of message corruption will be detected (a strong message
 * integrity check is the business of the security layer, not the formatting service).
 * However, there is an attempt to screen out forms of corruption that may seriously
 * compromise the functioning of the system, particularly inordinately large lengths which
 * result in the allocation of arrays large enough to cause OutOfMemory conditions.
 */

public class JMFMessageCorruptionException extends JMFException {

  private final static long serialVersionUID = -7751666081549831408L;

  public JMFMessageCorruptionException(String message) {
    super(message);
  }
  public JMFMessageCorruptionException(String message, Throwable cause) {
    super(message, cause);
  }
}
