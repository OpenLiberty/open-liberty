/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package app.timeout;

import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import fat.util.JobWaiter;

/**
 * This is complicated since we don't have an easy way to control the thread pool algorithm (and don't want to create one).
 * Much of the results for an individual run will be based on "luck".
 * There are two real disconnects here:
 *
 * First, the fact that a test passing doesn't prove a relevant thread is definitely being cleaned up. We may just be getting lucky/unlucky.
 *
 * Second, there is the fact that a failure in one test can be a side effect of an earlier test rather than the function being tested.
 * E.g, if a partition thread were to dirty the thread, but this wasn't noticed until the split-flow test, later, then the failure would
 * be tricky to trace back to the root cause.
 *
 * We accept this with that the thought that if these tests are run in a continuous test environment for long enough then all relevant problems will be detected.
 *
 * It's better then, to introduce the possibility of "side effects" in later tests since that amounts to more coverage of what was being tested in the earlier test.
 *
 * Another note: this is only a FULL mode test. We want some coverage of this going forwards, but for now, we think we have solved the problem, and don't want to
 * chew up this test time on every run.
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TranTimeoutCleanupServlet")
@Mode(TestMode.FULL)
public class TranTimeoutCleanupServlet extends FATServlet {

    public static Logger logger = Logger.getLogger("test");

    @Test
    public void testTranTimeoutCleanupJobs() throws Exception {
        int numIterations = 8;

        logger.fine("Running test = testTranTimeoutCleanupJobs");
        for (int i = 0; i < numIterations; i++) {
            logger.info("'Before' Job, iteration #" + i);
            new JobWaiter().completeNewJob("TranTimeoutCleanupBefore", null);
            logger.info("'After' Job, iteration #" + i);
            new JobWaiter().completeNewJob("TranTimeoutCleanupAfter", null);
        }
    }

    @Test
    public void testTranTimeoutCleanupPartitions() throws Exception {
        int numIterations = 3;

        logger.fine("Running test = testTranTimeoutCleanupPartitions");
        for (int i = 0; i < numIterations; i++) {
            logger.info("'Before' Job, iteration #" + i);
            new JobWaiter().completeNewJob("TranTimeoutCleanupBeforePartition", null);
            logger.info("'After' Job, iteration #" + i);
            new JobWaiter().completeNewJob("TranTimeoutCleanupAfter", null);
        }
    }

    @Test
    public void testTranTimeoutCleanupSplitFlows() throws Exception {

        int numIterations = 8;

        logger.fine("Running test = testTranTimeoutCleanupSplitFlows");
        for (int i = 0; i < numIterations; i++) {
            logger.info("'Before' Job, iteration #" + i);
            new JobWaiter().completeNewJob("TranTimeoutCleanupBeforeSplitFlow", null);
            logger.info("'After' Job, iteration #" + i);
            new JobWaiter().completeNewJob("TranTimeoutCleanupAfter", null);
        }
    }

}
