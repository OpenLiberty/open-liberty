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
package io.openliberty.threading.internal.java21;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.threading.VirtualThreadOps;

/**
 * Makes Jakarta Data's Repository annotation into a bean defining annotation.
 */
@Component(name = "io.openliberty.threading.internal.java21.VirtualThreadOperations",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           service = VirtualThreadOps.class)
public class VirtualThreadOperations implements VirtualThreadOps {
    @Override
    public ThreadFactory createFactoryOfVirtualThreads(String namePrefix,
                                                       long initialCountValue,
                                                       boolean inherit,
                                                       UncaughtExceptionHandler uncaughtHandler) {
        Thread.Builder builder = Thread.ofVirtual().name(namePrefix, initialCountValue).inheritInheritableThreadLocals(inherit);
        if (uncaughtHandler != null)
            builder = builder.uncaughtExceptionHandler(uncaughtHandler);
        return builder.factory();
    }

    @Override
    public Thread createVirtualThread(String name,
                                      boolean inherit,
                                      UncaughtExceptionHandler uncaughtHandler,
                                      Runnable runnable) {
        Thread.Builder builder = Thread.ofVirtual().name(name).inheritInheritableThreadLocals(inherit);
        if (uncaughtHandler != null)
            builder = builder.uncaughtExceptionHandler(uncaughtHandler);
        return builder.unstarted(runnable);
    }

    @Override
    public boolean isVirtual(Thread thread) {
        return thread.isVirtual();
    }
}