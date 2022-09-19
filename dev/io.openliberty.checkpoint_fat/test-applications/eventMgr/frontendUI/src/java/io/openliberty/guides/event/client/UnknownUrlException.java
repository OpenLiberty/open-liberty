// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]
package io.openliberty.guides.event.client;

public class UnknownUrlException extends Exception {

  private static final long serialVersionUID = 1L;

  public UnknownUrlException() {
    super();
  }

  public UnknownUrlException(String message) {
    super(message);
  }
}