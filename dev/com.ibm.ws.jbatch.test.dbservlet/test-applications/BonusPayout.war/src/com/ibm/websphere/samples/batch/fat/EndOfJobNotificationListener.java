/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.websphere.samples.batch.fat;

import javax.batch.api.listener.AbstractJobListener;
import javax.inject.Named;

/**
 *
 */
@Named("EndOfJobNotificationListener")
public class EndOfJobNotificationListener extends AbstractJobListener {

    /*
     * (non-Javadoc)
     * 
     * @see javax.batch.api.listener.JobListener#afterJob()
     */
    @Override
    public void afterJob() throws Exception {

        // OK to start checking that we're done now
        Object lock = EndOfJobNotificationListener.class;
        synchronized (lock) {
            lock.notifyAll();
        }
    }

}
