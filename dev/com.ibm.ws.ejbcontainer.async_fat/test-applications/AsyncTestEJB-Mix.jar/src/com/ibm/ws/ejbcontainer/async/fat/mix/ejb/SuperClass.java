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
package com.ibm.ws.ejbcontainer.async.fat.mix.ejb;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

public class SuperClass {
    public final static String CLASSNAME = SuperClass.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    public static final long MAX_ASYNC_WAIT = 3 * 60 * 1000; // 3 minutes

    public static CountDownLatch svInheritanceLatch = null;

    public static long superMethThreadId;

    public void test_inheritance() {
        svLogger.info("--> Entering method, test_inheritance, which is located in the super class and not overridden in the bean class.");

        superMethThreadId = Thread.currentThread().getId();
        svLogger.info("--> Depending on the bean class that invokes this method it will be either synchronous or asynchronous, superMethThreadId = " + superMethThreadId);
        svInheritanceLatch.countDown();

        svLogger.info("--> Exiting method, test_inheritance, which is located in the super class and not overridden in the bean class.");
        return;
    }
}