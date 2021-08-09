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
package com.ibm.websphere.samples.batch.fat;

import javax.batch.api.listener.AbstractJobListener;
import javax.inject.Named;

/**
 *
 */
@Named("EndOfJobNotificationListener")
public class EndOfJobNotificationListener extends AbstractJobListener {

    /*
     * (non-Javadoc)
     * 
     * @see javax.batch.api.listener.JobListener#afterJob()
     */
    @Override
    public void afterJob() throws Exception {

        // OK to start checking that we're done now
        Object lock = EndOfJobNotificationListener.class;
        synchronized (lock) {
            lock.notifyAll();
        }
    }

}
