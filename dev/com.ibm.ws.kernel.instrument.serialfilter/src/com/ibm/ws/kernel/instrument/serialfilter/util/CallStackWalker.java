/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.util;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Walks through the classes and method names on the current call stack,
 * correlating against the classes on the call stack as retrieved using
 * {@link SecurityManager#getClassContext()}.
 * <br>
 * Since not all entries in the call stack are actually associated with
 * Java classes at runtime, these call stack entries are skipped.
 */
public class CallStackWalker {
    @SuppressWarnings("unused")
    private static final StackIntrospector SI = StackIntrospector.newInstance();

    private final Deque<Class<?>> classes;
    private final Deque<StackTraceElement> elements;

    private CallStackWalker() {
        classes = new LinkedList<Class<?>>(Arrays.asList((Class<?>[]) SI.getClassContext()));
        elements = new LinkedList<StackTraceElement>(Arrays.asList(new Throwable().getStackTrace()));
        // step past the stack frames for this class
        while (classes.size() > 0 && classes.peek() != CallStackWalker.class) classes.pop();
        while (classes.size() > 0 && classes.peek() == CallStackWalker.class) classes.pop();
        final String name = CallStackWalker.class.getName();
        while (elements.size() > 0 && !!!name.equals(elements.peek().getClassName())) elements.pop();
        while (elements.size() > 0 && name.equals(elements.peek().getClassName())) elements.pop();
        skipClasslessStackFrames();
    }

    public static CallStackWalker forCurrentThread() {
        return new CallStackWalker();
    }

    /** Call after any advancement to bring this.elements into line with this.classes. */
    private void skipClasslessStackFrames() {
        // skip over any stack trace elements that are unmatched in the classes array
        if (classes.isEmpty()) return;
        while (elements.size() > 0 && !!!elements.peek().getClassName().equals(classes.peek().getName())) {
            elements.pop();
        }
    }

    public CallStackWalker skipTo(Class<?> firstInterestingClass) {
        while (size() > 0 && topClass() != firstInterestingClass) pop();
        return this;
    }

    public boolean isEmpty() {
        return classes.isEmpty();
    }

    public int size() {
        return classes.size();
    }

    public Class<?> topClass() {
        return classes.peek();
    }

    public String topMethod() {
        return elements.isEmpty() ? null : elements.peek().getMethodName();
    }

    public void pop() {
        classes.pop();
        if (elements.isEmpty()) return;
        elements.pop();
        skipClasslessStackFrames();
    }

    private static final class StackIntrospector extends SecurityManager {
        public Class[] getClassContext() {
            return super.getClassContext();
        }

        static StackIntrospector newInstance() {
            return AccessController.doPrivileged(Factory.INSTANCE);
        }

        private enum Factory implements PrivilegedAction<StackIntrospector> {
            INSTANCE;
            public StackIntrospector run() {
                return new StackIntrospector();
            }
        }
    }
}
