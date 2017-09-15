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
package com.ibm.ws.sib.matchspace.selector.impl;

// Gross hack to make the MatchParserTokenManager throw a IllegalStateException rather
// than an Error when something serious goes wrong.  These conditions represent something
// more serious than just an invalid selector (hence IllegalStateException is appropriate
// rather than ParseException), but in server type environments they are still something
// we want to catch and recover from (and we don't want to catch java.lang.Error in most
// contexts).

class Error extends IllegalStateException
{
  private static final long serialVersionUID = 7347119971472877739L;
  public Error()
  {
  }
  public Error(String msg)
  {
    super(msg);
  }
}
