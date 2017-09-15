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

/** A trivial wrapper for primitive values of the IDREF type.  This is needed to
 * unambiguously distinguish between BINARY and IDREF when inspecting an object argument
 * (for example, in the coder for anySimpleType).  */
public class JMFAddress {
  private byte[] content;
  
  /** Construct a JMFAddress from a non-null byte[] */
  public JMFAddress(byte[] content) {
    this.content = content;
  }
  
  /** Copy constructor */
  public JMFAddress(JMFAddress toCopy) {
    content = (byte[]) toCopy.getContent().clone();
  }
  
  /** Retrieve the content as a byte[] */
  public byte[] getContent() {
    return content;
  }
}
