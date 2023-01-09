/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package test.context.timing;

import jakarta.enterprise.concurrent.spi.ThreadContextRestorer;

/**
 * Example third-party thread context provider, to be used for testing purposes.
 * This context associates a timestamp with a thread.
 */
public class TimestampContextRestorer implements ThreadContextRestorer {
    private boolean restored;
    private final Long timestamp;

    TimestampContextRestorer(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public void endContext() {
        if (restored)
            throw new IllegalStateException("thread context was already restored");
        if (timestamp == null)
            Timestamp.local.remove();
        else
            Timestamp.local.set(timestamp);
        restored = true;
    }

    @Override
    public String toString() {
        return "TimestampContextRestorer@" + Integer.toHexString(hashCode()) + ":" + timestamp;
    }
}
