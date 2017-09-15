package com.ibm.websphere.exception;

/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
public class ExceptionInstantiationException extends DistributedException {

    private static final long serialVersionUID = 4996311351594139105L;

/**
 * Default constructor.
 */
public ExceptionInstantiationException() {
	super();
}
/**
 * Constructor with a message.
 * @param message java.lang.String Message text
 */
public ExceptionInstantiationException(String message) 
{
	super(message);
}
/**
 * Constructor with message text.
 * @param s java.lang.String
 */
public ExceptionInstantiationException(String resourceBundleName,
									String resourceKey,
									Object[] formatArguments,
									String defaultText) 
{
	super(resourceBundleName,resourceKey,formatArguments,defaultText,null);
}
/**
 * Constructor with message text and previous exception.
 * @param text java.lang.String
 * @param exception java.lang.Throwable
 */
public ExceptionInstantiationException(String resourceBundleName,
									String resourceKey,
									Object[] formatArguments,
									String defaultText, 
									Throwable exception)
{
	super(resourceBundleName,resourceKey,formatArguments,defaultText,exception);
}
/**
 * Constructor with a message and an exception to be chained.
 * @param message The message for this exception
 * @param exception java.lang.Throwable The exception to be chained
 */
public ExceptionInstantiationException(String message,Throwable exception) 
{
	super(message,exception);
}
/**
 * Constructor with previous exception.
 * @param exception java.lang.Throwable
 */
public ExceptionInstantiationException(Throwable exception) {
	super(exception);
}
}
