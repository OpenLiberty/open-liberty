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

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.ejb.Asynchronous;
import javax.ejb.PostActivate;
import javax.ejb.Stateful;

@Stateful(name = "NoInterfaceParent")
@Asynchronous
public class NoInterfaceParent {
    public final static String CLASSNAME = NoInterfaceParent.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** Static variable for checking if work was done asynchronously **/
    public static boolean asyncWorkDone = false;

    /** Static variable for thread bean is executing on for comparison to caller thread **/
    public static long beanThreadId = 0;

    public static final long MAX_ASYNC_WAIT = 3 * 60 * 1000; // 3 minutes

    public static final CountDownLatch svBeanLatch = new CountDownLatch(1);

    // Define the enum that calls out the possible values for our list
    public enum Event {
        INJECTION,
        POST_CONSTRUCT,
        PRE_DESTROY,
        PRE_PASSIVATE,
        POST_ACTIVATE,
        AROUND_INVOKE
    }

    // We put the list here, instead of on the bean class itself, so that we can access it from both the
    // lifecycle callback method defined on this class, and the lifecycle callback methods defined on the bean class.
    public ArrayList<Event> ivEvents = new ArrayList<Event>();

    public NoInterfaceParent() {
    }

    public void public_FireAndForget_MethodOnParentClassButNotOnInterfaces() {
        final String methodName = "public_FireAndForget_MethodOnParentClassButNotOnInterfaces";

        // This method exists so that we can prove a public method not called out any interface, but defined on the parent of the bean, is still reachable via
        // the no-interface view.
        // We update a passed in value and return the new value to prove to the caller that we actually executed the method code.
        svLogger.info("Executing NoInterfaceParent." + methodName);

        // save threadId value to static variable for verification method executed on different thread
        NoInterfaceParent.beanThreadId = Thread.currentThread().getId();
        svLogger.info("threadId: " + NoInterfaceParent.beanThreadId);

        // set static variable for work completed to true
        NoInterfaceParent.asyncWorkDone = true;

        svBeanLatch.countDown();

        return;
    }

    @PostActivate
    public void postActivateMethod() {
        svLogger.info("Executing NoInterfaceParent.postActivate method...");
        ivEvents.add(Event.POST_ACTIVATE);
    }
}