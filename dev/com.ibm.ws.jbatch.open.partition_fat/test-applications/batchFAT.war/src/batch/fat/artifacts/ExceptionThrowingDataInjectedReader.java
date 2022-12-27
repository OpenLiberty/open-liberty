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
package batch.fat.artifacts;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import batch.fat.common.ForceRollbackException;
import batch.fat.util.StringUtils;

public class ExceptionThrowingDataInjectedReader extends AbstractItemReader {

    private final static Logger logger = Logger.getLogger("com.ibm.ws.jbatch.open_fat");

    Integer nextToRead;
    List<String> originalReadData;

    @BatchProperty
    String readData = null;

    @BatchProperty
    String excClass = null;

    @BatchProperty(name = "throwOn")
    String throwOnStr = null;

    @BatchProperty
    String throwExcOnReaderClose = null;

    @Inject
    JobContext jobCtx = null;

    @Inject
    StepContext stepCtx = null;

    @Override
    public Object readItem() throws Exception {

        if (nextToRead >= originalReadData.size()) {
            logger.logp(Level.FINE, this.getClass().getCanonicalName(), "readItem", "At end of input array, nextToRead = " + nextToRead + ".  Ending chunk loop");
            return null;
        }

        String val = originalReadData.get(nextToRead++);

        logger.logp(Level.FINE, this.getClass().getCanonicalName(), "readItem", "nextToRead = " + nextToRead);

        // We only want to throw an exception here once to allow retry to complete.
        if (throwOn(nextToRead)) {
            if (exceptionThrowingEnabled()) {
                // Disable throw on retry.  Store state in context, not reader.
                stepCtx.setTransientUserData(Boolean.FALSE);
                // TODO - Add ability to throw another class for retry w/o rollback
                throw new ForceRollbackException("On readItem nextToRead = " + nextToRead);
            }
        }

        // Prepend exec id for uniqueness across test variations

        String retVal = Long.toString(jobCtx.getExecutionId()) + val;
        logger.fine("Returning: " + retVal);
        return retVal;
    }

    private boolean throwOn(int nextToRead) {
        Set<Integer> throwOnSet = StringUtils.splitToIntegerSet(throwOnStr);
        return throwOnSet.contains(nextToRead);
    }

    /**
     * @return 'false' if user data = Boolean.FALSE, otherwise 'true'
     */
    protected boolean exceptionThrowingEnabled() {
        Object userData = stepCtx.getTransientUserData();
        if (userData != null && userData.equals(Boolean.FALSE)) {
            logger.fine("Exceptions disabled");
            return false;
        } else {
            logger.fine("Exceptions enabled");
            return true;
        }
    }

    protected void disableExceptionThrowing() {
        stepCtx.setTransientUserData(Boolean.TRUE);
    }

    @Override
    public void open(Serializable checkpoint) throws Exception {
        if (checkpoint != null) {
            nextToRead = (Integer) checkpoint;
        } else {
            nextToRead = 0;
        }

        originalReadData = StringUtils.split(readData, ",");
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return nextToRead;
    }

    @Override
    public void close() throws Exception {
        boolean toThrow = Boolean.parseBoolean(throwExcOnReaderClose);
        if (toThrow) {
            throw new ForceRollbackException("On close()");
        } else {
            logger.fine("Not configured to throw: " + throwExcOnReaderClose);
        }
    }

}