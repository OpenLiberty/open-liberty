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
package io.openliberty.build.update.pom;

import java.io.File;

import io.openliberty.build.update.Update;
import io.openliberty.build.update.UpdateDirectory;
import io.openliberty.build.update.UpdateFactory;
import io.openliberty.build.update.UpdateFile;
import io.openliberty.build.update.UpdateRunner;
import io.openliberty.build.update.util.Logger;

/**
 * Update which adjusts the dependencies of all "pom.xml" files
 * in a target directory. Recursively update files in the target
 * directory and its sub-directories.
 *
 * See {@link UpdatePom} for more information.
 */
public class UpdatePomFiles extends UpdateDirectory {
    public static UpdateFactory createUpdateFactory() {
        return new UpdateFactory() {
            @Override
            public Class<? extends Update> getUpdateClass() {
                return UpdatePomFiles.class;
            }

            @Override
            public UpdatePomFiles createUpdate(File targetFile, File tmpDir, Logger logger, boolean failOnError) throws Exception {
                return new UpdatePomFiles(targetFile, tmpDir, logger, failOnError);
            }
        };
    }

    public static void main(String[] args) {
        try {
            @SuppressWarnings("unused")
            int updated = UpdateRunner.run(createUpdateFactory(), args);
        } catch (Exception e) {
            System.out.println("Failure");
            e.printStackTrace();
        }
    }

    //

    public UpdatePomFiles(File rootDir, File tmpDir, boolean failOnError) {
        super(rootDir, tmpDir, failOnError);
    }

    public UpdatePomFiles(File rootDir, File tmpDir) {
        super(rootDir, tmpDir);
    }

    public UpdatePomFiles(File rootDir, File tmpDir, Logger logger) {
        super(rootDir, tmpDir, logger);
    }

    public UpdatePomFiles(File rootDir, File tmpDir, Logger logger, boolean failOnError) {
        super(rootDir, tmpDir, logger, failOnError);
    }

    //

    /**
     * Select all "pom.xml" files.
     */
    @Override
    public String getCriteria() {
        return "**/*.pom";
    }

    @Override
    public boolean select(String targetPath) {
        // DO NOT USE 'endsWith', which would match "someDir/junkPom.xml".
        return targetPath.endsWith(".pom");
    }

    @Override
    public UpdateFile createChildUpdate(File targetFile) throws Exception {
        return new UpdatePom(targetFile, getTmpDir(), getLogger(), getFailOnError());
    }
}
