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
package io.openliberty.threading.virtual.internal;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.propertytypes.SatisfyingConditionTarget;
import org.osgi.service.condition.Condition;

import io.openliberty.threading.virtual.VirtualThreadOps;

/**
 * Makes Jakarta Data's Repository annotation into a bean defining annotation.
 */
@Component(name = "io.openliberty.threading.virtual.internal.VirtualThreadOperations",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           service = VirtualThreadOps.class)
@SatisfyingConditionTarget("(&(" + Condition.CONDITION_ID + "=io.openliberty.java.version)(io.openliberty.java.version>=21))")
public class VirtualThreadOperations implements VirtualThreadOps {

    @Activate // TODO remove
    protected void activate() {
        System.out.println("KJA1017 - VirtualThreadOperations activated");
    }

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

    @Override
    public boolean isSupported() {
        return true;
    }
}