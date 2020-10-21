/*
 * Copyright 2012 International Business Machines Corp.
 * 
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
