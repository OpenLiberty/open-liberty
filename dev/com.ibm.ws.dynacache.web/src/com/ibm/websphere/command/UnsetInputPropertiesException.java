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

import com.ibm.websphere.command.CommandException;

/**
 * UnsetInputPropertiesException is thrown by the execute() method
 * (in a TargetableCommandImpl class) if a command's isReadyToCallExecute()
 * method returns <code>false</code>.
 * 
 * @ibm-api
 */
public class UnsetInputPropertiesException 
extends CommandException
{
    private static final long serialVersionUID = 4578882603940482327L;
	/**
	 * Constructor without parameters.
	 */
	public
	UnsetInputPropertiesException()
	{        
		super();
	}
	/**
	 * Constructor with a message.
	 * 
	 * @param message A string describing the exception.
	 */
	public
	UnsetInputPropertiesException(String message)
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
	public UnsetInputPropertiesException(String resourceBundleName,             //d75515 add
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
	 */
	public UnsetInputPropertiesException(String resourceBundleName,           //d75515 add
							 String resourceKey,
							 Object formatArguments[],
							 String defaultText,
							 Throwable exception)
	{
	  super(resourceBundleName, resourceKey, formatArguments, defaultText, exception);
	}
}
