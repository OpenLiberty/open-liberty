/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.spi;

public enum RetryResultCategory {
    NO_EXCEPTION("No exception thrown"),
    EXCEPTION_NOT_IN_RETRY_ON("Exception thrown does not match retryOn condition"),
    EXCEPTION_IN_RETRY_ON("Exception thrown matches retryOn condition"),
    EXCEPTION_IN_ABORT_ON("Exception thrown matches abortOn condition"),
    MAX_RETRIES_REACHED("Max retries reached"),
    MAX_DURATION_REACHED("Max duration reached"),
    NO_RETRY("Retry not used");

    private final String reasonString;

    private RetryResultCategory(String reasonString) {
        this.reasonString = reasonString;
    }

    @Override
    public String toString() {
        return reasonString;
    }
}