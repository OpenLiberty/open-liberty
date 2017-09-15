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

package com.ibm.ws.sib.trm.topology;

/**
 * This class represents an invalid byte exception and is thrown when
 * a LinkCellule or Messaging engine is constructed from a byte[] which
 * was not obtained from a LinkCellule or MessagingEngine object.
 */

public class InvalidBytesException extends Exception {

  private static final long serialVersionUID = -3027843595522849353l; // cf WAS60.SIB

  public InvalidBytesException (String s) {
    super(s);
  }

}
