/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.state.impl;

import java.util.concurrent.TimeUnit;

import com.ibm.ws.microprofile.faulttolerance.spi.RetryResultCategory;
import com.ibm.ws.microprofile.faulttolerance20.impl.MethodResult;
import com.ibm.ws.microprofile.faulttolerance20.state.RetryState;

public class RetryStateNullImpl implements RetryState {

    // Null impl always returns shouldRetry() == false, so we have a constant result
    public static final RetryResult RESULT = new RetryResult() {

        @Override
        public boolean shouldRetry() {
            return false;
        }

        @Override
        public TimeUnit getDelayUnit() {
            return TimeUnit.MILLISECONDS;
        }

        @Override
        public long getDelay() {
            return 0;
        }

        @Override
        public RetryResultCategory getCategory() {
            return RetryResultCategory.NO_RETRY;
        }

        @Override
        public String toString() {
            return "@Retry annotation not used";
        }
    };

    @Override
    public void start() {
    }

    @Override
    public RetryResult recordResult(MethodResult<?> result) {
        return RESULT;
    }

}
