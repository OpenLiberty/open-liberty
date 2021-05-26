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

import java.io.File;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.kernel.feature.ServerReadyStatus;

import io.openliberty.checkpoint.spi.Checkpoint;
import io.openliberty.checkpoint.spi.Checkpoint.Phase;
import io.openliberty.checkpoint.spi.SnapshotFailed;

@Component(property = Constants.SERVICE_RANKING + ":Integer=-10000")
public class CheckpointApplications implements ServerReadyStatus {

    private final Checkpoint checkpoint;
    private final boolean doCheckpoint;

    @Activate
    public CheckpointApplications(@Reference Checkpoint checkpoint, BundleContext bc) {
        this.checkpoint = checkpoint;
        this.doCheckpoint = "applications".equals(bc.getProperty("io.openliberty.checkpoint"));
    }

    @Override
    public void check() {
        if (doCheckpoint) {
            try {
                checkpoint.snapshot(Phase.APPLICATIONS, new File("snapshot"));
            } catch (SnapshotFailed e) {
            }
        }
    }

}
