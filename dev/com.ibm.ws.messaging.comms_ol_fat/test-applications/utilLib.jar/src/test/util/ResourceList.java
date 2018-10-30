/* =============================================================================
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package test.util;

import java.util.Arrays;

/**
 * Allows AutoCloseable resources to be collected and subsequently closed (using try-with-resources mechanics)
 * when the ResourceList is closed (also potentially using try-with-resources).
 *
 * Resources will be closed in the opposite order to their insertion order.
 *
 * Not thread-safe.
 *
 * @author Neil Richards
 */
public final class ResourceList implements AutoCloseable {
    private static final String NAME = ResourceList.class.getName();
    private static final String INNER_NAME = ResourceHandler.class.getName();

    private static final class ResourceHandler implements AutoCloseable {
        private final AutoCloseable resource;
        private final StackTraceElement[] stack;
        private final ResourceHandler next;

        ResourceHandler(AutoCloseable resource, StackTraceElement[] stack, ResourceHandler next) {
            this.resource = resource;
            this.stack = stack;
            this.next = next;
        }

        @Override
        public void close() throws Exception {
            try (AutoCloseable ac = next) { // recurses using try-with-resources on the next ResourceHandler
                if (resource == null) return;
                try {
                    resource.close();
                } catch (Exception e) {
                    Exception e2 = new Exception("See causal exception, thrown when closing resource added at following call stack:", e);
                    e2.setStackTrace(stack);
                    throw e2;
                }
            }
        }
    }

    private ResourceHandler top = null;

    public void add(AutoCloseable... newResources) {
        final StackTraceElement[] stack = getStack();
        for (AutoCloseable resource: newResources) {
            top = new ResourceHandler(resource, stack, top);
        }
    }

    @Override
    public void close() throws Exception {
        try (AutoCloseable ac = top) { //try-with-resources on the topmost ResourceHandler
            nop();
        } catch (Exception e) {
            filterStacks(e);
            throw e;
        }
    }

    private static void nop() {}

    private static StackTraceElement[] getStack() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        int i = 0;
        while (!(stack[i].getClassName().equals(NAME)))  i++;
        return Arrays.copyOfRange(stack, i + 1, stack.length);
    }

    private static void filterStacks(Throwable e) {
        if (e == null) return;

        filterStacks(e.getCause());
        for (Throwable t: e.getSuppressed()) filterStacks(t);

        StackTraceElement[] frames = e.getStackTrace();
        int redacted = 0;
        for (int i = 0; i < frames.length; i++) {
            StackTraceElement frame = frames[i];
            if (frame.getClassName().equals(NAME)) break;
            if (!(frame.getClassName().equals(INNER_NAME))) continue;
            frames[i] = null;
            redacted++;
        }

        if (redacted == 0) return;

        StackTraceElement[] newFrames = new StackTraceElement[frames.length - redacted];
        int i = 0;
        for (StackTraceElement frame: frames) {
            if (frame == null) continue;
            newFrames[i] = frame;
            i++;
        }
        e.setStackTrace(newFrames);
    }
}
