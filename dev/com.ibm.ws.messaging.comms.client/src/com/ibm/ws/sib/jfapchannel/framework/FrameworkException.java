/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.framework;

/**
 * This class is used to throw generic framework problems. Typically this will involve wrapping a
 * channel framework exception.
 * 
 * @author Gareth Matthews
 */
public class FrameworkException extends Exception
{
   /** Serial version UId */
   private static final long serialVersionUID = 6261785028467855112L;
   
   /**
    * Constructor.
    * @param t
    */
   public FrameworkException(Throwable t)
   {
      super(t);
   }
   
   /**
    * Create a FrameworkException specifying an error message.
    * 
    * @param message
    */
   public FrameworkException(String message)
   {
      super(message);
   }
}
