/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
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

import static com.ibm.wsspi.kernel.service.condition.StartPhaseCondition.SERVICE_LATE_FILTER;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.propertytypes.SatisfyingConditionTarget;
import org.osgi.service.condition.Condition;

import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.wsspi.threading.ThreadTypeOverride;

import io.openliberty.threading.virtual.VirtualThreadOps;

/**
 * Virtual thread-related operations that are available because the Java level
 * is at least Java SE 21.
 */
@Component(name = "io.openliberty.threading.virtual.internal.VirtualThreadOperations",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           service = VirtualThreadOps.class)
@SatisfyingConditionTarget("(&(" + Condition.CONDITION_ID + "=" + JavaInfo.CONDITION_ID + ")(" + JavaInfo.CONDITION_ID + ">=21))")
public class VirtualThreadOperations implements VirtualThreadOps {
    private volatile ThreadTypeOverride overrideService;

    @Activate
    public VirtualThreadOperations(@Reference(target = SERVICE_LATE_FILTER) Condition servicesLate) {
        // NOTE normally a condition for activate would be expressed with a @SatisfyingConditionTarget annotation
        // on the component class.  This component already has a @SatisfyingConditionTarget for the Java version
        // and there is no good way to combine more than one distinct condition into a single filter.
    }

    @Reference(
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               unbind = "unsetOverrideService")
    protected void setOverrideService(ThreadTypeOverride vtos) {
        overrideService = vtos;
    }

    protected void unsetOverrideService(ThreadTypeOverride vtos) {
        if (overrideService == vtos)
            overrideService = null;
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

    @Override
    public boolean isVirtualThreadCreationEnabled() {
        ThreadTypeOverride override = overrideService;
        if (!ProductInfo.getBetaEdition()) {
            return true;
        }
        return override == null ? true : override.allowVirtualThreadCreation();
    }
}