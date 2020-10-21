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
package batch.fat.artifacts;

import javax.batch.api.listener.AbstractJobListener;

/**
 *
 */
public class EndOfJobNotificationListener extends AbstractJobListener {

    /**
     * Notifies this class object so that anyone
     * can take this into consideration with polling (e.g. only start polling when
     * notified of this).
     * 
     * IMPORTANT: This is only going to work with someone with access to this classloader!
     * E.g. the FAT client couldn't do this but a servlet could potentially.
     */
    @Override
    public void afterJob() throws Exception {

        Object lock = EndOfJobNotificationListener.class;
        synchronized (lock) {
            lock.notifyAll();
        }
    }

}
