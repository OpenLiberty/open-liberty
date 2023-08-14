/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package concurrent.cu3.web;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.inject.Produces;

/**
 * The producer in this class is valid prior to Jakarta Concurrency 3.1
 * in Jakarta EE 11, at which point injection of the Concurrency resources
 * without qualifiers is reserved for the default instances.
 * This test class is isolated under a separate package so that it can be
 * included in the application only when testing Jakarta EE 10 or lower features.
 */
public class UnqualifiedResourcesProducer {

    @PostConstruct
    public void init() {
        System.out.println("DefaultResourcesProducer initialized");
    }

    @Resource(lookup = "concurrent/timeoutExecutor")
    @Produces
    private ManagedExecutorService exec;
}
