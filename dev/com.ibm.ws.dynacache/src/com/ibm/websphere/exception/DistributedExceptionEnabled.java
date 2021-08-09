package com.ibm.websphere.exception;

/*******************************************************************************
 * Copyright (c) 2004, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
public interface DistributedExceptionEnabled {
    /**
     * Get a specific exception in a possible chain of exceptions.
     * If there are multiple exceptions in the chain, the most recent one thrown
     * will be returned.
     * If the exception does not exist or no exceptions have been chained,
     * null will be returned.
     * <dl><dt>
     * If the DistributedExceptionInfo attribute is not null, the
     * return value can be retrieved with the following code:</dt>
     * <dd><b>distributedExceptionInfo.getException(exceptionClassName);</b></dd>
     * </dl>
     * 
     * 
     * @exception com.ibm.websphere.exception.ExceptionInstantiationException
     *                An exception occurred while trying to re-create the exception object.
     *                If this exception is thrown, the relevant information can be retrieved
     *                by using the getExceptionInfo() method followed by recursively using
     *                the getPreviousExceptionInfo() method on the DistributedExceptionInfo
     *                object.
     * 
     * @param String exceptionClassName: The class name of the specific exception.
     * @return java.lang.Throwable: The specific exception in a chain of
     *         exceptions.
     */
    public Throwable getException(String exceptionClassName) throws ExceptionInstantiationException;

    /**
     * Retrieve the exception info for this exception.
     * <dl><dt>This could be coded as:</dt>
     * <dd><b>return distributedExceptionInfo;</b></dd>
     * </dl>
     * 
     * 
     * @return com.ibm.websphere.exception.DistributedExceptionInfo
     */
    public DistributedExceptionInfo getExceptionInfo();

    /**
     * Retrieve the message for this exception.
     * 
     * <p>The following is an example of the code that should be used:
     * <dl>
     * <dt><b>if (distributedExceptionInfo != null)</b></dt>
     * <dd><b>return distributedExceptionInfo.getMessage();</b></dd>
     * <dt><b>else</b></dt>
     * <dd><b>return null</b></dd>
     * </dl>
     * </p>
     * <p>Note: During the construction of the exception and the
     * DistributedExceptionInfo object, there is one situation that results
     * in a call to this method. Since distributedExceptionInfo is still null,
     * a NullPointerException could occur if the check for null is excluded.</p>
     * 
     * @return java.lang.String
     */
    public String getMessage();

    /**
     * Get the original exception in a possible chain of exceptions.
     * If no previous exceptions have been chained, null will be returned.
     * <dl><dt>
     * If the DistributedExceptionInfo attribute is not null, the
     * return value can be retrieved with the following code:
     * </dt>
     * <dd><b>distributedExceptionInfo.getOriginalException();</b></dd>
     * </dl>
     * 
     * @exception com.ibm.websphere.exception.ExceptionInstantiationException
     *                An exception occurred while trying to re-create the exception object.
     *                If this exception is thrown, the relevant information can be retrieved
     *                by using the getExceptionInfo() method followed by recursively using
     *                the getPreviousExceptionInfo() method on the DistributedExceptionInfo
     *                object.
     * 
     * @return java.lang.Throwable: The first exception in a chain of
     *         exceptions. If no exceptions have been chained, null will be returned.
     */
    public Throwable getOriginalException() throws ExceptionInstantiationException;

    /**
     * Get the previous exception, in a possible chain of exceptions.
     * <dl><dt>
     * If the DistributedExceptionInfo attribute is not null, the
     * return value can be retrieved with the following code:
     * </dt>
     * <dd><b>distributedExceptionInfo.getPreviousException();</b></dd>
     * </dl>
     * 
     * @exception com.ibm.websphere.exception.ExceptionInstantiationException
     *                An exception occurred while trying to re-create the exception object.
     *                If this exception is thrown, the relevant information can be retrieved
     *                by using the getExceptionInfo() method.
     * 
     * @return java.lang.Throwable: The previous exception. If there was no
     *         previous exception, null will be returned.
     */
    public Throwable getPreviousException() throws ExceptionInstantiationException;

    /**
     * Print the stack trace for this exception and all chained
     * exceptions.
     * This will include the stack trace from the location where the
     * exception
     * was created, as well as the stack traces of previous
     * exceptions in the exception chain.
     * 
     * <dl><dt>
     * If the DistributedExceptionInfo attribute is not null, the
     * the following code will accomplish this:
     * </dt>
     * <dd><b>distributedExceptionInfo.printStackTrace();</b></dd>
     * </dl>
     * 
     */
    public void printStackTrace();

    /**
     * Print the exception execution stack to a print writer.
     * This will include the stack trace from the location where
     * the exception
     * was created, as well as the stack traces of previous
     * exceptions in the exception chain.
     * 
     * <dl><dt>
     * If the DistributedExceptionInfo attribute is not null, the
     * the following code will accomplish this:
     * </dt>
     * <dd><b>distributedExceptionInfo.printStackTrace(pw);</b></dd>
     * </dl>
     * 
     * @param pw java.io.PrintWriter
     */
    public void printStackTrace(java.io.PrintWriter pw);

    /**
     * <dl><dt>This method is called by DistributedExceptionInfo to retrieve and
     * save the current stack trace.</dt>
     * <dd><b>super.printStackTrace(pw)</b></dd>
     * </dl>
     * 
     * @param param java.io.PrintWriter
     */
    public void printSuperStackTrace(java.io.PrintWriter pw);
}
