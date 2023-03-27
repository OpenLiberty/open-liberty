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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.maven.model.Model;

import io.openliberty.build.update.Update;
import io.openliberty.build.update.UpdateFactory;
import io.openliberty.build.update.UpdateFile;
import io.openliberty.build.update.UpdateRunner;
import io.openliberty.build.update.util.FileUtils;
import io.openliberty.build.update.util.Logger;

/**
 * Update which adjusts the dependencies of a single target POM file.
 *
 * Only select dependencies are removed, per {@link PomUtils#isDevGroup(Dependency)},
 * {@link PomUtils#filterGroup(Dependency)} and {@link PomUtils#filterArtifact(Dependency)}.
 *
 * If no dependencies were removed, the target file is not updated.
 *
 * If any dependencies were removed, rewrite the target file with the
 * dependences taken out. The write is done by writing to a temporary
 * location, then moving the new file in place of the original file.
 *
 * See:
 * https://maven.apache.org/guides/introduction/introduction-to-the-pom.html#What_is_a_POM
 */
public class UpdatePom extends UpdateFile {

    public static UpdateFactory createProcessFactory() {
        return new UpdateFactory() {
            @Override
            public Class<? extends Update> getUpdateClass() {
                return UpdatePom.class;
            }

            @Override
            public UpdatePom createUpdate(File targetFile, File tmpDir, Logger logger, boolean failOnError) throws Exception {
                return new UpdatePom(targetFile, tmpDir, logger, failOnError);
            }
        };
    }

    public static void main(String[] args) {
        try {
            @SuppressWarnings("unused")
            int updated = UpdateRunner.run(createProcessFactory(), args);
        } catch (Exception e) {
            System.out.println("Failure");
            e.printStackTrace();
        }
    }

    public UpdatePom(File jarFile, File tmpDir) {
        super(jarFile, tmpDir);
    }

    public UpdatePom(File jarFile, File tmpDir, boolean failOnError) {
        super(jarFile, tmpDir, failOnError);
    }

    public UpdatePom(File jarFile, File tmpDir, Logger logger) {
        super(jarFile, tmpDir, logger);
    }

    public UpdatePom(File jarFile, File tmpDir, Logger logger, boolean failOnError) {
        super(jarFile, tmpDir, logger, failOnError);
    }

    //

    /**
     * Main API:Read the POM from the target. Update the POM's dependencies.
     * If any dependencies were updated, rewrite the target with the POM updated
     * with the updated dependencies.
     *
     * Do nothing if the dependencies were not updated.
     *
     * @throws Exception Thrown if processing fails.
     */
    @Override
    public int basicRun() throws Exception {
        String m = "basicRun";

        logMark(m, "Reading POM");
        Model pomModel = readPomFromTarget();
        if (pomModel == null) {
            logTime(m, "No POM");
            return 0;
        }

        Model updatedPomModel = PomUtils.updateDependecies(pomModel);
        if (updatedPomModel == null) {
            logTime(m, "No updates to POM");
            return 0;
        }

        logTime(m, "Updating POM");
        replacePom(PomUtils.writePom(updatedPomModel));
        logTime(m, "Updated POM");

        return 1;
    }

    protected Model readPomFromTarget() throws Exception {
        try ( FileInputStream fileInputStream = new FileInputStream(getTargetFile()) ) {
            return PomUtils.readPom(fileInputStream);
        }
    }

    protected void replacePom(InputStream pomInput) throws IOException {
        File tmpDir = FileUtils.ensure(getTmpDir());

        File inputFile = getTargetFile();
        File outputFile = File.createTempFile(inputFile.getName(), null, tmpDir);

        byte[] transferBuffer = new byte[32 * 1024];
        FileUtils.write(pomInput, outputFile, transferBuffer);

        Path inputPath = Paths.get(inputFile.getPath());
        Path outputPath = Paths.get(outputFile.getPath());

        // TODO: Should this include 'StandardCopyOption.ATOMIC_MOVE'?
        // We want to be sure to never leave the target in a broken state.

        Files.move(inputPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
    }
}
