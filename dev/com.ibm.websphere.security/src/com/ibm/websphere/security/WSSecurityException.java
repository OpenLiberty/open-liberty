/*******************************************************************************
 * Copyright (c) 1997, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security;

/**
 * This exception is a generic WebSphere Security exception. Most other WebSphere security
 * exceptions extend this one.
 * 
 * @author IBM
 * @version 1.0
 * @ibm-api
 */

public class WSSecurityException extends java.security.GeneralSecurityException implements java.io.Serializable {

    private static final long serialVersionUID = -1594861211791629634L; //@vj1: Take versioning into account if incompatible changes are made to this class

    /**
     * <p>
     * A default constructor.
     * </p>
     */

    public WSSecurityException() {
        super();
    }

    /**
     * <p>
     * A constructor that accepts an error message. The error message can be retrieved
     * using the getMessage() API.
     * </p>
     * 
     * @param str An error message.
     */

    public WSSecurityException(String str) {
        super(str);
    }

    /**
     * <p>
     * A constructor that accepts a Throwable. The Throwable can be retrieved
     * using the getExceptions() or getCause() API.
     * </p>
     * 
     * @param str An error message.
     */

    public WSSecurityException(Throwable t) {
        super();

        if (exceptions != null)
            exceptions.add(t);
    }

    /**
     * <p>
     * A constructor accepts an error message and original exception. The exception
     * will be added to an ArrayList and other exceptions may be added along the way.
     * The error message can be retrieved using the getMessage() API.
     * </p>
     * 
     * @param str An error message.
     * @param t Any exception type that extends Throwable.
     */

    public WSSecurityException(String str, Throwable t) {
        super(str);
        if (exceptions != null)
            exceptions.add(t);
    }

    /**
     * <p>
     * Add an exception that will be stored in an ArrayList. The method
     * getExceptions can return all the exceptions added via addException. You
     * may also add an exception via the constructor. Use of this API allows
     * exceptions to be propogated back to the originating caller.
     * </p>
     * 
     * @param t Any exception type that extends Throwable
     */
    public void addException(Throwable t) {
        if (exceptions != null)
            exceptions.add(t);
    }

    /**
     * Returns the root cause exception.
     * 
     * @return The Throwable root cause exception.
     */

    @Override
    public Throwable getCause() {
        if (exceptions != null && exceptions.size() > 0)
            return (Throwable) exceptions.get(0);
        else
            return null;
    }

    /**
     * <p>
     * Returns an ArrayList of exceptions that have been added to this exception.
     * </p>
     * 
     * @param t Any exception type that extends Throwable
     */
    public java.util.ArrayList getExceptions() {
        if (exceptions != null)
            return (java.util.ArrayList) exceptions.clone();
        else
            return null;
    }

    /**
     * <p>
     * Formats and prints all the exceptions added to the ArrayList using the addException API.
     * The output will be printed to System error.
     * </p>
     */
    @Override
    public void printStackTrace() {
        if (exceptions != null && exceptions.size() > 0) {
            Throwable cause = (Throwable) exceptions.get(0);
            cause.printStackTrace();
        } else {

            super.printStackTrace();
        }
    }

    private transient final java.util.ArrayList exceptions = new java.util.ArrayList(5);
}
