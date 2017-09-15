/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.util.concurrent.CountDownLatch;

import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;

public class FATWorkListener implements WorkListener {
    Exception failure;
    final CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void workAccepted(WorkEvent event) {}

    @Override
    public void workCompleted(WorkEvent event) {
        failure = event.getException();
        latch.countDown();
    }

    @Override
    public void workRejected(WorkEvent event) {
        failure = event.getException();
        latch.countDown();
    }

    @Override
    public void workStarted(WorkEvent event) {}
}
