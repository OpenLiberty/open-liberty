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
package com.ibm.ws.concurrent.mp.cdi.context;

import static java.util.Collections.emptySet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;

import org.eclipse.microprofile.concurrent.spi.ThreadContextController;
import org.eclipse.microprofile.concurrent.spi.ThreadContextSnapshot;
import org.jboss.weld.context.WeldAlterableContext;
import org.jboss.weld.context.api.ContextualInstance;
import org.jboss.weld.context.bound.BoundConversationContext;
import org.jboss.weld.context.bound.BoundLiteral;
import org.jboss.weld.context.bound.BoundRequestContext;
import org.jboss.weld.context.bound.BoundSessionContext;
import org.jboss.weld.context.bound.MutableBoundRequest;
import org.jboss.weld.contexts.cache.RequestScopedCache;
import org.jboss.weld.manager.api.WeldManager;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.mp.cdi.ConcurrencyCDIExtension;

@Trivial
public class WeldContextSnapshot implements ThreadContextSnapshot {

    private static final TraceComponent tc = Tr.register(ConcurrencyCDIExtension.class);

    private final WeldManager manager;
    private final ContextualInstanceSnapshot contextToApply;

    public WeldContextSnapshot(boolean propagate, WeldManager manager) {
        this.manager = manager;
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

        RequestScopedCache.invalidate();

        return () -> {
            // This will run after a task has executed and will clean up the CDI context
            if (existingContexts.reqCtx != null)
                existingContexts.reqCtx.clearAndSet(existingContexts.reqInstances);
            else
                requestCtx.deactivate();

            if (existingContexts.sesCtx != null)
                existingContexts.sesCtx.clearAndSet(existingContexts.sesInstances);
            else
                sessionCtx.deactivate();

            if (existingContexts.conCtx != null)
                existingContexts.conCtx.clearAndSet(contextToApply.conInstances);
            else
                conversationCtx.deactivate();

            RequestScopedCache.invalidate();
        };
    }

    /**
     * Captures a snapshot of the set of ContextualInstances for the currently active
     * WeldAlterableContexts for the Request, Session, and Conversation scope.
     */
    @Trivial
    private static class ContextualInstanceSnapshot {
        public static final ContextualInstanceSnapshot EMPTY_SNAPSHOT = new ContextualInstanceSnapshot();

        public final WeldAlterableContext reqCtx;
        public final WeldAlterableContext sesCtx;
        public final WeldAlterableContext conCtx;

        public final Collection<ContextualInstance<?>> reqInstances;
        public final Collection<ContextualInstance<?>> sesInstances;
        public final Collection<ContextualInstance<?>> conInstances;

        private ContextualInstanceSnapshot() {
            reqInstances = sesInstances = conInstances = emptySet();
            reqCtx = sesCtx = conCtx = null;
        }

        public ContextualInstanceSnapshot(WeldManager manager) {
            WeldAlterableContext reqContext = null;
            WeldAlterableContext sesContext = null;
            WeldAlterableContext conContext = null;
            Collection<ContextualInstance<?>> reqInstances = null;
            Collection<ContextualInstance<?>> sesInstances = null;
            Collection<ContextualInstance<?>> conInstances = null;
            for (WeldAlterableContext ctx : manager.getActiveWeldAlterableContexts()) {
                Class<?> scope = ctx.getScope();
                if (scope == RequestScoped.class) {
                    reqInstances = ctx.getAllContextualInstances();
                    reqContext = ctx;
                } else if (scope == SessionScoped.class) {
                    sesInstances = ctx.getAllContextualInstances();
                    sesContext = ctx;
                } else if (scope == ConversationScoped.class) {
                    conInstances = ctx.getAllContextualInstances();
                    conContext = ctx;
                }
            }
            this.reqCtx = reqContext;
            this.sesCtx = sesContext;
            this.conCtx = conContext;
            this.reqInstances = reqInstances == null ? emptySet() : reqInstances;
            this.sesInstances = sesInstances == null ? emptySet() : sesInstances;
            this.conInstances = conInstances == null ? emptySet() : conInstances;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(super.toString())
                            .append('\n')
                            .append("RequestScoped instances = ")
                            .append(this.reqInstances)
                            .append('\n')
                            .append("SessionScoped instances = ")
                            .append(this.reqInstances)
                            .append('\n')
                            .append("ConversationScoped instances = ")
                            .append(this.reqInstances);
            return sb.toString();
        }
    }
}
