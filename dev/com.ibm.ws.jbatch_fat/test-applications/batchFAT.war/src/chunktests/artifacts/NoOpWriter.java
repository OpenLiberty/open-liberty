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

import java.io.Serializable;
import java.util.List;

import javax.batch.api.chunk.ItemWriter;

/**
 *
 */
public class NoOpWriter implements ItemWriter {

    /* (non-Javadoc)
     * @see javax.batch.api.chunk.ItemWriter#open(java.io.Serializable)
     */
    @Override
    public void open(Serializable checkpoint) throws Exception {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see javax.batch.api.chunk.ItemWriter#close()
     */
    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see javax.batch.api.chunk.ItemWriter#writeItems(java.util.List)
     */
    @Override
    public void writeItems(List<Object> items) throws Exception {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see javax.batch.api.chunk.ItemWriter#checkpointInfo()
     */
    @Override
    public Serializable checkpointInfo() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

}
