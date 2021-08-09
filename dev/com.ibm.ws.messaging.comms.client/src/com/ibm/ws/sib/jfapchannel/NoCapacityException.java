/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
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
 * Thrown when a conversation does not currently have the capacity
 * to accept a message for transmission.  This represents a
 * transitory failure condition.
 * @see com.ibm.ws.sib.jfapchannel.Conversation
 * @deprecated
 */
public class NoCapacityException extends SIResourceException
{
   private static final long serialVersionUID = 6326500461142936760L;   // LIDB3706-5.211, D274182
   
   // begin D226223
   public NoCapacityException()
   {
      super();
   }
   
   public NoCapacityException(String msg)
   {
      super(msg);
   }
   // end D226223
}
