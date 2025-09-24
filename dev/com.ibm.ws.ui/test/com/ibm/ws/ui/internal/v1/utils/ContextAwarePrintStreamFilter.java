/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.ui.internal.v1.utils;

import java.io.PrintStream;
import java.util.Locale;

/**
 * A PrintStream wrapper that will forward print() calls except for those
 * which originate (directly or indirectly) from a particular package. This
 * stream can be used to silence noisy 3rd party components that write
 * unconditionally to System.out and System.err.
 */
public final class ContextAwarePrintStreamFilter extends PrintStream {

    private final PrintStream out;
    private final String packageName;

    /**
     * @param out
     */
    public ContextAwarePrintStreamFilter(PrintStream out, String packageName) {
        super(out);
        this.out = out;
        this.packageName = packageName + '.';
    }

    @Override
    public PrintStream append(char c) {
        if (!isCalledFromForbiddenPackage()) {
            out.append(c);
        }
        return this;
    }

    @Override
    public PrintStream append(CharSequence csq) {
        if (!isCalledFromForbiddenPackage()) {
            out.append(csq);
        }
        return this;
    }

    @Override
    public PrintStream append(CharSequence csq, int start, int end) {
        if (!isCalledFromForbiddenPackage()) {
            out.append(csq, start, end);
        }
        return this;
    }

    @Override
    public PrintStream format(Locale l, String format, Object... args) {
        if (!isCalledFromForbiddenPackage()) {
            out.format(l, format, args);
        }
        return this;
    }

    @Override
    public PrintStream format(String format, Object... args) {
        if (!isCalledFromForbiddenPackage()) {
            out.format(format, args);
        }
        return this;
    }

    @Override
    public void print(boolean x) {
        if (!isCalledFromForbiddenPackage()) {
            out.print(x);
        }
    }

    @Override
    public void print(char x) {
        if (!isCalledFromForbiddenPackage()) {
            out.print(x);
        }
    }

    @Override
    public void print(char[] x) {
        if (!isCalledFromForbiddenPackage()) {
            out.print(x);
        }
    }

    @Override
    public void print(double x) {
        if (!isCalledFromForbiddenPackage()) {
            out.print(x);
        }
    }

    @Override
    public void print(float x) {
        if (!isCalledFromForbiddenPackage()) {
            out.print(x);
        }
    }

    @Override
    public void print(int x) {
        if (!isCalledFromForbiddenPackage()) {
            out.print(x);
        }
    }

    @Override
    public void print(long x) {
        if (!isCalledFromForbiddenPackage()) {
            out.print(x);
        }
    }

    @Override
    public void print(Object x) {
        if (!isCalledFromForbiddenPackage()) {
            out.print(x);
        }
    }

    @Override
    public void print(String x) {
        if (!isCalledFromForbiddenPackage()) {
            out.print(x);
        }
    }

    @Override
    public PrintStream printf(Locale l, String format, Object... args) {
        if (!isCalledFromForbiddenPackage()) {
            out.printf(l, format, args);
        }
        return this;
    }

    @Override
    public PrintStream printf(String format, Object... args) {
        if (!isCalledFromForbiddenPackage()) {
            out.printf(format, args);
        }
        return this;
    }

    @Override
    public void println() {
        if (!isCalledFromForbiddenPackage()) {
            out.println();
        }
    }

    @Override
    public void println(boolean x) {
        if (!isCalledFromForbiddenPackage()) {
            out.println(x);
        }
    }

    @Override
    public void println(char x) {
        if (!isCalledFromForbiddenPackage()) {
            out.println(x);
        }
    }

    @Override
    public void println(char[] x) {
        if (!isCalledFromForbiddenPackage()) {
            out.println(x);
        }
    }

    @Override
    public void println(double x) {
        if (!isCalledFromForbiddenPackage()) {
            out.println(x);
        }
    }

    @Override
    public void println(float x) {
        if (!isCalledFromForbiddenPackage()) {
            out.println(x);
        }
    }

    @Override
    public void println(int x) {
        if (!isCalledFromForbiddenPackage()) {
            out.println(x);
        }
    }

    @Override
    public void println(long x) {
        if (!isCalledFromForbiddenPackage()) {
            out.println(x);
        }
    }

    @Override
    public void println(Object x) {
        if (!isCalledFromForbiddenPackage()) {
            out.println(x);
        }
    }

    @Override
    public void println(String x) {
        if (!isCalledFromForbiddenPackage()) {
            out.println(x);
        }
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        if (!isCalledFromForbiddenPackage()) {
            out.write(buf, off, len);
        }
    }

    @Override
    public void write(int b) {
        if (!isCalledFromForbiddenPackage()) {
            out.write(b);
        }
    }

    /**
     * Returns true if one of the ancestor calls on the stack originates
     * from any class from the specified package or any of its sub-packages.
     */
    private boolean isCalledFromForbiddenPackage() {
        for (final StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            final String className = ste.getClassName();
            if (className != null && className.startsWith(packageName)) {
                return true;
            }
        }
        return false;
    }
}
