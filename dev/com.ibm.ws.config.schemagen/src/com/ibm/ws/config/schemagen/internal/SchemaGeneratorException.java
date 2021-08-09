/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.schemagen.internal;

/**
 * This subclass of RuntimeException is used in the tool to differenciate between how to handle
 * RuntimeExceptions with nice messages that are probably user error and ones which don't have
 * pretty error messages and are probably product bugs.
 */
public class SchemaGeneratorException extends RuntimeException {

  public SchemaGeneratorException() {
  }

  public SchemaGeneratorException(String message) {
    super(message);
  }

  public SchemaGeneratorException(Throwable cause) {
    super(cause);
  }

  public SchemaGeneratorException(String message, Throwable cause) {
    super(message, cause);
  }

}
