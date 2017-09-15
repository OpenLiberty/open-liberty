/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.exception;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Does the work for exception classes that implement the
 * DistributedExceptionEnabled interface.
 * 
 * @ibm-api
 */
public class DistributedExceptionInfo implements Serializable {

    private static final long serialVersionUID = 7298223355966269413L;

    private java.lang.String message;
    private java.lang.String className;
    transient private DistributedExceptionEnabled currentException = null;
    private java.lang.String stackTrace;
    private DistributedExceptionInfo previousExceptionInfo;
    private byte[] previousException = null;
    transient private Throwable previousExceptionObject = null;
    private java.lang.String lineSeparator;
    private java.lang.String resourceBundleName = null;
    private java.lang.String resourceKey = null;
    private java.lang.Object[] formatArguments;
    private static final java.lang.String DIST_EX_RESOURCE_BUNDLE_NAME = "com.ibm.ws.cache.resources.dynacache";

    /**
     * Constructor with current exception
     * 
     * @param currentException java.lang.Throwable
     */
    public DistributedExceptionInfo(DistributedExceptionEnabled currentException) {
        super();

        if (currentException != null) {
            setExceptionInfo(currentException);
        }
    }

    /**
     * Constructor with the current and previous exceptions
     * 
     * @param currentException java.lang.Throwable
     * @param previousException java.lang.Throwable
     */
    public DistributedExceptionInfo(DistributedExceptionEnabled currentException, Throwable previousException) {
        super();
        if (currentException != null) {
            setExceptionInfo(currentException);
        }

        if (previousException != null) {
            setPreviousExceptionInfo(previousException);
            this.previousExceptionObject = previousException;
        }
    }

    /**
     * Constructor with current exception
     * 
     * @param currentException java.lang.Throwable
     */
    public DistributedExceptionInfo(String defaultMessage, DistributedExceptionEnabled currentException) {
        super();

        if (currentException != null) {
            setExceptionInfo(currentException);
        }

        message = defaultMessage;
    }

    /**
     * Constructor with current exception
     * 
     * @param currentException java.lang.Throwable
     */
    public DistributedExceptionInfo(String defaultMessage, DistributedExceptionEnabled currentException, Throwable previousException) {
        super();

        if (currentException != null) {
            setExceptionInfo(currentException);
        }

        if (previousException != null) {
            setPreviousExceptionInfo(previousException);
            this.previousExceptionObject = previousException;
        }

        message = defaultMessage;
    }

    /**
     * Constructor with localization message information and the current exception..
     * 
     * @param resourceBundleName java.lang.String The name of resource bundle
     *            that will be used to retrieve the message for getMessage().
     * @param resourceKey java.lang.String The key in the resource bundle that
     *            will be used to select the specific message that is retrieved for
     *            getMessage().
     * @param formatArguments java.lang.Object[] The arguments to be passed to
     *            the MessageFormat class to act as replacement variables in the message
     *            that is retrieved from the resource bundle. Valid types are those supported
     *            by MessageFormat.
     * @param defaultText java.lang.String The default message that will be used in
     *            getMessage() if the resource bundle or the key cannot be found
     * @param currentException DistributedExceptionEnabled The current exception
     * @see getMessage()
     * @see java.text.MessageFormat
     */
    public DistributedExceptionInfo(String resourceBundleName, String resourceKey, Object[] formatArguments, String defaultText, DistributedExceptionEnabled currentException) {
        super();
        this.resourceBundleName = resourceBundleName;
        this.resourceKey = resourceKey;
        this.formatArguments = formatArguments;
        setMessage(defaultText);
        if (currentException != null) {
            setExceptionInfo(currentException);
        }
    }

