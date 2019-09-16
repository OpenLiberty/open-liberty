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

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.SessionContext;

// In the xml deployment descriptor, this class is defined as being:
// a) an EJB
// b) exposed through the NoInterface view
public class NoInterfaceBean3 {
    public final static String CLASSNAME = NoInterfaceBean3.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** Static variable for checking if work was done asynchronously **/
    public static boolean asyncWorkDone = false;

    /** Static variable for timeout value for performing work asynchronously **/
    public static int asyncTimeout = 5000;

    /** Static variable for thread bean is executing on for comparison to caller thread **/
    public static long beanThreadId = 0;

    // This variable should get populated via an injection called out via XML stanza.
    public String ivVariableForInjectedString;

    public static final long MAX_ASYNC_WAIT = 3 * 60 * 1000; // 3 minutes

    public static final CountDownLatch svBeanLatch = new CountDownLatch(1);

    // Inject the sessionContext for the bean
    @Resource
    SessionContext context;

    @Asynchronous
    public void test_fireAndForget() {
        final String methodName = "test_fireAndForget";

        svLogger.info("Executing NoInterfaceBean3." + methodName + " with NO input parm **");
        // save threadId value to static variable for verification method executed on different thread
        NoInterfaceBean3.beanThreadId = Thread.currentThread().getId();
        svLogger.info("threadId: " + NoInterfaceBean3.beanThreadId);

        // set static variable for work completed to true
        NoInterfaceBean3.asyncWorkDone = true;

        svBeanLatch.countDown();

        return;
    }

    public void methodWithNotSupportedTransactionDefinedViaXML() {
        // This method exists to verify that we respect method-level transactional settings, configured via XML, for the NoInterface view.
        // We do not expect to ever actually be allowed to execute this method.  If we actually get into this code, then something went wrong.
        //
        // We do not throw an exception from this method because the point of the test is to verify that the container itself through an exception
        // before we ever got into this method...and so if we throw one here, then it makes it harder to determine if we passed or failed (we'd have to determine
        // which exception we got back, and based on that determine who threw it), and so we just consider an exception coming back from this method as a *passing*
        // result, and no exception as a *failing* result.
        svLogger.info("Executing NoInterfaceBean3.methodWithNotSupportedTransactionDefinedViaXML....which means we did NOT respect the transactional configuration.");
    }

    public NoInterfaceBean3() {
    }
}