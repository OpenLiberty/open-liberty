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
package com.ibm.ws.sib.matchspace.impl;

/**This exception is thrown when a request is made for a field in the
 * message by field name where the field name does not exist in the
 * message format.
 */
public class NoSuchFieldNameException extends Exception
{

  private static final long serialVersionUID = -4181374423206925206L;
  // JSA note: this exception class has been divorced from the Greyhound
  // GeneralException framework so that FormattedMessage could be integrated into the
  // Gryphon codebase.  The issue of a common exception framework across Gryphon and
  // Greyhound is TBD.

  public NoSuchFieldNameException(String fieldName)
  {
    super("NoSuchFieldName: " + fieldName);
  }
}
