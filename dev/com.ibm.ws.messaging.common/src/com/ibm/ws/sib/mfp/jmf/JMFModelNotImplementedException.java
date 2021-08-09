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
 * Exception thrown when a message model ID found in a message or specified on a
 * JMFMessageData.getEncapsulation call is not implemented by any registered
 * JMFEncapsulationManager.
 */

public class JMFModelNotImplementedException extends JMFException {

  private final static long serialVersionUID = 3272624992720101842L;

  public JMFModelNotImplementedException(String message) {
    super(message);
  }
  public JMFModelNotImplementedException(String message, Throwable cause) {
    super(message, cause);
  }
}
