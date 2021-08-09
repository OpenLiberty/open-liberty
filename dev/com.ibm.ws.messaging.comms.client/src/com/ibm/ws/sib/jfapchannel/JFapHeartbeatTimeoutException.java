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
 * Thrown if a heart beat timeout occures.
 */
public class JFapHeartbeatTimeoutException extends SIConnectionDroppedException
{
   private static final long serialVersionUID = -7831712835689004896L;  // LIDB3706-5.211, D274182
   
   public JFapHeartbeatTimeoutException(String msg)
   {
      super(msg);
   }
   
   public JFapHeartbeatTimeoutException(String msg, Throwable t)
   {
      super(msg, t);
   }
}
