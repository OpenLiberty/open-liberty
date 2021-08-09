/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * Trace method calls
 */
public class Traced<T> {
    public static Container trace(Container container) {
        return container == null ? null : new TracedContainer(container);
    }

    public static Entry trace(Entry entry) {
        return entry == null ? null : new TracedEntry(entry);
    }

    final T delegate;

    private final String me;

    private Traced(T delegate) {
        this.delegate = delegate;
        this.me = this.getClass().getName() + "[" + delegate + "]";
    }

    private static String join(Object... args) {
        return Arrays.toString(args).replaceFirst("^\\[(.*)\\]$", "$1");
    }

    private interface MethodTrace {
        /** normal method exit with a return value: e.g. <code>return mt.exit(result) */
        <R> R exit(R result);

        /** normal method exit when return type is void */
        void exit();

        /** called from finally block to note that the method has completed (normally or otherwise) */
        void end();
    }

    final MethodTrace traceMethod(final String method, final Object... args) {
        return new MethodTrace() {
            boolean exitedNormally;

            @Override
            public <R> R exit(R result) {
                exitedNormally = true;
                System.err.println("### " + me + method + "(" + join(args) + ") returned " + result + ".");
                return result;
            }

            @Override
            public void exit() {
                exitedNormally = true;
                System.err.println("### " + me + method + "(" + join(args) + ") returned.");
            }

            @Override
            public void end() {
                if (!!!exitedNormally)
                    System.err.println("!!! " + me + method + "(" + join(args) + ") exited abnormally.");
            }
        };
    }

    /** Trace a container and any iterators or entries it returns */
    private static class TracedContainer extends Traced<Container> implements Container {

        public TracedContainer(Container container) {
            super(container);
        }

        @Override
        public <T> T adapt(Class<T> adaptTarget) throws UnableToAdaptException {
            final MethodTrace mt = traceMethod("adapt", adaptTarget);
            try {
                return mt.exit(delegate.adapt(adaptTarget));
            } finally {
                mt.end();
            }
        }

        @Override
        public Iterator<Entry> iterator() {
            final MethodTrace mt = traceMethod("iterator");
            try {
                return new TracedIterator<Entry>(mt.exit(delegate.iterator())) {
                    @Override
                    public Entry next() {
                        return trace(super.next());
                    }
                };
            } finally {
                mt.end();
            }
        }

        @Override
        public Entry getEntry(String path) {
            final MethodTrace mt = traceMethod("getEntry", path);
            try {
                return trace(mt.exit(delegate.getEntry(path)));
            } finally {
                mt.end();
            }
        }

        @Override
        public String getName() {
            final MethodTrace mt = traceMethod("getName");
            try {
                return mt.exit(delegate.getName());
            } finally {
                mt.end();
            }
        }

        @Override
        public String getPath() {
            final MethodTrace mt = traceMethod("getPath");
            try {
                return mt.exit(delegate.getPath());
            } finally {
                mt.end();
            }
        }

        @Override
        public Container getEnclosingContainer() {
            final MethodTrace mt = traceMethod("getEnclosingContainer");
            try {
                return mt.exit(delegate.getEnclosingContainer());
            } finally {
                mt.end();
            }
        }

        @Override
        public boolean isRoot() {
            final MethodTrace mt = traceMethod("isRoot");
            try {
                return mt.exit(delegate.isRoot());
            } finally {
                mt.end();
            }
        }

        @Override
        public Container getRoot() {
            final MethodTrace mt = traceMethod("getRoot");
            try {
                return mt.exit(delegate.getRoot());
            } finally {
                mt.end();
            }
        }

        @Override
        public Collection<URL> getURLs() {
            final MethodTrace mt = traceMethod("getURLs");
            try {
                return mt.exit(delegate.getURLs());
            } finally {
                mt.end();
            }
        }

        @Override
        @SuppressWarnings("deprecation")
        public String getPhysicalPath() {
            final MethodTrace mt = traceMethod("getPhysicalPath");
            try {
                return mt.exit(delegate.getPhysicalPath());
            } finally {
                mt.end();
            }
        }
    }

    /** Trace an entry and any containers it returns */
    private static class TracedEntry extends Traced<Entry> implements Entry {

        TracedEntry(Entry e) {
            super(e);
        }

        @Override
        public String getName() {
            MethodTrace mt = traceMethod("getName");
            try {
                return mt.exit(delegate.getName());
            } finally {
                mt.end();
            }
        }

        @Override
        public <T> T adapt(Class<T> adaptTarget) throws UnableToAdaptException {
            MethodTrace mt = traceMethod("adapt", adaptTarget);
            try {
                T result = mt.exit(delegate.adapt(adaptTarget));
                return (adaptTarget == Container.class) ? adaptTarget.cast(trace((Container) result)) : result;
            } finally {
                mt.end();
            }
        }

        @Override
        public String getPath() {
            MethodTrace mt = traceMethod("getPath");
            try {
                return mt.exit(delegate.getPath());
            } finally {
                mt.end();
            }
        }

        @Override
        public long getSize() {
            MethodTrace mt = traceMethod("getSize");
            try {
                return mt.exit(delegate.getSize());
            } finally {
                mt.end();
            }
        }

        @Override
        public long getLastModified() {
            MethodTrace mt = traceMethod("getLastModified");
            try {
                return mt.exit(delegate.getLastModified());
            } finally {
                mt.end();
            }
        }

        @Override
        public URL getResource() {
            MethodTrace mt = traceMethod("getResource");
            try {
                return mt.exit(delegate.getResource());
            } finally {
                mt.end();
            }
        }

        @SuppressWarnings("deprecation")
        @Override
        public String getPhysicalPath() {
            MethodTrace mt = traceMethod("getPhysicalPath");
            try {
                return mt.exit(delegate.getPhysicalPath());
            } finally {
                mt.end();
            }
        }

        @Override
        public Container getEnclosingContainer() {
            MethodTrace mt = traceMethod("getEnclosingContainer");
            try {
                return mt.exit(delegate.getEnclosingContainer());
            } finally {
                mt.end();
            }
        }

        @Override
        public Container getRoot() {
            MethodTrace mt = traceMethod("getRoot");
            try {
                return mt.exit(delegate.getRoot());
            } finally {
                mt.end();
            }
        }
    }

    /** Trace an iterator. Override the next() method to trace whatever it returns */
    private static abstract class TracedIterator<T> extends Traced<Iterator<T>> implements Iterator<T> {
        public TracedIterator(Iterator<T> i) {
            super(i);
        }

        @Override
        public boolean hasNext() {
            MethodTrace mt = traceMethod("hasNext");
            try {
                return mt.exit(delegate.hasNext());
            } finally {
                mt.end();
            }
        }

        @Override
        public T next() {
            MethodTrace mt = traceMethod("next");
            try {
                return mt.exit(delegate.next());
            } finally {
                mt.end();
            }
        }

        @Override
        public void remove() {
            MethodTrace mt = traceMethod("remove");
            try {
                delegate.remove();
                mt.exit();
            } finally {
                mt.end();
            }
        }
    }

    @Override
    public final boolean equals(Object o) {
        // WARNING: fails if compared directly to non-traced object
        return o instanceof Traced ? delegate.equals(((Traced<?>) o).delegate) : false;
    }

    @Override
    public final int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public final String toString() {
        return delegate.toString();
    }
}
