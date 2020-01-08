/*******************************************************************************
 * Copyright (c) 2000 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.command;

import com.ibm.websphere.exception.*;

/**
 * This is the base class for all Command exceptions. All application exceptions
 * used in a Command implementation must extend the CommandException class.
 * The CommandException class supports the chaining of distributed exceptions.
 * 
 * @ibm-api
 */
public class CommandException
extends DistributedException
{
   private static final long serialVersionUID = 6910737447054324276L;
   protected Throwable throwable = null;

   /**
    * Constructor without parameters.
    */
   public
   CommandException()
   {
      super();
   }
   /**
    * Constructor with a message parameter.
    *
    * @param message A string describing the exception.
    */
   public
   CommandException(String message)
   {
      super(message);
   }
        /**
    * Constructor with information for localizing messages.
    *
    * @param resourceBundleName The name of resource bundle
    *        that will be used to retrieve the message
    *        for getMessage() method.
    * @param resourceKey The key in the resource bundle that
    *        will be used to select the specific message that is
    *        retrieved for the getMessage() method.
    * @param formatArguments The arguments to be passed to
    *        the MessageFormat class to act as replacement variables
    *        in the message that is retrieved from the resource bundle.
    *        Valid types are those supported by MessageFormat.
    * @param defaultText The default message that will be
         *        used by the getMessage() method if the resource bundle or the
         *        key cannot be found.
    */
   public CommandException(String resourceBundleName,             //d75515 add
                      String resourceKey,
                      Object formatArguments[],
                      String defaultText)
   {
     super(resourceBundleName, resourceKey, formatArguments, defaultText);
   }
   /**
    * Constructor with information for localizing messages and an exception
         * to chain.
    *
    * @param resourceBundleName The name of resource bundle
    *        that will be used to retrieve the message
    *        for getMessage() method.
    * @param resourceKey The key in the resource bundle that
    *        will be used to select the specific message
    *        retrieved for the getMessage() method.
    * @param formatArguments The arguments to be passed to
    *        the MessageFormat class to act as replacement variables
    *        in the message that is retrieved from the resource bundle.
    *        Valid types are those supported by MessageFormat.
    * @param defaultText The default message that will be
         *        used by the getMessage() method if the resource bundle or the
         *        key cannot be found.
    * @param exception The exception that is to be chained.
   **/
   public CommandException(String resourceBundleName,           //d75515 add
                      String resourceKey,
                      Object formatArguments[],
                      String defaultText,
                      Throwable exception)
   {
     super(resourceBundleName, resourceKey, formatArguments, defaultText, exception);
   }
   /**
    * Constructor with a throwable exception, which is saved.
    *
    * @param throwable The exception to save.
    */
   public
   CommandException(Throwable throwable)
   {
      super(throwable);                         //d75515
      this.throwable = throwable;
   }

   /**
    * Constructor with a message and throwable exception, which is saved.
    *
    * @param throwable The exception to save.
    */
   public CommandException(String message, Throwable throwable)
   {
      super(message, throwable);
      this.throwable = throwable;
   }

   // I'm following conventions I've seen in ExceptionInInitializerError.
   // Optionally I could rename "throwable" to "detail" and make it public.
   // I wish this stuff was a little more consistent across the JDK.
   /**
    * Returns a throwable exception.
    *
    * @return A throwable exception.
    */
   public Throwable
   getException()
   {
      return throwable;
   }
}
