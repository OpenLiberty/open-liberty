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

import java.util.logging.Logger;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

@Dependent
@Named("TranTimeoutCleanupBatchlet")
public class SleepyTranBatchlet extends AbstractBatchlet {

    private final static Logger logger = Logger.getLogger("test");

    @Inject
    @BatchProperty
    String sleepTimeSeconds;

    @Inject
    TransactionalSleeper sleeper;

    @Override
    public String process() {

        logger.info("In SleepyTranBatchlet, thread = " + Thread.currentThread());

        int sleepTimeSecondsInt = Integer.parseInt(sleepTimeSeconds);

        sleeper.logTranStatus();
        sleeper.sleepyTran(sleepTimeSecondsInt);
        sleeper.logTranStatus();

        return null;
    }

}
