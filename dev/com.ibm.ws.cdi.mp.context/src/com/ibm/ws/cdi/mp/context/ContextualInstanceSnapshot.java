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

import static java.util.Collections.emptySet;

import java.util.Collection;

import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;

import org.jboss.weld.context.WeldAlterableContext;
import org.jboss.weld.context.api.ContextualInstance;
import org.jboss.weld.manager.api.WeldManager;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Captures a snapshot of the set of ContextualInstances for the currently active
 * WeldAlterableContexts for the Request, Session, and Conversation scope.
 */
@Trivial
public class ContextualInstanceSnapshot {
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

    public int getBeanCount() {
        return reqInstances.size() +
               sesInstances.size() +
               conInstances.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString())
                        .append(" beanCount=" + getBeanCount())
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