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
package io.openliberty.checkpoint.openj9.internal;

import java.io.File;

import org.eclipse.openj9.criu.CRIUSupport;
import org.eclipse.openj9.criu.CRIUSupport.CRIUResult;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.checkpoint.internal.criu.ExecuteCRIU;
import io.openliberty.checkpoint.spi.SnapshotResult;
import io.openliberty.checkpoint.spi.SnapshotResult.SnapshotResultType;

public class ExecuteCRIU_OpenJ9 implements BundleActivator, ExecuteCRIU {

    @Override
    @FFDCIgnore(ClassNotFoundException.class)
    public void start(BundleContext bc) {
        try {
            Class.forName("org.eclipse.openj9.criu.CRIUSupport");
            bc.registerService(ExecuteCRIU.class, this, null);
        } catch (ClassNotFoundException e) {
            // do nothing; not on open j9 that supports CRIU
        }
    }

    @Override
    public void stop(BundleContext bc) {

    }

    @Override
    public SnapshotResult dump(File directory) {
        SnapshotResult snapshotResult = new SnapshotResult(SnapshotResultType.SNAPSHOT_FAILED, "The criu dump command failed with error: Snapshot Failed", null);

        if (!CRIUSupport.isCRIUSupportEnabled()) {
            // TODO log appropriate message
            System.out.println("Must set the JVM option: -XX:+EnableCRIUSupport");
            return snapshotResult;
        }
        CRIUResult result = CRIUSupport.checkPointJVM(directory.toPath());

        switch (result.getType()) {
            case SUCCESS:
                snapshotResult = createSnapshotResult(SnapshotResultType.SUCCESS, "Success");
                break;

            case UNSUPPORTED_OPERATION:
                snapshotResult = createSnapshotResult(SnapshotResultType.UNSUPPORTED_OPERATION, "The criu dump command failed with error: Unsupported Operation");

            case INVALID_ARGUMENTS:
                snapshotResult = createSnapshotResult(SnapshotResultType.INVALID_ARGUMENTS, "The criu dump command failed with error: Invalid Arguments");
                break;

            case SYSTEM_CHECKPOINT_FAILURE:
                snapshotResult = createSnapshotResult(SnapshotResultType.SYSTEM_CHECKPOINT_FAILURE, "The criu dump command failed with error: System Checkpoint Failure");
                break;

            case JVM_CHECKPOINT_FAILURE:
                snapshotResult = createSnapshotResult(SnapshotResultType.JVM_CHECKPOINT_FAILURE, "The criu dump command failed with error: JVM Checkpoint Failure");
                break;

            case JVM_RESTORE_FAILURE:
                snapshotResult = createSnapshotResult(SnapshotResultType.JVM_RESTORE_FAILURE, "The criu dump command failed with error: JVM Restore Failure");
                break;

        }
        return snapshotResult;
    }

    /**
     * @return
     */
    private SnapshotResult createSnapshotResult(SnapshotResultType type, String msg) {
        SnapshotResult snapshotResult;
        snapshotResult = new SnapshotResult(type, msg, null);
        return snapshotResult;
    }

}
