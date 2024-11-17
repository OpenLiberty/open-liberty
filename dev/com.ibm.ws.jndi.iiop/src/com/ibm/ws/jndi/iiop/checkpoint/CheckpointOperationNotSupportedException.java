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

import java.util.concurrent.atomic.AtomicBoolean;

import javax.naming.OperationNotSupportedException;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointPhase;

public class CheckpointOperationNotSupportedException extends OperationNotSupportedException {

    private static final long serialVersionUID = -8671859750182405735L;

    private static final TraceComponent tc = Tr.register(CheckpointOperationNotSupportedException.class);

    public CheckpointOperationNotSupportedException(String appName) {
        super(getMessage("jndi.orb.context.failed.checkpoint", appName));
        if (!CheckpointPhase.getPhase().restored()) {
            CheckpointHookForOrbContext.add(appName);
        }
    }

    @Trivial
    public static final String getMessage(String key, Object... params) {
        return TraceNLS.getFormattedMessage(CheckpointOperationNotSupportedException.class, tc.getResourceBundleName(), key, params, key);
    }

    /**
     * Fail checkpoint when attempting to access the ORB context.
     */
    private static class CheckpointHookForOrbContext implements CheckpointHook {

        @Override
        public void prepare() {
            throw new IllegalStateException(getMessage("jndi.orb.context.failed.checkpoint", appName));
        }

        private final String appName;

        private CheckpointHookForOrbContext(String appName) {
            this.appName = appName;
        }

        private static final AtomicBoolean alreadyAdded = new AtomicBoolean(false);

        private static void add(String appName) {
            if (alreadyAdded.compareAndSet(false, true)) {
                CheckpointPhase.getPhase().addMultiThreadedHook(new CheckpointHookForOrbContext(appName));
            }
        }
    }
}
