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
 * Exception thrown when a the JMFSchema that would be required to make a call to the
 * JMFI correct is not the actual JMFSchema in use in the context of the call.
 */

public class JMFSchemaViolationException extends JMFException {

  private final static long serialVersionUID = -351451162120378552L;

  public JMFSchemaViolationException(String message) {
    super(message);
  }
  public JMFSchemaViolationException(String message, Throwable cause) {
    super(message, cause);
  }
}
