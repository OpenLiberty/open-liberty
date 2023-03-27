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
import io.openliberty.build.update.UpdateRunner;
import io.openliberty.build.update.util.Logger;

/**
 * Update which adjusts the dependencies of the first "pom.xml" entry
 * in archives in a target directory. Recursively update archives in
 * the target directory and its sub-directories.
 *
 * See {@link UpdatePomJar} for more information.
 */
public class UpdatePomJars extends UpdateDirectory {
    public static UpdateFactory createUpdateFactory() {
        return new UpdateFactory() {
            @Override
            public Class<? extends Update> getUpdateClass() {
                return UpdatePomJars.class;
            }

            @Override
            public UpdatePomJars createUpdate(File targetFile, File tmpDir, Logger logger, boolean failOnError) {
                return new UpdatePomJars(targetFile, tmpDir, logger, failOnError);
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

    public UpdatePomJars(File rootDir, File tmpDir) {
        super(rootDir, tmpDir);
    }

    public UpdatePomJars(File rootDir, File tmpDir, boolean failOnError) {
        super(rootDir, tmpDir, failOnError);
    }

    public UpdatePomJars(File rootDir, File tmpDir, Logger logger) {
        super(rootDir, tmpDir, logger);
    }

    public UpdatePomJars(File rootDir, File tmpDir, Logger logger, boolean failOnError) {
        super(rootDir, tmpDir, logger, failOnError);
    }

    //

    /**
     * Select all ".jar" files.
     */
    @Override
    public String getCriteria() {
        return "**/*.jar";
    }

    @Override
    public boolean select(String targetPath) {
        // 'endsWith' works fine, since the base name
        // of the file is ignored.
        return targetPath.endsWith(".jar");
    }

    @Override
    public UpdatePomJar createChildUpdate(File targetFile) throws Exception {
        return new UpdatePomJar(targetFile, getTmpDir(), getLogger(), getFailOnError());
    }
}
