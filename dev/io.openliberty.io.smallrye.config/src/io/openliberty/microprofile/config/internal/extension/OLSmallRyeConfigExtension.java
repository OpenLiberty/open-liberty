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
package io.openliberty.microprofile.config.internal.extension;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Extension;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.smallrye.config.inject.ConfigExtension;

@Component(service = WebSphereCDIExtension.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" }, immediate = true)
public class OLSmallRyeConfigExtension extends ConfigExtension implements Extension, WebSphereCDIExtension {
    private static final CheckpointPhase phase = CheckpointPhase.getPhase();
    private static final ThreadLocal<AtomicBoolean> recordingPaused = new ThreadLocal<AtomicBoolean>() {
        @Override
        protected AtomicBoolean initialValue() {
            return new AtomicBoolean();
        }
    };

    /**
     * An AutoCloseable to unpause recording of config reads.
     */
    static class UnpauseRecording implements AutoCloseable {
        private final boolean doClose;

        UnpauseRecording(boolean doClose) {
            this.doClose = doClose;
        }

        @Override
        public void close() {
            if (doClose) {
                recordingPaused.get().set(false);
            }
        }
    }

    private static final UnpauseRecording noOpCloseable = new UnpauseRecording(false);
    private static final UnpauseRecording unpauseRecordingCloseable = new UnpauseRecording(true);

    @Override
    @FFDCIgnore(Throwable.class) // Ignoring Throwable because try-with-resources block adds implicit catch(Throwable) which we want to ignore
    protected void validate(@Observes AfterDeploymentValidation adv) {
        // Do not record the configuration values read during the super.validate(). Start recording the values read after this method.
        // We want to record the configuration values only when the application reads it, therefore pausing it
        try (UnpauseRecording unpauseRecording = pauseRecordingReads()) {
            super.validate(adv);
        }
    }

    /**
     * Pauses the recording of configuration value reads.
     *
     * @return Returns a closeable that is used to unpause recording of reads.
     */
    static UnpauseRecording pauseRecordingReads() {
        if (phase.restored() || !recordingPaused.get().compareAndSet(false, true)) {
            return noOpCloseable;
        }
        return unpauseRecordingCloseable;
    }

    /**
     *
     * @return Returns true if recording of config reads is enabled, otherwise false.
     */
    public static boolean isRecording() {
        if (phase.restored()) {
            return false;
        }
        return !recordingPaused.get().get();
    }

}
