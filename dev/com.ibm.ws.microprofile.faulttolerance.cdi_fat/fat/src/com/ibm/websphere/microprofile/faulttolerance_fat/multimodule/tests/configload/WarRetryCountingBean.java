/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package com.ibm.websphere.microprofile.faulttolerance_fat.multimodule.tests.configload;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Retry;

/**
 * An application scoped bean which counts calls per request
 * <p>
 * This bean lives in a war and so should have access to that war's config
 */
@ApplicationScoped
public class WarRetryCountingBean {

    @RequestScoped
    public static class Counter {
        private final AtomicInteger counter = new AtomicInteger(0);

        public AtomicInteger getCounter() {
            return counter;
        }
    }

    @Inject
    Counter counter;

    @Retry(maxRetries = 3)
    public void call() throws CountingException {
        throw new CountingException(counter.getCounter().incrementAndGet());
    }

}
