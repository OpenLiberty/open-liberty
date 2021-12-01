/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.context.timing;

import java.util.Map;

import jakarta.enterprise.concurrent.spi.ThreadContextProvider;
import jakarta.enterprise.concurrent.spi.ThreadContextSnapshot;

/**
 * Example third-party thread context provider, to be used for testing purposes.
 * This context associates a timestamp with a thread.
 */
public class TimestampContextProvider implements ThreadContextProvider {
    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return new TimestampContextSnapshot(null);
    }

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        return new TimestampContextSnapshot(Timestamp.local.get());
    }

    @Override
    public String getThreadContextType() {
        return Timestamp.CONTEXT_NAME;
    }
}
