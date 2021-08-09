/*******************************************************************************
 * Copyright (c) 2001, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;

import javax.ejb.AccessLocalException;
import javax.ejb.EJBException;
import javax.ejb.NoSuchEJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.TransactionRequiredLocalException;
import javax.ejb.TransactionRolledbackLocalException;
import javax.naming.NamingException;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanO;
import com.ibm.ejs.container.ContainerException;
import com.ibm.ejs.container.UncheckedException;
import com.ibm.ejs.persistence.EJSPersistenceException;
import com.ibm.websphere.cpi.CPIException;
import com.ibm.websphere.cpmi.CPMIException;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.EJBMethodMetaData;
import com.ibm.ws.exception.WsNestedException;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.RecursiveInjectionException;

@SuppressWarnings("deprecation")
public class ExceptionUtil {
    private static TraceComponent tc = Tr.register(ExceptionUtil.class, "EJBContainer", "com.ibm.ejs.container.container");

    public final static String throwableToString(Throwable t) {
        //---------------------------------------------------------
        // String rep of a Throwable includes the associated
        // stack trace
        //---------------------------------------------------------

        StringWriter s = new StringWriter();
        PrintWriter p = new PrintWriter(s);
        printStackTrace(t, p);
        return s.toString();
    }

    //d408351 - determine if this exception should be logged:
    private final static boolean hasBeenLogged(Throwable t) {
        boolean hasBeenLogged = false;
        Throwable cause = t;
        Throwable lastCause = null;
        while (cause != null && cause != lastCause) {
            if (cause instanceof RecursiveInjectionException) {
                if (((RecursiveInjectionException) cause).ivLogged) {
                    hasBeenLogged = true;
                } else {
                    // if we have not logged this recursive exception, make sure that it is only logged once
                    ((RecursiveInjectionException) cause).ivLogged = true;
                    hasBeenLogged = false;
                }
                break;
            }
            lastCause = cause;
            cause = cause.getCause();
        }

        return hasBeenLogged;
    }

    //d408351 - added new signature with customizable TraceComponent
    public final static void logException(TraceComponent compTc, Throwable t, EJBMethodMetaData m, BeanO bean) {
        //d408351 - only log recursive exceptions if they have not been logged before
        if (hasBeenLogged(t)) {
            return;
        }

        BeanId beanId = null;
        if (bean != null) {
            beanId = bean.getId();
        }

        if (m == null) {
            if (beanId == null) {
                Tr.error(compTc, "NON_APPLICATION_EXCEPTION_CNTR0018E", t);
            } else {
                Tr.error(compTc, "NON_APPLICATION_EXCEPTION_ON_BEAN_CNTR0021E", new Object[] { t, beanId });
            }
        } else {
            String methodName = m.getMethodName();
            if (beanId == null) {
                Tr.error(compTc, "NON_APPLICATION_EXCEPTION_METHOD_CNTR0019E", new Object[] { t, methodName });
            } else {
                Tr.error(compTc, "NON_APPLICATION_EXCEPTION_METHOD_ON_BEAN_CNTR0020E", new Object[] { t, methodName, beanId });
            }

        }

    }

    public static void logException(Throwable t) {
        logException(ExceptionUtil.tc, t, null, null);
    }

    private final static void printStackTrace(Throwable t, PrintWriter p) {
        t.printStackTrace(p);
    }

    /**
     * Finds the root cause of a Throwable that occured. This routine will continue to
     * look through chained Throwables until it cannot find another chained Throwable
     * and return the last one in the chain as the root cause.
     *
     * @param throwable must be a non-null reference of a Throwable object
     *            to be processed.
     **/
    static public Throwable findRootCause(Throwable throwable) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "findRootCause: " + throwable);
        }

        Throwable root = throwable;
        Throwable next = root;

        while (next != null) {
            root = next;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "finding cause of: " + root.getClass().getName());
            }

            if (root instanceof java.rmi.RemoteException) {
                next = ((java.rmi.RemoteException) root).detail;
            } else if (root instanceof WsNestedException) // d162976
            {
                next = ((WsNestedException) root).getCause(); // d162976
            } else if (root instanceof TransactionRolledbackLocalException) //d180095 begin
            {
                next = ((TransactionRolledbackLocalException) root).getCause();
            } else if (root instanceof AccessLocalException) {
                next = ((AccessLocalException) root).getCause();
            } else if (root instanceof NoSuchObjectLocalException) {
                next = ((NoSuchObjectLocalException) root).getCause();
            } else if (root instanceof TransactionRequiredLocalException) {
                next = ((TransactionRequiredLocalException) root).getCause();
            }
            //            else if (root instanceof InvalidActivityLocalException)
