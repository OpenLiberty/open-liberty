/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.impl;

/**
 * Trivial pool for exchange receive listeners
 */
public class ExchangeReceiveListenerPool
{
   private final int size;
   private final ExchangeReceiveListener[] pool;
   private int entries = 0;
   
   public ExchangeReceiveListenerPool(int size)
   {
      this.size = size;
      pool = new ExchangeReceiveListener[size];
   }
   
   public synchronized ExchangeReceiveListener allocate(int expectedRequestNumber)
   {
      final ExchangeReceiveListener result;
      
      if (entries == 0)
      {
         result = new ExchangeReceiveListener(this);
      }
      else
      {
         --entries;         
         result = pool[entries];
         pool[entries] = null;
      }
      result.setExpectedRequestNumber(expectedRequestNumber);
      
      return result;
   }
   
   protected synchronized void release(ExchangeReceiveListener listener)
   {
      listener.reset();
      if (entries < size)
      {
         pool[entries] = listener;
         ++entries;
      }
   }
}
