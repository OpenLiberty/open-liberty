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
package io.openliberty.microprofile.faulttolerance30.internal.test.context;

import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

/**
 * Implementation of a test context which can be propagated
 */
public class TestContextProvider implements ThreadContextProvider {

    public static final String ID = "TEST_CONTEXT";
    private static final ThreadLocal<String> contextValue = new ThreadLocal<>();
    private static final TestContextSnapshot CLEARED = new TestContextSnapshot(null);

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> arg0) {
        return CLEARED;
    }

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> arg0) {
        return new TestContextSnapshot(contextValue.get());
    }

    @Override
    public String getThreadContextType() {
        return ID;
    }

    public static void setValue(String value) {
        contextValue.set(value);
    }

    public static String getValue() {
        return contextValue.get();
    }

    private static class TestContextSnapshot implements ThreadContextSnapshot {

        private final String value;

        public TestContextSnapshot(String value) {
            this.value = value;
        }

        @Override
        public ThreadContextController begin() {
            String oldValue = contextValue.get();
            contextValue.set(value);
            return new TestContextController(oldValue);
        }
    }

    private static class TestContextController implements ThreadContextController {

        private final String oldValue;

        public TestContextController(String oldValue) {
            this.oldValue = oldValue;
        }

        @Override
        public void endContext() throws IllegalStateException {
            contextValue.set(oldValue);
        }

    }

}
