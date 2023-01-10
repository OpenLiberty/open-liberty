/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.threadpool;

/**
 * An exception to indicate the thread pool is full.
 * 
 * @author Gareth Matthews
 */
public class ThreadPoolFullException extends Exception
{
   /** Serial UId */
   private static final long serialVersionUID = 8943458936502633773L;

   public ThreadPoolFullException(Throwable t)
   {
      super(t);
   }
}
