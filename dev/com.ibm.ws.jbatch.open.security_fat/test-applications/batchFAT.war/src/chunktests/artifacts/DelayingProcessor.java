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

import java.util.logging.Logger;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemProcessor;
import javax.inject.Inject;

/**
 *
 */
public class DelayingProcessor implements ItemProcessor {

    private final static Logger logger = Logger.getLogger("com.ibm.ws.jbatch.open_fat");

    @Inject
    @BatchProperty(name = "delay")
    String delayInSeconds = "0";

    Long initialTime = null;

    @Override
    public Object processItem(Object item) throws Exception {

        logger.fine("Entering processItem, delay in SECONDS = " + delayInSeconds + "; item = " + item);

        if (initialTime == null) {
            initialTime = System.currentTimeMillis();
        }

        long elapsed = 0L;
        long delayUntilMillis = 0L;
        do {
            logger.fine("Sleeping in processor");
            Thread.sleep(1800);
            Long currentTime = System.currentTimeMillis();
            elapsed = currentTime - initialTime;
            delayUntilMillis = 1000 * Long.parseLong(delayInSeconds);
        } while (delayUntilMillis > elapsed);

        return item;
    }
}
