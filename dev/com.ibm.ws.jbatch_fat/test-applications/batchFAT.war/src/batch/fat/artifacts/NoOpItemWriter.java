/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package batch.fat.artifacts;

import java.util.List;

import javax.batch.api.chunk.AbstractItemWriter;

public class NoOpItemWriter extends AbstractItemWriter {

    @Override
    public void writeItems(List<Object> items) throws Exception {}

}
