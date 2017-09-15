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
 * Exception thrown when trying to register a JMFSchema if its generated id appears
 * to clash with an existing but different schema.  Since these ids are 64bit ids
 * derived from an SHA-1 hashcode on the schema defintion clashes are not expected
 * to occur.
 *
 * If clashes do occur and the schema ids are being generated as zero, it is likely
 * that the JVM properties are incorrectly configured and the SHA-1 security
 * provider cannot be found.
 */

public class JMFSchemaIdException extends JMFException {

  private final static long serialVersionUID = 3879104843122112156L;

  public JMFSchemaIdException(String message) {
    super(message);
  }
  public JMFSchemaIdException(String message, Throwable cause) {
    super(message, cause);
  }
}
