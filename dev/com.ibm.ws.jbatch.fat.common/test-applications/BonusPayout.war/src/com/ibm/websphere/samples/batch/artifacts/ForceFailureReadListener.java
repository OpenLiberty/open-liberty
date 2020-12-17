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

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.listener.AbstractItemReadListener;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import com.ibm.websphere.samples.batch.beans.AccountDataObject;

/**
 * Designed to force failure after the Nth (0-indexed) record is read (either for the
 * step overall or on this partition).
 * 
 * In order to allow for an implementation to pool the listener artifact, however,
 * the count is performed relative to the first account number number read, rather
 * than kept as state in the form of a field of this class itself.
 */
@Named("ForceFailureReadListener")
public class ForceFailureReadListener extends AbstractItemReadListener {

    @Inject
    @BatchProperty(name = "forceFailure")
    private String failOnStr;

    // Helps initialize, toggle on/off, and cache the forced failure config.
    private ForcedFailureConfig failureConfig;

    @Override
    public void afterRead(Object item) throws Exception {

        if (item == null) {
            return; // End of step
        }

        if (failureConfig == null) {
            failureConfig = new ForcedFailureConfig((AccountDataObject) item);
        }

        failureConfig.possiblyFailOn(item);
    }

    private class ForcedFailureConfig {

        public void possiblyFailOn(Object item) {
            if (!failEnabled) {
                return;
            }

            int currentRecordNumber = ((AccountDataObject) item).getAccountNumber();

            if (currentRecordNumber - firstRecordNumber >= failOn) {
                String excMessage = "Forcing failure.  The failOn property = " + failOn +
                                    ", with the first record number = " + firstRecordNumber +
                                    ", and the current record number = " + currentRecordNumber;
                throw new IllegalStateException(excMessage);
            }
        }

        boolean failEnabled = false;
        int failOn = 0;
        int firstRecordNumber = 0;

        private ForcedFailureConfig(AccountDataObject item) {
            if (failOnStr != null && !failOnStr.isEmpty()) {
                failEnabled = true;
                failOn = Integer.parseInt(failOnStr);
                firstRecordNumber = item.getAccountNumber();
            }
        }
    }

}
