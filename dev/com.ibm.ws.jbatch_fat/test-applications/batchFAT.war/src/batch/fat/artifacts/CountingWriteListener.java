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

import java.util.List;

import javax.batch.api.chunk.listener.AbstractItemWriteListener;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

/**
 * Appends # of items written to step exit status.
 * Results in comma-separated sequence.
 */
public class CountingWriteListener extends AbstractItemWriteListener {

    @Inject
    StepContext stepCtx;

    @Override
    public void afterWrite(List<Object> items) throws Exception {
        String es = stepCtx.getExitStatus();
        String chunkSize = Integer.toString(items.size());

        if (es == null) {
            stepCtx.setExitStatus(chunkSize);
        } else {
            stepCtx.setExitStatus(es + "," + chunkSize);
        }
    }
}
