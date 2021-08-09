/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package app.timeout;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

@Dependent
@Named("TranTimeoutCleanupReader")
public class TranTimeoutCleanupReader extends AbstractItemReader {

    private final static Logger logger = Logger.getLogger("test");

    Integer toRead = 0;

    @BatchProperty(name = "total")
    String totalStr = "10";

    int total = 0;

    @PostConstruct
    public void setup() {
        total = Integer.parseInt(totalStr);
        logger.fine("In @PostConstruct, total = " + total);
    }

    @Override
    public void open(Serializable checkpoint) throws Exception {

        logger.info("In TranTimeoutCleanupReader, thread = " + Thread.currentThread());

        if (checkpoint != null) {
            toRead = (Integer) checkpoint;
        }
        logger.fine("In open(), start at #" + toRead);
    }

    @Override
    public Object readItem() throws Exception {
        logger.finer("readItem: " + toRead);
        if (toRead >= total) {
            logger.fine("Ending step after reading " + total + " items total.");
            return null;
        } else {
            return toRead++;
        }
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return toRead;
    }

}
