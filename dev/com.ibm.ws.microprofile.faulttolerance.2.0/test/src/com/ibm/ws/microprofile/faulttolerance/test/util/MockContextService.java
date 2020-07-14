/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.test.util;

import java.util.concurrent.Callable;

import com.ibm.ws.microprofile.faulttolerance.spi.context.ContextService;
import com.ibm.ws.microprofile.faulttolerance.spi.context.ContextSnapshot;

public class MockContextService implements ContextService {

    public int activateContextCount = 0;
    public int deactivateContextCount = 0;

    @Override
    public ContextSnapshot capture() {
        return new MockSnapshot();
    }

    public class MockSnapshot implements ContextSnapshot {

        @Override
        public void runWithContext(Runnable runnable) {
            try {
                activateContextCount++;
                runnable.run();
            } finally {
                deactivateContextCount++;
            }
        }

        @Override
        public <V> V runWithContext(Callable<V> callable) throws Exception {
            try {
                activateContextCount++;
                return callable.call();
            } finally {
                deactivateContextCount++;
            }
        }

    }

    public int getActivateContextCount() {
        return activateContextCount;
    }

    public int getDeactivateContextCount() {
        return deactivateContextCount;
    }

}
