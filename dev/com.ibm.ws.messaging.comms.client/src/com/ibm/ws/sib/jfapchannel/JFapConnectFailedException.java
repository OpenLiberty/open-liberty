/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel;

import com.ibm.websphere.sib.exception.SIResourceException;

/**
 * Exception thrown when a connection attempt fails.
 */
public class JFapConnectFailedException extends SIResourceException
{
   private static final long serialVersionUID = 1765853011911044025L;   // LIDB3706-5.211, D274182
   
   public JFapConnectFailedException(String msg)
   {
      super(msg);
   }
   
   public JFapConnectFailedException(String msg, Throwable t)
   {
      super(msg, t);
   }
}
