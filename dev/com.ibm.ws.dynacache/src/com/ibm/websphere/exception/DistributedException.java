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
import java.io.*;

/**
 * Provides exception functions desirable in a distributed environment. This
 * includes the following:
 * <ul>
 * <li>Support to allow exceptions to be chained, in the situation
 * where multiple exceptions are thrown during a series of method calls.
 * <li>Saving stack trace information so that printStackTrace() will
 * provide the stack trace of all chained exceptions.
 * <li>Methods to retrieve specific exceptions in the chain.
 * <li>Support for localized messages.
 * </ul>
 * @ibm-api
 */
 
public class DistributedException extends Exception implements DistributedExceptionEnabled {

    private static final long serialVersionUID = -8708570105949230167L;


	private DistributedExceptionInfo exceptionInfo = null;
/**
 * Default constructor.
 */
public DistributedException() {
	super();
	exceptionInfo = new DistributedExceptionInfo(this);
}
/**
 * Constructor with a message.
 * @param message java.lang.String Message text
 */
public DistributedException(String message) 
{
	super();
	exceptionInfo = new DistributedExceptionInfo(message,this);
}
/**
 * Constructor with localization message information.
 * @param resourceBundleName java.lang.String The name of resource bundle
 * that will be used to retrieve the message for getMessage().
 * @param resourceKey java.lang.String The key in the resource bundle that
 * will be used to select the specific message that is retrieved for
 * getMessage().
 * @param formatArguments java.lang.Object[] The arguments to be passed to
 * the MessageFormat class to act as replacement variables in the message
 * that is retrieved from the resource bundle. Valid types are those supported
 * by MessageFormat.
 * @param defaultText java.lang.String The default message that will be used in
 * getMessage() if the resource bundle or the key cannot be found.
 * @see getMessage()
 * @see java.text.MessageFormat 
 */
public DistributedException(String resourceBundleName,
							String resourceKey,
							Object[] formatArguments,
							String defaultText) 
{
	super();
	exceptionInfo = new DistributedExceptionInfo(resourceBundleName,
												resourceKey,
											 	formatArguments,
											 	defaultText,
											 	this);		
}
/**
 * Constructor with localization message information and an exception to be chained.
 * @param resourceBundleName java.lang.String The name of resource bundle
 * that will be used to retrieve the message for getMessage().
 * @param resourceKey java.lang.String The key in the resource bundle that
 * will be used to select the specific message that is retrieved for
 * getMessage().
 * @param formatArguments java.lang.Object[] The arguments to be passed to
 * the MessageFormat class to act as replacement variables in the message
 * that is retrieved from the resource bundle. Valid types are those supported
 * by MessageFormat.
 * @param defaultText java.lang.String The default message that will be used in
 * getMessage() if the resource bundle or the key cannot be found.
 * @param exception java.lang.Throwable The exception that is to be chained.
 * @see getMessage()
 * @see java.text.MessageFormat 
 */
public DistributedException(String resourceBundleName,
							String resourceKey,
							Object[] formatArguments,
							String defaultText, 
							Throwable exception) 
{
	super();
	if (exception == null)
	{
		exceptionInfo = new DistributedExceptionInfo(resourceBundleName,
													resourceKey,
												 	formatArguments,
												 	defaultText,
												 	this);		
	}
	else
	{
		exceptionInfo = new DistributedExceptionInfo(resourceBundleName,
													resourceKey,
												 	formatArguments,
												 	defaultText,
												 	this,
												 	exception);
	}
}
/**
 * Constructor with a message and an exception to be chained.
 * @param message The message for this exception
 * @param exception java.lang.Throwable The exception to be chained
 */
public DistributedException(String message,Throwable exception) 
{
	super();
	if (exception == null)
	{
		exceptionInfo = new DistributedExceptionInfo(message,this);
	}
	else
	{
		exceptionInfo = new DistributedExceptionInfo(message,this,exception);
	}
}
/**
 * Constructor with an exception to be chained.
 * @param exception java.lang.Throwable The exception to be chained
 */
public DistributedException(Throwable exception) 
{
	super();
	if (exception == null)
	{
		exceptionInfo = new DistributedExceptionInfo(this);
	}
	else
	{
		exceptionInfo = new DistributedExceptionInfo(this,exception);
	}
}
/**
 * Get a specific exception in a possible chain of exceptions.
 * If there are multiple exceptions in the chain, the most recent 
 * one thrown will be returned.
 * If the exceptions does not exist or no exceptions have been chained, 
 * null will be returned.
 *
 * @exception com.ibm.websphere.exception.ExceptionInstantiationException 
 * An exception occurred while trying to instantiate the exception object.
 * If this exception is thrown, the relevant information can be retrieved
 * by using the getExceptionInfo() method followed by recursively using
 * the getPreviousExceptionInfo() method on the DistributedExceptionInfo
 * object.
 *  
 * @param String exceptionClassName the class name of the specific exception. 
 * @return java.lang.Throwable The specific exception in a chain of 
 * exceptions. If no exceptions have been chained, null will be returned.
 */
public Throwable getException(String exceptionClassName)
				 throws ExceptionInstantiationException
{
	if (exceptionClassName == null)
	{
		return null;
	}
	
	Throwable ex = exceptionInfo.getException(exceptionClassName);
	return ex;
}
/**
 * Retrieve the DistributedExceptionInfo object.
 * This object is primarily used by a CORBA, non-Java client.
 * It also may be used by a Java client to retrieve information
 * about a previous exception when the getPreveiousException()
 * method throws an exception.
 * 
 * @return com.ibm.websphere.exception.DistributedExceptionInfo
 */
public DistributedExceptionInfo getExceptionInfo() {
	return exceptionInfo;
}
/**
 * Retrieve the text message for this exception. If a resource bundle and resource key
 * have been previously specified when the exception was created, an attempt will be
 * made to retrieve the message from the resource bundle for the language associated
 * with the current locale.
 * The default message (which may be null) will be returned
 * in any of the following situations:
 * <ul>
 * <li>No resource bundle name exists
 * <li>No resource key exists
 * <li>The resource bundle could not be found
 * <li>The key was not found in the resource bundle
 * </ul>
 * 
 * @return java.lang.String message for this exception
 */
public String getMessage()
{
	String message = null;
	if (exceptionInfo != null)
	{
		message = exceptionInfo.getMessage();
	}
	return message;
}
/**
 * Get the original exception in a possible chain of exceptions.
 * If no previous exceptions have been chained, null will be returned.
 *
 * @exception com.ibm.websphere.exception.ExceptionInstantiationException 
 * An exception occurred while trying to instantiate the exception object.
 * If this exception is thrown, the relevant information can be retrieved
 * by using the getExceptionInfo() method followed by recursively using
 * the getPreviousExceptionInfo() method on the DistributedExceptionInfo
 * object.
 * 
 * @return java.lang.Throwable The first exception in a chain of 
 * exceptions. If no exceptions have been chained, null will be returned.
 */
public Throwable getOriginalException()
				throws ExceptionInstantiationException
{
	Throwable origEx = exceptionInfo.getOriginalException();
	return origEx;
}
/**
 * Get the previous exception.
 *
 * @exception com.ibm.websphere.exception.ExceptionInstantiationException 
 * An exception occurred while trying to instantiate the exception object.
 * If this exception is thrown, the relevant information can be retrieved
 * by using the getExceptionInfo() method.
 * 
 * @return java.lang.Throwable The previous exception. If there was no
 * previous exception, null will be returned. .
 */
public Throwable getPreviousException() 
				throws ExceptionInstantiationException
{	
	Throwable prevEx = exceptionInfo.getPreviousException();
	return prevEx;
}
/**
 * Print the exception execution stack.
 * This will include the stack trace of where the exception
 * was created, as well as the stack traces of previous
 * exceptions in the exception chain.
 * 
 */
public void printStackTrace()
{	
    printStackTrace(new PrintWriter(System.err,true));
}
/**
 * Print the exception execution stack to a print writer.
 * This will include the stack trace of where the exception
 * was created, as well as the stack traces of previous
 * exceptions in the exception chain.
 *
 * @param pw java.io.PrintWriter
 */
public void printStackTrace(PrintWriter pw)
{	
		exceptionInfo.printStackTrace(pw);
}


/**
 * Print the exception execution stack.
 * This will include the stack trace of where the exception
 * was created, as well as the stack traces of previous
 * exceptions in the exception chain.
 *
 * @param pw java.io.PrintStream
 */
public void printStackTrace(java.io.PrintStream ps)
{   printStackTrace(new PrintWriter(ps,true));
}


/**
 * This method is used to get the stack trace of the current exception.
 * This method is called by DistributedExceptionInfo to retrieve and
 * save the current stack trace.
 * The information is saved and can be retrieved by printStackTrace();
 * 
 * @param param java.io.PrintWriter
 */
public void printSuperStackTrace(java.io.PrintWriter pw)
{
		super.printStackTrace(pw);
}


/**
 * This method is used to get the stack trace of the current exception.
 * This method is called by DistributedExceptionInfo to retrieve and
 * save the current stack trace.
 * The information is saved and can be retrieved by printStackTrace();
 *
 * @param param java.io.PrintStream
 */
public void printSuperStackTrace(java.io.PrintStream ps)
{
    super.printStackTrace(ps);
}


/**
 * Set the default message for this message. This will be the message returned
 * by getMessage() in any of the following situations:
 * <ul>
 * <li>
 * <li>
 * <li>
 * <li>  
 * </ul>
 * @param defaultText java.lang.String
 */
public void setDefaultMessage(String defaultText)
{
	exceptionInfo.setDefaultMessage(defaultText);
}
/**
 * Set the values to be used for finding the correct
 * translated version of the message and formatting it.
 * @param resourceBundleName java.lang.String - the name of the
 * resource bundle, which is a subclass of java.util.PropertyResourceBundle.
 * @param resourceKey java.lang.String - the key in the resource bundle
 * that specifies the text for the exception message
 * @param arguments java.lang.Object[] -the arguments used to format the
 * message. Valid values are those that are
 * allowed for java.text.MessageFormat.format().
 * @see java.text.MessageFormat
 */
public void setLocalizationInfo(String resourceBundleName, String resourceKey, Object[] formatArguments)
{
	exceptionInfo.setLocalizationInfo(resourceBundleName,resourceKey,formatArguments);
}
}
