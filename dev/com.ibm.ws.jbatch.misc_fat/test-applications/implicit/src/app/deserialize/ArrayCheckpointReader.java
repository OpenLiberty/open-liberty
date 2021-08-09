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
package app.deserialize;

import java.io.Serializable;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

@Dependent
@Named("ArrayCheckpointReader")
public class ArrayCheckpointReader extends AbstractItemReader {

    private final static Logger logger = Logger.getLogger("test");

    Integer[] toRead = new Integer[2];

    int itemsRead = 0;
    int failOn = -1;

    @Inject
    private JobContext jobCtx;

    @Inject
    private StepContext stepCtx;

    @Inject
    @BatchProperty(name = "startAtIndex")
    protected String startAtIndexStr;

    @Inject
    @BatchProperty(name = "maxRecordsToValidate")
    protected String maxRecordsToValidateStr;

    private Properties jobProps;

    @Override
    public void open(Serializable checkpoint) throws Exception {

        logger.info("In ArrayCheckpointReader, thread = " + Thread.currentThread());

        jobProps = jobCtx.getProperties();
        if (jobProps.get("forceFailure") != null) {
            failOn = Integer.parseInt(jobProps.getProperty("forceFailure"));
        }

        if ("true".equals(jobProps.get("userDataTest"))) {
            Integer[] ud = (Integer[]) stepCtx.getPersistentUserData();
            if (ud == null) {
                ud = new Integer[ReaderData.outerLoop.length];
                stepCtx.setPersistentUserData(ud);
            }
        }

        if (checkpoint != null) {
            toRead = (Integer[]) checkpoint;
        } else if (startAtIndexStr != null) {
            toRead[0] = Integer.parseInt(startAtIndexStr);
            toRead[1] = 0;
        } else {
            toRead[0] = 0;
            toRead[1] = 0;
        }

        logger.fine("In open(), start at " + toRead[0] + "," + toRead[1]);
    }

    @Override
    public Object readItem() throws Exception {
        if (maxRecordsToValidateStr != null && itemsRead >= Integer.parseInt(maxRecordsToValidateStr)) {
            completeStep();
            return null;
        }

        logger.finer("readItem: " + toRead);

        if (advanceReader()) {
            if ((failOn == itemsRead) && jobProps.get("restartCount") == null) {
                String msg = "Forcing failure in ArrayCheckpointReader, toRead = " + toRead[0] + "," + toRead[1];
                throw new IllegalStateException(msg);
            }

            if ("true".equals(jobProps.get("userDataTest"))) {
                Integer[] ud = (Integer[]) stepCtx.getPersistentUserData();
                ud[toRead[0]] = toRead[1] + 1;
                stepCtx.setPersistentUserData(ud);
            }

            return ReaderData.outerLoop[toRead[0]] * ReaderData.innerLoop[toRead[1]];
        } else {
            logger.fine("Ending step, reached end of input arrays.");
            completeStep();
            return null;
        }
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return toRead;
    }

    private boolean advanceReader() {
        toRead[1] = toRead[1] + 1;

        if (toRead[1] >= ReaderData.innerLoop.length) {

            toRead[1] = 0;
            toRead[0] = toRead[0] + 1;

            if (toRead[0] >= ReaderData.outerLoop.length) {
                // Reached end of both arrays
                return false;
            }
        }

        itemsRead++;
        return true;
    }

    private void completeStep() {
        stepCtx.setExitStatus(Integer.toString(itemsRead));
    }

}
