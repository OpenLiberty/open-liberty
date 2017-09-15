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

/**Thrown when a nonrecoverable error is found in the matching engine.
 *
 */
public class MatchingException extends Exception
{

  private static final long serialVersionUID = -8521349731067726076L;
  // JSA note: this exception class has been divorced from the Greyhound
  // GeneralException framework so that FormattedMessage could be integrated into the
  // Gryphon codebase.  The issue of a common exception framework across Gryphon and
  // Greyhound is TBD.

  public MatchingException()
  {
    super();
  }

  public MatchingException(String s)
  {
    super("Severe error in matching: " + s);
  }
  
  public MatchingException(Exception e)
  {
    super("Severe error in matching: ", e);
  }

}
