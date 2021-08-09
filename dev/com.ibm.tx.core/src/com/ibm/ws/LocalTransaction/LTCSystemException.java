package com.ibm.ws.LocalTransaction;
/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * 
 * <p> This class is private to WAS.
 * Any use of this class outside the WAS Express/ND codebase 
 * is not supported.
 *
 */
public class LTCSystemException extends java.lang.Exception
{

   private static final long serialVersionUID = 3096415451898873118L;

   private Exception exception = null;

   /**
    * Default constructor.
    * 
    */
   public LTCSystemException() 
   {
   }

   public LTCSystemException(String msg) 
   {
      super(msg);
   }

   public LTCSystemException(Exception ex) 
   {
      exception = ex;
   }

   public LTCSystemException(String msg, Exception ex) 
   {
      super(msg);
      exception = ex;
   }

   public Exception getNestedException()
   {
      return exception;
   }

}

