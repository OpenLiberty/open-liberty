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

import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.smallrye.config.inject.ConfigExtension;

@Component(service = WebSphereCDIExtension.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" }, immediate = true)
public class OLSmallRyeConfigExtension extends ConfigExtension implements Extension, WebSphereCDIExtension {
    static final CheckpointPhase phase = CheckpointPhase.getPhase();
    static final ThreadLocal<AtomicBoolean> recordingStopped = new ThreadLocal<AtomicBoolean>() {
        @Override
        protected AtomicBoolean initialValue() {
            return new AtomicBoolean();
        }
    };

    @Override
    protected void validate(@Observes AfterDeploymentValidation adv) {
        boolean stopped = stopRecordingReads();
        try {
            super.validate(adv);
        } finally {
            startRecordingReads(stopped);
        }
    }

    static void startRecordingReads(boolean stopped) {
        if (!stopped || phase == null || phase.restored()) {
            return;
        }
        recordingStopped.get().set(false);
    }

    static boolean stopRecordingReads() {
        if (phase == null || phase.restored()) {
            return false;
        }
        return recordingStopped.get().compareAndSet(false, true);
    }

    public static boolean isRecording() {
        if (phase == null || phase.restored()) {
            return false;
        }
        return !recordingStopped.get().get();
    }

}
