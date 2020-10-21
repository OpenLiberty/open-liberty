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

import javax.batch.runtime.context.JobContext;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

//
// This scope might not be the most useful for testing (compared to @Dependent).
// But this is what the customer for PI78436 used, so let's verify it.
// Refrain from testing the 'count', which won't scale easily to >1 test.
// 
@ApplicationScoped 
public class SimpleJobLogger {

    @Inject
    private JobContext jobContext;

    private int count = 0;

    public void log(String message) {
        System.out.println(hashCode() + "|" + jobContext.getJobName() + "[" + jobContext.getExecutionId() + "]: " + message);
        System.out.println("count=" + ++count);
    }

}
