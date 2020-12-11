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
package com.ibm.websphere.samples.batch.artifacts;

import java.io.BufferedReader;
import java.io.Serializable;
import java.util.logging.Logger;

import javax.batch.api.chunk.ItemReader;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import com.ibm.websphere.samples.batch.beans.AccountDataObject;
import com.ibm.websphere.samples.batch.util.BonusPayoutConstants;
import com.ibm.websphere.samples.batch.util.BonusPayoutUtils;

@Named("GeneratedCSVReader")
public class GeneratedCSVReader implements ItemReader, BonusPayoutConstants {

    private final static Logger logger = Logger.getLogger(BONUSPAYOUT_LOGGER);

    @Inject
    private JobContext jobCtx;

    // Next line to read, 0-indexed.
    int recordNumber = 0;

    private BufferedReader reader = null;

    @Override
    public Object readItem() throws Exception {
        String line = reader.readLine();
        if (line == null) {
            logger.fine("End of stream reached in " + this.getClass());
            return null;
        } else {
            AccountDataObject acct = BonusPayoutUtils.parseLine(line);
            recordNumber++;
            return acct;
        }
    }

    @Override
    public void open(Serializable checkpoint) throws Exception {

        if (checkpoint != null) {
            recordNumber = (Integer) checkpoint;
        }

        reader = new BonusPayoutUtils(jobCtx).openCurrentInstanceStreamReader();

        // Advance cursor (not worrying  much about performance)
        for (int i = 0; i < recordNumber; i++) {
            reader.readLine();
        }
    }

    @Override
    public void close() throws Exception {
        reader.close();
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return recordNumber;
    }
}
