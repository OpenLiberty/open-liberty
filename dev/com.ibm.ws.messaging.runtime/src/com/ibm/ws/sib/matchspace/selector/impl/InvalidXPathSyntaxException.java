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

/**
 * The XPath Compiler has encountered an XPath syntax error in the Selector.
 */
public class InvalidXPathSyntaxException extends Exception 
{
  private static final long serialVersionUID = -6330981097856396962L;
  public InvalidXPathSyntaxException(String selector)
  {
    super("Invalid XPath syntax: " + selector);
  }
}
