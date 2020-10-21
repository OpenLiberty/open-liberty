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

import javax.batch.api.AbstractBatchlet;

public class MyBatchlet extends AbstractBatchlet {

    @Override
    public String process() throws Exception {
        // In case we want to check at step level.
        return "DONE";
    }

}
