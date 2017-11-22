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
package cdi12.observersinjarsbeforebean;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import cdi12.observersinjars.SomeClass;

public class WarBeforeBeansObserver implements Extension {

    private static String beforeBeanDiscoveryCalled = "Method not called";

    private ClassLoader TCCL = null;
    private ClassLoader myCL = null;

    public static String correctClassLoader() {

        return beforeBeanDiscoveryCalled;
    }

    public void beforeBeanDescovery(@Observes BeforeBeanDiscovery event) {
        boolean classLoaderLoadedSuccessfully = true;
        TCCL = Thread.currentThread().getContextClassLoader();
        myCL = this.getClass().getClassLoader();

        try {
            Class.forName(WarBeforeBeansObserver.class.getCanonicalName(), false, TCCL);
        } catch (Throwable t) {
            classLoaderLoadedSuccessfully = false;
        }

        try {
            Class.forName(SomeClass.class.getCanonicalName(), false, TCCL);
        } catch (Throwable t) {
            classLoaderLoadedSuccessfully = false;
        }

        String stackTrace = "" + System.getProperty("line.separator") + System.getProperty("line.separator");

        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            stackTrace += ste + System.getProperty("line.separator");
        }

        if (classLoaderLoadedSuccessfully) {
            beforeBeanDiscoveryCalled = "true. myCL was: " + myCL.toString() + " TCCL was: " + TCCL.toString() + stackTrace;
        } else {
            beforeBeanDiscoveryCalled = "false. myCL was: " + myCL.toString() + " TCCL was: " + TCCL.toString() + stackTrace;
        }
    }
}