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
 * POMs processor:
 *
 * Filter development dependencies from a POM stored in archives beneath
 * a target directory.
 *
 * Iteration is done here instead of in gradle to avoid extra java executions,
 * which are expensive.
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

    @Override
    public String getCriteria() {
        return "**/pom.xml";
    }

    @Override
    public boolean select(String targetPath) {
        return targetPath.equals("pom.xml") || targetPath.endsWith("/pom.xml");
    }

    @Override
    public UpdateFile createChildUpdate(File targetFile) throws Exception {
        return new UpdatePom(targetFile, getTmpDir(), getLogger(), getFailOnError());
    }
}
