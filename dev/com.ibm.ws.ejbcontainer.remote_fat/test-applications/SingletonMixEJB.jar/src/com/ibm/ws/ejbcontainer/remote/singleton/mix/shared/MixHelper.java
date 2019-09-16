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

package com.ibm.ws.ejbcontainer.remote.singleton.mix.shared;

import javax.naming.NamingException;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

public class MixHelper {
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
    public static final int DELAY = 200;

    public static <T> T lookupDefaultLocal(Class<T> interfaceClass, String beanClassSimpleName) throws NamingException {
        return interfaceClass.cast(FATHelper.lookupDefaultBindingEJBJavaGlobal(interfaceClass.getName(), APPLICATION, "SingletonMixEJB", beanClassSimpleName));
    }

    public static <T> T lookupDefaultRemote(Class<T> interfaceClass, String beanClassSimpleName) throws NamingException {
        return interfaceClass.cast(FATHelper.lookupDefaultBindingEJBJavaGlobal(interfaceClass.getName(), "SingletonApp", "SingletonMixEJB",
                                                                               beanClassSimpleName));
    }
}
