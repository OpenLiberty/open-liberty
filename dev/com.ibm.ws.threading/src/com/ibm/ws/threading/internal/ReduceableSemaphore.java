/*******************************************************************************
 * Copyright (c) 2017,2021 IBM Corporation and others.
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
package com.ibm.ws.threading.internal;

import java.util.concurrent.Semaphore;

/**
 * Make the reducePermits method of Semaphore available for use by non-subclasses.
 */
public class ReduceableSemaphore extends Semaphore {
    private static final long serialVersionUID = 1L;

    public ReduceableSemaphore(int numPermits, boolean fair) {
        super(numPermits, fair);
    }

    @Override // to make visible
    public void reducePermits(int reduction) {
        super.reducePermits(reduction);
    }
}
