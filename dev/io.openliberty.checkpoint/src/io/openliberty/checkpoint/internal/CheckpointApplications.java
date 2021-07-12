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

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.feature.ServerReadyStatus;

import io.openliberty.checkpoint.spi.Checkpoint;
import io.openliberty.checkpoint.spi.Checkpoint.Phase;
import io.openliberty.checkpoint.spi.SnapshotResult;
import io.openliberty.checkpoint.spi.SnapshotResult.SnapshotResultType;

@Component(property = Constants.SERVICE_RANKING + ":Integer=-10000")
public class CheckpointApplications implements ServerReadyStatus {

    @SuppressWarnings("unused")
    private static final TraceComponent tc = Tr.register(CheckpointApplications.class);
    private final Checkpoint checkpoint;
    private final boolean doCheckpoint;

    @Activate
    public CheckpointApplications(@Reference Checkpoint checkpoint, BundleContext bc) {
        this.checkpoint = checkpoint;
        this.doCheckpoint = "applications".equals(bc.getProperty(Checkpoint.CHECKPOINT_PROPERTY_NAME));
    }

    @Override
    public void check() {
        if (doCheckpoint) {

            SnapshotResult snapshotResult = checkpoint.snapshot(Phase.APPLICATIONS);

            if (snapshotResult.getType() == SnapshotResultType.SNAPSHOT_FAILED) {
                new Thread(() -> System.exit(SnapshotResultType.SNAPSHOT_FAILED.getCode()), "Snapshot exit.").start();
            }
            // TODO should we always exit on failure?
        }
    }

}
