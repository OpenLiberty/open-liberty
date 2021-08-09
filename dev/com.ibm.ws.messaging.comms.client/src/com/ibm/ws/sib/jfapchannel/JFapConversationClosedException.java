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

import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;

/**
 * Thrown if an attempt is made to use a conversation which has
 * already been closed.
 */
public class JFapConversationClosedException extends SIConnectionDroppedException
{
   private static final long serialVersionUID = 637913911551379743L;    // LIDB3706-5.211, D274182
   
   public JFapConversationClosedException(String msg)
   {
      super(msg);
   }
   
   public JFapConversationClosedException(String msg, Throwable t)
   {
      super(msg, t);
   }
}