//            {
//                root = ((InvalidActivityLocalException) root).getCause();
//            }
//            else if (root instanceof ActivityRequiredLocalException)
//            {
//                root = ((ActivityRequiredLocalException) root).getCause();
//            }
//            else if (root instanceof ActivityCompletedLocalException)
//            {
//                next = ((ActivityCompletedLocalException) root).getCause(); //d180095 end
//            }
            else if (root instanceof NamingException) {
                next = ((NamingException) root).getRootCause();
            } else if (root instanceof InvocationTargetException) {
                next = ((InvocationTargetException) root).getTargetException();
            } else if (root instanceof org.omg.CORBA.portable.UnknownException) {
                next = ((org.omg.CORBA.portable.UnknownException) root).originalEx;
            } else if (root instanceof InjectionException) // d436080
            {
                next = root.getCause();
            } else {
                next = null;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "findRootCause returning: " + root);

        return root;
    } // findRootCause

    /**
     * Returns the 'external' cause of the specified Throwable, or null
     * if the cause is nonexistant or unknown. <p>
     *
     * The 'external' cause is defined to be the first chained exception
     * that is not an internal EJB Container exception. CSIExcpetion is an
     * example of an internal EJB Container exception. <p>
     *
     * If all chained exceptions are internal EJB Container exceptions, then
     * the last one in the chain will be converted to an EJBException and
     * that EJBException will be returned as the cause. <p>
     *
     * Null is returned if the specified Throwable does not have a chained
     * exception. <p>
     *
     * @param throwable must be a non-null reference of a Throwable object
     *            to be processed.
     *
     * @return the 'external' cause of the specified exception, or null.
     **/
    // d366807
    static public Throwable findCause(Throwable throwable) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "findCause: " + throwable);

        Throwable cause = throwable.getCause();

        if (cause == null && throwable instanceof EJBException)
            cause = ((EJBException) throwable).getCausedByException();

        if (cause != null) {
            // --------------------------------------------------------------------
            // If the cause happens to be a WebSphere specific subclass of
            // EJBException or RemoteException, then convert it to a plain
            // EJBException or unwrap it, and continue looking for a
            // non-internal WebSphere specific exception.
            // --------------------------------------------------------------------
            String causeMessage = null;
            while (cause instanceof ContainerException ||
                   cause instanceof UncheckedException ||
                   cause instanceof EJSPersistenceException ||
                   cause instanceof CPIException ||
                   cause instanceof CPMIException ||
                   cause instanceof CSIException ||
                   cause instanceof InjectionException || // d436080
                   (cause instanceof EJBException &&
                    cause instanceof WsNestedException)) {
                Throwable nextCause = cause.getCause();
                if (nextCause == null) {
                    // Nothing was nested in the WebSphere specific exception,
                    // so convert to EJBException, copying the message and stack.
                    StackTraceElement[] stackTrace = cause.getStackTrace();
                    if (causeMessage == null) {
                        causeMessage = cause.getMessage();
                    }
                    cause = new EJBException(causeMessage);
                    cause.setStackTrace(stackTrace);
                } else {
                    if (cause instanceof InjectionException) {
                        causeMessage = cause.getMessage();
                    }
                    cause = nextCause;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "findCause: " + cause);

        return cause;
    }

    /**
     * Returns an Exception with the message text "See nested Throwable"
     * and the specified cause, or the cause itself, if it is already an
     * Exception. <p>
     */
    // F71894.1
    public static Exception Exception(Throwable cause) {
        return (cause instanceof Exception) ? (Exception) cause : new Exception("See nested Throwable", cause);
    }

    /**
     * Returns an EJBException with the message text "See nested exception"
     * and the specified cause, or the cause itself, if it is already an
     * EJBException. <p>
     *
     * Identical to the method EJBException(message, cause), except the
     * message text is hard coded at "See nested exception" for convenience. <p>
     *
     * @param cause the cause of the EJBException. If already an EJBException
     *            it will just be returned; null is permitted.
     * @returns an EJBException with appropriate nested exception, stack,
     *          and message text.
     **/
    // d259882
    public static EJBException EJBException(Throwable cause) {
        return EJBException("See nested exception", cause);
    }

    /**
     * Returns an EJBException with the specified message text and cause. This
     * method should only be used when a EJBException instance is needed because
     * either the message text is necessary or because this exception will be
     * thrown to customer code and a subclass would be inappropriate. In
     * general, EJBException(message, cause) should be preferred. <p>
     *
     * This is a convienience method that consolidates all of the code
     * to properly construct an EJBException or detect that the cause
     * already is an EJBException (or WsEJBException). And, accounts
     * for the deficiencies of the EJBException class, taking care of
     * the following: <p>
     *
     * <ul>
     * <li> Nesting the appropriate exception in the EJBException,
     * including an instance of Throwable.
     * <li> Insuring the methods getCause() and getCausedByException() work
     * as expected.
     * <li> Unwrapping any WsEJBException or Container specific RemoteException
     * until a non-WebSphere nested exception is found.
     * <li> Just returning any nested EJBException.
     * <li> Insuring the stack is set appropriately.
     * <li> Insuring the 'message' text is appropriate and does not
     * result in repeated "nested exception is" text.
     * </ul> <p>
     *
     * This method is intended to be used in EJB Container code where an
     * exception has been caught and nested in a WebSphere specific exception
     * and needs to be reported back to customer code as an EJBException.
     * It may be used with out regard to whether the caught exception might
     * already be an EJBException. <p>
     *
     * @param message the preferred message text for the returned EJBException.
     * @param cause the cause of the EJBException. If already an EJBException
     *            it will just be returned; null is permitted.
     * @returns an EJBException with appropriate nested exception, stack,
     *          and message text.
     **/
    // d259882
    public static EJBException EJBException(String message,
                                            Throwable cause) {
        EJBException ejbex = null;

        // -----------------------------------------------------------------------
        // If a cause was not specified, then this method has been called to
        // just create a generic EJBException with the specified message.
        // -----------------------------------------------------------------------
        if (cause == null) {
            ejbex = new EJBException(message);
        }

        // -----------------------------------------------------------------------
        // If the cause happens to be a WebSphere specific subclass of
        // EJBException or RemoteException, then convert it to a plain
        // EJBException or at least unwrap it, so a plain EJBException
        // is created below.
        // -----------------------------------------------------------------------
        String causeMessage = null;
        while (cause != null &&
               (!(cause instanceof RecursiveInjectionException)) && // d408351
               (cause instanceof ContainerException ||
                cause instanceof UncheckedException ||
                cause instanceof EJSPersistenceException ||
                cause instanceof CPIException ||
                cause instanceof CPMIException ||
                cause instanceof CSIException ||
                cause instanceof InjectionException || // d436080
                cause instanceof ManagedObjectException ||
                (cause instanceof EJBException &&
                 cause instanceof WsNestedException))) {

            Throwable nextCause = cause.getCause();
            if (nextCause == null) {
                // Nothing was nested in the WebSphere specific exception,
                // so convert to EJBException, copying the message and stack.
                if (causeMessage == null) {
                    causeMessage = cause.getMessage();
                }
                ejbex = new EJBException(causeMessage);
                ejbex.setStackTrace(cause.getStackTrace());
            } else if (causeMessage == null && cause instanceof InjectionException) {
                causeMessage = cause.getMessage();
            }
            cause = nextCause;
        }

        // -----------------------------------------------------------------------
        // If the cause is not already an EJBException, then create a new
        // EJBException.  Since EJBException doesn't have a constructor that
        // accepts a Throwable, wrap any Throwable in an Exception...
        // but note that the cause on Throwable will be the root cause
        // (i.e. not wrapped in Exception).  In all cases, insure getCause()
        // works if there is a cause, and clear the stack if the EJBException
        // wasn't thrown by the customer.... let the cause stack point to
        // the failure.
        // -----------------------------------------------------------------------
        if (ejbex == null) {
            if (cause instanceof EJBException) {
                ejbex = (EJBException) cause;

                // EJBException doesn't normally set the cause on Throwable, so
                // let's do that to be nice :-)
                // Geronimo EJBException.getCause returns getCausedbyException, so
                // we do not expect this code to be used.                     F53643
                cause = ejbex.getCausedByException();
                if (cause != null && ejbex.getCause() == null)
                    ejbex.initCause(cause);
            } else {
                if (causeMessage == null) {
                    causeMessage = message;
                }
                ejbex = new EJBException(causeMessage, Exception(cause));

                // And finally... insure the cause is set on Throwable.
                // Geronimo EJBException.getCause returns getCausedbyException, so
                // we do not expect this code to be used.                     F53643
                if (ejbex.getCause() == null) { // F743-16279
                    ejbex.initCause(cause);
                }
            }
        }

        return ejbex;
    }

    /**
     * Returns a new NoSuchEJBException with the specified message text and
     * cause. The cause will be nested even if it is a NoSuchEJBException. <p>
     *
     * @param message the message text for the returned NoSuchEJBException.
     * @param cause the cause of the NoSuchEJBException; null is permitted.
     * @returns a NoSuchEJBException with appropriate nested exception, stack,
     *          and message text.
     **/
    // d632115
    public static NoSuchEJBException NoSuchEJBException(String message,
                                                        Throwable cause) {
        NoSuchEJBException nsejb;

        if (cause == null) {
            nsejb = new NoSuchEJBException(message);
        } else {
            if (cause instanceof Exception) {
                nsejb = new NoSuchEJBException(message, (Exception) cause);
            } else {
                Exception wrappedCause = new Exception("See nested Throwable", cause);
                nsejb = new NoSuchEJBException(message, wrappedCause);
                cause = wrappedCause;
            }

            // And finally... insure the cause is set on Throwable.
            // Geronimo EJBException.getCause returns getCausedbyException, so
            // we do not expect this code to be used.                     F53643
            if (nsejb.getCause() == null) {
                nsejb.initCause(cause);
            }
        }

        return nsejb;
    }

    /**
     * Returns a RemoteException with the message text "See nested exception"
     * and the specified cause, or the cause itself, if it is already a
     * RemoteException. <p>
     *
     * Identical to the method RemoteException(message, cause), except the
     * message text is hard coded at "See nested exception" for convenience. <p>
     *
     * @param cause the cause of the RemoteException. If already a RemoteException
     *            it will just be returned; null is permitted.
     * @returns a RemoteException with appropriate nested exception, stack,
     *          and message text.
     **/
    // d259882
    public static RemoteException RemoteException(Throwable cause) {
        return RemoteException("See nested exception", cause);
    }

    /**
     * Returns a RemoteException with the specified message text and
     * cause, or the cause itself, if it is already a RemoteException. <p>
     *
     * This is a convienience method that consolidates all of the code
     * to properly construct a RemoteException or detect that the cause
     * already is a RemoteException, taking care of the following: <p>
     *
     * <ul>
     * <li> Nesting the appropriate exception in the RemoteException.
     * <li> Unwrapping any WsEJBException or Container specific RemoteException
     * until a non-WebSphere nested exception is found.
     * <li> Just returning any nested RemoteException.
     * <li> Insuring the stack is set appropriately.
     * <li> Insuring the 'message' text is appropriate and does not
     * result in repeated "nested exception is" text.
     * </ul> <p>
     *
     * This method is intended to be used in EJB Container code where an
     * exception has been caught and nested in a WebSphere specific exception
     * and needs to be reported back to customer code as a RemoteException.
     * It may be used with out regard to whether the caught exception might
     * already be a RemoteException. <p>
     *
     * @param message the preferred message text for the returned RemoteException.
     * @param cause the cause of the RemoteException. If already a RemoteException
     *            it will just be returned; null is permitted.
     * @returns a RemoteException with appropriate nested exception, stack,
     *          and message text.
     **/
    // d259882
    public static RemoteException RemoteException(String message,
                                                  Throwable cause) {
        RemoteException remote = null;

        // -----------------------------------------------------------------------
        // If a cause was not specified, then this method has been called to
        // just create a generic RemoteException with the specified message.
        // -----------------------------------------------------------------------
        if (cause == null) {
            remote = new RemoteException(message);
        }

        // -----------------------------------------------------------------------
        // If the cause happens to be a WebSphere specific subclass of
        // EJBException or RemoteException, then convert it to a plain
        // RemoteException or at least unwrap it, so a plain RemoteException
        // is created below.
        // -----------------------------------------------------------------------
        String causeMessage = null;
        while (cause != null &&
               (cause instanceof ContainerException ||
                cause instanceof UncheckedException ||
                cause instanceof EJSPersistenceException ||
                cause instanceof CPIException ||
                cause instanceof CPMIException ||
                cause instanceof CSIException ||
                cause instanceof InjectionException || // d436080
                (cause instanceof EJBException &&
                 cause instanceof WsNestedException))) {
            Throwable nextCause = cause.getCause();
            if (nextCause == null) {
                // Nothing was nested in the WebSphere specific exception,
                // so convert to RemoteException, copying the message and stack.
                if (causeMessage == null) {
                    causeMessage = cause.getMessage();
                }
                remote = new RemoteException(causeMessage);
                remote.setStackTrace(cause.getStackTrace());
            } else if (causeMessage == null && cause instanceof InjectionException) {
                causeMessage = cause.getMessage();
            }
            cause = nextCause;
        }

        // -----------------------------------------------------------------------
        // If the cause is not already a RemoteException, then create a new
        // RemoteException and clear the stack ... let the cause stack point to
        // the failure.
        // -----------------------------------------------------------------------
        if (remote == null) {
            if (cause instanceof RemoteException) {
                remote = (RemoteException) cause;
            } else {
                if (causeMessage == null) {
                    causeMessage = message;
                }
                remote = new RemoteException(message, cause);
            }
        }

        return remote;
    }

    /**
     * Returns the 'base' message text of any exception, stripping off all
     * nested exception text ("nested exception is: etc."). <p>
     *
     * The getMessage() method of both RemoteException and EJBException have
     * the helpful/annoying behavior of including the message text/stack for
     * all nested exceptions as well. This causes duplicate text/stack
     * information to be logged by WebSphere RAS support, which also includes
     * the nested exception information. And, all of this can be further
     * duplicated in the EJB Container mapping code, which converts exceptions
     * to Remote or Local exceptions, and would like to include the same
     * message text in the mapped exception... which can result in the
     * exception text/stack being duplicated 4 or more times. <p>
     *
     * Also, toString() must be avoided, as it would include the name of
     * the exception in the message text. <p>
     *
     * This method will return just the base part of an exceptions message
     * text... removing all nested exception information. And, it will work
     * for RemoteException and EJBException, as well as any other Exceptions
     * that contain similar formatting. <p>
     **/
    public static String getBaseMessage(Throwable exception) {
        String message = null;

        // -----------------------------------------------------------------------
        // RemoteException contains a 'detail' field that holds the 'cause',
        // instead of the normal cause defined by Throwable.  When detail is
        // set, getMessage() will include the text and stack of the detail,
        // which may ends up duplicating the message text.
        //
        // To avoid this, 'detail' is temporarily cleared, resulting in
        // getMessage() returning only the message text specified when the
        // RemoteException was created.                                    d366807
        // -----------------------------------------------------------------------
        if (exception instanceof RemoteException) {
            RemoteException rex = (RemoteException) exception;
            Throwable detail = rex.detail;
            rex.detail = null;

            message = rex.getMessage();

            rex.detail = detail;

            // We frequently create remote exceptions with an empty message
            // to avoid redundant messages being printed in stack traces.  If the
            // remote exception has an empty message, use the detail.       d739198
            if ("".equals(message) && detail != null) {
                message = getBaseMessage(detail);
            }
        }

        // -----------------------------------------------------------------------
        // Other exceptions, like EJBException, do not contain a public field
        // that holds the 'cause'. So for these exceptions, the returned
        // message text is just parsed for the beginning of the nested
        // exceptions... and then truncated.
        // -----------------------------------------------------------------------
        else if (exception != null) {
            message = exception.getMessage();
            if (message != null) {
                if (message.startsWith("nested exception is:")) {
                    message = null;
                } else {
                    int nestIndex = message.indexOf("; nested exception is:");
                    if (nestIndex > -1)
                        message = message.substring(0, nestIndex);
                }
            }
        }
        return message;
    }

}
