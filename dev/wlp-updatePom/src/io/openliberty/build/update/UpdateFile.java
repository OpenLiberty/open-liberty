/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.build.update;

import java.io.File;

import io.openliberty.build.update.util.Logger;

/**
 * Main implementation for updating a single file.
 *
 * This implementation does nothing to the target file,
 * and always answers 0, indicating that the target was
 * not update.
 */
public class UpdateFile extends UpdateImpl {

    public UpdateFile(File targetFile, File tmpDir) {
        super(targetFile, tmpDir);
    }

    public UpdateFile(File targetFile, File tmpDir, boolean failOnError) {
        super(targetFile, tmpDir, failOnError);
    }

    public UpdateFile(File targetFile, File tmpDir, Logger logger) {
        super(targetFile, tmpDir, logger);
    }

    public UpdateFile(File targetFile, File tmpDir, Logger logger, boolean failOnError) {
        super(targetFile, tmpDir, logger, failOnError);
    }

    //

    @Override
    public int run() throws Exception {
        String m = "run";

        if (getFailOnError()) {
            return basicRun();
        } else {
            try {
                return basicRun();
            } catch (Exception e) {
                log(m, "Update failure: [ " + getTargetFile().getAbsolutePath() + " ]:");
                e.printStackTrace();
                return 0;
            }
        }
    }

    public int basicRun() throws Exception {
        String m = "basicRun";
        log(m, "Stub: [ " + getTargetFile().getAbsolutePath() + " ]");
        return 0;
    }
}