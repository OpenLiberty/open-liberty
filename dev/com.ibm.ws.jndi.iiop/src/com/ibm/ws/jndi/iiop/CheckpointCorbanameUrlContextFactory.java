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
package com.ibm.ws.jndi.iiop;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.OperationNotSupportedException;
import javax.naming.spi.ObjectFactory;

import org.osgi.framework.ServiceReference;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.condition.Condition;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jndi.WSName;
import com.ibm.ws.jndi.internal.Messages;
import com.ibm.ws.jndi.internal.ContextNode.CheckpointHookForRemoteContext;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;

import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@Component(configurationPolicy=IGNORE,property={"service.vendor=ibm","osgi.jndi.url.scheme=corbaname"})
public class CheckpointCorbanameUrlContextFactory extends CorbanameUrlContextFactory implements ObjectFactory, ApplicationRecycleComponent {

    static final TraceComponent tc = Tr.register(CheckpointCorbanameUrlContextFactory.class);

    private volatile ServiceReference<Condition> beforeCheckpointCondition = null;

    @Reference(service = Condition.class, //
               target = "(" + Condition.CONDITION_ID + "=" + CheckpointPhase.CONDITION_BEFORE_CHECKPOINT_ID + ")")
    protected void setBeforeCheckpointCondition(ServiceReference<Condition> beforeCheckpointCondition) {
        this.beforeCheckpointCondition = beforeCheckpointCondition;
    }

    protected void unsetBeforeCheckpointCondition(ServiceReference<Condition> runningCondition) {
        this.beforeCheckpointCondition = null;
    }

    @Override
    public Object getObjectInstance(Object o, Name n, Context c, Hashtable<?, ?> env) throws Exception {
        final String methodName = "getObjectInstance(): ";
        // by OSGi JNDI spec Name and Context should be null
        // if they are not then this code is being called in
        // the wrong way
        if (n != null || c != null)
            return null;

        // Object is String, String[] or null
        // Hashtable contains any environment properties

        if (o == null) {
            if (tc.isDebugEnabled()) Tr.debug(tc, methodName + "object was null - returning new OrbContext.");
//            registerCaller();
            throw new CheckpointOperationNotSupportedException();  // Set the hook and dump the call stack into a message.
//            return new OrbContext(orbRef.getORB(), env);
        }

        if (o instanceof String) {
            if (tc.isDebugEnabled()) Tr.debug(tc, methodName + "object was a string - performing a lookup on new OrbContext");
//            registerCaller();
            throw new CheckpointOperationNotSupportedException();  // See the hook and dump the call stack into a message.
//            return new OrbContext(orbRef.getORB(), env).lookup((String) o);
        }

        if (o instanceof String[]) {
            if (tc.isDebugEnabled()) Tr.debug(tc, methodName + "object was a string[] - ignoring");
        }

        throw new OperationNotSupportedException();
    }
    /**
     * Fail checkpoint whenever a remote intermediate context does exist.
     */
    private static class CheckpointHookForCorbanameContext implements CheckpointHook {

        @Override
        public void prepare() {
            throw new IllegalStateException(Messages.formatMessage("jndi.remote.context.failed.checkpoint", contextName.toString(), appName));
        }

        private final WSName contextName;
        private final String appName;

        private CheckpointHookForRemoteContext(WSName contextName, String appName) {
            this.contextName = contextName;
            this.appName = appName;
        }

        private static final AtomicBoolean alreadyAdded = new AtomicBoolean(false);

        private static void add(WSName contextName, String appName) {
            if (alreadyAdded.compareAndSet(false, true)) {
                CheckpointPhase.getPhase().addMultiThreadedHook(new CheckpointHookForRemoteContext(contextName, appName));
            }
        }
    }

}
