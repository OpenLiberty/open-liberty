/*******************************************************************************
 * Copyright (c) 2012,2022 IBM Corporation and others.
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
package test.jca.adapter;

import java.util.concurrent.LinkedBlockingQueue;

import jakarta.resource.spi.work.WorkEvent;
import jakarta.resource.spi.work.WorkListener;

/**
 * WorkListener that puts WorkEvents onto a queue
 */
public class BVTWorkListener extends LinkedBlockingQueue<WorkEvent> implements WorkListener {

    private static final long serialVersionUID = 8401602940362261779L;

    @Override
    public void workAccepted(WorkEvent event) {
        add(event);
    }

    @Override
    public void workCompleted(WorkEvent event) {
        add(event);
    }

    @Override
    public void workRejected(WorkEvent event) {
        add(event);
    }

    @Override
    public void workStarted(WorkEvent event) {
        add(event);
    }
}
