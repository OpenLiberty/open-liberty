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
package com.ibm.ws.sib.matchspace;

/**Thrown when a recoverable error is found in the matching engine.
 *
 */

public class BadMessageFormatMatchingException 
  extends Exception

{
  private static final long serialVersionUID = -9028567829578930282L;
    // JSA note: this exception class has been divorced from the Greyhound
    // GeneralException framework so that FormattedMessage could be integrated into the
    // Gryphon codebase.  The issue of a common exception framework across Gryphon and
    // Greyhound is TBD.

  public BadMessageFormatMatchingException()
  {
    super();
  }

  public BadMessageFormatMatchingException(String s)
  {
    super("Error in matching: " + s);
  }
  
  public BadMessageFormatMatchingException(Exception e)
  {
    super("Error in matching: ", e);
  }  
}
