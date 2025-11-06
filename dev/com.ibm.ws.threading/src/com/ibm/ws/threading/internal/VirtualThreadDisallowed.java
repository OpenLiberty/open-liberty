/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
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

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.propertytypes.SatisfyingConditionTarget;
import org.osgi.service.condition.Condition;

import com.ibm.ws.kernel.service.util.JavaInfo;

import io.openliberty.threading.virtual.VirtualThreadOps;

/**
 * Rejects operations related to virtual threads because the Java level is less
 * than Java SE 21.
 */
@Component(name = "com.ibm.ws.threading.internal.VirtualThreadDisallowed",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           service = VirtualThreadOps.class)
@SatisfyingConditionTarget("(&(" + Condition.CONDITION_ID + "=" + JavaInfo.CONDITION_ID + ")(!(" + JavaInfo.CONDITION_ID + ">=21)))")
public class VirtualThreadDisallowed implements VirtualThreadOps {

    @Override
    public ThreadFactory createFactoryOfVirtualThreads(String namePrefix,
                                                       long initialCountValue,
                                                       boolean inherit,
                                                       UncaughtExceptionHandler uncaughtHandler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Thread createVirtualThread(String name,
                                      boolean inherit,
                                      UncaughtExceptionHandler uncaughtHandler,
                                      Runnable runnable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSupported() {
        return false;
    }

    @Override
    public boolean isVirtualThreadCreationEnabled() {
        return false;
    }

    @Override
    public boolean isVirtual(Thread thread) {
        throw new UnsupportedOperationException();
    }
}