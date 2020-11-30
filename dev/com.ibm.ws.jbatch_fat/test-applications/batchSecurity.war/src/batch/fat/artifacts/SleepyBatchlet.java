/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.artifacts;

import java.util.logging.Logger;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.inject.Inject;

@javax.inject.Named("sleepyBatchlet")
public class SleepyBatchlet extends AbstractBatchlet {

    private final static Logger logger = Logger.getLogger(SleepyBatchlet.class.getName());

    private volatile boolean stopRequested = false;

    @Inject
    @BatchProperty(name = "sleep.time.seconds")
    String sleepTimeSeconds;
    private int sleep_time_seconds = 5; //default is 5 seconds

    /**
     * Main entry point.
     */
    @Override
    public String process() throws Exception {

        logger.fine("process: entry");

        if (sleepTimeSeconds != null) {
            sleep_time_seconds = Integer.parseInt(sleepTimeSeconds);
            logger.fine("process: sleep for: " + sleepTimeSeconds);
        }

        int i;
        for (i = 0; i < sleep_time_seconds && !stopRequested; ++i) {
            logger.fine("process: [" + i + "] sleeping for a second...");
            Thread.sleep(1 * 1000);
        }

        String exitStatus = "SleepyBatchlet:i=" + i + ";stopRequested=" + stopRequested;
        logger.fine("process: exitStatus: " + exitStatus);

        return exitStatus;
    }

    /**
     * Called if the batchlet is stopped by the container.
     */
    @Override
    public void stop() throws Exception {
        logger.fine("stop:");
        stopRequested = true;
    }

}
