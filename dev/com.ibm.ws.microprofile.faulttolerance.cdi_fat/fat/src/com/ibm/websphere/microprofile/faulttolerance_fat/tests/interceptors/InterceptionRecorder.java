/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.interceptors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.interceptor.InvocationContext;

/**
 * Class to record interceptions by test interceptors
 * <p>
 * In instance of this class should be passed as the first argument of the intercepted method and each interceptor should call {@link #record(Class, InvocationContext)}
 */
public class InterceptionRecorder {

    private final List<InterceptionEntry> entries = Collections.synchronizedList(new ArrayList<>());

    private final long callerThreadId;

    public InterceptionRecorder() {
        callerThreadId = Thread.currentThread().getId();
    }

    public List<InterceptionEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    private void record(Class<?> interceptor) {
        InterceptionThread thread = callerThreadId == Thread.currentThread().getId() ? InterceptionThread.CALLER_THREAD : InterceptionThread.ASYNC_THREAD;
        InterceptionEntry entry = new InterceptionEntry(interceptor, thread);
        entries.add(entry);
    }

    public static void record(Class<?> interceptor, InvocationContext invocationContext) {
        Object[] params = invocationContext.getParameters();
        if (params.length > 0 && params[0] instanceof InterceptionRecorder) {
            InterceptionRecorder record = (InterceptionRecorder) params[0];
            record.record(interceptor);
        } else {
            throw new AssertionError("Tried to record an interception but the first parameter is not an InterceptionRecord");
        }
    }

    public enum InterceptionThread {
        CALLER_THREAD,
        ASYNC_THREAD
    }

    public static class InterceptionEntry {
        private final Class<?> interceptor;
        private final InterceptionThread thread;

        public InterceptionEntry(Class<?> interceptor, InterceptionThread thread) {
            super();
            this.interceptor = interceptor;
            this.thread = thread;
        }

        public Class<?> getInterceptor() {
            return interceptor;
        }

        public InterceptionThread getThread() {
            return thread;
        }

        @Override
        public String toString() {
            return "InterceptionEntry [interceptor=" + interceptor + ", thread=" + thread + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((interceptor == null) ? 0 : interceptor.hashCode());
            result = prime * result + ((thread == null) ? 0 : thread.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            InterceptionEntry other = (InterceptionEntry) obj;
            if (interceptor == null) {
                if (other.interceptor != null)
                    return false;
            } else if (!interceptor.equals(other.interceptor))
                return false;
            if (thread != other.thread)
                return false;
            return true;
        }
    }

}
