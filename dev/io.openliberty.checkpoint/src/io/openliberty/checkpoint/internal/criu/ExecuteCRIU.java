/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.checkpoint.internal.criu;

import java.io.File;

public interface ExecuteCRIU {

    /**
     * Invokes the criu dump for the specified image directory, log file,
     * working directory, and env properties file.
     *
     * @param prepare
     * @param restore
     * @param imageDir
     * @param logFileName
     * @param workDir
     * @param envProps
     * @param unprivileged
     * @throws CheckpointFailedException
     */
    default void dump(Runnable prepare, Runnable restore, File imageDir, String logFileName, File workDir, File envProps, boolean unprivileged) throws CheckpointFailedException {
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
