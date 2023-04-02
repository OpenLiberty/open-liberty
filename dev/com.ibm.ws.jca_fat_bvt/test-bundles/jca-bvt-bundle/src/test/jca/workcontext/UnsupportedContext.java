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
package test.jca.workcontext;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.resource.spi.work.WorkContext;
import jakarta.resource.spi.work.WorkContextLifecycleListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * A fake work inflow context that isn't supported by WAS.
 * This can be used to test how WAS handles work inflow context that it doesn't recognize.
 */
public class UnsupportedContext implements WorkContext, WorkContextLifecycleListener {
    private static final long serialVersionUID = -2852835964851397650L;
    private static final TraceComponent tc = Tr.register(UnsupportedContext.class);

    static final String NAME = "unsupportedContext";

    private final transient ConcurrentLinkedQueue<String> contextSetupFailures = new ConcurrentLinkedQueue<String>();
    private final transient AtomicInteger contextSetupsCompleted = new AtomicInteger();

    /**
     * @see jakarta.resource.spi.work.WorkContextLifecycleListener#contextSetupComplete()
     */
    @Override
    public void contextSetupComplete() {
        int num = contextSetupsCompleted.incrementAndGet();
        Tr.debug(this, tc, "contextSetupComplete #" + num);
    }

    /**
     * @see jakarta.resource.spi.work.WorkContextLifecycleListener#contextSetupFailed(java.lang.String)
     */
    @Override
    public void contextSetupFailed(String errorCode) {
        contextSetupFailures.add(errorCode);
        Tr.debug(this, tc, "contextSetupFailed: " + errorCode);
    }

    public int getContextSetupsCompleted() {
        return contextSetupsCompleted.get();
    }

    public String[] getContextSetupFailures() {
        return contextSetupFailures.toArray(new String[contextSetupFailures.size()]);
    }

    /**
     * @see jakarta.resource.spi.work.WorkContext#getDescription()
     */
    @Override
    public String getDescription() {
        return "This thread context isn't supported by the application server";
    }

    /**
     * @see jakarta.resource.spi.work.WorkContext#getName()
     */
    @Override
    public String getName() {
        return NAME;
    }
}