    /**
     * Constructor with localization message information and the current exception..
     * 
     * @param resourceBundleName java.lang.String The name of resource bundle
     *            that will be used to retrieve the message for getMessage().
     * @param resourceKey java.lang.String The key in the resource bundle that
     *            will be used to select the specific message that is retrieved for
     *            getMessage().
     * @param formatArguments java.lang.Object[] The arguments to be passed to
     *            the MessageFormat class to act as replacement variables in the message
     *            that is retrieved from the resource bundle. Valid types are those supported
     *            by MessageFormat.
     * @param defaultText java.lang.String The default message that will be used in
     *            getMessage() if the resource bundle or the key cannot be found
     * @param currentException DistributedExceptionEnabled The current exception
     * @param previousException java.lang.Throwable The chained exception
     * @see getMessage()
     * @see java.text.MessageFormat
     */
    public DistributedExceptionInfo(String resourceBundleName, String resourceKey, Object[] formatArguments, String defaultText, DistributedExceptionEnabled currentException,
                                    Throwable previousException) {
        super();
        this.resourceBundleName = resourceBundleName;
        this.resourceKey = resourceKey;
        this.formatArguments = formatArguments;
        setMessage(defaultText);
        if (currentException != null) {
            setExceptionInfo(currentException);
        }

        if (previousException != null) {
            setPreviousExceptionInfo(previousException);
            this.previousExceptionObject = previousException;
        }
    }

