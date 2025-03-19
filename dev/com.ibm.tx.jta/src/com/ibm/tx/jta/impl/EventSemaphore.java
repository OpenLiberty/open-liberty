/*
 * Copyright (c) 2002, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.tx.jta.impl;

import java.util.concurrent.Phaser;

/**
 * The EventSemaphore interface provides operations that wait for and post an
 * event semaphore.
 * <p>
 * This is specifically to handle the situation where the event may have been
 * posted before the wait method is called. This behaviour is not supported by
 * the existing wait and notify methods.
 */
public final class EventSemaphore {
    private final Phaser phaser = new Phaser(1);
    private volatile int phase = phaser.getPhase();

    public void waitEvent() {
        phaser.awaitAdvance(phase);
    }

    public void post() {
        phaser.arrive();
    }

    public void clear() {
        phase = phaser.getPhase();
    }
}
