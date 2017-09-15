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

/**The syntax of the query string is invlid.
 *
*/
public class QuerySyntaxException extends Exception
{

  private static final long serialVersionUID = -1728691646663802285L;
  public QuerySyntaxException(String msg)
  {
    super(msg);
  }
}
