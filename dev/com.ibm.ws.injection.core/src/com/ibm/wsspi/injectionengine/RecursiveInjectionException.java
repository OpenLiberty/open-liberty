/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine;

/**
 * This exception is thrown when an InjectionEngine caller attempts to
 * inject an object into itself. The main place this can occur is when
 * injecting into Stateful Session Beans - if the bean attempts to inject an
 * instance of its same type (via interface/stub), this will cause an infinite
 * recursion scenario and will result in this exception being thrown.
 * <br/>
 * Here is an example:
 * <code>
 *
 * @Stateful @Local(MyLocal.class) public class MyBean implements MyLocal {
 * @EJB MyLocal anotherInstanceOfMyEJB;
 *      }
 *      </code>
 *
 */
public class RecursiveInjectionException extends InjectionException {

    private static final long serialVersionUID = 1023041353362607468L;

    public static enum RecursionDetection {
        NotRecursive,
        Recursive,
        RecursiveAlreadyLogged
    }

    public boolean ivLogged = false;

    /**
     * This method detects whether a given Throwable is the result of a
     * recursive injection. A recursive injection can exist when a bean
     * specifies to inject an instance of the same type of interface into
     * itself, or if it injects another EJB that directly or indirectly
     * injects the original EJB. If this occurs in Stateful Session beans,
     * we will fail because the injection process requires that the injected
     * EJB be started - which requires it to be injected.
     * <br/>
     * This method will attempt to detect an injection cycle by searching
     * the passed-in throwable for a StackOverflowError in the cause chain.
     * In the future we may want to provide a more elegant cycle-detection
     * strategy.
     * <br/>
     *
     * @param t - the Throwable to check
     * @return true if the passed-in Throwable's "cause chain" contains an
     *         instance of StackOverflowError
     */
    public static RecursionDetection detectRecursiveInjection(Throwable t) {
        RecursionDetection rd;
        boolean recursiveInjection = false;
        boolean logged = false;
        Throwable cause = t;
        Throwable lastCause = null;
        while (cause != null && cause != lastCause) {
            if (cause instanceof StackOverflowError ||
                cause instanceof RecursiveInjectionException) {
                recursiveInjection = true;

                if (cause instanceof RecursiveInjectionException &&
                    ((RecursiveInjectionException) cause).ivLogged) {
                    logged = true;
                    break;
                }
            }
            lastCause = cause;
            cause = cause.getCause();
        }

        if (recursiveInjection && logged) {
            rd = RecursionDetection.RecursiveAlreadyLogged;
        } else if (recursiveInjection) {
            rd = RecursionDetection.Recursive;
        } else {
            rd = RecursionDetection.NotRecursive;
        }

        return rd;
    }

    public RecursiveInjectionException() {
        super();
    }

    public RecursiveInjectionException(String message) {
        super(message);
    }

    public RecursiveInjectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public RecursiveInjectionException(Throwable cause) {
        super(cause);
    }

}
