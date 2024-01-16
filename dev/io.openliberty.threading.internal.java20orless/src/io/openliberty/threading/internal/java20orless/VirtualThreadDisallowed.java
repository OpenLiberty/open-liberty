/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.threading.internal.java20orless;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.threading.VirtualThreadOps;

/**
 * A placeholder for the absence of virtual thread operations prior to Java 21.
 */
@Component(name = "io.openliberty.threading.internal.java20orless.VirtualThreadDisallowed",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           service = VirtualThreadOps.class)
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
    public final boolean isSupported() {
        return false;
    }

    @Override
    public boolean isVirtual(Thread thread) {
        throw new UnsupportedOperationException();
    }
}