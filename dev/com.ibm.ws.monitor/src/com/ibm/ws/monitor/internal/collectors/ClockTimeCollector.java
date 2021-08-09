/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.monitor.internal.collectors;

public class ClockTimeCollector {

    long previous;

    long current;

    public long getPrevious() {
        return previous;
    }

    public long getCurrent() {
        return current;
    }

    public long getElapsed() {
        return current - previous;
    }

    public void begin() {
        previous = current;
        current = System.nanoTime();
    }

    public void end() {
        previous = current;
        current = System.nanoTime();
    }
}
