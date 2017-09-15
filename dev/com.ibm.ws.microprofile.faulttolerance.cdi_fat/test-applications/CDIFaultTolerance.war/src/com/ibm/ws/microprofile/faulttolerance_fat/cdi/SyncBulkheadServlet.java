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
package com.ibm.ws.microprofile.faulttolerance_fat.cdi;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.AsyncRunnerBean;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.BulkheadBean;

import componenttest.app.FATServlet;

/**
 * Servlet implementation class Test
 */
@WebServlet("/bulkhead")
public class SyncBulkheadServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    BulkheadBean bean1;

    @Inject
    AsyncRunnerBean runner;

    public void testSyncBulkheadSmall(HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {
        //connectA has a poolSize of 2
        //first two should be run straight away, in parallel, each around 5 seconds
        Future<Boolean> future1 = runner.call(() -> {
            return bean1.connectA("One");
        });
        //These sleep statements are fine tuning to ensure this test functions.
        //The increments are small enough that it shuld not impact the logic of this test.
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);
        Future<Boolean> future2 = runner.call(() -> {
            return bean1.connectA("Two");
        });
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);

        //next two should wait until the others have finished
        Future<Boolean> future3 = runner.call(() -> {
            return bean1.connectA("Three");
        });
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);
        Future<Boolean> future4 = runner.call(() -> {
            return bean1.connectA("Four");
        });
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);

        //total time should be just over 10s
        Thread.sleep((TestConstants.WORK_TIME * 2) + TestConstants.TEST_TIME_UNIT);

        if (!future1.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS)) {
            throw new AssertionError("Future1 did not complete properly");
        }
        if (!future2.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS)) {
            throw new AssertionError("Future2 did not complete properly");
        }
        if (!future3.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS)) {
            throw new AssertionError("Future3 did not complete properly");
        }
        if (!future4.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS)) {
            throw new AssertionError("Future4 did not complete properly");
        }

    }
}
