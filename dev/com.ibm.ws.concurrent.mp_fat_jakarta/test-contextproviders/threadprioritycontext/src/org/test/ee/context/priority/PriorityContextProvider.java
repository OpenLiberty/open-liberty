/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package org.test.ee.context.priority;

import java.util.Map;

import jakarta.enterprise.concurrent.spi.ThreadContextProvider;
import jakarta.enterprise.concurrent.spi.ThreadContextSnapshot;

/**
 * Example third-party thread context provider, to be used for testing purposes.
 * This context propagates the current thread priority to the task or action.
 */
public class PriorityContextProvider implements ThreadContextProvider {
    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return new PriorityContextSnapshot(Thread.NORM_PRIORITY);
    }

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        return new PriorityContextSnapshot(Thread.currentThread().getPriority());
    }

    @Override
    public String getThreadContextType() {
        return "Priority";
    }
}
