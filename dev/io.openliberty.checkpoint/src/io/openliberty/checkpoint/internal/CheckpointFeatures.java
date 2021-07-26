/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.internal;

import java.util.concurrent.Future;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.runtime.update.RuntimeUpdateListener;
import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;

import io.openliberty.checkpoint.internal.Checkpoint;
import io.openliberty.checkpoint.spi.CheckpointHookFactory.Phase;
import io.openliberty.checkpoint.spi.SnapshotFailed;
import io.openliberty.checkpoint.spi.SnapshotFailed.Type;

@Component
public class CheckpointFeatures implements RuntimeUpdateListener {

    private final Checkpoint checkpoint;
    private final boolean doCheckpoint;

    @Activate
    public CheckpointFeatures(@Reference Checkpoint checkpoint, BundleContext bc) {
        this.checkpoint = checkpoint;
        this.doCheckpoint = "features".equals(bc.getProperty(BootstrapConstants.CHECKPOINT_PROPERTY_NAME));
    }

    @Override
    public void notificationCreated(RuntimeUpdateManager updateManager, RuntimeUpdateNotification notification) {
        if (doCheckpoint && RuntimeUpdateNotification.APPLICATIONS_STARTING.equals(notification.getName())) {
            notification.onCompletion(new CompletionListener<Boolean>() {
                @Override
                public void successfulCompletion(Future<Boolean> future, Boolean result) {
                    if (result) {
                        try {
                            checkpoint.snapshot(Phase.FEATURES);
                        } catch (SnapshotFailed e) {
                            if (e.getType() == Type.SNAPSHOT_FAILED) {
                                new Thread(() -> System.exit(e.getErrorCode()), "Snapshot exit.").start();
                            }
                            // TODO should we always exit on failure?
                        }
                    }
                }

                @Override
                public void failedCompletion(Future<Boolean> future, Throwable t) {
                }
            });
        }
    }

}
