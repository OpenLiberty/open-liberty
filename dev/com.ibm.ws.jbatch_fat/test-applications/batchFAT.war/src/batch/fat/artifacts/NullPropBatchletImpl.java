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

import java.util.logging.Logger;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.inject.Inject;

/**
 *
 */
public class NullPropBatchletImpl extends AbstractBatchlet {

    private final static Logger logger = Logger.getLogger(NullPropBatchletImpl.class.getName());

    private volatile static int count = 1;

    public static String GOOD_EXIT_STATUS = "VERY GOOD INVOCATION";

    @Inject
    @BatchProperty
    public String sleepTime;
    int sleepVal = 0;

    @Inject
    @BatchProperty
    public String forceFailure = "false";
    Boolean fail;

    private void init() {
        try {
            fail = Boolean.parseBoolean(forceFailure);
        } catch (Exception e) {
            fail = false;
        }
        try {
            sleepVal = Integer.parseInt(sleepTime);
        } catch (Exception e) {
            sleepVal = 0;
        }
    }

    @Override
    public String process() throws Exception {
        init();
        if (fail) {
            throw new IllegalArgumentException("Forcing failure");
        }
        if (sleepTime != null) {
            Thread.sleep(sleepVal);
        }
        logger.fine("Running batchlet process(): " + count);
        count++;
        return GOOD_EXIT_STATUS;
    }

}
