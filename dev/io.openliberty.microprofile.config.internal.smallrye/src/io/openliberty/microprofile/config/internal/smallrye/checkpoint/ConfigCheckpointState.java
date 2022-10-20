/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal.smallrye.checkpoint;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.inject.spi.Extension;

import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.smallrye.config.inject.ConfigExtension;

public class ConfigCheckpointState extends ConfigExtension implements Extension, WebSphereCDIExtension {
    private static final CheckpointPhase PHASE = CheckpointPhase.getPhase();
    private static final UnpauseRecording NO_OP_CLOSABLE = new UnpauseRecording(false);
    private static final UnpauseRecording UNPAUSE_RECORDING_CLOSABLE = new UnpauseRecording(true);
    private static final ThreadLocal<AtomicBoolean> RECORDING_PAUSED = new ThreadLocal<AtomicBoolean>() {
        @Override
        protected AtomicBoolean initialValue() {
            return new AtomicBoolean();
        }
    };

    /**
     * An AutoCloseable to unpause recording of config reads.
     */
    public static class UnpauseRecording implements AutoCloseable {
        private final boolean doClose;

        UnpauseRecording(boolean doClose) {
            this.doClose = doClose;
        }

        @Override
        public void close() {
            if (doClose) {
                RECORDING_PAUSED.get().set(false);
            }
        }
    }

    /**
     * Pauses the recording of configuration value reads.
     *
     * @return Returns a closeable that is used to unpause recording of reads.
     */
    public static UnpauseRecording pauseRecordingReads() {
        if (PHASE.restored() || !RECORDING_PAUSED.get().compareAndSet(false, true)) {
            return NO_OP_CLOSABLE;
        }
        return UNPAUSE_RECORDING_CLOSABLE;
    }

    /**
     *
     * @return Returns true if recording of config reads is enabled, otherwise false.
     */
    public static boolean isRecording() {
        if (PHASE.restored()) {
            return false;
        }
        return !RECORDING_PAUSED.get().get();
    }
}
