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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.resource.spi.work.WorkContext;
import jakarta.resource.spi.work.WorkContextLifecycleListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This a fake work context that we made up for testing purposes.
 * It's just a per-thread java.util.Collection that can be
 * established via work context inflow.
 */
public class CollectionContext implements WorkContext, WorkContextLifecycleListener {
    private static final long serialVersionUID = -1987625019489770023L;
    private static final TraceComponent tc = Tr.register(CollectionContext.class);

    static final String NAME = "collectionContext";

    private final Collection<String> collection;
    private final transient ConcurrentLinkedQueue<String> contextSetupFailures = new ConcurrentLinkedQueue<String>();
    private final transient AtomicInteger contextSetupsCompleted = new AtomicInteger();

    public CollectionContext(String... elements) {
        super();
        collection = Collections.unmodifiableList(Arrays.asList(elements));
    }

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

    /**
     * @return a collection of elements that should be propagated to the collectionContext of the thread
     */
    public Collection<String> getCollection() {
        return collection;
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
        return "This fake thread context is a per-thread java.util.Collection";
    }

    /**
     * @see jakarta.resource.spi.work.WorkContext#getName()
     */
    @Override
    public String getName() {
        return NAME;
    }
}