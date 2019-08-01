/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.mp.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.jboss.weld.context.api.ContextualInstance;
import org.jboss.weld.context.bound.BoundConversationContext;
import org.jboss.weld.context.bound.BoundLiteral;
import org.jboss.weld.context.bound.BoundRequestContext;
import org.jboss.weld.context.bound.BoundSessionContext;
import org.jboss.weld.context.bound.MutableBoundRequest;
import org.jboss.weld.manager.api.WeldManager;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

@Trivial
public class WeldContextSnapshot implements ThreadContextSnapshot {

    private static final TraceComponent tc = Tr.register(WeldContextSnapshot.class);

    private final boolean propagate;
    private final WeldManager manager;
    private final ContextualInstanceSnapshot contextToApply;

    public WeldContextSnapshot(boolean propagate, WeldManager manager) {
        this.manager = manager;
        this.propagate = propagate;
        contextToApply = propagate ? new ContextualInstanceSnapshot(manager) : ContextualInstanceSnapshot.EMPTY_SNAPSHOT;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Snapshotted contextual instances to apply:", contextToApply);
    }

    @Override
    public ThreadContextController begin() {
        // Before we apply CDI context to the thread, take a snapshot of any existing
        // CDI context so that it may be restored after the task has executed
        final ContextualInstanceSnapshot existingContexts = new ContextualInstanceSnapshot(manager);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Snapshotted contextual instances to restore:", existingContexts);

        // Only lookup existing contexts if we don't obtain them through getActiveWeldAlterableContexts()
        BoundRequestContext requestCtx = existingContexts.reqCtx == null ? manager.instance().select(BoundRequestContext.class, BoundLiteral.INSTANCE).get() : null;
        BoundSessionContext sessionCtx = existingContexts.sesCtx == null ? manager.instance().select(BoundSessionContext.class, BoundLiteral.INSTANCE).get() : null;
        BoundConversationContext conversationCtx = existingContexts.conCtx == null ? manager.instance().select(BoundConversationContext.class, BoundLiteral.INSTANCE).get() : null;

        // Activate contexts and propagate all contexts that have some bean in them
        // If a given scope is already active, simply set the ContextualInstances
        // If a given scope is not active yet, activate it and then set the ContextualInstances
        Map<String, Object> requestMap = new HashMap<>();
        Map<String, Object> sessionMap = new HashMap<>();
        if (existingContexts.reqCtx != null) {
            existingContexts.reqCtx.clearAndSet(contextToApply.reqInstances);
        } else {
            requestCtx.associate(requestMap);
            requestCtx.activate();
            requestCtx.clearAndSet(contextToApply.reqInstances);
        }

        if (existingContexts.sesCtx != null) {
            existingContexts.sesCtx.clearAndSet(contextToApply.sesInstances);
        } else {
            sessionCtx.associate(sessionMap);
            sessionCtx.activate();
            sessionCtx.clearAndSet(contextToApply.sesInstances);
        }

        if (existingContexts.conCtx != null) {
            existingContexts.conCtx.clearAndSet(contextToApply.conInstances);
        } else {
            conversationCtx.associate(new MutableBoundRequest(requestMap, sessionMap));
            conversationCtx.activate();
            conversationCtx.clearAndSet(contextToApply.conInstances);
        }

        return () -> {
            // This will run after a task has executed and will restore the CDI context that was originally on the thread
            ContextualInstanceSnapshot afterTaskContexts = propagate ? new ContextualInstanceSnapshot(manager) : null;

            if (existingContexts.reqCtx != null)
                existingContexts.reqCtx.clearAndSet(existingContexts.reqInstances);
            else
                requestCtx.deactivate();

            if (existingContexts.sesCtx != null)
                existingContexts.sesCtx.clearAndSet(existingContexts.sesInstances);
            else
                sessionCtx.deactivate();

            if (existingContexts.conCtx != null)
                existingContexts.conCtx.clearAndSet(existingContexts.conInstances);
            else
                conversationCtx.deactivate();

            if (propagate && contextToApply.getBeanCount() != afterTaskContexts.getBeanCount()) {
                Set<ContextualInstance<?>> lazilyRegisteredBeans = new HashSet<>();
                lazilyRegisteredBeans.addAll(afterTaskContexts.reqInstances);
                lazilyRegisteredBeans.addAll(afterTaskContexts.sesInstances);
                lazilyRegisteredBeans.addAll(afterTaskContexts.conInstances);
                lazilyRegisteredBeans.removeAll(contextToApply.reqInstances);
                lazilyRegisteredBeans.removeAll(contextToApply.sesInstances);
                lazilyRegisteredBeans.removeAll(contextToApply.conInstances);

                Tr.error(tc, "CWWKC1158.cannot.lazy.enlist.beans", lazilyRegisteredBeans);
                throw new IllegalStateException(Tr.formatMessage(tc, "CWWKC1158.cannot.lazy.enlist.beans", lazilyRegisteredBeans));
            }
        };
    }
}
