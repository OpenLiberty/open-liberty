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

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemReader;

/**
 *
 */
public class SimpleIntegerItemReader extends AbstractItemReader {

    @BatchProperty
    String numToRead;

    int i = 0;

    @Override
    public Object readItem() {
        if (i++ <= Integer.parseInt(numToRead) - 1) {
            return i;
        } else {
            return null;
        }
    }
}
