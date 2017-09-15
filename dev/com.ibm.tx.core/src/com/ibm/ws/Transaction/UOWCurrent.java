package com.ibm.ws.Transaction;
/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import com.ibm.wsspi.tx.UOWEventListener;

/**
 * @ibm-was-base
 */
public interface UOWCurrent
{
   public static final int  UOW_NONE   = 0;
   public static final int  UOW_LOCAL  = 1;
   public static final int  UOW_GLOBAL = 2;

   int             getUOWType ();
   UOWCoordinator  getUOWCoord ();
   void            registerLTCCallback(UOWCallback callback); //Defect 130321
   void			   setUOWEventListener(UOWEventListener el);
   void			   unsetUOWEventListener(UOWEventListener el);
}
