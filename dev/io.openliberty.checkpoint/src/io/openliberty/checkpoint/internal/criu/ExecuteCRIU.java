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
package io.openliberty.checkpoint.internal.criu;

import java.io.File;

public interface ExecuteCRIU {

    /**
     * Invokes the criu dump for the specified image directory, log file and
     * working directory
     *
     * @param imageDir
     * @param logFileName
     * @param workDir
     * @throws CheckpointFailedException
     */
    default void dump(File imageDir, String logFileName, File workDir) throws CheckpointFailedException {
        // do nothing
    };

    /**
     * Asserts that the current JVM and platform has CRIU support which is needed to checkpoint/restore.
     *
     * @throws CheckpointFailedException with details of the platform or jvm incompatibility if CRIU is not supported
     * @see io.openliberty.checkpoint.internal.criu.CheckpointFailedException.TYPE
     */
    default void checkpointSupported() throws CheckpointFailedException {
        // do nothing
    };
}
