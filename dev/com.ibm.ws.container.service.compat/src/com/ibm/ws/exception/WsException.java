/*******************************************************************************
 * Copyright (c) 2001, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.exception;

/**
 * The <code>WsException</code> class is a subclass of <code>java.lang.Exception</code> that provides support for exception chaining
 * semantics that will be available in
 * the 1.4 timeframe. Exceptions that desire to inherit these semantics while
 * running on pre 1.4 JDKs should extend this class.
 * <p>
 * It should be noted that this class will provide only the exception chaining semantics available in 1.4. This is a subset of all the new functionality introduced in 1.4. Most of
 * that functionality requires support from the JVM that is not available on earlier editions.
 * <p>
 * Exceptions that subclass this exception only need to add the various constructors and inherit everything else from this class.
 * <p>
 * See the javadoc for the JDK 1.4 java.lang.Throwable for a full description of the exception chaining functionality.
 * <p>
 * <b>Note:</b> Since the Application Server only supports JDK 1.4 and newer JDKs, new exceptions should extend <code>java.lang.Exception</code> which supports exception chaining.
 * 
 * @ibm-spi
 */
@SuppressWarnings("deprecation")
public class WsException extends Exception implements WsNestedException {

    // D217143 - Add instance variables
    static final long serialVersionUID = 5441454934720178237L;
    private Throwable ivCause = this;
    private transient boolean causeInitialized = false;

    // D200273.1 - Us functionality of RuntimeException as we are now on JDK 1.4
    // We still need to implement the constructors to call Exception
    /**
     * Constructs a new WsException with <code>null</code> as its
     * detail message. The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}.
     */
    public WsException() {
        super();
    }

    /**
     * Constructs a new WsException with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     * 
     * @param message
     *            the detail message. The detail message is saved for
     *            later retrieval by the {@link #getMessage()} method.
     */
    public WsException(String message) {
        super(message);
    }

    /**
     * Constructs a new WsException with the specified cause and a
     * detail message of <tt>(cause==null ? null : cause.toString())</tt> (which
     * typically contains the class and detail message of <tt>cause</tt>). This
     * constructor is useful for WsExceptions
     * that are little more than wrappers for other throwables.
     * 
     * @param cause
     *            the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public WsException(Throwable cause) {
        super(cause);
        causeInitialized = true; // D217143 - 6.0 cause to print on 5.0
        ivCause = cause; // D217143 - 6.0 cause to print on 5.0
    }

    /**
     * Constructs a new WsException with the specified detail message and
     * cause.
     * <p>
     * Note that the detail message associated with <code>cause</code> is <i>not</i> automatically incorporated in this WsException's detail message.
     * 
     * @param message
     *            the detail message (which is saved for later retrieval
     *            by the {@link #getMessage()} method).
     * @param cause
     *            the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public WsException(String message, Throwable cause) {
        super(message, cause);
        causeInitialized = true; // D217143 - 6.0 cause to print on 5.0
        ivCause = cause; // D217143 - 6.0 cause to print on 5.0
    }

    // D217143 - Start additional methods

    /**
     * Return the Throwable that is considered the root cause of this WsException.
     * Null is returned if the root cause is nonexistent or unknown. The root
     * cause is the throwable that caused this WsException to get thrown.
     * <p>
     * The Throwable that is returned is either the Throwable supplied via one of the appropriate constructors, or that set via the {@link #initCause(Throwable)} method. While it
     * is
     * typically unnecessary to override this method, a subclass can override it to return a cause set by some other means, such as a legacy exception chaining infrastructure.
     * <p>
     * 
     * @return the Throwable that is the cause of this WsException, or null if
     *         the cause is nonexistent or unknown.
     */
    public Throwable getCause() {
        try {
            if (!causeInitialized && ivCause != this) {
                super.initCause(ivCause);
            }
        } catch (Throwable t) {
        } finally {
            causeInitialized = true;
        }
        return super.getCause();
    }

    /**
     * Initialize the cause field for this WsException to the specified value.
     * The cause is the Throwable that caused this WsException to get thrown.
     * <p>
     * This method can be called at most once. It is generally called from within a constructor that takes a Throwable, or immediately after constructing this object with a
     * constructor that does not accept a \ Throwable. Thus, if a constructor that takes Throwable as a parameter is used to construct this object, it cannot be called at all.
     * <p>
     * 
     * @param cause
     *            the Throwable which caused this WsException to be thrown.
     *            Null is tolerated.
     * @return a reference to this <code>Throwable</code> instance.
     * @exception IllegalArgumentException
     *                if the specified cause is this
     *                WsException. An exception cannot be its own cause.
     * @exception IllegalStateException
     *                if this WsException was created with a
     *                constructor that specified a cause, or this method has already
     *                been
     *                called on this object.
     */
    public synchronized Throwable initCause(Throwable cause) throws IllegalStateException, IllegalArgumentException {
        super.initCause(cause);
        ivCause = cause;
        causeInitialized = true;
        return this;
    }

    // D217143 End additional methods

}
