/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.persistent.fat.tests;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides common logic for persistent timer tests.
 */
public class PersistentTimerTestHelper {

    /**
     * Combines test specific expected failures with intermittent failures that
     * may occur in any persistent timer test.
     */
    public static String[] expectedFailures(String... expectedFailuresRegExps) {
        List<String> ignoreList = new ArrayList<String>();

        for (String failures : expectedFailuresRegExps) {
            ignoreList.add(failures);
        }

        // APPLICATION_SLOW_STARTUP=CWWKZ0022W: Application {0} has not started in {1} seconds.
        ignoreList.add("CWWKZ0022W");

        // CWWKC1556.task.exec.deferred=CWWKC1556W: Execution of tasks from application {0} is deferred until
        // the application and modules that scheduled the tasks are available.
        ignoreList.add("CWWKC1556W");

        // CNTR0092W: Attempted to access EnterpriseBean
        // PersistentTimerTestApp#PersistentTimerTestEJB.jar#NotSupportedTranBean, that has not been started.
        //
        // persistent.internal.InvokerTask run starts for a persistent timer during server shutdown,
        // PersistentTimerTaskHandler tries to access bean properties to run timeout but bean is gone.
        ignoreList.add("CNTR0092W");

        // DSRA0304E:  XAException occurred. XAException contents and details are:
        // DSRA0302E:  XAException occurred.  Error code is: XA_RBROLLBACK (100).  Exception is: null
        //
        // persistent.internal.InvokerTask run starts for a persistent timer during server shutdown,
        // but datasource has already been shutdown; can occur without any timers, just by database poll.
        ignoreList.add("DSRA0304E");
        ignoreList.add("DSRA0302E.*XA_RBROLLBACK");

        // J2CA0027E: An exception occurred while invoking end on an XA Resource Adapter from DataSource
        //            dataSource[DefaultDataSource], within transaction ID {XidImpl: formatId(57415344),
        //            gtrid_length(36), bqual_length(54),
        //
        // persistent.internal.InvokerTask run starts for a persistent timer during server shutdown,
        // but transaction service has already been shutdown.
        ignoreList.add("J2CA0027E");

        // CWWKC1503W: Persistent executor [EJBPersistentTimerExecutor] rolled back task [task id]
        //             (!EJBTimerP![j2eename]) due to failure javax.ejb.EJBException: Timeout method
        //             [method name] will not be invoked because server is stopping
        //
        // persistent.internal.InvokerTask run starts for a persistent timer during server shutdown,
        // but EJB timer service throws exception due to server stopping.
        ignoreList.add("CWWKC1503W.*server is stopping");

        String[] stringArr = new String[ignoreList.size()];
        return ignoreList.toArray(stringArr);
    }

}
