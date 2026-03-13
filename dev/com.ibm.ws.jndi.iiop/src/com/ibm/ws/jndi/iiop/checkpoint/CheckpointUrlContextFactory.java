/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.jndi.iiop.checkpoint;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.condition.Condition;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.checkpoint.spi.CheckpointPhase;

public abstract class CheckpointUrlContextFactory implements ObjectFactory {

    static final TraceComponent tc = Tr.register(CheckpointUrlContextFactory.class);

    protected volatile ServiceReference<Condition> beforeCheckpointCondition = null;

    // When satisfied this condition enables the immediate
    // activation of IIOP URL context factories during server
    // checkpoint. It is equivalent to applying the following
    // annotation to extensions of CheckpointURLContextFactory.
    // @SatisfyingConditionTarget("(" + CONDITION_ID + "=" + CheckpointPhase.CONDITION_BEFORE_CHECKPOINT_ID + ")")
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

        // By OSGi JNDI spec, Name and Context should be null.
        // If they are not then this code is being called in
        // the wrong way
        if (n != null || c != null)
            return null;

        // Object is null, String, or String[]. We don't care.
        if (tc.isDebugEnabled()) {
            if (o == null) {
                Tr.debug(tc, methodName + "object was null - operation not supported for checkpoint.");
            } else if (o instanceof String) {
                Tr.debug(tc, methodName + "object was a string - operation not supported for checkpoint");
            } else if (o instanceof String[]) {
                Tr.debug(tc, methodName + "object was a string[] - operation not supported for checkpoint");
            }
        }

        // Fail the lookup of the ORB context and prepare server
        // checkpoint to fail with error.
        throw new CheckpointOperationNotSupportedException(getApplicationName());
    }

    static String getApplicationName() {
        ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        return (cData != null) ? cData.getJ2EEName().getApplication() : "<null>";
    }

}
