/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.internal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.ServiceReference;

/**
 *
 */
final class DelayedMBeanHolder {

    /**
     * Current registration state of this DelayedMBeanHolder
     */
    public final AtomicReference<DelayedMBeanRegistrationState> registrationState =
                    new AtomicReference<DelayedMBeanRegistrationState>(DelayedMBeanRegistrationState.DELAYED);

    /**
     * Latch for threads to wait on while this DelayedMBeanHolder is in PROCESSING state.
     */
    public final CountDownLatch processingCompleteSignal = new CountDownLatch(1);

    private final ServiceReference<?> ref;

    public DelayedMBeanHolder(ServiceReference<?> ref) {
        this.ref = ref;
    }

    /**
     * @return the ref
     */
    public ServiceReference<?> getRef() {
        return ref;
    }

}
