/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
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
