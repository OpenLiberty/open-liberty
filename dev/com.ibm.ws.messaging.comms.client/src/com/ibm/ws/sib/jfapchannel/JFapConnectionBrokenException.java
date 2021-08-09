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
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * Exception thrown when a connection is broken.  Any
 * conversation receiving this exception can be confident
 * that its underlying connection is broken.
 */
public class JFapConnectionBrokenException extends SIConnectionLostException
{
   private static final long serialVersionUID = 8285991078219931523L;   // LIDB3706-5.211, D274182
   
   public JFapConnectionBrokenException(String msg)
   {
      super(msg);
   }
   
   public JFapConnectionBrokenException(String msg, Throwable t)
   {
      super(msg, t);
   }
}
