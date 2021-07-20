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
package io.openliberty.jigawatts.internal;

import java.io.File;
import java.io.IOException;

import org.openjdk.jigawatts.Jigawatts;
import org.osgi.service.component.annotations.Component;

import io.openliberty.checkpoint.internal.criu.ExecuteCRIU;
import io.openliberty.checkpoint.spi.SnapshotResult;
import io.openliberty.checkpoint.spi.SnapshotResult.SnapshotResultType;

// let other implementations win by using low ranking
@Component(property = "service.ranking:Integer=-100")
public class ExecuteCRIU_JNI implements ExecuteCRIU {

    @Override
    public SnapshotResult dump(File directory) {
        try {
            Jigawatts.saveTheWorld(directory.getAbsolutePath());
            return new SnapshotResult(SnapshotResultType.SUCCESS, "Success", null);
        } catch (IOException e) {
            return new SnapshotResult(SnapshotResultType.SNAPSHOT_FAILED, "The criu dump command failed with error: Snapshot Failed", e);
        }
    }

}
