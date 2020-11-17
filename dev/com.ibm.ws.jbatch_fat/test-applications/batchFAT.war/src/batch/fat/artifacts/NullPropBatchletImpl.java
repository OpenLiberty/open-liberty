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

import java.util.logging.Logger;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.inject.Inject;

/**
 *
 */
public class NullPropBatchletImpl extends AbstractBatchlet {

    private final static Logger logger = Logger.getLogger(NullPropBatchletImpl.class.getName());

    private volatile static int count = 1;

    public static String GOOD_EXIT_STATUS = "VERY GOOD INVOCATION";

    @Inject
    @BatchProperty
    public String sleepTime;
    int sleepVal = 0;

    @Inject
    @BatchProperty
    public String forceFailure = "false";
    Boolean fail;

    private void init() {
        try {
            fail = Boolean.parseBoolean(forceFailure);
        } catch (Exception e) {
            fail = false;
        }
        try {
            sleepVal = Integer.parseInt(sleepTime);
        } catch (Exception e) {
            sleepVal = 0;
        }
    }

    @Override
    public String process() throws Exception {
        init();
        if (fail) {
            throw new IllegalArgumentException("Forcing failure");
        }
        if (sleepTime != null) {
            Thread.sleep(sleepVal);
        }
        logger.fine("Running batchlet process(): " + count);
        count++;
        return GOOD_EXIT_STATUS;
    }

}
