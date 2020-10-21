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
package chunktests.artifacts;

import java.util.logging.Logger;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemProcessor;
import javax.inject.Inject;

/**
 *
 */
public class DelayingProcessor implements ItemProcessor {

    private final static Logger logger = Logger.getLogger("com.ibm.ws.jbatch_fat");

    @Inject
    @BatchProperty(name = "delay")
    String delayInSeconds = "0";

    Long initialTime = null;

    @Override
    public Object processItem(Object item) throws Exception {

        logger.fine("Entering processItem, delay in SECONDS = " + delayInSeconds + "; item = " + item);

        if (initialTime == null) {
            initialTime = System.currentTimeMillis();
        }

        long elapsed = 0L;
        long delayUntilMillis = 0L;
        do {
            logger.fine("Sleeping in processor");
            Thread.sleep(1800);
            Long currentTime = System.currentTimeMillis();
            elapsed = currentTime - initialTime;
            delayUntilMillis = 1000 * Long.parseLong(delayInSeconds);
        } while (delayUntilMillis > elapsed);

        return item;
    }
}
