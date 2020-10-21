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
package com.ibm.websphere.samples.batch.artifacts;

import java.util.List;

import javax.batch.api.chunk.AbstractItemWriter;
import javax.inject.Named;

@Named("NoOpWriter")
public class NoOpWriter extends AbstractItemWriter {

    @Override
    public void writeItems(List<Object> items) throws Exception {
        // Do nothing
    }

}