    /**
     * A private constructor with the previous exception. This is used by the
     * setPreviousExceptionInfo() method when the previous exception is not
     * a DistributedException
     * 
     * @param previousException java.lang.Throwable
     */
    private DistributedExceptionInfo(Throwable previousException) {
        super();

        if (previousException != null) {
            setClassName(previousException.getClass().getName());
            this.message = previousException.getMessage();
            //previous exception is really current exeception for this exceptioninfo object
            this.previousExceptionObject = previousException;
        }
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        if (previousExceptionObject != null) {
            serializePreviousException();
            if (currentException == null)
                setStackTrace(previousExceptionObject);
        }
        if (currentException != null)
            serializeCurrentException();
        out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    /**
     * Get the class name for this exception.
     * 
     * @return java.lang.String The class name
     */
    public java.lang.String getClassName() {
        return className;
    }

    /**
     * Get the default message for this exception
     * 
     * @return java.lang.String The default message
     */
    public String getDefaultMessage() {
        return message;
    }

    /**
     * This is called by getMessage() when the resource bundle or the
     * resource key for the message for this exception is not found.
     * This method will try to retrieve a localized message for this situation.
     * However, if there is a problem retrieving that message, at least
     * provide some information in English (instead of nothing at all).
     * 
     * @param resourceBundle java.lang.String
     * @param resourceKey java.lang.String
     */
    private String getErrorMsg(String resourceBundle, String resourceKey, String defaultEnglishMessage) {
        ResourceBundle bundle = null;
        String errorMsg = null;

        try {
            bundle = ResourceBundle.getBundle(resourceBundle);
        } catch (MissingResourceException e) {
            errorMsg = defaultEnglishMessage;
        }

        if (bundle != null) {
            try {
                errorMsg = bundle.getString(resourceKey);
            } catch (MissingResourceException e) {
                errorMsg = defaultEnglishMessage;
            }
        }

        return errorMsg;
    }

    /**
     * Get a specific exception in a possible chain of exceptions.
     * If there are multiple exceptions in the chain, the most recent one thrown
     * will be returned.
     * If the exceptions does not exist or no exceptions have been chained,
     * null will be returned.
     * 
     * @exception com.ibm.websphere.exception.ExceptionInstantiationException
     *                An exception occurred while trying to instantiate an exception object.
     *                If this exception is thrown, the relevant information can be retrieved
     *                using the getPreviousExceptionInfo() method.
     * 
     * @param String exceptionClassName the class name of the specific exception.
     * @return java.lang.Throwable The specific exception in a chain of
     *         exceptions. If no exceptions have been chained, null will be returned.
     */
    public Throwable getException(String exceptionClassName) throws ExceptionInstantiationException {
        if (exceptionClassName == null) {
            return null;
        }

        Throwable ex = null;
        if (previousExceptionInfo != null) {
            if (previousExceptionInfo.getClassName().equals(exceptionClassName)) {
                ex = getPreviousException();
            } else {
                ex = previousExceptionInfo.getException(exceptionClassName);
            }
        }
        return ex;
    }

    /**
     * Get the format arguments.
     * 
     * @return java.lang.Object[] The format arguments
     */
    public Object[] getFormatArguments() {
        return formatArguments;
    }

    /**
     * Get the line separator that was used when generating the stack trace.
     * 
     * @return java.lang.String
     */
    private java.lang.String getLineSeparator() {
        return lineSeparator;
    }

    /**
     * Retrieve the text message for this exception. The default message (which may be null)
     * will be returned
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
    public java.lang.String getMessage() {
        if (resourceBundleName != null && resourceKey != null) {
            String retrievedMessage = null;
            String internalMessage = null;
            String defaultEnglishMessage = null;
            ResourceBundle bundle = null;

            // retrieve the resource bundle
            try {
                bundle = ResourceBundle.getBundle(resourceBundleName);
            } catch (MissingResourceException e) {
                String key = null;
                if (message == null) {
                    key = "missingResourceBundleNoDft";
                    defaultEnglishMessage = "There was an error retrieving resource bundle {0}. \nThere is no default message.";
                } else {
                    key = "missingResourceBundleWithDft";
                    defaultEnglishMessage = "There was an error retrieving resource bundle {0}. \nThe default exception message is: {2}.";
                }

                internalMessage = getErrorMsg(DIST_EX_RESOURCE_BUNDLE_NAME, key, defaultEnglishMessage);
            }

            // if the resource bundle was successfully retrieved, get the specific message based
            // on the resource key
            if (bundle != null) {
                try {
                    retrievedMessage = bundle.getString(resourceKey);
                } catch (MissingResourceException e) {
                    String key = null;
                    if (message == null) {
                        key = "missingResourceKeyNoDft";
                        defaultEnglishMessage = "There was an error retrieving resource key {1} in resource bundle {0}. \nThere is no default message.";
                    } else {
                        key = "missingResourceKeyWithDft";
                        defaultEnglishMessage = "There was an error retrieving resource key {1} in resource bundle {0}. \nThe default exception message is: {2}.";
                    }

                    internalMessage = getErrorMsg(DIST_EX_RESOURCE_BUNDLE_NAME, key, defaultEnglishMessage);
                }

            }

            String formattedMessage = null;

            // format the message
            if (retrievedMessage != null) {
                if (formatArguments != null) {
                    formattedMessage = MessageFormat.format(retrievedMessage, formatArguments);
                } else {
                    formattedMessage = retrievedMessage;
                }

            } else if (internalMessage != null) {
                Object intFormatArguments[] = { resourceBundleName, resourceKey, message };
                formattedMessage = MessageFormat.format(internalMessage, intFormatArguments);
            } else { // shouldn't happen
                return message; // which could be null
            }

            return formattedMessage;
        } else { // resource information does not exist, so return the default message, if provided
            return message;
        }
    }

    /**
     * Get the original exception in a possible chain of exceptions.
     * If no previous exceptions have been chained, null will be returned.
     * 
     * @exception com.ibm.websphere.exception.ExceptionInstantiationException
     *                An exception occurred while trying to instantiate an exception object.
     *                If this exception is thrown, the relevant information can be retrieved
     *                by using the getPreviousExceptionInfo() method.
     * 
     * @return java.lang.Throwable The first exception in a chain of
     *         exceptions. If no exceptions have been chained, null will be returned.
     */
    public Throwable getOriginalException() throws ExceptionInstantiationException {
        Throwable prevEx = null;
        if (previousExceptionInfo != null) {
            prevEx = previousExceptionInfo.getOriginalException();
            if (prevEx == null) {
                prevEx = getPreviousException();
            }
        }
        return prevEx;
    }

    /**
     * Retrieves the previous exception
     * 
     * @exception com.ibm.websphere.exception.ExceptionInstantiationException
     *                An exception occurred while trying to instantiate an exception object.
     *                If this exception is thrown, the relevant information can be retrieved
     *                by using the getPreviousExceptionInfo() method.
     * 
     * 
     * @return java.lang.Throwable
     */
    public Throwable getPreviousException() throws ExceptionInstantiationException {
        Throwable ex = null;
        if (previousExceptionObject != null)
            return previousExceptionObject;
        if (previousException != null) {
            try {
                final ByteArrayInputStream bais = new ByteArrayInputStream(previousException);
                ObjectInputStream ois = (ObjectInputStream) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                    @Override
                    public Object run() throws IOException {
                        return new ObjectInputStream(bais);
                    }
                });
                ex = (Throwable) ois.readObject();
            } catch (PrivilegedActionException pae) {
                throw new ExceptionInstantiationException(pae.getException());
            } catch (Exception e) {
                throw new ExceptionInstantiationException(e);
            }
        }
        return ex;
    }

    /**
     * Retrieve the previous exception info object.
     * If it doesn't exist, null will be returned.
     * 
     * @return com.ibm.websphere.exception.DistributedExceptionInfo
     */
    public DistributedExceptionInfo getPreviousExceptionInfo() {
        return previousExceptionInfo;
    }

    /**
     * Get the resource bundle name
     * 
     * @return java.lang.String The resource bundle name
     */
    public String getResourceBundleName() {
        return resourceBundleName;
    }

    /**
     * Get the resource key that will be used to retrieve the message from the
     * resource bundle
     * 
     * @return java.lang.String
     */
    public String getResourceKey() {
        return resourceKey;
    }

    /**
     * Insert the method's description here.
     * Creation date: (3/9/00 10:25:31 PM)
     * 
     * @return java.lang.String
     * @param key java.lang.String
     */
    private String getResourceText(String key) {
        String text = null;
        if (key != null) {
            ResourceBundle bundle = null;

            // retrieve the resource bundle
            try {
                bundle = ResourceBundle.getBundle(DIST_EX_RESOURCE_BUNDLE_NAME);
            } catch (MissingResourceException e) {
                // do nothing
            }

            // if the resource bundle was successfully retrieved, get the specific text based
            // on the resource key
            if (bundle != null) {
                try {
                    text = bundle.getString(key);
                } catch (MissingResourceException e) {
                    // do nothing
                }
            }
        } else { // no key was provided
            // do nothing
        }
        return text;
    }

    /**
     * Print the stack trace to a print writer.
     * </p>
     * If this exception was thrown from a remote process,
     * the stack trace will include the stack from the
     * remote process.
     * 
     * @param param java.io.PrintWriter
     */
    public void printStackTrace(java.io.PrintWriter pw) {
        String lineSep = getLineSeparatorProperty();

        printText(pw, "currentException", "Current exception:");
        printStackTrace(lineSep, pw);
        printText(pw, "endOfExTraces", "End of exception traces");
    }

    /**
     * Print the stack trace to a print writer.
     * </p>
     * If this exception was thrown from a remote process,
     * the stack trace will include the stack from the
     * remote process.
     * 
     * @param param java.io.PrintWriter
     */
    void printStackTrace(String lineSep, java.io.PrintWriter pw) {
        printText(pw, "msg", "Message:");
        pw.println("   " + getMessage());
        printText(pw, "stackTrace", "Stack trace:");
        if (stackTrace == null) {
            if (currentException != null)
                serializeCurrentException();
            if (stackTrace == null && previousExceptionObject != null)
                setStackTrace(previousExceptionObject);
        }

        if (stackTrace != null) {
            if (lineSep.equals(lineSeparator)) {
                pw.println(stackTrace);
            } else {
                char traceLineSep = lineSep.charAt(0);
                char currentLineSep = lineSeparator.charAt(0);
                StringBuffer sb = new StringBuffer(stackTrace);
                for (int i = 0; i < sb.length(); i++) {
                    if (sb.charAt(i) == traceLineSep) {
                        sb.setCharAt(i, currentLineSep);
                    }
                }
                pw.println(stackTrace);
            }
        }

        if (previousExceptionInfo != null) {
            printText(pw, "previousException", "Previous exception:");
            previousExceptionInfo.printStackTrace(lineSep, pw);
        }
    }

    /**
     * Insert the method's description here.
     * Creation date: (3/9/00 10:25:31 PM)
     * 
     * @return java.lang.String
     * @param key java.lang.String
     */
    private void printText(PrintWriter pw, String key, String defaultText) {
        String text = null;
        if (key != null) {
            ResourceBundle bundle = null;

            // retrieve the resource bundle
            try {
                bundle = ResourceBundle.getBundle(DIST_EX_RESOURCE_BUNDLE_NAME);
            } catch (MissingResourceException e) {
                text = defaultText;
            }

            // if the resource bundle was successfully retrieved, get the specific text based
            // on the resource key
            if (bundle != null) {
                try {
                    text = bundle.getString(key);
                } catch (MissingResourceException e) {
                    text = defaultText;
                }
            }
        } else { // no key was provided
            text = defaultText;
        }

        pw.println(text);
    }

    /**
     * Set the class name for this exception
     * 
     * @param newClassName java.lang.String
     */
    private void setClassName(java.lang.String newClassName) {
        className = newClassName;
    }

    /**
     * Insert the method's description here.
     * Creation date: (2/28/00 11:26:22 AM)
     * 
     * @param defaultText java.lang.String
     */
    public void setDefaultMessage(String defaultText) {
        setMessage(defaultText);
    }

    private String getLineSeparatorProperty() {
        return (String) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                return System.getProperty("line.separator");
            }
        });
    }

    /**
     * Set the exceptionInfo attribute
     * 
     * @param newExceptionInfo com.ibm.websphere.exception.DistributedExceptionInfo
     */
    private void setExceptionInfo(DistributedExceptionEnabled e) {
        if (e != null) {
            setClassName(e.getClass().getName());
        }
        currentException = e;
    }

    private void serializeCurrentException() {
        // save current line separator for possible conversion during printStackTrace()
        setLineSeparator(getLineSeparatorProperty());
        // Convert stack trace to a String
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        //printStackTrace((DistributedExceptionEnabled)e,pw);
        currentException.printSuperStackTrace(pw);
        setStackTrace(sw.toString());
    }

    /**
     * TBD - delete
     * Set the exceptionInfo attribute for the previous exception
     * 
     * @param prevExceptionInfo Throwable
     */
    private void setExceptionInfo(Throwable e) {
        if (e != null) {
            setClassName(e.getClass().getName());
            // set stack trace also
        }
    }

    /**
     * Set the line separator for this exception
     * 
     * @param newLineSeparator java.lang.String
     */
    private void setLineSeparator(java.lang.String newLineSeparator) {
        lineSeparator = newLineSeparator;
    }

    /**
     * FOR WEBSPHERE INTERNAL USE ONLY
     * Set the localization information.
     * 
     * @param resourceBundleName java.lang.String
     * @param resourceKey java.lang.String
     * @param arguments java.lang.Object[]
     */
    public void setLocalizationInfo(String resourceBundleName, String resourceKey, Object[] formatArguments) {
        this.resourceBundleName = resourceBundleName;
        this.resourceKey = resourceKey;
        this.formatArguments = formatArguments;
    }

    /**
     * Set the message text for this exception.
     * 
     * @param newMessage java.lang.String
     */
    private void setMessage(java.lang.String newMessage) {
        message = newMessage;
    }

    /**
     * Set the previous exception attribute. The exception will be
     * converted to a byte array, so that an unmarshall exception will
     * not be thrown if the exception doesn't exist on the client or on
     * an intermediate server.
     * 
     * @param exception java.lang.Throwable
     */
    private void serializePreviousException() {
        try {
            final ByteArrayOutputStream bas = new ByteArrayOutputStream();
            ObjectOutputStream oos = (ObjectOutputStream) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                @Override
                public Object run() throws IOException {
                    return new ObjectOutputStream(bas);
                }
            });
            oos.writeObject(previousExceptionObject);
            previousException = bas.toByteArray();
        } catch (PrivilegedActionException pae) {
            pae.getException().printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the previous exception info object.
     * 
     * @param newPreviousExceptionInfo com.ibm.websphere.exception.DistributedExceptionInfo
     */
    private void setPreviousExceptionInfo(Throwable previousException) {
        if (previousException instanceof com.ibm.websphere.exception.DistributedExceptionEnabled) {
            previousExceptionInfo = ((DistributedExceptionEnabled) previousException).getExceptionInfo();
        } else {
            previousExceptionInfo = new DistributedExceptionInfo(previousException);
        }
    }

    /**
     * Set the stack trace
     * 
     * @param newStackTrace java.lang.String
     */
    private void setStackTrace(String st) {
        stackTrace = st;
    }

    /**
     * Set the stack trace
     * 
     * @param newStackTrace java.lang.String
     */
    private void setStackTrace(Throwable e) {
        // Convert stack trace to a String
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        stackTrace = sw.toString();
        setLineSeparator(getLineSeparatorProperty());
    }
}
