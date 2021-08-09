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

import java.io.IOException;

/**
 * The MatchParser has encountered EOF in the Selector InputStream.
 */
public class SelectorStreamEOFException extends IOException 
{
  private static final long serialVersionUID = -6330981097856396962L;
  public Throwable fillInStackTrace() 
  {
    return null;
  }  
}
