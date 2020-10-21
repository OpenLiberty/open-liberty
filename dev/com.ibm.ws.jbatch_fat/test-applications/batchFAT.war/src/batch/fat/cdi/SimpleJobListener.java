/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package batch.fat.cdi;

import javax.batch.api.listener.JobListener;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@Dependent
public class SimpleJobListener implements JobListener {

    @Inject
    private SimpleJobLogger logger;

    @Override
    public void beforeJob() throws Exception {
        logger.log("before job");
    }

    @Override
    public void afterJob() throws Exception {
        logger.log("after job");
    }

}
