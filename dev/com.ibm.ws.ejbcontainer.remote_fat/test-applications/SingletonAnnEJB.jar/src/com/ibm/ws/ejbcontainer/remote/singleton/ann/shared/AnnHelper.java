/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.singleton.ann.shared;

import java.lang.reflect.InvocationTargetException;

import javax.ejb.EJBException;
import javax.naming.NamingException;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

public class AnnHelper {
    /**
     * The accuracy of the time returned by System.currentTimeMillis/nanoTime on
     * some platforms (e.g., Windows) is lower than the accuracy used for
     * Thread.sleep/Object.wait/etc. See
     * <tt>http://blogs.sun.com/dholmes/entry/inside_the_hotspot_vm_clocks</tt>.
     * The website above suggests that 15ms is a good upper bound for the number
     * of milliseconds that a single reading can be off, and empirical evidence
     * supports this.
     */
    public static final long TIME_FUDGE_FACTOR = 15;

    /**
     * When comparing two time values, the start and end times can both be off
     * by up to {@link #TIME_FUDGE_FACTOR}, so this value should be used to
     * adjust the subtraction of two times.
     */
    public static final long DURATION_FUDGE_FACTOR = TIME_FUDGE_FACTOR * 2;

    public static final String APPLICATION = "SingletonApp";
    public static final int DELAY = 400;

    public static <T> T lookupDefaultLocal(Class<T> interfaceClass, String beanClassSimpleName) throws NamingException {
        return interfaceClass.cast(FATHelper.lookupDefaultBindingEJBJavaGlobal(interfaceClass.getName(), "SingletonApp", "SingletonAnnEJB", beanClassSimpleName));
    }

    public static <T> T lookupDefaultNoInterface(Class<T> interfaceClass,
                                                 String beanClassSimpleName) throws NamingException {
        return interfaceClass.cast(FATHelper.lookupJavaBinding("java:global/" + APPLICATION + "/" +
                                                               "SingletonAnnEJB" + "/" +
                                                               beanClassSimpleName));
    }

    public static <T> T lookupDefaultRemote(Class<T> interfaceClass,
                                            String beanClassSimpleName) throws NamingException {
        // FatHelper already does the narrow...
        return interfaceClass.cast(FATHelper.lookupDefaultBindingEJBJavaGlobal(interfaceClass.getName(),
                                                                               APPLICATION,
                                                                               "SingletonAnnEJB",
                                                                               beanClassSimpleName));
    }

    public static <T> T getTypedCause(Throwable t, Class<T> expectedType) {
        for (Throwable cause = t; cause != null;) {
            Class<?> c = cause.getClass();
            if (expectedType.isAssignableFrom(c)) {
                return expectedType.cast(cause);
            }

            if (cause instanceof java.rmi.RemoteException) {
                cause = ((java.rmi.RemoteException) cause).detail;
            } else if (cause instanceof EJBException) {
                cause = ((EJBException) cause).getCausedByException();
            } else if (cause instanceof NamingException) {
                cause = ((NamingException) cause).getRootCause();
            } else if (cause instanceof InvocationTargetException) {
                cause = ((InvocationTargetException) cause).getTargetException();
            } else if (cause instanceof org.omg.CORBA.portable.UnknownException) {
                cause = ((org.omg.CORBA.portable.UnknownException) cause).originalEx;
            } else {
                cause = cause.getCause();
            }
        }

        return null;
    }
}
